package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap; // CraftBukkit

public class CraftingManager extends ResourceDataJson {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogManager.getLogger();
    public Map<Recipes<?>, Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>>> recipes = ImmutableMap.of(); // CraftBukkit
    private boolean hasErrors;

    public CraftingManager() {
        super(CraftingManager.GSON, "recipes");
    }

    protected void a(Map<MinecraftKey, JsonElement> map, IResourceManager iresourcemanager, GameProfilerFiller gameprofilerfiller) {
        this.hasErrors = false;
        // CraftBukkit start - SPIGOT-5667 make sure all types are populated and mutable
        Map<Recipes<?>, Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>>> map1 = Maps.newHashMap();
        for (Recipes<?> recipeType : IRegistry.RECIPE_TYPE) {
            map1.put(recipeType, new Object2ObjectLinkedOpenHashMap<>());
        }
        // CraftBukkit end
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<MinecraftKey, JsonElement> entry = (Entry) iterator.next();
            MinecraftKey minecraftkey = (MinecraftKey) entry.getKey();

            try {
                IRecipe<?> irecipe = a(minecraftkey, ChatDeserializer.m((JsonElement) entry.getValue(), "top element"));

                // CraftBukkit start
                (map1.computeIfAbsent(irecipe.g(), (recipes) -> {
                    return new Object2ObjectLinkedOpenHashMap<>();
                    // CraftBukkit end
                })).put(minecraftkey, irecipe);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                CraftingManager.LOGGER.error("Parsing error loading recipe {}", minecraftkey, jsonparseexception);
            }
        }

        this.recipes = (Map) map1.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry1) -> {
            return (entry1.getValue()); // CraftBukkit
        }));
        CraftingManager.LOGGER.info("Loaded {} recipes", map1.size());
    }

    // CraftBukkit start
    public void addRecipe(IRecipe<?> irecipe) {
        org.spigotmc.AsyncCatcher.catchOp("Recipe Add"); // Spigot
        Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>> map = this.recipes.get(irecipe.g()); // CraftBukkit

        if (map.containsKey(irecipe.getKey())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + irecipe.getKey());
        } else {
            map.putAndMoveToFirst(irecipe.getKey(), irecipe); // CraftBukkit - SPIGOT-4638: last recipe gets priority
        }
    }
    // CraftBukkit end

    public boolean a() {
        return this.hasErrors;
    }

    public <C extends IInventory, T extends IRecipe<C>> Optional<T> craft(Recipes<T> recipes, C c0, World world) {
        // CraftBukkit start
        Optional<T> recipe = this.b(recipes).values().stream().flatMap((irecipe) -> {
            return SystemUtils.a(recipes.a(irecipe, world, c0));
        }).findFirst();
        c0.setCurrentRecipe(recipe.orElse(null)); // CraftBukkit - Clear recipe when no recipe is found
        // CraftBukkit end
        return recipe;
    }

    public <C extends IInventory, T extends IRecipe<C>> List<T> a(Recipes<T> recipes) {
        return (List) this.b(recipes).values().stream().map((irecipe) -> {
            return irecipe;
        }).collect(Collectors.toList());
    }

    public <C extends IInventory, T extends IRecipe<C>> List<T> b(Recipes<T> recipes, C c0, World world) {
        return (List) this.b(recipes).values().stream().flatMap((irecipe) -> {
            return SystemUtils.a(recipes.a(irecipe, world, c0));
        }).sorted(Comparator.comparing((irecipe) -> {
            return irecipe.getResult().n();
        })).collect(Collectors.toList());
    }

    private <C extends IInventory, T extends IRecipe<C>> Map<MinecraftKey, IRecipe<C>> b(Recipes<T> recipes) {
        return (Map) this.recipes.getOrDefault(recipes, new Object2ObjectLinkedOpenHashMap<>()); // CraftBukkit
    }

    public <C extends IInventory, T extends IRecipe<C>> NonNullList<ItemStack> c(Recipes<T> recipes, C c0, World world) {
        Optional<T> optional = this.craft(recipes, c0, world);

        if (optional.isPresent()) {
            return ((IRecipe) optional.get()).b(c0);
        } else {
            NonNullList<ItemStack> nonnulllist = NonNullList.a(c0.getSize(), ItemStack.EMPTY);

            for (int i = 0; i < nonnulllist.size(); ++i) {
                nonnulllist.set(i, c0.getItem(i));
            }

            return nonnulllist;
        }
    }

    public Optional<? extends IRecipe<?>> getRecipe(MinecraftKey minecraftkey) {
        return this.recipes.values().stream().map((map) -> {
            return map.get(minecraftkey); // CraftBukkit - decompile error
        }).filter(Objects::nonNull).findFirst();
    }

    public Collection<IRecipe<?>> b() {
        return (Collection) this.recipes.values().stream().flatMap((map) -> {
            return map.values().stream();
        }).collect(Collectors.toSet());
    }

    public Stream<MinecraftKey> d() {
        return this.recipes.values().stream().flatMap((map) -> {
            return map.keySet().stream();
        });
    }

    public static IRecipe<?> a(MinecraftKey minecraftkey, JsonObject jsonobject) {
        String s = ChatDeserializer.h(jsonobject, "type");

        return ((RecipeSerializer) IRegistry.RECIPE_SERIALIZER.getOptional(new MinecraftKey(s)).orElseThrow(() -> {
            return new JsonSyntaxException("Invalid or unsupported recipe type '" + s + "'");
        })).a(minecraftkey, jsonobject);
    }

    public void a(Iterable<IRecipe<?>> iterable) {
        this.hasErrors = false;
        Map<Recipes<?>, Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>>> map = Maps.newHashMap(); // CraftBukkit

        iterable.forEach((irecipe) -> {
            Map<MinecraftKey, IRecipe<?>> map1 = (Map) map.computeIfAbsent(irecipe.g(), (recipes) -> {
                return new Object2ObjectLinkedOpenHashMap<>(); // CraftBukkit
            });
            IRecipe<?> irecipe1 = (IRecipe) map1.put(irecipe.getKey(), irecipe);

            if (irecipe1 != null) {
                throw new IllegalStateException("Duplicate recipe ignored with ID " + irecipe.getKey());
            }
        });
        this.recipes = ImmutableMap.copyOf(map);
    }

    // CraftBukkit start
    public void clearRecipes() {
        this.recipes = Maps.newHashMap();

        for (Recipes<?> recipeType : IRegistry.RECIPE_TYPE) {
            this.recipes.put(recipeType, new Object2ObjectLinkedOpenHashMap<>());
        }
    }
    // CraftBukkit end
}
