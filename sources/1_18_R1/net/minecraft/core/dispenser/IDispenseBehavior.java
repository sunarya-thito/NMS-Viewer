package net.minecraft.core.dispenser;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.animal.horse.EntityHorseChestedAbstract;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.item.EntityTNTPrimed;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.EntityPotion;
import net.minecraft.world.entity.projectile.EntitySmallFireball;
import net.minecraft.world.entity.projectile.EntitySnowball;
import net.minecraft.world.entity.projectile.EntitySpectralArrow;
import net.minecraft.world.entity.projectile.EntityThrownExpBottle;
import net.minecraft.world.entity.projectile.EntityTippedArrow;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemBoneMeal;
import net.minecraft.world.item.ItemMonsterEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.BlockPumpkinCarved;
import net.minecraft.world.level.block.BlockRespawnAnchor;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.BlockSkull;
import net.minecraft.world.level.block.BlockTNT;
import net.minecraft.world.level.block.BlockWitherSkull;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.IFluidSource;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.world.item.ItemBucket;
import net.minecraft.world.level.block.BlockSapling;
import net.minecraft.world.level.block.IFluidContainer;
import net.minecraft.world.level.material.Material;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.DummyGeneratorAccess;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public interface IDispenseBehavior {

    Logger LOGGER = LogManager.getLogger();
    IDispenseBehavior NOOP = (isourceblock, itemstack) -> {
        return itemstack;
    };

    ItemStack dispense(ISourceBlock isourceblock, ItemStack itemstack);

    static void bootStrap() {
        BlockDispenser.registerBehavior(Items.ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                EntityTippedArrow entitytippedarrow = new EntityTippedArrow(world, iposition.x(), iposition.y(), iposition.z());

                entitytippedarrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return entitytippedarrow;
            }
        });
        BlockDispenser.registerBehavior(Items.TIPPED_ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                EntityTippedArrow entitytippedarrow = new EntityTippedArrow(world, iposition.x(), iposition.y(), iposition.z());

                entitytippedarrow.setEffectsFromItem(itemstack);
                entitytippedarrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return entitytippedarrow;
            }
        });
        BlockDispenser.registerBehavior(Items.SPECTRAL_ARROW, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                EntitySpectralArrow entityspectralarrow = new EntitySpectralArrow(world, iposition.x(), iposition.y(), iposition.z());

                entityspectralarrow.pickup = EntityArrow.PickupStatus.ALLOWED;
                return entityspectralarrow;
            }
        });
        BlockDispenser.registerBehavior(Items.EGG, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                return (IProjectile) SystemUtils.make(new EntityEgg(world, iposition.x(), iposition.y(), iposition.z()), (entityegg) -> {
                    entityegg.setItem(itemstack);
                });
            }
        });
        BlockDispenser.registerBehavior(Items.SNOWBALL, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                return (IProjectile) SystemUtils.make(new EntitySnowball(world, iposition.x(), iposition.y(), iposition.z()), (entitysnowball) -> {
                    entitysnowball.setItem(itemstack);
                });
            }
        });
        BlockDispenser.registerBehavior(Items.EXPERIENCE_BOTTLE, new DispenseBehaviorProjectile() {
            @Override
            protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack) {
                return (IProjectile) SystemUtils.make(new EntityThrownExpBottle(world, iposition.x(), iposition.y(), iposition.z()), (entitythrownexpbottle) -> {
                    entitythrownexpbottle.setItem(itemstack);
                });
            }

            @Override
            protected float getUncertainty() {
                return super.getUncertainty() * 0.5F;
            }

            @Override
            protected float getPower() {
                return super.getPower() * 1.25F;
            }
        });
        BlockDispenser.registerBehavior(Items.SPLASH_POTION, new IDispenseBehavior() {
            @Override
            public ItemStack dispense(ISourceBlock isourceblock, ItemStack itemstack) {
                return (new DispenseBehaviorProjectile() {
                    @Override
                    protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack1) {
                        return (IProjectile) SystemUtils.make(new EntityPotion(world, iposition.x(), iposition.y(), iposition.z()), (entitypotion) -> {
                            entitypotion.setItem(itemstack1);
                        });
                    }

                    @Override
                    protected float getUncertainty() {
                        return super.getUncertainty() * 0.5F;
                    }

                    @Override
                    protected float getPower() {
                        return super.getPower() * 1.25F;
                    }
                }).dispense(isourceblock, itemstack);
            }
        });
        BlockDispenser.registerBehavior(Items.LINGERING_POTION, new IDispenseBehavior() {
            @Override
            public ItemStack dispense(ISourceBlock isourceblock, ItemStack itemstack) {
                return (new DispenseBehaviorProjectile() {
                    @Override
                    protected IProjectile getProjectile(World world, IPosition iposition, ItemStack itemstack1) {
                        return (IProjectile) SystemUtils.make(new EntityPotion(world, iposition.x(), iposition.y(), iposition.z()), (entitypotion) -> {
                            entitypotion.setItem(itemstack1);
                        });
                    }

                    @Override
                    protected float getUncertainty() {
                        return super.getUncertainty() * 0.5F;
                    }

                    @Override
                    protected float getPower() {
                        return super.getPower() * 1.25F;
                    }
                }).dispense(isourceblock, itemstack);
            }
        });
        DispenseBehaviorItem dispensebehavioritem = new DispenseBehaviorItem() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                EntityTypes entitytypes = ((ItemMonsterEgg) itemstack.getItem()).getType(itemstack.getTag());

                // CraftBukkit start
                WorldServer worldserver = isourceblock.getLevel();
                ItemStack itemstack1 = itemstack.split(1);
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                try {
                    entitytypes.spawn(isourceblock.getLevel(), itemstack, (EntityHuman) null, isourceblock.getPos().relative(enumdirection), EnumMobSpawn.DISPENSER, enumdirection != EnumDirection.UP, false);
                } catch (Exception exception) {
                    LOGGER.error("Error while dispensing spawn egg from dispenser at {}", isourceblock.getPos(), exception); // CraftBukkit - decompile error
                    return ItemStack.EMPTY;
                }

                // itemstack.shrink(1); // Handled during event processing
                // CraftBukkit end
                isourceblock.getLevel().gameEvent(GameEvent.ENTITY_PLACE, isourceblock.getPos());
                return itemstack;
            }
        };
        Iterator iterator = ItemMonsterEgg.eggs().iterator();

        while (iterator.hasNext()) {
            ItemMonsterEgg itemmonsteregg = (ItemMonsterEgg) iterator.next();

            BlockDispenser.registerBehavior(itemmonsteregg, dispensebehavioritem);
        }

        BlockDispenser.registerBehavior(Items.ARMOR_STAND, new DispenseBehaviorItem() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                BlockPosition blockposition = isourceblock.getPos().relative(enumdirection);
                WorldServer worldserver = isourceblock.getLevel();

                // CraftBukkit start
                ItemStack itemstack1 = itemstack.split(1);
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                // CraftBukkit end

                EntityArmorStand entityarmorstand = new EntityArmorStand(worldserver, (double) blockposition.getX() + 0.5D, (double) blockposition.getY(), (double) blockposition.getZ() + 0.5D);

                EntityTypes.updateCustomEntityTag(worldserver, (EntityHuman) null, entityarmorstand, itemstack.getTag());
                entityarmorstand.setYRot(enumdirection.toYRot());
                worldserver.addFreshEntity(entityarmorstand);
                // itemstack.shrink(1); // CraftBukkit - Handled during event processing
                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Items.SADDLE, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                List<EntityLiving> list = isourceblock.getLevel().getEntitiesOfClass(EntityLiving.class, new AxisAlignedBB(blockposition), (entityliving) -> {
                    if (!(entityliving instanceof ISaddleable)) {
                        return false;
                    } else {
                        ISaddleable isaddleable = (ISaddleable) entityliving;

                        return !isaddleable.isSaddled() && isaddleable.isSaddleable();
                    }
                });

                if (!list.isEmpty()) {
                    // CraftBukkit start
                    ItemStack itemstack1 = itemstack.split(1);
                    World world = isourceblock.getLevel();
                    org.bukkit.block.Block block = world.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                    CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                    BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(block, craftItem.clone(), (org.bukkit.craftbukkit.entity.CraftLivingEntity) list.get(0).getBukkitEntity());
                    if (!BlockDispenser.eventFired) {
                        world.getCraftServer().getPluginManager().callEvent(event);
                    }

                    if (event.isCancelled()) {
                        itemstack.grow(1);
                        return itemstack;
                    }

                    if (!event.getItem().equals(craftItem)) {
                        itemstack.grow(1);
                        // Chain to handler for new item
                        ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                        IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                        if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != ItemArmor.DISPENSE_ITEM_BEHAVIOR) {
                            idispensebehavior.dispense(isourceblock, eventStack);
                            return itemstack;
                        }
                    }
                    // CraftBukkit end
                    ((ISaddleable) list.get(0)).equipSaddle(SoundCategory.BLOCKS);
                    // itemstack.shrink(1); // CraftBukkit - handled above
                    this.setSuccess(true);
                    return itemstack;
                } else {
                    return super.execute(isourceblock, itemstack);
                }
            }
        });
        DispenseBehaviorMaybe dispensebehaviormaybe = new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                List<EntityHorseAbstract> list = isourceblock.getLevel().getEntitiesOfClass(EntityHorseAbstract.class, new AxisAlignedBB(blockposition), (entityhorseabstract) -> {
                    return entityhorseabstract.isAlive() && entityhorseabstract.canWearArmor();
                });
                Iterator iterator1 = list.iterator();

                EntityHorseAbstract entityhorseabstract;

                do {
                    if (!iterator1.hasNext()) {
                        return super.execute(isourceblock, itemstack);
                    }

                    entityhorseabstract = (EntityHorseAbstract) iterator1.next();
                } while (!entityhorseabstract.isArmor(itemstack) || entityhorseabstract.isWearingArmor() || !entityhorseabstract.isTamed());

                // CraftBukkit start
                ItemStack itemstack1 = itemstack.split(1);
                World world = isourceblock.getLevel();
                org.bukkit.block.Block block = world.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(block, craftItem.clone(), (org.bukkit.craftbukkit.entity.CraftLivingEntity) entityhorseabstract.getBukkitEntity());
                if (!BlockDispenser.eventFired) {
                    world.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != ItemArmor.DISPENSE_ITEM_BEHAVIOR) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                entityhorseabstract.getSlot(401).set(CraftItemStack.asNMSCopy(event.getItem()));
                // CraftBukkit end
                this.setSuccess(true);
                return itemstack;
            }
        };

        BlockDispenser.registerBehavior(Items.LEATHER_HORSE_ARMOR, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.IRON_HORSE_ARMOR, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.GOLDEN_HORSE_ARMOR, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.DIAMOND_HORSE_ARMOR, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.WHITE_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.ORANGE_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.CYAN_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.BLUE_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.BROWN_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.BLACK_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.GRAY_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.GREEN_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.LIGHT_BLUE_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.LIGHT_GRAY_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.LIME_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.MAGENTA_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.PINK_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.PURPLE_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.RED_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.YELLOW_CARPET, dispensebehaviormaybe);
        BlockDispenser.registerBehavior(Items.CHEST, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                List<EntityHorseChestedAbstract> list = isourceblock.getLevel().getEntitiesOfClass(EntityHorseChestedAbstract.class, new AxisAlignedBB(blockposition), (entityhorsechestedabstract) -> {
                    return entityhorsechestedabstract.isAlive() && !entityhorsechestedabstract.hasChest();
                });
                Iterator iterator1 = list.iterator();

                EntityHorseChestedAbstract entityhorsechestedabstract;

                do {
                    if (!iterator1.hasNext()) {
                        return super.execute(isourceblock, itemstack);
                    }

                    entityhorsechestedabstract = (EntityHorseChestedAbstract) iterator1.next();
                    // CraftBukkit start
                } while (!entityhorsechestedabstract.isTamed());
                ItemStack itemstack1 = itemstack.split(1);
                World world = isourceblock.getLevel();
                org.bukkit.block.Block block = world.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseArmorEvent event = new BlockDispenseArmorEvent(block, craftItem.clone(), (org.bukkit.craftbukkit.entity.CraftLivingEntity) entityhorsechestedabstract.getBukkitEntity());
                if (!BlockDispenser.eventFired) {
                    world.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != ItemArmor.DISPENSE_ITEM_BEHAVIOR) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                entityhorsechestedabstract.getSlot(499).set(CraftItemStack.asNMSCopy(event.getItem()));
                // CraftBukkit end

                // itemstack.shrink(1); // CraftBukkit - handled above
                this.setSuccess(true);
                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Items.FIREWORK_ROCKET, new DispenseBehaviorItem() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                // CraftBukkit start
                WorldServer worldserver = isourceblock.getLevel();
                ItemStack itemstack1 = itemstack.split(1);
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(enumdirection.getStepX(), enumdirection.getStepY(), enumdirection.getStepZ()));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                itemstack1 = CraftItemStack.asNMSCopy(event.getItem());
                EntityFireworks entityfireworks = new EntityFireworks(isourceblock.getLevel(), itemstack, isourceblock.x(), isourceblock.y(), isourceblock.x(), true);

                IDispenseBehavior.setEntityPokingOutOfBlock(isourceblock, entityfireworks, enumdirection);
                entityfireworks.shoot((double) enumdirection.getStepX(), (double) enumdirection.getStepY(), (double) enumdirection.getStepZ(), 0.5F, 1.0F);
                isourceblock.getLevel().addFreshEntity(entityfireworks);
                // itemstack.shrink(1); // Handled during event processing
                // CraftBukkit end
                return itemstack;
            }

            @Override
            protected void playSound(ISourceBlock isourceblock) {
                isourceblock.getLevel().levelEvent(1004, isourceblock.getPos(), 0);
            }
        });
        BlockDispenser.registerBehavior(Items.FIRE_CHARGE, new DispenseBehaviorItem() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                IPosition iposition = BlockDispenser.getDispensePosition(isourceblock);
                double d0 = iposition.x() + (double) ((float) enumdirection.getStepX() * 0.3F);
                double d1 = iposition.y() + (double) ((float) enumdirection.getStepY() * 0.3F);
                double d2 = iposition.z() + (double) ((float) enumdirection.getStepZ() * 0.3F);
                WorldServer worldserver = isourceblock.getLevel();
                Random random = worldserver.random;
                double d3 = random.nextGaussian() * 0.05D + (double) enumdirection.getStepX();
                double d4 = random.nextGaussian() * 0.05D + (double) enumdirection.getStepY();
                double d5 = random.nextGaussian() * 0.05D + (double) enumdirection.getStepZ();

                // CraftBukkit start
                ItemStack itemstack1 = itemstack.split(1);
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d3, d4, d5));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                EntitySmallFireball entitysmallfireball = new EntitySmallFireball(worldserver, d0, d1, d2, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
                entitysmallfireball.setItem(itemstack1);
                entitysmallfireball.projectileSource = new org.bukkit.craftbukkit.projectiles.CraftBlockProjectileSource((TileEntityDispenser) isourceblock.getEntity());

                worldserver.addFreshEntity(entitysmallfireball);
                // itemstack.shrink(1); // Handled during event processing
                // CraftBukkit end
                return itemstack;
            }

            @Override
            protected void playSound(ISourceBlock isourceblock) {
                isourceblock.getLevel().levelEvent(1018, isourceblock.getPos(), 0);
            }
        });
        BlockDispenser.registerBehavior(Items.OAK_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.OAK));
        BlockDispenser.registerBehavior(Items.SPRUCE_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.SPRUCE));
        BlockDispenser.registerBehavior(Items.BIRCH_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.BIRCH));
        BlockDispenser.registerBehavior(Items.JUNGLE_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.JUNGLE));
        BlockDispenser.registerBehavior(Items.DARK_OAK_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.DARK_OAK));
        BlockDispenser.registerBehavior(Items.ACACIA_BOAT, new DispenseBehaviorBoat(EntityBoat.EnumBoatType.ACACIA));
        DispenseBehaviorItem dispensebehavioritem1 = new DispenseBehaviorItem() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                DispensibleContainerItem dispensiblecontaineritem = (DispensibleContainerItem) itemstack.getItem();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                WorldServer worldserver = isourceblock.getLevel();

                // CraftBukkit start
                int x = blockposition.getX();
                int y = blockposition.getY();
                int z = blockposition.getZ();
                IBlockData iblockdata = worldserver.getBlockState(blockposition);
                Material material = iblockdata.getMaterial();
                if (worldserver.isEmptyBlock(blockposition) || !material.isSolid() || material.isReplaceable() || (dispensiblecontaineritem instanceof ItemBucket && iblockdata.getBlock() instanceof IFluidContainer && ((IFluidContainer) iblockdata.getBlock()).canPlaceLiquid(worldserver, blockposition, iblockdata, ((ItemBucket) dispensiblecontaineritem).content))) {
                    org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                    CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                    BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(x, y, z));
                    if (!BlockDispenser.eventFired) {
                        worldserver.getCraftServer().getPluginManager().callEvent(event);
                    }

                    if (event.isCancelled()) {
                        return itemstack;
                    }

                    if (!event.getItem().equals(craftItem)) {
                        // Chain to handler for new item
                        ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                        IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                        if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                            idispensebehavior.dispense(isourceblock, eventStack);
                            return itemstack;
                        }
                    }

                    dispensiblecontaineritem = (DispensibleContainerItem) CraftItemStack.asNMSCopy(event.getItem()).getItem();
                }
                // CraftBukkit end

                if (dispensiblecontaineritem.emptyContents((EntityHuman) null, worldserver, blockposition, (MovingObjectPositionBlock) null)) {
                    dispensiblecontaineritem.checkExtraContent((EntityHuman) null, worldserver, itemstack, blockposition);
                    // CraftBukkit start - Handle stacked buckets
                    Item item = Items.BUCKET;
                    itemstack.shrink(1);
                    if (itemstack.isEmpty()) {
                        itemstack.setItem(Items.BUCKET);
                        itemstack.setCount(1);
                    } else if (((TileEntityDispenser) isourceblock.getEntity()).addItem(new ItemStack(item)) < 0) {
                        this.defaultDispenseItemBehavior.dispense(isourceblock, new ItemStack(item));
                    }
                    return itemstack;
                    // CraftBukkit end
                } else {
                    return this.defaultDispenseItemBehavior.dispense(isourceblock, itemstack);
                }
            }
        };

        BlockDispenser.registerBehavior(Items.LAVA_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.WATER_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.POWDER_SNOW_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.SALMON_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.COD_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.PUFFERFISH_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.TROPICAL_FISH_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.AXOLOTL_BUCKET, dispensebehavioritem1);
        BlockDispenser.registerBehavior(Items.BUCKET, new DispenseBehaviorItem() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                WorldServer worldserver = isourceblock.getLevel();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                IBlockData iblockdata = worldserver.getBlockState(blockposition);
                Block block = iblockdata.getBlock();

                if (block instanceof IFluidSource) {
                    ItemStack itemstack1 = ((IFluidSource) block).pickupBlock(DummyGeneratorAccess.INSTANCE, blockposition, iblockdata); // CraftBukkit

                    if (itemstack1.isEmpty()) {
                        return super.execute(isourceblock, itemstack);
                    } else {
                        worldserver.gameEvent((Entity) null, GameEvent.FLUID_PICKUP, blockposition);
                        Item item = itemstack1.getItem();

                        // CraftBukkit start
                        org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                        BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                        if (!BlockDispenser.eventFired) {
                            worldserver.getCraftServer().getPluginManager().callEvent(event);
                        }

                        if (event.isCancelled()) {
                            return itemstack;
                        }

                        if (!event.getItem().equals(craftItem)) {
                            // Chain to handler for new item
                            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                            IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                            if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                                idispensebehavior.dispense(isourceblock, eventStack);
                                return itemstack;
                            }
                        }

                        itemstack1 = ((IFluidSource) block).pickupBlock(worldserver, blockposition, iblockdata); // From above
                        // CraftBukkit end

                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            return new ItemStack(item);
                        } else {
                            if (((TileEntityDispenser) isourceblock.getEntity()).addItem(new ItemStack(item)) < 0) {
                                this.defaultDispenseItemBehavior.dispense(isourceblock, new ItemStack(item));
                            }

                            return itemstack;
                        }
                    }
                } else {
                    return super.execute(isourceblock, itemstack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.FLINT_AND_STEEL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                WorldServer worldserver = isourceblock.getLevel();

                // CraftBukkit start
                org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                // CraftBukkit end

                this.setSuccess(true);
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                BlockPosition blockposition = isourceblock.getPos().relative(enumdirection);
                IBlockData iblockdata = worldserver.getBlockState(blockposition);

                if (BlockFireAbstract.canBePlacedAt(worldserver, blockposition, enumdirection)) {
                    // CraftBukkit start - Ignition by dispensing flint and steel
                    if (!org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(worldserver, blockposition, isourceblock.getPos()).isCancelled()) {
                        worldserver.setBlockAndUpdate(blockposition, BlockFireAbstract.getState(worldserver, blockposition));
                        worldserver.gameEvent((Entity) null, GameEvent.BLOCK_PLACE, blockposition);
                    }
                    // CraftBukkit end
                } else if (!BlockCampfire.canLight(iblockdata) && !CandleBlock.canLight(iblockdata) && !CandleCakeBlock.canLight(iblockdata)) {
                    if (iblockdata.getBlock() instanceof BlockTNT) {
                        BlockTNT.explode(worldserver, blockposition);
                        worldserver.removeBlock(blockposition, false);
                    } else {
                        this.setSuccess(false);
                    }
                } else {
                    worldserver.setBlockAndUpdate(blockposition, (IBlockData) iblockdata.setValue(BlockProperties.LIT, true));
                    worldserver.gameEvent((Entity) null, GameEvent.BLOCK_CHANGE, blockposition);
                }

                if (this.isSuccess() && itemstack.hurt(1, worldserver.random, (EntityPlayer) null)) {
                    itemstack.setCount(0);
                }

                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Items.BONE_MEAL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                this.setSuccess(true);
                WorldServer worldserver = isourceblock.getLevel();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                // CraftBukkit start
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                worldserver.captureTreeGeneration = true;
                // CraftBukkit end

                if (!ItemBoneMeal.growCrop(itemstack, worldserver, blockposition) && !ItemBoneMeal.growWaterPlant(itemstack, worldserver, blockposition, (EnumDirection) null)) {
                    this.setSuccess(false);
                } else if (!worldserver.isClientSide) {
                    worldserver.levelEvent(1505, blockposition, 0);
                }
                // CraftBukkit start
                worldserver.captureTreeGeneration = false;
                if (worldserver.capturedBlockStates.size() > 0) {
                    TreeType treeType = BlockSapling.treeType;
                    BlockSapling.treeType = null;
                    Location location = new Location(worldserver.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                    List<org.bukkit.block.BlockState> blocks = new java.util.ArrayList<>(worldserver.capturedBlockStates.values());
                    worldserver.capturedBlockStates.clear();
                    StructureGrowEvent structureEvent = null;
                    if (treeType != null) {
                        structureEvent = new StructureGrowEvent(location, treeType, false, null, blocks);
                        org.bukkit.Bukkit.getPluginManager().callEvent(structureEvent);
                    }

                    BlockFertilizeEvent fertilizeEvent = new BlockFertilizeEvent(location.getBlock(), null, blocks);
                    fertilizeEvent.setCancelled(structureEvent != null && structureEvent.isCancelled());
                    org.bukkit.Bukkit.getPluginManager().callEvent(fertilizeEvent);

                    if (!fertilizeEvent.isCancelled()) {
                        for (org.bukkit.block.BlockState blockstate : blocks) {
                            blockstate.update(true);
                        }
                    }
                }
                // CraftBukkit end

                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.TNT, new DispenseBehaviorItem() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                WorldServer worldserver = isourceblock.getLevel();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                // CraftBukkit start
                // EntityTNTPrimed entitytntprimed = new EntityTNTPrimed(worldserver, (double) blockposition.getX() + 0.5D, (double) blockposition.getY(), (double) blockposition.getZ() + 0.5D, (EntityLiving) null);

                ItemStack itemstack1 = itemstack.split(1);
                org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

                BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector((double) blockposition.getX() + 0.5D, (double) blockposition.getY(), (double) blockposition.getZ() + 0.5D));
                if (!BlockDispenser.eventFired) {
                   worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    itemstack.grow(1);
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    itemstack.grow(1);
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }

                EntityTNTPrimed entitytntprimed = new EntityTNTPrimed(worldserver, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), (EntityLiving) null);
                // CraftBukkit end

                worldserver.addFreshEntity(entitytntprimed);
                worldserver.playSound((EntityHuman) null, entitytntprimed.getX(), entitytntprimed.getY(), entitytntprimed.getZ(), SoundEffects.TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
                worldserver.gameEvent((Entity) null, GameEvent.ENTITY_PLACE, blockposition);
                // itemstack.shrink(1); // CraftBukkit - handled above
                return itemstack;
            }
        });
        DispenseBehaviorMaybe dispensebehaviormaybe1 = new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                this.setSuccess(ItemArmor.dispenseArmor(isourceblock, itemstack));
                return itemstack;
            }
        };

        BlockDispenser.registerBehavior(Items.CREEPER_HEAD, dispensebehaviormaybe1);
        BlockDispenser.registerBehavior(Items.ZOMBIE_HEAD, dispensebehaviormaybe1);
        BlockDispenser.registerBehavior(Items.DRAGON_HEAD, dispensebehaviormaybe1);
        BlockDispenser.registerBehavior(Items.SKELETON_SKULL, dispensebehaviormaybe1);
        BlockDispenser.registerBehavior(Items.PLAYER_HEAD, dispensebehaviormaybe1);
        BlockDispenser.registerBehavior(Items.WITHER_SKELETON_SKULL, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                WorldServer worldserver = isourceblock.getLevel();
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                BlockPosition blockposition = isourceblock.getPos().relative(enumdirection);

                // CraftBukkit start
                org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                // CraftBukkit end

                if (worldserver.isEmptyBlock(blockposition) && BlockWitherSkull.canSpawnMob(worldserver, blockposition, itemstack)) {
                    worldserver.setBlock(blockposition, (IBlockData) Blocks.WITHER_SKELETON_SKULL.defaultBlockState().setValue(BlockSkull.ROTATION, enumdirection.getAxis() == EnumDirection.EnumAxis.Y ? 0 : enumdirection.getOpposite().get2DDataValue() * 4), 3);
                    worldserver.gameEvent((Entity) null, GameEvent.BLOCK_PLACE, blockposition);
                    TileEntity tileentity = worldserver.getBlockEntity(blockposition);

                    if (tileentity instanceof TileEntitySkull) {
                        BlockWitherSkull.checkSpawn(worldserver, blockposition, (TileEntitySkull) tileentity);
                    }

                    itemstack.shrink(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ItemArmor.dispenseArmor(isourceblock, itemstack));
                }

                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.CARVED_PUMPKIN, new DispenseBehaviorMaybe() {
            @Override
            protected ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                WorldServer worldserver = isourceblock.getLevel();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                BlockPumpkinCarved blockpumpkincarved = (BlockPumpkinCarved) Blocks.CARVED_PUMPKIN;

                // CraftBukkit start
                org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                // CraftBukkit end

                if (worldserver.isEmptyBlock(blockposition) && blockpumpkincarved.canSpawnGolem(worldserver, blockposition)) {
                    if (!worldserver.isClientSide) {
                        worldserver.setBlock(blockposition, blockpumpkincarved.defaultBlockState(), 3);
                        worldserver.gameEvent((Entity) null, GameEvent.BLOCK_PLACE, blockposition);
                    }

                    itemstack.shrink(1);
                    this.setSuccess(true);
                } else {
                    this.setSuccess(ItemArmor.dispenseArmor(isourceblock, itemstack));
                }

                return itemstack;
            }
        });
        BlockDispenser.registerBehavior(Blocks.SHULKER_BOX.asItem(), new DispenseBehaviorShulkerBox());
        EnumColor[] aenumcolor = EnumColor.values();
        int i = aenumcolor.length;

        for (int j = 0; j < i; ++j) {
            EnumColor enumcolor = aenumcolor[j];

            BlockDispenser.registerBehavior(BlockShulkerBox.getBlockByColor(enumcolor).asItem(), new DispenseBehaviorShulkerBox());
        }

        BlockDispenser.registerBehavior(Items.GLASS_BOTTLE.asItem(), new DispenseBehaviorMaybe() {
            private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

            private ItemStack takeLiquid(ISourceBlock isourceblock, ItemStack itemstack, ItemStack itemstack1) {
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    isourceblock.getLevel().gameEvent((Entity) null, GameEvent.FLUID_PICKUP, isourceblock.getPos());
                    return itemstack1.copy();
                } else {
                    if (((TileEntityDispenser) isourceblock.getEntity()).addItem(itemstack1.copy()) < 0) {
                        this.defaultDispenseItemBehavior.dispense(isourceblock, itemstack1.copy());
                    }

                    return itemstack;
                }
            }

            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                this.setSuccess(false);
                WorldServer worldserver = isourceblock.getLevel();
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                IBlockData iblockdata = worldserver.getBlockState(blockposition);

                // CraftBukkit start
                org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getPos().getX(), isourceblock.getPos().getY(), isourceblock.getPos().getZ());
                CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack);

                BlockDispenseEvent event = new BlockDispenseEvent(bukkitBlock, craftItem.clone(), new org.bukkit.util.Vector(blockposition.getX(), blockposition.getY(), blockposition.getZ()));
                if (!BlockDispenser.eventFired) {
                    worldserver.getCraftServer().getPluginManager().callEvent(event);
                }

                if (event.isCancelled()) {
                    return itemstack;
                }

                if (!event.getItem().equals(craftItem)) {
                    // Chain to handler for new item
                    ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                    IDispenseBehavior idispensebehavior = (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(eventStack.getItem());
                    if (idispensebehavior != IDispenseBehavior.NOOP && idispensebehavior != this) {
                        idispensebehavior.dispense(isourceblock, eventStack);
                        return itemstack;
                    }
                }
                // CraftBukkit end

                if (iblockdata.is(TagsBlock.BEEHIVES, (blockbase_blockdata) -> {
                    return blockbase_blockdata.hasProperty(BlockBeehive.HONEY_LEVEL);
                }) && (Integer) iblockdata.getValue(BlockBeehive.HONEY_LEVEL) >= 5) {
                    ((BlockBeehive) iblockdata.getBlock()).releaseBeesAndResetHoneyLevel(worldserver, iblockdata, blockposition, (EntityHuman) null, TileEntityBeehive.ReleaseStatus.BEE_RELEASED);
                    this.setSuccess(true);
                    return this.takeLiquid(isourceblock, itemstack, new ItemStack(Items.HONEY_BOTTLE));
                } else if (worldserver.getFluidState(blockposition).is((Tag) TagsFluid.WATER)) {
                    this.setSuccess(true);
                    return this.takeLiquid(isourceblock, itemstack, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER));
                } else {
                    return super.execute(isourceblock, itemstack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.GLOWSTONE, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING);
                BlockPosition blockposition = isourceblock.getPos().relative(enumdirection);
                WorldServer worldserver = isourceblock.getLevel();
                IBlockData iblockdata = worldserver.getBlockState(blockposition);

                this.setSuccess(true);
                if (iblockdata.is(Blocks.RESPAWN_ANCHOR)) {
                    if ((Integer) iblockdata.getValue(BlockRespawnAnchor.CHARGE) != 4) {
                        BlockRespawnAnchor.charge(worldserver, blockposition, iblockdata);
                        itemstack.shrink(1);
                    } else {
                        this.setSuccess(false);
                    }

                    return itemstack;
                } else {
                    return super.execute(isourceblock, itemstack);
                }
            }
        });
        BlockDispenser.registerBehavior(Items.SHEARS.asItem(), new DispenseBehaviorShears());
        BlockDispenser.registerBehavior(Items.HONEYCOMB, new DispenseBehaviorMaybe() {
            @Override
            public ItemStack execute(ISourceBlock isourceblock, ItemStack itemstack) {
                BlockPosition blockposition = isourceblock.getPos().relative((EnumDirection) isourceblock.getBlockState().getValue(BlockDispenser.FACING));
                WorldServer worldserver = isourceblock.getLevel();
                IBlockData iblockdata = worldserver.getBlockState(blockposition);
                Optional<IBlockData> optional = HoneycombItem.getWaxed(iblockdata);

                if (optional.isPresent()) {
                    worldserver.setBlockAndUpdate(blockposition, (IBlockData) optional.get());
                    worldserver.levelEvent(3003, blockposition, 0);
                    itemstack.shrink(1);
                    this.setSuccess(true);
                    return itemstack;
                } else {
                    return super.execute(isourceblock, itemstack);
                }
            }
        });
    }

    static void setEntityPokingOutOfBlock(ISourceBlock isourceblock, Entity entity, EnumDirection enumdirection) {
        entity.setPos(isourceblock.x() + (double) enumdirection.getStepX() * (0.5000099999997474D - (double) entity.getBbWidth() / 2.0D), isourceblock.y() + (double) enumdirection.getStepY() * (0.5000099999997474D - (double) entity.getBbHeight() / 2.0D) - (double) entity.getBbHeight() / 2.0D, isourceblock.z() + (double) enumdirection.getStepZ() * (0.5000099999997474D - (double) entity.getBbWidth() / 2.0D));
    }
}
