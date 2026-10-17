package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WispberryBushBlock;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.loottables.RRLootTables;
import java.util.function.BiConsumer;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class DatagenLootTableProvider implements LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
                RRLootTables.HARVEST_WISPBERRY,
                LootTable.lootTable()
                        .withPool(wispberryPool(3, 2f, 4f))
                        .withPool(wispberryPool(2, 1f, 2f))
        );
    }

    /** Wispberries from a bush of the given age, for picking and for breaking it. */
    static LootPool.Builder wispberryPool(int age, float min, float max) {
        return LootPool.lootPool()
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(RRBlocks.WISPBERRY_BUSH.get())
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(WispberryBushBlock.AGE, age)
                        )
                )
                .add(LootItem.lootTableItem(RRItems.WISPBERRY))
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}
