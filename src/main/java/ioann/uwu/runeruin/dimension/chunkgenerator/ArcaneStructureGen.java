package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.runes.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;

import static ioann.uwu.runeruin.dimension.Const.*;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_OFFSET;

public class ArcaneStructureGen {

    public static void generateArcaneStructure(ChunkAccess chunk, RandomState randomState) {

        for (int y = CEILING_VOID_Y + 1; y < LOST_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
                }
            }
        }
        for (int y = LOST_CAVES_CEILING_Y + 1; y < DEEP_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
                }
            }
        }
        for (int y = DEEP_CAVES_CEILING_Y + 1; y < BLOOMING_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
                }
            }
        }

        if (doGenerateColumn(chunk.getPos(), randomState)) {
            generateArcaneColumn(chunk, randomState);
        }
    }

    private static final LazyNoise topLevelBaselineNoise = RRChunkGenerator.topLevelBaselineNoise;
    private static final LazyNoise topLevelNoise = RRChunkGenerator.topLevelNoise;

    private static final Identifier FILL_ARCANE_STRUCTURE_ID = RR.id("fill_arcane_structure");

    private static void generateArcaneColumn(ChunkAccess chunk, RandomState randomState) {

        int chX = chunk.getPos().getBlockAt(0, 0, 0).getX();
        int chZ = chunk.getPos().getBlockAt(0, 0, 0).getZ();

        float baselineNoiseXZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 3);
        float baselineNoiseXN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 3);
        float baselineNoiseNZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 12);
        float baselineNoiseNN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 12);

        float maxNoise = Math.max(Math.max(baselineNoiseXZ, baselineNoiseXN), Math.max(baselineNoiseNZ, baselineNoiseNN));
        float baseLine = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * maxNoise + 1 + TOP_LAYER_OFFSET;

        for (int y = BLOOMING_CAVES_CEILING_Y; y < baseLine; y++) {
            for (int x = 3; x < 13; x++) {
                for (int z = 2; z < 14; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
                }
            }
            int x = 2;
            for (int z = 3; z < 13; z++) {
                chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
            }
            x = 13;
            for (int z = 3; z < 13; z++) {
                chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
            }
        }

        RandomSource random = randomState.getOrCreateRandomFactory(FILL_ARCANE_STRUCTURE_ID)
                .at(chunk.getPos().getMiddleBlockPosition(10));

        addSideRunes(chunk, random);
    }

    private static final List<List<List<Boolean>>> RUNES_DESCRIPTION = Runes.list();

    private static void addSideRunes(ChunkAccess chunk, RandomSource random) {

        int idx = random.nextIntBetweenInclusive(0, RUNES_DESCRIPTION.size() - 1);
        var runeDesc = RUNES_DESCRIPTION.get(idx);

        int y = BLOOMING_CAVES_CEILING_Y - 15 - random.nextIntBetweenInclusive(-2, 15);

        boolean rotate = random.nextBoolean();
        boolean opposite = random.nextBoolean();

        if (!rotate) {
            int x;
            int xInner;
            if (!opposite) {
                x = 2;
                xInner = 3;
            } else {
                x = 13;
                xInner = 12;
            }
            int z = 3;
            for (int zz = 0; zz < 10; zz++) {
                for (int yy = 0; yy < 16; yy++) {
                    if (runeDesc.get(yy).get(zz)) {
                        chunk.setBlockState(new BlockPos(x, y - yy, z + zz), Blocks.AIR.defaultBlockState());
                        chunk.setBlockState(new BlockPos(xInner, y - yy, z + zz), RRBlocks.DIAMOND_ARCANE_STONE.get().defaultBlockState());
                    }
                }
            }
        } else {
            int z;
            int zInner;
            if (!opposite) {
                z = 2;
                zInner = 3;
            } else {
                z = 13;
                zInner = 12;
            }
            int x = 3;
            for (int xx = 0; xx < 10; xx++) {
                for (int yy = 0; yy < 16; yy++) {
                    if (runeDesc.get(yy).get(xx)) {
                        chunk.setBlockState(new BlockPos(x + xx, y - yy, z), Blocks.AIR.defaultBlockState());
                        chunk.setBlockState(new BlockPos(x + xx, y - yy, zInner), RRBlocks.DIAMOND_ARCANE_STONE.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private static boolean doGenerateColumn(ChunkPos chunkPos, RandomState randomState) {
        boolean simple = doGenerateColumnSimple(chunkPos, randomState);
        if (!simple) {
            return false;
        }

        return (!doGenerateColumnSimple(new ChunkPos(chunkPos.x() + 1, chunkPos.z()), randomState)
                || !doGenerateColumnSimple(new ChunkPos(chunkPos.x() - 1, chunkPos.z()), randomState))
                && (!doGenerateColumnSimple(new ChunkPos(chunkPos.x(), chunkPos.z() + 1), randomState)
                || !doGenerateColumnSimple(new ChunkPos(chunkPos.x(), chunkPos.z() - 1), randomState))
                && (!doGenerateColumnSimple(new ChunkPos(chunkPos.x() + 1, chunkPos.z() + 1), randomState)
                || !doGenerateColumnSimple(new ChunkPos(chunkPos.x() - 1, chunkPos.z() - 1), randomState))
                && (!doGenerateColumnSimple(new ChunkPos(chunkPos.x() + 1, chunkPos.z() - 1), randomState)
                || !doGenerateColumnSimple(new ChunkPos(chunkPos.x() - 1, chunkPos.z() + 1), randomState));
    }

    private static boolean doGenerateColumnSimple(ChunkPos chunkPos, RandomState randomState) {

        BlockPos xzBlock = chunkPos.getBlockAt(15, 0, 15);
        BlockPos xnBlock = chunkPos.getBlockAt(15, 0, 0);
        BlockPos nzBlock = chunkPos.getBlockAt(0, 0, 15);
        BlockPos nnBlock = chunkPos.getBlockAt(0, 0, 0);

        float xzNoise = topLevelNoise.getOrCreateNoise(randomState).noise(xzBlock.getX(), xzBlock.getZ());
        float xnNoise = topLevelNoise.getOrCreateNoise(randomState).noise(xnBlock.getX(), xnBlock.getZ());
        float nzNoise = topLevelNoise.getOrCreateNoise(randomState).noise(nzBlock.getX(), nzBlock.getZ());
        float nnNoise = topLevelNoise.getOrCreateNoise(randomState).noise(nnBlock.getX(), nnBlock.getZ());

        // Generate column only if all vertices are on top layer
        // TODO: Or all vertices are on bottom layer to avoid floating islands
        for (float noise : List.of(xzNoise, xnNoise, nzNoise, nnNoise)) {
            if (noise < 0.01) {
                return false;
            }
        }

        ChunkPos xzChunk = new ChunkPos(chunkPos.x() + 1, chunkPos.z() + 1);
        ChunkPos xnChunk = new ChunkPos(chunkPos.x() + 1, chunkPos.z() - 1);
        ChunkPos nzChunk = new ChunkPos(chunkPos.x() - 1, chunkPos.z() + 1);
        ChunkPos nnChunk = new ChunkPos(chunkPos.x() - 1, chunkPos.z() - 1);

        // Generate column only if one of diagonal chunks is a hole
        for (ChunkPos chPos : List.of(xzChunk, xnChunk, nzChunk, nnChunk)) {
            int x = chPos.getMiddleBlockX();
            int z = chPos.getMiddleBlockZ();

            float noise = topLevelNoise.getOrCreateNoise(randomState).noise(x, z);
            if (noise < 0.01) {
                return true;
            }
        }

        // No diagonal chunks are holes. We are in the middle of an island, no need for column
        return false;
    }
}
