package net.minecraft.world.level.block.state.predicate;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockStatePredicate implements Predicate<IBlockData> {

    public static final Predicate<IBlockData> ANY = (iblockdata) -> {
        return true;
    };
    private final BlockStateList<Block, IBlockData> definition;
    private final Map<IBlockState<?>, Predicate<Object>> properties = Maps.newHashMap();

    private BlockStatePredicate(BlockStateList<Block, IBlockData> blockstatelist) {
        this.definition = blockstatelist;
    }

    public static BlockStatePredicate a(Block block) {
        return new BlockStatePredicate(block.getStates());
    }

    public boolean test(@Nullable IBlockData iblockdata) {
        if (iblockdata != null && iblockdata.getBlock().equals(this.definition.getBlock())) {
            if (this.properties.isEmpty()) {
                return true;
            } else {
                Iterator iterator = this.properties.entrySet().iterator();

                Entry entry;

                do {
                    if (!iterator.hasNext()) {
                        return true;
                    }

                    entry = (Entry) iterator.next();
                } while (this.a(iblockdata, (IBlockState) entry.getKey(), (Predicate) entry.getValue()));

                return false;
            }
        } else {
            return false;
        }
    }

    protected <T extends Comparable<T>> boolean a(IBlockData iblockdata, IBlockState<T> iblockstate, Predicate<Object> predicate) {
        T t0 = iblockdata.get(iblockstate);

        return predicate.test(t0);
    }

    public <V extends Comparable<V>> BlockStatePredicate a(IBlockState<V> iblockstate, Predicate<Object> predicate) {
        if (!this.definition.d().contains(iblockstate)) {
            throw new IllegalArgumentException(this.definition + " cannot support property " + iblockstate);
        } else {
            this.properties.put(iblockstate, predicate);
            return this;
        }
    }
}
