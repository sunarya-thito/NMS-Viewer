package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;

// CraftBukkit start
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.util.CraftChatMessage;
// CraftBukkit end

@Immutable
public class ChestLock {

    public static final ChestLock NO_LOCK = new ChestLock("");
    public static final String TAG_LOCK = "Lock";
    public final String key;

    public ChestLock(String s) {
        this.key = s;
    }

    public boolean a(ItemStack itemstack) {
        // CraftBukkit start - SPIGOT-6307: Check for color codes if the lock contains color codes
        if (this.key.isEmpty()) return true;
        if (!itemstack.isEmpty() && itemstack.hasName()) {
            if (this.key.indexOf(ChatColor.COLOR_CHAR) == -1) {
                // The lock key contains no color codes, so let's ignore colors in the item display name (vanilla Minecraft behavior):
                return this.key.equals(itemstack.getName().getString());
            } else {
                // The lock key contains color codes, so let's take them into account:
                return this.key.equals(CraftChatMessage.fromComponent(itemstack.getName()));
            }
        }
        return false;
        // CraftBukkit end
    }

    public void a(NBTTagCompound nbttagcompound) {
        if (!this.key.isEmpty()) {
            nbttagcompound.setString("Lock", this.key);
        }

    }

    public static ChestLock b(NBTTagCompound nbttagcompound) {
        return nbttagcompound.hasKeyOfType("Lock", 8) ? new ChestLock(nbttagcompound.getString("Lock")) : ChestLock.NO_LOCK;
    }
}
