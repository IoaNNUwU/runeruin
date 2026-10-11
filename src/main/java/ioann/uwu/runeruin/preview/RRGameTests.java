package ioann.uwu.runeruin.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.DeepMossLayerBlock;
import ioann.uwu.runeruin.blocks.GlowingMushroomBlock;
import ioann.uwu.runeruin.blocks.MossLayerPlantBlock;
import ioann.uwu.runeruin.blocks.MossberryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.CaveMushroomKind;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRBiomes;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.RRFeatures;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesAndLostCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.RRTerrainSurfaces;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.VoidGen;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonGenerator;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonStructure;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.dimension.structures.TemplatePiece;
import ioann.uwu.runeruin.entities.MossyProphet;
import ioann.uwu.runeruin.entities.MossyRam;
import ioann.uwu.runeruin.entities.MossyWarrior;
import ioann.uwu.runeruin.entities.RREntityTypes;
import ioann.uwu.runeruin.entities.RunicMinecart;
import ioann.uwu.runeruin.entities.Snail;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.loottables.RRLootTables;
import ioann.uwu.runeruin.preview.jobs.FeaturePreviewJob;
import ioann.uwu.runeruin.preview.jobs.FeaturePreviewJob.Surface;
import ioann.uwu.runeruin.region.RegionExport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = RR.MODID)
public final class RRGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, RR.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PREVIEW_GIANT_GOBLET =
        TEST_FUNCTIONS.register("preview_giant_goblet", () -> RRGameTests::previewGiantGoblet);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GIANT_GOBLET_WATER_SURFACE =
        TEST_FUNCTIONS.register("giant_goblet_water_surface", () -> RRGameTests::giantGobletWaterSurface);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PREVIEW_HANGING_TRACKS =
        TEST_FUNCTIONS.register("preview_hanging_tracks", () -> RRGameTests::previewHangingTracks);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNIC_RAIL_KEEPS_SPEED =
        TEST_FUNCTIONS.register("runic_rail_keeps_speed", () -> RRGameTests::runicRailKeepsSpeed);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNIC_RAIL_CROSSING =
        TEST_FUNCTIONS.register("runic_rail_crossing", () -> RRGameTests::runicRailCrossing);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RUNIC_MINECART_KEEPS_SPEED =
        TEST_FUNCTIONS.register("runic_minecart_keeps_speed", () -> RRGameTests::runicMinecartKeepsSpeed);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HANGING_TRACKS_STATION_LOOT =
        TEST_FUNCTIONS.register("hanging_tracks_station_loot", () -> RRGameTests::hangingTracksStationLoot);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSY_PROPHET_SUMMONS_WARRIORS =
        TEST_FUNCTIONS.register("mossy_prophet_summons_warriors", () -> RRGameTests::mossyProphetSummonsWarriors);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSY_PROPHET_LEAP =
        TEST_FUNCTIONS.register("mossy_prophet_leap", () -> RRGameTests::mossyProphetLeap);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSY_PROPHET_THROWS_WARRIOR =
        TEST_FUNCTIONS.register("mossy_prophet_throws_warrior", () -> RRGameTests::mossyProphetThrowsWarrior);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSY_WARRIOR_SPIN_JUMP =
        TEST_FUNCTIONS.register("mossy_warrior_spin_jump", () -> RRGameTests::mossyWarriorSpinJump);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHEN_MUSHROOM_SMALL_RADII =
        TEST_FUNCTIONS.register("ashen_mushroom_small_radii", () -> RRGameTests::ashenMushroomSmallRadii);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHEN_MUSHROOM_LARGE_RADII =
        TEST_FUNCTIONS.register("ashen_mushroom_large_radii", () -> RRGameTests::ashenMushroomLargeRadii);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WISPBERRY_ALIASES =
        TEST_FUNCTIONS.register("wispberry_aliases", () -> RRGameTests::wispberryAliases);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GLOWING_MUSHROOM_BONEMEAL =
        TEST_FUNCTIONS.register("glowing_mushroom_bonemeal", () -> RRGameTests::glowingMushroomBonemeal);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSBERRY_BUSH_BONEMEAL =
        TEST_FUNCTIONS.register("mossberry_bush_bonemeal", () -> RRGameTests::mossberryBushBonemeal);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MOSSBERRY_BUSH_HARVEST =
        TEST_FUNCTIONS.register("mossberry_bush_harvest", () -> RRGameTests::mossberryBushHarvest);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLANTS_IN_MOSS_LAYER =
        TEST_FUNCTIONS.register("plants_in_moss_layer", () -> RRGameTests::plantsInMossLayer);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GLOWING_MUSHROOM_PATCH =
        TEST_FUNCTIONS.register("glowing_mushroom_patch", () -> RRGameTests::glowingMushroomPatch);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BIOME_FEATURE_ORDER =
        TEST_FUNCTIONS.register("biome_feature_order", () -> RRGameTests::biomeFeatureOrder);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_SEED_CHANGES_GENERATION =
        TEST_FUNCTIONS.register("world_seed_changes_generation", () -> RRGameTests::worldSeedChangesGeneration);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BIOME_REGISTRY_COMPLETE =
        TEST_FUNCTIONS.register("biome_registry_complete", () -> RRGameTests::biomeRegistryComplete);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAYER_FLOORS_DIFFER =
        TEST_FUNCTIONS.register("layer_floors_differ", () -> RRGameTests::layerFloorsDiffer);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FLOATING_ISLANDS_NOT_GENERATED =
        TEST_FUNCTIONS.register("floating_islands_not_generated", () -> RRGameTests::floatingIslandsNotGenerated);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEEP_CAVES_CEILING =
        TEST_FUNCTIONS.register("deep_caves_ceiling", () -> RRGameTests::deepCavesCeiling);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> VOID_CEILING =
        TEST_FUNCTIONS.register("void_ceiling", () -> RRGameTests::voidCeiling);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BLOOMING_CAVES_CEILING =
        TEST_FUNCTIONS.register("blooming_caves_ceiling", () -> RRGameTests::bloomingCavesCeiling);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DINOSAUR_SKELETON_BURIAL =
        TEST_FUNCTIONS.register("dinosaur_skeleton_burial", () -> RRGameTests::dinosaurSkeletonBurial);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TEMPLATE_PIECE =
        TEST_FUNCTIONS.register("template_piece", () -> RRGameTests::templatePiece);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAVE_COMMAND =
        TEST_FUNCTIONS.register("save_command", () -> RRGameTests::saveCommand);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FEATURE_PLACEMENT =
        TEST_FUNCTIONS.register("feature_placement", () -> RRGameTests::featurePlacement);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PREVIEW_EXPORT_MARKS =
        TEST_FUNCTIONS.register("preview_export_marks", () -> RRGameTests::previewExportMarks);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FEATURE_REACH =
        TEST_FUNCTIONS.register("feature_reach", () -> RRGameTests::featureReach);

    // A feature may write only into the chunk of its origin and the eight around it, and the origin can
    // lie on a chunk edge. A block further from it along X or Z can fall outside: vanilla then logs
    // "Detected setBlock in a far chunk" and drops the block. Features do not check their blocks for
    // this, so their sizes must keep them inside.
    private static final int FEATURE_REACH_LIMIT = 16;
    private static final int FEATURE_REACH_SEEDS = 48;

    private record FeatureCase(String id, Surface surface, Block ground, int offset) {}

    // Every mod configured feature on the surface its placement finds in the world.
    // Built in the tests: mod blocks do not exist yet when the class loads.
    private static List<FeatureCase> featureCases() {
        Block glowingMoss = RRBlocks.GLOWING_MOSS.get();
        Block gobletBud = RRBlocks.GIANT_GOBLET_BUD.get();
        Block deepMoss = RRBlocks.DEEP_MOSS.get();
        Block voidStone = RRBlocks.VOID_STONE.get();
        return List.of(
            new FeatureCase("brown_dome_mushroom", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("trumpet_mushroom", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("red_arch_mushroom", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("pink_twist_mushroom", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("yellow_hat_mushroom", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("small_red_mushroom", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("big_red_mushroom", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("small_brown_mushroom", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("big_brown_mushroom", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("ashen_wall_mushroom", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("ashen_wall_mushroom_upper", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("ceiling_ball", Surface.CEILING, Blocks.STONE, 0),
            new FeatureCase("tuff_moss_boulder", Surface.FLOOR, Blocks.STONE, -2),
            new FeatureCase("mini_volcano", Surface.FLOOR, Blocks.STONE, -2),
            new FeatureCase("monolith", Surface.FLOOR, Blocks.STONE, -2),
            new FeatureCase("glowing_mushroom", Surface.FLOOR, glowingMoss, 1),
            new FeatureCase("goblet_moss_patch", Surface.FLOOR, gobletBud, 1),
            new FeatureCase("goblet_moss_patch_underwater", Surface.UNDERWATER, gobletBud, 1),
            new FeatureCase("water_lily", Surface.WATER, Blocks.STONE, 1),
            new FeatureCase("ashen_wall_mushroom_cluster", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("ashen_wall_mushroom_cluster_upper", Surface.WALL, Blocks.STONE, 0),
            new FeatureCase("powdered_moss", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("moss_hummock", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("moss_vegetation", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("stone_lily", Surface.FLOOR, Blocks.STONE, 1),
            new FeatureCase("deep_roots_grass", Surface.FLOOR, glowingMoss, 1),
            new FeatureCase("goblet_deep_roots", Surface.FLOOR, gobletBud, 1),
            new FeatureCase("moss_sprouts", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("glowing_moss_vegetation", Surface.FLOOR, glowingMoss, 1),
            new FeatureCase("wispberry_bush_patch", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("moss_pool_with_dripleaves", Surface.FLOOR, deepMoss, 1),
            new FeatureCase("small_glowing_mushroom", Surface.FLOOR, glowingMoss, 1),
            new FeatureCase("elden_giant_tree", Surface.FLOOR, Blocks.GRASS_BLOCK, 1),
            new FeatureCase("swamp_jungle_trees", Surface.FLOOR, Blocks.GRASS_BLOCK, 1),
            new FeatureCase("inverted_tree", Surface.CEILING, deepMoss, 0),
            new FeatureCase("long_ceiling_block_vine", Surface.CEILING, deepMoss, 0),
            new FeatureCase("ceiling_vine", Surface.CEILING, deepMoss, -1),
            new FeatureCase("hanging_chorus", Surface.CEILING, voidStone, -1),
            new FeatureCase("void_amethyst", Surface.CEILING, voidStone, -1),
            new FeatureCase("dust_bloom", Surface.CEILING, voidStone, -1),
            new FeatureCase("bead_vine", Surface.CEILING, voidStone, -1),
            new FeatureCase("small_lily_pad_patch", Surface.WATER, Blocks.STONE, 1),
            new FeatureCase("big_lily_pad_patch", Surface.WATER, Blocks.STONE, 1),
            new FeatureCase("floating_moss_patch", Surface.UNDERWATER, Blocks.STONE, 1),
            new FeatureCase("small_floating_moss_patch", Surface.UNDERWATER, Blocks.STONE, 1),
            new FeatureCase("goblet_kelp", Surface.UNDERWATER, Blocks.STONE, 1),
            new FeatureCase("goblet_seagrass", Surface.UNDERWATER, Blocks.STONE, 1),
            new FeatureCase("stone_spike", Surface.CAVE, Blocks.STONE, 0),
            new FeatureCase("dripstone_spike", Surface.CAVE, Blocks.STONE, 0),
            new FeatureCase("deepslate_spike", Surface.CAVE, Blocks.STONE, 0)
        );
    }

    private static void featurePlacement(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> failed = new ArrayList<>();
        for (FeatureCase c : featureCases()) {
            if (FeaturePreviewJob.place(server, RR.id(c.id()), c.surface(), c.ground().defaultBlockState(),
                    c.offset(), 1, 16, 0, 0).changedBlocks() == 0) {
                failed.add(c.id() + " on " + c.surface());
            }
        }
        helper.assertTrue(failed.isEmpty(), "placed nothing: " + String.join(", ", failed));
        helper.succeed();
    }

    // The largest distance along X or Z between a feature's origin and a block it writes, over many seeds.
    // The preview origin is a chunk corner: the worst case for giant spikes, which may be wider elsewhere.
    private static void featureReach(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Set<String> covered = new HashSet<>();
        List<String> measured = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (FeatureCase c : featureCases()) {
            covered.add(c.id());
            int reach = 0;
            // Seeds next to each other start a feature with nearly the same first random number.
            RandomSource seeds = RandomSource.create(1);
            for (int i = 0; i < FEATURE_REACH_SEEDS; i++) {
                FeaturePreviewJob.Placement placement = FeaturePreviewJob.place(server, RR.id(c.id()), c.surface(),
                    c.ground().defaultBlockState(), c.offset(), seeds.nextLong(), 2 * FEATURE_REACH_LIMIT, 0, 0);
                BoundingBox written = placement.world().writtenBox();
                if (written != null) {
                    BlockPos origin = placement.origin();
                    reach = Math.max(reach, Math.max(
                        Math.max(origin.getX() - written.minX(), written.maxX() - origin.getX()),
                        Math.max(origin.getZ() - written.minZ(), written.maxZ() - origin.getZ())));
                }
            }
            measured.add(c.id() + " " + reach);
            if (reach > FEATURE_REACH_LIMIT) {
                problems.add(c.id() + " writes " + reach + " blocks from its origin");
            }
        }
        server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).listElements()
            .filter(feature -> isModKey(feature.key()) && !covered.contains(feature.key().identifier().getPath()))
            .forEach(feature -> problems.add(feature.key().identifier() + " has no row in featureCases"));
        RR.LOGGER.info("Feature reach over {} seeds: {}", FEATURE_REACH_SEEDS, String.join(", ", measured));
        helper.assertTrue(problems.isEmpty(), "the limit is " + FEATURE_REACH_LIMIT + " blocks: " + String.join("; ", problems));
        helper.succeed();
    }

    // scripts/render_preview.py fades what the preview prepared and outlines blocks without collision.
    // Both come from the export: here the pool and its floor are prepared, the kelp is not.
    private static void previewExportMarks(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            PreviewArgs args = new PreviewArgs(RegionExport.resolveExportDir(server), Map.of(
                "id", "runeruin:goblet_kelp", "surface", "underwater", "seeds", "1,5-6", "name", "gametest_export_marks"));
            List<PreviewArgs> runs = args.perSeed();
            helper.assertTrue(runs.stream().map(run -> run.name("")).toList().equals(List.of(
                "gametest_export_marks_s1", "gametest_export_marks_s5", "gametest_export_marks_s6")), "seeds=1,5-6 gave " + runs.size() + " runs");

            JsonObject export = JsonParser.parseString(Files.readString(
                PreviewCatalog.require("feature").run(runs.getFirst(), server).jsonPath())).getAsJsonObject();
            Set<String> noCollision = new HashSet<>();
            export.getAsJsonArray("noCollision").forEach(token -> noCollision.add(token.getAsString()));
            JsonArray layers = export.getAsJsonArray("layers");
            int preparedKelp = 0;
            int kelp = 0;
            int freeGround = 0;
            for (int y = 0; y < layers.size(); y++) {
                for (int z = 0; z < layers.get(y).getAsJsonArray().size(); z++) {
                    String blocks = layers.get(y).getAsJsonArray().get(z).getAsString();
                    String prepared = export.getAsJsonArray("prepared").get(y).getAsJsonArray().get(z).getAsString();
                    for (int x = 0; x < blocks.length(); x++) {
                        String token = blocks.substring(x, x + 1);
                        String state = export.getAsJsonObject("palette").get(token).getAsString();
                        boolean marked = prepared.charAt(x) == '#';
                        if (state.contains("kelp")) {
                            kelp++;
                            preparedKelp += marked ? 1 : 0;
                            helper.assertTrue(noCollision.contains(token), "kelp is exported with a collision shape");
                        } else if (state.equals("minecraft:stone")) {
                            freeGround += marked ? 0 : 1;
                            helper.assertTrue(!noCollision.contains(token), "stone is exported without a collision shape");
                        }
                    }
                }
            }
            helper.assertTrue(kelp > 0 && preparedKelp == 0, preparedKelp + " of " + kelp + " kelp blocks are marked as prepared");
            helper.assertTrue(freeGround == 0, freeGround + " ground blocks are not marked as prepared");
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }
    // Registered biomes that never generate. biome_registry_complete fails if one of them generates.
    private static final Set<ResourceKey<Biome>> PARKED_BIOMES = Set.of(RRBiomes.GHOST_GROVE);
    // Registered placed features that no biome uses for now: big mushrooms grow only in the Deep caves.
    // biome_registry_complete fails if a biome uses one of them.
    private static final Set<ResourceKey<PlacedFeature>> PARKED_FEATURES = Set.of(
        CaveMushroomKind.TRUMPET.placedKey(),
        CaveMushroomKind.PINK_TWIST.placedKey()
    );

    private RRGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(RR.id("preview"));
        event.registerTest(
            RR.id("preview_giant_goblet"),
            new FunctionGameTestInstance(
                PREVIEW_GIANT_GOBLET.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("giant_goblet_water_surface"),
            new FunctionGameTestInstance(
                GIANT_GOBLET_WATER_SURFACE.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("preview_hanging_tracks"),
            new FunctionGameTestInstance(
                PREVIEW_HANGING_TRACKS.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("runic_rail_keeps_speed"),
            new FunctionGameTestInstance(
                RUNIC_RAIL_KEEPS_SPEED.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 80, 0, true)
            )
        );
        event.registerTest(
            RR.id("runic_rail_crossing"),
            new FunctionGameTestInstance(
                RUNIC_RAIL_CROSSING.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 80, 0, true)
            )
        );
        event.registerTest(
            RR.id("runic_minecart_keeps_speed"),
            new FunctionGameTestInstance(
                RUNIC_MINECART_KEEPS_SPEED.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 120, 0, true)
            )
        );
        event.registerTest(
            RR.id("hanging_tracks_station_loot"),
            new FunctionGameTestInstance(
                HANGING_TRACKS_STATION_LOOT.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("ashen_mushroom_small_radii"),
            new FunctionGameTestInstance(
                ASHEN_MUSHROOM_SMALL_RADII.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("ashen_mushroom_large_radii"),
            new FunctionGameTestInstance(
                ASHEN_MUSHROOM_LARGE_RADII.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("wispberry_aliases"),
            new FunctionGameTestInstance(
                WISPBERRY_ALIASES.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("glowing_mushroom_bonemeal"),
            new FunctionGameTestInstance(
                GLOWING_MUSHROOM_BONEMEAL.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("mossberry_bush_bonemeal"),
            new FunctionGameTestInstance(
                MOSSBERRY_BUSH_BONEMEAL.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("mossberry_bush_harvest"),
            new FunctionGameTestInstance(
                MOSSBERRY_BUSH_HARVEST.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("plants_in_moss_layer"),
            new FunctionGameTestInstance(
                PLANTS_IN_MOSS_LAYER.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("glowing_mushroom_patch"),
            new FunctionGameTestInstance(
                GLOWING_MUSHROOM_PATCH.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("biome_feature_order"),
            new FunctionGameTestInstance(
                BIOME_FEATURE_ORDER.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("world_seed_changes_generation"),
            new FunctionGameTestInstance(
                WORLD_SEED_CHANGES_GENERATION.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("biome_registry_complete"),
            new FunctionGameTestInstance(
                BIOME_REGISTRY_COMPLETE.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("layer_floors_differ"),
            new FunctionGameTestInstance(
                LAYER_FLOORS_DIFFER.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("floating_islands_not_generated"),
            new FunctionGameTestInstance(
                FLOATING_ISLANDS_NOT_GENERATED.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("void_ceiling"),
            new FunctionGameTestInstance(
                VOID_CEILING.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("deep_caves_ceiling"),
            new FunctionGameTestInstance(
                DEEP_CAVES_CEILING.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("blooming_caves_ceiling"),
            new FunctionGameTestInstance(
                BLOOMING_CAVES_CEILING.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("dinosaur_skeleton_burial"),
            new FunctionGameTestInstance(
                DINOSAUR_SKELETON_BURIAL.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("template_piece"),
            new FunctionGameTestInstance(
                TEMPLATE_PIECE.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("save_command"),
            new FunctionGameTestInstance(
                SAVE_COMMAND.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("feature_placement"),
            new FunctionGameTestInstance(
                FEATURE_PLACEMENT.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("preview_export_marks"),
            new FunctionGameTestInstance(
                PREVIEW_EXPORT_MARKS.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        event.registerTest(
            RR.id("feature_reach"),
            new FunctionGameTestInstance(
                FEATURE_REACH.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
        // Tests stand in a grid a few blocks apart, in the order they are registered here, and build into each
        // other's space: a test put in the middle moves all later ones, and some of them into each other.
        event.registerTest(
            RR.id("mossy_prophet_summons_warriors"),
            new FunctionGameTestInstance(
                MOSSY_PROPHET_SUMMONS_WARRIORS.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)
            )
        );
        event.registerTest(
            RR.id("mossy_prophet_leap"),
            new FunctionGameTestInstance(
                MOSSY_PROPHET_LEAP.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 160, 0, true)
            )
        );
        event.registerTest(
            RR.id("mossy_prophet_throws_warrior"),
            new FunctionGameTestInstance(
                MOSSY_PROPHET_THROWS_WARRIOR.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)
            )
        );
        event.registerTest(
            RR.id("mossy_warrior_spin_jump"),
            new FunctionGameTestInstance(
                MOSSY_WARRIOR_SPIN_JUMP.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)
            )
        );
    }

    // Vanilla sorts features lazily, when the dimension decorates its first chunk, and a cycle there
    // stops the world from loading. Run the same sort over every biome the dimension can pick.
    private static void biomeFeatureOrder(GameTestHelper helper) {
        try {
            RRBiomeSource biomes = RRBiomeSource.newDefault(helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME));
            FeatureSorter.buildFeaturesPerStep(
                List.copyOf(biomes.possibleBiomes()),
                biome -> biome.value().getGenerationSettings().features(),
                true
            );
            helper.succeed();
        } catch (IllegalStateException e) {
            helper.fail(e.getMessage());
        }
    }

    // Terrain and the biome map must follow the world seed: one seed regenerates the same world,
    // another seed a different one. In-game, ChunkMap hands the seed over through createState.
    private static void worldSeedChangesGeneration(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            BoundingBox column = new BoundingBox(0, 0, 0, 15, 511, 15);
            PreviewWorld seed1 = HeadlessTerrainGenerator.generate(server, 1, column);
            helper.assertTrue(sameBlocks(seed1, HeadlessTerrainGenerator.generate(server, 1, column), column),
                "seed 1 generated different terrain twice");
            helper.assertTrue(!sameBlocks(seed1, HeadlessTerrainGenerator.generate(server, 2, column), column),
                "seeds 1 and 2 generated the same terrain");
            // The moss of floors and ceilings is deep moss; vanilla moss is not generated.
            int deepMoss = 0;
            for (BlockPos pos : BlockPos.betweenClosed(column.minX(), column.minY(), column.minZ(), column.maxX(), column.maxY(), column.maxZ())) {
                helper.assertTrue(!seed1.get(pos).is(Blocks.MOSS_BLOCK), "the terrain has vanilla moss at " + pos.toShortString());
                deepMoss += seed1.get(pos).is(RRBlocks.DEEP_MOSS.get()) ? 1 : 0;
            }
            helper.assertTrue(deepMoss > 0, "the terrain has no deep moss");
            // Plates, hanging terrain and surfaces already followed the seed; the layer shapes did not.
            helper.assertTrue(RRChunkGenerator.topLevelNoise.getOrCreateNoise(HeadlessTerrainGenerator.randomState(server, 1)).noise(100, 100)
                    != RRChunkGenerator.topLevelNoise.getOrCreateNoise(HeadlessTerrainGenerator.randomState(server, 2)).noise(100, 100),
                "the terrain shape noise ignores the world seed");
            helper.assertTrue(!biomeMap(server, 1).equals(biomeMap(server, 2)),
                "seeds 1 and 2 generated the same biome map");

            // The gametest server loads no mod dimension, so replay what ChunkMap does for a level.
            RandomState levelRandom = RandomState.create(
                NoiseGeneratorSettings.dummy(), server.registryAccess().lookupOrThrow(Registries.NOISE), 3);
            new RRChunkGenerator(RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME)))
                .createState(server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), levelRandom, 3);
            RRChunkGenerator.topLevelNoise.getOrCreateNoise(levelRandom);
            helper.succeed();
        } catch (IOException | IllegalStateException e) {
            helper.fail(e.toString());
        }
    }

    // A skeleton picks its height from the floor noise before its chunks exist, so that noise must give the
    // floor the terrain builds. Then 20-50% of its blocks lie in the floor.
    private static void dinosaurSkeletonBurial(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            RandomState randomState = HeadlessTerrainGenerator.randomState(server, 1);
            IntBinaryOperator floorY = (x, z) -> DinosaurSkeletonStructure.floorY(x, z, randomState);
            PreviewWorld terrain = HeadlessTerrainGenerator.generate(server, 1,
                new BoundingBox(0, Const.LOST_CAVES_Y, 0, 15, Const.LOST_CAVES_Y + Const.TERRAIN_HEIGHT + 1, 15));
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = floorY.applyAsInt(x, z);
                    helper.assertTrue(!terrain.get(pos.set(x, y, z)).isAir() && terrain.get(pos.set(x, y + 1, z)).isAir(),
                        "the lost caves floor at " + x + " " + z + " does not end at Y " + y);
                }
            }

            int placed = 0;
            for (int seed = 1; seed <= 32; seed++) {
                Map<BlockPos, BlockState> bones = DinosaurSkeletonGenerator.generate(seed);
                helper.assertTrue(bones.equals(DinosaurSkeletonGenerator.generate(seed)), "seed " + seed + " built two different skeletons");
                OptionalInt y = DinosaurSkeletonStructure.buriedOriginY(bones, seed * 100, 0, floorY, RandomSource.create(seed));
                if (y.isEmpty()) {
                    continue;
                }
                placed++;
                double share = DinosaurSkeletonStructure.buriedShare(bones, new BlockPos(seed * 100, y.getAsInt(), 0), floorY);
                helper.assertTrue(share >= DinosaurSkeletonStructure.MIN_BURIED_SHARE && share <= DinosaurSkeletonStructure.MAX_BURIED_SHARE,
                    "seed " + seed + " has " + Math.round(share * 100.0) + "% of the skeleton in the floor");
            }
            helper.assertTrue(placed >= 24, "only " + placed + " of 32 skeletons found a height with 20-50% in the floor");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    // A piece of a structure that a person built: the folder of its template lists the variants, the piece
    // builds the template turned, and a piece saved with its chunk comes back turned the same way.
    private static void templatePiece(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager templates = level.getStructureManager();
        BlockPos build = helper.absolutePos(new BlockPos(0, 2, 0));
        level.setBlockAndUpdate(build, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(build.east(), Blocks.DIAMOND_BLOCK.defaultBlockState());
        for (String name : List.of("b", "a")) {
            Identifier id = RR.id("test_template_piece/" + name);
            templates.getOrCreate(id).fillFromWorld(level, build, new Vec3i(2, 1, 1), false, List.of(Blocks.STRUCTURE_VOID));
            helper.assertTrue(templates.save(id), "the template " + id + " was not saved");
        }
        List<Identifier> variants = TemplatePiece.variants(templates, "test_template_piece");
        helper.assertTrue(variants.equals(List.of(RR.id("test_template_piece/a"), RR.id("test_template_piece/b"))),
            "the folder lists " + variants);

        BlockPos target = build.above(3);
        TemplatePiece piece = new TemplatePiece(templates, variants.getFirst(), target, Rotation.CLOCKWISE_90, Mirror.NONE);
        TemplatePiece loaded = new TemplatePiece(templates, piece.createTag(StructurePieceSerializationContext.fromLevel(level)));
        helper.assertTrue(loaded.getBoundingBox().equals(piece.getBoundingBox()), "a saved piece came back as " + loaded.getBoundingBox());
        loaded.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
            loaded.getBoundingBox(), ChunkPos.containing(target), target);
        helper.assertTrue(level.getBlockState(target).is(Blocks.GOLD_BLOCK) && level.getBlockState(target.south()).is(Blocks.DIAMOND_BLOCK),
            "the template turned a quarter is not at " + target + " and south of it");
        helper.succeed();
    }

    // The command a person saves a build with: the selection becomes a template file in the resources of the
    // checkout the game was started from, without its structure void block.
    private static void saveCommand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos build = helper.absolutePos(new BlockPos(0, 2, 0));
        level.setBlockAndUpdate(build, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(build.east(), Blocks.STRUCTURE_VOID.defaultBlockState());
        String startedFrom = System.getProperty("runeruin.checkout");
        try {
            Path checkout = Files.createTempDirectory("runeruin_checkout");
            System.setProperty("runeruin.checkout", checkout.toString());
            CommandSourceStack player = ((ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE)).createCommandSourceStack()
                .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
            for (String command : List.of("rrpos1 " + build.toShortString().replace(",", ""),
                "rrpos2 " + build.east().toShortString().replace(",", ""), "rrsave test_save_command/build")) {
                level.getServer().getCommands().performPrefixedCommand(player, command);
            }
            Path file = checkout.resolve("src/main/resources/data/runeruin/structure/test_save_command/build.nbt");
            helper.assertTrue(Files.isRegularFile(file) && Files.size(file) > 0, "/rrsave wrote no " + file);
            StructureTemplate template = level.getStructureManager().get(RR.id("test_save_command/build")).orElseThrow();
            helper.assertTrue(template.getSize().equals(new Vec3i(2, 1, 1)), "the template is " + template.getSize());
            StructurePlaceSettings asBuilt = new StructurePlaceSettings();
            helper.assertTrue(template.filterBlocks(BlockPos.ZERO, asBuilt, Blocks.GOLD_BLOCK).size() == 1
                && template.filterBlocks(BlockPos.ZERO, asBuilt, Blocks.STRUCTURE_VOID).isEmpty(),
                "the template must hold the gold block and no structure void");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        } finally {
            if (startedFrom == null) {
                System.clearProperty("runeruin.checkout");
            } else {
                System.setProperty("runeruin.checkout", startedFrom);
            }
        }
    }

    // The lost and blooming cave floors used one unnamed noise and had the same relief, one layer apart.
    private static void layerFloorsDiffer(GameTestHelper helper) {
        try {
            RandomState randomState = HeadlessTerrainGenerator.randomState(helper.getLevel().getServer(), 1);
            int same = 0;
            for (int i = 0; i < 64; i++) {
                int x = i * 37;
                int z = i * 53;
                if (DeepCavesAndLostCavesGen.lostCavesFloorY(x, z, randomState) - Const.LOST_CAVES_Y
                        == TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x, z, randomState) - Const.BLOOMING_CAVES_Y) {
                    same++;
                }
            }
            helper.assertTrue(same < 32, "lost and blooming cave floors have the same height at " + same + " of 64 points");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    // An island too small for an arcane column floated with nothing under it, in the top layer and in the
    // deep caves floor. Such islands are not generated; a small island that stands on a column stays.
    private static void floatingIslandsNotGenerated(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            long seed = 2133132341L;
            helper.assertTrue(HeadlessTerrainGenerator.generate(server, seed, new BoundingBox(85786, 280, 131858, 85823, 308, 131884)).placedCount() == 0,
                "a top-layer island with no column under it was generated");
            helper.assertTrue(HeadlessTerrainGenerator.generate(server, seed, new BoundingBox(85151, 100, 130716, 85176, 160, 130755)).placedCount() == 0,
                "a small island of the deep caves floor was generated");

            BoundingBox box = new BoundingBox(86800, 260, 135376, 86847, 308, 135423);
            PreviewWorld supported = HeadlessTerrainGenerator.generate(server, seed, box);
            boolean column = false;
            boolean island = false;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                column |= pos.getY() == box.minY() && supported.get(pos).is(RRBlocks.ARCANE_STONE.get());
                island |= supported.get(pos).is(Blocks.GRASS_BLOCK);
            }
            helper.assertTrue(column && island, "a small top-layer island standing on a column was not generated");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    // The ceiling of the Deep caves has levels and ledges, nothing floats under it, it leaves the floor of the
    // Blooming caves closed, and no arcane plate lies under it.
    private static void deepCavesCeiling(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            RandomState randomState = HeadlessTerrainGenerator.randomState(server, 1);
            int size = 96;
            int minY = DeepCavesGen.LOWEST_Y - 1;
            int height = Const.BLOOMING_CAVES_Y + Const.TERRAIN_HEIGHT - minY + 1;
            PreviewWorld terrain = HeadlessTerrainGenerator.generate(server, 1, new BoundingBox(0, minY, 0, size - 1, minY + height - 1, size - 1));

            // 1 is a block, 2 is a block that a path of blocks ties to the floor of the Blooming caves.
            byte[] blocks = new byte[size * size * height];
            int[] tied = new int[blocks.length];
            int count = 0;
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            int ledges = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    int floorY = TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x + 8, z + 8, randomState);
                    int ceilingY = DeepCavesGen.ceilingSurfaceY(x, z, randomState);
                    lowest = Math.min(lowest, ceilingY);
                    highest = Math.max(highest, ceilingY);
                    // What lies on the edge may hang on something outside.
                    boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                    boolean ledge = false;
                    for (int y = minY; y <= floorY; y++) {
                        BlockState state = terrain.get(pos.set(x, y, z));
                        String at = " at " + x + " " + y + " " + z;
                        helper.assertTrue(y < ceilingY == state.isAir() || y > ceilingY, "the ceiling does not start at Y " + ceilingY + at);
                        helper.assertTrue(y < floorY - 6 || !state.isAir(), "the floor of the Blooming caves is open" + at);
                        helper.assertTrue(y < floorY || !state.is(Blocks.STONE), "the floor of the Blooming caves has no surface" + at);
                        helper.assertTrue(y >= floorY - 6 || !state.is(RRBlocks.ARCANE_STONE.get()), "arcane stone under the Blooming caves" + at);
                        ledge |= state.isAir() && y > ceilingY;
                        if (!state.isAir()) {
                            int block = (x * size + z) * height + y - minY;
                            blocks[block] = 1;
                            if (edge || y == floorY) {
                                blocks[block] = 2;
                                tied[count++] = block;
                            }
                        }
                    }
                    if (ledge) {
                        ledges++;
                    }
                }
            }
            int floating = floatingBlocks(blocks, tied, count, size, height);
            helper.assertTrue(floating == 0, floating + " blocks float under the ceiling of the Deep caves");
            helper.assertTrue(highest - lowest >= 15, "the ceiling of the Deep caves only spans Y " + lowest + " to " + highest);
            helper.assertTrue(ledges > 0, "the ceiling of the Deep caves has no ledge");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    // The ceiling of the Void has levels and ledges, nothing floats under it, it is void stone up to the deepslate
    // that closes the floor of the Lost caves, and the air under it lies in the Void biome, whose plants hang from it.
    private static void voidCeiling(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            RandomState randomState = HeadlessTerrainGenerator.randomState(server, 1);
            RRBiomeSource biomes = RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME));
            int size = 96;
            int minY = VoidGen.LOWEST_Y - 1;
            int height = Const.LOST_CAVES_Y + Const.TERRAIN_HEIGHT - minY + 2;
            PreviewWorld terrain = HeadlessTerrainGenerator.generate(server, 1, new BoundingBox(0, minY, 0, size - 1, minY + height - 1, size - 1));

            // 1 is a block, 2 is a block that a path of blocks ties to the floor of the Lost caves.
            byte[] blocks = new byte[size * size * height];
            int[] tied = new int[blocks.length];
            int count = 0;
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            int ledges = 0;
            int reads = 0;
            int misread = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    int floorY = DeepCavesAndLostCavesGen.lostCavesFloorY(x + 8, z + 8, randomState);
                    int ceilingY = VoidGen.ceilingSurfaceY(x, z, randomState);
                    lowest = Math.min(lowest, ceilingY);
                    highest = Math.max(highest, ceilingY);
                    // What lies on the edge may hang on something outside.
                    boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                    boolean ledge = false;
                    for (int y = minY; y <= floorY + 1; y++) {
                        BlockState state = terrain.get(pos.set(x, y, z));
                        String at = " at " + x + " " + y + " " + z;
                        helper.assertTrue(y < ceilingY == state.isAir() || y > ceilingY, "the ceiling does not start at Y " + ceilingY + at);
                        helper.assertTrue(state.isAir() ? y < floorY - 8 || y > floorY
                            : state.is(y <= floorY - 6 ? RRBlocks.VOID_STONE.get() : Blocks.DEEPSLATE),
                            state.getBlock().getName().getString() + " in the ceiling of the Void" + at);
                        ledge |= state.isAir() && y > ceilingY && y < floorY;
                        if (!state.isAir()) {
                            int block = (x * size + z) * height + y - minY;
                            blocks[block] = 1;
                            if (edge || y == floorY) {
                                blocks[block] = 2;
                                tied[count++] = block;
                            }
                            continue;
                        }
                        // A plant takes the biome of its block as the game does, from one of the 8 biomes around it.
                        // Where the floor of the Lost caves is steep, a few of them belong to the other layer.
                        for (int corner = 0; corner < 8; corner++) {
                            Holder<Biome> biome = biomes.getNoiseBiome((x - 2 >> 2) + (corner & 1), (y - 2 >> 2) + (corner >> 1 & 1),
                                (z - 2 >> 2) + (corner >> 2), randomState.sampler());
                            reads++;
                            if (biome.is(RRBiomes.VOID) != y < floorY) {
                                misread++;
                            }
                        }
                    }
                    if (ledge) {
                        ledges++;
                    }
                }
            }
            int floating = floatingBlocks(blocks, tied, count, size, height);
            helper.assertTrue(floating == 0, floating + " blocks float under the ceiling of the Void");
            helper.assertTrue(misread * 10_000L < reads, misread + " of " + reads + " biomes around the air of the Void and of the Lost caves are of the other layer");
            helper.assertTrue(highest - lowest >= 15, "the ceiling of the Void only spans Y " + lowest + " to " + highest);
            helper.assertTrue(ledges > 0, "the ceiling of the Void has no ledge");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    // The ceiling of the Blooming caves has terraces inside the islands and stays even at their edges, nothing
    // floats under it, it leaves the top layer closed, and all of it lies in the ceiling biomes, whose plants hang from it.
    private static void bloomingCavesCeiling(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            long seed = 12345;
            RandomState randomState = HeadlessTerrainGenerator.randomState(server, seed);
            RRBiomeSource biomes = RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME));
            int size = 112;
            int minY = TopLayerAndBloomingCavesGen.CEILING_LOWEST_Y - 3;
            int height = 40;
            PreviewWorld terrain = HeadlessTerrainGenerator.generate(server, seed, new BoundingBox(0, minY, 0, size - 1, minY + height - 1, size - 1));

            // The lowest block of the even ceiling, or 0 between the islands.
            int[] even = new int[size * size];
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    even[x * size + z] = TopLayerAndBloomingCavesGen.evenCeilingY(x, z, randomState).orElse(0);
                }
            }

            // 1 is a block of a terrace, 2 is a block that a path of blocks ties to the even ceiling.
            byte[] blocks = new byte[size * size * height];
            int[] tied = new int[blocks.length];
            int count = 0;
            int deepest = 0;
            int highest = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    int evenY = even[x * size + z];
                    if (evenY == 0) {
                        continue;
                    }
                    boolean atEdge = false;
                    for (int dx = -4; dx <= 4; dx++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            atEdge |= x + dx >= 0 && x + dx < size && z + dz >= 0 && z + dz < size && even[(x + dx) * size + z + dz] == 0;
                        }
                    }
                    int ceilingY = minY;
                    while (terrain.get(pos.set(x, ceilingY, z)).isAir()) {
                        ceilingY++;
                    }
                    // An arcane column passes through the ceiling, and its runes are cut into its sides.
                    if (terrain.get(pos).is(RRBlocks.ARCANE_STONE.get()) || terrain.get(pos).is(RRBlocks.DIAMOND_ARCANE_STONE.get())) {
                        continue;
                    }
                    String at = " at " + x + " " + z;
                    helper.assertTrue(!atEdge || ceilingY == evenY, "the ceiling at the edge of an island is at Y " + ceilingY + ", not " + evenY + at);
                    helper.assertTrue(ceilingY == evenY || ceilingY >= TopLayerAndBloomingCavesGen.CEILING_LOWEST_Y, "a terrace hangs down to Y " + ceilingY + at);
                    helper.assertTrue(terrain.get(pos).is(RRBlocks.DEEP_MOSS.get()), "the ceiling has no moss at Y " + ceilingY + at);
                    helper.assertTrue(!terrain.get(pos.set(x, ceilingY + 1, z)).is(RRBlocks.DEEP_MOSS.get()), "the moss of the ceiling is two blocks thick" + at);
                    Holder<Biome> biome = biomes.getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(ceilingY - 1), QuartPos.fromBlock(z), randomState.sampler());
                    helper.assertTrue(biome.is(RRBiomes.GLOWING_ROOTS) || biome.is(RRBiomes.GLOWING_BALLS),
                        "the air under the ceiling at Y " + ceilingY + at + " is in " + biome.getRegisteredName());
                    deepest = Math.max(deepest, evenY - ceilingY);
                    highest = Math.max(highest, ceilingY - evenY);
                    // Soil hangs over the surface under a cliff, so the highest block of a column is not its surface.
                    int surfaceY = Math.min(TopLayerAndBloomingCavesGen.topLayerSurfaceY(x, z, randomState).orElse(ceilingY), minY + height - 1);
                    for (int y = ceilingY; y <= surfaceY; y++) {
                        helper.assertTrue(y < surfaceY - 8 || !terrain.get(pos.set(x, y, z)).isAir(), "the top layer is open from below at " + x + " " + y + " " + z);
                    }
                    // What lies on the edge may hang on something outside.
                    boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                    for (int y = ceilingY; y < minY + height; y++) {
                        if (!terrain.get(pos.set(x, y, z)).isAir()) {
                            int block = (x * size + z) * height + y - minY;
                            blocks[block] = 1;
                            if (edge || y >= evenY) {
                                blocks[block] = 2;
                                tied[count++] = block;
                            }
                        }
                    }
                }
            }
            int floating = floatingBlocks(blocks, tied, count, size, height);
            helper.assertTrue(floating == 0, floating + " blocks float under the ceiling of the Blooming caves");
            helper.assertTrue(deepest >= 4 && highest >= 3, "the terraces hang only " + deepest + " blocks under the even ceiling and rise " + highest + " above it");
            helper.succeed();
        } catch (IOException e) {
            helper.fail(e.toString());
        }
    }

    /** Spreads from the tied blocks (2) through their neighbours and counts the blocks (1) that nothing reached. */
    private static int floatingBlocks(byte[] blocks, int[] tied, int count, int size, int height) {
        int[] steps = {1, height, height * size};
        int[] limits = {height, size, size};
        while (count > 0) {
            int block = tied[--count];
            int[] place = {block % height, block / height % size, block / height / size};
            for (int axis = 0; axis < 3; axis++) {
                for (int side = -1; side <= 1; side += 2) {
                    int next = block + side * steps[axis];
                    if (place[axis] + side >= 0 && place[axis] + side < limits[axis] && blocks[next] == 1) {
                        blocks[next] = 2;
                        tied[count++] = next;
                    }
                }
            }
        }
        int floating = 0;
        for (byte block : blocks) {
            if (block == 1) {
                floating++;
            }
        }
        return floating;
    }

    private static boolean sameBlocks(PreviewWorld a, PreviewWorld b, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    if (a.get(pos.set(x, y, z)) != b.get(pos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static List<Holder<Biome>> biomeMap(MinecraftServer server, long seed) throws IOException {
        RRBiomeSource biomes = RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME));
        Climate.Sampler sampler = HeadlessTerrainGenerator.randomState(server, seed).sampler();
        List<Holder<Biome>> map = new ArrayList<>();
        for (int y = 0; y < 512; y += 16) {
            for (int z = -512; z < 512; z += 32) {
                for (int x = -512; x < 512; x += 32) {
                    map.add(biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler));
                }
            }
        }
        return map;
    }

    // Every mod biome generates or is parked, has its own terrain surface and filters its features
    // by biome; every mod placed feature is used by some mod biome, directly or nested in another, or is parked.
    private static void biomeRegistryComplete(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Registry<Biome> biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        Set<Holder<Biome>> generated = RRBiomeSource.newDefault(biomeRegistry).possibleBiomes();
        Set<ConfiguredFeature<?, ?>> usedFeatures = new HashSet<>();
        List<String> problems = new ArrayList<>();
        biomeRegistry.listElements().filter(biome -> isModKey(biome.key())).forEach(biome -> {
            Identifier id = biome.key().identifier();
            boolean parked = PARKED_BIOMES.contains(biome.key());
            if (generated.contains(biome) == parked) {
                problems.add(id + (parked ? " is parked but generates" : " never generates; add it to RRBiomeSource or PARKED_BIOMES"));
            }
            if (!RRTerrainSurfaces.hasProfile(biome.key())) {
                problems.add(id + " has no RRTerrainSurfaces entry");
            }
            biome.value().getGenerationSettings().features().forEach(step -> step.forEach(placed -> {
                placed.value().getFeatures().forEach(configured -> usedFeatures.add(configured.value()));
                // Datagen only logs this; without the filter the feature also decorates neighbouring biomes.
                if (!placed.value().placement().contains(BiomeFilter.biome())) {
                    problems.add(placed.getRegisteredName() + " in " + id + " is missing BiomeFilter.biome()");
                }
            }));
        });
        registries.lookupOrThrow(Registries.PLACED_FEATURE).listElements()
            .filter(placed -> isModKey(placed.key()))
            .forEach(placed -> {
                boolean parked = PARKED_FEATURES.contains(placed.key());
                if (usedFeatures.contains(placed.value().feature().value()) == parked) {
                    problems.add(placed.key().identifier() + (parked ? " is parked but used by a biome" : " is not used by any biome; attach it to a biome or add it to PARKED_FEATURES"));
                }
            });
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static boolean isModKey(ResourceKey<?> key) {
        return key.identifier().getNamespace().equals(RR.MODID);
    }

    private static void previewGiantGoblet(GameTestHelper helper) {
        try {
            PreviewArgs args = new PreviewArgs(RegionExport.resolveExportDir(helper.getLevel().getServer()), java.util.Map.of())
                .with("seed", "1");
            PreviewJobs.Result result = PreviewCatalog.require("giant_goblet").run(args);
            helper.assertTrue(result.count("runeruin:giant_goblet_stem") > 100, "giant goblet placed too few stem blocks");
            helper.assertTrue(result.count("runeruin:giant_goblet_bud") > 50, "giant goblet placed too few bud blocks");
            helper.assertTrue(result.count("minecraft:water") > 100, "giant goblet placed too little water");
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }

    // A raft of floating moss reaches into a chunk where the goblet is not built yet and asks the goblet where
    // its water will be. The answer must be the water that the goblet builds, and the goblet, built chunk by
    // chunk afterwards, must keep the moss and change nothing else.
    private static void giantGobletWaterSurface(GameTestHelper helper) {
        BlockState moss = RRBlocks.FLOATING_MOSS.get().defaultBlockState();
        int[][] sizes = {{61, 20}, {81, 30}, {81, 40}, {101, 60}};
        for (int seed = 1; seed <= sizes.length; seed++) {
            int[] size = sizes[seed - 1];
            GiantGobletPiece goblet = new GiantGobletPiece(8, 8, size[0], size[1], seed);
            BoundingBox box = goblet.getBoundingBox();
            PreviewWorld built = PreviewWorld.create(seed);
            PreviewJobs.placePiece(goblet, built);

            PreviewWorld mossFirst = PreviewWorld.create(seed);
            int surface = 0;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                boolean open = built.get(pos).is(Blocks.WATER) && built.get(pos.above()).isAir();
                helper.assertTrue(goblet.isWaterSurface(pos) == open, "goblet " + seed + " at " + pos.toShortString()
                    + (open ? " has open water that it does not foresee" : " foresees open water that it does not build"));
                if (open) {
                    mossFirst.set(pos, moss);
                    surface++;
                }
            }
            helper.assertTrue(surface > 100, "goblet " + seed + " has only " + surface + " blocks of open water");

            PreviewJobs.placePieceAcrossChunks(goblet, mossFirst);
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                BlockState expected = goblet.isWaterSurface(pos) ? moss : built.get(pos);
                helper.assertTrue(mossFirst.get(pos) == expected, "goblet " + seed + " built around moss has "
                    + mossFirst.get(pos).getBlock().getName().getString() + " at " + pos.toShortString());
            }
        }
        helper.succeed();
    }

    // The preview job itself fails when no network fits or when the rails of a network break off anywhere.
    private static void previewHangingTracks(GameTestHelper helper) {
        try {
            MinecraftServer server = helper.getLevel().getServer();
            int fitted = 0;
            for (int seed = 1; seed <= 8; seed++) {
                PreviewArgs args = new PreviewArgs(RegionExport.resolveExportDir(server), java.util.Map.of())
                    .with("seed", Integer.toString(seed));
                PreviewJobs.Result result = PreviewCatalog.require("hanging_tracks").run(args);
                helper.assertTrue(result.count("minecraft:iron_chain") > 100, "hanging tracks " + seed + " hang on too few chains");
                // Under the ceiling and above the floor that the Deep caves have with this seed.
                // Between the steps of the ceiling a station sometimes has no room.
                try {
                    PreviewCatalog.require("hanging_tracks").run(args.with("terrain", "real"), server);
                    fitted++;
                } catch (IllegalStateException e) {
                    helper.assertTrue(e.getMessage().startsWith("No network fits"), e.getMessage());
                }
            }
            helper.assertTrue(fitted >= 6, "hanging tracks fit the Deep caves with only " + fitted + " of 8 seeds");
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }

    // Above everything that other tests build: the mushroom of glowing_mushroom_bonemeal hangs its cap
    // over the next row of tests, up to Y 18.
    private static final int TRACK_Y = 32;

    /**
     * The block at the given X of the chunk of a test. Entities tick only in the chunks of the test
     * structures, and the server puts those at a random place in every run: a minecart that leaves the
     * chunk of its test stops for good, unless a test next to it happens to hold the chunk it enters.
     */
    private static BlockPos inTestChunk(GameTestHelper helper, int x, int y) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return origin.offset(x - (origin.getX() & 15), y, 0);
    }

    /** A track of 16 rails to the east, across the chunk of the test, that climbs one block after the fifth; returns its first rail. */
    private static BlockPos slopedTrack(GameTestHelper helper, int y, Block rail) {
        BlockPos base = inTestChunk(helper, 0, y);
        var level = helper.getLevel();
        for (int x = 0; x < 16; x++) {
            int step = x < 6 ? 0 : 1;
            level.setBlockAndUpdate(base.offset(x, step - 1, 0), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.offset(x, step, 0), rail.defaultBlockState()
                .setValue(RailBlock.SHAPE, x == 5 ? RailShape.ASCENDING_EAST : RailShape.EAST_WEST));
        }
        return base;
    }

    // An empty minecart loses 4% of its speed every tick on a vanilla rail and slides back down a slope.
    private static void runicRailKeepsSpeed(GameTestHelper helper) {
        BlockPos base = slopedTrack(helper, TRACK_Y, RRBlocks.RUNIC_RAIL.get());
        var level = helper.getLevel();

        double speed = 0.2;
        Minecart minecart = EntityTypes.MINECART.create(level, EntitySpawnReason.COMMAND);
        minecart.setInitialPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        minecart.setDeltaMovement(speed, 0, 0);
        level.addFreshEntity(minecart);

        helper.runAfterDelay(50, () -> {
            helper.assertTrue(minecart.getY() > base.getY() + 0.9, "the minecart did not climb the slope: y " + minecart.getY());
            helper.assertTrue(minecart.getX() > base.getX() + 8, "the minecart stopped at x " + (minecart.getX() - base.getX()));
            helper.assertTrue(Math.abs(minecart.getDeltaMovement().horizontalDistance() - speed) < 1.0E-3,
                "the minecart changed its speed to " + minecart.getDeltaMovement().horizontalDistance());
            helper.succeed();
        });
    }

    // On vanilla rails a minecart with a rider slows down, most of all on a slope, and an empty one
    // at this speed rolls on for five blocks.
    private static void runicMinecartKeepsSpeed(GameTestHelper helper) {
        // Higher than the tracks of the runic rail tests, which can lie in the same chunk.
        BlockPos base = slopedTrack(helper, TRACK_Y + 14, Blocks.RAIL);
        var level = helper.getLevel();

        double speed = 0.2;
        RunicMinecart minecart = RREntityTypes.RUNIC_MINECART.get().create(level, EntitySpawnReason.COMMAND);
        minecart.setInitialPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        minecart.setDeltaMovement(speed, 0, 0);
        level.addFreshEntity(minecart);
        Pig rider = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        rider.setNoAi(true);
        rider.setPos(minecart.position());
        level.addFreshEntity(rider);
        rider.startRiding(minecart, true, false);

        helper.runAfterDelay(60, () -> {
            helper.assertTrue(minecart.isVehicle(), "the rider left the minecart");
            helper.assertTrue(minecart.getY() > base.getY() + 0.9, "the minecart did not climb the slope: y " + minecart.getY());
            helper.assertTrue(minecart.getX() > base.getX() + 8, "the minecart stopped at x " + (minecart.getX() - base.getX()));
            helper.assertTrue(Math.abs(minecart.getDeltaMovement().horizontalDistance() - speed) < 1.0E-3,
                "the minecart changed its speed to " + minecart.getDeltaMovement().horizontalDistance());
            // Removed, not dismounted: a pig standing on the rails would stop the minecart as well.
            rider.discard();
            double left = minecart.getX();
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(minecart.getDeltaMovement().horizontalDistance() < 1.0E-3,
                    "the empty minecart still moves at " + minecart.getDeltaMovement().horizontalDistance());
                helper.assertTrue(minecart.getX() - left < 1.5, "the empty minecart rolled on for " + (minecart.getX() - left));
                helper.succeed();
            });
        });
    }

    private static final int FIGHT_FLOOR_SIZE = 25;

    /** A stone floor for a fight, its corner at the origin of the test; mobs stand on the given height. */
    private static void fightFloor(GameTestHelper helper, int y) {
        BlockPos corner = helper.absolutePos(new BlockPos(0, y - 1, 0));
        BlockPos far = corner.offset(FIGHT_FLOOR_SIZE - 1, 0, FIGHT_FLOOR_SIZE - 1);
        ServerLevel level = helper.getLevel();
        // Without players, mobs move only in forced chunks that have got around to ticking entities, and the test
        // forces just the chunk of its origin. Chunks get ready in real time while the tests run their ticks as fast
        // as they can: a mob would hang in the air at a chunk border until the test is over, so wait for them here.
        // The runner releases every forced chunk after the batch.
        List<ChunkPos> chunks = ChunkPos.rangeClosed(ChunkPos.containing(corner), ChunkPos.containing(far)).toList();
        chunks.forEach(chunk -> level.setChunkForced(chunk.x(), chunk.z(), true));
        long deadline = System.nanoTime() + 30_000_000_000L;
        while (!chunks.stream().allMatch(chunk -> level.isPositionEntityTicking(chunk.getWorldPosition()))) {
            helper.assertTrue(System.nanoTime() < deadline, "the chunks of the fight floor do not start ticking");
            level.getChunkSource().pollTask();
        }
        for (BlockPos pos : BlockPos.betweenClosed(corner, far)) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    /** Ends a fight: its mobs would go on and fall from the floor onto the tests below. */
    private static void endFight(GameTestHelper helper, int y) {
        AABB floor = AABB.encapsulatingFullBlocks(
            helper.absolutePos(new BlockPos(0, y, 0)), helper.absolutePos(new BlockPos(FIGHT_FLOOR_SIZE - 1, y, FIGHT_FLOOR_SIZE - 1)));
        helper.getLevel().getEntitiesOfClass(Mob.class, floor.inflate(16)).forEach(Mob::discard);
    }

    /** A snail that only stands where it is put and outlives a fight. */
    private static Snail fightTarget(GameTestHelper helper, int x, int y, int z) {
        Snail snail = helper.spawn(RREntityTypes.SNAIL.get(), new BlockPos(x, y, z));
        snail.goalSelector.removeAllGoals(goal -> true);
        snail.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        snail.setHealth(200.0F);
        return snail;
    }

    // A Prophet with no warriors around it calls them before anything else.
    private static void mossyProphetSummonsWarriors(GameTestHelper helper) {
        int y = 50;
        fightFloor(helper, y);
        MossyProphet prophet = helper.spawn(RREntityTypes.MOSSY_PROPHET.get(), new BlockPos(8, y, 12));
        prophet.setTarget(fightTarget(helper, 16, y, 12));

        helper.succeedWhen(() -> {
            int warriors = helper.getLevel().getEntitiesOfClass(MossyWarrior.class, prophet.getBoundingBox().inflate(8)).size();
            helper.assertTrue(warriors >= 3 && warriors <= 5, "the Prophet has " + warriors + " warriors");
            endFight(helper, y);
        });
    }

    // The shockwave of a landing runs over the ground: it reaches a bystander, but not one that stands above it.
    private static void mossyProphetLeap(GameTestHelper helper) {
        int y = 70;
        fightFloor(helper, y);
        var level = helper.getLevel();
        // Enough warriors at its side, so the Prophet leaps instead of calling more.
        for (int z = 2; z <= 4; z++) {
            helper.spawn(RREntityTypes.MOSSY_WARRIOR.get(), new BlockPos(2, y, z)).setNoAi(true);
        }
        MossyProphet prophet = helper.spawn(RREntityTypes.MOSSY_PROPHET.get(), new BlockPos(6, y, 12));
        Snail target = fightTarget(helper, 16, y, 12);
        prophet.setTarget(target);
        Snail bystander = fightTarget(helper, 16, y, 16);
        level.setBlockAndUpdate(helper.absolutePos(new BlockPos(16, y + 2, 8)), Blocks.STONE.defaultBlockState());
        Snail above = fightTarget(helper, 16, y + 3, 8);

        double floor = prophet.getY();
        Vec3 aim = target.position();
        double[] peak = {floor};
        Vec3[] landing = {null};
        helper.onEachTick(() -> {
            peak[0] = Math.max(peak[0], prophet.getY());
            if (landing[0] == null && peak[0] - floor > 4.0 && prophet.onGround()) {
                landing[0] = prophet.position();
            }
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(landing[0] != null, "the Prophet rose by " + (peak[0] - floor) + " and did not land");
            helper.assertTrue(landing[0].distanceTo(aim) < 2.0, "the Prophet landed " + landing[0].distanceTo(aim) + " from its target");
            helper.assertTrue(prophet.getHealth() == prophet.getMaxHealth(), "the leap hurt the Prophet: " + prophet.getHealth());
            helper.assertTrue(bystander.getHealth() == 192.0F, "the bystander on the ground has health " + bystander.getHealth());
            helper.assertTrue(above.getHealth() == 200.0F, "the bystander above the ground has health " + above.getHealth());
            endFight(helper, y);
            helper.succeed();
        });
    }

    // A warrior beside the Prophet goes up into its hands and from there at the target, which it then fights.
    private static void mossyProphetThrowsWarrior(GameTestHelper helper) {
        int y = 110;
        fightFloor(helper, y);
        // Enough warriors, so the Prophet does not call more; these two do not move and cannot be picked up.
        for (int z = 2; z <= 3; z++) {
            helper.spawn(RREntityTypes.MOSSY_WARRIOR.get(), new BlockPos(2, y, z)).setNoAi(true);
        }
        MossyWarrior warrior = helper.spawn(RREntityTypes.MOSSY_WARRIOR.get(), new BlockPos(6, y, 9));
        MossyProphet prophet = helper.spawn(RREntityTypes.MOSSY_PROPHET.get(), new BlockPos(6, y, 12));
        Snail target = fightTarget(helper, 16, y, 12);
        prophet.setTarget(target);

        boolean[] held = {false};
        helper.onEachTick(() -> held[0] |= warrior.getVehicle() == prophet && warrior.getY() - prophet.getY() > 2.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(held[0], "the Prophet did not lift the warrior");
            helper.assertTrue(target.getHealth() == 192.0F, "the target has health " + target.getHealth());
            helper.assertTrue(warrior.getTarget() == target, "the thrown warrior fights " + warrior.getTarget());
            endFight(helper, y);
        });
    }

    // A warrior some blocks from its target somersaults onto it, which hurts more than its punch.
    private static void mossyWarriorSpinJump(GameTestHelper helper) {
        int y = 90;
        fightFloor(helper, y);
        MossyWarrior warrior = helper.spawn(RREntityTypes.MOSSY_WARRIOR.get(), new BlockPos(8, y, 12));
        Snail target = fightTarget(helper, 14, y, 12);
        warrior.setTarget(target);

        boolean[] spun = {false};
        helper.onEachTick(() -> spun[0] |= warrior.getAction() == MossyRam.Action.SPIN && !warrior.onGround());
        helper.succeedWhen(() -> {
            helper.assertTrue(spun[0], "the warrior did not somersault");
            helper.assertTrue(target.getHealth() == 195.0F, "the target has health " + target.getHealth());
            endFight(helper, y);
        });
    }

    // The chest of a station is what puts a runic minecart into every network.
    private static void hangingTracksStationLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(RRLootTables.HANGING_TRACKS_STATION);
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
            .create(LootContextParamSets.CHEST);
        for (long seed = 1; seed <= 20; seed++) {
            List<ItemStack> loot = table.getRandomItems(params, seed);
            helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(RRItems.RUNIC_MINECART)),
                "no runic minecart in the station chest with seed " + seed);
            helper.assertTrue(loot.size() > 1, "no supplies in the station chest with seed " + seed);
        }
        helper.succeed();
    }

    // On a vanilla rail that lies across its way a minecart turns aside.
    private static void runicRailCrossing(GameTestHelper helper) {
        // Higher than the track of runic_rail_keeps_speed, which can lie in the same chunk.
        BlockPos base = inTestChunk(helper, 8, TRACK_Y + 7);
        var level = helper.getLevel();
        BlockState rail = RRBlocks.RUNIC_RAIL.get().defaultBlockState();
        for (int i = -5; i <= 5; i++) {
            level.setBlockAndUpdate(base.offset(i, -1, 0), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(base.offset(0, -1, i), Blocks.STONE.defaultBlockState());
        }
        for (int i = -5; i <= 5; i++) {
            level.setBlock(base.offset(0, 0, i), rail.setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH), Block.UPDATE_CLIENTS);
            if (i != 0) {
                level.setBlock(base.offset(i, 0, 0), rail.setValue(RailBlock.SHAPE, RailShape.EAST_WEST), Block.UPDATE_CLIENTS);
            }
        }

        Minecart minecart = EntityTypes.MINECART.create(level, EntitySpawnReason.COMMAND);
        minecart.setInitialPos(base.getX() - 4.5, base.getY(), base.getZ() + 0.5);
        minecart.setDeltaMovement(0.2, 0, 0);
        level.addFreshEntity(minecart);

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(minecart.getX() > base.getX() + 2, "the minecart did not cross: x " + (minecart.getX() - base.getX()));
            helper.assertTrue(Math.abs(minecart.getZ() - base.getZ() - 0.5) < 0.1, "the minecart turned aside: z " + (minecart.getZ() - base.getZ()));
            helper.succeed();
        });
    }

    private static void ashenMushroomSmallRadii(GameTestHelper helper) {
        try {
            for (int radius = 1; radius <= 3; radius++) {
                PreviewWorld world = PreviewWorld.create(radius);
                BlockPos origin = new BlockPos(0, 64, 0);
                boolean placed = PreviewJobs.placeFeature(
                    new WallMushroomFeature(),
                    new WallMushroomFeature.Config(
                        BlockStateProvider.simple(RRBlocks.ASHEN_MUSHROOM_BLOCK.get()),
                        ConstantInt.of(radius * 2 + 1)
                    ),
                    origin,
                    world,
                    null
                );
                helper.assertTrue(placed, "ashen mushroom placement failed at radius " + radius);
                BoundingBox bounds = world.occupiedBox();
                if (radius == 1) {
                    assertSmallMushroomArc(helper, world, radius);
                    helper.assertTrue(bounds.getYSpan() == 2, "radius 1 must have exactly two cap layers");
                } else if (radius == 2) {
                    helper.assertTrue(bounds.getYSpan() == 1, "radius 2 must have exactly one cap layer");
                    assertFootprint(helper, world, bounds, 0, new String[] {
                        ".###.", "#####", "#####", "#####", ".###."
                    }, radius);
                    assertConnectedCap(helper, world, bounds, radius);
                } else {
                    helper.assertTrue(bounds.getYSpan() == 2, "radius 3 must have exactly two cap layers");
                    assertSmallMushroomArc(helper, world, radius);
                    assertFootprint(helper, world, bounds, 0, new String[] {
                        "..###..", ".#####.", "#######", "#######", "#######", ".#####.", "..###.."
                    }, radius);
                    assertFootprint(helper, world, bounds, 1, new String[] {
                        ".......", "..###..", ".#####.", ".#####.", ".#####.", "..###..", "......."
                    }, radius);
                }
            }
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }

    // Worlds saved before the mossberry bush became the wispberry bush still name the old ids.
    private static void wispberryAliases(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(RR.id("moss_berry_bush")) == RRBlocks.WISPBERRY_BUSH.get(),
            "moss_berry_bush is no longer read as the wispberry bush");
        helper.assertTrue(BuiltInRegistries.ITEM.getValue(RR.id("moss_berry")) == RRItems.WISPBERRY.get(),
            "moss_berry is no longer read as the wispberry");
        helper.succeed();
    }

    private static void glowingMushroomBonemeal(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 5, 0));
        var level = helper.getLevel();
        var mushroom = (GlowingMushroomBlock) RRBlocks.GLOWING_MUSHROOM.get();
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            }
        }

        var state = mushroom.defaultBlockState();
        level.setBlockAndUpdate(base, state);
        helper.assertTrue(mushroom.isValidBonemealTarget(level, base, state), "glowing mushroom rejected bonemeal");
        mushroom.performBonemeal(level, RandomSource.create(42), base, state);

        boolean grew = false;
        for (int x = -12; x <= 12 && !grew; x++) {
            for (int y = 0; y <= 24 && !grew; y++) {
                for (int z = -12; z <= 12; z++) {
                    var block = level.getBlockState(base.offset(x, y, z)).getBlock();
                    if (block == RRBlocks.GLOWING_MUSHROOM_CAP.get() || block == RRBlocks.GLOWING_MUSHROOM_STEM.get()) {
                        grew = true;
                        break;
                    }
                }
            }
        }
        helper.assertTrue(grew, "bonemeal did not grow a glowing mushroom");
        helper.succeed();
    }

    // Bone meal grows one or two of the smallest berries on the bush. One time in four it also grows one
    // berry next to it: on a bush that stands there, or on free moss, where it starts a new bush.
    private static void mossberryBushBonemeal(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 5, 0));
        var level = helper.getLevel();
        var bush = (MossberryBushBlock) RRBlocks.MOSSBERRY_BUSH.get();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1))) {
            level.setBlockAndUpdate(pos.below(), Blocks.MOSS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(base, bush.defaultBlockState());
        helper.assertTrue(bush.isValidBonemealTarget(level, base, bush.defaultBlockState()), "mossberry bush rejected bonemeal");

        RandomSource random = RandomSource.create(42);
        int uses = 40;
        int spreads = 0;
        for (int use = 0; use < uses; use++) {
            int before = berriesAround(level, base);
            bush.performBonemeal(level, random, base, level.getBlockState(base));
            int grown = berriesAround(level, base) - before;
            helper.assertTrue(grown == 0 || grown == 1, "one use of bonemeal grew " + grown + " berries around the bush");
            spreads += grown;
            if (use == 0) {
                List<MossberryBushBlock.Berry> berries = MossberryBushBlock.BERRIES.stream()
                    .filter(berry -> level.getBlockState(base).getValue(berry.property())).toList();
                helper.assertTrue(!berries.isEmpty() && berries.size() <= 2, "bonemeal grew " + berries.size() + " berries on its bush");
                helper.assertTrue(berries.stream().allMatch(berry -> berry.size() == 2), "bonemeal did not start with the smallest berries");
            }
        }
        // About ten of the forty; none or most of them would be another rule.
        helper.assertTrue(spreads >= 3 && spreads <= 20, uses + " uses of bonemeal grew " + spreads + " berries around the bush");

        // The first berry of a bush that bone meal started is the one nearest to the bush it was used on.
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1))) {
            var near = level.getBlockState(pos);
            List<MossberryBushBlock.Berry> berries = near.is(bush)
                ? MossberryBushBlock.BERRIES.stream().filter(berry -> near.getValue(berry.property())).toList()
                : List.of();
            if (pos.equals(base) || berries.size() != 1) {
                continue;
            }
            double fromX = (base.getX() - pos.getX()) * 16 + 8;
            double fromZ = (base.getZ() - pos.getZ()) * 16 + 8;
            double distance = Math.hypot(berries.getFirst().x() - fromX, berries.getFirst().z() - fromZ);
            helper.assertTrue(MossberryBushBlock.BERRIES.stream().allMatch(other -> Math.hypot(other.x() - fromX, other.z() - fromZ) >= distance),
                "bonemeal started a bush with a berry far from the bush it was used on");
        }
        helper.succeed();
    }

    private static int berriesAround(ServerLevel level, BlockPos base) {
        int berries = 0;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 0, 1))) {
            var state = level.getBlockState(pos);
            if (!pos.equals(base) && state.is(RRBlocks.MOSSBERRY_BUSH.get())) {
                berries += (int) MossberryBushBlock.BERRIES.stream().filter(berry -> state.getValue(berry.property())).count();
            }
        }
        return berries;
    }
    // A mossberry bush grows on moss only. Full of berries it glows like a ripe wispberry bush; one click
    // picks them all for at most three berries and leaves the bush with its twigs.
    private static void mossberryBushHarvest(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 5, 0));
        var level = helper.getLevel();
        var bare = RRBlocks.MOSSBERRY_BUSH.get().defaultBlockState().setValue(MossberryBushBlock.TWIGS, true);
        var full = bare;
        for (MossberryBushBlock.Berry berry : MossberryBushBlock.BERRIES) {
            full = full.setValue(berry.property(), true);
        }
        helper.assertTrue(MossberryBushBlock.lightLevel(bare) == 0, "a mossberry bush without berries glows");
        helper.assertTrue(MossberryBushBlock.lightLevel(full) == 9, "a full mossberry bush gives light " + MossberryBushBlock.lightLevel(full));

        var fullLayer = RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState().setValue(DeepMossLayerBlock.LAYERS, DeepMossLayerBlock.MAX_HEIGHT);
        for (var ground : List.of(Blocks.STONE.defaultBlockState(), RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState(), fullLayer,
                Blocks.MOSS_BLOCK.defaultBlockState(), RRBlocks.DEEP_MOSS.get().defaultBlockState())) {
            level.setBlockAndUpdate(pos.below(2), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), ground);
            boolean moss = ground != Blocks.STONE.defaultBlockState() && ground != RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState();
            helper.assertTrue(full.canSurvive(level, pos) == moss, "a mossberry bush " + (moss ? "does not grow" : "grows") + " on " + ground);
        }
        level.setBlockAndUpdate(pos, full);
        full.useWithoutItem(level, helper.makeMockPlayer(GameType.SURVIVAL), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));

        helper.assertTrue(level.getBlockState(pos) == bare, "picking left " + level.getBlockState(pos));
        int berries = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(entity -> entity.getItem().is(RRItems.MOSSBERRY))
            .mapToInt(entity -> entity.getItem().getCount())
            .sum();
        helper.assertTrue(berries >= 1 && berries <= 3, "picking dropped " + berries + " mossberries");
        helper.succeed();
    }

    // Moss sprouts and the berry bushes go into a deep moss layer they are placed on, and a layer placed on
    // them goes under them. The moss holds like a layer of its height, stays when a player breaks the plant and
    // takes the plant along when its ground goes. Moss hummocks grow such plants.
    private static void plantsInMossLayer(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 5, 0));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var layer = RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState();
        var layerItem = (BlockItem) RRBlocks.DEEP_MOSS_LAYER.asItem();
        for (var item : List.of(RRBlocks.MOSS_SPROUTS.asItem(), RRBlocks.SMALL_MOSS_SPROUTS.asItem(), RRItems.MOSSBERRY.get(), RRItems.WISPBERRY.get())) {
            Block plant = ((BlockItem) item).getBlock();
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, layer.setValue(DeepMossLayerBlock.LAYERS, 3));
            ((BlockItem) item).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(item), hit));
            BlockState placed = level.getBlockState(pos);
            helper.assertTrue(placed.is(plant) && placed.getValue(MossLayerPlantBlock.MOSS) == 3, plant + " placed on three layers of moss gave " + placed);

            layerItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(layerItem), hit));
            BlockState lifted = level.getBlockState(pos);
            BlockState moss = layer.setValue(DeepMossLayerBlock.LAYERS, 4);
            helper.assertTrue(lifted.is(plant) && lifted.getValue(MossLayerPlantBlock.MOSS) == 4, "a layer placed on " + placed + " gave " + lifted);
            helper.assertTrue(lifted.getCollisionShape(level, pos).max(Direction.Axis.Y) == moss.getCollisionShape(level, pos).max(Direction.Axis.Y),
                lifted + " does not hold like four layers of moss");

            lifted.onDestroyedByPlayer(level, pos, player, ItemStack.EMPTY, true, level.getFluidState(pos));
            helper.assertTrue(level.getBlockState(pos) == moss, "breaking " + lifted + " left " + level.getBlockState(pos));

            level.setBlockAndUpdate(pos, lifted);
            level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
            helper.assertTrue(level.getBlockState(pos).isAir(), lifted + " stayed in the air");

            level.setBlockAndUpdate(pos.below(), RRBlocks.DEEP_MOSS.get().defaultBlockState());
            level.setBlockAndUpdate(pos, plant.defaultBlockState());
            layerItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(layerItem), hit));
            helper.assertTrue(level.getBlockState(pos) == plant.defaultBlockState().setValue(MossLayerPlantBlock.MOSS, 1),
                "a layer placed on " + plant + " without moss gave " + level.getBlockState(pos));
        }

        // Bone meal starts new bushes in the moss layers around a bush, but not in a full one.
        var bush = (MossberryBushBlock) RRBlocks.MOSSBERRY_BUSH.get();
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            level.setBlockAndUpdate(near.below(), RRBlocks.DEEP_MOSS.get().defaultBlockState());
            level.setBlockAndUpdate(near, layer.setValue(DeepMossLayerBlock.LAYERS, near.getX() == pos.getX() ? DeepMossLayerBlock.MAX_HEIGHT : 2));
        }
        level.setBlockAndUpdate(pos, bush.defaultBlockState().setValue(MossLayerPlantBlock.MOSS, 2));
        RandomSource random = RandomSource.create(42);
        for (int use = 0; use < 60; use++) {
            bush.performBonemeal(level, random, pos, level.getBlockState(pos));
        }
        int started = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(near);
            if (near.getX() == pos.getX() && !near.equals(pos)) {
                helper.assertTrue(state.is(RRBlocks.DEEP_MOSS_LAYER), "bone meal replaced a full layer of moss with " + state);
            } else if (state.is(bush)) {
                helper.assertTrue(state.getValue(MossLayerPlantBlock.MOSS) == 2, "bone meal started " + state + " in two layers of moss");
                started++;
            }
        }
        helper.assertTrue(started > 1, "bone meal started no bush in the moss layers around the bush");

        BlockPos origin = new BlockPos(0, 64, 0);
        PreviewWorld world = PreviewWorld.create(42);
        world.fillBox(new BoundingBox(-12, 63, -12, 12, 63, 12), RRBlocks.DEEP_MOSS.get().defaultBlockState());
        PreviewJobs.placeFeature(RRFeatures.MOSS_HUMMOCK.get(), NoneFeatureConfiguration.NONE, origin, world, null);
        int inMoss = 0;
        for (BlockPos column : BlockPos.betweenClosed(origin.offset(-12, 0, -12), origin.offset(12, 0, 12))) {
            BlockState state = world.get(column);
            if (state.getBlock() instanceof MossLayerPlantBlock && state.getValue(MossLayerPlantBlock.MOSS) > 0) {
                inMoss++;
            }
        }
        helper.assertTrue(inMoss > 0, "a moss hummock grew no plant in its moss");
        helper.succeed();
    }

    private static void glowingMushroomPatch(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        PreviewWorld world = PreviewWorld.create(42);
        world.fillBox(new BoundingBox(-8, 63, -8, 8, 63, 8), Blocks.STONE.defaultBlockState());
        boolean placed = PreviewJobs.placeFeature(
            RRFeatures.GLOWING_MUSHROOM_PATCH.get(),
            NoneFeatureConfiguration.NONE,
            origin,
            world,
            null
        );
        helper.assertTrue(placed, "glowing mushroom patch placed nothing");

        int mushrooms = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.get(origin.offset(x, y, z)).is(RRBlocks.GLOWING_MUSHROOM.get())) {
                        mushrooms++;
                    }
                }
            }
        }
        helper.assertTrue(mushrooms > 1, "glowing mushroom patch placed fewer than two mushrooms");
        helper.succeed();
    }

    private static void ashenMushroomLargeRadii(GameTestHelper helper) {
        try {
            for (int diameter = 9; diameter <= 15; diameter++) {
                double radius = (diameter - 1) / 2.0;
                PreviewWorld world = PreviewWorld.create(diameter);
                BlockPos origin = new BlockPos(0, 64, 0);
                boolean placed = PreviewJobs.placeFeature(
                    new WallMushroomFeature(),
                    new WallMushroomFeature.Config(
                        BlockStateProvider.simple(RRBlocks.ASHEN_MUSHROOM_BLOCK.get()),
                        ConstantInt.of(diameter)
                    ),
                    origin,
                    world,
                    null
                );
                helper.assertTrue(placed, "ashen mushroom placement failed at radius " + radius);
                BoundingBox bounds = world.occupiedBox();
                if (radius <= 5.0) {
                    helper.assertTrue(bounds.getYSpan() == 3,
                        "radius " + radius + " must have at most three cap layers");
                } else {
                    helper.assertTrue(bounds.getYSpan() >= 3 && bounds.getYSpan() <= 4,
                        "large radius " + radius + " must have three or four cap layers");
                }
                assertOccupiedLayers(helper, world, bounds, radius);
                assertRoundedCapProfile(helper, world, bounds, radius);
                assertConnectedCap(helper, world, bounds, radius);
            }
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }

    private static void assertFootprint(
        GameTestHelper helper,
        PreviewWorld world,
        BoundingBox bounds,
        int layer,
        String[] expectedRows,
        int radius
    ) {
        helper.assertTrue(bounds.getXSpan() == expectedRows[0].length()
                && bounds.getZSpan() == expectedRows.length,
            "incorrect footprint bounds at radius " + radius);
        int y = bounds.minY() + layer;
        for (int z = 0; z < expectedRows.length; z++) {
            for (int x = 0; x < expectedRows[z].length(); x++) {
                boolean expected = expectedRows[z].charAt(x) == '#';
                boolean actual = world.get(new BlockPos(bounds.minX() + x, y, bounds.minZ() + z))
                    .is(RRBlocks.ASHEN_MUSHROOM_BLOCK.get());
                helper.assertTrue(actual == expected,
                    "unexpected radius " + radius + " block at layer " + layer + ", x=" + x + ", z=" + z);
            }
        }
    }

    private static void assertSmallMushroomArc(GameTestHelper helper, PreviewWorld world, int radius) {
        BoundingBox bounds = world.occupiedBox();
        Set<BlockPos> cap = new HashSet<>();
        int[] layerCounts = new int[bounds.getYSpan()];
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            int[] rowCounts = new int[bounds.getZSpan()];
            int firstSolidZ = Integer.MAX_VALUE;
            int lastSolidZ = Integer.MIN_VALUE;
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                int firstX = Integer.MAX_VALUE;
                int lastX = Integer.MIN_VALUE;
                int rowCount = 0;
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.get(pos).is(RRBlocks.ASHEN_MUSHROOM_BLOCK.get())) {
                        continue;
                    }
                    cap.add(pos);
                    layerCounts[y - bounds.minY()]++;
                    rowCount++;
                    firstX = Math.min(firstX, x);
                    lastX = Math.max(lastX, x);
                }
                helper.assertTrue(rowCount == 0 || lastX - firstX + 1 == rowCount,
                    "hole in radius " + radius + " cap row at y=" + y + ", z=" + z);
                rowCounts[z - bounds.minZ()] = rowCount;
                if (rowCount > 0) {
                    firstSolidZ = Math.min(firstSolidZ, z);
                    lastSolidZ = Math.max(lastSolidZ, z);
                }
            }
            for (int z = firstSolidZ; z <= lastSolidZ; z++) {
                helper.assertTrue(rowCounts[z - bounds.minZ()] > 0,
                    "hole in radius " + radius + " cap layer at y=" + y + ", z=" + z);
            }
        }

        helper.assertTrue(!cap.isEmpty(), "empty ashen mushroom at radius " + radius);
        int previousLayerCount = Integer.MAX_VALUE;
        for (int layer = 0; layer < layerCounts.length; layer++) {
            int count = layerCounts[layer];
            helper.assertTrue(count > 0, "empty vertical layer at radius " + radius);
            helper.assertTrue(count <= previousLayerCount, "cap widens toward its crown at radius " + radius);
            previousLayerCount = count;
        }
        helper.assertTrue(layerCounts[layerCounts.length - 1] < layerCounts[0],
            "radius " + radius + " does not form a curved arc");

        assertConnectedCap(helper, world, bounds, radius);
    }

    private static void assertOccupiedLayers(GameTestHelper helper, PreviewWorld world, BoundingBox bounds, double radius) {
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            boolean occupied = false;
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    if (world.get(new BlockPos(x, y, z)).is(RRBlocks.ASHEN_MUSHROOM_BLOCK.get())) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) {
                    break;
                }
            }
            helper.assertTrue(occupied, "empty vertical layer at radius " + radius);
        }
    }

    private static void assertRoundedCapProfile(GameTestHelper helper, PreviewWorld world, BoundingBox bounds, double radius) {
        int bottomSpan = 0;
        int previousSpan = Integer.MAX_VALUE;
        int topSpan = 0;
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    if (!world.get(new BlockPos(x, y, z)).is(RRBlocks.ASHEN_MUSHROOM_BLOCK.get())) {
                        continue;
                    }
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
            }
            int layerSpan = Math.max(maxX - minX + 1, maxZ - minZ + 1);
            if (y == bounds.minY()) {
                bottomSpan = layerSpan;
            }
            helper.assertTrue(layerSpan <= previousSpan,
                "large radius " + radius + " widens toward its crown at y=" + y);
            previousSpan = layerSpan;
            topSpan = layerSpan;
        }
        helper.assertTrue(topSpan * 4 <= bottomSpan * 3,
            "large radius " + radius + " crown is too wide to read as rounded");
    }

    private static void assertConnectedCap(GameTestHelper helper, PreviewWorld world, BoundingBox bounds, double radius) {
        Set<BlockPos> cap = new HashSet<>();
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.get(pos).is(RRBlocks.ASHEN_MUSHROOM_BLOCK.get())) {
                        cap.add(pos);
                    }
                }
            }
        }
        helper.assertTrue(!cap.isEmpty(), "empty ashen mushroom at radius " + radius);
        Set<BlockPos> unvisited = new HashSet<>(cap);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(unvisited.iterator().next());
        unvisited.remove(queue.peek());
        int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            for (int[] step : steps) {
                BlockPos neighbor = pos.offset(step[0], step[1], step[2]);
                if (unvisited.remove(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
        helper.assertTrue(unvisited.isEmpty(), "floating isolated cap blocks at radius " + radius);
    }
}
