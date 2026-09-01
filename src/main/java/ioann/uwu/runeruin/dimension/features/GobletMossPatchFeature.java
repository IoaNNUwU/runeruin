package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.MossBerryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.SpeleothemUtils;
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
    private static final float MOSS_BERRY_CHANCE = 0.3F;

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
        boolean underwater = config.vegetationChance() <= 0.0F;
        Set<BlockPos> surface = underwater
                ? this.placeGroundPatchUnderwater(level, config, random, origin, replaceable, xRadius, zRadius)
                : super.placeGroundPatch(level, config, random, origin, replaceable, xRadius, zRadius);
        if (!underwater) {
            this.distributeVegetation(context, level, config, random, surface, xRadius, zRadius);
        }
        if (surface.isEmpty()) {
            return false;
        }
        Set<BlockPos> ceiling = findMossCeiling(level, origin, config, replaceable, xRadius, zRadius, underwater);
        if (!ceiling.isEmpty()) {
            placeHangingMoss(level, ceiling, random, config, underwater, replaceable);
        }
        return true;
    }

    @Override
    protected void distributeVegetation(
            FeaturePlaceContext<VegetationPatchConfiguration> context,
            WorldGenLevel level,
            VegetationPatchConfiguration config,
            RandomSource random,
            Set<BlockPos> surface,
            int xRadius,
            int zRadius
    ) {
        super.distributeVegetation(context, level, config, random, surface, xRadius, zRadius);
        if (config.vegetationChance() > 0.0F) {
            placeMossBerryBushes(level, config, random, surface);
        }
    }

    private static void placeMossBerryBushes(
            WorldGenLevel level,
            VegetationPatchConfiguration config,
            RandomSource random,
            Set<BlockPos> surface
    ) {
        Direction above = config.surface().getDirection().getOpposite();

        for (BlockPos ground : surface) {
            if (random.nextFloat() >= MOSS_BERRY_CHANCE) {
                continue;
            }
            BlockPos bushPos = ground.relative(above);
            if (!level.isEmptyBlock(bushPos) || !level.getBlockState(ground).is(Blocks.MOSS_BLOCK)) {
                continue;
            }
            int age = random.nextInt(4);
            level.setBlock(
                    bushPos,
                    RRBlocks.MOSS_BERRY_BUSH.get().defaultBlockState().setValue(MossBerryBushBlock.AGE, age),
                    2
            );
        }
    }

    private Set<BlockPos> placeGroundPatchUnderwater(
            WorldGenLevel level,
            VegetationPatchConfiguration config,
            RandomSource random,
            BlockPos origin,
            Predicate<BlockState> replaceable,
            int xRadius,
            int zRadius
    ) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        BlockPos.MutableBlockPos belowPos = pos.mutable();
        Direction inwards = config.surface().getDirection();
        Direction outwards = inwards.getOpposite();
        Set<BlockPos> surface = new HashSet<>();
        Predicate<BlockState> passable = SpeleothemUtils::isEmptyOrWater;
        Predicate<BlockState> solid = SpeleothemUtils::isNeitherEmptyNorWater;

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            boolean isXEdge = dx == -xRadius || dx == xRadius;

            for (int dz = -zRadius; dz <= zRadius; dz++) {
                boolean isZEdge = dz == -zRadius || dz == zRadius;
                boolean isEdge = isXEdge || isZEdge;
                boolean isCorner = isXEdge && isZEdge;
                boolean isEdgeButNotCorner = isEdge && !isCorner;
                if (!isCorner && (!isEdgeButNotCorner || config.extraEdgeColumnChance() != 0.0F && !(random.nextFloat() > config.extraEdgeColumnChance()))) {
                    pos.setWithOffset(origin, dx, 0, dz);

                    for (int offset = 0; level.isStateAtPosition(pos, passable) && offset < config.verticalRange(); offset++) {
                        pos.move(inwards);
                    }

                    for (int steps = 0; level.isStateAtPosition(pos, solid) && steps < config.verticalRange(); steps++) {
                        pos.move(outwards);
                    }

                    belowPos.setWithOffset(pos, config.surface().getDirection());
                    BlockState belowState = level.getBlockState(belowPos);
                    if (level.isStateAtPosition(pos, passable)
                            && belowState.isFaceSturdy(level, belowPos, config.surface().getDirection().getOpposite())) {
                        // Underwater: не заменяем goblet_bud мхом, только собираем поверхность для hanging moss
                        if (replaceable.test(belowState) || belowState.is(Blocks.MOSS_BLOCK)) {
                            surface.add(belowPos.immutable());
                        }
                    }
                }
            }
        }

        return surface;
    }

    private static Set<BlockPos> findMossCeiling(
            WorldGenLevel level,
            BlockPos origin,
            VegetationPatchConfiguration config,
            Predicate<BlockState> replaceable,
            int xRadius,
            int zRadius,
            boolean underwater
    ) {
        Predicate<BlockState> passable = underwater ? SpeleothemUtils::isEmptyOrWater : BlockBehaviour.BlockStateBase::isAir;
        Set<BlockPos> ceiling = new HashSet<>();
        BlockPos.MutableBlockPos cursor = origin.mutable();
        int maxSteps = config.verticalRange() + config.depth().maxInclusive() + 8;

        for (int dx = -xRadius; dx <= xRadius; dx++) {
            for (int dz = -zRadius; dz <= zRadius; dz++) {
                cursor.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                int steps = 0;
                while (steps < maxSteps && passable.test(level.getBlockState(cursor))) {
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
                if (passable.test(level.getBlockState(cursor.below()))) {
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
            VegetationPatchConfiguration config,
            boolean underwater,
            Predicate<BlockState> replaceable
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
                placeRandomStrand(level, attach, random, moss, underwater, replaceable);
                placed++;
            }
        }
    }

    private static void placeRandomStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss, boolean underwater, Predicate<BlockState> replaceable) {
        switch (random.nextInt(3)) {
            case 0 -> placeDripStrand(level, attach, random, moss, underwater, replaceable);
            case 1 -> placeCloudStrand(level, attach, random, moss, underwater, replaceable);
            default -> placeTaperedClump(level, attach, random, moss, underwater, replaceable);
        }
    }

    private static boolean isPassable(BlockState state, boolean underwater) {
        return underwater ? SpeleothemUtils.isEmptyOrWater(state) : state.isAir();
    }

    /** Thin strand that wanders slightly sideways while descending. */
    private static void placeDripStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss, boolean underwater, Predicate<BlockState> replaceable) {
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
            if (!isPassable(level.getBlockState(pos), underwater)) {
                break;
            }
            if (!canHangFrom(level, pos, moss, replaceable)) {
                break;
            }
            level.setBlock(pos, moss, 2);
        }
    }

    /** Irregular blob per layer — sparse and uneven edges. */
    private static void placeCloudStrand(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss, boolean underwater, Predicate<BlockState> replaceable) {
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
                    if (!isPassable(level.getBlockState(pos), underwater) || !canHangFrom(level, pos, moss, replaceable)) {
                        continue;
                    }
                    level.setBlock(pos, moss, 2);
                }
            }
        }
    }

    /** Small tapering clump with noisy radius per layer. */
    private static void placeTaperedClump(WorldGenLevel level, BlockPos attach, RandomSource random, BlockState moss, boolean underwater, Predicate<BlockState> replaceable) {
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
                    if (!isPassable(level.getBlockState(pos), underwater) || !canHangFrom(level, pos, moss, replaceable)) {
                        continue;
                    }
                    level.setBlock(pos, moss, 2);
                }
            }
        }
    }

    /** Only hang blocks that have moss directly above — avoids floating edges on the bowl rim. */
    private static boolean canHangFrom(WorldGenLevel level, BlockPos pos, BlockState moss, Predicate<BlockState> replaceable) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(moss.getBlock()) || replaceable.test(above);
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
