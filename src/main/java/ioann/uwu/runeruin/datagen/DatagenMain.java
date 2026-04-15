package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRBiomes;
import ioann.uwu.runeruin.dimension.RRConfiguredFeatures;
import ioann.uwu.runeruin.dimension.RRDimension;
import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = RR.MODID)
public class DatagenMain {

    public static final RegistrySetBuilder DATAPACK_REGISTRY_BUILDER = new RegistrySetBuilder()
            .add(Registries.PLACED_FEATURE, RRPlacedFeatures::bootstrap)
            .add(Registries.CONFIGURED_FEATURE, RRConfiguredFeatures::bootstrap)
            .add(Registries.BIOME, RRBiomes::bootstrap)
            .add(Registries.DIMENSION_TYPE, RRDimension::bootstrapType)
            .add(Registries.LEVEL_STEM, RRDimension::bootstrapStem);

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {

        event.createDatapackRegistryObjects(DATAPACK_REGISTRY_BUILDER);

        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        gen.addProvider(true,
                new LootTableProvider(packOutput, Collections.emptySet(),
                    List.of(
                            new LootTableProvider.SubProviderEntry(
                                    DatagenBlockLootTableProvider::new,
                                    LootContextParamSets.BLOCK
                            ),
                            new LootTableProvider.SubProviderEntry(
                                    DatagenLootTableProvider::new,
                                    LootContextParamSets.BLOCK_INTERACT
                            )
                    ), lookupProvider
        ));

        gen.addProvider(true, new DatagenModelProvider(packOutput));

        gen.addProvider(true, new DatagenBlockTagProvider(packOutput, lookupProvider));

        gen.addProvider(true, new DatagenBiomeTagProvider(packOutput, lookupProvider));

        gen.addProvider(true, new DatagenRecipeProvider.Runner(packOutput, lookupProvider));
    }
}
