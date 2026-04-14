package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
    public static final DeferredBlock<Block> ARCANE_STONE_COLUMN = register("arcane_stone_column", ARCANE_STONE_PROPS, RotatedPillarBlock::new);

    public static final DeferredBlock<Block> ELDEN_SAPLING = register("elden_sapling",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).mapColor(MapColor.COLOR_YELLOW),
            p -> new SaplingBlock(TreeGrower.CHERRY, p));

    public static final DeferredBlock<Block> POTTED_ELDEN_SAPLING = register("potted_elden_sapling",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_CHERRY_SAPLING).mapColor(MapColor.COLOR_YELLOW),
            p -> new FlowerPotBlock(ELDEN_SAPLING.get(), p));

    public static final DeferredBlock<Block> ELDEN_LOG = register("elden_log",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG),
            RotatedPillarBlock::new);

    public static final DeferredBlock<Block> ELDEN_PLANKS = register("elden_planks",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    public static final DeferredBlock<Block> ELDEN_LEAVES = register("elden_leaves",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_YELLOW),
            p -> new UntintedParticleLeavesBlock(0.1f, ParticleTypes.CLOUD, p) // TODO: ParticleType
    );

    public static final DeferredBlock<Block> MOSS_LIGHT = register("moss_light",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE)
                    .lightLevel(_ -> 7),
            MossLightBlock::new
    );

    public static final DeferredBlock<Block> MOSS_BERRY_BUSH = REGISTRY.registerBlock(
            "moss_berry_bush",
            MossBerryBushBlock::new,
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .lightLevel(MossBerryBushBlock::getLightLevel)
    );

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
