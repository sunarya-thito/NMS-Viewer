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

public class CriterionTriggerUsedTotem implements CriterionTrigger<CriterionTriggerUsedTotem.b> {

    private static final MinecraftKey a = new MinecraftKey("used_totem");
    private final Map<AdvancementDataPlayer, CriterionTriggerUsedTotem.a> b = Maps.newHashMap();

    public CriterionTriggerUsedTotem() {}

    public MinecraftKey a() {
        return CriterionTriggerUsedTotem.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerUsedTotem.b> criteriontrigger_a) {
        CriterionTriggerUsedTotem.a criteriontriggerusedtotem_a = (CriterionTriggerUsedTotem.a) this.b.get(advancementdataplayer);

        if (criteriontriggerusedtotem_a == null) {
            criteriontriggerusedtotem_a = new CriterionTriggerUsedTotem.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerusedtotem_a);
        }

        criteriontriggerusedtotem_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerUsedTotem.b> criteriontrigger_a) {
        CriterionTriggerUsedTotem.a criteriontriggerusedtotem_a = (CriterionTriggerUsedTotem.a) this.b.get(advancementdataplayer);

        if (criteriontriggerusedtotem_a != null) {
            criteriontriggerusedtotem_a.b(criteriontrigger_a);
            if (criteriontriggerusedtotem_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerUsedTotem.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionItem criterionconditionitem = CriterionConditionItem.a(jsonobject.get("item"));

        return new CriterionTriggerUsedTotem.b(criterionconditionitem);
    }

    public void a(EntityPlayer entityplayer, ItemStack itemstack) {
        CriterionTriggerUsedTotem.a criteriontriggerusedtotem_a = (CriterionTriggerUsedTotem.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerusedtotem_a != null) {
            criteriontriggerusedtotem_a.a(itemstack);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerUsedTotem.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerUsedTotem.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerUsedTotem.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(ItemStack itemstack) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerUsedTotem.b) criteriontrigger_a.a()).a(itemstack)) {
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

        private final CriterionConditionItem a;

        public b(CriterionConditionItem criterionconditionitem) {
            super(CriterionTriggerUsedTotem.a);
            this.a = criterionconditionitem;
        }

        public boolean a(ItemStack itemstack) {
            return this.a.a(itemstack);
        }
    }
}
