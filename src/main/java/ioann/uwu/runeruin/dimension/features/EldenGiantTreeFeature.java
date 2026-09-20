package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.EldenVinesBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Cherry-like tree with a 2x2 trunk, radiating branches, and a small crown on each branch tip. */
public class EldenGiantTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CROWN_BOTTOM = -3;
    private static final int CROWN_TOP = 3;
    private static final int CROWN_RADIUS = 4;
    private static final int TOP_BRANCH_MIN_LENGTH = 4;
    private static final int TOP_BRANCH_MAX_LENGTH = 6;
    private static final int MAX_DOWNWARD_EXTENSION = 3;
    private static final int LEAF_LITTER_PATCH_ATTEMPTS = 32;
    private static final int LEAF_LITTER_RADIUS = 8;

    private record RootCandidate(BlockPos position, Direction direction) {
    }

    public EldenGiantTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int trunkHeight = 14 + random.nextInt(5);

        if (!RRBlocks.ELDEN_SAPLING.get().defaultBlockState().canSurvive(level, origin)
                || origin.getY() + trunkHeight + TOP_BRANCH_MAX_LENGTH + CROWN_TOP >= level.getMaxY()) {
            return false;
        }

        BlockState trunkState = RRBlocks.ELDEN_WOOD.get().defaultBlockState();
        BlockState logState = RRBlocks.ELDEN_LOG.get().defaultBlockState();
        Set<BlockPos> trunkPositions = new LinkedHashSet<>();

        for (int y = 0; y < trunkHeight; y++) {
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    BlockPos trunkPos = origin.offset(x, y, z);
                    if (!isTreeReplaceable(level.getBlockState(trunkPos))) {
                        return false;
                    }
                    trunkPositions.add(trunkPos);
                }
            }
        }
        addUndergroundTrunk(level, origin, trunkPositions);

        Map<BlockPos, BlockState> branchLogs = new LinkedHashMap<>();
        List<BlockPos> branchTips = new ArrayList<>();
        Set<BlockPos> rootPositions = addBaseRoots(branchLogs, level, origin, random, trunkPositions, logState);
        for (int tier = 0; tier < 3; tier++) {
            int startY = Math.max(4, Math.round(trunkHeight * (0.34F + tier * 0.21F)));
            double angleOffset = tier == 1 ? Math.PI / 4.0 : random.nextDouble() * (Math.PI / 4.0);

            for (int branch = 0; branch < 4; branch++) {
                double angle = angleOffset + branch * Math.PI / 2.0;
                double dirX = Math.cos(angle);
                double dirZ = Math.sin(angle);
                int trunkX = dirX > 0.25 ? 1 : dirX < -0.25 ? 0 : random.nextInt(2);
                int trunkZ = dirZ > 0.25 ? 1 : dirZ < -0.25 ? 0 : random.nextInt(2);
                BlockPos branchStart = origin.offset(trunkX, startY, trunkZ);
                int length = 6 + random.nextInt(4);
                int rise = 1 + random.nextInt(3);
                List<BlockPos> path = addBranch(branchLogs, branchStart, angle, length, rise, trunkPositions, logState);
                if (!path.isEmpty()) {
                    branchTips.add(path.getLast());
                }

                if (path.size() > 3) {
                    BlockPos fork = path.get(Math.min(path.size() - 1, (path.size() * 2) / 3));
                    double forkAngle = angle + (random.nextBoolean() ? 1 : -1) * (Math.PI / 3.0);
                    List<BlockPos> forkPath = addBranch(
                            branchLogs,
                            fork,
                            forkAngle,
                            3 + random.nextInt(3),
                            1 + random.nextInt(2),
                            trunkPositions,
                            logState
                    );
                    if (!forkPath.isEmpty()) {
                        branchTips.add(forkPath.getLast());
                    }
                }
            }
        }

        // Always branch out from the trunk's top so the crown does not leave a bare central stump.
        int topBranchCount = 3 + random.nextInt(3);
        double topAngleOffset = random.nextDouble() * Math.PI * 2.0;
        for (int branch = 0; branch < topBranchCount; branch++) {
            double angle = topAngleOffset + branch * Math.PI * 2.0 / topBranchCount;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            int trunkX = dirX >= 0 ? 1 : 0;
            int trunkZ = dirZ >= 0 ? 1 : 0;
            BlockPos branchStart = origin.offset(trunkX, trunkHeight - 1, trunkZ);
            int length = TOP_BRANCH_MIN_LENGTH
                    + random.nextInt(TOP_BRANCH_MAX_LENGTH - TOP_BRANCH_MIN_LENGTH + 1);
            List<BlockPos> path = addBranch(
                    branchLogs,
                    branchStart,
                    angle,
                    length,
                    length,
                    trunkPositions,
                    logState
            );
            if (!path.isEmpty()) {
                branchTips.add(path.getLast());
            }
        }

        for (BlockPos trunkPos : trunkPositions) {
            level.setBlock(trunkPos, trunkState, 19);
        }

        Set<BlockPos> placedWood = new LinkedHashSet<>(trunkPositions);
        for (Map.Entry<BlockPos, BlockState> branchLog : branchLogs.entrySet()) {
            BlockPos branchPos = branchLog.getKey();
            BlockState existingState = level.getBlockState(branchPos);
            boolean replaceable = rootPositions.contains(branchPos)
                    ? isRootReplaceable(existingState, level, branchPos)
                    : isTreeReplaceable(existingState);
            if (replaceable) {
                level.setBlock(branchPos, branchLog.getValue(), 19);
                placedWood.add(branchPos);
            }
        }

        Set<BlockPos> leaves = createCrowns(branchTips, random);
        leaves.removeAll(branchLogs.keySet());
        leaves.removeAll(placedWood);
        Set<BlockPos> placeableLeaves = new LinkedHashSet<>();
        for (BlockPos leafPos : leaves) {
            if (isTreeReplaceable(level.getBlockState(leafPos))) {
                placeableLeaves.add(leafPos);
            }
        }

        Map<BlockPos, Integer> leafDistances = calculateLeafDistances(placedWood, placeableLeaves);
        for (BlockPos leafPos : placeableLeaves) {
            int distance = leafDistances.getOrDefault(leafPos, LeavesBlock.DECAY_DISTANCE);
            BlockState leafState = RRBlocks.ELDEN_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, distance);
            level.setBlock(leafPos, leafState, 19);
        }

        placeEldenVines(level, placeableLeaves, random);
        placeEldenLeafLitter(level, origin, random);

        return true;
    }

    private static void placeEldenLeafLitter(WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < LEAF_LITTER_PATCH_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 3.0 + random.nextDouble() * (LEAF_LITTER_RADIUS - 3.0);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos target = findLeafLitterPosition(level, x, z, origin.getY());
            if (target == null) {
                continue;
            }

            BlockState existing = level.getBlockState(target);
            if (existing.is(RRBlocks.ELDEN_LEAF_LITTER.get())) {
                int amount = existing.getValue(BlockStateProperties.SEGMENT_AMOUNT);
                if (amount < LeafLitterBlock.MAX_SEGMENT && random.nextBoolean()) {
                    level.setBlock(target, existing.setValue(BlockStateProperties.SEGMENT_AMOUNT, amount + 1), 19);
                }
                continue;
            }

            if (!existing.isAir() && (!existing.canBeReplaced() || !existing.getFluidState().isEmpty())) {
                continue;
            }

            BlockState litter = RRBlocks.ELDEN_LEAF_LITTER.get().defaultBlockState()
                    .setValue(LeafLitterBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))
                    .setValue(BlockStateProperties.SEGMENT_AMOUNT, 1 + random.nextInt(LeafLitterBlock.MAX_SEGMENT));
            level.setBlock(target, litter, 19);
        }
    }

    private static BlockPos findLeafLitterPosition(WorldGenLevel level, int x, int z, int baseY) {
        for (int y = baseY + 1; y >= baseY - LEAF_LITTER_RADIUS; y--) {
            BlockPos support = new BlockPos(x, y, z);
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) {
                continue;
            }

            BlockPos target = support.above();
            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.is(RRBlocks.ELDEN_LEAF_LITTER.get())
                    || (state.canBeReplaced() && state.getFluidState().isEmpty())) {
                return target;
            }
        }
        return null;
    }

    private static void placeEldenVines(WorldGenLevel level, Set<BlockPos> leaves, RandomSource random) {
        for (BlockPos leafPos : leaves) {
            if (random.nextFloat() >= 0.08F) {
                continue;
            }

            BlockPos vinePos = leafPos.below();
            if (vinePos.getY() < level.getMinY() || !level.getBlockState(vinePos).isAir()) {
                continue;
            }

            int length = 1 + random.nextInt(4);
            for (int segment = 0; segment < length && vinePos.getY() >= level.getMinY(); segment++) {
                if (!level.getBlockState(vinePos).isAir()) {
                    break;
                }

                boolean hasOrb = segment == 0 || random.nextInt(4) == 0;
                BlockState vineState = RRBlocks.ELDEN_VINES.get().defaultBlockState()
                        .setValue(EldenVinesBlock.ORB, hasOrb);
                level.setBlock(vinePos, vineState, 19);
                vinePos = vinePos.below();
            }
        }
    }

    private static boolean isTreeReplaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static boolean isRootReplaceable(BlockState state, WorldGenLevel level, BlockPos pos) {
        return isTreeReplaceable(state)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.GRASS_BLOCKS)
                || !state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isNonFullTreeReplaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isRootReplaceable(state, level, pos) && !state.isCollisionShapeFullBlock(level, pos);
    }

    private static void addUndergroundTrunk(WorldGenLevel level, BlockPos origin, Set<BlockPos> trunkPositions) {
        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z <= 1; z++) {
                for (int depth = 1; depth <= MAX_DOWNWARD_EXTENSION; depth++) {
                    BlockPos undergroundPos = origin.offset(x, -depth, z);
                    if (undergroundPos.getY() < level.getMinY()
                            || !isNonFullTreeReplaceable(level, undergroundPos)) {
                        break;
                    }
                    trunkPositions.add(undergroundPos);
                }
            }
        }
    }

    private static Set<BlockPos> addBaseRoots(
            Map<BlockPos, BlockState> roots,
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            Set<BlockPos> trunkPositions,
        BlockState baseLog
    ) {
        Set<BlockPos> rootPositions = new LinkedHashSet<>();
        List<RootCandidate> candidates = new ArrayList<>(List.of(
                new RootCandidate(origin.offset(random.nextInt(2), 0, -1), Direction.NORTH),
                new RootCandidate(origin.offset(2, 0, random.nextInt(2)), Direction.EAST),
                new RootCandidate(origin.offset(random.nextInt(2), 0, 2), Direction.SOUTH),
                new RootCandidate(origin.offset(-1, 0, random.nextInt(2)), Direction.WEST)
        ));
        int rootCount = 3 + random.nextInt(2);

        for (int i = 0; i < rootCount; i++) {
            int selected = i + random.nextInt(candidates.size() - i);
            RootCandidate candidate = candidates.get(selected);
            candidates.set(selected, candidates.get(i));
            candidates.set(i, candidate);

            BlockPos rootPos = candidate.position();
            if (trunkPositions.contains(rootPos)
                    || !isRootReplaceable(level.getBlockState(rootPos), level, rootPos)) {
                continue;
            }

            BlockPos belowRootPos = rootPos.below();
            boolean moveRootDown = belowRootPos.getY() >= level.getMinY()
                    && level.getBlockState(belowRootPos).isAir();
            BlockPos rootBasePos = moveRootDown ? belowRootPos : rootPos;
            if (trunkPositions.contains(rootBasePos)
                    || !isRootReplaceable(level.getBlockState(rootBasePos), level, rootBasePos)) {
                continue;
            }

            roots.put(rootBasePos, baseLog.setValue(RotatedPillarBlock.AXIS, candidate.direction().getAxis()));
            rootPositions.add(rootBasePos);

            BlockPos lowerRootPos = rootBasePos.below();
            boolean lowerRootReplaceable = lowerRootPos.getY() >= level.getMinY()
                    && isRootReplaceable(level.getBlockState(lowerRootPos), level, lowerRootPos);
            boolean extendDown = lowerRootReplaceable
                    && (random.nextBoolean() || isNonFullTreeReplaceable(level, lowerRootPos));
            int rootDepth = 1 + random.nextInt(MAX_DOWNWARD_EXTENSION);
            for (int depth = 0; depth < rootDepth && extendDown; depth++) {
                roots.put(lowerRootPos, baseLog.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
                rootPositions.add(lowerRootPos);
                lowerRootPos = lowerRootPos.below();
                extendDown = lowerRootPos.getY() >= level.getMinY()
                        && isNonFullTreeReplaceable(level, lowerRootPos);
            }
        }

        return rootPositions;
    }

    private static List<BlockPos> addBranch(
            Map<BlockPos, BlockState> branches,
            BlockPos start,
            double angle,
            int length,
            int rise,
            Set<BlockPos> trunkPositions,
            BlockState baseLog
    ) {
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        List<BlockPos> path = new java.util.ArrayList<>();
        BlockPos previous = start;

        for (int step = 1; step <= length; step++) {
            int targetX = start.getX() + (int) Math.round(dirX * step);
            int targetY = start.getY() + (int) Math.round((double) rise * step / length);
            int targetZ = start.getZ() + (int) Math.round(dirZ * step);

            while (previous.getX() != targetX || previous.getY() != targetY || previous.getZ() != targetZ) {
                int x = previous.getX();
                int y = previous.getY();
                int z = previous.getZ();
                Direction.Axis axis;

                if (x != targetX && (z == targetZ || Math.abs(dirX) >= Math.abs(dirZ))) {
                    x += Integer.signum(targetX - x);
                    axis = Direction.Axis.X;
                } else if (z != targetZ) {
                    z += Integer.signum(targetZ - z);
                    axis = Direction.Axis.Z;
                } else {
                    y += Integer.signum(targetY - y);
                    axis = Direction.Axis.Y;
                }

                BlockPos pos = new BlockPos(x, y, z);
                if (!trunkPositions.contains(pos)) {
                    branches.put(pos, baseLog.setValue(RotatedPillarBlock.AXIS, axis));
                }
                path.add(pos);
                previous = pos;
            }
        }

        return path;
    }

    private static Set<BlockPos> createCrowns(List<BlockPos> branchTips, RandomSource random) {
        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (BlockPos branchTip : branchTips) {
            BlockPos center = branchTip.above();
            double scale = 0.85 + random.nextDouble() * 0.2;

            for (int dy = CROWN_BOTTOM; dy <= CROWN_TOP; dy++) {
                double radius = crownRadius(dy) * scale;
                double radiusSquared = radius * radius;

                for (int dx = -CROWN_RADIUS; dx <= CROWN_RADIUS; dx++) {
                    for (int dz = -CROWN_RADIUS; dz <= CROWN_RADIUS; dz++) {
                        double distanceSquared = (double) dx * dx + (double) dz * dz;
                        if (distanceSquared > radiusSquared) {
                            continue;
                        }

                        double edgeDistance = radiusSquared - distanceSquared;
                        if (edgeDistance < 1.2 && random.nextFloat() < 0.05F) {
                            continue;
                        }

                        leaves.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }

        return leaves;
    }

    private static double crownRadius(int dy) {
        return switch (dy) {
            case -3 -> 1.7;
            case -2 -> 2.8;
            case -1 -> 3.5;
            case 0 -> 4.0;
            case 1 -> 3.8;
            case 2 -> 2.8;
            default -> 1.7;
        };
    }

    private static Map<BlockPos, Integer> calculateLeafDistances(Set<BlockPos> wood, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (BlockPos woodPos : wood) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = woodPos.relative(direction);
                if (leaves.contains(neighbor) && distances.putIfAbsent(neighbor, 1) == null) {
                    queue.add(neighbor);
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos leafPos = queue.remove();
            int nextDistance = distances.get(leafPos) + 1;
            if (nextDistance >= LeavesBlock.DECAY_DISTANCE) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = leafPos.relative(direction);
                if (leaves.contains(neighbor) && distances.putIfAbsent(neighbor, nextDistance) == null) {
                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }
}
