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

public class CriterionTriggerPlacedBlock implements CriterionTrigger<CriterionTriggerPlacedBlock.b> {

    private static final MinecraftKey a = new MinecraftKey("placed_block");
    private final Map<AdvancementDataPlayer, CriterionTriggerPlacedBlock.a> b = Maps.newHashMap();

    public CriterionTriggerPlacedBlock() {}

    public MinecraftKey a() {
        return CriterionTriggerPlacedBlock.a;
    }

    public void a(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerPlacedBlock.b> criteriontrigger_a) {
        CriterionTriggerPlacedBlock.a criteriontriggerplacedblock_a = (CriterionTriggerPlacedBlock.a) this.b.get(advancementdataplayer);

        if (criteriontriggerplacedblock_a == null) {
            criteriontriggerplacedblock_a = new CriterionTriggerPlacedBlock.a(advancementdataplayer);
            this.b.put(advancementdataplayer, criteriontriggerplacedblock_a);
        }

        criteriontriggerplacedblock_a.a(criteriontrigger_a);
    }

    public void b(AdvancementDataPlayer advancementdataplayer, CriterionTrigger.a<CriterionTriggerPlacedBlock.b> criteriontrigger_a) {
        CriterionTriggerPlacedBlock.a criteriontriggerplacedblock_a = (CriterionTriggerPlacedBlock.a) this.b.get(advancementdataplayer);

        if (criteriontriggerplacedblock_a != null) {
            criteriontriggerplacedblock_a.b(criteriontrigger_a);
            if (criteriontriggerplacedblock_a.a()) {
                this.b.remove(advancementdataplayer);
            }
        }

    }

    public void a(AdvancementDataPlayer advancementdataplayer) {
        this.b.remove(advancementdataplayer);
    }

    public CriterionTriggerPlacedBlock.b b(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
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

        CriterionConditionLocation criterionconditionlocation = CriterionConditionLocation.a(jsonobject.get("location"));
        CriterionConditionItem criterionconditionitem = CriterionConditionItem.a(jsonobject.get("item"));

        return new CriterionTriggerPlacedBlock.b(block, hashmap, criterionconditionlocation, criterionconditionitem);
    }

    public void a(EntityPlayer entityplayer, BlockPosition blockposition, ItemStack itemstack) {
        IBlockData iblockdata = entityplayer.world.getType(blockposition);
        CriterionTriggerPlacedBlock.a criteriontriggerplacedblock_a = (CriterionTriggerPlacedBlock.a) this.b.get(entityplayer.getAdvancementData());

        if (criteriontriggerplacedblock_a != null) {
            criteriontriggerplacedblock_a.a(iblockdata, blockposition, entityplayer.x(), itemstack);
        }

    }

    public CriterionInstance a(JsonObject jsonobject, JsonDeserializationContext jsondeserializationcontext) {
        return this.b(jsonobject, jsondeserializationcontext);
    }

    static class a {

        private final AdvancementDataPlayer a;
        private final Set<CriterionTrigger.a<CriterionTriggerPlacedBlock.b>> b = Sets.newHashSet();

        public a(AdvancementDataPlayer advancementdataplayer) {
            this.a = advancementdataplayer;
        }

        public boolean a() {
            return this.b.isEmpty();
        }

        public void a(CriterionTrigger.a<CriterionTriggerPlacedBlock.b> criteriontrigger_a) {
            this.b.add(criteriontrigger_a);
        }

        public void b(CriterionTrigger.a<CriterionTriggerPlacedBlock.b> criteriontrigger_a) {
            this.b.remove(criteriontrigger_a);
        }

        public void a(IBlockData iblockdata, BlockPosition blockposition, WorldServer worldserver, ItemStack itemstack) {
            ArrayList arraylist = null;
            Iterator iterator = this.b.iterator();

            CriterionTrigger.a criteriontrigger_a;

            while (iterator.hasNext()) {
                criteriontrigger_a = (CriterionTrigger.a) iterator.next();
                if (((CriterionTriggerPlacedBlock.b) criteriontrigger_a.a()).a(iblockdata, blockposition, worldserver, itemstack)) {
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
        private final CriterionConditionLocation c;
        private final CriterionConditionItem d;

        public b(@Nullable Block block, @Nullable Map<IBlockState<?>, Object> map, CriterionConditionLocation criterionconditionlocation, CriterionConditionItem criterionconditionitem) {
            super(CriterionTriggerPlacedBlock.a);
            this.a = block;
            this.b = map;
            this.c = criterionconditionlocation;
            this.d = criterionconditionitem;
        }

        public boolean a(IBlockData iblockdata, BlockPosition blockposition, WorldServer worldserver, ItemStack itemstack) {
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

                return !this.c.a(worldserver, (float) blockposition.getX(), (float) blockposition.getY(), (float) blockposition.getZ()) ? false : this.d.a(itemstack);
            }
        }
    }
}
