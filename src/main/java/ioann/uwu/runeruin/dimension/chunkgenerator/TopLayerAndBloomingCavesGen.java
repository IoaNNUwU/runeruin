package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ioann.uwu.runeruin.dimension.Const.*;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;

public class TopLayerAndBloomingCavesGen {

    private static final int MAX_HANGING_LENGTH = 6;
    private static final int MAX_NEIGHBOR_BOTTOM_DIFFERENCE = 2;
    private static final int HANGING_PEAK_RADIUS = 3;
    private static final int HANGING_PEAK_CHANCE = 16;
    private static final int MAX_SOIL_JOIN_CLEAR_LENGTH = 3;
    private static final int MAX_STONE_BRIDGE_LENGTH = 2;
    private static final int MAX_STONE_BRIDGE_CLEAR_LENGTH = 6;
    private static final int MAX_CLEAR_LENGTH = 7;

    private static final LazyNoise floorNoise = new LazyNoise("bloomingCavesFloorNoise", SingleNoise::new);

    public static int bloomingCavesFloorY(int x, int z, RandomState randomState) {
        float noise = floorNoise.getOrCreateNoise(randomState).noise(x, z);
        int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));
        return BLOOMING_CAVES_Y + biomeHeight;
    }

    public static void generateBloomingCavesFloor(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getMiddleBlockX() + x;
                int zz = chunk.getPos().getMiddleBlockZ() + z;

                int topY = bloomingCavesFloorY(xx, zz, randomState);

                for (int y = BLOOMING_CAVES_Y; y < topY; y++) {
                    chunk.setBlockState(pos.set(x, y, z), stone);
                }
                chunk.setBlockState(pos.set(x, topY, z), RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState));
            }
        }
    }

    private static final LazyNoise bloomingCavesCeilingNoise = new LazyNoise(
            "bloomingCavesCeilingNoise",
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("bloomingCavesCeilingNoise1" + seed)),
                    Noise.constant(1f)
            )
    );

    private static final LazyNoise bedrockNoise = new LazyNoise("bedrockNoise", PositionalRandomNoise::new);

    private static final LazyNoise topLevelNoise = RRChunkGenerator.topLevelNoise;
    private static final LazyNoise topLevelBaselineNoise = RRChunkGenerator.topLevelBaselineNoise;
    private static final LazyNoise flattenedBaseTopLevelNoise = RRChunkGenerator.flattenedBaseTopLevelNoise;

    public static void generateBloomingCavesCeiling(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                float ceilingNoise = bloomingCavesCeilingNoise.getOrCreateNoise(randomState).noise(xx, zz);
                ceilingNoise = ceilingNoise * flattenedBaseTopLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (ceilingNoise < 0.01) {
                    continue;
                }

                float ceilingHeight = (int) (CEILING_TERRAIN_HEIGHT * ceilingNoise);

                float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                float baseLine = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;
                int ceilingSurfaceY = (int) (baseLine - ceilingHeight);

                BlockState blockState = bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f
                        ? deepslate
                        : stone;
                chunk.setBlockState(pos.set(x, (int) baseLine, z), blockState);

                for (int y = (int) (baseLine - ceilingHeight + 1); y < baseLine - 1; y++) {
                    chunk.setBlockState(pos.set(x, y, z), deepslate);
                }
                chunk.setBlockState(pos.set(x, ceilingSurfaceY, z), underside(chunk, xx, zz, ceilingSurfaceY, randomState));
            }
        }
    }

    public static void generateTopLayerFloor(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                TerrainColumn column = topLayerColumnAt(xx, zz, randomState);
                if (!column.present()) {
                    continue;
                }

                float baseLine = column.baseline();
                int topY = column.topY();
                Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(xx), QuartPos.fromBlock(topY), QuartPos.fromBlock(zz));
                BlockState subfloor = RRTerrainSurfaces.usesGrassySubfloor(biome) ? dirt : stone;

                int bottomY = (int) baseLine;
                int subfloorStartY = Math.max(bottomY, topY - 2);
                for (int y = bottomY; y < subfloorStartY; y++) {
                    chunk.setBlockState(pos.set(x, y, z), stone);
                }
                for (int y = subfloorStartY; y < topY; y++) {
                    chunk.setBlockState(pos.set(x, y, z), subfloor);
                }
                chunk.setBlockState(pos.set(x, topY, z), RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState));
            }
        }
    }

    public static void generateHangingSoil(ChunkAccess chunk, RandomState randomState) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int highestBloomingCeilingY = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_OFFSET;
        Map<HangingSoilColumn, HangingSoilCandidate> coreCandidates = new LinkedHashMap<>();
        Map<HangingSoilColumn, HangingSoilCandidate> slopeCandidates = new LinkedHashMap<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkMinX + x;
                int worldZ = chunkMinZ + z;
                TerrainColumn target = topLayerColumnAt(worldX, worldZ, randomState);

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    int supportX = worldX + direction.getStepX();
                    int supportZ = worldZ + direction.getStepZ();
                    TerrainColumn support = topLayerColumnAt(supportX, supportZ, randomState);
                    if (!support.present()) {
                        continue;
                    }

                    if (support.topY() > highestBloomingCeilingY + 1) {
                        int startY = support.topY() - 1;
                        boolean dirtSupport = !isInsideChunk(supportX, supportZ, chunkMinX, chunkMinZ)
                                || chunk.getBlockState(new BlockPos(supportX, startY, supportZ)).is(Blocks.DIRT);
                        if (dirtSupport && (!target.present() || startY > target.topY())) {
                            addHangingSoilCandidate(chunk, new BlockPos(worldX, startY, worldZ), coreCandidates);
                        }
                    }

                    int startY = support.topY() - 1;
                    int supportBottomY = (int) support.baseline();
                    boolean exposedBottom = !target.present() || supportBottomY < (int) target.baseline();
                    boolean exposedSlope = !target.present() || support.topY() - target.topY() > 1;
                    if (!exposedBottom && !exposedSlope) {
                        continue;
                    }
                    boolean grassSupport = !isInsideChunk(supportX, supportZ, chunkMinX, chunkMinZ)
                            || chunk.getBlockState(new BlockPos(supportX, support.topY(), supportZ)).is(Blocks.GRASS_BLOCK);
                    if (!grassSupport) {
                        continue;
                    }
                    BlockPos start = new BlockPos(worldX, startY, worldZ);
                    if (exposedBottom && (!target.present() || startY > target.topY())) {
                        addHangingSoilCandidate(chunk, start, coreCandidates);
                    }
                    if (exposedSlope) {
                        addHangingSoilCandidate(chunk, start, slopeCandidates);
                    }
                }
            }
        }

        Map<HangingSoilColumn, HangingSoilCandidate> candidates = new LinkedHashMap<>(coreCandidates);
        for (Map.Entry<HangingSoilColumn, HangingSoilCandidate> entry : slopeCandidates.entrySet()) {
            if (!candidates.containsKey(entry.getKey())) {
                HangingSoilCandidate smoothed = smoothSlopeCandidate(entry.getValue(), coreCandidates);
                if (smoothed != null) {
                    candidates.put(entry.getKey(), smoothed);
                }
            }
        }

        PositionalRandomFactory orderRandom = randomState.getOrCreateRandomFactory(RR.id("hanging_soil_order"));
        PositionalRandomFactory lengthRandom = randomState.getOrCreateRandomFactory(RR.id("hanging_soil_length"));
        Map<HangingSoilCandidate, Long> orderKeys = new LinkedHashMap<>();
        for (HangingSoilCandidate candidate : candidates.values()) {
            orderKeys.put(candidate, orderRandom.at(candidate.start()).nextLong());
        }
        List<HangingSoilCandidate> starts = new ArrayList<>(candidates.values());
        starts.sort(Comparator.<HangingSoilCandidate>comparingInt(candidate -> candidate.start().getY()).reversed()
                .thenComparingLong(orderKeys::get)
                .thenComparingInt(candidate -> candidate.start().getX())
                .thenComparingInt(candidate -> candidate.start().getZ()));

        List<HangingSoilStart> placed = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (HangingSoilCandidate candidate : starts) {
            BlockPos start = candidate.start();
            int clearBelow = candidate.clearLength() - 1;
            boolean joinsNearbySoil = candidate.joinsSoil()
                    && candidate.clearLength() <= MAX_SOIL_JOIN_CLEAR_LENGTH;
            int maxHangingLength = joinsNearbySoil
                    ? clearBelow
                    : Math.min(MAX_HANGING_LENGTH, clearBelow);
            int minHangingLength = joinsNearbySoil
                    ? maxHangingLength
                    : Math.min(2, maxHangingLength);
            if (candidate.bridgeToStone()) {
                int maxStoneBridgeLength = MAX_STONE_BRIDGE_LENGTH;
                if (candidate.clearLength() == MAX_STONE_BRIDGE_CLEAR_LENGTH) {
                    maxStoneBridgeLength = desiredHangingLength(start, lengthRandom) >= 4 ? 0 : 1;
                }
                minHangingLength = Math.max(
                        minHangingLength,
                        candidate.clearLength() - maxStoneBridgeLength - 1
                );
            }
            int hangingLength = chooseHangingLength(
                    start, minHangingLength, maxHangingLength, placed, lengthRandom
            );

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

            placed.add(new HangingSoilStart(start, hangingLength));
            for (int i = 0; i <= hangingLength; i++) {
                pos.set(start.getX() - chunkMinX, start.getY() - i, start.getZ() - chunkMinZ);
                chunk.setBlockState(pos, i == 0 ? grass : dirt);
            }
            if (joinsNearbySoil) {
                pos.set(
                        start.getX() - chunkMinX,
                        start.getY() - candidate.clearLength(),
                        start.getZ() - chunkMinZ
                );
                chunk.setBlockState(pos, dirt);
            }
            if (candidate.bridgeToStone()) {
                for (int i = hangingLength + 1; i < candidate.clearLength(); i++) {
                    pos.set(start.getX() - chunkMinX, start.getY() - i, start.getZ() - chunkMinZ);
                    chunk.setBlockState(pos, stone);
                }
            }
        }
    }

    private static void addHangingSoilCandidate(
            ChunkAccess chunk,
            BlockPos start,
            Map<HangingSoilColumn, HangingSoilCandidate> candidates
    ) {
        int clearLength = 0;
        while (clearLength < MAX_CLEAR_LENGTH && chunk.getBlockState(start.below(clearLength)).isAir()) {
            clearLength++;
        }
        if (clearLength > 0) {
            HangingSoilColumn column = new HangingSoilColumn(start.getX(), start.getZ());
            BlockState below = chunk.getBlockState(start.below(clearLength));
            boolean joinsSoil = below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT);
            boolean bridgeToStone = clearLength <= MAX_STONE_BRIDGE_CLEAR_LENGTH
                    && below.is(BlockTags.BASE_STONE_OVERWORLD);
            HangingSoilCandidate candidate = new HangingSoilCandidate(start, clearLength, joinsSoil, bridgeToStone);
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
    }

    private static TerrainColumn topLayerColumnAt(int x, int z, RandomState randomState) {
        float noise = topLevelNoise.getOrCreateNoise(randomState).noise(x, z);
        if (noise < 0.01f) {
            return new TerrainColumn(false, 0, 0);
        }

        float biomeHeight = noise * TOP_LAYER_TERRAIN_HEIGHT - ARCANE_PLATE_HEIGHT;
        float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(x, z);
        float baseline = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;
        int topY = (int) (baseline + biomeHeight);
        if (topY - (int) baseline <= 2) {
            return new TerrainColumn(false, 0, 0);
        }
        return new TerrainColumn(true, baseline, topY);
    }

    private static boolean isInsideChunk(int x, int z, int chunkMinX, int chunkMinZ) {
        return x >= chunkMinX && x < chunkMinX + 16 && z >= chunkMinZ && z < chunkMinZ + 16;
    }

    private static HangingSoilCandidate smoothSlopeCandidate(
            HangingSoilCandidate candidate,
            Map<HangingSoilColumn, HangingSoilCandidate> coreCandidates
    ) {
        HangingSoilColumn column = new HangingSoilColumn(candidate.start().getX(), candidate.start().getZ());
        int cardinalNeighbors = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            HangingSoilColumn neighbor = new HangingSoilColumn(
                    column.x() + direction.getStepX(),
                    column.z() + direction.getStepZ()
            );
            if (coreCandidates.containsKey(neighbor) && ++cardinalNeighbors == 2) {
                return candidate;
            }
        }
        if (cardinalNeighbors != 1 || candidate.clearLength() != 2 || !candidate.joinsSoil()) {
            return null;
        }

        int diagonalNeighbors = 0;
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (coreCandidates.containsKey(new HangingSoilColumn(column.x() + dx, column.z() + dz))) {
                    diagonalNeighbors++;
                }
            }
        }
        return diagonalNeighbors >= 2
                ? new HangingSoilCandidate(candidate.start().below(), 1, true, false)
                : null;
    }

    private static int chooseHangingLength(
            BlockPos start,
            int minLength,
            int maxLength,
            List<HangingSoilStart> placed,
            PositionalRandomFactory random
    ) {
        int desiredLength = desiredHangingLength(start, random);
        List<Integer> options = new ArrayList<>();
        int fewestExcess = Integer.MAX_VALUE;
        int closestToDesired = Integer.MAX_VALUE;
        int fewestMatches = Integer.MAX_VALUE;
        for (int length = minLength; length <= maxLength; length++) {
            int bottomY = start.getY() - length;
            int excess = 0;
            int matches = 0;
            for (HangingSoilStart existing : placed) {
                if (areNearbyHangingStarts(start, existing.pos())) {
                    int difference = Math.abs(bottomY - existing.bottomY());
                    excess += Math.max(0, difference - MAX_NEIGHBOR_BOTTOM_DIFFERENCE);
                    matches += difference == 0 ? 1 : 0;
                }
            }
            int distanceToDesired = Math.abs(length - desiredLength);
            if (excess < fewestExcess
                    || excess == fewestExcess && distanceToDesired < closestToDesired
                    || excess == fewestExcess && distanceToDesired == closestToDesired && matches < fewestMatches) {
                options.clear();
                fewestExcess = excess;
                closestToDesired = distanceToDesired;
                fewestMatches = matches;
            }
            if (excess == fewestExcess && distanceToDesired == closestToDesired && matches == fewestMatches) {
                options.add(length);
            }
        }
        return options.get(random.at(start).nextInt(options.size()));
    }

    private static int desiredHangingLength(BlockPos start, PositionalRandomFactory random) {
        // Max-cones make rare long peaks taper consistently, including across chunk borders.
        int desired = 2 + random.at(start.getX(), 0, start.getZ()).nextInt(2);
        for (int dx = -HANGING_PEAK_RADIUS; dx <= HANGING_PEAK_RADIUS; dx++) {
            int maxDz = HANGING_PEAK_RADIUS - Math.abs(dx);
            for (int dz = -maxDz; dz <= maxDz; dz++) {
                RandomSource peakRandom = random.at(start.getX() + dx, 1, start.getZ() + dz);
                if (peakRandom.nextInt(HANGING_PEAK_CHANCE) == 0) {
                    int peakLength = 5 + peakRandom.nextInt(2);
                    desired = Math.max(desired, peakLength - Math.abs(dx) - Math.abs(dz));
                }
            }
        }
        return desired;
    }

    private static boolean areNearbyHangingStarts(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getZ() - second.getZ()) == 1;
    }

    private record TerrainColumn(boolean present, float baseline, int topY) {}

    private record HangingSoilColumn(int x, int z) {}

    private record HangingSoilCandidate(
            BlockPos start,
            int clearLength,
            boolean joinsSoil,
            boolean bridgeToStone
    ) {}

    private record HangingSoilStart(BlockPos pos, int hangingLength) {
        private int bottomY() {
            return pos.getY() - hangingLength;
        }
    }

    /** Visible underside of the plate: top-layer biome when an island is present, otherwise the ceiling biome at Y. */
    private static BlockState underside(ChunkAccess chunk, int xx, int zz, int ceilingY, RandomState randomState) {
        TerrainColumn topLayer = topLayerColumnAt(xx, zz, randomState);
        if (!topLayer.present()) {
            return RRTerrainSurfaces.ceilingAt(chunk, xx, ceilingY, zz, randomState);
        }

        Holder<Biome> biome = chunk.getNoiseBiome(
                QuartPos.fromBlock(xx), QuartPos.fromBlock(topLayer.topY()), QuartPos.fromBlock(zz)
        );
        return RRTerrainSurfaces.ceilingFor(biome, xx, ceilingY, zz, randomState);
    }
}
