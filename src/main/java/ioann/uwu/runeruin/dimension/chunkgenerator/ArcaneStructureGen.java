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
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_OFFSET;

public class ArcaneStructureGen {

    public static void generateArcaneStructure(TerrainWriter terrain, RandomState randomState) {
        ChunkPos chunkPos = terrain.chunk().getPos();
        if (hasColumn(chunkPos.x(), chunkPos.z(), randomState)) {
            generateArcaneColumn(terrain, randomState);
        }
    }

    public static boolean hasColumn(int chunkX, int chunkZ, RandomState randomState) {
        Noise topLevel = topLevelNoise.getOrCreateNoise(randomState);
        return doGenerateColumn(chunkX, chunkZ, (x, z) -> topLevel.noise(x, z) >= 0.01f);
    }

    /** How many blocks lie between a block and the column of a chunk: 0 inside the column. */
    public static int distanceToColumn(int chunkX, int chunkZ, int x, int z) {
        int minX = (chunkX << 4) + 2;
        int minZ = (chunkZ << 4) + 2;
        return Math.max(
                Math.max(0, Math.max(minX - x, x - minX - 11)),
                Math.max(0, Math.max(minZ - z, z - minZ - 11))
        );
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

        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                if ((x == 2 || x == 13) && (z == 2 || z == 13)) {
                    continue;
                }
                // From the stone that the Deep caves never open: no column hangs into them.
                for (int y = DeepCavesGen.roofY(chX + x, chZ + z, randomState.sampler()); y < baseLine; y++) {
                    terrain.set(x, y, z, arcaneStone);
                }
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
