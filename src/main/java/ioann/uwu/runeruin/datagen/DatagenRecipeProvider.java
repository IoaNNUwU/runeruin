package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
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
                .save(this.output);

        this.shaped(RecipeCategory.BUILDING_BLOCKS, RRBlocks.DIAMOND_ARCANE_STONE, 2)
                .define('#', RRBlocks.ARCANE_STONE)
                .define('D', Items.DIAMOND)
                .pattern("#D")
                .pattern("D#")
                .unlockedBy(getHasName(RRBlocks.ARCANE_STONE), has(RRBlocks.ARCANE_STONE))
                .save(this.output, "diamond_arcane_stone");

        this.shaped(RecipeCategory.BUILDING_BLOCKS, RRBlocks.DIAMOND_ARCANE_STONE, 2)
                .define('#', RRBlocks.ARCANE_STONE)
                .define('D', Items.DIAMOND)
                .pattern("D#")
                .pattern("#D")
                .unlockedBy(getHasName(RRBlocks.ARCANE_STONE), has(RRBlocks.ARCANE_STONE))
                .save(this.output, "diamond_arcane_stone_mirrored");

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
