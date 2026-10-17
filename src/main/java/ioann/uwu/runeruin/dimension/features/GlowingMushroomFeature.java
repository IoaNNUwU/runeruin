package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public class GlowingMushroomFeature extends Feature<GlowingMushroomFeature.Config> {

    private static final double CAP_SHELL_THICKNESS = 3.0;
    private static final double MIN_TILT_DEGREES = 10.0;
    private static final double MAX_TILT_DEGREES = 18.0;
    private static final double MIN_TWIN_TILT_DEGREES = 27.0;
    private static final double MAX_TWIN_TILT_DEGREES = 36.0;
    private static final float TWIN_CHANCE = 0.25F;
    private static final double MIN_STEM_SWERVE = 0.7;
    private static final double MAX_STEM_SWERVE = 1.4;
    private static final double MAX_STEM_BULGE = 1.0;
    // With the thinnest stem one block in radius, discs this close along the line always share a block.
    private static final double STEM_SAMPLE_STEP = 0.125;

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
        CapGeometry cap = createCapGeometry(base, stemHeight, capDiameter, capHeight);

        Set<BlockPos> placedCap = new HashSet<>(tiltPositions(cap.outer(), base, capDiameter, tilt));
        fillCapSlices(placedCap);
        Set<BlockPos> cavity = new HashSet<>(tiltPositions(cap.cavity(), base, capDiameter, tilt));
        placedCap.removeAll(cavity);
        keepLargestConnectedComponent(placedCap);
        Set<BlockPos> placedStem = createStem(
                base,
                stemHeight,
                capHeight,
                capDiameter,
                config.stemWaistDiameter().sample(random),
                random,
                tilt
        );
        placedStem.removeAll(placedCap);
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
        return new Tilt(Math.sin(angle), Math.cos(angle), Math.cos(direction), Math.sin(direction));
    }

    private static List<BlockPos> tiltPositions(List<BlockPos> positions, BlockPos base, int capDiameter, Tilt tilt) {
        // Even-sized caps are centered half a block toward the negative axes by their source geometry.
        double centerOffset = capDiameter % 2 == 0 ? -0.5 : 0.0;
        Set<BlockPos> transformed = new HashSet<>();

        for (BlockPos pos : positions) {
            Vec3 rotated = tilt.apply(
                    pos.getX() - base.getX() - centerOffset,
                    pos.getY() - base.getY() + 0.5,
                    pos.getZ() - base.getZ() - centerOffset
            );
            transformed.add(base.offset(
                    Mth.floor(centerOffset + 0.5 + rotated.x),
                    Math.max(0, Mth.floor(rotated.y)),
                    Mth.floor(centerOffset + 0.5 + rotated.z)
            ));
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

    private static CapGeometry createCapGeometry(
            BlockPos base,
            int stemHeight,
            int diameter,
            int height
    ) {
        List<BlockPos> outer = new ArrayList<>();
        List<BlockPos> cavity = new ArrayList<>();
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
                        // The top two layers stay solid: the stem ends where they begin.
                        if (layer < height - 1
                                && innerRadius > 0.0
                                && distanceSquared < innerRadius * innerRadius - 1.0e-6) {
                            cavity.add(pos);
                        }
                    }
                }
            }
        }
        return new CapGeometry(outer, cavity);
    }

    private record Tilt(double sin, double cos, double directionX, double directionZ) {

        /** Leans a point around the foot of the mushroom's axis; the point is given from that foot. */
        Vec3 apply(double x, double y, double z) {
            double alongTilt = x * directionX + z * directionZ;
            double acrossTilt = z * directionX - x * directionZ;
            double rotatedAlong = alongTilt * cos + y * sin;
            return new Vec3(
                    rotatedAlong * directionX - acrossTilt * directionZ,
                    y * cos - alongTilt * sin,
                    rotatedAlong * directionZ + acrossTilt * directionX
            );
        }
    }

    private record SliceLine(int y, int fixed) {}

    private record CapGeometry(List<BlockPos> outer, List<BlockPos> cavity) {}

    private record MushroomShape(Set<BlockPos> stem, Set<BlockPos> cap) {}

    /**
     * Discs strung on a line that leans with the cap and swerves from knot to knot, each knot with a
     * thickness of its own: no two stems bend, swell and pinch alike. The line is built already leaning
     * and its samples are close enough for neighbouring discs to share a block, so the stem has no gaps
     * from the foot to the solid top of the cap.
     */
    private static Set<BlockPos> createStem(
            BlockPos base,
            int height,
            int capHeight,
            int capDiameter,
            int waistDiameter,
            RandomSource random,
            Tilt tilt
    ) {
        double axis = capDiameter % 2 == 0 ? 0.0 : 0.5;
        double waistRadius = 0.5 + waistDiameter * 0.25;
        int segments = Math.max(2, height / 2);
        // Knots from the foot to the bottom of the cap, then one more where the solid top of the cap begins:
        // a thin end there stays inside the cap however it leans.
        double[] offsetX = new double[segments + 2];
        double[] offsetZ = new double[segments + 2];
        double[] radius = new double[segments + 2];
        radius[0] = 2.25 + random.nextDouble() * 0.75;
        double swerveDirection = random.nextDouble() * Math.PI * 2.0;
        for (int knot = 1; knot < segments; knot++) {
            // Each knot swerves to the other side of the axis than the one before, give or take a quarter turn.
            swerveDirection += Math.PI + (random.nextDouble() - 0.5) * Math.PI;
            double swerve = MIN_STEM_SWERVE + random.nextDouble() * (MAX_STEM_SWERVE - MIN_STEM_SWERVE);
            offsetX[knot] = Math.cos(swerveDirection) * swerve;
            offsetZ[knot] = Math.sin(swerveDirection) * swerve;
        }
        for (int knot = 1; knot <= segments; knot++) {
            radius[knot] = waistRadius + random.nextDouble() * MAX_STEM_BULGE;
        }
        radius[segments + 1] = waistRadius;

        Set<BlockPos> stem = new HashSet<>();
        int top = height + capHeight - 1;
        for (double y = 0.0; y <= top; y += STEM_SAMPLE_STEP) {
            int knot = y < height ? (int) (y * segments / height) : segments;
            double knotY = (double) height * knot / segments;
            double nextKnotY = knot < segments ? (double) height * (knot + 1) / segments : top;
            double progress = (y - knotY) / (nextKnotY - knotY);
            double eased = progress * progress * (3.0 - 2.0 * progress);
            Vec3 center = tilt.apply(
                    Mth.lerp(eased, offsetX[knot], offsetX[knot + 1]),
                    y,
                    Mth.lerp(eased, offsetZ[knot], offsetZ[knot + 1])
            );
            addDisc(
                    stem,
                    base,
                    axis + center.x,
                    Math.max(0, Mth.floor(center.y)),
                    axis + center.z,
                    Mth.lerp(eased, radius[knot], radius[knot + 1])
            );
        }
        return stem;
    }

    private static void addDisc(
            Set<BlockPos> positions,
            BlockPos base,
            double centerX,
            int y,
            double centerZ,
            double radius
    ) {
        for (int x = Mth.floor(centerX - radius); x <= Mth.floor(centerX + radius); x++) {
            double distanceX = x + 0.5 - centerX;
            for (int z = Mth.floor(centerZ - radius); z <= Mth.floor(centerZ + radius); z++) {
                double distanceZ = z + 0.5 - centerZ;
                if (distanceX * distanceX + distanceZ * distanceZ <= radius * radius) {
                    positions.add(base.offset(x, y, z));
                }
            }
        }
    }

    private static boolean canPlace(WorldGenLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (level.isOutsideBuildHeight(pos)) {
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
