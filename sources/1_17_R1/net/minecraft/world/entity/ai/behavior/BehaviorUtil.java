package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;

public class BehaviorUtil {

    public BehaviorUtil() {}

    public static void a(EntityLiving entityliving, EntityLiving entityliving1, float f) {
        c(entityliving, entityliving1);
        b(entityliving, entityliving1, f);
    }

    public static boolean a(BehaviorController<?> behaviorcontroller, EntityLiving entityliving) {
        return behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).filter((list) -> {
            return list.contains(entityliving);
        }).isPresent();
    }

    public static boolean a(BehaviorController<?> behaviorcontroller, MemoryModuleType<? extends EntityLiving> memorymoduletype, EntityTypes<?> entitytypes) {
        return a(behaviorcontroller, memorymoduletype, (entityliving) -> {
            return entityliving.getEntityType() == entitytypes;
        });
    }

    private static boolean a(BehaviorController<?> behaviorcontroller, MemoryModuleType<? extends EntityLiving> memorymoduletype, Predicate<EntityLiving> predicate) {
        return behaviorcontroller.getMemory(memorymoduletype).filter(predicate).filter(EntityLiving::isAlive).filter((entityliving) -> {
            return a(behaviorcontroller, entityliving);
        }).isPresent();
    }

    private static void c(EntityLiving entityliving, EntityLiving entityliving1) {
        a(entityliving, entityliving1);
        a(entityliving1, entityliving);
    }

    public static void a(EntityLiving entityliving, EntityLiving entityliving1) {
        entityliving.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, (new BehaviorPositionEntity(entityliving1, true))); // CraftBukkit - decompile error
    }

    private static void b(EntityLiving entityliving, EntityLiving entityliving1, float f) {
        boolean flag = true;

        a(entityliving, (Entity) entityliving1, f, 2);
        a(entityliving1, (Entity) entityliving, f, 2);
    }

    public static void a(EntityLiving entityliving, Entity entity, float f, int i) {
        MemoryTarget memorytarget = new MemoryTarget(new BehaviorPositionEntity(entity, false), f, i);

        entityliving.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, (new BehaviorPositionEntity(entity, true))); // CraftBukkit - decompile error
        entityliving.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, memorytarget); // CraftBukkit - decompile error
    }

    public static void a(EntityLiving entityliving, BlockPosition blockposition, float f, int i) {
        MemoryTarget memorytarget = new MemoryTarget(new BehaviorTarget(blockposition), f, i);

        entityliving.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, (new BehaviorTarget(blockposition))); // CraftBukkit - decompile error
        entityliving.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, memorytarget); // CraftBukkit - decompile error
    }

    public static void a(EntityLiving entityliving, ItemStack itemstack, Vec3D vec3d) {
        if (itemstack.isEmpty()) return; // CraftBukkit - SPIGOT-4940: no empty loot
        double d0 = entityliving.getHeadY() - 0.30000001192092896D;
        EntityItem entityitem = new EntityItem(entityliving.level, entityliving.locX(), d0, entityliving.locZ(), itemstack);
        float f = 0.3F;
        Vec3D vec3d1 = vec3d.d(entityliving.getPositionVector());

        vec3d1 = vec3d1.d().a(0.30000001192092896D);
        entityitem.setMot(vec3d1);
        entityitem.defaultPickupDelay();
        // CraftBukkit start
        org.bukkit.event.entity.EntityDropItemEvent event = new org.bukkit.event.entity.EntityDropItemEvent(entityliving.getBukkitEntity(), (org.bukkit.entity.Item) entityitem.getBukkitEntity());
        entityitem.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        entityliving.level.addEntity(entityitem);
    }

    public static SectionPosition a(WorldServer worldserver, SectionPosition sectionposition, int i) {
        int j = worldserver.b(sectionposition);
        Stream<SectionPosition> stream = SectionPosition.a(sectionposition, i).filter((sectionposition1) -> { // CraftBukkit - decompile error
            return worldserver.b(sectionposition1) < j;
        });

        Objects.requireNonNull(worldserver);
        return (SectionPosition) stream.min(Comparator.comparingInt(worldserver::b)).orElse(sectionposition);
    }

    public static boolean a(EntityInsentient entityinsentient, EntityLiving entityliving, int i) {
        Item item = entityinsentient.getItemInMainHand().getItem();

        if (item instanceof ItemProjectileWeapon && entityinsentient.a((ItemProjectileWeapon) item)) {
            int j = ((ItemProjectileWeapon) item).d() - i;

            return entityinsentient.a((Entity) entityliving, (double) j);
        } else {
            return a(entityinsentient, entityliving);
        }
    }

    public static boolean a(EntityInsentient entityinsentient, EntityLiving entityliving) {
        double d0 = entityinsentient.h(entityliving.locX(), entityliving.locY(), entityliving.locZ());

        return d0 <= entityinsentient.i(entityliving);
    }

    public static boolean a(EntityLiving entityliving, EntityLiving entityliving1, double d0) {
        Optional<EntityLiving> optional = entityliving.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET);

        if (!optional.isPresent()) {
            return false;
        } else {
            double d1 = entityliving.e(((EntityLiving) optional.get()).getPositionVector());
            double d2 = entityliving.e(entityliving1.getPositionVector());

            return d2 > d1 + d0 * d0;
        }
    }

    public static boolean b(EntityLiving entityliving, EntityLiving entityliving1) {
        BehaviorController<?> behaviorcontroller = entityliving.getBehaviorController();

        return !behaviorcontroller.hasMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES) ? false : ((List) behaviorcontroller.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get()).contains(entityliving1);
    }

    public static EntityLiving a(EntityLiving entityliving, Optional<EntityLiving> optional, EntityLiving entityliving1) {
        return !optional.isPresent() ? entityliving1 : a(entityliving, (EntityLiving) optional.get(), entityliving1);
    }

    public static EntityLiving a(EntityLiving entityliving, EntityLiving entityliving1, EntityLiving entityliving2) {
        Vec3D vec3d = entityliving1.getPositionVector();
        Vec3D vec3d1 = entityliving2.getPositionVector();

        return entityliving.e(vec3d) < entityliving.e(vec3d1) ? entityliving1 : entityliving2;
    }

    public static Optional<EntityLiving> a(EntityLiving entityliving, MemoryModuleType<UUID> memorymoduletype) {
        Optional<UUID> optional = entityliving.getBehaviorController().getMemory(memorymoduletype);

        return optional.map((uuid) -> {
            return ((WorldServer) entityliving.level).getEntity(uuid);
        }).map((entity) -> {
            return entity instanceof EntityLiving ? (EntityLiving) entity : null;
        });
    }

    public static Stream<EntityVillager> a(EntityVillager entityvillager, Predicate<EntityVillager> predicate) {
        return (Stream) entityvillager.getBehaviorController().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).map((list) -> {
            return list.stream().filter((entityliving) -> {
                return entityliving instanceof EntityVillager && entityliving != entityvillager;
            }).map((entityliving) -> {
                return (EntityVillager) entityliving;
            }).filter(EntityLiving::isAlive).filter(predicate);
        }).orElseGet(Stream::empty);
    }

    @Nullable
    public static Vec3D a(EntityCreature entitycreature, int i, int j) {
        Vec3D vec3d = DefaultRandomPos.a(entitycreature, i, j);

        for (int k = 0; vec3d != null && !entitycreature.level.getType(new BlockPosition(vec3d)).a((IBlockAccess) entitycreature.level, new BlockPosition(vec3d), PathMode.WATER) && k++ < 10; vec3d = DefaultRandomPos.a(entitycreature, i, j)) {
            ;
        }

        return vec3d;
    }
}
