package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;

public class BlockCoralFan extends BlockCoralFanAbstract {

    private final Block deadBlock;

    protected BlockCoralFan(Block block, BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.deadBlock = block;
    }

    @Override
    public void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        this.tryScheduleDieTick(iblockdata, world, blockposition);
    }

    @Override
    public void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        if (!scanForWater(iblockdata, worldserver, blockposition)) {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callBlockFadeEvent(worldserver, blockposition, this.deadBlock.defaultBlockState().setValue(BlockCoralFan.WATERLOGGED, false)).isCancelled()) {
                return;
            }
            // CraftBukkit end
            worldserver.setBlock(blockposition, (IBlockData) this.deadBlock.defaultBlockState().setValue(BlockCoralFan.WATERLOGGED, false), 2);
        }

    }

    @Override
    public IBlockData updateShape(IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1) {
        if (enumdirection == EnumDirection.DOWN && !iblockdata.canSurvive(generatoraccess, blockposition)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            this.tryScheduleDieTick(iblockdata, generatoraccess, blockposition);
            if ((Boolean) iblockdata.getValue(BlockCoralFan.WATERLOGGED)) {
                generatoraccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(generatoraccess));
            }

            return super.updateShape(iblockdata, enumdirection, iblockdata1, generatoraccess, blockposition, blockposition1);
        }
    }
}
