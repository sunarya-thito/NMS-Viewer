package net.minecraft.world.level.block;

import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

// CraftBukkit start
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.BlockFormEvent;
// CraftBukkit end

public class BlockConcretePowder extends BlockFalling {

    private final IBlockData concrete;

    public BlockConcretePowder(Block block, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.concrete = block.getBlockData();
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1, EntityFallingBlock entityfallingblock) {
        if (canHarden(world, blockposition, iblockdata1)) {
            org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockFormEvent(world, blockposition, this.concrete, 3); // CraftBukkit
        }

    }

    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        World world = blockactioncontext.getWorld();
        BlockPosition blockposition = blockactioncontext.getClickPosition();
        IBlockData iblockdata = world.getType(blockposition);

        // CraftBukkit start
        if (!canHarden(world, blockposition, iblockdata)) {
            return super.getPlacedState(blockactioncontext);
        }

        // TODO: An event factory call for methods like this
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, blockposition);
        blockState.setData(this.concrete);

        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        world.getMinecraftServer().server.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            return blockState.getHandle();
        }

        return super.getPlacedState(blockactioncontext);
        // CraftBukkit end
    }

    private static boolean canHarden(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata) {
        return n(iblockdata) || a(iblockaccess, blockposition);
    }

    private static boolean a(IBlockAccess iblockaccess, BlockPosition blockposition) {
        boolean flag = false;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.i();
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;

        for (int j = 0; j < i; ++j) {
            EnumDirection enumdirection = aenumdirection[j];
            IBlockData iblockdata = iblockaccess.getType(blockposition_mutableblockposition);

            if (enumdirection != EnumDirection.DOWN || n(iblockdata)) {
                blockposition_mutableblockposition.a((BaseBlockPosition) blockposition, enumdirection);
                iblockdata = iblockaccess.getType(blockposition_mutableblockposition);
                if (n(iblockdata) && !iblockdata.d(iblockaccess, blockposition, enumdirection.opposite())) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    private static boolean n(IBlockData iblockdata) {
        return iblockdata.getFluid().a((Tag) TagsFluid.WATER);
    }

    @Override
    public IBlockData updateState(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        // CraftBukkit start
        if (a((IBlockAccess) generatoraccess, blockposition)) {
            // Suppress during worldgen
            if (!(generatoraccess instanceof World)) {
                return this.concrete;
            }
            CraftBlockState blockState = CraftBlockStates.getBlockState(generatoraccess, blockposition);
            blockState.setData(this.concrete);

            BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
            ((World) generatoraccess).getCraftServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                return blockState.getHandle();
            }
        }

        return super.updateState(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
        // CraftBukkit end
    }

    @Override
    public int d(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return iblockdata.d(iblockaccess, blockposition).col;
    }
}
