package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;

public class TopLayerAndBloomingCavesGen {

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

                float noise = topLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (noise < 0.01) {
                    continue;
                }

                float biomeHeight = noise * (TOP_LAYER_TERRAIN_HEIGHT) - ARCANE_PLATE_HEIGHT;

                float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                float baseLine = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;

                int topY = (int) (baseLine + biomeHeight);
                Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(xx), QuartPos.fromBlock(topY), QuartPos.fromBlock(zz));
                BlockState subfloor = RRTerrainSurfaces.usesGrassySubfloor(biome) ? dirt : stone;

                for (int y = (int) baseLine; y < baseLine + biomeHeight - 2; y++) {
                    chunk.setBlockState(pos.set(x, y, z), stone);
                }
                for (int y = (int) (baseLine + biomeHeight) - 2; y < baseLine + biomeHeight; y++) {
                    chunk.setBlockState(pos.set(x, y, z), subfloor);
                }
                chunk.setBlockState(pos.set(x, topY, z), RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState));
            }
        }
    }

    /** Visible underside of the plate: top-layer biome when an island is present, otherwise the ceiling biome at Y. */
    private static BlockState underside(ChunkAccess chunk, int xx, int zz, int ceilingY, RandomState randomState) {
        float topNoise = topLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);
        if (topNoise < 0.01f) {
            return RRTerrainSurfaces.ceilingAt(chunk, xx, ceilingY, zz, randomState);
        }

        float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
        float topBaseLine = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;
        int topY = (int) (topBaseLine + topNoise * TOP_LAYER_TERRAIN_HEIGHT - ARCANE_PLATE_HEIGHT);
        Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(xx), QuartPos.fromBlock(topY), QuartPos.fromBlock(zz));
        return RRTerrainSurfaces.ceilingFor(biome, xx, ceilingY, zz, randomState);
    }
}
