// mc-dev import
package net.minecraft.stats;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutStatistic;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerStatisticManager extends StatisticManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final File file;
    private final Set<Statistic<?>> dirty = Sets.newHashSet();

    public ServerStatisticManager(MinecraftServer minecraftserver, File file) {
        this.server = minecraftserver;
        this.file = file;
        // Spigot start
        for ( Map.Entry<MinecraftKey, Integer> entry : org.spigotmc.SpigotConfig.forcedStats.entrySet() )
        {
            Statistic<MinecraftKey> wrapper = StatisticList.CUSTOM.get( entry.getKey() );
            this.stats.put( wrapper, entry.getValue().intValue() );
        }
        // Spigot end
        if (file.isFile()) {
            try {
                this.parseLocal(minecraftserver.getFixerUpper(), FileUtils.readFileToString(file));
            } catch (IOException ioexception) {
                ServerStatisticManager.LOGGER.error("Couldn't read statistics file {}", file, ioexception);
            } catch (JsonParseException jsonparseexception) {
                ServerStatisticManager.LOGGER.error("Couldn't parse statistics file {}", file, jsonparseexception);
            }
        }

    }

    public void save() {
        if ( org.spigotmc.SpigotConfig.disableStatSaving ) return; // Spigot
        try {
            FileUtils.writeStringToFile(this.file, this.toJson());
        } catch (IOException ioexception) {
            ServerStatisticManager.LOGGER.error("Couldn't save stats", ioexception);
        }

    }

    @Override
    public void setValue(EntityHuman entityhuman, Statistic<?> statistic, int i) {
        if ( org.spigotmc.SpigotConfig.disableStatSaving ) return; // Spigot
        super.setValue(entityhuman, statistic, i);
        this.dirty.add(statistic);
    }

    private Set<Statistic<?>> getDirty() {
        Set<Statistic<?>> set = Sets.newHashSet(this.dirty);

        this.dirty.clear();
        return set;
    }

    public void parseLocal(DataFixer datafixer, String s) {
        try {
            JsonReader jsonreader = new JsonReader(new StringReader(s));

            label52:
            {
                try {
                    jsonreader.setLenient(false);
                    JsonElement jsonelement = Streams.parse(jsonreader);

                    if (!jsonelement.isJsonNull()) {
                        NBTTagCompound nbttagcompound = fromJson(jsonelement.getAsJsonObject());

                        if (!nbttagcompound.contains("DataVersion", 99)) {
                            nbttagcompound.putInt("DataVersion", 1343);
                        }

                        nbttagcompound = GameProfileSerializer.update(datafixer, DataFixTypes.STATS, nbttagcompound, nbttagcompound.getInt("DataVersion"));
                        if (!nbttagcompound.contains("stats", 10)) {
                            break label52;
                        }

                        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("stats");
                        Iterator iterator = nbttagcompound1.getAllKeys().iterator();

                        while (true) {
                            if (!iterator.hasNext()) {
                                break label52;
                            }

                            String s1 = (String) iterator.next();

                            if (nbttagcompound1.contains(s1, 10)) {
                                SystemUtils.ifElse(IRegistry.STAT_TYPE.getOptional(new MinecraftKey(s1)), (statisticwrapper) -> {
                                    NBTTagCompound nbttagcompound2 = nbttagcompound1.getCompound(s1);
                                    Iterator iterator1 = nbttagcompound2.getAllKeys().iterator();

                                    while (iterator1.hasNext()) {
                                        String s2 = (String) iterator1.next();

                                        if (nbttagcompound2.contains(s2, 99)) {
                                            SystemUtils.ifElse(this.getStat(statisticwrapper, s2), (statistic) -> {
                                                this.stats.put(statistic, nbttagcompound2.getInt(s2));
                                            }, () -> {
                                                ServerStatisticManager.LOGGER.warn("Invalid statistic in {}: Don't know what {} is", this.file, s2);
                                            });
                                        } else {
                                            ServerStatisticManager.LOGGER.warn("Invalid statistic value in {}: Don't know what {} is for key {}", this.file, nbttagcompound2.get(s2), s2);
                                        }
                                    }

                                }, () -> {
                                    ServerStatisticManager.LOGGER.warn("Invalid statistic type in {}: Don't know what {} is", this.file, s1);
                                });
                            }
                        }
                    }

                    ServerStatisticManager.LOGGER.error("Unable to parse Stat data from {}", this.file);
                } catch (Throwable throwable) {
                    try {
                        jsonreader.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }

                    throw throwable;
                }

                jsonreader.close();
                return;
            }

            jsonreader.close();
        } catch (IOException | JsonParseException jsonparseexception) {
            ServerStatisticManager.LOGGER.error("Unable to parse Stat data from {}", this.file, jsonparseexception);
        }

    }

    private <T> Optional<Statistic<T>> getStat(StatisticWrapper<T> statisticwrapper, String s) {
        // CraftBukkit - decompile error start
        Optional<MinecraftKey> optional = Optional.ofNullable(MinecraftKey.tryParse(s));
        IRegistry<T> iregistry = statisticwrapper.getRegistry();

        return optional.flatMap(iregistry::getOptional).map(statisticwrapper::get);
        // CraftBukkit - decompile error end
    }

    private static NBTTagCompound fromJson(JsonObject jsonobject) {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        Iterator iterator = jsonobject.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, JsonElement> entry = (Entry) iterator.next();
            JsonElement jsonelement = (JsonElement) entry.getValue();

            if (jsonelement.isJsonObject()) {
                nbttagcompound.put((String) entry.getKey(), fromJson(jsonelement.getAsJsonObject()));
            } else if (jsonelement.isJsonPrimitive()) {
                JsonPrimitive jsonprimitive = jsonelement.getAsJsonPrimitive();

                if (jsonprimitive.isNumber()) {
                    nbttagcompound.putInt((String) entry.getKey(), jsonprimitive.getAsInt());
                }
            }
        }

        return nbttagcompound;
    }

    protected String toJson() {
        Map<StatisticWrapper<?>, JsonObject> map = Maps.newHashMap();
        ObjectIterator objectiterator = this.stats.object2IntEntrySet().iterator();

        while (objectiterator.hasNext()) {
            it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Statistic<?>> it_unimi_dsi_fastutil_objects_object2intmap_entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry) objectiterator.next();
            Statistic<?> statistic = (Statistic) it_unimi_dsi_fastutil_objects_object2intmap_entry.getKey();

            ((JsonObject) map.computeIfAbsent(statistic.getType(), (statisticwrapper) -> {
                return new JsonObject();
            })).addProperty(getKey(statistic).toString(), it_unimi_dsi_fastutil_objects_object2intmap_entry.getIntValue());
        }

        JsonObject jsonobject = new JsonObject();
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<StatisticWrapper<?>, JsonObject> entry = (Entry) iterator.next();

            jsonobject.add(IRegistry.STAT_TYPE.getKey((StatisticWrapper) entry.getKey()).toString(), (JsonElement) entry.getValue());
        }

        JsonObject jsonobject1 = new JsonObject();

        jsonobject1.add("stats", jsonobject);
        jsonobject1.addProperty("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        return jsonobject1.toString();
    }

    private static <T> MinecraftKey getKey(Statistic<T> statistic) {
        return statistic.getType().getRegistry().getKey(statistic.getValue());
    }

    public void markAllDirty() {
        this.dirty.addAll(this.stats.keySet());
    }

    public void sendStats(EntityPlayer entityplayer) {
        Object2IntMap<Statistic<?>> object2intmap = new Object2IntOpenHashMap();
        Iterator iterator = this.getDirty().iterator();

        while (iterator.hasNext()) {
            Statistic<?> statistic = (Statistic) iterator.next();

            object2intmap.put(statistic, this.getValue(statistic));
        }

        entityplayer.connection.send(new PacketPlayOutStatistic(object2intmap));
    }
}
