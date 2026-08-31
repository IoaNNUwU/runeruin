package ioann.uwu.runeruin.dimension.features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

/**
 * Lush-caves moss patch that only replaces goblet bud, eats the whole bud column,
 * and hangs irregular moss strands underneath. Stem / vein blocks are never written.
 */
public class GobletMossPatchFeature extends VegetationPatchFeature {

    private static final IntProvider STRAND_COUNT = UniformInt.of(5, 14);
    private static final IntProvider DRIP_HEIGHT = UniformInt.of(2, 8);
    private static final IntProvider CLOUD_HEIGHT = UniformInt.of(3, 9);
    private static final IntProvider CLUMP_HEIGHT = UniformInt.of(4, 8);
    private static final IntProvider CLUMP_RADIUS = UniformInt.of(1, 3);

    public GobletMossPatchFeature() {
        super(VegetationPatchConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        VegetationPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        Predicate<BlockState> replaceable = state -> state.is(config.replaceable());
        int xRadius = config.xzRadius().sample(random) + 1;
        int zRadius = config.xzRadius().sample(random) + 1;
        Set<BlockPos> surface = this.placeGroundPatch(level, config, random, origin, replaceable, xRadius, zRadius);
        this.distributeVegetation(context, level, config, random, surface, xRadius, zRadius);
        if (surface.isEmpty()) {
            return false;
        }
        Set<BlockPos> ceiling = findMossCeiling(level, origin, config, replaceable, xRadius, zRadius);
        if (!ceiling.isEmpty()) {
            placeHangingMoss(level, ceiling, random, config);
        }
        return true;
    }

    private static Set<BlockPos> findMossCeiling(
            WorldGenLevel level,
            BlockPos origin,
            VegetationPatchConfiguration config,
            Predicate<BlockState> replaceable,
            int xRadius,
            int zRadius
    ) {
        Set<BlockPos> ceiling = new HashSet<>();
        BlockPos.MutableBlockPos cursor = origin.mutable();
        int maxSteps = config.verticalRange() + config.depth().maxInclusive() + 8;

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dz = -zRadius; dz <= zRadius; dz++) {
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                int steps = 0;
                while (steps < maxSteps && level.getBlockState(cursor).isAir()) {
                    cursor.move(Direction.DOWN);
                    steps++;
                }
                if (!isMossColumn(level.getBlockState(cursor), replaceable)) {
                    continue;
                }
                while (steps < maxSteps && isMossColumn(level.getBlockState(cursor.below()), replaceable)) {
                    cursor.move(Direction.DOWN);
                    steps++;
                }
                if (level.getBlockState(cursor.below()).isAir()) {
                    ceiling.add(cursor.immutable());
                }
            }
        }
        return ceiling;
    }

    private static boolean isMossColumn(BlockState state, Predicate<BlockState> replaceable) {
        return state.is(Blocks.MOSS_BLOCK) || replaceable.test(state);
    }

    private static void placeHangingMoss(
            WorldGenLevel level,
            Set<BlockPos> ceiling,
            RandomSource random,
            VegetationPatchConfiguration config
    ) {
        BlockState moss = config.groundState().getState(level, random, ceiling.iterator().next());
        List<BlockPos> candidates = new ArrayList<>(ceiling);
        shuffle(candidates, random);

        int targetStrands = Math.min(candidates.size(), STRAND_COUNT.sample(random));
        int placed = 0;

        for (BlockPos attach : candidates) {
            if (placed >= targetStrands && random.nextFloat() > 0.2F) {
                continue;
            }
            if (placed < targetStrands || random.nextFloat() < 0.3F) {
                placeRandomStrand(level, attach, random, moss);
                placed++;
            }
        }
    }

    private static void placeRandomStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss) {
        switch (random.nextInt(3)) {
            case 0 -> placeDripStrand(level, attach, random, moss);
            case 1 -> placeCloudStrand(level, attach, random, moss);
            default -> placeTaperedClump(level, attach, random, moss);
        }
    }

    /** Thin strand that wanders slightly sideways while descending. */
    private static void placeDripStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss) {
        int height = DRIP_HEIGHT.sample(random);
        BlockPos.MutableBlockPos pos = attach.mutable();

        for (int i = 0; i < height; i++) {
            pos.move(Direction.DOWN);
            if (random.nextFloat() < 0.4F) {
                pos.move(random.nextBoolean() ? Direction.EAST : Direction.WEST);
            }
            if (random.nextFloat() < 0.4F) {
                pos.move(random.nextBoolean() ? Direction.NORTH : Direction.SOUTH);
            }
            if (!level.getBlockState(pos).isAir()) {
                break;
            }
            if (!canHangFrom(level, pos, moss)) {
                break;
            }
            level.setBlock(pos, moss, 2);
        }
    }

    /** Irregular blob per layer — sparse and uneven edges. */
    private static void placeCloudStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss) {
        int height = CLOUD_HEIGHT.sample(random);
        int baseSpread = UniformInt.of(1, 2).sample(random);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = 1; dy <= height; dy++) {
            int spread = Math.max(0, baseSpread + random.nextInt(3) - 1 - dy / 3);
            int y = attach.getY() - dy;
            for (int dx = -spread; dx <= spread; dx++) {
                for (int dz = -spread; dz <= spread; dz++) {
                    if (dx * dx + dz * dz > spread * spread + random.nextInt(2)) {
                        continue;
                    }
                    if (random.nextFloat() > 0.5F - dy * 0.04F) {
                        continue;
                    }
                    pos.set(attach.getX() + dx, y, attach.getZ() + dz);
                    if (!level.getBlockState(pos).isAir() || !canHangFrom(level, pos, moss)) {
                        continue;
                    }
                    level.setBlock(pos, moss, 2);
                }
            }
        }
    }

    /** Small tapering clump with noisy radius per layer. */
    private static void placeTaperedClump(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss) {
        int radius = CLUMP_RADIUS.sample(random);
        int height = CLUMP_HEIGHT.sample(random);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int denom = Math.max(height - 1, 1);

        for (int dy = 1; dy <= height; dy++) {
            int layerRadius = (int) Math.round(radius * (height - dy) / (double) denom);
            layerRadius += random.nextInt(3) - 1;
            layerRadius = Math.max(0, layerRadius);
            int y = attach.getY() - dy;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > layerRadius * layerRadius) {
                        continue;
                    }
                    if (random.nextFloat() > 0.65F) {
                        continue;
                    }
                    pos.set(attach.getX() + dx, y, attach.getZ() + dz);
                    if (!level.getBlockState(pos).isAir() || !canHangFrom(level, pos, moss)) {
                        continue;
                    }
                    level.setBlock(pos, moss, 2);
                }
            }
        }
    }

    /** Only hang blocks that have moss directly above — avoids floating edges on the bowl rim. */
    private static boolean canHangFrom(WorldGenLevel level, BlockPos pos, BlockState moss) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(moss.getBlock());
    }

    private static void shuffle(List<BlockPos> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
