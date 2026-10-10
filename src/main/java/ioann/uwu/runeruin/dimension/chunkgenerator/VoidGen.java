package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;

public class VoidGen {

    private static final LazyNoise ceilingNoise = LazyNoise.single("voidCeilingNoise", 1f);

    public static void generateVoidCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getMiddleBlockX() + x;
                int zz = chunk.getPos().getMiddleBlockZ() + z;

                float noise = ceilingNoise.getOrCreateNoise(randomState).noise(xx, zz);

                int biomeHeight = (int) (CEILING_TERRAIN_MIN_HEIGHT + noise * (CEILING_TERRAIN_HEIGHT - CEILING_TERRAIN_MIN_HEIGHT));
                // The whole ceiling is made of the biome's block, not only its surface.
                BlockState stone = RRTerrainSurfaces.ceilingAt(chunk, xx, CEILING_VOID_Y - biomeHeight, zz, randomState);

                for (int y = 0; y < biomeHeight + 1; y++) {
                    terrain.set(x, CEILING_VOID_Y - y, z, stone);
                }
            }
        }
    }
}
