package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeMapClone extends IRecipeComplex {

    public RecipeMapClone(MinecraftKey minecraftkey) {
        super(minecraftkey);
    }

    public boolean matches(InventoryCrafting inventorycrafting, World world) {
        int i = 0;
        ItemStack itemstack = ItemStack.EMPTY;

        for (int j = 0; j < inventorycrafting.getContainerSize(); ++j) {
            ItemStack itemstack1 = inventorycrafting.getItem(j);

            if (!itemstack1.isEmpty()) {
                if (itemstack1.is(Items.FILLED_MAP)) {
                    if (!itemstack.isEmpty()) {
                        return false;
                    }

                    itemstack = itemstack1;
                } else {
                    if (!itemstack1.is(Items.MAP)) {
                        return false;
                    }

                    ++i;
                }
            }
        }

        return !itemstack.isEmpty() && i > 0;
    }

    public ItemStack assemble(InventoryCrafting inventorycrafting) {
        int i = 0;
        ItemStack itemstack = ItemStack.EMPTY;

        for (int j = 0; j < inventorycrafting.getContainerSize(); ++j) {
            ItemStack itemstack1 = inventorycrafting.getItem(j);

            if (!itemstack1.isEmpty()) {
                if (itemstack1.is(Items.FILLED_MAP)) {
                    if (!itemstack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemstack = itemstack1;
                } else {
                    if (!itemstack1.is(Items.MAP)) {
                        return ItemStack.EMPTY;
                    }

                    ++i;
                }
            }
        }

        if (!itemstack.isEmpty() && i >= 1) {
            ItemStack itemstack2 = itemstack.copy();

            itemstack2.setCount(i + 1);
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int i, int j) {
        return i >= 3 && j >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.MAP_CLONING;
    }
}
