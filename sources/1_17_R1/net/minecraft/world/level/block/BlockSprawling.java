package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockSprawling extends Block {

    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    public static final BlockStateBoolean NORTH = BlockProperties.NORTH;
    public static final BlockStateBoolean EAST = BlockProperties.EAST;
    public static final BlockStateBoolean SOUTH = BlockProperties.SOUTH;
    public static final BlockStateBoolean WEST = BlockProperties.WEST;
    public static final BlockStateBoolean UP = BlockProperties.UP;
    public static final BlockStateBoolean DOWN = BlockProperties.DOWN;
    public static final Map<EnumDirection, BlockStateBoolean> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map) SystemUtils.a((Object) Maps.newEnumMap(EnumDirection.class), (enummap) -> {
        enummap.put(EnumDirection.NORTH, BlockSprawling.NORTH);
        enummap.put(EnumDirection.EAST, BlockSprawling.EAST);
        enummap.put(EnumDirection.SOUTH, BlockSprawling.SOUTH);
        enummap.put(EnumDirection.WEST, BlockSprawling.WEST);
        enummap.put(EnumDirection.UP, BlockSprawling.UP);
        enummap.put(EnumDirection.DOWN, BlockSprawling.DOWN);
    }));
    protected final VoxelShape[] shapeByIndex;

    protected BlockSprawling(float f, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.shapeByIndex = this.a(f);
    }

    private VoxelShape[] a(float f) {
        float f1 = 0.5F - f;
        float f2 = 0.5F + f;
        VoxelShape voxelshape = Block.a((double) (f1 * 16.0F), (double) (f1 * 16.0F), (double) (f1 * 16.0F), (double) (f2 * 16.0F), (double) (f2 * 16.0F), (double) (f2 * 16.0F));
        VoxelShape[] avoxelshape = new VoxelShape[BlockSprawling.DIRECTIONS.length];

        for (int i = 0; i < BlockSprawling.DIRECTIONS.length; ++i) {
            EnumDirection enumdirection = BlockSprawling.DIRECTIONS[i];

            avoxelshape[i] = VoxelShapes.create(0.5D + Math.min((double) (-f), (double) enumdirection.getAdjacentX() * 0.5D), 0.5D + Math.min((double) (-f), (double) enumdirection.getAdjacentY() * 0.5D), 0.5D + Math.min((double) (-f), (double) enumdirection.getAdjacentZ() * 0.5D), 0.5D + Math.max((double) f, (double) enumdirection.getAdjacentX() * 0.5D), 0.5D + Math.max((double) f, (double) enumdirection.getAdjacentY() * 0.5D), 0.5D + Math.max((double) f, (double) enumdirection.getAdjacentZ() * 0.5D));
        }

        VoxelShape[] avoxelshape1 = new VoxelShape[64];

        for (int j = 0; j < 64; ++j) {
            VoxelShape voxelshape1 = voxelshape;

            for (int k = 0; k < BlockSprawling.DIRECTIONS.length; ++k) {
                if ((j & 1 << k) != 0) {
                    voxelshape1 = VoxelShapes.a(voxelshape1, avoxelshape[k]);
                }
            }

            avoxelshape1[j] = voxelshape1;
        }

        return avoxelshape1;
    }

    @Override
    public boolean c(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return false;
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return this.shapeByIndex[this.h(iblockdata)];
    }

    protected int h(IBlockData iblockdata) {
        int i = 0;

        for (int j = 0; j < BlockSprawling.DIRECTIONS.length; ++j) {
            if ((Boolean) iblockdata.get((IBlockState) BlockSprawling.PROPERTY_BY_DIRECTION.get(BlockSprawling.DIRECTIONS[j]))) {
                i |= 1 << j;
            }
        }

        return i;
    }
}
