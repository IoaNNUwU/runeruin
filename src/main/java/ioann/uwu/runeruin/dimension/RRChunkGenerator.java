package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import ioann.uwu.runeruin.dimension.noise.TopLevelNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
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

    public static final int VOID_HEIGHT = 50;
    public static final int UNDERGROUND_LEVEL_HEIGHT = 75;
    public static final int ARCANE_PLATE_HEIGHT = 5;

    public static final int TOP_LAYER_TERRAIN_HEIGHT = 50;
    public static final int TERRAIN_HEIGHT = 25;
    public static final int TERRAIN_MIN_HEIGHT = 10;
    public static final int CEILING_TERRAIN_HEIGHT = 15;
    public static final int CEILING_TERRAIN_MIN_HEIGHT = 5;

    public static final int CEILING_VOID_Y = VOID_HEIGHT;
    public static final int LOST_CAVES_Y = CEILING_VOID_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int LOST_CAVES_CEILING_Y = LOST_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int DEEP_CAVES_Y = LOST_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int DEEP_CAVES_CEILING_Y = DEEP_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int BLOOMING_CAVES_Y = DEEP_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int BLOOMING_CAVES_CEILING_Y = BLOOMING_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int TOP_LAYER_Y = BLOOMING_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;

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

            generateTerrain(chunk);

            fillArcaneStructure(chunk);
            return chunk;
        });
    }

    private static final Noise undergroundNoise = new SingleNoise("undergroundNoise".hashCode());

    private static final Noise topLevelNoise = new TopLevelNoise();

    private void generateTerrain(ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int[] layerStartingHeights = new int[]{
                        LOST_CAVES_Y, DEEP_CAVES_Y, BLOOMING_CAVES_Y
                };

                for (int i = 0; i < 3; i++) {
                    int xOffset = 4272 * i;
                    int zOffset = 3372 * i;

                    float noise = undergroundNoise.noise(
                            chunk.getPos().getMiddleBlockX() + x + xOffset,
                            chunk.getPos().getMiddleBlockZ() + z + zOffset
                    );

                    int biomeHeight = (int) (TERRAIN_MIN_HEIGHT + noise * (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT));

                    for (int y = layerStartingHeights[i]; y < layerStartingHeights[i] + biomeHeight + 1; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                float noise = topLevelNoise.noise(
                        chunk.getPos().getMiddleBlockX() + x,
                        chunk.getPos().getMiddleBlockZ() + z
                );

                if (Float.isNaN(noise)) {
                    continue;
                }

                int biomeHeight = (int) (noise * (TOP_LAYER_TERRAIN_HEIGHT));

                for (int y = TOP_LAYER_Y; y < TOP_LAYER_Y + biomeHeight - 3; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
                }
                for (int y = TOP_LAYER_Y + biomeHeight - 3; y < TOP_LAYER_Y + biomeHeight; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState());
                }
                chunk.setBlockState(new BlockPos(x, TOP_LAYER_Y + biomeHeight, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

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

            // TODO:
            float xzNoise = topLevelNoise.noise(xzBlock.getX(), xzBlock.getZ());
            float xnNoise = topLevelNoise.noise(xnBlock.getX(), xnBlock.getZ());
            float nzNoise = topLevelNoise.noise(nzBlock.getX(), nzBlock.getZ());
            float nnNoise = topLevelNoise.noise(nnBlock.getX(), nnBlock.getZ());

            return new ChunkVerticesNoise(xzNoise, xnNoise, nzNoise, nnNoise);
        }
    }

    private static boolean isTopLayer(ChunkAccess chunk) {
        return isTopLayer(ChunkVerticesNoise.fromChunk(chunk));
    }

    private static boolean isTopLayer(ChunkVerticesNoise chunk) {

        return true;

        /*
        boolean isAtLeastOneCornerTopLayer = isTopLayer(chunk.xzNoise) || isTopLayer(chunk.xnNoise)
                || isTopLayer(chunk.nzNoise) || isTopLayer(chunk.nnNoise);

        return isAtLeastOneCornerTopLayer;
         */
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
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return (int) (TOP_LAYER_Y + TERRAIN_MIN_HEIGHT + (TERRAIN_HEIGHT - TERRAIN_MIN_HEIGHT) * topLevelNoise.noise(x, z));
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(0, new BlockState[]{});
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {

    }
}
