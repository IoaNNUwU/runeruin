package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class RRBlocks {

    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(RR.MODID);

    private static final UnaryOperator<BlockBehaviour.Properties> ARCANE_STONE_PROPS = _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE);

    public static final DeferredBlock<Block> ARCANE_STONE = register("arcane_stone", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> ARCANE_STONE_BRICKS = register("arcane_stone_bricks", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> POLISHED_ARCANE_STONE = register("polished_arcane_stone", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> ARCANE_STONE_PILLAR = register("arcane_stone_pillar", ARCANE_STONE_PROPS, RotatedPillarBlock::new);

    private static DeferredBlock<Block> register(String name, UnaryOperator<BlockBehaviour.Properties> props) {
        DeferredBlock<Block> blockRecord = REGISTRY.registerSimpleBlock(name, props);
        RRItems.REGISTRY.registerSimpleBlockItem(name, blockRecord);

        return blockRecord;
    }

    private static DeferredBlock<Block> register(String name, UnaryOperator<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends Block> block) {
        DeferredBlock<Block> blockRecord = REGISTRY.registerBlock(name, block, props);
        RRItems.REGISTRY.registerSimpleBlockItem(name, blockRecord);

        return blockRecord;
    }
}
