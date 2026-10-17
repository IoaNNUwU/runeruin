package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.ARCANE_PLATE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.CEILING_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_MIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_OFFSET;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_Y;

public class TopLayerAndBloomingCavesGen {

    private static final LazyNoise floorNoise = LazyNoise.single("bloomingCavesFloorNoise", 1f);

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
        return bloomingCavesFloorY(x, z, randomState.sampler());
    }

    public static int bloomingCavesFloorY(int x, int z, Climate.Sampler sampler) {
        float noise = floorNoise.getOrCreateNoise(sampler).noise(x, z);
        int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));
        return BLOOMING_CAVES_Y + biomeHeight;
    }

    public static void generateBloomingCavesFloor(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xx = chunk.getPos().getMinBlockX() + x;
                int zz = chunk.getPos().getMinBlockZ() + z;
                // The floor reads its noise 8 blocks aside; its biome is the one of its own column.
                int topY = bloomingCavesFloorY(xx + 8, zz + 8, randomState);
                for (int y = BLOOMING_CAVES_Y; y < topY; y++) {
                    terrain.set(x, y, z, stone);
                }
                RRTerrainSurfaces.placeFloorSurface(
                        terrain, x, topY, z, RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState)
                );
            }
        }
    }

    private static final LazyNoise bloomingCavesCeilingNoise = new LazyNoise(
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("bloomingCavesCeilingNoise1" + seed)),
                    Noise.constant(1f)
            )
    );
    private static final LazyNoise bedrockNoise = new LazyNoise(seed -> new PositionalRandomNoise("bloomingCavesCeilingRock", seed));
    private static final LazyNoise topLevelNoise = RRChunkGenerator.topLevelNoise;
    private static final LazyNoise topLevelBaselineNoise = RRChunkGenerator.topLevelBaselineNoise;
    private static final LazyNoise flattenedBaseTopLevelNoise = RRChunkGenerator.flattenedBaseTopLevelNoise;

    private static final LazyNoise ceilingReliefNoise = LazyNoise.single("bloomingCavesCeilingRelief", 9f);

    /** No terrace of the Blooming caves ceiling hangs lower than this. */
    public static final int CEILING_LOWEST_Y = BLOOMING_CAVES_CEILING_Y - 23;
    // One terrace hangs this far under the even ceiling, and one rises as far above it.
    private static final int CEILING_STEP = 5;
    // The highest terrace opens only the lowest block of an island: in the terraces an island is 10 blocks thick or more.
    private static final int CEILING_RISE_INTO_ISLAND = 1;
    /** How far the ceiling biomes rise into an island with the terraces, so that their plants hang from all of them. */
    public static final int CEILING_BIOME_RISE = 4;
    // The terraces begin where the noise of the islands passes the first value and are whole from the second:
    // closer to the edge of an island the ceiling stays even.
    private static final float TERRACES_BEGIN = 0.70f;
    private static final float TERRACES_WHOLE = 0.80f;
    private static final int NO_CEILING = Integer.MIN_VALUE;
    private static final int CEILING_MIN_Y = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_OFFSET - CEILING_TERRAIN_HEIGHT;
    private static final int CEILING_MAX_Y = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_OFFSET + TOP_LAYER_MAX_BASELINE_HEIGHT + CEILING_RISE_INTO_ISLAND + 2;

    private static final TerracedCeiling CEILING = new TerracedCeiling("bloomingCavesCeiling", CEILING_MIN_Y, CEILING_MAX_Y, 2, CeilingColumns::new);

    /** Whether the terraces of the ceiling reach this column of an island: its edges keep the even ceiling. */
    public static boolean ceilingHasTerraces(int x, int z, Climate.Sampler sampler) {
        return flattenedBaseTopLevelNoise.getOrCreateNoise(sampler).noise(x, z) > TERRACES_BEGIN;
    }

    /** The height of the underside of the islands. The block at this height is the highest of the ceiling. */
    private static float baseLine(int x, int z, RandomState randomState) {
        return BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * topLevelBaselineNoise.getOrCreateNoise(randomState).noise(x, z) + TOP_LAYER_OFFSET;
    }

    private static int evenCeilingY(int x, int z, float island, float baseLine, RandomState randomState) {
        float thickness = bloomingCavesCeilingNoise.getOrCreateNoise(randomState).noise(x, z) * island;
        return thickness < 0.01 ? NO_CEILING : (int) (baseLine - (int) (CEILING_TERRAIN_HEIGHT * thickness));
    }

    /** The lowest block of the even ceiling that the edges of the islands keep, if there is a ceiling here. */
    public static OptionalInt evenCeilingY(int x, int z, RandomState randomState) {
        int y = evenCeilingY(x, z, flattenedBaseTopLevelNoise.getOrCreateNoise(randomState).noise(x, z), baseLine(x, z, randomState), randomState);
        return y == NO_CEILING ? OptionalInt.empty() : OptionalInt.of(y);
    }

    public static void generateBloomingCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        ChunkPos pos = chunk.getPos();
        TerracedCeiling.Chunk ceiling = CEILING.chunk(pos.x(), pos.z(), randomState);
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int lowest = ceiling.lowestY(x, z);
                if (lowest > CEILING_MAX_Y) {
                    continue;
                }
                int xx = pos.getMinBlockX() + x;
                int zz = pos.getMinBlockZ() + z;
                int underside = (int) baseLine(xx, zz, randomState);
                for (int y = Math.min(lowest, underside + 1); y <= CEILING_MAX_Y; y++) {
                    boolean solid = ceiling.stone(x, y, z);
                    // Under the islands the ceiling is built, inside them it is dug out.
                    if (y > underside) {
                        if (!solid) {
                            terrain.set(x, y, z, air);
                        }
                    } else if (solid) {
                        terrain.set(x, y, z, y < underside || bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f ? deepslate : stone);
                    }
                }
                for (int y = lowest; y <= CEILING_MAX_Y; y++) {
                    if (!ceiling.stone(x, y, z) || y > lowest && ceiling.stone(x, y - 1, z)) {
                        continue;
                    }
                    // One block of moss: under the top layer it is not two blocks thick as on the other cave surfaces.
                    terrain.set(x, y, z, underside(chunk, xx, zz, y, randomState));
                }
            }
        }
    }

    /**
     * What the ceiling of the columns around a chunk hangs between. Near the edge of an island and around an
     * arcane column it is the even ceiling; further in it turns into terraces: one under the even ceiling, one at
     * its height and one above it.
     */
    private static final class CeilingColumns implements TerracedCeiling.Columns {
        private final int chunkX;
        private final int chunkZ;
        private final RandomState randomState;
        // 0 is unknown, 1 is none, 2 is an arcane column: this chunk and its 8 neighbours.
        private final byte[] arcaneColumns = new byte[9];

        CeilingColumns(int chunkX, int chunkZ, RandomState randomState) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.randomState = randomState;
        }

        @Override
        public TerracedCeiling.Column at(int x, int z) {
            float island = flattenedBaseTopLevelNoise.getOrCreateNoise(this.randomState).noise(x, z);
            float baseLine = baseLine(x, z, this.randomState);
            int even = evenCeilingY(x, z, island, baseLine, this.randomState);
            if (even == NO_CEILING) {
                return null;
            }
            int underside = (int) baseLine;
            float strength = Mth.clamp((island - TERRACES_BEGIN) / (TERRACES_WHOLE - TERRACES_BEGIN), 0f, 1f);
            strength *= strength * (3 - 2 * strength);
            if (strength > 0) {
                strength *= awayFromArcaneColumns(x, z);
            }
            if (strength == 0) {
                return new TerracedCeiling.Column(even, 0, even, 0);
            }
            int lowest = Math.max(CEILING_LOWEST_Y, even - CEILING_STEP);
            int highest = Math.max(even, Math.min(even + CEILING_STEP, underside + CEILING_RISE_INTO_ISLAND));
            // The fine relief only lowers the ceiling, so the highest terrace stays in the ceiling biomes.
            float relief = ceilingReliefNoise.getOrCreateNoise(this.randomState).noise(x, z) * 2f;
            return new TerracedCeiling.Column(
                    even + strength * (Math.max(CEILING_LOWEST_Y, lowest - relief) - even), strength * (highest - lowest), highest + 2, strength
            );
        }

        /** 0 next to an arcane column, so that it ends in the even ceiling and its runes stay open, 1 away from them. */
        private float awayFromArcaneColumns(int x, int z) {
            float away = 1;
            for (int i = 0; i < 9; i++) {
                int columnX = this.chunkX + i / 3 - 1;
                int columnZ = this.chunkZ + i % 3 - 1;
                if (this.arcaneColumns[i] == 0) {
                    this.arcaneColumns[i] = (byte) (ArcaneStructureGen.hasColumn(columnX, columnZ, this.randomState) ? 2 : 1);
                }
                if (this.arcaneColumns[i] == 2) {
                    away = Math.min(away, (ArcaneStructureGen.distanceToColumn(columnX, columnZ, x, z) - 3) / 9f);
                }
            }
            return Math.max(away, 0);
        }
    }

    public static void generateTopLayerFloor(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xx = chunk.getPos().getMinBlockX() + x;
                int zz = chunk.getPos().getMinBlockZ() + z;
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
                    terrain.set(x, y, z, stone);
                }
                for (int y = subfloorStartY; y < topY; y++) {
                    terrain.set(x, y, z, subfloor);
                }
                terrain.set(x, topY, z, RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState));
            }
        }
    }

    public static void generateHangingSoil(TerrainWriter terrain, RandomState randomState) {
        HangingTerrainGenerator.generate(terrain, randomState, HANGING_SOIL);
    }

    /** The surface block of the top layer in this column, if an island is here. */
    public static OptionalInt topLayerSurfaceY(int x, int z, RandomState randomState) {
        HangingTerrainGenerator.TerrainColumn column = topLayerColumnAt(x, z, randomState);
        return column.present() ? OptionalInt.of(column.topY()) : OptionalInt.empty();
    }

    private static HangingTerrainGenerator.TerrainColumn topLayerColumnAt(int x, int z, RandomState randomState) {
        HangingTerrainGenerator.TerrainColumn column = islandColumnAt(x, z, topLevelNoise, TOP_LAYER_Y, randomState);
        // Columns of 2 blocks or less are left out; the Deep caves floor keeps them.
        return column.topY() - (int) column.baseline() <= 2 ? HangingTerrainGenerator.TerrainColumn.NONE : column;
    }

    /** An island of the top layer or of the Deep caves floor: the relief noise stands on a baseline from {@code baseY}. */
    static HangingTerrainGenerator.TerrainColumn islandColumnAt(int x, int z, LazyNoise relief, int baseY, RandomState randomState) {
        float noise = relief.getOrCreateNoise(randomState).noise(x, z);
        if (noise < 0.01f) {
            return HangingTerrainGenerator.TerrainColumn.NONE;
        }
        float biomeHeight = noise * TOP_LAYER_TERRAIN_HEIGHT - ARCANE_PLATE_HEIGHT;
        float baseline = baseY + TOP_LAYER_MAX_BASELINE_HEIGHT * topLevelBaselineNoise.getOrCreateNoise(randomState).noise(x, z) + TOP_LAYER_OFFSET;
        return new HangingTerrainGenerator.TerrainColumn(true, baseline, (int) (baseline + biomeHeight));
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
