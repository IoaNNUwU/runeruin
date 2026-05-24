package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

public class RRStructureSets {

    public static final ResourceKey<StructureSet> GIANT_GOBLETS = RR.resourceKey(Registries.STRUCTURE_SET, "giant_goblets");

    public static void bootstrap(BootstrapContext<StructureSet> ctx) {
        var structures = ctx.lookup(Registries.STRUCTURE);
        var biomes = ctx.lookup(Registries.BIOME);

        ctx.register(
                GIANT_GOBLETS,
                new StructureSet(
                        structures.getOrThrow(RRStructures.GIANT_GOBLET),
                        new RandomSpreadStructurePlacement(4, 2, RandomSpreadType.LINEAR, 2892828)
                )
        );
    }
}
