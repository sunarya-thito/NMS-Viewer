package net.minecraft.util;

import javax.annotation.Nullable;

public class MemoryReserve {

    @Nullable
    private static byte[] reserve = null;

    public MemoryReserve() {}

    public static void a() {
        MemoryReserve.reserve = new byte[10485760];
    }

    public static void b() {
        MemoryReserve.reserve = new byte[0];
    }
}
