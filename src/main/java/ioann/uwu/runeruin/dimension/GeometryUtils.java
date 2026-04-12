package ioann.uwu.runeruin.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

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
}
