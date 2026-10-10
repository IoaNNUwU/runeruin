package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A big mushroom with a curving stem and a slightly tilted shell cap. */
public class CaveMushroomFeature extends Feature<CaveMushroomFeature.Config> {

    private static final double MAX_BLOCKED_FRACTION = 0.12;
    private static final int MAX_FOOT_DEPTH = 3;
    private static final double MIN_TILT_DEGREES = 8.0;
    private static final double MAX_TILT_DEGREES = 25.0;

    public CaveMushroomFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();

        BlockPos floor = origin.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }

        Shape shape = config.shape();
        double capRadius = config.capRadius().sample(random);
        int layers = shape.layers(capRadius);
        double roofDepth = shape.roofDepth(layers);
        // The stem runs on through the cap's hollow so its top ends right under the cap's roof.
        int height = config.height().sample(random) + (int) Math.round(roofDepth);
        double baseRadius = config.stemRadius().sample(random);
        double topRadius = Math.max(1.0, baseRadius * 0.6);
        int bend = config.bend().sample(random);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);

        Set<BlockPos> stem = new HashSet<>();
        Set<BlockPos> cap = new HashSet<>();
        Set<BlockPos> rim = new HashSet<>();

        // Sample finer than one block so a strongly bent stem stays connected.
        int steps = height * 4;
        double centerX = 0;
        double centerZ = 0;
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            double along = bend * t * t;
            double side = config.sway() * Math.sin(t * Math.PI * 2.0);
            centerX = dirX * along - dirZ * side;
            centerZ = dirZ * along + dirX * side;
            addDisc(stem, origin, centerX, (int) Math.round(t * (height - 1)), centerZ,
                    baseRadius + (topRadius - baseRadius) * t);
        }

        // The cap leans the way the stem leans overall (its top is displaced by `bend` along `angle`).
        double tilt = Math.atan2(bend, height - 1);
        tilt = Math.max(Math.toRadians(MIN_TILT_DEGREES), Math.min(Math.toRadians(MAX_TILT_DEGREES), tilt));
        // Put the cap's base plane so that its roof underside on the axis sits just above the stem top.
        double pivotX = centerX - roofDepth * Math.sin(tilt) * dirX;
        double pivotZ = centerZ - roofDepth * Math.sin(tilt) * dirZ;
        double pivotY = height - 0.5 - roofDepth * Math.cos(tilt);
        addCap(shape, capRadius, layers, origin, pivotX, pivotY, pivotZ, tilt, angle, cap, rim);
        if (cap.isEmpty()) {
            return false;
        }
        removeIsolated(cap, rim);
        growStemIntoCap(stem, cap, rim, origin, centerX, height, centerZ, topRadius, height + 2);
        stem.removeAll(cap);
        stem.removeAll(rim);

        // Let the stem stand on uneven floor instead of floating above low spots.
        for (BlockPos pos : List.copyOf(stem)) {
            if (pos.getY() != origin.getY()) {
                continue;
            }
            for (int depth = 1; depth <= MAX_FOOT_DEPTH && level.getBlockState(pos.below(depth)).isAir(); depth++) {
                stem.add(pos.below(depth));
            }
        }

        int checked = 0;
        int blocked = 0;
        for (Set<BlockPos> part : List.of(stem, cap, rim)) {
            for (BlockPos pos : part) {
                if (level.isOutsideBuildHeight(pos)) {
                    return false;
                }
                if (part == stem && pos.getY() - origin.getY() < 2) {
                    continue;
                }
                checked++;
                if (!isReplaceable(level, pos)) {
                    blocked++;
                }
            }
        }
        if (blocked > checked * MAX_BLOCKED_FRACTION) {
            return false;
        }

        fill(level, random, stem, config.stem());
        fill(level, random, cap, config.cap());
        fill(level, random, rim, config.rim());
        return true;
    }

    /**
     * Builds the cap by mapping every nearby block centre back into the untilted cap frame and testing it against the
     * continuous cap surface, so the tilt stays smooth instead of shifting rounded layers.
     * The cap pivots around the centre of its base plane.
     */
    private static void addCap(Shape shape, double capRadius, int layers, BlockPos origin, double pivotX, double pivotY,
                               double pivotZ, double tilt, double tiltDirection, Set<BlockPos> cap, Set<BlockPos> rim) {
        double tiltX = Math.cos(tiltDirection);
        double tiltZ = Math.sin(tiltDirection);
        int extent = (int) Math.ceil(capRadius + layers + 2);

        for (int x = (int) Math.floor(pivotX) - extent; x <= (int) Math.ceil(pivotX) + extent; x++) {
            for (int z = (int) Math.floor(pivotZ) - extent; z <= (int) Math.ceil(pivotZ) + extent; z++) {
                double dx = x - pivotX;
                double dz = z - pivotZ;
                double along = dx * tiltX + dz * tiltZ;
                double across = -dx * tiltZ + dz * tiltX;
                for (int y = (int) Math.floor(pivotY) - extent; y <= (int) Math.ceil(pivotY) + layers + extent; y++) {
                    double dy = y - pivotY;
                    double localAlong = along * Math.cos(tilt) - dy * Math.sin(tilt);
                    double height = along * Math.sin(tilt) + dy * Math.cos(tilt);
                    int part = shape.classify(capRadius, layers, Math.hypot(localAlong, across), height);
                    if (part != Shape.OUTSIDE) {
                        (part == Shape.RIM ? rim : cap).add(origin.offset(x, y, z));
                    }
                }
            }
        }
    }

    /** Drops single blocks that only touch the cap diagonally or not at all. */
    private static void removeIsolated(Set<BlockPos> cap, Set<BlockPos> rim) {
        for (Set<BlockPos> part : List.of(cap, rim)) {
            part.removeIf(pos -> Direction.stream().noneMatch(d -> cap.contains(pos.relative(d)) || rim.contains(pos.relative(d))));
        }
    }

    /** Continues the stem upward until each column meets the cap, so no thinner pole is needed inside the cap. */
    private static void growStemIntoCap(Set<BlockPos> stem, Set<BlockPos> cap, Set<BlockPos> rim, BlockPos origin,
                                        double cx, int fromY, double cz, double radius, int maxY) {
        Set<BlockPos> column = new HashSet<>();
        addDisc(column, origin, cx, fromY, cz, radius);
        for (BlockPos start : column) {
            Set<BlockPos> pending = new HashSet<>();
            for (int y = start.getY(); y <= origin.getY() + maxY; y++) {
                BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
                if (cap.contains(pos) || rim.contains(pos)) {
                    stem.addAll(pending);
                    break;
                }
                pending.add(pos);
            }
        }
    }

    private static void addDisc(Set<BlockPos> blocks, BlockPos origin, double cx, int y, double cz, double radius) {
        for (int x = (int) Math.floor(cx - radius); x <= (int) Math.ceil(cx + radius); x++) {
            for (int z = (int) Math.floor(cz - radius); z <= (int) Math.ceil(cz + radius); z++) {
                double dx = x - cx;
                double dz = z - cz;
                if (dx * dx + dz * dz <= radius * radius + 0.25) {
                    blocks.add(origin.offset(x, y, z));
                }
            }
        }
    }

    private static boolean isReplaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced());
    }

    private static void fill(WorldGenLevel level, RandomSource random, Set<BlockPos> blocks, BlockStateProvider provider) {
        for (BlockPos pos : blocks) {
            if (isReplaceable(level, pos)) {
                level.setBlock(pos, provider.getState(level, random, pos), GeometryUtils.BULK_FLAG);
            }
        }
    }

    public enum Shape implements StringRepresentable {
        /** Thick rounded cap. */
        DOME("dome"),
        /** Thin wide paraboloid that droops at the edge. */
        UMBRELLA("umbrella"),
        /** Trumpet that flares upward and is hollow from above. */
        FUNNEL("funnel"),
        /** Flat hat with a raised crown. */
        DISC("disc");

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        private static final double SHELL_WIDTH = 2.0;
        private static final double MIN_ROOF_THICKNESS = 1.2;

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        static final int OUTSIDE = 0;
        static final int CAP = 1;
        static final int RIM = 2;

        /** Height of the cap in blocks above its base plane. */
        int layers(double capRadius) {
            return switch (this) {
                case DOME -> Math.max(3, (int) Math.round(capRadius * 0.75));
                case UMBRELLA -> Math.max(2, (int) Math.round(capRadius * 0.5));
                case FUNNEL -> (int) Math.round(capRadius * 1.3);
                case DISC -> 2;
            };
        }

        /** Height above the base plane where the hollow under the cap's roof ends on the axis; 0 if the cap is solid there. */
        double roofDepth(int layers) {
            return switch (this) {
                case DOME -> layers + 0.5 - SHELL_WIDTH;
                case UMBRELLA -> layers + 0.5 - MIN_ROOF_THICKNESS;
                default -> 0;
            };
        }

        /**
         * Classifies a point at horizontal distance {@code d} from the cap axis and height {@code v} above the base
         * plane, using continuous surfaces. Shells are at least {@code SHELL_WIDTH} thick so no stray blocks remain.
         */
        int classify(double capRadius, int layers, double d, double v) {
            double top = layers + 0.5;
            if (v < -0.5 || v > top) {
                return OUTSIDE;
            }
            double rel = Math.max(v, 0) / top;
            double outer;
            boolean hollow;
            switch (this) {
                case DOME -> {
                    outer = capRadius * Math.sqrt(1 - rel * rel);
                    double innerTop = top - SHELL_WIDTH;
                    hollow = v < innerTop && d < (capRadius - SHELL_WIDTH) * Math.sqrt(1 - Math.pow(Math.max(v, 0) / innerTop, 2));
                }
                case UMBRELLA -> {
                    outer = capRadius * Math.sqrt(1 - rel);
                    double surface = top * (1 - d * d / (capRadius * capRadius));
                    double slope = 2 * top * d / (capRadius * capRadius);
                    hollow = v < surface - Math.min(3.0, Math.max(MIN_ROOF_THICKNESS, SHELL_WIDTH * slope));
                }
                case FUNNEL -> {
                    outer = 2 + (capRadius - 2) * Math.pow(rel, 1.6);
                    hollow = v >= 1.5 && d <= outer - SHELL_WIDTH;
                }
                default -> {
                    outer = capRadius - 1.2 * (v + 0.5);
                    hollow = false;
                }
            }
            if (d > outer || hollow) {
                return OUTSIDE;
            }
            boolean brim = v < (this == DISC ? 1.5 : 0.5);
            return brim && d > outer - 1.2 ? RIM : CAP;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public record Config(
            BlockStateProvider stem,
            BlockStateProvider cap,
            BlockStateProvider rim,
            Shape shape,
            IntProvider height,
            IntProvider stemRadius,
            IntProvider capRadius,
            IntProvider bend,
            int sway
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                BlockStateProvider.CODEC.fieldOf("stem").forGetter(Config::stem),
                BlockStateProvider.CODEC.fieldOf("cap").forGetter(Config::cap),
                BlockStateProvider.CODEC.fieldOf("rim").forGetter(Config::rim),
                Shape.CODEC.fieldOf("shape").forGetter(Config::shape),
                IntProviders.codec(3, 24).fieldOf("height").forGetter(Config::height),
                IntProviders.codec(1, 5).fieldOf("stem_radius").forGetter(Config::stemRadius),
                IntProviders.codec(3, 10).fieldOf("cap_radius").forGetter(Config::capRadius),
                IntProviders.codec(0, 12).fieldOf("bend").forGetter(Config::bend),
                Codec.intRange(0, 6).fieldOf("sway").forGetter(Config::sway)
        ).apply(codec, Config::new));
    }
}
