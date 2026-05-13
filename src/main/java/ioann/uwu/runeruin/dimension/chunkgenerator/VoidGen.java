package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;

public class VoidGen {

    private static final LazyNoise ceilingNoise = new LazyNoise("voidCeilingNoise", SingleNoise::new);

    public static void generateVoidCeiling(ChunkAccess chunk, RandomState randomState) {

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = ceilingNoise.getOrCreateNoise(randomState)
                        .noise(chunk.getPos().getMiddleBlockX() + x, chunk.getPos().getMiddleBlockZ() + z);

                int biomeHeight = (int) (CEILING_TERRAIN_MIN_HEIGHT + noise * (CEILING_TERRAIN_HEIGHT - CEILING_TERRAIN_MIN_HEIGHT));

                for (int y = 0; y < biomeHeight + 1; y++) {

                    int yy = CEILING_VOID_Y - y;

                    chunk.setBlockState(new BlockPos(x, yy, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
    }
}
