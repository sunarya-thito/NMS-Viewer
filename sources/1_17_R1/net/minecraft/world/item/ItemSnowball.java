package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.level.World;

public class ItemSnowball extends Item {

    public ItemSnowball(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        // CraftBukkit - moved down
        // world.playSound((EntityHuman) null, entityhuman.locX(), entityhuman.locY(), entityhuman.locZ(), SoundEffects.SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            EntitySnowball entitysnowball = new EntitySnowball(world, entityhuman);

            entitysnowball.setItem(itemstack);
            entitysnowball.a(entityhuman, entityhuman.getXRot(), entityhuman.getYRot(), 0.0F, 1.5F, 1.0F);
            if (world.addEntity(entitysnowball)) {
                if (!entityhuman.getAbilities().instabuild) {
                    itemstack.subtract(1);
                }

                world.playSound((EntityHuman) null, entityhuman.locX(), entityhuman.locY(), entityhuman.locZ(), SoundEffects.SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            } else if (entityhuman instanceof net.minecraft.server.level.EntityPlayer) {
                ((net.minecraft.server.level.EntityPlayer) entityhuman).getBukkitEntity().updateInventory();
            }
        }
        // CraftBukkit end

        entityhuman.b(StatisticList.ITEM_USED.b(this));
        // CraftBukkit start - moved up
        /*
        if (!entityhuman.getAbilities().instabuild) {
            itemstack.subtract(1);
        }
        */

        return InteractionResultWrapper.a(itemstack, world.isClientSide());
    }
}
