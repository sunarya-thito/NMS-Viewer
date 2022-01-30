package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockSnow extends Block {

    public static final int MAX_HEIGHT = 8;
    public static final BlockStateInteger LAYERS = BlockProperties.LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{VoxelShapes.a(), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D), Block.a(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    public static final int HEIGHT_IMPASSABLE = 5;

    protected BlockSnow(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockSnow.LAYERS, 1));
    }

    @Override
    public boolean a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        switch (pathmode) {
            case LAND:
                return (Integer) iblockdata.get(BlockSnow.LAYERS) < 5;
            case WATER:
                return false;
            case AIR:
                return false;
            default:
                return false;
        }
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockSnow.SHAPE_BY_LAYER[(Integer) iblockdata.get(BlockSnow.LAYERS)];
    }

    @Override
    public VoxelShape c(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockSnow.SHAPE_BY_LAYER[(Integer) iblockdata.get(BlockSnow.LAYERS) - 1];
    }

    @Override
    public VoxelShape f(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return BlockSnow.SHAPE_BY_LAYER[(Integer) iblockdata.get(BlockSnow.LAYERS)];
    }

    @Override
    public VoxelShape b(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockSnow.SHAPE_BY_LAYER[(Integer) iblockdata.get(BlockSnow.LAYERS)];
    }

    @Override
    public boolean g_(IBlockData iblockdata) {
        return true;
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        IBlockData iblockdata1 = iworldreader.getType(blockposition.down());

        return !iblockdata1.a(Blocks.ICE) && !iblockdata1.a(Blocks.PACKED_ICE) && !iblockdata1.a(Blocks.BARRIER) ? (!iblockdata1.a(Blocks.HONEY_BLOCK) && !iblockdata1.a(Blocks.SOUL_SAND) ? Block.a(iblockdata1.getCollisionShape(iworldreader, blockposition.down()), EnumDirection.UP) || iblockdata1.a((Block) this) && (Integer) iblockdata1.get(BlockSnow.LAYERS) == 8 : true) : false;
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        return !iblockdata.canPlace(generatoraccess, blockposition) ? Blocks.AIR.getBlockData() : super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (worldserver.getBrightness(EnumSkyBlock.BLOCK, blockposition) > 11) {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockFadeEvent(worldserver, blockposition, Blocks.AIR.getBlockData()).isCancelled()) {
                return;
            }
            // CraftBukkit end
            c(iblockdata, (World) worldserver, blockposition);
            worldserver.a(blockposition, false);
        }

    }

    @Override
    public boolean a(IBlockData iblockdata, BlockActionContext blockactioncontext) {
        int i = (Integer) iblockdata.get(BlockSnow.LAYERS);

        return blockactioncontext.getItemStack().a(this.getItem()) && i < 8 ? (blockactioncontext.c() ? blockactioncontext.getClickedFace() == EnumDirection.UP : true) : i == 1;
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        IBlockData iblockdata = blockactioncontext.getWorld().getType(blockactioncontext.getClickPosition());

        if (iblockdata.a((Block) this)) {
            int i = (Integer) iblockdata.get(BlockSnow.LAYERS);

            return (IBlockData) iblockdata.set(BlockSnow.LAYERS, Math.min(8, i + 1));
        } else {
            return super.getPlacedState(blockactioncontext);
        }
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockSnow.LAYERS);
    }
}
