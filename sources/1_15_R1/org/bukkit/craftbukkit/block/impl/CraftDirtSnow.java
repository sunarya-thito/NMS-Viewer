/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftDirtSnow extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.Snowable {

    public CraftDirtSnow() {
        super();
    }

    public CraftDirtSnow(net.minecraft.server.IBlockData state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftSnowable

    private static final net.minecraft.server.BlockStateBoolean SNOWY = getBoolean(net.minecraft.server.BlockDirtSnow.class, "snowy");

    @Override
    public boolean isSnowy() {
        return get(SNOWY);
    }

    @Override
    public void setSnowy(boolean snowy) {
        set(SNOWY, snowy);
    }
}
