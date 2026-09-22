package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.blocks.MossBerryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
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
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
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
        dropOther(RRBlocks.ARCANE_STONE_PORTAL.get(), RRBlocks.ARCANE_STONE.get());

        dropSelf(RRBlocks.DIAMOND_ARCANE_STONE.get());
        dropSelf(RRBlocks.ASHEN_MUSHROOM_BLOCK.get());

        dropSelf(RRBlocks.ELDEN_SAPLING.get());
        dropSelf(RRBlocks.ELDEN_LOG.get());
        dropSelf(RRBlocks.ELDEN_WOOD.get());
        dropSelf(RRBlocks.ELDEN_PLANKS.get());
        dropSelf(RRBlocks.ELDEN_STAIRS.get());
        dropSelf(RRBlocks.ELDEN_SLAB.get());
        dropSelf(RRBlocks.ELDEN_FENCE.get());
        dropSelf(RRBlocks.ELDEN_FENCE_GATE.get());
        dropSelf(RRBlocks.ELDEN_DOOR.get());
        dropSelf(RRBlocks.ELDEN_TRAPDOOR.get());
        dropSelf(RRBlocks.ELDEN_PRESSURE_PLATE.get());
        dropSelf(RRBlocks.ELDEN_BUTTON.get());
        dropSelf(RRBlocks.ELDEN_SIGN.get());
        dropSelf(RRBlocks.ELDEN_HANGING_SIGN.get());
        add(RRBlocks.ELDEN_WALL_SIGN.get(), block -> createSingleItemTable(RRBlocks.ELDEN_SIGN.get()));
        add(RRBlocks.ELDEN_WALL_HANGING_SIGN.get(), block -> createSingleItemTable(RRBlocks.ELDEN_HANGING_SIGN.get()));
        add(RRBlocks.ELDEN_LEAF_LITTER.get(), block -> createSegmentedBlockDrops(block));
        dropSelf(RRBlocks.GIANT_GOBLET_STEM.get());
        dropSelf(RRBlocks.GIANT_GOBLET_BUD.get());
        dropSelf(RRBlocks.DEEP_ROOTS.get());

        dropSelf(RRBlocks.ELDEN_LEAVES.get());
        add(RRBlocks.ELDEN_VINES.get(), noDrop());
        dropSelf(RRBlocks.BAOBAB_LOG.get());
        dropSelf(RRBlocks.BAOBAB_WOOD.get());
        dropSelf(RRBlocks.BAOBAB_LEAVES.get());

        dropSelf(RRBlocks.INVERTED_LEAVES_1.get());
        dropSelf(RRBlocks.INVERTED_LEAVES_2.get());
        dropSelf(RRBlocks.INVERTED_TREE_WOOD.get());
        dropSelf(RRBlocks.INVERTED_TREE_PLANKS.get());
        dropSelf(RRBlocks.INVERTED_TREE_STAIRS.get());
        dropSelf(RRBlocks.INVERTED_TREE_SLAB.get());
        dropSelf(RRBlocks.INVERTED_TREE_FENCE.get());
        dropSelf(RRBlocks.INVERTED_TREE_FENCE_GATE.get());
        dropSelf(RRBlocks.INVERTED_TREE_DOOR.get());
        dropSelf(RRBlocks.INVERTED_TREE_TRAPDOOR.get());
        dropSelf(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.get());
        dropSelf(RRBlocks.INVERTED_TREE_BUTTON.get());
        dropSelf(RRBlocks.INVERTED_TREE_SIGN.get());
        dropSelf(RRBlocks.INVERTED_TREE_HANGING_SIGN.get());
        add(RRBlocks.INVERTED_TREE_WALL_SIGN.get(), block -> createSingleItemTable(RRBlocks.INVERTED_TREE_SIGN.get()));
        add(RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.get(), block -> createSingleItemTable(RRBlocks.INVERTED_TREE_HANGING_SIGN.get()));

        dropPottedContents(RRBlocks.POTTED_ELDEN_SAPLING.get());

        dropSelf(RRBlocks.MOSS_LIGHT.get());
        add(RRBlocks.POWDERED_MOSS.get(), noDrop());
        dropSelf(RRBlocks.FIREFLY_IN_A_JAR.get());
        dropSelf(RRBlocks.GLOWING_MOSS.get());
        dropSelf(RRBlocks.GLOWING_MOSS_CARPET.get());
        dropSelf(RRBlocks.FLOATING_MOSS.get());
        add(RRBlocks.GLOWING_MUSHROOM_CAP.get(), this::createGlowingMushroomBlockDrop);
        add(RRBlocks.GLOWING_MUSHROOM_STEM.get(), this::createGlowingMushroomBlockDrop);
        dropSelf(RRBlocks.GLOWING_MUSHROOM.get());
        dropSelf(RRBlocks.LAPIS_LIGHT.get());
        dropSelf(RRBlocks.BIG_LILY_PAD.get());

        add(RRBlocks.RUNE_RUIN_PORTAL.get(), noDrop());

        createMossBerry();

        add(RRBlocks.ELDEN_LEAVES.get(), block -> createLeavesDrops(
                block,
                RRBlocks.ELDEN_SAPLING.get(),
                BlockLootSubProvider.NORMAL_LEAVES_SAPLING_CHANCES
        ));
    }

    private LootTable.Builder createGlowingMushroomBlockDrop(Block original) {
        return createSilkTouchDispatchTable(
                original,
                (LootPoolEntryContainer.Builder<?>) applyExplosionDecay(
                        original,
                        LootItem.lootTableItem(RRBlocks.GLOWING_MUSHROOM.get()).apply(
                                SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(1, 2.0F / 9.0F))
                        )
                )
        );
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
