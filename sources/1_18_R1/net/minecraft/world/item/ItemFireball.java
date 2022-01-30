package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
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
    public EnumInteractionResult useOn(ItemActionContext itemactioncontext) {
        World world = itemactioncontext.getLevel();
        BlockPosition blockposition = itemactioncontext.getClickedPos();
        IBlockData iblockdata = world.getBlockState(blockposition);
        boolean flag = false;

        if (!BlockCampfire.canLight(iblockdata) && !CandleBlock.canLight(iblockdata) && !CandleCakeBlock.canLight(iblockdata)) {
            blockposition = blockposition.relative(itemactioncontext.getClickedFace());
            if (BlockFireAbstract.canBePlacedAt(world, blockposition, itemactioncontext.getHorizontalDirection())) {
                // CraftBukkit start - fire BlockIgniteEvent
                if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, itemactioncontext.getPlayer()).isCancelled()) {
                    if (!itemactioncontext.getPlayer().getAbilities().instabuild) {
                        itemactioncontext.getItemInHand().shrink(1);
                    }
                    return EnumInteractionResult.PASS;
                }
                // CraftBukkit end
                this.playSound(world, blockposition);
                world.setBlockAndUpdate(blockposition, BlockFireAbstract.getState(world, blockposition));
                world.gameEvent(itemactioncontext.getPlayer(), GameEvent.BLOCK_PLACE, blockposition);
                flag = true;
            }
        } else {
            // CraftBukkit start - fire BlockIgniteEvent
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition, org.bukkit.event.block.BlockIgniteEvent.IgniteCause.FIREBALL, itemactioncontext.getPlayer()).isCancelled()) {
                if (!itemactioncontext.getPlayer().getAbilities().instabuild) {
                    itemactioncontext.getItemInHand().shrink(1);
                }
                return EnumInteractionResult.PASS;
            }
            // CraftBukkit end
            this.playSound(world, blockposition);
            world.setBlockAndUpdate(blockposition, (IBlockData) iblockdata.setValue(BlockProperties.LIT, true));
            world.gameEvent(itemactioncontext.getPlayer(), GameEvent.BLOCK_PLACE, blockposition);
            flag = true;
        }

        if (flag) {
            itemactioncontext.getItemInHand().shrink(1);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return EnumInteractionResult.FAIL;
        }
    }

    private void playSound(World world, BlockPosition blockposition) {
        Random random = world.getRandom();

        world.playSound((EntityHuman) null, blockposition, SoundEffects.FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }
}
