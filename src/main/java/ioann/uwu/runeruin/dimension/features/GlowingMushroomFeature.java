package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlowingMushroomFeature extends Feature<GlowingMushroomFeature.Config> {

    private static final double CAP_SHELL_THICKNESS = 3.0;
    private static final double MIN_TILT_DEGREES = 10.0;
    private static final double MAX_TILT_DEGREES = 18.0;
    private static final double MIN_TWIN_TILT_DEGREES = 27.0;
    private static final double MAX_TWIN_TILT_DEGREES = 36.0;
    private static final float TWIN_CHANCE = 0.25F;

    public GlowingMushroomFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos base = context.origin();
        RandomSource random = context.random();

        BlockPos floorPos = base.below();
        if (!level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP)) {
            return false;
        }

        Set<BlockPos> placedStem = new HashSet<>();
        Set<BlockPos> placedCap = new HashSet<>();
        boolean builtTwin = random.nextFloat() < TWIN_CHANCE
                && tryBuildTwin(level, context.config(), base, random, placedStem, placedCap);
        if (!builtTwin) {
            MushroomShape mushroom = createMushroom(
                    level, context.config(), base, context.config().stemHeight().sample(random), random,
                    sampleTilt(random, MIN_TILT_DEGREES, MAX_TILT_DEGREES)
            );
            placedStem.addAll(mushroom.stem());
            placedCap.addAll(mushroom.cap());
        }

        // Caps take precedence where tilted or paired stems cross the solid cap volume.
        placedStem.removeAll(placedCap);
        if (!canPlace(level, new ArrayList<>(placedStem)) || !canPlace(level, new ArrayList<>(placedCap))) {
            return false;
        }

        BlockState stemState = context.config().stem().getState(level, random, base);
        BlockState capState = context.config().cap().getState(level, random, base);
        for (BlockPos pos : placedStem) {
            level.setBlock(pos, stemState, GeometryUtils.BULK_FLAG);
        }
        for (BlockPos pos : placedCap) {
            level.setBlock(pos, capState, GeometryUtils.BULK_FLAG);
        }
        return true;
    }

    private static boolean tryBuildTwin(
            WorldGenLevel level,
            Config config,
            BlockPos base,
            RandomSource random,
            Set<BlockPos> stems,
            Set<BlockPos> caps
    ) {
        double splitDirection = random.nextDouble() * Math.PI * 2.0;
        int separation = 1 + random.nextInt(2);
        int offsetX = (int) Math.round(Math.cos(splitDirection) * separation);
        int offsetZ = (int) Math.round(Math.sin(splitDirection) * separation);
        BlockPos firstBase = base.offset(-offsetX, 0, -offsetZ);
        BlockPos secondBase = base.offset(offsetX, 0, offsetZ);
        if (!hasFloorSupport(level, firstBase) || !hasFloorSupport(level, secondBase)) {
            return false;
        }

        int firstHeight = config.stemHeight().sample(random);
        int secondHeight = config.stemHeight().sample(random);

        double directionJitter = Math.toRadians(35.0);
        Tilt firstTilt = sampleTilt(
                random,
                MIN_TWIN_TILT_DEGREES,
                MAX_TWIN_TILT_DEGREES,
                splitDirection + Math.PI + (random.nextDouble() - 0.5) * directionJitter
        );
        Tilt secondTilt = sampleTilt(
                random,
                MIN_TWIN_TILT_DEGREES,
                MAX_TWIN_TILT_DEGREES,
                splitDirection + (random.nextDouble() - 0.5) * directionJitter
        );

        MushroomShape first = createMushroom(level, config, firstBase, firstHeight, random, firstTilt);
        MushroomShape second = createMushroom(level, config, secondBase, secondHeight, random, secondTilt);
        stems.addAll(first.stem());
        stems.addAll(second.stem());
        caps.addAll(first.cap());
        caps.addAll(second.cap());
        return true;
    }

    private static boolean hasFloorSupport(WorldGenLevel level, BlockPos base) {
        BlockPos floorPos = base.below();
        return level.getBlockState(floorPos).isFaceSturdy(level, floorPos, Direction.UP);
    }

    private static MushroomShape createMushroom(
            WorldGenLevel level,
            Config config,
            BlockPos base,
            int stemHeight,
            RandomSource random,
            Tilt tilt
    ) {
        int capDiameter = config.capDiameter().sample(random);
        int capHeight = (capDiameter + 1) / 2;
        int stemBaseDiameter = 5 + random.nextInt(2);
        int stemTopDiameter = 3 + random.nextInt(2);
        int stemWaistDiameter = Math.min(
                config.stemWaistDiameter().sample(random),
                Math.min(stemBaseDiameter, stemTopDiameter) - 1
        );
        stemWaistDiameter = Math.max(2, stemWaistDiameter);

        List<BlockPos> stem = createStemPositions(
                base,
                stemHeight,
                capHeight,
                capDiameter,
                stemBaseDiameter,
                stemWaistDiameter,
                stemTopDiameter
        );
        CapGeometry cap = createCapGeometry(base, stemHeight, capDiameter, capHeight);

        Set<BlockPos> placedStem = new HashSet<>(tiltPositions(stem, base, capDiameter, tilt));
        Set<BlockPos> placedCap = new HashSet<>(tiltPositions(cap.outer(), base, capDiameter, tilt));
        fillCapSlices(placedCap);
        Set<BlockPos> cavity = new HashSet<>(tiltPositions(cap.cavity(), base, capDiameter, tilt));
        placedCap.removeAll(cavity);
        Set<BlockPos> crown = new HashSet<>(tiltPositions(cap.crown(), base, capDiameter, tilt));
        placedStem.removeAll(placedCap);
        keepLowestConnectedComponent(placedStem);
        keepLargestConnectedComponent(placedCap);
        crown.retainAll(placedCap);
        connectStemToCrown(placedStem, placedCap, crown);
        adaptStemBaseToTerrain(level, placedStem, placedCap, base);
        return new MushroomShape(placedStem, placedCap);
    }

    private static void adaptStemBaseToTerrain(
            WorldGenLevel level,
            Set<BlockPos> stem,
            Set<BlockPos> cap,
            BlockPos base
    ) {
        Map<BlockPos, BlockPos> loweredByOriginal = new HashMap<>();
        Map<BlockPos, BlockPos> originalByLowered = new HashMap<>();
        for (BlockPos pos : stem) {
            if (pos.getY() > base.getY() + 1 || stem.contains(pos.below())) {
                continue;
            }

            BlockPos lowered = pos.below();
            BlockPos support = lowered.below();
            if (stem.contains(lowered)
                    || cap.contains(lowered)
                    || level.isOutsideBuildHeight(lowered)
                    || level.isOutsideBuildHeight(support)
                    || !level.ensureCanWrite(lowered)
                    || !isAirLike(level, lowered)) {
                continue;
            }

            BlockState supportState = level.getBlockState(support);
            if (supportState.isFaceSturdy(level, support, Direction.UP)) {
                loweredByOriginal.put(pos, lowered);
                originalByLowered.put(lowered, pos);
            }
        }

        if (loweredByOriginal.isEmpty()) {
            return;
        }

        Set<BlockPos> fixedStem = new HashSet<>(stem);
        fixedStem.removeAll(loweredByOriginal.keySet());
        Set<BlockPos> adjustedStem = new HashSet<>(fixedStem);
        Set<BlockPos> remaining = new HashSet<>(originalByLowered.keySet());
        Deque<BlockPos> queue = new ArrayDeque<>();

        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> loweredComponent = new HashSet<>();
            queue.add(start);
            remaining.remove(start);
            while (!queue.isEmpty()) {
                BlockPos lowered = queue.removeFirst();
                loweredComponent.add(lowered);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = lowered.relative(direction);
                    if (remaining.remove(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }

            List<BlockPos> originals = new ArrayList<>();
            boolean touchesFixedStem = false;
            BlockPos anchor = null;
            for (BlockPos lowered : loweredComponent) {
                BlockPos original = originalByLowered.get(lowered);
                originals.add(original);
                if (isAdjacentTo(fixedStem, lowered)) {
                    touchesFixedStem = true;
                }
                if (anchor == null && isAdjacentTo(fixedStem, original)) {
                    anchor = original;
                }
            }

            if (touchesFixedStem) {
                adjustedStem.addAll(loweredComponent);
            } else if (anchor != null) {
                // Keep one transition block where needed to connect the lowered foot to the stem.
                adjustedStem.addAll(loweredComponent);
                adjustedStem.add(anchor);
            } else {
                adjustedStem.addAll(originals);
            }
        }

        stem.clear();
        stem.addAll(adjustedStem);
    }

    private static boolean isAdjacentTo(Set<BlockPos> blocks, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (blocks.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static Tilt sampleTilt(RandomSource random, double minDegrees, double maxDegrees) {
        return sampleTilt(random, minDegrees, maxDegrees, random.nextDouble() * Math.PI * 2.0);
    }

    private static Tilt sampleTilt(RandomSource random, double minDegrees, double maxDegrees, double direction) {
        double angle = Math.toRadians(minDegrees + random.nextDouble() * (maxDegrees - minDegrees));
        return new Tilt(angle, direction);
    }

    private static List<BlockPos> tiltPositions(List<BlockPos> positions, BlockPos base, int capDiameter, Tilt tilt) {
        if (tilt.angleRadians() == 0.0) {
            return new ArrayList<>(positions);
        }

        // Even-sized caps are centered half a block toward the negative axes by their source geometry.
        double centerOffset = capDiameter % 2 == 0 ? -0.5 : 0.0;
        double directionX = Math.cos(tilt.directionRadians());
        double directionZ = Math.sin(tilt.directionRadians());
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double cosTilt = Math.cos(tilt.angleRadians());
        double sinTilt = Math.sin(tilt.angleRadians());
        Set<BlockPos> transformed = new HashSet<>();

        for (BlockPos pos : positions) {
            double x = pos.getX() - base.getX() - centerOffset;
            double y = pos.getY() - base.getY() + 0.5;
            double z = pos.getZ() - base.getZ() - centerOffset;

            double alongTilt = x * directionX + z * directionZ;
            double acrossTilt = x * perpendicularX + z * perpendicularZ;
            double rotatedAlong = alongTilt * cosTilt + y * sinTilt;
            double rotatedY = -alongTilt * sinTilt + y * cosTilt;
            double rotatedX = rotatedAlong * directionX + acrossTilt * perpendicularX;
            double rotatedZ = rotatedAlong * directionZ + acrossTilt * perpendicularZ;

            int blockX = (int) Math.floor(centerOffset + 0.5 + rotatedX);
            int blockY = Math.max(0, (int) Math.floor(rotatedY));
            int blockZ = (int) Math.floor(centerOffset + 0.5 + rotatedZ);
            transformed.add(base.offset(blockX, blockY, blockZ));
        }
        return new ArrayList<>(transformed);
    }

    private static void fillCapSlices(Set<BlockPos> cap) {
        boolean changed;
        do {
            changed = fillCapScanLines(cap, true);
            changed |= fillCapScanLines(cap, false);
        } while (changed);
    }

    private static boolean fillCapScanLines(Set<BlockPos> cap, boolean alongX) {
        Map<SliceLine, int[]> spans = new HashMap<>();
        for (BlockPos pos : cap) {
            SliceLine line = new SliceLine(pos.getY(), alongX ? pos.getZ() : pos.getX());
            int coordinate = alongX ? pos.getX() : pos.getZ();
            int[] span = spans.get(line);
            if (span == null) {
                spans.put(line, new int[]{coordinate, coordinate});
            } else {
                span[0] = Math.min(span[0], coordinate);
                span[1] = Math.max(span[1], coordinate);
            }
        }

        boolean changed = false;
        for (Map.Entry<SliceLine, int[]> entry : spans.entrySet()) {
            SliceLine line = entry.getKey();
            int[] span = entry.getValue();
            for (int coordinate = span[0]; coordinate <= span[1]; coordinate++) {
                BlockPos pos = alongX
                        ? new BlockPos(coordinate, line.y(), line.fixed())
                        : new BlockPos(line.fixed(), line.y(), coordinate);
                changed |= cap.add(pos);
            }
        }
        return changed;
    }

    private static void keepLargestConnectedComponent(Set<BlockPos> blocks) {
        if (blocks.size() < 2) {
            return;
        }

        Set<BlockPos> remaining = new HashSet<>(blocks);
        Set<BlockPos> largest = Set.of();
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> component = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            remaining.remove(start);
            queue.addLast(start);

            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                component.add(pos);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = pos.relative(direction);
                    if (remaining.remove(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }

            if (component.size() > largest.size()) {
                largest = component;
            }
        }
        blocks.retainAll(largest);
    }

    private static void keepLowestConnectedComponent(Set<BlockPos> blocks) {
        if (blocks.size() < 2) {
            return;
        }

        Set<BlockPos> remaining = new HashSet<>(blocks);
        Set<BlockPos> lowest = Set.of();
        int lowestY = Integer.MAX_VALUE;
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> component = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            remaining.remove(start);
            queue.addLast(start);
            int componentLowestY = start.getY();

            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                component.add(pos);
                componentLowestY = Math.min(componentLowestY, pos.getY());
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = pos.relative(direction);
                    if (remaining.remove(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }

            if (componentLowestY < lowestY
                    || (componentLowestY == lowestY && component.size() > lowest.size())) {
                lowest = component;
                lowestY = componentLowestY;
            }
        }
        blocks.retainAll(lowest);
    }

    private static void connectStemToCrown(Set<BlockPos> stem, Set<BlockPos> cap, Set<BlockPos> crown) {
        if (stem.isEmpty() || cap.isEmpty() || crown.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : cap) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        for (BlockPos pos : stem) {
            minY = Math.min(minY, pos.getY());
        }

        Deque<BlockPos> queue = new ArrayDeque<>(stem);
        Set<BlockPos> visited = new HashSet<>(stem);
        Map<BlockPos, BlockPos> parents = new HashMap<>();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (crown.contains(neighbor)) {
                    BlockPos path = current;
                    while (!stem.contains(path)) {
                        if (!cap.contains(path)) {
                            stem.add(path);
                        }
                        path = parents.get(path);
                    }
                    return;
                }
                if (neighbor.getX() < minX - 1 || neighbor.getX() > maxX + 1
                        || neighbor.getY() < minY - 1 || neighbor.getY() > maxY + 1
                        || neighbor.getZ() < minZ - 1 || neighbor.getZ() > maxZ + 1
                        || cap.contains(neighbor) || !visited.add(neighbor)) {
                    continue;
                }
                parents.put(neighbor, current);
                queue.addLast(neighbor);
            }
        }
    }

    private static CapGeometry createCapGeometry(
            BlockPos base,
            int stemHeight,
            int diameter,
            int height
    ) {
        List<BlockPos> outer = new ArrayList<>();
        List<BlockPos> cavity = new ArrayList<>();
        List<BlockPos> crown = new ArrayList<>();
        double radius = diameter / 2.0;
        boolean evenDiameter = diameter % 2 == 0;
        int minOffset = -(diameter / 2);
        int maxOffset = (diameter - 1) / 2;

        for (int layer = 0; layer <= height; layer++) {
            double fraction = layer / (double) height;
            double layerRadius = radius * Math.sqrt(Math.max(0.0, 1.0 - fraction * fraction));
            // Keep the apex wide enough to retain a rounded, sealed crown.
            if (evenDiameter) {
                layerRadius = Math.max(layerRadius, 2.0);
            } else if (layer == height) {
                layerRadius = Math.max(layerRadius, Math.sqrt(2.0));
            }

            double innerRadius = Math.max(0.0, layerRadius - CAP_SHELL_THICKNESS);
            double centerOffset = evenDiameter ? 0.5 : 0.0;
            for (int x = minOffset; x <= maxOffset; x++) {
                double centeredX = x + centerOffset;
                for (int z = minOffset; z <= maxOffset; z++) {
                    double centeredZ = z + centerOffset;
                    double distanceSquared = centeredX * centeredX + centeredZ * centeredZ;
                    if (distanceSquared <= layerRadius * layerRadius + 1.0e-6) {
                        BlockPos pos = base.offset(x, stemHeight + layer, z);
                        outer.add(pos);
                        if (layer >= height - 1) {
                            crown.add(pos);
                        } else if (innerRadius > 0.0
                                && distanceSquared < innerRadius * innerRadius - 1.0e-6) {
                            cavity.add(pos);
                        }
                    }
                }
            }
        }
        return new CapGeometry(outer, cavity, crown);
    }

    private record Tilt(double angleRadians, double directionRadians) {
    }

    private record SliceLine(int y, int fixed) {}

    private record CapGeometry(List<BlockPos> outer, List<BlockPos> cavity, List<BlockPos> crown) {}

    private record MushroomShape(Set<BlockPos> stem, Set<BlockPos> cap) {}

    private static List<BlockPos> createStemPositions(
            BlockPos base,
            int height,
            int capHeight,
            int capDiameter,
            int baseDiameter,
            int waistDiameter,
            int topDiameter
    ) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            double progress = height <= 1 ? 0.0 : y / (double) (height - 1);
            double endDiameter = baseDiameter + (topDiameter - baseDiameter) * progress;
            double hourglass = 4.0 * progress * (1.0 - progress);
            double diameterAtLayer = waistDiameter + (endDiameter - waistDiameter) * (1.0 - hourglass);
            int diameter = Math.max(waistDiameter, (int) Math.round(diameterAtLayer));
            addDiscLayer(positions, base, y, diameter);
        }

        // Keep the connector white, then flare the inner stalk upward into the cap's crown.
        boolean evenCap = capDiameter % 2 == 0;
        int capAlignedDiameter = evenCap ? 4 : 3;
        int innerStartDiameter = waistDiameter;
        for (int layer = 0; layer < capHeight; layer++) {
            double progress = layer / (double) (capHeight - 1);
            double easedProgress = progress * progress * (3.0 - 2.0 * progress);
            double diameterAtLayer = innerStartDiameter
                    + (capAlignedDiameter - innerStartDiameter) * easedProgress;
            addCapAlignedStemDiscLayer(
                    positions,
                    base,
                    height + layer,
                    diameterAtLayer,
                    evenCap
            );
        }
        return positions;
    }

    private static void addDiscLayer(List<BlockPos> positions, BlockPos base, int y, int diameter) {
        boolean evenDiameter = diameter % 2 == 0;
        double centerOffset = evenDiameter ? 0.5 : 0.0;
        int minOffset = -(diameter / 2);
        int maxOffset = (diameter - 1) / 2;
        double radius = Math.max(0.5, diameter / 2.0 - 0.25);
        double radiusSquared = radius * radius;

        for (int x = minOffset; x <= maxOffset; x++) {
            double centeredX = x + centerOffset;
            for (int z = minOffset; z <= maxOffset; z++) {
                double centeredZ = z + centerOffset;
                if (centeredX * centeredX + centeredZ * centeredZ <= radiusSquared + 1.0e-6) {
                    positions.add(base.offset(x, y, z));
                }
            }
        }
    }

    private static void addCapAlignedStemDiscLayer(
            List<BlockPos> positions,
            BlockPos base,
            int y,
            double diameter,
            boolean evenCap
    ) {
        double centerOffset = evenCap ? 0.5 : 0.0;
        double radius = diameter / 2.0;
        double radiusSquared = radius * radius;
        int minOffset = -2;
        int maxOffset = evenCap ? 1 : 2;

        for (int x = minOffset; x <= maxOffset; x++) {
            double centeredX = x + centerOffset;
            for (int z = minOffset; z <= maxOffset; z++) {
                double centeredZ = z + centerOffset;
                if (centeredX * centeredX + centeredZ * centeredZ <= radiusSquared + 1.0e-6) {
                    positions.add(base.offset(x, y, z));
                }
            }
        }
    }

    private static boolean canPlace(WorldGenLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) {
                return false;
            }
            BlockState existing = level.getBlockState(pos);
            if (!isAirLike(level, pos) && (!existing.canBeReplaced() || !existing.getFluidState().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAirLike(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.is(RRBlocks.GLOWING_MUSHROOM.get())
                || !state.isCollisionShapeFullBlock(level, pos);
    }

    public record Config(
            BlockStateProvider cap,
            BlockStateProvider stem,
            IntProvider capDiameter,
            IntProvider stemHeight,
            IntProvider stemWaistDiameter
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                BlockStateProvider.CODEC.fieldOf("cap").forGetter(Config::cap),
                BlockStateProvider.CODEC.fieldOf("stem").forGetter(Config::stem),
                IntProviders.codec(12, 16).fieldOf("cap_diameter").forGetter(Config::capDiameter),
                IntProviders.codec(4, 9).fieldOf("stem_height").forGetter(Config::stemHeight),
                IntProviders.codec(2, 3).fieldOf("stem_waist_diameter").forGetter(Config::stemWaistDiameter)
        ).apply(codec, Config::new));
    }
}
