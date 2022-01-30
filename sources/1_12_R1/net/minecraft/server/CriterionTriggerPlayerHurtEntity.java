package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CriterionTriggerPlayerHurtEntity implements CriterionTrigger<CriterionTriggerPlayerHurtEntity.b> {

    private static final MinecraftKey a = new MinecraftKey("player_hurt_entity");
    private final Map<AdvancementDataPlayer, CriterionTriggerPlayerHurtEntity.a> b = Maps.newHashMap();

    public CriterionTriggerPlayerHurtEntity() {}

    public MinecraftKey a() {
        return CriterionTriggerPlayerHurtEntity.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerPlayerHurtEntity.b> criteriontrigger_a) {
        CriterionTriggerPlayerHurtEntity.a criteriontriggerplayerhurtentity_a = (CriterionTriggerPlayerHurtEntity.a) this.b.get(advancementdataplayer);

        if (criteriontriggerplayerhurtentity_a == null) {
            criteriontriggerplayerhurtentity_a = new CriterionTriggerPlayerHurtEntity.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerplayerhurtentity_a);
        }

        criteriontriggerplayerhurtentity_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerPlayerHurtEntity.b> criteriontrigger_a) {
        CriterionTriggerPlayerHurtEntity.a criteriontriggerplayerhurtentity_a = (CriterionTriggerPlayerHurtEntity.a) this.b.get(advancementdataplayer);

        if (criteriontriggerplayerhurtentity_a != null) {
            criteriontriggerplayerhurtentity_a.b(criteriontrigger_a);
            if (criteriontriggerplayerhurtentity_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerPlayerHurtEntity.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionDamage criterionconditiondamage = CriterionConditionDamage.a(jsonobject.get("damage"));
        CriterionConditionEntity criterionconditionentity = CriterionConditionEntity.a(jsonobject.get("entity"));

        return new CriterionTriggerPlayerHurtEntity.b(criterionconditiondamage, criterionconditionentity);
    }

    public void a(EntityPlayer entityplayer, Entity entity, DamageSource damagesource, float f, float f1, boolean flag) {
        CriterionTriggerPlayerHurtEntity.a criteriontriggerplayerhurtentity_a = (CriterionTriggerPlayerHurtEntity.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerplayerhurtentity_a != null) {
            criteriontriggerplayerhurtentity_a.a(entityplayer, entity, damagesource, f, f1, flag);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerPlayerHurtEntity.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerPlayerHurtEntity.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerPlayerHurtEntity.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(EntityPlayer entityplayer, Entity entity, DamageSource damagesource, float f, float f1, boolean flag) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerPlayerHurtEntity.b) criteriontrigger_a.a()).a(entityplayer, entity, damagesource, f, f1, flag)) {
                    if (arraylist == null) {
                        arraylist = Lists.newArrayList();
                    }

                    arraylist.add(criteriontrigger_a);
                }
            }

            if (arraylist != null) {
                iterator = arraylist.iterator();

                while (iterator.hasNext()) {
                    criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                    criteriontrigger_a.a(this.a);
                }
            }

        }
    }

    public static class b extends CriterionInstanceAbstract {

        private final CriterionConditionDamage a;
        private final CriterionConditionEntity b;

        public b(CriterionConditionDamage criterionconditiondamage, CriterionConditionEntity criterionconditionentity) {
            super(CriterionTriggerPlayerHurtEntity.a);
            this.a = criterionconditiondamage;
            this.b = criterionconditionentity;
        }

        public boolean a(EntityPlayer entityplayer, Entity entity, DamageSource damagesource, float f, float f1, boolean flag) {
            return !this.a.a(entityplayer, damagesource, f, f1, flag) ? false : this.b.a(entityplayer, entity);
        }
    }
}
