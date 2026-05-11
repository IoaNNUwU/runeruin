package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.chunkgenerator.*;
import ioann.uwu.runeruin.dimension.noise.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
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
            ArcaneStructureGen.generateArcaneStructure(chunk, randomState);
            return chunk;
        });
    }

    private static final LazyNoise baseTopLevelNoise = new LazyNoise("baseTopLevelNoise", seed -> Noise.multi(
            new SingleNoise(Noise.hashString("bigNoise1" + seed), 0.5f),
            new SingleNoise(Noise.hashString("bigNoise2" + seed), 0.4f),
            new SingleNoise(Noise.hashString("bigNoise3" + seed), 0.3f)
    ));

    public static final LazyNoise flattenedBaseTopLevelNoise = LazyNoise.chain(
            "flattenedBaseTopLevelNoise",
            baseTopLevelNoise,
            noise -> Noise.flatten(0.152f, noise)
    );

    public static final LazyNoise topLevelNoise = LazyNoise.chain(
            "topLevelNoise",
            baseTopLevelNoise,
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

    private static void generateTerrain(ChunkAccess chunk, RandomState randomState) {

        TopLayerAndBloomingCavesGen.generateTopLayerFloor(chunk, randomState);
        TopLayerAndBloomingCavesGen.generateBloomingCavesCeiling(chunk, randomState);

        TopLayerAndBloomingCavesGen.generateBloomingCavesFloor(chunk, randomState);
        DeepCavesGen.generateDeepCavesCeiling(chunk, randomState);

        DeepCavesGen.generateDeepCavesFloor(chunk, randomState);
        LostCavesGen.generateLostCavesCeiling(chunk, randomState);

        LostCavesGen.generateLostCavesFloor(chunk, randomState);
        VoidGen.generateVoidCeiling(chunk, randomState);
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
