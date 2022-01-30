package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockPortal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.phys.Vec3D;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
// CraftBukkit end

public class BlockPortalShape {

    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBase.e FRAME = (iblockdata, iblockaccess, blockposition) -> {
        return iblockdata.is(Blocks.OBSIDIAN);
    };
    private final GeneratorAccess level;
    private final EnumDirection.EnumAxis axis;
    private final EnumDirection rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPosition bottomLeft;
    private int height;
    private final int width;
    org.bukkit.craftbukkit.util.BlockStateListPopulator blocks; // CraftBukkit - add field

    public static Optional<BlockPortalShape> findEmptyPortalShape(GeneratorAccess generatoraccess, BlockPosition blockposition, EnumDirection.EnumAxis enumdirection_enumaxis) {
        return findPortalShape(generatoraccess, blockposition, (blockportalshape) -> {
            return blockportalshape.isValid() && blockportalshape.numPortalBlocks == 0;
        }, enumdirection_enumaxis);
    }

    public static Optional<BlockPortalShape> findPortalShape(GeneratorAccess generatoraccess, BlockPosition blockposition, Predicate<BlockPortalShape> predicate, EnumDirection.EnumAxis enumdirection_enumaxis) {
        Optional<BlockPortalShape> optional = Optional.of(new BlockPortalShape(generatoraccess, blockposition, enumdirection_enumaxis)).filter(predicate);

        if (optional.isPresent()) {
            return optional;
        } else {
            EnumDirection.EnumAxis enumdirection_enumaxis1 = enumdirection_enumaxis == EnumDirection.EnumAxis.X ? EnumDirection.EnumAxis.Z : EnumDirection.EnumAxis.X;

            return Optional.of(new BlockPortalShape(generatoraccess, blockposition, enumdirection_enumaxis1)).filter(predicate);
        }
    }

    public BlockPortalShape(GeneratorAccess generatoraccess, BlockPosition blockposition, EnumDirection.EnumAxis enumdirection_enumaxis) {
        blocks = new org.bukkit.craftbukkit.util.BlockStateListPopulator(generatoraccess.getMinecraftWorld()); // CraftBukkit
        this.level = generatoraccess;
        this.axis = enumdirection_enumaxis;
        this.rightDir = enumdirection_enumaxis == EnumDirection.EnumAxis.X ? EnumDirection.WEST : EnumDirection.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(blockposition);
        if (this.bottomLeft == null) {
            this.bottomLeft = blockposition;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }

    }

    @Nullable
    private BlockPosition calculateBottomLeft(BlockPosition blockposition) {
        for (int i = Math.max(this.level.getMinBuildHeight(), blockposition.getY() - 21); blockposition.getY() > i && isEmpty(this.level.getBlockState(blockposition.below())); blockposition = blockposition.below()) {
            ;
        }

        EnumDirection enumdirection = this.rightDir.getOpposite();
        int j = this.getDistanceUntilEdgeAboveFrame(blockposition, enumdirection) - 1;

        return j < 0 ? null : blockposition.relative(enumdirection, j);
    }

    private int calculateWidth() {
        int i = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);

        return i >= 2 && i <= 21 ? i : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (int i = 0; i <= 21; ++i) {
            blockposition_mutableblockposition.set(blockposition).move(enumdirection, i);
            IBlockData iblockdata = this.level.getBlockState(blockposition_mutableblockposition);

            if (!isEmpty(iblockdata)) {
                if (BlockPortalShape.FRAME.test(iblockdata, this.level, blockposition_mutableblockposition)) {
                    blocks.setBlock(blockposition_mutableblockposition, iblockdata, 18); // CraftBukkit - lower left / right
                    return i;
                }
                break;
            }

            IBlockData iblockdata1 = this.level.getBlockState(blockposition_mutableblockposition.move(EnumDirection.DOWN));

            if (!BlockPortalShape.FRAME.test(iblockdata1, this.level, blockposition_mutableblockposition)) {
                break;
            }
            blocks.setBlock(blockposition_mutableblockposition, iblockdata1, 18); // CraftBukkit - bottom row
        }

        return 0;
    }

    private int calculateHeight() {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
        int i = this.getDistanceUntilTop(blockposition_mutableblockposition);

        return i >= 3 && i <= 21 && this.hasTopFrame(blockposition_mutableblockposition, i) ? i : 0;
    }

    private boolean hasTopFrame(BlockPosition.MutableBlockPosition blockposition_mutableblockposition, int i) {
        for (int j = 0; j < this.width; ++j) {
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition1 = blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, j);

            if (!BlockPortalShape.FRAME.test(this.level.getBlockState(blockposition_mutableblockposition1), this.level, blockposition_mutableblockposition1)) {
                return false;
            }
            blocks.setBlock(blockposition_mutableblockposition1, this.level.getBlockState(blockposition_mutableblockposition1), 18); // CraftBukkit - upper row
        }

        return true;
    }

    private int getDistanceUntilTop(BlockPosition.MutableBlockPosition blockposition_mutableblockposition) {
        for (int i = 0; i < 21; ++i) {
            blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, -1);
            if (!BlockPortalShape.FRAME.test(this.level.getBlockState(blockposition_mutableblockposition), this.level, blockposition_mutableblockposition)) {
                return i;
            }

            blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, this.width);
            if (!BlockPortalShape.FRAME.test(this.level.getBlockState(blockposition_mutableblockposition), this.level, blockposition_mutableblockposition)) {
                return i;
            }

            for (int j = 0; j < this.width; ++j) {
                blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, j);
                IBlockData iblockdata = this.level.getBlockState(blockposition_mutableblockposition);

                if (!isEmpty(iblockdata)) {
                    return i;
                }

                if (iblockdata.is(Blocks.NETHER_PORTAL)) {
                    ++this.numPortalBlocks;
                }
            }
            // CraftBukkit start - left and right
            blocks.setBlock(blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, -1), this.level.getBlockState(blockposition_mutableblockposition), 18);
            blocks.setBlock(blockposition_mutableblockposition.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, this.width), this.level.getBlockState(blockposition_mutableblockposition), 18);
            // CraftBukkit end
        }

        return 21;
    }

    private static boolean isEmpty(IBlockData iblockdata) {
        return iblockdata.isAir() || iblockdata.is((Tag) TagsBlock.FIRE) || iblockdata.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    // CraftBukkit start - return boolean
    public boolean createPortalBlocks() {
        org.bukkit.World bworld = this.level.getMinecraftWorld().getWorld();

        // Copy below for loop
        IBlockData iblockdata = (IBlockData) Blocks.NETHER_PORTAL.defaultBlockState().setValue(BlockPortal.AXIS, this.axis);

        BlockPosition.betweenClosed(this.bottomLeft, this.bottomLeft.relative(EnumDirection.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach((blockposition) -> {
            blocks.setBlock(blockposition, iblockdata, 18);
        });

        PortalCreateEvent event = new PortalCreateEvent((java.util.List<org.bukkit.block.BlockState>) (java.util.List) blocks.getList(), bworld, null, PortalCreateEvent.CreateReason.FIRE);
        this.level.getMinecraftWorld().getServer().server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }
        // CraftBukkit end
        BlockPosition.betweenClosed(this.bottomLeft, this.bottomLeft.relative(EnumDirection.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach((blockposition) -> {
            this.level.setBlock(blockposition, iblockdata, 18);
        });
        return true; // CraftBukkit
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3D getRelativePosition(BlockUtil.Rectangle blockutil_rectangle, EnumDirection.EnumAxis enumdirection_enumaxis, Vec3D vec3d, EntitySize entitysize) {
        double d0 = (double) blockutil_rectangle.axis1Size - (double) entitysize.width;
        double d1 = (double) blockutil_rectangle.axis2Size - (double) entitysize.height;
        BlockPosition blockposition = blockutil_rectangle.minCorner;
        double d2;

        if (d0 > 0.0D) {
            float f = (float) blockposition.get(enumdirection_enumaxis) + entitysize.width / 2.0F;

            d2 = MathHelper.clamp(MathHelper.inverseLerp(vec3d.get(enumdirection_enumaxis) - (double) f, 0.0D, d0), 0.0D, 1.0D);
        } else {
            d2 = 0.5D;
        }

        EnumDirection.EnumAxis enumdirection_enumaxis1;
        double d3;

        if (d1 > 0.0D) {
            enumdirection_enumaxis1 = EnumDirection.EnumAxis.Y;
            d3 = MathHelper.clamp(MathHelper.inverseLerp(vec3d.get(enumdirection_enumaxis1) - (double) blockposition.get(enumdirection_enumaxis1), 0.0D, d1), 0.0D, 1.0D);
        } else {
            d3 = 0.0D;
        }

        enumdirection_enumaxis1 = enumdirection_enumaxis == EnumDirection.EnumAxis.X ? EnumDirection.EnumAxis.Z : EnumDirection.EnumAxis.X;
        double d4 = vec3d.get(enumdirection_enumaxis1) - ((double) blockposition.get(enumdirection_enumaxis1) + 0.5D);

        return new Vec3D(d2, d3, d4);
    }

    public static ShapeDetectorShape createPortalInfo(WorldServer worldserver, BlockUtil.Rectangle blockutil_rectangle, EnumDirection.EnumAxis enumdirection_enumaxis, Vec3D vec3d, EntitySize entitysize, Vec3D vec3d1, float f, float f1, CraftPortalEvent portalEventInfo) { // CraftBukkit
        BlockPosition blockposition = blockutil_rectangle.minCorner;
        IBlockData iblockdata = worldserver.getBlockState(blockposition);
        EnumDirection.EnumAxis enumdirection_enumaxis1 = (EnumDirection.EnumAxis) iblockdata.getOptionalValue(BlockProperties.HORIZONTAL_AXIS).orElse(EnumDirection.EnumAxis.X);
        double d0 = (double) blockutil_rectangle.axis1Size;
        double d1 = (double) blockutil_rectangle.axis2Size;
        int i = enumdirection_enumaxis == enumdirection_enumaxis1 ? 0 : 90;
        Vec3D vec3d2 = enumdirection_enumaxis == enumdirection_enumaxis1 ? vec3d1 : new Vec3D(vec3d1.z, vec3d1.y, -vec3d1.x);
        double d2 = (double) entitysize.width / 2.0D + (d0 - (double) entitysize.width) * vec3d.x();
        double d3 = (d1 - (double) entitysize.height) * vec3d.y();
        double d4 = 0.5D + vec3d.z();
        boolean flag = enumdirection_enumaxis1 == EnumDirection.EnumAxis.X;
        Vec3D vec3d3 = new Vec3D((double) blockposition.getX() + (flag ? d2 : d4), (double) blockposition.getY() + d3, (double) blockposition.getZ() + (flag ? d4 : d2));

        return new ShapeDetectorShape(vec3d3, vec3d2, f + (float) i, f1, worldserver, portalEventInfo); // CraftBukkit
    }
}
