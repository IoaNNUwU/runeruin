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
                RRBlocks.DIAMOND_ARCANE_STONE.getKey(),

                RRBlocks.MOSS_LIGHT.getKey(),
                RRBlocks.LAPIS_LIGHT.getKey()
        );

        // TODO: ELDEN_SAPLING

        tag(BlockTags.MINEABLE_WITH_AXE).add(
                RRBlocks.ELDEN_LOG.getKey(),
                RRBlocks.ELDEN_PLANKS.getKey()
        );

        tag(BlockTags.MINEABLE_WITH_HOE).add(
                RRBlocks.ELDEN_LEAVES.getKey(),
                RRBlocks.MOSS_LIGHT.getKey(),
                RRBlocks.LAPIS_LIGHT.getKey()
        );


        tag(RRTags.VEGETABLES_NON_REPLACEABLE).add(
                RRBlocks.ARCANE_STONE.getKey(),
                BlockItemIds.MOSSY_COBBLESTONE_WALL.block(),
                BlockItemIds.MOSSY_COBBLESTONE_SLAB.block(),
                BlockItemIds.PALE_OAK_WOOD.block()
        );

        tag(BlockTags.PORTALS).add(RRBlocks.RUNE_RUIN_PORTAL.getKey());
    }
}
