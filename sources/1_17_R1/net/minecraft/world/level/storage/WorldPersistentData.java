package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.PersistentBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldPersistentData {

    private static final Logger LOGGER = LogManager.getLogger();
    public final Map<String, PersistentBase> cache = Maps.newHashMap();
    private final DataFixer fixerUpper;
    private final File dataFolder;

    public WorldPersistentData(File file, DataFixer datafixer) {
        this.fixerUpper = datafixer;
        this.dataFolder = file;
    }

    private File a(String s) {
        return new File(this.dataFolder, s + ".dat");
    }

    public <T extends PersistentBase> T a(Function<NBTTagCompound, T> function, Supplier<T> supplier, String s) {
        T t0 = this.a(function, s);

        if (t0 != null) {
            return t0;
        } else {
            T t1 = (PersistentBase) supplier.get();

            this.a(s, t1);
            return t1;
        }
    }

    @Nullable
    public <T extends PersistentBase> T a(Function<NBTTagCompound, T> function, String s) {
        PersistentBase persistentbase = (PersistentBase) this.cache.get(s);

        if (persistentbase == null && !this.cache.containsKey(s)) {
            persistentbase = this.b(function, s);
            this.cache.put(s, persistentbase);
        }

        return persistentbase;
    }

    @Nullable
    private <T extends PersistentBase> T b(Function<NBTTagCompound, T> function, String s) {
        try {
            File file = this.a(s);

            if (file.exists()) {
                NBTTagCompound nbttagcompound = this.a(s, SharedConstants.getGameVersion().getWorldVersion());

                return (PersistentBase) function.apply(nbttagcompound.getCompound("data"));
            }
        } catch (Exception exception) {
            WorldPersistentData.LOGGER.error("Error loading saved data: {}", s, exception);
        }

        return null;
    }

    public void a(String s, PersistentBase persistentbase) {
        this.cache.put(s, persistentbase);
    }

    public NBTTagCompound a(String s, int i) throws IOException {
        File file = this.a(s);
        FileInputStream fileinputstream = new FileInputStream(file);

        NBTTagCompound nbttagcompound;

        try {
            PushbackInputStream pushbackinputstream = new PushbackInputStream(fileinputstream, 2);

            try {
                NBTTagCompound nbttagcompound1;

                if (this.a(pushbackinputstream)) {
                    nbttagcompound1 = NBTCompressedStreamTools.a((InputStream) pushbackinputstream);
                } else {
                    DataInputStream datainputstream = new DataInputStream(pushbackinputstream);

                    try {
                        nbttagcompound1 = NBTCompressedStreamTools.a((DataInput) datainputstream);
                    } catch (Throwable throwable) {
                        try {
                            datainputstream.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }

                        throw throwable;
                    }

                    datainputstream.close();
                }

                int j = nbttagcompound1.hasKeyOfType("DataVersion", 99) ? nbttagcompound1.getInt("DataVersion") : 1343;

                nbttagcompound = GameProfileSerializer.a(this.fixerUpper, DataFixTypes.SAVED_DATA, nbttagcompound1, j, i);
            } catch (Throwable throwable2) {
                try {
                    pushbackinputstream.close();
                } catch (Throwable throwable3) {
                    throwable2.addSuppressed(throwable3);
                }

                throw throwable2;
            }

            pushbackinputstream.close();
        } catch (Throwable throwable4) {
            try {
                fileinputstream.close();
            } catch (Throwable throwable5) {
                throwable4.addSuppressed(throwable5);
            }

            throw throwable4;
        }

        fileinputstream.close();
        return nbttagcompound;
    }

    private boolean a(PushbackInputStream pushbackinputstream) throws IOException {
        byte[] abyte = new byte[2];
        boolean flag = false;
        int i = pushbackinputstream.read(abyte, 0, 2);

        if (i == 2) {
            int j = (abyte[1] & 255) << 8 | abyte[0] & 255;

            if (j == 35615) {
                flag = true;
            }
        }

        if (i != 0) {
            pushbackinputstream.unread(abyte, 0, i);
        }

        return flag;
    }

    public void a() {
        this.cache.forEach((s, persistentbase) -> {
            if (persistentbase != null) {
                persistentbase.a(this.a(s));
            }

        });
    }
}
