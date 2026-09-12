package ioann.uwu.runeruin.creativetab;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class RRCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RR.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = REGISTRY.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.runeruin")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> RRItems.RUNE_OF_SPACE.get().getDefaultInstance())
            .displayItems((_, output) -> output.acceptAll(List.of(
                    // --- Items ---
                    RRItems.RUNE_OF_SPACE.toStack(),

                    RRItems.MOSS_BERRY.toStack(),
                    RRBlocks.LAPIS_LIGHT.toStack(),

                    // --- Blocks ---
                    RRBlocks.ARCANE_STONE.toStack(),
                    RRBlocks.ARCANE_STONE_BRICKS.toStack(),
                    RRBlocks.ARCANE_STONE_PILLAR.toStack(),
                    RRBlocks.ARCANE_STONE_COLUMN.toStack(),
                    RRBlocks.ARCANE_STONE_PORTAL.toStack(),
                    RRBlocks.POLISHED_ARCANE_STONE.toStack(),

                    RRBlocks.DIAMOND_ARCANE_STONE.toStack(),

                    RRBlocks.MOSS_LIGHT.toStack(),
                    RRBlocks.FIREFLY_IN_A_JAR.toStack(),
                    RRBlocks.GLOWING_MOSS.toStack(),
                    RRBlocks.GLOWING_MOSS_CARPET.toStack(),
                    RRBlocks.BIG_LILY_PAD.toStack(),

                    RRBlocks.ELDEN_SAPLING.toStack(),
                    RRBlocks.ELDEN_LOG.toStack(),
                    RRBlocks.ELDEN_WOOD.toStack(),
                    RRBlocks.ELDEN_PLANKS.toStack(),
                    RRBlocks.GIANT_GOBLET_STEM.toStack(),
                    RRBlocks.GIANT_GOBLET_BUD.toStack(),
                    RRBlocks.DEEP_ROOTS.toStack(),
                    RRBlocks.ELDEN_LEAVES.toStack(),
                    RRBlocks.ELDEN_BERRY_VINE.toStack(),

                    RRBlocks.INVERTED_LEAVES_1.toStack(),
                    RRBlocks.INVERTED_LEAVES_2.toStack(),
                    RRBlocks.INVERTED_TREE_WOOD.toStack(),
                    RRBlocks.INVERTED_TREE_PLANKS.toStack(),
                    RRBlocks.INVERTED_TREE_STAIRS.toStack(),
                    RRBlocks.INVERTED_TREE_SLAB.toStack(),
                    RRBlocks.INVERTED_TREE_FENCE.toStack(),
                    RRBlocks.INVERTED_TREE_FENCE_GATE.toStack(),
                    RRBlocks.INVERTED_TREE_DOOR.toStack(),
                    RRBlocks.INVERTED_TREE_TRAPDOOR.toStack(),
                    RRBlocks.INVERTED_TREE_PRESSURE_PLATE.toStack(),
                    RRBlocks.INVERTED_TREE_BUTTON.toStack(),
                    RRBlocks.INVERTED_TREE_SIGN.toStack(),
                    RRBlocks.INVERTED_TREE_HANGING_SIGN.toStack()
            ))).build());
}
