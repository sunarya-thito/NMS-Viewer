package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class CauldronRenameFix extends DataFix {

    public CauldronRenameFix(Schema schema, boolean flag) {
        super(schema, flag);
    }

    private static Dynamic<?> fix(Dynamic<?> dynamic) {
        Optional<String> optional = dynamic.get("Name").asString().result();

        if (optional.equals(Optional.of("minecraft:cauldron"))) {
            Dynamic<?> dynamic1 = dynamic.get("Properties").orElseEmptyMap();

            return dynamic1.get("level").asString("0").equals("0") ? dynamic.remove("Properties") : dynamic.set("Name", dynamic.createString("minecraft:water_cauldron"));
        } else {
            return dynamic;
        }
    }

    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("cauldron_rename_fix", this.getInputSchema().getType(DataConverterTypes.BLOCK_STATE), (typed) -> {
            return typed.update(DSL.remainderFinder(), CauldronRenameFix::fix);
        });
    }
}
