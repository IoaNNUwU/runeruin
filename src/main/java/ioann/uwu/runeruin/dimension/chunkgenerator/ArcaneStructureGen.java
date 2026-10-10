package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SupportedIslandsNoise;
import ioann.uwu.runeruin.dimension.runes.Runes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public static void generateArcaneStructure(TerrainWriter terrain, RandomState randomState) {
        BlockState arcaneStone = RRBlocks.ARCANE_STONE.get().defaultBlockState();

        for (int y = CEILING_VOID_Y + 1; y < LOST_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    terrain.set(x, y, z, arcaneStone);
                }
            }
        }
        /*
        for (int y = LOST_CAVES_CEILING_Y + 1; y < DEEP_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    terrain.set(x, y, z, arcaneStone);
                }
            }
        }
         */
        for (int y = DEEP_CAVES_CEILING_Y + 1; y < BLOOMING_CAVES_Y; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    terrain.set(x, y, z, arcaneStone);
                }
            }
        }

        ChunkPos chunkPos = terrain.chunk().getPos();
        Noise topLevel = topLevelNoise.getOrCreateNoise(randomState);
        if (doGenerateColumn(chunkPos.x(), chunkPos.z(), (x, z) -> topLevel.noise(x, z) >= 0.01f)) {
            generateArcaneColumn(terrain, randomState);
        }
    }

    private static final LazyNoise topLevelBaselineNoise = RRChunkGenerator.topLevelBaselineNoise;
    private static final LazyNoise topLevelNoise = RRChunkGenerator.topLevelNoise;

    private static final Identifier FILL_ARCANE_STRUCTURE_ID = RR.id("fill_arcane_structure");

    private static void generateArcaneColumn(TerrainWriter terrain, RandomState randomState) {
        BlockState arcaneStone = RRBlocks.ARCANE_STONE.get().defaultBlockState();

        int chX = terrain.chunk().getPos().getBlockAt(0, 0, 0).getX();
        int chZ = terrain.chunk().getPos().getBlockAt(0, 0, 0).getZ();

        float baselineNoiseXZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 3);
        float baselineNoiseXN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 3);
        float baselineNoiseNZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 12);
        float baselineNoiseNN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 12);

        float maxNoise = Math.max(Math.max(baselineNoiseXZ, baselineNoiseXN), Math.max(baselineNoiseNZ, baselineNoiseNN));
        float baseLine = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * maxNoise + 1 + TOP_LAYER_OFFSET;

        for (int y = BLOOMING_CAVES_Y; y < baseLine; y++) {
            for (int x = 3; x < 13; x++) {
                for (int z = 2; z < 14; z++) {
                    terrain.set(x, y, z, arcaneStone);
                }
            }
            int x = 2;
            for (int z = 3; z < 13; z++) {
                terrain.set(x, y, z, arcaneStone);
            }
            x = 13;
            for (int z = 3; z < 13; z++) {
                terrain.set(x, y, z, arcaneStone);
            }
        }

        RandomSource random = randomState.getOrCreateRandomFactory(FILL_ARCANE_STRUCTURE_ID)
                .at(terrain.chunk().getPos().getMiddleBlockPosition(10));

        addSideRunes(terrain, random);
    }

    private static final List<List<List<Boolean>>> RUNES_DESCRIPTION = Runes.list();

    private static void addSideRunes(TerrainWriter terrain, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState diamondArcane = RRBlocks.DIAMOND_ARCANE_STONE.get().defaultBlockState();

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
                        terrain.set(x, y - yy, z + zz, air);
                        terrain.set(xInner, y - yy, z + zz, diamondArcane);
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
                        terrain.set(x + xx, y - yy, z, air);
                        terrain.set(x + xx, y - yy, zInner, diamondArcane);
                    }
                }
            }
        }
    }

    /** Whether a column stands in this chunk. Islands with no column under them are not generated: {@link SupportedIslandsNoise}. */
    public static boolean doGenerateColumn(int chunkX, int chunkZ, SupportedIslandsNoise.Land topLayer) {
        if (!doGenerateColumnSimple(chunkX, chunkZ, topLayer)) {
            return false;
        }

        return (!doGenerateColumnSimple(chunkX + 1, chunkZ, topLayer)
                || !doGenerateColumnSimple(chunkX - 1, chunkZ, topLayer))
                && (!doGenerateColumnSimple(chunkX, chunkZ + 1, topLayer)
                || !doGenerateColumnSimple(chunkX, chunkZ - 1, topLayer))
                && (!doGenerateColumnSimple(chunkX + 1, chunkZ + 1, topLayer)
                || !doGenerateColumnSimple(chunkX - 1, chunkZ - 1, topLayer))
                && (!doGenerateColumnSimple(chunkX + 1, chunkZ - 1, topLayer)
                || !doGenerateColumnSimple(chunkX - 1, chunkZ + 1, topLayer));
    }

    private static boolean doGenerateColumnSimple(int chunkX, int chunkZ, SupportedIslandsNoise.Land topLayer) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        // Generate column only if all vertices are on top layer
        if (!topLayer.at(minX + 15, minZ + 15) || !topLayer.at(minX + 15, minZ)
                || !topLayer.at(minX, minZ + 15) || !topLayer.at(minX, minZ)) {
            return false;
        }

        // Generate column only if the middle of one of diagonal chunks is a hole.
        // Otherwise we are in the middle of an island, no need for column
        return !topLayer.at(minX + 24, minZ + 24) || !topLayer.at(minX + 24, minZ - 8)
                || !topLayer.at(minX - 8, minZ + 24) || !topLayer.at(minX - 8, minZ - 8);
    }
}
