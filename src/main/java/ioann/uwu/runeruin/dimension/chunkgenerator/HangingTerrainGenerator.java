package ioann.uwu.runeruin.dimension.chunkgenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Builds deterministic hanging profiles along exposed edges of noise-defined terrain. */
public final class HangingTerrainGenerator {

    private static final int MAX_HANGING_LENGTH = 6;
    private static final int MAX_CLIFF_RUN_SCAN = 32;
    private static final int MAX_JOIN_CLEAR_LENGTH = 3;
    private static final int MAX_BRIDGE_LENGTH = 2;
    private static final int MAX_BRIDGE_CLEAR_LENGTH = 6;
    private static final int MAX_CLEAR_LENGTH = 7;
    private static final int[] WIDE_HANGING_TEMPLATE = {1, 3, 2, 1, 2};
    private static final int[] NATURAL_FALLBACK_PATTERN = {
            2, 3, 3, 2, 1, 2, 2, 1, 1, 2, 3, 2
    };

    private HangingTerrainGenerator() {}

    public static void generate(ChunkAccess chunk, RandomState randomState, Profile profile) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        Map<Column, Candidate> coreCandidates = new LinkedHashMap<>();
        Map<Column, Candidate> slopeCandidates = new LinkedHashMap<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkMinX + x;
                int worldZ = chunkMinZ + z;
                TerrainColumn target = profile.columns().at(worldX, worldZ, randomState);

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    int supportX = worldX + direction.getStepX();
                    int supportZ = worldZ + direction.getStepZ();
                    TerrainColumn support = profile.columns().at(supportX, supportZ, randomState);
                    if (!support.present()) {
                        continue;
                    }

                    if (support.topY() > profile.highUndersideY() + 1) {
                        int startY = support.topY() - 1;
                        boolean validSupport = !isInsideChunk(supportX, supportZ, chunkMinX, chunkMinZ)
                                || profile.supports().core().test(chunk.getBlockState(new BlockPos(supportX, startY, supportZ)));
                        if (validSupport && (!target.present() || startY > target.topY())) {
                            addCandidate(chunk, profile, new BlockPos(worldX, startY, worldZ), coreCandidates);
                        }
                    }

                    int startY = support.topY() - 1;
                    int supportBottomY = (int) support.baseline();
                    boolean exposedBottom = !target.present() || supportBottomY < (int) target.baseline();
                    boolean exposedSlope = !target.present() || support.topY() - target.topY() > 1;
                    if (!exposedBottom && !exposedSlope) {
                        continue;
                    }
                    boolean validSurface = !isInsideChunk(supportX, supportZ, chunkMinX, chunkMinZ)
                            || profile.supports().surface().test(chunk.getBlockState(new BlockPos(supportX, support.topY(), supportZ)));
                    if (!validSurface) {
                        continue;
                    }
                    BlockPos start = new BlockPos(worldX, startY, worldZ);
                    if (exposedBottom && (!target.present() || startY > target.topY())) {
                        addCandidate(chunk, profile, start, coreCandidates);
                    }
                    if (exposedSlope) {
                        addCandidate(chunk, profile, start, slopeCandidates);
                    }
                }
            }
        }

        Map<Column, Candidate> candidates = new LinkedHashMap<>(coreCandidates);
        for (Map.Entry<Column, Candidate> entry : slopeCandidates.entrySet()) {
            if (!candidates.containsKey(entry.getKey())) {
                Candidate smoothed = smoothSlopeCandidate(entry.getValue(), coreCandidates);
                if (smoothed != null) {
                    candidates.put(entry.getKey(), smoothed);
                }
            }
        }

        PositionalRandomFactory shapeRandom = randomState.getOrCreateRandomFactory(profile.shapeRandom());
        Map<Column, Integer> shapeLengths = new LinkedHashMap<>();
        for (Candidate candidate : candidates.values()) {
            BlockPos start = candidate.start();
            shapeLengths.put(
                    new Column(start.getX(), start.getZ()),
                    shapedHangingLength(candidate, randomState, shapeRandom, profile)
            );
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Candidate candidate : candidates.values()) {
            BlockPos start = candidate.start();
            int shapeLength = shapeLengths.get(new Column(start.getX(), start.getZ()));
            Plan plan = planHanging(candidate, shapeLength);
            int hangingLength = plan.length();

            boolean clear = true;
            for (int i = 0; i <= hangingLength; i++) {
                if (!chunk.getBlockState(start.below(i)).isAir()) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                continue;
            }

            BlockState cap = profile.materials().cap().at(chunk, start, randomState);
            BlockState body = profile.materials().body().at(chunk, start, randomState);
            for (int i = 0; i <= hangingLength; i++) {
                pos.set(start.getX() - chunkMinX, start.getY() - i, start.getZ() - chunkMinZ);
                chunk.setBlockState(pos, i == 0 ? cap : body);
            }
            if (plan.joinsNearbyTerrain()) {
                pos.set(
                        start.getX() - chunkMinX,
                        start.getY() - candidate.clearLength(),
                        start.getZ() - chunkMinZ
                );
                chunk.setBlockState(pos, body);
            }
            if (candidate.bridge()) {
                for (int i = hangingLength + 1; i < candidate.clearLength(); i++) {
                    pos.set(start.getX() - chunkMinX, start.getY() - i, start.getZ() - chunkMinZ);
                    chunk.setBlockState(pos, profile.materials().bridge());
                }
            }
        }
    }

    private static void addCandidate(
            ChunkAccess chunk,
            Profile profile,
            BlockPos start,
            Map<Column, Candidate> candidates
    ) {
        int clearLength = 0;
        while (clearLength < MAX_CLEAR_LENGTH && chunk.getBlockState(start.below(clearLength)).isAir()) {
            clearLength++;
        }
        if (clearLength == 0) {
            return;
        }

        Column column = new Column(start.getX(), start.getZ());
        BlockState below = chunk.getBlockState(start.below(clearLength));
        Candidate candidate = new Candidate(
                start,
                clearLength,
                profile.supports().join().test(below),
                clearLength <= MAX_BRIDGE_CLEAR_LENGTH && profile.supports().bridge().test(below)
        );
        candidates.merge(column, candidate, (existing, added) -> {
            if (added.start().getY() > existing.start().getY()) {
                return added;
            }
            if (added.start().getY() == existing.start().getY()
                    && added.clearLength() > existing.clearLength()) {
                return added;
            }
            return existing;
        });
    }

    private static boolean isInsideChunk(int x, int z, int chunkMinX, int chunkMinZ) {
        return x >= chunkMinX && x < chunkMinX + 16 && z >= chunkMinZ && z < chunkMinZ + 16;
    }

    private static Candidate smoothSlopeCandidate(Candidate candidate, Map<Column, Candidate> coreCandidates) {
        Column column = new Column(candidate.start().getX(), candidate.start().getZ());
        int cardinalNeighbors = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Column neighbor = new Column(
                    column.x() + direction.getStepX(),
                    column.z() + direction.getStepZ()
            );
            if (coreCandidates.containsKey(neighbor) && ++cardinalNeighbors == 2) {
                return candidate;
            }
        }
        if (cardinalNeighbors != 1 || candidate.clearLength() != 2 || !candidate.joinsTerrain()) {
            return null;
        }

        int diagonalNeighbors = 0;
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (coreCandidates.containsKey(new Column(column.x() + dx, column.z() + dz))) {
                    diagonalNeighbors++;
                }
            }
        }
        return diagonalNeighbors >= 2
                ? new Candidate(candidate.start().below(), 1, true, false)
                : null;
    }

    private static Plan planHanging(Candidate candidate, int desiredLength) {
        int clearBelow = candidate.clearLength() - 1;
        boolean joinsNearbyTerrain = candidate.joinsTerrain()
                && candidate.clearLength() <= MAX_JOIN_CLEAR_LENGTH;
        int maxLength = joinsNearbyTerrain ? clearBelow : Math.min(MAX_HANGING_LENGTH, clearBelow);
        int minLength = joinsNearbyTerrain ? maxLength : 0;
        if (candidate.bridge()) {
            int maxBridgeLength = MAX_BRIDGE_LENGTH;
            if (candidate.clearLength() == MAX_BRIDGE_CLEAR_LENGTH) {
                maxBridgeLength = desiredLength >= 4 ? 0 : 1;
            }
            minLength = Math.max(minLength, candidate.clearLength() - maxBridgeLength - 1);
        }
        return new Plan(Math.max(minLength, Math.min(maxLength, desiredLength)), joinsNearbyTerrain);
    }

    // A hanging profile follows a whole exposed wall and is oriented by its corner geometry.
    // Narrow walls use the matching outer part of the template; long walls continue with a
    // deterministic 1-3 block fallback whose equal-height runs never exceed two columns.
    private static int shapedHangingLength(
            Candidate candidate,
            RandomState randomState,
            PositionalRandomFactory random,
            Profile profile
    ) {
        BlockPos start = candidate.start();
        CliffFace face = cliffFaceAt(start.getX(), start.getZ(), randomState, profile);
        if (face == null) {
            return randomFallbackLength(start, random);
        }

        int axisX = face.supportDirection().getAxis() == Direction.Axis.Z ? 1 : 0;
        int axisZ = axisX == 0 ? 1 : 0;
        WallRun before = traceLowerWall(start, face, -axisX, -axisZ, randomState, profile);
        WallRun after = traceLowerWall(start, face, axisX, axisZ, randomState, profile);
        int runWidth = before.length() + 1 + after.length();
        if (runWidth == 1) {
            return randomFallbackLength(start, random);
        }
        int wallY = face.startY() - 2;
        boolean negativeEndIsInner = isInnerCorner(before.endX(), before.endZ(), wallY, randomState, profile.columns());
        boolean positiveEndIsInner = isInnerCorner(after.endX(), after.endZ(), wallY, randomState, profile.columns());

        boolean reverse = runWidth >= WIDE_HANGING_TEMPLATE.length
                ? negativeEndIsInner && !positiveEndIsInner
                : positiveEndIsInner && !negativeEndIsInner;
        if (negativeEndIsInner == positiveEndIsInner) {
            reverse = random.at(before.endX(), 3, before.endZ()).nextBoolean();
        }
        int index = reverse ? after.length() : before.length();
        int edgeX = reverse ? after.endX() : before.endX();
        int edgeZ = reverse ? after.endZ() : before.endZ();
        int heightOffset = face.startY() - Math.min(before.minStartY(), after.minStartY());
        int baseLength;
        if (runWidth >= WIDE_HANGING_TEMPLATE.length) {
            if (index >= WIDE_HANGING_TEMPLATE.length) {
                baseLength = naturalFallbackLength(edgeX, edgeZ, index - WIDE_HANGING_TEMPLATE.length, random);
            } else {
                baseLength = WIDE_HANGING_TEMPLATE[index];
            }
        } else if (index == Math.min(runWidth, WIDE_HANGING_TEMPLATE.length) - 1) {
            baseLength = 1;
        } else {
            baseLength = runWidth == 2
                    ? 2
                    : WIDE_HANGING_TEMPLATE.length - Math.min(runWidth, WIDE_HANGING_TEMPLATE.length) + index;
        }
        return baseLength + heightOffset;
    }

    private static int naturalFallbackLength(int edgeX, int edgeZ, int index, PositionalRandomFactory random) {
        int variant = random.at(edgeX, 5, edgeZ).nextInt(4);
        int patternIndex = index % NATURAL_FALLBACK_PATTERN.length;
        if ((variant & 1) != 0) {
            patternIndex = NATURAL_FALLBACK_PATTERN.length - 1 - patternIndex;
        }
        int length = NATURAL_FALLBACK_PATTERN[patternIndex];
        return (variant & 2) == 0 ? length : 4 - length;
    }

    private static WallRun traceLowerWall(
            BlockPos start,
            CliffFace face,
            int stepX,
            int stepZ,
            RandomState randomState,
            Profile profile
    ) {
        int currentX = start.getX();
        int currentZ = start.getZ();
        int minStartY = face.startY();
        int length = 0;
        for (int distance = 1; distance <= MAX_CLIFF_RUN_SCAN; distance++) {
            int nextX = start.getX() + stepX * distance;
            int nextZ = start.getZ() + stepZ * distance;
            CliffFace nextFace = cliffFaceAt(nextX, nextZ, randomState, profile);
            if (nextFace == null || nextFace.supportDirection() != face.supportDirection()) {
                break;
            }
            if (!isLowerWallExposed(nextX, nextZ, nextFace, randomState, profile.columns())) {
                break;
            }
            currentX = nextX;
            currentZ = nextZ;
            minStartY = Math.min(minStartY, nextFace.startY());
            length++;
        }
        return new WallRun(length, currentX, currentZ, minStartY);
    }

    private static boolean isLowerWallExposed(
            int x,
            int z,
            CliffFace face,
            RandomState randomState,
            ColumnProvider columns
    ) {
        int wallY = face.startY() - 2;
        TerrainColumn target = columns.at(x, z, randomState);
        return !target.present() || wallY < (int) target.baseline() || wallY > target.topY();
    }

    private static int randomFallbackLength(BlockPos start, PositionalRandomFactory random) {
        return 2 + random.at(start).nextInt(2);
    }

    private static boolean isInnerCorner(
            int x,
            int z,
            int wallY,
            RandomState randomState,
            ColumnProvider columns
    ) {
        boolean north = supportsWallAt(x, z - 1, wallY, randomState, columns);
        boolean east = supportsWallAt(x + 1, z, wallY, randomState, columns);
        boolean south = supportsWallAt(x, z + 1, wallY, randomState, columns);
        boolean west = supportsWallAt(x - 1, z, wallY, randomState, columns);
        return north && east || east && south || south && west || west && north;
    }

    private static boolean supportsWallAt(
            int x,
            int z,
            int wallY,
            RandomState randomState,
            ColumnProvider columns
    ) {
        TerrainColumn column = columns.at(x, z, randomState);
        return column.present() && (int) column.baseline() <= wallY && column.topY() >= wallY;
    }

    private static CliffFace cliffFaceAt(
            int x,
            int z,
            RandomState randomState,
            Profile profile
    ) {
        TerrainColumn target = profile.columns().at(x, z, randomState);
        CliffFace face = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            TerrainColumn support = profile.columns().at(x + direction.getStepX(), z + direction.getStepZ(), randomState);
            if (!support.present()) {
                continue;
            }
            boolean highUnderside = support.topY() > profile.highUndersideY() + 1;
            boolean exposedBottom = !target.present() || (int) support.baseline() < (int) target.baseline();
            boolean exposedSlope = !target.present() || support.topY() - target.topY() > 1;
            if (!highUnderside && !exposedBottom && !exposedSlope) {
                continue;
            }
            CliffFace candidate = new CliffFace(direction, support.topY() - 1);
            if (face == null || candidate.startY() > face.startY()) {
                face = candidate;
            }
        }
        return face;
    }

    @FunctionalInterface
    public interface ColumnProvider {
        TerrainColumn at(int x, int z, RandomState randomState);
    }

    @FunctionalInterface
    public interface MaterialProvider {
        BlockState at(ChunkAccess chunk, BlockPos start, RandomState randomState);
    }

    public record TerrainColumn(boolean present, float baseline, int topY) {}

    public record Supports(
            Predicate<BlockState> core,
            Predicate<BlockState> surface,
            Predicate<BlockState> join,
            Predicate<BlockState> bridge
    ) {}

    public record Materials(MaterialProvider cap, MaterialProvider body, BlockState bridge) {
        public static Materials fixed(BlockState cap, BlockState body, BlockState bridge) {
            return new Materials((chunk, start, randomState) -> cap, (chunk, start, randomState) -> body, bridge);
        }
    }

    public record Profile(
            ColumnProvider columns,
            int highUndersideY,
            Supports supports,
            Materials materials,
            Identifier shapeRandom
    ) {}

    private record Column(int x, int z) {}

    private record Candidate(BlockPos start, int clearLength, boolean joinsTerrain, boolean bridge) {}

    private record Plan(int length, boolean joinsNearbyTerrain) {}

    private record CliffFace(Direction supportDirection, int startY) {}

    private record WallRun(int length, int endX, int endZ, int minStartY) {}
}
