package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidTypes;

public class BlockCoralFanWall extends BlockCoralFanWallAbstract {

    private final Block deadBlock;

    protected BlockCoralFanWall(Block block, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.deadBlock = block;
    }

    @Override
    public void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        this.a(iblockdata, (GeneratorAccess) world, blockposition);
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!e(iblockdata, worldserver, blockposition)) {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockFadeEvent(worldserver, blockposition, this.deadBlock.getBlockData().set(BlockCoralFanWall.WATERLOGGED, false).set(BlockCoralFanWall.FACING, iblockdata.get(BlockCoralFanWall.FACING))).isCancelled()) {
                return;
            }
            // CraftBukkit end
            worldserver.setTypeAndData(blockposition, (IBlockData) ((IBlockData) this.deadBlock.getBlockData().set(BlockCoralFanWall.WATERLOGGED, false)).set(BlockCoralFanWall.FACING, (EnumDirection) iblockdata.get(BlockCoralFanWall.FACING)), 2);
        }

    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (enumdirection.opposite() == iblockdata.get(BlockCoralFanWall.FACING) && !iblockdata.canPlace(generatoraccess, blockposition)) {
            return Blocks.AIR.getBlockData();
        } else {
            if ((Boolean) iblockdata.get(BlockCoralFanWall.WATERLOGGED)) {
                generatoraccess.getFluidTickList().a(blockposition, FluidTypes.WATER, FluidTypes.WATER.a((IWorldReader) generatoraccess));
            }

            this.a(iblockdata, generatoraccess, blockposition);
            return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
        }
    }
}
