package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class DatagenRecipeProvider extends RecipeProvider {

    protected DatagenRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {

        buildInvertedTreeRecipes();

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

    private void buildInvertedTreeRecipes() {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, RRBlocks.INVERTED_TREE_PLANKS, 4)
                .requires(RRBlocks.INVERTED_TREE_WOOD)
                .group("planks")
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_WOOD), has(RRBlocks.INVERTED_TREE_WOOD))
                .save(this.output);

        this.stairBuilder(RRBlocks.INVERTED_TREE_STAIRS, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);
        this.slab(RecipeCategory.BUILDING_BLOCKS, RRBlocks.INVERTED_TREE_SLAB, RRBlocks.INVERTED_TREE_PLANKS);

        this.fenceBuilder(RRBlocks.INVERTED_TREE_FENCE, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);
        this.fenceGateBuilder(RRBlocks.INVERTED_TREE_FENCE_GATE, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);

        this.doorBuilder(RRBlocks.INVERTED_TREE_DOOR, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);
        this.trapdoorBuilder(RRBlocks.INVERTED_TREE_TRAPDOOR, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);
        this.pressurePlate(RRBlocks.INVERTED_TREE_PRESSURE_PLATE, RRBlocks.INVERTED_TREE_PLANKS);

        this.buttonBuilder(RRBlocks.INVERTED_TREE_BUTTON, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);

        this.signBuilder(RRBlocks.INVERTED_TREE_SIGN, Ingredient.of(RRBlocks.INVERTED_TREE_PLANKS))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_PLANKS), has(RRBlocks.INVERTED_TREE_PLANKS))
                .save(this.output);
        this.hangingSignBuilder(RRBlocks.INVERTED_TREE_HANGING_SIGN, Ingredient.of(RRBlocks.INVERTED_TREE_WOOD))
                .unlockedBy(getHasName(RRBlocks.INVERTED_TREE_WOOD), has(RRBlocks.INVERTED_TREE_WOOD))
                .save(this.output);
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
