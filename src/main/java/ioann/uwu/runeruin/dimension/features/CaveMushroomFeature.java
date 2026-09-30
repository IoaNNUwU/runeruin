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
    private static final double MAX_TILT_DEGREES = 18.0;
    private static final double MIN_SHELL_WIDTH = 2.0;

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

        int height = config.height().sample(random);
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

        Shape shape = config.shape();
        double capRadius = config.capRadius().sample(random);
        int layers = shape.layers(capRadius);
        addCap(shape, capRadius, layers, origin, centerX, height - 1, centerZ, random, cap, rim);
        if (cap.isEmpty()) {
            return false;
        }
        removeIsolated(cap, rim);
        growStemIntoCap(stem, cap, rim, origin, centerX, height, centerZ, topRadius, height - 1 + layers + 2);
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
                if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) {
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
     * Builds the cap by mapping every nearby block back into the untilted cap frame, so tilting never leaves holes.
     * The cap pivots around the centre of its lowest layer, which sits on top of the stem.
     */
    private static void addCap(Shape shape, double capRadius, int layers, BlockPos origin, double pivotX, int pivotY,
                               double pivotZ, RandomSource random, Set<BlockPos> cap, Set<BlockPos> rim) {
        double tilt = Math.toRadians(MIN_TILT_DEGREES + random.nextDouble() * (MAX_TILT_DEGREES - MIN_TILT_DEGREES));
        double tiltAngle = random.nextDouble() * Math.PI * 2.0;
        double tiltX = Math.cos(tiltAngle);
        double tiltZ = Math.sin(tiltAngle);
        int extent = (int) Math.ceil(capRadius + layers + 2);

        for (int x = (int) Math.floor(pivotX) - extent; x <= (int) Math.ceil(pivotX) + extent; x++) {
            for (int z = (int) Math.floor(pivotZ) - extent; z <= (int) Math.ceil(pivotZ) + extent; z++) {
                double dx = x - pivotX;
                double dz = z - pivotZ;
                double along = dx * tiltX + dz * tiltZ;
                double across = -dx * tiltZ + dz * tiltX;
                for (int y = -extent; y <= layers + extent; y++) {
                    double localAlong = along * Math.cos(tilt) - y * Math.sin(tilt);
                    int k = (int) Math.round(along * Math.sin(tilt) + y * Math.cos(tilt));
                    if (k < 0 || k > layers) {
                        continue;
                    }
                    double distance = Math.sqrt(localAlong * localAlong + across * across);
                    double outer = shape.radius(capRadius, k, layers);
                    if (distance > outer || distance <= shape.innerRadius(capRadius, k, layers)) {
                        continue;
                    }
                    boolean isRim = distance > outer - 1.2 && (k == 0 || shape == Shape.DISC && k == 1);
                    (isRim ? rim : cap).add(origin.offset(x, pivotY + y, z));
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
        DOME("dome", 2),
        /** Thin wide paraboloid that droops at the edge. */
        UMBRELLA("umbrella", 1),
        /** Trumpet that flares upward and is hollow from above. */
        FUNNEL("funnel", 1),
        /** Flat hat with a raised crown. */
        DISC("disc", 3);

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        private final String name;
        private final int thickness;

        Shape(String name, int thickness) {
            this.name = name;
            this.thickness = thickness;
        }

        int layers(double capRadius) {
            return switch (this) {
                case DOME -> Math.max(3, (int) Math.round(capRadius * 0.75));
                case UMBRELLA -> Math.max(2, (int) Math.round(capRadius * 0.5));
                case FUNNEL -> (int) Math.round(capRadius * 1.3);
                case DISC -> 2;
            };
        }

        /** Cap radius at layer {@code k}; negative above the top so the last layers stay solid. */
        double radius(double capRadius, int k, int layers) {
            if (k > layers) {
                return -1;
            }
            double radius = switch (this) {
                case DOME -> capRadius * Math.sqrt(1 - Math.pow(k / (double) layers, 2));
                case UMBRELLA -> capRadius * Math.sqrt(1 - k / (double) layers);
                case FUNNEL -> 2 + (capRadius - 2) * Math.pow(k / (double) layers, 1.6);
                case DISC -> capRadius - new int[]{0, 1, 3}[k];
            };
            return Math.max(radius, 1.5);
        }

        /** Radius of the empty core of layer {@code k}; shells are at least {@code MIN_SHELL_WIDTH} wide so no stray blocks remain. */
        double innerRadius(double capRadius, int k, int layers) {
            double outer = radius(capRadius, k, layers);
            if (this == FUNNEL) {
                return k >= 2 ? outer - MIN_SHELL_WIDTH : -1;
            }
            return Math.min(radius(capRadius, k + this.thickness, layers), outer - MIN_SHELL_WIDTH);
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
