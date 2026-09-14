package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic block-plan builder shared by the baobab structure and its preview. */
public final class BaobabTreeGenerator {
    private static final int GROUND_SCAN_STEPS = 32;
    public static final int MAX_TREE_HEIGHT = 40;
    public static final int MAX_HORIZONTAL_EXTENT = 44;
    private static final int CROWN_SCALE_START_HEIGHT = 30;
    private static final double MAX_CROWN_RADIUS_SCALE_REDUCTION = 0.25;
    private static final int HANGING_LEAF_VINE_COUNT_MULTIPLIER = 3;
    private static final double ADDITIONAL_LEAF_VINE_MIN_SEPARATION_SQUARED = 3.0;
    private static final float CANOPY_SIDE_MOSS_CHANCE = 0.68F;
    private static final double MIN_HEIGHT_PER_RADIUS = 1.0;
    private static final double MAX_HEIGHT_PER_RADIUS = 1.5;
    public static final int GROUND_PROFILE_WIDTH = MAX_HORIZONTAL_EXTENT * 2 + 1;

    private BaobabTreeGenerator() {
    }

    public static int sampleHeight(int radius, RandomSource random) {
        int minHeight = Math.min(MAX_TREE_HEIGHT, (int) Math.ceil(radius * MIN_HEIGHT_PER_RADIUS));
        int maxHeight = Math.min(MAX_TREE_HEIGHT, (int) Math.floor(radius * MAX_HEIGHT_PER_RADIUS));
        return random.nextInt(minHeight, maxHeight + 1);
    }

    public static GroundProfile sampleGroundProfile(int centerX, int centerZ, RandomState randomState) {
        int[] groundHeights = new int[GROUND_PROFILE_WIDTH * GROUND_PROFILE_WIDTH];
        for (int dx = -MAX_HORIZONTAL_EXTENT; dx <= MAX_HORIZONTAL_EXTENT; dx++) {
            for (int dz = -MAX_HORIZONTAL_EXTENT; dz <= MAX_HORIZONTAL_EXTENT; dz++) {
                groundHeights[profileIndex(dx, dz)] = TopLayerAndBloomingCavesGen.bloomingCavesFloorY(
                        centerX + dx, centerZ + dz, randomState) - 1;
            }
        }
        return new GroundProfile(centerX, centerZ, groundHeights);
    }

    public static Map<BlockPos, BlockState> generate(
            int originX,
            int originZ,
            int radius,
            int height,
            long seed,
            GroundProfile ground,
            BlockState trunk,
            BlockState leaves
    ) {
        RandomSource random = RandomSource.create(seed);
        BlockPos origin = new BlockPos(originX, ground.groundYAt(originX, originZ) + 1, originZ);
        int trunkHeight = (int) Math.round(height * 0.60);
        int trunkBaseRadius = Math.max(5, (int) Math.round(radius * 0.22));
        int trunkTopRadius = Math.max(3, (int) Math.round(trunkBaseRadius * 0.72));
        int trunkCapHeight = Math.max(3, trunkTopRadius);
        int trunkApexY = trunkHeight + trunkCapHeight;
        int crownVerticalRadius = Math.max(4, (int) Math.round(radius * 0.18));
        double canopyScale = crownRadiusScale(height);
        List<PendingLeafVines> additionalLeafVines = new ArrayList<>();

        leaves = leaves.trySetValue(LeavesBlock.PERSISTENT, true);
        Map<BlockPos, BlockState> tree = new LinkedHashMap<>();

        addTrunk(tree, ground, origin, trunk, trunkHeight, trunkBaseRadius, trunkTopRadius);
        addRoundedTrunkCap(tree, origin, trunk, trunkHeight, trunkTopRadius, trunkCapHeight);
        addButtressRoots(tree, ground, origin, trunk, trunkBaseRadius, radius, random);

        int branchCount = 6 + random.nextInt(2);
        double branchAngle = random.nextDouble() * Math.PI * 2;
        double angleStep = Math.PI * 2 / branchCount;
        int branchRadius = Math.max(1, (int) Math.round(trunkTopRadius * 0.55));
        int crownOffset = Math.max(3, (int) Math.round(radius * (0.57 + random.nextDouble() * 0.04)));
        int baseCenterY = height - crownVerticalRadius;
        int branchBaseWidth = Math.max(2, Math.min(3, (int) Math.round(trunkTopRadius * 0.55)));

        for (int i = 0; i < branchCount; i++) {
            double angle = branchAngle + angleStep * i;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            int centerX = (int) Math.round(dx * crownOffset);
            int centerZ = (int) Math.round(dz * crownOffset);
            int startY = trunkHeight - random.nextInt(3);
            int centerY = Math.min(baseCenterY + random.nextInt(7) - 3, height - crownVerticalRadius);

            addCurvedBranch(tree, origin, trunk, dx, dz, branchRadius, startY, crownOffset, centerY,
                    branchBaseWidth, 2);

            double crownRadius = radius * (0.33 + random.nextDouble() * 0.05) * canopyScale;
            double crownDepth = crownRadius * (0.86 + random.nextDouble() * 0.12);
            addCrown(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
            addHangingLeafVines(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random, additionalLeafVines,
                    leafVineSeed(seed, centerX, centerY, centerZ));
        }

        int upperBranchCount = 2 + random.nextInt(2);
        double upperAngleStep = Math.PI * 2 / upperBranchCount;
        double upperFirstAngle = branchAngle + angleStep * 0.5;
        int upperStartRadius = 0;
        for (int i = 0; i < upperBranchCount; i++) {
            double angle = upperFirstAngle + upperAngleStep * i + (random.nextDouble() - 0.5) * 0.18;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            int startY = trunkApexY;
            int centerY = Math.min(baseCenterY + random.nextInt(7) - 3, height - crownVerticalRadius);
            int rise = Math.max(2, centerY - startY);
            int endRadius = upperStartRadius + rise;
            int centerX = (int) Math.round(dx * endRadius);
            int centerZ = (int) Math.round(dz * endRadius);

            addCurvedBranch(tree, origin, trunk, dx, dz, upperStartRadius, startY, endRadius, centerY,
                    Math.max(2, branchBaseWidth - 1), 2);

            double crownRadius = radius * (0.25 + random.nextDouble() * 0.04) * canopyScale;
            double crownDepth = crownRadius * (0.86 + random.nextDouble() * 0.12);
            addCrown(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
            addHangingLeafVines(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random, additionalLeafVines,
                    leafVineSeed(seed ^ 0x9E3779B97F4A7C15L, centerX, centerY, centerZ));
        }

        Set<BlockPos> canopyMoss = addCanopyMossBiome(tree, ground, leaves, trunk, radius, random);
        addVanillaVines(tree, ground, origin, trunk, leaves, trunkHeight, trunkBaseRadius, radius, random);
        addAdditionalHangingLeafVines(tree, origin, leaves, additionalLeafVines);
        addCanopySideMoss(tree, ground, leaves, canopyMoss, random);
        return tree;
    }

    private static double crownRadiusScale(int height) {
        double heightProgress = Math.max(0.0, Math.min(1.0,
                (height - CROWN_SCALE_START_HEIGHT) / (double) (MAX_TREE_HEIGHT - CROWN_SCALE_START_HEIGHT)));
        return 1.0 - heightProgress * MAX_CROWN_RADIUS_SCALE_REDUCTION;
    }

    private static int profileIndex(int dx, int dz) {
        return (dx + MAX_HORIZONTAL_EXTENT) * GROUND_PROFILE_WIDTH + dz + MAX_HORIZONTAL_EXTENT;
    }

    public static int groundProfileLength() {
        return GROUND_PROFILE_WIDTH * GROUND_PROFILE_WIDTH;
    }

    public static int groundProfileIndex(int dx, int dz) {
        if (Math.abs(dx) > MAX_HORIZONTAL_EXTENT || Math.abs(dz) > MAX_HORIZONTAL_EXTENT) {
            throw new IndexOutOfBoundsException("Outside baobab ground profile: " + dx + ", " + dz);
        }
        return profileIndex(dx, dz);
    }

    public static final class GroundProfile {
        private final int centerX;
        private final int centerZ;
        private final int[] groundHeights;

        public GroundProfile(int centerX, int centerZ, int[] groundHeights) {
            if (groundHeights.length != GROUND_PROFILE_WIDTH * GROUND_PROFILE_WIDTH) {
                throw new IllegalArgumentException("Unexpected baobab ground profile size: " + groundHeights.length);
            }
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.groundHeights = groundHeights.clone();
        }

        public int[] copyHeights() {
            return this.groundHeights.clone();
        }

        public int groundYAt(int x, int z) {
            int dx = x - this.centerX;
            int dz = z - this.centerZ;
            if (Math.abs(dx) > MAX_HORIZONTAL_EXTENT || Math.abs(dz) > MAX_HORIZONTAL_EXTENT) {
                return this.groundHeights[profileIndex(0, 0)];
            }
            return this.groundHeights[profileIndex(dx, dz)];
        }

        public boolean isReplaceable(BlockPos pos) {
            return pos.getY() > this.groundYAt(pos.getX(), pos.getZ());
        }

        public boolean isSolid(BlockPos pos) {
            return pos.getY() <= this.groundYAt(pos.getX(), pos.getZ());
        }

    }

    public static boolean canTreeReplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS);
    }

    private static void addButtressRoots(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            BlockPos origin,
            BlockState trunk,
            int trunkBaseRadius,
            int radius,
            RandomSource random
    ) {
        Map<BlockPos, BlockState> roots = new LinkedHashMap<>();
        int rootCount = 6 + random.nextInt(3);
        int startRadius = Math.max(1, trunkBaseRadius - 1);
        int typicalStartY = Math.max(3, (int) Math.round(radius * 0.15));
        double firstAngle = random.nextDouble() * Math.PI * 2;
        double angleStep = Math.PI * 2 / rootCount;

        for (int i = 0; i < rootCount; i++) {
            double angle = firstAngle + angleStep * i;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            int startY = Math.max(3, typicalStartY + random.nextInt(3) - 1);
            RootTip tip = findRootTip(ground, origin, dx, dz, startRadius, startY);
            if (tip == null) {
                continue;
            }

            // Each root starts outward, then curves down into the local ground like an inverted branch.
            addCurvedRoot(roots, origin, trunk, dx, dz, startRadius, startY, tip.radius(), tip.y(),
                    2 + random.nextInt(2));
        }

        // Roots may meet uneven ground; keep the exposed part instead of failing the whole tree.
        for (Map.Entry<BlockPos, BlockState> entry : roots.entrySet()) {
            BlockPos pos = entry.getKey();
            if (tree.containsKey(pos) || ground.isSolid(pos)) {
                continue;
            }
            tree.put(pos, entry.getValue());
        }
    }

    private static RootTip findRootTip(
            GroundProfile groundProfile,
            BlockPos origin,
            double dx,
            double dz,
            int startRadius,
            int startY
    ) {
        int endRadius = startRadius + startY;
        int endY = 0;
        for (int attempt = 0; attempt < 4; attempt++) {
            int endX = (int) Math.round(dx * endRadius);
            int endZ = (int) Math.round(dz * endRadius);
            BlockPos ground = findGround(groundProfile, origin.getX() + endX, origin.getZ() + endZ,
                    origin.getY() + startY);
            if (ground == null) {
                return null;
            }

            endY = Math.min(startY - 1, ground.getY() + 1 - origin.getY());
            int adjustedRadius = startRadius + Math.max(1, startY - endY);
            if (adjustedRadius == endRadius) {
                return new RootTip(endRadius, endY);
            }
            endRadius = adjustedRadius;
        }

        int endX = (int) Math.round(dx * endRadius);
        int endZ = (int) Math.round(dz * endRadius);
        BlockPos ground = findGround(groundProfile, origin.getX() + endX, origin.getZ() + endZ,
                origin.getY() + startY);
        if (ground == null) {
            return null;
        }
        endY = Math.min(startY - 1, ground.getY() + 1 - origin.getY());
        return new RootTip(endRadius, endY);
    }

    private static BlockPos findGround(
            GroundProfile groundProfile,
            int x,
            int z,
            int startY
    ) {
        int groundY = groundProfile.groundYAt(x, z);
        int y = Math.min(groundY, startY);
        return startY - y <= GROUND_SCAN_STEPS ? new BlockPos(x, y, z) : null;
    }

    private static void addTrunk(
            Map<BlockPos, BlockState> tree,
            GroundProfile groundProfile,
            BlockPos origin,
            BlockState trunk,
            int height,
            int baseRadius,
            int topRadius
    ) {
        int bound = (int) Math.ceil(baseRadius + 1.0);
        int diameter = bound * 2 + 1;
        int[][] groundHeights = new int[diameter][diameter];
        int lowestTrunkY = 0;
        int groundScanStartY = origin.getY() + GROUND_SCAN_STEPS / 2;
        for (int x = -bound; x <= bound; x++) {
            for (int z = -bound; z <= bound; z++) {
                BlockPos ground = findGround(groundProfile, origin.getX() + x, origin.getZ() + z, groundScanStartY);
                int groundY = ground == null ? origin.getY() - 1 : ground.getY();
                groundHeights[x + bound][z + bound] = groundY;
                lowestTrunkY = Math.min(lowestTrunkY, groundY + 1 - origin.getY());
            }
        }

        // Let the flared foot descend to the local ground in each column instead of failing on uneven terrain.
        for (int y = lowestTrunkY; y <= height; y++) {
            int worldY = origin.getY() + y;
            int profileY = Math.max(0, y);
            double progress = profileY / (double) height;
            double easedProgress = progress * progress * (3.0 - 2.0 * progress);
            double footFlare = 0.85 * Math.exp(-profileY / Math.max(2.0, baseRadius * 1.35));
            double localRadius = baseRadius - (baseRadius - topRadius) * easedProgress + footFlare;
            for (int x = -bound; x <= bound; x++) {
                for (int z = -bound; z <= bound; z++) {
                    if (worldY <= groundHeights[x + bound][z + bound]) {
                        continue;
                    }

                    double distance = Math.sqrt(x * (double) x + z * (double) z);
                    if (distance == 0.0) {
                        put(tree, origin.offset(x, y, z), trunk);
                        continue;
                    }

                    double angle = Math.atan2(z, x);
                    double gentleBarkVariation = 0.24 * Math.sin(angle * 3.0 + progress * 4.8)
                            + 0.16 * Math.cos(angle * 5.0 - progress * 3.2);
                    if (distance <= localRadius + gentleBarkVariation) {
                        put(tree, origin.offset(x, y, z), trunk);
                    }
                }
            }
        }
    }

    private static void addRoundedTrunkCap(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState trunk,
            int bodyTopY,
            int topRadius,
            int capHeight
    ) {
        for (int y = 0; y <= capHeight; y++) {
            double progress = y / (double) capHeight;
            double radius = topRadius * Math.sqrt(Math.max(0.0, 1.0 - progress * progress));
            int bound = (int) Math.ceil(radius);
            for (int x = -bound; x <= bound; x++) {
                for (int z = -bound; z <= bound; z++) {
                    if (x * x + z * z <= radius * radius + 0.25) {
                        put(tree, origin.offset(x, bodyTopY + y, z), trunk);
                    }
                }
            }
        }
    }

    private static void addBranch(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState trunk,
            int startX,
            int startY,
            int startZ,
            int endX,
            int endY,
            int endZ,
            int width
    ) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;
        int deltaZ = endZ - startZ;
        double pathLength = Math.max(1.0e-6, Math.sqrt(deltaX * (double) deltaX + deltaY * (double) deltaY
                + deltaZ * (double) deltaZ));
        int steps = Math.max(1, (int) Math.ceil(pathLength * 2.0));

        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            double centerX = startX + deltaX * t;
            double centerY = startY + deltaY * t;
            double centerZ = startZ + deltaZ * t;
            putRootTubeSlice(tree, origin, trunk, centerX, centerY, centerZ,
                    deltaX, deltaY, deltaZ, width);
        }
    }

    private static void addCurvedRoot(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState wood,
            double dx,
            double dz,
            int startRadius,
            int startY,
            int endRadius,
            int endY,
            int width
    ) {
        int reach = endRadius - startRadius;
        int drop = startY - endY;
        if (reach <= 0 || drop <= 0) {
            addBranch(tree, origin, wood,
                    (int) Math.round(dx * startRadius), startY, (int) Math.round(dz * startRadius),
                    (int) Math.round(dx * endRadius), endY, (int) Math.round(dz * endRadius), width);
            return;
        }

        // Begin nearly horizontal, then turn down so the root reaches the ground without a straight diagonal.
        double arcAngle = 2.0 * Math.atan2(drop, reach);
        double arcRadius = reach / Math.sin(arcAngle);
        int steps = Math.max(1, (int) Math.ceil(arcRadius * arcAngle * 2.0));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double angle = arcAngle * progress;
            double radial = startRadius + arcRadius * Math.sin(angle);
            double x = dx * radial;
            double y = startY - arcRadius * (1.0 - Math.cos(angle));
            double z = dz * radial;
            double radialTangent = arcRadius * Math.cos(angle);
            double verticalTangent = -arcRadius * Math.sin(angle);
            putRootTubeSlice(tree, origin, wood, x, y, z,
                    dx * radialTangent, verticalTangent, dz * radialTangent, width);
        }
    }

    private static void putRootTubeSlice(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState wood,
            double centerX,
            double centerY,
            double centerZ,
            double tangentX,
            double tangentY,
            double tangentZ,
            int width
    ) {
        double pathLength = Math.max(1.0e-6,
                Math.sqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ));
        double horizontalLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
        double tangentYRatio = tangentY / pathLength;
        double sideX = horizontalLength > 1.0e-6 ? -tangentZ / horizontalLength : 1.0;
        double sideZ = horizontalLength > 1.0e-6 ? tangentX / horizontalLength : 0.0;
        double verticalPlaneX = horizontalLength > 1.0e-6 ? tangentYRatio * tangentX / horizontalLength : 0.0;
        double verticalPlaneY = -horizontalLength / pathLength;
        double verticalPlaneZ = horizontalLength > 1.0e-6 ? tangentYRatio * tangentZ / horizontalLength : 0.0;
        double sectionRadius = width * 0.5;
        int sectionBound = (int) Math.ceil(sectionRadius);

        for (int a = -sectionBound; a <= sectionBound; a++) {
            for (int b = -sectionBound; b <= sectionBound; b++) {
                if (a * a + b * b > sectionRadius * sectionRadius + 0.25) {
                    continue;
                }
                int x = (int) Math.round(centerX + sideX * a + verticalPlaneX * b);
                int y = (int) Math.round(centerY + verticalPlaneY * b);
                int z = (int) Math.round(centerZ + sideZ * a + verticalPlaneZ * b);
                put(tree, origin.offset(x, y, z), wood);
            }
        }
    }

    private static void addCurvedBranch(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState wood,
            double dx,
            double dz,
            int startRadius,
            int startY,
            int endRadius,
            int endY,
            int baseWidth,
            int tipWidth
    ) {
        int reach = endRadius - startRadius;
        int rise = endY - startY;
        if (reach <= 0 || rise <= 0) {
            addBranch(tree, origin, wood,
                    (int) Math.round(dx * startRadius), startY, (int) Math.round(dz * startRadius),
                    (int) Math.round(dx * endRadius), endY, (int) Math.round(dz * endRadius), tipWidth);
            return;
        }

        // Start nearly horizontal and sweep upward along the same circular arc used by the goblet arms.
        double arcAngle = 2.0 * Math.atan2(rise, reach);
        double arcRadius = reach / Math.sin(arcAngle);
        int steps = Math.max(1, (int) Math.ceil(arcRadius * arcAngle * 2.0));
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            double angle = arcAngle * progress;
            double radial = startRadius + arcRadius * Math.sin(angle);
            double x = dx * radial;
            double y = startY + arcRadius * (1.0 - Math.cos(angle));
            double z = dz * radial;
            int width = Math.max(tipWidth, (int) Math.round(baseWidth + (tipWidth - baseWidth) * progress));
            double radialTangent = arcRadius * Math.cos(angle);
            double verticalTangent = arcRadius * Math.sin(angle);
            putWoodTubeSlice(tree, origin, wood, x, y, z,
                    dx * radialTangent, verticalTangent, dz * radialTangent, width);
        }
    }

    private static void putWoodTubeSlice(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState wood,
            double centerX,
            double centerY,
            double centerZ,
            double tangentX,
            double tangentY,
            double tangentZ,
            int width
    ) {
        double pathLength = Math.max(1.0e-6,
                Math.sqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ));
        double horizontalLength = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
        double sideX = horizontalLength > 1.0e-6 ? -tangentZ / horizontalLength : 1.0;
        double sideZ = horizontalLength > 1.0e-6 ? tangentX / horizontalLength : 0.0;
        double verticalPlaneX = horizontalLength > 1.0e-6
                ? tangentY / pathLength * tangentX / horizontalLength : 0.0;
        double verticalPlaneY = -horizontalLength / pathLength;
        double verticalPlaneZ = horizontalLength > 1.0e-6
                ? tangentY / pathLength * tangentZ / horizontalLength : 0.0;
        double sectionRadius = width * 0.5;
        double firstOffset = -(width - 1) * 0.5;

        for (int aIndex = 0; aIndex < width; aIndex++) {
            for (int bIndex = 0; bIndex < width; bIndex++) {
                double a = firstOffset + aIndex;
                double b = firstOffset + bIndex;
                if (a * a + b * b > sectionRadius * sectionRadius) {
                    continue;
                }
                int x = (int) Math.round(centerX + sideX * a + verticalPlaneX * b);
                int y = (int) Math.round(centerY + verticalPlaneY * b);
                int z = (int) Math.round(centerZ + sideZ * a + verticalPlaneZ * b);
                put(tree, origin.offset(x, y, z), wood);
            }
        }
    }

    private static void addCrown(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState leaves,
            int centerX,
            int centerY,
            int centerZ,
            double radiusX,
            double radiusZ,
            int radiusY,
            RandomSource random
    ) {
        int boundX = (int) Math.ceil(radiusX);
        int boundZ = (int) Math.ceil(radiusZ);
        double inverseRadiusX2 = 1.0 / (radiusX * radiusX);
        double inverseRadiusZ2 = 1.0 / (radiusZ * radiusZ);
        double inverseRadiusY2 = 1.0 / (radiusY * (double) radiusY);

        for (int x = -boundX; x <= boundX; x++) {
            for (int z = -boundZ; z <= boundZ; z++) {
                double horizontalDistance = x * x * inverseRadiusX2 + z * z * inverseRadiusZ2;
                double angle = Math.atan2(z / radiusZ, x / radiusX);
                double unevenEdge = 1.0 + 0.045 * Math.sin(angle * 3.0 + 0.7)
                        + 0.03 * Math.cos(angle * 5.0 - 1.1);
                if (horizontalDistance > unevenEdge) {
                    continue;
                }

                int verticalBound = (int) Math.ceil(radiusY
                        * Math.sqrt(Math.max(0.0, unevenEdge - horizontalDistance)));
                for (int y = -verticalBound; y <= verticalBound; y++) {
                    double distance = horizontalDistance + y * y * inverseRadiusY2;
                    double missingLeafChance = distance > 0.78 ? 0.14 : distance > 0.42 ? 0.045 : 0.012;
                    if (distance > unevenEdge || random.nextFloat() < missingLeafChance) {
                        continue;
                    }
                    putIfAbsent(tree, origin.offset(centerX + x, centerY + y, centerZ + z), leaves);
                }
            }
        }
    }

    private static void addHangingLeafVines(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState leaves,
            int centerX,
            int centerY,
            int centerZ,
            double radiusX,
            double radiusZ,
            int radiusY,
            RandomSource random,
            List<PendingLeafVines> additionalLeafVines,
            long additionalVineSeed
    ) {
        List<BlockPos> candidates = new ArrayList<>();
        List<BlockPos> expandedCandidates = new ArrayList<>();
        int boundX = (int) Math.ceil(radiusX);
        int boundZ = (int) Math.ceil(radiusZ);
        for (int x = -boundX; x <= boundX; x++) {
            for (int z = -boundZ; z <= boundZ; z++) {
                double horizontalDistance = x * x / (radiusX * radiusX) + z * z / (radiusZ * radiusZ);
                if (horizontalDistance < 0.02 || horizontalDistance > 1.02) {
                    continue;
                }

                int drop = (int) Math.ceil(radiusY * Math.sqrt(Math.max(0.0, 1.0 - horizontalDistance)));
                BlockPos attach = origin.offset(centerX + x, centerY - drop, centerZ + z);
                BlockState at = tree.get(attach);
                if (at == null || at.getBlock() != leaves.getBlock()) {
                    continue;
                }
                expandedCandidates.add(attach);
                if (horizontalDistance >= 0.22 && horizontalDistance <= 0.88
                        && !tree.containsKey(attach.below())) {
                    candidates.add(attach);
                }
            }
        }

        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos temp = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, temp);
        }

        int originalTarget = 6 + random.nextInt(5) + (radiusX >= 10.0 ? 2 : 0);
        int targetStrands = (int) Math.round(originalTarget * 1.5);
        List<BlockPos> selected = new ArrayList<>();
        for (BlockPos attach : candidates) {
            if (selected.size() >= targetStrands) {
                break;
            }
            boolean tooClose = selected.stream().anyMatch(other ->
                    other.distSqr(attach) < 10.0);
            if (tooClose) {
                continue;
            }
            selected.add(attach);
            placeLeafVine(tree, origin, attach, leaves, random);
        }

        additionalLeafVines.add(new PendingLeafVines(
                List.copyOf(candidates),
                List.copyOf(expandedCandidates),
                List.copyOf(selected),
                selected.size() * (HANGING_LEAF_VINE_COUNT_MULTIPLIER - 1),
                additionalVineSeed
        ));
    }

    private static void addAdditionalHangingLeafVines(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockState leaves,
            List<PendingLeafVines> pendingVines
    ) {
        Set<BlockPos> usedAnchors = new LinkedHashSet<>();
        List<List<BlockPos>> expandedCandidatesByCrown = new ArrayList<>();
        List<RandomSource> additionalRandomByCrown = new ArrayList<>();
        for (PendingLeafVines pending : pendingVines) {
            usedAnchors.addAll(pending.baseSelection());
        }

        for (PendingLeafVines pending : pendingVines) {
            RandomSource random = RandomSource.create(pending.seed());
            List<BlockPos> candidates = new ArrayList<>(pending.candidates());
            shufflePositions(candidates, random);
            List<BlockPos> selected = new ArrayList<>(pending.baseSelection());
            int added = 0;

            for (BlockPos attach : candidates) {
                if (added >= pending.target()) {
                    break;
                }
                BlockState at = tree.get(attach);
                if (at == null || at.getBlock() != leaves.getBlock() || tree.containsKey(attach.below())) {
                    continue;
                }

                boolean tooClose = selected.stream().anyMatch(other ->
                        other.distSqr(attach) < ADDITIONAL_LEAF_VINE_MIN_SEPARATION_SQUARED);
                if (tooClose || usedAnchors.contains(attach)) {
                    continue;
                }
                selected.add(attach);
                usedAnchors.add(attach.immutable());
                placeLeafVine(tree, origin, attach, leaves, random);
                added++;
            }

            RandomSource additionalRandom = RandomSource.create(pending.seed() ^ 0xD1B54A32D192ED03L);
            List<BlockPos> expandedCandidates = new ArrayList<>(pending.expandedCandidates());
            shufflePositions(expandedCandidates, additionalRandom);
            expandedCandidatesByCrown.add(expandedCandidates);
            additionalRandomByCrown.add(additionalRandom);
        }

        // Match the actual number of existing unique strands across the whole tree.
        // Crowns with more free underside can make up for crowns whose edge is crowded.
        int targetAdditional = usedAnchors.size();
        int[] candidateIndexes = new int[expandedCandidatesByCrown.size()];
        int addedVisibleStrands = 0;
        while (addedVisibleStrands < targetAdditional) {
            boolean placedThisRound = false;
            for (int crown = 0; crown < expandedCandidatesByCrown.size()
                    && addedVisibleStrands < targetAdditional; crown++) {
                List<BlockPos> crownCandidates = expandedCandidatesByCrown.get(crown);
                RandomSource random = additionalRandomByCrown.get(crown);
                while (candidateIndexes[crown] < crownCandidates.size()) {
                    BlockPos attach = crownCandidates.get(candidateIndexes[crown]++);
                    if (usedAnchors.contains(attach)) {
                        continue;
                    }
                    BlockState at = tree.get(attach);
                    if (at == null || at.getBlock() != leaves.getBlock()) {
                        continue;
                    }
                    if (!placeAdditionalLeafVine(tree, origin, attach, leaves, random)) {
                        continue;
                    }
                    usedAnchors.add(attach.immutable());
                    addedVisibleStrands++;
                    placedThisRound = true;
                    break;
                }
            }
            if (!placedThisRound) {
                break;
            }
        }
    }

    private static long leafVineSeed(long treeSeed, int centerX, int centerY, int centerZ) {
        long seed = treeSeed * 31L + centerX;
        seed = seed * 31L + centerY;
        return seed * 31L + centerZ;
    }

    private static void placeLeafVine(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockPos attach,
            BlockState leaves,
            RandomSource random
    ) {
        int x = attach.getX() - origin.getX();
        int z = attach.getZ() - origin.getZ();
        int length = 3 + random.nextInt(6);
        BlockPos end = attach;
        for (int dy = 1; dy <= length; dy++) {
            if (dy > 1 && random.nextFloat() < 0.28F) {
                switch (random.nextInt(4)) {
                    case 0 -> x++;
                    case 1 -> x--;
                    case 2 -> z++;
                    default -> z--;
                }
            }

            BlockPos pos = origin.offset(x, attach.getY() - origin.getY() - dy, z);
            BlockState existing = tree.get(pos);
            if (existing != null && existing.getBlock() != leaves.getBlock()) {
                break;
            }
            putIfAbsent(tree, pos, leaves);
            end = pos;

            if (random.nextFloat() < 0.22F) {
                int sideX = random.nextBoolean() ? 1 : -1;
                BlockPos tuft = pos.offset(sideX, 0, random.nextInt(3) - 1);
                BlockState tuftState = tree.get(tuft);
                if (tuftState == null || tuftState.getBlock() == leaves.getBlock()) {
                    putIfAbsent(tree, tuft, leaves);
                }
            }
        }

        if (random.nextBoolean()) {
            BlockPos tip = end.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            BlockState tipState = tree.get(tip);
            if (tipState == null || tipState.getBlock() == leaves.getBlock()) {
                putIfAbsent(tree, tip, leaves);
            }
        }
    }

    private static boolean placeAdditionalLeafVine(
            Map<BlockPos, BlockState> tree,
            BlockPos origin,
            BlockPos attach,
            BlockState leaves,
            RandomSource random
    ) {
        List<BlockPos> newPath = new ArrayList<>();
        List<BlockPos> sideTufts = new ArrayList<>();
        int x = attach.getX() - origin.getX();
        int z = attach.getZ() - origin.getZ();
        int length = 3 + random.nextInt(6);
        BlockPos end = attach;
        for (int dy = 1; dy <= length; dy++) {
            if (dy > 1 && random.nextFloat() < 0.28F) {
                switch (random.nextInt(4)) {
                    case 0 -> x++;
                    case 1 -> x--;
                    case 2 -> z++;
                    default -> z--;
                }
            }

            BlockPos pos = origin.offset(x, attach.getY() - origin.getY() - dy, z);
            BlockState existing = tree.get(pos);
            if (existing != null && existing.getBlock() != leaves.getBlock()) {
                break;
            }
            if (existing == null) {
                newPath.add(pos.immutable());
            }
            end = pos;

            if (random.nextFloat() < 0.22F) {
                int sideX = random.nextBoolean() ? 1 : -1;
                BlockPos tuft = pos.offset(sideX, 0, random.nextInt(3) - 1);
                BlockState tuftState = tree.get(tuft);
                if (tuftState == null) {
                    sideTufts.add(tuft.immutable());
                }
            }
        }

        // Count only strands that extend the visible foliage with at least one new block.
        if (newPath.isEmpty()) {
            return false;
        }

        newPath.forEach(pos -> putIfAbsent(tree, pos, leaves));
        sideTufts.forEach(pos -> putIfAbsent(tree, pos, leaves));
        if (random.nextBoolean()) {
            BlockPos tip = end.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (!tree.containsKey(tip)) {
                putIfAbsent(tree, tip, leaves);
            }
        }
        return true;
    }

    private static Set<BlockPos> addCanopyMossBiome(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            BlockState leaves,
            BlockState trunk,
            int radius,
            RandomSource random
    ) {
        Map<CanopyColumn, BlockPos> highestLeaves = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : tree.entrySet()) {
            if (entry.getValue().getBlock() != leaves.getBlock()) {
                continue;
            }
            BlockPos leaf = entry.getKey();
            CanopyColumn column = new CanopyColumn(leaf.getX(), leaf.getZ());
            highestLeaves.merge(column, leaf,
                    (current, candidate) -> current.getY() >= candidate.getY() ? current : candidate);
        }

        List<BlockPos> surfaceLeaves = new ArrayList<>();
        for (BlockPos leaf : highestLeaves.values()) {
            BlockPos above = leaf.above();
            if (tree.containsKey(above) || !ground.isReplaceable(above)) {
                continue;
            }

            int nearbySurface = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = highestLeaves.get(new CanopyColumn(
                        leaf.getX() + direction.getStepX(), leaf.getZ() + direction.getStepZ()));
                if (neighbor != null && Math.abs(neighbor.getY() - leaf.getY()) <= 2) {
                    nearbySurface++;
                }
            }
            if (nearbySurface >= 2) {
                surfaceLeaves.add(leaf);
            }
        }

        Set<BlockPos> mossBlocks = new LinkedHashSet<>();
        for (BlockPos leaf : surfaceLeaves) {
            BlockPos moss = leaf.immutable();
            tree.put(moss, Blocks.MOSS_BLOCK.defaultBlockState());
            mossBlocks.add(moss);
        }

        List<CanopyPool> pools = addCanopyPools(tree, leaves, trunk,
                mossBlocks, radius, random);
        addCanopyPlants(tree, ground, mossBlocks, pools, random);
        return mossBlocks;
    }

    private static void addCanopySideMoss(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            BlockState leaves,
            Set<BlockPos> canopyMoss,
            RandomSource random
    ) {
        if (canopyMoss.isEmpty()) {
            return;
        }
        Map<Integer, Integer> leavesByY = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : tree.entrySet()) {
            if (entry.getValue().getBlock() == leaves.getBlock()) {
                leavesByY.merge(entry.getKey().getY(), 1, Integer::sum);
            }
        }
        int densestLayer = leavesByY.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int denseThreshold = Math.max(1, densestLayer / 4);
        int firstDenseY = Integer.MAX_VALUE;
        int lastDenseY = Integer.MIN_VALUE;
        for (Map.Entry<Integer, Integer> entry : leavesByY.entrySet()) {
            if (entry.getValue() >= denseThreshold) {
                firstDenseY = Math.min(firstDenseY, entry.getKey());
                lastDenseY = Math.max(lastDenseY, entry.getKey());
            }
        }
        if (firstDenseY == Integer.MAX_VALUE) {
            return;
        }
        int anchorY = firstDenseY + (lastDenseY - firstDenseY) / 2 + 1;
        Map<BlockPos, Set<Direction>> candidates = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : tree.entrySet()) {
            BlockPos support = entry.getKey();
            BlockState state = entry.getValue();
            if ((state.getBlock() != leaves.getBlock() && !canopyMoss.contains(support))
                    || support.getY() < anchorY || support.getY() > anchorY + 1) {
                continue;
            }
            boolean connectedAbove = hasMossAboveOrBeside(support, tree);
            if ((!canopyMoss.contains(support) || support.getY() == anchorY) && !connectedAbove) {
                continue;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = support.relative(direction).immutable();
                if (!tree.containsKey(side) && ground.isReplaceable(side)
                        && isClearOutside(side, direction, tree)) {
                    candidates.computeIfAbsent(side, ignored -> new LinkedHashSet<>()).add(direction);
                }
            }
        }

        List<BlockPos> starts = new ArrayList<>(candidates.keySet());
        shufflePositions(starts, random);
        starts.sort((left, right) -> Integer.compare(right.getY(), left.getY()));
        List<MossStart> placedStarts = new ArrayList<>();
        for (BlockPos start : starts) {
            if (tree.containsKey(start)
                    || (start.getY() != anchorY && random.nextFloat() >= CANOPY_SIDE_MOSS_CHANCE)) {
                continue;
            }

            List<Direction> clearDirections = new ArrayList<>();
            List<Integer> clearLengths = new ArrayList<>();
            for (Direction direction : candidates.get(start)) {
                int clearLength = 0;
                BlockPos pos = start;
                while (clearLength < 3 && !tree.containsKey(pos) && ground.isReplaceable(pos)
                        && isClearOutside(pos, direction, tree)) {
                    clearLength++;
                    pos = pos.below();
                }
                if (clearLength > 0) {
                    clearDirections.add(direction);
                    clearLengths.add(clearLength);
                }
            }
            if (clearDirections.isEmpty()) {
                continue;
            }

            int directionIndex = random.nextInt(clearDirections.size());
            int maxHangingLength = Math.min(2, clearLengths.get(directionIndex) - 1);
            int hangingLength = chooseHangingLength(start, maxHangingLength, placedStarts, random);
            if (hangingLength < 0) {
                continue;
            }
            placedStarts.add(new MossStart(start, hangingLength));
            BlockPos pos = start;
            for (int i = 0; i <= hangingLength; i++) {
                tree.put(pos.immutable(), Blocks.MOSS_BLOCK.defaultBlockState());
                pos = pos.below();
            }
        }
    }

    private static int chooseHangingLength(
            BlockPos start,
            int maxLength,
            List<MossStart> placedStarts,
            RandomSource random
    ) {
        int[] weights = {1, 2, 3};
        List<Integer> options = new ArrayList<>();
        for (int length = 0; length <= maxLength; length++) {
            boolean conflicts = false;
            for (MossStart placed : placedStarts) {
                if (areNearbyMossStarts(start, placed.pos()) && placed.hangingLength() == length) {
                    conflicts = true;
                    break;
                }
            }
            if (!conflicts) {
                options.add(length);
            }
        }

        if (options.isEmpty()) {
            return -1;
        }

        int totalWeight = 0;
        for (int length : options) {
            totalWeight += weights[length];
        }
        int roll = random.nextInt(totalWeight);
        for (int length : options) {
            roll -= weights[length];
            if (roll < 0) {
                return length;
            }
        }
        return options.getLast();
    }

    private static boolean areNearbyMossStarts(BlockPos first, BlockPos second) {
        int horizontalDistance = Math.abs(first.getX() - second.getX())
                + Math.abs(first.getZ() - second.getZ());
        return first.getY() == second.getY() && horizontalDistance == 1;
    }

    private static boolean hasMossAboveOrBeside(BlockPos pos, Map<BlockPos, BlockState> tree) {
        BlockPos above = pos.above();
        if (tree.getOrDefault(above, Blocks.AIR.defaultBlockState()).is(Blocks.MOSS_BLOCK)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (tree.getOrDefault(above.relative(direction), Blocks.AIR.defaultBlockState()).is(Blocks.MOSS_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClearOutside(
            BlockPos pos,
            Direction direction,
            Map<BlockPos, BlockState> tree
    ) {
        for (int distance = 1; distance <= 2; distance++) {
            if (tree.containsKey(pos.relative(direction, distance))) {
                return false;
            }
        }
        return true;
    }

    private static List<CanopyPool> addCanopyPools(
            Map<BlockPos, BlockState> tree,
            BlockState leaves,
            BlockState trunk,
            Set<BlockPos> mossBlocks,
            int radius,
            RandomSource random
    ) {
        List<BlockPos> candidates = new ArrayList<>(mossBlocks);
        shufflePositions(candidates, random);
        List<CanopyPool> pools = new ArrayList<>();
        Map<CanopyColumn, BlockPos> mossSurface = new LinkedHashMap<>();
        for (BlockPos moss : mossBlocks) {
            mossSurface.put(new CanopyColumn(moss.getX(), moss.getZ()), moss);
        }
        int targetPools = 2 + radius / 24 + random.nextInt(2);

        for (BlockPos first : candidates) {
            if (pools.size() >= targetPools) {
                break;
            }
            if (!tree.getOrDefault(first, Blocks.AIR.defaultBlockState()).is(Blocks.MOSS_BLOCK)) {
                continue;
            }
            if (tooClose(pools.stream().map(CanopyPool::center).toList(), first, 100.0)) {
                continue;
            }

            List<BlockPos> waterTiles = new ArrayList<>();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > 5) {
                        continue;
                    }
                    BlockPos tile = mossSurface.get(new CanopyColumn(
                            first.getX() + dx, first.getZ() + dz));
                    if (tile == null || Math.abs(tile.getY() - first.getY()) > 1
                            || !tree.getOrDefault(tile, Blocks.AIR.defaultBlockState()).is(Blocks.MOSS_BLOCK)) {
                        continue;
                    }
                    if (distanceSquared <= 2 || random.nextFloat() < 0.62F) {
                        waterTiles.add(tile);
                    }
                }
            }
            if (waterTiles.size() < 5) {
                continue;
            }

            boolean supported = true;
            for (BlockPos tile : waterTiles) {
                BlockState below = tree.get(tile.below());
                if (below == null || (below.getBlock() != leaves.getBlock()
                        && below.getBlock() != trunk.getBlock()
                        && !below.is(Blocks.MOSS_BLOCK))) {
                    supported = false;
                    break;
                }
            }
            if (!supported) {
                continue;
            }

            for (BlockPos tile : waterTiles) {
                tree.put(tile.immutable(), Blocks.WATER.defaultBlockState());
                mossBlocks.remove(tile);
            }
            pools.add(new CanopyPool(first.immutable(), List.copyOf(waterTiles)));
        }
        return pools;
    }

    private static void addCanopyPlants(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            Set<BlockPos> mossBlocks,
            List<CanopyPool> pools,
            RandomSource random
    ) {
        Set<BlockPos> plantedSoils = new LinkedHashSet<>();
        for (CanopyPool pool : pools) {
            List<BlockPos> banks = new ArrayList<>();
            for (BlockPos tile : pool.waterTiles()) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos soil = tile.relative(direction);
                    if (mossBlocks.contains(soil) && plantedSoils.add(soil)) {
                        banks.add(soil);
                    }
                }
            }

            shufflePositions(banks, random);
            int dripleafTarget = Math.min(banks.size(), 3 + random.nextInt(3));
            int dripleavesPlaced = 0;
            for (BlockPos soil : banks) {
                if (dripleavesPlaced >= dripleafTarget) {
                    break;
                }
                BlockPos lowerPos = soil.above();
                BlockPos upperPos = lowerPos.above();
                if (tree.containsKey(lowerPos) || tree.containsKey(upperPos)
                        || !ground.isReplaceable(lowerPos) || !ground.isReplaceable(upperPos)) {
                    continue;
                }
                Direction facing = switch (random.nextInt(4)) {
                    case 0 -> Direction.NORTH;
                    case 1 -> Direction.EAST;
                    case 2 -> Direction.SOUTH;
                    default -> Direction.WEST;
                };
                BlockState dripleaf = Blocks.SMALL_DRIPLEAF.defaultBlockState()
                        .setValue(SmallDripleafBlock.FACING, facing);
                tree.put(lowerPos.immutable(), dripleaf);
                tree.put(upperPos.immutable(), dripleaf.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                plantedSoils.add(soil);
                dripleavesPlaced++;
            }

            List<BlockPos> vegetationPatch = new ArrayList<>();
            for (BlockPos soil : mossBlocks) {
                int dx = soil.getX() - pool.center().getX();
                int dz = soil.getZ() - pool.center().getZ();
                if (dx * dx + dz * dz <= 36 && Math.abs(soil.getY() - pool.center().getY()) <= 2
                        && !plantedSoils.contains(soil)) {
                    vegetationPatch.add(soil);
                }
            }
            shufflePositions(vegetationPatch, random);
            int vegetationTarget = Math.min(vegetationPatch.size(), 6 + random.nextInt(5));
            int vegetationPlaced = 0;
            for (BlockPos soil : vegetationPatch) {
                if (vegetationPlaced >= vegetationTarget) {
                    break;
                }
                BlockPos plantPos = soil.above();
                if (tree.containsKey(plantPos) || !ground.isReplaceable(plantPos)) {
                    continue;
                }

                float plantChoice = random.nextFloat();
                BlockState plant = plantChoice < 0.38F ? Blocks.SHORT_GRASS.defaultBlockState()
                        : plantChoice < 0.72F ? Blocks.FERN.defaultBlockState()
                        : Blocks.PINK_PETALS.defaultBlockState();
                tree.put(plantPos.immutable(), plant);
                plantedSoils.add(soil);
                vegetationPlaced++;
            }
        }
    }

    private static void shufflePositions(List<BlockPos> positions, RandomSource random) {
        for (int i = positions.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos temp = positions.get(i);
            positions.set(i, positions.get(j));
            positions.set(j, temp);
        }
    }

    private static void addVanillaVines(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            BlockPos origin,
            BlockState trunk,
            BlockState leaves,
            int trunkHeight,
            int trunkBaseRadius,
            int radius,
            RandomSource random
    ) {
        Map<BlockPos, VineAnchor> trunkCandidates = new LinkedHashMap<>();
        Map<BlockPos, VineAnchor> leafCandidates = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : tree.entrySet()) {
            BlockPos support = entry.getKey();
            BlockState supportState = entry.getValue();
            boolean isTrunk = supportState.getBlock() == trunk.getBlock()
                    && support.getY() >= origin.getY() + 3
                    && support.getY() <= origin.getY() + trunkHeight - 2
                    && Math.hypot(support.getX() - origin.getX(), support.getZ() - origin.getZ())
                    <= trunkBaseRadius + 0.5;
            boolean isLeaf = supportState.getBlock() == leaves.getBlock();
            if (!isTrunk && !isLeaf) {
                continue;
            }

            for (Direction outward : Direction.Plane.HORIZONTAL) {
                BlockPos vinePos = support.relative(outward);
                if (tree.containsKey(vinePos) || !ground.isReplaceable(vinePos)) {
                    continue;
                }
                VineAnchor anchor = new VineAnchor(vinePos, VineBlock.getPropertyForFace(outward.getOpposite()));
                if (isTrunk) {
                    trunkCandidates.putIfAbsent(vinePos, anchor);
                } else {
                    leafCandidates.putIfAbsent(vinePos, anchor);
                }
            }
        }

        List<VineAnchor> trunkAnchors = new ArrayList<>(trunkCandidates.values());
        shuffle(trunkAnchors, random);
        List<BlockPos> usedAnchors = new ArrayList<>();
        int trunkVineCount = (3 + random.nextInt(4)) * 3;
        for (VineAnchor anchor : trunkAnchors) {
            if (usedAnchors.size() >= trunkVineCount) {
                break;
            }
            if (tooClose(usedAnchors, anchor.pos(), 16.0)) {
                continue;
            }
            if (placeVanillaVineColumn(tree, ground, anchor, 2 + random.nextInt(5))) {
                usedAnchors.add(anchor.pos());
            }
        }

        List<VineAnchor> leafAnchors = new ArrayList<>(leafCandidates.values());
        shuffle(leafAnchors, random);
        usedAnchors.clear();
        int leafVineCount = (5 + radius / 10 + random.nextInt(5)) * 3;
        for (VineAnchor anchor : leafAnchors) {
            if (usedAnchors.size() >= leafVineCount) {
                break;
            }
            if (tooClose(usedAnchors, anchor.pos(), 25.0)) {
                continue;
            }
            if (placeVanillaVineColumn(tree, ground, anchor, 2 + random.nextInt(4))) {
                usedAnchors.add(anchor.pos());
            }
        }
    }

    private static boolean placeVanillaVineColumn(
            Map<BlockPos, BlockState> tree,
            GroundProfile ground,
            VineAnchor anchor,
            int length
    ) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(anchor.facing(), true);
        BlockPos pos = anchor.pos();
        boolean placed = false;
        for (int step = 0; step < length; step++, pos = pos.below()) {
            if (tree.containsKey(pos) || !ground.isReplaceable(pos)) {
                break;
            }
            tree.put(pos.immutable(), vine);
            placed = true;
        }
        return placed;
    }

    private static boolean tooClose(List<BlockPos> anchors, BlockPos candidate, double minDistanceSqr) {
        return anchors.stream().anyMatch(other -> other.distSqr(candidate) < minDistanceSqr);
    }

    private static void shuffle(List<VineAnchor> anchors, RandomSource random) {
        for (int i = anchors.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            VineAnchor temp = anchors.get(i);
            anchors.set(i, anchors.get(j));
            anchors.set(j, temp);
        }
    }

    private static void put(Map<BlockPos, BlockState> tree, BlockPos pos, BlockState state) {
        tree.put(pos.immutable(), state);
    }

    private static void putIfAbsent(Map<BlockPos, BlockState> tree, BlockPos pos, BlockState state) {
        tree.putIfAbsent(pos.immutable(), state);
    }

    private record RootTip(int radius, int y) {
    }

    private record CanopyColumn(int x, int z) {
    }

    private record CanopyPool(BlockPos center, List<BlockPos> waterTiles) {
    }

    private record MossStart(BlockPos pos, int hangingLength) {
    }

    private record VineAnchor(BlockPos pos, BooleanProperty facing) {
    }

    private record PendingLeafVines(
            List<BlockPos> candidates,
            List<BlockPos> expandedCandidates,
            List<BlockPos> baseSelection,
            int target,
            long seed
    ) {
    }
}
