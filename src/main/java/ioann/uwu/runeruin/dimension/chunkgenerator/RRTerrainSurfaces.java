package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.GlowingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.HashMap;
import java.util.Map;

public final class RRTerrainSurfaces {

    @FunctionalInterface
    public interface SurfaceResolver {
        BlockState resolve(RandomSource random);
    }

    private record Profile(SurfaceResolver floor, SurfaceResolver ceiling) {}

    private static final SurfaceResolver STONE = fixed(Blocks.STONE);
    private static final SurfaceResolver DEEPSLATE = fixed(Blocks.DEEPSLATE);
    private static final SurfaceResolver MOSS = fixed(Blocks.MOSS_BLOCK);
    private static final SurfaceResolver PALE_MOSS = fixed(Blocks.PALE_MOSS_BLOCK);
    private static final SurfaceResolver GRASS = fixed(Blocks.GRASS_BLOCK);
    private static final SurfaceResolver GLOWING_MOSS = random ->
            GlowingMossBlock.stateForPlacement(RRBlocks.GLOWING_MOSS.get().defaultBlockState(), random);

    private static final SurfaceResolver DEFAULT_FLOOR = STONE;
    private static final SurfaceResolver DEFAULT_CEILING = MOSS;

    private static final Map<ResourceKey<Biome>, Profile> PROFILES = new HashMap<>();

    static {
        register(RRBiomes.ELDEN_GARDEN, GRASS, MOSS);
        register(RRBiomes.SIMILAR_FOREST, GRASS, MOSS);
        register(Biomes.FOREST, GRASS, MOSS);

        register(RRBiomes.GLOWING_ROOTS, MOSS, MOSS);
        register(RRBiomes.GLOWING_BALLS, MOSS, MOSS);

        register(RRBiomes.JUNGLE_SWAMP, MOSS, MOSS);
        register(RRBiomes.STONE_FOREST, MOSS, MOSS);
        register(RRBiomes.GHOST_GROVE, PALE_MOSS, PALE_MOSS);

        register(RRBiomes.DEEP_ROOTS, STONE, MOSS);
        register(RRBiomes.DEEP_INVERTED_FOREST, STONE, MOSS);

        register(RRBiomes.DEEP_DRIPSTONE_CAVES, STONE, STONE);
        register(RRBiomes.GLOWING_MOSS_CAVES, GLOWING_MOSS, STONE);
        register(RRBiomes.STONE_SPIKE_CAVES, STONE, STONE);
        register(RRBiomes.DEEPSLATE_SPIKE_CAVES, STONE, STONE);

        register(RRBiomes.SPARKLING_CAVES, DEEPSLATE, DEEPSLATE);
        register(RRBiomes.SPARKLING_CAVES_CEILING, DEEPSLATE, DEEPSLATE);
    }

    private RRTerrainSurfaces() {}

    private static void register(ResourceKey<Biome> biome, SurfaceResolver floor, SurfaceResolver ceiling) {
        PROFILES.put(biome, new Profile(floor, ceiling));
    }

    private static SurfaceResolver fixed(Block block) {
        BlockState state = block.defaultBlockState();
        return random -> state;
    }

    public static BlockState floor(Holder<Biome> biome, RandomSource random) {
        return profile(biome).floor.resolve(random);
    }

    public static BlockState ceiling(Holder<Biome> biome, RandomSource random) {
        return profile(biome).ceiling.resolve(random);
    }

    public static BlockState floorAt(ChunkAccess chunk, int x, int y, int z, RandomState randomState) {
        return floor(
                chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)),
                surfaceRandom(randomState, x, y, z)
        );
    }

    public static BlockState ceilingAt(ChunkAccess chunk, int x, int y, int z, RandomState randomState) {
        return ceiling(
                chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)),
                surfaceRandom(randomState, x, y, z)
        );
    }

    public static BlockState ceilingFor(Holder<Biome> biome, int x, int y, int z, RandomState randomState) {
        return ceiling(biome, surfaceRandom(randomState, x, y, z));
    }

    public static boolean usesGrassySubfloor(Holder<Biome> biome) {
        return biome.is(RRBiomes.ELDEN_GARDEN)
                || biome.is(RRBiomes.SIMILAR_FOREST)
                || biome.is(Biomes.FOREST);
    }

    private static Profile profile(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(PROFILES::get)
                .orElse(defaultProfile());
    }

    private static Profile defaultProfile() {
        return new Profile(DEFAULT_FLOOR, DEFAULT_CEILING);
    }

    private static RandomSource surfaceRandom(RandomState randomState, int x, int y, int z) {
        return randomState.getOrCreateRandomFactory(RR.id("terrain_surface")).at(x, y, z);
    }
}
