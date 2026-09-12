package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRConfiguredFeatures;
import ioann.uwu.runeruin.dimension.RRTags;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.portal.RuneRuinPortalBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class RRBlocks {

    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(RR.MODID);

    private static final UnaryOperator<BlockBehaviour.Properties> ARCANE_STONE_PROPS = _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE);

    private static final BlockSetType INVERTED_TREE_BLOCK_SET = BlockSetType.register(new BlockSetType("inverted_tree"));
    private static final WoodType INVERTED_TREE_WOOD_TYPE = WoodType.register(new WoodType("inverted_tree", INVERTED_TREE_BLOCK_SET));

    private static final TreeGrower ELDEN_TREE_GROWER = new TreeGrower(
            "elden",
            Optional.of(RRConfiguredFeatures.ELDEN_GIANT_TREE),
            Optional.empty(),
            Optional.empty()
    );

    public static final DeferredBlock<Block> ARCANE_STONE = register("arcane_stone", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> ARCANE_STONE_BRICKS = register("arcane_stone_bricks", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> POLISHED_ARCANE_STONE = register("polished_arcane_stone", ARCANE_STONE_PROPS);
    public static final DeferredBlock<Block> ARCANE_STONE_PILLAR = register("arcane_stone_pillar", ARCANE_STONE_PROPS, RotatedPillarBlock::new);
    public static final DeferredBlock<Block> ARCANE_STONE_COLUMN = register("arcane_stone_column", ARCANE_STONE_PROPS, RotatedPillarBlock::new);
    public static final DeferredBlock<Block> ARCANE_STONE_PORTAL = register("arcane_stone_portal", ARCANE_STONE_PROPS, ArcaneStonePortalBlock::new);

    public static final DeferredBlock<Block> DIAMOND_ARCANE_STONE = register("diamond_arcane_stone", _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK));
    public static final DeferredBlock<Block> LAPIS_LIGHT = register("lapis_light",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PEARLESCENT_FROGLIGHT)
                    .lightLevel(_ -> 6)
    );

    public static final DeferredBlock<Block> FIREFLY_IN_A_JAR = register("firefly_in_a_jar",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(MapColor.COLOR_YELLOW)
                    .noOcclusion()
                    .lightLevel(_ -> 15)
    );

    public static final DeferredBlock<Block> ELDEN_SAPLING = register("elden_sapling",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).mapColor(MapColor.COLOR_YELLOW),
            p -> new SaplingBlock(ELDEN_TREE_GROWER, p));

    public static final DeferredBlock<Block> POTTED_ELDEN_SAPLING = register("potted_elden_sapling",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_CHERRY_SAPLING).mapColor(MapColor.COLOR_YELLOW),
            p -> new FlowerPotBlock(ELDEN_SAPLING.get(), p));

    public static final DeferredBlock<Block> ELDEN_LOG = register("elden_log",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG),
            RotatedPillarBlock::new);

    /** Bark on every side, used for the wide core of giant Elden tree trunks. */
    public static final DeferredBlock<Block> ELDEN_WOOD = register("elden_wood",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD));

    public static final DeferredBlock<Block> ELDEN_PLANKS = register("elden_planks",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    /** Gray pale-oak look; planks recipes, not tagged as logs/wood. */
    public static final DeferredBlock<Block> GIANT_GOBLET_STEM = register("giant_goblet_stem",
            _ -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD));

    /** Bowl / rim of the giant goblet; warped-wart look. */
    public static final DeferredBlock<Block> GIANT_GOBLET_BUD = register("giant_goblet_bud",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_WART_BLOCK));

    /** Nether-roots-like decoration that can only grow on giant goblet buds. */
    public static final DeferredBlock<Block> DEEP_ROOTS = register("deep_roots",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_ROOTS),
            p -> new NetherRootsBlock(RRTags.SUPPORTS_DEEP_ROOTS, p));

    public static final DeferredBlock<Block> ELDEN_LEAVES = register("elden_leaves",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_YELLOW),
            p -> new UntintedParticleLeavesBlock(0.1f, ParticleTypes.CLOUD, p) // TODO: ParticleType
    );

    public static final DeferredBlock<Block> ELDEN_VINES = register("elden_vines",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CAVE_VINES_PLANT)
                    .mapColor(MapColor.COLOR_BROWN)
                    .lightLevel(EldenVinesBlock::getLightLevel)
                    .randomTicks(),
            EldenVinesBlock::new
    );

    /** Temporary inverted-tree leaves: Cherry Leaves for the first variant. */
    public static final DeferredBlock<Block> INVERTED_LEAVES_1 = register("inverted_leaves_1",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES),
            p -> new UntintedParticleLeavesBlock(0.1F, ParticleTypes.CHERRY_LEAVES, p)
    );

    /** Temporary inverted-tree leaves: Pink Glazed Terracotta texture with leaf behavior. */
    public static final DeferredBlock<Block> INVERTED_LEAVES_2 = register("inverted_leaves_2",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES),
            p -> new UntintedParticleLeavesBlock(0.1F, ParticleTypes.CHERRY_LEAVES, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_WOOD = register("inverted_tree_wood",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_WOOD),
            RotatedPillarBlock::new
    );

    public static final DeferredBlock<Block> BAOBAB_LOG = register("baobab_log",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG),
            RotatedPillarBlock::new
    );

    public static final DeferredBlock<Block> BAOBAB_WOOD = register("baobab_wood",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_WOOD)
    );

    public static final DeferredBlock<Block> BAOBAB_LEAVES = register("baobab_leaves",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES),
            p -> new TintedParticleLeavesBlock(0.01F, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_PLANKS = register("inverted_tree_planks",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_PLANKS)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_STAIRS = register("inverted_tree_stairs",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_STAIRS),
            p -> new StairBlock(INVERTED_TREE_PLANKS.get().defaultBlockState(), p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_SLAB = register("inverted_tree_slab",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_SLAB),
            SlabBlock::new
    );

    public static final DeferredBlock<Block> INVERTED_TREE_FENCE = register("inverted_tree_fence",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_FENCE),
            FenceBlock::new
    );

    public static final DeferredBlock<Block> INVERTED_TREE_FENCE_GATE = register("inverted_tree_fence_gate",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_FENCE_GATE),
            p -> new FenceGateBlock(INVERTED_TREE_WOOD_TYPE, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_DOOR = register("inverted_tree_door",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_DOOR),
            p -> new DoorBlock(INVERTED_TREE_BLOCK_SET, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_TRAPDOOR = register("inverted_tree_trapdoor",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_TRAPDOOR),
            p -> new TrapDoorBlock(INVERTED_TREE_BLOCK_SET, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_PRESSURE_PLATE = register("inverted_tree_pressure_plate",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_PRESSURE_PLATE),
            p -> new PressurePlateBlock(INVERTED_TREE_BLOCK_SET, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_BUTTON = register("inverted_tree_button",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_BUTTON),
            p -> new ButtonBlock(INVERTED_TREE_BLOCK_SET, 30, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_SIGN = registerNoItem("inverted_tree_sign",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_SIGN),
            p -> new StandingSignBlock(INVERTED_TREE_WOOD_TYPE, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_WALL_SIGN = registerNoItem("inverted_tree_wall_sign",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_WALL_SIGN),
            p -> new WallSignBlock(INVERTED_TREE_WOOD_TYPE, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_HANGING_SIGN = registerNoItem("inverted_tree_hanging_sign",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_HANGING_SIGN),
            p -> new CeilingHangingSignBlock(INVERTED_TREE_WOOD_TYPE, p)
    );

    public static final DeferredBlock<Block> INVERTED_TREE_WALL_HANGING_SIGN = registerNoItem("inverted_tree_wall_hanging_sign",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_WALL_HANGING_SIGN),
            p -> new WallHangingSignBlock(INVERTED_TREE_WOOD_TYPE, p)
    );

    static {
        RRItems.REGISTRY.registerItem("inverted_tree_sign",
                p -> new SignItem(INVERTED_TREE_SIGN.get(), INVERTED_TREE_WALL_SIGN.get(), p),
                p -> p.stacksTo(16).useBlockDescriptionPrefix()
        );
        RRItems.REGISTRY.registerItem("inverted_tree_hanging_sign",
                p -> new HangingSignItem(INVERTED_TREE_HANGING_SIGN.get(), INVERTED_TREE_WALL_HANGING_SIGN.get(), p),
                p -> p.stacksTo(16).useBlockDescriptionPrefix()
        );
    }

    public static final DeferredBlock<Block> MOSS_LIGHT = register("moss_light",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE)
                    .lightLevel(_ -> 7),
            MossLightBlock::new
    );

    public static final DeferredBlock<Block> GLOWING_MOSS = registerGlowingMoss("glowing_moss", MapColor.COLOR_CYAN);
    public static final DeferredBlock<Block> GLOWING_MOSS_CARPET = registerGlowingMossCarpet("glowing_moss_carpet", MapColor.COLOR_CYAN);

    public static final DeferredBlock<Block> MOSS_BERRY_BUSH = REGISTRY.registerBlock(
            "moss_berry_bush",
            MossBerryBushBlock::new,
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .lightLevel(MossBerryBushBlock::getLightLevel)
    );

    /** One registered block; the 2x2/3x3 shapes are represented by BigLilyPadBlock.PART. */
    public static final DeferredBlock<Block> BIG_LILY_PAD = registerWaterLily("big_lily_pad",
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD),
            BigLilyPadBlock::new
    );

    /** Interior portal block; no BlockItem (like nether portal). */
    public static final DeferredBlock<Block> RUNE_RUIN_PORTAL = REGISTRY.registerBlock(
            "rune_ruin_portal",
            RuneRuinPortalBlock::new,
            _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL)
    );

    private static DeferredBlock<Block> registerGlowingMoss(String name, MapColor color) {
        return register(name,
                _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK)
                        .mapColor(color)
                        .lightLevel(GlowingMossBlock::getLightLevel)
                        .randomTicks(),
                GlowingMossBlock::new);
    }

    private static DeferredBlock<Block> registerGlowingMossCarpet(String name, MapColor color) {
        return register(name,
                _ -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)
                        .mapColor(color)
                        .lightLevel(GlowingMossBlock::getLightLevel)
                        .randomTicks(),
                GlowingMossCarpetBlock::new);
    }

    private static DeferredBlock<Block> registerWaterLily(
            String name,
            UnaryOperator<BlockBehaviour.Properties> props,
            Function<BlockBehaviour.Properties, ? extends Block> block
    ) {
        DeferredBlock<Block> blockRecord = REGISTRY.registerBlock(name, block, props);
        RRItems.REGISTRY.registerItem(name,
                p -> new PlaceOnWaterBlockItem(blockRecord.get(), p),
                p -> p.useBlockDescriptionPrefix()
        );
        return blockRecord;
    }

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

    private static DeferredBlock<Block> registerNoItem(String name, UnaryOperator<BlockBehaviour.Properties> props, Function<BlockBehaviour.Properties, ? extends Block> block) {
        return REGISTRY.registerBlock(name, block, props);
    }
}
