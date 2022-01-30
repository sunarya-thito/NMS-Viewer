package net.minecraft.world.level.block;

import java.util.Iterator;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsEntity;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.phys.MovingObjectPositionBlock;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class BlockChorusFlower extends Block {

    public static final int DEAD_AGE = 5;
    public static final BlockStateInteger AGE = BlockProperties.AGE_5;
    private final BlockChorusFruit plant;

    protected BlockChorusFlower(BlockChorusFruit blockchorusfruit, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.plant = blockchorusfruit;
        this.k((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockChorusFlower.AGE, 0));
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!iblockdata.canPlace(worldserver, blockposition)) {
            worldserver.b(blockposition, true);
        }

    }

    @Override
    public boolean isTicking(IBlockData iblockdata) {
        return (Integer) iblockdata.get(BlockChorusFlower.AGE) < 5;
    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        BlockPosition blockposition1 = blockposition.up();

        if (worldserver.isEmpty(blockposition1) && blockposition1.getY() < worldserver.getMaxBuildHeight()) {
            int i = (Integer) iblockdata.get(BlockChorusFlower.AGE);

            if (i < 5) {
                boolean flag = false;
                boolean flag1 = false;
                IBlockData iblockdata1 = worldserver.getType(blockposition.down());
                int j;

                if (iblockdata1.a(Blocks.END_STONE)) {
                    flag = true;
                } else if (iblockdata1.a((Block) this.plant)) {
                    j = 1;

                    for (int k = 0; k < 4; ++k) {
                        IBlockData iblockdata2 = worldserver.getType(blockposition.down(j + 1));

                        if (!iblockdata2.a((Block) this.plant)) {
                            if (iblockdata2.a(Blocks.END_STONE)) {
                                flag1 = true;
                            }
                            break;
                        }

                        ++j;
                    }

                    if (j < 2 || j <= random.nextInt(flag1 ? 5 : 4)) {
                        flag = true;
                    }
                } else if (iblockdata1.isAir()) {
                    flag = true;
                }

                if (flag && b((IWorldReader) worldserver, blockposition1, (EnumDirection) null) && worldserver.isEmpty(blockposition.up(2))) {
                    // CraftBukkit start - add event
                    if (CraftEventFactory.handleBlockSpreadEvent(worldserver, blockposition, blockposition1, this.getBlockData().set(BlockChorusFlower.AGE, Integer.valueOf(i)), 2)) {
                        worldserver.setTypeAndData(blockposition, this.plant.a((IBlockAccess) worldserver, blockposition), 2);
                        this.a(worldserver, blockposition1, i);
                    }
                    // CraftBukkit end
                } else if (i < 4) {
                    j = random.nextInt(4);
                    if (flag1) {
                        ++j;
                    }

                    boolean flag2 = false;

                    for (int l = 0; l < j; ++l) {
                        EnumDirection enumdirection = EnumDirection.EnumDirectionLimit.HORIZONTAL.a(random);
                        BlockPosition blockposition2 = blockposition.shift(enumdirection);

                        if (worldserver.isEmpty(blockposition2) && worldserver.isEmpty(blockposition2.down()) && b((IWorldReader) worldserver, blockposition2, enumdirection.opposite())) {
                            // CraftBukkit start - add event
                            if (CraftEventFactory.handleBlockSpreadEvent(worldserver, blockposition, blockposition2, this.getBlockData().set(BlockChorusFlower.AGE, Integer.valueOf(i + 1)), 2)) {
                                this.a(worldserver, blockposition2, i + 1);
                                flag2 = true;
                            }
                            // CraftBukkit end
                        }
                    }

                    if (flag2) {
                        worldserver.setTypeAndData(blockposition, this.plant.a((IBlockAccess) worldserver, blockposition), 2);
                    } else {
                        // CraftBukkit - add event
                        if (CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition, this.getBlockData().set(BlockChorusFlower.AGE, Integer.valueOf(5)), 2)) {
                            this.a((World) worldserver, blockposition);
                        }
                        // CraftBukkit end
                    }
                } else {
                    // CraftBukkit - add event
                    if (CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition, this.getBlockData().set(BlockChorusFlower.AGE, Integer.valueOf(5)), 2)) {
                        this.a((World) worldserver, blockposition);
                    }
                    // CraftBukkit end
                }

            }
        }
    }

    private void a(World world, BlockPosition blockposition, int i) {
        world.setTypeAndData(blockposition, (IBlockData) this.getBlockData().set(BlockChorusFlower.AGE, i), 2);
        world.triggerEffect(1033, blockposition, 0);
    }

    private void a(World world, BlockPosition blockposition) {
        world.setTypeAndData(blockposition, (IBlockData) this.getBlockData().set(BlockChorusFlower.AGE, 5), 2);
        world.triggerEffect(1034, blockposition, 0);
    }

    private static boolean b(IWorldReader iworldreader, BlockPosition blockposition, @Nullable EnumDirection enumdirection) {
        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

        EnumDirection enumdirection1;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            enumdirection1 = (EnumDirection) iterator.next();
        } while (enumdirection1 == enumdirection || iworldreader.isEmpty(blockposition.shift(enumdirection1)));

        return false;
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (enumdirection != EnumDirection.UP && !iblockdata.canPlace(generatoraccess, blockposition)) {
            generatoraccess.getBlockTickList().a(blockposition, this, 1);
        }

        return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
    }

    @Override
    public boolean canPlace(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        IBlockData iblockdata1 = iworldreader.getType(blockposition.down());

        if (!iblockdata1.a((Block) this.plant) && !iblockdata1.a(Blocks.END_STONE)) {
            if (!iblockdata1.isAir()) {
                return false;
            } else {
                boolean flag = false;
                Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

                while (iterator.hasNext()) {
                    EnumDirection enumdirection = (EnumDirection) iterator.next();
                    IBlockData iblockdata2 = iworldreader.getType(blockposition.shift(enumdirection));

                    if (iblockdata2.a((Block) this.plant)) {
                        if (flag) {
                            return false;
                        }

                        flag = true;
                    } else if (!iblockdata2.isAir()) {
                        return false;
                    }
                }

                return flag;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockChorusFlower.AGE);
    }

    public static void a(GeneratorAccess generatoraccess, BlockPosition blockposition, Random random, int i) {
        generatoraccess.setTypeAndData(blockposition, ((BlockChorusFruit) Blocks.CHORUS_PLANT).a((IBlockAccess) generatoraccess, blockposition), 2);
        a(generatoraccess, blockposition, random, blockposition, i, 0);
    }

    private static void a(GeneratorAccess generatoraccess, BlockPosition blockposition, Random random, BlockPosition blockposition1, int i, int j) {
        BlockChorusFruit blockchorusfruit = (BlockChorusFruit) Blocks.CHORUS_PLANT;
        int k = random.nextInt(4) + 1;

        if (j == 0) {
            ++k;
        }

        for (int l = 0; l < k; ++l) {
            BlockPosition blockposition2 = blockposition.up(l + 1);

            if (!b((IWorldReader) generatoraccess, blockposition2, (EnumDirection) null)) {
                return;
            }

            generatoraccess.setTypeAndData(blockposition2, blockchorusfruit.a((IBlockAccess) generatoraccess, blockposition2), 2);
            generatoraccess.setTypeAndData(blockposition2.down(), blockchorusfruit.a((IBlockAccess) generatoraccess, blockposition2.down()), 2);
        }

        boolean flag = false;

        if (j < 4) {
            int i1 = random.nextInt(4);

            if (j == 0) {
                ++i1;
            }

            for (int j1 = 0; j1 < i1; ++j1) {
                EnumDirection enumdirection = EnumDirection.EnumDirectionLimit.HORIZONTAL.a(random);
                BlockPosition blockposition3 = blockposition.up(k).shift(enumdirection);

                if (Math.abs(blockposition3.getX() - blockposition1.getX()) < i && Math.abs(blockposition3.getZ() - blockposition1.getZ()) < i && generatoraccess.isEmpty(blockposition3) && generatoraccess.isEmpty(blockposition3.down()) && b((IWorldReader) generatoraccess, blockposition3, enumdirection.opposite())) {
                    flag = true;
                    generatoraccess.setTypeAndData(blockposition3, blockchorusfruit.a((IBlockAccess) generatoraccess, blockposition3), 2);
                    generatoraccess.setTypeAndData(blockposition3.shift(enumdirection.opposite()), blockchorusfruit.a((IBlockAccess) generatoraccess, blockposition3.shift(enumdirection.opposite())), 2);
                    a(generatoraccess, blockposition3, random, blockposition1, i, j + 1);
                }
            }
        }

        if (!flag) {
            generatoraccess.setTypeAndData(blockposition.up(k), (IBlockData) Blocks.CHORUS_FLOWER.getBlockData().set(BlockChorusFlower.AGE, 5), 2);
        }

    }

    @Override
    public void a(World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, IProjectile iprojectile) {
        BlockPosition blockposition = movingobjectpositionblock.getBlockPosition();

        if (!world.isClientSide && iprojectile.a(world, blockposition) && iprojectile.getEntityType().a((Tag) TagsEntity.IMPACT_PROJECTILES)) {
            world.a(blockposition, true, iprojectile);
        }

    }
}
