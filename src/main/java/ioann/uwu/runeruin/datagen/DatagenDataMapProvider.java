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
                .add(RRBiomes.ELDEN_GARDEN, new TerrainProfile(10, 10, 5, 0, 0, new TerrainProfile.Cliffs(13, 1, 0.5f)), false)
                .add(RRBiomes.SIMILAR_FOREST, new TerrainProfile(10, 14, 5, 0, 0, new TerrainProfile.Cliffs(14, 0.9f, 0.5f)), false)
                // Half of the plains is calm, and what stands on the other half is wider and fewer than in the forests.
                .add(RRBiomes.PLAINS, new TerrainProfile(9, 9, 5, 0, 0.45f, new TerrainProfile.Cliffs(13, 0.35f, 0.5f)), false)
                .add(RRBiomes.HILLS, new TerrainProfile(9, 28, 6, 0, 0, new TerrainProfile.Cliffs(14, 0.6f, 0.52f)), false)
                .add(RRBiomes.SWAMP, new TerrainProfile(11, 10, 5, 0, 0.5f, new TerrainProfile.Cliffs(13, 1, 0.5f)), false)
                // The steps of the plateau are its cliffs: nothing stands on them, so that the land does not rise in layers of walls.
                .add(RRBiomes.PLATEAU, new TerrainProfile(8, 48, 5, 1, 0, TerrainProfile.Cliffs.NONE), false)
                .add(RRBiomes.DESERT, new TerrainProfile(10, 10, 5, 0, 0.5f, new TerrainProfile.Cliffs(13, 1, 0.5f)), false);
    }
}
