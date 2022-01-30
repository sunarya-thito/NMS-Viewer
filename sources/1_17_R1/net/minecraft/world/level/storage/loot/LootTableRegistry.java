package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootTableRegistry extends ResourceDataJson {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = LootSerialization.c().create();
    private Map<MinecraftKey, LootTable> tables = ImmutableMap.of();
    public Map<LootTable, MinecraftKey> lootTableToKey = ImmutableMap.of(); // CraftBukkit
    private final LootPredicateManager predicateManager;

    public LootTableRegistry(LootPredicateManager lootpredicatemanager) {
        super(LootTableRegistry.GSON, "loot_tables");
        this.predicateManager = lootpredicatemanager;
    }

    public LootTable getLootTable(MinecraftKey minecraftkey) {
        return (LootTable) this.tables.getOrDefault(minecraftkey, LootTable.EMPTY);
    }

    protected void a(Map<MinecraftKey, JsonElement> map, IResourceManager iresourcemanager, GameProfilerFiller gameprofilerfiller) {
        Builder<MinecraftKey, LootTable> builder = ImmutableMap.builder();
        JsonElement jsonelement = (JsonElement) map.remove(LootTables.EMPTY);

        if (jsonelement != null) {
            LootTableRegistry.LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", LootTables.EMPTY);
        }

        map.forEach((minecraftkey, jsonelement1) -> {
            try {
                LootTable loottable = (LootTable) LootTableRegistry.GSON.fromJson(jsonelement1, LootTable.class);

                builder.put(minecraftkey, loottable);
            } catch (Exception exception) {
                LootTableRegistry.LOGGER.error("Couldn't parse loot table {}", minecraftkey, exception);
            }

        });
        builder.put(LootTables.EMPTY, LootTable.EMPTY);
        ImmutableMap<MinecraftKey, LootTable> immutablemap = builder.build();
        LootContextParameterSet lootcontextparameterset = LootContextParameterSets.ALL_PARAMS;
        LootPredicateManager lootpredicatemanager = this.predicateManager;

        Objects.requireNonNull(this.predicateManager);
        Function<MinecraftKey, net.minecraft.world.level.storage.loot.predicates.LootItemCondition> function = lootpredicatemanager::a; // CraftBukkit - decompile error

        Objects.requireNonNull(immutablemap);
        LootCollector lootcollector = new LootCollector(lootcontextparameterset, function, immutablemap::get);

        immutablemap.forEach((minecraftkey, loottable) -> {
            a(lootcollector, minecraftkey, loottable);
        });
        lootcollector.a().forEach((s, s1) -> {
            LootTableRegistry.LOGGER.warn("Found validation problem in {}: {}", s, s1);
        });
        this.tables = immutablemap;
        // CraftBukkit start - build a reversed registry map
        ImmutableMap.Builder<LootTable, MinecraftKey> lootTableToKeyBuilder = ImmutableMap.builder();
        this.tables.forEach((lootTable, key) -> lootTableToKeyBuilder.put(key, lootTable));
        this.lootTableToKey = lootTableToKeyBuilder.build();
        // CraftBukkit end
    }

    public static void a(LootCollector lootcollector, MinecraftKey minecraftkey, LootTable loottable) {
        loottable.a(lootcollector.a(loottable.getLootContextParameterSet()).a("{" + minecraftkey + "}", minecraftkey));
    }

    public static JsonElement a(LootTable loottable) {
        return LootTableRegistry.GSON.toJsonTree(loottable);
    }

    public Set<MinecraftKey> a() {
        return this.tables.keySet();
    }
}
