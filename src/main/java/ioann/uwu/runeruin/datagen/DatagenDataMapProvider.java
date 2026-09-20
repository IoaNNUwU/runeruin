package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class DatagenDataMapProvider extends DataMapProvider {
    public DatagenDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(NeoForgeDataMaps.COMPOSTABLES).add(
                RRBlocks.ELDEN_LEAF_LITTER.get().asItem().builtInRegistryHolder(),
                new Compostable(0.3F),
                false
        );
    }
}
