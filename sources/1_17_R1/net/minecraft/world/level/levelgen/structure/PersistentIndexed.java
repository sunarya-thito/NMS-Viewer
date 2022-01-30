package net.minecraft.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.saveddata.PersistentBase;

public class PersistentIndexed extends PersistentBase {

    private static final String TAG_REMAINING_INDEXES = "Remaining";
    private static final String TAG_All_INDEXES = "All";
    private final LongSet all;
    private final LongSet remaining;

    private PersistentIndexed(LongSet longset, LongSet longset1) {
        this.all = longset;
        this.remaining = longset1;
    }

    public PersistentIndexed() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public static PersistentIndexed b(NBTTagCompound nbttagcompound) {
        return new PersistentIndexed(new LongOpenHashSet(nbttagcompound.getLongArray("All")), new LongOpenHashSet(nbttagcompound.getLongArray("Remaining")));
    }

    @Override
    public NBTTagCompound a(NBTTagCompound nbttagcompound) {
        nbttagcompound.a("All", this.all.toLongArray());
        nbttagcompound.a("Remaining", this.remaining.toLongArray());
        return nbttagcompound;
    }

    public void a(long i) {
        this.all.add(i);
        this.remaining.add(i);
    }

    public boolean b(long i) {
        return this.all.contains(i);
    }

    public boolean c(long i) {
        return this.remaining.contains(i);
    }

    public void d(long i) {
        this.remaining.remove(i);
    }

    public LongSet a() {
        return this.all;
    }
}
