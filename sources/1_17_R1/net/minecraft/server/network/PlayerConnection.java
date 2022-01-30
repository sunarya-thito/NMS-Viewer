package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.EnumChatFormat;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PlayerConnectionUtils;
import net.minecraft.network.protocol.game.PacketListenerPlayIn;
import net.minecraft.network.protocol.game.PacketPlayInAbilities;
import net.minecraft.network.protocol.game.PacketPlayInAdvancements;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
import net.minecraft.network.protocol.game.PacketPlayInAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayInBEdit;
import net.minecraft.network.protocol.game.PacketPlayInBeacon;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayInBlockPlace;
import net.minecraft.network.protocol.game.PacketPlayInBoatMove;
import net.minecraft.network.protocol.game.PacketPlayInChat;
import net.minecraft.network.protocol.game.PacketPlayInClientCommand;
import net.minecraft.network.protocol.game.PacketPlayInCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayInCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyChange;
import net.minecraft.network.protocol.game.PacketPlayInDifficultyLock;
import net.minecraft.network.protocol.game.PacketPlayInEnchantItem;
import net.minecraft.network.protocol.game.PacketPlayInEntityAction;
import net.minecraft.network.protocol.game.PacketPlayInEntityNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInFlying;
import net.minecraft.network.protocol.game.PacketPlayInHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayInItemName;
import net.minecraft.network.protocol.game.PacketPlayInJigsawGenerate;
import net.minecraft.network.protocol.game.PacketPlayInKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayInPickItem;
import net.minecraft.network.protocol.game.PacketPlayInRecipeDisplayed;
import net.minecraft.network.protocol.game.PacketPlayInRecipeSettings;
import net.minecraft.network.protocol.game.PacketPlayInResourcePackStatus;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandBlock;
import net.minecraft.network.protocol.game.PacketPlayInSetCommandMinecart;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayInSetJigsaw;
import net.minecraft.network.protocol.game.PacketPlayInSettings;
import net.minecraft.network.protocol.game.PacketPlayInSpectate;
import net.minecraft.network.protocol.game.PacketPlayInSteerVehicle;
import net.minecraft.network.protocol.game.PacketPlayInStruct;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.network.protocol.game.PacketPlayInTeleportAccept;
import net.minecraft.network.protocol.game.PacketPlayInTileNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayInTrSel;
import net.minecraft.network.protocol.game.PacketPlayInUpdateSign;
import net.minecraft.network.protocol.game.PacketPlayInUseEntity;
import net.minecraft.network.protocol.game.PacketPlayInUseItem;
import net.minecraft.network.protocol.game.PacketPlayInVehicleMove;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutKeepAlive;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.game.PacketPlayOutNBTQuery;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutTabComplete;
import net.minecraft.network.protocol.game.PacketPlayOutVehicleMove;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.UtilColor;
import net.minecraft.util.thread.IAsyncTaskHandler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IJumpable;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.EnumChatVisibility;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.ContainerBeacon;
import net.minecraft.world.inventory.ContainerMerchant;
import net.minecraft.world.inventory.ContainerRecipeBook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemBucket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockCommand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.animal.EntityFish;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.phys.MovingObjectPosition;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.util.NumberConversions;
// CraftBukkit end

public class PlayerConnection implements ServerPlayerConnection, PacketListenerPlayIn {

    static final Logger LOGGER = LogManager.getLogger();
    private static final int LATENCY_CHECK_INTERVAL = 15000;
    public final NetworkManager connection;
    private final MinecraftServer server;
    public EntityPlayer player;
    private int tickCount;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    // CraftBukkit start - multithreaded fields
    private AtomicInteger chatSpamTickCount = new AtomicInteger();
    // CraftBukkit end
    private int dropSpamTickCount;
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    @Nullable
    private Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    @Nullable
    private Vec3D awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;

    public PlayerConnection(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        this.server = minecraftserver;
        this.connection = networkmanager;
        networkmanager.setPacketListener(this);
        this.player = entityplayer;
        entityplayer.connection = this;
        entityplayer.Q().a();

        // CraftBukkit start - add fields and methods
        this.cserver = minecraftserver.server;
    }

    private final org.bukkit.craftbukkit.CraftServer cserver;
    public boolean processedDisconnect;
    private int lastTick = MinecraftServer.currentTick;
    private int allowedPlayerTicks = 1;
    private int lastDropTick = MinecraftServer.currentTick;
    private int lastBookTick  = MinecraftServer.currentTick;
    private int dropCount = 0;
    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;

    // Get position of last block hit for BlockDamageLevel.STOPPED
    private double lastPosX = Double.MAX_VALUE;
    private double lastPosY = Double.MAX_VALUE;
    private double lastPosZ = Double.MAX_VALUE;
    private float lastPitch = Float.MAX_VALUE;
    private float lastYaw = Float.MAX_VALUE;
    private boolean justTeleported = false;
    private boolean hasMoved; // Spigot

    public CraftPlayer getPlayer() {
        return (this.player == null) ? null : (CraftPlayer) this.player.getBukkitEntity();
    }
    // CraftBukkit end

    public void tick() {
        org.bukkit.craftbukkit.SpigotTimings.playerConnectionTimer.startTiming(); // Spigot
        this.syncPosition();
        this.player.xo = this.player.locX();
        this.player.yo = this.player.locY();
        this.player.zo = this.player.locZ();
        this.player.playerTick();
        this.player.setLocation(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        ++this.tickCount;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping()) {
            if (++this.aboveGroundTickCount > 80) {
                PlayerConnection.LOGGER.warn("{} was kicked for floating too long!", this.player.getDisplayName().getString());
                this.disconnect(new ChatMessage("multiplayer.disconnect.flying"));
                return;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }

        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle != this.player && this.lastVehicle.getRidingPassenger() == this.player) {
            this.vehicleFirstGoodX = this.lastVehicle.locX();
            this.vehicleFirstGoodY = this.lastVehicle.locY();
            this.vehicleFirstGoodZ = this.lastVehicle.locZ();
            this.vehicleLastGoodX = this.lastVehicle.locX();
            this.vehicleLastGoodY = this.lastVehicle.locY();
            this.vehicleLastGoodZ = this.lastVehicle.locZ();
            if (this.clientVehicleIsFloating && this.player.getRootVehicle().getRidingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > 80) {
                    PlayerConnection.LOGGER.warn("{} was kicked for floating a vehicle too long!", this.player.getDisplayName().getString());
                    this.disconnect(new ChatMessage("multiplayer.disconnect.flying"));
                    return;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        } else {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        }

        this.server.getMethodProfiler().enter("keepAlive");
        long i = SystemUtils.getMonotonicMillis();

        if (i - this.keepAliveTime >= 25000L) { // CraftBukkit
            if (this.keepAlivePending) {
                this.disconnect(new ChatMessage("disconnect.timeout"));
            } else {
                this.keepAlivePending = true;
                this.keepAliveTime = i;
                this.keepAliveChallenge = i;
                this.sendPacket(new PacketPlayOutKeepAlive(this.keepAliveChallenge));
            }
        }

        this.server.getMethodProfiler().exit();
        // CraftBukkit start
        for (int spam; (spam = this.chatSpamTickCount.get()) > 0 && !chatSpamTickCount.compareAndSet(spam, spam - 1); ) ;
        /* Use thread-safe field access instead
        if (this.chatSpamTickCount > 0) {
            --this.chatSpamTickCount;
        }
        */
        // CraftBukkit end

        if (this.dropSpamTickCount > 0) {
            --this.dropSpamTickCount;
        }

        if (this.player.F() > 0L && this.server.getIdleTimeout() > 0 && SystemUtils.getMonotonicMillis() - this.player.F() > (long) (this.server.getIdleTimeout() * 1000 * 60)) {
            this.player.resetIdleTimer(); // CraftBukkit - SPIGOT-854
            this.disconnect(new ChatMessage("multiplayer.disconnect.idling"));
        }
        org.bukkit.craftbukkit.SpigotTimings.playerConnectionTimer.stopTiming(); // Spigot

    }

    public void syncPosition() {
        this.firstGoodX = this.player.locX();
        this.firstGoodY = this.player.locY();
        this.firstGoodZ = this.player.locZ();
        this.lastGoodX = this.player.locX();
        this.lastGoodY = this.player.locY();
        this.lastGoodZ = this.player.locZ();
    }

    @Override
    public NetworkManager a() {
        return this.connection;
    }

    private boolean isExemptPlayer() {
        return this.server.a(this.player.getProfile());
    }

    // CraftBukkit start
    @Deprecated
    public void disconnect(IChatBaseComponent ichatbasecomponent) {
        disconnect(CraftChatMessage.fromComponent(ichatbasecomponent));
    }
    // CraftBukkit end

    public void disconnect(String s) {
        // CraftBukkit start - fire PlayerKickEvent
        if (this.processedDisconnect) {
            return;
        }
        String leaveMessage = EnumChatFormat.YELLOW + this.player.getName() + " left the game.";

        PlayerKickEvent event = new PlayerKickEvent(this.player.getBukkitEntity(), s, leaveMessage);

        if (this.cserver.getServer().isRunning()) {
            this.cserver.getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            // Do not kick the player
            return;
        }
        // Send the possibly modified leave message
        s = event.getReason();
        final IChatBaseComponent ichatbasecomponent = CraftChatMessage.fromString(s, true)[0];
        // CraftBukkit end

        this.connection.sendPacket(new PacketPlayOutKickDisconnect(ichatbasecomponent), (future) -> {
            this.connection.close(ichatbasecomponent);
        });
        this.a(ichatbasecomponent); // CraftBukkit - fire quit instantly
        this.connection.stopReading();
        MinecraftServer minecraftserver = this.server;
        NetworkManager networkmanager = this.connection;

        Objects.requireNonNull(this.connection);
        // CraftBukkit - Don't wait
        minecraftserver.postToMainThread(networkmanager::handleDisconnection);
    }

    private <T, R> void a(T t0, Consumer<R> consumer, BiFunction<ITextFilter, T, CompletableFuture<R>> bifunction) {
        IAsyncTaskHandler<?> iasynctaskhandler = this.player.getWorldServer().getMinecraftServer();
        Consumer<R> consumer1 = (object) -> {
            if (this.a().isConnected()) {
                consumer.accept(object);
            } else {
                PlayerConnection.LOGGER.debug("Ignoring packet due to disconnection");
            }

        };

        ((CompletableFuture) bifunction.apply(this.player.Q(), t0)).thenAcceptAsync(consumer1, iasynctaskhandler);
    }

    private void a(String s, Consumer<ITextFilter.a> consumer) {
        this.a(s, consumer, ITextFilter::a); // CraftBukkit - decompile error
    }

    private void a(List<String> list, Consumer<List<ITextFilter.a>> consumer) {
        this.a(list, consumer, ITextFilter::a); // CraftBukkit - decompile error
    }

    @Override
    public void a(PacketPlayInSteerVehicle packetplayinsteervehicle) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsteervehicle, this, this.player.getWorldServer());
        this.player.a(packetplayinsteervehicle.b(), packetplayinsteervehicle.c(), packetplayinsteervehicle.d(), packetplayinsteervehicle.e());
    }

    private static boolean c(double d0, double d1, double d2, float f, float f1) {
        return Double.isNaN(d0) || Double.isNaN(d1) || Double.isNaN(d2) || !Floats.isFinite(f1) || !Floats.isFinite(f);
    }

    private static double a(double d0) {
        return MathHelper.a(d0, -3.0E7D, 3.0E7D);
    }

    private static double b(double d0) {
        return MathHelper.a(d0, -2.0E7D, 2.0E7D);
    }

    @Override
    public void a(PacketPlayInVehicleMove packetplayinvehiclemove) {
        PlayerConnectionUtils.ensureMainThread(packetplayinvehiclemove, this, this.player.getWorldServer());
        if (c(packetplayinvehiclemove.getX(), packetplayinvehiclemove.getY(), packetplayinvehiclemove.getZ(), packetplayinvehiclemove.getYaw(), packetplayinvehiclemove.getPitch())) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_vehicle_movement"));
        } else {
            Entity entity = this.player.getRootVehicle();

            if (entity != this.player && entity.getRidingPassenger() == this.player && entity == this.lastVehicle) {
                WorldServer worldserver = this.player.getWorldServer();
                double d0 = entity.locX();
                double d1 = entity.locY();
                double d2 = entity.locZ();
                double d3 = a(packetplayinvehiclemove.getX());
                double d4 = b(packetplayinvehiclemove.getY());
                double d5 = a(packetplayinvehiclemove.getZ());
                float f = MathHelper.g(packetplayinvehiclemove.getYaw());
                float f1 = MathHelper.g(packetplayinvehiclemove.getPitch());
                double d6 = d3 - this.vehicleFirstGoodX;
                double d7 = d4 - this.vehicleFirstGoodY;
                double d8 = d5 - this.vehicleFirstGoodZ;
                double d9 = entity.getMot().g();
                double d10 = d6 * d6 + d7 * d7 + d8 * d8;


                // CraftBukkit start - handle custom speeds and skipped ticks
                this.allowedPlayerTicks += (System.currentTimeMillis() / 50) - this.lastTick;
                this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                this.lastTick = (int) (System.currentTimeMillis() / 50);

                ++this.receivedMovePacketCount;
                int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                if (i > Math.max(this.allowedPlayerTicks, 5)) {
                    PlayerConnection.LOGGER.debug(this.player.getName() + " is sending move packets too frequently (" + i + " packets since last tick)");
                    i = 1;
                }

                if (d10 > 0) {
                    allowedPlayerTicks -= 1;
                } else {
                    allowedPlayerTicks = 20;
                }
                double speed;
                if (player.getAbilities().flying) {
                    speed = player.getAbilities().flyingSpeed * 20f;
                } else {
                    speed = player.getAbilities().walkingSpeed * 10f;
                }
                speed *= 2f; // TODO: Get the speed of the vehicle instead of the player

                if (d10 - d9 > Math.max(100.0D, Math.pow((double) (org.spigotmc.SpigotConfig.movedTooQuicklyMultiplier * (float) i * speed), 2)) && !this.isExemptPlayer()) {
                // CraftBukkit end
                    PlayerConnection.LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getDisplayName().getString(), this.player.getDisplayName().getString(), d6, d7, d8);
                    this.connection.sendPacket(new PacketPlayOutVehicleMove(entity));
                    return;
                }

                boolean flag = worldserver.getCubes(entity, entity.getBoundingBox().shrink(0.0625D));

                d6 = d3 - this.vehicleLastGoodX;
                d7 = d4 - this.vehicleLastGoodY - 1.0E-6D;
                d8 = d5 - this.vehicleLastGoodZ;
                entity.move(EnumMoveType.PLAYER, new Vec3D(d6, d7, d8));
                double d11 = d7;

                d6 = d3 - entity.locX();
                d7 = d4 - entity.locY();
                if (d7 > -0.5D || d7 < 0.5D) {
                    d7 = 0.0D;
                }

                d8 = d5 - entity.locZ();
                d10 = d6 * d6 + d7 * d7 + d8 * d8;
                boolean flag1 = false;

                if (d10 > org.spigotmc.SpigotConfig.movedWronglyThreshold) { // Spigot
                    flag1 = true;
                    PlayerConnection.LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getDisplayName().getString(), this.player.getDisplayName().getString(), Math.sqrt(d10));
                }
                Location curPos = this.getPlayer().getLocation(); // Spigot

                entity.setLocation(d3, d4, d5, f, f1);
                player.setLocation(d3, d4, d5, this.player.getYRot(), this.player.getXRot()); // CraftBukkit
                boolean flag2 = worldserver.getCubes(entity, entity.getBoundingBox().shrink(0.0625D));

                if (flag && (flag1 || !flag2)) {
                    entity.setLocation(d0, d1, d2, f, f1);
                    player.setLocation(d0, d1, d2, this.player.getYRot(), this.player.getXRot()); // CraftBukkit
                    this.connection.sendPacket(new PacketPlayOutVehicleMove(entity));
                    return;
                }

                // CraftBukkit start - fire PlayerMoveEvent
                Player player = this.getPlayer();
                // Spigot Start
                if ( !hasMoved )
                {
                    lastPosX = curPos.getX();
                    lastPosY = curPos.getY();
                    lastPosZ = curPos.getZ();
                    lastYaw = curPos.getYaw();
                    lastPitch = curPos.getPitch();
                    hasMoved = true;
                }
                // Spigot End
                Location from = new Location(player.getWorld(), lastPosX, lastPosY, lastPosZ, lastYaw, lastPitch); // Get the Players previous Event location.
                Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

                // If the packet contains movement information then we update the To location with the correct XYZ.
                to.setX(packetplayinvehiclemove.getX());
                to.setY(packetplayinvehiclemove.getY());
                to.setZ(packetplayinvehiclemove.getZ());


                // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
                to.setYaw(packetplayinvehiclemove.getYaw());
                to.setPitch(packetplayinvehiclemove.getPitch());

                // Prevent 40 event-calls for less than a single pixel of movement >.>
                double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

                if ((delta > 1f / 256 || deltaAngle > 10f) && !this.player.isFrozen()) {
                    this.lastPosX = to.getX();
                    this.lastPosY = to.getY();
                    this.lastPosZ = to.getZ();
                    this.lastYaw = to.getYaw();
                    this.lastPitch = to.getPitch();

                    // Skip the first time we do this
                    if (true) { // Spigot - don't skip any move events
                        Location oldTo = to.clone();
                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                        this.cserver.getPluginManager().callEvent(event);

                        // If the event is cancelled we move the player back to their old location.
                        if (event.isCancelled()) {
                            teleport(from);
                            return;
                        }

                        // If a Plugin has changed the To destination then we teleport the Player
                        // there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                        // We only do this if the Event was not cancelled.
                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                            this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            return;
                        }

                        // Check to see if the Players Location has some how changed during the call of the event.
                        // This can happen due to a plugin teleporting the player instead of using .setTo()
                        if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
                            this.justTeleported = false;
                            return;
                        }
                    }
                }
                // CraftBukkit end

                this.player.getWorldServer().getChunkProvider().movePlayer(this.player);
                this.player.checkMovement(this.player.locX() - d0, this.player.locY() - d1, this.player.locZ() - d2);
                this.clientVehicleIsFloating = d11 >= -0.03125D && !this.server.getAllowFlight() && this.a(entity);
                this.vehicleLastGoodX = entity.locX();
                this.vehicleLastGoodY = entity.locY();
                this.vehicleLastGoodZ = entity.locZ();
            }

        }
    }

    private boolean a(Entity entity) {
        return entity.level.a(entity.getBoundingBox().g(0.0625D).b(0.0D, -0.55D, 0.0D)).allMatch(BlockBase.BlockData::isAir);
    }

    @Override
    public void a(PacketPlayInTeleportAccept packetplayinteleportaccept) {
        PlayerConnectionUtils.ensureMainThread(packetplayinteleportaccept, this, this.player.getWorldServer());
        if (packetplayinteleportaccept.b() == this.awaitingTeleport && this.awaitingPositionFromClient != null) { // CraftBukkit
            this.player.setLocation(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            if (this.player.H()) {
                this.player.I();
            }

            this.awaitingPositionFromClient = null;
            this.player.getWorldServer().getChunkProvider().movePlayer(this.player); // CraftBukkit
        }

    }

    @Override
    public void a(PacketPlayInRecipeDisplayed packetplayinrecipedisplayed) {
        PlayerConnectionUtils.ensureMainThread(packetplayinrecipedisplayed, this, this.player.getWorldServer());
        Optional<? extends IRecipe<?>> optional = this.server.getCraftingManager().getRecipe(packetplayinrecipedisplayed.b()); // CraftBukkit - decompile error
        RecipeBookServer recipebookserver = this.player.getRecipeBook();

        Objects.requireNonNull(recipebookserver);
        optional.ifPresent(recipebookserver::e);
    }

    @Override
    public void a(PacketPlayInRecipeSettings packetplayinrecipesettings) {
        PlayerConnectionUtils.ensureMainThread(packetplayinrecipesettings, this, this.player.getWorldServer());
        this.player.getRecipeBook().a(packetplayinrecipesettings.b(), packetplayinrecipesettings.c(), packetplayinrecipesettings.d());
    }

    @Override
    public void a(PacketPlayInAdvancements packetplayinadvancements) {
        PlayerConnectionUtils.ensureMainThread(packetplayinadvancements, this, this.player.getWorldServer());
        if (packetplayinadvancements.c() == PacketPlayInAdvancements.Status.OPENED_TAB) {
            MinecraftKey minecraftkey = packetplayinadvancements.d();
            Advancement advancement = this.server.getAdvancementData().a(minecraftkey);

            if (advancement != null) {
                this.player.getAdvancementData().a(advancement);
            }
        }

    }

    @Override
    public void a(PacketPlayInTabComplete packetplayintabcomplete) {
        PlayerConnectionUtils.ensureMainThread(packetplayintabcomplete, this, this.player.getWorldServer());
        // CraftBukkit start
        if (chatSpamTickCount.addAndGet(1) > 500 && !this.server.getPlayerList().isOp(this.player.getProfile())) {
            this.disconnect(new ChatMessage("disconnect.spam", new Object[0]));
            return;
        }
        // CraftBukkit end
        StringReader stringreader = new StringReader(packetplayintabcomplete.c());

        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }

        ParseResults<CommandListenerWrapper> parseresults = this.server.getCommandDispatcher().a().parse(stringreader, this.player.getCommandListener());

        this.server.getCommandDispatcher().a().getCompletionSuggestions(parseresults).thenAccept((suggestions) -> {
            if (suggestions.isEmpty()) return; // CraftBukkit - don't send through empty suggestions - prevents [<args>] from showing for plugins with nothing more to offer
            this.connection.sendPacket(new PacketPlayOutTabComplete(packetplayintabcomplete.b(), suggestions));
        });
    }

    @Override
    public void a(PacketPlayInSetCommandBlock packetplayinsetcommandblock) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsetcommandblock, this, this.player.getWorldServer());
        if (!this.server.getEnableCommandBlock()) {
            this.player.sendMessage(new ChatMessage("advMode.notEnabled"), SystemUtils.NIL_UUID);
        } else if (!this.player.isCreativeAndOp()) {
            this.player.sendMessage(new ChatMessage("advMode.notAllowed"), SystemUtils.NIL_UUID);
        } else {
            CommandBlockListenerAbstract commandblocklistenerabstract = null;
            TileEntityCommand tileentitycommand = null;
            BlockPosition blockposition = packetplayinsetcommandblock.b();
            TileEntity tileentity = this.player.level.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityCommand) {
                tileentitycommand = (TileEntityCommand) tileentity;
                commandblocklistenerabstract = tileentitycommand.getCommandBlock();
            }

            String s = packetplayinsetcommandblock.c();
            boolean flag = packetplayinsetcommandblock.d();

            if (commandblocklistenerabstract != null) {
                TileEntityCommand.Type tileentitycommand_type = tileentitycommand.t();
                IBlockData iblockdata = this.player.level.getType(blockposition);
                EnumDirection enumdirection = (EnumDirection) iblockdata.get(BlockCommand.FACING);
                IBlockData iblockdata1;

                switch (packetplayinsetcommandblock.g()) {
                    case SEQUENCE:
                        iblockdata1 = Blocks.CHAIN_COMMAND_BLOCK.getBlockData();
                        break;
                    case AUTO:
                        iblockdata1 = Blocks.REPEATING_COMMAND_BLOCK.getBlockData();
                        break;
                    case REDSTONE:
                    default:
                        iblockdata1 = Blocks.COMMAND_BLOCK.getBlockData();
                }

                IBlockData iblockdata2 = (IBlockData) ((IBlockData) iblockdata1.set(BlockCommand.FACING, enumdirection)).set(BlockCommand.CONDITIONAL, packetplayinsetcommandblock.e());

                if (iblockdata2 != iblockdata) {
                    this.player.level.setTypeAndData(blockposition, iblockdata2, 2);
                    tileentity.b(iblockdata2);
                    this.player.level.getChunkAtWorldCoords(blockposition).setTileEntity(tileentity);
                }

                commandblocklistenerabstract.setCommand(s);
                commandblocklistenerabstract.a(flag);
                if (!flag) {
                    commandblocklistenerabstract.b((IChatBaseComponent) null);
                }

                tileentitycommand.b(packetplayinsetcommandblock.f());
                if (tileentitycommand_type != packetplayinsetcommandblock.g()) {
                    tileentitycommand.h();
                }

                commandblocklistenerabstract.f();
                if (!UtilColor.b(s)) {
                    this.player.sendMessage(new ChatMessage("advMode.setCommand.success", new Object[]{s}), SystemUtils.NIL_UUID);
                }
            }

        }
    }

    @Override
    public void a(PacketPlayInSetCommandMinecart packetplayinsetcommandminecart) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsetcommandminecart, this, this.player.getWorldServer());
        if (!this.server.getEnableCommandBlock()) {
            this.player.sendMessage(new ChatMessage("advMode.notEnabled"), SystemUtils.NIL_UUID);
        } else if (!this.player.isCreativeAndOp()) {
            this.player.sendMessage(new ChatMessage("advMode.notAllowed"), SystemUtils.NIL_UUID);
        } else {
            CommandBlockListenerAbstract commandblocklistenerabstract = packetplayinsetcommandminecart.a(this.player.level);

            if (commandblocklistenerabstract != null) {
                commandblocklistenerabstract.setCommand(packetplayinsetcommandminecart.b());
                commandblocklistenerabstract.a(packetplayinsetcommandminecart.c());
                if (!packetplayinsetcommandminecart.c()) {
                    commandblocklistenerabstract.b((IChatBaseComponent) null);
                }

                commandblocklistenerabstract.f();
                this.player.sendMessage(new ChatMessage("advMode.setCommand.success", new Object[]{packetplayinsetcommandminecart.b()}), SystemUtils.NIL_UUID);
            }

        }
    }

    @Override
    public void a(PacketPlayInPickItem packetplayinpickitem) {
        PlayerConnectionUtils.ensureMainThread(packetplayinpickitem, this, this.player.getWorldServer());
        this.player.getInventory().c(packetplayinpickitem.b());
        this.player.connection.sendPacket(new PacketPlayOutSetSlot(-2, 0, this.player.getInventory().selected, this.player.getInventory().getItem(this.player.getInventory().selected)));
        this.player.connection.sendPacket(new PacketPlayOutSetSlot(-2, 0, packetplayinpickitem.b(), this.player.getInventory().getItem(packetplayinpickitem.b())));
        this.player.connection.sendPacket(new PacketPlayOutHeldItemSlot(this.player.getInventory().selected));
    }

    @Override
    public void a(PacketPlayInItemName packetplayinitemname) {
        PlayerConnectionUtils.ensureMainThread(packetplayinitemname, this, this.player.getWorldServer());
        if (this.player.containerMenu instanceof ContainerAnvil) {
            ContainerAnvil containeranvil = (ContainerAnvil) this.player.containerMenu;
            String s = SharedConstants.a(packetplayinitemname.b());

            if (s.length() <= 50) {
                containeranvil.a(s);
            }
        }

    }

    @Override
    public void a(PacketPlayInBeacon packetplayinbeacon) {
        PlayerConnectionUtils.ensureMainThread(packetplayinbeacon, this, this.player.getWorldServer());
        if (this.player.containerMenu instanceof ContainerBeacon) {
            ((ContainerBeacon) this.player.containerMenu).c(packetplayinbeacon.b(), packetplayinbeacon.c());
        }

    }

    @Override
    public void a(PacketPlayInStruct packetplayinstruct) {
        PlayerConnectionUtils.ensureMainThread(packetplayinstruct, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockposition = packetplayinstruct.b();
            IBlockData iblockdata = this.player.level.getType(blockposition);
            TileEntity tileentity = this.player.level.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityStructure) {
                TileEntityStructure tileentitystructure = (TileEntityStructure) tileentity;

                tileentitystructure.setUsageMode(packetplayinstruct.d());
                tileentitystructure.setStructureName(packetplayinstruct.e());
                tileentitystructure.a(packetplayinstruct.f());
                tileentitystructure.a(packetplayinstruct.g());
                tileentitystructure.a(packetplayinstruct.h());
                tileentitystructure.a(packetplayinstruct.i());
                tileentitystructure.b(packetplayinstruct.j());
                tileentitystructure.a(packetplayinstruct.k());
                tileentitystructure.d(packetplayinstruct.l());
                tileentitystructure.e(packetplayinstruct.m());
                tileentitystructure.a(packetplayinstruct.n());
                tileentitystructure.a(packetplayinstruct.o());
                if (tileentitystructure.g()) {
                    String s = tileentitystructure.getStructureName();

                    if (packetplayinstruct.c() == TileEntityStructure.UpdateType.SAVE_AREA) {
                        if (tileentitystructure.z()) {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.save_success", new Object[]{s})), false);
                        } else {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.save_failure", new Object[]{s})), false);
                        }
                    } else if (packetplayinstruct.c() == TileEntityStructure.UpdateType.LOAD_AREA) {
                        if (!tileentitystructure.B()) {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.load_not_found", new Object[]{s})), false);
                        } else if (tileentitystructure.a(this.player.getWorldServer())) {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.load_success", new Object[]{s})), false);
                        } else {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.load_prepare", new Object[]{s})), false);
                        }
                    } else if (packetplayinstruct.c() == TileEntityStructure.UpdateType.SCAN_AREA) {
                        if (tileentitystructure.y()) {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.size_success", new Object[]{s})), false);
                        } else {
                            this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.size_failure")), false);
                        }
                    }
                } else {
                    this.player.a((IChatBaseComponent) (new ChatMessage("structure_block.invalid_structure_name", new Object[]{packetplayinstruct.e()})), false);
                }

                tileentitystructure.update();
                this.player.level.notify(blockposition, iblockdata, iblockdata, 3);
            }

        }
    }

    @Override
    public void a(PacketPlayInSetJigsaw packetplayinsetjigsaw) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsetjigsaw, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockposition = packetplayinsetjigsaw.b();
            IBlockData iblockdata = this.player.level.getType(blockposition);
            TileEntity tileentity = this.player.level.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityJigsaw) {
                TileEntityJigsaw tileentityjigsaw = (TileEntityJigsaw) tileentity;

                tileentityjigsaw.a(packetplayinsetjigsaw.c());
                tileentityjigsaw.b(packetplayinsetjigsaw.d());
                tileentityjigsaw.c(packetplayinsetjigsaw.e());
                tileentityjigsaw.a(packetplayinsetjigsaw.f());
                tileentityjigsaw.a(packetplayinsetjigsaw.g());
                tileentityjigsaw.update();
                this.player.level.notify(blockposition, iblockdata, iblockdata, 3);
            }

        }
    }

    @Override
    public void a(PacketPlayInJigsawGenerate packetplayinjigsawgenerate) {
        PlayerConnectionUtils.ensureMainThread(packetplayinjigsawgenerate, this, this.player.getWorldServer());
        if (this.player.isCreativeAndOp()) {
            BlockPosition blockposition = packetplayinjigsawgenerate.b();
            TileEntity tileentity = this.player.level.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityJigsaw) {
                TileEntityJigsaw tileentityjigsaw = (TileEntityJigsaw) tileentity;

                tileentityjigsaw.a(this.player.getWorldServer(), packetplayinjigsawgenerate.c(), packetplayinjigsawgenerate.d());
            }

        }
    }

    @Override
    public void a(PacketPlayInTrSel packetplayintrsel) {
        PlayerConnectionUtils.ensureMainThread(packetplayintrsel, this, this.player.getWorldServer());
        int i = packetplayintrsel.b();
        Container container = this.player.containerMenu;

        if (container instanceof ContainerMerchant) {
            ContainerMerchant containermerchant = (ContainerMerchant) container;
            CraftEventFactory.callTradeSelectEvent(this.player, i, containermerchant); // CraftBukkit

            containermerchant.d(i);
            containermerchant.g(i);
        }

    }

    @Override
    public void a(PacketPlayInBEdit packetplayinbedit) {
        // CraftBukkit start
        if (this.lastBookTick + 20 > MinecraftServer.currentTick) {
            this.disconnect("Book edited too quickly!");
            return;
        }
        this.lastBookTick = MinecraftServer.currentTick;
        // CraftBukkit end
        int i = packetplayinbedit.d();

        if (PlayerInventory.d(i) || i == 40) {
            List<String> list = Lists.newArrayList();
            Optional<String> optional = packetplayinbedit.c();

            Objects.requireNonNull(list);
            optional.ifPresent(list::add);
            Stream<String> stream = packetplayinbedit.b().stream().limit(100L); // CraftBukkit - decompile error

            Objects.requireNonNull(list);
            stream.forEach(list::add);
            this.a((List) list, optional.isPresent() ? (list1) -> {
                this.a((ITextFilter.a) list1.get(0), list1.subList(1, list1.size()), i);
            } : (list1) -> {
                this.a(list1, i);
            });
        }
    }

    private void a(List<ITextFilter.a> list, int i) {
        ItemStack itemstack = this.player.getInventory().getItem(i);

        if (itemstack.a(Items.WRITABLE_BOOK)) {
            this.a(list, UnaryOperator.identity(), itemstack.cloneItemStack(), i, itemstack); // CraftBukkit
        }
    }

    private void a(ITextFilter.a itextfilter_a, List<ITextFilter.a> list, int i) {
        ItemStack itemstack = this.player.getInventory().getItem(i);

        if (itemstack.a(Items.WRITABLE_BOOK)) {
            ItemStack itemstack1 = new ItemStack(Items.WRITTEN_BOOK);
            NBTTagCompound nbttagcompound = itemstack.getTag();

            if (nbttagcompound != null) {
                itemstack1.setTag(nbttagcompound.clone());
            }

            itemstack1.a("author", (NBTBase) NBTTagString.a(this.player.getDisplayName().getString()));
            if (this.player.R()) {
                itemstack1.a("title", (NBTBase) NBTTagString.a(itextfilter_a.b()));
            } else {
                itemstack1.a("filtered_title", (NBTBase) NBTTagString.a(itextfilter_a.b()));
                itemstack1.a("title", (NBTBase) NBTTagString.a(itextfilter_a.a()));
            }

            this.a(list, (s) -> {
                return IChatBaseComponent.ChatSerializer.a((IChatBaseComponent) (new ChatComponentText(s)));
            }, itemstack1, i, itemstack); // CraftBukkit
            this.player.getInventory().setItem(i, itemstack); // CraftBukkit - event factory updates the hand book
        }
    }

    private void a(List<ITextFilter.a> list, UnaryOperator<String> unaryoperator, ItemStack itemstack, int slot, ItemStack handItem) { // CraftBukkit
        NBTTagList nbttaglist = new NBTTagList();

        if (this.player.R()) {
            Stream<NBTTagString> stream = list.stream().map((itextfilter_a) -> { // CraftBukkit - decompile error
                return NBTTagString.a((String) unaryoperator.apply(itextfilter_a.b()));
            });

            Objects.requireNonNull(nbttaglist);
            stream.forEach(nbttaglist::add);
        } else {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            int i = 0;

            for (int j = list.size(); i < j; ++i) {
                ITextFilter.a itextfilter_a = (ITextFilter.a) list.get(i);
                String s = itextfilter_a.a();

                nbttaglist.add(NBTTagString.a((String) unaryoperator.apply(s)));
                String s1 = itextfilter_a.b();

                if (!s.equals(s1)) {
                    nbttagcompound.setString(String.valueOf(i), (String) unaryoperator.apply(s1));
                }
            }

            if (!nbttagcompound.isEmpty()) {
                itemstack.a("filtered_pages", (NBTBase) nbttagcompound);
            }
        }

        itemstack.a("pages", (NBTBase) nbttaglist);
        CraftEventFactory.handleEditBookEvent(player, slot, handItem, itemstack); // CraftBukkit
    }

    @Override
    public void a(PacketPlayInEntityNBTQuery packetplayinentitynbtquery) {
        PlayerConnectionUtils.ensureMainThread(packetplayinentitynbtquery, this, this.player.getWorldServer());
        if (this.player.l(2)) {
            Entity entity = this.player.getWorldServer().getEntity(packetplayinentitynbtquery.c());

            if (entity != null) {
                NBTTagCompound nbttagcompound = entity.save(new NBTTagCompound());

                this.player.connection.sendPacket(new PacketPlayOutNBTQuery(packetplayinentitynbtquery.b(), nbttagcompound));
            }

        }
    }

    @Override
    public void a(PacketPlayInTileNBTQuery packetplayintilenbtquery) {
        PlayerConnectionUtils.ensureMainThread(packetplayintilenbtquery, this, this.player.getWorldServer());
        if (this.player.l(2)) {
            TileEntity tileentity = this.player.getWorldServer().getTileEntity(packetplayintilenbtquery.c());
            NBTTagCompound nbttagcompound = tileentity != null ? tileentity.save(new NBTTagCompound()) : null;

            this.player.connection.sendPacket(new PacketPlayOutNBTQuery(packetplayintilenbtquery.b(), nbttagcompound));
        }
    }

    @Override
    public void a(PacketPlayInFlying packetplayinflying) {
        PlayerConnectionUtils.ensureMainThread(packetplayinflying, this, this.player.getWorldServer());
        if (c(packetplayinflying.a(0.0D), packetplayinflying.b(0.0D), packetplayinflying.c(0.0D), packetplayinflying.a(0.0F), packetplayinflying.b(0.0F))) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_player_movement"));
        } else {
            WorldServer worldserver = this.player.getWorldServer();

            if (!this.player.wonGame && !this.player.isFrozen()) { // CraftBukkit
                if (this.tickCount == 0) {
                    this.syncPosition();
                }

                if (this.awaitingPositionFromClient != null) {
                    if (this.tickCount - this.awaitingTeleportTime > 20) {
                        this.awaitingTeleportTime = this.tickCount;
                        this.b(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
                    }
                    this.allowedPlayerTicks = 20; // CraftBukkit
                } else {
                    this.awaitingTeleportTime = this.tickCount;
                    double d0 = a(packetplayinflying.a(this.player.locX()));
                    double d1 = b(packetplayinflying.b(this.player.locY()));
                    double d2 = a(packetplayinflying.c(this.player.locZ()));
                    float f = MathHelper.g(packetplayinflying.a(this.player.getYRot()));
                    float f1 = MathHelper.g(packetplayinflying.b(this.player.getXRot()));

                    if (this.player.isPassenger()) {
                        this.player.setLocation(this.player.locX(), this.player.locY(), this.player.locZ(), f, f1);
                        this.player.getWorldServer().getChunkProvider().movePlayer(this.player);
                        this.allowedPlayerTicks = 20; // CraftBukkit
                    } else {
                        // CraftBukkit - Make sure the move is valid but then reset it for plugins to modify
                        double prevX = player.locX();
                        double prevY = player.locY();
                        double prevZ = player.locZ();
                        float prevYaw = player.getYRot();
                        float prevPitch = player.getXRot();
                        // CraftBukkit end
                        double d3 = this.player.locX();
                        double d4 = this.player.locY();
                        double d5 = this.player.locZ();
                        double d6 = this.player.locY();
                        double d7 = d0 - this.firstGoodX;
                        double d8 = d1 - this.firstGoodY;
                        double d9 = d2 - this.firstGoodZ;
                        double d10 = this.player.getMot().g();
                        double d11 = d7 * d7 + d8 * d8 + d9 * d9;

                        if (this.player.isSleeping()) {
                            if (d11 > 1.0D) {
                                this.b(this.player.locX(), this.player.locY(), this.player.locZ(), f, f1);
                            }

                        } else {
                            ++this.receivedMovePacketCount;
                            int i = this.receivedMovePacketCount - this.knownMovePacketCount;

                            // CraftBukkit start - handle custom speeds and skipped ticks
                            this.allowedPlayerTicks += (System.currentTimeMillis() / 50) - this.lastTick;
                            this.allowedPlayerTicks = Math.max(this.allowedPlayerTicks, 1);
                            this.lastTick = (int) (System.currentTimeMillis() / 50);

                            if (i > Math.max(this.allowedPlayerTicks, 5)) {
                                PlayerConnection.LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getDisplayName().getString(), i);
                                i = 1;
                            }

                            if (packetplayinflying.hasRot || d11 > 0) {
                                allowedPlayerTicks -= 1;
                            } else {
                                allowedPlayerTicks = 20;
                            }
                            double speed;
                            if (player.getAbilities().flying) {
                                speed = player.getAbilities().flyingSpeed * 20f;
                            } else {
                                speed = player.getAbilities().walkingSpeed * 10f;
                            }

                            if (!this.player.H() && (!this.player.getWorldServer().getGameRules().getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) || !this.player.isGliding())) {
                                float f2 = this.player.isGliding() ? 300.0F : 100.0F;

                                if (d11 - d10 > Math.max(f2, Math.pow((double) (org.spigotmc.SpigotConfig.movedTooQuicklyMultiplier * (float) i * speed), 2)) && !this.isExemptPlayer()) {
                                // CraftBukkit end
                                    PlayerConnection.LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getDisplayName().getString(), d7, d8, d9);
                                    this.b(this.player.locX(), this.player.locY(), this.player.locZ(), this.player.getYRot(), this.player.getXRot());
                                    return;
                                }
                            }

                            AxisAlignedBB axisalignedbb = this.player.getBoundingBox();

                            d7 = d0 - this.lastGoodX;
                            d8 = d1 - this.lastGoodY;
                            d9 = d2 - this.lastGoodZ;
                            boolean flag = d8 > 0.0D;

                            if (this.player.isOnGround() && !packetplayinflying.b() && flag) {
                                this.player.jump();
                            }

                            this.player.move(EnumMoveType.PLAYER, new Vec3D(d7, d8, d9));
                            this.player.setOnGround(packetplayinflying.b()); // CraftBukkit - SPIGOT-5810, SPIGOT-5835: reset by this.player.move
                            double d12 = d8;

                            d7 = d0 - this.player.locX();
                            d8 = d1 - this.player.locY();
                            if (d8 > -0.5D || d8 < 0.5D) {
                                d8 = 0.0D;
                            }

                            d9 = d2 - this.player.locZ();
                            d11 = d7 * d7 + d8 * d8 + d9 * d9;
                            boolean flag1 = false;

                            if (!this.player.H() && d11 > org.spigotmc.SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.gameMode.isCreative() && this.player.gameMode.getGameMode() != EnumGamemode.SPECTATOR) { // Spigot
                                flag1 = true;
                                PlayerConnection.LOGGER.warn("{} moved wrongly!", this.player.getDisplayName().getString());
                            }

                            this.player.setLocation(d0, d1, d2, f, f1);
                            if (!this.player.noPhysics && !this.player.isSleeping() && (flag1 && worldserver.getCubes(this.player, axisalignedbb) || this.a((IWorldReader) worldserver, axisalignedbb))) {
                                this.b(d3, d4, d5, f, f1);
                            } else {
                                // CraftBukkit start - fire PlayerMoveEvent
                                // Rest to old location first
                                this.player.setLocation(prevX, prevY, prevZ, prevYaw, prevPitch);

                                Player player = this.getPlayer();
                                Location from = new Location(player.getWorld(), lastPosX, lastPosY, lastPosZ, lastYaw, lastPitch); // Get the Players previous Event location.
                                Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

                                // If the packet contains movement information then we update the To location with the correct XYZ.
                                if (packetplayinflying.hasPos) {
                                    to.setX(packetplayinflying.x);
                                    to.setY(packetplayinflying.y);
                                    to.setZ(packetplayinflying.z);
                                }

                                // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
                                if (packetplayinflying.hasRot) {
                                    to.setYaw(packetplayinflying.yRot);
                                    to.setPitch(packetplayinflying.xRot);
                                }

                                // Prevent 40 event-calls for less than a single pixel of movement >.>
                                double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
                                float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

                                if ((delta > 1f / 256 || deltaAngle > 10f) && !this.player.isFrozen()) {
                                    this.lastPosX = to.getX();
                                    this.lastPosY = to.getY();
                                    this.lastPosZ = to.getZ();
                                    this.lastYaw = to.getYaw();
                                    this.lastPitch = to.getPitch();

                                    // Skip the first time we do this
                                    if (from.getX() != Double.MAX_VALUE) {
                                        Location oldTo = to.clone();
                                        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                                        this.cserver.getPluginManager().callEvent(event);

                                        // If the event is cancelled we move the player back to their old location.
                                        if (event.isCancelled()) {
                                            teleport(from);
                                            return;
                                        }

                                        // If a Plugin has changed the To destination then we teleport the Player
                                        // there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                                        // We only do this if the Event was not cancelled.
                                        if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                                            this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                            return;
                                        }

                                        // Check to see if the Players Location has some how changed during the call of the event.
                                        // This can happen due to a plugin teleporting the player instead of using .setTo()
                                        if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
                                            this.justTeleported = false;
                                            return;
                                        }
                                    }
                                }
                                this.player.setLocation(d0, d1, d2, f, f1); // Copied from above

                                // MC-135989, SPIGOT-5564: isRiptiding
                                this.clientIsFloating = d12 >= -0.03125D && this.player.gameMode.getGameMode() != EnumGamemode.SPECTATOR && !this.server.getAllowFlight() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !this.player.isGliding() && this.a((Entity) this.player) && !this.player.isRiptiding();
                                // CraftBukkit end
                                this.player.getWorldServer().getChunkProvider().movePlayer(this.player);
                                this.player.a(this.player.locY() - d6, packetplayinflying.b());
                                // this.player.setOnGround(packetplayinflying.b()); // CraftBukkit - moved up
                                if (flag) {
                                    this.player.fallDistance = 0.0F;
                                }

                                this.player.checkMovement(this.player.locX() - d3, this.player.locY() - d4, this.player.locZ() - d5);
                                this.lastGoodX = this.player.locX();
                                this.lastGoodY = this.player.locY();
                                this.lastGoodZ = this.player.locZ();
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean a(IWorldReader iworldreader, AxisAlignedBB axisalignedbb) {
        Stream<VoxelShape> stream = iworldreader.d(this.player, this.player.getBoundingBox().shrink(9.999999747378752E-6D), (entity) -> {
            return true;
        });
        VoxelShape voxelshape = VoxelShapes.a(axisalignedbb.shrink(9.999999747378752E-6D));

        return stream.anyMatch((voxelshape1) -> {
            return !VoxelShapes.c(voxelshape1, voxelshape, OperatorBoolean.AND);
        });
    }

    // CraftBukkit start - Delegate to teleport(Location)
    public void a(double d0, double d1, double d2, float f, float f1) {
        this.a(d0, d1, d2, f, f1, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void a(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.a(d0, d1, d2, f, f1, Collections.emptySet(), true, cause);
    }

    public void b(double d0, double d1, double d2, float f, float f1) {
        this.b(d0, d1, d2, f, f1, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void b(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.a(d0, d1, d2, f, f1, Collections.emptySet(), false, cause);
    }

    public void a(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set) {
        this.a(d0, d1, d2, f, f1, set, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void a(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set, PlayerTeleportEvent.TeleportCause cause) {
        this.a(d0, d1, d2, f, f1, set, false, cause);
    }

    public boolean a(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set, boolean flag, PlayerTeleportEvent.TeleportCause cause) { // CraftBukkit - Return event status
        Player player = this.getPlayer();
        Location from = player.getLocation();

        double x = d0;
        double y = d1;
        double z = d2;
        float yaw = f;
        float pitch = f1;

        Location to = new Location(this.getPlayer().getWorld(), x, y, z, yaw, pitch);
        // SPIGOT-5171: Triggered on join
        if (from.equals(to)) {
            this.internalTeleport(d0, d1, d2, f, f1, set, flag);
            return false; // CraftBukkit - Return event status
        }

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), cause);
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled() || !to.equals(event.getTo())) {
            set.clear(); // Can't relative teleport
            to = event.isCancelled() ? event.getFrom() : event.getTo();
            d0 = to.getX();
            d1 = to.getY();
            d2 = to.getZ();
            f = to.getYaw();
            f1 = to.getPitch();
        }

        this.internalTeleport(d0, d1, d2, f, f1, set, flag);
        return event.isCancelled(); // CraftBukkit - Return event status
    }

    public void teleport(Location dest) {
        internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.<PacketPlayOutPosition.EnumPlayerTeleportFlags>emptySet(), true);
    }

    private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set, boolean flag) {
        // CraftBukkit start
        if (Float.isNaN(f)) {
            f = 0;
        }
        if (Float.isNaN(f1)) {
            f1 = 0;
        }

        this.justTeleported = true;
        // CraftBukkit end
        double d3 = set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X) ? this.player.locX() : 0.0D;
        double d4 = set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y) ? this.player.locY() : 0.0D;
        double d5 = set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z) ? this.player.locZ() : 0.0D;
        float f2 = set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT) ? this.player.getYRot() : 0.0F;
        float f3 = set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT) ? this.player.getXRot() : 0.0F;

        this.awaitingPositionFromClient = new Vec3D(d0, d1, d2);
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        // CraftBukkit start - update last location
        this.lastPosX = this.awaitingPositionFromClient.x;
        this.lastPosY = this.awaitingPositionFromClient.y;
        this.lastPosZ = this.awaitingPositionFromClient.z;
        this.lastYaw = f;
        this.lastPitch = f1;
        // CraftBukkit end

        this.awaitingTeleportTime = this.tickCount;
        this.player.setLocation(d0, d1, d2, f, f1);
        this.player.connection.sendPacket(new PacketPlayOutPosition(d0 - d3, d1 - d4, d2 - d5, f - f2, f1 - f3, set, this.awaitingTeleport, flag));
    }

    @Override
    public void a(PacketPlayInBlockDig packetplayinblockdig) {
        PlayerConnectionUtils.ensureMainThread(packetplayinblockdig, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        BlockPosition blockposition = packetplayinblockdig.b();

        this.player.resetIdleTimer();
        PacketPlayInBlockDig.EnumPlayerDigType packetplayinblockdig_enumplayerdigtype = packetplayinblockdig.d();

        switch (packetplayinblockdig_enumplayerdigtype) {
            case SWAP_ITEM_WITH_OFFHAND:
                if (!this.player.isSpectator()) {
                    ItemStack itemstack = this.player.b(EnumHand.OFF_HAND);

                    // CraftBukkit start - inspiration taken from DispenserRegistry (See SpigotCraft#394)
                    CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack);
                    CraftItemStack offHand = CraftItemStack.asCraftMirror(this.player.b(EnumHand.MAIN_HAND));
                    PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(getPlayer(), mainHand.clone(), offHand.clone());
                    this.cserver.getPluginManager().callEvent(swapItemsEvent);
                    if (swapItemsEvent.isCancelled()) {
                        return;
                    }
                    if (swapItemsEvent.getOffHandItem().equals(offHand)) {
                        this.player.a(EnumHand.OFF_HAND, this.player.b(EnumHand.MAIN_HAND));
                    } else {
                        this.player.a(EnumHand.OFF_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getOffHandItem()));
                    }
                    if (swapItemsEvent.getMainHandItem().equals(mainHand)) {
                        this.player.a(EnumHand.MAIN_HAND, itemstack);
                    } else {
                        this.player.a(EnumHand.MAIN_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getMainHandItem()));
                    }
                    // CraftBukkit end
                    this.player.clearActiveItem();
                }

                return;
            case DROP_ITEM:
                if (!this.player.isSpectator()) {
                    // limit how quickly items can be dropped
                    // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
                    if (this.lastDropTick != MinecraftServer.currentTick) {
                        this.dropCount = 0;
                        this.lastDropTick = MinecraftServer.currentTick;
                    } else {
                        // Else we increment the drop count and check the amount.
                        this.dropCount++;
                        if (this.dropCount >= 20) {
                            LOGGER.warn(this.player.getName() + " dropped their items too quickly!");
                            this.disconnect("You dropped your items too quickly (Hacking?)");
                            return;
                        }
                    }
                    // CraftBukkit end
                    this.player.dropItem(false);
                }

                return;
            case DROP_ALL_ITEMS:
                if (!this.player.isSpectator()) {
                    this.player.dropItem(true);
                }

                return;
            case RELEASE_USE_ITEM:
                this.player.releaseActiveItem();
                return;
            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
                this.player.gameMode.a(blockposition, packetplayinblockdig_enumplayerdigtype, packetplayinblockdig.c(), this.player.level.getMaxBuildHeight());
                return;
            default:
                throw new IllegalArgumentException("Invalid player action");
        }
    }

    private static boolean a(EntityPlayer entityplayer, ItemStack itemstack) {
        if (itemstack.isEmpty()) {
            return false;
        } else {
            Item item = itemstack.getItem();

            return (item instanceof ItemBlock || item instanceof ItemBucket) && !entityplayer.getCooldownTracker().hasCooldown(item);
        }
    }

    // Spigot start - limit place/interactions
    private int limitedPackets;
    private long lastLimitedPacket = -1;

    private boolean checkLimit(long timestamp) {
        if (lastLimitedPacket != -1 && timestamp - lastLimitedPacket < 30 && limitedPackets++ >= 4) {
            return false;
        }

        if (lastLimitedPacket == -1 || timestamp - lastLimitedPacket >= 30) {
            lastLimitedPacket = timestamp;
            limitedPackets = 0;
            return true;
        }

        return true;
    }
    // Spigot end

    @Override
    public void a(PacketPlayInUseItem packetplayinuseitem) {
        PlayerConnectionUtils.ensureMainThread(packetplayinuseitem, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        if (!checkLimit(packetplayinuseitem.timestamp)) return; // Spigot - check limit
        WorldServer worldserver = this.player.getWorldServer();
        EnumHand enumhand = packetplayinuseitem.b();
        ItemStack itemstack = this.player.b(enumhand);
        MovingObjectPositionBlock movingobjectpositionblock = packetplayinuseitem.c();
        BlockPosition blockposition = movingobjectpositionblock.getBlockPosition();
        EnumDirection enumdirection = movingobjectpositionblock.getDirection();

        this.player.resetIdleTimer();
        int i = this.player.level.getMaxBuildHeight();

        if (blockposition.getY() < i) {
            if (this.awaitingPositionFromClient == null && this.player.h((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D) < 64.0D && worldserver.a((EntityHuman) this.player, blockposition)) {
                // CraftBukkit start - Check if we can actually do something over this large a distance
                Location eyeLoc = this.getPlayer().getEyeLocation();
                double reachDistance = NumberConversions.square(eyeLoc.getX() - blockposition.getX()) + NumberConversions.square(eyeLoc.getY() - blockposition.getY()) + NumberConversions.square(eyeLoc.getZ() - blockposition.getZ());
                if (reachDistance > (this.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE ? CREATIVE_PLACE_DISTANCE_SQUARED : SURVIVAL_PLACE_DISTANCE_SQUARED)) {
                    return;
                }
                this.player.clearActiveItem(); // SPIGOT-4706
                // CraftBukkit end
                EnumInteractionResult enuminteractionresult = this.player.gameMode.a(this.player, worldserver, itemstack, enumhand, movingobjectpositionblock);

                if (enumdirection == EnumDirection.UP && !enuminteractionresult.a() && blockposition.getY() >= i - 1 && a(this.player, itemstack)) {
                    IChatMutableComponent ichatmutablecomponent = (new ChatMessage("build.tooHigh", new Object[]{i - 1})).a(EnumChatFormat.RED);

                    this.player.a((IChatBaseComponent) ichatmutablecomponent, ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
                } else if (enuminteractionresult.b()) {
                    this.player.swingHand(enumhand, true);
                }
            }
        } else {
            IChatMutableComponent ichatmutablecomponent1 = (new ChatMessage("build.tooHigh", new Object[]{i - 1})).a(EnumChatFormat.RED);

            this.player.a((IChatBaseComponent) ichatmutablecomponent1, ChatMessageType.GAME_INFO, SystemUtils.NIL_UUID);
        }

        this.player.connection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
        this.player.connection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection)));
    }

    @Override
    public void a(PacketPlayInBlockPlace packetplayinblockplace) {
        PlayerConnectionUtils.ensureMainThread(packetplayinblockplace, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        if (!checkLimit(packetplayinblockplace.timestamp)) return; // Spigot - check limit
        WorldServer worldserver = this.player.getWorldServer();
        EnumHand enumhand = packetplayinblockplace.b();
        ItemStack itemstack = this.player.b(enumhand);

        this.player.resetIdleTimer();
        if (!itemstack.isEmpty()) {
            // CraftBukkit start
            // Raytrace to look for 'rogue armswings'
            float f1 = this.player.getXRot();
            float f2 = this.player.getYRot();
            double d0 = this.player.locX();
            double d1 = this.player.locY() + (double) this.player.getHeadHeight();
            double d2 = this.player.locZ();
            Vec3D vec3d = new Vec3D(d0, d1, d2);

            float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
            float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d3 = player.gameMode.getGameMode()== EnumGamemode.CREATIVE ? 5.0D : 4.5D;
            Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
            MovingObjectPosition movingobjectposition = this.player.level.rayTrace(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.OUTLINE, RayTrace.FluidCollisionOption.NONE, player));

            boolean cancelled;
            if (movingobjectposition == null || movingobjectposition.getType() != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack, enumhand);
                cancelled = event.useItemInHand() == Event.Result.DENY;
            } else {
                MovingObjectPositionBlock movingobjectpositionblock = (MovingObjectPositionBlock) movingobjectposition;
                if (player.gameMode.firedInteract && player.gameMode.interactPosition.equals(movingobjectpositionblock.getBlockPosition()) && player.gameMode.interactHand == enumhand && ItemStack.equals(player.gameMode.interactItemStack, itemstack)) {
                    cancelled = player.gameMode.interactResult;
                } else {
                    org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, movingobjectpositionblock.getBlockPosition(), movingobjectpositionblock.getDirection(), itemstack, true, enumhand);
                    cancelled = event.useItemInHand() == Event.Result.DENY;
                }
                player.gameMode.firedInteract = false;
            }

            if (cancelled) {
                this.player.getBukkitEntity().updateInventory(); // SPIGOT-2524
                return;
            }
            EnumInteractionResult enuminteractionresult = this.player.gameMode.a(this.player, worldserver, itemstack, enumhand);

            if (enuminteractionresult.b()) {
                this.player.swingHand(enumhand, true);
            }

        }
    }

    @Override
    public void a(PacketPlayInSpectate packetplayinspectate) {
        PlayerConnectionUtils.ensureMainThread(packetplayinspectate, this, this.player.getWorldServer());
        if (this.player.isSpectator()) {
            Iterator iterator = this.server.getWorlds().iterator();

            while (iterator.hasNext()) {
                WorldServer worldserver = (WorldServer) iterator.next();
                Entity entity = packetplayinspectate.a(worldserver);

                if (entity != null) {
                    this.player.a(worldserver, entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.SPECTATE); // CraftBukkit
                    return;
                }
            }
        }

    }

    @Override
    public void a(PacketPlayInResourcePackStatus packetplayinresourcepackstatus) {
        PlayerConnectionUtils.ensureMainThread(packetplayinresourcepackstatus, this, this.player.getWorldServer());
        if (packetplayinresourcepackstatus.b() == PacketPlayInResourcePackStatus.EnumResourcePackStatus.DECLINED && this.server.aX()) {
            PlayerConnection.LOGGER.info("Disconnecting {} due to resource pack rejection", this.player.getDisplayName());
            this.disconnect(new ChatMessage("multiplayer.requiredTexturePrompt.disconnect"));
        }
        this.cserver.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(getPlayer(), PlayerResourcePackStatusEvent.Status.values()[packetplayinresourcepackstatus.action.ordinal()])); // CraftBukkit

    }

    @Override
    public void a(PacketPlayInBoatMove packetplayinboatmove) {
        PlayerConnectionUtils.ensureMainThread(packetplayinboatmove, this, this.player.getWorldServer());
        Entity entity = this.player.getVehicle();

        if (entity instanceof EntityBoat) {
            ((EntityBoat) entity).a(packetplayinboatmove.b(), packetplayinboatmove.c());
        }

    }

    @Override
    public void a(ServerboundPongPacket serverboundpongpacket) {}

    @Override
    public void a(IChatBaseComponent ichatbasecomponent) {
        // CraftBukkit start - Rarely it would send a disconnect line twice
        if (this.processedDisconnect) {
            return;
        } else {
            this.processedDisconnect = true;
        }
        // CraftBukkit end
        PlayerConnection.LOGGER.info("{} lost connection: {}", this.player.getDisplayName().getString(), ichatbasecomponent.getString());
        // CraftBukkit start - Replace vanilla quit message handling with our own.
        /*
        this.server.invalidatePingSample();
        this.server.getPlayerList().sendMessage((new ChatMessage("multiplayer.player.left", new Object[]{this.player.getScoreboardDisplayName()})).a(EnumChatFormat.YELLOW), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
        */

        this.player.p();
        String quitMessage = this.server.getPlayerList().disconnect(this.player);
        if ((quitMessage != null) && (quitMessage.length() > 0)) {
            this.server.getPlayerList().sendMessage(CraftChatMessage.fromString(quitMessage));
        }
        // CraftBukkit end
        this.player.Q().b();
        if (this.isExemptPlayer()) {
            PlayerConnection.LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.safeShutdown(false);
        }

    }

    @Override
    public void sendPacket(Packet<?> packet) {
        this.a(packet, (GenericFutureListener) null);
    }

    public void a(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> genericfuturelistener) {
        // CraftBukkit start
        if (packet == null || this.processedDisconnect) { // Spigot
            return;
        } else if (packet instanceof PacketPlayOutSpawnPosition) {
            PacketPlayOutSpawnPosition packet6 = (PacketPlayOutSpawnPosition) packet;
            this.player.compassTarget = new Location(this.getPlayer().getWorld(), packet6.pos.getX(), packet6.pos.getY(), packet6.pos.getZ());
        }
        // CraftBukkit end

        try {
            this.connection.sendPacket(packet, genericfuturelistener);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Sending packet");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Packet being sent");

            crashreportsystemdetails.a("Packet class", () -> {
                return packet.getClass().getCanonicalName();
            });
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void a(PacketPlayInHeldItemSlot packetplayinhelditemslot) {
        PlayerConnectionUtils.ensureMainThread(packetplayinhelditemslot, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        if (packetplayinhelditemslot.b() >= 0 && packetplayinhelditemslot.b() < PlayerInventory.getHotbarSize()) {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer(), this.player.getInventory().selected, packetplayinhelditemslot.b());
            this.cserver.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.sendPacket(new PacketPlayOutHeldItemSlot(this.player.getInventory().selected));
                this.player.resetIdleTimer();
                return;
            }
            // CraftBukkit end
            if (this.player.getInventory().selected != packetplayinhelditemslot.b() && this.player.getRaisedHand() == EnumHand.MAIN_HAND) {
                this.player.clearActiveItem();
            }

            this.player.getInventory().selected = packetplayinhelditemslot.b();
            this.player.resetIdleTimer();
        } else {
            PlayerConnection.LOGGER.warn("{} tried to set an invalid carried item", this.player.getDisplayName().getString());
            this.disconnect("Invalid hotbar selection (Hacking?)"); // CraftBukkit
        }
    }

    @Override
    public void a(PacketPlayInChat packetplayinchat) {
        // CraftBukkit start - async chat
        // SPIGOT-3638
        if (this.server.isStopped()) {
            return;
        }
        // CraftBukkit end
        String s = StringUtils.normalizeSpace(packetplayinchat.b());

        for (int i = 0; i < s.length(); ++i) {
            if (!SharedConstants.isAllowedChatCharacter(s.charAt(i))) {
                this.disconnect(new ChatMessage("multiplayer.disconnect.illegal_characters"));
                return;
            }
        }

        if (s.startsWith("/")) {
            PlayerConnectionUtils.ensureMainThread(packetplayinchat, this, this.player.getWorldServer());
            this.a(ITextFilter.a.a(s));
        } else {
            this.a(ITextFilter.a.a(s)); // CraftBukkit - filter NYI
        }

    }

    private void a(ITextFilter.a itextfilter_a) {
        if (this.player.isRemoved() || this.player.getChatFlags() == EnumChatVisibility.HIDDEN) { // CraftBukkit - dead men tell no tales
            this.sendPacket(new PacketPlayOutChat((new ChatMessage("chat.disabled.options")).a(EnumChatFormat.RED), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID));
        } else {
            this.player.resetIdleTimer();
            String s = itextfilter_a.a();

            // CraftBukkit start
            boolean isSync = s.startsWith("/");
            if (isSync) {
                try {
                    this.server.server.playerCommandState = true;
                    this.handleCommand(s);
                } finally {
                    this.server.server.playerCommandState = false;
                }
            } else if (s.isEmpty()) {
                LOGGER.warn(this.player.getName() + " tried to send an empty message");
            } else if (getPlayer().isConversing()) {
                final String conversationInput = s;
                this.server.processQueue.add(new Runnable() {
                    @Override
                    public void run() {
                        getPlayer().acceptConversationInput(conversationInput);
                    }
                });
            } else if (this.player.getChatFlags() == EnumChatVisibility.SYSTEM) { // Re-add "Command Only" flag check
                this.sendPacket(new PacketPlayOutChat((new ChatMessage("chat.cannotSend")).a(EnumChatFormat.RED), ChatMessageType.SYSTEM, SystemUtils.NIL_UUID));
            } else if (true) {
                this.chat(s, true);
                // CraftBukkit end - the below is for reference. :)
            } else {
                String s1 = itextfilter_a.b();
                ChatMessage chatmessage = s1.isEmpty() ? null : new ChatMessage("chat.type.text", new Object[]{this.player.getScoreboardDisplayName(), s1});
                ChatMessage chatmessage1 = new ChatMessage("chat.type.text", new Object[]{this.player.getScoreboardDisplayName(), s});

                this.server.getPlayerList().a(chatmessage1, (entityplayer) -> {
                    return this.player.b(entityplayer) ? chatmessage : chatmessage1;
                }, ChatMessageType.CHAT, this.player.getUniqueID());
            }

            // Spigot start - spam exclusions
            boolean counted = true;
            for ( String exclude : org.spigotmc.SpigotConfig.spamExclusions )
            {
                if ( exclude != null && s.startsWith( exclude ) )
                {
                    counted = false;
                    break;
                }
            }
            // Spigot end
            // CraftBukkit start - replaced with thread safe throttle
            // this.chatThrottle += 20;
            if (counted && chatSpamTickCount.addAndGet(20) > 200 && !this.server.getPlayerList().isOp(this.player.getProfile())) { // Spigot
                if (!isSync) {
                    Waitable waitable = new Waitable() {
                        @Override
                        protected Object evaluate() {
                            PlayerConnection.this.disconnect(new ChatMessage("disconnect.spam"));
                            return null;
                        }
                    };

                    this.server.processQueue.add(waitable);

                    try {
                        waitable.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    this.disconnect(new ChatMessage("disconnect.spam"));
                }
                // CraftBukkit end
            }

        }
    }

    // CraftBukkit start - add method
    public void chat(String s, boolean async) {
        if (s.isEmpty() || this.player.getChatFlags() == EnumChatVisibility.HIDDEN) {
            return;
        }

        if (!async && s.startsWith("/")) {
            this.handleCommand(s);
        } else if (this.player.getChatFlags() == EnumChatVisibility.SYSTEM) {
            // Do nothing, this is coming from a plugin
        } else {
            Player player = this.getPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet(server));
            this.cserver.getPluginManager().callEvent(event);

            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                // Evil plugins still listening to deprecated event
                final PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                Waitable waitable = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        org.bukkit.Bukkit.getPluginManager().callEvent(queueEvent);

                        if (queueEvent.isCancelled()) {
                            return null;
                        }

                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        PlayerConnection.this.server.console.sendMessage(message);
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            for (EntityPlayer recipient : server.getPlayerList().players) {
                                recipient.getBukkitEntity().sendMessage(PlayerConnection.this.player.getUniqueID(), message);
                            }
                        } else {
                            for (Player player : queueEvent.getRecipients()) {
                                player.sendMessage(PlayerConnection.this.player.getUniqueID(), message);
                            }
                        }
                        return null;
                    }};
                if (async) {
                    server.processQueue.add(waitable);
                } else {
                    waitable.run();
                }
                try {
                    waitable.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception processing chat event", e.getCause());
                }
            } else {
                if (event.isCancelled()) {
                    return;
                }

                s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                server.console.sendMessage(s);
                if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                    for (EntityPlayer recipient : server.getPlayerList().players) {
                        recipient.getBukkitEntity().sendMessage(PlayerConnection.this.player.getUniqueID(), s);
                    }
                } else {
                    for (Player recipient : event.getRecipients()) {
                        recipient.sendMessage(PlayerConnection.this.player.getUniqueID(), s);
                    }
                }
            }
        }
    }
    // CraftBukkit end

    private void handleCommand(String s) {
        org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.startTiming(); // Spigot
        // CraftBukkit start - whole method
        if ( org.spigotmc.SpigotConfig.logCommands ) // Spigot
        this.LOGGER.info(this.player.getName() + " issued server command: " + s);

        CraftPlayer player = this.getPlayer();

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, s, new LazyPlayerSet(server));
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
            return;
        }

        try {
            if (this.cserver.dispatchCommand(event.getPlayer(), event.getMessage().substring(1))) {
                return;
            }
        } catch (org.bukkit.command.CommandException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(PlayerConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return;
        } finally {
            org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
        }
        // this.minecraftServer.getCommandDispatcher().a(this.player.getCommandListener(), s);
        // CraftBukkit end
    }

    @Override
    public void a(PacketPlayInArmAnimation packetplayinarmanimation) {
        PlayerConnectionUtils.ensureMainThread(packetplayinarmanimation, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        this.player.resetIdleTimer();
        // CraftBukkit start - Raytrace to look for 'rogue armswings'
        float f1 = this.player.getXRot();
        float f2 = this.player.getYRot();
        double d0 = this.player.locX();
        double d1 = this.player.locY() + (double) this.player.getHeadHeight();
        double d2 = this.player.locZ();
        Vec3D vec3d = new Vec3D(d0, d1, d2);

        float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = player.gameMode.getGameMode()== EnumGamemode.CREATIVE ? 5.0D : 4.5D;
        Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        MovingObjectPosition movingobjectposition = this.player.level.rayTrace(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.OUTLINE, RayTrace.FluidCollisionOption.NONE, player));

        if (movingobjectposition == null || movingobjectposition.getType() != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.getInventory().getItemInHand(), EnumHand.MAIN_HAND);
        }

        // Arm swing animation
        PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayer());
        this.cserver.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        // CraftBukkit end
        this.player.swingHand(packetplayinarmanimation.b());
    }

    @Override
    public void a(PacketPlayInEntityAction packetplayinentityaction) {
        PlayerConnectionUtils.ensureMainThread(packetplayinentityaction, this, this.player.getWorldServer());
        // CraftBukkit start
        if (this.player.isRemoved()) return;
        switch (packetplayinentityaction.c()) {
            case PRESS_SHIFT_KEY:
            case RELEASE_SHIFT_KEY:
                PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayer(), packetplayinentityaction.c() == PacketPlayInEntityAction.EnumPlayerAction.PRESS_SHIFT_KEY);
                this.cserver.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
                break;
            case START_SPRINTING:
            case STOP_SPRINTING:
                PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getPlayer(), packetplayinentityaction.c() == PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING);
                this.cserver.getPluginManager().callEvent(e2);

                if (e2.isCancelled()) {
                    return;
                }
                break;
        }
        // CraftBukkit end
        this.player.resetIdleTimer();
        IJumpable ijumpable;

        switch (packetplayinentityaction.c()) {
            case PRESS_SHIFT_KEY:
                this.player.setSneaking(true);
                break;
            case RELEASE_SHIFT_KEY:
                this.player.setSneaking(false);
                break;
            case START_SPRINTING:
                this.player.setSprinting(true);
                break;
            case STOP_SPRINTING:
                this.player.setSprinting(false);
                break;
            case STOP_SLEEPING:
                if (this.player.isSleeping()) {
                    this.player.wakeup(false, true);
                    this.awaitingPositionFromClient = this.player.getPositionVector();
                }
                break;
            case START_RIDING_JUMP:
                if (this.player.getVehicle() instanceof IJumpable) {
                    ijumpable = (IJumpable) this.player.getVehicle();
                    int i = packetplayinentityaction.d();

                    if (ijumpable.a() && i > 0) {
                        ijumpable.b(i);
                    }
                }
                break;
            case STOP_RIDING_JUMP:
                if (this.player.getVehicle() instanceof IJumpable) {
                    ijumpable = (IJumpable) this.player.getVehicle();
                    ijumpable.b();
                }
                break;
            case OPEN_INVENTORY:
                if (this.player.getVehicle() instanceof EntityHorseAbstract) {
                    ((EntityHorseAbstract) this.player.getVehicle()).f((EntityHuman) this.player);
                }
                break;
            case START_FALL_FLYING:
                if (!this.player.fo()) {
                    this.player.stopGliding();
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid client command!");
        }

    }

    @Override
    public void a(PacketPlayInUseEntity packetplayinuseentity) {
        PlayerConnectionUtils.ensureMainThread(packetplayinuseentity, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        WorldServer worldserver = this.player.getWorldServer();
        final Entity entity = packetplayinuseentity.a(worldserver);
        // Spigot Start
        if ( entity == player && !player.isSpectator() )
        {
            disconnect( "Cannot interact with self!" );
            return;
        }
        // Spigot End

        this.player.resetIdleTimer();
        this.player.setSneaking(packetplayinuseentity.b());
        if (entity != null) {
            double d0 = 36.0D;

            if (this.player.f(entity) < 36.0D) {
                packetplayinuseentity.a(new PacketPlayInUseEntity.c() {
                    private void a(EnumHand enumhand, PlayerConnection.a playerconnection_a, PlayerInteractEntityEvent event) { // CraftBukkit
                        ItemStack itemstack = PlayerConnection.this.player.b(enumhand).cloneItemStack();
                        // CraftBukkit start
                        ItemStack itemInHand = PlayerConnection.this.player.b(enumhand);
                        boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof EntityInsentient;
                        Item origItem = player.getInventory().getItemInHand() == null ? null : player.getInventory().getItemInHand().getItem();

                        cserver.getPluginManager().callEvent(event);

                        // Fish bucket - SPIGOT-4048
                        if ((entity instanceof EntityFish && origItem != null && origItem.getItem() == Items.WATER_BUCKET) && (event.isCancelled() || player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getItem() != origItem)) {
                            sendPacket(new PacketPlayOutSpawnEntityLiving((EntityFish) entity));
                            player.containerMenu.updateInventory();
                        }

                        if (triggerLeashUpdate && (event.isCancelled() || player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getItem() != origItem)) {
                            // Refresh the current leash state
                            sendPacket(new PacketPlayOutAttachEntity(entity, ((EntityInsentient) entity).getLeashHolder()));
                        }

                        if (event.isCancelled() || player.getInventory().getItemInHand() == null || player.getInventory().getItemInHand().getItem() != origItem) {
                            // Refresh the current entity metadata
                            sendPacket(new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), true));
                        }

                        if (event.isCancelled()) {
                            return;
                        }
                        // CraftBukkit end

                        EnumInteractionResult enuminteractionresult = playerconnection_a.run(PlayerConnection.this.player, entity, enumhand);

                        // CraftBukkit start
                        if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                             player.containerMenu.updateInventory();
                        }
                        // CraftBukkit end

                        if (enuminteractionresult.a()) {
                            CriterionTriggers.PLAYER_INTERACTED_WITH_ENTITY.a(PlayerConnection.this.player, itemstack, entity);
                            if (enuminteractionresult.b()) {
                                PlayerConnection.this.player.swingHand(enumhand, true);
                            }
                        }

                    }

                    @Override
                    public void a(EnumHand enumhand) {
                        this.a(enumhand, EntityHuman::a, new PlayerInteractEntityEvent((Player) getPlayer(), entity.getBukkitEntity(), (enumhand == EnumHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND));
                    }

                    @Override
                    public void a(EnumHand enumhand, Vec3D vec3d) {
                        this.a(enumhand, (entityplayer, entity1, enumhand1) -> {
                            return entity1.a((EntityHuman) entityplayer, vec3d, enumhand1);
                        }, new PlayerInteractAtEntityEvent((Player) getPlayer(), entity.getBukkitEntity(), new org.bukkit.util.Vector(vec3d.x, vec3d.y, vec3d.z), (enumhand == EnumHand.OFF_HAND) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND)); // CraftBukkit
                    }

                    @Override
                    public void a() {
                        // CraftBukkit start
                        if (!(entity instanceof EntityItem) && !(entity instanceof EntityExperienceOrb) && !(entity instanceof EntityArrow) && (entity != PlayerConnection.this.player || player.isSpectator())) {
                            ItemStack itemInHand = PlayerConnection.this.player.getItemInMainHand();
                            PlayerConnection.this.player.attack(entity);

                            if (!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
                                player.containerMenu.updateInventory();
                            }
                            // CraftBukkit end
                        } else {
                            PlayerConnection.this.disconnect(new ChatMessage("multiplayer.disconnect.invalid_entity_attacked"));
                            PlayerConnection.LOGGER.warn("Player {} tried to attack an invalid entity", PlayerConnection.this.player.getDisplayName().getString());
                        }
                    }
                });
            }
        }

    }

    @Override
    public void a(PacketPlayInClientCommand packetplayinclientcommand) {
        PlayerConnectionUtils.ensureMainThread(packetplayinclientcommand, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        PacketPlayInClientCommand.EnumClientCommand packetplayinclientcommand_enumclientcommand = packetplayinclientcommand.b();

        switch (packetplayinclientcommand_enumclientcommand) {
            case PERFORM_RESPAWN:
                if (this.player.wonGame) {
                    this.player.wonGame = false;
                    this.player = this.server.getPlayerList().moveToWorld(this.player, true);
                    CriterionTriggers.CHANGED_DIMENSION.a(this.player, World.END, World.OVERWORLD);
                } else {
                    if (this.player.getHealth() > 0.0F) {
                        return;
                    }

                    this.player = this.server.getPlayerList().moveToWorld(this.player, false);
                    if (this.server.isHardcore()) {
                        this.player.a(EnumGamemode.SPECTATOR);
                        ((GameRules.GameRuleBoolean) this.player.getWorldServer().getGameRules().get(GameRules.RULE_SPECTATORSGENERATECHUNKS)).a(false, this.server);
                    }
                }
                break;
            case REQUEST_STATS:
                this.player.getStatisticManager().a(this.player);
        }

    }

    @Override
    public void a(PacketPlayInCloseWindow packetplayinclosewindow) {
        PlayerConnectionUtils.ensureMainThread(packetplayinclosewindow, this, this.player.getWorldServer());

        if (this.player.isFrozen()) return; // CraftBukkit
        CraftEventFactory.handleInventoryCloseEvent(this.player); // CraftBukkit

        this.player.o();
    }

    @Override
    public void a(PacketPlayInWindowClick packetplayinwindowclick) {
        PlayerConnectionUtils.ensureMainThread(packetplayinwindowclick, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        this.player.resetIdleTimer();
        if (this.player.containerMenu.containerId == packetplayinwindowclick.b() && this.player.containerMenu.canUse(this.player)) { // CraftBukkit
            boolean cancelled = this.player.isSpectator(); // CraftBukkit - see below if
            if (false/*this.player.isSpectator()*/) { // CraftBukkit
                this.player.containerMenu.updateInventory();
            } else {
                boolean flag = packetplayinwindowclick.h() != this.player.containerMenu.getStateId();

                this.player.containerMenu.h();
                // CraftBukkit start - Call InventoryClickEvent
                if (packetplayinwindowclick.c() < -1 && packetplayinwindowclick.c() != -999) {
                    return;
                }

                InventoryView inventory = this.player.containerMenu.getBukkitView();
                SlotType type = inventory.getSlotType(packetplayinwindowclick.c());

                InventoryClickEvent event;
                ClickType click = ClickType.UNKNOWN;
                InventoryAction action = InventoryAction.UNKNOWN;

                ItemStack itemstack = ItemStack.EMPTY;

                switch (packetplayinwindowclick.g()) {
                    case PICKUP:
                        if (packetplayinwindowclick.d() == 0) {
                            click = ClickType.LEFT;
                        } else if (packetplayinwindowclick.d() == 1) {
                            click = ClickType.RIGHT;
                        }
                        if (packetplayinwindowclick.d() == 0 || packetplayinwindowclick.d() == 1) {
                            action = InventoryAction.NOTHING; // Don't want to repeat ourselves
                            if (packetplayinwindowclick.c() == -999) {
                                if (!player.containerMenu.getCarried().isEmpty()) {
                                    action = packetplayinwindowclick.d() == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                                }
                            } else if (packetplayinwindowclick.c() < 0)  {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                                if (slot != null) {
                                    ItemStack clickedItem = slot.getItem();
                                    ItemStack cursor = player.containerMenu.getCarried();
                                    if (clickedItem.isEmpty()) {
                                        if (!cursor.isEmpty()) {
                                            action = packetplayinwindowclick.d() == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                        }
                                    } else if (slot.isAllowed(player)) {
                                        if (cursor.isEmpty()) {
                                            action = packetplayinwindowclick.d() == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                        } else if (slot.isAllowed(cursor)) {
                                            if (clickedItem.doMaterialsMatch(cursor) && ItemStack.equals(clickedItem, cursor)) {
                                                int toPlace = packetplayinwindowclick.d() == 0 ? cursor.getCount() : 1;
                                                toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.getCount());
                                                toPlace = Math.min(toPlace, slot.container.getMaxStackSize() - clickedItem.getCount());
                                                if (toPlace == 1) {
                                                    action = InventoryAction.PLACE_ONE;
                                                } else if (toPlace == cursor.getCount()) {
                                                    action = InventoryAction.PLACE_ALL;
                                                } else if (toPlace < 0) {
                                                    action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE; // this happens with oversized stacks
                                                } else if (toPlace != 0) {
                                                    action = InventoryAction.PLACE_SOME;
                                                }
                                            } else if (cursor.getCount() <= slot.getMaxStackSize()) {
                                                action = InventoryAction.SWAP_WITH_CURSOR;
                                            }
                                        } else if (cursor.getItem() == clickedItem.getItem() && ItemStack.equals(cursor, clickedItem)) {
                                            if (clickedItem.getCount() >= 0) {
                                                if (clickedItem.getCount() + cursor.getCount() <= cursor.getMaxStackSize()) {
                                                    // As of 1.5, this is result slots only
                                                    action = InventoryAction.PICKUP_ALL;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    // TODO check on updates
                    case QUICK_MOVE:
                        if (packetplayinwindowclick.d() == 0) {
                            click = ClickType.SHIFT_LEFT;
                        } else if (packetplayinwindowclick.d() == 1) {
                            click = ClickType.SHIFT_RIGHT;
                        }
                        if (packetplayinwindowclick.d() == 0 || packetplayinwindowclick.d() == 1) {
                            if (packetplayinwindowclick.c() < 0) {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                                if (slot != null && slot.isAllowed(this.player) && slot.hasItem()) {
                                    action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        }
                        break;
                    case SWAP:
                        if ((packetplayinwindowclick.d() >= 0 && packetplayinwindowclick.d() < 9) || packetplayinwindowclick.d() == 40) {
                            click = (packetplayinwindowclick.d() == 40) ? ClickType.SWAP_OFFHAND : ClickType.NUMBER_KEY;
                            Slot clickedSlot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                            if (clickedSlot.isAllowed(player)) {
                                ItemStack hotbar = this.player.getInventory().getItem(packetplayinwindowclick.d());
                                boolean canCleanSwap = hotbar.isEmpty() || (clickedSlot.container == player.getInventory() && clickedSlot.isAllowed(hotbar)); // the slot will accept the hotbar item
                                if (clickedSlot.hasItem()) {
                                    if (canCleanSwap) {
                                        action = InventoryAction.HOTBAR_SWAP;
                                    } else {
                                        action = InventoryAction.HOTBAR_MOVE_AND_READD;
                                    }
                                } else if (!clickedSlot.hasItem() && !hotbar.isEmpty() && clickedSlot.isAllowed(hotbar)) {
                                    action = InventoryAction.HOTBAR_SWAP;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        }
                        break;
                    case CLONE:
                        if (packetplayinwindowclick.d() == 2) {
                            click = ClickType.MIDDLE;
                            if (packetplayinwindowclick.c() < 0) {
                                action = InventoryAction.NOTHING;
                            } else {
                                Slot slot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                                if (slot != null && slot.hasItem() && player.getAbilities().instabuild && player.containerMenu.getCarried().isEmpty()) {
                                    action = InventoryAction.CLONE_STACK;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        } else {
                            click = ClickType.UNKNOWN;
                            action = InventoryAction.UNKNOWN;
                        }
                        break;
                    case THROW:
                        if (packetplayinwindowclick.c() >= 0) {
                            if (packetplayinwindowclick.d() == 0) {
                                click = ClickType.DROP;
                                Slot slot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                                if (slot != null && slot.hasItem() && slot.isAllowed(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
                                    action = InventoryAction.DROP_ONE_SLOT;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            } else if (packetplayinwindowclick.d() == 1) {
                                click = ClickType.CONTROL_DROP;
                                Slot slot = this.player.containerMenu.getSlot(packetplayinwindowclick.c());
                                if (slot != null && slot.hasItem() && slot.isAllowed(player) && !slot.getItem().isEmpty() && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
                                    action = InventoryAction.DROP_ALL_SLOT;
                                } else {
                                    action = InventoryAction.NOTHING;
                                }
                            }
                        } else {
                            // Sane default (because this happens when they are holding nothing. Don't ask why.)
                            click = ClickType.LEFT;
                            if (packetplayinwindowclick.d() == 1) {
                                click = ClickType.RIGHT;
                            }
                            action = InventoryAction.NOTHING;
                        }
                        break;
                    case QUICK_CRAFT:
                        this.player.containerMenu.a(packetplayinwindowclick.c(), packetplayinwindowclick.d(), packetplayinwindowclick.g(), this.player);
                        break;
                    case PICKUP_ALL:
                        click = ClickType.DOUBLE_CLICK;
                        action = InventoryAction.NOTHING;
                        if (packetplayinwindowclick.c() >= 0 && !this.player.containerMenu.getCarried().isEmpty()) {
                            ItemStack cursor = this.player.containerMenu.getCarried();
                            action = InventoryAction.NOTHING;
                            // Quick check for if we have any of the item
                            if (inventory.getTopInventory().contains(CraftMagicNumbers.getMaterial(cursor.getItem())) || inventory.getBottomInventory().contains(CraftMagicNumbers.getMaterial(cursor.getItem()))) {
                                action = InventoryAction.COLLECT_TO_CURSOR;
                            }
                        }
                        break;
                    default:
                        break;
                }

                if (packetplayinwindowclick.g() != InventoryClickType.QUICK_CRAFT) {
                    if (click == ClickType.NUMBER_KEY) {
                        event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.c(), click, action, packetplayinwindowclick.d());
                    } else {
                        event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.c(), click, action);
                    }

                    org.bukkit.inventory.Inventory top = inventory.getTopInventory();
                    if (packetplayinwindowclick.c() == 0 && top instanceof CraftingInventory) {
                        org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();
                        if (recipe != null) {
                            if (click == ClickType.NUMBER_KEY) {
                                event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.c(), click, action, packetplayinwindowclick.d());
                            } else {
                                event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.c(), click, action);
                            }
                        }
                    }

                    if (packetplayinwindowclick.c() == 2 && top instanceof SmithingInventory) {
                        org.bukkit.inventory.ItemStack result = ((SmithingInventory) top).getResult();
                        if (result != null) {
                            if (click == ClickType.NUMBER_KEY) {
                                event = new SmithItemEvent(inventory, type, packetplayinwindowclick.c(), click, action, packetplayinwindowclick.d());
                            } else {
                                event = new SmithItemEvent(inventory, type, packetplayinwindowclick.c(), click, action);
                            }
                        }
                    }

                    event.setCancelled(cancelled);
                    Container oldContainer = this.player.containerMenu; // SPIGOT-1224
                    cserver.getPluginManager().callEvent(event);
                    if (this.player.containerMenu != oldContainer) {
                        return;
                    }

                    switch (event.getResult()) {
                        case ALLOW:
                        case DEFAULT:
                            this.player.containerMenu.a(packetplayinwindowclick.c(), packetplayinwindowclick.d(), packetplayinwindowclick.g(), this.player);
                            break;
                        case DENY:
                            /* Needs enum constructor in InventoryAction
                            if (action.modifiesOtherSlots()) {

                            } else {
                                if (action.modifiesCursor()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(-1, -1, this.player.inventory.getCarried()));
                                }
                                if (action.modifiesClicked()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(this.player.activeContainer.windowId, packet102windowclick.slot, this.player.activeContainer.getSlot(packet102windowclick.slot).getItem()));
                                }
                            }*/
                            switch (action) {
                                // Modified other slots
                                case PICKUP_ALL:
                                case MOVE_TO_OTHER_INVENTORY:
                                case HOTBAR_MOVE_AND_READD:
                                case HOTBAR_SWAP:
                                case COLLECT_TO_CURSOR:
                                case UNKNOWN:
                                    this.player.containerMenu.updateInventory();
                                    break;
                                // Modified cursor and clicked
                                case PICKUP_SOME:
                                case PICKUP_HALF:
                                case PICKUP_ONE:
                                case PLACE_ALL:
                                case PLACE_SOME:
                                case PLACE_ONE:
                                case SWAP_WITH_CURSOR:
                                    this.player.connection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                    this.player.connection.sendPacket(new PacketPlayOutSetSlot(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packetplayinwindowclick.c(), this.player.containerMenu.getSlot(packetplayinwindowclick.c()).getItem()));
                                    break;
                                // Modified clicked only
                                case DROP_ALL_SLOT:
                                case DROP_ONE_SLOT:
                                    this.player.connection.sendPacket(new PacketPlayOutSetSlot(this.player.containerMenu.containerId, this.player.inventoryMenu.incrementStateId(), packetplayinwindowclick.c(), this.player.containerMenu.getSlot(packetplayinwindowclick.c()).getItem()));
                                    break;
                                // Modified cursor only
                                case DROP_ALL_CURSOR:
                                case DROP_ONE_CURSOR:
                                case CLONE_STACK:
                                    this.player.connection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventoryMenu.incrementStateId(), this.player.containerMenu.getCarried()));
                                    break;
                                // Nothing
                                case NOTHING:
                                    break;
                            }
                    }

                    if (event instanceof CraftItemEvent || event instanceof SmithItemEvent) {
                        // Need to update the inventory on crafting to
                        // correctly support custom recipes
                        player.containerMenu.updateInventory();
                    }
                }
                // CraftBukkit end
                ObjectIterator objectiterator = Int2ObjectMaps.fastIterable(packetplayinwindowclick.f()).iterator();

                while (objectiterator.hasNext()) {
                    Entry<ItemStack> entry = (Entry) objectiterator.next();

                    this.player.containerMenu.b(entry.getIntKey(), (ItemStack) entry.getValue());
                }

                this.player.containerMenu.a(packetplayinwindowclick.e());
                this.player.containerMenu.i();
                if (flag) {
                    this.player.containerMenu.e();
                } else {
                    this.player.containerMenu.d();
                }
            }
        }

    }

    @Override
    public void a(PacketPlayInAutoRecipe packetplayinautorecipe) {
        PlayerConnectionUtils.ensureMainThread(packetplayinautorecipe, this, this.player.getWorldServer());
        this.player.resetIdleTimer();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == packetplayinautorecipe.b() && this.player.containerMenu instanceof ContainerRecipeBook) {
            this.server.getCraftingManager().getRecipe(packetplayinautorecipe.c()).ifPresent((irecipe) -> {
                ((ContainerRecipeBook) this.player.containerMenu).a(packetplayinautorecipe.d(), irecipe, this.player);
            });
        }
    }

    @Override
    public void a(PacketPlayInEnchantItem packetplayinenchantitem) {
        PlayerConnectionUtils.ensureMainThread(packetplayinenchantitem, this, this.player.getWorldServer());
        if (this.player.isFrozen()) return; // CraftBukkit
        this.player.resetIdleTimer();
        if (this.player.containerMenu.containerId == packetplayinenchantitem.b() && !this.player.isSpectator()) {
            this.player.containerMenu.a((EntityHuman) this.player, packetplayinenchantitem.c());
            this.player.containerMenu.d();
        }

    }

    @Override
    public void a(PacketPlayInSetCreativeSlot packetplayinsetcreativeslot) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsetcreativeslot, this, this.player.getWorldServer());
        if (this.player.gameMode.isCreative()) {
            boolean flag = packetplayinsetcreativeslot.b() < 0;
            ItemStack itemstack = packetplayinsetcreativeslot.getItemStack();
            NBTTagCompound nbttagcompound = itemstack.b("BlockEntityTag");

            if (!itemstack.isEmpty() && nbttagcompound != null && nbttagcompound.hasKey("x") && nbttagcompound.hasKey("y") && nbttagcompound.hasKey("z") && this.player.getBukkitEntity().hasPermission("minecraft.nbt.copy")) { // Spigot
                BlockPosition blockposition = new BlockPosition(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
                TileEntity tileentity = this.player.level.getTileEntity(blockposition);

                if (tileentity != null) {
                    NBTTagCompound nbttagcompound1 = tileentity.save(new NBTTagCompound());

                    nbttagcompound1.remove("x");
                    nbttagcompound1.remove("y");
                    nbttagcompound1.remove("z");
                    itemstack.a("BlockEntityTag", (NBTBase) nbttagcompound1);
                }
            }

            boolean flag1 = packetplayinsetcreativeslot.b() >= 1 && packetplayinsetcreativeslot.b() <= 45;
            boolean flag2 = itemstack.isEmpty() || itemstack.getDamage() >= 0 && itemstack.getCount() <= 64 && !itemstack.isEmpty();
            if (flag || (flag1 && !ItemStack.matches(this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.b()).getItem(), packetplayinsetcreativeslot.getItemStack()))) { // Insist on valid slot
                // CraftBukkit start - Call click event
                InventoryView inventory = this.player.inventoryMenu.getBukkitView();
                org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.getItemStack());

                SlotType type = SlotType.QUICKBAR;
                if (flag) {
                    type = SlotType.OUTSIDE;
                } else if (packetplayinsetcreativeslot.b() < 36) {
                    if (packetplayinsetcreativeslot.b() >= 5 && packetplayinsetcreativeslot.b() < 9) {
                        type = SlotType.ARMOR;
                    } else {
                        type = SlotType.CONTAINER;
                    }
                }
                InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.b(), item);
                cserver.getPluginManager().callEvent(event);

                itemstack = CraftItemStack.asNMSCopy(event.getCursor());

                switch (event.getResult()) {
                case ALLOW:
                    // Plugin cleared the id / stacksize checks
                    flag2 = true;
                    break;
                case DEFAULT:
                    break;
                case DENY:
                    // Reset the slot
                    if (packetplayinsetcreativeslot.b() >= 0) {
                        this.player.connection.sendPacket(new PacketPlayOutSetSlot(this.player.inventoryMenu.containerId, this.player.inventoryMenu.incrementStateId(), packetplayinsetcreativeslot.b(), this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.b()).getItem()));
                        this.player.connection.sendPacket(new PacketPlayOutSetSlot(-1, this.player.inventoryMenu.incrementStateId(), -1, ItemStack.EMPTY));
                    }
                    return;
                }
            }
            // CraftBukkit end

            if (flag1 && flag2) {
                this.player.inventoryMenu.getSlot(packetplayinsetcreativeslot.b()).set(itemstack);
                this.player.inventoryMenu.d();
            } else if (flag && flag2 && this.dropSpamTickCount < 200) {
                this.dropSpamTickCount += 20;
                this.player.drop(itemstack, true);
            }
        }

    }

    @Override
    public void a(PacketPlayInUpdateSign packetplayinupdatesign) {
        List<String> list = (List) Stream.of(packetplayinupdatesign.c()).map(EnumChatFormat::a).collect(Collectors.toList());

        this.a(list, (list1) -> {
            this.a(packetplayinupdatesign, list1);
        });
    }

    private void a(PacketPlayInUpdateSign packetplayinupdatesign, List<ITextFilter.a> list) {
        if (this.player.isFrozen()) return; // CraftBukkit
        this.player.resetIdleTimer();
        WorldServer worldserver = this.player.getWorldServer();
        BlockPosition blockposition = packetplayinupdatesign.b();

        if (worldserver.isLoaded(blockposition)) {
            IBlockData iblockdata = worldserver.getType(blockposition);
            TileEntity tileentity = worldserver.getTileEntity(blockposition);

            if (!(tileentity instanceof TileEntitySign)) {
                return;
            }

            TileEntitySign tileentitysign = (TileEntitySign) tileentity;

            if (!tileentitysign.d() || !this.player.getUniqueID().equals(tileentitysign.f())) {
                PlayerConnection.LOGGER.warn("Player {} just tried to change non-editable sign", this.player.getDisplayName().getString());
                this.sendPacket(tileentity.getUpdatePacket()); // CraftBukkit
                return;
            }

            // CraftBukkit start
            Player player = this.player.getBukkitEntity();
            int x = packetplayinupdatesign.b().getX();
            int y = packetplayinupdatesign.b().getY();
            int z = packetplayinupdatesign.b().getZ();
            String[] lines = new String[4];

            for (int i = 0; i < list.size(); ++i) {
                ITextFilter.a itextfilter_a = (ITextFilter.a) list.get(i);

                if (this.player.R()) {
                    lines[i] = EnumChatFormat.a(new ChatComponentText(EnumChatFormat.a(itextfilter_a.b())).getString());
                } else {
                    lines[i] = EnumChatFormat.a(new ChatComponentText(EnumChatFormat.a(itextfilter_a.a())).getString());
                }
            }
            SignChangeEvent event = new SignChangeEvent((org.bukkit.craftbukkit.block.CraftBlock) player.getWorld().getBlockAt(x, y, z), this.player.getBukkitEntity(), lines);
            this.cserver.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                IChatBaseComponent[] components = org.bukkit.craftbukkit.block.CraftSign.sanitizeLines(event.getLines());
                for (int i = 0; i < components.length; i++) {
                    tileentitysign.a(i, components[i]);
                }
                tileentitysign.isEditable = false;
            }
            // CraftBukkit end

            tileentitysign.update();
            worldserver.notify(blockposition, iblockdata, iblockdata, 3);
        }

    }

    @Override
    public void a(PacketPlayInKeepAlive packetplayinkeepalive) {
        PlayerConnectionUtils.ensureMainThread(packetplayinkeepalive, this, this.player.getWorldServer()); // CraftBukkit
        if (this.keepAlivePending && packetplayinkeepalive.b() == this.keepAliveChallenge) {
            int i = (int) (SystemUtils.getMonotonicMillis() - this.keepAliveTime);

            this.player.latency = (this.player.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isExemptPlayer()) {
            this.disconnect(new ChatMessage("disconnect.timeout"));
        }

    }

    @Override
    public void a(PacketPlayInAbilities packetplayinabilities) {
        PlayerConnectionUtils.ensureMainThread(packetplayinabilities, this, this.player.getWorldServer());
        // CraftBukkit start
        if (this.player.getAbilities().mayfly && this.player.getAbilities().flying != packetplayinabilities.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.player.getBukkitEntity(), packetplayinabilities.isFlying());
            this.cserver.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.getAbilities().flying = packetplayinabilities.isFlying(); // Actually set the player's flying status
            } else {
                this.player.updateAbilities(); // Tell the player their ability was reverted
            }
        }
        // CraftBukkit end
    }

    @Override
    public void a(PacketPlayInSettings packetplayinsettings) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsettings, this, this.player.getWorldServer());
        this.player.a(packetplayinsettings);
    }

    // CraftBukkit start
    private static final MinecraftKey CUSTOM_REGISTER = new MinecraftKey("register");
    private static final MinecraftKey CUSTOM_UNREGISTER = new MinecraftKey("unregister");

    @Override
    public void a(PacketPlayInCustomPayload packetplayincustompayload) {
        PlayerConnectionUtils.ensureMainThread(packetplayincustompayload, this, this.player.getWorldServer());
        if (packetplayincustompayload.identifier.equals(CUSTOM_REGISTER)) {
            try {
                String channels = packetplayincustompayload.data.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    getPlayer().addChannel(channel);
                }
            } catch (Exception ex) {
                PlayerConnection.LOGGER.error("Couldn\'t register custom payload", ex);
                this.disconnect("Invalid payload REGISTER!");
            }
        } else if (packetplayincustompayload.identifier.equals(CUSTOM_UNREGISTER)) {
            try {
                String channels = packetplayincustompayload.data.toString(com.google.common.base.Charsets.UTF_8);
                for (String channel : channels.split("\0")) {
                    getPlayer().removeChannel(channel);
                }
            } catch (Exception ex) {
                PlayerConnection.LOGGER.error("Couldn\'t unregister custom payload", ex);
                this.disconnect("Invalid payload UNREGISTER!");
            }
        } else {
            try {
                byte[] data = new byte[packetplayincustompayload.data.readableBytes()];
                packetplayincustompayload.data.readBytes(data);
                cserver.getMessenger().dispatchIncomingMessage(player.getBukkitEntity(), packetplayincustompayload.identifier.toString(), data);
            } catch (Exception ex) {
                PlayerConnection.LOGGER.error("Couldn\'t dispatch custom payload", ex);
                this.disconnect("Invalid custom payload!");
            }
        }

    }

    public final boolean isDisconnected() {
        return !this.player.joining && !this.connection.isConnected();
    }
    // CraftBukkit end

    @Override
    public void a(PacketPlayInDifficultyChange packetplayindifficultychange) {
        PlayerConnectionUtils.ensureMainThread(packetplayindifficultychange, this, this.player.getWorldServer());
        if (this.player.l(2) || this.isExemptPlayer()) {
            this.server.a(packetplayindifficultychange.b(), false);
        }
    }

    @Override
    public void a(PacketPlayInDifficultyLock packetplayindifficultylock) {
        PlayerConnectionUtils.ensureMainThread(packetplayindifficultylock, this, this.player.getWorldServer());
        if (this.player.l(2) || this.isExemptPlayer()) {
            this.server.b(packetplayindifficultylock.b());
        }
    }

    @Override
    public EntityPlayer d() {
        return this.player;
    }

    @FunctionalInterface
    private interface a {

        EnumInteractionResult run(EntityPlayer entityplayer, Entity entity, EnumHand enumhand);
    }
}
