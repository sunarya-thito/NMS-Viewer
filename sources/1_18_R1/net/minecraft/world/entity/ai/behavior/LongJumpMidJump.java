package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.EntityHuman;

public class LongJumpMidJump extends Behavior<EntityInsentient> {

    public static final int TIME_OUT_DURATION = 100;
    private final UniformInt timeBetweenLongJumps;
    private SoundEffect landingSound;

    public LongJumpMidJump(UniformInt uniformint, SoundEffect soundeffect) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), 100);
        this.timeBetweenLongJumps = uniformint;
        this.landingSound = soundeffect;
    }

    protected boolean canStillUse(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        return !entityinsentient.isOnGround();
    }

    protected void start(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        entityinsentient.setDiscardFriction(true);
        entityinsentient.setPose(EntityPose.LONG_JUMPING);
    }

    protected void stop(WorldServer worldserver, EntityInsentient entityinsentient, long i) {
        if (entityinsentient.isOnGround()) {
            entityinsentient.setDeltaMovement(entityinsentient.getDeltaMovement().scale(0.10000000149011612D));
            worldserver.playSound((EntityHuman) null, (Entity) entityinsentient, this.landingSound, SoundCategory.NEUTRAL, 2.0F, 1.0F);
        }

        entityinsentient.setDiscardFriction(false);
        entityinsentient.setPose(EntityPose.STANDING);
        entityinsentient.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
        entityinsentient.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, (Object) this.timeBetweenLongJumps.sample(worldserver.random));
    }
}
