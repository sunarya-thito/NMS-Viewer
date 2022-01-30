package net.minecraft.server;

public class ItemCarrotStick extends Item {

    public ItemCarrotStick() {
        this.b(CreativeModeTab.e);
        this.d(1);
        this.setMaxDurability(25);
    }

    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (world.isClientSide) {
            return new InteractionResultWrapper(EnumInteractionResult.PASS, itemstack);
        } else {
            if (entityhuman.isPassenger() && entityhuman.bJ() instanceof EntityPig) {
                EntityPig entitypig = (EntityPig) entityhuman.bJ();

                if (itemstack.k() - itemstack.getData() >= 7 && entitypig.dm()) {
                    itemstack.damage(7, entityhuman);
                    if (itemstack.isEmpty()) {
                        ItemStack itemstack1 = new ItemStack(Items.FISHING_ROD);

                        itemstack1.setTag(itemstack.getTag());
                        return new InteractionResultWrapper(EnumInteractionResult.SUCCESS, itemstack1);
                    }

                    return new InteractionResultWrapper(EnumInteractionResult.SUCCESS, itemstack);
                }
            }

            entityhuman.b(StatisticList.b((Item) this));
            return new InteractionResultWrapper(EnumInteractionResult.PASS, itemstack);
        }
    }
}
