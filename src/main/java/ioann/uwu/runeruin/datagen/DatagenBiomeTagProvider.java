package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;

import java.util.concurrent.CompletableFuture;

public class DatagenBiomeTagProvider extends BiomeTagsProvider {

    public DatagenBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RR.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        this.tag(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS)
                .add(RRBiomes.STONE_FOREST);

    }
}
