package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.UtilColor;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.CommandBlockListenerAbstract;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateDirection;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit

public class BlockCommand extends BlockTileEntity implements GameMasterBlock {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final BlockStateDirection FACING = BlockDirectional.FACING;
    public static final BlockStateBoolean CONDITIONAL = BlockProperties.CONDITIONAL;
    private final boolean automatic;

    public BlockCommand(BlockBase.Info blockbase_info, boolean flag) {
        super(blockbase_info);
        this.k((IBlockData) ((IBlockData) ((IBlockData) this.stateDefinition.getBlockData()).set(BlockCommand.FACING, EnumDirection.NORTH)).set(BlockCommand.CONDITIONAL, false));
        this.automatic = flag;
    }

    @Override
    public TileEntity createTile(BlockPosition blockposition, IBlockData iblockdata) {
        TileEntityCommand tileentitycommand = new TileEntityCommand(blockposition, iblockdata);

        tileentitycommand.b(this.automatic);
        return tileentitycommand;
    }

    @Override
    public void doPhysics(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition blockposition1, boolean flag) {
        if (!world.isClientSide) {
            TileEntity tileentity = world.getTileEntity(blockposition);

            if (tileentity instanceof TileEntityCommand) {
                TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;
                boolean flag1 = world.isBlockIndirectlyPowered(blockposition);
                boolean flag2 = tileentitycommand.f();
                // CraftBukkit start
                org.bukkit.block.Block bukkitBlock = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
                int old = flag2 ? 15 : 0;
                int current = flag1 ? 15 : 0;

                BlockRedstoneEvent eventRedstone = new BlockRedstoneEvent(bukkitBlock, old, current);
                world.getCraftServer().getPluginManager().callEvent(eventRedstone);
                flag1 = eventRedstone.getNewCurrent() > 0;
                // CraftBukkit end

                tileentitycommand.a(flag1);
                if (!flag2 && !tileentitycommand.g() && tileentitycommand.t() != TileEntityCommand.Type.SEQUENCE) {
                    if (flag1) {
                        tileentitycommand.j();
                        world.getBlockTickList().a(blockposition, this, 1);
                    }

                }
            }
        }
    }

    @Override
    public void tickAlways(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random) {
        TileEntity tileentity = worldserver.getTileEntity(blockposition);

        if (tileentity instanceof TileEntityCommand) {
            TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;
            CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();
            boolean flag = !UtilColor.b(commandblocklistenerabstract.getCommand());
            TileEntityCommand.Type tileentitycommand_type = tileentitycommand.t();
            boolean flag1 = tileentitycommand.i();

            if (tileentitycommand_type == TileEntityCommand.Type.AUTO) {
                tileentitycommand.j();
                if (flag1) {
                    this.a(iblockdata, worldserver, blockposition, commandblocklistenerabstract, flag);
                } else if (tileentitycommand.u()) {
                    commandblocklistenerabstract.a(0);
                }

                if (tileentitycommand.f() || tileentitycommand.g()) {
                    worldserver.getBlockTickList().a(blockposition, this, 1);
                }
            } else if (tileentitycommand_type == TileEntityCommand.Type.REDSTONE) {
                if (flag1) {
                    this.a(iblockdata, worldserver, blockposition, commandblocklistenerabstract, flag);
                } else if (tileentitycommand.u()) {
                    commandblocklistenerabstract.a(0);
                }
            }

            worldserver.updateAdjacentComparators(blockposition, this);
        }

    }

    private void a(IBlockData iblockdata, World world, BlockPosition blockposition, CommandBlockListenerAbstract commandblocklistenerabstract, boolean flag) {
        if (flag) {
            commandblocklistenerabstract.a(world);
        } else {
            commandblocklistenerabstract.a(0);
        }

        a(world, blockposition, (EnumDirection) iblockdata.get(BlockCommand.FACING));
    }

    @Override
    public EnumInteractionResult interact(IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock) {
        TileEntity tileentity = world.getTileEntity(blockposition);

        if (tileentity instanceof TileEntityCommand && entityhuman.isCreativeAndOp()) {
            entityhuman.a((TileEntityCommand) tileentity);
            return EnumInteractionResult.a(world.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public boolean isComplexRedstone(IBlockData iblockdata) {
        return true;
    }

    @Override
    public int a(IBlockData iblockdata, World world, BlockPosition blockposition) {
        TileEntity tileentity = world.getTileEntity(blockposition);

        return tileentity instanceof TileEntityCommand ? ((TileEntityCommand) tileentity).getCommandBlock().j() : 0;
    }

    @Override
    public void postPlace(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        TileEntity tileentity = world.getTileEntity(blockposition);

        if (tileentity instanceof TileEntityCommand) {
            TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;
            CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();

            if (itemstack.hasName()) {
                commandblocklistenerabstract.setName(itemstack.getName());
            }

            if (!world.isClientSide) {
                if (itemstack.b("BlockEntityTag") == null) {
                    commandblocklistenerabstract.a(world.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
                    tileentitycommand.b(this.automatic);
                }

                if (tileentitycommand.t() == TileEntityCommand.Type.SEQUENCE) {
                    boolean flag = world.isBlockIndirectlyPowered(blockposition);

                    tileentitycommand.a(flag);
                }
            }

        }
    }

    @Override
    public EnumRenderType b_(IBlockData iblockdata) {
        return EnumRenderType.MODEL;
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return (IBlockData) iblockdata.set(BlockCommand.FACING, enumblockrotation.a((EnumDirection) iblockdata.get(BlockCommand.FACING)));
    }

    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.a(enumblockmirror.a((EnumDirection) iblockdata.get(BlockCommand.FACING)));
    }

    @Override
    protected void a(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.a(BlockCommand.FACING, BlockCommand.CONDITIONAL);
    }

    @Override
    public IBlockData getPlacedState(BlockActionContext blockactioncontext) {
        return (IBlockData) this.getBlockData().set(BlockCommand.FACING, blockactioncontext.d().opposite());
    }

    private static void a(World world, BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.i();
        GameRules gamerules = world.getGameRules();

        IBlockData iblockdata;
        int i;

        for (i = gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH); i-- > 0; enumdirection = (EnumDirection) iblockdata.get(BlockCommand.FACING)) {
            blockposition_mutableblockposition.c(enumdirection);
            iblockdata = world.getType(blockposition_mutableblockposition);
            Block block = iblockdata.getBlock();

            if (!iblockdata.a(Blocks.CHAIN_COMMAND_BLOCK)) {
                break;
            }

            TileEntity tileentity = world.getTileEntity(blockposition_mutableblockposition);

            if (!(tileentity instanceof TileEntityCommand)) {
                break;
            }

            TileEntityCommand tileentitycommand = (TileEntityCommand) tileentity;

            if (tileentitycommand.t() != TileEntityCommand.Type.SEQUENCE) {
                break;
            }

            if (tileentitycommand.f() || tileentitycommand.g()) {
                CommandBlockListenerAbstract commandblocklistenerabstract = tileentitycommand.getCommandBlock();

                if (tileentitycommand.j()) {
                    if (!commandblocklistenerabstract.a(world)) {
                        break;
                    }

                    world.updateAdjacentComparators(blockposition_mutableblockposition, block);
                } else if (tileentitycommand.u()) {
                    commandblocklistenerabstract.a(0);
                }
            }
        }

        if (i <= 0) {
            int j = Math.max(gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);

            BlockCommand.LOGGER.warn("Command Block chain tried to execute more than {} steps!", j);
        }

    }
}
