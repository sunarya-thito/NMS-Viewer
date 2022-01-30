package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.grower.AzaleaTreeGrower;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public class AzaleaBlock extends BlockPlant implements IBlockFragilePlantElement {

    private static final AzaleaTreeGrower TREE_GROWER = new AzaleaTreeGrower();
    private static final VoxelShape SHAPE = VoxelShapes.a(Block.a(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D), Block.a(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D));

    protected AzaleaBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return AzaleaBlock.SHAPE;
    }

    @Override
    protected boolean d(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.a(Blocks.CLAY) || super.d(iblockdata, iblockaccess, blockposition);
    }

    @Override
    public boolean a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return iblockaccess.getFluid(blockposition.up()).isEmpty();
    }

    @Override
    public boolean a(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return (double) world.random.nextFloat() < 0.45D;
    }

    @Override
    public void a(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        // CraftBukkit start
        worldserver.captureTreeGeneration = true;
        // CraftBukkit end
        AzaleaBlock.TREE_GROWER.a(worldserver, worldserver.getChunkProvider().getChunkGenerator(), blockposition, iblockdata, random);
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
