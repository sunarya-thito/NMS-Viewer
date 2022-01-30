package net.minecraft.world.level.block;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.core.Position;
import net.minecraft.core.SourceBlock;
import net.minecraft.core.dispenser.DispenseBehaviorItem;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.entity.TileEntityDropper;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPositionBlock;

public class BlockDispenser extends BlockTileEntity {

    public static final BlockStateDirection FACING = BlockDirectional.FACING;
    public static final BlockStateBoolean TRIGGERED = BlockProperties.TRIGGERED;
    public static final Map<Item, IDispenseBehavior> DISPENSER_REGISTRY = (Map) SystemUtils.a((new Object2ObjectOpenHashMap()), (object2objectopenhashmap) -> { // CraftBukkit - decompile error
        object2objectopenhashmap.defaultReturnValue(new DispenseBehaviorItem());
    });
    private static final int TRIGGER_DURATION = 4;
    public static boolean eventFired = false; // CraftBukkit

    public static void a(IMaterial imaterial, IDispenseBehavior idispensebehavior) {
        BlockDispenser.DISPENSER_REGISTRY.put(imaterial.getItem(), idispensebehavior);
    }

    protected BlockDispenser(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockDispenser.FACING, EnumDirection.NORTH)).set(BlockDispenser.TRIGGERED, false));
    }

    @Override
    public EnumInteractionResult interact(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        if (world.isClientSide) {
            return EnumInteractionResult.SUCCESS;
        } else {
            TileEntity tileentity = world.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityDispenser) {
                entityhuman.openContainer((TileEntityDispenser) tileentity);
                if (tileentity instanceof TileEntityDropper) {
                    entityhuman.a(StatisticList.INSPECT_DROPPER);
                } else {
                    entityhuman.a(StatisticList.INSPECT_DISPENSER);
                }
            }

            return EnumInteractionResult.CONSUME;
        }
    }

    public void dispense(WorldServer worldserver, BlockPosition blockposition) {
        SourceBlock sourceblock = new SourceBlock(worldserver, blockposition);
        TileEntityDispenser tileentitydispenser = (TileEntityDispenser) sourceblock.getTileEntity();
        int i = tileentitydispenser.h();

        if (i < 0) {
            worldserver.triggerEffect(1001, blockposition, 0);
            worldserver.a(GameEvent.DISPENSE_FAIL, blockposition);
        } else {
            ItemStack itemstack = tileentitydispenser.getItem(i);
            IDispenseBehavior idispensebehavior = this.a(itemstack);

            if (idispensebehavior != IDispenseBehavior.NOOP) {
                eventFired = false; // CraftBukkit - reset event status
                tileentitydispenser.setItem(i, idispensebehavior.dispense(sourceblock, itemstack));
            }

        }
    }

    protected IDispenseBehavior a(ItemStack itemstack) {
        return (IDispenseBehavior) BlockDispenser.DISPENSER_REGISTRY.get(itemstack.getItem());
    }

    @Override
    public void doPhysics(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition blockposition1, boolean flag) {
        boolean flag1 = world.isBlockIndirectlyPowered(blockposition) || world.isBlockIndirectlyPowered(blockposition.up());
        boolean flag2 = (Boolean) iblockdata.get(BlockDispenser.TRIGGERED);

        if (flag1 && !flag2) {
            world.getBlockTickList().a(blockposition, this, 4);
            world.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockDispenser.TRIGGERED, true), 4);
        } else if (!flag1 && flag2) {
            world.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockDispenser.TRIGGERED, false), 4);
        }

    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        this.dispense(worldserver, blockposition);
    }

    @Override
    public TileEntity createTile(BlockPosition blockposition, IBlockData iblockdata) {
        return new TileEntityDispenser(blockposition, iblockdata);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        return (IBlockData) this.getBlockData().set(BlockDispenser.FACING, blockactioncontext.d().opposite());
    }

    @Override
    public void postPlace(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        if (itemstack.hasName()) {
            TileEntity tileentity = world.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityDispenser) {
                ((TileEntityDispenser) tileentity).setCustomName(itemstack.getName());
            }
        }

    }

    @Override
    public void remove(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        if (!iblockdata.a(iblockdata1.getBlock())) {
            TileEntity tileentity = world.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityDispenser) {
                InventoryUtils.dropInventory(world, blockposition, (TileEntityDispenser) tileentity);
                world.updateAdjacentComparators(blockposition, this);
            }

            super.remove(iblockdata, world, blockposition, iblockdata1, flag);
        }
    }

    public static IPosition a(ISourceBlock isourceblock) {
        EnumDirection enumdirection = (EnumDirection) isourceblock.getBlockData().get(BlockDispenser.FACING);
        double d0 = isourceblock.getX() + 0.7D * (double) enumdirection.getAdjacentX();
        double d1 = isourceblock.getY() + 0.7D * (double) enumdirection.getAdjacentY();
        double d2 = isourceblock.getZ() + 0.7D * (double) enumdirection.getAdjacentZ();

        return new Position(d0, d1, d2);
    }

    @Override
    public boolean isComplexRedstone(IBlockData iblockdata) {
        return true;
    }

    @Override
    public int a(IBlockData iblockdata, World world, BlockPosition blockposition) {
        return Container.a(world.getTileEntity(blockposition));
    }

    @Override
    public EnumRenderType b_(IBlockData iblockdata) {
        return EnumRenderType.MODEL;
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.set(BlockDispenser.FACING, enumblockrotation.a((EnumDirection) iblockdata.get(BlockDispenser.FACING)));
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.a(enumblockmirror.a((EnumDirection) iblockdata.get(BlockDispenser.FACING)));
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockDispenser.FACING, BlockDispenser.TRIGGERED);
    }
}
