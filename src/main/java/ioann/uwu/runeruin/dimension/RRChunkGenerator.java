package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.chunkgenerator.*;
import ioann.uwu.runeruin.dimension.noise.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
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
        if (SharedConstants.DEBUG_DISABLE_CARVERS) {
            return;
        }

        NoiseGeneratorSettings carverSettings = NoiseGeneratorSettings.dummy();
        NoiseBasedChunkGenerator carverGenerator = new NoiseBasedChunkGenerator(getBiomeSource(), Holder.direct(carverSettings));
        ChunkPos pos = chunkAccess.getPos();
        NoiseChunk noiseChunk = NoiseChunk.forChunk(
                chunkAccess,
                randomState,
                Beardifier.forStructuresInChunk(structureManager, pos),
                carverSettings,
                (x, y, z) -> new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState()),
                Blender.of(worldGenRegion)
        );
        CarvingContext context = new CarvingContext(
                carverGenerator,
                worldGenRegion.registryAccess(),
                chunkAccess.getHeightAccessorForGeneration(),
                noiseChunk,
                randomState,
                SurfaceRules.state(Blocks.DIRT.defaultBlockState())
        );
        CarvingMask mask = ((ProtoChunk) chunkAccess).getOrCreateCarvingMask();
        BiomeManager correctBiomeManager = biomeManager.withDifferentSource(
                (quartX, quartY, quartZ) -> getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, randomState.sampler())
        );
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        int carverBiomeY = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT;
        Aquifer aquifer = noiseChunk.aquifer();

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                ChunkPos sourcePos = new ChunkPos(pos.x() + dx, pos.z() + dz);
                ChunkAccess sourceChunk = worldGenRegion.getChunk(sourcePos.x(), sourcePos.z());
                Holder<Biome> topBiome = getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock(sourcePos.getMinBlockX()),
                        QuartPos.fromBlock(carverBiomeY),
                        QuartPos.fromBlock(sourcePos.getMinBlockZ()),
                        randomState.sampler()
                );
                Iterable<Holder<ConfiguredWorldCarver<?>>> carvers = sourceChunk.carverBiome(
                        () -> getBiomeGenerationSettings(topBiome)
                ).getCarvers();
                int index = 0;

                for (Holder<ConfiguredWorldCarver<?>> carverHolder : carvers) {
                    ConfiguredWorldCarver<?> carver = carverHolder.value();
                    random.setLargeFeatureSeed(l + index, sourcePos.x(), sourcePos.z());
                    if (carver.isStartChunk(random)) {
                        carver.carve(context, chunkAccess, correctBiomeManager::getBiome, random, aquifer, sourcePos, mask);
                    }
                    index++;
                }
            }
        }
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
            TopLayerAndBloomingCavesGen.generateHangingSoil(chunk, randomState);
            return chunk;
        });
    }

    private static void generateTerrain(ChunkAccess chunk, RandomState randomState) {

        TopLayerAndBloomingCavesGen.generateTopLayerFloor(chunk, randomState);
        TopLayerAndBloomingCavesGen.generateBloomingCavesCeiling(chunk, randomState);

        TopLayerAndBloomingCavesGen.generateBloomingCavesFloor(chunk, randomState);

        DeepCavesGen.generateDeepCavesCeiling(chunk, randomState);

        DeepCavesAndLostCavesGen.generateDeepCavesFloor(chunk, randomState);
        DeepCavesAndLostCavesGen.generateLostCavesCeiling(chunk, randomState);
        DeepCavesAndLostCavesGen.generateLostCavesFloor(chunk, randomState);

        /*
        DeepCavesGen.generateDeepCavesFloor(chunk, randomState);
        LostCavesGen.generateLostCavesCeiling(chunk, randomState);

        LostCavesGen.generateLostCavesFloor(chunk, randomState);
         */

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

    private static final LazyNoise baseLostTopLevelNoise = new LazyNoise("baseLostTopLevelNoise", seed -> Noise.multi(
            new SingleNoise(Noise.hashString("bigLostNoise1" + seed), 0.5f),
            new SingleNoise(Noise.hashString("bigLostNoise2" + seed), 0.4f),
            new SingleNoise(Noise.hashString("bigLostNoise3" + seed), 0.3f)
    ));

    public static final LazyNoise flattenedLostBaseTopLevelNoise = LazyNoise.chain(
            "flattenedLostBaseTopLevelNoise",
            baseLostTopLevelNoise,
            noise -> Noise.flatten(0.152f, noise)
    );

    public static final LazyNoise lostTopLevelNoise = LazyNoise.chain(
            "topLevelNoise",
            baseLostTopLevelNoise,
            flattenedLostBaseTopLevelNoise,
            TopLevelNoise::new
    );

    public static final LazyNoise lostTopLevelBaselineNoise = new LazyNoise(
            "topLevelBaselineNoise",
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise1" + seed), 1f),
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise2" + seed), 0.1f),
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise3" + seed), 0.4f)
            )
    );

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
