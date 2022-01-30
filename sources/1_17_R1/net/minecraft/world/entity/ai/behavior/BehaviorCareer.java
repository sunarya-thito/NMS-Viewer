package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.IPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;

// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
// CraftBukkit end

public class BehaviorCareer extends Behavior<EntityVillager> {

    public BehaviorCareer() {
        super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    protected boolean a(WorldServer worldserver, EntityVillager entityvillager) {
        BlockPosition blockposition = ((GlobalPos) entityvillager.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get()).getBlockPosition();

        return blockposition.a((IPosition) entityvillager.getPositionVector(), 2.0D) || entityvillager.fJ();
    }

    protected void a(WorldServer worldserver, EntityVillager entityvillager, long i) {
        GlobalPos globalpos = (GlobalPos) entityvillager.getBehaviorController().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();

        entityvillager.getBehaviorController().removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entityvillager.getBehaviorController().setMemory(MemoryModuleType.JOB_SITE, globalpos); // CraftBukkit - decompile error
        worldserver.broadcastEntityEffect(entityvillager, (byte) 14);
        if (entityvillager.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftserver = worldserver.getMinecraftServer();

            Optional.ofNullable(minecraftserver.getWorldServer(globalpos.getDimensionManager())).flatMap((worldserver1) -> {
                return worldserver1.A().c(globalpos.getBlockPosition());
            }).flatMap((villageplacetype) -> {
                return IRegistry.VILLAGER_PROFESSION.g().filter((villagerprofession) -> {
                    return villagerprofession.b() == villageplacetype;
                }).findFirst();
            }).ifPresent((villagerprofession) -> {
                // CraftBukkit start - Fire VillagerCareerChangeEvent where Villager gets employed
                VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(entityvillager, CraftVillager.nmsToBukkitProfession(villagerprofession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
                if (event.isCancelled()) {
                    return;
                }

                entityvillager.setVillagerData(entityvillager.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession())));
                // CraftBukkit end
                entityvillager.c(worldserver);
            });
        }
    }
}
