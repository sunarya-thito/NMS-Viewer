package net.minecraft.server;

public class EntityMinecartRideable extends EntityMinecartAbstract {

    public EntityMinecartRideable(World world) {
        super(world);
    }

    public EntityMinecartRideable(World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
    }

    public static void a(DataConverterManager dataconvertermanager) {
        EntityMinecartAbstract.a(dataconvertermanager, EntityMinecartRideable.class);
    }

    public boolean b(EntityHuman entityhuman, EnumHand enumhand) {
        if (entityhuman.isSneaking()) {
            return false;
        } else if (this.isVehicle()) {
            return true;
        } else {
            if (!this.world.isClientSide) {
                entityhuman.startRiding(this);
            }

            return true;
        }
    }

    public void a(int i, int j, int k, boolean flag) {
        if (flag) {
            if (this.isVehicle()) {
                this.az();
            }

            if (this.getType() == 0) {
                this.e(-this.u());
                this.d(10);
                this.setDamage(50.0F);
                this.ap();
            }
        }

    }

    public EntityMinecartAbstract.EnumMinecartType v() {
        return EntityMinecartAbstract.EnumMinecartType.RIDEABLE;
    }
}
