package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionFileBitSet {

    private final BitSet used = new BitSet();

    public RegionFileBitSet() {}

    public void a(int i, int j) {
        this.used.set(i, i + j);
    }

    public void b(int i, int j) {
        this.used.clear(i, i + j);
    }

    public int a(int i) {
        int j = 0;

        while (true) {
            int k = this.used.nextClearBit(j);
            int l = this.used.nextSetBit(k);

            if (l == -1 || l - k >= i) {
                this.a(k, i);
                return k;
            }

            j = l;
        }
    }

    @VisibleForTesting
    public IntSet a() {
        return (IntSet) this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
    }
}
