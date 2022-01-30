package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockReed extends Block {

    public static final BlockStateInteger AGE = BlockProperties.AGE_15;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.a(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    protected BlockReed(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockReed.AGE, 0));
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockReed.SHAPE;
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!iblockdata.canPlace(worldserver, blockposition)) {
            worldserver.b(blockposition, true);
        }

    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (worldserver.isEmpty(blockposition.up())) {
            int i;

            for (i = 1; worldserver.getType(blockposition.down(i)).a((Block) this); ++i) {
                ;
            }

            if (i < 3) {
                int j = (Integer) iblockdata.get(BlockReed.AGE);

                if (j >= (byte) range(3, ((100.0F / worldserver.spigotConfig.caneModifier) * 15) + 0.5F, 15)) { // Spigot
                    org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition.up(), this.getBlockData()); // CraftBukkit
                    worldserver.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockReed.AGE, 0), 4);
                } else {
                    worldserver.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockReed.AGE, j + 1), 4);
                }
            }
        }

    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (!iblockdata.canPlace(generatoraccess, blockposition)) {
            generatoraccess.getBlockTickList().a(blockposition, this, 1);
        }

        return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        IBlockData iblockdata1 = iworldreader.getType(blockposition.down());

        if (iblockdata1.a((Block) this)) {
            return true;
        } else {
            if (iblockdata1.a((Tag) TagsBlock.DIRT) || iblockdata1.a(Blocks.SAND) || iblockdata1.a(Blocks.RED_SAND)) {
                BlockPosition blockposition1 = blockposition.down();
                Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

                while (iterator.hasNext()) {
                    EnumDirection enumdirection = (EnumDirection) iterator.next();
                    IBlockData iblockdata2 = iworldreader.getType(blockposition1.shift(enumdirection));
                    Fluid fluid = iworldreader.getFluid(blockposition1.shift(enumdirection));

                    if (fluid.a((Tag) TagsFluid.WATER) || iblockdata2.a(Blocks.FROSTED_ICE)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockReed.AGE);
    }
}
