package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.piston.BlockPistonMoving;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
// CraftBukkit end

public class BlockSoil extends Block {

    public static final BlockStateInteger MOISTURE = BlockProperties.MOISTURE;
    protected static final VoxelShape SHAPE = Block.a(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    public static final int MAX_MOISTURE = 7;

    protected BlockSoil(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockSoil.MOISTURE, 0));
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (enumdirection == EnumDirection.UP && !iblockdata.canPlace(generatoraccess, blockposition)) {
            generatoraccess.getBlockTickList().a(blockposition, this, 1);
        }

        return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        IBlockData iblockdata1 = iworldreader.getType(blockposition.up());

        return !iblockdata1.getMaterial().isBuildable() || iblockdata1.getBlock() instanceof BlockFenceGate || iblockdata1.getBlock() instanceof BlockPistonMoving;
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        return !this.getBlockData().canPlace(blockactioncontext.getWorld(), blockactioncontext.getClickPosition()) ? Blocks.DIRT.getBlockData() : super.getPlacedState(blockactioncontext);
    }

    @Override
    public boolean g_(IBlockData iblockdata) {
        return true;
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockSoil.SHAPE;
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!iblockdata.canPlace(worldserver, blockposition)) {
            fade(iblockdata, worldserver, blockposition);
        }

    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        int i = (Integer) iblockdata.get(BlockSoil.MOISTURE);

        if (!a((IWorldReader) worldserver, blockposition) && !worldserver.isRainingAt(blockposition.up())) {
            if (i > 0) {
                org.bukkit.craftbukkit.event.CraftEventFactory.handleMoistureChangeEvent(worldserver, blockposition, (IBlockData) iblockdata.set(BlockSoil.MOISTURE, i - 1), 2); // CraftBukkit
            } else if (!a((IBlockAccess) worldserver, blockposition)) {
                fade(iblockdata, worldserver, blockposition);
            }
        } else if (i < 7) {
            org.bukkit.craftbukkit.event.CraftEventFactory.handleMoistureChangeEvent(worldserver, blockposition, (IBlockData) iblockdata.set(BlockSoil.MOISTURE, 7), 2); // CraftBukkit
        }

    }

    @Override
    public void fallOn(World world, IBlockData iblockdata, BlockPosition blockposition, Entity entity, float f) {
        super.fallOn(world, iblockdata, blockposition, entity, f); // CraftBukkit - moved here as game rules / events shouldn't affect fall damage.
        if (!world.isClientSide && world.random.nextFloat() < f - 0.5F && entity instanceof EntityLiving && (entity instanceof EntityHuman || world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) && entity.getWidth() * entity.getWidth() * entity.getHeight() > 0.512F) {
            // CraftBukkit start - Interact soil
            org.bukkit.event.Cancellable cancellable;
            if (entity instanceof EntityHuman) {
                cancellable = CraftEventFactory.callPlayerInteractEvent((EntityHuman) entity, org.bukkit.event.block.Action.PHYSICAL, blockposition, null, null, null);
            } else {
                cancellable = new EntityInteractEvent(entity.getBukkitEntity(), world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                world.getCraftServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
            }

            if (cancellable.isCancelled()) {
                return;
            }

            if (CraftEventFactory.callEntityChangeBlockEvent(entity, blockposition, Blocks.DIRT.getBlockData()).isCancelled()) {
                return;
            }
            // CraftBukkit end
            fade(iblockdata, world, blockposition);
        }

        // super.fallOn(world, iblockdata, blockposition, entity, f); // CraftBukkit - moved up
    }

    public static void fade(IBlockData iblockdata, World world, BlockPosition blockposition) {
        // CraftBukkit start
        if (CraftEventFactory.callBlockFadeEvent(world, blockposition, Blocks.DIRT.getBlockData()).isCancelled()) {
            return;
        }
        // CraftBukkit end
        world.setTypeUpdate(blockposition, a(iblockdata, Blocks.DIRT.getBlockData(), world, blockposition));
    }

    private static boolean a(IBlockAccess iblockaccess, BlockPosition blockposition) {
        Block block = iblockaccess.getType(blockposition.up()).getBlock();

        return block instanceof BlockCrops || block instanceof BlockStem || block instanceof BlockStemAttached;
    }

    private static boolean a(IWorldReader iworldreader, BlockPosition blockposition) {
        Iterator iterator = BlockPosition.a(blockposition.c(-4, 0, -4), blockposition.c(4, 1, 4)).iterator();

        BlockPosition blockposition1;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            blockposition1 = (BlockPosition) iterator.next();
        } while (!iworldreader.getFluid(blockposition1).a((Tag) TagsFluid.WATER));

        return true;
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockSoil.MOISTURE);
    }

    @Override
    public boolean a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        return false;
    }
}
