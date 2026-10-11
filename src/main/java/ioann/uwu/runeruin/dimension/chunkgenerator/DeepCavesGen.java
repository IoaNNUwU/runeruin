package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_HEIGHT;

/** The ceiling of the Deep caves: terraces from {@link #LOWEST_Y} up into the floor of the Blooming caves. */
public class DeepCavesGen {

    /** Nothing of the ceiling is lower than this. */
    public static final int LOWEST_Y = DEEP_CAVES_CEILING_Y - 12;
    /** Stone that always stays under the surface of the Blooming caves floor. */
    public static final int CRUST = 6;
    /** The highest {@link #roofY}: nothing is open from here up to the Blooming caves. */
    public static final int HIGHEST_Y = BLOOMING_CAVES_Y + TERRAIN_HEIGHT - CRUST;

    private static final LazyNoise reliefNoise = LazyNoise.single("deepCavesCeilingRelief", 9f);

    private static final TerracedCeiling CEILING = new TerracedCeiling("deepCavesCeiling", LOWEST_Y, HIGHEST_Y, 3,
            (chunkX, chunkZ, randomState) -> (x, z) -> {
                int roof = roofY(x, z, randomState.sampler());
                // The highest terrace stays 2 blocks under the roof, in the ceiling biome with what hangs from it.
                return new TerracedCeiling.Column(
                        LOWEST_Y + (reliefNoise.getOrCreateNoise(randomState).noise(x, z) - 0.5f) * 3f, roof - 2 - LOWEST_Y, roof, 1
                );
            });

    /** The lowest block that is never open: stone from here up to the floor of the Blooming caves. */
    public static int roofY(int x, int z, Climate.Sampler sampler) {
        // The floor reads its noise 8 blocks aside.
        return TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x + 8, z + 8, sampler) - CRUST;
    }

    /** The lowest block of the Deep caves ceiling in this column. */
    public static int ceilingSurfaceY(int x, int z, RandomState randomState) {
        return CEILING.chunk(x >> 4, z >> 4, randomState).lowestY(x, z);
    }

    public static void generateDeepCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        ChunkPos pos = chunk.getPos();
        TerracedCeiling.Chunk ceiling = CEILING.chunk(pos.x(), pos.z(), randomState);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = LOWEST_Y; y <= HIGHEST_Y; y++) {
                    boolean solid = ceiling.stone(x, y, z);
                    // Under the Blooming caves the ceiling is built, in their floor it is dug out.
                    if (solid != y >= BLOOMING_CAVES_Y) {
                        terrain.set(x, y, z, solid ? stone : air);
                    }
                }
                for (int y = LOWEST_Y; y <= HIGHEST_Y; y++) {
                    if (!ceiling.stone(x, y, z) || y > LOWEST_Y && ceiling.stone(x, y - 1, z)) {
                        continue;
                    }
                    BlockState surface = RRTerrainSurfaces.ceilingAt(chunk, pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z, randomState);
                    if (y == HIGHEST_Y || ceiling.stone(x, y + 1, z)) {
                        RRTerrainSurfaces.placeCeilingSurface(terrain, x, y, z, surface);
                    } else {
                        terrain.set(x, y, z, surface);
                    }
                }
            }
        }
    }
}
