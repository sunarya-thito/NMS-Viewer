package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;

public abstract class IRecipeComplex implements RecipeCrafting {

    private final MinecraftKey id;

    public IRecipeComplex(MinecraftKey minecraftkey) {
        this.id = minecraftkey;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public ItemStack getResult() {
        return ItemStack.EMPTY;
    }

    // CraftBukkit start
    @Override
    public org.bukkit.inventory.Recipe toBukkitRecipe() {
        return new org.bukkit.craftbukkit.inventory.CraftComplexRecipe(this);
    }
    // CraftBukkit end
}
