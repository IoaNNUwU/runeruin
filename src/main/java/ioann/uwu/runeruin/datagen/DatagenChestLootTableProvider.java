package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.loottables.RRLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class DatagenChestLootTableProvider implements LootTableSubProvider {

    private final HolderLookup.Provider registries;

    public DatagenChestLootTableProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        // What miners left on the hanging tracks: chests, barrels and chest minecarts.
        output.accept(
                RRLootTables.HANGING_TRACKS_SUPPLY,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3f, 6f))
                                .add(item(RRBlocks.RUNIC_RAIL, 20, 4, 12))
                                .add(item(Items.TORCH, 15, 2, 8))
                                .add(item(Items.COAL, 15, 3, 8))
                                .add(item(Items.BREAD, 15, 1, 3))
                                .add(item(Items.IRON_INGOT, 10, 1, 4))
                                .add(item(Blocks.OAK_PLANKS, 10, 4, 12))
                                .add(item(Blocks.IRON_CHAIN, 8, 2, 6))
                                .add(item(RRItems.WISPBERRY, 8, 2, 5))
                                .add(item(Items.LANTERN, 5, 1, 2))
                                .add(item(Items.MINECART, 5, 1, 1))
                                .add(item(Items.IRON_PICKAXE, 3, 1, 1))
                                .add(item(Items.NAME_TAG, 2, 1, 1))
                        )
        );

        output.accept(
                RRLootTables.HANGING_TRACKS_TREASURE,
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .setRolls(UniformGenerator.between(4f, 7f))
                                .add(item(RRBlocks.RUNIC_RAIL, 15, 16, 32))
                                .add(item(Items.IRON_INGOT, 15, 3, 8))
                                .add(item(Items.GOLD_INGOT, 15, 2, 6))
                                .add(item(Items.EMERALD, 10, 2, 5))
                                .add(item(Items.REDSTONE, 10, 4, 9))
                                .add(item(Items.LAPIS_LAZULI, 10, 4, 9))
                                .add(item(Items.DIAMOND, 5, 1, 3))
                                .add(item(Items.MINECART, 5, 1, 1))
                                .add(item(Items.BOOK, 5, 1, 1)
                                        .apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                                .add(item(Items.GOLDEN_APPLE, 4, 1, 1))
                        )
        );
    }

    private static LootPoolSingletonContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}
