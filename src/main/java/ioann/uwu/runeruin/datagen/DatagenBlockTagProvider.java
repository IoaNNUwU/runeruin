package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
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
                RRBlocks.ARCANE_STONE.get(),
                RRBlocks.ARCANE_STONE_BRICKS.get(),
                RRBlocks.POLISHED_ARCANE_STONE.get(),
                RRBlocks.ARCANE_STONE_PILLAR.get(),
                RRBlocks.ARCANE_STONE_COLUMN.get(),
                RRBlocks.DIAMOND_ARCANE_STONE.get(),

                RRBlocks.MOSS_LIGHT.get()
        );

        // TODO: ELDEN_SAPLING

        tag(BlockTags.MINEABLE_WITH_AXE).add(
                RRBlocks.ELDEN_LOG.get(),
                RRBlocks.ELDEN_PLANKS.get()
        );

        tag(BlockTags.MINEABLE_WITH_HOE).add(
                RRBlocks.ELDEN_LEAVES.get(),
                RRBlocks.MOSS_LIGHT.get()
        );


        tag(RRTags.VEGETABLES_NON_REPLACEABLE).add(
                RRBlocks.ARCANE_STONE.get(),
                Blocks.MOSSY_COBBLESTONE_WALL,
                Blocks.MOSSY_COBBLESTONE_SLAB,
                Blocks.PALE_OAK_WOOD
        );
    }
}
