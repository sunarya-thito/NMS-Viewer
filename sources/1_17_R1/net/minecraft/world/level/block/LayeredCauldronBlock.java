package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

// CraftBukkit start
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.CauldronLevelChangeEvent;
// CraftBukkit end

public class LayeredCauldronBlock extends AbstractCauldronBlock {

    public static final int MIN_FILL_LEVEL = 1;
    public static final int MAX_FILL_LEVEL = 3;
    public static final BlockStateInteger LEVEL = BlockProperties.LEVEL_CAULDRON;
    private static final int BASE_CONTENT_HEIGHT = 6;
    private static final double HEIGHT_PER_LEVEL = 3.0D;
    public static final Predicate<BiomeBase.Precipitation> RAIN = (biomebase_precipitation) -> {
        return biomebase_precipitation == BiomeBase.Precipitation.RAIN;
    };
    public static final Predicate<BiomeBase.Precipitation> SNOW = (biomebase_precipitation) -> {
        return biomebase_precipitation == BiomeBase.Precipitation.SNOW;
    };
    private final Predicate<BiomeBase.Precipitation> fillPredicate;

    public LayeredCauldronBlock(BlockBase.Info blockbase_info, Predicate<BiomeBase.Precipitation> predicate, Map<Item, CauldronInteraction> map) {
        super(blockbase_info, map);
        this.fillPredicate = predicate;
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(LayeredCauldronBlock.LEVEL, 1));
    }

    @Override
    public boolean c(IBlockData iblockdata) {
        return (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL) == 3;
    }

    @Override
    protected boolean a(FluidType fluidtype) {
        return fluidtype == FluidTypes.WATER && this.fillPredicate == LayeredCauldronBlock.RAIN;
    }

    @Override
    protected double a(IBlockData iblockdata) {
        return (6.0D + (double) (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL) * 3.0D) / 16.0D;
    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity) {
        if (!world.isClientSide && entity.isBurning() && this.a(iblockdata, blockposition, entity)) {
            // CraftBukkit start
            if (entity.a(world, blockposition)) {
                if (!decreaseLevel(iblockdata, world, blockposition, entity, CauldronLevelChangeEvent.ChangeReason.EXTINGUISH)) {
                    return;
                }
            }
            entity.extinguish();
            // CraftBukkit end
        }

    }

    protected void d(IBlockData iblockdata, World world, BlockPosition blockposition) {
        e(iblockdata, world, blockposition);
    }

    public static void e(IBlockData iblockdata, World world, BlockPosition blockposition) {
        // CraftBukkit start
        decreaseLevel(iblockdata, world, blockposition, null, CauldronLevelChangeEvent.ChangeReason.UNKNOWN);
    }

    public static boolean decreaseLevel(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        int i = (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL) - 1;

        return changeLevel(iblockdata, world, blockposition, i == 0 ? Blocks.CAULDRON.getBlockData() : iblockdata.set(LayeredCauldronBlock.LEVEL, i), entity, reason);
    }

    // CraftBukkit start
    public static boolean changeLevel(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData newBlock, Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        CraftBlockState newState = CraftBlockStates.getBlockState(world, blockposition);
        newState.setData(newBlock);

        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(
                world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()),
                (entity == null) ? null : entity.getBukkitEntity(), reason, newState
        );
        world.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        newState.update(true);
        return true;
    }
    // CraftBukkit end

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, BiomeBase.Precipitation biomebase_precipitation) {
        if (BlockCauldron.a(world, biomebase_precipitation) && (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL) != 3 && this.fillPredicate.test(biomebase_precipitation)) {
            changeLevel(iblockdata, world, blockposition, (IBlockData) iblockdata.a((IBlockState) LayeredCauldronBlock.LEVEL), null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL); // CraftBukkit
        }
    }

    @Override
    public int a(IBlockData iblockdata, World world, BlockPosition blockposition) {
        return (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL);
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(LayeredCauldronBlock.LEVEL);
    }

    @Override
    protected void a(IBlockData iblockdata, World world, BlockPosition blockposition, FluidType fluidtype) {
        if (!this.c(iblockdata)) {
            // CraftBukkit start
            if (!changeLevel(iblockdata, world, blockposition, (IBlockData) iblockdata.set(LayeredCauldronBlock.LEVEL, (Integer) iblockdata.get(LayeredCauldronBlock.LEVEL) + 1), null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL)) {
                return;
            }
            // CraftBukkit end
            world.triggerEffect(1047, blockposition, 0);
        }
    }
}
