package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IFluidContainer;
import net.minecraft.world.level.block.IFluidSource;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypeFlowing;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;

// CraftBukkit start
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.WorldServer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.DummyGeneratorAccess;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
// CraftBukkit end

public class ItemBucket extends Item implements DispensibleContainerItem {

    public final FluidType content;

    public ItemBucket(FluidType fluidtype, Item.Info item_info) {
        super(item_info);
        this.content = fluidtype;
    }

    @Override
    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);
        MovingObjectPositionBlock movingobjectpositionblock = a(world, entityhuman, this.content == FluidTypes.EMPTY ? RayTrace.FluidCollisionOption.SOURCE_ONLY : RayTrace.FluidCollisionOption.NONE);

        if (movingobjectpositionblock.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
            return InteractionResultWrapper.pass(itemstack);
        } else if (movingobjectpositionblock.getType() != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            return InteractionResultWrapper.pass(itemstack);
        } else {
            BlockPosition blockposition = movingobjectpositionblock.getBlockPosition();
            EnumDirection enumdirection = movingobjectpositionblock.getDirection();
            BlockPosition blockposition1 = blockposition.shift(enumdirection);

            if (world.a(entityhuman, blockposition) && entityhuman.a(blockposition1, enumdirection, itemstack)) {
                IBlockData iblockdata;

                if (this.content == FluidTypes.EMPTY) {
                    iblockdata = world.getType(blockposition);
                    if (iblockdata.getBlock() instanceof IFluidSource) {
                        IFluidSource ifluidsource = (IFluidSource) iblockdata.getBlock();
                        // CraftBukkit start
                        ItemStack dummyFluid = ifluidsource.removeFluid(DummyGeneratorAccess.INSTANCE, blockposition, iblockdata);
                        if (dummyFluid.isEmpty()) return InteractionResultWrapper.fail(itemstack); // Don't fire event if the bucket won't be filled.
                        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent((WorldServer) world, entityhuman, blockposition, blockposition, movingobjectpositionblock.getDirection(), itemstack, dummyFluid.getItem());

                        if (event.isCancelled()) {
                            ((EntityPlayer) entityhuman).connection.sendPacket(new PacketPlayOutBlockChange(world, blockposition)); // SPIGOT-5163 (see PlayerInteractManager)
                            ((EntityPlayer) entityhuman).getBukkitEntity().updateInventory(); // SPIGOT-4541
                            return InteractionResultWrapper.fail(itemstack);
                        }
                        // CraftBukkit end
                        ItemStack itemstack1 = ifluidsource.removeFluid(world, blockposition, iblockdata);

                        if (!itemstack1.isEmpty()) {
                            entityhuman.b(StatisticList.ITEM_USED.b(this));
                            ifluidsource.V_().ifPresent((soundeffect) -> {
                                entityhuman.playSound(soundeffect, 1.0F, 1.0F);
                            });
                            world.a((Entity) entityhuman, GameEvent.FLUID_PICKUP, blockposition);
                            ItemStack itemstack2 = ItemLiquidUtil.a(itemstack, entityhuman, CraftItemStack.asNMSCopy(event.getItemStack())); // CraftBukkit

                            if (!world.isClientSide) {
                                CriterionTriggers.FILLED_BUCKET.a((EntityPlayer) entityhuman, itemstack1);
                            }

                            return InteractionResultWrapper.a(itemstack2, world.isClientSide());
                        }
                    }

                    return InteractionResultWrapper.fail(itemstack);
                } else {
                    iblockdata = world.getType(blockposition);
                    BlockPosition blockposition2 = iblockdata.getBlock() instanceof IFluidContainer && this.content == FluidTypes.WATER ? blockposition : blockposition1;

                    if (this.a(entityhuman, world, blockposition2, movingobjectpositionblock, movingobjectpositionblock.getDirection(), blockposition, itemstack)) { // CraftBukkit
                        this.a(entityhuman, world, itemstack, blockposition2);
                        if (entityhuman instanceof EntityPlayer) {
                            CriterionTriggers.PLACED_BLOCK.a((EntityPlayer) entityhuman, blockposition2, itemstack);
                        }

                        entityhuman.b(StatisticList.ITEM_USED.b(this));
                        return InteractionResultWrapper.a(a(itemstack, entityhuman), world.isClientSide());
                    } else {
                        return InteractionResultWrapper.fail(itemstack);
                    }
                }
            } else {
                return InteractionResultWrapper.fail(itemstack);
            }
        }
    }

    public static ItemStack a(ItemStack itemstack, EntityHuman entityhuman) {
        return !entityhuman.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : itemstack;
    }

    @Override
    public void a(@Nullable EntityHuman entityhuman, World world, ItemStack itemstack, BlockPosition blockposition) {}

    @Override
    public boolean a(@Nullable EntityHuman entityhuman, World world, BlockPosition blockposition, @Nullable MovingObjectPositionBlock movingobjectpositionblock) {
        return a(entityhuman, world, blockposition, movingobjectpositionblock, null, null, null);
    }

    public boolean a(EntityHuman entityhuman, World world, BlockPosition blockposition, @Nullable MovingObjectPositionBlock movingobjectpositionblock, EnumDirection enumdirection, BlockPosition clicked, ItemStack itemstack) {
        // CraftBukkit end
        if (!(this.content instanceof FluidTypeFlowing)) {
            return false;
        } else {
            IBlockData iblockdata = world.getType(blockposition);
            Block block = iblockdata.getBlock();
            Material material = iblockdata.getMaterial();
            boolean flag = iblockdata.a(this.content);
            boolean flag1 = iblockdata.isAir() || flag || block instanceof IFluidContainer && ((IFluidContainer) block).canPlace(world, blockposition, iblockdata, this.content);

            // CraftBukkit start
            if (flag1 && entityhuman != null) {
                PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent((WorldServer) world, entityhuman, blockposition, clicked, enumdirection, itemstack);
                if (event.isCancelled()) {
                    ((EntityPlayer) entityhuman).connection.sendPacket(new PacketPlayOutBlockChange(world, blockposition)); // SPIGOT-4238: needed when looking through entity
                    ((EntityPlayer) entityhuman).getBukkitEntity().updateInventory(); // SPIGOT-4541
                    return false;
                }
            }
            // CraftBukkit end
            if (!flag1) {
                return movingobjectpositionblock != null && this.a(entityhuman, world, movingobjectpositionblock.getBlockPosition().shift(movingobjectpositionblock.getDirection()), (MovingObjectPositionBlock) null, enumdirection, clicked, itemstack); // CraftBukkit
            } else if (world.getDimensionManager().isNether() && this.content.a((Tag) TagsFluid.WATER)) {
                int i = blockposition.getX();
                int j = blockposition.getY();
                int k = blockposition.getZ();

                world.playSound(entityhuman, blockposition, SoundEffects.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                for (int l = 0; l < 8; ++l) {
                    world.addParticle(Particles.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0D, 0.0D, 0.0D);
                }

                return true;
            } else if (block instanceof IFluidContainer && this.content == FluidTypes.WATER) {
                ((IFluidContainer) block).place(world, blockposition, iblockdata, ((FluidTypeFlowing) this.content).a(false));
                this.a(entityhuman, (GeneratorAccess) world, blockposition);
                return true;
            } else {
                if (!world.isClientSide && flag && !material.isLiquid()) {
                    world.b(blockposition, true);
                }

                if (!world.setTypeAndData(blockposition, this.content.h().getBlockData(), 11) && !iblockdata.getFluid().isSource()) {
                    return false;
                } else {
                    this.a(entityhuman, (GeneratorAccess) world, blockposition);
                    return true;
                }
            }
        }
    }

    protected void a(@Nullable EntityHuman entityhuman, GeneratorAccess generatoraccess, BlockPosition blockposition) {
        SoundEffect soundeffect = this.content.a((Tag) TagsFluid.LAVA) ? SoundEffects.BUCKET_EMPTY_LAVA : SoundEffects.BUCKET_EMPTY;

        generatoraccess.playSound(entityhuman, blockposition, soundeffect, SoundCategory.BLOCKS, 1.0F, 1.0F);
        generatoraccess.a((Entity) entityhuman, GameEvent.FLUID_PLACE, blockposition);
    }
}
