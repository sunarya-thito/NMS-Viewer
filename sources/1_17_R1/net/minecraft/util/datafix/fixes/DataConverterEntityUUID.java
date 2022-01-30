package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class DataConverterEntityUUID extends DataConverterUUIDBase {

    private static final Set<String> ABSTRACT_HORSES = Sets.newHashSet();
    private static final Set<String> TAMEABLE_ANIMALS = Sets.newHashSet();
    private static final Set<String> ANIMALS = Sets.newHashSet();
    private static final Set<String> MOBS = Sets.newHashSet();
    private static final Set<String> LIVING_ENTITIES = Sets.newHashSet();
    private static final Set<String> PROJECTILES = Sets.newHashSet();

    public DataConverterEntityUUID(Schema schema) {
        super(schema, DataConverterTypes.ENTITY);
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("EntityUUIDFixes", this.getInputSchema().getType(this.typeReference), (typed) -> {
            typed = typed.update(DSL.remainderFinder(), DataConverterEntityUUID::c);

            String s;
            Iterator iterator;

            for (iterator = DataConverterEntityUUID.ABSTRACT_HORSES.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::l)) {
                s = (String) iterator.next();
            }

            for (iterator = DataConverterEntityUUID.TAMEABLE_ANIMALS.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::l)) {
                s = (String) iterator.next();
            }

            for (iterator = DataConverterEntityUUID.ANIMALS.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::m)) {
                s = (String) iterator.next();
            }

            for (iterator = DataConverterEntityUUID.MOBS.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::n)) {
                s = (String) iterator.next();
            }

            for (iterator = DataConverterEntityUUID.LIVING_ENTITIES.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::b)) {
                s = (String) iterator.next();
            }

            for (iterator = DataConverterEntityUUID.PROJECTILES.iterator(); iterator.hasNext(); typed = this.a(typed, s, DataConverterEntityUUID::o)) {
                s = (String) iterator.next();
            }

            typed = this.a(typed, "minecraft:bee", DataConverterEntityUUID::k);
            typed = this.a(typed, "minecraft:zombified_piglin", DataConverterEntityUUID::k);
            typed = this.a(typed, "minecraft:fox", DataConverterEntityUUID::j);
            typed = this.a(typed, "minecraft:item", DataConverterEntityUUID::i);
            typed = this.a(typed, "minecraft:shulker_bullet", DataConverterEntityUUID::h);
            typed = this.a(typed, "minecraft:area_effect_cloud", DataConverterEntityUUID::g);
            typed = this.a(typed, "minecraft:zombie_villager", DataConverterEntityUUID::f);
            typed = this.a(typed, "minecraft:evoker_fangs", DataConverterEntityUUID::e);
            typed = this.a(typed, "minecraft:piglin", DataConverterEntityUUID::d);
            return typed;
        });
    }

    private static Dynamic<?> d(Dynamic<?> dynamic) {
        return dynamic.update("Brain", (dynamic1) -> {
            return dynamic1.update("memories", (dynamic2) -> {
                return dynamic2.update("minecraft:angry_at", (dynamic3) -> {
                    return (Dynamic) a(dynamic3, "value", "value").orElseGet(() -> {
                        DataConverterEntityUUID.LOGGER.warn("angry_at has no value.");
                        return dynamic3;
                    });
                });
            });
        });
    }

    private static Dynamic<?> e(Dynamic<?> dynamic) {
        return (Dynamic) c(dynamic, "OwnerUUID", "Owner").orElse(dynamic);
    }

    private static Dynamic<?> f(Dynamic<?> dynamic) {
        return (Dynamic) c(dynamic, "ConversionPlayer", "ConversionPlayer").orElse(dynamic);
    }

    private static Dynamic<?> g(Dynamic<?> dynamic) {
        return (Dynamic) c(dynamic, "OwnerUUID", "Owner").orElse(dynamic);
    }

    private static Dynamic<?> h(Dynamic<?> dynamic) {
        dynamic = (Dynamic) b(dynamic, "Owner", "Owner").orElse(dynamic);
        return (Dynamic) b(dynamic, "Target", "Target").orElse(dynamic);
    }

    private static Dynamic<?> i(Dynamic<?> dynamic) {
        dynamic = (Dynamic) b(dynamic, "Owner", "Owner").orElse(dynamic);
        return (Dynamic) b(dynamic, "Thrower", "Thrower").orElse(dynamic);
    }

    private static Dynamic<?> j(Dynamic<?> dynamic) {
        Optional<Dynamic<?>> optional = dynamic.get("TrustedUUIDs").result().map((dynamic1) -> {
            return dynamic.createList(dynamic1.asStream().map((dynamic2) -> {
                return (Dynamic) a(dynamic2).orElseGet(() -> {
                    DataConverterEntityUUID.LOGGER.warn("Trusted contained invalid data.");
                    return dynamic2;
                });
            }));
        });

        return (Dynamic) DataFixUtils.orElse(optional.map((dynamic1) -> {
            return dynamic.remove("TrustedUUIDs").set("Trusted", dynamic1);
        }), dynamic);
    }

    private static Dynamic<?> k(Dynamic<?> dynamic) {
        return (Dynamic) a(dynamic, "HurtBy", "HurtBy").orElse(dynamic);
    }

    private static Dynamic<?> l(Dynamic<?> dynamic) {
        Dynamic<?> dynamic1 = m(dynamic);

        return (Dynamic) a(dynamic1, "OwnerUUID", "Owner").orElse(dynamic1);
    }

    private static Dynamic<?> m(Dynamic<?> dynamic) {
        Dynamic<?> dynamic1 = n(dynamic);

        return (Dynamic) c(dynamic1, "LoveCause", "LoveCause").orElse(dynamic1);
    }

    private static Dynamic<?> n(Dynamic<?> dynamic) {
        return b(dynamic).update("Leash", (dynamic1) -> {
            return (Dynamic) c(dynamic1, "UUID", "UUID").orElse(dynamic1);
        });
    }

    public static Dynamic<?> b(Dynamic<?> dynamic) {
        return dynamic.update("Attributes", (dynamic1) -> {
            return dynamic.createList(dynamic1.asStream().map((dynamic2) -> {
                return dynamic2.update("Modifiers", (dynamic3) -> {
                    return dynamic2.createList(dynamic3.asStream().map((dynamic4) -> {
                        return (Dynamic) c(dynamic4, "UUID", "UUID").orElse(dynamic4);
                    }));
                });
            }));
        });
    }

    private static Dynamic<?> o(Dynamic<?> dynamic) {
        return (Dynamic) DataFixUtils.orElse(dynamic.get("OwnerUUID").result().map((dynamic1) -> {
            return dynamic.remove("OwnerUUID").set("Owner", dynamic1);
        }), dynamic);
    }

    public static Dynamic<?> c(Dynamic<?> dynamic) {
        return (Dynamic) c(dynamic, "UUID", "UUID").orElse(dynamic);
    }

    static {
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:donkey");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:horse");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:llama");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:mule");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:skeleton_horse");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:trader_llama");
        DataConverterEntityUUID.ABSTRACT_HORSES.add("minecraft:zombie_horse");
        DataConverterEntityUUID.TAMEABLE_ANIMALS.add("minecraft:cat");
        DataConverterEntityUUID.TAMEABLE_ANIMALS.add("minecraft:parrot");
        DataConverterEntityUUID.TAMEABLE_ANIMALS.add("minecraft:wolf");
        DataConverterEntityUUID.ANIMALS.add("minecraft:bee");
        DataConverterEntityUUID.ANIMALS.add("minecraft:chicken");
        DataConverterEntityUUID.ANIMALS.add("minecraft:cow");
        DataConverterEntityUUID.ANIMALS.add("minecraft:fox");
        DataConverterEntityUUID.ANIMALS.add("minecraft:mooshroom");
        DataConverterEntityUUID.ANIMALS.add("minecraft:ocelot");
        DataConverterEntityUUID.ANIMALS.add("minecraft:panda");
        DataConverterEntityUUID.ANIMALS.add("minecraft:pig");
        DataConverterEntityUUID.ANIMALS.add("minecraft:polar_bear");
        DataConverterEntityUUID.ANIMALS.add("minecraft:rabbit");
        DataConverterEntityUUID.ANIMALS.add("minecraft:sheep");
        DataConverterEntityUUID.ANIMALS.add("minecraft:turtle");
        DataConverterEntityUUID.ANIMALS.add("minecraft:hoglin");
        DataConverterEntityUUID.MOBS.add("minecraft:bat");
        DataConverterEntityUUID.MOBS.add("minecraft:blaze");
        DataConverterEntityUUID.MOBS.add("minecraft:cave_spider");
        DataConverterEntityUUID.MOBS.add("minecraft:cod");
        DataConverterEntityUUID.MOBS.add("minecraft:creeper");
        DataConverterEntityUUID.MOBS.add("minecraft:dolphin");
        DataConverterEntityUUID.MOBS.add("minecraft:drowned");
        DataConverterEntityUUID.MOBS.add("minecraft:elder_guardian");
        DataConverterEntityUUID.MOBS.add("minecraft:ender_dragon");
        DataConverterEntityUUID.MOBS.add("minecraft:enderman");
        DataConverterEntityUUID.MOBS.add("minecraft:endermite");
        DataConverterEntityUUID.MOBS.add("minecraft:evoker");
        DataConverterEntityUUID.MOBS.add("minecraft:ghast");
        DataConverterEntityUUID.MOBS.add("minecraft:giant");
        DataConverterEntityUUID.MOBS.add("minecraft:guardian");
        DataConverterEntityUUID.MOBS.add("minecraft:husk");
        DataConverterEntityUUID.MOBS.add("minecraft:illusioner");
        DataConverterEntityUUID.MOBS.add("minecraft:magma_cube");
        DataConverterEntityUUID.MOBS.add("minecraft:pufferfish");
        DataConverterEntityUUID.MOBS.add("minecraft:zombified_piglin");
        DataConverterEntityUUID.MOBS.add("minecraft:salmon");
        DataConverterEntityUUID.MOBS.add("minecraft:shulker");
        DataConverterEntityUUID.MOBS.add("minecraft:silverfish");
        DataConverterEntityUUID.MOBS.add("minecraft:skeleton");
        DataConverterEntityUUID.MOBS.add("minecraft:slime");
        DataConverterEntityUUID.MOBS.add("minecraft:snow_golem");
        DataConverterEntityUUID.MOBS.add("minecraft:spider");
        DataConverterEntityUUID.MOBS.add("minecraft:squid");
        DataConverterEntityUUID.MOBS.add("minecraft:stray");
        DataConverterEntityUUID.MOBS.add("minecraft:tropical_fish");
        DataConverterEntityUUID.MOBS.add("minecraft:vex");
        DataConverterEntityUUID.MOBS.add("minecraft:villager");
        DataConverterEntityUUID.MOBS.add("minecraft:iron_golem");
        DataConverterEntityUUID.MOBS.add("minecraft:vindicator");
        DataConverterEntityUUID.MOBS.add("minecraft:pillager");
        DataConverterEntityUUID.MOBS.add("minecraft:wandering_trader");
        DataConverterEntityUUID.MOBS.add("minecraft:witch");
        DataConverterEntityUUID.MOBS.add("minecraft:wither");
        DataConverterEntityUUID.MOBS.add("minecraft:wither_skeleton");
        DataConverterEntityUUID.MOBS.add("minecraft:zombie");
        DataConverterEntityUUID.MOBS.add("minecraft:zombie_villager");
        DataConverterEntityUUID.MOBS.add("minecraft:phantom");
        DataConverterEntityUUID.MOBS.add("minecraft:ravager");
        DataConverterEntityUUID.MOBS.add("minecraft:piglin");
        DataConverterEntityUUID.LIVING_ENTITIES.add("minecraft:armor_stand");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:arrow");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:dragon_fireball");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:firework_rocket");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:fireball");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:llama_spit");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:small_fireball");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:snowball");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:spectral_arrow");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:egg");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:ender_pearl");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:experience_bottle");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:potion");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:trident");
        DataConverterEntityUUID.PROJECTILES.add("minecraft:wither_skull");
    }
}
