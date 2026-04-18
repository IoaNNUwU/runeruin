package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public class CeilingBlockVineFeature extends Feature<CeilingBlockVineFeature.Config> {

    public CeilingBlockVineFeature() {
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

        float maxLength = config.maxLength.sample(random);
        int height = (int) (maxLength / 2 + (maxLength / 2) * random.nextFloat());

        if (isValidPlacement(level, origin, ceilingBlock, trunkBlock, height)) {

            // --- Roots ---
            for (int y = origin.getY() - 1; y <= origin.getY() + 1; y++) {
                for (int x = origin.getX() - 1; x <= origin.getX() + 2; x++) {
                    for (int z = origin.getZ(); z <= origin.getZ() + 1; z++) {
                        level.setBlock(new BlockPos(x, y, z), trunkBlock, 1);
                    }
                }
                for (int x = origin.getX(); x <= origin.getX() + 1; x++) {
                    int z = origin.getZ() - 1;
                    level.setBlock(new BlockPos(x, y, z), trunkBlock, 1);

                    z = origin.getZ() + 2;
                    level.setBlock(new BlockPos(x, y, z), trunkBlock, 1);
                }
            }

            for (int y = origin.getY() + 1; y < origin.getY() + 4; y++) {
                for (int x = origin.getX(); x < origin.getX() + 2; x++) {
                    for (int z = origin.getZ(); z < origin.getZ() + 2; z++) {
                        level.setBlock(new BlockPos(x, y, z), trunkBlock, 1);
                    }
                }
            }

            List<BlockPos> additionalRoots = List.of(
                    new BlockPos(origin.getX() - 1, origin.getY() - 2, origin.getZ() + 1),
                    new BlockPos(origin.getX() + 1, origin.getY() - 2, origin.getZ() - 1),
                    new BlockPos(origin.getX(), origin.getY() - 2, origin.getZ() + 2),
                    new BlockPos(origin.getX() + 2, origin.getY() - 2, origin.getZ() + 1)
            );

            for (BlockPos blockPos : additionalRoots) {
                level.setBlock(blockPos, trunkBlock, 1);
                BlockPos upperBlockPos = new BlockPos(blockPos.getX(), blockPos.getY() + 4, blockPos.getZ());
                level.setBlock(upperBlockPos, trunkBlock, 1);
            }

            boolean mirror = random.nextBoolean();
            boolean rotate = random.nextBoolean();

            // --- Trunk ---
            int segmentHeight = random.nextIntBetweenInclusive(5, 8);
            int nSegments = height / segmentHeight;

            for (int nSeg = 0; nSeg < nSegments; nSeg++) {

                int xOffset;
                int zOffset;
                if (nSeg % 2 == 0) {
                    xOffset = 0;
                    zOffset = 0;
                } else {
                    xOffset = mirror ? -1 : 1;
                    zOffset = rotate ? -1 : 1;
                }

                // --- Main segment body ---
                for (int y = 1; y <= segmentHeight; y++) {
                    int yy = y + segmentHeight * nSeg;

                    BlockPos blockPos = new BlockPos(origin.getX() + xOffset, origin.getY() - yy, origin.getZ() + zOffset);
                    level.setBlock(blockPos, trunkBlock, 1);

                    blockPos = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - yy, origin.getZ() + zOffset);
                    level.setBlock(blockPos, trunkBlock, 1);

                    blockPos = new BlockPos(origin.getX() + xOffset, origin.getY() - yy, origin.getZ() + zOffset + 1);
                    level.setBlock(blockPos, trunkBlock, 1);

                    blockPos = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - yy, origin.getZ() + zOffset + 1);
                    level.setBlock(blockPos, trunkBlock, 1);
                }

                // --- Additional 2 blocks on top and bottom of the segment for smoothness ---
                for (int y : List.of(segmentHeight * nSeg, segmentHeight * (nSeg + 1) + 1)) {

                    BlockPos blockPos1;
                    BlockPos blockPos2;
                    if (rotate) {
                        if (mirror) {
                            blockPos1 = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - y, origin.getZ() + zOffset);
                            blockPos2 = new BlockPos(origin.getX() + xOffset, origin.getY() - y, origin.getZ() + zOffset + 1);
                        } else {
                            blockPos1 = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - y, origin.getZ() + zOffset + 1);
                            blockPos2 = new BlockPos(origin.getX() + xOffset, origin.getY() - y, origin.getZ() + zOffset);
                        }
                    } else {
                        if (mirror) {
                            blockPos1 = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - y, origin.getZ() + zOffset + 1);
                            blockPos2 = new BlockPos(origin.getX() + xOffset, origin.getY() - y, origin.getZ() + zOffset);
                        } else {
                            blockPos1 = new BlockPos(origin.getX() + xOffset + 1, origin.getY() - y, origin.getZ() + zOffset);
                            blockPos2 = new BlockPos(origin.getX() + xOffset, origin.getY() - y, origin.getZ() + zOffset + 1);
                        }
                    }

                    level.setBlock(blockPos1, trunkBlock, 1);
                    level.setBlock(blockPos2, trunkBlock, 1);
                }

                // --- Thorns in the middle of the segment ---
                for (int y : List.of((segmentHeight * nSeg) + segmentHeight / 2, (segmentHeight * nSeg) + segmentHeight / 2 + 1)) {

                    BlockPos blockPos1;
                    BlockPos blockPos2;
                    if (nSeg % 2 == 0) {
                        if (!mirror) {
                            if (rotate) {
                                blockPos1 = new BlockPos(origin.getX() - 1, origin.getY() - y, origin.getZ() + 1);
                                blockPos2 = new BlockPos(origin.getX(), origin.getY() - y, origin.getZ() + 2);
                            } else {
                                blockPos1 = new BlockPos(origin.getX() - 1, origin.getY() - y, origin.getZ());
                                blockPos2 = new BlockPos(origin.getX(), origin.getY() - y, origin.getZ() - 1);
                            }
                        } else {
                            if (rotate) {
                                blockPos1 = new BlockPos(origin.getX() + 2, origin.getY() - y, origin.getZ() + 1);
                                blockPos2 = new BlockPos(origin.getX() + 1, origin.getY() - y, origin.getZ() + 2);
                            } else {
                                blockPos1 = new BlockPos(origin.getX() + 2, origin.getY() - y, origin.getZ());
                                blockPos2 = new BlockPos(origin.getX() + 1, origin.getY() - y, origin.getZ() - 1);
                            }
                        }
                    } else {
                        if (!mirror) {
                            if (!rotate) {
                                blockPos1 = new BlockPos(origin.getX() + 3, origin.getY() - y, origin.getZ() + 2);
                                blockPos2 = new BlockPos(origin.getX() + 2, origin.getY() - y, origin.getZ() + 3);
                            } else {
                                blockPos1 = new BlockPos(origin.getX() + 3, origin.getY() - y, origin.getZ() - 1);
                                blockPos2 = new BlockPos(origin.getX() + 2, origin.getY() - y, origin.getZ() - 2);
                            }
                        } else {
                            if (!rotate) {
                                blockPos1 = new BlockPos(origin.getX() - 2, origin.getY() - y, origin.getZ() + 2);
                                blockPos2 = new BlockPos(origin.getX() - 1, origin.getY() - y, origin.getZ() + 3);
                            } else {
                                blockPos1 = new BlockPos(origin.getX() - 2, origin.getY() - y, origin.getZ() - 1);
                                blockPos2 = new BlockPos(origin.getX() - 1, origin.getY() - y, origin.getZ() - 2);
                            }
                        }
                    }

                    level.setBlock(blockPos1, trunkBlock, 1);
                    level.setBlock(blockPos2, trunkBlock, 1);
                }
            }

            BlockPos tipOrigin;
            BlockPos tipSide1;
            BlockPos tipSide2;
            BlockPos tipThorns;

            // --- Tip of the vine ---
            if (nSegments % 2 == 0) {
                if (!mirror) {
                    if (rotate) {
                        tipOrigin = new BlockPos(origin.getX() + 1, origin.getY() - segmentHeight * nSegments, origin.getZ());
                        tipSide1 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() + 1);
                        tipSide2 = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                        tipThorns = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() - 1, tipOrigin.getZ() + 1);
                    } else {
                        tipOrigin = new BlockPos(origin.getX() + 1, origin.getY() - segmentHeight * nSegments, origin.getZ() + 1);
                        tipSide1 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() - 1);
                        tipSide2 = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                        tipThorns = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() - 1, tipOrigin.getZ()- 1);
                    }
                } else {
                    if (rotate) {
                        tipOrigin = new BlockPos(origin.getX(), origin.getY() - segmentHeight * nSegments, origin.getZ());
                        tipSide1 = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                        tipSide2 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() + 1);
                        tipThorns = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() - 1, tipOrigin.getZ() + 1);
                    } else {
                        tipOrigin = new BlockPos(origin.getX(), origin.getY() - segmentHeight * nSegments, origin.getZ() + 1);
                        tipSide1 = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                        tipSide2 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() - 1);
                        tipThorns = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() - 1, tipOrigin.getZ() - 1);
                    }
                }
            } else {
                if (!mirror) {
                    tipOrigin = new BlockPos(origin.getX() + 1, origin.getY() - segmentHeight * nSegments, origin.getZ() + 1);
                    tipSide1 = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                    tipSide2 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() + 1);
                    tipThorns = new BlockPos(tipOrigin.getX() + 1, tipOrigin.getY() - 1, tipOrigin.getZ() + 1);
                } else {
                    tipOrigin = new BlockPos(origin.getX(), origin.getY() - segmentHeight * nSegments, origin.getZ());
                    tipSide1 = new BlockPos(tipOrigin.getX(), tipOrigin.getY() + 1, tipOrigin.getZ() - 1);
                    tipSide2 = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() + 1, tipOrigin.getZ());
                    tipThorns = new BlockPos(tipOrigin.getX() - 1, tipOrigin.getY() - 1, tipOrigin.getZ() - 1);
                }
            }

            for (int y = 0; y < 6; y++) {
                BlockPos blockPos = new BlockPos(tipOrigin.getX(), tipOrigin.getY() - y, tipOrigin.getZ());
                level.setBlock(blockPos, trunkBlock, 1);
            }
            for (int y = 0; y < 4; y++) {
                BlockPos blockPos = new BlockPos(tipSide1.getX(), tipOrigin.getY() - y, tipSide1.getZ());
                level.setBlock(blockPos, trunkBlock, 1);
                blockPos = new BlockPos(tipSide2.getX(), tipOrigin.getY() - y, tipSide2.getZ());
                level.setBlock(blockPos, trunkBlock, 1);
            }
            for (int y = 0; y < 2; y++) {
                BlockPos blockPos = new BlockPos(tipThorns.getX(), tipThorns.getY() - y, tipThorns.getZ());
                level.setBlock(blockPos, trunkBlock, 1);
            }

            // --- Berries ---
            List<BlockStateProvider> berryBlocks = config.berryBlocks;
            if (berryBlocks.isEmpty()) {
                return true;
            }

            int rand = random.nextIntBetweenInclusive(0, berryBlocks.size() - 1);
            BlockState berryBlock = config.berryBlocks.get(rand).getState(level, random, origin);

            for (int nSeg = 0; nSeg < nSegments; nSeg++) {

                int xOffset;
                int zOffset;
                if (nSeg % 2 == 0) {
                    xOffset = 0;
                    zOffset = 0;
                } else {
                    xOffset = mirror ? -1 : 1;
                    zOffset = rotate ? -1 : 1;
                }

                for (int y = 1; y < segmentHeight; y = y + 2) {
                    Direction dir = Direction.getRandom(random);

                    for (int i = 0; i < 5; i++) {

                        BlockPos blockPos = new BlockPos(
                                origin.getX() + xOffset,
                                origin.getY() - nSeg * segmentHeight - y,
                                origin.getZ() + zOffset
                        ).relative(dir, i);

                        if (level.getBlockState(blockPos).isAir()) {
                            level.setBlock(blockPos, berryBlock, 1);
                            break;
                        }
                    }
                }
            }

            return true;
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

        for (int y = 1; y < height; y++) {
            BlockPos blockPos = new BlockPos(origin.getX(), origin.getY() - y, origin.getZ());
            if (!level.getBlockState(blockPos).isAir()) {
                return false;
            }
        }

        return true;
    }

    public record Config(
            BlockStateProvider placeOn,
            BlockStateProvider trunkBlock,
            List<BlockStateProvider> berryBlocks,
            IntProvider maxLength
    ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("place_on").forGetter(Config::placeOn),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                        Codec.list(BlockStateProvider.CODEC).fieldOf("berry_blocks").forGetter(Config::berryBlocks),
                        IntProviders.codec(5, Const.BLOOMING_CAVES_CEILING_Y - Const.BLOOMING_CAVES_Y)
                                .fieldOf("max_length").forGetter(Config::maxLength)
                ).apply(codec, Config::new));
    }
}
