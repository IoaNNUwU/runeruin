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


                    // --- Blocks ---
                    RRBlocks.ARCANE_STONE.toStack(),
                    RRBlocks.ARCANE_STONE_BRICKS.toStack(),
                    RRBlocks.ARCANE_STONE_PILLAR.toStack(),
                    RRBlocks.ARCANE_STONE_COLUMN.toStack(),
                    RRBlocks.POLISHED_ARCANE_STONE.toStack(),

                    RRBlocks.ELDEN_SAPLING.toStack(),
                    RRBlocks.ELDEN_LOG.toStack(),
                    RRBlocks.ELDEN_PLANKS.toStack(),
                    RRBlocks.ELDEN_LEAVES.toStack()
            ))).build());
}
