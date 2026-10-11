package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.DeepMossLayerBlock;
import ioann.uwu.runeruin.blocks.MossLayerPlantBlock;
import ioann.uwu.runeruin.blocks.MossberryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WispberryBushBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * A group of deep moss mounds standing together: a main one and a few smaller ones against its side, each
 * tallest in the middle. Moss sprouts and wispberry bushes grow out of the mounds and around them, on the
 * moss floor. A few groups also have a clump of mossberry bushes.
 */
public class MossHummockFeature extends Feature<NoneFeatureConfiguration> {

    /** Columns around the origin that a group can reach; small enough to stay inside the neighbouring chunks. */
    private static final int REACH = 10;
    private static final int FLOOR_SEARCH = 3;
    /** The part of the columns, of the mounds and around them, that grow sprouts; half of the sprouts are small. */
    private static final float SPROUTS_CHANCE = 0.24F;
    private static final float WISPBERRY_CHANCE = 0.08F;
    /** The part of the groups that have a clump of mossberry bushes, and how much of a clump is bushes. */
    private static final float MOSSBERRY_CLUMP_CHANCE = 0.2F;
    private static final float MOSSBERRY_CHANCE = 0.7F;

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

        // The clump lies somewhere on the main mound or at its foot, about three columns wide.
        boolean clump = random.nextFloat() < MOSSBERRY_CLUMP_CHANCE;
        float clumpAngle = random.nextFloat() * Mth.TWO_PI;
        float clumpDistance = random.nextFloat() * (mainRadius + 1);
        float clumpX = Mth.cos(clumpAngle) * clumpDistance;
        float clumpZ = Mth.sin(clumpAngle) * clumpDistance;

        BlockState moss = RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState();
        boolean placed = false;
        for (int x = 1 - REACH; x < REACH; x++) {
            for (int z = 1 - REACH; z < REACH; z++) {
                int height = layers[x + REACH][z + REACH];
                boolean rim = height == 0 && (layers[x + REACH - 1][z + REACH] | layers[x + REACH + 1][z + REACH]
                        | layers[x + REACH][z + REACH - 1] | layers[x + REACH][z + REACH + 1]) > 0;
                float roll = random.nextFloat();
                // A ragged edge instead of a drawn circle.
                if (height == 1 && roll < 0.25F) {
                    continue;
                }
                BlockState plant = clump && Mth.square(x - clumpX) + Mth.square(z - clumpZ) < 2.5F
                        ? (random.nextFloat() < MOSSBERRY_CHANCE ? mossberryBush(random) : null)
                        : height > 0 || rim ? plant(random, roll) : null;
                if (height == 0 && plant == null) {
                    continue;
                }
                BlockPos pos = findFloor(level, origin.offset(x, 0, z), moss);
                if (pos == null) {
                    continue;
                }
                if (height > 0) {
                    // Most of a mound is bare moss; a plant in the rest stands in the moss of its column.
                    level.setBlock(pos, plant == null || height > MossLayerPlantBlock.MAX_MOSS
                            ? moss.setValue(DeepMossLayerBlock.LAYERS, height)
                            : plant.setValue(MossLayerPlantBlock.MOSS, height), 2);
                    placed = true;
                } else if (plant.canSurvive(level, pos)) {
                    level.setBlock(pos, plant, 2);
                }
            }
        }
        return placed;
    }

    /** Sprouts or a wispberry bush of any age for a part of the columns, nothing for the rest. */
    private static @Nullable BlockState plant(RandomSource random, float roll) {
        if (roll >= 1 - WISPBERRY_CHANCE) {
            return RRBlocks.WISPBERRY_BUSH.get().defaultBlockState().setValue(WispberryBushBlock.AGE, random.nextInt(4));
        }
        if (roll >= 1 - WISPBERRY_CHANCE - SPROUTS_CHANCE) {
            return (random.nextBoolean() ? RRBlocks.MOSS_SPROUTS : RRBlocks.SMALL_MOSS_SPROUTS).get().defaultBlockState();
        }
        return null;
    }

    /** A bush with one to three berries; a place that comes up twice still holds one. */
    private static BlockState mossberryBush(RandomSource random) {
        BlockState bush = RRBlocks.MOSSBERRY_BUSH.get().defaultBlockState().setValue(MossberryBushBlock.TWIGS, random.nextBoolean());
        for (int i = 1 + random.nextInt(3); i > 0; i--) {
            bush = bush.setValue(Util.getRandom(MossberryBushBlock.BERRIES, random).property(), true);
        }
        return bush;
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

    /** The highest free block near the origin level that moss can lie on; the bushes keep to the same ground. */
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
