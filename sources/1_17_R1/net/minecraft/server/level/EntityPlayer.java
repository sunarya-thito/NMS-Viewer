package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.EnumChatFormat;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.PacketPlayInSettings;
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutCamera;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutLookAt;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutOpenBook;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowHorse;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindowMerchant;
import net.minecraft.network.protocol.game.PacketPlayOutRemoveEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutResourcePackSend;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.network.protocol.game.PacketPlayOutUnloadChunk;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateHealth;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import net.minecraft.network.protocol.game.PacketPlayOutWindowItems;
import net.minecraft.network.protocol.game.PacketPlayOutWorldEvent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.stats.ServerStatisticManager;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.EnumHand;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.EnumChatVisibility;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerHorse;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.ICrafting;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SlotResult;
import net.minecraft.world.item.ItemCooldown;
import net.minecraft.world.item.ItemCooldownPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMapBase;
import net.minecraft.world.item.ItemWrittenBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BlockFacingHorizontal;
import net.minecraft.world.level.block.BlockPortal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.portal.ShapeDetectorShape;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.nbt.NBTBase;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.event.CraftPortalEvent;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftDimensionUtil;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.MainHand;
// CraftBukkit end

public class EntityPlayer extends EntityHuman {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    public PlayerConnection connection;
    public final MinecraftServer server;
    public final PlayerInteractManager gameMode;
    private final AdvancementDataPlayer advancements;
    private final ServerStatisticManager stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    public int lastSentExp = -99999999;
    public int spawnInvulnerableTime = 60;
    private EnumChatVisibility chatVisibility;
    private boolean canChatColor;
    private long lastActionTime;
    private Entity camera;
    public boolean isChangingDimension;
    private boolean seenCredits;
    private final RecipeBookServer recipeBook;
    private Vec3D levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    @Nullable
    private Vec3D enteredNetherPosition;
    private SectionPosition lastSectionPos;
    private ResourceKey<World> respawnDimension;
    @Nullable
    private BlockPosition respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final ITextFilter textFilter;
    private boolean textFilteringEnabled;
    private final ContainerSynchronizer containerSynchronizer;
    private final ICrafting containerListener;
    private int containerCounter;
    public int latency;
    public boolean wonGame;

    // CraftBukkit start
    public String displayName;
    public IChatBaseComponent listName;
    public org.bukkit.Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    public boolean joining = true;
    public boolean sentListPacket = false;
    public Integer clientViewDistance;
    // CraftBukkit end

    public EntityPlayer(MinecraftServer minecraftserver, WorldServer worldserver, GameProfile gameprofile) {
        super(worldserver, worldserver.getSpawn(), worldserver.x(), gameprofile);
        this.chatVisibility = EnumChatVisibility.FULL;
        this.canChatColor = true;
        this.lastActionTime = SystemUtils.getMonotonicMillis();
        this.recipeBook = new RecipeBookServer();
        this.lastSectionPos = SectionPosition.a(0, 0, 0);
        this.respawnDimension = World.OVERWORLD;
        this.containerSynchronizer = new ContainerSynchronizer() {
            @Override
            public void sendInitialData(Container container, NonNullList<ItemStack> nonnulllist, ItemStack itemstack, int[] aint) {
                EntityPlayer.this.connection.sendPacket(new PacketPlayOutWindowItems(container.containerId, container.incrementStateId(), nonnulllist, itemstack));

                for (int i = 0; i < aint.length; ++i) {
                    this.b(container, i, aint[i]);
                }

            }

            @Override
            public void sendSlotChange(Container container, int i, ItemStack itemstack) {
                EntityPlayer.this.connection.sendPacket(new PacketPlayOutSetSlot(container.containerId, container.incrementStateId(), i, itemstack));
            }

            @Override
            public void sendCarriedChange(Container container, ItemStack itemstack) {
                EntityPlayer.this.connection.sendPacket(new PacketPlayOutSetSlot(-1, container.incrementStateId(), -1, itemstack));
            }

            @Override
            public void sendDataChange(Container container, int i, int j) {
                this.b(container, i, j);
            }

            private void b(Container container, int i, int j) {
                EntityPlayer.this.connection.sendPacket(new PacketPlayOutWindowData(container.containerId, i, j));
            }
        };
        this.containerListener = new ICrafting() {
            @Override
            public void a(Container container, int i, ItemStack itemstack) {
                Slot slot = container.getSlot(i);

                if (!(slot instanceof SlotResult)) {
                    if (slot.container == EntityPlayer.this.getInventory()) {
                        CriterionTriggers.INVENTORY_CHANGED.a(EntityPlayer.this, EntityPlayer.this.getInventory(), itemstack);
                    }

                }
            }

            @Override
            public void setContainerData(Container container, int i, int j) {}
        };
        this.textFilter = minecraftserver.a(this);
        this.gameMode = minecraftserver.b(this);
        this.server = minecraftserver;
        this.stats = minecraftserver.getPlayerList().getStatisticManager(this);
        this.advancements = minecraftserver.getPlayerList().f(this);
        this.maxUpStep = 1.0F;
        this.d(worldserver);

        // CraftBukkit start
        this.displayName = this.getName();
        this.bukkitPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
    }

    // Yes, this doesn't match Vanilla, but it's the best we can do for now.
    // If this is an issue, PRs are welcome
    public final BlockPosition getSpawnPoint(WorldServer worldserver) {
        BlockPosition blockposition = worldserver.getSpawn();

        if (worldserver.getDimensionManager().hasSkyLight() && worldserver.serverLevelData.getGameType() != EnumGamemode.ADVENTURE) {
            int i = Math.max(0, this.server.a(worldserver));
            int j = MathHelper.floor(worldserver.getWorldBorder().b((double) blockposition.getX(), (double) blockposition.getZ()));

            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long) (i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int) l;
            int j1 = this.v(i1);
            int k1 = (new Random()).nextInt(i1);

            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPosition blockposition1 = WorldProviderNormal.a(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i, false);

                if (blockposition1 != null) {
                    return blockposition1;
                }
            }
        }

        return blockposition;
    }
    // CraftBukkit end

    private void d(WorldServer worldserver) {
        BlockPosition blockposition = worldserver.getSpawn();

        if (worldserver.getDimensionManager().hasSkyLight() && worldserver.serverLevelData.getGameType() != EnumGamemode.ADVENTURE) { // CraftBukkit
            int i = Math.max(0, this.server.a(worldserver));
            int j = MathHelper.floor(worldserver.getWorldBorder().b((double) blockposition.getX(), (double) blockposition.getZ()));

            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long) (i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int) l;
            int j1 = this.v(i1);
            int k1 = (new Random()).nextInt(i1);

            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPosition blockposition1 = WorldProviderNormal.a(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i, false);

                if (blockposition1 != null) {
                    this.setPositionRotation(blockposition1, 0.0F, 0.0F);
                    if (worldserver.getCubes(this)) {
                        break;
                    }
                }
            }
        } else {
            this.setPositionRotation(blockposition, 0.0F, 0.0F);

            while (!worldserver.getCubes(this) && this.locY() < (double) (worldserver.getMaxBuildHeight() - 1)) {
                this.setPosition(this.locX(), this.locY() + 1.0D, this.locZ());
            }
        }

    }

    private int v(int i) {
        return i <= 16 ? i - 1 : 17;
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        if (nbttagcompound.hasKeyOfType("enteredNetherPosition", 10)) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("enteredNetherPosition");

            this.enteredNetherPosition = new Vec3D(nbttagcompound1.getDouble("x"), nbttagcompound1.getDouble("y"), nbttagcompound1.getDouble("z"));
        }

        this.seenCredits = nbttagcompound.getBoolean("seenCredits");
        if (nbttagcompound.hasKeyOfType("recipeBook", 10)) {
            this.recipeBook.a(nbttagcompound.getCompound("recipeBook"), this.server.getCraftingManager());
        }
        this.getBukkitEntity().readExtraData(nbttagcompound); // CraftBukkit

        if (this.isSleeping()) {
            this.entityWakeup();
        }

        // CraftBukkit start
        String spawnWorld = nbttagcompound.getString("SpawnWorld");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            this.respawnDimension = oldWorld.getHandle().getDimensionKey();
        }
        // CraftBukkit end

        if (nbttagcompound.hasKeyOfType("SpawnX", 99) && nbttagcompound.hasKeyOfType("SpawnY", 99) && nbttagcompound.hasKeyOfType("SpawnZ", 99)) {
            this.respawnPosition = new BlockPosition(nbttagcompound.getInt("SpawnX"), nbttagcompound.getInt("SpawnY"), nbttagcompound.getInt("SpawnZ"));
            this.respawnForced = nbttagcompound.getBoolean("SpawnForced");
            this.respawnAngle = nbttagcompound.getFloat("SpawnAngle");
            if (nbttagcompound.hasKey("SpawnDimension")) {
                DataResult dataresult = World.RESOURCE_KEY_CODEC.parse(DynamicOpsNBT.INSTANCE, nbttagcompound.get("SpawnDimension"));
                Logger logger = EntityPlayer.LOGGER;

                Objects.requireNonNull(logger);
                this.respawnDimension = (ResourceKey) dataresult.resultOrPartial(logger::error).orElse(World.OVERWORLD);
            }
        }

    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        this.k(nbttagcompound);
        nbttagcompound.setBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();

            nbttagcompound1.setDouble("x", this.enteredNetherPosition.x);
            nbttagcompound1.setDouble("y", this.enteredNetherPosition.y);
            nbttagcompound1.setDouble("z", this.enteredNetherPosition.z);
            nbttagcompound.set("enteredNetherPosition", nbttagcompound1);
        }

        Entity entity = this.getRootVehicle();
        Entity entity1 = this.getVehicle();

        // CraftBukkit start - handle non-persistent vehicles
        boolean persistVehicle = true;
        if (entity1 != null) {
            Entity vehicle;
            for (vehicle = entity1; vehicle != null; vehicle = vehicle.getVehicle()) {
                if (!vehicle.persist) {
                    persistVehicle = false;
                    break;
                }
            }
        }

        if (persistVehicle && entity1 != null && entity != this && entity.hasSinglePlayerPassenger()) {
            // CraftBukkit end
            NBTTagCompound nbttagcompound2 = new NBTTagCompound();
            NBTTagCompound nbttagcompound3 = new NBTTagCompound();

            entity.e(nbttagcompound3);
            nbttagcompound2.a("Attach", entity1.getUniqueID());
            nbttagcompound2.set("Entity", nbttagcompound3);
            nbttagcompound.set("RootVehicle", nbttagcompound2);
        }

        nbttagcompound.set("recipeBook", this.recipeBook.save());
        nbttagcompound.setString("Dimension", this.level.getDimensionKey().a().toString());
        if (this.respawnPosition != null) {
            nbttagcompound.setInt("SpawnX", this.respawnPosition.getX());
            nbttagcompound.setInt("SpawnY", this.respawnPosition.getY());
            nbttagcompound.setInt("SpawnZ", this.respawnPosition.getZ());
            nbttagcompound.setBoolean("SpawnForced", this.respawnForced);
            nbttagcompound.setFloat("SpawnAngle", this.respawnAngle);
            DataResult<NBTBase> dataresult = MinecraftKey.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.respawnDimension.a()); // CraftBukkit - decompile error
            Logger logger = EntityPlayer.LOGGER;

            Objects.requireNonNull(logger);
            dataresult.resultOrPartial(logger::error).ifPresent((nbtbase) -> {
                nbttagcompound.set("SpawnDimension", nbtbase);
            });
        }
        this.getBukkitEntity().setExtraData(nbttagcompound); // CraftBukkit

    }

    // CraftBukkit start - World fallback code, either respawn location or global spawn
    public void spawnIn(World world) {
        this.level = world;
        if (world == null) {
            this.unsetRemoved();
            Vec3D position = null;
            if (this.respawnDimension != null) {
                world = this.getWorldServer().getCraftServer().getHandle().getServer().getWorldServer(this.respawnDimension);
                if (world != null && this.getSpawn() != null) {
                    position = EntityHuman.getBed((WorldServer) world, this.getSpawn(), this.getSpawnAngle(), false, false).orElse(null);
                }
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3D.a(((WorldServer) world).getSpawn());
            }
            this.level = world;
            this.setPosition(position.getX(), position.getY(), position.getZ());
        }
        this.gameMode.a((WorldServer) world);
    }
    // CraftBukkit end

    public void a(int i) {
        float f = (float) this.getExpToLevel();
        float f1 = (f - 1.0F) / f;

        this.experienceProgress = MathHelper.a((float) i / f, 0.0F, f1);
        this.lastSentExp = -1;
    }

    public void b(int i) {
        this.experienceLevel = i;
        this.lastSentExp = -1;
    }

    @Override
    public void levelDown(int i) {
        super.levelDown(i);
        this.lastSentExp = -1;
    }

    @Override
    public void enchantDone(ItemStack itemstack, int i) {
        super.enchantDone(itemstack, i);
        this.lastSentExp = -1;
    }

    public void initMenu(Container container) {
        container.addSlotListener(this.containerListener);
        container.a(this.containerSynchronizer);
    }

    public void syncInventory() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void enterCombat() {
        super.enterCombat();
        this.connection.sendPacket(new ClientboundPlayerCombatEnterPacket());
    }

    @Override
    public void exitCombat() {
        super.exitCombat();
        this.connection.sendPacket(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    protected void a(IBlockData iblockdata) {
        CriterionTriggers.ENTER_BLOCK.a(this, iblockdata);
    }

    @Override
    protected ItemCooldown j() {
        return new ItemCooldownPlayer(this);
    }

    @Override
    public void tick() {
        // CraftBukkit start
        if (this.joining) {
            this.joining = false;
        }
        // CraftBukkit end
        this.gameMode.a();
        --this.spawnInvulnerableTime;
        if (this.invulnerableTime > 0) {
            --this.invulnerableTime;
        }

        this.containerMenu.d();
        if (!this.level.isClientSide && !this.containerMenu.canUse(this)) {
            this.closeInventory();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getSpecatorTarget();

        if (entity != this) {
            if (entity.isAlive()) {
                this.setLocation(entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
                this.getWorldServer().getChunkProvider().movePlayer(this);
                if (this.fa()) {
                    this.setSpectatorTarget(this);
                }
            } else {
                this.setSpectatorTarget(this);
            }
        }

        CriterionTriggers.TICK.a(this);
        if (this.levitationStartPos != null) {
            CriterionTriggers.LEVITATION.a(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.advancements.b(this);
    }

    public void playerTick() {
        try {
            if (!this.isSpectator() || !this.cM()) {
                super.tick();
            }

            for (int i = 0; i < this.getInventory().getSize(); ++i) {
                ItemStack itemstack = this.getInventory().getItem(i);

                if (itemstack.getItem().M_()) {
                    Packet<?> packet = ((ItemWorldMapBase) itemstack.getItem()).a(itemstack, this.level, (EntityHuman) this);

                    if (packet != null) {
                        this.connection.sendPacket(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.sendPacket(new PacketPlayOutUpdateHealth(this.getBukkitEntity().getScaledHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel())); // CraftBukkit
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionHearts() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionHearts();
                this.a(IScoreboardCriteria.HEALTH, MathHelper.f(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.a(IScoreboardCriteria.FOOD, MathHelper.f((float) this.lastRecordedFoodLevel));
            }

            if (this.getAirTicks() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirTicks();
                this.a(IScoreboardCriteria.AIR, MathHelper.f((float) this.lastRecordedAirLevel));
            }

            if (this.getArmorStrength() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorStrength();
                this.a(IScoreboardCriteria.ARMOR, MathHelper.f((float) this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.a(IScoreboardCriteria.EXPERIENCE, MathHelper.f((float) this.lastRecordedExperience));
            }

            // CraftBukkit start - Force max health updates
            if (this.maxHealthCache != this.getMaxHealth()) {
                this.getBukkitEntity().updateScaledHealth();
            }
            // CraftBukkit end

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.a(IScoreboardCriteria.LEVEL, MathHelper.f((float) this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.sendPacket(new PacketPlayOutExperience(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.tickCount % 20 == 0) {
                CriterionTriggers.LOCATION.a(this);
            }

            // CraftBukkit start - initialize oldLevel and fire PlayerLevelChangeEvent
            if (this.oldLevel == -1) {
                this.oldLevel = this.experienceLevel;
            }

            if (this.oldLevel != this.experienceLevel) {
                CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
                this.oldLevel = this.experienceLevel;
            }
            // CraftBukkit end
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Ticking player");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Player being ticked");

            this.appendEntityCrashDetails(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    private void a(IScoreboardCriteria iscoreboardcriteria, int i) {
        // CraftBukkit - Use our scores instead
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(iscoreboardcriteria, this.getName(), (scoreboardscore) -> {
            scoreboardscore.setScore(i);
        });
    }

    @Override
    public void die(DamageSource damagesource) {
        boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        // CraftBukkit start - fire PlayerDeathEvent
        if (this.isRemoved()) {
            return;
        }
        java.util.List<org.bukkit.inventory.ItemStack> loot = new java.util.ArrayList<org.bukkit.inventory.ItemStack>(this.getInventory().getSize());
        boolean keepInventory = this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();

        if (!keepInventory) {
            for (ItemStack item : this.getInventory().getContents()) {
                if (!item.isEmpty() && !EnchantmentManager.shouldNotDrop(item)) {
                    loot.add(CraftItemStack.asCraftMirror(item));
                }
            }
        }
        // SPIGOT-5071: manually add player loot tables (SPIGOT-5195 - ignores keepInventory rule)
        this.a(damagesource, this.lastHurtByPlayerTime > 0);
        for (org.bukkit.inventory.ItemStack item : this.drops) {
            loot.add(item);
        }
        this.drops.clear(); // SPIGOT-5188: make sure to clear

        IChatBaseComponent defaultMessage = this.getCombatTracker().getDeathMessage();

        String deathmessage = defaultMessage.getString();
        keepLevel = keepInventory; // SPIGOT-2222: pre-set keepLevel
        org.bukkit.event.entity.PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent(this, loot, deathmessage, keepInventory);

        // SPIGOT-943 - only call if they have an inventory open
        if (this.containerMenu != this.inventoryMenu) {
            this.closeInventory();
        }

        String deathMessage = event.getDeathMessage();

        if (deathMessage != null && deathMessage.length() > 0 && flag) { // TODO: allow plugins to override?
            IChatBaseComponent ichatbasecomponent;
            if (deathMessage.equals(deathmessage)) {
                ichatbasecomponent = this.getCombatTracker().getDeathMessage();
            } else {
                ichatbasecomponent = org.bukkit.craftbukkit.util.CraftChatMessage.fromStringOrNull(deathMessage);
            }

            this.connection.a((Packet) (new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), ichatbasecomponent)), (future) -> {
                if (!future.isSuccess()) {
                    boolean flag1 = true;
                    String s = ichatbasecomponent.a(256);
                    ChatMessage chatmessage = new ChatMessage("death.attack.message_too_long", new Object[]{(new ChatComponentText(s)).a(EnumChatFormat.YELLOW)});
                    IChatMutableComponent ichatmutablecomponent = (new ChatMessage("death.attack.even_more_magic", new Object[]{this.getScoreboardDisplayName()})).format((chatmodifier) -> {
                        return chatmodifier.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, chatmessage));
                    });

                    this.connection.sendPacket(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), ichatmutablecomponent));
                }

            });
            ScoreboardTeamBase scoreboardteambase = this.getScoreboardTeam();

            if (scoreboardteambase != null && scoreboardteambase.getDeathMessageVisibility() != ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS) {
                if (scoreboardteambase.getDeathMessageVisibility() == ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().a((EntityHuman) this, ichatbasecomponent);
                } else if (scoreboardteambase.getDeathMessageVisibility() == ScoreboardTeamBase.EnumNameTagVisibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().b(this, ichatbasecomponent);
                }
            } else {
                this.server.getPlayerList().sendMessage(ichatbasecomponent, ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
            }
        } else {
            this.connection.sendPacket(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), ChatComponentText.EMPTY));
        }

        this.releaseShoulderEntities();
        if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.fI();
        }
        // SPIGOT-5478 must be called manually now
        this.dropExperience();
        // we clean the player's inventory after the EntityDeathEvent is called so plugins can get the exact state of the inventory.
        if (!event.getKeepInventory()) {
            this.getInventory().clear();
        }

        this.setSpectatorTarget(this); // Remove spectated target
        // CraftBukkit end

        // CraftBukkit - Get our scores instead
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(IScoreboardCriteria.DEATH_COUNT, this.getName(), ScoreboardScore::incrementScore);
        EntityLiving entityliving = this.getKillingEntity();

        if (entityliving != null) {
            this.b(StatisticList.ENTITY_KILLED_BY.b(entityliving.getEntityType()));
            entityliving.a(this, this.deathScore, damagesource);
            this.f(entityliving);
        }

        this.level.broadcastEntityEffect(this, (byte) 3);
        this.a(StatisticList.DEATHS);
        this.a(StatisticList.CUSTOM.b(StatisticList.TIME_SINCE_DEATH));
        this.a(StatisticList.CUSTOM.b(StatisticList.TIME_SINCE_REST));
        this.extinguish();
        this.setTicksFrozen(0);
        this.a_(false);
        this.getCombatTracker().g();
    }

    private void fI() {
        AxisAlignedBB axisalignedbb = (new AxisAlignedBB(this.getChunkCoordinates())).grow(32.0D, 10.0D, 32.0D);

        this.level.a(EntityInsentient.class, axisalignedbb, IEntitySelector.NO_SPECTATORS).stream().filter((entityinsentient) -> {
            return entityinsentient instanceof IEntityAngerable;
        }).forEach((entityinsentient) -> {
            ((IEntityAngerable) entityinsentient).a_((EntityHuman) this);
        });
    }

    @Override
    public void a(Entity entity, int i, DamageSource damagesource) {
        if (entity != this) {
            super.a(entity, i, damagesource);
            this.addScore(i);
            String s = this.getName();
            String s1 = entity.getName();

            // CraftBukkit - Get our scores instead
            this.level.getCraftServer().getScoreboardManager().getScoreboardScores(IScoreboardCriteria.KILL_COUNT_ALL, s, ScoreboardScore::incrementScore);
            if (entity instanceof EntityHuman) {
                this.a(StatisticList.PLAYER_KILLS);
                // CraftBukkit - Get our scores instead
                this.level.getCraftServer().getScoreboardManager().getScoreboardScores(IScoreboardCriteria.KILL_COUNT_PLAYERS, s, ScoreboardScore::incrementScore);
            } else {
                this.a(StatisticList.MOB_KILLS);
            }

            this.a(s, s1, IScoreboardCriteria.TEAM_KILL);
            this.a(s1, s, IScoreboardCriteria.KILLED_BY_TEAM);
            CriterionTriggers.PLAYER_KILLED_ENTITY.a(this, entity, damagesource);
        }
    }

    private void a(String s, String s1, IScoreboardCriteria[] aiscoreboardcriteria) {
        ScoreboardTeam scoreboardteam = this.getScoreboard().getPlayerTeam(s1);

        if (scoreboardteam != null) {
            int i = scoreboardteam.getColor().b();

            if (i >= 0 && i < aiscoreboardcriteria.length) {
                // CraftBukkit - Get our scores instead
                this.level.getCraftServer().getScoreboardManager().getScoreboardScores(aiscoreboardcriteria[i], s, ScoreboardScore::incrementScore);
            }
        }

    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else {
            boolean flag = this.server.k() && this.canPvP() && "fall".equals(damagesource.msgId);

            if (!flag && this.spawnInvulnerableTime > 0 && damagesource != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                if (damagesource instanceof EntityDamageSource) {
                    Entity entity = damagesource.getEntity();

                    if (entity instanceof EntityHuman && !this.a((EntityHuman) entity)) {
                        return false;
                    }

                    if (entity instanceof EntityArrow) {
                        EntityArrow entityarrow = (EntityArrow) entity;
                        Entity entity1 = entityarrow.getShooter();

                        if (entity1 instanceof EntityHuman && !this.a((EntityHuman) entity1)) {
                            return false;
                        }
                    }
                }

                return super.damageEntity(damagesource, f);
            }
        }
    }

    @Override
    public boolean a(EntityHuman entityhuman) {
        return !this.canPvP() ? false : super.a(entityhuman);
    }

    private boolean canPvP() {
        // CraftBukkit - this.server.getPvP() -> this.world.pvpMode
        return this.level.pvpMode;
    }

    @Nullable
    @Override
    protected ShapeDetectorShape a(WorldServer worldserver) {
        ShapeDetectorShape shapedetectorshape = super.a(worldserver);
        worldserver = (shapedetectorshape == null) ? worldserver : shapedetectorshape.world; // CraftBukkit

        if (shapedetectorshape != null && this.level.getTypeKey() == DimensionManager.OVERWORLD_LOCATION && worldserver != null && worldserver.getTypeKey() == DimensionManager.END_LOCATION) { // CraftBukkit
            Vec3D vec3d = shapedetectorshape.pos.add(0.0D, -1.0D, 0.0D);

            return new ShapeDetectorShape(vec3d, Vec3D.ZERO, 90.0F, 0.0F, worldserver, shapedetectorshape.portalEventInfo); // CraftBukkit
        } else {
            return shapedetectorshape;
        }
    }

    @Nullable
    @Override
    public Entity b(WorldServer worldserver) {
        // CraftBukkit start
        return b(worldserver, TeleportCause.UNKNOWN);
    }

    @Nullable
    public Entity b(WorldServer worldserver, PlayerTeleportEvent.TeleportCause cause) {
        // CraftBukkit end
        if (this.isSleeping()) return this; // CraftBukkit - SPIGOT-3154
        // this.worldChangeInvuln = true; // CraftBukkit - Moved down and into PlayerList#changeDimension
        WorldServer worldserver1 = this.getWorldServer();
        ResourceKey<DimensionManager> resourcekey = worldserver1.getTypeKey(); // CraftBukkit

        if (resourcekey == DimensionManager.END_LOCATION && worldserver != null && worldserver.getTypeKey() == DimensionManager.OVERWORLD_LOCATION) { // CraftBukkit
            this.isChangingDimension = true; // CraftBukkit - Moved down from above
            this.decouple();
            this.getWorldServer().a(this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return this;
        } else {
            // CraftBukkit start
            /*
            WorldData worlddata = worldserver.getWorldData();

            this.connection.sendPacket(new PacketPlayOutRespawn(worldserver.getDimensionManager(), worldserver.getDimensionKey(), BiomeManager.a(worldserver.getSeed()), this.gameMode.getGameMode(), this.gameMode.c(), worldserver.isDebugWorld(), worldserver.isFlatWorld(), true));
            this.connection.sendPacket(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
            PlayerList playerlist = this.server.getPlayerList();

            playerlist.d(this);
            worldserver1.a(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            */
            // CraftBukkit end
            ShapeDetectorShape shapedetectorshape = this.a(worldserver);

            if (shapedetectorshape != null) {
                worldserver1.getMethodProfiler().enter("moving");
                worldserver = shapedetectorshape.world; // CraftBukkit
                if (worldserver == null) { } else // CraftBukkit - empty to fall through to null to event
                if (resourcekey == DimensionManager.OVERWORLD_LOCATION && worldserver.getTypeKey() == DimensionManager.NETHER_LOCATION) { // CraftBukkit
                    this.enteredNetherPosition = this.getPositionVector();
                } else if (worldserver.getTypeKey() == DimensionManager.END_LOCATION && shapedetectorshape.portalEventInfo != null && shapedetectorshape.portalEventInfo.getCanCreatePortal()) { // CraftBukkit
                    this.a(worldserver, new BlockPosition(shapedetectorshape.pos));
                }
                // CraftBukkit start
            } else {
                return null;
            }
            Location enter = this.getBukkitEntity().getLocation();
            Location exit = (worldserver == null) ? null : new Location(worldserver.getWorld(), shapedetectorshape.pos.x, shapedetectorshape.pos.y, shapedetectorshape.pos.z, shapedetectorshape.yRot, shapedetectorshape.xRot);
            PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, cause);
            Bukkit.getServer().getPluginManager().callEvent(tpEvent);
            if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                return null;
            }
            exit = tpEvent.getTo();
            worldserver = ((CraftWorld) exit.getWorld()).getHandle();
            // CraftBukkit end

            worldserver1.getMethodProfiler().exit();
            worldserver1.getMethodProfiler().enter("placing");
            if (true) { // CraftBukkit
                this.isChangingDimension = true; // CraftBukkit - Set teleport invulnerability only if player changing worlds

                this.connection.sendPacket(new PacketPlayOutRespawn(worldserver.getDimensionManager(), worldserver.getDimensionKey(), BiomeManager.a(worldserver.getSeed()), this.gameMode.getGameMode(), this.gameMode.c(), worldserver.isDebugWorld(), worldserver.isFlatWorld(), true));
                this.connection.sendPacket(new PacketPlayOutServerDifficulty(this.level.getDifficulty(), this.level.getWorldData().isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();

                playerlist.d(this);
                worldserver1.a(this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.unsetRemoved();

                // CraftBukkit end
                this.spawnIn(worldserver);
                worldserver.addPlayerPortal(this);
                this.connection.teleport(exit); // CraftBukkit - use internal teleport without event
                this.connection.syncPosition(); // CraftBukkit - sync position after changing it (from PortalTravelAgent#findAndteleport)
                worldserver1.getMethodProfiler().exit();
                this.triggerDimensionAdvancements(worldserver1);
                this.connection.sendPacket(new PacketPlayOutAbilities(this.getAbilities()));
                playerlist.a(this, worldserver);
                playerlist.updateClient(this);
                Iterator iterator = this.getEffects().iterator();

                while (iterator.hasNext()) {
                    MobEffect mobeffect = (MobEffect) iterator.next();

                    this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), mobeffect));
                }

                this.connection.sendPacket(new PacketPlayOutWorldEvent(1032, BlockPosition.ZERO, 0, false));
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;

                // CraftBukkit start
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), worldserver1.getWorld());
                this.level.getCraftServer().getPluginManager().callEvent(changeEvent);
                // CraftBukkit end
            }

            return this;
        }
    }

    // CraftBukkit start
    @Override
    protected CraftPortalEvent callPortalEvent(Entity entity, WorldServer exitWorldServer, BlockPosition exitPosition, TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = new Location(exitWorldServer.getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ(), getYRot(), getXRot());
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, searchRadius, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }
    // CraftBukkit end

    private void a(WorldServer worldserver, BlockPosition blockposition) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.i();

        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -1; k < 3; ++k) {
                    IBlockData iblockdata = k == -1 ? Blocks.OBSIDIAN.getBlockData() : Blocks.AIR.getBlockData();

                    worldserver.setTypeUpdate(blockposition_mutableblockposition.g(blockposition).e(j, k, i), iblockdata);
                }
            }
        }

    }

    @Override
    protected Optional<BlockUtil.Rectangle> findOrCreatePortal(WorldServer worldserver, BlockPosition blockposition, boolean flag, int searchRadius, boolean canCreatePortal, int createRadius) { // CraftBukkit
        Optional<BlockUtil.Rectangle> optional = super.findOrCreatePortal(worldserver, blockposition, flag, searchRadius, canCreatePortal, createRadius); // CraftBukkit

        if (optional.isPresent() || !canCreatePortal) { // CraftBukkit
            return optional;
        } else {
            EnumDirection.EnumAxis enumdirection_enumaxis = (EnumDirection.EnumAxis) this.level.getType(this.portalEntrancePos).d(BlockPortal.AXIS).orElse(EnumDirection.EnumAxis.X);
            Optional<BlockUtil.Rectangle> optional1 = worldserver.getTravelAgent().createPortal(blockposition, enumdirection_enumaxis, this, createRadius); // CraftBukkit

            if (!optional1.isPresent()) {
                // EntityPlayer.LOGGER.error("Unable to create a portal, likely target out of worldborder"); // CraftBukkit
            }

            return optional1;
        }
    }

    public void triggerDimensionAdvancements(WorldServer worldserver) {
        ResourceKey<World> resourcekey = worldserver.getDimensionKey();
        ResourceKey<World> resourcekey1 = this.level.getDimensionKey();
        // CraftBukkit start
        ResourceKey<World> maindimensionkey = CraftDimensionUtil.getMainDimensionKey(worldserver);
        ResourceKey<World> maindimensionkey1 = CraftDimensionUtil.getMainDimensionKey(this.level);

        CriterionTriggers.CHANGED_DIMENSION.a(this, maindimensionkey, maindimensionkey1);
        if (maindimensionkey != resourcekey || maindimensionkey1 != resourcekey1) {
            CriterionTriggers.CHANGED_DIMENSION.a(this, resourcekey, resourcekey1);
        }

        if (maindimensionkey == World.NETHER && maindimensionkey1 == World.OVERWORLD && this.enteredNetherPosition != null) {
            // CraftBukkit end
            CriterionTriggers.NETHER_TRAVEL.a(this, this.enteredNetherPosition);
        }

        if (maindimensionkey1 != World.NETHER) { // CraftBukkit
            this.enteredNetherPosition = null;
        }

    }

    @Override
    public boolean a(EntityPlayer entityplayer) {
        return entityplayer.isSpectator() ? this.getSpecatorTarget() == this : (this.isSpectator() ? false : super.a(entityplayer));
    }

    private void a(TileEntity tileentity) {
        if (tileentity != null) {
            PacketPlayOutTileEntityData packetplayouttileentitydata = tileentity.getUpdatePacket();

            if (packetplayouttileentitydata != null) {
                this.connection.sendPacket(packetplayouttileentitydata);
            }
        }

    }

    @Override
    public void receive(Entity entity, int i) {
        super.receive(entity, i);
        this.containerMenu.d();
    }

    // CraftBukkit start - moved bed result checks from below into separate method
    private Either<EntityHuman.EnumBedResult, Unit> getBedResult(BlockPosition blockposition, EnumDirection enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level.getDimensionManager().isNatural() || !this.level.getDimensionManager().isBedWorks()) {
                return Either.left(EntityHuman.EnumBedResult.NOT_POSSIBLE_HERE);
            } else if (!this.a(blockposition, enumdirection)) {
                return Either.left(EntityHuman.EnumBedResult.TOO_FAR_AWAY);
            } else if (this.b(blockposition, enumdirection)) {
                return Either.left(EntityHuman.EnumBedResult.OBSTRUCTED);
            } else {
                this.setRespawnPosition(this.level.getDimensionKey(), blockposition, this.getYRot(), false, true);
                if (this.level.isDay()) {
                    return Either.left(EntityHuman.EnumBedResult.NOT_POSSIBLE_NOW);
                } else {
                    if (!this.isCreative()) {
                        double d0 = 8.0D;
                        double d1 = 5.0D;
                        Vec3D vec3d = Vec3D.c((BaseBlockPosition) blockposition);
                        List<EntityMonster> list = this.level.a(EntityMonster.class, new AxisAlignedBB(vec3d.getX() - 8.0D, vec3d.getY() - 5.0D, vec3d.getZ() - 8.0D, vec3d.getX() + 8.0D, vec3d.getY() + 5.0D, vec3d.getZ() + 8.0D), (entitymonster) -> {
                            return entitymonster.f((EntityHuman) this);
                        });

                        if (!list.isEmpty()) {
                            return Either.left(EntityHuman.EnumBedResult.NOT_SAFE);
                        }
                    }

                    return Either.right(Unit.INSTANCE);
                }
            }
        } else {
            return Either.left(EntityHuman.EnumBedResult.OTHER_PROBLEM);
        }
    }

    @Override
    public Either<EntityHuman.EnumBedResult, Unit> sleep(BlockPosition blockposition, boolean force) {
        EnumDirection enumdirection = (EnumDirection) this.level.getType(blockposition).get(BlockFacingHorizontal.FACING);
        Either<EntityHuman.EnumBedResult, Unit> bedResult = this.getBedResult(blockposition, enumdirection);

        if (bedResult.left().orElse(null) == EntityHuman.EnumBedResult.OTHER_PROBLEM) {
            return bedResult; // return immediately if the result is not bypassable by plugins
        }

        if (force) {
            bedResult = Either.right(Unit.INSTANCE);
        }

        bedResult = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerBedEnterEvent(this, blockposition, bedResult);
        if (bedResult.left().isPresent()) {
            return bedResult;
        }

        {
            {
                {
                    Either<EntityHuman.EnumBedResult, Unit> either = super.sleep(blockposition, force).ifRight((unit) -> {
                        this.a(StatisticList.SLEEP_IN_BED);
                        CriterionTriggers.SLEPT_IN_BED.a(this);
                    });

                    if (!this.getWorldServer().d()) {
                        this.a((IChatBaseComponent) (new ChatMessage("sleep.not_possible")), true);
                    }

                    ((WorldServer) this.level).everyoneSleeping();
                    return either;
                }
            }
        }
        // CraftBukkit end
    }

    @Override
    public void entitySleep(BlockPosition blockposition) {
        this.a(StatisticList.CUSTOM.b(StatisticList.TIME_SINCE_REST));
        super.entitySleep(blockposition);
    }

    private boolean a(BlockPosition blockposition, EnumDirection enumdirection) {
        return this.g(blockposition) || this.g(blockposition.shift(enumdirection.opposite()));
    }

    private boolean g(BlockPosition blockposition) {
        Vec3D vec3d = Vec3D.c((BaseBlockPosition) blockposition);

        return Math.abs(this.locX() - vec3d.getX()) <= 3.0D && Math.abs(this.locY() - vec3d.getY()) <= 2.0D && Math.abs(this.locZ() - vec3d.getZ()) <= 3.0D;
    }

    private boolean b(BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition blockposition1 = blockposition.up();

        return !this.f(blockposition1) || !this.f(blockposition1.shift(enumdirection.opposite()));
    }

    @Override
    public void wakeup(boolean flag, boolean flag1) {
        if (!this.isSleeping()) return; // CraftBukkit - Can't leave bed if not in one!
        // CraftBukkit start - fire PlayerBedLeaveEvent
        CraftPlayer player = this.getBukkitEntity();
        BlockPosition bedPosition = this.getBedPosition().orElse(null);

        org.bukkit.block.Block bed;
        if (bedPosition != null) {
            bed = this.level.getWorld().getBlockAt(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ());
        } else {
            bed = this.level.getWorld().getBlockAt(player.getLocation());
        }

        PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
        this.level.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        if (this.isSleeping()) {
            this.getWorldServer().getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutAnimation(this, 2));
        }

        super.wakeup(flag, flag1);
        if (this.connection != null) {
            this.connection.b(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean a(Entity entity, boolean flag) {
        Entity entity1 = this.getVehicle();

        if (!super.a(entity, flag)) {
            return false;
        } else {
            Entity entity2 = this.getVehicle();

            if (entity2 != entity1 && this.connection != null) {
                this.connection.b(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
            }

            return true;
        }
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();

        super.stopRiding();
        Entity entity1 = this.getVehicle();

        if (entity1 != entity && this.connection != null) {
            this.connection.a(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
        }

    }

    @Override
    public void a(double d0, double d1, double d2) {
        this.bo();
        if (this.connection != null) {
            this.connection.a(d0, d1, d2, this.getYRot(), this.getXRot());
        }

    }

    @Override
    public boolean isInvulnerable(DamageSource damagesource) {
        return super.isInvulnerable(damagesource) || this.H() || this.getAbilities().invulnerable && damagesource == DamageSource.WITHER;
    }

    @Override
    protected void a(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {}

    @Override
    protected void c(BlockPosition blockposition) {
        if (!this.isSpectator()) {
            super.c(blockposition);
        }

    }

    public void a(double d0, boolean flag) {
        if (!this.cM()) {
            BlockPosition blockposition = this.av();

            super.a(d0, flag, this.level.getType(blockposition), blockposition);
        }
    }

    @Override
    public void openSign(TileEntitySign tileentitysign) {
        tileentitysign.a(this.getUniqueID());
        this.connection.sendPacket(new PacketPlayOutBlockChange(this.level, tileentitysign.getPosition()));
        this.connection.sendPacket(new PacketPlayOutOpenSignEditor(tileentitysign.getPosition()));
    }

    public int nextContainerCounter() { // CraftBukkit - void -> int
        this.containerCounter = this.containerCounter % 100 + 1;
        return containerCounter; // CraftBukkit
    }

    @Override
    public OptionalInt openContainer(@Nullable ITileInventory itileinventory) {
        if (itileinventory == null) {
            return OptionalInt.empty();
        } else {
            // CraftBukkit start - SPIGOT-6552: Handle inventory closing in CraftEventFactory#callInventoryOpenEvent(...)
            /*
            if (this.containerMenu != this.inventoryMenu) {
                this.closeInventory();
            }
            */
            // CraftBukkit end

            this.nextContainerCounter();
            Container container = itileinventory.createMenu(this.containerCounter, this.getInventory(), this);

            // CraftBukkit start - Inventory open hook
            if (container != null) {
                container.setTitle(itileinventory.getScoreboardDisplayName());

                boolean cancelled = false;
                container = CraftEventFactory.callInventoryOpenEvent(this, container, cancelled);
                if (container == null && !cancelled) { // Let pre-cancelled events fall through
                    // SPIGOT-5263 - close chest if cancelled
                    if (itileinventory instanceof IInventory) {
                        ((IInventory) itileinventory).closeContainer(this);
                    } else if (itileinventory instanceof BlockChest.DoubleInventory) {
                        // SPIGOT-5355 - double chests too :(
                        ((BlockChest.DoubleInventory) itileinventory).inventorylargechest.closeContainer(this);
                    }
                    return OptionalInt.empty();
                }
            }
            // CraftBukkit end
            if (container == null) {
                if (this.isSpectator()) {
                    this.a((IChatBaseComponent) (new ChatMessage("container.spectatorCantOpen")).a(EnumChatFormat.RED), true);
                }

                return OptionalInt.empty();
            } else {
                // CraftBukkit start
                this.containerMenu = container;
                this.connection.sendPacket(new PacketPlayOutOpenWindow(container.containerId, container.getType(), container.getTitle()));
                // CraftBukkit end
                this.initMenu(container);
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void openTrade(int i, MerchantRecipeList merchantrecipelist, int j, int k, boolean flag, boolean flag1) {
        this.connection.sendPacket(new PacketPlayOutOpenWindowMerchant(i, merchantrecipelist, j, k, flag, flag1));
    }

    @Override
    public void openHorseInventory(EntityHorseAbstract entityhorseabstract, IInventory iinventory) {
        // CraftBukkit start - Inventory open hook
        this.nextContainerCounter();
        Container container = new ContainerHorse(this.containerCounter, this.getInventory(), iinventory, entityhorseabstract);
        container.setTitle(entityhorseabstract.getScoreboardDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent(this, container);

        if (container == null) {
            iinventory.closeContainer(this);
            return;
        }
        // CraftBukkit end
        if (this.containerMenu != this.inventoryMenu) {
            this.closeInventory();
        }

        // this.nextContainerCounter(); // CraftBukkit - moved up
        this.connection.sendPacket(new PacketPlayOutOpenWindowHorse(this.containerCounter, iinventory.getSize(), entityhorseabstract.getId()));
        this.containerMenu = container; // CraftBukkit
        this.initMenu(this.containerMenu);
    }

    @Override
    public void openBook(ItemStack itemstack, EnumHand enumhand) {
        if (itemstack.a(Items.WRITTEN_BOOK)) {
            if (ItemWrittenBook.a(itemstack, this.getCommandListener(), (EntityHuman) this)) {
                this.containerMenu.d();
            }

            this.connection.sendPacket(new PacketPlayOutOpenBook(enumhand));
        }

    }

    @Override
    public void a(TileEntityCommand tileentitycommand) {
        tileentitycommand.c(true);
        this.a((TileEntity) tileentitycommand);
    }

    @Override
    public void closeInventory() {
        CraftEventFactory.handleInventoryCloseEvent(this); // CraftBukkit
        this.connection.sendPacket(new PacketPlayOutCloseWindow(this.containerMenu.containerId));
        this.o();
    }

    public void o() {
        this.containerMenu.b((EntityHuman) this);
        this.inventoryMenu.a(this.containerMenu);
        this.containerMenu = this.inventoryMenu;
    }

    public void a(float f, float f1, boolean flag, boolean flag1) {
        if (this.isPassenger()) {
            if (f >= -1.0F && f <= 1.0F) {
                this.xxa = f;
            }

            if (f1 >= -1.0F && f1 <= 1.0F) {
                this.zza = f1;
            }

            this.jumping = flag;
            this.setSneaking(flag1);
        }

    }

    @Override
    public void a(Statistic<?> statistic, int i) {
        this.stats.b(this, statistic, i);
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(statistic, this.getName(), (scoreboardscore) -> { // CraftBukkit - Get our scores instead
            scoreboardscore.addScore(i);
        });
    }

    @Override
    public void a(Statistic<?> statistic) {
        this.stats.setStatistic(this, statistic, 0);
        this.level.getCraftServer().getScoreboardManager().getScoreboardScores(statistic, this.getName(), ScoreboardScore::c); // CraftBukkit - Get our scores instead
    }

    @Override
    public int discoverRecipes(Collection<IRecipe<?>> collection) {
        return this.recipeBook.a(collection, this);
    }

    @Override
    public void a(MinecraftKey[] aminecraftkey) {
        List<IRecipe<?>> list = Lists.newArrayList();
        MinecraftKey[] aminecraftkey1 = aminecraftkey;
        int i = aminecraftkey.length;

        for (int j = 0; j < i; ++j) {
            MinecraftKey minecraftkey = aminecraftkey1[j];
            Optional<? extends IRecipe<?>> optional = this.server.getCraftingManager().getRecipe(minecraftkey); // CraftBukkit - decompile error

            Objects.requireNonNull(list);
            optional.ifPresent(list::add);
        }

        this.discoverRecipes(list);
    }

    @Override
    public int undiscoverRecipes(Collection<IRecipe<?>> collection) {
        return this.recipeBook.b(collection, this);
    }

    @Override
    public void giveExp(int i) {
        super.giveExp(i);
        this.lastSentExp = -1;
    }

    public void p() {
        this.disconnected = true;
        this.ejectPassengers();
        if (this.isSleeping()) {
            this.wakeup(true, false);
        }

    }

    public boolean q() {
        return this.disconnected;
    }

    public void triggerHealthUpdate() {
        this.lastSentHealth = -1.0E8F;
        this.lastSentExp = -1; // CraftBukkit - Added to reset
    }

    // CraftBukkit start - Support multi-line messages
    public void sendMessage(UUID uuid, IChatBaseComponent[] ichatbasecomponent) {
        for (IChatBaseComponent component : ichatbasecomponent) {
            this.sendMessage(component, (uuid == null) ? SystemUtils.NIL_UUID : uuid);
        }
    }
    // CraftBukkit end

    @Override
    public void a(IChatBaseComponent ichatbasecomponent, boolean flag) {
        this.a(ichatbasecomponent, flag ? ChatMessageType.GAME_INFO : ChatMessageType.CHAT, SystemUtils.NIL_UUID);
    }

    @Override
    protected void s() {
        if (!this.useItem.isEmpty() && this.isHandRaised()) {
            this.connection.sendPacket(new PacketPlayOutEntityStatus(this, (byte) 9));
            super.s();
        }

    }

    @Override
    public void a(ArgumentAnchor.Anchor argumentanchor_anchor, Vec3D vec3d) {
        super.a(argumentanchor_anchor, vec3d);
        this.connection.sendPacket(new PacketPlayOutLookAt(argumentanchor_anchor, vec3d.x, vec3d.y, vec3d.z));
    }

    public void a(ArgumentAnchor.Anchor argumentanchor_anchor, Entity entity, ArgumentAnchor.Anchor argumentanchor_anchor1) {
        Vec3D vec3d = argumentanchor_anchor1.a(entity);

        super.a(argumentanchor_anchor, vec3d);
        this.connection.sendPacket(new PacketPlayOutLookAt(argumentanchor_anchor, entity, argumentanchor_anchor1));
    }

    public void copyFrom(EntityPlayer entityplayer, boolean flag) {
        this.textFilteringEnabled = entityplayer.textFilteringEnabled;
        this.gameMode.a(entityplayer.gameMode.getGameMode(), entityplayer.gameMode.c());
        if (flag) {
            this.getInventory().a(entityplayer.getInventory());
            this.setHealth(entityplayer.getHealth());
            this.foodData = entityplayer.foodData;
            this.experienceLevel = entityplayer.experienceLevel;
            this.totalExperience = entityplayer.totalExperience;
            this.experienceProgress = entityplayer.experienceProgress;
            this.setScore(entityplayer.getScore());
            this.portalEntrancePos = entityplayer.portalEntrancePos;
        } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || entityplayer.isSpectator()) {
            this.getInventory().a(entityplayer.getInventory());
            this.experienceLevel = entityplayer.experienceLevel;
            this.totalExperience = entityplayer.totalExperience;
            this.experienceProgress = entityplayer.experienceProgress;
            this.setScore(entityplayer.getScore());
        }

        this.enchantmentSeed = entityplayer.enchantmentSeed;
        this.enderChestInventory = entityplayer.enderChestInventory;
        this.getDataWatcher().set(EntityPlayer.DATA_PLAYER_MODE_CUSTOMISATION, (Byte) entityplayer.getDataWatcher().get(EntityPlayer.DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        // this.recipeBook.a((RecipeBook) entityplayer.recipeBook); // CraftBukkit
        this.seenCredits = entityplayer.seenCredits;
        this.enteredNetherPosition = entityplayer.enteredNetherPosition;
        this.setShoulderEntityLeft(entityplayer.getShoulderEntityLeft());
        this.setShoulderEntityRight(entityplayer.getShoulderEntityRight());

    }

    @Override
    protected void a(MobEffect mobeffect, @Nullable Entity entity) {
        super.a(mobeffect, entity);
        this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), mobeffect));
        if (mobeffect.getMobEffect() == MobEffects.LEVITATION) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.getPositionVector();
        }

        CriterionTriggers.EFFECTS_CHANGED.a(this, entity);
    }

    @Override
    protected void a(MobEffect mobeffect, boolean flag, @Nullable Entity entity) {
        super.a(mobeffect, flag, entity);
        this.connection.sendPacket(new PacketPlayOutEntityEffect(this.getId(), mobeffect));
        CriterionTriggers.EFFECTS_CHANGED.a(this, entity);
    }

    @Override
    protected void a(MobEffect mobeffect) {
        super.a(mobeffect);
        this.connection.sendPacket(new PacketPlayOutRemoveEntityEffect(this.getId(), mobeffect.getMobEffect()));
        if (mobeffect.getMobEffect() == MobEffects.LEVITATION) {
            this.levitationStartPos = null;
        }

        CriterionTriggers.EFFECTS_CHANGED.a(this, (Entity) null);
    }

    @Override
    public void enderTeleportTo(double d0, double d1, double d2) {
        this.connection.b(d0, d1, d2, this.getYRot(), this.getXRot());
    }

    @Override
    public void teleportAndSync(double d0, double d1, double d2) {
        this.enderTeleportTo(d0, d1, d2);
        this.connection.syncPosition();
    }

    @Override
    public void a(Entity entity) {
        this.getWorldServer().getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutAnimation(entity, 4));
    }

    @Override
    public void b(Entity entity) {
        this.getWorldServer().getChunkProvider().broadcastIncludingSelf(this, new PacketPlayOutAnimation(entity, 5));
    }

    @Override
    public void updateAbilities() {
        if (this.connection != null) {
            this.connection.sendPacket(new PacketPlayOutAbilities(this.getAbilities()));
            this.C();
        }
    }

    public WorldServer getWorldServer() {
        return (WorldServer) this.level;
    }

    public boolean a(EnumGamemode enumgamemode) {
        if (!this.gameMode.setGameMode(enumgamemode)) {
            return false;
        } else {
            this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.CHANGE_GAME_MODE, (float) enumgamemode.getId()));
            if (enumgamemode == EnumGamemode.SPECTATOR) {
                this.releaseShoulderEntities();
                this.stopRiding();
            } else {
                this.setSpectatorTarget(this);
            }

            this.updateAbilities();
            this.eD();
            return true;
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameMode() == EnumGamemode.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameMode() == EnumGamemode.CREATIVE;
    }

    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {
        this.a(ichatbasecomponent, ChatMessageType.SYSTEM, uuid);
    }

    public void a(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
        if (this.a(chatmessagetype)) {
            this.connection.a((Packet) (new PacketPlayOutChat(ichatbasecomponent, chatmessagetype, uuid)), (future) -> {
                if (!future.isSuccess() && (chatmessagetype == ChatMessageType.GAME_INFO || chatmessagetype == ChatMessageType.SYSTEM) && this.a(ChatMessageType.SYSTEM)) {
                    boolean flag = true;
                    String s = ichatbasecomponent.a(256);
                    IChatMutableComponent ichatmutablecomponent = (new ChatComponentText(s)).a(EnumChatFormat.YELLOW);

                    this.connection.sendPacket(new PacketPlayOutChat((new ChatMessage("multiplayer.message_not_delivered", new Object[]{ichatmutablecomponent})).a(EnumChatFormat.RED), ChatMessageType.SYSTEM, uuid));
                }

            });
        }
    }

    public String v() {
        String s = this.connection.connection.getSocketAddress().toString();

        s = s.substring(s.indexOf("/") + 1);
        s = s.substring(0, s.indexOf(":"));
        return s;
    }

    public String locale = "en_us"; // CraftBukkit - add, lowercase
    public void a(PacketPlayInSettings packetplayinsettings) {
        // CraftBukkit start
        if (getMainHand() != packetplayinsettings.getMainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(getBukkitEntity(), getMainHand() == EnumMainHand.LEFT ? MainHand.LEFT : MainHand.RIGHT);
            this.server.server.getPluginManager().callEvent(event);
        }
        if (!this.locale.equals(packetplayinsettings.language)) {
            PlayerLocaleChangeEvent event = new PlayerLocaleChangeEvent(getBukkitEntity(), packetplayinsettings.language);
            this.server.server.getPluginManager().callEvent(event);
        }
        this.locale = packetplayinsettings.language;
        this.clientViewDistance = packetplayinsettings.viewDistance;
        // CraftBukkit end
        this.chatVisibility = packetplayinsettings.d();
        this.canChatColor = packetplayinsettings.e();
        this.textFilteringEnabled = packetplayinsettings.h();
        this.getDataWatcher().set(EntityPlayer.DATA_PLAYER_MODE_CUSTOMISATION, (byte) packetplayinsettings.f());
        this.getDataWatcher().set(EntityPlayer.DATA_PLAYER_MAIN_HAND, (byte) (packetplayinsettings.getMainHand() == EnumMainHand.LEFT ? 0 : 1));
    }

    public boolean w() {
        return this.canChatColor;
    }

    public EnumChatVisibility getChatFlags() {
        return this.chatVisibility;
    }

    private boolean a(ChatMessageType chatmessagetype) {
        switch (this.chatVisibility) {
            case HIDDEN:
                return chatmessagetype == ChatMessageType.GAME_INFO;
            case SYSTEM:
                return chatmessagetype == ChatMessageType.SYSTEM || chatmessagetype == ChatMessageType.GAME_INFO;
            case FULL:
            default:
                return true;
        }
    }

    public void setResourcePack(String s, String s1, boolean flag, @Nullable IChatBaseComponent ichatbasecomponent) {
        this.connection.sendPacket(new PacketPlayOutResourcePackSend(s, s1, flag, ichatbasecomponent));
    }

    @Override
    protected int y() {
        return this.server.b(this.getProfile());
    }

    public void resetIdleTimer() {
        this.lastActionTime = SystemUtils.getMonotonicMillis();
    }

    public ServerStatisticManager getStatisticManager() {
        return this.stats;
    }

    public RecipeBookServer getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void C() {
        if (this.isSpectator()) {
            this.dP();
            this.setInvisible(true);
        } else {
            super.C();
        }

    }

    public Entity getSpecatorTarget() {
        return (Entity) (this.camera == null ? this : this.camera);
    }

    public void setSpectatorTarget(Entity entity) {
        Entity entity1 = this.getSpecatorTarget();

        this.camera = (Entity) (entity == null ? this : entity);
        if (entity1 != this.camera) {
            this.connection.sendPacket(new PacketPlayOutCamera(this.camera));
            this.connection.b(this.camera.locX(), this.camera.locY(), this.camera.locZ(), this.getYRot(), this.getXRot(), TeleportCause.SPECTATE); // CraftBukkit
        }

    }

    @Override
    protected void E() {
        if (!this.isChangingDimension) {
            super.E();
        }

    }

    @Override
    public void attack(Entity entity) {
        if (this.gameMode.getGameMode() == EnumGamemode.SPECTATOR) {
            this.setSpectatorTarget(entity);
        } else {
            super.attack(entity);
        }

    }

    public long F() {
        return this.lastActionTime;
    }

    @Nullable
    public IChatBaseComponent getPlayerListName() {
        return listName; // CraftBukkit
    }

    @Override
    public void swingHand(EnumHand enumhand) {
        super.swingHand(enumhand);
        this.resetAttackCooldown();
    }

    public boolean H() {
        return this.isChangingDimension;
    }

    public void I() {
        this.isChangingDimension = false;
    }

    public AdvancementDataPlayer getAdvancementData() {
        return this.advancements;
    }

    // CraftBukkit start
    public void a(WorldServer worldserver, double d0, double d1, double d2, float f, float f1) {
        this.a(worldserver, d0, d1, d2, f, f1, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void a(WorldServer worldserver, double d0, double d1, double d2, float f, float f1, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        // CraftBukkit end
        this.setSpectatorTarget(this);
        this.stopRiding();
        /* CraftBukkit start - replace with bukkit handling for multi-world
        if (worldserver == this.level) {
            this.connection.b(d0, d1, d2, f, f1);
        } else {
            WorldServer worldserver1 = this.getWorldServer();
            WorldData worlddata = worldserver.getWorldData();

            this.connection.sendPacket(new PacketPlayOutRespawn(worldserver.getDimensionManager(), worldserver.getDimensionKey(), BiomeManager.a(worldserver.getSeed()), this.gameMode.getGameMode(), this.gameMode.c(), worldserver.isDebugWorld(), worldserver.isFlatWorld(), true));
            this.connection.sendPacket(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
            this.server.getPlayerList().d(this);
            worldserver1.a(this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            this.setPositionRotation(d0, d1, d2, f, f1);
            this.spawnIn(worldserver);
            worldserver.addPlayerCommand(this);
            this.triggerDimensionAdvancements(worldserver1);
            this.connection.b(d0, d1, d2, f, f1);
            this.server.getPlayerList().a(this, worldserver);
            this.server.getPlayerList().updateClient(this);
        }
        */
        this.getBukkitEntity().teleport(new Location(worldserver.getWorld(), d0, d1, d2, f, f1), cause);
        // CraftBukkit end

    }

    @Nullable
    public BlockPosition getSpawn() {
        return this.respawnPosition;
    }

    public float getSpawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<World> getSpawnDimension() {
        return this.respawnDimension;
    }

    public boolean isSpawnForced() {
        return this.respawnForced;
    }

    public void setRespawnPosition(ResourceKey<World> resourcekey, @Nullable BlockPosition blockposition, float f, boolean flag, boolean flag1) {
        if (blockposition != null) {
            boolean flag2 = blockposition.equals(this.respawnPosition) && resourcekey.equals(this.respawnDimension);

            if (flag1 && !flag2) {
                this.sendMessage(new ChatMessage("block.minecraft.set_spawn"), SystemUtils.NIL_UUID);
            }

            this.respawnPosition = blockposition;
            this.respawnDimension = resourcekey;
            this.respawnAngle = f;
            this.respawnForced = flag;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = World.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }

    }

    public void a(ChunkCoordIntPair chunkcoordintpair, Packet<?> packet, Packet<?> packet1) {
        this.connection.sendPacket(packet1);
        this.connection.sendPacket(packet);
    }

    public void a(ChunkCoordIntPair chunkcoordintpair) {
        if (this.isAlive()) {
            this.connection.sendPacket(new PacketPlayOutUnloadChunk(chunkcoordintpair.x, chunkcoordintpair.z));
        }

    }

    public SectionPosition O() {
        return this.lastSectionPos;
    }

    public void a(SectionPosition sectionposition) {
        this.lastSectionPos = sectionposition;
    }

    @Override
    public void a(SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1) {
        this.connection.sendPacket(new PacketPlayOutNamedSoundEffect(soundeffect, soundcategory, this.locX(), this.locY(), this.locZ(), f, f1));
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutNamedEntitySpawn(this);
    }

    @Override
    public EntityItem a(ItemStack itemstack, boolean flag, boolean flag1) {
        EntityItem entityitem = super.a(itemstack, flag, flag1);

        if (entityitem == null) {
            return null;
        } else {
            this.level.addEntity(entityitem);
            ItemStack itemstack1 = entityitem.getItemStack();

            if (flag1) {
                if (!itemstack1.isEmpty()) {
                    this.a(StatisticList.ITEM_DROPPED.b(itemstack1.getItem()), itemstack.getCount());
                }

                this.a(StatisticList.DROP);
            }

            return entityitem;
        }
    }

    public ITextFilter Q() {
        return this.textFilter;
    }

    public void spawnIn(WorldServer worldserver) {
        this.level = worldserver;
        this.gameMode.a(worldserver);
    }

    @Nullable
    private static EnumGamemode a(@Nullable NBTTagCompound nbttagcompound, String s) {
        return nbttagcompound != null && nbttagcompound.hasKeyOfType(s, 99) ? EnumGamemode.getById(nbttagcompound.getInt(s)) : null;
    }

    private EnumGamemode b(@Nullable EnumGamemode enumgamemode) {
        EnumGamemode enumgamemode1 = this.server.aY();

        return enumgamemode1 != null ? enumgamemode1 : (enumgamemode != null ? enumgamemode : this.server.getGamemode());
    }

    public void c(@Nullable NBTTagCompound nbttagcompound) {
        this.gameMode.a(this.b(a(nbttagcompound, "playerGameType")), a(nbttagcompound, "previousPlayerGameType"));
    }

    private void k(NBTTagCompound nbttagcompound) {
        nbttagcompound.setInt("playerGameType", this.gameMode.getGameMode().getId());
        EnumGamemode enumgamemode = this.gameMode.c();

        if (enumgamemode != null) {
            nbttagcompound.setInt("previousPlayerGameType", enumgamemode.getId());
        }

    }

    public boolean R() {
        return this.textFilteringEnabled;
    }

    public boolean b(EntityPlayer entityplayer) {
        return entityplayer == this ? false : this.textFilteringEnabled || entityplayer.textFilteringEnabled;
    }

    @Override
    public boolean a(World world, BlockPosition blockposition) {
        return super.a(world, blockposition) && world.a((EntityHuman) this, blockposition);
    }

    @Override
    protected void a(ItemStack itemstack) {
        CriterionTriggers.USING_ITEM.a(this, itemstack);
        super.a(itemstack);
    }

    public boolean dropItem(boolean flag) {
        PlayerInventory playerinventory = this.getInventory();
        ItemStack itemstack = playerinventory.a(flag);

        this.containerMenu.b(playerinventory, playerinventory.selected).ifPresent((i) -> {
            this.containerMenu.a(i, playerinventory.getItemInHand());
        });
        return this.a(itemstack, false, true) != null;
    }

    // CraftBukkit start - Add per-player time and weather.
    public long timeOffset = 0;
    public boolean relativeTime = true;

    public long getPlayerTime() {
        if (this.relativeTime) {
            // Adds timeOffset to the current server time.
            return this.level.getDayTime() + this.timeOffset;
        } else {
            // Adds timeOffset to the beginning of this day.
            return this.level.getDayTime() - (this.level.getDayTime() % 24000) + this.timeOffset;
        }
    }

    public WeatherType weather = null;

    public WeatherType getPlayerWeather() {
        return this.weather;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }

        if (plugin) {
            this.weather = type;
        }

        if (type == WeatherType.DOWNFALL) {
            this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.STOP_RAINING, 0));
        } else {
            this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.START_RAINING, 0));
        }
    }

    private float pluginRainPosition;
    private float pluginRainPositionPrevious;

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            // Vanilla
            if (oldRain != newRain) {
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, newRain));
            }
        } else {
            // Plugin
            if (pluginRainPositionPrevious != pluginRainPosition) {
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, pluginRainPosition));
            }
        }

        if (oldThunder != newThunder) {
            if (weather == WeatherType.DOWNFALL || weather == null) {
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                this.connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, 0));
            }
        }
    }

    public void tickWeather() {
        if (this.weather == null) return;

        pluginRainPositionPrevious = pluginRainPosition;
        if (weather == WeatherType.DOWNFALL) {
            pluginRainPosition += 0.01;
        } else {
            pluginRainPosition -= 0.01;
        }

        pluginRainPosition = MathHelper.a(pluginRainPosition, 0.0F, 1.0F);
    }

    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(this.level.getWorldData().hasStorm() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getName() + " at " + this.locX() + "," + this.locY() + "," + this.locZ() + ")";
    }

    // SPIGOT-1903, MC-98153
    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.setPositionRotation(x, y, z, yaw, pitch);
        this.connection.syncPosition();
    }

    @Override
    public boolean isFrozen() {
        return super.isFrozen() || !getBukkitEntity().isOnline();
    }

    @Override
    public Scoreboard getScoreboard() {
        return getBukkitEntity().getScoreboard().getHandle();
    }

    public void reset() {
        float exp = 0;
        boolean keepInventory = this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            exp = this.experienceProgress;
            this.newTotalExp = this.totalExperience;
            this.newLevel = this.experienceLevel;
        }

        this.setHealth(this.getMaxHealth());
        this.clearActiveItem(); // CraftBukkit - SPIGOT-6682: Clear active item on reset
        this.remainingFireTicks = 0;
        this.fallDistance = 0;
        this.foodData = new FoodMetaData(this);
        this.experienceLevel = this.newLevel;
        this.totalExperience = this.newTotalExp;
        this.experienceProgress = 0;
        this.deathTime = 0;
        this.setArrowCount(0, true); // CraftBukkit - ArrowBodyCountChangeEvent
        this.removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.DEATH);
        this.effectsDirty = true;
        this.containerMenu = this.inventoryMenu;
        this.lastHurtByPlayer = null;
        this.lastHurtByMob = null;
        this.combatTracker = new CombatTracker(this);
        this.lastSentExp = -1;
        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            this.experienceProgress = exp;
        } else {
            this.giveExp(this.newExp);
        }
        this.keepLevel = false;
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) super.getBukkitEntity();
    }
    // CraftBukkit end
}
