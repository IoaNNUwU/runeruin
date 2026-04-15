package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.GhostGrove;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.SwampJungle;
import ioann.uwu.runeruin.dimension.biomes.toplayer.EldenGarden;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RRBiomes {

    // --- Top Layer ---
    public static final ResourceKey<Biome> ELDEN_GARDEN = ResourceKey.create(Registries.BIOME, RR.id("elden_garden"));

    // --- Blooming caves Layer ---
    public static final ResourceKey<Biome> JUNGLE_SWAMP = ResourceKey.create(Registries.BIOME, RR.id("jungle_swamp"));
    public static final ResourceKey<Biome> STONE_FOREST = ResourceKey.create(Registries.BIOME, RR.id("stone_forest"));
    public static final ResourceKey<Biome> GHOST_GROVE = ResourceKey.create(Registries.BIOME, RR.id("ghost_grove"));

    public static void bootstrap(BootstrapContext<Biome> ctx) {

        // --- Top Layer ---
        ctx.register(ELDEN_GARDEN, EldenGarden.bootstrap(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));

        // --- Blooming caves Layer ---
        ctx.register(JUNGLE_SWAMP, SwampJungle.bootstrap(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));
        ctx.register(STONE_FOREST, StoneForest.bootstrap(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));
        ctx.register(GHOST_GROVE, GhostGrove.bootstrap(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));
    }
}
