package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.references.BlockItemIds;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class DatagenBlockTagProvider extends BlockTagsProvider {

    public DatagenBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RR.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                RRBlocks.ARCANE_STONE.getKey(),
                RRBlocks.ARCANE_STONE_BRICKS.getKey(),
                RRBlocks.POLISHED_ARCANE_STONE.getKey(),
                RRBlocks.ARCANE_STONE_PILLAR.getKey(),
                RRBlocks.ARCANE_STONE_COLUMN.getKey(),
                RRBlocks.ARCANE_STONE_PORTAL.getKey(),
                RRBlocks.DIAMOND_ARCANE_STONE.getKey(),

                RRBlocks.MOSS_LIGHT.getKey(),
                RRBlocks.LAPIS_LIGHT.getKey(),
                RRBlocks.FIREFLY_IN_A_JAR.getKey()
        );

        // TODO: ELDEN_SAPLING

        tag(BlockTags.MINEABLE_WITH_AXE).add(
                RRBlocks.ELDEN_LOG.getKey(),
                RRBlocks.BAOBAB_LOG.getKey(),
                RRBlocks.BAOBAB_WOOD.getKey(),
                RRBlocks.ELDEN_PLANKS.getKey(),
                RRBlocks.INVERTED_TREE_WOOD.getKey(),
                RRBlocks.INVERTED_TREE_PLANKS.getKey(),
                RRBlocks.INVERTED_TREE_STAIRS.getKey(),
                RRBlocks.INVERTED_TREE_SLAB.getKey(),
                RRBlocks.INVERTED_TREE_FENCE.getKey(),
                RRBlocks.INVERTED_TREE_FENCE_GATE.getKey(),
                RRBlocks.INVERTED_TREE_DOOR.getKey(),
                RRBlocks.INVERTED_TREE_TRAPDOOR.getKey(),
                RRBlocks.INVERTED_TREE_PRESSURE_PLATE.getKey(),
                RRBlocks.INVERTED_TREE_BUTTON.getKey(),
                RRBlocks.INVERTED_TREE_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_WALL_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_HANGING_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.getKey()
        );

        tag(BlockTags.MINEABLE_WITH_HOE).add(
                RRBlocks.ELDEN_LEAVES.getKey(),
                RRBlocks.BAOBAB_LEAVES.getKey(),
                RRBlocks.INVERTED_LEAVES_1.getKey(),
                RRBlocks.INVERTED_LEAVES_2.getKey(),
                RRBlocks.GIANT_GOBLET_STEM.getKey(),
                RRBlocks.GIANT_GOBLET_BUD.getKey(),
                RRBlocks.MOSS_LIGHT.getKey(),
                RRBlocks.GLOWING_MOSS.getKey(),
                RRBlocks.GLOWING_MOSS_CARPET.getKey(),
                RRBlocks.LAPIS_LIGHT.getKey()
        );

        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(
                RRBlocks.DEEP_ROOTS.getKey(),
                RRBlocks.BIG_LILY_PAD.getKey()
        );

        tag(BlockTags.FROG_PREFER_JUMP_TO).add(RRBlocks.BIG_LILY_PAD.getKey());

        tag(BlockTags.LOGS).add(
                RRBlocks.INVERTED_TREE_WOOD.getKey(),
                RRBlocks.BAOBAB_LOG.getKey(),
                RRBlocks.BAOBAB_WOOD.getKey()
        );
        tag(BlockTags.LEAVES).add(
                RRBlocks.INVERTED_LEAVES_1.getKey(),
                RRBlocks.INVERTED_LEAVES_2.getKey(),
                RRBlocks.BAOBAB_LEAVES.getKey()
        );
        tag(BlockTags.PLANKS).add(RRBlocks.INVERTED_TREE_PLANKS.getKey());
        tag(BlockTags.WOODEN_STAIRS).add(RRBlocks.INVERTED_TREE_STAIRS.getKey());
        tag(BlockTags.WOODEN_SLABS).add(RRBlocks.INVERTED_TREE_SLAB.getKey());
        tag(BlockTags.WOODEN_FENCES).add(RRBlocks.INVERTED_TREE_FENCE.getKey());
        tag(BlockTags.FENCES).add(RRBlocks.INVERTED_TREE_FENCE.getKey());
        tag(BlockTags.FENCE_GATES).add(RRBlocks.INVERTED_TREE_FENCE_GATE.getKey());
        tag(BlockTags.WOODEN_DOORS).add(RRBlocks.INVERTED_TREE_DOOR.getKey());
        tag(BlockTags.DOORS).add(RRBlocks.INVERTED_TREE_DOOR.getKey());
        tag(BlockTags.WOODEN_TRAPDOORS).add(RRBlocks.INVERTED_TREE_TRAPDOOR.getKey());
        tag(BlockTags.TRAPDOORS).add(RRBlocks.INVERTED_TREE_TRAPDOOR.getKey());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.getKey());
        tag(BlockTags.PRESSURE_PLATES).add(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.getKey());
        tag(BlockTags.WOODEN_BUTTONS).add(RRBlocks.INVERTED_TREE_BUTTON.getKey());
        tag(BlockTags.BUTTONS).add(RRBlocks.INVERTED_TREE_BUTTON.getKey());
        tag(BlockTags.STANDING_SIGNS).add(RRBlocks.INVERTED_TREE_SIGN.getKey());
        tag(BlockTags.WALL_SIGNS).add(RRBlocks.INVERTED_TREE_WALL_SIGN.getKey());
        tag(BlockTags.SIGNS).add(RRBlocks.INVERTED_TREE_SIGN.getKey(), RRBlocks.INVERTED_TREE_WALL_SIGN.getKey());
        tag(BlockTags.CEILING_HANGING_SIGNS).add(RRBlocks.INVERTED_TREE_HANGING_SIGN.getKey());
        tag(BlockTags.WALL_HANGING_SIGNS).add(RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.getKey());
        tag(BlockTags.ALL_HANGING_SIGNS).add(
                RRBlocks.INVERTED_TREE_HANGING_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.getKey()
        );
        tag(BlockTags.ALL_SIGNS).add(
                RRBlocks.INVERTED_TREE_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_WALL_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_HANGING_SIGN.getKey(),
                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.getKey()
        );

        tag(BlockTags.PLANKS).add(RRBlocks.GIANT_GOBLET_STEM.getKey());

        tag(BlockTags.MOSS_BLOCKS).add(RRBlocks.GLOWING_MOSS.getKey());


        tag(RRTags.VEGETABLES_NON_REPLACEABLE).add(
                RRBlocks.ARCANE_STONE.getKey(),
                BlockItemIds.MOSSY_COBBLESTONE_WALL.block(),
                BlockItemIds.MOSSY_COBBLESTONE_SLAB.block(),
                RRBlocks.GIANT_GOBLET_STEM.getKey(),
                RRBlocks.GIANT_GOBLET_BUD.getKey()
        );

        tag(RRTags.GOBLET_MOSS_REPLACEABLE).add(RRBlocks.GIANT_GOBLET_BUD.getKey());
        tag(RRTags.SUPPORTS_DEEP_ROOTS).add(RRBlocks.GIANT_GOBLET_BUD.getKey());

        tag(BlockTags.PORTALS).add(RRBlocks.RUNE_RUIN_PORTAL.getKey());
    }
}
