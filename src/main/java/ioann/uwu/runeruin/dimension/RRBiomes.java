package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.GhostGrove;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.SwampJungle;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingBallsCeilingBiome;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingRootsCeilingBiome;
import ioann.uwu.runeruin.dimension.biomes.deepcavesceiling.DeepRootsCeilingBiome;
import ioann.uwu.runeruin.dimension.biomes.deepcavesceiling.InvertedForest;
import ioann.uwu.runeruin.dimension.biomes.toplayer.EldenGarden;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;

public class RRBiomes {

    // Top Layer
    public static final ResourceKey<Biome> ELDEN_GARDEN = RR.resourceKey(Registries.BIOME, "elden_garden");

    // Blooming caves ceiling
    public static final ResourceKey<Biome> GLOWING_ROOTS = RR.resourceKey(Registries.BIOME, "glowing_roots");
    public static final ResourceKey<Biome> GLOWING_BALLS = RR.resourceKey(Registries.BIOME, "glowing_balls");

    // Blooming caves
    public static final ResourceKey<Biome> JUNGLE_SWAMP = RR.resourceKey(Registries.BIOME, "jungle_swamp");
    public static final ResourceKey<Biome> STONE_FOREST = RR.resourceKey(Registries.BIOME, "stone_forest");
    public static final ResourceKey<Biome> GHOST_GROVE = RR.resourceKey(Registries.BIOME, "ghost_grove");

    // Deep caves ceiling
    public static final ResourceKey<Biome> DEEP_ROOTS = RR.resourceKey(Registries.BIOME, "deep_roots");
    public static final ResourceKey<Biome> DEEP_INVERTED_FOREST = RR.resourceKey(Registries.BIOME, "inverted_forest");

    public static void bootstrap(BootstrapContext<Biome> ctx) {

        var placedFeatures = ctx.lookup(Registries.PLACED_FEATURE);
        var configuredCravers = ctx.lookup(Registries.CONFIGURED_CARVER);

        // --- Top Layer ---
        ctx.register(ELDEN_GARDEN, EldenGarden.bootstrap(placedFeatures, configuredCravers));

        // --- Blooming caves ceiling ---
        ctx.register(GLOWING_ROOTS, GlowingRootsCeilingBiome.bootstrap(placedFeatures, configuredCravers));
        ctx.register(GLOWING_BALLS, GlowingBallsCeilingBiome.bootstrap(placedFeatures, configuredCravers));

        // --- Blooming caves Layer ---
        ctx.register(JUNGLE_SWAMP, SwampJungle.bootstrap(placedFeatures, configuredCravers));
        ctx.register(STONE_FOREST, StoneForest.bootstrap(placedFeatures, configuredCravers));
        ctx.register(GHOST_GROVE, GhostGrove.bootstrap(placedFeatures, configuredCravers));

        // Deep caves ceiling
        ctx.register(DEEP_ROOTS, DeepRootsCeilingBiome.bootstrap(placedFeatures, configuredCravers));
        ctx.register(DEEP_INVERTED_FOREST, InvertedForest.bootstrap(placedFeatures, configuredCravers));
    }
}
