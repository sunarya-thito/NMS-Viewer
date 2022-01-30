package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockCake extends Block {

    public static final int MAX_BITES = 6;
    public static final BlockStateInteger BITES = BlockProperties.BITES;
    public static final int FULL_CAKE_SIGNAL = b(0);
    protected static final float AABB_OFFSET = 1.0F;
    protected static final float AABB_SIZE_PER_BITE = 2.0F;
    protected static final VoxelShape[] SHAPE_BY_BITE = new VoxelShape[]{Block.a(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(3.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(5.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(7.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(9.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(11.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D), Block.a(13.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D)};

    protected BlockCake(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockCake.BITES, 0));
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCake.SHAPE_BY_BITE[(Integer) iblockdata.get(BlockCake.BITES)];
    }

    @Override
    public EnumInteractionResult interact(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        ItemStack itemstack = entityhuman.b(enumhand);
        Item item = itemstack.getItem();

        if (itemstack.a((Tag) TagsItem.CANDLES) && (Integer) iblockdata.get(BlockCake.BITES) == 0) {
            Block block = Block.asBlock(item);

            if (block instanceof CandleBlock) {
                if (!entityhuman.isCreative()) {
                    itemstack.subtract(1);
                }

                world.playSound((EntityHuman) null, blockposition, SoundEffects.CAKE_ADD_CANDLE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.setTypeUpdate(blockposition, CandleCakeBlock.a(block));
                world.a((Entity) entityhuman, GameEvent.BLOCK_CHANGE, blockposition);
                entityhuman.b(StatisticList.ITEM_USED.b(item));
                return EnumInteractionResult.SUCCESS;
            }
        }

        if (world.isClientSide) {
            if (a((GeneratorAccess) world, blockposition, iblockdata, entityhuman).a()) {
                return EnumInteractionResult.SUCCESS;
            }

            if (itemstack.isEmpty()) {
                return EnumInteractionResult.CONSUME;
            }
        }

        return a((GeneratorAccess) world, blockposition, iblockdata, entityhuman);
    }

    protected static EnumInteractionResult a(GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata, EntityHuman entityhuman) {
        if (!entityhuman.s(false)) {
            return EnumInteractionResult.PASS;
        } else {
            entityhuman.a(StatisticList.EAT_CAKE_SLICE);
            // CraftBukkit start
            // entityhuman.getFoodData().eat(2, 0.1F);
            int oldFoodLevel = entityhuman.getFoodData().foodLevel;

            org.bukkit.event.entity.FoodLevelChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callFoodLevelChangeEvent(entityhuman, 2 + oldFoodLevel);

            if (!event.isCancelled()) {
                entityhuman.getFoodData().eat(event.getFoodLevel() - oldFoodLevel, 0.1F);
            }

            ((net.minecraft.server.level.EntityPlayer) entityhuman).getBukkitEntity().sendHealthUpdate();
            // CraftBukkit end
            int i = (Integer) iblockdata.get(BlockCake.BITES);

            generatoraccess.a((Entity) entityhuman, GameEvent.EAT, blockposition);
            if (i < 6) {
                generatoraccess.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockCake.BITES, i + 1), 3);
            } else {
                generatoraccess.a(blockposition, false);
                generatoraccess.a((Entity) entityhuman, GameEvent.BLOCK_DESTROY, blockposition);
            }

            return EnumInteractionResult.SUCCESS;
        }
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        return enumdirection == EnumDirection.DOWN && !iblockdata.canPlace(generatoraccess, blockposition) ? Blocks.AIR.getBlockData() : super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getType(blockposition.down()).getMaterial().isBuildable();
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockCake.BITES);
    }

    @Override
    public int a(IBlockData iblockdata, World world, BlockPosition blockposition) {
        return b((Integer) iblockdata.get(BlockCake.BITES));
    }

    public static int b(int i) {
        return (7 - i) * 2;
    }

    @Override
    public boolean isComplexRedstone(IBlockData iblockdata) {
        return true;
    }

    @Override
    public boolean a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        return false;
    }
}
