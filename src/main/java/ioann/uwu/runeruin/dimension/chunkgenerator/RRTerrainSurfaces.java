package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.GlowingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRBiomes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

public final class RRTerrainSurfaces {

    @FunctionalInterface
    public interface SurfaceResolver {
        BlockState resolve(RandomSource random);
    }

    private record Profile(SurfaceResolver floor, SurfaceResolver ceiling) {}

    private static final SurfaceResolver STONE = fixed(Blocks.STONE);
    private static final SurfaceResolver DEEPSLATE = fixed(Blocks.DEEPSLATE);
    // Not fixed(): the mod's blocks may not be registered yet when this class loads.
    private static final SurfaceResolver MOSS = random -> RRBlocks.DEEP_MOSS.get().defaultBlockState();
    private static final SurfaceResolver PALE_MOSS = fixed(Blocks.PALE_MOSS_BLOCK);
    private static final SurfaceResolver GRASS = fixed(Blocks.GRASS_BLOCK);
    private static final SurfaceResolver GLOWING_MOSS = random ->
            GlowingMossBlock.stateForPlacement(RRBlocks.GLOWING_MOSS.get().defaultBlockState(), random);
    private static final SurfaceResolver VOID_STONE = random -> RRBlocks.VOID_STONE.get().defaultBlockState();

    private static final Profile DEFAULT_PROFILE = new Profile(STONE, MOSS);
    private static final Identifier SURFACE_RANDOM = RR.id("terrain_surface");

    private static final Map<ResourceKey<Biome>, Profile> PROFILES = new HashMap<>();

    static {
        register(RRBiomes.ELDEN_GARDEN, GRASS, MOSS);
        register(RRBiomes.SIMILAR_FOREST, GRASS, MOSS);

        register(RRBiomes.GLOWING_ROOTS, MOSS, MOSS);
        register(RRBiomes.GLOWING_BALLS, MOSS, MOSS);

        register(RRBiomes.JUNGLE_SWAMP, MOSS, MOSS);
        register(RRBiomes.STONE_FOREST, MOSS, MOSS);
        register(RRBiomes.GHOST_GROVE, PALE_MOSS, PALE_MOSS);

        register(RRBiomes.DEEP_ROOTS, STONE, MOSS);
        register(RRBiomes.INVERTED_FOREST, STONE, MOSS);

        register(RRBiomes.DEEP_DRIPSTONE_CAVES, MOSS, STONE);
        register(RRBiomes.GLOWING_MOSS_CAVES, GLOWING_MOSS, STONE);
        register(RRBiomes.STONE_SPIKE_CAVES, MOSS, STONE);
        register(RRBiomes.DEEPSLATE_SPIKE_CAVES, MOSS, STONE);

        register(RRBiomes.SPARKLING_CAVES, DEEPSLATE, DEEPSLATE);
        register(RRBiomes.SPARKLING_CAVES_CEILING, DEEPSLATE, DEEPSLATE);

        register(RRBiomes.VOID, VOID_STONE, VOID_STONE);
    }

    private RRTerrainSurfaces() {}

    private static void register(ResourceKey<Biome> biome, SurfaceResolver floor, SurfaceResolver ceiling) {
        PROFILES.put(biome, new Profile(floor, ceiling));
    }

    private static SurfaceResolver fixed(Block block) {
        BlockState state = block.defaultBlockState();
        return random -> state;
    }

    private static BlockState floor(Holder<Biome> biome, RandomSource random) {
        return profile(biome).floor.resolve(random);
    }

    private static BlockState ceiling(Holder<Biome> biome, RandomSource random) {
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

    public static void placeFloorSurface(TerrainWriter terrain, int x, int y, int z, BlockState state) {
        placeSurface(terrain, x, y, z, state, -1);
    }

    public static void placeCeilingSurface(TerrainWriter terrain, int x, int y, int z, BlockState state) {
        placeSurface(terrain, x, y, z, state, 1);
    }

    private static void placeSurface(TerrainWriter terrain, int x, int y, int z, BlockState state, int inwardDirection) {
        terrain.set(x, y, z, state);
        if (state.is(BlockTags.MOSS_BLOCKS)) {
            terrain.set(x, y + inwardDirection, z, state);
        }
    }

    public static boolean hasProfile(ResourceKey<Biome> biome) {
        return PROFILES.containsKey(biome);
    }

    /** Dirt lies under a grass floor. */
    public static boolean usesGrassySubfloor(Holder<Biome> biome) {
        return profile(biome).floor == GRASS;
    }

    private static Profile profile(Holder<Biome> biome) {
        return biome.unwrapKey().map(PROFILES::get).orElse(DEFAULT_PROFILE);
    }

    private static RandomSource surfaceRandom(RandomState randomState, int x, int y, int z) {
        return randomState.getOrCreateRandomFactory(SURFACE_RANDOM).at(x, y, z);
    }
}
