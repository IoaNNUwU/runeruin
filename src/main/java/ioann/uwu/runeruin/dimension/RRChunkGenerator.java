package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.chunkgenerator.ArcaneStructureGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesAndLostCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.TerrainWriter;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerRelief;
import ioann.uwu.runeruin.dimension.chunkgenerator.VoidGen;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import ioann.uwu.runeruin.dimension.noise.SupportedIslandsNoise;
import ioann.uwu.runeruin.dimension.noise.TopLevelNoise;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_Y;
import static ioann.uwu.runeruin.dimension.Const.WORLD_HEIGHT;

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
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long legacyLevelSeed) {
        // ChunkMap calls this right after creating the level's RandomState, and it is the only place the
        // world seed reaches the generator. Bind it before super: structure placement already asks for biomes.
        LazyNoise.bindSeed(randomState, legacyLevelSeed);
        TopLayerRelief.bind(randomState, getBiomeSource());
        return super.createState(structureSets, randomState, legacyLevelSeed);
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess) {
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
                    random.setLargeFeatureSeed(seed + index, sourcePos.x(), sourcePos.z());
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
    public int getGenDepth() {
        return WORLD_HEIGHT;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {

        return CompletableFuture.supplyAsync(() -> {
            try (TerrainWriter terrain = new TerrainWriter(chunk)) {
                generateTerrain(terrain, randomState);
                ArcaneStructureGen.generateArcaneStructure(terrain, randomState);
                TopLayerAndBloomingCavesGen.generateHangingSoil(terrain, randomState);
                DeepCavesAndLostCavesGen.generateHangingMoss(terrain, randomState);
            }
            return chunk;
        }, Util.backgroundExecutor().forName("wgen_fill_noise"));
    }

    private static void generateTerrain(TerrainWriter terrain, RandomState randomState) {

        TopLayerAndBloomingCavesGen.generateTopLayerFloor(terrain, randomState);
        TopLayerAndBloomingCavesGen.generateBloomingCavesCeiling(terrain, randomState);

        TopLayerAndBloomingCavesGen.generateBloomingCavesFloor(terrain, randomState);

        DeepCavesGen.generateDeepCavesCeiling(terrain, randomState);

        DeepCavesAndLostCavesGen.generateDeepCavesFloor(terrain, randomState);
        DeepCavesAndLostCavesGen.generateLostCavesCeiling(terrain, randomState);
        DeepCavesAndLostCavesGen.generateLostCavesFloor(terrain, randomState);

        VoidGen.generateVoidCeiling(terrain, randomState);
    }

    @Override
    public int getSeaLevel() {
        return BLOOMING_CAVES_CEILING_Y;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    // Islands too small for an arcane column to stand under them would float, so they are not generated.
    // The deep caves floor has no columns but takes the same size limit.
    private static Noise withoutSmallIslands(Noise base) {
        return new SupportedIslandsNoise(base, ArcaneStructureGen::doGenerateColumn);
    }

    /** The islands of the top layer: land above {@link SupportedIslandsNoise#LAND}, rising evenly inland from the edge. */
    public static final LazyNoise baseTopLevelNoise = new LazyNoise(seed -> withoutSmallIslands(Noise.multi(
            new SingleNoise(Noise.hashString("bigNoise1" + seed), 0.5f),
            new SingleNoise(Noise.hashString("bigNoise2" + seed), 0.4f),
            new SingleNoise(Noise.hashString("bigNoise3" + seed), 0.3f)
    )));

    public static final LazyNoise flattenedBaseTopLevelNoise = LazyNoise.chain(baseTopLevelNoise, noise -> Noise.flatten(0.152f, noise));

    public static final LazyNoise topLevelNoise = LazyNoise.ofLevel(level -> new TopLevelNoise(
            "topLevelDetail", level.seed(), baseTopLevelNoise.forLevel(level), flattenedBaseTopLevelNoise.forLevel(level)
    ));

    // The baseline of the top layer islands, of the Blooming caves ceiling, the Lost caves ceiling and the Deep
    // caves floor; its seed strings keep their first name.
    public static final LazyNoise topLevelBaselineNoise = new LazyNoise(
            seed -> Noise.multi(
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise1" + seed), 1f),
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise2" + seed), 0.1f),
                    new SingleNoise(Noise.hashString("lostTopLevelBaselineNoise3" + seed), 0.4f)
            )
    );

    private static final LazyNoise baseLostTopLevelNoise = new LazyNoise(seed -> withoutSmallIslands(Noise.multi(
            new SingleNoise(Noise.hashString("bigLostNoise1" + seed), 0.5f),
            new SingleNoise(Noise.hashString("bigLostNoise2" + seed), 0.4f),
            new SingleNoise(Noise.hashString("bigLostNoise3" + seed), 0.3f)
    )));

    public static final LazyNoise flattenedLostBaseTopLevelNoise = LazyNoise.chain(baseLostTopLevelNoise, noise -> Noise.flatten(0.152f, noise));

    public static final LazyNoise lostTopLevelNoise = LazyNoise.ofLevel(level -> new TopLevelNoise(
            "lostTopLevelDetail", level.seed(), baseLostTopLevelNoise.forLevel(level), flattenedLostBaseTopLevelNoise.forLevel(level)
    ));

    /** The first free block above an island of the top layer, or else above the Blooming caves floor, open to the sky there. */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return TopLayerAndBloomingCavesGen.topLayerSurfaceY(x, z, randomState)
                .orElseGet(() -> TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x + 8, z + 8, randomState)) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return new NoiseColumn(0, new BlockState[]{});
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {

    }
}
