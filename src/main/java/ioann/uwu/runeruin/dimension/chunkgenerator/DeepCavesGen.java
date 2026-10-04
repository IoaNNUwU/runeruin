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

    public static void generateDeepCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getMiddleBlockX() + x;
                int zz = chunk.getPos().getMiddleBlockZ() + z;

                float noise = ceilingNoise.getOrCreateNoise(randomState).noise(xx, zz);

                int biomeHeight = (int) (CEILING_TERRAIN_MIN_HEIGHT + noise * (CEILING_TERRAIN_HEIGHT - CEILING_TERRAIN_MIN_HEIGHT));
                int ceilingSurfaceY = DEEP_CAVES_CEILING_Y - biomeHeight;

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
