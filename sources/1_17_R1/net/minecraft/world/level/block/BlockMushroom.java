package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import org.bukkit.TreeType;
// CraftBukkit end

public class BlockMushroom extends BlockPlant implements IBlockFragilePlantElement {

    protected static final float AABB_OFFSET = 3.0F;
    protected static final VoxelShape SHAPE = Block.a(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
    private final Supplier<WorldGenFeatureConfigured<?, ?>> featureSupplier;

    public BlockMushroom(BlockBase.Info blockbase_info, Supplier<WorldGenFeatureConfigured<?, ?>> supplier) {
        super(blockbase_info);
        this.featureSupplier = supplier;
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockMushroom.SHAPE;
    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (random.nextInt(Math.max(1, (int) (100.0F / worldserver.spigotConfig.mushroomModifier) * 25)) == 0) { // Spigot
            int i = 5;
            boolean flag = true;
            Iterator iterator = BlockPosition.a(blockposition.c(-4, -1, -4), blockposition.c(4, 1, 4)).iterator();

            while (iterator.hasNext()) {
                BlockPosition blockposition1 = (BlockPosition) iterator.next();

                if (worldserver.getType(blockposition1).a((Block) this)) {
                    --i;
                    if (i <= 0) {
                        return;
                    }
                }
            }

            BlockPosition blockposition2 = blockposition.c(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);

            for (int j = 0; j < 4; ++j) {
                if (worldserver.isEmpty(blockposition2) && iblockdata.canPlace(worldserver, blockposition2)) {
                    blockposition = blockposition2;
                }

                blockposition2 = blockposition.c(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            }

            if (worldserver.isEmpty(blockposition2) && iblockdata.canPlace(worldserver, blockposition2)) {
                org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockSpreadEvent(worldserver, blockposition, blockposition2, iblockdata, 2); // CraftBukkit
            }
        }

    }

    @Override
    protected boolean d(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.i(iblockaccess, blockposition);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        BlockPosition blockposition1 = blockposition.down();
        IBlockData iblockdata1 = iworldreader.getType(blockposition1);

        return iblockdata1.a((Tag) TagsBlock.MUSHROOM_GROW_BLOCK) ? true : iworldreader.getLightLevel(blockposition, 0) < 13 && this.d(iblockdata1, iworldreader, blockposition1);
    }

    public boolean a(WorldServer worldserver, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        worldserver.a(blockposition, false);
        BlockSapling.treeType = (this == Blocks.BROWN_MUSHROOM) ? TreeType.BROWN_MUSHROOM : TreeType.BROWN_MUSHROOM; // CraftBukkit
        if (((WorldGenFeatureConfigured) this.featureSupplier.get()).a(worldserver, worldserver.getChunkProvider().getChunkGenerator(), random, blockposition)) {
            return true;
        } else {
            worldserver.setTypeAndData(blockposition, iblockdata, 3);
            return false;
        }
    }

    @Override
    public boolean a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return true;
    }

    @Override
    public boolean a(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return (double) random.nextFloat() < 0.4D;
    }

    @Override
    public void a(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        this.a(worldserver, blockposition, iblockdata, random);
    }
}
