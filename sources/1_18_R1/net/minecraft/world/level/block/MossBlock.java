package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class MossBlock extends Block implements IBlockFragilePlantElement {

    public MossBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return iblockaccess.getBlockState(blockposition.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        CaveFeatures.MOSS_PATCH_BONEMEAL.place(worldserver, worldserver.getChunkSource().getGenerator(), random, blockposition.above());
    }
}
