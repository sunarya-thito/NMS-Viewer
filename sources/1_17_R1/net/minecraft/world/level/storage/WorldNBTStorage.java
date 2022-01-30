package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.io.FileInputStream;
import java.io.InputStream;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
// CraftBukkit end

public class WorldNBTStorage {

    private static final Logger LOGGER = LogManager.getLogger();
    private final File playerDir;
    protected final DataFixer fixerUpper;

    public WorldNBTStorage(Convertable.ConversionSession convertable_conversionsession, DataFixer datafixer) {
        this.fixerUpper = datafixer;
        this.playerDir = convertable_conversionsession.getWorldFolder(SavedFile.PLAYER_DATA_DIR).toFile();
        this.playerDir.mkdirs();
    }

    public void save(EntityHuman entityhuman) {
        if (org.spigotmc.SpigotConfig.disablePlayerDataSaving) return; // Spigot
        try {
            NBTTagCompound nbttagcompound = entityhuman.save(new NBTTagCompound());
            File file = File.createTempFile(entityhuman.getUniqueIDString() + "-", ".dat", this.playerDir);

            NBTCompressedStreamTools.a(nbttagcompound, file);
            File file1 = new File(this.playerDir, entityhuman.getUniqueIDString() + ".dat");
            File file2 = new File(this.playerDir, entityhuman.getUniqueIDString() + ".dat_old");

            SystemUtils.a(file1, file, file2);
        } catch (Exception exception) {
            WorldNBTStorage.LOGGER.warn("Failed to save player data for {}", entityhuman.getDisplayName().getString());
        }

    }

    @Nullable
    public NBTTagCompound load(EntityHuman entityhuman) {
        NBTTagCompound nbttagcompound = null;

        try {
            File file = new File(this.playerDir, entityhuman.getUniqueIDString() + ".dat");
            // Spigot Start
            boolean usingWrongFile = false;
            if ( !file.exists() )
            {
                file = new File( this.playerDir, java.util.UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + entityhuman.getName() ).getBytes( "UTF-8" ) ).toString() + ".dat");
                if ( file.exists() )
                {
                    usingWrongFile = true;
                    org.bukkit.Bukkit.getServer().getLogger().warning( "Using offline mode UUID file for player " + entityhuman.getName() + " as it is the only copy we can find." );
                }
            }
            // Spigot End

            if (file.exists() && file.isFile()) {
                nbttagcompound = NBTCompressedStreamTools.a(file);
            }
            // Spigot Start
            if ( usingWrongFile )
            {
                file.renameTo( new File( file.getPath() + ".offline-read" ) );
            }
            // Spigot End
        } catch (Exception exception) {
            WorldNBTStorage.LOGGER.warn("Failed to load player data for {}", entityhuman.getDisplayName().getString());
        }

        if (nbttagcompound != null) {
            // CraftBukkit start
            if (entityhuman instanceof EntityPlayer) {
                CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();
                // Only update first played if it is older than the one we have
                long modified = new File(this.playerDir, entityhuman.getUniqueID().toString() + ".dat").lastModified();
                if (modified < player.getFirstPlayed()) {
                    player.setFirstPlayed(modified);
                }
            }
            // CraftBukkit end
            int i = nbttagcompound.hasKeyOfType("DataVersion", 3) ? nbttagcompound.getInt("DataVersion") : -1;

            entityhuman.load(GameProfileSerializer.a(this.fixerUpper, DataFixTypes.PLAYER, nbttagcompound, i));
        }

        return nbttagcompound;
    }

    // CraftBukkit start
    public NBTTagCompound getPlayerData(String s) {
        try {
            File file1 = new File(this.playerDir, s + ".dat");

            if (file1.exists()) {
                return NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file1)));
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load player data for " + s);
        }

        return null;
    }
    // CraftBukkit end

    public String[] getSeenPlayers() {
        String[] astring = this.playerDir.list();

        if (astring == null) {
            astring = new String[0];
        }

        for (int i = 0; i < astring.length; ++i) {
            if (astring[i].endsWith(".dat")) {
                astring[i] = astring[i].substring(0, astring[i].length() - 4);
            }
        }

        return astring;
    }

    // CraftBukkit start
    public File getPlayerDir() {
        return playerDir;
    }
    // CraftBukkit end
}
