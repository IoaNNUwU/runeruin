package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.GhostGrove;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.SwampJungle;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingBallsCeilingBiome;
import ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling.GlowingRootsCeilingBiome;
import ioann.uwu.runeruin.dimension.biomes.toplayer.EldenGarden;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;

public class RRBiomes {

    // --- Top Layer ---
    public static final ResourceKey<Biome> ELDEN_GARDEN = ResourceKey.create(Registries.BIOME, RR.id("elden_garden"));

    // --- Blooming caves ceiling ---
    public static final ResourceKey<Biome> GLOWING_ROOTS = ResourceKey.create(Registries.BIOME, RR.id("glowing_roots"));
    public static final ResourceKey<Biome> GLOWING_BALLS = ResourceKey.create(Registries.BIOME, RR.id("glowing_balls"));

    // --- Blooming caves Layer ---
    public static final ResourceKey<Biome> JUNGLE_SWAMP = ResourceKey.create(Registries.BIOME, RR.id("jungle_swamp"));
    public static final ResourceKey<Biome> STONE_FOREST = ResourceKey.create(Registries.BIOME, RR.id("stone_forest"));
    public static final ResourceKey<Biome> GHOST_GROVE = ResourceKey.create(Registries.BIOME, RR.id("ghost_grove"));

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
    }
}
