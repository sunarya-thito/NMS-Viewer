package net.minecraft.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.entity.CraftEnderDragon;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;

public class DragonControllerManager {

    private static final Logger a = LogManager.getLogger();
    private final EntityEnderDragon b; // PAIL: Rename enderDragon
    private final IDragonController[] c = new IDragonController[DragonControllerPhase.c()]; // PAIL: Rename dragonControllers 
    private IDragonController d; // PAIL: Rename currentDragonController

    public DragonControllerManager(EntityEnderDragon entityenderdragon) {
        this.b = entityenderdragon;
        this.a(DragonControllerPhase.k);
    }

    public void a(DragonControllerPhase<?> dragoncontrollerphase) { // PAIL: Rename setControllerPhase
        if (this.d == null || dragoncontrollerphase != this.d.i()) { // PAIL: Rename getControllerPhase
            if (this.d != null) {
                this.d.e(); // PAIL: Rename
            }

            // CraftBukkit start - Call EnderDragonChangePhaseEvent
            EnderDragonChangePhaseEvent event = new EnderDragonChangePhaseEvent((CraftEnderDragon) this.b.getBukkitEntity(), this.d == null ? null : CraftEnderDragon.getBukkitPhase(this.d.i()), CraftEnderDragon.getBukkitPhase(dragoncontrollerphase));
            this.b.world.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            dragoncontrollerphase = CraftEnderDragon.getMinecraftPhase(event.getNewPhase());
            // CraftBukkit end
            
            this.d = this.b(dragoncontrollerphase); // PAIL: Rename getDragonController
            if (!this.b.world.isClientSide) {
                this.b.getDataWatcher().set(EntityEnderDragon.a, Integer.valueOf(dragoncontrollerphase.b())); // PAIL: Rename getId
            }

            DragonControllerManager.a.debug("Dragon is now in phase {} on the {}", new Object[] { dragoncontrollerphase, this.b.world.isClientSide ? "client" : "server"});
            this.d.d(); // PAIL: Rename reset
        }
    }

    public IDragonController a() {
        return this.d;
    }

    public <T extends IDragonController> T b(DragonControllerPhase<T> dragoncontrollerphase) {
        int i = dragoncontrollerphase.b();

        if (this.c[i] == null) {
            this.c[i] = dragoncontrollerphase.a(this.b);
        }

        return (T) this.c[i];
    }
}
