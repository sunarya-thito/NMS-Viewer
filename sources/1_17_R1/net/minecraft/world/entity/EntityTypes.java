package net.minecraft.world.entity;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.util.MathHelper;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityBee;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.entity.animal.EntityCod;
import net.minecraft.world.entity.animal.EntityCow;
import net.minecraft.world.entity.animal.EntityDolphin;
import net.minecraft.world.entity.animal.EntityFox;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntityMushroomCow;
import net.minecraft.world.entity.animal.EntityOcelot;
import net.minecraft.world.entity.animal.EntityPanda;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.entity.animal.EntityPig;
import net.minecraft.world.entity.animal.EntityPolarBear;
import net.minecraft.world.entity.animal.EntityPufferFish;
import net.minecraft.world.entity.animal.EntityRabbit;
import net.minecraft.world.entity.animal.EntitySalmon;
import net.minecraft.world.entity.animal.EntitySheep;
import net.minecraft.world.entity.animal.EntitySnowman;
import net.minecraft.world.entity.animal.EntitySquid;
import net.minecraft.world.entity.animal.EntityTropicalFish;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.animal.EntityWolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.EntityHorse;
import net.minecraft.world.entity.animal.horse.EntityHorseDonkey;
import net.minecraft.world.entity.animal.horse.EntityHorseMule;
import net.minecraft.world.entity.animal.horse.EntityHorseSkeleton;
import net.minecraft.world.entity.animal.horse.EntityHorseZombie;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.entity.animal.horse.EntityLlamaTrader;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.decoration.EntityLeash;
import net.minecraft.world.entity.decoration.EntityPainting;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.monster.EntityBlaze;
import net.minecraft.world.entity.monster.EntityCaveSpider;
import net.minecraft.world.entity.monster.EntityCreeper;
import net.minecraft.world.entity.monster.EntityDrowned;
import net.minecraft.world.entity.monster.EntityEnderman;
import net.minecraft.world.entity.monster.EntityEndermite;
import net.minecraft.world.entity.monster.EntityEvoker;
import net.minecraft.world.entity.monster.EntityGhast;
import net.minecraft.world.entity.monster.EntityGiantZombie;
import net.minecraft.world.entity.monster.EntityGuardian;
import net.minecraft.world.entity.monster.EntityGuardianElder;
import net.minecraft.world.entity.monster.EntityIllagerIllusioner;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import net.minecraft.world.entity.monster.EntityPhantom;
import net.minecraft.world.entity.monster.EntityPigZombie;
import net.minecraft.world.entity.monster.EntityPillager;
import net.minecraft.world.entity.monster.EntityRavager;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.monster.EntitySilverfish;
import net.minecraft.world.entity.monster.EntitySkeleton;
import net.minecraft.world.entity.monster.EntitySkeletonStray;
import net.minecraft.world.entity.monster.EntitySkeletonWither;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.entity.monster.EntitySpider;
import net.minecraft.world.entity.monster.EntityStrider;
import net.minecraft.world.entity.monster.EntityVex;
import net.minecraft.world.entity.monster.EntityVindicator;
import net.minecraft.world.entity.monster.EntityWitch;
import net.minecraft.world.entity.monster.EntityZoglin;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.monster.EntityZombieHusk;
import net.minecraft.world.entity.monster.EntityZombieVillager;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglinBrute;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.EntityVillagerTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityDragonFireball;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.entity.projectile.EntityEnderPearl;
import net.minecraft.world.entity.projectile.EntityEnderSignal;
import net.minecraft.world.entity.projectile.EntityEvokerFangs;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.entity.projectile.EntityLargeFireball;
import net.minecraft.world.entity.projectile.EntityLlamaSpit;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.entity.projectile.EntityShulkerBullet;
import net.minecraft.world.entity.projectile.EntitySmallFireball;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.entity.projectile.EntityThrownExpBottle;
import net.minecraft.world.entity.projectile.EntityThrownTrident;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.entity.projectile.EntityWitherSkull;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.entity.vehicle.EntityMinecartChest;
import net.minecraft.world.entity.vehicle.EntityMinecartCommandBlock;
import net.minecraft.world.entity.vehicle.EntityMinecartFurnace;
import net.minecraft.world.entity.vehicle.EntityMinecartHopper;
import net.minecraft.world.entity.vehicle.EntityMinecartMobSpawner;
import net.minecraft.world.entity.vehicle.EntityMinecartRideable;
import net.minecraft.world.entity.vehicle.EntityMinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityTypes<T extends Entity> implements EntityTypeTest<Entity, T> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String ENTITY_TAG = "EntityTag";
    private static final float MAGIC_HORSE_WIDTH = 1.3964844F;
    public static final EntityTypes<EntityAreaEffectCloud> AREA_EFFECT_CLOUD = a("area_effect_cloud", EntityTypes.Builder.a(EntityAreaEffectCloud::new, EnumCreatureType.MISC).c().a(6.0F, 0.5F).trackingRange(10).updateInterval(10)); // CraftBukkit - SPIGOT-3729: track area effect clouds
    public static final EntityTypes<EntityArmorStand> ARMOR_STAND = a("armor_stand", EntityTypes.Builder.a(EntityArmorStand::new, EnumCreatureType.MISC).a(0.5F, 1.975F).trackingRange(10));
    public static final EntityTypes<EntityTippedArrow> ARROW = a("arrow", EntityTypes.Builder.a(EntityTippedArrow::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<Axolotl> AXOLOTL = a("axolotl", EntityTypes.Builder.a(Axolotl::new, EnumCreatureType.UNDERGROUND_WATER_CREATURE).a(0.75F, 0.42F).trackingRange(10));
    public static final EntityTypes<EntityBat> BAT = a("bat", EntityTypes.Builder.a(EntityBat::new, EnumCreatureType.AMBIENT).a(0.5F, 0.9F).trackingRange(5));
    public static final EntityTypes<EntityBee> BEE = a("bee", EntityTypes.Builder.a(EntityBee::new, EnumCreatureType.CREATURE).a(0.7F, 0.6F).trackingRange(8));
    public static final EntityTypes<EntityBlaze> BLAZE = a("blaze", EntityTypes.Builder.a(EntityBlaze::new, EnumCreatureType.MONSTER).c().a(0.6F, 1.8F).trackingRange(8));
    public static final EntityTypes<EntityBoat> BOAT = a("boat", EntityTypes.Builder.a(EntityBoat::new, EnumCreatureType.MISC).a(1.375F, 0.5625F).trackingRange(10));
    public static final EntityTypes<EntityCat> CAT = a("cat", EntityTypes.Builder.a(EntityCat::new, EnumCreatureType.CREATURE).a(0.6F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityCaveSpider> CAVE_SPIDER = a("cave_spider", EntityTypes.Builder.a(EntityCaveSpider::new, EnumCreatureType.MONSTER).a(0.7F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityChicken> CHICKEN = a("chicken", EntityTypes.Builder.a(EntityChicken::new, EnumCreatureType.CREATURE).a(0.4F, 0.7F).trackingRange(10));
    public static final EntityTypes<EntityCod> COD = a("cod", EntityTypes.Builder.a(EntityCod::new, EnumCreatureType.WATER_AMBIENT).a(0.5F, 0.3F).trackingRange(4));
    public static final EntityTypes<EntityCow> COW = a("cow", EntityTypes.Builder.a(EntityCow::new, EnumCreatureType.CREATURE).a(0.9F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityCreeper> CREEPER = a("creeper", EntityTypes.Builder.a(EntityCreeper::new, EnumCreatureType.MONSTER).a(0.6F, 1.7F).trackingRange(8));
    public static final EntityTypes<EntityDolphin> DOLPHIN = a("dolphin", EntityTypes.Builder.a(EntityDolphin::new, EnumCreatureType.WATER_CREATURE).a(0.9F, 0.6F));
    public static final EntityTypes<EntityHorseDonkey> DONKEY = a("donkey", EntityTypes.Builder.a(EntityHorseDonkey::new, EnumCreatureType.CREATURE).a(1.3964844F, 1.5F).trackingRange(10));
    public static final EntityTypes<EntityDragonFireball> DRAGON_FIREBALL = a("dragon_fireball", EntityTypes.Builder.a(EntityDragonFireball::new, EnumCreatureType.MISC).a(1.0F, 1.0F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityDrowned> DROWNED = a("drowned", EntityTypes.Builder.a(EntityDrowned::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityGuardianElder> ELDER_GUARDIAN = a("elder_guardian", EntityTypes.Builder.a(EntityGuardianElder::new, EnumCreatureType.MONSTER).a(1.9975F, 1.9975F).trackingRange(10));
    public static final EntityTypes<EntityEnderCrystal> END_CRYSTAL = a("end_crystal", EntityTypes.Builder.a(EntityEnderCrystal::new, EnumCreatureType.MISC).a(2.0F, 2.0F).trackingRange(16).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityEnderDragon> ENDER_DRAGON = a("ender_dragon", EntityTypes.Builder.a(EntityEnderDragon::new, EnumCreatureType.MONSTER).c().a(16.0F, 8.0F).trackingRange(10));
    public static final EntityTypes<EntityEnderman> ENDERMAN = a("enderman", EntityTypes.Builder.a(EntityEnderman::new, EnumCreatureType.MONSTER).a(0.6F, 2.9F).trackingRange(8));
    public static final EntityTypes<EntityEndermite> ENDERMITE = a("endermite", EntityTypes.Builder.a(EntityEndermite::new, EnumCreatureType.MONSTER).a(0.4F, 0.3F).trackingRange(8));
    public static final EntityTypes<EntityEvoker> EVOKER = a("evoker", EntityTypes.Builder.a(EntityEvoker::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityEvokerFangs> EVOKER_FANGS = a("evoker_fangs", EntityTypes.Builder.a(EntityEvokerFangs::new, EnumCreatureType.MISC).a(0.5F, 0.8F).trackingRange(6).updateInterval(2));
    public static final EntityTypes<EntityExperienceOrb> EXPERIENCE_ORB = a("experience_orb", EntityTypes.Builder.a(EntityExperienceOrb::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(6).updateInterval(20));
    public static final EntityTypes<EntityEnderSignal> EYE_OF_ENDER = a("eye_of_ender", EntityTypes.Builder.a(EntityEnderSignal::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(4));
    public static final EntityTypes<EntityFallingBlock> FALLING_BLOCK = a("falling_block", EntityTypes.Builder.a(EntityFallingBlock::new, EnumCreatureType.MISC).a(0.98F, 0.98F).trackingRange(10).updateInterval(20));
    public static final EntityTypes<EntityFireworks> FIREWORK_ROCKET = a("firework_rocket", EntityTypes.Builder.a(EntityFireworks::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityFox> FOX = a("fox", EntityTypes.Builder.a(EntityFox::new, EnumCreatureType.CREATURE).a(0.6F, 0.7F).trackingRange(8).a(Blocks.SWEET_BERRY_BUSH));
    public static final EntityTypes<EntityGhast> GHAST = a("ghast", EntityTypes.Builder.a(EntityGhast::new, EnumCreatureType.MONSTER).c().a(4.0F, 4.0F).trackingRange(10));
    public static final EntityTypes<EntityGiantZombie> GIANT = a("giant", EntityTypes.Builder.a(EntityGiantZombie::new, EnumCreatureType.MONSTER).a(3.6F, 12.0F).trackingRange(10));
    public static final EntityTypes<GlowItemFrame> GLOW_ITEM_FRAME = a("glow_item_frame", EntityTypes.Builder.a(GlowItemFrame::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<GlowSquid> GLOW_SQUID = a("glow_squid", EntityTypes.Builder.a(GlowSquid::new, EnumCreatureType.UNDERGROUND_WATER_CREATURE).a(0.8F, 0.8F).trackingRange(10));
    public static final EntityTypes<Goat> GOAT = a("goat", EntityTypes.Builder.a(Goat::new, EnumCreatureType.CREATURE).a(0.9F, 1.3F).trackingRange(10));
    public static final EntityTypes<EntityGuardian> GUARDIAN = a("guardian", EntityTypes.Builder.a(EntityGuardian::new, EnumCreatureType.MONSTER).a(0.85F, 0.85F).trackingRange(8));
    public static final EntityTypes<EntityHoglin> HOGLIN = a("hoglin", EntityTypes.Builder.a(EntityHoglin::new, EnumCreatureType.MONSTER).a(1.3964844F, 1.4F).trackingRange(8));
    public static final EntityTypes<EntityHorse> HORSE = a("horse", EntityTypes.Builder.a(EntityHorse::new, EnumCreatureType.CREATURE).a(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntityZombieHusk> HUSK = a("husk", EntityTypes.Builder.a(EntityZombieHusk::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityIllagerIllusioner> ILLUSIONER = a("illusioner", EntityTypes.Builder.a(EntityIllagerIllusioner::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityIronGolem> IRON_GOLEM = a("iron_golem", EntityTypes.Builder.a(EntityIronGolem::new, EnumCreatureType.MISC).a(1.4F, 2.7F).trackingRange(10));
    public static final EntityTypes<EntityItem> ITEM = a("item", EntityTypes.Builder.a(EntityItem::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(6).updateInterval(20));
    public static final EntityTypes<EntityItemFrame> ITEM_FRAME = a("item_frame", EntityTypes.Builder.a(EntityItemFrame::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLargeFireball> FIREBALL = a("fireball", EntityTypes.Builder.a(EntityLargeFireball::new, EnumCreatureType.MISC).a(1.0F, 1.0F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityLeash> LEASH_KNOT = a("leash_knot", EntityTypes.Builder.a(EntityLeash::new, EnumCreatureType.MISC).b().a(0.375F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLightning> LIGHTNING_BOLT = a("lightning_bolt", EntityTypes.Builder.a(EntityLightning::new, EnumCreatureType.MISC).b().a(0.0F, 0.0F).trackingRange(16).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityLlama> LLAMA = a("llama", EntityTypes.Builder.a(EntityLlama::new, EnumCreatureType.CREATURE).a(0.9F, 1.87F).trackingRange(10));
    public static final EntityTypes<EntityLlamaSpit> LLAMA_SPIT = a("llama_spit", EntityTypes.Builder.a(EntityLlamaSpit::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityMagmaCube> MAGMA_CUBE = a("magma_cube", EntityTypes.Builder.a(EntityMagmaCube::new, EnumCreatureType.MONSTER).c().a(2.04F, 2.04F).trackingRange(8));
    public static final EntityTypes<Marker> MARKER = a("marker", EntityTypes.Builder.a(Marker::new, EnumCreatureType.MISC).a(0.0F, 0.0F).trackingRange(0));
    public static final EntityTypes<EntityMinecartRideable> MINECART = a("minecart", EntityTypes.Builder.a(EntityMinecartRideable::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartChest> CHEST_MINECART = a("chest_minecart", EntityTypes.Builder.a(EntityMinecartChest::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartCommandBlock> COMMAND_BLOCK_MINECART = a("command_block_minecart", EntityTypes.Builder.a(EntityMinecartCommandBlock::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartFurnace> FURNACE_MINECART = a("furnace_minecart", EntityTypes.Builder.a(EntityMinecartFurnace::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartHopper> HOPPER_MINECART = a("hopper_minecart", EntityTypes.Builder.a(EntityMinecartHopper::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartMobSpawner> SPAWNER_MINECART = a("spawner_minecart", EntityTypes.Builder.a(EntityMinecartMobSpawner::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityMinecartTNT> TNT_MINECART = a("tnt_minecart", EntityTypes.Builder.a(EntityMinecartTNT::new, EnumCreatureType.MISC).a(0.98F, 0.7F).trackingRange(8));
    public static final EntityTypes<EntityHorseMule> MULE = a("mule", EntityTypes.Builder.a(EntityHorseMule::new, EnumCreatureType.CREATURE).a(1.3964844F, 1.6F).trackingRange(8));
    public static final EntityTypes<EntityMushroomCow> MOOSHROOM = a("mooshroom", EntityTypes.Builder.a(EntityMushroomCow::new, EnumCreatureType.CREATURE).a(0.9F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityOcelot> OCELOT = a("ocelot", EntityTypes.Builder.a(EntityOcelot::new, EnumCreatureType.CREATURE).a(0.6F, 0.7F).trackingRange(10));
    public static final EntityTypes<EntityPainting> PAINTING = a("painting", EntityTypes.Builder.a(EntityPainting::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(10).updateInterval(Integer.MAX_VALUE));
    public static final EntityTypes<EntityPanda> PANDA = a("panda", EntityTypes.Builder.a(EntityPanda::new, EnumCreatureType.CREATURE).a(1.3F, 1.25F).trackingRange(10));
    public static final EntityTypes<EntityParrot> PARROT = a("parrot", EntityTypes.Builder.a(EntityParrot::new, EnumCreatureType.CREATURE).a(0.5F, 0.9F).trackingRange(8));
    public static final EntityTypes<EntityPhantom> PHANTOM = a("phantom", EntityTypes.Builder.a(EntityPhantom::new, EnumCreatureType.MONSTER).a(0.9F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityPig> PIG = a("pig", EntityTypes.Builder.a(EntityPig::new, EnumCreatureType.CREATURE).a(0.9F, 0.9F).trackingRange(10));
    public static final EntityTypes<EntityPiglin> PIGLIN = a("piglin", EntityTypes.Builder.a(EntityPiglin::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPiglinBrute> PIGLIN_BRUTE = a("piglin_brute", EntityTypes.Builder.a(EntityPiglinBrute::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPillager> PILLAGER = a("pillager", EntityTypes.Builder.a(EntityPillager::new, EnumCreatureType.MONSTER).d().a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPolarBear> POLAR_BEAR = a("polar_bear", EntityTypes.Builder.a(EntityPolarBear::new, EnumCreatureType.CREATURE).a(Blocks.POWDER_SNOW).a(1.4F, 1.4F).trackingRange(10));
    public static final EntityTypes<EntityTNTPrimed> TNT = a("tnt", EntityTypes.Builder.a(EntityTNTPrimed::new, EnumCreatureType.MISC).c().a(0.98F, 0.98F).trackingRange(10).updateInterval(10));
    public static final EntityTypes<EntityPufferFish> PUFFERFISH = a("pufferfish", EntityTypes.Builder.a(EntityPufferFish::new, EnumCreatureType.WATER_AMBIENT).a(0.7F, 0.7F).trackingRange(4));
    public static final EntityTypes<EntityRabbit> RABBIT = a("rabbit", EntityTypes.Builder.a(EntityRabbit::new, EnumCreatureType.CREATURE).a(0.4F, 0.5F).trackingRange(8));
    public static final EntityTypes<EntityRavager> RAVAGER = a("ravager", EntityTypes.Builder.a(EntityRavager::new, EnumCreatureType.MONSTER).a(1.95F, 2.2F).trackingRange(10));
    public static final EntityTypes<EntitySalmon> SALMON = a("salmon", EntityTypes.Builder.a(EntitySalmon::new, EnumCreatureType.WATER_AMBIENT).a(0.7F, 0.4F).trackingRange(4));
    public static final EntityTypes<EntitySheep> SHEEP = a("sheep", EntityTypes.Builder.a(EntitySheep::new, EnumCreatureType.CREATURE).a(0.9F, 1.3F).trackingRange(10));
    public static final EntityTypes<EntityShulker> SHULKER = a("shulker", EntityTypes.Builder.a(EntityShulker::new, EnumCreatureType.MONSTER).c().d().a(1.0F, 1.0F).trackingRange(10));
    public static final EntityTypes<EntityShulkerBullet> SHULKER_BULLET = a("shulker_bullet", EntityTypes.Builder.a(EntityShulkerBullet::new, EnumCreatureType.MISC).a(0.3125F, 0.3125F).trackingRange(8));
    public static final EntityTypes<EntitySilverfish> SILVERFISH = a("silverfish", EntityTypes.Builder.a(EntitySilverfish::new, EnumCreatureType.MONSTER).a(0.4F, 0.3F).trackingRange(8));
    public static final EntityTypes<EntitySkeleton> SKELETON = a("skeleton", EntityTypes.Builder.a(EntitySkeleton::new, EnumCreatureType.MONSTER).a(0.6F, 1.99F).trackingRange(8));
    public static final EntityTypes<EntityHorseSkeleton> SKELETON_HORSE = a("skeleton_horse", EntityTypes.Builder.a(EntityHorseSkeleton::new, EnumCreatureType.CREATURE).a(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntitySlime> SLIME = a("slime", EntityTypes.Builder.a(EntitySlime::new, EnumCreatureType.MONSTER).a(2.04F, 2.04F).trackingRange(10));
    public static final EntityTypes<EntitySmallFireball> SMALL_FIREBALL = a("small_fireball", EntityTypes.Builder.a(EntitySmallFireball::new, EnumCreatureType.MISC).a(0.3125F, 0.3125F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntitySnowman> SNOW_GOLEM = a("snow_golem", EntityTypes.Builder.a(EntitySnowman::new, EnumCreatureType.MISC).a(Blocks.POWDER_SNOW).a(0.7F, 1.9F).trackingRange(8));
    public static final EntityTypes<EntitySnowball> SNOWBALL = a("snowball", EntityTypes.Builder.a(EntitySnowball::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntitySpectralArrow> SPECTRAL_ARROW = a("spectral_arrow", EntityTypes.Builder.a(EntitySpectralArrow::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<EntitySpider> SPIDER = a("spider", EntityTypes.Builder.a(EntitySpider::new, EnumCreatureType.MONSTER).a(1.4F, 0.9F).trackingRange(8));
    public static final EntityTypes<EntitySquid> SQUID = a("squid", EntityTypes.Builder.a(EntitySquid::new, EnumCreatureType.WATER_CREATURE).a(0.8F, 0.8F).trackingRange(8));
    public static final EntityTypes<EntitySkeletonStray> STRAY = a("stray", EntityTypes.Builder.a(EntitySkeletonStray::new, EnumCreatureType.MONSTER).a(0.6F, 1.99F).a(Blocks.POWDER_SNOW).trackingRange(8));
    public static final EntityTypes<EntityStrider> STRIDER = a("strider", EntityTypes.Builder.a(EntityStrider::new, EnumCreatureType.CREATURE).c().a(0.9F, 1.7F).trackingRange(10));
    public static final EntityTypes<EntityEgg> EGG = a("egg", EntityTypes.Builder.a(EntityEgg::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityEnderPearl> ENDER_PEARL = a("ender_pearl", EntityTypes.Builder.a(EntityEnderPearl::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityThrownExpBottle> EXPERIENCE_BOTTLE = a("experience_bottle", EntityTypes.Builder.a(EntityThrownExpBottle::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityPotion> POTION = a("potion", EntityTypes.Builder.a(EntityPotion::new, EnumCreatureType.MISC).a(0.25F, 0.25F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityThrownTrident> TRIDENT = a("trident", EntityTypes.Builder.a(EntityThrownTrident::new, EnumCreatureType.MISC).a(0.5F, 0.5F).trackingRange(4).updateInterval(20));
    public static final EntityTypes<EntityLlamaTrader> TRADER_LLAMA = a("trader_llama", EntityTypes.Builder.a(EntityLlamaTrader::new, EnumCreatureType.CREATURE).a(0.9F, 1.87F).trackingRange(10));
    public static final EntityTypes<EntityTropicalFish> TROPICAL_FISH = a("tropical_fish", EntityTypes.Builder.a(EntityTropicalFish::new, EnumCreatureType.WATER_AMBIENT).a(0.5F, 0.4F).trackingRange(4));
    public static final EntityTypes<EntityTurtle> TURTLE = a("turtle", EntityTypes.Builder.a(EntityTurtle::new, EnumCreatureType.CREATURE).a(1.2F, 0.4F).trackingRange(10));
    public static final EntityTypes<EntityVex> VEX = a("vex", EntityTypes.Builder.a(EntityVex::new, EnumCreatureType.MONSTER).c().a(0.4F, 0.8F).trackingRange(8));
    public static final EntityTypes<EntityVillager> VILLAGER = a("villager", EntityTypes.Builder.a(EntityVillager::new, EnumCreatureType.MISC).a(0.6F, 1.95F).trackingRange(10));
    public static final EntityTypes<EntityVindicator> VINDICATOR = a("vindicator", EntityTypes.Builder.a(EntityVindicator::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityVillagerTrader> WANDERING_TRADER = a("wandering_trader", EntityTypes.Builder.a(EntityVillagerTrader::new, EnumCreatureType.CREATURE).a(0.6F, 1.95F).trackingRange(10));
    public static final EntityTypes<EntityWitch> WITCH = a("witch", EntityTypes.Builder.a(EntityWitch::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityWither> WITHER = a("wither", EntityTypes.Builder.a(EntityWither::new, EnumCreatureType.MONSTER).c().a(Blocks.WITHER_ROSE).a(0.9F, 3.5F).trackingRange(10));
    public static final EntityTypes<EntitySkeletonWither> WITHER_SKELETON = a("wither_skeleton", EntityTypes.Builder.a(EntitySkeletonWither::new, EnumCreatureType.MONSTER).c().a(Blocks.WITHER_ROSE).a(0.7F, 2.4F).trackingRange(8));
    public static final EntityTypes<EntityWitherSkull> WITHER_SKULL = a("wither_skull", EntityTypes.Builder.a(EntityWitherSkull::new, EnumCreatureType.MISC).a(0.3125F, 0.3125F).trackingRange(4).updateInterval(10));
    public static final EntityTypes<EntityWolf> WOLF = a("wolf", EntityTypes.Builder.a(EntityWolf::new, EnumCreatureType.CREATURE).a(0.6F, 0.85F).trackingRange(10));
    public static final EntityTypes<EntityZoglin> ZOGLIN = a("zoglin", EntityTypes.Builder.a(EntityZoglin::new, EnumCreatureType.MONSTER).c().a(1.3964844F, 1.4F).trackingRange(8));
    public static final EntityTypes<EntityZombie> ZOMBIE = a("zombie", EntityTypes.Builder.a(EntityZombie::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityHorseZombie> ZOMBIE_HORSE = a("zombie_horse", EntityTypes.Builder.a(EntityHorseZombie::new, EnumCreatureType.CREATURE).a(1.3964844F, 1.6F).trackingRange(10));
    public static final EntityTypes<EntityZombieVillager> ZOMBIE_VILLAGER = a("zombie_villager", EntityTypes.Builder.a(EntityZombieVillager::new, EnumCreatureType.MONSTER).a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityPigZombie> ZOMBIFIED_PIGLIN = a("zombified_piglin", EntityTypes.Builder.a(EntityPigZombie::new, EnumCreatureType.MONSTER).c().a(0.6F, 1.95F).trackingRange(8));
    public static final EntityTypes<EntityHuman> PLAYER = a("player", EntityTypes.Builder.a(EnumCreatureType.MISC).b().a().a(0.6F, 1.8F).trackingRange(32).updateInterval(2));
    public static final EntityTypes<EntityFishingHook> FISHING_BOBBER = a("fishing_bobber", EntityTypes.Builder.a(EntityFishingHook::new, EnumCreatureType.MISC).b().a().a(0.25F, 0.25F).trackingRange(4).updateInterval(5));
    private final EntityTypes.b<T> factory;
    private final EnumCreatureType category;
    private final ImmutableSet<Block> immuneTo;
    private final boolean serialize;
    private final boolean summon;
    private final boolean fireImmune;
    private final boolean canSpawnFarFromPlayer;
    private final int clientTrackingRange;
    private final int updateInterval;
    @Nullable
    private String descriptionId;
    @Nullable
    private IChatBaseComponent description;
    @Nullable
    private MinecraftKey lootTable;
    private final EntitySize dimensions;

    private static <T extends Entity> EntityTypes<T> a(String s, EntityTypes.Builder entitytypes_builder) { // CraftBukkit - decompile error
        return (EntityTypes) IRegistry.a((IRegistry) IRegistry.ENTITY_TYPE, s, (Object) entitytypes_builder.a(s));
    }

    public static MinecraftKey getName(EntityTypes<?> entitytypes) {
        return IRegistry.ENTITY_TYPE.getKey(entitytypes);
    }

    public static Optional<EntityTypes<?>> a(String s) {
        return IRegistry.ENTITY_TYPE.getOptional(MinecraftKey.a(s));
    }

    public EntityTypes(EntityTypes.b<T> entitytypes_b, EnumCreatureType enumcreaturetype, boolean flag, boolean flag1, boolean flag2, boolean flag3, ImmutableSet<Block> immutableset, EntitySize entitysize, int i, int j) {
        this.factory = entitytypes_b;
        this.category = enumcreaturetype;
        this.canSpawnFarFromPlayer = flag3;
        this.serialize = flag;
        this.summon = flag1;
        this.fireImmune = flag2;
        this.immuneTo = immutableset;
        this.dimensions = entitysize;
        this.clientTrackingRange = i;
        this.updateInterval = j;
    }

    @Nullable
    public Entity spawnCreature(WorldServer worldserver, @Nullable ItemStack itemstack, @Nullable EntityHuman entityhuman, BlockPosition blockposition, EnumMobSpawn enummobspawn, boolean flag, boolean flag1) {
        return this.spawnCreature(worldserver, itemstack == null ? null : itemstack.getTag(), itemstack != null && itemstack.hasName() ? itemstack.getName() : null, entityhuman, blockposition, enummobspawn, flag, flag1);
    }

    @Nullable
    public T spawnCreature(WorldServer worldserver, @Nullable NBTTagCompound nbttagcompound, @Nullable IChatBaseComponent ichatbasecomponent, @Nullable EntityHuman entityhuman, BlockPosition blockposition, EnumMobSpawn enummobspawn, boolean flag, boolean flag1) {
        // CraftBukkit start
        return this.spawnCreature(worldserver, nbttagcompound, ichatbasecomponent, entityhuman, blockposition, enummobspawn, flag, flag1, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
    }

    @Nullable
    public T spawnCreature(WorldServer worldserver, @Nullable NBTTagCompound nbttagcompound, @Nullable IChatBaseComponent ichatbasecomponent, @Nullable EntityHuman entityhuman, BlockPosition blockposition, EnumMobSpawn enummobspawn, boolean flag, boolean flag1, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        T t0 = this.createCreature(worldserver, nbttagcompound, ichatbasecomponent, entityhuman, blockposition, enummobspawn, flag, flag1);

        if (t0 != null) {
            worldserver.addAllEntities(t0, spawnReason);
            return !t0.isRemoved() ? t0 : null; // Don't return an entity when CreatureSpawnEvent is canceled
            // CraftBukkit end
        }

        return t0;
    }

    @Nullable
    public T createCreature(WorldServer worldserver, @Nullable NBTTagCompound nbttagcompound, @Nullable IChatBaseComponent ichatbasecomponent, @Nullable EntityHuman entityhuman, BlockPosition blockposition, EnumMobSpawn enummobspawn, boolean flag, boolean flag1) {
        T t0 = this.a((World) worldserver);

        if (t0 == null) {
            return null;
        } else {
            double d0;

            if (flag) {
                t0.setPosition((double) blockposition.getX() + 0.5D, (double) (blockposition.getY() + 1), (double) blockposition.getZ() + 0.5D);
                d0 = a(worldserver, blockposition, flag1, t0.getBoundingBox());
            } else {
                d0 = 0.0D;
            }

            t0.setPositionRotation((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + d0, (double) blockposition.getZ() + 0.5D, MathHelper.g(worldserver.random.nextFloat() * 360.0F), 0.0F);
            if (t0 instanceof EntityInsentient) {
                EntityInsentient entityinsentient = (EntityInsentient) t0;

                entityinsentient.yHeadRot = entityinsentient.getYRot();
                entityinsentient.yBodyRot = entityinsentient.getYRot();
                entityinsentient.prepare(worldserver, worldserver.getDamageScaler(entityinsentient.getChunkCoordinates()), enummobspawn, (GroupDataEntity) null, nbttagcompound);
                entityinsentient.K();
            }

            if (ichatbasecomponent != null && t0 instanceof EntityLiving) {
                t0.setCustomName(ichatbasecomponent);
            }

            try { a((World) worldserver, entityhuman, t0, nbttagcompound); } catch (Throwable t) { LOGGER.warn("Error loading spawn egg NBT", t); } // CraftBukkit - SPIGOT-5665
            return t0;
        }
    }

    protected static double a(IWorldReader iworldreader, BlockPosition blockposition, boolean flag, AxisAlignedBB axisalignedbb) {
        AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(blockposition);

        if (flag) {
            axisalignedbb1 = axisalignedbb1.b(0.0D, -1.0D, 0.0D);
        }

        Stream<VoxelShape> stream = iworldreader.d((Entity) null, axisalignedbb1, (entity) -> {
            return true;
        });

        return 1.0D + VoxelShapes.a(EnumDirection.EnumAxis.Y, axisalignedbb, stream, flag ? -2.0D : -1.0D);
    }

    public static void a(World world, @Nullable EntityHuman entityhuman, @Nullable Entity entity, @Nullable NBTTagCompound nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.hasKeyOfType("EntityTag", 10)) {
            MinecraftServer minecraftserver = world.getMinecraftServer();

            if (minecraftserver != null && entity != null) {
                if (world.isClientSide || !entity.cy() || entityhuman != null && minecraftserver.getPlayerList().isOp(entityhuman.getProfile())) {
                    NBTTagCompound nbttagcompound1 = entity.save(new NBTTagCompound());
                    UUID uuid = entity.getUniqueID();

                    nbttagcompound1.a(nbttagcompound.getCompound("EntityTag"));
                    entity.a_(uuid);
                    entity.load(nbttagcompound1);
                }
            }
        }
    }

    public boolean b() {
        return this.serialize;
    }

    public boolean c() {
        return this.summon;
    }

    public boolean d() {
        return this.fireImmune;
    }

    public boolean e() {
        return this.canSpawnFarFromPlayer;
    }

    public EnumCreatureType f() {
        return this.category;
    }

    public String g() {
        if (this.descriptionId == null) {
            this.descriptionId = SystemUtils.a("entity", IRegistry.ENTITY_TYPE.getKey(this));
        }

        return this.descriptionId;
    }

    public IChatBaseComponent h() {
        if (this.description == null) {
            this.description = new ChatMessage(this.g());
        }

        return this.description;
    }

    public String toString() {
        return this.g();
    }

    public String i() {
        int i = this.g().lastIndexOf(46);

        return i == -1 ? this.g() : this.g().substring(i + 1);
    }

    public MinecraftKey j() {
        if (this.lootTable == null) {
            MinecraftKey minecraftkey = IRegistry.ENTITY_TYPE.getKey(this);

            this.lootTable = new MinecraftKey(minecraftkey.getNamespace(), "entities/" + minecraftkey.getKey());
        }

        return this.lootTable;
    }

    public float k() {
        return this.dimensions.width;
    }

    public float l() {
        return this.dimensions.height;
    }

    @Nullable
    public T a(World world) {
        return this.factory.create(this, world);
    }

    @Nullable
    public static Entity a(int i, World world) {
        return a(world, (EntityTypes) IRegistry.ENTITY_TYPE.fromId(i));
    }

    public static Optional<Entity> a(NBTTagCompound nbttagcompound, World world) {
        return SystemUtils.a(a(nbttagcompound).map((entitytypes) -> {
            return entitytypes.a(world);
        }), (entity) -> {
            entity.load(nbttagcompound);
        }, () -> {
            EntityTypes.LOGGER.warn("Skipping Entity with id {}", nbttagcompound.getString("id"));
        });
    }

    @Nullable
    private static Entity a(World world, @Nullable EntityTypes<?> entitytypes) {
        return entitytypes == null ? null : entitytypes.a(world);
    }

    public AxisAlignedBB a(double d0, double d1, double d2) {
        float f = this.k() / 2.0F;

        return new AxisAlignedBB(d0 - (double) f, d1, d2 - (double) f, d0 + (double) f, d1 + (double) this.l(), d2 + (double) f);
    }

    public boolean a(IBlockData iblockdata) {
        return this.immuneTo.contains(iblockdata.getBlock()) ? false : (!this.fireImmune && PathfinderNormal.a(iblockdata) ? true : iblockdata.a(Blocks.WITHER_ROSE) || iblockdata.a(Blocks.SWEET_BERRY_BUSH) || iblockdata.a(Blocks.CACTUS) || iblockdata.a(Blocks.POWDER_SNOW));
    }

    public EntitySize m() {
        return this.dimensions;
    }

    public static Optional<EntityTypes<?>> a(NBTTagCompound nbttagcompound) {
        return IRegistry.ENTITY_TYPE.getOptional(new MinecraftKey(nbttagcompound.getString("id")));
    }

    @Nullable
    public static Entity a(NBTTagCompound nbttagcompound, World world, Function<Entity, Entity> function) {
        return (Entity) b(nbttagcompound, world).map(function).map((entity) -> {
            if (nbttagcompound.hasKeyOfType("Passengers", 9)) {
                NBTTagList nbttaglist = nbttagcompound.getList("Passengers", 10);

                for (int i = 0; i < nbttaglist.size(); ++i) {
                    Entity entity1 = a(nbttaglist.getCompound(i), world, function);

                    if (entity1 != null) {
                        entity1.a(entity, true);
                    }
                }
            }

            return entity;
        }).orElse(null); // CraftBukkit - decompile error
    }

    public static Stream<Entity> a(final List<? extends NBTBase> list, final World world) {
        final Spliterator<? extends NBTBase> spliterator = list.spliterator();

        return StreamSupport.stream(new Spliterator<Entity>() {
            public boolean tryAdvance(Consumer<? super Entity> consumer) {
                return spliterator.tryAdvance((nbtbase) -> {
                    EntityTypes.a((NBTTagCompound) nbtbase, world, (entity) -> {
                        consumer.accept(entity);
                        return entity;
                    });
                });
            }

            public Spliterator<Entity> trySplit() {
                return null;
            }

            public long estimateSize() {
                return (long) list.size();
            }

            public int characteristics() {
                return 1297;
            }
        }, false);
    }

    private static Optional<Entity> b(NBTTagCompound nbttagcompound, World world) {
        try {
            return a(nbttagcompound, world);
        } catch (RuntimeException runtimeexception) {
            EntityTypes.LOGGER.warn("Exception loading entity: ", runtimeexception);
            return Optional.empty();
        }
    }

    public int getChunkRange() {
        return this.clientTrackingRange;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }

    public boolean isDeltaTracking() {
        return this != EntityTypes.PLAYER && this != EntityTypes.LLAMA_SPIT && this != EntityTypes.WITHER && this != EntityTypes.BAT && this != EntityTypes.ITEM_FRAME && this != EntityTypes.GLOW_ITEM_FRAME && this != EntityTypes.LEASH_KNOT && this != EntityTypes.PAINTING && this != EntityTypes.END_CRYSTAL && this != EntityTypes.EVOKER_FANGS;
    }

    public boolean a(Tag<EntityTypes<?>> tag) {
        return tag.isTagged(this);
    }

    @Nullable
    public T a(Entity entity) {
        return entity.getEntityType() == this ? (T) entity : null; // CraftBukkit - decompile error
    }

    @Override
    public Class<? extends Entity> a() {
        return Entity.class;
    }

    public static class Builder<T extends Entity> {

        private final EntityTypes.b<T> factory;
        private final EnumCreatureType category;
        private ImmutableSet<Block> immuneTo = ImmutableSet.of();
        private boolean serialize = true;
        private boolean summon = true;
        private boolean fireImmune;
        private boolean canSpawnFarFromPlayer;
        private int clientTrackingRange = 5;
        private int updateInterval = 3;
        private EntitySize dimensions = EntitySize.b(0.6F, 1.8F);

        private Builder(EntityTypes.b<T> entitytypes_b, EnumCreatureType enumcreaturetype) {
            this.factory = entitytypes_b;
            this.category = enumcreaturetype;
            this.canSpawnFarFromPlayer = enumcreaturetype == EnumCreatureType.CREATURE || enumcreaturetype == EnumCreatureType.MISC;
        }

        public static <T extends Entity> EntityTypes.Builder<T> a(EntityTypes.b entitytypes_b, EnumCreatureType enumcreaturetype) { // CraftBukkit - decompile error
            return new EntityTypes.Builder<>(entitytypes_b, enumcreaturetype);
        }

        public static <T extends Entity> EntityTypes.Builder<T> a(EnumCreatureType enumcreaturetype) {
            return new EntityTypes.Builder<>((entitytypes, world) -> {
                return null;
            }, enumcreaturetype);
        }

        public EntityTypes.Builder<T> a(float f, float f1) {
            this.dimensions = EntitySize.b(f, f1);
            return this;
        }

        public EntityTypes.Builder<T> a() {
            this.summon = false;
            return this;
        }

        public EntityTypes.Builder<T> b() {
            this.serialize = false;
            return this;
        }

        public EntityTypes.Builder<T> c() {
            this.fireImmune = true;
            return this;
        }

        public EntityTypes.Builder<T> a(Block... ablock) {
            this.immuneTo = ImmutableSet.copyOf(ablock);
            return this;
        }

        public EntityTypes.Builder<T> d() {
            this.canSpawnFarFromPlayer = true;
            return this;
        }

        public EntityTypes.Builder<T> trackingRange(int i) {
            this.clientTrackingRange = i;
            return this;
        }

        public EntityTypes.Builder<T> updateInterval(int i) {
            this.updateInterval = i;
            return this;
        }

        public EntityTypes<T> a(String s) {
            if (this.serialize) {
                SystemUtils.a(DataConverterTypes.ENTITY_TREE, s);
            }

            return new EntityTypes<>(this.factory, this.category, this.serialize, this.summon, this.fireImmune, this.canSpawnFarFromPlayer, this.immuneTo, this.dimensions, this.clientTrackingRange, this.updateInterval);
        }
    }

    public interface b<T extends Entity> {

        T create(EntityTypes<T> entitytypes, World world);
    }
}
