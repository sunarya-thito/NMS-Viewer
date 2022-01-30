package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.EnchantmentProtection;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFenceGate;
import net.minecraft.world.level.block.BlockHoney;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.EnumRenderType;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.portal.BlockPortalShape;
import net.minecraft.world.level.portal.ShapeDetectorShape;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.world.level.dimension.WorldDimension;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.event.CraftPortalEvent;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.spigotmc.CustomTimingsHandler; // Spigot
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Pose;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.PluginManager;
// CraftBukkit end

public abstract class Entity implements INamableTileEntity, EntityAccess, ICommandListener {

    // CraftBukkit start
    private static final int CURRENT_LEVEL = 2;
    static boolean isLevelAtLeast(NBTTagCompound tag, int level) {
        return tag.contains("Bukkit.updateLevel") && tag.getInt("Bukkit.updateLevel") >= level;
    }

    private CraftEntity bukkitEntity;

    public CraftEntity getBukkitEntity() {
        if (bukkitEntity == null) {
            bukkitEntity = CraftEntity.getEntity(level.getCraftServer(), this);
        }
        return bukkitEntity;
    }

    @Override
    public CommandSender getBukkitSender(CommandListenerWrapper wrapper) {
        return getBukkitEntity();
    }

    // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    public int getDefaultMaxAirSupply() {
        return TOTAL_AIR_SUPPLY;
    }
    // CraftBukkit end

    protected static final Logger LOGGER = LogManager.getLogger();
    public static final String ID_TAG = "id";
    public static final String PASSENGERS_TAG = "Passengers";
    private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
    private static final List<ItemStack> EMPTY_LIST = Collections.emptyList();
    public static final int BOARDING_COOLDOWN = 60;
    public static final int TOTAL_AIR_SUPPLY = 300;
    public static final int MAX_ENTITY_TAG_COUNT = 1024;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW = 0.5000001D;
    public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
    public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int FREEZE_HURT_FREQUENCY = 40;
    private static final AxisAlignedBB INITIAL_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private static final double WATER_FLOW_SCALE = 0.014D;
    private static final double LAVA_FAST_FLOW_SCALE = 0.007D;
    private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335D;
    public static final String UUID_TAG = "UUID";
    private static double viewScale = 1.0D;
    private final EntityTypes<?> type;
    private int id;
    public boolean blocksBuilding;
    public ImmutableList<Entity> passengers;
    protected int boardingCooldown;
    @Nullable
    private Entity vehicle;
    public World level;
    public double xo;
    public double yo;
    public double zo;
    private Vec3D position;
    private BlockPosition blockPosition;
    private ChunkCoordIntPair chunkPosition;
    private Vec3D deltaMovement;
    private float yRot;
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AxisAlignedBB bb;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean minorHorizontalCollision;
    public boolean hurtMarked;
    protected Vec3D stuckSpeedMultiplier;
    @Nullable
    private Entity.RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float walkDistO;
    public float walkDist;
    public float moveDist;
    public float flyDist;
    public float fallDistance;
    private float nextStep;
    public double xOld;
    public double yOld;
    public double zOld;
    public float maxUpStep;
    public boolean noPhysics;
    protected final Random random;
    public int tickCount;
    public int remainingFireTicks;
    public boolean wasTouchingWater;
    protected Object2DoubleMap<Tag<FluidType>> fluidHeight;
    protected boolean wasEyeInWater;
    @Nullable
    protected Tag<FluidType> fluidOnEyes;
    public int invulnerableTime;
    protected boolean firstTick;
    protected final DataWatcher entityData;
    protected static final DataWatcherObject<Byte> DATA_SHARED_FLAGS_ID = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BYTE);
    protected static final int FLAG_ONFIRE = 0;
    private static final int FLAG_SHIFT_KEY_DOWN = 1;
    private static final int FLAG_SPRINTING = 3;
    private static final int FLAG_SWIMMING = 4;
    private static final int FLAG_INVISIBLE = 5;
    protected static final int FLAG_GLOWING = 6;
    protected static final int FLAG_FALL_FLYING = 7;
    private static final DataWatcherObject<Integer> DATA_AIR_SUPPLY_ID = DataWatcher.defineId(Entity.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Optional<IChatBaseComponent>> DATA_CUSTOM_NAME = DataWatcher.defineId(Entity.class, DataWatcherRegistry.OPTIONAL_COMPONENT);
    private static final DataWatcherObject<Boolean> DATA_CUSTOM_NAME_VISIBLE = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_SILENT = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_NO_GRAVITY = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    protected static final DataWatcherObject<EntityPose> DATA_POSE = DataWatcher.defineId(Entity.class, DataWatcherRegistry.POSE);
    private static final DataWatcherObject<Integer> DATA_TICKS_FROZEN = DataWatcher.defineId(Entity.class, DataWatcherRegistry.INT);
    private EntityInLevelCallback levelCallback;
    private Vec3D packetCoordinates;
    public boolean noCulling;
    public boolean hasImpulse;
    public int portalCooldown;
    protected boolean isInsidePortal;
    protected int portalTime;
    protected BlockPosition portalEntrancePos;
    private boolean invulnerable;
    protected UUID uuid;
    protected String stringUUID;
    private boolean hasGlowingTag;
    private final Set<String> tags;
    private final double[] pistonDeltas;
    private long pistonDeltasGameTime;
    private EntitySize dimensions;
    private float eyeHeight;
    public boolean isInPowderSnow;
    public boolean wasInPowderSnow;
    public boolean wasOnFire;
    private float crystalSoundIntensity;
    private int lastCrystalSoundPlayTick;
    public boolean hasVisualFire;
    @Nullable
    private IBlockData feetBlockState;
    // CraftBukkit start
    public boolean persist = true;
    public boolean valid;
    public boolean generation;
    public int maxAirTicks = getDefaultMaxAirSupply(); // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean lastDamageCancelled; // SPIGOT-5339, SPIGOT-6252, SPIGOT-6777: Keep track if the event was canceled
    public boolean persistentInvisibility = false;
    public BlockPosition lastLavaContact;
    public CustomTimingsHandler tickTimer = org.bukkit.craftbukkit.SpigotTimings.getEntityTimings(this); // Spigot
    // Spigot start
    public final org.spigotmc.ActivationRange.ActivationType activationType = org.spigotmc.ActivationRange.initializeEntityActivationType(this);
    public final boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;
    public void inactiveTick() { }
    // Spigot end

    public float getBukkitYaw() {
        return this.yRot;
    }

    public boolean isChunkLoaded() {
        return level.hasChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }
    // CraftBukkit end

    public Entity(EntityTypes<?> entitytypes, World world) {
        this.id = Entity.ENTITY_COUNTER.incrementAndGet();
        this.passengers = ImmutableList.of();
        this.deltaMovement = Vec3D.ZERO;
        this.bb = Entity.INITIAL_AABB;
        this.stuckSpeedMultiplier = Vec3D.ZERO;
        this.nextStep = 1.0F;
        this.random = new Random();
        this.remainingFireTicks = -this.getFireImmuneTicks();
        this.fluidHeight = new Object2DoubleArrayMap(2);
        this.firstTick = true;
        this.levelCallback = EntityInLevelCallback.NULL;
        this.uuid = MathHelper.createInsecureUUID(this.random);
        this.stringUUID = this.uuid.toString();
        this.tags = Sets.newHashSet();
        this.pistonDeltas = new double[]{0.0D, 0.0D, 0.0D};
        this.feetBlockState = null;
        this.type = entitytypes;
        this.level = world;
        this.dimensions = entitytypes.getDimensions();
        this.position = Vec3D.ZERO;
        this.blockPosition = BlockPosition.ZERO;
        this.chunkPosition = ChunkCoordIntPair.ZERO;
        this.packetCoordinates = Vec3D.ZERO;
        // Spigot start
        if (world != null) {
            this.defaultActivationState = org.spigotmc.ActivationRange.initializeEntityActivationState(this, world.spigotConfig);
        } else {
            this.defaultActivationState = false;
        }
        // Spigot end
        this.entityData = new DataWatcher(this);
        this.entityData.define(Entity.DATA_SHARED_FLAGS_ID, (byte) 0);
        this.entityData.define(Entity.DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        this.entityData.define(Entity.DATA_CUSTOM_NAME_VISIBLE, false);
        this.entityData.define(Entity.DATA_CUSTOM_NAME, Optional.empty());
        this.entityData.define(Entity.DATA_SILENT, false);
        this.entityData.define(Entity.DATA_NO_GRAVITY, false);
        this.entityData.define(Entity.DATA_POSE, EntityPose.STANDING);
        this.entityData.define(Entity.DATA_TICKS_FROZEN, 0);
        this.defineSynchedData();
        this.getEntityData().registrationLocked = true; // Spigot
        this.setPos(0.0D, 0.0D, 0.0D);
        this.eyeHeight = this.getEyeHeight(EntityPose.STANDING, this.dimensions);
    }

    public boolean isColliding(BlockPosition blockposition, IBlockData iblockdata) {
        VoxelShape voxelshape = iblockdata.getCollisionShape(this.level, blockposition, VoxelShapeCollision.of(this));
        VoxelShape voxelshape1 = voxelshape.move((double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ());

        return VoxelShapes.joinIsNotEmpty(voxelshape1, VoxelShapes.create(this.getBoundingBox()), OperatorBoolean.AND);
    }

    public int getTeamColor() {
        ScoreboardTeamBase scoreboardteambase = this.getTeam();

        return scoreboardteambase != null && scoreboardteambase.getColor().getColor() != null ? scoreboardteambase.getColor().getColor() : 16777215;
    }

    public boolean isSpectator() {
        return false;
    }

    public final void unRide() {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }

    }

    public void setPacketCoordinates(double d0, double d1, double d2) {
        this.setPacketCoordinates(new Vec3D(d0, d1, d2));
    }

    public void setPacketCoordinates(Vec3D vec3d) {
        this.packetCoordinates = vec3d;
    }

    public Vec3D getPacketCoordinates() {
        return this.packetCoordinates;
    }

    public EntityTypes<?> getType() {
        return this.type;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public boolean addTag(String s) {
        return this.tags.size() >= 1024 ? false : this.tags.add(s);
    }

    public boolean removeTag(String s) {
        return this.tags.remove(s);
    }

    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
    }

    public final void discard() {
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    protected abstract void defineSynchedData();

    public DataWatcher getEntityData() {
        return this.entityData;
    }

    public boolean equals(Object object) {
        return object instanceof Entity ? ((Entity) object).id == this.id : false;
    }

    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason entity_removalreason) {
        this.setRemoved(entity_removalreason);
        if (entity_removalreason == Entity.RemovalReason.KILLED) {
            this.gameEvent(GameEvent.ENTITY_KILLED);
        }

    }

    public void onClientRemoval() {}

    public void setPose(EntityPose entitypose) {
        // CraftBukkit start
        if (entitypose == this.getPose()) {
            return;
        }
        this.level.getCraftServer().getPluginManager().callEvent(new EntityPoseChangeEvent(this.getBukkitEntity(), Pose.values()[entitypose.ordinal()]));
        // CraftBukkit end
        this.entityData.set(Entity.DATA_POSE, entitypose);
    }

    public EntityPose getPose() {
        return (EntityPose) this.entityData.get(Entity.DATA_POSE);
    }

    public boolean closerThan(Entity entity, double d0) {
        double d1 = entity.position.x - this.position.x;
        double d2 = entity.position.y - this.position.y;
        double d3 = entity.position.z - this.position.z;

        return d1 * d1 + d2 * d2 + d3 * d3 < d0 * d0;
    }

    protected void setRot(float f, float f1) {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(f)) {
            f = 0;
        }

        if (f == Float.POSITIVE_INFINITY || f == Float.NEGATIVE_INFINITY) {
            if (this instanceof EntityPlayer) {
                this.level.getCraftServer().getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Hacking?)");
            }
            f = 0;
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(f1)) {
            f1 = 0;
        }

        if (f1 == Float.POSITIVE_INFINITY || f1 == Float.NEGATIVE_INFINITY) {
            if (this instanceof EntityPlayer) {
                this.level.getCraftServer().getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Hacking?)");
            }
            f1 = 0;
        }
        // CraftBukkit end

        this.setYRot(f % 360.0F);
        this.setXRot(f1 % 360.0F);
    }

    public final void setPos(Vec3D vec3d) {
        this.setPos(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public void setPos(double d0, double d1, double d2) {
        this.setPosRaw(d0, d1, d2);
        this.setBoundingBox(this.makeBoundingBox());
    }

    protected AxisAlignedBB makeBoundingBox() {
        return this.dimensions.makeBoundingBox(this.position);
    }

    protected void reapplyPosition() {
        this.setPos(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double d0, double d1) {
        float f = (float) d1 * 0.15F;
        float f1 = (float) d0 * 0.15F;

        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + f1);
        this.setXRot(MathHelper.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += f;
        this.yRotO += f1;
        this.xRotO = MathHelper.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }

    }

    public void tick() {
        this.baseTick();
    }

    // CraftBukkit start
    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!(this instanceof EntityPlayer)) {
            this.handleNetherPortal();
        }
    }
    // CraftBukkit end

    public void baseTick() {
        this.level.getProfiler().push("entityBaseTick");
        this.feetBlockState = null;
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            --this.boardingCooldown;
        }

        this.walkDistO = this.walkDist;
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        if (this instanceof EntityPlayer) this.handleNetherPortal(); // CraftBukkit - // Moved up to postTick
        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.updateInWaterStateAndDoFluidPushing();
        this.updateFluidOnEyes();
        this.updateSwimming();
        if (this.level.isClientSide) {
            this.clearFire();
        } else if (this.remainingFireTicks > 0) {
            if (this.fireImmune()) {
                this.setRemainingFireTicks(this.remainingFireTicks - 4);
                if (this.remainingFireTicks < 0) {
                    this.clearFire();
                }
            } else {
                if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                    this.hurt(DamageSource.ON_FIRE, 1.0F);
                }

                this.setRemainingFireTicks(this.remainingFireTicks - 1);
            }

            if (this.getTicksFrozen() > 0) {
                this.setTicksFrozen(0);
                this.level.levelEvent((EntityHuman) null, 1009, this.blockPosition, 1);
            }
        }

        if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
            // CraftBukkit start
        } else {
            this.lastLavaContact = null;
            // CraftBukkit end
        }

        this.checkOutOfWorld();
        if (!this.level.isClientSide) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        this.level.getProfiler().pop();
    }

    public void setSharedFlagOnFire(boolean flag) {
        this.setSharedFlag(0, flag || this.hasVisualFire);
    }

    public void checkOutOfWorld() {
        if (this.getY() < (double) (this.level.getMinBuildHeight() - 64)) {
            this.outOfWorld();
        }

    }

    public void setPortalCooldown() {
        this.portalCooldown = this.getDimensionChangingDelay();
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    protected void processPortalCooldown() {
        if (this.isOnPortalCooldown()) {
            --this.portalCooldown;
        }

    }

    public int getPortalWaitTime() {
        return 0;
    }

    public void lavaHurt() {
        if (!this.fireImmune()) {
            // CraftBukkit start - Fallen in lava TODO: this event spams!
            if (this instanceof EntityLiving && remainingFireTicks <= 0) {
                // not on fire yet
                org.bukkit.block.Block damager = (lastLavaContact == null) ? null : org.bukkit.craftbukkit.block.CraftBlock.at(level, lastLavaContact);
                org.bukkit.entity.Entity damagee = this.getBukkitEntity();
                EntityCombustEvent combustEvent = new org.bukkit.event.entity.EntityCombustByBlockEvent(damager, damagee, 15);
                this.level.getCraftServer().getPluginManager().callEvent(combustEvent);

                if (!combustEvent.isCancelled()) {
                    this.setSecondsOnFire(combustEvent.getDuration(), false);
                }
            } else {
                // This will be called every single tick the entity is in lava, so don't throw an event
                this.setSecondsOnFire(15, false);
            }
            CraftEventFactory.blockDamage = (lastLavaContact) == null ? null : org.bukkit.craftbukkit.block.CraftBlock.at(level, lastLavaContact);
            if (this.hurt(DamageSource.LAVA, 4.0F)) {
                this.playSound(SoundEffects.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
            }
            CraftEventFactory.blockDamage = null;
            // CraftBukkit end - we also don't throw an event unless the object in lava is living, to save on some event calls

        }
    }

    public void setSecondsOnFire(int i) {
        // CraftBukkit start
        this.setSecondsOnFire(i, true);
    }

    public void setSecondsOnFire(int i, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), i);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            i = event.getDuration();
        }
        // CraftBukkit end
        int j = i * 20;

        if (this instanceof EntityLiving) {
            j = EnchantmentProtection.getFireAfterDampener((EntityLiving) this, j);
        }

        if (this.remainingFireTicks < j) {
            this.setRemainingFireTicks(j);
        }

    }

    public void setRemainingFireTicks(int i) {
        this.remainingFireTicks = i;
    }

    public int getRemainingFireTicks() {
        return this.remainingFireTicks;
    }

    public void clearFire() {
        this.setRemainingFireTicks(0);
    }

    protected void outOfWorld() {
        this.discard();
    }

    public boolean isFree(double d0, double d1, double d2) {
        return this.isFree(this.getBoundingBox().move(d0, d1, d2));
    }

    private boolean isFree(AxisAlignedBB axisalignedbb) {
        return this.level.noCollision(this, axisalignedbb) && !this.level.containsAnyLiquid(axisalignedbb);
    }

    public void setOnGround(boolean flag) {
        this.onGround = flag;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void move(EnumMoveType enummovetype, Vec3D vec3d) {
        org.bukkit.craftbukkit.SpigotTimings.entityMoveTimer.startTiming(); // Spigot
        if (this.noPhysics) {
            this.setPos(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z);
        } else {
            this.wasOnFire = this.isOnFire();
            if (enummovetype == EnumMoveType.PISTON) {
                vec3d = this.limitPistonMovement(vec3d);
                if (vec3d.equals(Vec3D.ZERO)) {
                    return;
                }
            }

            this.level.getProfiler().push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7D) {
                vec3d = vec3d.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3D.ZERO;
                this.setDeltaMovement(Vec3D.ZERO);
            }

            vec3d = this.maybeBackOffFromEdge(vec3d, enummovetype);
            Vec3D vec3d1 = this.collide(vec3d);

            if (vec3d1.lengthSqr() > 1.0E-7D) {
                this.setPos(this.getX() + vec3d1.x, this.getY() + vec3d1.y, this.getZ() + vec3d1.z);
            }

            this.level.getProfiler().pop();
            this.level.getProfiler().push("rest");
            this.horizontalCollision = !MathHelper.equal(vec3d.x, vec3d1.x) || !MathHelper.equal(vec3d.z, vec3d1.z);
            this.verticalCollision = vec3d.y != vec3d1.y;
            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3d1);
            } else {
                this.minorHorizontalCollision = false;
            }

            this.onGround = this.verticalCollision && vec3d.y < 0.0D;
            BlockPosition blockposition = this.getOnPos();
            IBlockData iblockdata = this.level.getBlockState(blockposition);

            this.checkFallDamage(vec3d1.y, this.onGround, iblockdata, blockposition);
            if (this.isRemoved()) {
                this.level.getProfiler().pop();
            } else {
                Vec3D vec3d2 = this.getDeltaMovement();

                if (vec3d.x != vec3d1.x) {
                    this.setDeltaMovement(0.0D, vec3d2.y, vec3d2.z);
                }

                if (vec3d.z != vec3d1.z) {
                    this.setDeltaMovement(vec3d2.x, vec3d2.y, 0.0D);
                }

                Block block = iblockdata.getBlock();

                if (vec3d.y != vec3d1.y) {
                    block.updateEntityAfterFallOn(this.level, this);
                }

                // CraftBukkit start
                if (horizontalCollision && getBukkitEntity() instanceof Vehicle) {
                    Vehicle vehicle = (Vehicle) this.getBukkitEntity();
                    org.bukkit.block.Block bl = this.level.getWorld().getBlockAt(MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()));

                    if (vec3d.x > vec3d1.x) {
                        bl = bl.getRelative(BlockFace.EAST);
                    } else if (vec3d.x < vec3d1.x) {
                        bl = bl.getRelative(BlockFace.WEST);
                    } else if (vec3d.z > vec3d1.z) {
                        bl = bl.getRelative(BlockFace.SOUTH);
                    } else if (vec3d.z < vec3d1.z) {
                        bl = bl.getRelative(BlockFace.NORTH);
                    }

                    if (!bl.getType().isAir()) {
                        VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, bl);
                        level.getCraftServer().getPluginManager().callEvent(event);
                    }
                }
                // CraftBukkit end

                if (this.onGround && !this.isSteppingCarefully()) {
                    block.stepOn(this.level, blockposition, iblockdata, this);
                }

                Entity.MovementEmission entity_movementemission = this.getMovementEmission();

                if (entity_movementemission.emitsAnything() && !this.isPassenger()) {
                    double d0 = vec3d1.x;
                    double d1 = vec3d1.y;
                    double d2 = vec3d1.z;

                    this.flyDist = (float) ((double) this.flyDist + vec3d1.length() * 0.6D);
                    if (!iblockdata.is((Tag) TagsBlock.CLIMBABLE) && !iblockdata.is(Blocks.POWDER_SNOW)) {
                        d1 = 0.0D;
                    }

                    this.walkDist += (float) vec3d1.horizontalDistance() * 0.6F;
                    this.moveDist += (float) Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 0.6F;
                    if (this.moveDist > this.nextStep && !iblockdata.isAir()) {
                        this.nextStep = this.nextStep();
                        if (this.isInWater()) {
                            if (entity_movementemission.emitsSounds()) {
                                Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
                                float f = entity == this ? 0.35F : 0.4F;
                                Vec3D vec3d3 = entity.getDeltaMovement();
                                float f1 = Math.min(1.0F, (float) Math.sqrt(vec3d3.x * vec3d3.x * 0.20000000298023224D + vec3d3.y * vec3d3.y + vec3d3.z * vec3d3.z * 0.20000000298023224D) * f);

                                this.playSwimSound(f1);
                            }

                            if (entity_movementemission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        } else {
                            if (entity_movementemission.emitsSounds()) {
                                this.playAmethystStepSound(iblockdata);
                                this.playStepSound(blockposition, iblockdata);
                            }

                            if (entity_movementemission.emitsEvents() && !iblockdata.is((Tag) TagsBlock.OCCLUDES_VIBRATION_SIGNALS)) {
                                this.gameEvent(GameEvent.STEP);
                            }
                        }
                    } else if (iblockdata.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float f2 = this.getBlockSpeedFactor();

                this.setDeltaMovement(this.getDeltaMovement().multiply((double) f2, 1.0D, (double) f2));
                if (this.level.getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6D)).noneMatch((iblockdata1) -> {
                    return iblockdata1.is((Tag) TagsBlock.FIRE) || iblockdata1.is(Blocks.LAVA);
                })) {
                    if (this.remainingFireTicks <= 0) {
                        this.setRemainingFireTicks(-this.getFireImmuneTicks());
                    }

                    if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                        this.playEntityOnFireExtinguishedSound();
                    }
                }

                if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                    this.setRemainingFireTicks(-this.getFireImmuneTicks());
                }

                this.level.getProfiler().pop();
            }
        }
        org.bukkit.craftbukkit.SpigotTimings.entityMoveTimer.stopTiming(); // Spigot
    }

    protected boolean isHorizontalCollisionMinor(Vec3D vec3d) {
        return false;
    }

    protected void tryCheckInsideBlocks() {
        try {
            this.checkInsideBlocks();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Checking entity block collision");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Entity being checked for collision");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    protected void playEntityOnFireExtinguishedSound() {
        this.playSound(SoundEffects.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void processFlappingMovement() {
        if (this.isFlapping()) {
            this.onFlap();
            if (this.getMovementEmission().emitsEvents()) {
                this.gameEvent(GameEvent.FLAP);
            }
        }

    }

    public BlockPosition getOnPos() {
        int i = MathHelper.floor(this.position.x);
        int j = MathHelper.floor(this.position.y - 0.20000000298023224D);
        int k = MathHelper.floor(this.position.z);
        BlockPosition blockposition = new BlockPosition(i, j, k);

        if (this.level.getBlockState(blockposition).isAir()) {
            BlockPosition blockposition1 = blockposition.below();
            IBlockData iblockdata = this.level.getBlockState(blockposition1);

            if (iblockdata.is((Tag) TagsBlock.FENCES) || iblockdata.is((Tag) TagsBlock.WALLS) || iblockdata.getBlock() instanceof BlockFenceGate) {
                return blockposition1;
            }
        }

        return blockposition;
    }

    protected float getBlockJumpFactor() {
        float f = this.level.getBlockState(this.blockPosition()).getBlock().getJumpFactor();
        float f1 = this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();

        return (double) f == 1.0D ? f1 : f;
    }

    protected float getBlockSpeedFactor() {
        IBlockData iblockdata = this.level.getBlockState(this.blockPosition());
        float f = iblockdata.getBlock().getSpeedFactor();

        return !iblockdata.is(Blocks.WATER) && !iblockdata.is(Blocks.BUBBLE_COLUMN) ? ((double) f == 1.0D ? this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f) : f;
    }

    protected BlockPosition getBlockPosBelowThatAffectsMyMovement() {
        return new BlockPosition(this.position.x, this.getBoundingBox().minY - 0.5000001D, this.position.z);
    }

    protected Vec3D maybeBackOffFromEdge(Vec3D vec3d, EnumMoveType enummovetype) {
        return vec3d;
    }

    protected Vec3D limitPistonMovement(Vec3D vec3d) {
        if (vec3d.lengthSqr() <= 1.0E-7D) {
            return vec3d;
        } else {
            long i = this.level.getGameTime();

            if (i != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0D);
                this.pistonDeltasGameTime = i;
            }

            double d0;

            if (vec3d.x != 0.0D) {
                d0 = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.X, vec3d.x);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3D.ZERO : new Vec3D(d0, 0.0D, 0.0D);
            } else if (vec3d.y != 0.0D) {
                d0 = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.Y, vec3d.y);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3D.ZERO : new Vec3D(0.0D, d0, 0.0D);
            } else if (vec3d.z != 0.0D) {
                d0 = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.Z, vec3d.z);
                return Math.abs(d0) <= 9.999999747378752E-6D ? Vec3D.ZERO : new Vec3D(0.0D, 0.0D, d0);
            } else {
                return Vec3D.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(EnumDirection.EnumAxis enumdirection_enumaxis, double d0) {
        int i = enumdirection_enumaxis.ordinal();
        double d1 = MathHelper.clamp(d0 + this.pistonDeltas[i], -0.51D, 0.51D);

        d0 = d1 - this.pistonDeltas[i];
        this.pistonDeltas[i] = d1;
        return d0;
    }

    private Vec3D collide(Vec3D vec3d) {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        List<VoxelShape> list = this.level.getEntityCollisions(this, axisalignedbb.expandTowards(vec3d));
        Vec3D vec3d1 = vec3d.lengthSqr() == 0.0D ? vec3d : collideBoundingBox(this, vec3d, axisalignedbb, this.level, list);
        boolean flag = vec3d.x != vec3d1.x;
        boolean flag1 = vec3d.y != vec3d1.y;
        boolean flag2 = vec3d.z != vec3d1.z;
        boolean flag3 = this.onGround || flag1 && vec3d.y < 0.0D;

        if (this.maxUpStep > 0.0F && flag3 && (flag || flag2)) {
            Vec3D vec3d2 = collideBoundingBox(this, new Vec3D(vec3d.x, (double) this.maxUpStep, vec3d.z), axisalignedbb, this.level, list);
            Vec3D vec3d3 = collideBoundingBox(this, new Vec3D(0.0D, (double) this.maxUpStep, 0.0D), axisalignedbb.expandTowards(vec3d.x, 0.0D, vec3d.z), this.level, list);

            if (vec3d3.y < (double) this.maxUpStep) {
                Vec3D vec3d4 = collideBoundingBox(this, new Vec3D(vec3d.x, 0.0D, vec3d.z), axisalignedbb.move(vec3d3), this.level, list).add(vec3d3);

                if (vec3d4.horizontalDistanceSqr() > vec3d2.horizontalDistanceSqr()) {
                    vec3d2 = vec3d4;
                }
            }

            if (vec3d2.horizontalDistanceSqr() > vec3d1.horizontalDistanceSqr()) {
                return vec3d2.add(collideBoundingBox(this, new Vec3D(0.0D, -vec3d2.y + vec3d.y, 0.0D), axisalignedbb.move(vec3d2), this.level, list));
            }
        }

        return vec3d1;
    }

    public static Vec3D collideBoundingBox(@Nullable Entity entity, Vec3D vec3d, AxisAlignedBB axisalignedbb, World world, List<VoxelShape> list) {
        Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size() + 1);

        if (!list.isEmpty()) {
            builder.addAll(list);
        }

        WorldBorder worldborder = world.getWorldBorder();
        boolean flag = entity != null && worldborder.isInsideCloseToBorder(entity, axisalignedbb.expandTowards(vec3d));

        if (flag) {
            builder.add(worldborder.getCollisionShape());
        }

        builder.addAll(world.getBlockCollisions(entity, axisalignedbb.expandTowards(vec3d)));
        return collideWithShapes(vec3d, axisalignedbb, builder.build());
    }

    private static Vec3D collideWithShapes(Vec3D vec3d, AxisAlignedBB axisalignedbb, List<VoxelShape> list) {
        if (list.isEmpty()) {
            return vec3d;
        } else {
            double d0 = vec3d.x;
            double d1 = vec3d.y;
            double d2 = vec3d.z;

            if (d1 != 0.0D) {
                d1 = VoxelShapes.collide(EnumDirection.EnumAxis.Y, axisalignedbb, list, d1);
                if (d1 != 0.0D) {
                    axisalignedbb = axisalignedbb.move(0.0D, d1, 0.0D);
                }
            }

            boolean flag = Math.abs(d0) < Math.abs(d2);

            if (flag && d2 != 0.0D) {
                d2 = VoxelShapes.collide(EnumDirection.EnumAxis.Z, axisalignedbb, list, d2);
                if (d2 != 0.0D) {
                    axisalignedbb = axisalignedbb.move(0.0D, 0.0D, d2);
                }
            }

            if (d0 != 0.0D) {
                d0 = VoxelShapes.collide(EnumDirection.EnumAxis.X, axisalignedbb, list, d0);
                if (!flag && d0 != 0.0D) {
                    axisalignedbb = axisalignedbb.move(d0, 0.0D, 0.0D);
                }
            }

            if (!flag && d2 != 0.0D) {
                d2 = VoxelShapes.collide(EnumDirection.EnumAxis.Z, axisalignedbb, list, d2);
            }

            return new Vec3D(d0, d1, d2);
        }
    }

    protected float nextStep() {
        return (float) ((int) this.moveDist + 1);
    }

    protected SoundEffect getSwimSound() {
        return SoundEffects.GENERIC_SWIM;
    }

    protected SoundEffect getSwimSplashSound() {
        return SoundEffects.GENERIC_SPLASH;
    }

    protected SoundEffect getSwimHighSpeedSplashSound() {
        return SoundEffects.GENERIC_SPLASH;
    }

    protected void checkInsideBlocks() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        BlockPosition blockposition = new BlockPosition(axisalignedbb.minX + 0.001D, axisalignedbb.minY + 0.001D, axisalignedbb.minZ + 0.001D);
        BlockPosition blockposition1 = new BlockPosition(axisalignedbb.maxX - 0.001D, axisalignedbb.maxY - 0.001D, axisalignedbb.maxZ - 0.001D);

        if (this.level.hasChunksAt(blockposition, blockposition1)) {
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

            for (int i = blockposition.getX(); i <= blockposition1.getX(); ++i) {
                for (int j = blockposition.getY(); j <= blockposition1.getY(); ++j) {
                    for (int k = blockposition.getZ(); k <= blockposition1.getZ(); ++k) {
                        blockposition_mutableblockposition.set(i, j, k);
                        IBlockData iblockdata = this.level.getBlockState(blockposition_mutableblockposition);

                        try {
                            iblockdata.entityInside(this.level, blockposition_mutableblockposition, this);
                            this.onInsideBlock(iblockdata);
                        } catch (Throwable throwable) {
                            CrashReport crashreport = CrashReport.forThrowable(throwable, "Colliding entity with block");
                            CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Block being collided with");

                            CrashReportSystemDetails.populateBlockDetails(crashreportsystemdetails, this.level, blockposition_mutableblockposition, iblockdata);
                            throw new ReportedException(crashreport);
                        }
                    }
                }
            }
        }

    }

    protected void onInsideBlock(IBlockData iblockdata) {}

    public void gameEvent(GameEvent gameevent, @Nullable Entity entity, BlockPosition blockposition) {
        this.level.gameEvent(entity, gameevent, blockposition);
    }

    public void gameEvent(GameEvent gameevent, @Nullable Entity entity) {
        this.gameEvent(gameevent, entity, this.blockPosition);
    }

    public void gameEvent(GameEvent gameevent, BlockPosition blockposition) {
        this.gameEvent(gameevent, this, blockposition);
    }

    public void gameEvent(GameEvent gameevent) {
        this.gameEvent(gameevent, this.blockPosition);
    }

    protected void playStepSound(BlockPosition blockposition, IBlockData iblockdata) {
        if (!iblockdata.getMaterial().isLiquid()) {
            IBlockData iblockdata1 = this.level.getBlockState(blockposition.above());
            SoundEffectType soundeffecttype = iblockdata1.is((Tag) TagsBlock.INSIDE_STEP_SOUND_BLOCKS) ? iblockdata1.getSoundType() : iblockdata.getSoundType();

            this.playSound(soundeffecttype.getStepSound(), soundeffecttype.getVolume() * 0.15F, soundeffecttype.getPitch());
        }
    }

    private void playAmethystStepSound(IBlockData iblockdata) {
        if (iblockdata.is((Tag) TagsBlock.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20) {
            this.crystalSoundIntensity = (float) ((double) this.crystalSoundIntensity * Math.pow(0.996999979019165D, (double) (this.tickCount - this.lastCrystalSoundPlayTick)));
            this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
            float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
            float f1 = 0.1F + this.crystalSoundIntensity * 1.2F;

            this.playSound(SoundEffects.AMETHYST_BLOCK_CHIME, f1, f);
            this.lastCrystalSoundPlayTick = this.tickCount;
        }

    }

    protected void playSwimSound(float f) {
        this.playSound(this.getSwimSound(), f, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {}

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEffect soundeffect, float f, float f1) {
        if (!this.isSilent()) {
            this.level.playSound((EntityHuman) null, this.getX(), this.getY(), this.getZ(), soundeffect, this.getSoundSource(), f, f1);
        }

    }

    public boolean isSilent() {
        return (Boolean) this.entityData.get(Entity.DATA_SILENT);
    }

    public void setSilent(boolean flag) {
        this.entityData.set(Entity.DATA_SILENT, flag);
    }

    public boolean isNoGravity() {
        return (Boolean) this.entityData.get(Entity.DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean flag) {
        this.entityData.set(Entity.DATA_NO_GRAVITY, flag);
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.ALL;
    }

    public boolean occludesVibrations() {
        return false;
    }

    protected void checkFallDamage(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {
        if (flag) {
            if (this.fallDistance > 0.0F) {
                iblockdata.getBlock().fallOn(this.level, iblockdata, blockposition, this, this.fallDistance);
                if (!iblockdata.is((Tag) TagsBlock.OCCLUDES_VIBRATION_SIGNALS)) {
                    this.gameEvent(GameEvent.HIT_GROUND);
                }
            }

            this.resetFallDistance();
        } else if (d0 < 0.0D) {
            this.fallDistance = (float) ((double) this.fallDistance - d0);
        }

    }

    public boolean fireImmune() {
        return this.getType().fireImmune();
    }

    public boolean causeFallDamage(float f, float f1, DamageSource damagesource) {
        if (this.isVehicle()) {
            Iterator iterator = this.getPassengers().iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();

                entity.causeFallDamage(f, f1, damagesource);
            }
        }

        return false;
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    private boolean isInRain() {
        BlockPosition blockposition = this.blockPosition();

        return this.level.isRainingAt(blockposition) || this.level.isRainingAt(new BlockPosition((double) blockposition.getX(), this.getBoundingBox().maxY, (double) blockposition.getZ()));
    }

    private boolean isInBubbleColumn() {
        return this.level.getBlockState(this.blockPosition()).is(Blocks.BUBBLE_COLUMN);
    }

    public boolean isInWaterOrRain() {
        return this.isInWater() || this.isInRain();
    }

    public boolean isInWaterRainOrBubble() {
        return this.isInWater() || this.isInRain() || this.isInBubbleColumn();
    }

    public boolean isInWaterOrBubble() {
        return this.isInWater() || this.isInBubbleColumn();
    }

    public boolean isUnderWater() {
        return this.wasEyeInWater && this.isInWater();
    }

    public void updateSwimming() {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
        } else {
            this.setSwimming(this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level.getFluidState(this.blockPosition).is((Tag) TagsFluid.WATER));
        }

    }

    protected boolean updateInWaterStateAndDoFluidPushing() {
        this.fluidHeight.clear();
        this.updateInWaterStateAndDoWaterCurrentPushing();
        double d0 = this.level.dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
        boolean flag = this.updateFluidHeightAndDoFluidPushing(TagsFluid.LAVA, d0);

        return this.isInWater() || flag;
    }

    void updateInWaterStateAndDoWaterCurrentPushing() {
        if (this.getVehicle() instanceof EntityBoat) {
            this.wasTouchingWater = false;
        } else if (this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014D)) {
            if (!this.wasTouchingWater && !this.firstTick) {
                this.doWaterSplashEffect();
            }

            this.resetFallDistance();
            this.wasTouchingWater = true;
            this.clearFire();
        } else {
            this.wasTouchingWater = false;
        }

    }

    private void updateFluidOnEyes() {
        this.wasEyeInWater = this.isEyeInFluid(TagsFluid.WATER);
        this.fluidOnEyes = null;
        double d0 = this.getEyeY() - 0.1111111119389534D;
        Entity entity = this.getVehicle();

        if (entity instanceof EntityBoat) {
            EntityBoat entityboat = (EntityBoat) entity;

            if (!entityboat.isUnderWater() && entityboat.getBoundingBox().maxY >= d0 && entityboat.getBoundingBox().minY <= d0) {
                return;
            }
        }

        BlockPosition blockposition = new BlockPosition(this.getX(), d0, this.getZ());
        Fluid fluid = this.level.getFluidState(blockposition);
        Iterator iterator = TagsFluid.getStaticTags().iterator();

        Tag tag;

        do {
            if (!iterator.hasNext()) {
                return;
            }

            tag = (Tag) iterator.next();
        } while (!fluid.is(tag));

        double d1 = (double) ((float) blockposition.getY() + fluid.getHeight(this.level, blockposition));

        if (d1 > d0) {
            this.fluidOnEyes = tag;
        }

    }

    protected void doWaterSplashEffect() {
        Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
        float f = entity == this ? 0.2F : 0.9F;
        Vec3D vec3d = entity.getDeltaMovement();
        float f1 = Math.min(1.0F, (float) Math.sqrt(vec3d.x * vec3d.x * 0.20000000298023224D + vec3d.y * vec3d.y + vec3d.z * vec3d.z * 0.20000000298023224D) * f);

        if (f1 < 0.25F) {
            this.playSound(this.getSwimSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSwimHighSpeedSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float f2 = (float) MathHelper.floor(this.getY());

        double d0;
        double d1;
        int i;

        for (i = 0; (float) i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            d0 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            d1 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            this.level.addParticle(Particles.BUBBLE, this.getX() + d0, (double) (f2 + 1.0F), this.getZ() + d1, vec3d.x, vec3d.y - this.random.nextDouble() * 0.20000000298023224D, vec3d.z);
        }

        for (i = 0; (float) i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            d0 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            d1 = (this.random.nextDouble() * 2.0D - 1.0D) * (double) this.dimensions.width;
            this.level.addParticle(Particles.SPLASH, this.getX() + d0, (double) (f2 + 1.0F), this.getZ() + d1, vec3d.x, vec3d.y, vec3d.z);
        }

        this.gameEvent(GameEvent.SPLASH);
    }

    protected IBlockData getBlockStateOn() {
        return this.level.getBlockState(this.getOnPos());
    }

    public boolean canSpawnSprintParticle() {
        return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
    }

    protected void spawnSprintParticle() {
        int i = MathHelper.floor(this.getX());
        int j = MathHelper.floor(this.getY() - 0.20000000298023224D);
        int k = MathHelper.floor(this.getZ());
        BlockPosition blockposition = new BlockPosition(i, j, k);
        IBlockData iblockdata = this.level.getBlockState(blockposition);

        if (iblockdata.getRenderShape() != EnumRenderType.INVISIBLE) {
            Vec3D vec3d = this.getDeltaMovement();

            this.level.addParticle(new ParticleParamBlock(Particles.BLOCK, iblockdata), this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, vec3d.x * -4.0D, 1.5D, vec3d.z * -4.0D);
        }

    }

    public boolean isEyeInFluid(Tag<FluidType> tag) {
        return this.fluidOnEyes == tag;
    }

    public boolean isInLava() {
        return !this.firstTick && this.fluidHeight.getDouble(TagsFluid.LAVA) > 0.0D;
    }

    public void moveRelative(float f, Vec3D vec3d) {
        Vec3D vec3d1 = getInputVector(vec3d, f, this.getYRot());

        this.setDeltaMovement(this.getDeltaMovement().add(vec3d1));
    }

    private static Vec3D getInputVector(Vec3D vec3d, float f, float f1) {
        double d0 = vec3d.lengthSqr();

        if (d0 < 1.0E-7D) {
            return Vec3D.ZERO;
        } else {
            Vec3D vec3d1 = (d0 > 1.0D ? vec3d.normalize() : vec3d).scale((double) f);
            float f2 = MathHelper.sin(f1 * 0.017453292F);
            float f3 = MathHelper.cos(f1 * 0.017453292F);

            return new Vec3D(vec3d1.x * (double) f3 - vec3d1.z * (double) f2, vec3d1.y, vec3d1.z * (double) f3 + vec3d1.x * (double) f2);
        }
    }

    public float getBrightness() {
        return this.level.hasChunkAt(this.getBlockX(), this.getBlockZ()) ? this.level.getBrightness(new BlockPosition(this.getX(), this.getEyeY(), this.getZ())) : 0.0F;
    }

    public void absMoveTo(double d0, double d1, double d2, float f, float f1) {
        this.absMoveTo(d0, d1, d2);
        this.setYRot(f % 360.0F);
        this.setXRot(MathHelper.clamp(f1, -90.0F, 90.0F) % 360.0F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void absMoveTo(double d0, double d1, double d2) {
        double d3 = MathHelper.clamp(d0, -3.0E7D, 3.0E7D);
        double d4 = MathHelper.clamp(d2, -3.0E7D, 3.0E7D);

        this.xo = d3;
        this.yo = d1;
        this.zo = d4;
        this.setPos(d3, d1, d4);
        if (valid) level.getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4); // CraftBukkit
    }

    public void moveTo(Vec3D vec3d) {
        this.moveTo(vec3d.x, vec3d.y, vec3d.z);
    }

    public void moveTo(double d0, double d1, double d2) {
        this.moveTo(d0, d1, d2, this.getYRot(), this.getXRot());
    }

    public void moveTo(BlockPosition blockposition, float f, float f1) {
        this.moveTo((double) blockposition.getX() + 0.5D, (double) blockposition.getY(), (double) blockposition.getZ() + 0.5D, f, f1);
    }

    public void moveTo(double d0, double d1, double d2, float f, float f1) {
        this.setPosRaw(d0, d1, d2);
        this.setYRot(f);
        this.setXRot(f1);
        this.setOldPosAndRot();
        this.reapplyPosition();
    }

    public final void setOldPosAndRot() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();

        this.xo = d0;
        this.yo = d1;
        this.zo = d2;
        this.xOld = d0;
        this.yOld = d1;
        this.zOld = d2;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public float distanceTo(Entity entity) {
        float f = (float) (this.getX() - entity.getX());
        float f1 = (float) (this.getY() - entity.getY());
        float f2 = (float) (this.getZ() - entity.getZ());

        return MathHelper.sqrt(f * f + f1 * f1 + f2 * f2);
    }

    public double distanceToSqr(double d0, double d1, double d2) {
        double d3 = this.getX() - d0;
        double d4 = this.getY() - d1;
        double d5 = this.getZ() - d2;

        return d3 * d3 + d4 * d4 + d5 * d5;
    }

    public double distanceToSqr(Entity entity) {
        return this.distanceToSqr(entity.position());
    }

    public double distanceToSqr(Vec3D vec3d) {
        double d0 = this.getX() - vec3d.x;
        double d1 = this.getY() - vec3d.y;
        double d2 = this.getZ() - vec3d.z;

        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public void playerTouch(EntityHuman entityhuman) {}

    public void push(Entity entity) {
        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                double d0 = entity.getX() - this.getX();
                double d1 = entity.getZ() - this.getZ();
                double d2 = MathHelper.absMax(d0, d1);

                if (d2 >= 0.009999999776482582D) {
                    d2 = Math.sqrt(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D) {
                        d3 = 1.0D;
                    }

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.05000000074505806D;
                    d1 *= 0.05000000074505806D;
                    if (!this.isVehicle()) {
                        this.push(-d0, 0.0D, -d1);
                    }

                    if (!entity.isVehicle()) {
                        entity.push(d0, 0.0D, d1);
                    }
                }

            }
        }
    }

    public void push(double d0, double d1, double d2) {
        this.setDeltaMovement(this.getDeltaMovement().add(d0, d1, d2));
        this.hasImpulse = true;
    }

    protected void markHurt() {
        this.hurtMarked = true;
    }

    public boolean hurt(DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(damagesource)) {
            return false;
        } else {
            this.markHurt();
            return false;
        }
    }

    public final Vec3D getViewVector(float f) {
        return this.calculateViewVector(this.getViewXRot(f), this.getViewYRot(f));
    }

    public float getViewXRot(float f) {
        return f == 1.0F ? this.getXRot() : MathHelper.lerp(f, this.xRotO, this.getXRot());
    }

    public float getViewYRot(float f) {
        return f == 1.0F ? this.getYRot() : MathHelper.lerp(f, this.yRotO, this.getYRot());
    }

    protected final Vec3D calculateViewVector(float f, float f1) {
        float f2 = f * 0.017453292F;
        float f3 = -f1 * 0.017453292F;
        float f4 = MathHelper.cos(f3);
        float f5 = MathHelper.sin(f3);
        float f6 = MathHelper.cos(f2);
        float f7 = MathHelper.sin(f2);

        return new Vec3D((double) (f5 * f6), (double) (-f7), (double) (f4 * f6));
    }

    public final Vec3D getUpVector(float f) {
        return this.calculateUpVector(this.getViewXRot(f), this.getViewYRot(f));
    }

    protected final Vec3D calculateUpVector(float f, float f1) {
        return this.calculateViewVector(f - 90.0F, f1);
    }

    public final Vec3D getEyePosition() {
        return new Vec3D(this.getX(), this.getEyeY(), this.getZ());
    }

    public final Vec3D getEyePosition(float f) {
        double d0 = MathHelper.lerp((double) f, this.xo, this.getX());
        double d1 = MathHelper.lerp((double) f, this.yo, this.getY()) + (double) this.getEyeHeight();
        double d2 = MathHelper.lerp((double) f, this.zo, this.getZ());

        return new Vec3D(d0, d1, d2);
    }

    public Vec3D getLightProbePosition(float f) {
        return this.getEyePosition(f);
    }

    public final Vec3D getPosition(float f) {
        double d0 = MathHelper.lerp((double) f, this.xo, this.getX());
        double d1 = MathHelper.lerp((double) f, this.yo, this.getY());
        double d2 = MathHelper.lerp((double) f, this.zo, this.getZ());

        return new Vec3D(d0, d1, d2);
    }

    public MovingObjectPosition pick(double d0, float f, boolean flag) {
        Vec3D vec3d = this.getEyePosition(f);
        Vec3D vec3d1 = this.getViewVector(f);
        Vec3D vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);

        return this.level.clip(new RayTrace(vec3d, vec3d2, RayTrace.BlockCollisionOption.OUTLINE, flag ? RayTrace.FluidCollisionOption.ANY : RayTrace.FluidCollisionOption.NONE, this));
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    // CraftBukkit start - collidable API
    public boolean canCollideWithBukkit(Entity entity) {
        return isPushable();
    }
    // CraftBukkit end

    public void awardKillScore(Entity entity, int i, DamageSource damagesource) {
        if (entity instanceof EntityPlayer) {
            CriterionTriggers.ENTITY_KILLED_PLAYER.trigger((EntityPlayer) entity, this, damagesource);
        }

    }

    public boolean shouldRender(double d0, double d1, double d2) {
        double d3 = this.getX() - d0;
        double d4 = this.getY() - d1;
        double d5 = this.getZ() - d2;
        double d6 = d3 * d3 + d4 * d4 + d5 * d5;

        return this.shouldRenderAtSqrDistance(d6);
    }

    public boolean shouldRenderAtSqrDistance(double d0) {
        double d1 = this.getBoundingBox().getSize();

        if (Double.isNaN(d1)) {
            d1 = 1.0D;
        }

        d1 *= 64.0D * Entity.viewScale;
        return d0 < d1 * d1;
    }

    public boolean saveAsPassenger(NBTTagCompound nbttagcompound) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String s = this.getEncodeId();

            if (!this.persist || s == null) { // CraftBukkit - persist flag
                return false;
            } else {
                nbttagcompound.putString("id", s);
                this.saveWithoutId(nbttagcompound);
                return true;
            }
        }
    }

    public boolean save(NBTTagCompound nbttagcompound) {
        return this.isPassenger() ? false : this.saveAsPassenger(nbttagcompound);
    }

    public NBTTagCompound saveWithoutId(NBTTagCompound nbttagcompound) {
        try {
            if (this.vehicle != null) {
                nbttagcompound.put("Pos", this.newDoubleList(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
            } else {
                nbttagcompound.put("Pos", this.newDoubleList(this.getX(), this.getY(), this.getZ()));
            }

            Vec3D vec3d = this.getDeltaMovement();

            nbttagcompound.put("Motion", this.newDoubleList(vec3d.x, vec3d.y, vec3d.z));

            // CraftBukkit start - Checking for NaN pitch/yaw and resetting to zero
            // TODO: make sure this is the best way to address this.
            if (Float.isNaN(this.yRot)) {
                this.yRot = 0;
            }

            if (Float.isNaN(this.xRot)) {
                this.xRot = 0;
            }
            // CraftBukkit end

            nbttagcompound.put("Rotation", this.newFloatList(this.getYRot(), this.getXRot()));
            nbttagcompound.putFloat("FallDistance", this.fallDistance);
            nbttagcompound.putShort("Fire", (short) this.remainingFireTicks);
            nbttagcompound.putShort("Air", (short) this.getAirSupply());
            nbttagcompound.putBoolean("OnGround", this.onGround);
            nbttagcompound.putBoolean("Invulnerable", this.invulnerable);
            nbttagcompound.putInt("PortalCooldown", this.portalCooldown);
            nbttagcompound.putUUID("UUID", this.getUUID());
            // CraftBukkit start
            // PAIL: Check above UUID reads 1.8 properly, ie: UUIDMost / UUIDLeast
            nbttagcompound.putLong("WorldUUIDLeast", ((WorldServer) this.level).getWorld().getUID().getLeastSignificantBits());
            nbttagcompound.putLong("WorldUUIDMost", ((WorldServer) this.level).getWorld().getUID().getMostSignificantBits());
            nbttagcompound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
            if (!this.persist) {
                nbttagcompound.putBoolean("Bukkit.persist", this.persist);
            }
            if (this.persistentInvisibility) {
                nbttagcompound.putBoolean("Bukkit.invisible", this.persistentInvisibility);
            }
            // SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
            if (maxAirTicks != getDefaultMaxAirSupply()) {
                nbttagcompound.putInt("Bukkit.MaxAirSupply", getMaxAirSupply());
            }
            nbttagcompound.putInt("Spigot.ticksLived", this.tickCount);
            // CraftBukkit end
            IChatBaseComponent ichatbasecomponent = this.getCustomName();

            if (ichatbasecomponent != null) {
                nbttagcompound.putString("CustomName", IChatBaseComponent.ChatSerializer.toJson(ichatbasecomponent));
            }

            if (this.isCustomNameVisible()) {
                nbttagcompound.putBoolean("CustomNameVisible", this.isCustomNameVisible());
            }

            if (this.isSilent()) {
                nbttagcompound.putBoolean("Silent", this.isSilent());
            }

            if (this.isNoGravity()) {
                nbttagcompound.putBoolean("NoGravity", this.isNoGravity());
            }

            if (this.hasGlowingTag) {
                nbttagcompound.putBoolean("Glowing", true);
            }

            int i = this.getTicksFrozen();

            if (i > 0) {
                nbttagcompound.putInt("TicksFrozen", this.getTicksFrozen());
            }

            if (this.hasVisualFire) {
                nbttagcompound.putBoolean("HasVisualFire", this.hasVisualFire);
            }

            NBTTagList nbttaglist;
            Iterator iterator;

            if (!this.tags.isEmpty()) {
                nbttaglist = new NBTTagList();
                iterator = this.tags.iterator();

                while (iterator.hasNext()) {
                    String s = (String) iterator.next();

                    nbttaglist.add(NBTTagString.valueOf(s));
                }

                nbttagcompound.put("Tags", nbttaglist);
            }

            this.addAdditionalSaveData(nbttagcompound);
            if (this.isVehicle()) {
                nbttaglist = new NBTTagList();
                iterator = this.getPassengers().iterator();

                while (iterator.hasNext()) {
                    Entity entity = (Entity) iterator.next();
                    NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                    if (entity.saveAsPassenger(nbttagcompound1)) {
                        nbttaglist.add(nbttagcompound1);
                    }
                }

                if (!nbttaglist.isEmpty()) {
                    nbttagcompound.put("Passengers", nbttaglist);
                }
            }

            // CraftBukkit start - stores eventually existing bukkit values
            if (this.bukkitEntity != null) {
                this.bukkitEntity.storeBukkitValues(nbttagcompound);
            }
            // CraftBukkit end
            return nbttagcompound;
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Saving entity NBT");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Entity being saved");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    public void load(NBTTagCompound nbttagcompound) {
        try {
            NBTTagList nbttaglist = nbttagcompound.getList("Pos", 6);
            NBTTagList nbttaglist1 = nbttagcompound.getList("Motion", 6);
            NBTTagList nbttaglist2 = nbttagcompound.getList("Rotation", 5);
            double d0 = nbttaglist1.getDouble(0);
            double d1 = nbttaglist1.getDouble(1);
            double d2 = nbttaglist1.getDouble(2);

            this.setDeltaMovement(Math.abs(d0) > 10.0D ? 0.0D : d0, Math.abs(d1) > 10.0D ? 0.0D : d1, Math.abs(d2) > 10.0D ? 0.0D : d2);
            this.setPosRaw(nbttaglist.getDouble(0), MathHelper.clamp(nbttaglist.getDouble(1), -2.0E7D, 2.0E7D), nbttaglist.getDouble(2));
            this.setYRot(nbttaglist2.getFloat(0));
            this.setXRot(nbttaglist2.getFloat(1));
            this.setOldPosAndRot();
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
            this.fallDistance = nbttagcompound.getFloat("FallDistance");
            this.remainingFireTicks = nbttagcompound.getShort("Fire");
            if (nbttagcompound.contains("Air")) {
                this.setAirSupply(nbttagcompound.getShort("Air"));
            }

            this.onGround = nbttagcompound.getBoolean("OnGround");
            this.invulnerable = nbttagcompound.getBoolean("Invulnerable");
            this.portalCooldown = nbttagcompound.getInt("PortalCooldown");
            if (nbttagcompound.hasUUID("UUID")) {
                this.uuid = nbttagcompound.getUUID("UUID");
                this.stringUUID = this.uuid.toString();
            }

            if (Double.isFinite(this.getX()) && Double.isFinite(this.getY()) && Double.isFinite(this.getZ())) {
                if (Double.isFinite((double) this.getYRot()) && Double.isFinite((double) this.getXRot())) {
                    this.reapplyPosition();
                    this.setRot(this.getYRot(), this.getXRot());
                    if (nbttagcompound.contains("CustomName", 8)) {
                        String s = nbttagcompound.getString("CustomName");

                        try {
                            this.setCustomName(IChatBaseComponent.ChatSerializer.fromJson(s));
                        } catch (Exception exception) {
                            Entity.LOGGER.warn("Failed to parse entity custom name {}", s, exception);
                        }
                    }

                    this.setCustomNameVisible(nbttagcompound.getBoolean("CustomNameVisible"));
                    this.setSilent(nbttagcompound.getBoolean("Silent"));
                    this.setNoGravity(nbttagcompound.getBoolean("NoGravity"));
                    this.setGlowingTag(nbttagcompound.getBoolean("Glowing"));
                    this.setTicksFrozen(nbttagcompound.getInt("TicksFrozen"));
                    this.hasVisualFire = nbttagcompound.getBoolean("HasVisualFire");
                    if (nbttagcompound.contains("Tags", 9)) {
                        this.tags.clear();
                        NBTTagList nbttaglist3 = nbttagcompound.getList("Tags", 8);
                        int i = Math.min(nbttaglist3.size(), 1024);

                        for (int j = 0; j < i; ++j) {
                            this.tags.add(nbttaglist3.getString(j));
                        }
                    }

                    this.readAdditionalSaveData(nbttagcompound);
                    if (this.repositionEntityAfterLoad()) {
                        this.reapplyPosition();
                    }

                } else {
                    throw new IllegalStateException("Entity has invalid rotation");
                }
            } else {
                throw new IllegalStateException("Entity has invalid position");
            }

            // CraftBukkit start
            if (this instanceof EntityLiving) {
                EntityLiving entity = (EntityLiving) this;

                this.tickCount = nbttagcompound.getInt("Spigot.ticksLived");

                // Reset the persistence for tamed animals
                if (entity instanceof EntityTameableAnimal && !isLevelAtLeast(nbttagcompound, 2) && !nbttagcompound.getBoolean("PersistenceRequired")) {
                    EntityInsentient entityinsentient = (EntityInsentient) entity;
                    entityinsentient.setPersistenceRequired(!entityinsentient.removeWhenFarAway(0));
                }
            }
            this.persist = !nbttagcompound.contains("Bukkit.persist") || nbttagcompound.getBoolean("Bukkit.persist");
            // SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
            if (nbttagcompound.contains("Bukkit.MaxAirSupply")) {
                maxAirTicks = nbttagcompound.getInt("Bukkit.MaxAirSupply");
            }
            // CraftBukkit end

            // CraftBukkit start - Reset world
            if (this instanceof EntityPlayer) {
                Server server = Bukkit.getServer();
                org.bukkit.World bworld = null;

                // TODO: Remove World related checks, replaced with WorldUID
                String worldName = nbttagcompound.getString("world");

                if (nbttagcompound.contains("WorldUUIDMost") && nbttagcompound.contains("WorldUUIDLeast")) {
                    UUID uid = new UUID(nbttagcompound.getLong("WorldUUIDMost"), nbttagcompound.getLong("WorldUUIDLeast"));
                    bworld = server.getWorld(uid);
                } else {
                    bworld = server.getWorld(worldName);
                }

                if (bworld == null) {
                    bworld = ((org.bukkit.craftbukkit.CraftServer) server).getServer().getLevel(World.OVERWORLD).getWorld();
                }

                ((EntityPlayer) this).setLevel(bworld == null ? null : ((CraftWorld) bworld).getHandle());
            }
            this.getBukkitEntity().readBukkitValues(nbttagcompound);
            if (nbttagcompound.contains("Bukkit.invisible")) {
                boolean bukkitInvisible = nbttagcompound.getBoolean("Bukkit.invisible");
                this.setInvisible(bukkitInvisible);
                this.persistentInvisibility = bukkitInvisible;
            }
            // CraftBukkit end

        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Loading entity NBT");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Entity being loaded");

            this.fillCrashReportCategory(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Nullable
    public final String getEncodeId() {
        EntityTypes<?> entitytypes = this.getType();
        MinecraftKey minecraftkey = EntityTypes.getKey(entitytypes);

        return entitytypes.canSerialize() && minecraftkey != null ? minecraftkey.toString() : null;
    }

    protected abstract void readAdditionalSaveData(NBTTagCompound nbttagcompound);

    protected abstract void addAdditionalSaveData(NBTTagCompound nbttagcompound);

    protected NBTTagList newDoubleList(double... adouble) {
        NBTTagList nbttaglist = new NBTTagList();
        double[] adouble1 = adouble;
        int i = adouble.length;

        for (int j = 0; j < i; ++j) {
            double d0 = adouble1[j];

            nbttaglist.add(NBTTagDouble.valueOf(d0));
        }

        return nbttaglist;
    }

    protected NBTTagList newFloatList(float... afloat) {
        NBTTagList nbttaglist = new NBTTagList();
        float[] afloat1 = afloat;
        int i = afloat.length;

        for (int j = 0; j < i; ++j) {
            float f = afloat1[j];

            nbttaglist.add(NBTTagFloat.valueOf(f));
        }

        return nbttaglist;
    }

    @Nullable
    public EntityItem spawnAtLocation(IMaterial imaterial) {
        return this.spawnAtLocation(imaterial, 0);
    }

    @Nullable
    public EntityItem spawnAtLocation(IMaterial imaterial, int i) {
        return this.spawnAtLocation(new ItemStack(imaterial), (float) i);
    }

    @Nullable
    public EntityItem spawnAtLocation(ItemStack itemstack) {
        return this.spawnAtLocation(itemstack, 0.0F);
    }

    @Nullable
    public EntityItem spawnAtLocation(ItemStack itemstack, float f) {
        if (itemstack.isEmpty()) {
            return null;
        } else if (this.level.isClientSide) {
            return null;
        } else {
            // CraftBukkit start - Capture drops for death event
            if (this instanceof EntityLiving && !((EntityLiving) this).forceDrops) {
                ((EntityLiving) this).drops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(itemstack));
                return null;
            }
            // CraftBukkit end
            EntityItem entityitem = new EntityItem(this.level, this.getX(), this.getY() + (double) f, this.getZ(), itemstack);

            entityitem.setDefaultPickUpDelay();
            // CraftBukkit start
            EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) entityitem.getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return null;
            }
            // CraftBukkit end
            this.level.addFreshEntity(entityitem);
            return entityitem;
        }
    }

    public boolean isAlive() {
        return !this.isRemoved();
    }

    public boolean isInWall() {
        if (this.noPhysics) {
            return false;
        } else {
            Vec3D vec3d = this.getEyePosition();
            float f = this.dimensions.width * 0.8F;
            AxisAlignedBB axisalignedbb = AxisAlignedBB.ofSize(vec3d, (double) f, 1.0E-6D, (double) f);

            return this.level.getBlockStates(axisalignedbb).filter(Predicate.not(BlockBase.BlockData::isAir)).anyMatch((iblockdata) -> {
                BlockPosition blockposition = new BlockPosition(vec3d);

                return iblockdata.isSuffocating(this.level, blockposition) && VoxelShapes.joinIsNotEmpty(iblockdata.getCollisionShape(this.level, blockposition).move(vec3d.x, vec3d.y, vec3d.z), VoxelShapes.create(axisalignedbb), OperatorBoolean.AND);
            });
        }
    }

    public EnumInteractionResult interact(EntityHuman entityhuman, EnumHand enumhand) {
        return EnumInteractionResult.PASS;
    }

    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith() && !this.isPassengerOfSameVehicle(entity);
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public void rideTick() {
        this.setDeltaMovement(Vec3D.ZERO);
        this.tick();
        if (this.isPassenger()) {
            this.getVehicle().positionRider(this);
        }
    }

    public void positionRider(Entity entity) {
        this.positionRider(entity, Entity::setPos);
    }

    private void positionRider(Entity entity, Entity.MoveFunction entity_movefunction) {
        if (this.hasPassenger(entity)) {
            double d0 = this.getY() + this.getPassengersRidingOffset() + entity.getMyRidingOffset();

            entity_movefunction.accept(entity, this.getX(), d0, this.getZ());
        }
    }

    public void onPassengerTurned(Entity entity) {}

    public double getMyRidingOffset() {
        return 0.0D;
    }

    public double getPassengersRidingOffset() {
        return (double) this.dimensions.height * 0.75D;
    }

    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }

    public boolean showVehicleHealth() {
        return this instanceof EntityLiving;
    }

    public boolean startRiding(Entity entity, boolean flag) {
        if (entity == this.vehicle) {
            return false;
        } else {
            for (Entity entity1 = entity; entity1.vehicle != null; entity1 = entity1.vehicle) {
                if (entity1.vehicle == this) {
                    return false;
                }
            }

            if (!flag && (!this.canRide(entity) || !entity.canAddPassenger(this))) {
                return false;
            } else {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(EntityPose.STANDING);
                this.vehicle = entity;
                if (!this.vehicle.addPassenger(this)) this.vehicle = null; // CraftBukkit
                entity.getIndirectPassengersStream().filter((entity2) -> {
                    return entity2 instanceof EntityPlayer;
                }).forEach((entity2) -> {
                    CriterionTriggers.START_RIDING_TRIGGER.trigger((EntityPlayer) entity2);
                });
                return true;
            }
        }
    }

    protected boolean canRide(Entity entity) {
        return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
    }

    protected boolean canEnterPose(EntityPose entitypose) {
        return this.level.noCollision(this, this.getBoundingBoxForPose(entitypose).deflate(1.0E-7D));
    }

    public void ejectPassengers() {
        for (int i = this.passengers.size() - 1; i >= 0; --i) {
            ((Entity) this.passengers.get(i)).stopRiding();
        }

    }

    public void removeVehicle() {
        if (this.vehicle != null) {
            Entity entity = this.vehicle;

            this.vehicle = null;
            if (!entity.removePassenger(this)) this.vehicle = entity; // CraftBukkit
        }

    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected boolean addPassenger(Entity entity) { // CraftBukkit
        if (entity.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            // CraftBukkit start
            com.google.common.base.Preconditions.checkState(!entity.passengers.contains(this), "Circular entity riding! %s %s", this, entity);

            CraftEntity craft = (CraftEntity) entity.getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && entity.getBukkitEntity() instanceof LivingEntity) {
                VehicleEnterEvent event = new VehicleEnterEvent(
                        (Vehicle) getBukkitEntity(),
                         entity.getBukkitEntity()
                );
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) entity.getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityMountEvent event = new org.spigotmc.event.entity.EntityMountEvent(entity.getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(entity);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);

                if (!this.level.isClientSide && entity instanceof EntityHuman && !(this.getControllingPassenger() instanceof EntityHuman)) {
                    list.add(0, entity);
                } else {
                    list.add(entity);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

        }
        return true; // CraftBukkit
    }

    protected boolean removePassenger(Entity entity) { // CraftBukkit
        if (entity.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            // CraftBukkit start
            CraftEntity craft = (CraftEntity) entity.getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && entity.getBukkitEntity() instanceof LivingEntity) {
                VehicleExitEvent event = new VehicleExitEvent(
                        (Vehicle) getBukkitEntity(),
                        (LivingEntity) entity.getBukkitEntity()
                );
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) entity.getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityDismountEvent event = new org.spigotmc.event.entity.EntityDismountEvent(entity.getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.size() == 1 && this.passengers.get(0) == entity) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = (ImmutableList) this.passengers.stream().filter((entity1) -> {
                    return entity1 != entity;
                }).collect(ImmutableList.toImmutableList());
            }

            entity.boardingCooldown = 60;
        }
        return true; // CraftBukkit
    }

    protected boolean canAddPassenger(Entity entity) {
        return this.passengers.isEmpty();
    }

    public void lerpTo(double d0, double d1, double d2, float f, float f1, int i, boolean flag) {
        this.setPos(d0, d1, d2);
        this.setRot(f, f1);
    }

    public void lerpHeadTo(float f, int i) {
        this.setYHeadRot(f);
    }

    public float getPickRadius() {
        return 0.0F;
    }

    public Vec3D getLookAngle() {
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    public Vec2F getRotationVector() {
        return new Vec2F(this.getXRot(), this.getYRot());
    }

    public Vec3D getForward() {
        return Vec3D.directionFromRotation(this.getRotationVector());
    }

    public void handleInsidePortal(BlockPosition blockposition) {
        if (this.isOnPortalCooldown()) {
            this.setPortalCooldown();
        } else {
            if (!this.level.isClientSide && !blockposition.equals(this.portalEntrancePos)) {
                this.portalEntrancePos = blockposition.immutable();
            }

            this.isInsidePortal = true;
        }
    }

    protected void handleNetherPortal() {
        if (this.level instanceof WorldServer) {
            int i = this.getPortalWaitTime();
            WorldServer worldserver = (WorldServer) this.level;

            if (this.isInsidePortal) {
                MinecraftServer minecraftserver = worldserver.getServer();
                ResourceKey<World> resourcekey = this.level.getTypeKey() == WorldDimension.NETHER ? World.OVERWORLD : World.NETHER; // CraftBukkit
                WorldServer worldserver1 = minecraftserver.getLevel(resourcekey);

                if (true && !this.isPassenger() && this.portalTime++ >= i) { // CraftBukkit
                    this.level.getProfiler().push("portal");
                    this.portalTime = i;
                    this.setPortalCooldown();
                    // CraftBukkit start
                    if (this instanceof EntityPlayer) {
                        ((EntityPlayer) this).changeDimension(worldserver1, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                    } else {
                        this.changeDimension(worldserver1);
                    }
                    // CraftBukkit end
                    this.level.getProfiler().pop();
                }

                this.isInsidePortal = false;
            } else {
                if (this.portalTime > 0) {
                    this.portalTime -= 4;
                }

                if (this.portalTime < 0) {
                    this.portalTime = 0;
                }
            }

            this.processPortalCooldown();
        }
    }

    public int getDimensionChangingDelay() {
        return 300;
    }

    public void lerpMotion(double d0, double d1, double d2) {
        this.setDeltaMovement(d0, d1, d2);
    }

    public void handleEntityEvent(byte b0) {
        switch (b0) {
            case 53:
                BlockHoney.showSlideParticles(this);
            default:
        }
    }

    public void animateHurt() {}

    public Iterable<ItemStack> getHandSlots() {
        return Entity.EMPTY_LIST;
    }

    public Iterable<ItemStack> getArmorSlots() {
        return Entity.EMPTY_LIST;
    }

    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorSlots());
    }

    public void setItemSlot(EnumItemSlot enumitemslot, ItemStack itemstack) {}

    public boolean isOnFire() {
        boolean flag = this.level != null && this.level.isClientSide;

        return !this.fireImmune() && (this.remainingFireTicks > 0 || flag && this.getSharedFlag(0));
    }

    public boolean isPassenger() {
        return this.getVehicle() != null;
    }

    public boolean isVehicle() {
        return !this.passengers.isEmpty();
    }

    public boolean rideableUnderWater() {
        return true;
    }

    public void setShiftKeyDown(boolean flag) {
        this.setSharedFlag(1, flag);
    }

    public boolean isShiftKeyDown() {
        return this.getSharedFlag(1);
    }

    public boolean isSteppingCarefully() {
        return this.isShiftKeyDown();
    }

    public boolean isSuppressingBounce() {
        return this.isShiftKeyDown();
    }

    public boolean isDiscrete() {
        return this.isShiftKeyDown();
    }

    public boolean isDescending() {
        return this.isShiftKeyDown();
    }

    public boolean isCrouching() {
        return this.getPose() == EntityPose.CROUCHING;
    }

    public boolean isSprinting() {
        return this.getSharedFlag(3);
    }

    public void setSprinting(boolean flag) {
        this.setSharedFlag(3, flag);
    }

    public boolean isSwimming() {
        return this.getSharedFlag(4);
    }

    public boolean isVisuallySwimming() {
        return this.getPose() == EntityPose.SWIMMING;
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWater();
    }

    public void setSwimming(boolean flag) {
        // CraftBukkit start
        if (valid && this.isSwimming() != flag && this instanceof EntityLiving) {
            if (CraftEventFactory.callToggleSwimEvent((EntityLiving) this, flag).isCancelled()) {
                return;
            }
        }
        // CraftBukkit end
        this.setSharedFlag(4, flag);
    }

    public final boolean hasGlowingTag() {
        return this.hasGlowingTag;
    }

    public final void setGlowingTag(boolean flag) {
        this.hasGlowingTag = flag;
        this.setSharedFlag(6, this.isCurrentlyGlowing());
    }

    public boolean isCurrentlyGlowing() {
        return this.level.isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
    }

    public boolean isInvisible() {
        return this.getSharedFlag(5);
    }

    public boolean isInvisibleTo(EntityHuman entityhuman) {
        if (entityhuman.isSpectator()) {
            return false;
        } else {
            ScoreboardTeamBase scoreboardteambase = this.getTeam();

            return scoreboardteambase != null && entityhuman != null && entityhuman.getTeam() == scoreboardteambase && scoreboardteambase.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    @Nullable
    public GameEventListenerRegistrar getGameEventListenerRegistrar() {
        return null;
    }

    @Nullable
    public ScoreboardTeamBase getTeam() {
        return this.level.getScoreboard().getPlayersTeam(this.getScoreboardName());
    }

    public boolean isAlliedTo(Entity entity) {
        return this.isAlliedTo(entity.getTeam());
    }

    public boolean isAlliedTo(ScoreboardTeamBase scoreboardteambase) {
        return this.getTeam() != null ? this.getTeam().isAlliedTo(scoreboardteambase) : false;
    }

    // CraftBukkit - start
    public void setInvisible(boolean flag) {
        if (!this.persistentInvisibility) { // Prevent Minecraft from removing our invisibility flag
            this.setSharedFlag(5, flag);
        }
        // CraftBukkit - end
    }

    public boolean getSharedFlag(int i) {
        return ((Byte) this.entityData.get(Entity.DATA_SHARED_FLAGS_ID) & 1 << i) != 0;
    }

    public void setSharedFlag(int i, boolean flag) {
        byte b0 = (Byte) this.entityData.get(Entity.DATA_SHARED_FLAGS_ID);

        if (flag) {
            this.entityData.set(Entity.DATA_SHARED_FLAGS_ID, (byte) (b0 | 1 << i));
        } else {
            this.entityData.set(Entity.DATA_SHARED_FLAGS_ID, (byte) (b0 & ~(1 << i)));
        }

    }

    public int getMaxAirSupply() {
        return maxAirTicks; // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    }

    public int getAirSupply() {
        return (Integer) this.entityData.get(Entity.DATA_AIR_SUPPLY_ID);
    }

    public void setAirSupply(int i) {
        // CraftBukkit start
        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), i);
        // Suppress during worldgen
        if (this.valid) {
            event.getEntity().getServer().getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            return;
        }
        this.entityData.set(Entity.DATA_AIR_SUPPLY_ID, event.getAmount());
        // CraftBukkit end
    }

    public int getTicksFrozen() {
        return (Integer) this.entityData.get(Entity.DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int i) {
        this.entityData.set(Entity.DATA_TICKS_FROZEN, i);
    }

    public float getPercentFrozen() {
        int i = this.getTicksRequiredToFreeze();

        return (float) Math.min(this.getTicksFrozen(), i) / (float) i;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void thunderHit(WorldServer worldserver, EntityLightning entitylightning) {
        this.setRemainingFireTicks(this.remainingFireTicks + 1);
        // CraftBukkit start
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = entitylightning.getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit end

        if (this.remainingFireTicks == 0) {
            // CraftBukkit start - Call a combust event when lightning strikes
            EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
            pluginManager.callEvent(entityCombustEvent);
            if (!entityCombustEvent.isCancelled()) {
                this.setSecondsOnFire(entityCombustEvent.getDuration(), false);
            }
            // CraftBukkit end
        }

        // CraftBukkit start
        if (thisBukkitEntity instanceof Hanging) {
            HangingBreakByEntityEvent hangingEvent = new HangingBreakByEntityEvent((Hanging) thisBukkitEntity, stormBukkitEntity);
            pluginManager.callEvent(hangingEvent);

            if (hangingEvent.isCancelled()) {
                return;
            }
        }

        if (this.fireImmune()) {
            return;
        }
        CraftEventFactory.entityDamage = entitylightning;
        if (!this.hurt(DamageSource.LIGHTNING_BOLT, 5.0F)) {
            CraftEventFactory.entityDamage = null;
            return;
        }
        // CraftBukkit end
    }

    public void onAboveBubbleCol(boolean flag) {
        Vec3D vec3d = this.getDeltaMovement();
        double d0;

        if (flag) {
            d0 = Math.max(-0.9D, vec3d.y - 0.03D);
        } else {
            d0 = Math.min(1.8D, vec3d.y + 0.1D);
        }

        this.setDeltaMovement(vec3d.x, d0, vec3d.z);
    }

    public void onInsideBubbleColumn(boolean flag) {
        Vec3D vec3d = this.getDeltaMovement();
        double d0;

        if (flag) {
            d0 = Math.max(-0.3D, vec3d.y - 0.03D);
        } else {
            d0 = Math.min(0.7D, vec3d.y + 0.06D);
        }

        this.setDeltaMovement(vec3d.x, d0, vec3d.z);
        this.resetFallDistance();
    }

    public void killed(WorldServer worldserver, EntityLiving entityliving) {}

    public void resetFallDistance() {
        this.fallDistance = 0.0F;
    }

    protected void moveTowardsClosestSpace(double d0, double d1, double d2) {
        BlockPosition blockposition = new BlockPosition(d0, d1, d2);
        Vec3D vec3d = new Vec3D(d0 - (double) blockposition.getX(), d1 - (double) blockposition.getY(), d2 - (double) blockposition.getZ());
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
        EnumDirection enumdirection = EnumDirection.UP;
        double d3 = Double.MAX_VALUE;
        EnumDirection[] aenumdirection = new EnumDirection[]{EnumDirection.NORTH, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.EAST, EnumDirection.UP};
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            EnumDirection enumdirection1 = aenumdirection[j];

            blockposition_mutableblockposition.setWithOffset(blockposition, enumdirection1);
            if (!this.level.getBlockState(blockposition_mutableblockposition).isCollisionShapeFullBlock(this.level, blockposition_mutableblockposition)) {
                double d4 = vec3d.get(enumdirection1.getAxis());
                double d5 = enumdirection1.getAxisDirection() == EnumDirection.EnumAxisDirection.POSITIVE ? 1.0D - d4 : d4;

                if (d5 < d3) {
                    d3 = d5;
                    enumdirection = enumdirection1;
                }
            }
        }

        float f = this.random.nextFloat() * 0.2F + 0.1F;
        float f1 = (float) enumdirection.getAxisDirection().getStep();
        Vec3D vec3d1 = this.getDeltaMovement().scale(0.75D);

        if (enumdirection.getAxis() == EnumDirection.EnumAxis.X) {
            this.setDeltaMovement((double) (f1 * f), vec3d1.y, vec3d1.z);
        } else if (enumdirection.getAxis() == EnumDirection.EnumAxis.Y) {
            this.setDeltaMovement(vec3d1.x, (double) (f1 * f), vec3d1.z);
        } else if (enumdirection.getAxis() == EnumDirection.EnumAxis.Z) {
            this.setDeltaMovement(vec3d1.x, vec3d1.y, (double) (f1 * f));
        }

    }

    public void makeStuckInBlock(IBlockData iblockdata, Vec3D vec3d) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = vec3d;
    }

    private static IChatBaseComponent removeAction(IChatBaseComponent ichatbasecomponent) {
        IChatMutableComponent ichatmutablecomponent = ichatbasecomponent.plainCopy().setStyle(ichatbasecomponent.getStyle().withClickEvent((ChatClickable) null));
        Iterator iterator = ichatbasecomponent.getSiblings().iterator();

        while (iterator.hasNext()) {
            IChatBaseComponent ichatbasecomponent1 = (IChatBaseComponent) iterator.next();

            ichatmutablecomponent.append(removeAction(ichatbasecomponent1));
        }

        return ichatmutablecomponent;
    }

    @Override
    public IChatBaseComponent getName() {
        IChatBaseComponent ichatbasecomponent = this.getCustomName();

        return ichatbasecomponent != null ? removeAction(ichatbasecomponent) : this.getTypeName();
    }

    protected IChatBaseComponent getTypeName() {
        return this.type.getDescription();
    }

    public boolean is(Entity entity) {
        return this == entity;
    }

    public float getYHeadRot() {
        return 0.0F;
    }

    public void setYHeadRot(float f) {}

    public void setYBodyRot(float f) {}

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity entity) {
        return false;
    }

    public String toString() {
        String s = this.level == null ? "~NULL~" : this.level.toString();

        return this.removalReason != null ? String.format(Locale.ROOT, "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]", this.getClass().getSimpleName(), this.getName().getString(), this.id, s, this.getX(), this.getY(), this.getZ(), this.removalReason) : String.format(Locale.ROOT, "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]", this.getClass().getSimpleName(), this.getName().getString(), this.id, s, this.getX(), this.getY(), this.getZ());
    }

    public boolean isInvulnerableTo(DamageSource damagesource) {
        return this.isRemoved() || this.invulnerable && damagesource != DamageSource.OUT_OF_WORLD && !damagesource.isCreativePlayer();
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean flag) {
        this.invulnerable = flag;
    }

    public void copyPosition(Entity entity) {
        this.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    public void restoreFrom(Entity entity) {
        NBTTagCompound nbttagcompound = entity.saveWithoutId(new NBTTagCompound());

        nbttagcompound.remove("Dimension");
        this.load(nbttagcompound);
        this.portalCooldown = entity.portalCooldown;
        this.portalEntrancePos = entity.portalEntrancePos;
    }

    @Nullable
    public Entity changeDimension(WorldServer worldserver) {
        // CraftBukkit start
        return teleportTo(worldserver, null);
    }

    @Nullable
    public Entity teleportTo(WorldServer worldserver, BlockPosition location) {
        // CraftBukkit end
        if (this.level instanceof WorldServer && !this.isRemoved()) {
            this.level.getProfiler().push("changeDimension");
            // CraftBukkit start
            // this.decouple();
            if (worldserver == null) {
                return null;
            }
            // CraftBukkit end
            this.level.getProfiler().push("reposition");
            ShapeDetectorShape shapedetectorshape = (location == null) ? this.findDimensionEntryPoint(worldserver) : new ShapeDetectorShape(new Vec3D(location.getX(), location.getY(), location.getZ()), Vec3D.ZERO, this.yRot, this.xRot, worldserver, null); // CraftBukkit

            if (shapedetectorshape == null) {
                return null;
            } else {
                // CraftBukkit start
                worldserver = shapedetectorshape.world;
                this.unRide();
                // CraftBukkit end

                this.level.getProfiler().popPush("reloading");
                Entity entity = this.getType().create(worldserver);

                if (entity != null) {
                    entity.restoreFrom(this);
                    entity.moveTo(shapedetectorshape.pos.x, shapedetectorshape.pos.y, shapedetectorshape.pos.z, shapedetectorshape.yRot, entity.getXRot());
                    entity.setDeltaMovement(shapedetectorshape.speed);
                    worldserver.addDuringTeleport(entity);
                    if (worldserver.getTypeKey() == WorldDimension.END) { // CraftBukkit
                        WorldServer.makeObsidianPlatform(worldserver, this); // CraftBukkit
                    }
                    // CraftBukkit start - Forward the CraftEntity to the new entity
                    this.getBukkitEntity().setHandle(entity);
                    entity.bukkitEntity = this.getBukkitEntity();

                    if (this instanceof EntityInsentient) {
                        ((EntityInsentient) this).dropLeash(true, false); // Unleash to prevent duping of leads.
                    }
                    // CraftBukkit end
                }

                this.removeAfterChangingDimensions();
                this.level.getProfiler().pop();
                ((WorldServer) this.level).resetEmptyTime();
                worldserver.resetEmptyTime();
                this.level.getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

    protected void removeAfterChangingDimensions() {
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
    }

    @Nullable
    protected ShapeDetectorShape findDimensionEntryPoint(WorldServer worldserver) {
        // CraftBukkit start
        if (worldserver == null) {
            return null;
        }
        boolean flag = this.level.getTypeKey() == WorldDimension.END && worldserver.getTypeKey() == WorldDimension.OVERWORLD; // fromEndToOverworld
        boolean flag1 = worldserver.getTypeKey() == WorldDimension.END; // targetIsEnd
        // CraftBukkit end

        if (!flag && !flag1) {
            boolean flag2 = worldserver.getTypeKey() == WorldDimension.NETHER; // CraftBukkit

            if (this.level.getTypeKey() != WorldDimension.NETHER && !flag2) { // CraftBukkit
                return null;
            } else {
                WorldBorder worldborder = worldserver.getWorldBorder();
                double d0 = DimensionManager.getTeleportationScale(this.level.dimensionType(), worldserver.dimensionType());
                BlockPosition blockposition = worldborder.clampToBounds(this.getX() * d0, this.getY(), this.getZ() * d0);
                // CraftBukkit start
                CraftPortalEvent event = callPortalEvent(this, worldserver, blockposition, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, flag2 ? 16 : 128, 16);
                if (event == null) {
                    return null;
                }
                final WorldServer worldserverFinal = worldserver = ((CraftWorld) event.getTo().getWorld()).getHandle();
                worldborder = worldserverFinal.getWorldBorder();
                blockposition = worldborder.clampToBounds(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

                return (ShapeDetectorShape) this.getExitPortal(worldserver, blockposition, flag2, worldborder, event.getSearchRadius(), event.getCanCreatePortal(), event.getCreationRadius()).map((blockutil_rectangle) -> {
                    // CraftBukkit end
                    IBlockData iblockdata = this.level.getBlockState(this.portalEntrancePos);
                    EnumDirection.EnumAxis enumdirection_enumaxis;
                    Vec3D vec3d;

                    if (iblockdata.hasProperty(BlockProperties.HORIZONTAL_AXIS)) {
                        enumdirection_enumaxis = (EnumDirection.EnumAxis) iblockdata.getValue(BlockProperties.HORIZONTAL_AXIS);
                        BlockUtil.Rectangle blockutil_rectangle1 = BlockUtil.getLargestRectangleAround(this.portalEntrancePos, enumdirection_enumaxis, 21, EnumDirection.EnumAxis.Y, 21, (blockposition1) -> {
                            return this.level.getBlockState(blockposition1) == iblockdata;
                        });

                        vec3d = this.getRelativePortalPosition(enumdirection_enumaxis, blockutil_rectangle1);
                    } else {
                        enumdirection_enumaxis = EnumDirection.EnumAxis.X;
                        vec3d = new Vec3D(0.5D, 0.0D, 0.0D);
                    }

                    return BlockPortalShape.createPortalInfo(worldserverFinal, blockutil_rectangle, enumdirection_enumaxis, vec3d, this.getDimensions(this.getPose()), this.getDeltaMovement(), this.getYRot(), this.getXRot(), event); // CraftBukkit
                }).orElse(null); // CraftBuukkit - decompile error
            }
        } else {
            BlockPosition blockposition1;

            if (flag1) {
                blockposition1 = WorldServer.END_SPAWN_POINT;
            } else {
                blockposition1 = worldserver.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, worldserver.getSharedSpawnPos());
            }
            // CraftBukkit start
            CraftPortalEvent event = callPortalEvent(this, worldserver, blockposition1, PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
            if (event == null) {
                return null;
            }
            blockposition1 = new BlockPosition(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

            return new ShapeDetectorShape(new Vec3D((double) blockposition1.getX() + 0.5D, (double) blockposition1.getY(), (double) blockposition1.getZ() + 0.5D), this.getDeltaMovement(), this.getYRot(), this.getXRot(), ((CraftWorld) event.getTo().getWorld()).getHandle(), event);
            // CraftBukkit end
        }
    }

    protected Vec3D getRelativePortalPosition(EnumDirection.EnumAxis enumdirection_enumaxis, BlockUtil.Rectangle blockutil_rectangle) {
        return BlockPortalShape.getRelativePosition(blockutil_rectangle, enumdirection_enumaxis, this.position(), this.getDimensions(this.getPose()));
    }

    // CraftBukkit start
    protected CraftPortalEvent callPortalEvent(Entity entity, WorldServer exitWorldServer, BlockPosition exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
        Location enter = bukkitEntity.getLocation();
        Location exit = new Location(exitWorldServer.getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ());

        EntityPortalEvent event = new EntityPortalEvent(bukkitEntity, enter, exit, searchRadius);
        event.getEntity().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !entity.isAlive()) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    protected Optional<BlockUtil.Rectangle> getExitPortal(WorldServer worldserver, BlockPosition blockposition, boolean flag, WorldBorder worldborder, int searchRadius, boolean canCreatePortal, int createRadius) {
        return worldserver.getPortalForcer().findPortalAround(blockposition, worldborder, searchRadius);
        // CraftBukkit end
    }

    public boolean canChangeDimensions() {
        return true;
    }

    public float getBlockExplosionResistance(Explosion explosion, IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, Fluid fluid, float f) {
        return f;
    }

    public boolean shouldBlockExplode(Explosion explosion, IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, float f) {
        return true;
    }

    public int getMaxFallDistance() {
        return 3;
    }

    public boolean isIgnoringBlockTriggers() {
        return false;
    }

    public void fillCrashReportCategory(CrashReportSystemDetails crashreportsystemdetails) {
        crashreportsystemdetails.setDetail("Entity Type", () -> {
            MinecraftKey minecraftkey = EntityTypes.getKey(this.getType());

            return minecraftkey + " (" + this.getClass().getCanonicalName() + ")";
        });
        crashreportsystemdetails.setDetail("Entity ID", (Object) this.id);
        crashreportsystemdetails.setDetail("Entity Name", () -> {
            return this.getName().getString();
        });
        crashreportsystemdetails.setDetail("Entity's Exact location", (Object) String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        crashreportsystemdetails.setDetail("Entity's Block location", (Object) CrashReportSystemDetails.formatLocation(this.level, MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ())));
        Vec3D vec3d = this.getDeltaMovement();

        crashreportsystemdetails.setDetail("Entity's Momentum", (Object) String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3d.x, vec3d.y, vec3d.z));
        crashreportsystemdetails.setDetail("Entity's Passengers", () -> {
            return this.getPassengers().toString();
        });
        crashreportsystemdetails.setDetail("Entity's Vehicle", () -> {
            return String.valueOf(this.getVehicle());
        });
    }

    public boolean displayFireAnimation() {
        return this.isOnFire() && !this.isSpectator();
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
        this.stringUUID = this.uuid.toString();
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public String getStringUUID() {
        return this.stringUUID;
    }

    public String getScoreboardName() {
        return this.stringUUID;
    }

    public boolean isPushedByFluid() {
        return true;
    }

    public static double getViewScale() {
        return Entity.viewScale;
    }

    public static void setViewScale(double d0) {
        Entity.viewScale = d0;
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return ScoreboardTeam.formatNameForTeam(this.getTeam(), this.getName()).withStyle((chatmodifier) -> {
            return chatmodifier.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID());
        });
    }

    public void setCustomName(@Nullable IChatBaseComponent ichatbasecomponent) {
        this.entityData.set(Entity.DATA_CUSTOM_NAME, Optional.ofNullable(ichatbasecomponent));
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return (IChatBaseComponent) ((Optional) this.entityData.get(Entity.DATA_CUSTOM_NAME)).orElse((Object) null);
    }

    @Override
    public boolean hasCustomName() {
        return ((Optional) this.entityData.get(Entity.DATA_CUSTOM_NAME)).isPresent();
    }

    public void setCustomNameVisible(boolean flag) {
        this.entityData.set(Entity.DATA_CUSTOM_NAME_VISIBLE, flag);
    }

    public boolean isCustomNameVisible() {
        return (Boolean) this.entityData.get(Entity.DATA_CUSTOM_NAME_VISIBLE);
    }

    public final void teleportToWithTicket(double d0, double d1, double d2) {
        if (this.level instanceof WorldServer) {
            ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(new BlockPosition(d0, d1, d2));

            ((WorldServer) this.level).getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkcoordintpair, 0, this.getId());
            this.level.getChunk(chunkcoordintpair.x, chunkcoordintpair.z);
            this.teleportTo(d0, d1, d2);
        }
    }

    public void dismountTo(double d0, double d1, double d2) {
        this.teleportTo(d0, d1, d2);
    }

    public void teleportTo(double d0, double d1, double d2) {
        if (this.level instanceof WorldServer) {
            this.moveTo(d0, d1, d2, this.getYRot(), this.getXRot());
            this.getSelfAndPassengers().forEach((entity) -> {
                UnmodifiableIterator unmodifiableiterator = entity.passengers.iterator();

                while (unmodifiableiterator.hasNext()) {
                    Entity entity1 = (Entity) unmodifiableiterator.next();

                    entity.positionRider(entity1, Entity::moveTo);
                }

            });
        }
    }

    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    public void onSyncedDataUpdated(DataWatcherObject<?> datawatcherobject) {
        if (Entity.DATA_POSE.equals(datawatcherobject)) {
            this.refreshDimensions();
        }

    }

    public void refreshDimensions() {
        EntitySize entitysize = this.dimensions;
        EntityPose entitypose = this.getPose();
        EntitySize entitysize1 = this.getDimensions(entitypose);

        this.dimensions = entitysize1;
        this.eyeHeight = this.getEyeHeight(entitypose, entitysize1);
        this.reapplyPosition();
        boolean flag = (double) entitysize1.width <= 4.0D && (double) entitysize1.height <= 4.0D;

        if (!this.level.isClientSide && !this.firstTick && !this.noPhysics && flag && (entitysize1.width > entitysize.width || entitysize1.height > entitysize.height) && !(this instanceof EntityHuman)) {
            Vec3D vec3d = this.position().add(0.0D, (double) entitysize.height / 2.0D, 0.0D);
            double d0 = (double) Math.max(0.0F, entitysize1.width - entitysize.width) + 1.0E-6D;
            double d1 = (double) Math.max(0.0F, entitysize1.height - entitysize.height) + 1.0E-6D;
            VoxelShape voxelshape = VoxelShapes.create(AxisAlignedBB.ofSize(vec3d, d0, d1, d0));

            this.level.findFreePosition(this, voxelshape, vec3d, (double) entitysize1.width, (double) entitysize1.height, (double) entitysize1.width).ifPresent((vec3d1) -> {
                this.setPos(vec3d1.add(0.0D, (double) (-entitysize1.height) / 2.0D, 0.0D));
            });
        }

    }

    public EnumDirection getDirection() {
        return EnumDirection.fromYRot((double) this.getYRot());
    }

    public EnumDirection getMotionDirection() {
        return this.getDirection();
    }

    protected ChatHoverable createHoverEvent() {
        return new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_ENTITY, new ChatHoverable.b(this.getType(), this.getUUID(), this.getName()));
    }

    public boolean broadcastToPlayer(EntityPlayer entityplayer) {
        return true;
    }

    @Override
    public final AxisAlignedBB getBoundingBox() {
        return this.bb;
    }

    public AxisAlignedBB getBoundingBoxForCulling() {
        return this.getBoundingBox();
    }

    protected AxisAlignedBB getBoundingBoxForPose(EntityPose entitypose) {
        EntitySize entitysize = this.getDimensions(entitypose);
        float f = entitysize.width / 2.0F;
        Vec3D vec3d = new Vec3D(this.getX() - (double) f, this.getY(), this.getZ() - (double) f);
        Vec3D vec3d1 = new Vec3D(this.getX() + (double) f, this.getY() + (double) entitysize.height, this.getZ() + (double) f);

        return new AxisAlignedBB(vec3d, vec3d1);
    }

    public final void setBoundingBox(AxisAlignedBB axisalignedbb) {
        // CraftBukkit start - block invalid bounding boxes
        double minX = axisalignedbb.minX,
                minY = axisalignedbb.minY,
                minZ = axisalignedbb.minZ,
                maxX = axisalignedbb.maxX,
                maxY = axisalignedbb.maxY,
                maxZ = axisalignedbb.maxZ;
        double len = axisalignedbb.maxX - axisalignedbb.minX;
        if (len < 0) maxX = minX;
        if (len > 64) maxX = minX + 64.0;

        len = axisalignedbb.maxY - axisalignedbb.minY;
        if (len < 0) maxY = minY;
        if (len > 64) maxY = minY + 64.0;

        len = axisalignedbb.maxZ - axisalignedbb.minZ;
        if (len < 0) maxZ = minZ;
        if (len > 64) maxZ = minZ + 64.0;
        this.bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        // CraftBukkit end
    }

    protected float getEyeHeight(EntityPose entitypose, EntitySize entitysize) {
        return entitysize.height * 0.85F;
    }

    public float getEyeHeight(EntityPose entitypose) {
        return this.getEyeHeight(entitypose, this.getDimensions(entitypose));
    }

    public final float getEyeHeight() {
        return this.eyeHeight;
    }

    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double) this.getEyeHeight(), (double) (this.getBbWidth() * 0.4F));
    }

    public SlotAccess getSlot(int i) {
        return SlotAccess.NULL;
    }

    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {}

    public World getCommandSenderWorld() {
        return this.level;
    }

    @Nullable
    public MinecraftServer getServer() {
        return this.level.getServer();
    }

    public EnumInteractionResult interactAt(EntityHuman entityhuman, Vec3D vec3d, EnumHand enumhand) {
        return EnumInteractionResult.PASS;
    }

    public boolean ignoreExplosion() {
        return false;
    }

    public void doEnchantDamageEffects(EntityLiving entityliving, Entity entity) {
        if (entity instanceof EntityLiving) {
            EnchantmentManager.doPostHurtEffects((EntityLiving) entity, entityliving);
        }

        EnchantmentManager.doPostDamageEffects(entityliving, entity);
    }

    public void startSeenByPlayer(EntityPlayer entityplayer) {}

    public void stopSeenByPlayer(EntityPlayer entityplayer) {}

    public float rotate(EnumBlockRotation enumblockrotation) {
        float f = MathHelper.wrapDegrees(this.getYRot());

        switch (enumblockrotation) {
            case CLOCKWISE_180:
                return f + 180.0F;
            case COUNTERCLOCKWISE_90:
                return f + 270.0F;
            case CLOCKWISE_90:
                return f + 90.0F;
            default:
                return f;
        }
    }

    public float mirror(EnumBlockMirror enumblockmirror) {
        float f = MathHelper.wrapDegrees(this.getYRot());

        switch (enumblockmirror) {
            case LEFT_RIGHT:
                return -f;
            case FRONT_BACK:
                return 180.0F - f;
            default:
                return f;
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    @Nullable
    public Entity getControllingPassenger() {
        return null;
    }

    public final List<Entity> getPassengers() {
        return this.passengers;
    }

    @Nullable
    public Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : (Entity) this.passengers.get(0);
    }

    public boolean hasPassenger(Entity entity) {
        return this.passengers.contains(entity);
    }

    public boolean hasPassenger(Predicate<Entity> predicate) {
        UnmodifiableIterator unmodifiableiterator = this.passengers.iterator();

        Entity entity;

        do {
            if (!unmodifiableiterator.hasNext()) {
                return false;
            }

            entity = (Entity) unmodifiableiterator.next();
        } while (!predicate.test(entity));

        return true;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
    }

    @Override
    public Stream<Entity> getSelfAndPassengers() {
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getIndirectPassengers() {
        return () -> {
            return this.getIndirectPassengersStream().iterator();
        };
    }

    public boolean hasExactlyOnePlayerPassenger() {
        return this.getIndirectPassengersStream().filter((entity) -> {
            return entity instanceof EntityHuman;
        }).count() == 1L;
    }

    public Entity getRootVehicle() {
        Entity entity;

        for (entity = this; entity.isPassenger(); entity = entity.getVehicle()) {
            ;
        }

        return entity;
    }

    public boolean isPassengerOfSameVehicle(Entity entity) {
        return this.getRootVehicle() == entity.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity entity) {
        return this.getIndirectPassengersStream().anyMatch((entity1) -> {
            return entity1 == entity;
        });
    }

    public boolean isControlledByLocalInstance() {
        Entity entity = this.getControllingPassenger();

        return entity instanceof EntityHuman ? ((EntityHuman) entity).isLocalPlayer() : !this.level.isClientSide;
    }

    protected static Vec3D getCollisionHorizontalEscapeVector(double d0, double d1, float f) {
        double d2 = (d0 + d1 + 9.999999747378752E-6D) / 2.0D;
        float f1 = -MathHelper.sin(f * 0.017453292F);
        float f2 = MathHelper.cos(f * 0.017453292F);
        float f3 = Math.max(Math.abs(f1), Math.abs(f2));

        return new Vec3D((double) f1 * d2 / (double) f3, 0.0D, (double) f2 * d2 / (double) f3);
    }

    public Vec3D getDismountLocationForPassenger(EntityLiving entityliving) {
        return new Vec3D(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Nullable
    public Entity getVehicle() {
        return this.vehicle;
    }

    public EnumPistonReaction getPistonPushReaction() {
        return EnumPistonReaction.NORMAL;
    }

    public SoundCategory getSoundSource() {
        return SoundCategory.NEUTRAL;
    }

    public int getFireImmuneTicks() {
        return 1;
    }

    public CommandListenerWrapper createCommandSourceStack() {
        return new CommandListenerWrapper(this, this.position(), this.getRotationVector(), this.level instanceof WorldServer ? (WorldServer) this.level : null, this.getPermissionLevel(), this.getName().getString(), this.getDisplayName(), this.level.getServer(), this);
    }

    protected int getPermissionLevel() {
        return 0;
    }

    public boolean hasPermissions(int i) {
        return this.getPermissionLevel() >= i;
    }

    @Override
    public boolean acceptsSuccess() {
        return this.level.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }

    public void lookAt(ArgumentAnchor.Anchor argumentanchor_anchor, Vec3D vec3d) {
        Vec3D vec3d1 = argumentanchor_anchor.apply(this);
        double d0 = vec3d.x - vec3d1.x;
        double d1 = vec3d.y - vec3d1.y;
        double d2 = vec3d.z - vec3d1.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        this.setXRot(MathHelper.wrapDegrees((float) (-(MathHelper.atan2(d1, d3) * 57.2957763671875D))));
        this.setYRot(MathHelper.wrapDegrees((float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F));
        this.setYHeadRot(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public boolean updateFluidHeightAndDoFluidPushing(Tag<FluidType> tag, double d0) {
        if (this.touchingUnloadedChunk()) {
            return false;
        } else {
            AxisAlignedBB axisalignedbb = this.getBoundingBox().deflate(0.001D);
            int i = MathHelper.floor(axisalignedbb.minX);
            int j = MathHelper.ceil(axisalignedbb.maxX);
            int k = MathHelper.floor(axisalignedbb.minY);
            int l = MathHelper.ceil(axisalignedbb.maxY);
            int i1 = MathHelper.floor(axisalignedbb.minZ);
            int j1 = MathHelper.ceil(axisalignedbb.maxZ);
            double d1 = 0.0D;
            boolean flag = this.isPushedByFluid();
            boolean flag1 = false;
            Vec3D vec3d = Vec3D.ZERO;
            int k1 = 0;
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = k; i2 < l; ++i2) {
                    for (int j2 = i1; j2 < j1; ++j2) {
                        blockposition_mutableblockposition.set(l1, i2, j2);
                        Fluid fluid = this.level.getFluidState(blockposition_mutableblockposition);

                        if (fluid.is(tag)) {
                            double d2 = (double) ((float) i2 + fluid.getHeight(this.level, blockposition_mutableblockposition));

                            if (d2 >= axisalignedbb.minY) {
                                flag1 = true;
                                d1 = Math.max(d2 - axisalignedbb.minY, d1);
                                if (flag) {
                                    Vec3D vec3d1 = fluid.getFlow(this.level, blockposition_mutableblockposition);

                                    if (d1 < 0.4D) {
                                        vec3d1 = vec3d1.scale(d1);
                                    }

                                    vec3d = vec3d.add(vec3d1);
                                    ++k1;
                                }
                                // CraftBukkit start - store last lava contact location
                                if (tag == TagsFluid.LAVA) {
                                    this.lastLavaContact = blockposition_mutableblockposition.immutable();
                                }
                                // CraftBukkit end
                            }
                        }
                    }
                }
            }

            if (vec3d.length() > 0.0D) {
                if (k1 > 0) {
                    vec3d = vec3d.scale(1.0D / (double) k1);
                }

                if (!(this instanceof EntityHuman)) {
                    vec3d = vec3d.normalize();
                }

                Vec3D vec3d2 = this.getDeltaMovement();

                vec3d = vec3d.scale(d0 * 1.0D);
                double d3 = 0.003D;

                if (Math.abs(vec3d2.x) < 0.003D && Math.abs(vec3d2.z) < 0.003D && vec3d.length() < 0.0045000000000000005D) {
                    vec3d = vec3d.normalize().scale(0.0045000000000000005D);
                }

                this.setDeltaMovement(this.getDeltaMovement().add(vec3d));
            }

            this.fluidHeight.put(tag, d1);
            return flag1;
        }
    }

    public boolean touchingUnloadedChunk() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox().inflate(1.0D);
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.minZ);
        int l = MathHelper.ceil(axisalignedbb.maxZ);

        return !this.level.hasChunksAt(i, k, j, l);
    }

    public double getFluidHeight(Tag<FluidType> tag) {
        return this.fluidHeight.getDouble(tag);
    }

    public double getFluidJumpThreshold() {
        return (double) this.getEyeHeight() < 0.4D ? 0.0D : 0.4D;
    }

    public final float getBbWidth() {
        return this.dimensions.width;
    }

    public final float getBbHeight() {
        return this.dimensions.height;
    }

    public abstract Packet<?> getAddEntityPacket();

    public EntitySize getDimensions(EntityPose entitypose) {
        return this.type.getDimensions();
    }

    public Vec3D position() {
        return this.position;
    }

    @Override
    public BlockPosition blockPosition() {
        return this.blockPosition;
    }

    public IBlockData getFeetBlockState() {
        if (this.feetBlockState == null) {
            this.feetBlockState = this.level.getBlockState(this.blockPosition());
        }

        return this.feetBlockState;
    }

    public BlockPosition eyeBlockPosition() {
        return new BlockPosition(this.getEyePosition(1.0F));
    }

    public ChunkCoordIntPair chunkPosition() {
        return this.chunkPosition;
    }

    public Vec3D getDeltaMovement() {
        return this.deltaMovement;
    }

    public void setDeltaMovement(Vec3D vec3d) {
        this.deltaMovement = vec3d;
    }

    public void setDeltaMovement(double d0, double d1, double d2) {
        this.setDeltaMovement(new Vec3D(d0, d1, d2));
    }

    public final int getBlockX() {
        return this.blockPosition.getX();
    }

    public final double getX() {
        return this.position.x;
    }

    public double getX(double d0) {
        return this.position.x + (double) this.getBbWidth() * d0;
    }

    public double getRandomX(double d0) {
        return this.getX((2.0D * this.random.nextDouble() - 1.0D) * d0);
    }

    public final int getBlockY() {
        return this.blockPosition.getY();
    }

    public final double getY() {
        return this.position.y;
    }

    public double getY(double d0) {
        return this.position.y + (double) this.getBbHeight() * d0;
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getEyeY() {
        return this.position.y + (double) this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double getZ() {
        return this.position.z;
    }

    public double getZ(double d0) {
        return this.position.z + (double) this.getBbWidth() * d0;
    }

    public double getRandomZ(double d0) {
        return this.getZ((2.0D * this.random.nextDouble() - 1.0D) * d0);
    }

    public final void setPosRaw(double d0, double d1, double d2) {
        if (this.position.x != d0 || this.position.y != d1 || this.position.z != d2) {
            this.position = new Vec3D(d0, d1, d2);
            int i = MathHelper.floor(d0);
            int j = MathHelper.floor(d1);
            int k = MathHelper.floor(d2);

            if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPosition(i, j, k);
                this.feetBlockState = null;
                if (SectionPosition.blockToSectionCoord(i) != this.chunkPosition.x || SectionPosition.blockToSectionCoord(k) != this.chunkPosition.z) {
                    this.chunkPosition = new ChunkCoordIntPair(this.blockPosition);
                }
            }

            this.levelCallback.onMove();
            GameEventListenerRegistrar gameeventlistenerregistrar = this.getGameEventListenerRegistrar();

            if (gameeventlistenerregistrar != null) {
                gameeventlistenerregistrar.onListenerMove(this.level);
            }
        }

    }

    public void checkDespawn() {}

    public Vec3D getRopeHoldPosition(float f) {
        return this.getPosition(f).add(0.0D, (double) this.eyeHeight * 0.7D, 0.0D);
    }

    public void recreateFromPacket(PacketPlayOutSpawnEntity packetplayoutspawnentity) {
        int i = packetplayoutspawnentity.getId();
        double d0 = packetplayoutspawnentity.getX();
        double d1 = packetplayoutspawnentity.getY();
        double d2 = packetplayoutspawnentity.getZ();

        this.setPacketCoordinates(d0, d1, d2);
        this.moveTo(d0, d1, d2);
        this.setXRot((float) (packetplayoutspawnentity.getxRot() * 360) / 256.0F);
        this.setYRot((float) (packetplayoutspawnentity.getyRot() * 360) / 256.0F);
        this.setId(i);
        this.setUUID(packetplayoutspawnentity.getUUID());
    }

    @Nullable
    public ItemStack getPickResult() {
        return null;
    }

    public void setIsInPowderSnow(boolean flag) {
        this.isInPowderSnow = flag;
    }

    public boolean canFreeze() {
        return !TagsEntity.FREEZE_IMMUNE_ENTITY_TYPES.contains(this.getType());
    }

    public float getYRot() {
        return this.yRot;
    }

    public void setYRot(float f) {
        if (!Float.isFinite(f)) {
            SystemUtils.logAndPauseIfInIde("Invalid entity rotation: " + f + ", discarding.");
        } else {
            this.yRot = f;
        }
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float f) {
        if (!Float.isFinite(f)) {
            SystemUtils.logAndPauseIfInIde("Invalid entity rotation: " + f + ", discarding.");
        } else {
            this.xRot = f;
        }
    }

    public final boolean isRemoved() {
        return this.removalReason != null;
    }

    @Nullable
    public Entity.RemovalReason getRemovalReason() {
        return this.removalReason;
    }

    @Override
    public void setRemoved(Entity.RemovalReason entity_removalreason) {
        if (this.removalReason == null) {
            this.removalReason = entity_removalreason;
        }

        if (this.removalReason.shouldDestroy()) {
            this.stopRiding();
        }

        this.getPassengers().forEach(Entity::stopRiding);
        this.levelCallback.onRemove(entity_removalreason);
    }

    public void unsetRemoved() {
        this.removalReason = null;
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback entityinlevelcallback) {
        this.levelCallback = entityinlevelcallback;
    }

    @Override
    public boolean shouldBeSaved() {
        return this.removalReason != null && !this.removalReason.shouldSave() ? false : (this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger());
    }

    @Override
    public boolean isAlwaysTicking() {
        return false;
    }

    public boolean mayInteract(World world, BlockPosition blockposition) {
        return true;
    }

    public World getLevel() {
        return this.level;
    }

    public static enum RemovalReason {

        KILLED(true, false), DISCARDED(true, false), UNLOADED_TO_CHUNK(false, true), UNLOADED_WITH_PLAYER(false, false), CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(boolean flag, boolean flag1) {
            this.destroy = flag;
            this.save = flag1;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }

    public static enum MovementEmission {

        NONE(false, false), SOUNDS(true, false), EVENTS(false, true), ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(boolean flag, boolean flag1) {
            this.sounds = flag;
            this.events = flag1;
        }

        public boolean emitsAnything() {
            return this.events || this.sounds;
        }

        public boolean emitsEvents() {
            return this.events;
        }

        public boolean emitsSounds() {
            return this.sounds;
        }
    }

    @FunctionalInterface
    public interface MoveFunction {

        void accept(Entity entity, double d0, double d1, double d2);
    }
}
