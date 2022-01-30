package net.minecraft.network;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTReadLimiter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

import org.bukkit.craftbukkit.inventory.CraftItemStack; // CraftBukkit

public class PacketDataSerializer extends ByteBuf {

    private static final int MAX_VARINT_SIZE = 5;
    private static final int MAX_VARLONG_SIZE = 10;
    private static final int DEFAULT_NBT_QUOTA = 2097152;
    private final ByteBuf source;
    public static final short MAX_STRING_LENGTH = 32767;
    public static final int MAX_COMPONENT_STRING_LENGTH = 262144;

    public PacketDataSerializer(ByteBuf bytebuf) {
        this.source = bytebuf;
    }

    public static int getVarIntSize(int i) {
        for (int j = 1; j < 5; ++j) {
            if ((i & -1 << j * 7) == 0) {
                return j;
            }
        }

        return 5;
    }

    public static int getVarLongSize(long i) {
        for (int j = 1; j < 10; ++j) {
            if ((i & -1L << j * 7) == 0L) {
                return j;
            }
        }

        return 10;
    }

    public <T> T readWithCodec(Codec<T> codec) {
        NBTTagCompound nbttagcompound = this.readAnySizeNbt();
        DataResult<T> dataresult = codec.parse(DynamicOpsNBT.INSTANCE, nbttagcompound);

        dataresult.error().ifPresent((partialresult) -> {
            String s = partialresult.message();

            throw new EncoderException("Failed to decode: " + s + " " + nbttagcompound);
        });
        return dataresult.result().get();
    }

    public <T> void writeWithCodec(Codec<T> codec, T t0) {
        DataResult<NBTBase> dataresult = codec.encodeStart(DynamicOpsNBT.INSTANCE, t0);

        dataresult.error().ifPresent((partialresult) -> {
            String s = partialresult.message();

            throw new EncoderException("Failed to encode: " + s + " " + t0);
        });
        this.writeNbt((NBTTagCompound) dataresult.result().get());
    }

    public static <T> IntFunction<T> limitValue(IntFunction<T> intfunction, int i) {
        return (j) -> {
            if (j > i) {
                throw new DecoderException("Value " + j + " is larger than limit " + i);
            } else {
                return intfunction.apply(j);
            }
        };
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> intfunction, Function<PacketDataSerializer, T> function) {
        int i = this.readVarInt();
        C c0 = intfunction.apply(i); // CraftBukkit - decompile error

        for (int j = 0; j < i; ++j) {
            c0.add(function.apply(this));
        }

        return c0;
    }

    public <T> void writeCollection(Collection<T> collection, BiConsumer<PacketDataSerializer, T> biconsumer) {
        this.writeVarInt(collection.size());
        Iterator<T> iterator = collection.iterator(); // CraftBukkit - decompile error

        while (iterator.hasNext()) {
            T t0 = iterator.next();

            biconsumer.accept(this, t0);
        }

    }

    public <T> List<T> readList(Function<PacketDataSerializer, T> function) {
        return (List) this.readCollection((java.util.function.IntFunction) Lists::newArrayListWithCapacity, function); // CraftBukkit - decompile error
    }

    public IntList readIntIdList() {
        int i = this.readVarInt();
        IntArrayList intarraylist = new IntArrayList();

        for (int j = 0; j < i; ++j) {
            intarraylist.add(this.readVarInt());
        }

        return intarraylist;
    }

    public void writeIntIdList(IntList intlist) {
        this.writeVarInt(intlist.size());
        intlist.forEach((java.util.function.IntConsumer) this::writeVarInt); // CraftBukkit - decompile error
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> intfunction, Function<PacketDataSerializer, K> function, Function<PacketDataSerializer, V> function1) {
        int i = this.readVarInt();
        M m0 = intfunction.apply(i); // CraftBukkit - decompile error

        for (int j = 0; j < i; ++j) {
            K k0 = function.apply(this);
            V v0 = function1.apply(this);

            m0.put(k0, v0);
        }

        return m0;
    }

    public <K, V> Map<K, V> readMap(Function<PacketDataSerializer, K> function, Function<PacketDataSerializer, V> function1) {
        return this.readMap(Maps::newHashMapWithExpectedSize, function, function1);
    }

    public <K, V> void writeMap(Map<K, V> map, BiConsumer<PacketDataSerializer, K> biconsumer, BiConsumer<PacketDataSerializer, V> biconsumer1) {
        this.writeVarInt(map.size());
        map.forEach((object, object1) -> {
            biconsumer.accept(this, object);
            biconsumer1.accept(this, object1);
        });
    }

    public void readWithCount(Consumer<PacketDataSerializer> consumer) {
        int i = this.readVarInt();

        for (int j = 0; j < i; ++j) {
            consumer.accept(this);
        }

    }

    public <T> void writeOptional(Optional<T> optional, BiConsumer<PacketDataSerializer, T> biconsumer) {
        if (optional.isPresent()) {
            this.writeBoolean(true);
            biconsumer.accept(this, optional.get());
        } else {
            this.writeBoolean(false);
        }

    }

    public <T> Optional<T> readOptional(Function<PacketDataSerializer, T> function) {
        return this.readBoolean() ? Optional.of(function.apply(this)) : Optional.empty();
    }

    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    public PacketDataSerializer writeByteArray(byte[] abyte) {
        this.writeVarInt(abyte.length);
        this.writeBytes(abyte);
        return this;
    }

    public byte[] readByteArray(int i) {
        int j = this.readVarInt();

        if (j > i) {
            throw new DecoderException("ByteArray with size " + j + " is bigger than allowed " + i);
        } else {
            byte[] abyte = new byte[j];

            this.readBytes(abyte);
            return abyte;
        }
    }

    public PacketDataSerializer writeVarIntArray(int[] aint) {
        this.writeVarInt(aint.length);
        int[] aint1 = aint;
        int i = aint.length;

        for (int j = 0; j < i; ++j) {
            int k = aint1[j];

            this.writeVarInt(k);
        }

        return this;
    }

    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    public int[] readVarIntArray(int i) {
        int j = this.readVarInt();

        if (j > i) {
            throw new DecoderException("VarIntArray with size " + j + " is bigger than allowed " + i);
        } else {
            int[] aint = new int[j];

            for (int k = 0; k < aint.length; ++k) {
                aint[k] = this.readVarInt();
            }

            return aint;
        }
    }

    public PacketDataSerializer writeLongArray(long[] along) {
        this.writeVarInt(along.length);
        long[] along1 = along;
        int i = along.length;

        for (int j = 0; j < i; ++j) {
            long k = along1[j];

            this.writeLong(k);
        }

        return this;
    }

    public long[] readLongArray() {
        return this.readLongArray((long[]) null);
    }

    public long[] readLongArray(@Nullable long[] along) {
        return this.readLongArray(along, this.readableBytes() / 8);
    }

    public long[] readLongArray(@Nullable long[] along, int i) {
        int j = this.readVarInt();

        if (along == null || along.length != j) {
            if (j > i) {
                throw new DecoderException("LongArray with size " + j + " is bigger than allowed " + i);
            }

            along = new long[j];
        }

        for (int k = 0; k < along.length; ++k) {
            along[k] = this.readLong();
        }

        return along;
    }

    @VisibleForTesting
    public byte[] accessByteBufWithCorrectSize() {
        int i = this.writerIndex();
        byte[] abyte = new byte[i];

        this.getBytes(0, abyte);
        return abyte;
    }

    public BlockPosition readBlockPos() {
        return BlockPosition.of(this.readLong());
    }

    public PacketDataSerializer writeBlockPos(BlockPosition blockposition) {
        this.writeLong(blockposition.asLong());
        return this;
    }

    public ChunkCoordIntPair readChunkPos() {
        return new ChunkCoordIntPair(this.readLong());
    }

    public PacketDataSerializer writeChunkPos(ChunkCoordIntPair chunkcoordintpair) {
        this.writeLong(chunkcoordintpair.toLong());
        return this;
    }

    public SectionPosition readSectionPos() {
        return SectionPosition.of(this.readLong());
    }

    public PacketDataSerializer writeSectionPos(SectionPosition sectionposition) {
        this.writeLong(sectionposition.asLong());
        return this;
    }

    public IChatBaseComponent readComponent() {
        return IChatBaseComponent.ChatSerializer.fromJson(this.readUtf(262144));
    }

    public PacketDataSerializer writeComponent(IChatBaseComponent ichatbasecomponent) {
        return this.writeUtf(IChatBaseComponent.ChatSerializer.toJson(ichatbasecomponent), 262144);
    }

    public <T extends Enum<T>> T readEnum(Class<T> oclass) {
        return ((T[]) oclass.getEnumConstants())[this.readVarInt()]; // CraftBukkit - fix decompile error
    }

    public PacketDataSerializer writeEnum(Enum<?> oenum) {
        return this.writeVarInt(oenum.ordinal());
    }

    public int readVarInt() {
        int i = 0;
        int j = 0;

        byte b0;

        do {
            b0 = this.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 128) == 128);

        return i;
    }

    public long readVarLong() {
        long i = 0L;
        int j = 0;

        byte b0;

        do {
            b0 = this.readByte();
            i |= (long) (b0 & 127) << j++ * 7;
            if (j > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((b0 & 128) == 128);

        return i;
    }

    public PacketDataSerializer writeUUID(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public PacketDataSerializer writeVarInt(int i) {
        while ((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        this.writeByte(i);
        return this;
    }

    public PacketDataSerializer writeVarLong(long i) {
        while ((i & -128L) != 0L) {
            this.writeByte((int) (i & 127L) | 128);
            i >>>= 7;
        }

        this.writeByte((int) i);
        return this;
    }

    public PacketDataSerializer writeNbt(@Nullable NBTTagCompound nbttagcompound) {
        if (nbttagcompound == null) {
            this.writeByte(0);
        } else {
            try {
                NBTCompressedStreamTools.write(nbttagcompound, (DataOutput) (new ByteBufOutputStream(this)));
            } catch (Exception ioexception) { // CraftBukkit - IOException -> Exception
                throw new EncoderException(ioexception);
            }
        }

        return this;
    }

    @Nullable
    public NBTTagCompound readNbt() {
        return this.readNbt(new NBTReadLimiter(2097152L));
    }

    @Nullable
    public NBTTagCompound readAnySizeNbt() {
        return this.readNbt(NBTReadLimiter.UNLIMITED);
    }

    @Nullable
    public NBTTagCompound readNbt(NBTReadLimiter nbtreadlimiter) {
        int i = this.readerIndex();
        byte b0 = this.readByte();

        if (b0 == 0) {
            return null;
        } else {
            this.readerIndex(i);

            try {
                return NBTCompressedStreamTools.read(new ByteBufInputStream(this), nbtreadlimiter);
            } catch (IOException ioexception) {
                throw new EncoderException(ioexception);
            }
        }
    }

    public PacketDataSerializer writeItem(ItemStack itemstack) {
        if (itemstack.isEmpty() || itemstack.getItem() == null) { // CraftBukkit - NPE fix itemstack.getItem()
            this.writeBoolean(false);
        } else {
            this.writeBoolean(true);
            Item item = itemstack.getItem();

            this.writeVarInt(Item.getId(item));
            this.writeByte(itemstack.getCount());
            NBTTagCompound nbttagcompound = null;

            if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
                // Spigot start - filter
                itemstack = itemstack.copy();
                CraftItemStack.setItemMeta(itemstack, CraftItemStack.getItemMeta(itemstack));
                // Spigot end
                nbttagcompound = itemstack.getTag();
            }

            this.writeNbt(nbttagcompound);
        }

        return this;
    }

    public ItemStack readItem() {
        if (!this.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            int i = this.readVarInt();
            byte b0 = this.readByte();
            ItemStack itemstack = new ItemStack(Item.byId(i), b0);

            itemstack.setTag(this.readNbt());
            // CraftBukkit start
            if (itemstack.getTag() != null) {
                CraftItemStack.setItemMeta(itemstack, CraftItemStack.getItemMeta(itemstack));
            }
            // CraftBukkit end
            return itemstack;
        }
    }

    public String readUtf() {
        return this.readUtf(32767);
    }

    public String readUtf(int i) {
        int j = this.readVarInt();

        if (j > i * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ")");
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String s = this.toString(this.readerIndex(), j, StandardCharsets.UTF_8);

            this.readerIndex(this.readerIndex() + j);
            if (s.length() > i) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + j + " > " + i + ")");
            } else {
                return s;
            }
        }
    }

    public PacketDataSerializer writeUtf(String s) {
        return this.writeUtf(s, 32767);
    }

    public PacketDataSerializer writeUtf(String s, int i) {
        byte[] abyte = s.getBytes(StandardCharsets.UTF_8);

        if (abyte.length > i) {
            throw new EncoderException("String too big (was " + abyte.length + " bytes encoded, max " + i + ")");
        } else {
            this.writeVarInt(abyte.length);
            this.writeBytes(abyte);
            return this;
        }
    }

    public MinecraftKey readResourceLocation() {
        return new MinecraftKey(this.readUtf(32767));
    }

    public PacketDataSerializer writeResourceLocation(MinecraftKey minecraftkey) {
        this.writeUtf(minecraftkey.toString());
        return this;
    }

    public Date readDate() {
        return new Date(this.readLong());
    }

    public PacketDataSerializer writeDate(Date date) {
        this.writeLong(date.getTime());
        return this;
    }

    public MovingObjectPositionBlock readBlockHitResult() {
        BlockPosition blockposition = this.readBlockPos();
        EnumDirection enumdirection = (EnumDirection) this.readEnum(EnumDirection.class);
        float f = this.readFloat();
        float f1 = this.readFloat();
        float f2 = this.readFloat();
        boolean flag = this.readBoolean();

        return new MovingObjectPositionBlock(new Vec3D((double) blockposition.getX() + (double) f, (double) blockposition.getY() + (double) f1, (double) blockposition.getZ() + (double) f2), enumdirection, blockposition, flag);
    }

    public void writeBlockHitResult(MovingObjectPositionBlock movingobjectpositionblock) {
        BlockPosition blockposition = movingobjectpositionblock.getBlockPos();

        this.writeBlockPos(blockposition);
        this.writeEnum(movingobjectpositionblock.getDirection());
        Vec3D vec3d = movingobjectpositionblock.getLocation();

        this.writeFloat((float) (vec3d.x - (double) blockposition.getX()));
        this.writeFloat((float) (vec3d.y - (double) blockposition.getY()));
        this.writeFloat((float) (vec3d.z - (double) blockposition.getZ()));
        this.writeBoolean(movingobjectpositionblock.isInside());
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(this.readLongArray());
    }

    public void writeBitSet(BitSet bitset) {
        this.writeLongArray(bitset.toLongArray());
    }

    public int capacity() {
        return this.source.capacity();
    }

    public ByteBuf capacity(int i) {
        return this.source.capacity(i);
    }

    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    public ByteOrder order() {
        return this.source.order();
    }

    public ByteBuf order(ByteOrder byteorder) {
        return this.source.order(byteorder);
    }

    public ByteBuf unwrap() {
        return this.source.unwrap();
    }

    public boolean isDirect() {
        return this.source.isDirect();
    }

    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    public int readerIndex() {
        return this.source.readerIndex();
    }

    public ByteBuf readerIndex(int i) {
        return this.source.readerIndex(i);
    }

    public int writerIndex() {
        return this.source.writerIndex();
    }

    public ByteBuf writerIndex(int i) {
        return this.source.writerIndex(i);
    }

    public ByteBuf setIndex(int i, int j) {
        return this.source.setIndex(i, j);
    }

    public int readableBytes() {
        return this.source.readableBytes();
    }

    public int writableBytes() {
        return this.source.writableBytes();
    }

    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.source.isReadable();
    }

    public boolean isReadable(int i) {
        return this.source.isReadable(i);
    }

    public boolean isWritable() {
        return this.source.isWritable();
    }

    public boolean isWritable(int i) {
        return this.source.isWritable(i);
    }

    public ByteBuf clear() {
        return this.source.clear();
    }

    public ByteBuf markReaderIndex() {
        return this.source.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return this.source.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return this.source.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return this.source.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return this.source.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return this.source.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int i) {
        return this.source.ensureWritable(i);
    }

    public int ensureWritable(int i, boolean flag) {
        return this.source.ensureWritable(i, flag);
    }

    public boolean getBoolean(int i) {
        return this.source.getBoolean(i);
    }

    public byte getByte(int i) {
        return this.source.getByte(i);
    }

    public short getUnsignedByte(int i) {
        return this.source.getUnsignedByte(i);
    }

    public short getShort(int i) {
        return this.source.getShort(i);
    }

    public short getShortLE(int i) {
        return this.source.getShortLE(i);
    }

    public int getUnsignedShort(int i) {
        return this.source.getUnsignedShort(i);
    }

    public int getUnsignedShortLE(int i) {
        return this.source.getUnsignedShortLE(i);
    }

    public int getMedium(int i) {
        return this.source.getMedium(i);
    }

    public int getMediumLE(int i) {
        return this.source.getMediumLE(i);
    }

    public int getUnsignedMedium(int i) {
        return this.source.getUnsignedMedium(i);
    }

    public int getUnsignedMediumLE(int i) {
        return this.source.getUnsignedMediumLE(i);
    }

    public int getInt(int i) {
        return this.source.getInt(i);
    }

    public int getIntLE(int i) {
        return this.source.getIntLE(i);
    }

    public long getUnsignedInt(int i) {
        return this.source.getUnsignedInt(i);
    }

    public long getUnsignedIntLE(int i) {
        return this.source.getUnsignedIntLE(i);
    }

    public long getLong(int i) {
        return this.source.getLong(i);
    }

    public long getLongLE(int i) {
        return this.source.getLongLE(i);
    }

    public char getChar(int i) {
        return this.source.getChar(i);
    }

    public float getFloat(int i) {
        return this.source.getFloat(i);
    }

    public double getDouble(int i) {
        return this.source.getDouble(i);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf) {
        return this.source.getBytes(i, bytebuf);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
        return this.source.getBytes(i, bytebuf, j);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.source.getBytes(i, bytebuf, j, k);
    }

    public ByteBuf getBytes(int i, byte[] abyte) {
        return this.source.getBytes(i, abyte);
    }

    public ByteBuf getBytes(int i, byte[] abyte, int j, int k) {
        return this.source.getBytes(i, abyte, j, k);
    }

    public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
        return this.source.getBytes(i, bytebuffer);
    }

    public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
        return this.source.getBytes(i, outputstream, j);
    }

    public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
        return this.source.getBytes(i, gatheringbytechannel, j);
    }

    public int getBytes(int i, FileChannel filechannel, long j, int k) throws IOException {
        return this.source.getBytes(i, filechannel, j, k);
    }

    public CharSequence getCharSequence(int i, int j, Charset charset) {
        return this.source.getCharSequence(i, j, charset);
    }

    public ByteBuf setBoolean(int i, boolean flag) {
        return this.source.setBoolean(i, flag);
    }

    public ByteBuf setByte(int i, int j) {
        return this.source.setByte(i, j);
    }

    public ByteBuf setShort(int i, int j) {
        return this.source.setShort(i, j);
    }

    public ByteBuf setShortLE(int i, int j) {
        return this.source.setShortLE(i, j);
    }

    public ByteBuf setMedium(int i, int j) {
        return this.source.setMedium(i, j);
    }

    public ByteBuf setMediumLE(int i, int j) {
        return this.source.setMediumLE(i, j);
    }

    public ByteBuf setInt(int i, int j) {
        return this.source.setInt(i, j);
    }

    public ByteBuf setIntLE(int i, int j) {
        return this.source.setIntLE(i, j);
    }

    public ByteBuf setLong(int i, long j) {
        return this.source.setLong(i, j);
    }

    public ByteBuf setLongLE(int i, long j) {
        return this.source.setLongLE(i, j);
    }

    public ByteBuf setChar(int i, int j) {
        return this.source.setChar(i, j);
    }

    public ByteBuf setFloat(int i, float f) {
        return this.source.setFloat(i, f);
    }

    public ByteBuf setDouble(int i, double d0) {
        return this.source.setDouble(i, d0);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf) {
        return this.source.setBytes(i, bytebuf);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j) {
        return this.source.setBytes(i, bytebuf, j);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.source.setBytes(i, bytebuf, j, k);
    }

    public ByteBuf setBytes(int i, byte[] abyte) {
        return this.source.setBytes(i, abyte);
    }

    public ByteBuf setBytes(int i, byte[] abyte, int j, int k) {
        return this.source.setBytes(i, abyte, j, k);
    }

    public ByteBuf setBytes(int i, ByteBuffer bytebuffer) {
        return this.source.setBytes(i, bytebuffer);
    }

    public int setBytes(int i, InputStream inputstream, int j) throws IOException {
        return this.source.setBytes(i, inputstream, j);
    }

    public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
        return this.source.setBytes(i, scatteringbytechannel, j);
    }

    public int setBytes(int i, FileChannel filechannel, long j, int k) throws IOException {
        return this.source.setBytes(i, filechannel, j, k);
    }

    public ByteBuf setZero(int i, int j) {
        return this.source.setZero(i, j);
    }

    public int setCharSequence(int i, CharSequence charsequence, Charset charset) {
        return this.source.setCharSequence(i, charsequence, charset);
    }

    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    public byte readByte() {
        return this.source.readByte();
    }

    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    public short readShort() {
        return this.source.readShort();
    }

    public short readShortLE() {
        return this.source.readShortLE();
    }

    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    public int readMedium() {
        return this.source.readMedium();
    }

    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    public int readInt() {
        return this.source.readInt();
    }

    public int readIntLE() {
        return this.source.readIntLE();
    }

    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    public long readLong() {
        return this.source.readLong();
    }

    public long readLongLE() {
        return this.source.readLongLE();
    }

    public char readChar() {
        return this.source.readChar();
    }

    public float readFloat() {
        return this.source.readFloat();
    }

    public double readDouble() {
        return this.source.readDouble();
    }

    public ByteBuf readBytes(int i) {
        return this.source.readBytes(i);
    }

    public ByteBuf readSlice(int i) {
        return this.source.readSlice(i);
    }

    public ByteBuf readRetainedSlice(int i) {
        return this.source.readRetainedSlice(i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf) {
        return this.source.readBytes(bytebuf);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i) {
        return this.source.readBytes(bytebuf, i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
        return this.source.readBytes(bytebuf, i, j);
    }

    public ByteBuf readBytes(byte[] abyte) {
        return this.source.readBytes(abyte);
    }

    public ByteBuf readBytes(byte[] abyte, int i, int j) {
        return this.source.readBytes(abyte, i, j);
    }

    public ByteBuf readBytes(ByteBuffer bytebuffer) {
        return this.source.readBytes(bytebuffer);
    }

    public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
        return this.source.readBytes(outputstream, i);
    }

    public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
        return this.source.readBytes(gatheringbytechannel, i);
    }

    public CharSequence readCharSequence(int i, Charset charset) {
        return this.source.readCharSequence(i, charset);
    }

    public int readBytes(FileChannel filechannel, long i, int j) throws IOException {
        return this.source.readBytes(filechannel, i, j);
    }

    public ByteBuf skipBytes(int i) {
        return this.source.skipBytes(i);
    }

    public ByteBuf writeBoolean(boolean flag) {
        return this.source.writeBoolean(flag);
    }

    public ByteBuf writeByte(int i) {
        return this.source.writeByte(i);
    }

    public ByteBuf writeShort(int i) {
        return this.source.writeShort(i);
    }

    public ByteBuf writeShortLE(int i) {
        return this.source.writeShortLE(i);
    }

    public ByteBuf writeMedium(int i) {
        return this.source.writeMedium(i);
    }

    public ByteBuf writeMediumLE(int i) {
        return this.source.writeMediumLE(i);
    }

    public ByteBuf writeInt(int i) {
        return this.source.writeInt(i);
    }

    public ByteBuf writeIntLE(int i) {
        return this.source.writeIntLE(i);
    }

    public ByteBuf writeLong(long i) {
        return this.source.writeLong(i);
    }

    public ByteBuf writeLongLE(long i) {
        return this.source.writeLongLE(i);
    }

    public ByteBuf writeChar(int i) {
        return this.source.writeChar(i);
    }

    public ByteBuf writeFloat(float f) {
        return this.source.writeFloat(f);
    }

    public ByteBuf writeDouble(double d0) {
        return this.source.writeDouble(d0);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf) {
        return this.source.writeBytes(bytebuf);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i) {
        return this.source.writeBytes(bytebuf, i);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i, int j) {
        return this.source.writeBytes(bytebuf, i, j);
    }

    public ByteBuf writeBytes(byte[] abyte) {
        return this.source.writeBytes(abyte);
    }

    public ByteBuf writeBytes(byte[] abyte, int i, int j) {
        return this.source.writeBytes(abyte, i, j);
    }

    public ByteBuf writeBytes(ByteBuffer bytebuffer) {
        return this.source.writeBytes(bytebuffer);
    }

    public int writeBytes(InputStream inputstream, int i) throws IOException {
        return this.source.writeBytes(inputstream, i);
    }

    public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
        return this.source.writeBytes(scatteringbytechannel, i);
    }

    public int writeBytes(FileChannel filechannel, long i, int j) throws IOException {
        return this.source.writeBytes(filechannel, i, j);
    }

    public ByteBuf writeZero(int i) {
        return this.source.writeZero(i);
    }

    public int writeCharSequence(CharSequence charsequence, Charset charset) {
        return this.source.writeCharSequence(charsequence, charset);
    }

    public int indexOf(int i, int j, byte b0) {
        return this.source.indexOf(i, j, b0);
    }

    public int bytesBefore(byte b0) {
        return this.source.bytesBefore(b0);
    }

    public int bytesBefore(int i, byte b0) {
        return this.source.bytesBefore(i, b0);
    }

    public int bytesBefore(int i, int j, byte b0) {
        return this.source.bytesBefore(i, j, b0);
    }

    public int forEachByte(ByteProcessor byteprocessor) {
        return this.source.forEachByte(byteprocessor);
    }

    public int forEachByte(int i, int j, ByteProcessor byteprocessor) {
        return this.source.forEachByte(i, j, byteprocessor);
    }

    public int forEachByteDesc(ByteProcessor byteprocessor) {
        return this.source.forEachByteDesc(byteprocessor);
    }

    public int forEachByteDesc(int i, int j, ByteProcessor byteprocessor) {
        return this.source.forEachByteDesc(i, j, byteprocessor);
    }

    public ByteBuf copy() {
        return this.source.copy();
    }

    public ByteBuf copy(int i, int j) {
        return this.source.copy(i, j);
    }

    public ByteBuf slice() {
        return this.source.slice();
    }

    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    public ByteBuf slice(int i, int j) {
        return this.source.slice(i, j);
    }

    public ByteBuf retainedSlice(int i, int j) {
        return this.source.retainedSlice(i, j);
    }

    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    public ByteBuffer nioBuffer(int i, int j) {
        return this.source.nioBuffer(i, j);
    }

    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.source.internalNioBuffer(i, j);
    }

    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.source.nioBuffers(i, j);
    }

    public boolean hasArray() {
        return this.source.hasArray();
    }

    public byte[] array() {
        return this.source.array();
    }

    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    public String toString(Charset charset) {
        return this.source.toString(charset);
    }

    public String toString(int i, int j, Charset charset) {
        return this.source.toString(i, j, charset);
    }

    public int hashCode() {
        return this.source.hashCode();
    }

    public boolean equals(Object object) {
        return this.source.equals(object);
    }

    public int compareTo(ByteBuf bytebuf) {
        return this.source.compareTo(bytebuf);
    }

    public String toString() {
        return this.source.toString();
    }

    public ByteBuf retain(int i) {
        return this.source.retain(i);
    }

    public ByteBuf retain() {
        return this.source.retain();
    }

    public ByteBuf touch() {
        return this.source.touch();
    }

    public ByteBuf touch(Object object) {
        return this.source.touch(object);
    }

    public int refCnt() {
        return this.source.refCnt();
    }

    public boolean release() {
        return this.source.release();
    }

    public boolean release(int i) {
        return this.source.release(i);
    }
}
