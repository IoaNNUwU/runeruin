package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class DatagenRecipeProvider extends RecipeProvider {

    protected DatagenRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {

        this.shaped(RecipeCategory.DECORATIONS, RRBlocks.MOSS_LIGHT, 1)
                .define('#', Blocks.COBBLESTONE)
                .define('M', RRItems.MOSS_BERRY)
                .pattern("###")
                .pattern("#M#")
                .pattern("###")
                .unlockedBy(getHasName(RRItems.MOSS_BERRY), has(RRItems.MOSS_BERRY))
                .save(this.output); // , "string"

    }

    public static final class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookupProvider, RecipeOutput output) {
            return new DatagenRecipeProvider(lookupProvider, output);
        }

        @Override
        public String getName() {
            return "RuneRuin recipes";
        }
    }
}
