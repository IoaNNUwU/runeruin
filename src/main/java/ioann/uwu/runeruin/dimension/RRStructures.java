package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.GiantGobletStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;

public class RRStructures {

    public static final ResourceKey<Structure> GIANT_GOBLET = RR.resourceKey(Registries.STRUCTURE, "giant_goblet");

    public static void bootstrap(BootstrapContext<Structure> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);

        ctx.register(GIANT_GOBLET, new GiantGobletStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(RRBiomeTags.HAS_GIANT_GOBLET))
                        .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES)
                        .build()
        ));
    }
}
