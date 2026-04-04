package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RRChunkGenerator extends ChunkGenerator {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> REGISTRY =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<RRChunkGenerator>> CHUNK_GENERATOR =
            REGISTRY.register("runeruin_chunk_generator", () -> RRChunkGenerator.CODEC);

    public RRChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public static final MapCodec<RRChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
            .xmap(RRChunkGenerator::new, RRChunkGenerator::getBiomeSource);

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private static final int VOID_HEIGHT = 50;
    private static final int UNDERGROUND_LEVEL_HEIGHT = 75;
    private static final int ARCANE_PLATE_HEIGHT = 5;

    private static final int BIOME_HEIGHT = 25;
    private static final int BIOME_MIN_HEIGHT = 10;
    private static final int CEILING_BIOME_HEIGHT = 15;
    private static final int CEILING_BIOME_MIN_HEIGHT = 5;

    private static final int CEILING_VOID_Y = VOID_HEIGHT;
    private static final int LOST_CAVES_Y = CEILING_VOID_Y + ARCANE_PLATE_HEIGHT + 1;
    private static final int LOST_CAVES_CEILING_Y = LOST_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    private static final int DEEP_CAVES_Y = LOST_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    private static final int DEEP_CAVES_CEILING_Y = DEEP_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    private static final int BLOOMING_CAVES_Y = DEEP_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    private static final int BLOOMING_CAVES_CEILING_Y = BLOOMING_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    private static final int TOP_LAYER_Y = BLOOMING_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;

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

    private static final RandomSource randomSource = new WorldgenRandom(new LegacyRandomSource(292912813912L));
    private static final FastNoise arcaneStructureNoise = new FastNoise(randomSource.nextInt());
    private static final FastNoise worldGenNoise = new FastNoise(randomSource.nextInt());

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {

            generateTerrain(chunk);

            fillArcaneStructure(chunk);
            return chunk;
        });
    }

    private void generateTerrain(ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int[] layerStartingHeights = new int[]{
                        LOST_CAVES_Y, DEEP_CAVES_Y, BLOOMING_CAVES_Y
                };

                for (int i = 0; i < 3; i++) {
                    int xOffset = 4272 * i;
                    int zOffset = 3372 * i;

                    float noise = worldGenNoise.GetNoise(
                            chunk.getPos().getMiddleBlockX() + x + xOffset,
                            chunk.getPos().getMiddleBlockZ() + z + zOffset
                    );
                    float normalizedNoise = normalizeNoise(noise);

                    int biomeHeight = (int) (BIOME_MIN_HEIGHT + normalizedNoise * (BIOME_HEIGHT - BIOME_MIN_HEIGHT));

                    for (int y = layerStartingHeights[i]; y < layerStartingHeights[i] + biomeHeight + 1; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                    }

                }
            }
        }

        if (isTopLayer(chunk)) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    float noise = worldGenNoise.GetNoise(
                            chunk.getPos().getMiddleBlockX() + x + 6133,
                            chunk.getPos().getMiddleBlockZ() + z + 2223
                    );
                    float normalizedNoise = normalizeNoise(noise);

                    int biomeHeight = (int) (BIOME_MIN_HEIGHT + normalizedNoise * (BIOME_HEIGHT - BIOME_MIN_HEIGHT));

                    for (int y = TOP_LAYER_Y; y < TOP_LAYER_Y + biomeHeight + 1; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }

    }

    // how far away peaks are from each other and how wide they are
    // default 1.0f - for normal world gen
    // 0.2f         - smooth edges. ideal for generation wide plateaus
    private static final float XZ_SCALE = 0.4f;

    private void fillArcaneStructure(ChunkAccess chunk) {

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

        ChunkVerticesNoise chunkNoise = ChunkVerticesNoise.fromChunk(chunk);

        if (isTopLayer(chunkNoise)) {
            for (int y = BLOOMING_CAVES_CEILING_Y + 1; y < TOP_LAYER_Y; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE.get().defaultBlockState());
                    }
                }
            }
        }

        if (isVertexOfTopLayer(chunkNoise)) {
            generateArcaneColumn(chunk);
        }

        if (RRChunkGenerator.isTopLayerBorder(chunkNoise)) {
            generateTopLevelBorder(chunk, chunkNoise);
        }


    }

    private static void generateArcaneColumn(ChunkAccess chunk) {
        for (int y = LOST_CAVES_Y; y < BLOOMING_CAVES_CEILING_Y + 1; y++) {
            for (int x = 2; x < 14; x++) {
                for (int z = 2; z < 14; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.ARCANE_STONE_PILLAR.get().defaultBlockState());
                }
            }
        }
    }

    private static void generateTopLevelBorder(ChunkAccess chunk, @Nullable ChunkVerticesNoise noise) {
        for (int y = BLOOMING_CAVES_CEILING_Y; y < TOP_LAYER_Y + 1; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockState(new BlockPos(x, y, z), RRBlocks.POLISHED_ARCANE_STONE.get().defaultBlockState());
                }
            }
        }
    }

    private record ChunkVerticesNoise(float xzNoise, float xnNoise, float nzNoise, float nnNoise) {
        public static ChunkVerticesNoise fromChunk(ChunkAccess chunk) {
            BlockPos xzBlock = chunk.getPos().getBlockAt(15, 0, 15);
            BlockPos xnBlock = chunk.getPos().getBlockAt(15, 0, 0);
            BlockPos nzBlock = chunk.getPos().getBlockAt(0, 0, 15);
            BlockPos nnBlock = chunk.getPos().getBlockAt(0, 0, 0);

            float xzNoise = arcaneStructureNoise.GetNoise(xzBlock.getX() * XZ_SCALE, xzBlock.getZ() * XZ_SCALE);
            float xnNoise = arcaneStructureNoise.GetNoise(xnBlock.getX() * XZ_SCALE, xnBlock.getZ() * XZ_SCALE);
            float nzNoise = arcaneStructureNoise.GetNoise(nzBlock.getX() * XZ_SCALE, nzBlock.getZ() * XZ_SCALE);
            float nnNoise = arcaneStructureNoise.GetNoise(nnBlock.getX() * XZ_SCALE, nnBlock.getZ() * XZ_SCALE);

            xzNoise = normalizeNoise(xzNoise);
            xnNoise = normalizeNoise(xnNoise);
            nzNoise = normalizeNoise(nzNoise);
            nnNoise = normalizeNoise(nnNoise);

            return new ChunkVerticesNoise(xzNoise, xnNoise, nzNoise, nnNoise);
        }
    }

    private static boolean isTopLayer(ChunkAccess chunk) {
        return isTopLayer(ChunkVerticesNoise.fromChunk(chunk));
    }

    private static boolean isTopLayer(ChunkVerticesNoise chunk) {
        boolean isAtLeastOneCornerTopLayer = isTopLayer(chunk.xzNoise) || isTopLayer(chunk.xnNoise)
                || isTopLayer(chunk.nzNoise) || isTopLayer(chunk.nnNoise);

        return isAtLeastOneCornerTopLayer;

    }

    private static boolean isTopLayerBorder(ChunkAccess chunk) {
        return isTopLayer(ChunkVerticesNoise.fromChunk(chunk));
    }

    private static boolean isTopLayerBorder(ChunkVerticesNoise chunk) {

        boolean isAllCornersTopLevel = isTopLayer(chunk.xzNoise) && isTopLayer(chunk.xnNoise)
                && isTopLayer(chunk.nzNoise) && isTopLayer(chunk.nnNoise);

        boolean isAllCornersLowLevel = !isTopLayer(chunk.xzNoise) && !isTopLayer(chunk.xnNoise)
                && !isTopLayer(chunk.nzNoise) && !isTopLayer(chunk.nnNoise);

        return !isAllCornersTopLevel && !isAllCornersLowLevel;
    }

    private static boolean isVertexOfTopLayer(ChunkAccess chunk) {
        return isVertexOfTopLayer(ChunkVerticesNoise.fromChunk(chunk));
    }

    private static boolean isVertexOfTopLayer(ChunkVerticesNoise chunk) {

        boolean isSingleCornerTopLevel =
                (isTopLayer(chunk.xzNoise) && !isTopLayer(chunk.xnNoise) && !isTopLayer(chunk.nzNoise) && !isTopLayer(chunk.nnNoise)) ||
                        (!isTopLayer(chunk.xzNoise) && isTopLayer(chunk.xnNoise) && !isTopLayer(chunk.nzNoise) && !isTopLayer(chunk.nnNoise)) ||
                        (!isTopLayer(chunk.xzNoise) && !isTopLayer(chunk.xnNoise) && isTopLayer(chunk.nzNoise) && !isTopLayer(chunk.nnNoise)) ||
                        (!isTopLayer(chunk.xzNoise) && !isTopLayer(chunk.xnNoise) && !isTopLayer(chunk.nzNoise) && isTopLayer(chunk.nnNoise));

        return isSingleCornerTopLevel;
    }

    // Controls amount of generated top layer
    // 0.5f means 50% of chunks will be top layer and 50% holes
    // 0.33f means 33% of chunks will be top layer and 66% holes
    private static final float TOP_LAYER_PERCENTAGE = 0.5f;

    /// @param noise noise in range `-1..1`
    /// @return noise in range `0..1`
    private static float normalizeNoise(float noise) {
        return (noise + 1) / 2;
    }

    private static boolean isTopLayer(float normalizedNoise) {
        return normalizedNoise > TOP_LAYER_PERCENTAGE;
    }

    @Override
    public int getSeaLevel() {
        return 10;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return TOP_LAYER_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(0, new BlockState[]{});
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {

    }
}
