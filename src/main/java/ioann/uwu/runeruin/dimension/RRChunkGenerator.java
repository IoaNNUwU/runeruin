package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.noise.*;
import ioann.uwu.runeruin.dimension.runes.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ioann.uwu.runeruin.dimension.Const.*;

public class RRChunkGenerator extends ChunkGenerator {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> REGISTRY = DeferredRegister.create(Registries.CHUNK_GENERATOR, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RRChunkGenerator>> CHUNK_GENERATOR = REGISTRY.register("runeruin_chunk_generator", () -> RRChunkGenerator.CODEC);

    public RRChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public static final MapCodec<RRChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source").xmap(RRChunkGenerator::new, RRChunkGenerator::getBiomeSource);

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess) {

    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {

    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {

    }

    @Override
    public int getGenDepth() { // MAX HEIGHT
        return 512;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {

        return CompletableFuture.supplyAsync(() -> {
            // TODO: Split chunk into sections to avoid unnecessary blocking
            generateTerrain(chunk, randomState);
            fillArcaneStructure(chunk, randomState);
            return chunk;
        });
    }

    private static final LazyNoise undergroundNoise = new LazyNoise("undergroundNoise", SingleNoise::new);

    private static final LazyNoise baseTopLevelNoise = new LazyNoise("baseTopLevelNoise", seed -> Noise.multi(
            new SingleNoise(Noise.hashString("bigNoise1" + seed), 0.5f),
            new SingleNoise(Noise.hashString("bigNoise2" + seed), 0.4f),
            new SingleNoise(Noise.hashString("bigNoise3" + seed), 0.3f)
    ));

    private static final LazyNoise flattenedBaseTopLevelNoise = LazyNoise.chain(
            "flattenedBaseTopLevelNoise",
            baseTopLevelNoise,
            noise -> Noise.flatten(0.152f, noise)
    );

    public static final LazyNoise topLevelNoise = LazyNoise.chain(
            "topLevelNoise",
            flattenedBaseTopLevelNoise,
            TopLevelNoise::new
    );

    public static final LazyNoise topLevelBaselineNoise = new LazyNoise(
            "topLevelBaselineNoise",
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("topLevelBaselineNoise1" + seed), 1f),
                    new SingleNoise(Noise.hashString("topLevelBaselineNoise2" + seed), 0.1f),
                    new SingleNoise(Noise.hashString("topLevelBaselineNoise3" + seed), 0.4f)
            )
    );

    private static final LazyNoise bloomingCavesCeilingNoise = new LazyNoise(
            "bloomingCavesCeilingNoise",
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("bloomingCavesCeilingNoise1" + seed)),
                    Noise.constant(1f)
            )
    );

    private static final LazyNoise bedrockNoise = new LazyNoise("bedrockNoise", PositionalRandomNoise::new);

    private void generateTerrain(ChunkAccess chunk, RandomState randomState) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int[] layerStartingHeights = new int[]{LOST_CAVES_Y, DEEP_CAVES_Y, BLOOMING_CAVES_Y};

                for (int i = 0; i < 3; i++) {
                    int xOffset = 4272 * i;
                    int zOffset = 3372 * i;

                    float noise = undergroundNoise.getOrCreateNoise(randomState)
                            .noise(chunk.getPos().getMiddleBlockX() + x + xOffset, chunk.getPos().getMiddleBlockZ() + z + zOffset);

                    int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));

                    for (int y = layerStartingHeights[i]; y < layerStartingHeights[i] + biomeHeight + 1; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int y = DEEP_CAVES_CEILING_Y;
                chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                y = LOST_CAVES_CEILING_Y;
                chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                y = CEILING_VOID_Y;
                chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
            }
        }

        // --- CEILING BIOME ---
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                float ceilingNoise = bloomingCavesCeilingNoise.getOrCreateNoise(randomState).noise(xx, zz);
                ceilingNoise = ceilingNoise * flattenedBaseTopLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (ceilingNoise < 0.01) {
                    continue;
                }

                int ceilingHeight = (int) (CEILING_TERRAIN_HEIGHT * ceilingNoise);

                float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                int baseLine = BLOOMING_CAVES_CEILING_Y + (int) (TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise) + TOP_LAYER_OFFSET;

                BlockState blockState;
                if (bedrockNoise.getOrCreateNoise(randomState).noise(xx, 1f, zz) > 0.5f) {
                    blockState = Blocks.DEEPSLATE.defaultBlockState();
                } else {
                    blockState = Blocks.STONE.defaultBlockState();
                }
                chunk.setBlockState(new BlockPos(x, baseLine, z), blockState);

                for (int y = baseLine - ceilingHeight + 1; y < baseLine; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DEEPSLATE.defaultBlockState());
                }
                chunk.setBlockState(new BlockPos(x, baseLine - ceilingHeight, z), Blocks.MOSS_BLOCK.defaultBlockState());
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int xx = chunk.getPos().getBlockAt(0, 0, 0).getX() + x;
                int zz = chunk.getPos().getBlockAt(0, 0, 0).getZ() + z;

                float noise = topLevelNoise.getOrCreateNoise(randomState).noise(xx, zz);

                if (noise < 0.01) {
                    continue;
                }

                int biomeHeight = (int) (noise * (TOP_LAYER_TERRAIN_HEIGHT)) - ARCANE_PLATE_HEIGHT;

                float baselineNoise = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(xx, zz);
                int baseLine = TOP_LAYER_Y + (int) (TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise) + TOP_LAYER_OFFSET;

                for (int y = baseLine; y < baseLine + biomeHeight - 2; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
                for (int y = baseLine + biomeHeight - 2; y < baseLine + biomeHeight; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
                }
                chunk.setBlockState(new BlockPos(x, baseLine + biomeHeight, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    private void fillArcaneStructure(ChunkAccess chunk, RandomState randomState) {

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

    private static final Identifier FILL_ARCANE_STRUCTURE_ID = RR.id("fill_arcane_structure");

    private static final List<List<List<Boolean>>> RUNES_DESCRIPTION = Runes.list();

    private static void generateArcaneColumn(ChunkAccess chunk, RandomState randomState) {

        RandomSource random = randomState.getOrCreateRandomFactory(FILL_ARCANE_STRUCTURE_ID).at(chunk.getPos().getMiddleBlockPosition(10));

        int chX = chunk.getPos().getBlockAt(0, 0, 0).getX();
        int chZ = chunk.getPos().getBlockAt(0, 0, 0).getZ();

        float baselineNoiseXZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 3);
        float baselineNoiseXN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 3);
        float baselineNoiseNZ = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 3, chZ + 12);
        float baselineNoiseNN = topLevelBaselineNoise.getOrCreateNoise(randomState).noise(chX + 12, chZ + 12);

        float maxNoise = Math.max(Math.max(baselineNoiseXZ, baselineNoiseXN), Math.max(baselineNoiseNZ, baselineNoiseNN));
        int baseLine = BLOOMING_CAVES_CEILING_Y + (int) (TOP_LAYER_MAX_BASELINE_HEIGHT * maxNoise) + 1 + TOP_LAYER_OFFSET;

        for (int y = LOST_CAVES_Y; y < baseLine; y++) {
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

        // --- Side wall Runes ---
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

    @Override
    public int getSeaLevel() {
        return BLOOMING_CAVES_CEILING_Y;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {

        float topNoise = topLevelNoise.getOrCreateNoise(randomState).noise(x, z);
        if (topNoise > 0.01f) {
            return (int) (topNoise * (TOP_LAYER_TERRAIN_HEIGHT)) - ARCANE_PLATE_HEIGHT + TOP_LAYER_Y;
        } else {
            return BLOOMING_CAVES_Y + TERRAIN_HEIGHT;
        }
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(0, new BlockState[]{});
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {

    }
}
