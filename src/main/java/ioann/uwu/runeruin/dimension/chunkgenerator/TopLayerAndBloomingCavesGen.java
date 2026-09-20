package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;

public class TopLayerAndBloomingCavesGen {

    private static final LazyNoise floorNoise = new LazyNoise("bloomingCavesFloorNoise", SingleNoise::new);

    private static final HangingTerrainGenerator.Profile HANGING_SOIL = new HangingTerrainGenerator.Profile(
            TopLayerAndBloomingCavesGen::topLayerColumnAt,
            BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_OFFSET,
            new HangingTerrainGenerator.Supports(
                    state -> state.is(Blocks.DIRT),
                    state -> state.is(Blocks.GRASS_BLOCK),
                    state -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT),
                    state -> state.is(BlockTags.BASE_STONE_OVERWORLD)
            ),
            HangingTerrainGenerator.Materials.fixed(
                    Blocks.GRASS_BLOCK.defaultBlockState(),
                    Blocks.DIRT.defaultBlockState(),
                    Blocks.STONE.defaultBlockState()
            ),
            RR.id("hanging_soil_shape")
    );

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
                ceilingNoise *= flattenedBaseTopLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);
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
                HangingTerrainGenerator.TerrainColumn column = topLayerColumnAt(xx, zz, randomState);
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
        HangingTerrainGenerator.generate(chunk, randomState, HANGING_SOIL);
    }

    private static HangingTerrainGenerator.TerrainColumn topLayerColumnAt(int x, int z, RandomState randomState) {
        float noise = topLevelNoise.getOrCreateNoise(randomState).noise(x, z);
        if (noise < 0.01f) {
            return new HangingTerrainGenerator.TerrainColumn(false, 0, 0);
        }
        float biomeHeight = noise * TOP_LAYER_TERRAIN_HEIGHT - ARCANE_PLATE_HEIGHT;
        float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(x, z);
        float baseline = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;
        int topY = (int) (baseline + biomeHeight);
        if (topY - (int) baseline <= 2) {
            return new HangingTerrainGenerator.TerrainColumn(false, 0, 0);
        }
        return new HangingTerrainGenerator.TerrainColumn(true, baseline, topY);
    }

    /** Visible underside of the plate: top-layer biome when an island is present, otherwise the ceiling biome at Y. */
    private static BlockState underside(ChunkAccess chunk, int xx, int zz, int ceilingY, RandomState randomState) {
        HangingTerrainGenerator.TerrainColumn topLayer = topLayerColumnAt(xx, zz, randomState);
        if (!topLayer.present()) {
            return RRTerrainSurfaces.ceilingAt(chunk, xx, ceilingY, zz, randomState);
        }
        Holder<Biome> biome = chunk.getNoiseBiome(
                QuartPos.fromBlock(xx), QuartPos.fromBlock(topLayer.topY()), QuartPos.fromBlock(zz)
        );
        return RRTerrainSurfaces.ceilingFor(biome, xx, ceilingY, zz, randomState);
    }
}
