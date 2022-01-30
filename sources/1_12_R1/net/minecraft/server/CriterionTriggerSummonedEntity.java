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

public class CriterionTriggerSummonedEntity implements CriterionTrigger<CriterionTriggerSummonedEntity.b> {

    private static final MinecraftKey a = new MinecraftKey("summoned_entity");
    private final Map<AdvancementDataPlayer, CriterionTriggerSummonedEntity.a> b = Maps.newHashMap();

    public CriterionTriggerSummonedEntity() {}

    public MinecraftKey a() {
        return CriterionTriggerSummonedEntity.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerSummonedEntity.b> criteriontrigger_a) {
        CriterionTriggerSummonedEntity.a criteriontriggersummonedentity_a = (CriterionTriggerSummonedEntity.a) this.b.get(advancementdataplayer);

        if (criteriontriggersummonedentity_a == null) {
            criteriontriggersummonedentity_a = new CriterionTriggerSummonedEntity.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggersummonedentity_a);
        }

        criteriontriggersummonedentity_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerSummonedEntity.b> criteriontrigger_a) {
        CriterionTriggerSummonedEntity.a criteriontriggersummonedentity_a = (CriterionTriggerSummonedEntity.a) this.b.get(advancementdataplayer);

        if (criteriontriggersummonedentity_a != null) {
            criteriontriggersummonedentity_a.b(criteriontrigger_a);
            if (criteriontriggersummonedentity_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerSummonedEntity.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionEntity criterionconditionentity = CriterionConditionEntity.a(jsonobject.get("entity"));

        return new CriterionTriggerSummonedEntity.b(criterionconditionentity);
    }

    public void a(EntityPlayer entityplayer, Entity entity) {
        CriterionTriggerSummonedEntity.a criteriontriggersummonedentity_a = (CriterionTriggerSummonedEntity.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggersummonedentity_a != null) {
            criteriontriggersummonedentity_a.a(entityplayer, entity);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerSummonedEntity.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerSummonedEntity.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerSummonedEntity.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(EntityPlayer entityplayer, Entity entity) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerSummonedEntity.b) criteriontrigger_a.a()).a(entityplayer, entity)) {
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

        private final CriterionConditionEntity a;

        public b(CriterionConditionEntity criterionconditionentity) {
            super(CriterionTriggerSummonedEntity.a);
            this.a = criterionconditionentity;
        }

        public boolean a(EntityPlayer entityplayer, Entity entity) {
            return this.a.a(entityplayer, entity);
        }
    }
}
