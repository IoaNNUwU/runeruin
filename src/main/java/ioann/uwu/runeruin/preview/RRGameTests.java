package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.GlowingMushroomBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRFeatures;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import ioann.uwu.runeruin.region.RegionExport;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHEN_MUSHROOM_SMALL_RADII =
        TEST_FUNCTIONS.register("ashen_mushroom_small_radii", () -> RRGameTests::ashenMushroomSmallRadii);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHEN_MUSHROOM_LARGE_RADII =
        TEST_FUNCTIONS.register("ashen_mushroom_large_radii", () -> RRGameTests::ashenMushroomLargeRadii);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GLOWING_MUSHROOM_BONEMEAL =
        TEST_FUNCTIONS.register("glowing_mushroom_bonemeal", () -> RRGameTests::glowingMushroomBonemeal);
    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GLOWING_MUSHROOM_PATCH =
        TEST_FUNCTIONS.register("glowing_mushroom_patch", () -> RRGameTests::glowingMushroomPatch);

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
            RR.id("glowing_mushroom_bonemeal"),
            new FunctionGameTestInstance(
                GLOWING_MUSHROOM_BONEMEAL.getKey(),
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
