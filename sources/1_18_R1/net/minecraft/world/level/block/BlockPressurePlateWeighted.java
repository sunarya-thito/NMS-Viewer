package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;

import org.bukkit.event.entity.EntityInteractEvent; // CraftBukkit

public class BlockPressurePlateWeighted extends BlockPressurePlateAbstract {

    public static final BlockStateInteger POWER = BlockProperties.POWER;
    private final int maxWeight;

    protected BlockPressurePlateWeighted(int i, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) this.stateDefinition.any()).setValue(BlockPressurePlateWeighted.POWER, 0));
        this.maxWeight = i;
    }

    @Override
    protected int getSignalStrength(World world, BlockPosition blockposition) {
        // CraftBukkit start
        // int i = Math.min(world.getEntitiesOfClass(Entity.class, BlockPressurePlateWeighted.TOUCH_AABB.move(blockposition)).size(), this.maxWeight);
        int i = 0;
        java.util.Iterator iterator = world.getEntitiesOfClass(Entity.class, BlockPressurePlateWeighted.TOUCH_AABB.move(blockposition)).iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            org.bukkit.event.Cancellable cancellable;

            if (entity instanceof EntityHuman) {
                cancellable = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent((EntityHuman) entity, org.bukkit.event.block.Action.PHYSICAL, blockposition, null, null, null);
            } else {
                cancellable = new EntityInteractEvent(entity.getBukkitEntity(), world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                world.getCraftServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
            }

            // We only want to block turning the plate on if all events are cancelled
            if (!cancellable.isCancelled()) {
                i++;
            }
        }

        i = Math.min(i, this.maxWeight);
        // CraftBukkit end

        if (i > 0) {
            float f = (float) Math.min(this.maxWeight, i) / (float) this.maxWeight;

            return MathHelper.ceil(f * 15.0F);
        } else {
            return 0;
        }
    }

    @Override
    protected void playOnSound(GeneratorAccess generatoraccess, BlockPosition blockposition) {
        generatoraccess.playSound((EntityHuman) null, blockposition, SoundEffects.METAL_PRESSURE_PLATE_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.90000004F);
    }

    @Override
    protected void playOffSound(GeneratorAccess generatoraccess, BlockPosition blockposition) {
        generatoraccess.playSound((EntityHuman) null, blockposition, SoundEffects.METAL_PRESSURE_PLATE_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 0.75F);
    }

    @Override
    protected int getSignalForState(IBlockData iblockdata) {
        return (Integer) iblockdata.getValue(BlockPressurePlateWeighted.POWER);
    }

    @Override
    protected IBlockData setSignalForState(IBlockData iblockdata, int i) {
        return (IBlockData) iblockdata.setValue(BlockPressurePlateWeighted.POWER, i);
    }

    @Override
    protected int getPressedTime() {
        return 10;
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockPressurePlateWeighted.POWER);
    }
}
