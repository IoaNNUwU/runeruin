package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.GhostGrove;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.JungleSwamp;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingBalls;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingRoots;
import ioann.uwu.runeruin.dimension.biomes.deepcaves.DeepDripstoneCaves;
import ioann.uwu.runeruin.dimension.biomes.deepcaves.DeepslateSpikeCaves;
import ioann.uwu.runeruin.dimension.biomes.deepcaves.GlowingMossCaves;
import ioann.uwu.runeruin.dimension.biomes.deepcaves.StoneSpikeCaves;
import ioann.uwu.runeruin.dimension.biomes.deepcavesceiling.DeepRoots;
import ioann.uwu.runeruin.dimension.biomes.deepcavesceiling.InvertedForest;
import ioann.uwu.runeruin.dimension.biomes.lostcaves.SparklingCaves;
import ioann.uwu.runeruin.dimension.biomes.lostcavesceiling.SparklingCavesCeiling;
import ioann.uwu.runeruin.dimension.biomes.toplayer.Desert;
import ioann.uwu.runeruin.dimension.biomes.toplayer.EldenGarden;
import ioann.uwu.runeruin.dimension.biomes.toplayer.Hills;
import ioann.uwu.runeruin.dimension.biomes.toplayer.Plains;
import ioann.uwu.runeruin.dimension.biomes.toplayer.Plateau;
import ioann.uwu.runeruin.dimension.biomes.toplayer.SimilarForest;
import ioann.uwu.runeruin.dimension.biomes.toplayer.Swamp;
import ioann.uwu.runeruin.dimension.biomes.voidlayer.VoidBiome;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class RRBiomes {

    // Top Layer
    public static final ResourceKey<Biome> ELDEN_GARDEN = RR.resourceKey(Registries.BIOME, "elden_garden");
    public static final ResourceKey<Biome> SIMILAR_FOREST = RR.resourceKey(Registries.BIOME, "similar_forest");
    public static final ResourceKey<Biome> PLAINS = RR.resourceKey(Registries.BIOME, "plains");
    public static final ResourceKey<Biome> HILLS = RR.resourceKey(Registries.BIOME, "hills");
    public static final ResourceKey<Biome> SWAMP = RR.resourceKey(Registries.BIOME, "swamp");
    public static final ResourceKey<Biome> PLATEAU = RR.resourceKey(Registries.BIOME, "plateau");
    public static final ResourceKey<Biome> DESERT = RR.resourceKey(Registries.BIOME, "desert");

    // Blooming caves ceiling
    public static final ResourceKey<Biome> GLOWING_ROOTS = RR.resourceKey(Registries.BIOME, "glowing_roots");
    public static final ResourceKey<Biome> GLOWING_BALLS = RR.resourceKey(Registries.BIOME, "glowing_balls");

    // Blooming caves
    public static final ResourceKey<Biome> JUNGLE_SWAMP = RR.resourceKey(Registries.BIOME, "jungle_swamp");
    public static final ResourceKey<Biome> STONE_FOREST = RR.resourceKey(Registries.BIOME, "stone_forest");
    public static final ResourceKey<Biome> GHOST_GROVE = RR.resourceKey(Registries.BIOME, "ghost_grove");

    // Deep caves ceiling
    public static final ResourceKey<Biome> DEEP_ROOTS = RR.resourceKey(Registries.BIOME, "deep_roots");
    public static final ResourceKey<Biome> INVERTED_FOREST = RR.resourceKey(Registries.BIOME, "inverted_forest");

    // Deep caves
    public static final ResourceKey<Biome> DEEP_DRIPSTONE_CAVES = RR.resourceKey(Registries.BIOME, "deep_dripstone_caves");
    public static final ResourceKey<Biome> GLOWING_MOSS_CAVES = RR.resourceKey(Registries.BIOME, "glowing_moss_caves");
    public static final ResourceKey<Biome> STONE_SPIKE_CAVES = RR.resourceKey(Registries.BIOME, "stone_spike_caves");
    public static final ResourceKey<Biome> DEEPSLATE_SPIKE_CAVES = RR.resourceKey(Registries.BIOME, "deepslate_spike_caves");

    // Lost caves ceiling
    public static final ResourceKey<Biome> SPARKLING_CAVES_CEILING = RR.resourceKey(Registries.BIOME, "sparkling_caves_ceiling");

    // Lost caves
    public static final ResourceKey<Biome> SPARKLING_CAVES = RR.resourceKey(Registries.BIOME, "sparkling_caves");

    // Void
    public static final ResourceKey<Biome> VOID = RR.resourceKey(Registries.BIOME, "void");

    public static void bootstrap(BootstrapContext<Biome> ctx) {

        var placedFeatures = ctx.lookup(Registries.PLACED_FEATURE);
        var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);

        // Top Layer
        ctx.register(ELDEN_GARDEN, EldenGarden.bootstrap(placedFeatures, carvers));
        ctx.register(SIMILAR_FOREST, SimilarForest.bootstrap(placedFeatures, carvers));
        ctx.register(PLAINS, Plains.bootstrap(placedFeatures, carvers));
        ctx.register(HILLS, Hills.bootstrap(placedFeatures, carvers));
        ctx.register(SWAMP, Swamp.bootstrap(placedFeatures, carvers));
        ctx.register(PLATEAU, Plateau.bootstrap(placedFeatures, carvers));
        ctx.register(DESERT, Desert.bootstrap(placedFeatures, carvers));

        // Blooming caves ceiling
        ctx.register(GLOWING_ROOTS, GlowingRoots.bootstrap(placedFeatures, carvers));
        ctx.register(GLOWING_BALLS, GlowingBalls.bootstrap(placedFeatures, carvers));

        // Blooming caves
        ctx.register(JUNGLE_SWAMP, JungleSwamp.bootstrap(placedFeatures, carvers));
        ctx.register(STONE_FOREST, StoneForest.bootstrap(placedFeatures, carvers));
        ctx.register(GHOST_GROVE, GhostGrove.bootstrap(placedFeatures, carvers));

        // Deep caves ceiling
        ctx.register(DEEP_ROOTS, DeepRoots.bootstrap(placedFeatures, carvers));
        ctx.register(INVERTED_FOREST, InvertedForest.bootstrap(placedFeatures, carvers));

        // Deep caves
        ctx.register(DEEP_DRIPSTONE_CAVES, DeepDripstoneCaves.bootstrap(placedFeatures, carvers));
        ctx.register(GLOWING_MOSS_CAVES, GlowingMossCaves.bootstrap(placedFeatures, carvers));
        ctx.register(STONE_SPIKE_CAVES, StoneSpikeCaves.bootstrap(placedFeatures, carvers));
        ctx.register(DEEPSLATE_SPIKE_CAVES, DeepslateSpikeCaves.bootstrap(placedFeatures, carvers));

        // Lost caves ceiling
        ctx.register(SPARKLING_CAVES_CEILING, SparklingCavesCeiling.bootstrap(placedFeatures, carvers));

        // Lost caves
        ctx.register(SPARKLING_CAVES, SparklingCaves.bootstrap(placedFeatures, carvers));

        // Void
        ctx.register(VOID, VoidBiome.bootstrap(placedFeatures, carvers));
    }
}
