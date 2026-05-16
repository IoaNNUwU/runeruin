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
import java.util.function.Supplier;

public class InvertedTreeFeature extends Feature<InvertedTreeFeature.Config> {

    public InvertedTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        BlockState ceilingBlock = config.placeOn.getState(level, random, origin);
        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);

        BlockPos finalOrigin = origin;
        List<BlockState> leaves = config.leavesBlock.stream()
                .map(ski -> ski.getState(level, random, finalOrigin))
                .toList();

        float maxLength = config.maxLength.sample(random);
        int height = (int) (maxLength + (maxLength / 2) * random.nextFloat());

        if (isValidPlacement(level, origin, ceilingBlock, trunkBlock, height)) {

            origin = origin.below();

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = -4; y <= 2; y++) {
                        int xx = origin.getX() + x;
                        int yy = origin.getY() + y;
                        int zz = origin.getZ() + z;

                        BlockPos blockPos = new BlockPos(xx, yy, zz);

                        level.setBlock(blockPos, trunkBlock, Block.UPDATE_ALL);
                    }
                }
            }

            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        int xx = origin.getX() + x;
                        int yy = origin.getY() + y;
                        int zz = origin.getZ() + z;

                        BlockPos blockPos = new BlockPos(xx, yy, zz);

                        level.setBlock(blockPos, trunkBlock, Block.UPDATE_ALL);
                    }
                }
            }

            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = -1; y <= 1; y++) {
                        int xx = origin.getX() + x;
                        int yy = origin.getY() + y;
                        int zz = origin.getZ() + z;

                        BlockPos blockPos = new BlockPos(xx, yy, zz);

                        level.setBlock(blockPos, trunkBlock, Block.UPDATE_ALL);
                    }
                }
            }

            BlockPos newBlockPos = origin.below(2);
            List<BlockPos> blockPoses = List.of(
                    newBlockPos.west(2),
                    newBlockPos.east(2),
                    newBlockPos.north(2),
                    newBlockPos.south(2)
            );
            for (BlockPos pos : blockPoses) {
                level.setBlock(pos, trunkBlock, Block.UPDATE_ALL);
            }

            for (int y = 0; y < height; y++) {
                newBlockPos = origin.below(y);
                blockPoses = List.of(newBlockPos, newBlockPos.west(), newBlockPos.east(), newBlockPos.north(), newBlockPos.south());

                for (BlockPos pos : blockPoses) {
                    level.setBlock(pos, trunkBlock, Block.UPDATE_ALL);
                }
            }

            BlockPos bottomTrunkPos = origin.below(height);
            int radius = height / 2;

            GeometryUtils.BlockStateSupplier branchSupplier = (x, y, z) -> {

                double rotation = Math.atan2(x - bottomTrunkPos.getX(), z - bottomTrunkPos.getZ());
                double distSqr = bottomTrunkPos.atY(y).distToLowCornerSqr(x, y, z);

                // System.out.println(distSqr);

                int branchCount = random.nextInt(2, 5);
                double branchAngle = random.nextInt(0, 100) * Math.PI / 100d;

                double branchOffset = Math.PI / branchCount;

                // System.out.println(branchAngle);

                boolean condition = distSqr < ((double) radius) / 1.1 ? rotation > (-0.1) && rotation < (0.5)
                        : rotation > -0.1 && rotation < 0.2;

                if (condition) {
                    return distSqr < ((double) radius) / 1.1 ? Blocks.DIAMOND_BLOCK.defaultBlockState()
                            : Blocks.IRON_BLOCK.defaultBlockState();
                } else {
                    return Blocks.AIR.defaultBlockState();
                }
            };

            GeometryUtils.emptySphere(
                    level,
                    bottomTrunkPos,
                    branchSupplier,
                    radius - 1,
                    radius * 3 / 4 - 1,
                    0,
                    0
            );

            GeometryUtils.BlockStateSupplier leaveSupplier = (_, y, _) -> {
                int idx = random.nextInt(0, leaves.size());

                if (random.nextInt(0, 7) == 0) {
                    return Blocks.AIR.defaultBlockState();
                }

                if (y == bottomTrunkPos.getY() + 3) {
                    return random.nextBoolean()
                            ? leaves.get(idx).trySetValue(LeavesBlock.PERSISTENT, true)
                            : Blocks.AIR.defaultBlockState();
                }

                return leaves.get(idx)
                        .trySetValue(LeavesBlock.PERSISTENT, true);
            };

            GeometryUtils.emptySphere(level, bottomTrunkPos, leaveSupplier, radius, radius * 3 / 4, radius / 2 - 1, 0);
        }

        return false;
    }

    private static boolean isValidPlacement(WorldGenLevel level, BlockPos origin, BlockState ceilingBlock, BlockState trunkBlock, int height) {
        if (!level.getBlockState(origin).is(ceilingBlock.getBlock())) {
            return false;
        }

        List<BlockPos> noBlocks = List.of(
                origin.above(3).north(2),
                origin.above(3).south(2),
                origin.above(3).east(2),
                origin.above(3).west(2)
        );

        for (BlockPos blockPos : noBlocks) {
            if (level.isEmptyBlock(blockPos)) {
                return false;
            }
        }

        for (BlockPos blockPos : List.of(origin.north(), origin.south(), origin.west(), origin.east())) {
            if (level.getBlockState(blockPos).is(trunkBlock.getBlock())) {
                return false;
            }
            if (level.getBlockState(blockPos).is(RRBlocks.ARCANE_STONE)) {
                return false;
            }
        }

        int radius = height / 2;

        for (int y = 5; y < height * 2; y++) {
            List<BlockPos> poses = List.of(
                    new BlockPos(origin.getX(), origin.getY() - y, origin.getZ()),
                    new BlockPos(origin.getX() + radius, origin.getY() - y, origin.getZ()),
                    new BlockPos(origin.getX() - radius, origin.getY() - y, origin.getZ()),
                    new BlockPos(origin.getX(), origin.getY() - y, origin.getZ() + radius),
                    new BlockPos(origin.getX(), origin.getY() - y, origin.getZ() - radius)
            );

            for (BlockPos blockPos : poses) {
                if (!level.getBlockState(blockPos).isAir()) {
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
