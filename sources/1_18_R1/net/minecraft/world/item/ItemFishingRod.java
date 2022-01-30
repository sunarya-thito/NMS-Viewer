package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

import org.bukkit.event.player.PlayerFishEvent; // CraftBukkit

public class ItemFishingRod extends Item implements ItemVanishable {

    public ItemFishingRod(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.getItemInHand(enumhand);
        int i;

        if (entityhuman.fishing != null) {
            if (!world.isClientSide) {
                i = entityhuman.fishing.retrieve(itemstack);
                itemstack.hurtAndBreak(i, entityhuman, (entityhuman1) -> {
                    entityhuman1.broadcastBreakEvent(enumhand);
                });
            }

            world.playSound((EntityHuman) null, entityhuman.getX(), entityhuman.getY(), entityhuman.getZ(), SoundEffects.FISHING_BOBBER_RETRIEVE, SoundCategory.NEUTRAL, 1.0F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            world.gameEvent(entityhuman, GameEvent.FISHING_ROD_REEL_IN, (Entity) entityhuman);
        } else {
            // world.playSound((EntityHuman) null, entityhuman.getX(), entityhuman.getY(), entityhuman.getZ(), SoundEffects.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!world.isClientSide) {
                i = EnchantmentManager.getFishingSpeedBonus(itemstack);
                int j = EnchantmentManager.getFishingLuckBonus(itemstack);

                // CraftBukkit start
                EntityFishingHook entityfishinghook = new EntityFishingHook(entityhuman, world, j, i);
                PlayerFishEvent playerFishEvent = new PlayerFishEvent((org.bukkit.entity.Player) entityhuman.getBukkitEntity(), null, (org.bukkit.entity.FishHook) entityfishinghook.getBukkitEntity(), PlayerFishEvent.State.FISHING);
                world.getCraftServer().getPluginManager().callEvent(playerFishEvent);

                if (playerFishEvent.isCancelled()) {
                    entityhuman.fishing = null;
                    return InteractionResultWrapper.pass(itemstack);
                }
                world.playSound((EntityHuman) null, entityhuman.getX(), entityhuman.getY(), entityhuman.getZ(), SoundEffects.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
                world.addFreshEntity(entityfishinghook);
                // CraftBukkit end
            }

            entityhuman.awardStat(StatisticList.ITEM_USED.get(this));
            world.gameEvent(entityhuman, GameEvent.FISHING_ROD_CAST, (Entity) entityhuman);
        }

        return InteractionResultWrapper.sidedSuccess(itemstack, world.isClientSide());
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
