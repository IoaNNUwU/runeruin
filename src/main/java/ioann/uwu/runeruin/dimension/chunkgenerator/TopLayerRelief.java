package ioann.uwu.runeruin.dimension.chunkgenerator;

import com.google.common.collect.MapMaker;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.TerrainProfile;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The land that the biomes of the top layer ask for: their {@link TerrainProfile}s, blended over the biomes within
 * {@link #RADIUS} quarts around, so that the land changes smoothly from one biome to the next.
 */
public final class TopLayerRelief {

    // The terrain only gets the sampler of a level's RandomState, so RRChunkGenerator.createState binds the biome
    // source to it, as LazyNoise does with the seed. Weak keys: the land of a level goes away with it.
    private static final Map<Climate.Sampler, TopLayerRelief> LEVELS = new MapMaker().weakKeys().makeMap();
    private static final int RADIUS = 4;
    // The land of a chunk is kept at the corners of its quarts, 5 x 5 of them, blended from the biomes around them.
    private static final int CORNERS = 5;
    private static final int BIOMES = CORNERS + 2 * RADIUS;
    private static final Identifier FLOOR_SCATTER = RR.id("top_layer_floor_scatter");

    private final RRBiomeSource biomes;
    // The last chunks asked about, in the slot of their low coordinates, as in SupportedIslandsNoise.
    private final AtomicReferenceArray<ChunkLand> chunks = new AtomicReferenceArray<>(64 * 64);

    private TopLayerRelief(RRBiomeSource biomes) {
        this.biomes = biomes;
    }

    public static void bind(RandomState randomState, BiomeSource biomeSource) {
        if (biomeSource instanceof RRBiomeSource biomes) {
            LEVELS.put(randomState.sampler(), new TopLayerRelief(biomes));
        }
    }

    static Land at(int x, int z, Climate.Sampler sampler) {
        return of(sampler).land(x, z, sampler);
    }

    /**
     * The biome whose floor lies on this column: the biome of the block itself with its border scattered by up to
     * 2 blocks, not the 4 x 4 squares in which a chunk keeps its biomes.
     */
    static Holder<Biome> floorBiome(int x, int z, RandomState randomState) {
        RandomSource random = randomState.getOrCreateRandomFactory(FLOOR_SCATTER).at(x, 0, z);
        return of(randomState.sampler()).biomes.topLayerBiome(x + random.nextInt(5) - 2, z + random.nextInt(5) - 2, randomState.sampler());
    }

    private static TopLayerRelief of(Climate.Sampler sampler) {
        TopLayerRelief relief = LEVELS.get(sampler);
        if (relief == null) {
            throw new IllegalStateException("No biome source is bound to this RandomState; call TopLayerRelief.bind first");
        }
        return relief;
    }

    private Land land(int x, int z, Climate.Sampler sampler) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int slot = (chunkZ & 63) << 6 | chunkX & 63;
        ChunkLand chunk = chunks.get(slot);
        if (chunk == null || chunk.chunkX != chunkX || chunk.chunkZ != chunkZ) {
            chunk = new ChunkLand(chunkX, chunkZ, corners(chunkX, chunkZ, sampler));
            chunks.set(slot, chunk);
        }
        int corner = ((x & 15) >> 2) * CORNERS + ((z & 15) >> 2);
        Land[] corners = chunk.corners;
        return Land.lerp((x & 3) / 4f, (z & 3) / 4f, corners[corner], corners[corner + CORNERS], corners[corner + 1], corners[corner + CORNERS + 1]);
    }

    private Land[] corners(int chunkX, int chunkZ, Climate.Sampler sampler) {
        int minQuartX = QuartPos.fromSection(chunkX) - RADIUS;
        int minQuartZ = QuartPos.fromSection(chunkZ) - RADIUS;
        TerrainProfile[] profiles = new TerrainProfile[BIOMES * BIOMES];
        for (int i = 0; i < BIOMES; i++) {
            for (int j = 0; j < BIOMES; j++) {
                profiles[i * BIOMES + j] = TerrainProfile.of(
                        biomes.topLayerBiome(QuartPos.toBlock(minQuartX + i), QuartPos.toBlock(minQuartZ + j), sampler)
                );
            }
        }
        Land[] corners = new Land[CORNERS * CORNERS];
        for (int cornerX = 0; cornerX < CORNERS; cornerX++) {
            for (int cornerZ = 0; cornerZ < CORNERS; cornerZ++) {
                corners[cornerX * CORNERS + cornerZ] = blend(profiles, cornerX + RADIUS, cornerZ + RADIUS);
            }
        }
        return corners;
    }

    /** The profiles around a quart, weighed less the further they are. */
    private static Land blend(TerrainProfile[] profiles, int centerX, int centerZ) {
        float total = 0;
        float height = 0;
        float hills = 0;
        float bumps = 0;
        float plateau = 0;
        float water = 0;
        float wet = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                TerrainProfile profile = profiles[(centerX + dx) * BIOMES + centerZ + dz];
                float weight = (RADIUS + 1 - Math.abs(dx)) * (RADIUS + 1 - Math.abs(dz));
                total += weight;
                height += weight * profile.height();
                hills += weight * profile.hills();
                bumps += weight * profile.bumps();
                plateau += weight * profile.plateau();
                if (profile.water() > 0) {
                    water += weight * profile.water();
                    wet += weight;
                }
            }
        }
        return new Land(height / total, hills / total, bumps / total, plateau / total, wet > 0 ? water / wet : 0, wet / total);
    }

    /**
     * A blended {@link TerrainProfile}: {@code water} is the height of the water of the biomes around that have
     * it, and {@code wet} the share of those biomes, 1 where every biome around has water.
     */
    record Land(float height, float hills, float bumps, float plateau, float water, float wet) {

        static Land lerp(float x, float z, Land x0z0, Land x1z0, Land x0z1, Land x1z1) {
            return new Land(
                    lerp(x, z, x0z0.height, x1z0.height, x0z1.height, x1z1.height),
                    lerp(x, z, x0z0.hills, x1z0.hills, x0z1.hills, x1z1.hills),
                    lerp(x, z, x0z0.bumps, x1z0.bumps, x0z1.bumps, x1z1.bumps),
                    lerp(x, z, x0z0.plateau, x1z0.plateau, x0z1.plateau, x1z1.plateau),
                    lerp(x, z, x0z0.water, x1z0.water, x0z1.water, x1z1.water),
                    lerp(x, z, x0z0.wet, x1z0.wet, x0z1.wet, x1z1.wet)
            );
        }

        private static float lerp(float x, float z, float x0z0, float x1z0, float x0z1, float x1z1) {
            float z0 = x0z0 + x * (x1z0 - x0z0);
            float z1 = x0z1 + x * (x1z1 - x0z1);
            return z0 + z * (z1 - z0);
        }
    }

    private record ChunkLand(int chunkX, int chunkZ, Land[] corners) {}
}
