// mc-dev import
package net.minecraft.server;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.Advancements;
import net.minecraft.advancements.critereon.LootDeserializationContext;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementDataWorld extends ResourceDataJson {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = (new GsonBuilder()).create();
    public Advancements advancements = new Advancements();
    private final LootPredicateManager predicateManager;

    public AdvancementDataWorld(LootPredicateManager lootpredicatemanager) {
        super(AdvancementDataWorld.GSON, "advancements");
        this.predicateManager = lootpredicatemanager;
    }

    protected void a(Map<MinecraftKey, JsonElement> map, IResourceManager iresourcemanager, GameProfilerFiller gameprofilerfiller) {
        Map<MinecraftKey, Advancement.SerializedAdvancement> map1 = Maps.newHashMap();

        map.forEach((minecraftkey, jsonelement) -> {
            // Spigot start
            if (org.spigotmc.SpigotConfig.disabledAdvancements != null && (org.spigotmc.SpigotConfig.disabledAdvancements.contains("*") || org.spigotmc.SpigotConfig.disabledAdvancements.contains(minecraftkey.toString()) || org.spigotmc.SpigotConfig.disabledAdvancements.contains(minecraftkey.getNamespace()))) {
                return;
            }
            // Spigot end

            try {
                JsonObject jsonobject = ChatDeserializer.m(jsonelement, "advancement");
                Advancement.SerializedAdvancement advancement_serializedadvancement = Advancement.SerializedAdvancement.a(jsonobject, new LootDeserializationContext(minecraftkey, this.predicateManager));

                map1.put(minecraftkey, advancement_serializedadvancement);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                AdvancementDataWorld.LOGGER.error("Parsing error loading custom advancement {}: {}", minecraftkey, jsonparseexception.getMessage());
            }

        });
        Advancements advancements = new Advancements();

        advancements.a((Map) map1);
        Iterator iterator = advancements.b().iterator();

        while (iterator.hasNext()) {
            Advancement advancement = (Advancement) iterator.next();

            if (advancement.c() != null) {
                AdvancementTree.a(advancement);
            }
        }

        this.advancements = advancements;
    }

    @Nullable
    public Advancement a(MinecraftKey minecraftkey) {
        return this.advancements.a(minecraftkey);
    }

    public Collection<Advancement> getAdvancements() {
        return this.advancements.c();
    }
}
