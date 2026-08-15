package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public class InvertedTreeFeature extends Feature<InvertedTreeFeature.Config> {

    private static final int[][] CEILING_ANCHORS = {
            {0, 3, -2},
            {0, 3, 2},
            {2, 3, 0},
            {-2, 3, 0}
    };

    private static final int[][] CARDINALS = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
    };

    private static final int[][] TRUNK_CROSS = {
            {0, 0},
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    private static final int[][] TRUNK_CROSS_FAR = {
            {-2, 0},
            {2, 0},
            {0, -2},
            {0, 2}
    };

    private static final int[][] ROOT_EXTRA_A = {
            {1, -4, 1},
            {-1, -4, -1}
    };

    private static final int[][] ROOT_EXTRA_B = {
            {1, -4, -1},
            {-1, -4, 1}
    };

    private static final int[][] EXTENSION_A = {
            {-1, 1, 1},
            {-1, 0, 1},
            {1, 1, -1},
            {1, 0, -1},
            {1, -1, 1},
            {1, 0, 1},
            {-1, -1, -1},
            {-1, 0, -1}
    };

    private static final int[][] EXTENSION_B = {
            {-1, 1, -1},
            {-1, 0, -1},
            {1, 1, 1},
            {1, 0, 1},
            {1, -1, -1},
            {1, 0, -1},
            {-1, -1, 1},
            {-1, 0, 1}
    };

    private static final int[][] CROWN_EXTRA_A = {
            {-1, 2, 1},
            {-1, 1, 2},
            {-2, 1, 1},
            {-1, 0, 2},
            {-2, 0, 1},
            {1, 2, -1},
            {1, 1, -2},
            {2, 1, -1},
            {1, 0, -2},
            {2, 0, -1}
    };

    private static final int[][] CROWN_EXTRA_B = {
            {-1, 2, -1},
            {-1, 1, -2},
            {-2, 1, -1},
            {-1, 0, -2},
            {-2, 0, -1},
            {1, 2, 1},
            {2, 1, 1},
            {1, 1, 2},
            {2, 0, 1},
            {1, 0, 2}
    };

    public InvertedTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState ceilingBlock = config.placeOn.getState(level, random, origin);
        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);
        BlockState air = Blocks.AIR.defaultBlockState();

        BlockPos ceilingOrigin = origin;
        List<BlockState> leaves = config.leavesBlock.stream()
                .map(provider -> provider.getState(level, random, ceilingOrigin))
                .toList();

        float maxLength = config.maxLength.sample(random);
        int height = (int) (maxLength + (maxLength / 2) * random.nextFloat());

        if (!isValidPlacement(level, origin, ceilingBlock, trunkBlock, height, mutable)) {
            return false;
        }

        origin = origin.below();
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -3; y <= 2; y++) {
                    setSolid(level, mutable, ox + x, oy + y, oz + z, trunkBlock);
                }
            }
        }

        int[][] extraRoots = random.nextBoolean() ? ROOT_EXTRA_A : ROOT_EXTRA_B;
        for (int[] offset : extraRoots) {
            setSolid(level, mutable, ox + offset[0], oy + offset[1], oz + offset[2], trunkBlock);
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    setSolid(level, mutable, ox + x, oy + y, oz + z, trunkBlock);
                }
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    setSolid(level, mutable, ox + x, oy + y, oz + z, trunkBlock);
                }
            }
        }

        for (int[] offset : TRUNK_CROSS_FAR) {
            setSolid(level, mutable, ox + offset[0], oy - 2, oz + offset[1], trunkBlock);
        }

        for (int y = 0; y < height; y++) {
            int yy = oy - y;
            for (int[] offset : TRUNK_CROSS) {
                setSolid(level, mutable, ox + offset[0], yy, oz + offset[1], trunkBlock);
            }
        }

        int bx = ox;
        int by = oy - height;
        int bz = oz;
        BlockPos bottomTrunkPos = mutable.set(bx, by, bz).immutable();
        int radius = height / 2;

        double branchAngle = random.nextInt(0, 100) * Math.PI / 2 / 100d;
        int branchCount = random.nextInt(4, 7);
        double branchOffset = 2 * Math.PI / branchCount;

        double[] sin = new double[branchCount];
        double[] cos = new double[branchCount];
        for (int i = 0; i < branchCount; i++) {
            double angle = branchAngle + branchOffset * i;
            sin[i] = Math.sin(angle);
            cos[i] = Math.cos(angle);
        }

        GeometryUtils.BlockStateSupplier branchSupplier = (x, y, z) -> {
            int dx = x - bx;
            int dz = z - bz;
            double len2 = (double) dx * dx + (double) dz * dz;

            double threshold;
            if ((double) radius / 1.2 > len2) {
                threshold = 0.6;
            } else if ((double) radius * 2 > len2) {
                threshold = 0.45;
            } else {
                threshold = 0.2;
            }
            double half = threshold * 0.5;
            double half2 = half * half;

            if (len2 == 0) {
                for (int i = 0; i < branchCount; i++) {
                    double angle = (branchAngle + branchOffset * i) % (Math.PI * 2);
                    if (angle < 0) {
                        angle += Math.PI * 2;
                    }
                    if (angle < half || angle > Math.PI * 2 - half) {
                        return trunkBlock;
                    }
                }
                return air;
            }

            for (int i = 0; i < branchCount; i++) {
                double cross = dx * cos[i] - dz * sin[i];
                double dot = dx * sin[i] + dz * cos[i];
                if (dot >= 0 && cross * cross < half2 * len2) {
                    return trunkBlock;
                }
            }
            return air;
        };

        GeometryUtils.emptySphere(
                level,
                bottomTrunkPos,
                branchSupplier,
                radius - 1,
                radius * 3 / 4 - 1,
                0,
                0,
                GeometryUtils.SOLID_FLAG
        );

        if (height > 13) {
            int tx = ox;
            int ty = oy - (height - 3) / 2;
            int tz = oz;
            BlockPos trunkBranchesOrigin = new BlockPos(tx, ty, tz);

            int[][] extension = random.nextBoolean() ? EXTENSION_A : EXTENSION_B;
            for (int[] offset : extension) {
                setSolid(level, mutable, tx + offset[0], ty + offset[1], tz + offset[2], trunkBlock);
            }

            if (random.nextBoolean()) {
                int randYOffset = random.nextInt(-2, 2);
                int[][] targets = extraBranchTargets(random.nextInt(0, 4), randYOffset);

                for (int[] offset : targets) {
                    int targetX = tx + offset[0];
                    int targetY = ty + offset[1];
                    int targetZ = tz + offset[2];
                    int yThresholdBranches = targetY + 1;
                    BlockPos target = new BlockPos(targetX, targetY, targetZ);

                    GeometryUtils.BlockStateSupplier additionalBranchesLeavesSupplier = (_, y, _) -> {
                        int idx = random.nextInt(0, leaves.size());

                        if (random.nextInt(0, 7) == 0) {
                            return air;
                        }

                        if (y == yThresholdBranches) {
                            return random.nextBoolean()
                                    ? leaves.get(idx).trySetValue(LeavesBlock.PERSISTENT, true)
                                    : air;
                        }

                        if (y > yThresholdBranches) {
                            return air;
                        }

                        return leaves.get(idx).trySetValue(LeavesBlock.PERSISTENT, true);
                    };

                    GeometryUtils.emptySphere(
                            level,
                            target,
                            additionalBranchesLeavesSupplier,
                            4,
                            4,
                            0,
                            0,
                            Block.UPDATE_ALL
                    );

                    GeometryUtils.curvedLine(
                            level,
                            trunkBranchesOrigin.above(3),
                            target.below(1),
                            (_, _, _) -> trunkBlock
                    );
                }
            }
        }

        int yThreshold = by + radius / 2 - 1;

        GeometryUtils.BlockStateSupplier leaveSupplier = (_, y, _) -> {
            int idx = random.nextInt(0, leaves.size());

            if (random.nextInt(0, 7) == 0) {
                return air;
            }

            if (y == yThreshold) {
                return random.nextBoolean()
                        ? leaves.get(idx).trySetValue(LeavesBlock.PERSISTENT, true)
                        : air;
            }

            if (y > yThreshold) {
                return air;
            }

            return leaves.get(idx).trySetValue(LeavesBlock.PERSISTENT, true);
        };

        GeometryUtils.emptySphere(
                level,
                bottomTrunkPos,
                leaveSupplier,
                radius,
                radius * 3 / 4,
                0,
                0,
                Block.UPDATE_ALL
        );

        int crownY = yThreshold + 1;
        BlockPos branchesOrigin = new BlockPos(ox, crownY, oz);

        GeometryUtils.cube(level, branchesOrigin, (_, _, _) -> trunkBlock, 1, 1);

        int[][] crownExtra = random.nextBoolean() ? CROWN_EXTRA_A : CROWN_EXTRA_B;
        for (int[] offset : crownExtra) {
            setSolid(level, mutable, ox + offset[0], crownY + offset[1], oz + offset[2], trunkBlock);
        }

        return false;
    }

    private static int[][] extraBranchTargets(int rand, int randYOffset) {
        return switch (rand) {
            case 0 -> new int[][]{{4, randYOffset, 5}};
            case 1 -> new int[][]{{5, randYOffset, 4}, {-4, randYOffset, -5}};
            case 2 -> new int[][]{{5, randYOffset, -4}, {4, randYOffset, -5}, {4, randYOffset, 5}};
            default -> new int[][]{{-5, randYOffset, -4}, {-4, randYOffset, -5}};
        };
    }

    private static void setSolid(WorldGenLevel level, BlockPos.MutableBlockPos mutable, int x, int y, int z, BlockState state) {
        level.setBlock(mutable.set(x, y, z), state, GeometryUtils.SOLID_FLAG);
    }

    private static boolean isValidPlacement(
            WorldGenLevel level,
            BlockPos origin,
            BlockState ceilingBlock,
            BlockState trunkBlock,
            int height,
            BlockPos.MutableBlockPos mutable
    ) {
        if (!level.getBlockState(origin).is(ceilingBlock.getBlock())) {
            return false;
        }

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int[] offset : CEILING_ANCHORS) {
            if (level.isEmptyBlock(mutable.set(ox + offset[0], oy + offset[1], oz + offset[2]))) {
                return false;
            }
        }

        for (int[] offset : CARDINALS) {
            BlockState neighbor = level.getBlockState(mutable.set(ox + offset[0], oy, oz + offset[1]));
            if (neighbor.is(trunkBlock.getBlock()) || neighbor.is(RRBlocks.ARCANE_STONE)) {
                return false;
            }
        }

        int radius = height / 2;
        int[][] probes = {
                {0, 0},
                {radius, 0},
                {-radius, 0},
                {0, radius},
                {0, -radius}
        };

        for (int y = 5; y < height * 2; y++) {
            int yy = oy - y;
            for (int[] probe : probes) {
                if (!level.getBlockState(mutable.set(ox + probe[0], yy, oz + probe[1])).isAir()) {
                    return false;
                }
            }
        }

        return true;
    }

    public record Config(
            BlockStateProvider placeOn,
            BlockStateProvider trunkBlock,
            List<BlockStateProvider> leavesBlock,
            IntProvider maxLength
    ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("place_on").forGetter(Config::placeOn),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                        Codec.list(BlockStateProvider.CODEC).fieldOf("leaves_block").forGetter(Config::leavesBlock),
                        IntProviders.codec(5, Const.BLOOMING_CAVES_CEILING_Y - Const.BLOOMING_CAVES_Y)
                                .fieldOf("max_length").forGetter(Config::maxLength)
                ).apply(codec, Config::new));
    }
}
