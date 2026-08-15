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

public class DeepCavesAndLostCavesGen {

    private static final LazyNoise floorNoise = new LazyNoise("LostCavesFloorNoise", SingleNoise::new);

    public static void generateLostCavesFloor(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = floorNoise.getOrCreateNoise(randomState)
                        .noise(chunk.getPos().getMiddleBlockX() + x, chunk.getPos().getMiddleBlockZ() + z);

                int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));

                for (int y = LOST_CAVES_Y; y < LOST_CAVES_Y + biomeHeight + 1; y++) {
                    chunk.setBlockState(pos.set(x, y, z), deepslate);
                }
            }
        }
    }

    private static final LazyNoise lostCavesCeilingNoise = new LazyNoise(
            "LostCavesCeilingNoise",
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("LostCavesCeilingNoise1" + seed)),
                    Noise.constant(1f)
            )
    );

    private static final LazyNoise bedrockNoise = new LazyNoise("deepBedrockNoise", PositionalRandomNoise::new);

    private static final LazyNoise lostTopLevelNoise = RRChunkGenerator.lostTopLevelNoise;
    private static final LazyNoise lostTopLevelBaselineNoise = RRChunkGenerator.lostTopLevelBaselineNoise;
    private static final LazyNoise flattenedBaseLostTopLevelNoise = RRChunkGenerator.flattenedLostBaseTopLevelNoise;

    public static void generateLostCavesCeiling(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                float ceilingNoise = lostCavesCeilingNoise.getOrCreateNoise(randomState).noise(xx, zz);
                ceilingNoise = ceilingNoise * flattenedBaseLostTopLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (ceilingNoise < 0.01) {
                    continue;
                }

                float ceilingHeight = (int) (CEILING_TERRAIN_HEIGHT * ceilingNoise);

                float baselineNoise = lostTopLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                float baseLine = LOST_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET;

                BlockState blockState = bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f
                        ? deepslate
                        : stone;
                chunk.setBlockState(pos.set(x, (int) baseLine, z), blockState);

                for (int y = (int) (baseLine - ceilingHeight + 1); y < baseLine - 1; y++) {
                    chunk.setBlockState(pos.set(x, y, z), deepslate);
                }
                chunk.setBlockState(pos.set(x, (int) (baseLine - ceilingHeight), z), deepslate);
            }
        }
    }

    public static void generateDeepCavesFloor(ChunkAccess chunk, RandomState randomState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                float noise = lostTopLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (noise < 0.01) {
                    continue;
                }

                float biomeHeight = noise * (TOP_LAYER_TERRAIN_HEIGHT) - ARCANE_PLATE_HEIGHT;

                float baselineNoise = lostTopLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                float baseLine = DEEP_CAVES_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET - ARCANE_PLATE_HEIGHT;

                for (int y = (int) (baseLine); y < baseLine + biomeHeight - 2; y++) {
                    chunk.setBlockState(pos.set(x, y, z), stone);
                }
                for (int y = (int) (baseLine + biomeHeight) - 2; y < baseLine + biomeHeight; y++) {
                    chunk.setBlockState(pos.set(x, y, z), stone);
                }
                chunk.setBlockState(pos.set(x, (int) (baseLine + biomeHeight), z), stone);
            }
        }
    }
}
