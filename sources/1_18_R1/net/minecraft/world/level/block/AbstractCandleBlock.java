package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class AbstractCandleBlock extends Block {

    public static final int LIGHT_PER_CANDLE = 3;
    public static final BlockStateBoolean LIT = BlockProperties.LIT;

    protected AbstractCandleBlock(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    protected abstract Iterable<Vec3D> getParticleOffsets(IBlockData iblockdata);

    public static boolean isLit(IBlockData iblockdata) {
        return iblockdata.hasProperty(AbstractCandleBlock.LIT) && (iblockdata.is((Tag) TagsBlock.CANDLES) || iblockdata.is((Tag) TagsBlock.CANDLE_CAKES)) && (Boolean) iblockdata.getValue(AbstractCandleBlock.LIT);
    }

    @Override
    public void onProjectileHit(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        if (!world.isClientSide && iprojectile.isOnFire() && this.canBeLit(iblockdata)) {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, movingobjectpositionblock.getBlockPos(), iprojectile).isCancelled()) {
                return;
            }
            // CraftBukkit end
            setLit(world, iblockdata, movingobjectpositionblock.getBlockPos(), true);
        }

    }

    protected boolean canBeLit(IBlockData iblockdata) {
        return !(Boolean) iblockdata.getValue(AbstractCandleBlock.LIT);
    }

    @Override
    public void animateTick(IBlockData iblockdata, World world, BlockPosition blockposition, Random random) {
        if ((Boolean) iblockdata.getValue(AbstractCandleBlock.LIT)) {
            this.getParticleOffsets(iblockdata).forEach((vec3d) -> {
                addParticlesAndSound(world, vec3d.add((double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ()), random);
            });
        }
    }

    private static void addParticlesAndSound(World world, Vec3D vec3d, Random random) {
        float f = random.nextFloat();

        if (f < 0.3F) {
            world.addParticle(Particles.SMOKE, vec3d.x, vec3d.y, vec3d.z, 0.0D, 0.0D, 0.0D);
            if (f < 0.17F) {
                world.playLocalSound(vec3d.x + 0.5D, vec3d.y + 0.5D, vec3d.z + 0.5D, SoundEffects.CANDLE_AMBIENT, SoundCategory.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
            }
        }

        world.addParticle(Particles.SMALL_FLAME, vec3d.x, vec3d.y, vec3d.z, 0.0D, 0.0D, 0.0D);
    }

    public static void extinguish(@Nullable EntityHuman entityhuman, IBlockData iblockdata, GeneratorAccess generatoraccess, BlockPosition blockposition) {
        setLit(generatoraccess, iblockdata, blockposition, false);
        if (iblockdata.getBlock() instanceof AbstractCandleBlock) {
            ((AbstractCandleBlock) iblockdata.getBlock()).getParticleOffsets(iblockdata).forEach((vec3d) -> {
                generatoraccess.addParticle(Particles.SMOKE, (double) blockposition.getX() + vec3d.x(), (double) blockposition.getY() + vec3d.y(), (double) blockposition.getZ() + vec3d.z(), 0.0D, 0.10000000149011612D, 0.0D);
            });
        }

        generatoraccess.playSound((EntityHuman) null, blockposition, SoundEffects.CANDLE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        generatoraccess.gameEvent(entityhuman, GameEvent.BLOCK_CHANGE, blockposition);
    }

    private static void setLit(GeneratorAccess generatoraccess, IBlockData iblockdata, BlockPosition blockposition, boolean flag) {
        generatoraccess.setBlock(blockposition, (IBlockData) iblockdata.setValue(AbstractCandleBlock.LIT, flag), 11);
    }
}
