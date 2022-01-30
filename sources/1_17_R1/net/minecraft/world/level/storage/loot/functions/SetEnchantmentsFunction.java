package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class SetEnchantmentsFunction extends LootItemFunctionConditional {

    final Map<Enchantment, NumberProvider> enchantments;
    final boolean add;

    SetEnchantmentsFunction(LootItemCondition[] alootitemcondition, Map<Enchantment, NumberProvider> map, boolean flag) {
        super(alootitemcondition);
        this.enchantments = ImmutableMap.copyOf(map);
        this.add = flag;
    }

    @Override
    public LootItemFunctionType a() {
        return LootItemFunctions.SET_ENCHANTMENTS;
    }

    @Override
    public Set<LootContextParameter<?>> b() {
        return (Set) this.enchantments.values().stream().flatMap((numberprovider) -> {
            return numberprovider.b().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack a(ItemStack itemstack, LootTableInfo loottableinfo) {
        Object2IntMap<Enchantment> object2intmap = new Object2IntOpenHashMap();

        this.enchantments.forEach((enchantment, numberprovider) -> {
            object2intmap.put(enchantment, numberprovider.a(loottableinfo));
        });
        if (itemstack.getItem() == Items.BOOK) {
            ItemStack itemstack1 = new ItemStack(Items.ENCHANTED_BOOK);

            object2intmap.forEach((enchantment, integer) -> {
                ItemEnchantedBook.a(itemstack1, new WeightedRandomEnchant(enchantment, integer));
            });
            return itemstack1;
        } else {
            Map<Enchantment, Integer> map = EnchantmentManager.a(itemstack);

            if (this.add) {
                object2intmap.forEach((enchantment, integer) -> {
                    a(map, enchantment, Math.max((Integer) map.getOrDefault(enchantment, 0) + integer, 0));
                });
            } else {
                object2intmap.forEach((enchantment, integer) -> {
                    a(map, enchantment, Math.max(integer, 0));
                });
            }

            EnchantmentManager.a(map, itemstack);
            return itemstack;
        }
    }

    private static void a(Map<Enchantment, Integer> map, Enchantment enchantment, int i) {
        if (i == 0) {
            map.remove(enchantment);
        } else {
            map.put(enchantment, i);
        }

    }

    public static class b extends LootItemFunctionConditional.c<SetEnchantmentsFunction> {

        public b() {}

        public void a(JsonObject jsonobject, SetEnchantmentsFunction setenchantmentsfunction, JsonSerializationContext jsonserializationcontext) {
            super.a(jsonobject, (LootItemFunctionConditional) setenchantmentsfunction, jsonserializationcontext);
            JsonObject jsonobject1 = new JsonObject();

            setenchantmentsfunction.enchantments.forEach((enchantment, numberprovider) -> {
                MinecraftKey minecraftkey = IRegistry.ENCHANTMENT.getKey(enchantment);

                if (minecraftkey == null) {
                    throw new IllegalArgumentException("Don't know how to serialize enchantment " + enchantment);
                } else {
                    jsonobject1.add(minecraftkey.toString(), jsonserializationcontext.serialize(numberprovider));
                }
            });
            jsonobject.add("enchantments", jsonobject1);
            jsonobject.addProperty("add", setenchantmentsfunction.add);
        }

        @Override
        public SetEnchantmentsFunction b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext, LootItemCondition[] alootitemcondition) {
            Map<Enchantment, NumberProvider> map = Maps.newHashMap();

            if (jsonobject.has("enchantments")) {
                JsonObject jsonobject1 = ChatDeserializer.t(jsonobject, "enchantments");
                Iterator iterator = jsonobject1.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<String, JsonElement> entry = (Entry) iterator.next();
                    String s = (String) entry.getKey();
                    JsonElement jsonelement = (JsonElement) entry.getValue();
                    Enchantment enchantment = (Enchantment) IRegistry.ENCHANTMENT.getOptional(new MinecraftKey(s)).orElseThrow(() -> {
                        return new JsonSyntaxException("Unknown enchantment '" + s + "'");
                    });
                    NumberProvider numberprovider = (NumberProvider) jsondeserializationcontext.deserialize(jsonelement, NumberProvider.class);

                    map.put(enchantment, numberprovider);
                }
            }

            boolean flag = ChatDeserializer.a(jsonobject, "add", false);

            return new SetEnchantmentsFunction(alootitemcondition, map, flag);
        }
    }

    public static class a extends LootItemFunctionConditional.a<SetEnchantmentsFunction.a> {

        private final Map<Enchantment, NumberProvider> enchantments;
        private final boolean add;

        public a() {
            this(false);
        }

        public a(boolean flag) {
            this.enchantments = Maps.newHashMap();
            this.add = flag;
        }

        @Override
        protected SetEnchantmentsFunction.a d() {
            return this;
        }

        public SetEnchantmentsFunction.a a(Enchantment enchantment, NumberProvider numberprovider) {
            this.enchantments.put(enchantment, numberprovider);
            return this;
        }

        @Override
        public LootItemFunction b() {
            return new SetEnchantmentsFunction(this.g(), this.enchantments, this.add);
        }
    }
}
