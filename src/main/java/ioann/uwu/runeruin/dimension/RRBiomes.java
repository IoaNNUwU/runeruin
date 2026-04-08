package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class RRBiomes {

    public static final ResourceKey<Biome> ELDEN_GARDEN = ResourceKey.create(Registries.BIOME, RR.id("elden_garden"));

    public static void bootstrap(BootstrapContext<Biome> ctx) {

        var biome = OverworldBiomes.meadowOrCherryGrove(
                ctx.lookup(Registries.PLACED_FEATURE),
                ctx.lookup(Registries.CONFIGURED_CARVER),
                true
        );

        ctx.register(ELDEN_GARDEN, biome);
    }
}
