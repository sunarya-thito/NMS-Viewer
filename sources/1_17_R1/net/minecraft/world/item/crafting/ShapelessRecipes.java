package net.minecraft.world.item.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
// CraftBukkit end

public class ShapelessRecipes implements RecipeCrafting {

    private final MinecraftKey id;
    final String group;
    final ItemStack result;
    final NonNullList<RecipeItemStack> ingredients;

    public ShapelessRecipes(MinecraftKey minecraftkey, String s, ItemStack itemstack, NonNullList<RecipeItemStack> nonnulllist) {
        this.id = minecraftkey;
        this.group = s;
        this.result = itemstack;
        this.ingredients = nonnulllist;
    }

    // CraftBukkit start
    @SuppressWarnings("unchecked")
    public org.bukkit.inventory.ShapelessRecipe toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, this);
        recipe.setGroup(this.group);

        for (RecipeItemStack list : this.ingredients) {
            recipe.addIngredient(CraftRecipe.toBukkit(list));
        }
        return recipe;
    }
    // CraftBukkit end

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SHAPELESS_RECIPE;
    }

    @Override
    public String d() {
        return this.group;
    }

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    @Override
    public NonNullList<RecipeItemStack> a() {
        return this.ingredients;
    }

    public boolean a(InventoryCrafting inventorycrafting, World world) {
        AutoRecipeStackManager autorecipestackmanager = new AutoRecipeStackManager();
        int i = 0;

        for (int j = 0; j < inventorycrafting.getSize(); ++j) {
            ItemStack itemstack = inventorycrafting.getItem(j);

            if (!itemstack.isEmpty()) {
                ++i;
                autorecipestackmanager.a(itemstack, 1);
            }
        }

        return i == this.ingredients.size() && autorecipestackmanager.a(this, (IntList) null);
    }

    public ItemStack a(InventoryCrafting inventorycrafting) {
        return this.result.cloneItemStack();
    }

    @Override
    public boolean a(int i, int j) {
        return i * j >= this.ingredients.size();
    }

    public static class a implements RecipeSerializer<ShapelessRecipes> {

        public a() {}

        @Override
        public ShapelessRecipes a(MinecraftKey minecraftkey, JsonObject jsonobject) {
            String s = ChatDeserializer.a(jsonobject, "group", "");
            NonNullList<RecipeItemStack> nonnulllist = a(ChatDeserializer.u(jsonobject, "ingredients"));

            if (nonnulllist.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else if (nonnulllist.size() > 9) {
                throw new JsonParseException("Too many ingredients for shapeless recipe");
            } else {
                ItemStack itemstack = ShapedRecipes.a(ChatDeserializer.t(jsonobject, "result"));

                return new ShapelessRecipes(minecraftkey, s, itemstack, nonnulllist);
            }
        }

        private static NonNullList<RecipeItemStack> a(JsonArray jsonarray) {
            NonNullList<RecipeItemStack> nonnulllist = NonNullList.a();

            for (int i = 0; i < jsonarray.size(); ++i) {
                RecipeItemStack recipeitemstack = RecipeItemStack.a(jsonarray.get(i));

                if (!recipeitemstack.d()) {
                    nonnulllist.add(recipeitemstack);
                }
            }

            return nonnulllist;
        }

        @Override
        public ShapelessRecipes a(MinecraftKey minecraftkey, PacketDataSerializer packetdataserializer) {
            String s = packetdataserializer.p();
            int i = packetdataserializer.j();
            NonNullList<RecipeItemStack> nonnulllist = NonNullList.a(i, RecipeItemStack.EMPTY);

            for (int j = 0; j < nonnulllist.size(); ++j) {
                nonnulllist.set(j, RecipeItemStack.b(packetdataserializer));
            }

            ItemStack itemstack = packetdataserializer.o();

            return new ShapelessRecipes(minecraftkey, s, itemstack, nonnulllist);
        }

        public void a(PacketDataSerializer packetdataserializer, ShapelessRecipes shapelessrecipes) {
            packetdataserializer.a(shapelessrecipes.group);
            packetdataserializer.d(shapelessrecipes.ingredients.size());
            Iterator iterator = shapelessrecipes.ingredients.iterator();

            while (iterator.hasNext()) {
                RecipeItemStack recipeitemstack = (RecipeItemStack) iterator.next();

                recipeitemstack.a(packetdataserializer);
            }

            packetdataserializer.a(shapelessrecipes.result);
        }
    }
}
