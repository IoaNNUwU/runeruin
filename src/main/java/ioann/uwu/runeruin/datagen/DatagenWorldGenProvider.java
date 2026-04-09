package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatagenWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.PLACED_FEATURE, RRPlacedFeatures::bootstrap)
            .add(Registries.CONFIGURED_FEATURE, RRConfiguredFeatures::bootstrap)
            .add(Registries.BIOME, RRBiomes::bootstrap)
            .add(Registries.DIMENSION_TYPE, RRDimension::bootstrapType)
            .add(Registries.LEVEL_STEM, RRDimension::bootstrapStem);


    public DatagenWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(RR.MODID));
    }
}
