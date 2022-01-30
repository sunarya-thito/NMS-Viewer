package net.minecraft.world.entity.monster;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalCrossbowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemBanner;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityPillager extends EntityIllagerAbstract implements ICrossbow, InventoryCarrier {

    private static final DataWatcherObject<Boolean> IS_CHARGING_CROSSBOW = DataWatcher.a(EntityPillager.class, DataWatcherRegistry.BOOLEAN);
    private static final int INVENTORY_SIZE = 5;
    private static final int SLOT_OFFSET = 300;
    private static final float CROSSBOW_POWER = 1.6F;
    public final InventorySubcontainer inventory = new InventorySubcontainer(5);

    public EntityPillager(EntityTypes<? extends EntityPillager> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new EntityRaider.a(this, 10.0F));
        this.goalSelector.a(3, new PathfinderGoalCrossbowAttack<>(this, 1.0D, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.a(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 15.0F, 1.0F));
        this.goalSelector.a(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 15.0F));
        this.targetSelector.a(1, (new PathfinderGoalHurtByTarget(this, new Class[]{EntityRaider.class})).a(new Class[0])); // CraftBukkit - decompile error
        this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false));
        this.targetSelector.a(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    public static AttributeProvider.Builder p() {
        return EntityMonster.fB().a(GenericAttributes.MOVEMENT_SPEED, 0.3499999940395355D).a(GenericAttributes.MAX_HEALTH, 24.0D).a(GenericAttributes.ATTACK_DAMAGE, 5.0D).a(GenericAttributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(EntityPillager.IS_CHARGING_CROSSBOW, false);
    }

    @Override
    public boolean a(ItemProjectileWeapon itemprojectileweapon) {
        return itemprojectileweapon == Items.CROSSBOW;
    }

    public boolean fw() {
        return (Boolean) this.entityData.get(EntityPillager.IS_CHARGING_CROSSBOW);
    }

    @Override
    public void b(boolean flag) {
        this.entityData.set(EntityPillager.IS_CHARGING_CROSSBOW, flag);
    }

    @Override
    public void a() {
        this.noActionTime = 0;
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.inventory.getSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);

            if (!itemstack.isEmpty()) {
                nbttaglist.add(itemstack.save(new NBTTagCompound()));
            }
        }

        nbttagcompound.set("Inventory", nbttaglist);
    }

    @Override
    public EntityIllagerAbstract.a n() {
        return this.fw() ? EntityIllagerAbstract.a.CROSSBOW_CHARGE : (this.a(Items.CROSSBOW) ? EntityIllagerAbstract.a.CROSSBOW_HOLD : (this.isAggressive() ? EntityIllagerAbstract.a.ATTACKING : EntityIllagerAbstract.a.NEUTRAL));
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        NBTTagList nbttaglist = nbttagcompound.getList("Inventory", 10);

        for (int i = 0; i < nbttaglist.size(); ++i) {
            ItemStack itemstack = ItemStack.a(nbttaglist.getCompound(i));

            if (!itemstack.isEmpty()) {
                this.inventory.a(itemstack);
            }
        }

        this.setCanPickupLoot(true);
    }

    @Override
    public float a(BlockPosition blockposition, IWorldReader iworldreader) {
        IBlockData iblockdata = iworldreader.getType(blockposition.down());

        return !iblockdata.a(Blocks.GRASS_BLOCK) && !iblockdata.a(Blocks.SAND) ? 0.5F - iworldreader.z(blockposition) : 10.0F;
    }

    @Override
    public int getMaxSpawnGroup() {
        return 1;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess worldaccess, DifficultyDamageScaler difficultydamagescaler, EnumMobSpawn enummobspawn, @Nullable GroupDataEntity groupdataentity, @Nullable NBTTagCompound nbttagcompound) {
        this.a(difficultydamagescaler);
        this.b(difficultydamagescaler);
        return super.prepare(worldaccess, difficultydamagescaler, enummobspawn, groupdataentity, nbttagcompound);
    }

    @Override
    protected void a(DifficultyDamageScaler difficultydamagescaler) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
    }

    @Override
    protected void x(float f) {
        super.x(f);
        if (this.random.nextInt(300) == 0) {
            ItemStack itemstack = this.getItemInMainHand();

            if (itemstack.a(Items.CROSSBOW)) {
                Map<Enchantment, Integer> map = EnchantmentManager.a(itemstack);

                map.putIfAbsent(Enchantments.PIERCING, 1);
                EnchantmentManager.a(map, itemstack);
                this.setSlot(EnumItemSlot.MAINHAND, itemstack);
            }
        }

    }

    @Override
    public boolean p(Entity entity) {
        return super.p(entity) ? true : (entity instanceof EntityLiving && ((EntityLiving) entity).getMonsterType() == EnumMonsterType.ILLAGER ? this.getScoreboardTeam() == null && entity.getScoreboardTeam() == null : false);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundDeath() {
        return SoundEffects.PILLAGER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource damagesource) {
        return SoundEffects.PILLAGER_HURT;
    }

    @Override
    public void a(EntityLiving entityliving, float f) {
        this.b(this, 1.6F);
    }

    @Override
    public void a(EntityLiving entityliving, ItemStack itemstack, IProjectile iprojectile, float f) {
        this.a(this, entityliving, iprojectile, f, 1.6F);
    }

    @Override
    public IInventory getInventory() {
        return this.inventory;
    }

    @Override
    protected void b(EntityItem entityitem) {
        ItemStack itemstack = entityitem.getItemStack();

        if (itemstack.getItem() instanceof ItemBanner) {
            super.b(entityitem);
        } else if (this.m(itemstack)) {
            this.a(entityitem);
            ItemStack itemstack1 = this.inventory.a(itemstack);

            if (itemstack1.isEmpty()) {
                entityitem.die();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    private boolean m(ItemStack itemstack) {
        return this.fL() && itemstack.a(Items.WHITE_BANNER);
    }

    @Override
    public SlotAccess k(int i) {
        int j = i - 300;

        return j >= 0 && j < this.inventory.getSize() ? SlotAccess.a(this.inventory, j) : super.k(i);
    }

    @Override
    public void a(int i, boolean flag) {
        Raid raid = this.fK();
        boolean flag1 = this.random.nextFloat() <= raid.w();

        if (flag1) {
            ItemStack itemstack = new ItemStack(Items.CROSSBOW);
            Map<Enchantment, Integer> map = Maps.newHashMap();

            if (i > raid.a(EnumDifficulty.NORMAL)) {
                map.put(Enchantments.QUICK_CHARGE, 2);
            } else if (i > raid.a(EnumDifficulty.EASY)) {
                map.put(Enchantments.QUICK_CHARGE, 1);
            }

            map.put(Enchantments.MULTISHOT, 1);
            EnchantmentManager.a((Map) map, itemstack);
            this.setSlot(EnumItemSlot.MAINHAND, itemstack);
        }

    }

    @Override
    public SoundEffect t() {
        return SoundEffects.PILLAGER_CELEBRATE;
    }
}
