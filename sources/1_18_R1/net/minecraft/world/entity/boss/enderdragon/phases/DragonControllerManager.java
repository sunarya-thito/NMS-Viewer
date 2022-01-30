package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftEnderDragon;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
// CraftBukkit end

public class DragonControllerManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private final EntityEnderDragon dragon;
    private final IDragonController[] phases = new IDragonController[DragonControllerPhase.getCount()];
    @Nullable
    private IDragonController currentPhase;

    public DragonControllerManager(EntityEnderDragon entityenderdragon) {
        this.dragon = entityenderdragon;
        this.setPhase(DragonControllerPhase.HOVERING);
    }

    public void setPhase(DragonControllerPhase<?> dragoncontrollerphase) {
        if (this.currentPhase == null || dragoncontrollerphase != this.currentPhase.getPhase()) {
            if (this.currentPhase != null) {
                this.currentPhase.end();
            }

            // CraftBukkit start - Call EnderDragonChangePhaseEvent
            EnderDragonChangePhaseEvent event = new EnderDragonChangePhaseEvent(
                    (CraftEnderDragon) this.dragon.getBukkitEntity(),
                    (this.currentPhase == null) ? null : CraftEnderDragon.getBukkitPhase(this.currentPhase.getPhase()),
                    CraftEnderDragon.getBukkitPhase(dragoncontrollerphase)
            );
            this.dragon.level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            dragoncontrollerphase = CraftEnderDragon.getMinecraftPhase(event.getNewPhase());
            // CraftBukkit end

            this.currentPhase = this.getPhase(dragoncontrollerphase);
            if (!this.dragon.level.isClientSide) {
                this.dragon.getEntityData().set(EntityEnderDragon.DATA_PHASE, dragoncontrollerphase.getId());
            }

            DragonControllerManager.LOGGER.debug("Dragon is now in phase {} on the {}", dragoncontrollerphase, this.dragon.level.isClientSide ? "client" : "server");
            this.currentPhase.begin();
        }
    }

    public IDragonController getCurrentPhase() {
        return this.currentPhase;
    }

    public <T extends IDragonController> T getPhase(DragonControllerPhase<T> dragoncontrollerphase) {
        int i = dragoncontrollerphase.getId();

        if (this.phases[i] == null) {
            this.phases[i] = dragoncontrollerphase.createInstance(this.dragon);
        }

        return (T) this.phases[i]; // CraftBukkit - decompile error
    }
}
