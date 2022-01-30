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

public class CriterionTriggerBredAnimals implements CriterionTrigger<CriterionTriggerBredAnimals.b> {

    private static final MinecraftKey a = new MinecraftKey("bred_animals");
    private final Map<AdvancementDataPlayer, CriterionTriggerBredAnimals.a> b = Maps.newHashMap();

    public CriterionTriggerBredAnimals() {}

    public MinecraftKey a() {
        return CriterionTriggerBredAnimals.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerBredAnimals.b> criteriontrigger_a) {
        CriterionTriggerBredAnimals.a criteriontriggerbredanimals_a = (CriterionTriggerBredAnimals.a) this.b.get(advancementdataplayer);

        if (criteriontriggerbredanimals_a == null) {
            criteriontriggerbredanimals_a = new CriterionTriggerBredAnimals.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerbredanimals_a);
        }

        criteriontriggerbredanimals_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerBredAnimals.b> criteriontrigger_a) {
        CriterionTriggerBredAnimals.a criteriontriggerbredanimals_a = (CriterionTriggerBredAnimals.a) this.b.get(advancementdataplayer);

        if (criteriontriggerbredanimals_a != null) {
            criteriontriggerbredanimals_a.b(criteriontrigger_a);
            if (criteriontriggerbredanimals_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerBredAnimals.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionEntity criterionconditionentity = CriterionConditionEntity.a(jsonobject.get("parent"));
        CriterionConditionEntity criterionconditionentity1 = CriterionConditionEntity.a(jsonobject.get("partner"));
        CriterionConditionEntity criterionconditionentity2 = CriterionConditionEntity.a(jsonobject.get("child"));

        return new CriterionTriggerBredAnimals.b(criterionconditionentity, criterionconditionentity1, criterionconditionentity2);
    }

    public void a(EntityPlayer entityplayer, EntityAnimal entityanimal, EntityAnimal entityanimal1, EntityAgeable entityageable) {
        CriterionTriggerBredAnimals.a criteriontriggerbredanimals_a = (CriterionTriggerBredAnimals.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerbredanimals_a != null) {
            criteriontriggerbredanimals_a.a(entityplayer, entityanimal, entityanimal1, entityageable);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerBredAnimals.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerBredAnimals.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerBredAnimals.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(EntityPlayer entityplayer, EntityAnimal entityanimal, EntityAnimal entityanimal1, EntityAgeable entityageable) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerBredAnimals.b) criteriontrigger_a.a()).a(entityplayer, entityanimal, entityanimal1, entityageable)) {
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
        private final CriterionConditionEntity b;
        private final CriterionConditionEntity c;

        public b(CriterionConditionEntity criterionconditionentity, CriterionConditionEntity criterionconditionentity1, CriterionConditionEntity criterionconditionentity2) {
            super(CriterionTriggerBredAnimals.a);
            this.a = criterionconditionentity;
            this.b = criterionconditionentity1;
            this.c = criterionconditionentity2;
        }

        public boolean a(EntityPlayer entityplayer, EntityAnimal entityanimal, EntityAnimal entityanimal1, EntityAgeable entityageable) {
            return !this.c.a(entityplayer, entityageable) ? false : this.a.a(entityplayer, entityanimal) && this.b.a(entityplayer, entityanimal1) || this.a.a(entityplayer, entityanimal1) && this.b.a(entityplayer, entityanimal);
        }
    }
}
