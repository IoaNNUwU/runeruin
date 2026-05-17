package ioann.uwu.runeruin.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class GeometryUtils {

    public static void smoothCyl(WorldGenLevel level, BlockPos origin, BlockState block, int radius, int height) {
        cyl(level, origin, block, radius, height - 1);
        cyl(level, origin, block, radius - 1, height);
    }

    public static void cyl(WorldGenLevel level, BlockPos origin, BlockState block, int radius, int height) {

        for (int y = 0; y <= height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    BlockPos originHeight = new BlockPos(origin.getX(), yy, origin.getZ());

                    if (originHeight.distToCenterSqr(xx, yy, zz) < radius * radius) {
                        level.setBlock(new BlockPos(xx, yy, zz), block, 1);
                    }
                }
            }
        }
    }

    public static void sphere(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    double distance = origin.distToCenterSqr(xx + 0.5d, yy + 0.5d, zz + 0.5d);

                    if (distance < (radius * radius) * 0.95f) {
                        level.setBlock(new BlockPos(xx, yy, zz), block.get(), 1);
                    }
                }
            }
        }
    }

    public static void bottomHalfEmptySphere(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= 0; y++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    double distance = origin.distToCenterSqr(xx + 0.5d, yy + 0.5d, zz + 0.5d);

                    if (distance > (radius - 1) * (radius - 1) && distance < ((radius + 1) * (radius + 1)) * 0.95f) {
                        level.setBlock(new BlockPos(xx, yy, zz), block.get(), 1);
                    }
                }
            }
        }
    }

    public static void emptyCyl(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int xx = origin.getX() + x;
                int zz = origin.getZ() + z;
                int yy = origin.getY();

                double distance = origin.distToCenterSqr(xx + 0.5d, yy + 0.5d, zz + 0.5d);

                if (distance > (radius - 1) * (radius - 1) && distance < ((radius + 1) * (radius + 1)) * 0.95f) {
                    level.setBlock(new BlockPos(xx, yy, zz), block.get(), 1);
                }

            }
        }
    }

    @FunctionalInterface
    public interface BlockStateSupplier {
        BlockState apply(int x, int y, int z);

        default BlockState apply(BlockPos blockPos) {
            return this.apply(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }

    public static void emptySphere(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height, int cropFromTop, int cropFromBottom) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -height + cropFromBottom - 1; y <= height - cropFromTop; y++) {

                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    float fx = xx - origin.getX();
                    float fz = zz - origin.getZ();
                    float fy = yy - origin.getY();

                    float fRadius = radius;
                    float fHeight = height;

                    float dist = (fx * fx) / (fRadius * fRadius)
                            + (fz * fz) / (fRadius * fRadius)
                            + (fy * fy) / (fHeight * fHeight);

                    if (0.55 < dist && dist < 1) {

                        BlockState blockState = block.apply(xx, yy, zz);
                        if (blockState.isAir()) {
                            continue;
                        }

                        level.setBlock(new BlockPos(xx, yy, zz), blockState, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    public static void cube(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -radius / 2 - 1; x <= radius / 2 + 1; x++) {
            for (int z = -radius / 2 - 1; z <= radius / 2 + 1; z++) {
                for (int y = -height / 2 - 1; y <= height / 2 + 1; y++) {
                    int xx = origin.getX() + x;
                    int yy = origin.getY() + y;
                    int zz = origin.getZ() + z;

                    BlockState blockState = block.apply(xx, yy, zz);
                    if (blockState.isAir()) {
                        continue;
                    }

                    mutable.setX(xx);
                    mutable.setY(yy);
                    mutable.setZ(zz);

                    level.setBlock(mutable, blockState, Block.UPDATE_ALL);
                }
            }
        }
    }

    public static void line(WorldGenLevel level, BlockPos origin, BlockPos target, BlockStateSupplier blockSupplier) {

        BlockPos.MutableBlockPos curBlockPos = origin.mutable();

        while (!curBlockPos.equals(target)) {
            BlockState block = blockSupplier.apply(curBlockPos);

            level.setBlock(curBlockPos, block, Block.UPDATE_ALL);

            int dx = 0;
            if (curBlockPos.getX() < target.getX()) {
                dx = 1;
            } else if (curBlockPos.getX() > target.getX()) {
                dx = -1;
            }

            int dy = 0;
            if (curBlockPos.getY() < target.getY()) {
                dy = 1;
            } else if (curBlockPos.getY() > target.getY()) {
                dy = -1;
            }

            int dz = 0;
            if (curBlockPos.getZ() < target.getZ()) {
                dz = 1;
            } else if (curBlockPos.getZ() > target.getZ()) {
                dz = -1;
            }

            curBlockPos.move(dx, dy, dz);

            level.setBlock(curBlockPos.below(), block, Block.UPDATE_ALL);
        }
    }

    public static void curvedLine(WorldGenLevel level, BlockPos origin, BlockPos target, BlockStateSupplier blockSupplier) {
        line(level, origin, target.above(), blockSupplier);

        BlockState block = blockSupplier.apply(target.getX(), target.getY(), target.getZ());
        level.setBlock(target.below(), block, Block.UPDATE_ALL);
    }
}
