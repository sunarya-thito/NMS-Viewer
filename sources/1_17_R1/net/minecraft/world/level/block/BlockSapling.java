package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.grower.WorldGenTreeProvider;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public class BlockSapling extends BlockPlant implements IBlockFragilePlantElement {

    public static final BlockStateInteger STAGE = BlockProperties.STAGE;
    protected static final float AABB_OFFSET = 6.0F;
    protected static final VoxelShape SHAPE = Block.a(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);
    private final WorldGenTreeProvider treeGrower;
    public static TreeType treeType; // CraftBukkit

    protected BlockSapling(WorldGenTreeProvider worldgentreeprovider, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.treeGrower = worldgentreeprovider;
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockSapling.STAGE, 0));
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockSapling.SHAPE;
    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (worldserver.getLightLevel(blockposition.up()) >= 9 && random.nextInt(Math.max(2, (int) (((100.0F / worldserver.spigotConfig.saplingModifier) * 7) + 0.5F))) == 0) { // Spigot
            // CraftBukkit start
            worldserver.captureTreeGeneration = true;
            // CraftBukkit end
            this.grow(worldserver, blockposition, iblockdata, random);
            // CraftBukkit start
            worldserver.captureTreeGeneration = false;
            if (worldserver.capturedBlockStates.size() > 0) {
                TreeType treeType = BlockSapling.treeType;
                BlockSapling.treeType = null;
                Location location = new Location(worldserver.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                java.util.List<BlockState> blocks = new java.util.ArrayList<>(worldserver.capturedBlockStates.values());
                worldserver.capturedBlockStates.clear();
                StructureGrowEvent event = null;
                if (treeType != null) {
                    event = new StructureGrowEvent(location, treeType, false, null, blocks);
                    org.bukkit.Bukkit.getPluginManager().callEvent(event);
                }
                if (event == null || !event.isCancelled()) {
                    for (BlockState blockstate : blocks) {
                        blockstate.update(true);
                    }
                }
            }
            // CraftBukkit end
        }

    }

    public void grow(WorldServer worldserver, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        if ((Integer) iblockdata.get(BlockSapling.STAGE) == 0) {
            worldserver.setTypeAndData(blockposition, (IBlockData) iblockdata.a((IBlockState) BlockSapling.STAGE), 4);
        } else {
            this.treeGrower.a(worldserver, worldserver.getChunkProvider().getChunkGenerator(), blockposition, iblockdata, random);
        }

    }

    @Override
    public boolean a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return true;
    }

    @Override
    public boolean a(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return (double) world.random.nextFloat() < 0.45D;
    }

    @Override
    public void a(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        this.grow(worldserver, blockposition, iblockdata, random);
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockSapling.STAGE);
    }
}
