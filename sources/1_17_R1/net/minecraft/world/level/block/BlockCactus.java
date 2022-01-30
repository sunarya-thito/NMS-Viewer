package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class BlockCactus extends Block {

    public static final BlockStateInteger AGE = BlockProperties.AGE_15;
    public static final int MAX_AGE = 15;
    protected static final int AABB_OFFSET = 1;
    protected static final VoxelShape COLLISION_SHAPE = Block.a(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);
    protected static final VoxelShape OUTLINE_SHAPE = Block.a(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    protected BlockCactus(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockCactus.AGE, 0));
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!iblockdata.canPlace(worldserver, blockposition)) {
            worldserver.b(blockposition, true);
        }

    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        BlockPosition blockposition1 = blockposition.up();

        if (worldserver.isEmpty(blockposition1)) {
            int i;

            for (i = 1; worldserver.getType(blockposition.down(i)).a((Block) this); ++i) {
                ;
            }

            if (i < 3) {
                int j = (Integer) iblockdata.get(BlockCactus.AGE);

                if (j >= (byte) range(3, ((100.0F / worldserver.spigotConfig.cactusModifier) * 15) + 0.5F, 15)) { // Spigot
                    CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition1, this.getBlockData()); // CraftBukkit
                    IBlockData iblockdata1 = (IBlockData) iblockdata.set(BlockCactus.AGE, 0);

                    worldserver.setTypeAndData(blockposition, iblockdata1, 4);
                    iblockdata1.doPhysics(worldserver, blockposition1, this, blockposition, false);
                } else {
                    worldserver.setTypeAndData(blockposition, (IBlockData) iblockdata.set(BlockCactus.AGE, j + 1), 4);
                }

            }
        }
    }

    @Override
    public VoxelShape c(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCactus.COLLISION_SHAPE;
    }

    @Override
    public VoxelShape a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return BlockCactus.OUTLINE_SHAPE;
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (!iblockdata.canPlace(generatoraccess, blockposition)) {
            generatoraccess.getBlockTickList().a(blockposition, this, 1);
        }

        return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

        EnumDirection enumdirection;
        Material material;

        do {
            if (!iterator.hasNext()) {
                IBlockData iblockdata1 = iworldreader.getType(blockposition.down());

                return (iblockdata1.a(Blocks.CACTUS) || iblockdata1.a(Blocks.SAND) || iblockdata1.a(Blocks.RED_SAND)) && !iworldreader.getType(blockposition.up()).getMaterial().isLiquid();
            }

            enumdirection = (EnumDirection) iterator.next();
            IBlockData iblockdata2 = iworldreader.getType(blockposition.shift(enumdirection));

            material = iblockdata2.getMaterial();
        } while (!material.isBuildable() && !iworldreader.getFluid(blockposition.shift(enumdirection)).a((Tag) TagsFluid.LAVA));

        return false;
    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity) {
        CraftEventFactory.blockDamage = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()); // CraftBukkit
        entity.damageEntity(DamageSource.CACTUS, 1.0F);
        CraftEventFactory.blockDamage = null; // CraftBukkit
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockCactus.AGE);
    }

    @Override
    public boolean a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, PathMode pathmode) {
        return false;
    }
}
