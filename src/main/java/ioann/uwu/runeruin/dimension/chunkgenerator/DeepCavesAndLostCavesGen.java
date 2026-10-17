package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.ARCANE_PLATE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.CEILING_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.CEILING_VOID_Y;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_MIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_OFFSET;

public class DeepCavesAndLostCavesGen {

    private static final LazyNoise floorNoise = LazyNoise.single("LostCavesFloorNoise", 1f);

    private static final HangingTerrainGenerator.Profile HANGING_MOSS = new HangingTerrainGenerator.Profile(
            DeepCavesAndLostCavesGen::deepCavesFloorColumnAt,
            LOST_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_OFFSET,
            new HangingTerrainGenerator.Supports(
                    state -> state.is(Blocks.STONE) || state.is(BlockTags.MOSS_BLOCKS),
                    state -> state.is(BlockTags.MOSS_BLOCKS),
                    state -> state.is(BlockTags.MOSS_BLOCKS),
                    state -> state.is(BlockTags.BASE_STONE_OVERWORLD)
            ),
            new HangingTerrainGenerator.Materials(
                    DeepCavesAndLostCavesGen::mossAt,
                    DeepCavesAndLostCavesGen::mossAt,
                    Blocks.STONE.defaultBlockState()
            ),
            RR.id("hanging_moss_shape")
    );

    public static int lostCavesFloorY(int x, int z, RandomState randomState) {
        return lostCavesFloorY(x, z, randomState.sampler());
    }

    public static int lostCavesFloorY(int x, int z, Climate.Sampler sampler) {
        float noise = floorNoise.getOrCreateNoise(sampler).noise(x, z);
        int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));
        return LOST_CAVES_Y + biomeHeight;
    }

    public static void generateLostCavesFloor(TerrainWriter terrain, RandomState randomState) {
        ChunkPos pos = terrain.chunk().getPos();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // The floor reads its noise 8 blocks aside.
                int topY = lostCavesFloorY(pos.getMinBlockX() + x + 8, pos.getMinBlockZ() + z + 8, randomState);
                for (int y = CEILING_VOID_Y + 1; y <= topY; y++) {
                    terrain.set(x, y, z, deepslate);
                }
            }
        }
    }

    private static final LazyNoise lostCavesCeilingNoise = new LazyNoise(
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("LostCavesCeilingNoise1" + seed)),
                    Noise.constant(1f)
            )
    );

    private static final LazyNoise bedrockNoise = new LazyNoise(seed -> new PositionalRandomNoise("lostCavesCeilingRock", seed));

    private static final LazyNoise lostTopLevelNoise = RRChunkGenerator.lostTopLevelNoise;
    private static final LazyNoise topLevelBaselineNoise = RRChunkGenerator.topLevelBaselineNoise;
    private static final LazyNoise flattenedBaseLostTopLevelNoise = RRChunkGenerator.flattenedLostBaseTopLevelNoise;

    private static final int NO_CEILING = Integer.MIN_VALUE;

    /** The block at this height is the highest of the Lost caves ceiling; the islands of the Deep caves floor start above it. */
    private static float lostCavesCeilingBaseLine(int x, int z, RandomState randomState) {
        return LOST_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * topLevelBaselineNoise.getOrCreateNoise(randomState).noise(x, z) + TOP_LAYER_OFFSET;
    }

    /** The lowest block of the Lost caves ceiling, or {@link #NO_CEILING}. */
    private static int lostCavesCeilingLowestY(int x, int z, float baseLine, RandomState randomState) {
        float thickness = lostCavesCeilingNoise.getOrCreateNoise(randomState).noise(x, z)
                * flattenedBaseLostTopLevelNoise.getOrCreateNoise(randomState).noise(x, z);
        return thickness < 0.01f ? NO_CEILING : (int) (baseLine - (int) (CEILING_TERRAIN_HEIGHT * thickness));
    }

    public static void generateLostCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkPos pos = terrain.chunk().getPos();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xx = pos.getMinBlockX() + x;
                int zz = pos.getMinBlockZ() + z;
                float baseLine = lostCavesCeilingBaseLine(xx, zz, randomState);
                int lowest = lostCavesCeilingLowestY(xx, zz, baseLine, randomState);
                if (lowest == NO_CEILING) {
                    continue;
                }
                for (int y = lowest; y < (int) baseLine; y++) {
                    terrain.set(x, y, z, deepslate);
                }
                terrain.set(x, (int) baseLine, z, bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f ? deepslate : stone);
            }
        }
    }

    public static void generateDeepCavesFloor(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int xx = chunk.getPos().getMinBlockX() + x;
                int zz = chunk.getPos().getMinBlockZ() + z;
                HangingTerrainGenerator.TerrainColumn column = deepCavesFloorColumnAt(xx, zz, randomState);
                if (!column.present()) {
                    continue;
                }
                int topY = column.topY();
                // Two blocks of stone at least carry the surface, also where the island is thinner.
                for (int y = Math.min((int) column.baseline(), topY - 2); y < topY; y++) {
                    terrain.set(x, y, z, stone);
                }
                RRTerrainSurfaces.placeFloorSurface(
                        terrain, x, topY, z, RRTerrainSurfaces.floorAt(chunk, xx, topY, zz, randomState)
                );
            }
        }
    }

    public static void generateHangingMoss(TerrainWriter terrain, RandomState randomState) {
        HangingTerrainGenerator.generate(terrain, randomState, HANGING_MOSS);
    }

    private static HangingTerrainGenerator.TerrainColumn deepCavesFloorColumnAt(int x, int z, RandomState randomState) {
        return TopLayerAndBloomingCavesGen.islandColumnAt(x, z, lostTopLevelNoise, DEEP_CAVES_Y - ARCANE_PLATE_HEIGHT, randomState);
    }

    private static BlockState mossAt(ChunkAccess chunk, BlockPos start, RandomState randomState) {
        return RRTerrainSurfaces.floorAt(chunk, start.getX(), start.getY(), start.getZ(), randomState);
    }

    /**
     * True if lost-caves ceiling or deep-caves floor stone occupies any Y in {@code [minY, maxY]} at this column.
     */
    public static boolean interLayerTerrainOverlaps(int x, int z, int minY, int maxY, RandomState randomState) {
        HangingTerrainGenerator.TerrainColumn floor = deepCavesFloorColumnAt(x, z, randomState);
        if (floor.present() && rangesOverlap((int) floor.baseline(), floor.topY(), minY, maxY)) {
            return true;
        }
        float baseLine = lostCavesCeilingBaseLine(x, z, randomState);
        int lowest = lostCavesCeilingLowestY(x, z, baseLine, randomState);
        return lowest != NO_CEILING && rangesOverlap(lowest, (int) baseLine, minY, maxY);
    }

    private static boolean rangesOverlap(int a0, int a1, int b0, int b1) {
        return a1 >= b0 && a0 <= b1;
    }
}
