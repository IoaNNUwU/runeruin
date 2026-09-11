package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.BigLilyPadBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Water-aware vegetation patch for the connected big lily pad.
 *
 * <p>Vanilla {@code vegetation_patch} replaces solid ground and therefore
 * cannot use the top block of a water column as its surface. This feature
 * keeps the patch-style placement, but collects exposed water cells instead
 * and writes complete 1x1, 2x2, or 3x3 pads directly. Direct placement is
 * intentional: world generation does not call {@code setPlacedBy}, so relying
 * on the block's player-placement merge would leave every cell as a singleton.</p>
 */
public class LilyPadPatchFeature extends Feature<LilyPadPatchFeature.Config> {

    public LilyPadPatchFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        RandomSource random = context.random();

        int xRadius = config.xzRadius().sample(random) + 1;
        int zRadius = config.xzRadius().sample(random) + 1;
        List<BlockPos> waterSurfaces = collectWaterSurfaces(
                level,
                context.origin(),
                xRadius,
                zRadius,
                config.verticalRange()
        );
        if (waterSurfaces.isEmpty()) {
            return false;
        }

        Util.shuffle(waterSurfaces, random);

        int placed = 0;
        placed += placePads(level, random, waterSurfaces, config.threeByThreePads().sample(random), 3);
        placed += placePads(level, random, waterSurfaces, config.twoByTwoPads().sample(random), 2);
        placed += placePads(level, random, waterSurfaces, config.oneByOnePads().sample(random), 1);
        return placed > 0;
    }

    private static List<BlockPos> collectWaterSurfaces(
            WorldGenLevel level,
            BlockPos origin,
            int xRadius,
            int zRadius,
            int verticalRange
    ) {
        List<BlockPos> surfaces = new ArrayList<>();
        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dz = -zRadius; dz <= zRadius; dz++) {
                BlockPos surface = findWaterSurface(level, origin.offset(dx, 0, dz), verticalRange);
                if (surface != null) {
                    surfaces.add(surface);
                }
            }
        }
        return surfaces;
    }

    private static BlockPos findWaterSurface(WorldGenLevel level, BlockPos origin, int verticalRange) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int steps = 0; steps <= verticalRange; steps++) {
            BlockState state = level.getBlockState(cursor);
            if (state.is(Blocks.WATER)) {
                BlockPos surface = cursor.above();
                return level.isEmptyBlock(surface) && level.getFluidState(surface).isEmpty()
                        ? surface
                        : null;
            }
            if (!state.isAir()) {
                return null;
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static int placePads(
            WorldGenLevel level,
            RandomSource random,
            List<BlockPos> waterSurfaces,
            int targetCount,
            int size
    ) {
        int placed = 0;
        for (BlockPos anchor : waterSurfaces) {
            if (placed >= targetCount) {
                break;
            }

            BlockPos padOrigin = anchor.offset(-random.nextInt(size), 0, -random.nextInt(size));
            if (placePad(level, padOrigin, size, Direction.Plane.HORIZONTAL.getRandomDirection(random))) {
                placed++;
            }
        }
        return placed;
    }

    private static boolean placePad(WorldGenLevel level, BlockPos origin, int size, Direction facing) {
        BlockState baseState = RRBlocks.BIG_LILY_PAD.get().defaultBlockState();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos pos = origin.offset(x, 0, z);
                if (!level.isEmptyBlock(pos)
                        || !level.getFluidState(pos).isEmpty()
                        || !level.getBlockState(pos.below()).is(Blocks.WATER)) {
                    return false;
                }

                BlockState state = baseState
                        .setValue(BigLilyPadBlock.PART, BigLilyPadBlock.partAt(size, x, z))
                        .setValue(BigLilyPadBlock.FACING, facing);
                if (!state.canSurvive(level, pos)) {
                    return false;
                }
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                level.setBlock(
                        origin.offset(x, 0, z),
                        baseState
                                .setValue(BigLilyPadBlock.PART, BigLilyPadBlock.partAt(size, x, z))
                                .setValue(BigLilyPadBlock.FACING, facing),
                        2
                );
            }
        }
        return true;
    }

    public record Config(
            IntProvider threeByThreePads,
            IntProvider twoByTwoPads,
            IntProvider oneByOnePads,
            IntProvider xzRadius,
            int verticalRange
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                IntProviders.codec(0, 32).fieldOf("three_by_three_pads").forGetter(Config::threeByThreePads),
                IntProviders.codec(0, 32).fieldOf("two_by_two_pads").forGetter(Config::twoByTwoPads),
                IntProviders.codec(0, 64).fieldOf("one_by_one_pads").forGetter(Config::oneByOnePads),
                IntProviders.codec(1, 32).fieldOf("xz_radius").forGetter(Config::xzRadius),
                Codec.intRange(1, 32).fieldOf("vertical_range").forGetter(Config::verticalRange)
        ).apply(codec, Config::new));
    }

}
