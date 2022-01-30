package net.minecraft.server;

public class EnchantmentTridentLoyalty extends Enchantment {

    public EnchantmentTridentLoyalty(Enchantment.Rarity enchantment_rarity, EnumItemSlot... aenumitemslot) {
        super(enchantment_rarity, EnchantmentSlotType.TRIDENT, aenumitemslot);
    }

    public int a(int i) {
        return 5 + i * 7;
    }

    public int b(int i) {
        return 50;
    }

    public int getMaxLevel() {
        return 3;
    }

    public boolean a(Enchantment enchantment) {
        return super.a(enchantment);
    }
}
