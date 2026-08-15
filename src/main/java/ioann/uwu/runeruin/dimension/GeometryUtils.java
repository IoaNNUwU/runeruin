package ioann.uwu.runeruin.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class GeometryUtils {

    public static final int BULK_FLAG = Block.UPDATE_CLIENTS;
    public static final int SOLID_FLAG = Block.UPDATE_KNOWN_SHAPE;

    @FunctionalInterface
    public interface BlockStateSupplier {
        BlockState apply(int x, int y, int z);

        default BlockState apply(BlockPos blockPos) {
            return this.apply(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }

    @FunctionalInterface
    public interface SphereVisitor {
        void visit(int x, int y, int z, int d2);
    }

    public static boolean insideSphere(int d2, int radius) {
        if (radius <= 0) {
            return false;
        }
        return d2 * 20 < radius * radius * 19;
    }

    public static void smoothCyl(WorldGenLevel level, BlockPos origin, BlockState block, int radius, int height) {
        cyl(level, origin, block, radius, height - 1);
        cyl(level, origin, block, radius - 1, height);
    }

    public static void cyl(WorldGenLevel level, BlockPos origin, BlockState block, int radius, int height) {
        if (radius < 0 || height < 0) {
            return;
        }

        int r2 = radius * radius;
        int[] zLimit = diskZLimit(radius, r2);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int y = 0; y <= height; y++) {
            int yy = oy + y;
            for (int x = -radius; x <= radius; x++) {
                int maxZ = zLimit[Math.abs(x)];
                if (maxZ < 0) {
                    continue;
                }
                int xx = ox + x;
                for (int z = -maxZ; z <= maxZ; z++) {
                    set(level, mutable.set(xx, yy, oz + z), block, SOLID_FLAG);
                }
            }
        }
    }

    public static void sphere(WorldGenLevel level, BlockPos origin, BlockState block, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        forEachInSphere(origin, radius, (x, y, z, d2) -> set(level, mutable.set(x, y, z), block, SOLID_FLAG));
    }

    public static void sphere(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        forEachInSphere(origin, radius, (x, y, z, d2) -> set(level, mutable.set(x, y, z), block.get(), BULK_FLAG));
    }

    public static void forEachInSphere(BlockPos origin, int radius, SphereVisitor visitor) {
        if (radius < 0) {
            return;
        }

        int r2 = radius * radius;
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int y = -radius; y <= radius; y++) {
            int y2 = y * y;
            int rem = r2 - y2;
            if (rem < 0) {
                continue;
            }
            int xzBound = sqrtFloor(rem);
            for (int x = -xzBound; x <= xzBound; x++) {
                int x2 = x * x;
                int zBound = sqrtFloor(rem - x2);
                for (int z = -zBound; z <= zBound; z++) {
                    int d2 = x2 + y2 + z * z;
                    if (d2 * 20 < r2 * 19) {
                        visitor.visit(ox + x, oy + y, oz + z, d2);
                    }
                }
            }
        }
    }

    public static void bottomHalfEmptySphere(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {
        if (radius < 0) {
            return;
        }

        int inner2 = (radius - 1) * (radius - 1);
        int outer2 = (radius + 1) * (radius + 1);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int y = -radius; y <= 0; y++) {
            int y2 = y * y;
            int rem = outer2 - y2;
            if (rem < 0) {
                continue;
            }
            int xzBound = sqrtFloor(rem);
            for (int x = -xzBound; x <= xzBound; x++) {
                int x2 = x * x;
                int zBound = sqrtFloor(rem - x2);
                int xx = ox + x;
                int yy = oy + y;
                for (int z = -zBound; z <= zBound; z++) {
                    int d2 = x2 + y2 + z * z;
                    if (d2 > inner2 && d2 * 20 < outer2 * 19) {
                        set(level, mutable.set(xx, yy, oz + z), block.get(), BULK_FLAG);
                    }
                }
            }
        }
    }

    public static void emptyCyl(WorldGenLevel level, BlockPos origin, Supplier<BlockState> block, int radius) {
        if (radius < 0) {
            return;
        }

        int inner2 = (radius - 1) * (radius - 1);
        int outer2 = (radius + 1) * (radius + 1);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        int xzBound = sqrtFloor(outer2);
        for (int x = -xzBound; x <= xzBound; x++) {
            int x2 = x * x;
            int rem = outer2 - x2;
            if (rem < 0) {
                continue;
            }
            int zBound = sqrtFloor(rem);
            int xx = ox + x;
            for (int z = -zBound; z <= zBound; z++) {
                int d2 = x2 + z * z;
                if (d2 > inner2 && d2 * 20 < outer2 * 19) {
                    set(level, mutable.set(xx, oy, oz + z), block.get(), BULK_FLAG);
                }
            }
        }
    }

    public static void emptySphere(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height, int cropFromTop, int cropFromBottom) {
        emptySphere(level, origin, block, radius, height, cropFromTop, cropFromBottom, BULK_FLAG);
    }

    public static void emptySphere(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height, int cropFromTop, int cropFromBottom, int flags) {
        if (radius <= 0 || height <= 0) {
            return;
        }

        float invR2 = 1f / (radius * radius);
        float invH2 = 1f / (height * height);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        int yMin = -height + cropFromBottom - 1;
        int yMax = height - cropFromTop;

        for (int y = yMin; y <= yMax; y++) {
            float fy = y;
            float yTerm = (fy * fy) * invH2;
            if (yTerm >= 1f) {
                continue;
            }
            float xzRemain = 1f - yTerm;
            int xzBound = (int) Math.sqrt(xzRemain / invR2);
            int yy = oy + y;
            for (int x = -xzBound; x <= xzBound; x++) {
                float fx = x;
                float xTerm = (fx * fx) * invR2;
                if (xTerm + yTerm >= 1f) {
                    continue;
                }
                int xx = ox + x;
                for (int z = -xzBound; z <= xzBound; z++) {
                    float fz = z;
                    float dist = xTerm + (fz * fz) * invR2 + yTerm;
                    if (0.55f < dist && dist < 1f) {
                        BlockState blockState = block.apply(xx, yy, oz + z);
                        if (blockState.isAir()) {
                            continue;
                        }
                        set(level, mutable.set(xx, yy, oz + z), blockState, flags);
                    }
                }
            }
        }
    }

    public static void cube(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height) {
        cube(level, origin, block, radius, height, SOLID_FLAG);
    }

    public static void cube(WorldGenLevel level, BlockPos origin, BlockStateSupplier block, int radius, int height, int flags) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int x = -radius / 2 - 1; x <= radius / 2 + 1; x++) {
            int xx = ox + x;
            for (int z = -radius / 2 - 1; z <= radius / 2 + 1; z++) {
                int zz = oz + z;
                for (int y = -height / 2 - 1; y <= height / 2 + 1; y++) {
                    int yy = oy + y;
                    BlockState blockState = block.apply(xx, yy, zz);
                    if (blockState.isAir()) {
                        continue;
                    }
                    set(level, mutable.set(xx, yy, zz), blockState, flags);
                }
            }
        }
    }

    public static void line(WorldGenLevel level, BlockPos origin, BlockPos target, BlockStateSupplier blockSupplier) {
        line(level, origin, target, blockSupplier, SOLID_FLAG);
    }

    public static void line(WorldGenLevel level, BlockPos origin, BlockPos target, BlockStateSupplier blockSupplier, int flags) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int x0 = origin.getX();
        int y0 = origin.getY();
        int z0 = origin.getZ();
        int x1 = target.getX();
        int y1 = target.getY();
        int z1 = target.getZ();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int dm = Math.max(dx, Math.max(dy, dz));

        if (dm == 0) {
            BlockState block = blockSupplier.apply(x0, y0, z0);
            set(level, mutable.set(x0, y0, z0), block, flags);
            set(level, mutable.set(x0, y0 - 1, z0), block, flags);
            return;
        }

        int xErr = dm / 2;
        int yErr = dm / 2;
        int zErr = dm / 2;
        int x = x0;
        int y = y0;
        int z = z0;

        for (int i = 0; i <= dm; i++) {
            BlockState block = blockSupplier.apply(x, y, z);
            set(level, mutable.set(x, y, z), block, flags);
            set(level, mutable.set(x, y - 1, z), block, flags);

            if (i == dm) {
                break;
            }

            xErr -= dx;
            if (xErr < 0) {
                xErr += dm;
                x += sx;
            }
            yErr -= dy;
            if (yErr < 0) {
                yErr += dm;
                y += sy;
            }
            zErr -= dz;
            if (zErr < 0) {
                zErr += dm;
                z += sz;
            }
        }
    }

    public static void curvedLine(WorldGenLevel level, BlockPos origin, BlockPos target, BlockStateSupplier blockSupplier) {
        line(level, origin, target.above(), blockSupplier);

        BlockState block = blockSupplier.apply(target.getX(), target.getY(), target.getZ());
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        set(level, mutable.set(target.getX(), target.getY() - 1, target.getZ()), block, SOLID_FLAG);
    }

    private static void set(WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockState state, int flags) {
        level.setBlock(pos, state, flags);
    }

    private static int[] diskZLimit(int radius, int r2) {
        int[] zLimit = new int[radius + 1];
        for (int x = 0; x <= radius; x++) {
            int rem = r2 - x * x;
            if (rem <= 0) {
                zLimit[x] = -1;
            } else {
                zLimit[x] = sqrtFloor(rem - 1);
            }
        }
        return zLimit;
    }

    private static int sqrtFloor(int value) {
        return (int) Math.sqrt(value);
    }
}
