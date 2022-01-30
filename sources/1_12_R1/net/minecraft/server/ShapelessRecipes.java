package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Iterator;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
// CraftBukkit end

public class ShapelessRecipes implements IRecipe {

    private final ItemStack result;
    private final NonNullList<RecipeItemStack> ingredients;
    private final String c;
    // CraftBukkit start
    public MinecraftKey key;

    @Override
    public void setKey(MinecraftKey key) {
        this.key = key;
    }
    // CraftBukkit end

    public ShapelessRecipes(String s, ItemStack itemstack, NonNullList<RecipeItemStack> nonnulllist) {
        this.c = s;
        this.result = itemstack;
        this.ingredients = nonnulllist;
    }

    // CraftBukkit start
    @SuppressWarnings("unchecked")
    public org.bukkit.inventory.ShapelessRecipe toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, this);
        for (RecipeItemStack list : this.ingredients) {
            if (list != null) {
                net.minecraft.server.ItemStack stack = list.choices[0];
                recipe.addIngredient(org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(stack.getItem()), (list.choices.length) > 1 ? 32767 : stack.getData());
            }
        }
        return recipe;
    }
    // CraftBukkit end

    public ItemStack b() {
        return this.result;
    }

    public NonNullList<RecipeItemStack> d() {
        return this.ingredients;
    }

    public NonNullList<ItemStack> b(InventoryCrafting inventorycrafting) {
        NonNullList nonnulllist = NonNullList.a(inventorycrafting.getSize(), ItemStack.a);

        for (int i = 0; i < nonnulllist.size(); ++i) {
            ItemStack itemstack = inventorycrafting.getItem(i);

            if (itemstack.getItem().r()) {
                nonnulllist.set(i, new ItemStack(itemstack.getItem().q()));
            }
        }

        return nonnulllist;
    }

    public boolean a(InventoryCrafting inventorycrafting, World world) {
        ArrayList arraylist = Lists.newArrayList(this.ingredients);

        for (int i = 0; i < inventorycrafting.i(); ++i) {
            for (int j = 0; j < inventorycrafting.j(); ++j) {
                ItemStack itemstack = inventorycrafting.c(j, i);

                if (!itemstack.isEmpty()) {
                    boolean flag = false;
                    Iterator iterator = arraylist.iterator();

                    while (iterator.hasNext()) {
                        RecipeItemStack recipeitemstack = (RecipeItemStack) iterator.next();

                        if (recipeitemstack.a(itemstack)) {
                            flag = true;
                            arraylist.remove(recipeitemstack);
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }
            }
        }

        return arraylist.isEmpty();
    }

    public ItemStack craftItem(InventoryCrafting inventorycrafting) {
        return this.result.cloneItemStack();
    }

    public static ShapelessRecipes a(JsonObject jsonobject) {
        String s = ChatDeserializer.a(jsonobject, "group", "");
        NonNullList nonnulllist = a(ChatDeserializer.u(jsonobject, "ingredients"));

        if (nonnulllist.isEmpty()) {
            throw new JsonParseException("No ingredients for shapeless recipe");
        } else if (nonnulllist.size() > 9) {
            throw new JsonParseException("Too many ingredients for shapeless recipe");
        } else {
            ItemStack itemstack = ShapedRecipes.a(ChatDeserializer.t(jsonobject, "result"), true);

            return new ShapelessRecipes(s, itemstack, nonnulllist);
        }
    }

    private static NonNullList<RecipeItemStack> a(JsonArray jsonarray) {
        NonNullList nonnulllist = NonNullList.a();

        for (int i = 0; i < jsonarray.size(); ++i) {
            RecipeItemStack recipeitemstack = ShapedRecipes.a(jsonarray.get(i));

            if (recipeitemstack != RecipeItemStack.a) {
                nonnulllist.add(recipeitemstack);
            }
        }

        return nonnulllist;
    }
}
