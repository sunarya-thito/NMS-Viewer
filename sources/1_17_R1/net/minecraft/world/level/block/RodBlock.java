package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class RodBlock extends BlockDirectional {

    protected static final float AABB_MIN = 6.0F;
    protected static final float AABB_MAX = 10.0F;
    protected static final VoxelShape Y_AXIS_AABB = Block.a(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.a(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D);
    protected static final VoxelShape X_AXIS_AABB = Block.a(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);

    protected RodBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        switch (((EnumDirection) iblockdata.get(RodBlock.FACING)).n()) {
            case X:
            default:
                return RodBlock.X_AXIS_AABB;
            case Z:
                return RodBlock.Z_AXIS_AABB;
            case Y:
                return RodBlock.Y_AXIS_AABB;
        }
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.set(RodBlock.FACING, enumblockrotation.a((EnumDirection) iblockdata.get(RodBlock.FACING)));
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return (IBlockData) iblockdata.set(RodBlock.FACING, enumblockmirror.b((EnumDirection) iblockdata.get(RodBlock.FACING)));
    }

    @Override
    public boolean a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        return false;
    }
}
