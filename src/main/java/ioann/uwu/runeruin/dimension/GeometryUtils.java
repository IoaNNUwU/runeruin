package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;
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
    }

    public static void emptySphere(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height, int cropFromTop, int cropFromBottom) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -height + cropFromBottom; y <= height - cropFromTop; y++) {

                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    float fx = xx + 0.5f - origin.getX();
                    float fz = zz + 0.5f - origin.getZ();
                    float fy = yy + 0.5f - origin.getY();

                    float fRadius = radius;
                    float fHeight = height;

                    float dist = (fx * fx) / (fRadius * fRadius)
                            + (fz * fz) / (fRadius * fRadius)
                            + (fy * fy) / (fHeight * fHeight);

                    if (0.65 < dist && dist <= 1) {
                        level.setBlock(new BlockPos(xx, yy, zz), block.apply(xx, yy, zz), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
