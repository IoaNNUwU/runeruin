package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public class DatagenBlockLootTableProvider extends BlockLootSubProvider {

    protected DatagenBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(RRBlocks.ARCANE_STONE.get());
        dropSelf(RRBlocks.ARCANE_STONE_BRICKS.get());
        dropSelf(RRBlocks.POLISHED_ARCANE_STONE.get());
        dropSelf(RRBlocks.ARCANE_STONE_PILLAR.get());
    }

    @Override
    protected @NonNull Iterable<Block> getKnownBlocks() {
        return RRBlocks.REGISTRY.getEntries().stream().map(Holder::value)::iterator;
    }
}
