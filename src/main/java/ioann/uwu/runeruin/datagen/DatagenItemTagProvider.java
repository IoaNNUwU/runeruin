package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class DatagenItemTagProvider extends ItemTagsProvider {

    public DatagenItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, RR.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider provider) {
        tag(ItemTags.PLANKS).add(itemKey(RRBlocks.INVERTED_TREE_PLANKS.get()));
        tag(ItemTags.LOGS).add(itemKey(RRBlocks.INVERTED_TREE_WOOD.get()));
        tag(ItemTags.LOGS_THAT_BURN).add(itemKey(RRBlocks.INVERTED_TREE_WOOD.get()));
        tag(ItemTags.LOGS).add(itemKey(RRBlocks.BAOBAB_LOG.get()));
        tag(ItemTags.LOGS).add(itemKey(RRBlocks.BAOBAB_WOOD.get()));
        tag(ItemTags.LOGS_THAT_BURN).add(itemKey(RRBlocks.BAOBAB_LOG.get()));
        tag(ItemTags.LOGS_THAT_BURN).add(itemKey(RRBlocks.BAOBAB_WOOD.get()));
        tag(ItemTags.LEAVES).add(
                itemKey(RRBlocks.INVERTED_LEAVES_1.get()),
                itemKey(RRBlocks.INVERTED_LEAVES_2.get()),
                itemKey(RRBlocks.BAOBAB_LEAVES.get())
        );

        tag(ItemTags.WOODEN_BUTTONS).add(itemKey(RRBlocks.INVERTED_TREE_BUTTON.get()));
        tag(ItemTags.WOODEN_DOORS).add(itemKey(RRBlocks.INVERTED_TREE_DOOR.get()));
        tag(ItemTags.WOODEN_STAIRS).add(itemKey(RRBlocks.INVERTED_TREE_STAIRS.get()));
        tag(ItemTags.WOODEN_SLABS).add(itemKey(RRBlocks.INVERTED_TREE_SLAB.get()));
        tag(ItemTags.WOODEN_FENCES).add(itemKey(RRBlocks.INVERTED_TREE_FENCE.get()));
        tag(ItemTags.FENCE_GATES).add(itemKey(RRBlocks.INVERTED_TREE_FENCE_GATE.get()));
        tag(ItemTags.WOODEN_PRESSURE_PLATES).add(itemKey(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.get()));
        tag(ItemTags.WOODEN_TRAPDOORS).add(itemKey(RRBlocks.INVERTED_TREE_TRAPDOOR.get()));
        tag(ItemTags.SIGNS).add(itemKey(RRBlocks.INVERTED_TREE_SIGN.get()));
        tag(ItemTags.HANGING_SIGNS).add(itemKey(RRBlocks.INVERTED_TREE_HANGING_SIGN.get()));
    }

    private static ResourceKey<Item> itemKey(Block block) {
        return block.asItem().builtInRegistryHolder().key();
    }
}
