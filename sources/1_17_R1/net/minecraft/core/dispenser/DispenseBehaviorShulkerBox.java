package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.ISourceBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.context.BlockActionContextDirectional;
import net.minecraft.world.level.block.BlockDispenser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public class DispenseBehaviorShulkerBox extends DispenseBehaviorMaybe {

    private static final Logger LOGGER = LogManager.getLogger();

    public DispenseBehaviorShulkerBox() {}

    @Override
    protected ItemStack a(ISourceBlock isourceblock, ItemStack itemstack) {
        this.a(false);
        Item item = itemstack.getItem();

        if (item instanceof ItemBlock) {
            EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockData().get(BlockDispenser.FACING);
            BlockPosition blockposition = isourceblock.getBlockPosition().shift(enumdirection);
            EnumDirection enumdirection1 = isourceblock.getWorld().isEmpty(blockposition.down()) ? enumdirection : EnumDirection.UP;

            // CraftBukkit start
            org.bukkit.block.Block bukkitBlock = isourceblock.getWorld().getWorld().getBlockAt(isourceblock.getBlockPosition().getX(), isourceblock.getBlockPosition().getY(), isourceblock.getBlockPosition().getZ());
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

            BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
            if (!BlockDispenser.eventFired) {
                isourceblock.getWorld().getCraftServer().getPluginManager().callEvent(event);
            }

            if (event.isCancelled()) {
                return itemstack;
            }

            if (!event.getItem().equals(craftItem)) {
                // Chain to handler for new item
                ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                    idispensebehavior.dispense(isourceblock, eventStack);
                    return itemstack;
                }
            }
            // CraftBukkit end

            try {
                this.a(((ItemBlock) item).a((BlockActionContext) (new BlockActionContextDirectional(isourceblock.getWorld(), blockposition, enumdirection, itemstack, enumdirection1))).a());
            } catch (Exception exception) {
                DispenseBehaviorShulkerBox.LOGGER.error("Error trying to place shulker box at {}", blockposition, exception);
            }
        }

        return itemstack;
    }
}
