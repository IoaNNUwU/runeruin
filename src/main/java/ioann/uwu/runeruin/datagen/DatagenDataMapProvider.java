package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRBiomes;
import ioann.uwu.runeruin.dimension.TerrainProfile;
import ioann.uwu.runeruin.items.RRItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

public class DatagenDataMapProvider extends DataMapProvider {
    public DatagenDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(RRBlocks.ELDEN_LEAF_LITTER.get().asItem().builtInRegistryHolder(), new Compostable(0.3F), false)
                .add(RRBlocks.DEEP_MOSS.get().asItem().builtInRegistryHolder(), new Compostable(0.65F), false)
                .add(RRBlocks.DEEP_MOSS_LAYER.get().asItem().builtInRegistryHolder(), new Compostable(0.3F), false)
                .add(RRItems.MOSSBERRY, new Compostable(0.3F), false)
                .add(RRBlocks.CLOVER.get().asItem().builtInRegistryHolder(), new Compostable(0.3F), false)
                .add(RRBlocks.MOSS_SPROUTS.get().asItem().builtInRegistryHolder(), new Compostable(0.3F), false)
                .add(RRBlocks.SMALL_MOSS_SPROUTS.get().asItem().builtInRegistryHolder(), new Compostable(0.3F), false);

        builder(TerrainProfile.DATA_MAP)
                // The forests had the relief of the islands as they were, TerrainProfile.DEFAULT; island_detail brings it back.
                .add(RRBiomes.ELDEN_GARDEN, new TerrainProfile(10, 10, 5, 0, 0, 0), false)
                .add(RRBiomes.SIMILAR_FOREST, new TerrainProfile(10, 14, 5, 0, 0, 0), false)
                .add(RRBiomes.PLAINS, new TerrainProfile(9, 3, 4, 0, 0, 0), false)
                .add(RRBiomes.HILLS, new TerrainProfile(9, 32, 6, 0, 0, 0), false)
                .add(RRBiomes.SWAMP, new TerrainProfile(11, 0, 4, 0, 13, 0), false)
                .add(RRBiomes.PLATEAU, new TerrainProfile(8, 28, 3, 1, 0, 0), false)
                .add(RRBiomes.DESERT, new TerrainProfile(10, 7, 4, 0, 0, 0), false);
    }
}
