package net.minecraft.world.entity.boss.enderdragon.phases;

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
    private final IDragonController[] phases = new IDragonController[DragonControllerPhase.c()];
    private IDragonController currentPhase;

    public DragonControllerManager(EntityEnderDragon entityenderdragon) {
        this.dragon = entityenderdragon;
        this.setControllerPhase(DragonControllerPhase.HOVERING);
    }

    public void setControllerPhase(DragonControllerPhase<?> dragoncontrollerphase) {
        if (this.currentPhase == null || dragoncontrollerphase != this.currentPhase.getControllerPhase()) {
            if (this.currentPhase != null) {
                this.currentPhase.e();
            }

            // CraftBukkit start - Call EnderDragonChangePhaseEvent
            EnderDragonChangePhaseEvent event = new EnderDragonChangePhaseEvent(
                    (CraftEnderDragon) this.dragon.getBukkitEntity(),
                    (this.currentPhase == null) ? null : CraftEnderDragon.getBukkitPhase(this.currentPhase.getControllerPhase()),
                    CraftEnderDragon.getBukkitPhase(dragoncontrollerphase)
            );
            this.dragon.level.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            dragoncontrollerphase = CraftEnderDragon.getMinecraftPhase(event.getNewPhase());
            // CraftBukkit end

            this.currentPhase = this.b(dragoncontrollerphase);
            if (!this.dragon.level.isClientSide) {
                this.dragon.getDataWatcher().set(EntityEnderDragon.DATA_PHASE, dragoncontrollerphase.b());
            }

            DragonControllerManager.LOGGER.debug("Dragon is now in phase {} on the {}", dragoncontrollerphase, this.dragon.level.isClientSide ? "client" : "server");
            this.currentPhase.d();
        }
    }

    public IDragonController a() {
        return this.currentPhase;
    }

    public <T extends IDragonController> T b(DragonControllerPhase<T> dragoncontrollerphase) {
        int i = dragoncontrollerphase.b();

        if (this.phases[i] == null) {
            this.phases[i] = dragoncontrollerphase.a(this.dragon);
        }

        return (T) this.phases[i]; // CraftBukkit - decompile error
    }
}
