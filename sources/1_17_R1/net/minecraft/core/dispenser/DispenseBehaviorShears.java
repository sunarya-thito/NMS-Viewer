package net.minecraft.core.dispenser;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.ISourceBlock;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.IShearable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBeehive;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.entity.TileEntityBeehive;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public class DispenseBehaviorShears extends DispenseBehaviorMaybe {

    public DispenseBehaviorShears() {}

    @Override
    protected ItemStack a(ISourceBlock isourceblock, ItemStack itemstack) {
        WorldServer worldserver = isourceblock.getWorld();
        // CraftBukkit start
        org.bukkit.block.Block bukkitBlock = worldserver.getWorld().getBlockAt(isourceblock.getBlockPosition().getX(), isourceblock.getBlockPosition().getY(), isourceblock.getBlockPosition().getZ());
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

        if (!worldserver.isClientSide()) {
            BlockPosition blockposition = isourceblock.getBlockPosition().shift((EnumDirection) isourceblock.getBlockData().get(BlockDispenser.FACING));

            this.a(a((WorldServer) worldserver, blockposition) || b((WorldServer) worldserver, blockposition, bukkitBlock, craftItem)); // CraftBukkit
            if (this.a() && itemstack.isDamaged(1, worldserver.getRandom(), (EntityPlayer) null)) {
                itemstack.setCount(0);
            }
        }

        return itemstack;
    }

    private static boolean a(WorldServer worldserver, BlockPosition blockposition) {
        IBlockData iblockdata = worldserver.getType(blockposition);

        if (iblockdata.a((Tag) TagsBlock.BEEHIVES)) {
            int i = (Integer) iblockdata.get(BlockBeehive.HONEY_LEVEL);

            if (i >= 5) {
                worldserver.playSound((EntityHuman) null, blockposition, SoundEffects.BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
                BlockBeehive.a((World) worldserver, blockposition);
                ((BlockBeehive) iblockdata.getBlock()).a((World) worldserver, iblockdata, blockposition, (EntityHuman) null, TileEntityBeehive.ReleaseStatus.BEE_RELEASED);
                worldserver.a((Entity) null, GameEvent.SHEAR, blockposition);
                return true;
            }
        }

        return false;
    }

    private static boolean b(WorldServer worldserver, BlockPosition blockposition, org.bukkit.block.Block bukkitBlock, CraftItemStack craftItem) { // CraftBukkit - add args
        List<EntityLiving> list = worldserver.a(EntityLiving.class, new AxisAlignedBB(blockposition), IEntitySelector.NO_SPECTATORS);
        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            EntityLiving entityliving = (EntityLiving) iterator.next();

            if (entityliving instanceof IShearable) {
                IShearable ishearable = (IShearable) entityliving;

                if (ishearable.canShear()) {
                    // CraftBukkit start
                    if (CraftEventFactory.callBlockShearEntityEvent(entityliving, bukkitBlock, craftItem).isCancelled()) {
                        continue;
                    }
                    // CraftBukkit end
                    ishearable.shear(SoundCategory.BLOCKS);
                    worldserver.a((Entity) null, GameEvent.SHEAR, blockposition);
                    return true;
                }
            }
        }

        return false;
    }
}
