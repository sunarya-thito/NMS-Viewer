package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.EntityBat;
import net.minecraft.world.entity.animal.EntityTurtle;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
// CraftBukkit end

public class BlockTurtleEgg extends Block {

    public static final int MAX_HATCH_LEVEL = 2;
    public static final int MIN_EGGS = 1;
    public static final int MAX_EGGS = 4;
    private static final VoxelShape ONE_EGG_AABB = Block.a(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
    private static final VoxelShape MULTIPLE_EGGS_AABB = Block.a(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    public static final BlockStateInteger HATCH = BlockProperties.HATCH;
    public static final BlockStateInteger EGGS = BlockProperties.EGGS;

    public BlockTurtleEgg(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockTurtleEgg.HATCH, 0)).set(BlockTurtleEgg.EGGS, 1));
    }

    @Override
    public void stepOn(World world, BlockPosition blockposition, IBlockData iblockdata, Entity entity) {
        this.a(world, iblockdata, blockposition, entity, 100);
        super.stepOn(world, blockposition, iblockdata, entity);
    }

    @Override
    public void fallOn(World world, IBlockData iblockdata, BlockPosition blockposition, Entity entity, float f) {
        if (!(entity instanceof EntityZombie)) {
            this.a(world, iblockdata, blockposition, entity, 3);
        }

        super.fallOn(world, iblockdata, blockposition, entity, f);
    }

    private void a(World world, IBlockData iblockdata, BlockPosition blockposition, Entity entity, int i) {
        if (this.a(world, entity)) {
            if (!world.isClientSide && world.random.nextInt(i) == 0 && iblockdata.a(Blocks.TURTLE_EGG)) {
                // CraftBukkit start - Step on eggs
                org.bukkit.event.Cancellable cancellable;
                if (entity instanceof EntityHuman) {
                    cancellable = CraftEventFactory.callPlayerInteractEvent((EntityHuman) entity, org.bukkit.event.block.Action.PHYSICAL, blockposition, null, null, null);
                } else {
                    cancellable = new EntityInteractEvent(entity.getBukkitEntity(), CraftBlock.at(world, blockposition));
                    world.getCraftServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
                }

                if (cancellable.isCancelled()) {
                    return;
                }
                // CraftBukkit end
                this.a(world, blockposition, iblockdata);
            }

        }
    }

    private void a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        world.playSound((EntityHuman) null, blockposition, SoundEffects.TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.random.nextFloat() * 0.2F);
        int i = (Integer) iblockdata.get(BlockTurtleEgg.EGGS);

        if (i <= 1) {
            world.b(blockposition, false);
        } else {
            world.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockTurtleEgg.EGGS, i - 1), 2);
            world.triggerEffect(2001, blockposition, Block.getCombinedId(iblockdata));
        }

    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (this.a((World) worldserver) && a((IBlockAccess) worldserver, blockposition)) {
            int i = (Integer) iblockdata.get(BlockTurtleEgg.HATCH);

            if (i < 2) {
                // CraftBukkit start - Call BlockGrowEvent
                if (!CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition, iblockdata.set(BlockTurtleEgg.HATCH, i + 1), 2)) {
                    return;
                }
                // CraftBukkit end
                worldserver.playSound((EntityHuman) null, blockposition, SoundEffects.TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                // worldserver.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockTurtleEgg.a, i + 1), 2); // CraftBukkit - handled above
            } else {
                // CraftBukkit start - Call BlockFadeEvent
                if (CraftEventFactory.callBlockFadeEvent(worldserver, blockposition, Blocks.AIR.getBlockData()).isCancelled()) {
                    return;
                }
                // CraftBukkit end
                worldserver.playSound((EntityHuman) null, blockposition, SoundEffects.TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
                worldserver.a(blockposition, false);

                for (int j = 0; j < (Integer) iblockdata.get(BlockTurtleEgg.EGGS); ++j) {
                    worldserver.triggerEffect(2001, blockposition, Block.getCombinedId(iblockdata));
                    EntityTurtle entityturtle = (EntityTurtle) EntityTypes.TURTLE.a((World) worldserver);

                    entityturtle.setAgeRaw(-24000);
                    entityturtle.setHomePos(blockposition);
                    entityturtle.setPositionRotation((double) blockposition.getX() + 0.3D + (double) j * 0.2D, (double) blockposition.getY(), (double) blockposition.getZ() + 0.3D, 0.0F, 0.0F);
                    worldserver.addEntity(entityturtle, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.EGG); // CraftBukkit
                }
            }
        }

    }

    public static boolean a(IBlockAccess iblockaccess, BlockPosition blockposition) {
        return b(iblockaccess, blockposition.down());
    }

    public static boolean b(IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockaccess.getType(blockposition).a((Tag) TagsBlock.SAND);
    }

    @Override
    public void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        if (a((IBlockAccess) world, blockposition) && !world.isClientSide) {
            world.triggerEffect(2005, blockposition, 0);
        }

    }

    private boolean a(World world) {
        float f = world.f(1.0F);

        return (double) f < 0.69D && (double) f > 0.65D ? true : world.random.nextInt(500) == 0;
    }

    @Override
    public void a(World world, EntityHuman entityhuman, BlockPosition blockposition, IBlockData iblockdata, @Nullable TileEntity tileentity, ItemStack itemstack) {
        super.a(world, entityhuman, blockposition, iblockdata, tileentity, itemstack);
        this.a(world, blockposition, iblockdata);
    }

    @Override
    public boolean a(IBlockData iblockdata, BlockActionContext blockactioncontext) {
        return !blockactioncontext.isSneaking() && blockactioncontext.getItemStack().a(this.getItem()) && (Integer) iblockdata.get(BlockTurtleEgg.EGGS) < 4 ? true : super.a(iblockdata, blockactioncontext);
    }

    @Nullable
    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        IBlockData iblockdata = blockactioncontext.getWorld().getType(blockactioncontext.getClickPosition());

        return iblockdata.a((Block) this) ? (IBlockData) iblockdata.set(BlockTurtleEgg.EGGS, Math.min(4, (Integer) iblockdata.get(BlockTurtleEgg.EGGS) + 1)) : super.getPlacedState(blockactioncontext);
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return (Integer) iblockdata.get(BlockTurtleEgg.EGGS) > 1 ? BlockTurtleEgg.MULTIPLE_EGGS_AABB : BlockTurtleEgg.ONE_EGG_AABB;
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockTurtleEgg.HATCH, BlockTurtleEgg.EGGS);
    }

    private boolean a(World world, Entity entity) {
        return !(entity instanceof EntityTurtle) && !(entity instanceof EntityBat) ? (!(entity instanceof EntityLiving) ? false : entity instanceof EntityHuman || world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) : false;
    }
}
