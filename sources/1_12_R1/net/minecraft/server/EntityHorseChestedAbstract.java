package net.minecraft.server;

public abstract class EntityHorseChestedAbstract extends EntityHorseAbstract {

    private static final DataWatcherObject<Boolean> bH = DataWatcher.a(EntityHorseChestedAbstract.class, DataWatcherRegistry.h);

    public EntityHorseChestedAbstract(World world) {
        super(world);
        this.bF = false;
    }

    protected void i() {
        super.i();
        this.datawatcher.register(EntityHorseChestedAbstract.bH, Boolean.valueOf(false));
    }

    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeInstance(GenericAttributes.maxHealth).setValue((double) this.dM());
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.17499999701976776D);
        this.getAttributeInstance(EntityHorseChestedAbstract.attributeJumpStrength).setValue(0.5D);
    }

    public boolean isCarryingChest() {
        return ((Boolean) this.datawatcher.get(EntityHorseChestedAbstract.bH)).booleanValue();
    }

    public void setCarryingChest(boolean flag) {
        this.datawatcher.set(EntityHorseChestedAbstract.bH, Boolean.valueOf(flag));
    }

    protected int dn() {
        return this.isCarryingChest() ? 17 : super.dn();
    }

    public double aG() {
        return super.aG() - 0.25D;
    }

    protected SoundEffect do_() {
        super.do_();
        return SoundEffects.aD;
    }

    public void die(DamageSource damagesource) {
        // super.die(damagesource); // CraftBukkit - moved down
        if (this.isCarryingChest()) {
            if (!this.world.isClientSide) {
                this.a(Item.getItemOf(Blocks.CHEST), 1);
            }

            // this.setCarryingChest(false); // CraftBukkit - moved down
        }
        // CraftBukkit start
        super.die(damagesource);
        this.setCarryingChest(false);
        // CraftBukkit end

    }

    public static void b(DataConverterManager dataconvertermanager, Class<?> oclass) {
        EntityHorseAbstract.c(dataconvertermanager, oclass);
        dataconvertermanager.a(DataConverterTypes.ENTITY, (DataInspector) (new DataInspectorItemList(oclass, new String[] { "Items"})));
    }

    public void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        nbttagcompound.setBoolean("ChestedHorse", this.isCarryingChest());
        if (this.isCarryingChest()) {
            NBTTagList nbttaglist = new NBTTagList();

            for (int i = 2; i < this.inventoryChest.getSize(); ++i) {
                ItemStack itemstack = this.inventoryChest.getItem(i);

                if (!itemstack.isEmpty()) {
                    NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                    nbttagcompound1.setByte("Slot", (byte) i);
                    itemstack.save(nbttagcompound1);
                    nbttaglist.add(nbttagcompound1);
                }
            }

            nbttagcompound.set("Items", nbttaglist);
        }

    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        this.setCarryingChest(nbttagcompound.getBoolean("ChestedHorse"));
        if (this.isCarryingChest()) {
            NBTTagList nbttaglist = nbttagcompound.getList("Items", 10);

            this.loadChest();

            for (int i = 0; i < nbttaglist.size(); ++i) {
                NBTTagCompound nbttagcompound1 = nbttaglist.get(i);
                int j = nbttagcompound1.getByte("Slot") & 255;

                if (j >= 2 && j < this.inventoryChest.getSize()) {
                    this.inventoryChest.setItem(j, new ItemStack(nbttagcompound1));
                }
            }
        }

        this.dD();
    }

    public boolean c(int i, ItemStack itemstack) {
        if (i == 499) {
            if (this.isCarryingChest() && itemstack.isEmpty()) {
                this.setCarryingChest(false);
                this.loadChest();
                return true;
            }

            if (!this.isCarryingChest() && itemstack.getItem() == Item.getItemOf(Blocks.CHEST)) {
                this.setCarryingChest(true);
                this.loadChest();
                return true;
            }
        }

        return super.c(i, itemstack);
    }

    public boolean a(EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (itemstack.getItem() == Items.SPAWN_EGG) {
            return super.a(entityhuman, enumhand);
        } else {
            if (!this.isBaby()) {
                if (this.isTamed() && entityhuman.isSneaking()) {
                    this.c(entityhuman);
                    return true;
                }

                if (this.isVehicle()) {
                    return super.a(entityhuman, enumhand);
                }
            }

            if (!itemstack.isEmpty()) {
                boolean flag = this.b(entityhuman, itemstack);

                if (!flag && !this.isTamed()) {
                    if (itemstack.a(entityhuman, (EntityLiving) this, enumhand)) {
                        return true;
                    }

                    this.dK();
                    return true;
                }

                if (!flag && !this.isCarryingChest() && itemstack.getItem() == Item.getItemOf(Blocks.CHEST)) {
                    this.setCarryingChest(true);
                    this.dp();
                    flag = true;
                    this.loadChest();
                }

                if (!flag && !this.isBaby() && !this.dG() && itemstack.getItem() == Items.SADDLE) {
                    this.c(entityhuman);
                    return true;
                }

                if (flag) {
                    if (!entityhuman.abilities.canInstantlyBuild) {
                        itemstack.subtract(1);
                    }

                    return true;
                }
            }

            if (this.isBaby()) {
                return super.a(entityhuman, enumhand);
            } else if (itemstack.a(entityhuman, (EntityLiving) this, enumhand)) {
                return true;
            } else {
                this.g(entityhuman);
                return true;
            }
        }
    }

    protected void dp() {
        this.a(SoundEffects.aE, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    public int dt() {
        return 5;
    }
}
