package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.GiantGobletStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;

import java.util.Map;

public class RRStructures {

    public static final ResourceKey<Structure> GIANT_GOBLET = RR.resourceKey(Registries.STRUCTURE, "giant_goblet");

    public static void bootstrap(BootstrapContext<Structure> ctx) {

        var biomes = ctx.lookup(Registries.BIOME);

        ctx.register(RRStructures.GIANT_GOBLET, new GiantGobletStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(RRBiomeTags.HAS_GIANT_GOBLET))
                        .spawnOverrides( // TODO: Override spawn
                                Map.of(
                                        MobCategory.MONSTER,
                                        new StructureSpawnOverride(
                                                StructureSpawnOverride.BoundingBoxType.PIECE, WeightedList.of(new MobSpawnSettings.SpawnerData(EntityTypes.WITCH, 1, 1))
                                        ),
                                        MobCategory.CREATURE,
                                        new StructureSpawnOverride(
                                                StructureSpawnOverride.BoundingBoxType.PIECE, WeightedList.of(new MobSpawnSettings.SpawnerData(EntityTypes.CAT, 1, 1))
                                        )
                                )
                        )
                        .build()
        ));
    }
}
