package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSword;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

public class BlockBambooSapling extends Block implements IBlockFragilePlantElement {

    protected static final float SAPLING_AABB_OFFSET = 4.0F;
    protected static final VoxelShape SAPLING_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D);

    public BlockBambooSapling(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public BlockBase.EnumRandomOffset getOffsetType() {
        return BlockBase.EnumRandomOffset.XZ;
    }

    @Override
    public VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        Vec3D vec3d = iblockdata.getOffset(iblockaccess, blockposition);

        return BlockBambooSapling.SAPLING_SHAPE.move(vec3d.x, vec3d.y, vec3d.z);
    }

    @Override
    public void randomTick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (random.nextInt(3) == 0 && worldserver.isEmptyBlock(blockposition.above()) && worldserver.getRawBrightness(blockposition.above(), 0) >= 9) {
            this.growBamboo(worldserver, blockposition);
        }

    }

    @Override
    public boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return iworldreader.getBlockState(blockposition.below()).is((Tag) TagsBlock.BAMBOO_PLANTABLE_ON);
    }

    @Override
    public IBlockData updateShape(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (!iblockdata.canSurvive(generatoraccess, blockposition)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (enumdirection == EnumDirection.UP && iblockdata1.is(Blocks.BAMBOO)) {
                generatoraccess.setBlock(blockposition, Blocks.BAMBOO.defaultBlockState(), 2);
            }

            return super.updateShape(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
        }
    }

    @Override
    public ItemStack getCloneItemStack(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata) {
        return new ItemStack(Items.BAMBOO);
    }

    @Override
    public boolean isValidBonemealTarget(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return iblockaccess.getBlockState(blockposition.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void performBonemeal(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        this.growBamboo(worldserver, blockposition);
    }

    @Override
    public float getDestroyProgress(IBlockData iblockdata, EntityHuman entityhuman, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return entityhuman.getMainHandItem().getItem() instanceof ItemSword ? 1.0F : super.getDestroyProgress(iblockdata, entityhuman, iblockaccess, blockposition);
    }

    protected void growBamboo(World world, BlockPosition blockposition) {
        org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockSpreadEvent(world, blockposition, blockposition.above(), (IBlockData) Blocks.BAMBOO.defaultBlockState().setValue(BlockBamboo.LEAVES, BlockPropertyBambooSize.SMALL), 3); // CraftBukkit - BlockSpreadEvent
    }
}
