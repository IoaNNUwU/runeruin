package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.*;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;

public class TopLayerAndBloomingCavesGen {

    private static final LazyNoise floorNoise = new LazyNoise("bloomingCavesFloorNoise", SingleNoise::new);

    public static void generateBloomingCavesFloor(ChunkAccess chunk, RandomState randomState) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = floorNoise.getOrCreateNoise(randomState)
                        .noise(chunk.getPos().getMiddleBlockX() + x, chunk.getPos().getMiddleBlockZ() + z);

                int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));

                for (int y = BLOOMING_CAVES_Y; y < BLOOMING_CAVES_Y + biomeHeight + 1; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
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

                BlockState blockState;
                if (bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f) {
                    blockState = Blocks.DEEPSLATE.defaultBlockState();
                } else {
                    blockState = Blocks.STONE.defaultBlockState();
                }
                chunk.setBlockState(new BlockPos(x, (int) baseLine, z), blockState);

                for (int y = (int) (baseLine - ceilingHeight + 1); y < baseLine - 1; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DEEPSLATE.defaultBlockState());
                }
                chunk.setBlockState(new BlockPos(x, (int) (baseLine - ceilingHeight), z), Blocks.MOSS_BLOCK.defaultBlockState());
            }
        }
    }

    public static void generateTopLayerFloor(ChunkAccess chunk, RandomState randomState) {
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

                for (int y = (int) (baseLine); y < baseLine + biomeHeight - 2; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
                for (int y = (int) (baseLine + biomeHeight) - 2; y < baseLine + biomeHeight; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
                }
                chunk.setBlockState(new BlockPos(x, (int) (baseLine + biomeHeight), z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }
}
