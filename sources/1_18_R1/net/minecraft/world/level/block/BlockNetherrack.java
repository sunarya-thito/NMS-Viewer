package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockNetherrack extends Block implements IBlockFragilePlantElement {

    public BlockNetherrack(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        if (!iblockaccess.getBlockState(blockposition.above()).propagatesSkylightDown(iblockaccess, blockposition)) {
            return false;
        } else {
            Iterator iterator = BlockPosition.betweenClosed(blockposition.offset(-1, -1, -1), blockposition.offset(1, 1, 1)).iterator();

            BlockPosition blockposition1;

            do {
                if (!iterator.hasNext()) {
                    return false;
                }

                blockposition1 = (BlockPosition) iterator.next();
            } while (!iblockaccess.getBlockState(blockposition1).is((Tag) TagsBlock.NYLIUM));

            return true;
        }
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        boolean flag = false;
        boolean flag1 = false;
        Iterator iterator = BlockPosition.betweenClosed(blockposition.offset(-1, -1, -1), blockposition.offset(1, 1, 1)).iterator();

        while (iterator.hasNext()) {
            BlockPosition blockposition1 = (BlockPosition) iterator.next();
            IBlockData iblockdata1 = worldserver.getBlockState(blockposition1);

            if (iblockdata1.is(Blocks.WARPED_NYLIUM)) {
                flag1 = true;
            }

            if (iblockdata1.is(Blocks.CRIMSON_NYLIUM)) {
                flag = true;
            }

            if (flag1 && flag) {
                break;
            }
        }

        if (flag1 && flag) {
            worldserver.setBlock(blockposition, random.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        } else if (flag1) {
            worldserver.setBlock(blockposition, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
        } else if (flag) {
            worldserver.setBlock(blockposition, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
        }

    }
}
