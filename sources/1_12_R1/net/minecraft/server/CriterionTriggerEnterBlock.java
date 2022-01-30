package net.minecraft.server;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

public class CriterionTriggerEnterBlock implements CriterionTrigger<CriterionTriggerEnterBlock.b> {

    private static final MinecraftKey a = new MinecraftKey("enter_block");
    private final Map<AdvancementDataPlayer, CriterionTriggerEnterBlock.a> b = Maps.newHashMap();

    public CriterionTriggerEnterBlock() {}

    public MinecraftKey a() {
        return CriterionTriggerEnterBlock.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerEnterBlock.b> criteriontrigger_a) {
        CriterionTriggerEnterBlock.a criteriontriggerenterblock_a = (CriterionTriggerEnterBlock.a) this.b.get(advancementdataplayer);

        if (criteriontriggerenterblock_a == null) {
            criteriontriggerenterblock_a = new CriterionTriggerEnterBlock.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerenterblock_a);
        }

        criteriontriggerenterblock_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerEnterBlock.b> criteriontrigger_a) {
        CriterionTriggerEnterBlock.a criteriontriggerenterblock_a = (CriterionTriggerEnterBlock.a) this.b.get(advancementdataplayer);

        if (criteriontriggerenterblock_a != null) {
            criteriontriggerenterblock_a.b(criteriontrigger_a);
            if (criteriontriggerenterblock_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerEnterBlock.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        Block block = null;

        if (jsonobject.has("block")) {
            MinecraftKey minecraftkey = new MinecraftKey(ChatDeserializer.h(jsonobject, "block"));

            if (!Block.REGISTRY.d(minecraftkey)) {
                throw new JsonSyntaxException("Unknown block type \'" + minecraftkey + "\'");
            }

            block = (Block) Block.REGISTRY.get(minecraftkey);
        }

        HashMap hashmap = null;

        if (jsonobject.has("state")) {
            if (block == null) {
                throw new JsonSyntaxException("Can\'t define block state without a specific block type");
            }

            BlockStateList blockstatelist = block.s();

            IBlockState iblockstate;
            Optional optional;

            for (Iterator iterator = ChatDeserializer.t(jsonobject, "state").entrySet().iterator(); iterator.hasNext(); hashmap.put(iblockstate, optional.get())) {
                Entry entry = (Entry) iterator.next();

                iblockstate = blockstatelist.a((String) entry.getKey());
                if (iblockstate == null) {
                    throw new JsonSyntaxException("Unknown block state property \'" + (String) entry.getKey() + "\' for block \'" + Block.REGISTRY.b(block) + "\'");
                }

                String s = ChatDeserializer.a((JsonElement) entry.getValue(), (String) entry.getKey());

                optional = iblockstate.b(s);
                if (!optional.isPresent()) {
                    throw new JsonSyntaxException("Invalid block state value \'" + s + "\' for property \'" + (String) entry.getKey() + "\' on block \'" + Block.REGISTRY.b(block) + "\'");
                }

                if (hashmap == null) {
                    hashmap = Maps.newHashMap();
                }
            }
        }

        return new CriterionTriggerEnterBlock.b(block, hashmap);
    }

    public void a(EntityPlayer entityplayer, IBlockData iblockdata) {
        CriterionTriggerEnterBlock.a criteriontriggerenterblock_a = (CriterionTriggerEnterBlock.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerenterblock_a != null) {
            criteriontriggerenterblock_a.a(iblockdata);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerEnterBlock.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerEnterBlock.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerEnterBlock.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(IBlockData iblockdata) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerEnterBlock.b) criteriontrigger_a.a()).a(iblockdata)) {
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

        private final Block a;
        private final Map<IBlockState<?>, Object> b;

        public b(@Nullable Block block, @Nullable Map<IBlockState<?>, Object> map) {
            super(CriterionTriggerEnterBlock.a);
            this.a = block;
            this.b = map;
        }

        public boolean a(IBlockData iblockdata) {
            if (this.a != null && iblockdata.getBlock() != this.a) {
                return false;
            } else {
                if (this.b != null) {
                    Iterator iterator = this.b.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Entry entry = (Entry) iterator.next();

                        if (iblockdata.get((IBlockState) entry.getKey()) != entry.getValue()) {
                            return false;
                        }
                    }
                }

                return true;
            }
        }
    }
}
