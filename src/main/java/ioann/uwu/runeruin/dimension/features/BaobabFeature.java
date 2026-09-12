package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A thick-trunked baobab with buttress roots, mossy canopy biomes, and crowns made from curved branches. */
public class BaobabFeature extends Feature<BaobabFeature.Config> {
    private static final int GROUND_SCAN_STEPS = 32;
    private static final double MIN_HEIGHT_PER_RADIUS = 1.8;
    private static final double MAX_HEIGHT_PER_RADIUS = 2.1;

    public BaobabFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos requestedOrigin = ctx.origin();
        FeatureChunkBounds chunkBounds = new FeatureChunkBounds(requestedOrigin);
        BlockPos ground = findGround(level, chunkBounds, requestedOrigin.getX(), requestedOrigin.getZ(),
                requestedOrigin.getY() - 1);
        if (ground == null) {
            return false;
        }
        BlockPos origin = ground.above();

        RandomSource random = ctx.random();
        int radius = ctx.config().radius.sample(random);
        int minHeight = (int) Math.ceil(radius * MIN_HEIGHT_PER_RADIUS);
        int maxHeight = (int) Math.floor(radius * MAX_HEIGHT_PER_RADIUS);
        int height = random.nextInt(minHeight, maxHeight + 1);
        int trunkHeight = (int) Math.round(height * 0.69);
        int trunkBaseRadius = Math.max(5, (int) Math.round(radius * 0.22));
        int trunkTopRadius = Math.max(3, (int) Math.round(trunkBaseRadius * 0.72));
        int trunkCapHeight = Math.max(3, trunkTopRadius);
        int trunkApexY = trunkHeight + trunkCapHeight;
        int crownVerticalRadius = Math.max(4, (int) Math.round(radius * 0.18));

        BlockState trunk = ctx.config().trunkBlock.getState(level, random, origin);
        BlockState leaves = ctx.config().leavesBlock.getState(level, random, origin)
                .trySetValue(LeavesBlock.PERSISTENT, true);
        Map<BlockPos, BlockState> tree = new LinkedHashMap<>();

        addTrunk(tree, level, chunkBounds, origin, trunk, trunkHeight, trunkBaseRadius, trunkTopRadius);
        addRoundedTrunkCap(tree, origin, trunk, trunkHeight, trunkTopRadius, trunkCapHeight);
        addButtressRoots(tree, level, chunkBounds, origin, trunk, trunkBaseRadius, radius, random);

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
            int centerY = baseCenterY + random.nextInt(7) - 3;

            addCurvedBranch(tree, origin, trunk, dx, dz, branchRadius, startY, crownOffset, centerY,
                    branchBaseWidth, 2);

            double crownRadius = radius * (0.33 + random.nextDouble() * 0.05);
            double crownDepth = crownRadius * (0.86 + random.nextDouble() * 0.12);
            addCrown(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
            addHangingLeafVines(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
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
            int centerY = baseCenterY + random.nextInt(7) - 3;
            int rise = Math.max(2, centerY - startY);
            int endRadius = upperStartRadius + rise;
            int centerX = (int) Math.round(dx * endRadius);
            int centerZ = (int) Math.round(dz * endRadius);

            addCurvedBranch(tree, origin, trunk, dx, dz, upperStartRadius, startY, endRadius, centerY,
                    Math.max(2, branchBaseWidth - 1), 2);

            double crownRadius = radius * (0.25 + random.nextDouble() * 0.04);
            double crownDepth = crownRadius * (0.86 + random.nextDouble() * 0.12);
            addCrown(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
            addHangingLeafVines(tree, origin, leaves, centerX, centerY, centerZ,
                    crownRadius, crownDepth, crownVerticalRadius, random);
        }

        addCanopyMossBiome(tree, level, chunkBounds, leaves, trunk, radius, random);
        addVanillaVines(tree, level, chunkBounds, origin, trunk, leaves, trunkHeight, trunkBaseRadius, radius, random);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        var iterator = tree.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (!chunkBounds.contains(pos) || level.isOutsideBuildHeight(pos.getY())) {
                iterator.remove();
                continue;
            }
            if (!level.ensureCanWrite(pos)) {
                iterator.remove();
                continue;
            }

            BlockState existing = level.getBlockState(pos);
            if (!canTreeReplace(existing)) {
                if (isOptionalTreeDecoration(entry.getValue(), leaves)) {
                    iterator.remove();
                } else {
                    return false;
                }
            }
        }

        for (Map.Entry<BlockPos, BlockState> entry : tree.entrySet()) {
            BlockPos pos = entry.getKey();
            if (chunkBounds.contains(pos) && level.ensureCanWrite(pos)) {
                level.setBlock(mutable.set(pos), entry.getValue(), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    private static void addButtressRoots(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
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
            RootTip tip = findRootTip(level, chunkBounds, origin, dx, dz, startRadius, startY);
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
            if (tree.containsKey(pos) || !chunkBounds.contains(pos)
                    || level.isOutsideBuildHeight(pos.getY()) || !level.ensureCanWrite(pos)) {
                continue;
            }

            BlockState existing = level.getBlockState(pos);
            if (canTreeReplace(existing)) {
                tree.put(pos, entry.getValue());
            }
        }
    }

    private static RootTip findRootTip(
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
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
            BlockPos ground = findGround(level, chunkBounds, origin.getX() + endX, origin.getZ() + endZ,
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
        BlockPos ground = findGround(level, chunkBounds, origin.getX() + endX, origin.getZ() + endZ,
                origin.getY() + startY);
        if (ground == null) {
            return null;
        }
        endY = Math.min(startY - 1, ground.getY() + 1 - origin.getY());
        return new RootTip(endRadius, endY);
    }

    private static BlockPos findGround(
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
            int x,
            int z,
            int startY
    ) {
        if (!chunkBounds.contains(new BlockPos(x, startY, z))) {
            return null;
        }

        for (int distance = 0; distance <= GROUND_SCAN_STEPS; distance++) {
            int y = startY - distance;
            if (level.isOutsideBuildHeight(y)) {
                break;
            }

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                continue;
            }
            if (state.isFaceSturdy(level, pos, Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canTreeReplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS);
    }

    private static void addTrunk(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
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
                BlockPos ground = findGround(level, chunkBounds, origin.getX() + x, origin.getZ() + z, groundScanStartY);
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
            RandomSource random
    ) {
        List<BlockPos> candidates = new ArrayList<>();
        int boundX = (int) Math.ceil(radiusX);
        int boundZ = (int) Math.ceil(radiusZ);
        for (int x = -boundX; x <= boundX; x++) {
            for (int z = -boundZ; z <= boundZ; z++) {
                double horizontalDistance = x * x / (radiusX * radiusX) + z * z / (radiusZ * radiusZ);
                if (horizontalDistance < 0.22 || horizontalDistance > 0.88) {
                    continue;
                }

                int drop = (int) Math.ceil(radiusY * Math.sqrt(Math.max(0.0, 1.0 - horizontalDistance)));
                BlockPos attach = origin.offset(centerX + x, centerY - drop, centerZ + z);
                BlockState at = tree.get(attach);
                if (at == null || at.getBlock() != leaves.getBlock() || tree.containsKey(attach.below())) {
                    continue;
                }
                candidates.add(attach);
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

    private static void addCanopyMossBiome(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
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
            if (tree.containsKey(above) || level.isOutsideBuildHeight(above.getY())
                    || !chunkBounds.contains(above) || !level.ensureCanWrite(above)
                    || !canTreeReplace(level.getBlockState(above))) {
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
            if (nearbySurface >= 3) {
                surfaceLeaves.add(leaf);
            }
        }

        Set<BlockPos> mossCoveredLeaves = new LinkedHashSet<>();
        Set<BlockPos> mossBlocks = new LinkedHashSet<>();
        List<BlockPos> orderedSurface = new ArrayList<>(surfaceLeaves);
        shufflePositions(orderedSurface, random);
        for (BlockPos leaf : orderedSurface) {
            BlockPos floor = leaf.above();
            int adjacentMoss = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborLeaf = highestLeaves.get(new CanopyColumn(
                        leaf.getX() + direction.getStepX(), leaf.getZ() + direction.getStepZ()));
                if (neighborLeaf != null && Math.abs(neighborLeaf.getY() - leaf.getY()) <= 1
                        && mossCoveredLeaves.contains(neighborLeaf)) {
                    adjacentMoss++;
                }
            }

            float coverageChance = adjacentMoss > 0 ? 0.82F : 0.42F;
            if (random.nextFloat() >= coverageChance) {
                continue;
            }

            mossCoveredLeaves.add(leaf);
            if (random.nextFloat() < 0.38F) {
                tree.put(floor.immutable(), Blocks.MOSS_BLOCK.defaultBlockState());
                mossBlocks.add(floor.immutable());
            } else {
                tree.put(floor.immutable(), Blocks.MOSS_CARPET.defaultBlockState());
            }
        }

        List<BlockPos> pools = addCanopyPools(tree, level, chunkBounds, leaves, trunk, highestLeaves,
                mossBlocks, radius, random);
        addCanopyPlants(tree, level, chunkBounds, mossBlocks, pools, random);
    }

    private static List<BlockPos> addCanopyPools(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
            BlockState leaves,
            BlockState trunk,
            Map<CanopyColumn, BlockPos> highestLeaves,
            Set<BlockPos> mossBlocks,
            int radius,
            RandomSource random
    ) {
        List<BlockPos> candidates = new ArrayList<>(mossBlocks);
        shufflePositions(candidates, random);
        List<BlockPos> pools = new ArrayList<>();
        int targetPools = 2 + radius / 12 + random.nextInt(3);

        for (BlockPos first : candidates) {
            if (pools.size() >= targetPools) {
                break;
            }
            if (!tree.getOrDefault(first, Blocks.AIR.defaultBlockState()).is(Blocks.MOSS_BLOCK)) {
                continue;
            }
            if (tooClose(pools, first, 36.0)) {
                continue;
            }

            List<BlockPos> waterTiles = new ArrayList<>();
            waterTiles.add(first);
            if (random.nextFloat() < 0.4F) {
                List<Direction> directions = List.of(
                        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
                int directionOffset = random.nextInt(directions.size());
                for (int i = 0; i < directions.size(); i++) {
                    BlockPos second = first.relative(directions.get((directionOffset + i) % directions.size()));
                    BlockPos support = highestLeaves.get(new CanopyColumn(second.getX(), second.getZ()));
                    if (mossBlocks.contains(second) && support != null && support.getY() == second.getY() - 1) {
                        waterTiles.add(second);
                        break;
                    }
                }
            }

            Set<BlockPos> waterTileSet = new LinkedHashSet<>(waterTiles);
            Map<BlockPos, Direction> rim = new LinkedHashMap<>();
            for (BlockPos tile : waterTiles) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos edge = tile.relative(direction);
                    if (!waterTileSet.contains(edge)) {
                        rim.putIfAbsent(edge, direction.getOpposite());
                    }
                }
            }

            Set<BlockPos> rimMoss = new LinkedHashSet<>();
            boolean enclosed = true;
            for (Map.Entry<BlockPos, Direction> edgeEntry : rim.entrySet()) {
                BlockPos edge = edgeEntry.getKey();
                BlockState planned = tree.get(edge);
                if (planned != null) {
                    if (planned.is(Blocks.MOSS_BLOCK) || planned.is(Blocks.MOSS_CARPET)) {
                        rimMoss.add(edge);
                    } else if (planned.getBlock() != leaves.getBlock() && planned.getBlock() != trunk.getBlock()) {
                        enclosed = false;
                        break;
                    }
                    continue;
                }

                if (!chunkBounds.contains(edge) || level.isOutsideBuildHeight(edge.getY())
                        || !level.ensureCanWrite(edge)) {
                    enclosed = false;
                    break;
                }
                BlockState existing = level.getBlockState(edge);
                if (!canTreeReplace(existing)) {
                    if (!existing.isFaceSturdy(level, edge, edgeEntry.getValue())) {
                        enclosed = false;
                        break;
                    }
                    continue;
                }

                BlockPos supportPos = edge.below();
                BlockState plannedSupport = tree.get(supportPos);
                BlockPos surfaceSupport = highestLeaves.get(new CanopyColumn(edge.getX(), edge.getZ()));
                boolean supported = plannedSupport != null && !plannedSupport.isAir()
                        || surfaceSupport != null && surfaceSupport.getY() == supportPos.getY()
                        || chunkBounds.contains(supportPos)
                        && level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP);
                if (!supported) {
                    enclosed = false;
                    break;
                }
                rimMoss.add(edge);
            }

            if (!enclosed) {
                continue;
            }
            for (BlockPos edge : rimMoss) {
                tree.put(edge.immutable(), Blocks.MOSS_BLOCK.defaultBlockState());
                mossBlocks.add(edge.immutable());
            }
            for (BlockPos tile : waterTiles) {
                tree.put(tile.immutable(), Blocks.WATER.defaultBlockState());
                mossBlocks.remove(tile);
            }
            pools.add(first);
        }
        return pools;
    }

    private static void addCanopyPlants(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
            Set<BlockPos> mossBlocks,
            List<BlockPos> pools,
            RandomSource random
    ) {
        for (BlockPos pool : pools) {
            if (random.nextFloat() >= 0.45F) {
                continue;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos soil = pool.relative(direction);
                if (!mossBlocks.contains(soil) || random.nextFloat() >= 0.35F) {
                    continue;
                }
                BlockPos lowerPos = soil.above();
                BlockPos upperPos = lowerPos.above();
                if (tree.containsKey(lowerPos) || tree.containsKey(upperPos)
                        || level.isOutsideBuildHeight(upperPos.getY())
                        || !chunkBounds.contains(lowerPos) || !chunkBounds.contains(upperPos)
                        || !level.ensureCanWrite(upperPos)
                        || !canTreeReplace(level.getBlockState(lowerPos))
                        || !canTreeReplace(level.getBlockState(upperPos))) {
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
                break;
            }
        }

        for (BlockPos soil : mossBlocks) {
            if (random.nextFloat() >= 0.18F) {
                continue;
            }
            BlockPos plantPos = soil.above();
            if (tree.containsKey(plantPos) || level.isOutsideBuildHeight(plantPos.getY())
                    || !chunkBounds.contains(plantPos) || !level.ensureCanWrite(plantPos)
                    || !canTreeReplace(level.getBlockState(plantPos))) {
                continue;
            }

            float plantChoice = random.nextFloat();
            BlockState plant = plantChoice < 0.12F ? Blocks.FLOWERING_AZALEA.defaultBlockState()
                    : plantChoice < 0.28F ? Blocks.AZALEA.defaultBlockState()
                    : plantChoice < 0.57F ? Blocks.SHORT_GRASS.defaultBlockState()
                    : plantChoice < 0.78F ? Blocks.FERN.defaultBlockState()
                    : Blocks.PINK_PETALS.defaultBlockState();
            tree.put(plantPos.immutable(), plant);
        }
    }

    private static boolean isOptionalTreeDecoration(BlockState state, BlockState leaves) {
        Block block = state.getBlock();
        return block == leaves.getBlock()
                || block == Blocks.VINE
                || block == Blocks.MOSS_BLOCK
                || block == Blocks.MOSS_CARPET
                || block == Blocks.WATER
                || block == Blocks.AZALEA
                || block == Blocks.FLOWERING_AZALEA
                || block == Blocks.SHORT_GRASS
                || block == Blocks.FERN
                || block == Blocks.PINK_PETALS
                || block == Blocks.SMALL_DRIPLEAF;
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
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
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
                if (tree.containsKey(vinePos) || !chunkBounds.contains(vinePos)
                        || !canTreeReplace(level.getBlockState(vinePos))) {
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
            if (placeVanillaVineColumn(tree, level, chunkBounds, anchor, 2 + random.nextInt(5))) {
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
            if (placeVanillaVineColumn(tree, level, chunkBounds, anchor, 2 + random.nextInt(4))) {
                usedAnchors.add(anchor.pos());
            }
        }
    }

    private static boolean placeVanillaVineColumn(
            Map<BlockPos, BlockState> tree,
            WorldGenLevel level,
            FeatureChunkBounds chunkBounds,
            VineAnchor anchor,
            int length
    ) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(anchor.facing(), true);
        BlockPos pos = anchor.pos();
        boolean placed = false;
        for (int step = 0; step < length; step++, pos = pos.below()) {
            if (tree.containsKey(pos) || !chunkBounds.contains(pos) || level.isOutsideBuildHeight(pos.getY())
                    || !canTreeReplace(level.getBlockState(pos))) {
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

    private record VineAnchor(BlockPos pos, BooleanProperty facing) {
    }

    public record Config(BlockStateProvider trunkBlock, BlockStateProvider leavesBlock, IntProvider radius)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                BlockStateProvider.CODEC.fieldOf("leaves_block").forGetter(Config::leavesBlock),
                IntProviders.codec(20, 40).fieldOf("radius").forGetter(Config::radius)
        ).apply(codec, Config::new));
    }
}
