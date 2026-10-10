package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;

public class DeepCavesGen {

    private static final LazyNoise ceilingNoise = new LazyNoise("deepCavesCeilingNoise",
            (seed) -> Noise.multi(
                    new SingleNoise(Noise.hashString("deepCavesCeilingNoise1" + seed), 10f),
                    new SingleNoise(Noise.hashString("deepCavesCeilingNoise2" + seed), 1f),
                    new SingleNoise(Noise.hashString("deepCavesCeilingNoise3" + seed), 4f)
            )
    );

    /** The lowest block of the Deep caves ceiling in this column. The ceiling reads its noise 8 blocks aside. */
    public static int ceilingSurfaceY(int x, int z, RandomState randomState) {
        float noise = ceilingNoise.getOrCreateNoise(randomState).noise(x + 8, z + 8);
        return DEEP_CAVES_CEILING_Y - (int) (CEILING_TERRAIN_MIN_HEIGHT + noise * (CEILING_TERRAIN_HEIGHT - CEILING_TERRAIN_MIN_HEIGHT));
    }

    public static void generateDeepCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getMiddleBlockX() + x;
                int zz = chunk.getPos().getMiddleBlockZ() + z;

                int ceilingSurfaceY = ceilingSurfaceY(chunk.getPos().getMinBlockX() + x, chunk.getPos().getMinBlockZ() + z, randomState);
                int biomeHeight = DEEP_CAVES_CEILING_Y - ceilingSurfaceY;

                for (int y = 0; y < biomeHeight; y++) {
                    terrain.set(x, DEEP_CAVES_CEILING_Y - y, z, stone);
                }
                RRTerrainSurfaces.placeCeilingSurface(
                        terrain, x, ceilingSurfaceY, z,
                        RRTerrainSurfaces.ceilingAt(chunk, xx, ceilingSurfaceY, zz, randomState)
                );
            }
        }
    }
}
