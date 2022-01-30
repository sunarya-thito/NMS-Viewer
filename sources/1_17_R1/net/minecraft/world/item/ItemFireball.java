package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemFireball extends Item {

    public ItemFireball(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public EnumInteractionResult a(ItemActionContext itemactioncontext) {
        World world = itemactioncontext.getWorld();
        BlockPosition blockposition = itemactioncontext.getClickPosition();
        IBlockData iblockdata = world.getType(blockposition);
        boolean flag = false;

        if (!BlockCampfire.h(iblockdata) && !CandleBlock.g(iblockdata) && !CandleCakeBlock.g(iblockdata)) {
            blockposition = blockposition.shift(itemactioncontext.getClickedFace());
            if (BlockFireAbstract.a(world, blockposition, itemactioncontext.g())) {
                // CraftBukkit start - fire BlockIgniteEvent
                if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, itemactioncontext.getEntity()).isCancelled()) {
                    if (!itemactioncontext.getEntity().getAbilities().instabuild) {
                        itemactioncontext.getItemStack().subtract(1);
                    }
                    return EnumInteractionResult.PASS;
                }
                // CraftBukkit end
                this.a(world, blockposition);
                world.setTypeUpdate(blockposition, BlockFireAbstract.a((IBlockAccess) world, blockposition));
                world.a((Entity) itemactioncontext.getEntity(), GameEvent.BLOCK_PLACE, blockposition);
                flag = true;
            }
        } else {
            // CraftBukkit start - fire BlockIgniteEvent
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, itemactioncontext.getEntity()).isCancelled()) {
                if (!itemactioncontext.getEntity().getAbilities().instabuild) {
                    itemactioncontext.getItemStack().subtract(1);
                }
                return EnumInteractionResult.PASS;
            }
            // CraftBukkit end
            this.a(world, blockposition);
            world.setTypeUpdate(blockposition, (IBlockData) iblockdata.set(BlockProperties.LIT, true));
            world.a((Entity) itemactioncontext.getEntity(), GameEvent.BLOCK_PLACE, blockposition);
            flag = true;
        }

        if (flag) {
            itemactioncontext.getItemStack().subtract(1);
            return EnumInteractionResult.a(world.isClientSide);
        } else {
            return EnumInteractionResult.FAIL;
        }
    }

    private void a(World world, BlockPosition blockposition) {
        Random random = world.getRandom();

        world.playSound((EntityHuman) null, blockposition, SoundEffects.FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
