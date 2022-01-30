package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentPiercing extends Enchantment {

    public EnchantmentPiercing(Enchantment.Rarity enchantment_rarity, EnumItemSlot... aenumitemslot) {
        super(enchantment_rarity, EnchantmentSlotType.CROSSBOW, aenumitemslot);
    }

    @Override
    public int a(int i) {
        return 1 + (i - 1) * 10;
    }

    @Override
    public int b(int i) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public boolean a(Enchantment enchantment) {
        return super.a(enchantment) && enchantment != Enchantments.MULTISHOT;
    }
}
