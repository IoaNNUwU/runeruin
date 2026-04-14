package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.MossBerryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.loottables.RRLootTables;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class DatagenLootTableProvider implements LootTableSubProvider {

    public DatagenLootTableProvider(HolderLookup.Provider provider) {

    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
                RRLootTables.HARVEST_MOSS_BERRY,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(RRBlocks.MOSS_BERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(MossBerryBushBlock.AGE, 3)
                                        )
                                )
                                .add(LootItem.lootTableItem(RRItems.MOSS_BERRY))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2f, 4f)))
                        )
                        .withPool(LootPool.lootPool()
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(RRBlocks.MOSS_BERRY_BUSH.get())
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(MossBerryBushBlock.AGE, 2)
                                        )
                                )
                                .add(LootItem.lootTableItem(RRItems.MOSS_BERRY))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1f, 2f)))
                        )
        );
    }
}
