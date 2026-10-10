package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.MossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * A group of moss layer mounds standing together: a main one and a few smaller ones against its side, each
 * tallest in the middle. Plants grow mostly near the tops, moss carpets lie around the rim.
 */
public class MossHummockFeature extends Feature<NoneFeatureConfiguration> {

    /** Columns around the origin that a group can reach; small enough to stay inside the neighbouring chunks. */
    private static final int REACH = 10;
    private static final int FLOOR_SEARCH = 3;
    private static final float CARPET_CHANCE = 0.35F;

    public MossHummockFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Moss height in layers per column; where mounds overlap the taller one wins.
        int[][] layers = new int[REACH * 2 + 1][REACH * 2 + 1];
        float mainRadius = 2.5F + random.nextFloat() * 1.5F;
        addMound(layers, 0, 0, mainRadius, 4 + random.nextInt(4));
        for (int i = 1 + random.nextInt(3); i > 0; i--) {
            float radius = 1.5F + random.nextFloat() * 1.3F;
            float angle = random.nextFloat() * Mth.TWO_PI;
            float distance = (mainRadius + radius) * 0.75F;
            addMound(layers, Mth.cos(angle) * distance, Mth.sin(angle) * distance, radius, 2 + random.nextInt(3));
        }

        BlockState moss = RRBlocks.MOSS_LAYER.get().defaultBlockState();
        boolean placed = false;
        for (int x = 1 - REACH; x < REACH; x++) {
            for (int z = 1 - REACH; z < REACH; z++) {
                int height = layers[x + REACH][z + REACH];
                BlockPos column = origin.offset(x, 0, z);
                if (height > 0) {
                    // A ragged edge instead of a drawn circle.
                    if (height == 1 && random.nextFloat() < 0.25F) {
                        continue;
                    }
                    BlockPos pos = findFloor(level, column, moss);
                    if (pos != null) {
                        BlockState state = moss.setValue(MossLayerBlock.LAYERS, height);
                        if (random.nextFloat() < 0.1F + 0.13F * height) {
                            // Up to four plants, fewer on low moss; a place that comes up twice still holds one.
                            for (int i = 1 + random.nextInt(Math.min(height, 4)); i > 0; i--) {
                                state = state.setValue(Util.getRandom(MossLayerBlock.PLANTS, random).property(), true);
                            }
                            state = state.setValue(MossLayerBlock.TWIGS, random.nextBoolean());
                        }
                        level.setBlock(pos, state, 2);
                        placed = true;
                    }
                } else if ((layers[x + REACH - 1][z + REACH] | layers[x + REACH + 1][z + REACH]
                        | layers[x + REACH][z + REACH - 1] | layers[x + REACH][z + REACH + 1]) > 0
                        && random.nextFloat() < CARPET_CHANCE) {
                    BlockPos pos = findFloor(level, column, moss);
                    if (pos != null) {
                        level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                    }
                }
            }
        }
        return placed;
    }

    /** A round mound that falls from {@code peak} layers in the middle to one layer at the edge. */
    private static void addMound(int[][] layers, float centerX, float centerZ, float radius, int peak) {
        for (int x = Mth.ceil(centerX - radius); x <= centerX + radius; x++) {
            for (int z = Mth.ceil(centerZ - radius); z <= centerZ + radius; z++) {
                float distance = Mth.square(x - centerX) + Mth.square(z - centerZ);
                int height = Mth.ceil(peak * (1 - distance / (radius * radius)));
                layers[x + REACH][z + REACH] = Math.max(layers[x + REACH][z + REACH], height);
            }
        }
    }

    /** The highest free block near the origin level that moss can lie on; the carpets keep to the same ground. */
    private static @Nullable BlockPos findFloor(WorldGenLevel level, BlockPos column, BlockState moss) {
        for (int dy = FLOOR_SEARCH; dy >= -FLOOR_SEARCH; dy--) {
            BlockPos pos = column.above(dy);
            if (level.isEmptyBlock(pos) && moss.canSurvive(level, pos)) {
                return pos;
            }
        }
        return null;
    }
}
