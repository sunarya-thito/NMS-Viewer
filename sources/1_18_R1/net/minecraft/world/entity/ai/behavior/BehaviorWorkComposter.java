package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BlockComposter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorWorkComposter extends BehaviorWork {

    private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

    public BehaviorWorkComposter() {}

    @Override
    protected void useWorkstation(WorldServer worldserver, EntityVillager entityvillager) {
        Optional<GlobalPos> optional = entityvillager.getBrain().getMemory(MemoryModuleType.JOB_SITE);

        if (optional.isPresent()) {
            GlobalPos globalpos = (GlobalPos) optional.get();
            IBlockData iblockdata = worldserver.getBlockState(globalpos.pos());

            if (iblockdata.is(Blocks.COMPOSTER)) {
                this.makeBread(entityvillager);
                this.compostItems(worldserver, entityvillager, globalpos, iblockdata);
            }

        }
    }

    private void compostItems(WorldServer worldserver, EntityVillager entityvillager, GlobalPos globalpos, IBlockData iblockdata) {
        BlockPosition blockposition = globalpos.pos();

        if ((Integer) iblockdata.getValue(BlockComposter.LEVEL) == 8) {
            iblockdata = BlockComposter.extractProduce(iblockdata, worldserver, blockposition, entityvillager); // CraftBukkit
        }

        int i = 20;
        boolean flag = true;
        int[] aint = new int[BehaviorWorkComposter.COMPOSTABLE_ITEMS.size()];
        InventorySubcontainer inventorysubcontainer = entityvillager.getInventory();
        int j = inventorysubcontainer.getContainerSize();
        IBlockData iblockdata1 = iblockdata;

        for (int k = j - 1; k >= 0 && i > 0; --k) {
            ItemStack itemstack = inventorysubcontainer.getItem(k);
            int l = BehaviorWorkComposter.COMPOSTABLE_ITEMS.indexOf(itemstack.getItem());

            if (l != -1) {
                int i1 = itemstack.getCount();
                int j1 = aint[l] + i1;

                aint[l] = j1;
                int k1 = Math.min(Math.min(j1 - 10, i), i1);

                if (k1 > 0) {
                    i -= k1;

                    for (int l1 = 0; l1 < k1; ++l1) {
                        iblockdata1 = BlockComposter.insertItem(iblockdata1, worldserver, itemstack, blockposition, entityvillager); // CraftBukkit
                        if ((Integer) iblockdata1.getValue(BlockComposter.LEVEL) == 7) {
                            this.spawnComposterFillEffects(worldserver, iblockdata, blockposition, iblockdata1);
                            return;
                        }
                    }
                }
            }
        }

        this.spawnComposterFillEffects(worldserver, iblockdata, blockposition, iblockdata1);
    }

    private void spawnComposterFillEffects(WorldServer worldserver, IBlockData iblockdata, BlockPosition blockposition, IBlockData iblockdata1) {
        worldserver.levelEvent(1500, blockposition, iblockdata1 != iblockdata ? 1 : 0);
    }

    private void makeBread(EntityVillager entityvillager) {
        InventorySubcontainer inventorysubcontainer = entityvillager.getInventory();

        if (inventorysubcontainer.countItem(Items.BREAD) <= 36) {
            int i = inventorysubcontainer.countItem(Items.WHEAT);
            boolean flag = true;
            boolean flag1 = true;
            int j = Math.min(3, i / 3);

            if (j != 0) {
                int k = j * 3;

                inventorysubcontainer.removeItemType(Items.WHEAT, k);
                ItemStack itemstack = inventorysubcontainer.addItem(new ItemStack(Items.BREAD, j));

                if (!itemstack.isEmpty()) {
                    entityvillager.spawnAtLocation(itemstack, 0.5F);
                }

            }
        }
    }
}
