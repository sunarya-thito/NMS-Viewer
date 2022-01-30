package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;

public class CriterionTriggerImpossible implements CriterionTrigger<CriterionTriggerImpossible.a> {

    static final MinecraftKey ID = new MinecraftKey("impossible");

    public CriterionTriggerImpossible() {}

    @Override
    public MinecraftKey a() {
        return CriterionTriggerImpossible.ID;
    }

    @Override
    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerImpossible.a> criteriontrigger_a) {}

    @Override
    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerImpossible.a> criteriontrigger_a) {}

    @Override
    public void a(AdvancementDataPlayer advancementdataplayer) {}

    @Override
    public CriterionTriggerImpossible.a a(JsonObject jsonobject, LootDeserializationContext lootdeserializationcontext) {
        return new CriterionTriggerImpossible.a();
    }

    public static class a implements CriterionInstance {

        public a() {}

        @Override
        public MinecraftKey a() {
            return CriterionTriggerImpossible.ID;
        }

        @Override
        public JsonObject a(LootSerializationContext lootserializationcontext) {
            return new JsonObject();
        }
    }
}
