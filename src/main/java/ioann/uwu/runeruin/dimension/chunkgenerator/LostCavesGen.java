package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;

public class LostCavesGen {

    private static final LazyNoise floorNoise = new LazyNoise("lostCavesFloorNoise", SingleNoise::new);

    public static void generateLostCavesCeiling(ChunkAccess chunk, RandomState randomState) {

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = floorNoise.getOrCreateNoise(randomState)
                        .noise(chunk.getPos().getMiddleBlockX() + x, chunk.getPos().getMiddleBlockZ() + z);

                int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));

                for (int y = LOST_CAVES_Y; y < LOST_CAVES_Y + biomeHeight + 1; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
    }

    private static final LazyNoise ceilingNoise = new LazyNoise("lostCavesCeilingNoise", SingleNoise::new);

    public static void generateLostCavesFloor(ChunkAccess chunk, RandomState randomState) {

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = ceilingNoise.getOrCreateNoise(randomState)
                        .noise(chunk.getPos().getMiddleBlockX() + x, chunk.getPos().getMiddleBlockZ() + z);

                int biomeHeight = (int) (CEILING_TERRAIN_MIN_HEIGHT + noise * (CEILING_TERRAIN_HEIGHT - CEILING_TERRAIN_MIN_HEIGHT));

                for (int y = 0; y < biomeHeight + 1; y++) {

                    int yy = LOST_CAVES_CEILING_Y - y;

                    chunk.setBlockState(new BlockPos(x, yy, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
    }
}
