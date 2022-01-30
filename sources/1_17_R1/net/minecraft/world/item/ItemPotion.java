package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemPotion extends Item {

    private static final int DRINK_DURATION = 32;

    public ItemPotion(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public ItemStack createItemStack() {
        return PotionUtil.a(super.createItemStack(), Potions.WATER);
    }

    @Override
    public ItemStack a(ItemStack itemstack, World world, EntityLiving entityliving) {
        EntityHuman entityhuman = entityliving instanceof EntityHuman ? (EntityHuman) entityliving : null;

        if (entityhuman instanceof EntityPlayer) {
            CriterionTriggers.CONSUME_ITEM.a((EntityPlayer) entityhuman, itemstack);
        }

        if (!world.isClientSide) {
            List<MobEffect> list = PotionUtil.getEffects(itemstack);
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                MobEffect mobeffect = (MobEffect) iterator.next();

                if (mobeffect.getMobEffect().isInstant()) {
                    mobeffect.getMobEffect().applyInstantEffect(entityhuman, entityhuman, entityliving, mobeffect.getAmplifier(), 1.0D);
                } else {
                    entityliving.addEffect(new MobEffect(mobeffect), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.POTION_DRINK); // CraftBukkit
                }
            }
        }

        if (entityhuman != null) {
            entityhuman.b(StatisticList.ITEM_USED.b(this));
            if (!entityhuman.getAbilities().instabuild) {
                itemstack.subtract(1);
            }
        }

        if (entityhuman == null || !entityhuman.getAbilities().instabuild) {
            if (itemstack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (entityhuman != null) {
                entityhuman.getInventory().pickup(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        world.a((Entity) entityliving, GameEvent.DRINKING_FINISH, entityliving.cT());
        return itemstack;
    }

    @Override
    public int b(ItemStack itemstack) {
        return 32;
    }

    @Override
    public EnumAnimation c(ItemStack itemstack) {
        return EnumAnimation.DRINK;
    }

    @Override
    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        return ItemLiquidUtil.a(world, entityhuman, enumhand);
    }

    @Override
    public String j(ItemStack itemstack) {
        return PotionUtil.d(itemstack).b(this.getName() + ".effect.");
    }

    @Override
    public void a(ItemStack itemstack, @Nullable World world, List<IChatBaseComponent> list, TooltipFlag tooltipflag) {
        PotionUtil.a(itemstack, list, 1.0F);
    }

    @Override
    public boolean i(ItemStack itemstack) {
        return super.i(itemstack) || !PotionUtil.getEffects(itemstack).isEmpty();
    }

    @Override
    public void a(CreativeModeTab creativemodetab, NonNullList<ItemStack> nonnulllist) {
        if (this.a(creativemodetab)) {
            Iterator iterator = IRegistry.POTION.iterator();

            while (iterator.hasNext()) {
                PotionRegistry potionregistry = (PotionRegistry) iterator.next();

                if (potionregistry != Potions.EMPTY) {
                    nonnulllist.add(PotionUtil.a(new ItemStack(this), potionregistry));
                }
            }
        }

    }
}
