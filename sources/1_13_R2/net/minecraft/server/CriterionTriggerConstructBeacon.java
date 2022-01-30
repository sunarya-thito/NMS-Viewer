package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CriterionTriggerConstructBeacon implements CriterionTrigger<CriterionTriggerConstructBeacon.b> {

    private static final MinecraftKey a = new MinecraftKey("construct_beacon");
    private final Map<AdvancementDataPlayer, CriterionTriggerConstructBeacon.a> b = Maps.newHashMap();

    public CriterionTriggerConstructBeacon() {}

    public MinecraftKey a() {
        return CriterionTriggerConstructBeacon.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerConstructBeacon.b> criteriontrigger_a) {
        CriterionTriggerConstructBeacon.a criteriontriggerconstructbeacon_a = (CriterionTriggerConstructBeacon.a) this.b.get(advancementdataplayer);

        if (criteriontriggerconstructbeacon_a == null) {
            criteriontriggerconstructbeacon_a = new CriterionTriggerConstructBeacon.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerconstructbeacon_a);
        }

        criteriontriggerconstructbeacon_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerConstructBeacon.b> criteriontrigger_a) {
        CriterionTriggerConstructBeacon.a criteriontriggerconstructbeacon_a = (CriterionTriggerConstructBeacon.a) this.b.get(advancementdataplayer);

        if (criteriontriggerconstructbeacon_a != null) {
            criteriontriggerconstructbeacon_a.b(criteriontrigger_a);
            if (criteriontriggerconstructbeacon_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerConstructBeacon.b a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionValue.IntegerRange criterionconditionvalue_integerrange = CriterionConditionValue.IntegerRange.a(jsonobject.get("level"));

        return new CriterionTriggerConstructBeacon.b(criterionconditionvalue_integerrange);
    }

    public void a(EntityPlayer entityplayer, TileEntityBeacon tileentitybeacon) {
        CriterionTriggerConstructBeacon.a criteriontriggerconstructbeacon_a = (CriterionTriggerConstructBeacon.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerconstructbeacon_a != null) {
            criteriontriggerconstructbeacon_a.a(tileentitybeacon);
        }

    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerConstructBeacon.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerConstructBeacon.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerConstructBeacon.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(TileEntityBeacon tileentitybeacon) {
            List<CriterionTrigger.a<CriterionTriggerConstructBeacon.b>> list = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerConstructBeacon.b) criteriontrigger_a.a()).a(tileentitybeacon)) {
                    if (list == null) {
                        list = Lists.newArrayList();
                    }

                    list.add(criteriontrigger_a);
                }
            }

            if (list != null) {
                iterator = list.iterator();

                while (iterator.hasNext()) {
                    criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                    criteriontrigger_a.a(this.a);
                }
            }

        }
    }

    public static class b extends CriterionInstanceAbstract {

        private final CriterionConditionValue.IntegerRange a;

        public b(CriterionConditionValue.IntegerRange criterionconditionvalue_integerrange) {
            super(CriterionTriggerConstructBeacon.a);
            this.a = criterionconditionvalue_integerrange;
        }

        public static CriterionTriggerConstructBeacon.b a(CriterionConditionValue.IntegerRange criterionconditionvalue_integerrange) {
            return new CriterionTriggerConstructBeacon.b(criterionconditionvalue_integerrange);
        }

        public boolean a(TileEntityBeacon tileentitybeacon) {
            return this.a.d(tileentitybeacon.s());
        }

        public JsonElement b() {
            JsonObject jsonobject = new JsonObject();

            jsonobject.add("level", this.a.d());
            return jsonobject;
        }
    }
}
