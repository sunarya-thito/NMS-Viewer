package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class ItemBlockWallable extends ItemBlock {

    public final Block wallBlock;

    public ItemBlockWallable(Block block, Block block1, Item.Info item_info) {
        super(block, item_info);
        this.wallBlock = block1;
    }

    @Nullable
    @Override
    protected IBlockData getPlacementState(BlockActionContext blockactioncontext) {
        IBlockData iblockdata = this.wallBlock.getStateForPlacement(blockactioncontext);
        IBlockData iblockdata1 = null;
        World world = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        EnumDirection[] aenumdirection = blockactioncontext.getNearestLookingDirections();
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            EnumDirection enumdirection = aenumdirection[j];

            if (enumdirection != EnumDirection.UP) {
                IBlockData iblockdata2 = enumdirection == EnumDirection.DOWN ? this.getBlock().getStateForPlacement(blockactioncontext) : iblockdata;

                if (iblockdata2 != null && iblockdata2.canSurvive(world, blockposition)) {
                    iblockdata1 = iblockdata2;
                    break;
                }
            }
        }

        return iblockdata1 != null && world.isUnobstructed(iblockdata1, blockposition, VoxelShapeCollision.empty()) ? iblockdata1 : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(this.wallBlock, item);
    }
}
