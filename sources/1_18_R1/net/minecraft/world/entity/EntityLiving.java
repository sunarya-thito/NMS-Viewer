package net.minecraft.world.entity;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutCollect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.damagesource.CombatMath;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeDefaults;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityBird;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.food.FoodInfo;
import net.minecraft.world.item.EnumAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemElytra;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.enchantment.EnchantmentFrostWalker;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBed;
import net.minecraft.world.level.block.BlockHoney;
import net.minecraft.world.level.block.BlockLadder;
import net.minecraft.world.level.block.BlockSkullAbstract;
import net.minecraft.world.level.block.BlockTrapdoor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardTeam;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Function;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.entity.animal.EntityAnimal;
import org.bukkit.Location;
import org.bukkit.craftbukkit.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ArrowBodyCountChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
// CraftBukkit end

import org.bukkit.craftbukkit.SpigotTimings; // Spigot

public abstract class EntityLiving extends Entity {

    private static final UUID SPEED_MODIFIER_SPRINTING_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
    private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(EntityLiving.SPEED_MODIFIER_SPRINTING_UUID, "Sprinting speed boost", 0.30000001192092896D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static final int HAND_SLOTS = 2;
    public static final int ARMOR_SLOTS = 4;
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int SWING_DURATION = 6;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003D;
    public static final double DEFAULT_BASE_GRAVITY = 0.08D;
    public static final int DEATH_DURATION = 20;
    private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final int USE_ITEM_INTERVAL = 4;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0D;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final DataWatcherObject<Byte> DATA_LIVING_ENTITY_FLAGS = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.BYTE);
    public static final DataWatcherObject<Float> DATA_HEALTH_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_EFFECT_COLOR_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_EFFECT_AMBIENCE_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.BOOLEAN);
    public static final DataWatcherObject<Integer> DATA_ARROW_COUNT_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_STINGER_COUNT_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Optional<BlockPosition>> SLEEPING_POS_ID = DataWatcher.defineId(EntityLiving.class, DataWatcherRegistry.OPTIONAL_BLOCK_POS);
    protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
    protected static final EntitySize SLEEPING_DIMENSIONS = EntitySize.fixed(0.2F, 0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    private final AttributeMapBase attributes;
    public CombatTracker combatTracker = new CombatTracker(this);
    public final Map<MobEffectList, MobEffect> activeEffects = Maps.newHashMap();
    private final NonNullList<ItemStack> lastHandItemStacks;
    private final NonNullList<ItemStack> lastArmorItemStacks;
    public boolean swinging;
    private boolean discardFriction;
    public EnumHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public float hurtDir;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    public float animationSpeedOld;
    public float animationSpeed;
    public float animationPosition;
    public int invulnerableDuration;
    public final float timeOffs;
    public final float rotA;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public float flyingSpeed;
    @Nullable
    public EntityHuman lastHurtByPlayer;
    protected int lastHurtByPlayerTime;
    protected boolean dead;
    protected int noActionTime;
    protected float oRun;
    protected float run;
    protected float animStep;
    protected float animStepO;
    protected float rotOffs;
    protected int deathScore;
    public float lastHurt;
    protected boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYRot;
    protected double lerpXRot;
    protected double lyHeadRot;
    protected int lerpHeadSteps;
    public boolean effectsDirty;
    @Nullable
    public EntityLiving lastHurtByMob;
    public int lastHurtByMobTimestamp;
    private EntityLiving lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private BlockPosition lastPos;
    private Optional<BlockPosition> lastClimbablePos;
    @Nullable
    private DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    private float swimAmount;
    private float swimAmountO;
    protected BehaviorController<?> brain;
    // CraftBukkit start
    public int expToDrop;
    public boolean forceDrops;
    public ArrayList<org.bukkit.inventory.ItemStack> drops = new ArrayList<org.bukkit.inventory.ItemStack>();
    public final org.bukkit.craftbukkit.attribute.CraftAttributeMap craftAttributes;
    public boolean collides = true;
    public Set<UUID> collidableExemptions = new HashSet<>();
    public boolean bukkitPickUpLoot;

    @Override
    public float getBukkitYaw() {
        return getYHeadRot();
    }
    // CraftBukkit end
    // Spigot start
    public void inactiveTick()
    {
        super.inactiveTick();
        ++this.noActionTime; // Above all the floats
    }
    // Spigot end

    protected EntityLiving(EntityTypes<? extends EntityLiving> entitytypes, World world) {
        super(entitytypes, world);
        this.lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
        this.lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
        this.discardFriction = false;
        this.invulnerableDuration = 20;
        this.flyingSpeed = 0.02F;
        this.effectsDirty = true;
        this.useItem = ItemStack.EMPTY;
        this.lastClimbablePos = Optional.empty();
        this.attributes = new AttributeMapBase(AttributeDefaults.getSupplier(entitytypes));
        this.craftAttributes = new CraftAttributeMap(attributes); // CraftBukkit
        // CraftBukkit - setHealth(getMaxHealth()) inlined and simplified to skip the instanceof check for EntityPlayer, as getBukkitEntity() is not initialized in constructor
        this.entityData.set(EntityLiving.DATA_HEALTH_ID, (float) this.getAttribute(GenericAttributes.MAX_HEALTH).getValue());
        this.blocksBuilding = true;
        this.rotA = (float) ((Math.random() + 1.0D) * 0.009999999776482582D);
        this.reapplyPosition();
        this.timeOffs = (float) Math.random() * 12398.0F;
        this.setYRot((float) (Math.random() * 6.2831854820251465D));
        this.yHeadRot = this.getYRot();
        this.maxUpStep = 0.6F;
        DynamicOpsNBT dynamicopsnbt = DynamicOpsNBT.INSTANCE;

        this.brain = this.makeBrain(new Dynamic(dynamicopsnbt, (NBTBase) dynamicopsnbt.createMap((Map) ImmutableMap.of(dynamicopsnbt.createString("memories"), (NBTBase) dynamicopsnbt.emptyMap()))));
    }

    public BehaviorController<?> getBrain() {
        return this.brain;
    }

    protected BehaviorController.b<?> brainProvider() {
        return BehaviorController.provider(ImmutableList.of(), ImmutableList.of());
    }

    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return this.brainProvider().makeBrain(dynamic);
    }

    @Override
    public void kill() {
        this.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
    }

    public boolean canAttackType(EntityTypes<?> entitytypes) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EntityLiving.DATA_LIVING_ENTITY_FLAGS, (byte) 0);
        this.entityData.define(EntityLiving.DATA_EFFECT_COLOR_ID, 0);
        this.entityData.define(EntityLiving.DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.define(EntityLiving.DATA_ARROW_COUNT_ID, 0);
        this.entityData.define(EntityLiving.DATA_STINGER_COUNT_ID, 0);
        this.entityData.define(EntityLiving.DATA_HEALTH_ID, 1.0F);
        this.entityData.define(EntityLiving.SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeProvider.Builder createLivingAttributes() {
        return AttributeProvider.builder().add(GenericAttributes.MAX_HEALTH).add(GenericAttributes.KNOCKBACK_RESISTANCE).add(GenericAttributes.MOVEMENT_SPEED).add(GenericAttributes.ARMOR).add(GenericAttributes.ARMOR_TOUGHNESS);
    }

    @Override
    protected void checkFallDamage(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {
        if (!this.isInWater()) {
            this.updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (!this.level.isClientSide && flag && this.fallDistance > 0.0F) {
            this.removeSoulSpeed();
            this.tryAddSoulSpeed();
        }

        if (!this.level.isClientSide && this.fallDistance > 3.0F && flag) {
            float f = (float) MathHelper.ceil(this.fallDistance - 3.0F);

            if (!iblockdata.isAir()) {
                double d1 = Math.min((double) (0.2F + f / 15.0F), 2.5D);
                int i = (int) (150.0D * d1);

                // CraftBukkit start - visiblity api
                if (this instanceof EntityPlayer) {
                    ((WorldServer) this.level).sendParticles((EntityPlayer) this, new ParticleParamBlock(Particles.BLOCK, iblockdata), this.getX(), this.getY(), this.getZ(), i, 0.0D, 0.0D, 0.0D, 0.15000000596046448D, false);
                } else {
                    ((WorldServer) this.level).sendParticles(new ParticleParamBlock(Particles.BLOCK, iblockdata), this.getX(), this.getY(), this.getZ(), i, 0.0D, 0.0D, 0.0D, 0.15000000596046448D);
                }
                // CraftBukkit end
            }
        }

        super.checkFallDamage(d0, flag, iblockdata, blockposition);
    }

    public boolean canBreatheUnderwater() {
        return this.getMobType() == EnumMonsterType.UNDEAD;
    }

    public float getSwimAmount(float f) {
        return MathHelper.lerp(f, this.swimAmountO, this.swimAmount);
    }

    @Override
    public void baseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.canSpawnSoulSpeedParticle()) {
            this.spawnSoulSpeedParticle();
        }

        super.baseTick();
        this.level.getProfiler().push("livingEntityBaseTick");
        if (this.fireImmune() || this.level.isClientSide) {
            this.clearFire();
        }

        if (this.isAlive()) {
            boolean flag = this instanceof EntityHuman;

            if (this.isInWall()) {
                this.hurt(DamageSource.IN_WALL, 1.0F);
            } else if (flag && !this.level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double d0 = this.level.getWorldBorder().getDistanceToBorder(this) + this.level.getWorldBorder().getDamageSafeZone();

                if (d0 < 0.0D) {
                    double d1 = this.level.getWorldBorder().getDamagePerBlock();

                    if (d1 > 0.0D) {
                        this.hurt(DamageSource.IN_WALL, (float) Math.max(1, MathHelper.floor(-d0 * d1)));
                    }
                }
            }

            if (this.isEyeInFluid(TagsFluid.WATER) && !this.level.getBlockState(new BlockPosition(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean flag1 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!flag || !((EntityHuman) this).getAbilities().invulnerable);

                if (flag1) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.getAirSupply() == -20) {
                        this.setAirSupply(0);
                        Vec3D vec3d = this.getDeltaMovement();

                        for (int i = 0; i < 8; ++i) {
                            double d2 = this.random.nextDouble() - this.random.nextDouble();
                            double d3 = this.random.nextDouble() - this.random.nextDouble();
                            double d4 = this.random.nextDouble() - this.random.nextDouble();

                            this.level.addParticle(Particles.BUBBLE, this.getX() + d2, this.getY() + d3, this.getZ() + d4, vec3d.x, vec3d.y, vec3d.z);
                        }

                        this.hurt(DamageSource.DROWN, 2.0F);
                    }
                }

                if (!this.level.isClientSide && this.isPassenger() && this.getVehicle() != null && !this.getVehicle().rideableUnderWater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            if (!this.level.isClientSide) {
                BlockPosition blockposition = this.blockPosition();

                if (!Objects.equal(this.lastPos, blockposition)) {
                    this.lastPos = blockposition;
                    this.onChangedBlock(blockposition);
                }
            }
        }

        if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
            if (!this.level.isClientSide && this.wasOnFire) {
                this.playEntityOnFireExtinguishedSound();
            }

            this.clearFire();
        }

        if (this.hurtTime > 0) {
            --this.hurtTime;
        }

        if (this.invulnerableTime > 0 && !(this instanceof EntityPlayer)) {
            --this.invulnerableTime;
        }

        if (this.isDeadOrDying() && this.level.shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerTime > 0) {
            --this.lastHurtByPlayerTime;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        if (this.lastHurtByMob != null) {
            if (!this.lastHurtByMob.isAlive()) {
                this.setLastHurtByMob((EntityLiving) null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob((EntityLiving) null);
            }
        }

        this.tickEffects();
        this.animStepO = this.animStep;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.level.getProfiler().pop();
    }

    public boolean canSpawnSoulSpeedParticle() {
        return this.tickCount % 5 == 0 && this.getDeltaMovement().x != 0.0D && this.getDeltaMovement().z != 0.0D && !this.isSpectator() && EnchantmentManager.hasSoulSpeed(this) && this.onSoulSpeedBlock();
    }

    protected void spawnSoulSpeedParticle() {
        Vec3D vec3d = this.getDeltaMovement();

        this.level.addParticle(Particles.SOUL, this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), vec3d.x * -0.2D, 0.1D, vec3d.z * -0.2D);
        float f = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;

        this.playSound(SoundEffects.SOUL_ESCAPE, f, 0.6F + this.random.nextFloat() * 0.4F);
    }

    protected boolean onSoulSpeedBlock() {
        return this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is((Tag) TagsBlock.SOUL_SPEED_BLOCKS);
    }

    @Override
    protected float getBlockSpeedFactor() {
        return this.onSoulSpeedBlock() && EnchantmentManager.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
    }

    protected boolean shouldRemoveSoulSpeed(IBlockData iblockdata) {
        return !iblockdata.isAir() || this.isFallFlying();
    }

    protected void removeSoulSpeed() {
        AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

        if (attributemodifiable != null) {
            if (attributemodifiable.getModifier(EntityLiving.SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
                attributemodifiable.removeModifier(EntityLiving.SPEED_MODIFIER_SOUL_SPEED_UUID);
            }

        }
    }

    protected void tryAddSoulSpeed() {
        if (!this.getBlockStateOn().isAir()) {
            int i = EnchantmentManager.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);

            if (i > 0 && this.onSoulSpeedBlock()) {
                AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

                if (attributemodifiable == null) {
                    return;
                }

                attributemodifiable.addTransientModifier(new AttributeModifier(EntityLiving.SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", (double) (0.03F * (1.0F + (float) i * 0.35F)), AttributeModifier.Operation.ADDITION));
                if (this.getRandom().nextFloat() < 0.04F) {
                    ItemStack itemstack = this.getItemBySlot(EnumItemSlot.FEET);

                    itemstack.hurtAndBreak(1, this, (entityliving) -> {
                        entityliving.broadcastBreakEvent(EnumItemSlot.FEET);
                    });
                }
            }
        }

    }

    protected void removeFrost() {
        AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

        if (attributemodifiable != null) {
            if (attributemodifiable.getModifier(EntityLiving.SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
                attributemodifiable.removeModifier(EntityLiving.SPEED_MODIFIER_POWDER_SNOW_UUID);
            }

        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOn().isAir()) {
            int i = this.getTicksFrozen();

            if (i > 0) {
                AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

                if (attributemodifiable == null) {
                    return;
                }

                float f = -0.05F * this.getPercentFrozen();

                attributemodifiable.addTransientModifier(new AttributeModifier(EntityLiving.SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", (double) f, AttributeModifier.Operation.ADDITION));
            }
        }

    }

    protected void onChangedBlock(BlockPosition blockposition) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.FROST_WALKER, this);

        if (i > 0) {
            EnchantmentFrostWalker.onEntityMoved(this, this.level, blockposition, i);
        }

        if (this.shouldRemoveSoulSpeed(this.getBlockStateOn())) {
            this.removeSoulSpeed();
        }

        this.tryAddSoulSpeed();
    }

    public boolean isBaby() {
        return false;
    }

    public float getScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    protected boolean isAffectedByFluids() {
        return true;
    }

    @Override
    public boolean rideableUnderWater() {
        return false;
    }

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 20 && !this.isRemoved() && !this.level.isClientSide()) { // CraftBukkit - (this.deathTicks == 20) -> (this.deathTicks >= 20 && !this.dead)
            this.level.broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }

    }

    protected boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot() {
        return !this.isBaby();
    }

    protected int decreaseAirSupply(int i) {
        int j = EnchantmentManager.getRespiration(this);

        return j > 0 && this.random.nextInt(j + 1) > 0 ? i : i - 1;
    }

    protected int increaseAirSupply(int i) {
        return Math.min(i + 4, this.getMaxAirSupply());
    }

    protected int getExperienceReward(EntityHuman entityhuman) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    public Random getRandom() {
        return this.random;
    }

    @Nullable
    public EntityLiving getLastHurtByMob() {
        return this.lastHurtByMob;
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(@Nullable EntityHuman entityhuman) {
        this.lastHurtByPlayer = entityhuman;
        this.lastHurtByPlayerTime = this.tickCount;
    }

    public void setLastHurtByMob(@Nullable EntityLiving entityliving) {
        this.lastHurtByMob = entityliving;
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    @Nullable
    public EntityLiving getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity entity) {
        if (entity instanceof EntityLiving) {
            this.lastHurtMob = (EntityLiving) entity;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int i) {
        this.noActionTime = i;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean flag) {
        this.discardFriction = flag;
    }

    protected void equipEventAndSound(ItemStack itemstack) {
        // CraftBukkit start
        this.equipEventAndSound(itemstack, false);
    }

    protected void equipEventAndSound(ItemStack itemstack, boolean silent) {
        SoundEffect soundeffect = itemstack.getEquipSound();

        if (!itemstack.isEmpty() && soundeffect != null && !this.isSpectator() && !silent) {
            // CraftBukkit end
            this.gameEvent(GameEvent.EQUIP);
            this.playSound(soundeffect, 1.0F, 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        nbttagcompound.putFloat("Health", this.getHealth());
        nbttagcompound.putShort("HurtTime", (short) this.hurtTime);
        nbttagcompound.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        nbttagcompound.putShort("DeathTime", (short) this.deathTime);
        nbttagcompound.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        nbttagcompound.put("Attributes", this.getAttributes().save());
        if (!this.activeEffects.isEmpty()) {
            NBTTagList nbttaglist = new NBTTagList();
            Iterator iterator = this.activeEffects.values().iterator();

            while (iterator.hasNext()) {
                MobEffect mobeffect = (MobEffect) iterator.next();

                nbttaglist.add(mobeffect.save(new NBTTagCompound()));
            }

            nbttagcompound.put("ActiveEffects", nbttaglist);
        }

        nbttagcompound.putBoolean("FallFlying", this.isFallFlying());
        this.getSleepingPos().ifPresent((blockposition) -> {
            nbttagcompound.putInt("SleepingX", blockposition.getX());
            nbttagcompound.putInt("SleepingY", blockposition.getY());
            nbttagcompound.putInt("SleepingZ", blockposition.getZ());
        });
        DataResult<NBTBase> dataresult = this.brain.serializeStart(DynamicOpsNBT.INSTANCE);
        Logger logger = EntityLiving.LOGGER;

        java.util.Objects.requireNonNull(logger);
        dataresult.resultOrPartial(logger::error).ifPresent((nbtbase) -> {
            nbttagcompound.put("Brain", nbtbase);
        });
    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.setAbsorptionAmount(nbttagcompound.getFloat("AbsorptionAmount"));
        if (nbttagcompound.contains("Attributes", 9) && this.level != null && !this.level.isClientSide) {
            this.getAttributes().load(nbttagcompound.getList("Attributes", 10));
        }

        if (nbttagcompound.contains("ActiveEffects", 9)) {
            NBTTagList nbttaglist = nbttagcompound.getList("ActiveEffects", 10);

            for (int i = 0; i < nbttaglist.size(); ++i) {
                NBTTagCompound nbttagcompound1 = nbttaglist.getCompound(i);
                MobEffect mobeffect = MobEffect.load(nbttagcompound1);

                if (mobeffect != null) {
                    this.activeEffects.put(mobeffect.getEffect(), mobeffect);
                }
            }
        }

        // CraftBukkit start
        if (nbttagcompound.contains("Bukkit.MaxHealth")) {
            NBTBase nbtbase = nbttagcompound.get("Bukkit.MaxHealth");
            if (nbtbase.getId() == 5) {
                this.getAttribute(GenericAttributes.MAX_HEALTH).setBaseValue(((NBTTagFloat) nbtbase).getAsDouble());
            } else if (nbtbase.getId() == 3) {
                this.getAttribute(GenericAttributes.MAX_HEALTH).setBaseValue(((NBTTagInt) nbtbase).getAsDouble());
            }
        }
        // CraftBukkit end

        if (nbttagcompound.contains("Health", 99)) {
            this.setHealth(nbttagcompound.getFloat("Health"));
        }

        this.hurtTime = nbttagcompound.getShort("HurtTime");
        this.deathTime = nbttagcompound.getShort("DeathTime");
        this.lastHurtByMobTimestamp = nbttagcompound.getInt("HurtByTimestamp");
        if (nbttagcompound.contains("Team", 8)) {
            String s = nbttagcompound.getString("Team");
            ScoreboardTeam scoreboardteam = this.level.getScoreboard().getPlayerTeam(s);
            boolean flag = scoreboardteam != null && this.level.getScoreboard().addPlayerToTeam(this.getStringUUID(), scoreboardteam);

            if (!flag) {
                EntityLiving.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", s);
            }
        }

        if (nbttagcompound.getBoolean("FallFlying")) {
            this.setSharedFlag(7, true);
        }

        if (nbttagcompound.contains("SleepingX", 99) && nbttagcompound.contains("SleepingY", 99) && nbttagcompound.contains("SleepingZ", 99)) {
            BlockPosition blockposition = new BlockPosition(nbttagcompound.getInt("SleepingX"), nbttagcompound.getInt("SleepingY"), nbttagcompound.getInt("SleepingZ"));

            this.setSleepingPos(blockposition);
            this.entityData.set(EntityLiving.DATA_POSE, EntityPose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(blockposition);
            }
        }

        if (nbttagcompound.contains("Brain", 10)) {
            this.brain = this.makeBrain(new Dynamic(DynamicOpsNBT.INSTANCE, nbttagcompound.get("Brain")));
        }

    }

    // CraftBukkit start
    private boolean isTickingEffects = false;
    private List<ProcessableEffect> effectsToProcess = Lists.newArrayList();

    private static class ProcessableEffect {

        private MobEffectList type;
        private MobEffect effect;
        private final EntityPotionEffectEvent.Cause cause;

        private ProcessableEffect(MobEffect effect, EntityPotionEffectEvent.Cause cause) {
            this.effect = effect;
            this.cause = cause;
        }

        private ProcessableEffect(MobEffectList type, EntityPotionEffectEvent.Cause cause) {
            this.type = type;
            this.cause = cause;
        }
    }
    // CraftBukkit end

    protected void tickEffects() {
        Iterator iterator = this.activeEffects.keySet().iterator();

        isTickingEffects = true; // CraftBukkit
        try {
            while (iterator.hasNext()) {
                MobEffectList mobeffectlist = (MobEffectList) iterator.next();
                MobEffect mobeffect = (MobEffect) this.activeEffects.get(mobeffectlist);

                if (!mobeffect.tick(this, () -> {
                    this.onEffectUpdated(mobeffect, true, (Entity) null);
                })) {
                    if (!this.level.isClientSide) {
                        // CraftBukkit start
                        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(this, mobeffect, null, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.EXPIRATION);
                        if (event.isCancelled()) {
                            continue;
                        }
                        // CraftBukkit end
                        iterator.remove();
                        this.onEffectRemoved(mobeffect);
                    }
                } else if (mobeffect.getDuration() % 600 == 0) {
                    this.onEffectUpdated(mobeffect, false, (Entity) null);
                }
            }
        } catch (ConcurrentModificationException concurrentmodificationexception) {
            ;
        }
        // CraftBukkit start
        isTickingEffects = false;
        for (ProcessableEffect e : effectsToProcess) {
            if (e.effect != null) {
                addEffect(e.effect, e.cause);
            } else {
                removeEffect(e.type, e.cause);
            }
        }
        effectsToProcess.clear();
        // CraftBukkit end

        if (this.effectsDirty) {
            if (!this.level.isClientSide) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
            }

            this.effectsDirty = false;
        }

        int i = (Integer) this.entityData.get(EntityLiving.DATA_EFFECT_COLOR_ID);
        boolean flag = (Boolean) this.entityData.get(EntityLiving.DATA_EFFECT_AMBIENCE_ID);

        if (i > 0) {
            boolean flag1;

            if (this.isInvisible()) {
                flag1 = this.random.nextInt(15) == 0;
            } else {
                flag1 = this.random.nextBoolean();
            }

            if (flag) {
                flag1 &= this.random.nextInt(5) == 0;
            }

            if (flag1 && i > 0) {
                double d0 = (double) (i >> 16 & 255) / 255.0D;
                double d1 = (double) (i >> 8 & 255) / 255.0D;
                double d2 = (double) (i >> 0 & 255) / 255.0D;

                this.level.addParticle(flag ? Particles.AMBIENT_ENTITY_EFFECT : Particles.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d0, d1, d2);
            }
        }

    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            Collection<MobEffect> collection = this.activeEffects.values();

            this.entityData.set(EntityLiving.DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(collection));
            this.entityData.set(EntityLiving.DATA_EFFECT_COLOR_ID, PotionUtil.getColor(collection));
            this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
        }

    }

    private void updateGlowingStatus() {
        boolean flag = this.isCurrentlyGlowing();

        if (this.getSharedFlag(6) != flag) {
            this.setSharedFlag(6, flag);
        }

    }

    public double getVisibilityPercent(@Nullable Entity entity) {
        double d0 = 1.0D;

        if (this.isDiscrete()) {
            d0 *= 0.8D;
        }

        if (this.isInvisible()) {
            float f = this.getArmorCoverPercentage();

            if (f < 0.1F) {
                f = 0.1F;
            }

            d0 *= 0.7D * (double) f;
        }

        if (entity != null) {
            ItemStack itemstack = this.getItemBySlot(EnumItemSlot.HEAD);
            EntityTypes<?> entitytypes = entity.getType();

            if (entitytypes == EntityTypes.SKELETON && itemstack.is(Items.SKELETON_SKULL) || entitytypes == EntityTypes.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD) || entitytypes == EntityTypes.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
                d0 *= 0.5D;
            }
        }

        return d0;
    }

    public boolean canAttack(EntityLiving entityliving) {
        return entityliving instanceof EntityHuman && this.level.getDifficulty() == EnumDifficulty.PEACEFUL ? false : entityliving.canBeSeenAsEnemy();
    }

    public boolean canAttack(EntityLiving entityliving, PathfinderTargetCondition pathfindertargetcondition) {
        return pathfindertargetcondition.test(this, entityliving);
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffect> collection) {
        Iterator iterator = collection.iterator();

        MobEffect mobeffect;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            mobeffect = (MobEffect) iterator.next();
        } while (!mobeffect.isVisible() || mobeffect.isAmbient());

        return false;
    }

    protected void removeEffectParticles() {
        this.entityData.set(EntityLiving.DATA_EFFECT_AMBIENCE_ID, false);
        this.entityData.set(EntityLiving.DATA_EFFECT_COLOR_ID, 0);
    }

    // CraftBukkit start
    public boolean removeAllEffects() {
        return removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN);
    }

    public boolean removeAllEffects(EntityPotionEffectEvent.Cause cause) {
        // CraftBukkit end
        if (this.level.isClientSide) {
            return false;
        } else {
            Iterator<MobEffect> iterator = this.activeEffects.values().iterator();

            boolean flag;

            for (flag = false; iterator.hasNext(); flag = true) {
                // CraftBukkit start
                MobEffect effect = (MobEffect) iterator.next();
                EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(this, effect, null, cause, EntityPotionEffectEvent.Action.CLEARED);
                if (event.isCancelled()) {
                    continue;
                }
                this.onEffectRemoved(effect);
                // CraftBukkit end
                iterator.remove();
            }

            return flag;
        }
    }

    public Collection<MobEffect> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<MobEffectList, MobEffect> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(MobEffectList mobeffectlist) {
        return this.activeEffects.containsKey(mobeffectlist);
    }

    @Nullable
    public MobEffect getEffect(MobEffectList mobeffectlist) {
        return (MobEffect) this.activeEffects.get(mobeffectlist);
    }

    public final boolean addEffect(MobEffect mobeffect) {
        return this.addEffect(mobeffect, (Entity) null);
    }

    // CraftBukkit start
    public boolean addEffect(MobEffect mobeffect, EntityPotionEffectEvent.Cause cause) {
        return this.addEffect(mobeffect, (Entity) null, cause);
    }

    public boolean addEffect(MobEffect mobeffect, @Nullable Entity entity) {
        return this.addEffect(mobeffect, entity, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN);
    }

    public boolean addEffect(MobEffect mobeffect, @Nullable Entity entity, EntityPotionEffectEvent.Cause cause) {
        org.spigotmc.AsyncCatcher.catchOp("effect add"); // Spigot
        if (isTickingEffects) {
            effectsToProcess.add(new ProcessableEffect(mobeffect, cause));
            return true;
        }
        // CraftBukkit end

        if (!this.canBeAffected(mobeffect)) {
            return false;
        } else {
            MobEffect mobeffect1 = (MobEffect) this.activeEffects.get(mobeffect.getEffect());

            // CraftBukkit start
            boolean override = false;
            if (mobeffect1 != null) {
                override = new MobEffect(mobeffect1).update(mobeffect);
            }

            EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(this, mobeffect1, mobeffect, cause, override);
            if (event.isCancelled()) {
                return false;
            }
            // CraftBukkit end

            if (mobeffect1 == null) {
                this.activeEffects.put(mobeffect.getEffect(), mobeffect);
                this.onEffectAdded(mobeffect, entity);
                return true;
                // CraftBukkit start
            } else if (event.isOverride()) {
                mobeffect1.update(mobeffect);
                this.onEffectUpdated(mobeffect1, true, entity);
                // CraftBukkit end
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean canBeAffected(MobEffect mobeffect) {
        if (this.getMobType() == EnumMonsterType.UNDEAD) {
            MobEffectList mobeffectlist = mobeffect.getEffect();

            if (mobeffectlist == MobEffects.REGENERATION || mobeffectlist == MobEffects.POISON) {
                return false;
            }
        }

        return true;
    }

    public void forceAddEffect(MobEffect mobeffect, @Nullable Entity entity) {
        if (this.canBeAffected(mobeffect)) {
            MobEffect mobeffect1 = (MobEffect) this.activeEffects.put(mobeffect.getEffect(), mobeffect);

            if (mobeffect1 == null) {
                this.onEffectAdded(mobeffect, entity);
            } else {
                this.onEffectUpdated(mobeffect, true, entity);
            }

        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.getMobType() == EnumMonsterType.UNDEAD;
    }

    // CraftBukkit start
    @Nullable
    public MobEffect removeEffectNoUpdate(@Nullable MobEffectList mobeffectlist) {
        return c(mobeffectlist, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN);
    }

    @Nullable
    public MobEffect c(@Nullable MobEffectList mobeffectlist, EntityPotionEffectEvent.Cause cause) {
        if (isTickingEffects) {
            effectsToProcess.add(new ProcessableEffect(mobeffectlist, cause));
            return null;
        }

        MobEffect effect = this.activeEffects.get(mobeffectlist);
        if (effect == null) {
            return null;
        }

        EntityPotionEffectEvent event = CraftEventFactory.callEntityPotionEffectChangeEvent(this, effect, null, cause);
        if (event.isCancelled()) {
            return null;
        }

        return (MobEffect) this.activeEffects.remove(mobeffectlist);
    }

    public boolean removeEffect(MobEffectList mobeffectlist) {
        return removeEffect(mobeffectlist, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN);
    }

    public boolean removeEffect(MobEffectList mobeffectlist, EntityPotionEffectEvent.Cause cause) {
        MobEffect mobeffect = this.c(mobeffectlist, cause);
        // CraftBukkit end

        if (mobeffect != null) {
            this.onEffectRemoved(mobeffect);
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffect mobeffect, @Nullable Entity entity) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            mobeffect.getEffect().addAttributeModifiers(this, this.getAttributes(), mobeffect.getAmplifier());
        }

    }

    protected void onEffectUpdated(MobEffect mobeffect, boolean flag, @Nullable Entity entity) {
        this.effectsDirty = true;
        if (flag && !this.level.isClientSide) {
            MobEffectList mobeffectlist = mobeffect.getEffect();

            mobeffectlist.removeAttributeModifiers(this, this.getAttributes(), mobeffect.getAmplifier());
            mobeffectlist.addAttributeModifiers(this, this.getAttributes(), mobeffect.getAmplifier());
        }

    }

    protected void onEffectRemoved(MobEffect mobeffect) {
        this.effectsDirty = true;
        if (!this.level.isClientSide) {
            mobeffect.getEffect().removeAttributeModifiers(this, this.getAttributes(), mobeffect.getAmplifier());
        }

    }

    // CraftBukkit start - Delegate so we can handle providing a reason for health being regained
    public void heal(float f) {
        heal(f, EntityRegainHealthEvent.RegainReason.CUSTOM);
    }

    public void heal(float f, EntityRegainHealthEvent.RegainReason regainReason) {
        float f1 = this.getHealth();

        if (f1 > 0.0F) {
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(this.getBukkitEntity(), f, regainReason);
            // Suppress during worldgen
            if (this.valid) {
                this.level.getCraftServer().getPluginManager().callEvent(event);
            }

            if (!event.isCancelled()) {
                this.setHealth((float) (this.getHealth() + event.getAmount()));
            }
            // CraftBukkit end
        }

    }

    public float getHealth() {
        // CraftBukkit start - Use unscaled health
        if (this instanceof EntityPlayer) {
            return (float) ((EntityPlayer) this).getBukkitEntity().getHealth();
        }
        // CraftBukkit end
        return (Float) this.entityData.get(EntityLiving.DATA_HEALTH_ID);
    }

    public void setHealth(float f) {
        // CraftBukkit start - Handle scaled health
        if (this instanceof EntityPlayer) {
            org.bukkit.craftbukkit.entity.CraftPlayer player = ((EntityPlayer) this).getBukkitEntity();
            // Squeeze
            if (f < 0.0F) {
                player.setRealHealth(0.0D);
            } else if (f > player.getMaxHealth()) {
                player.setRealHealth(player.getMaxHealth());
            } else {
                player.setRealHealth(f);
            }

            player.updateScaledHealth(false);
            return;
        }
        // CraftBukkit end
        this.entityData.set(EntityLiving.DATA_HEALTH_ID, MathHelper.clamp(f, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(damagesource)) {
            return false;
        } else if (this.level.isClientSide) {
            return false;
        } else if (this.isRemoved() || this.dead || this.getHealth() <= 0.0F) { // CraftBukkit - Don't allow entities that got set to dead/killed elsewhere to get damaged and die
            return false;
        } else if (damagesource.isFire() && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping() && !this.level.isClientSide) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            float f1 = f;
            boolean flag = f > 0.0F && this.isDamageSourceBlocked(damagesource); // Copied from below
            float f2 = 0.0F;

            // CraftBukkit - Moved into damageEntity0(DamageSource, float)
            if (false && f > 0.0F && this.isDamageSourceBlocked(damagesource)) {
                this.hurtCurrentlyUsedShield(f);
                f2 = f;
                f = 0.0F;
                if (!damagesource.isProjectile()) {
                    Entity entity = damagesource.getDirectEntity();

                    if (entity instanceof EntityLiving) {
                        this.blockUsingShield((EntityLiving) entity);
                    }
                }

                flag = true;
            }

            this.animationSpeed = 1.5F;
            boolean flag1 = true;

            if ((float) this.invulnerableTime > (float) this.invulnerableDuration / 2.0F) { // CraftBukkit - restore use of maxNoDamageTicks
                if (f <= this.lastHurt) {
                    return false;
                }

                // CraftBukkit start
                if (!this.damageEntity0(damagesource, f - this.lastHurt)) {
                    return false;
                }
                // CraftBukkit end
                this.lastHurt = f;
                flag1 = false;
            } else {
                // CraftBukkit start
                if (!this.damageEntity0(damagesource, f)) {
                    return false;
                }
                this.lastHurt = f;
                this.invulnerableTime = this.invulnerableDuration; // CraftBukkit - restore use of maxNoDamageTicks
                // this.damageEntity0(damagesource, f);
                // CraftBukkit end
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            // CraftBukkit - Moved into damageEntity0(DamageSource, float)
            if (false && damagesource.isDamageHelmet() && !this.getItemBySlot(EnumItemSlot.HEAD).isEmpty()) {
                this.hurtHelmet(damagesource, f);
                f *= 0.75F;
            }

            // CraftBukkit start
            if (this instanceof EntityAnimal) {
                ((EntityAnimal) this).resetLove();
                if (this instanceof EntityTameableAnimal) {
                    ((EntityTameableAnimal) this).setOrderedToSit(false);
                }
            }
            // CraftBukkit end

            this.hurtDir = 0.0F;
            Entity entity1 = damagesource.getEntity();

            if (entity1 != null) {
                if (entity1 instanceof EntityLiving && !damagesource.isNoAggro()) {
                    this.setLastHurtByMob((EntityLiving) entity1);
                }

                if (entity1 instanceof EntityHuman) {
                    this.lastHurtByPlayerTime = 100;
                    this.lastHurtByPlayer = (EntityHuman) entity1;
                } else if (entity1 instanceof EntityWolf) {
                    EntityWolf entitywolf = (EntityWolf) entity1;

                    if (entitywolf.isTame()) {
                        this.lastHurtByPlayerTime = 100;
                        EntityLiving entityliving = entitywolf.getOwner();

                        if (entityliving != null && entityliving.getType() == EntityTypes.PLAYER) {
                            this.lastHurtByPlayer = (EntityHuman) entityliving;
                        } else {
                            this.lastHurtByPlayer = null;
                        }
                    }
                }
            }

            if (flag1) {
                if (flag) {
                    this.level.broadcastEntityEvent(this, (byte) 29);
                } else if (damagesource instanceof EntityDamageSource && ((EntityDamageSource) damagesource).isThorns()) {
                    this.level.broadcastEntityEvent(this, (byte) 33);
                } else {
                    byte b0;

                    if (damagesource == DamageSource.DROWN) {
                        b0 = 36;
                    } else if (damagesource.isFire()) {
                        b0 = 37;
                    } else if (damagesource == DamageSource.SWEET_BERRY_BUSH) {
                        b0 = 44;
                    } else if (damagesource == DamageSource.FREEZE) {
                        b0 = 57;
                    } else {
                        b0 = 2;
                    }

                    this.level.broadcastEntityEvent(this, b0);
                }

                if (damagesource != DamageSource.DROWN && (!flag || f > 0.0F)) {
                    this.markHurt();
                }

                if (entity1 != null) {
                    double d0 = entity1.getX() - this.getX();

                    double d1;

                    for (d1 = entity1.getZ() - this.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                        d0 = (Math.random() - Math.random()) * 0.01D;
                    }

                    this.hurtDir = (float) (MathHelper.atan2(d1, d0) * 57.2957763671875D - (double) this.getYRot());
                    this.knockback(0.4000000059604645D, d0, d1);
                } else {
                    this.hurtDir = (float) ((int) (Math.random() * 2.0D) * 180);
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(damagesource)) {
                    SoundEffect soundeffect = this.getDeathSound();

                    if (flag1 && soundeffect != null) {
                        this.playSound(soundeffect, this.getSoundVolume(), this.getVoicePitch());
                    }

                    this.die(damagesource);
                }
            } else if (flag1) {
                this.playHurtSound(damagesource);
            }

            boolean flag2 = !flag || f > 0.0F;

            if (flag2) {
                this.lastDamageSource = damagesource;
                this.lastDamageStamp = this.level.getGameTime();
            }

            if (this instanceof EntityPlayer) {
                CriterionTriggers.ENTITY_HURT_PLAYER.trigger((EntityPlayer) this, damagesource, f1, f, flag);
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    ((EntityPlayer) this).awardStat(StatisticList.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f2 * 10.0F));
                }
            }

            if (entity1 instanceof EntityPlayer) {
                CriterionTriggers.PLAYER_HURT_ENTITY.trigger((EntityPlayer) entity1, this, damagesource, f1, f, flag);
            }

            return flag2;
        }
    }

    protected void blockUsingShield(EntityLiving entityliving) {
        entityliving.blockedByShield(this);
    }

    protected void blockedByShield(EntityLiving entityliving) {
        entityliving.knockback(0.5D, entityliving.getX() - this.getX(), entityliving.getZ() - this.getZ());
    }

    private boolean checkTotemDeathProtection(DamageSource damagesource) {
        if (damagesource.isBypassInvul()) {
            return false;
        } else {
            ItemStack itemstack = null;
            EnumHand[] aenumhand = EnumHand.values();
            int i = aenumhand.length;

            // CraftBukkit start
            ItemStack itemstack1 = ItemStack.EMPTY;
            for (int j = 0; j < i; ++j) {
                EnumHand enumhand = aenumhand[j];
                itemstack1 = this.getItemInHand(enumhand);

                if (itemstack1.is(Items.TOTEM_OF_UNDYING)) {
                    itemstack = itemstack1.copy();
                    // itemstack1.subtract(1); // CraftBukkit
                    break;
                }
            }

            EntityResurrectEvent event = new EntityResurrectEvent((LivingEntity) this.getBukkitEntity());
            event.setCancelled(itemstack == null);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                if (!itemstack1.isEmpty()) {
                    itemstack1.shrink(1);
                }
                if (itemstack != null && this instanceof EntityPlayer) {
                    // CraftBukkit end
                    EntityPlayer entityplayer = (EntityPlayer) this;

                    entityplayer.awardStat(StatisticList.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                    CriterionTriggers.USED_TOTEM.trigger(entityplayer, itemstack);
                }

                this.setHealth(1.0F);
                // CraftBukkit start
                this.removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffect(MobEffects.REGENERATION, 900, 1), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffect(MobEffects.ABSORPTION, 100, 1), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.TOTEM);
                this.addEffect(new MobEffect(MobEffects.FIRE_RESISTANCE, 800, 0), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.TOTEM);
                // CraftBukkit end
                this.level.broadcastEntityEvent(this, (byte) 35);
            }

            return !event.isCancelled();
        }
    }

    @Nullable
    public DamageSource getLastDamageSource() {
        if (this.level.getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource damagesource) {
        SoundEffect soundeffect = this.getHurtSound(damagesource);

        if (soundeffect != null) {
            this.playSound(soundeffect, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    public boolean isDamageSourceBlocked(DamageSource damagesource) {
        Entity entity = damagesource.getDirectEntity();
        boolean flag = false;

        if (entity instanceof EntityArrow) {
            EntityArrow entityarrow = (EntityArrow) entity;

            if (entityarrow.getPierceLevel() > 0) {
                flag = true;
            }
        }

        if (!damagesource.isBypassArmor() && this.isBlocking() && !flag) {
            Vec3D vec3d = damagesource.getSourcePosition();

            if (vec3d != null) {
                Vec3D vec3d1 = this.getViewVector(1.0F);
                Vec3D vec3d2 = vec3d.vectorTo(this.position()).normalize();

                vec3d2 = new Vec3D(vec3d2.x, 0.0D, vec3d2.z);
                if (vec3d2.dot(vec3d1) < 0.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    private void breakItem(ItemStack itemstack) {
        if (!itemstack.isEmpty()) {
            if (!this.isSilent()) {
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEffects.ITEM_BREAK, this.getSoundSource(), 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F, false);
            }

            this.spawnItemParticles(itemstack, 5);
        }

    }

    public void die(DamageSource damagesource) {
        if (!this.isRemoved() && !this.dead) {
            Entity entity = damagesource.getEntity();
            EntityLiving entityliving = this.getKillCredit();

            if (this.deathScore >= 0 && entityliving != null) {
                entityliving.awardKillScore(this, this.deathScore, damagesource);
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            if (!this.level.isClientSide && this.hasCustomName()) {
                if (org.spigotmc.SpigotConfig.logNamedDeaths) EntityLiving.LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString()); // Spigot
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level instanceof WorldServer) {
                if (entity != null) {
                    entity.killed((WorldServer) this.level, this);
                }

                this.dropAllDeathLoot(damagesource);
                this.createWitherRose(entityliving);
            }

            this.level.broadcastEntityEvent(this, (byte) 3);
            this.setPose(EntityPose.DYING);
        }
    }

    protected void createWitherRose(@Nullable EntityLiving entityliving) {
        if (!this.level.isClientSide) {
            boolean flag = false;

            if (entityliving instanceof EntityWither) {
                if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    BlockPosition blockposition = this.blockPosition();
                    IBlockData iblockdata = Blocks.WITHER_ROSE.defaultBlockState();

                    if (this.level.getBlockState(blockposition).isAir() && iblockdata.canSurvive(this.level, blockposition)) {
                        // CraftBukkit start - call EntityBlockFormEvent for Wither Rose
                        flag = org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockFormEvent(this.level, blockposition, iblockdata, 3, this);
                        // CraftBukkit end
                    }
                }

                if (!flag) {
                    EntityItem entityitem = new EntityItem(this.level, this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));

                    // CraftBukkit start
                    org.bukkit.event.entity.EntityDropItemEvent event = new org.bukkit.event.entity.EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) entityitem.getBukkitEntity());
                    CraftEventFactory.callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    // CraftBukkit end
                    this.level.addFreshEntity(entityitem);
                }
            }

        }
    }

    protected void dropAllDeathLoot(DamageSource damagesource) {
        Entity entity = damagesource.getEntity();
        int i;

        if (entity instanceof EntityHuman) {
            i = EnchantmentManager.getMobLooting((EntityLiving) entity);
        } else {
            i = 0;
        }

        boolean flag = this.lastHurtByPlayerTime > 0;

        this.dropEquipment(); // CraftBukkit - from below
        if (this.shouldDropLoot() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.dropFromLootTable(damagesource, flag);
            this.dropCustomDeathLoot(damagesource, i, flag);
        }
        // CraftBukkit start - Call death event
        CraftEventFactory.callEntityDeathEvent(this, this.drops);
        this.drops = new ArrayList<>();
        // CraftBukkit end

        // this.dropInventory();// CraftBukkit - moved up
        this.dropExperience();
    }

    protected void dropEquipment() {}

    // CraftBukkit start
    public int getExpReward() {
        if (this.level instanceof WorldServer && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            int i = this.getExperienceReward(this.lastHurtByPlayer);
            return i;
        } else {
            return 0;
        }
    }
    // CraftBukkit end

    protected void dropExperience() {
        // CraftBukkit start - Update getExpReward() above if the removed if() changes!
        if (true) {
            EntityExperienceOrb.award((WorldServer) this.level, this.position(), this.expToDrop);
            this.expToDrop = 0;
        }
        // CraftBukkit end

    }

    protected void dropCustomDeathLoot(DamageSource damagesource, int i, boolean flag) {}

    public MinecraftKey getLootTable() {
        return this.getType().getDefaultLootTable();
    }

    protected void dropFromLootTable(DamageSource damagesource, boolean flag) {
        MinecraftKey minecraftkey = this.getLootTable();
        LootTable loottable = this.level.getServer().getLootTables().get(minecraftkey);
        LootTableInfo.Builder loottableinfo_builder = this.createLootContext(flag, damagesource);

        loottable.getRandomItems(loottableinfo_builder.create(LootContextParameterSets.ENTITY), this::spawnAtLocation);
    }

    protected LootTableInfo.Builder createLootContext(boolean flag, DamageSource damagesource) {
        LootTableInfo.Builder loottableinfo_builder = (new LootTableInfo.Builder((WorldServer) this.level)).withRandom(this.random).withParameter(LootContextParameters.THIS_ENTITY, this).withParameter(LootContextParameters.ORIGIN, this.position()).withParameter(LootContextParameters.DAMAGE_SOURCE, damagesource).withOptionalParameter(LootContextParameters.KILLER_ENTITY, damagesource.getEntity()).withOptionalParameter(LootContextParameters.DIRECT_KILLER_ENTITY, damagesource.getDirectEntity());

        if (flag && this.lastHurtByPlayer != null) {
            loottableinfo_builder = loottableinfo_builder.withParameter(LootContextParameters.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
        }

        return loottableinfo_builder;
    }

    public void knockback(double d0, double d1, double d2) {
        d0 *= 1.0D - this.getAttributeValue(GenericAttributes.KNOCKBACK_RESISTANCE);
        if (d0 > 0.0D) {
            this.hasImpulse = true;
            Vec3D vec3d = this.getDeltaMovement();
            Vec3D vec3d1 = (new Vec3D(d1, 0.0D, d2)).normalize().scale(d0);

            this.setDeltaMovement(vec3d.x / 2.0D - vec3d1.x, this.onGround ? Math.min(0.4D, vec3d.y / 2.0D + d0) : vec3d.y, vec3d.z / 2.0D - vec3d1.z);
        }
    }

    @Nullable
    protected SoundEffect getHurtSound(DamageSource damagesource) {
        return SoundEffects.GENERIC_HURT;
    }

    @Nullable
    protected SoundEffect getDeathSound() {
        return SoundEffects.GENERIC_DEATH;
    }

    private SoundEffect getFallDamageSound(int i) {
        return i > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public EntityLiving.a getFallSounds() {
        return new EntityLiving.a(SoundEffects.GENERIC_SMALL_FALL, SoundEffects.GENERIC_BIG_FALL);
    }

    protected SoundEffect getDrinkingSound(ItemStack itemstack) {
        return itemstack.getDrinkingSound();
    }

    public SoundEffect getEatingSound(ItemStack itemstack) {
        return itemstack.getEatingSound();
    }

    @Override
    public void setOnGround(boolean flag) {
        super.setOnGround(flag);
        if (flag) {
            this.lastClimbablePos = Optional.empty();
        }

    }

    public Optional<BlockPosition> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPosition blockposition = this.blockPosition();
            IBlockData iblockdata = this.getFeetBlockState();

            if (iblockdata.is((Tag) TagsBlock.CLIMBABLE)) {
                this.lastClimbablePos = Optional.of(blockposition);
                return true;
            } else if (iblockdata.getBlock() instanceof BlockTrapdoor && this.trapdoorUsableAsLadder(blockposition, iblockdata)) {
                this.lastClimbablePos = Optional.of(blockposition);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPosition blockposition, IBlockData iblockdata) {
        if ((Boolean) iblockdata.getValue(BlockTrapdoor.OPEN)) {
            IBlockData iblockdata1 = this.level.getBlockState(blockposition.below());

            if (iblockdata1.is(Blocks.LADDER) && iblockdata1.getValue(BlockLadder.FACING) == iblockdata.getValue(BlockTrapdoor.FACING)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    @Override
    public boolean causeFallDamage(float f, float f1, DamageSource damagesource) {
        boolean flag = super.causeFallDamage(f, f1, damagesource);
        int i = this.calculateFallDamage(f, f1);

        if (i > 0) {
            // CraftBukkit start
            if (!this.hurt(damagesource, (float) i)) {
                return true;
            }
            // CraftBukkit end
            this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
            this.playBlockFallSound();
            // this.damageEntity(damagesource, (float) i); // CraftBukkit - moved up
            return true;
        } else {
            return flag;
        }
    }

    protected int calculateFallDamage(float f, float f1) {
        MobEffect mobeffect = this.getEffect(MobEffects.JUMP);
        float f2 = mobeffect == null ? 0.0F : (float) (mobeffect.getAmplifier() + 1);

        return MathHelper.ceil((f - 3.0F - f2) * f1);
    }

    protected void playBlockFallSound() {
        if (!this.isSilent()) {
            int i = MathHelper.floor(this.getX());
            int j = MathHelper.floor(this.getY() - 0.20000000298023224D);
            int k = MathHelper.floor(this.getZ());
            IBlockData iblockdata = this.level.getBlockState(new BlockPosition(i, j, k));

            if (!iblockdata.isAir()) {
                SoundEffectType soundeffecttype = iblockdata.getSoundType();

                this.playSound(soundeffecttype.getFallSound(), soundeffecttype.getVolume() * 0.5F, soundeffecttype.getPitch() * 0.75F);
            }

        }
    }

    @Override
    public void animateHurt() {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        this.hurtDir = 0.0F;
    }

    public int getArmorValue() {
        return MathHelper.floor(this.getAttributeValue(GenericAttributes.ARMOR));
    }

    protected void hurtArmor(DamageSource damagesource, float f) {}

    protected void hurtHelmet(DamageSource damagesource, float f) {}

    protected void hurtCurrentlyUsedShield(float f) {}

    protected float getDamageAfterArmorAbsorb(DamageSource damagesource, float f) {
        if (!damagesource.isBypassArmor()) {
            // this.damageArmor(damagesource, f); // CraftBukkit - Moved into damageEntity0(DamageSource, float)
            f = CombatMath.getDamageAfterAbsorb(f, (float) this.getArmorValue(), (float) this.getAttributeValue(GenericAttributes.ARMOR_TOUGHNESS));
        }

        return f;
    }

    protected float getDamageAfterMagicAbsorb(DamageSource damagesource, float f) {
        if (damagesource.isBypassMagic()) {
            return f;
        } else {
            int i;

            // CraftBukkit - Moved to damageEntity0(DamageSource, float)
            if (false && this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f1 = f * (float) j;
                float f2 = f;

                f = Math.max(f1 / 25.0F, 0.0F);
                float f3 = f2 - f;

                if (f3 > 0.0F && f3 < 3.4028235E37F) {
                    if (this instanceof EntityPlayer) {
                        ((EntityPlayer) this).awardStat(StatisticList.DAMAGE_RESISTED, Math.round(f3 * 10.0F));
                    } else if (damagesource.getEntity() instanceof EntityPlayer) {
                        ((EntityPlayer) damagesource.getEntity()).awardStat(StatisticList.DAMAGE_DEALT_RESISTED, Math.round(f3 * 10.0F));
                    }
                }
            }

            if (f <= 0.0F) {
                return 0.0F;
            } else {
                i = EnchantmentManager.getDamageProtection(this.getArmorSlots(), damagesource);
                if (i > 0) {
                    f = CombatMath.getDamageAfterMagicAbsorb(f, (float) i);
                }

                return f;
            }
        }
    }

    // CraftBukkit start
    protected boolean damageEntity0(final DamageSource damagesource, float f) { // void -> boolean, add final
       if (!this.isInvulnerableTo(damagesource)) {
            final boolean human = this instanceof EntityHuman;
            float originalDamage = f;
            Function<Double, Double> hardHat = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    if (damagesource.isDamageHelmet() && !EntityLiving.this.getItemBySlot(EnumItemSlot.HEAD).isEmpty()) {
                        return -(f - (f * 0.75F));

                    }
                    return -0.0;
                }
            };
            float hardHatModifier = hardHat.apply((double) f).floatValue();
            f += hardHatModifier;

            Function<Double, Double> blocking = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -((EntityLiving.this.isDamageSourceBlocked(damagesource)) ? f : 0.0);
                }
            };
            float blockingModifier = blocking.apply((double) f).floatValue();
            f += blockingModifier;

            Function<Double, Double> armor = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(f - EntityLiving.this.getDamageAfterArmorAbsorb(damagesource, f.floatValue()));
                }
            };
            float armorModifier = armor.apply((double) f).floatValue();
            f += armorModifier;

            Function<Double, Double> resistance = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    if (!damagesource.isBypassMagic() && EntityLiving.this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                        int i = (EntityLiving.this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                        int j = 25 - i;
                        float f1 = f.floatValue() * (float) j;
                        return -(f - (f1 / 25.0F));
                    }
                    return -0.0;
                }
            };
            float resistanceModifier = resistance.apply((double) f).floatValue();
            f += resistanceModifier;

            Function<Double, Double> magic = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(f - EntityLiving.this.getDamageAfterMagicAbsorb(damagesource, f.floatValue()));
                }
            };
            float magicModifier = magic.apply((double) f).floatValue();
            f += magicModifier;

            Function<Double, Double> absorption = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(Math.max(f - Math.max(f - EntityLiving.this.getAbsorptionAmount(), 0.0F), 0.0F));
                }
            };
            float absorptionModifier = absorption.apply((double) f).floatValue();

            EntityDamageEvent event = CraftEventFactory.handleLivingEntityDamageEvent(this, damagesource, originalDamage, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, hardHat, blocking, armor, resistance, magic, absorption);
            if (damagesource.getEntity() instanceof EntityHuman) {
                ((EntityHuman) damagesource.getEntity()).resetAttackStrengthTicker(); // Moved from EntityHuman in order to make the cooldown reset get called after the damage event is fired
            }
            if (event.isCancelled()) {
                return false;
            }

            f = (float) event.getFinalDamage();

            // Resistance
            if (event.getDamage(DamageModifier.RESISTANCE) < 0) {
                float f3 = (float) -event.getDamage(DamageModifier.RESISTANCE);
                if (f3 > 0.0F && f3 < 3.4028235E37F) {
                    if (this instanceof EntityPlayer) {
                        ((EntityPlayer) this).awardStat(StatisticList.DAMAGE_RESISTED, Math.round(f3 * 10.0F));
                    } else if (damagesource.getEntity() instanceof EntityPlayer) {
                        ((EntityPlayer) damagesource.getEntity()).awardStat(StatisticList.DAMAGE_DEALT_RESISTED, Math.round(f3 * 10.0F));
                    }
                }
            }

            // Apply damage to helmet
            if (damagesource.isDamageHelmet() && !this.getItemBySlot(EnumItemSlot.HEAD).isEmpty()) {
                this.hurtHelmet(damagesource, f);
            }

            // Apply damage to armor
            if (!damagesource.isBypassArmor()) {
                float armorDamage = (float) (event.getDamage() + event.getDamage(DamageModifier.BLOCKING) + event.getDamage(DamageModifier.HARD_HAT));
                this.hurtArmor(damagesource, armorDamage);
            }

            // Apply blocking code // PAIL: steal from above
            if (event.getDamage(DamageModifier.BLOCKING) < 0) {
                this.level.broadcastEntityEvent(this, (byte) 29); // SPIGOT-4635 - shield damage sound
                this.hurtCurrentlyUsedShield((float) -event.getDamage(DamageModifier.BLOCKING));
                Entity entity = damagesource.getDirectEntity();

                if (entity instanceof EntityLiving) {
                    this.blockUsingShield((EntityLiving) entity);
                }
            }

            absorptionModifier = (float) -event.getDamage(DamageModifier.ABSORPTION);
            this.setAbsorptionAmount(Math.max(this.getAbsorptionAmount() - absorptionModifier, 0.0F));
            float f2 = absorptionModifier;

            if (f2 > 0.0F && f2 < 3.4028235E37F && this instanceof EntityHuman) {
                ((EntityHuman) this).awardStat(StatisticList.DAMAGE_ABSORBED, Math.round(f2 * 10.0F));
            }
            if (f2 > 0.0F && f2 < 3.4028235E37F && damagesource.getEntity() instanceof EntityPlayer) {
                ((EntityPlayer) damagesource.getEntity()).awardStat(StatisticList.DAMAGE_DEALT_ABSORBED, Math.round(f2 * 10.0F));
            }

            if (f > 0 || !human) {
                if (human) {
                    // PAIL: Be sure to drag all this code from the EntityHuman subclass each update.
                    ((EntityHuman) this).causeFoodExhaustion(damagesource.getFoodExhaustion(), org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.DAMAGED); // CraftBukkit - EntityExhaustionEvent
                    if (f < 3.4028235E37F) {
                        ((EntityHuman) this).awardStat(StatisticList.DAMAGE_TAKEN, Math.round(f * 10.0F));
                    }
                }
                // CraftBukkit end
                float f3 = this.getHealth();

                this.setHealth(f3 - f);
                this.getCombatTracker().recordDamage(damagesource, f3, f);
                // CraftBukkit start
                if (!human) {
                    this.setAbsorptionAmount(this.getAbsorptionAmount() - f);
                }
                this.gameEvent(GameEvent.ENTITY_DAMAGED, damagesource.getEntity());

                return true;
            } else {
                // Duplicate triggers if blocking
                if (event.getDamage(DamageModifier.BLOCKING) < 0) {
                    if (this instanceof EntityPlayer) {
                        CriterionTriggers.ENTITY_HURT_PLAYER.trigger((EntityPlayer) this, damagesource, f, originalDamage, true);
                        f2 = (float) -event.getDamage(DamageModifier.BLOCKING);
                        if (f2 > 0.0F && f2 < 3.4028235E37F) {
                            ((EntityPlayer) this).awardStat(StatisticList.DAMAGE_BLOCKED_BY_SHIELD, Math.round(originalDamage * 10.0F));
                        }
                    }

                    if (damagesource.getEntity() instanceof EntityPlayer) {
                        CriterionTriggers.PLAYER_HURT_ENTITY.trigger((EntityPlayer) damagesource.getEntity(), this, damagesource, f, originalDamage, true);
                    }

                    return false;
                } else {
                    return originalDamage > 0;
                }
                // CraftBukkit end
            }
        }
        return false; // CraftBukkit
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    @Nullable
    public EntityLiving getKillCredit() {
        return (EntityLiving) (this.combatTracker.getKiller() != null ? this.combatTracker.getKiller() : (this.lastHurtByPlayer != null ? this.lastHurtByPlayer : (this.lastHurtByMob != null ? this.lastHurtByMob : null)));
    }

    public final float getMaxHealth() {
        return (float) this.getAttributeValue(GenericAttributes.MAX_HEALTH);
    }

    public final int getArrowCount() {
        return (Integer) this.entityData.get(EntityLiving.DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int i) {
        // CraftBukkit start
        setArrowCount(i, false);
    }

    public final void setArrowCount(int i, boolean flag) {
        ArrowBodyCountChangeEvent event = CraftEventFactory.callArrowBodyCountChangeEvent(this, getArrowCount(), i, flag);
        if (event.isCancelled()) {
            return;
        }
        this.entityData.set(EntityLiving.DATA_ARROW_COUNT_ID, event.getNewAmount());
    }
    // CraftBukkit end

    public final int getStingerCount() {
        return (Integer) this.entityData.get(EntityLiving.DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int i) {
        this.entityData.set(EntityLiving.DATA_STINGER_COUNT_ID, i);
    }

    private int getCurrentSwingDuration() {
        return MobEffectUtil.hasDigSpeed(this) ? 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this)) : (this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6);
    }

    public void swing(EnumHand enumhand) {
        this.swing(enumhand, false);
    }

    public void swing(EnumHand enumhand, boolean flag) {
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = enumhand;
            if (this.level instanceof WorldServer) {
                PacketPlayOutAnimation packetplayoutanimation = new PacketPlayOutAnimation(this, enumhand == EnumHand.MAIN_HAND ? 0 : 3);
                ChunkProviderServer chunkproviderserver = ((WorldServer) this.level).getChunkSource();

                if (flag) {
                    chunkproviderserver.broadcastAndSend(this, packetplayoutanimation);
                } else {
                    chunkproviderserver.broadcast(this, packetplayoutanimation);
                }
            }
        }

    }

    @Override
    public void handleEntityEvent(byte b0) {
        switch (b0) {
            case 2:
            case 33:
            case 36:
            case 37:
            case 44:
            case 57:
                this.animationSpeed = 1.5F;
                this.invulnerableTime = 20;
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
                this.hurtDir = 0.0F;
                if (b0 == 33) {
                    this.playSound(SoundEffects.THORNS_HIT, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                DamageSource damagesource;

                if (b0 == 37) {
                    damagesource = DamageSource.ON_FIRE;
                } else if (b0 == 36) {
                    damagesource = DamageSource.DROWN;
                } else if (b0 == 44) {
                    damagesource = DamageSource.SWEET_BERRY_BUSH;
                } else if (b0 == 57) {
                    damagesource = DamageSource.FREEZE;
                } else {
                    damagesource = DamageSource.GENERIC;
                }

                SoundEffect soundeffect = this.getHurtSound(damagesource);

                if (soundeffect != null) {
                    this.playSound(soundeffect, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                this.hurt(DamageSource.GENERIC, 0.0F);
                this.lastDamageSource = damagesource;
                this.lastDamageStamp = this.level.getGameTime();
                break;
            case 3:
                SoundEffect soundeffect1 = this.getDeathSound();

                if (soundeffect1 != null) {
                    this.playSound(soundeffect1, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                if (!(this instanceof EntityHuman)) {
                    this.setHealth(0.0F);
                    this.die(DamageSource.GENERIC);
                }
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 31:
            case 32:
            case 34:
            case 35:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 45:
            case 53:
            case 56:
            case 58:
            case 59:
            default:
                super.handleEntityEvent(b0);
                break;
            case 29:
                this.playSound(SoundEffects.SHIELD_BLOCK, 1.0F, 0.8F + this.level.random.nextFloat() * 0.4F);
                break;
            case 30:
                this.playSound(SoundEffects.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
                break;
            case 46:
                boolean flag = true;

                for (int i = 0; i < 128; ++i) {
                    double d0 = (double) i / 127.0D;
                    float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    double d1 = MathHelper.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth() * 2.0D;
                    double d2 = MathHelper.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * (double) this.getBbHeight();
                    double d3 = MathHelper.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth() * 2.0D;

                    this.level.addParticle(Particles.PORTAL, d1, d2, d3, (double) f, (double) f1, (double) f2);
                }

                return;
            case 47:
                this.breakItem(this.getItemBySlot(EnumItemSlot.MAINHAND));
                break;
            case 48:
                this.breakItem(this.getItemBySlot(EnumItemSlot.OFFHAND));
                break;
            case 49:
                this.breakItem(this.getItemBySlot(EnumItemSlot.HEAD));
                break;
            case 50:
                this.breakItem(this.getItemBySlot(EnumItemSlot.CHEST));
                break;
            case 51:
                this.breakItem(this.getItemBySlot(EnumItemSlot.LEGS));
                break;
            case 52:
                this.breakItem(this.getItemBySlot(EnumItemSlot.FEET));
                break;
            case 54:
                BlockHoney.showJumpParticles(this);
                break;
            case 55:
                this.swapHandItems();
                break;
            case 60:
                this.makePoofParticles();
        }

    }

    private void makePoofParticles() {
        for (int i = 0; i < 20; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;

            this.level.addParticle(Particles.POOF, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

    private void swapHandItems() {
        ItemStack itemstack = this.getItemBySlot(EnumItemSlot.OFFHAND);

        this.setItemSlot(EnumItemSlot.OFFHAND, this.getItemBySlot(EnumItemSlot.MAINHAND));
        this.setItemSlot(EnumItemSlot.MAINHAND, itemstack);
    }

    @Override
    protected void outOfWorld() {
        this.hurt(DamageSource.OUT_OF_WORLD, 4.0F);
    }

    protected void updateSwingTime() {
        int i = this.getCurrentSwingDuration();

        if (this.swinging) {
            ++this.swingTime;
            if (this.swingTime >= i) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float) this.swingTime / (float) i;
    }

    @Nullable
    public AttributeModifiable getAttribute(AttributeBase attributebase) {
        return this.getAttributes().getInstance(attributebase);
    }

    public double getAttributeValue(AttributeBase attributebase) {
        return this.getAttributes().getValue(attributebase);
    }

    public double getAttributeBaseValue(AttributeBase attributebase) {
        return this.getAttributes().getBaseValue(attributebase);
    }

    public AttributeMapBase getAttributes() {
        return this.attributes;
    }

    public EnumMonsterType getMobType() {
        return EnumMonsterType.UNDEFINED;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EnumItemSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EnumItemSlot.OFFHAND);
    }

    public boolean isHolding(Item item) {
        return this.isHolding((itemstack) -> {
            return itemstack.is(item);
        });
    }

    public boolean isHolding(Predicate<ItemStack> predicate) {
        return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(EnumHand enumhand) {
        if (enumhand == EnumHand.MAIN_HAND) {
            return this.getItemBySlot(EnumItemSlot.MAINHAND);
        } else if (enumhand == EnumHand.OFF_HAND) {
            return this.getItemBySlot(EnumItemSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + enumhand);
        }
    }

    public void setItemInHand(EnumHand enumhand, ItemStack itemstack) {
        if (enumhand == EnumHand.MAIN_HAND) {
            this.setItemSlot(EnumItemSlot.MAINHAND, itemstack);
        } else {
            if (enumhand != EnumHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + enumhand);
            }

            this.setItemSlot(EnumItemSlot.OFFHAND, itemstack);
        }

    }

    public boolean hasItemInSlot(EnumItemSlot enumitemslot) {
        return !this.getItemBySlot(enumitemslot).isEmpty();
    }

    @Override
    public abstract Iterable<ItemStack> getArmorSlots();

    public abstract ItemStack getItemBySlot(EnumItemSlot enumitemslot);

    // CraftBukkit start
    public void setItemSlot(EnumItemSlot enumitemslot, ItemStack itemstack, boolean silent) {
        this.setItemSlot(enumitemslot, itemstack);
    }
    // CraftBukkit end

    @Override
    public abstract void setItemSlot(EnumItemSlot enumitemslot, ItemStack itemstack);

    protected void verifyEquippedItem(ItemStack itemstack) {
        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null) {
            itemstack.getItem().verifyTagAfterLoad(nbttagcompound);
        }

    }

    public float getArmorCoverPercentage() {
        Iterable<ItemStack> iterable = this.getArmorSlots();
        int i = 0;
        int j = 0;

        for (Iterator iterator = iterable.iterator(); iterator.hasNext(); ++i) {
            ItemStack itemstack = (ItemStack) iterator.next();

            if (!itemstack.isEmpty()) {
                ++j;
            }
        }

        return i > 0 ? (float) j / (float) i : 0.0F;
    }

    @Override
    public void setSprinting(boolean flag) {
        super.setSprinting(flag);
        AttributeModifiable attributemodifiable = this.getAttribute(GenericAttributes.MOVEMENT_SPEED);

        if (attributemodifiable.getModifier(EntityLiving.SPEED_MODIFIER_SPRINTING_UUID) != null) {
            attributemodifiable.removeModifier(EntityLiving.SPEED_MODIFIER_SPRINTING);
        }

        if (flag) {
            attributemodifiable.addTransientModifier(EntityLiving.SPEED_MODIFIER_SPRINTING);
        }

    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public void push(Entity entity) {
        if (!this.isSleeping()) {
            super.push(entity);
        }

    }

    private void dismountVehicle(Entity entity) {
        Vec3D vec3d;

        if (this.isRemoved()) {
            vec3d = this.position();
        } else if (!entity.isRemoved() && !this.level.getBlockState(entity.blockPosition()).is((Tag) TagsBlock.PORTALS)) {
            vec3d = entity.getDismountLocationForPassenger(this);
        } else {
            double d0 = Math.max(this.getY(), entity.getY());

            vec3d = new Vec3D(this.getX(), d0, this.getZ());
        }

        this.dismountTo(vec3d.x, vec3d.y, vec3d.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return 0.42F * this.getBlockJumpFactor();
    }

    public double getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP) ? (double) (0.1F * (float) (this.getEffect(MobEffects.JUMP).getAmplifier() + 1)) : 0.0D;
    }

    protected void jumpFromGround() {
        double d0 = (double) this.getJumpPower() + this.getJumpBoostPower();
        Vec3D vec3d = this.getDeltaMovement();

        this.setDeltaMovement(vec3d.x, d0, vec3d.z);
        if (this.isSprinting()) {
            float f = this.getYRot() * 0.017453292F;

            this.setDeltaMovement(this.getDeltaMovement().add((double) (-MathHelper.sin(f) * 0.2F), 0.0D, (double) (MathHelper.cos(f) * 0.2F)));
        }

        this.hasImpulse = true;
    }

    protected void goDownInWater() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03999999910593033D, 0.0D));
    }

    protected void jumpInLiquid(Tag<FluidType> tag) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.03999999910593033D, 0.0D));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidType fluidtype) {
        return false;
    }

    public void travel(Vec3D vec3d) {
        if (this.isEffectiveAi() || this.isControlledByLocalInstance()) {
            double d0 = 0.08D;
            boolean flag = this.getDeltaMovement().y <= 0.0D;

            if (flag && this.hasEffect(MobEffects.SLOW_FALLING)) {
                d0 = 0.01D;
                this.resetFallDistance();
            }

            Fluid fluid = this.level.getFluidState(this.blockPosition());
            double d1;
            float f;

            if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluid.getType())) {
                d1 = this.getY();
                f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
                float f1 = 0.02F;
                float f2 = (float) EnchantmentManager.getDepthStrider(this);

                if (f2 > 3.0F) {
                    f2 = 3.0F;
                }

                if (!this.onGround) {
                    f2 *= 0.5F;
                }

                if (f2 > 0.0F) {
                    f += (0.54600006F - f) * f2 / 3.0F;
                    f1 += (this.getSpeed() - f1) * f2 / 3.0F;
                }

                if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                    f = 0.96F;
                }

                this.moveRelative(f1, vec3d);
                this.move(EnumMoveType.SELF, this.getDeltaMovement());
                Vec3D vec3d1 = this.getDeltaMovement();

                if (this.horizontalCollision && this.onClimbable()) {
                    vec3d1 = new Vec3D(vec3d1.x, 0.2D, vec3d1.z);
                }

                this.setDeltaMovement(vec3d1.multiply((double) f, 0.800000011920929D, (double) f));
                Vec3D vec3d2 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());

                this.setDeltaMovement(vec3d2);
                if (this.horizontalCollision && this.isFree(vec3d2.x, vec3d2.y + 0.6000000238418579D - this.getY() + d1, vec3d2.z)) {
                    this.setDeltaMovement(vec3d2.x, 0.30000001192092896D, vec3d2.z);
                }
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluid.getType())) {
                d1 = this.getY();
                this.moveRelative(0.02F, vec3d);
                this.move(EnumMoveType.SELF, this.getDeltaMovement());
                Vec3D vec3d3;

                if (this.getFluidHeight(TagsFluid.LAVA) <= this.getFluidJumpThreshold()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.800000011920929D, 0.5D));
                    vec3d3 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                    this.setDeltaMovement(vec3d3);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d0 / 4.0D, 0.0D));
                }

                vec3d3 = this.getDeltaMovement();
                if (this.horizontalCollision && this.isFree(vec3d3.x, vec3d3.y + 0.6000000238418579D - this.getY() + d1, vec3d3.z)) {
                    this.setDeltaMovement(vec3d3.x, 0.30000001192092896D, vec3d3.z);
                }
            } else if (this.isFallFlying()) {
                Vec3D vec3d4 = this.getDeltaMovement();

                if (vec3d4.y > -0.5D) {
                    this.fallDistance = 1.0F;
                }

                Vec3D vec3d5 = this.getLookAngle();

                f = this.getXRot() * 0.017453292F;
                double d2 = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
                double d3 = vec3d4.horizontalDistance();
                double d4 = vec3d5.length();
                float f3 = MathHelper.cos(f);

                f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
                vec3d4 = this.getDeltaMovement().add(0.0D, d0 * (-1.0D + (double) f3 * 0.75D), 0.0D);
                double d5;

                if (vec3d4.y < 0.0D && d2 > 0.0D) {
                    d5 = vec3d4.y * -0.1D * (double) f3;
                    vec3d4 = vec3d4.add(vec3d5.x * d5 / d2, d5, vec3d5.z * d5 / d2);
                }

                if (f < 0.0F && d2 > 0.0D) {
                    d5 = d3 * (double) (-MathHelper.sin(f)) * 0.04D;
                    vec3d4 = vec3d4.add(-vec3d5.x * d5 / d2, d5 * 3.2D, -vec3d5.z * d5 / d2);
                }

                if (d2 > 0.0D) {
                    vec3d4 = vec3d4.add((vec3d5.x / d2 * d3 - vec3d4.x) * 0.1D, 0.0D, (vec3d5.z / d2 * d3 - vec3d4.z) * 0.1D);
                }

                this.setDeltaMovement(vec3d4.multiply(0.9900000095367432D, 0.9800000190734863D, 0.9900000095367432D));
                this.move(EnumMoveType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level.isClientSide) {
                    d5 = this.getDeltaMovement().horizontalDistance();
                    double d6 = d3 - d5;
                    float f4 = (float) (d6 * 10.0D - 3.0D);

                    if (f4 > 0.0F) {
                        this.playSound(this.getFallDamageSound((int) f4), 1.0F, 1.0F);
                        this.hurt(DamageSource.FLY_INTO_WALL, f4);
                    }
                }

                if (this.onGround && !this.level.isClientSide) {
                    if (getSharedFlag(7) && !CraftEventFactory.callToggleGlideEvent(this, false).isCancelled()) // CraftBukkit
                    this.setSharedFlag(7, false);
                }
            } else {
                BlockPosition blockposition = this.getBlockPosBelowThatAffectsMyMovement();
                float f5 = this.level.getBlockState(blockposition).getBlock().getFriction();

                f = this.onGround ? f5 * 0.91F : 0.91F;
                Vec3D vec3d6 = this.handleRelativeFrictionAndCalculateMovement(vec3d, f5);
                double d7 = vec3d6.y;

                if (this.hasEffect(MobEffects.LEVITATION)) {
                    d7 += (0.05D * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec3d6.y) * 0.2D;
                    this.resetFallDistance();
                } else if (this.level.isClientSide && !this.level.hasChunkAt(blockposition)) {
                    if (this.getY() > (double) this.level.getMinBuildHeight()) {
                        d7 = -0.1D;
                    } else {
                        d7 = 0.0D;
                    }
                } else if (!this.isNoGravity()) {
                    d7 -= d0;
                }

                if (this.shouldDiscardFriction()) {
                    this.setDeltaMovement(vec3d6.x, d7, vec3d6.z);
                } else {
                    this.setDeltaMovement(vec3d6.x * (double) f, d7 * 0.9800000190734863D, vec3d6.z * (double) f);
                }
            }
        }

        this.calculateEntityAnimation(this, this instanceof EntityBird);
    }

    public void calculateEntityAnimation(EntityLiving entityliving, boolean flag) {
        entityliving.animationSpeedOld = entityliving.animationSpeed;
        double d0 = entityliving.getX() - entityliving.xo;
        double d1 = flag ? entityliving.getY() - entityliving.yo : 0.0D;
        double d2 = entityliving.getZ() - entityliving.zo;
        float f = (float) Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;

        if (f > 1.0F) {
            f = 1.0F;
        }

        entityliving.animationSpeed += (f - entityliving.animationSpeed) * 0.4F;
        entityliving.animationPosition += entityliving.animationSpeed;
    }

    public Vec3D handleRelativeFrictionAndCalculateMovement(Vec3D vec3d, float f) {
        this.moveRelative(this.getFrictionInfluencedSpeed(f), vec3d);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(EnumMoveType.SELF, this.getDeltaMovement());
        Vec3D vec3d1 = this.getDeltaMovement();

        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            vec3d1 = new Vec3D(vec3d1.x, 0.2D, vec3d1.z);
        }

        return vec3d1;
    }

    public Vec3D getFluidFallingAdjustedMovement(double d0, boolean flag, Vec3D vec3d) {
        if (!this.isNoGravity() && !this.isSprinting()) {
            double d1;

            if (flag && Math.abs(vec3d.y - 0.005D) >= 0.003D && Math.abs(vec3d.y - d0 / 16.0D) < 0.003D) {
                d1 = -0.003D;
            } else {
                d1 = vec3d.y - d0 / 16.0D;
            }

            return new Vec3D(vec3d.x, d1, vec3d.z);
        } else {
            return vec3d;
        }
    }

    private Vec3D handleOnClimbable(Vec3D vec3d) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float f = 0.15F;
            double d0 = MathHelper.clamp(vec3d.x, -0.15000000596046448D, 0.15000000596046448D);
            double d1 = MathHelper.clamp(vec3d.z, -0.15000000596046448D, 0.15000000596046448D);
            double d2 = Math.max(vec3d.y, -0.15000000596046448D);

            if (d2 < 0.0D && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof EntityHuman) {
                d2 = 0.0D;
            }

            vec3d = new Vec3D(d0, d2, d1);
        }

        return vec3d;
    }

    private float getFrictionInfluencedSpeed(float f) {
        return this.onGround ? this.getSpeed() * (0.21600002F / (f * f * f)) : this.flyingSpeed;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float f) {
        this.speed = f;
    }

    public boolean doHurtTarget(Entity entity) {
        this.setLastHurtMob(entity);
        return false;
    }

    @Override
    public void tick() {
        SpigotTimings.timerEntityBaseTick.startTiming(); // Spigot
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level.isClientSide) {
            int i = this.getArrowCount();

            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                --this.removeArrowTime;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();

            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                --this.removeStingerTime;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && !this.checkBedExists()) {
                this.stopSleeping();
            }
        }

        SpigotTimings.timerEntityBaseTick.stopTiming(); // Spigot
        this.aiStep();
        SpigotTimings.timerEntityTickRest.startTiming(); // Spigot
        double d0 = this.getX() - this.xo;
        double d1 = this.getZ() - this.zo;
        float f = (float) (d0 * d0 + d1 * d1);
        float f1 = this.yBodyRot;
        float f2 = 0.0F;

        this.oRun = this.run;
        float f3 = 0.0F;

        if (f > 0.0025000002F) {
            f3 = 1.0F;
            f2 = (float) Math.sqrt((double) f) * 3.0F;
            float f4 = (float) MathHelper.atan2(d1, d0) * 57.295776F - 90.0F;
            float f5 = MathHelper.abs(MathHelper.wrapDegrees(this.getYRot()) - f4);

            if (95.0F < f5 && f5 < 265.0F) {
                f1 = f4 - 180.0F;
            } else {
                f1 = f4;
            }
        }

        if (this.attackAnim > 0.0F) {
            f1 = this.getYRot();
        }

        if (!this.onGround) {
            f3 = 0.0F;
        }

        this.run += (f3 - this.run) * 0.3F;
        this.level.getProfiler().push("headTurn");
        f2 = this.tickHeadTurn(f1, f2);
        this.level.getProfiler().pop();
        this.level.getProfiler().push("rangeChecks");

        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        this.level.getProfiler().pop();
        this.animStep += f2;
        if (this.isFallFlying()) {
            ++this.fallFlyTicks;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

        SpigotTimings.timerEntityTickRest.stopTiming(); // Spigot
    }

    public void detectEquipmentUpdates() {
        Map<EnumItemSlot, ItemStack> map = this.collectEquipmentChanges();

        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }

    }

    @Nullable
    private Map<EnumItemSlot, ItemStack> collectEquipmentChanges() {
        Map<EnumItemSlot, ItemStack> map = null;
        EnumItemSlot[] aenumitemslot = EnumItemSlot.values();
        int i = aenumitemslot.length;

        for (int j = 0; j < i; ++j) {
            EnumItemSlot enumitemslot = aenumitemslot[j];
            ItemStack itemstack;

            switch (enumitemslot.getType()) {
                case HAND:
                    itemstack = this.getLastHandItem(enumitemslot);
                    break;
                case ARMOR:
                    itemstack = this.getLastArmorItem(enumitemslot);
                    break;
                default:
                    continue;
            }

            ItemStack itemstack1 = this.getItemBySlot(enumitemslot);

            if (!ItemStack.matches(itemstack1, itemstack)) {
                if (map == null) {
                    map = Maps.newEnumMap(EnumItemSlot.class);
                }

                map.put(enumitemslot, itemstack1);
                if (!itemstack.isEmpty()) {
                    this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(enumitemslot));
                }

                if (!itemstack1.isEmpty()) {
                    this.getAttributes().addTransientAttributeModifiers(itemstack1.getAttributeModifiers(enumitemslot));
                }
            }
        }

        return map;
    }

    private void handleHandSwap(Map<EnumItemSlot, ItemStack> map) {
        ItemStack itemstack = (ItemStack) map.get(EnumItemSlot.MAINHAND);
        ItemStack itemstack1 = (ItemStack) map.get(EnumItemSlot.OFFHAND);

        if (itemstack != null && itemstack1 != null && ItemStack.matches(itemstack, this.getLastHandItem(EnumItemSlot.OFFHAND)) && ItemStack.matches(itemstack1, this.getLastHandItem(EnumItemSlot.MAINHAND))) {
            ((WorldServer) this.level).getChunkSource().broadcast(this, new PacketPlayOutEntityStatus(this, (byte) 55));
            map.remove(EnumItemSlot.MAINHAND);
            map.remove(EnumItemSlot.OFFHAND);
            this.setLastHandItem(EnumItemSlot.MAINHAND, itemstack.copy());
            this.setLastHandItem(EnumItemSlot.OFFHAND, itemstack1.copy());
        }

    }

    private void handleEquipmentChanges(Map<EnumItemSlot, ItemStack> map) {
        List<Pair<EnumItemSlot, ItemStack>> list = Lists.newArrayListWithCapacity(map.size());

        map.forEach((enumitemslot, itemstack) -> {
            ItemStack itemstack1 = itemstack.copy();

            list.add(Pair.of(enumitemslot, itemstack1));
            switch (enumitemslot.getType()) {
                case HAND:
                    this.setLastHandItem(enumitemslot, itemstack1);
                    break;
                case ARMOR:
                    this.setLastArmorItem(enumitemslot, itemstack1);
            }

        });
        ((WorldServer) this.level).getChunkSource().broadcast(this, new PacketPlayOutEntityEquipment(this.getId(), list));
    }

    private ItemStack getLastArmorItem(EnumItemSlot enumitemslot) {
        return (ItemStack) this.lastArmorItemStacks.get(enumitemslot.getIndex());
    }

    private void setLastArmorItem(EnumItemSlot enumitemslot, ItemStack itemstack) {
        this.lastArmorItemStacks.set(enumitemslot.getIndex(), itemstack);
    }

    private ItemStack getLastHandItem(EnumItemSlot enumitemslot) {
        return (ItemStack) this.lastHandItemStacks.get(enumitemslot.getIndex());
    }

    private void setLastHandItem(EnumItemSlot enumitemslot, ItemStack itemstack) {
        this.lastHandItemStacks.set(enumitemslot.getIndex(), itemstack);
    }

    protected float tickHeadTurn(float f, float f1) {
        float f2 = MathHelper.wrapDegrees(f - this.yBodyRot);

        this.yBodyRot += f2 * 0.3F;
        float f3 = MathHelper.wrapDegrees(this.getYRot() - this.yBodyRot);
        boolean flag = f3 < -90.0F || f3 >= 90.0F;

        if (f3 < -75.0F) {
            f3 = -75.0F;
        }

        if (f3 >= 75.0F) {
            f3 = 75.0F;
        }

        this.yBodyRot = this.getYRot() - f3;
        if (f3 * f3 > 2500.0F) {
            this.yBodyRot += f3 * 0.2F;
        }

        if (flag) {
            f1 *= -1.0F;
        }

        return f1;
    }

    public void aiStep() {
        if (this.noJumpDelay > 0) {
            --this.noJumpDelay;
        }

        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.setPacketCoordinates(this.getX(), this.getY(), this.getZ());
        }

        if (this.lerpSteps > 0) {
            double d0 = this.getX() + (this.lerpX - this.getX()) / (double) this.lerpSteps;
            double d1 = this.getY() + (this.lerpY - this.getY()) / (double) this.lerpSteps;
            double d2 = this.getZ() + (this.lerpZ - this.getZ()) / (double) this.lerpSteps;
            double d3 = MathHelper.wrapDegrees(this.lerpYRot - (double) this.getYRot());

            this.setYRot(this.getYRot() + (float) d3 / (float) this.lerpSteps);
            this.setXRot(this.getXRot() + (float) (this.lerpXRot - (double) this.getXRot()) / (float) this.lerpSteps);
            --this.lerpSteps;
            this.setPos(d0, d1, d2);
            this.setRot(this.getYRot(), this.getXRot());
        } else if (!this.isEffectiveAi()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        }

        if (this.lerpHeadSteps > 0) {
            this.yHeadRot = (float) ((double) this.yHeadRot + MathHelper.wrapDegrees(this.lyHeadRot - (double) this.yHeadRot) / (double) this.lerpHeadSteps);
            --this.lerpHeadSteps;
        }

        Vec3D vec3d = this.getDeltaMovement();
        double d4 = vec3d.x;
        double d5 = vec3d.y;
        double d6 = vec3d.z;

        if (Math.abs(vec3d.x) < 0.003D) {
            d4 = 0.0D;
        }

        if (Math.abs(vec3d.y) < 0.003D) {
            d5 = 0.0D;
        }

        if (Math.abs(vec3d.z) < 0.003D) {
            d6 = 0.0D;
        }

        this.setDeltaMovement(d4, d5, d6);
        this.level.getProfiler().push("ai");
        SpigotTimings.timerEntityAI.startTiming(); // Spigot
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi()) {
            this.level.getProfiler().push("newAi");
            this.serverAiStep();
            this.level.getProfiler().pop();
        }
        SpigotTimings.timerEntityAI.stopTiming(); // Spigot

        this.level.getProfiler().pop();
        this.level.getProfiler().push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double d7;

            if (this.isInLava()) {
                d7 = this.getFluidHeight(TagsFluid.LAVA);
            } else {
                d7 = this.getFluidHeight(TagsFluid.WATER);
            }

            boolean flag = this.isInWater() && d7 > 0.0D;
            double d8 = this.getFluidJumpThreshold();

            if (flag && (!this.onGround || d7 > d8)) {
                this.jumpInLiquid(TagsFluid.WATER);
            } else if (this.isInLava() && (!this.onGround || d7 > d8)) {
                this.jumpInLiquid(TagsFluid.LAVA);
            } else if ((this.onGround || flag && d7 <= d8) && this.noJumpDelay == 0) {
                this.jumpFromGround();
                this.noJumpDelay = 10;
            }
        } else {
            this.noJumpDelay = 0;
        }

        this.level.getProfiler().pop();
        this.level.getProfiler().push("travel");
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
        this.updateFallFlying();
        AxisAlignedBB axisalignedbb = this.getBoundingBox();

        SpigotTimings.timerEntityAIMove.startTiming(); // Spigot
        this.travel(new Vec3D((double) this.xxa, (double) this.yya, (double) this.zza));
        SpigotTimings.timerEntityAIMove.stopTiming(); // Spigot
        this.level.getProfiler().pop();
        this.level.getProfiler().push("freezing");
        boolean flag1 = this.getType().is(TagsEntity.FREEZE_HURTS_EXTRA_TYPES);
        int i;

        if (!this.level.isClientSide && !this.isDeadOrDying()) {
            i = this.getTicksFrozen();
            if (this.isInPowderSnow && this.canFreeze()) {
                this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
            } else {
                this.setTicksFrozen(Math.max(0, i - 2));
            }
        }

        this.removeFrost();
        this.tryAddFrost();
        if (!this.level.isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            i = flag1 ? 5 : 1;
            this.hurt(DamageSource.FREEZE, (float) i);
        }

        this.level.getProfiler().pop();
        this.level.getProfiler().push("push");
        if (this.autoSpinAttackTicks > 0) {
            --this.autoSpinAttackTicks;
            this.checkAutoSpinAttack(axisalignedbb, this.getBoundingBox());
        }

        SpigotTimings.timerEntityAICollision.startTiming(); // Spigot
        this.pushEntities();
        SpigotTimings.timerEntityAICollision.stopTiming(); // Spigot
        this.level.getProfiler().pop();
        if (!this.level.isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
            this.hurt(DamageSource.DROWN, 1.0F);
        }

    }

    public boolean isSensitiveToWater() {
        return false;
    }

    private void updateFallFlying() {
        boolean flag = this.getSharedFlag(7);

        if (flag && !this.onGround && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            ItemStack itemstack = this.getItemBySlot(EnumItemSlot.CHEST);

            if (itemstack.is(Items.ELYTRA) && ItemElytra.isFlyEnabled(itemstack)) {
                flag = true;
                int i = this.fallFlyTicks + 1;

                if (!this.level.isClientSide && i % 10 == 0) {
                    int j = i / 10;

                    if (j % 2 == 0) {
                        itemstack.hurtAndBreak(1, this, (entityliving) -> {
                            entityliving.broadcastBreakEvent(EnumItemSlot.CHEST);
                        });
                    }

                    this.gameEvent(GameEvent.ELYTRA_FREE_FALL);
                }
            } else {
                flag = false;
            }
        } else {
            flag = false;
        }

        if (!this.level.isClientSide) {
            if (flag != this.getSharedFlag(7) && !CraftEventFactory.callToggleGlideEvent(this, flag).isCancelled()) // CraftBukkit
            this.setSharedFlag(7, flag);
        }

    }

    protected void serverAiStep() {}

    protected void pushEntities() {
        List<Entity> list = this.level.getEntities((Entity) this, this.getBoundingBox(), IEntitySelector.pushableBy(this));

        if (!list.isEmpty()) {
            int i = this.level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            int j;

            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                j = 0;

                for (int k = 0; k < list.size(); ++k) {
                    if (!((Entity) list.get(k)).isPassenger()) {
                        ++j;
                    }
                }

                if (j > i - 1) {
                    this.hurt(DamageSource.CRAMMING, 6.0F);
                }
            }

            for (j = 0; j < list.size(); ++j) {
                Entity entity = (Entity) list.get(j);

                this.doPush(entity);
            }
        }

    }

    protected void checkAutoSpinAttack(AxisAlignedBB axisalignedbb, AxisAlignedBB axisalignedbb1) {
        AxisAlignedBB axisalignedbb2 = axisalignedbb.minmax(axisalignedbb1);
        List<Entity> list = this.level.getEntities(this, axisalignedbb2);

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); ++i) {
                Entity entity = (Entity) list.get(i);

                if (entity instanceof EntityLiving) {
                    this.doAutoAttackOnTouch((EntityLiving) entity);
                    this.autoSpinAttackTicks = 0;
                    this.setDeltaMovement(this.getDeltaMovement().scale(-0.2D));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level.isClientSide && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
        }

    }

    protected void doPush(Entity entity) {
        entity.push(this);
    }

    protected void doAutoAttackOnTouch(EntityLiving entityliving) {}

    public void startAutoSpinAttack(int i) {
        this.autoSpinAttackTicks = i;
        if (!this.level.isClientSide) {
            this.setLivingEntityFlag(4, true);
        }

    }

    public boolean isAutoSpinAttack() {
        return ((Byte) this.entityData.get(EntityLiving.DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();

        super.stopRiding();
        if (entity != null && entity != this.getVehicle() && !this.level.isClientSide) {
            this.dismountVehicle(entity);
        }

    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.oRun = this.run;
        this.run = 0.0F;
        this.resetFallDistance();
    }

    @Override
    public void lerpTo(double d0, double d1, double d2, float f, float f1, int i, boolean flag) {
        this.lerpX = d0;
        this.lerpY = d1;
        this.lerpZ = d2;
        this.lerpYRot = (double) f;
        this.lerpXRot = (double) f1;
        this.lerpSteps = i;
    }

    @Override
    public void lerpHeadTo(float f, int i) {
        this.lyHeadRot = (double) f;
        this.lerpHeadSteps = i;
    }

    public void setJumping(boolean flag) {
        this.jumping = flag;
    }

    public void onItemPickup(EntityItem entityitem) {
        EntityHuman entityhuman = entityitem.getThrower() != null ? this.level.getPlayerByUUID(entityitem.getThrower()) : null;

        if (entityhuman instanceof EntityPlayer) {
            CriterionTriggers.ITEM_PICKED_UP_BY_ENTITY.trigger((EntityPlayer) entityhuman, entityitem.getItem(), this);
        }

    }

    public void take(Entity entity, int i) {
        if (!entity.isRemoved() && !this.level.isClientSide && (entity instanceof EntityItem || entity instanceof EntityArrow || entity instanceof EntityExperienceOrb)) {
            ((WorldServer) this.level).getChunkSource().broadcast(entity, new PacketPlayOutCollect(entity.getId(), this.getId(), i));
        }

    }

    public boolean hasLineOfSight(Entity entity) {
        if (entity.level != this.level) {
            return false;
        } else {
            Vec3D vec3d = new Vec3D(this.getX(), this.getEyeY(), this.getZ());
            Vec3D vec3d1 = new Vec3D(entity.getX(), entity.getEyeY(), entity.getZ());

            return vec3d1.distanceTo(vec3d) > 128.0D ? false : this.level.clip(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this)).getType() == MovingObjectPosition.EnumMovingObjectType.MISS;
        }
    }

    @Override
    public float getViewYRot(float f) {
        return f == 1.0F ? this.yHeadRot : MathHelper.lerp(f, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float f) {
        float f1 = this.attackAnim - this.oAttackAnim;

        if (f1 < 0.0F) {
            ++f1;
        }

        return this.oAttackAnim + f1 * f;
    }

    public boolean isEffectiveAi() {
        return !this.level.isClientSide;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved() && this.collides; // CraftBukkit
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable() && this.collides; // CraftBukkit
    }

    // CraftBukkit start - collidable API
    @Override
    public boolean canCollideWithBukkit(Entity entity) {
        return isPushable() && this.collides != this.collidableExemptions.contains(entity.getUUID());
    }
    // CraftBukkit end

    @Override
    protected void markHurt() {
        this.hurtMarked = this.random.nextDouble() >= this.getAttributeValue(GenericAttributes.KNOCKBACK_RESISTANCE);
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    @Override
    public void setYHeadRot(float f) {
        this.yHeadRot = f;
    }

    @Override
    public void setYBodyRot(float f) {
        this.yBodyRot = f;
    }

    @Override
    protected Vec3D getRelativePortalPosition(EnumDirection.EnumAxis enumdirection_enumaxis, BlockUtil.Rectangle blockutil_rectangle) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(enumdirection_enumaxis, blockutil_rectangle));
    }

    public static Vec3D resetForwardDirectionOfRelativePortalPosition(Vec3D vec3d) {
        return new Vec3D(vec3d.x, vec3d.y, 0.0D);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public void setAbsorptionAmount(float f) {
        if (f < 0.0F) {
            f = 0.0F;
        }

        this.absorptionAmount = f;
    }

    public void onEnterCombat() {}

    public void onLeaveCombat() {}

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract EnumMainHand getMainArm();

    public boolean isUsingItem() {
        return ((Byte) this.entityData.get(EntityLiving.DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public EnumHand getUsedItemHand() {
        return ((Byte) this.entityData.get(EntityLiving.DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            if (ItemStack.isSameIgnoreDurability(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }

    }

    protected void updateUsingItem(ItemStack itemstack) {
        itemstack.onUseTick(this.level, this, this.getUseItemRemainingTicks());
        if (this.shouldTriggerItemUseEffects()) {
            this.triggerItemUseEffects(itemstack, 5);
        }

        if (--this.useItemRemaining == 0 && !this.level.isClientSide && !itemstack.useOnRelease()) {
            this.completeUsingItem();
        }

    }

    private boolean shouldTriggerItemUseEffects() {
        int i = this.getUseItemRemainingTicks();
        FoodInfo foodinfo = this.useItem.getItem().getFoodProperties();
        boolean flag = foodinfo != null && foodinfo.isFastFood();

        flag |= i <= this.useItem.getUseDuration() - 7;
        return flag && i % 4 == 0;
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }

    }

    protected void setLivingEntityFlag(int i, boolean flag) {
        byte b0 = (Byte) this.entityData.get(EntityLiving.DATA_LIVING_ENTITY_FLAGS);
        int j;

        if (flag) {
            j = b0 | i;
        } else {
            j = b0 & ~i;
        }

        this.entityData.set(EntityLiving.DATA_LIVING_ENTITY_FLAGS, (byte) j);
    }

    public void startUsingItem(EnumHand enumhand) {
        ItemStack itemstack = this.getItemInHand(enumhand);

        if (!itemstack.isEmpty() && !this.isUsingItem()) {
            this.useItem = itemstack;
            this.useItemRemaining = itemstack.getUseDuration();
            if (!this.level.isClientSide) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, enumhand == EnumHand.OFF_HAND);
            }

        }
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> datawatcherobject) {
        super.onSyncedDataUpdated(datawatcherobject);
        if (EntityLiving.SLEEPING_POS_ID.equals(datawatcherobject)) {
            if (this.level.isClientSide) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (EntityLiving.DATA_LIVING_ENTITY_FLAGS.equals(datawatcherobject) && this.level.isClientSide) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration();
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }

    }

    @Override
    public void lookAt(ArgumentAnchor.Anchor argumentanchor_anchor, Vec3D vec3d) {
        super.lookAt(argumentanchor_anchor, vec3d);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    protected void triggerItemUseEffects(ItemStack itemstack, int i) {
        if (!itemstack.isEmpty() && this.isUsingItem()) {
            if (itemstack.getUseAnimation() == EnumAnimation.DRINK) {
                this.playSound(this.getDrinkingSound(itemstack), 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }

            if (itemstack.getUseAnimation() == EnumAnimation.EAT) {
                this.spawnItemParticles(itemstack, i);
                this.playSound(this.getEatingSound(itemstack), 0.5F + 0.5F * (float) this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

        }
    }

    private void spawnItemParticles(ItemStack itemstack, int i) {
        for (int j = 0; j < i; ++j) {
            Vec3D vec3d = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);

            vec3d = vec3d.xRot(-this.getXRot() * 0.017453292F);
            vec3d = vec3d.yRot(-this.getYRot() * 0.017453292F);
            double d0 = (double) (-this.random.nextFloat()) * 0.6D - 0.3D;
            Vec3D vec3d1 = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);

            vec3d1 = vec3d1.xRot(-this.getXRot() * 0.017453292F);
            vec3d1 = vec3d1.yRot(-this.getYRot() * 0.017453292F);
            vec3d1 = vec3d1.add(this.getX(), this.getEyeY(), this.getZ());
            this.level.addParticle(new ParticleParamItem(Particles.ITEM, itemstack), vec3d1.x, vec3d1.y, vec3d1.z, vec3d.x, vec3d.y + 0.05D, vec3d.z);
        }

    }

    protected void completeUsingItem() {
        if (!this.level.isClientSide || this.isUsingItem()) {
            EnumHand enumhand = this.getUsedItemHand();

            if (!this.useItem.equals(this.getItemInHand(enumhand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    this.triggerItemUseEffects(this.useItem, 16);
                    // CraftBukkit start - fire PlayerItemConsumeEvent
                    ItemStack itemstack;
                    if (this instanceof EntityPlayer) {
                        org.bukkit.inventory.ItemStack craftItem = CraftItemStack.asBukkitCopy(this.useItem);
                        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent((Player) this.getBukkitEntity(), craftItem);
                        level.getCraftServer().getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // Update client
                            ((EntityPlayer) this).getBukkitEntity().updateInventory();
                            ((EntityPlayer) this).getBukkitEntity().updateScaledHealth();
                            return;
                        }

                        itemstack = (craftItem.equals(event.getItem())) ? this.useItem.finishUsingItem(this.level, this) : CraftItemStack.asNMSCopy(event.getItem()).finishUsingItem(level, this);
                    } else {
                        itemstack = this.useItem.finishUsingItem(this.level, this);
                    }
                    // CraftBukkit end

                    if (itemstack != this.useItem) {
                        this.setItemInHand(enumhand, itemstack);
                    }

                    this.stopUsingItem();
                }

            }
        }
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isUsingItem() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
    }

    public void releaseUsingItem() {
        if (!this.useItem.isEmpty()) {
            this.useItem.releaseUsing(this.level, this, this.getUseItemRemainingTicks());
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (!this.level.isClientSide) {
            this.setLivingEntityFlag(1, false);
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        if (this.isUsingItem() && !this.useItem.isEmpty()) {
            Item item = this.useItem.getItem();

            return item.getUseAnimation(this.useItem) != EnumAnimation.BLOCK ? false : item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
        } else {
            return false;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.getPose() == EntityPose.FALL_FLYING;
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double d0, double d1, double d2, boolean flag) {
        // CraftBukkit start
        return randomTeleport(d0, d1, d2, flag, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN).orElse(false);
    }

    public Optional<Boolean> randomTeleport(double d0, double d1, double d2, boolean flag, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        // CraftBukkit end
        double d3 = this.getX();
        double d4 = this.getY();
        double d5 = this.getZ();
        double d6 = d1;
        boolean flag1 = false;
        BlockPosition blockposition = new BlockPosition(d0, d1, d2);
        World world = this.level;

        if (world.hasChunkAt(blockposition)) {
            boolean flag2 = false;

            while (!flag2 && blockposition.getY() > world.getMinBuildHeight()) {
                BlockPosition blockposition1 = blockposition.below();
                IBlockData iblockdata = world.getBlockState(blockposition1);

                if (iblockdata.getMaterial().blocksMotion()) {
                    flag2 = true;
                } else {
                    --d6;
                    blockposition = blockposition1;
                }
            }

            if (flag2) {
                // CraftBukkit start - Teleport event
                // this.teleportTo(d0, d6, d2);

                // first set position, to check if the place to teleport is valid
                this.setPos(d0, d6, d2);
                if (world.noCollision((Entity) this) && !world.containsAnyLiquid(this.getBoundingBox())) {
                    flag1 = true;
                }
                // now revert and call event if the teleport place is valid
                this.setPos(d3, d4, d5);

                if (flag1) {
                    if (!(this instanceof EntityPlayer)) {
                        EntityTeleportEvent teleport = new EntityTeleportEvent(this.getBukkitEntity(), new Location(this.level.getWorld(), d3, d4, d5), new Location(this.level.getWorld(), d0, d6, d2));
                        this.level.getCraftServer().getPluginManager().callEvent(teleport);
                        if (!teleport.isCancelled()) {
                            Location to = teleport.getTo();
                            this.teleportTo(to.getX(), to.getY(), to.getZ());
                        } else {
                            return Optional.empty();
                        }
                    } else {
                        // player teleport event is called in the underlining code
                        if (((EntityPlayer) this).connection.teleport(d0, d6, d2, this.getYRot(), this.getXRot(), java.util.Collections.emptySet(), false, cause)) {
                            return Optional.empty();
                        }
                    }
                }
                // CraftBukkit end
            }
        }

        if (!flag1) {
            // this.enderTeleportTo(d3, d4, d5); // CraftBukkit - already set the location back
            return Optional.of(false); // CraftBukkit
        } else {
            if (flag) {
                world.broadcastEntityEvent(this, (byte) 46);
            }

            if (this instanceof EntityCreature) {
                ((EntityCreature) this).getNavigation().stop();
            }

            return Optional.of(true); // CraftBukkit
        }
    }

    public boolean isAffectedByPotions() {
        return true;
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPosition blockposition, boolean flag) {}

    public boolean canTakeItem(ItemStack itemstack) {
        return false;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new PacketPlayOutSpawnEntityLiving(this);
    }

    @Override
    public EntitySize getDimensions(EntityPose entitypose) {
        return entitypose == EntityPose.SLEEPING ? EntityLiving.SLEEPING_DIMENSIONS : super.getDimensions(entitypose).scale(this.getScale());
    }

    public ImmutableList<EntityPose> getDismountPoses() {
        return ImmutableList.of(EntityPose.STANDING);
    }

    public AxisAlignedBB getLocalBoundsForPose(EntityPose entitypose) {
        EntitySize entitysize = this.getDimensions(entitypose);

        return new AxisAlignedBB((double) (-entitysize.width / 2.0F), 0.0D, (double) (-entitysize.width / 2.0F), (double) (entitysize.width / 2.0F), (double) entitysize.height, (double) (entitysize.width / 2.0F));
    }

    public Optional<BlockPosition> getSleepingPos() {
        return (Optional) this.entityData.get(EntityLiving.SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPosition blockposition) {
        this.entityData.set(EntityLiving.SLEEPING_POS_ID, Optional.of(blockposition));
    }

    public void clearSleepingPos() {
        this.entityData.set(EntityLiving.SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPosition blockposition) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        IBlockData iblockdata = this.level.getBlockState(blockposition);

        if (iblockdata.getBlock() instanceof BlockBed) {
            this.level.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockBed.OCCUPIED, true), 3);
        }

        this.setPose(EntityPose.SLEEPING);
        this.setPosToBed(blockposition);
        this.setSleepingPos(blockposition);
        this.setDeltaMovement(Vec3D.ZERO);
        this.hasImpulse = true;
    }

    private void setPosToBed(BlockPosition blockposition) {
        this.setPos((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.6875D, (double) blockposition.getZ() + 0.5D);
    }

    private boolean checkBedExists() {
        return (Boolean) this.getSleepingPos().map((blockposition) -> {
            return this.level.getBlockState(blockposition).getBlock() instanceof BlockBed;
        }).orElse(false);
    }

    public void stopSleeping() {
        Optional<BlockPosition> optional = this.getSleepingPos(); // CraftBukkit - decompile error
        World world = this.level;

        java.util.Objects.requireNonNull(this.level);
        optional.filter(world::hasChunkAt).ifPresent((blockposition) -> {
            IBlockData iblockdata = this.level.getBlockState(blockposition);

            if (iblockdata.getBlock() instanceof BlockBed) {
                this.level.setBlock(blockposition, (IBlockData) iblockdata.setValue(BlockBed.OCCUPIED, false), 3);
                Vec3D vec3d = (Vec3D) BlockBed.findStandUpPosition(this.getType(), this.level, blockposition, this.getYRot()).orElseGet(() -> {
                    BlockPosition blockposition1 = blockposition.above();

                    return new Vec3D((double) blockposition1.getX() + 0.5D, (double) blockposition1.getY() + 0.1D, (double) blockposition1.getZ() + 0.5D);
                });
                Vec3D vec3d1 = Vec3D.atBottomCenterOf(blockposition).subtract(vec3d).normalize();
                float f = (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d1.z, vec3d1.x) * 57.2957763671875D - 90.0D);

                this.setPos(vec3d.x, vec3d.y, vec3d.z);
                this.setYRot(f);
                this.setXRot(0.0F);
            }

        });
        Vec3D vec3d = this.position();

        this.setPose(EntityPose.STANDING);
        this.setPos(vec3d.x, vec3d.y, vec3d.z);
        this.clearSleepingPos();
    }

    @Nullable
    public EnumDirection getBedOrientation() {
        BlockPosition blockposition = (BlockPosition) this.getSleepingPos().orElse(null); // CraftBukkit - decompile error

        return blockposition != null ? BlockBed.getBedOrientation(this.level, blockposition) : null;
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    @Override
    protected final float getEyeHeight(EntityPose entitypose, EntitySize entitysize) {
        return entitypose == EntityPose.SLEEPING ? 0.2F : this.getStandingEyeHeight(entitypose, entitysize);
    }

    protected float getStandingEyeHeight(EntityPose entitypose, EntitySize entitysize) {
        return super.getEyeHeight(entitypose, entitysize);
    }

    public ItemStack getProjectile(ItemStack itemstack) {
        return ItemStack.EMPTY;
    }

    public ItemStack eat(World world, ItemStack itemstack) {
        if (itemstack.isEdible()) {
            world.gameEvent(this, GameEvent.EAT, this.eyeBlockPosition());
            world.playSound((EntityHuman) null, this.getX(), this.getY(), this.getZ(), this.getEatingSound(itemstack), SoundCategory.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
            this.addEatEffect(itemstack, world, this);
            if (!(this instanceof EntityHuman) || !((EntityHuman) this).getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            this.gameEvent(GameEvent.EAT);
        }

        return itemstack;
    }

    private void addEatEffect(ItemStack itemstack, World world, EntityLiving entityliving) {
        Item item = itemstack.getItem();

        if (item.isEdible()) {
            List<Pair<MobEffect, Float>> list = item.getFoodProperties().getEffects();
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                Pair<MobEffect, Float> pair = (Pair) iterator.next();

                if (!world.isClientSide && pair.getFirst() != null && world.random.nextFloat() < (Float) pair.getSecond()) {
                    entityliving.addEffect(new MobEffect((MobEffect) pair.getFirst()), EntityPotionEffectEvent.Cause.FOOD); // CraftBukkit
                }
            }
        }

    }

    private static byte entityEventForEquipmentBreak(EnumItemSlot enumitemslot) {
        switch (enumitemslot) {
            case MAINHAND:
                return 47;
            case OFFHAND:
                return 48;
            case HEAD:
                return 49;
            case CHEST:
                return 50;
            case FEET:
                return 52;
            case LEGS:
                return 51;
            default:
                return 47;
        }
    }

    public void broadcastBreakEvent(EnumItemSlot enumitemslot) {
        this.level.broadcastEntityEvent(this, entityEventForEquipmentBreak(enumitemslot));
    }

    public void broadcastBreakEvent(EnumHand enumhand) {
        this.broadcastBreakEvent(enumhand == EnumHand.MAIN_HAND ? EnumItemSlot.MAINHAND : EnumItemSlot.OFFHAND);
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        if (this.getItemBySlot(EnumItemSlot.HEAD).is(Items.DRAGON_HEAD)) {
            float f = 0.5F;

            return this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D);
        } else {
            return super.getBoundingBoxForCulling();
        }
    }

    public static EnumItemSlot getEquipmentSlotForItem(ItemStack itemstack) {
        Item item = itemstack.getItem();

        return !itemstack.is(Items.CARVED_PUMPKIN) && (!(item instanceof ItemBlock) || !(((ItemBlock) item).getBlock() instanceof BlockSkullAbstract)) ? (item instanceof ItemArmor ? ((ItemArmor) item).getSlot() : (itemstack.is(Items.ELYTRA) ? EnumItemSlot.CHEST : (itemstack.is(Items.SHIELD) ? EnumItemSlot.OFFHAND : EnumItemSlot.MAINHAND))) : EnumItemSlot.HEAD;
    }

    private static SlotAccess createEquipmentSlotAccess(EntityLiving entityliving, EnumItemSlot enumitemslot) {
        return enumitemslot != EnumItemSlot.HEAD && enumitemslot != EnumItemSlot.MAINHAND && enumitemslot != EnumItemSlot.OFFHAND ? SlotAccess.forEquipmentSlot(entityliving, enumitemslot, (itemstack) -> {
            return itemstack.isEmpty() || EntityInsentient.getEquipmentSlotForItem(itemstack) == enumitemslot;
        }) : SlotAccess.forEquipmentSlot(entityliving, enumitemslot);
    }

    @Nullable
    private static EnumItemSlot getEquipmentSlot(int i) {
        return i == 100 + EnumItemSlot.HEAD.getIndex() ? EnumItemSlot.HEAD : (i == 100 + EnumItemSlot.CHEST.getIndex() ? EnumItemSlot.CHEST : (i == 100 + EnumItemSlot.LEGS.getIndex() ? EnumItemSlot.LEGS : (i == 100 + EnumItemSlot.FEET.getIndex() ? EnumItemSlot.FEET : (i == 98 ? EnumItemSlot.MAINHAND : (i == 99 ? EnumItemSlot.OFFHAND : null)))));
    }

    @Override
    public SlotAccess getSlot(int i) {
        EnumItemSlot enumitemslot = getEquipmentSlot(i);

        return enumitemslot != null ? createEquipmentSlotAccess(this, enumitemslot) : super.getSlot(i);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            boolean flag = !this.getItemBySlot(EnumItemSlot.HEAD).is((Tag) TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EnumItemSlot.CHEST).is((Tag) TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EnumItemSlot.LEGS).is((Tag) TagsItem.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EnumItemSlot.FEET).is((Tag) TagsItem.FREEZE_IMMUNE_WEARABLES);

            return flag && super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level.isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    public void recreateFromPacket(PacketPlayOutSpawnEntityLiving packetplayoutspawnentityliving) {
        double d0 = packetplayoutspawnentityliving.getX();
        double d1 = packetplayoutspawnentityliving.getY();
        double d2 = packetplayoutspawnentityliving.getZ();
        float f = (float) (packetplayoutspawnentityliving.getyRot() * 360) / 256.0F;
        float f1 = (float) (packetplayoutspawnentityliving.getxRot() * 360) / 256.0F;

        this.setPacketCoordinates(d0, d1, d2);
        this.yBodyRot = (float) (packetplayoutspawnentityliving.getyHeadRot() * 360) / 256.0F;
        this.yHeadRot = (float) (packetplayoutspawnentityliving.getyHeadRot() * 360) / 256.0F;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(packetplayoutspawnentityliving.getId());
        this.setUUID(packetplayoutspawnentityliving.getUUID());
        this.absMoveTo(d0, d1, d2, f, f1);
        this.setDeltaMovement((double) ((float) packetplayoutspawnentityliving.getXd() / 8000.0F), (double) ((float) packetplayoutspawnentityliving.getYd() / 8000.0F), (double) ((float) packetplayoutspawnentityliving.getZd() / 8000.0F));
    }

    // CraftBukkit start - decompile error
    public static record a(SoundEffect small, SoundEffect big) {

        /*
        private final SoundEffect small;
        private final SoundEffect big;

        public a(SoundEffect soundeffect, SoundEffect soundeffect1) {
            this.small = soundeffect;
            this.big = soundeffect1;
        }

        public SoundEffect small() {
            return this.small;
        }

        public SoundEffect big() {
            return this.big;
        }
         */
        // CraftBukkit end
    }
}
