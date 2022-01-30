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

public class CriterionTriggerEntityHurtPlayer implements CriterionTrigger<CriterionTriggerEntityHurtPlayer.b> {

    private static final MinecraftKey a = new MinecraftKey("entity_hurt_player");
    private final Map<AdvancementDataPlayer, CriterionTriggerEntityHurtPlayer.a> b = Maps.newHashMap();

    public CriterionTriggerEntityHurtPlayer() {}

    public MinecraftKey a() {
        return CriterionTriggerEntityHurtPlayer.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerEntityHurtPlayer.b> criteriontrigger_a) {
        CriterionTriggerEntityHurtPlayer.a criteriontriggerentityhurtplayer_a = (CriterionTriggerEntityHurtPlayer.a) this.b.get(advancementdataplayer);

        if (criteriontriggerentityhurtplayer_a == null) {
            criteriontriggerentityhurtplayer_a = new CriterionTriggerEntityHurtPlayer.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerentityhurtplayer_a);
        }

        criteriontriggerentityhurtplayer_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerEntityHurtPlayer.b> criteriontrigger_a) {
        CriterionTriggerEntityHurtPlayer.a criteriontriggerentityhurtplayer_a = (CriterionTriggerEntityHurtPlayer.a) this.b.get(advancementdataplayer);

        if (criteriontriggerentityhurtplayer_a != null) {
            criteriontriggerentityhurtplayer_a.b(criteriontrigger_a);
            if (criteriontriggerentityhurtplayer_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerEntityHurtPlayer.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        CriterionConditionDamage criterionconditiondamage = CriterionConditionDamage.a(jsonobject.get("damage"));

        return new CriterionTriggerEntityHurtPlayer.b(criterionconditiondamage);
    }

    public void a(EntityPlayer entityplayer, DamageSource damagesource, float f, float f1, boolean flag) {
        CriterionTriggerEntityHurtPlayer.a criteriontriggerentityhurtplayer_a = (CriterionTriggerEntityHurtPlayer.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerentityhurtplayer_a != null) {
            criteriontriggerentityhurtplayer_a.a(entityplayer, damagesource, f, f1, flag);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerEntityHurtPlayer.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerEntityHurtPlayer.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerEntityHurtPlayer.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(EntityPlayer entityplayer, DamageSource damagesource, float f, float f1, boolean flag) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerEntityHurtPlayer.b) criteriontrigger_a.a()).a(entityplayer, damagesource, f, f1, flag)) {
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

        public b(CriterionConditionDamage criterionconditiondamage) {
            super(CriterionTriggerEntityHurtPlayer.a);
            this.a = criterionconditiondamage;
        }

        public boolean a(EntityPlayer entityplayer, DamageSource damagesource, float f, float f1, boolean flag) {
            return this.a.a(entityplayer, damagesource, f, f1, flag);
        }
    }
}
