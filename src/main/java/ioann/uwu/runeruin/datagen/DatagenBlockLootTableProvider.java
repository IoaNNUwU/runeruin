package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.MossBerryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
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
        dropSelf(RRBlocks.ARCANE_STONE_COLUMN.get());

        dropSelf(RRBlocks.DIAMOND_ARCANE_STONE.get());

        dropSelf(RRBlocks.ELDEN_SAPLING.get());
        dropSelf(RRBlocks.ELDEN_LOG.get());
        dropSelf(RRBlocks.ELDEN_PLANKS.get());

        dropSelf(RRBlocks.ELDEN_LEAVES.get());

        dropPottedContents(RRBlocks.POTTED_ELDEN_SAPLING.get());

        dropSelf(RRBlocks.MOSS_LIGHT.get());
        dropSelf(RRBlocks.LAPIS_LIGHT.get());

        createMossBerry();

        // createLeavesDrops(RRBlocks.ELDEN_LEAVES.get(), RRBlocks.ELDEN_SAPLING.get(), BlockLootSubProvider.NORMAL_LEAVES_SAPLING_CHANCES);
    }

    private void createMossBerry() {

        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        add(RRBlocks.MOSS_BERRY_BUSH.get(), block -> applyExplosionDecay(
                block,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(RRBlocks.MOSS_BERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(MossBerryBushBlock.AGE, 3)
                                        )
                                )
                                .add(LootItem.lootTableItem(RRItems.MOSS_BERRY))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2f, 4f)))
                                .apply(ApplyBonusCount.addUniformBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                        )
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(RRBlocks.MOSS_BERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(MossBerryBushBlock.AGE, 2)
                                        )
                                )
                                .add(LootItem.lootTableItem(RRItems.MOSS_BERRY))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 2f)))
                                .apply(ApplyBonusCount.addUniformBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                        )
        ));
    }

    @Override
    protected @NonNull Iterable<Block> getKnownBlocks() {
        return RRBlocks.REGISTRY.getEntries().stream().map(Holder::value)::iterator;
    }
}
