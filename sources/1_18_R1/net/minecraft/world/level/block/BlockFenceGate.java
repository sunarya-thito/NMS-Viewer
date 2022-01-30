package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockFenceGate extends BlockFacingHorizontal {

    public static final BlockStateBoolean OPEN = BlockProperties.OPEN;
    public static final BlockStateBoolean POWERED = BlockProperties.POWERED;
    public static final BlockStateBoolean IN_WALL = BlockProperties.IN_WALL;
    protected static final VoxelShape Z_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape X_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    protected static final VoxelShape Z_SHAPE_LOW = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
    protected static final VoxelShape X_SHAPE_LOW = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D);
    protected static final VoxelShape Z_COLLISION_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 24.0D, 10.0D);
    protected static final VoxelShape X_COLLISION_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 24.0D, 16.0D);
    protected static final VoxelShape Z_OCCLUSION_SHAPE = VoxelShapes.or(Block.box(0.0D, 5.0D, 7.0D, 2.0D, 16.0D, 9.0D), Block.box(14.0D, 5.0D, 7.0D, 16.0D, 16.0D, 9.0D));
    protected static final VoxelShape X_OCCLUSION_SHAPE = VoxelShapes.or(Block.box(7.0D, 5.0D, 0.0D, 9.0D, 16.0D, 2.0D), Block.box(7.0D, 5.0D, 14.0D, 9.0D, 16.0D, 16.0D));
    protected static final VoxelShape Z_OCCLUSION_SHAPE_LOW = VoxelShapes.or(Block.box(0.0D, 2.0D, 7.0D, 2.0D, 13.0D, 9.0D), Block.box(14.0D, 2.0D, 7.0D, 16.0D, 13.0D, 9.0D));
    protected static final VoxelShape X_OCCLUSION_SHAPE_LOW = VoxelShapes.or(Block.box(7.0D, 2.0D, 0.0D, 9.0D, 13.0D, 2.0D), Block.box(7.0D, 2.0D, 14.0D, 9.0D, 13.0D, 16.0D));

    public BlockFenceGate(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.stateDefinition.any()).setValue(BlockFenceGate.OPEN, false)).setValue(BlockFenceGate.POWERED, false)).setValue(BlockFenceGate.IN_WALL, false));
    }

    @Override
    public VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (Boolean) iblockdata.getValue(BlockFenceGate.IN_WALL) ? (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == EnumDirection.EnumAxis.X ? BlockFenceGate.X_SHAPE_LOW : BlockFenceGate.Z_SHAPE_LOW) : (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == EnumDirection.EnumAxis.X ? BlockFenceGate.X_SHAPE : BlockFenceGate.Z_SHAPE);
    }

    @Override
    public IBlockData updateShape(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        EnumDirection.EnumAxis enumdirection_enumaxis = enumdirection.getAxis();

        if (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getClockWise().getAxis() != enumdirection_enumaxis) {
            return super.updateShape(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
        } else {
            boolean flag = this.isWall(iblockdata1) || this.isWall(generatoraccess.getBlockState(blockposition.relative(enumdirection.getOpposite())));

            return (IBlockData) iblockdata.setValue(BlockFenceGate.IN_WALL, flag);
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN) ? VoxelShapes.empty() : (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == EnumDirection.EnumAxis.Z ? BlockFenceGate.Z_COLLISION_SHAPE : BlockFenceGate.X_COLLISION_SHAPE);
    }

    @Override
    public VoxelShape getOcclusionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return (Boolean) iblockdata.getValue(BlockFenceGate.IN_WALL) ? (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == EnumDirection.EnumAxis.X ? BlockFenceGate.X_OCCLUSION_SHAPE_LOW : BlockFenceGate.Z_OCCLUSION_SHAPE_LOW) : (((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == EnumDirection.EnumAxis.X ? BlockFenceGate.X_OCCLUSION_SHAPE : BlockFenceGate.Z_OCCLUSION_SHAPE);
    }

    @Override
    public boolean isPathfindable(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        switch (pathmode) {
            case LAND:
                return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);
            case WATER:
                return false;
            case AIR:
                return (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);
            default:
                return false;
        }
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        World world = blockactioncontext.getLevel();
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        boolean flag = world.hasNeighborSignal(blockposition);
        EnumDirection enumdirection = blockactioncontext.getHorizontalDirection();
        EnumDirection.EnumAxis enumdirection_enumaxis = enumdirection.getAxis();
        boolean flag1 = enumdirection_enumaxis == EnumDirection.EnumAxis.Z && (this.isWall(world.getBlockState(blockposition.west())) || this.isWall(world.getBlockState(blockposition.east()))) || enumdirection_enumaxis == EnumDirection.EnumAxis.X && (this.isWall(world.getBlockState(blockposition.north())) || this.isWall(world.getBlockState(blockposition.south())));

        return (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockFenceGate.FACING, enumdirection)).setValue(BlockFenceGate.OPEN, flag)).setValue(BlockFenceGate.POWERED, flag)).setValue(BlockFenceGate.IN_WALL, flag1);
    }

    private boolean isWall(IBlockData iblockdata) {
        return iblockdata.is((Tag) TagsBlock.WALLS);
    }

    @Override
    public EnumInteractionResult use(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        if ((Boolean) iblockdata.getValue(BlockFenceGate.OPEN)) {
            iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.OPEN, false);
            world.setBlock(blockposition, iblockdata, 10);
        } else {
            EnumDirection enumdirection = entityhuman.getDirection();

            if (iblockdata.getValue(BlockFenceGate.FACING) == enumdirection.getOpposite()) {
                iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.FACING, enumdirection);
            }

            iblockdata = (IBlockData) iblockdata.setValue(BlockFenceGate.OPEN, true);
            world.setBlock(blockposition, iblockdata, 10);
        }

        boolean flag = (Boolean) iblockdata.getValue(BlockFenceGate.OPEN);

        world.levelEvent(entityhuman, flag ? 1008 : 1014, blockposition, 0);
        world.gameEvent(entityhuman, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockposition);
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void neighborChanged(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition blockposition1, boolean flag) {
        if (!world.isClientSide) {
            boolean flag1 = world.hasNeighborSignal(blockposition);
            // CraftBukkit start
            boolean oldPowered = iblockdata.getValue(BlockFenceGate.POWERED);
            if (oldPowered != flag1) {
                int newPower = flag1 ? 15 : 0;
                int oldPower = oldPowered ? 15 : 0;
                org.bukkit.block.Block bukkitBlock = org.bukkit.craftbukkit.block.CraftBlock.at(world, blockposition);
                org.bukkit.event.block.BlockRedstoneEvent eventRedstone = new org.bukkit.event.block.BlockRedstoneEvent(bukkitBlock, oldPower, newPower);
                world.getCraftServer().getPluginManager().callEvent(eventRedstone);
                flag1 = eventRedstone.getNewCurrent() > 0;
            }
            // CraftBukkit end

            if ((Boolean) iblockdata.getValue(BlockFenceGate.POWERED) != flag1) {
                world.setBlock(blockposition, (IBlockData) ((IBlockData) iblockdata.setValue(BlockFenceGate.POWERED, flag1)).setValue(BlockFenceGate.OPEN, flag1), 2);
                if ((Boolean) iblockdata.getValue(BlockFenceGate.OPEN) != flag1) {
                    world.levelEvent((EntityHuman) null, flag1 ? 1008 : 1014, blockposition, 0);
                    world.gameEvent(flag1 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockposition);
                }
            }

        }
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockFenceGate.FACING, BlockFenceGate.OPEN, BlockFenceGate.POWERED, BlockFenceGate.IN_WALL);
    }

    public static boolean connectsToDirection(IBlockData iblockdata, EnumDirection enumdirection) {
        return ((EnumDirection) iblockdata.getValue(BlockFenceGate.FACING)).getAxis() == enumdirection.getClockWise().getAxis();
    }
}
