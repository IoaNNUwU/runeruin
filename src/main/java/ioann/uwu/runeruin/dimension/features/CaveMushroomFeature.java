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

/** A big mushroom with a curving stem and a shell cap; {@code hanging} mirrors it onto a ceiling. */
public class CaveMushroomFeature extends Feature<CaveMushroomFeature.Config> {

    private static final double MAX_BLOCKED_FRACTION = 0.12;
    private static final int MAX_FOOT_DEPTH = 3;

    public CaveMushroomFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int sign = config.hanging() ? -1 : 1;

        BlockPos support = origin.below(sign);
        if (!level.getBlockState(support).isFaceSturdy(level, support, config.hanging() ? Direction.DOWN : Direction.UP)) {
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
            addDisc(stem, origin, sign, centerX, (int) Math.round(t * (height - 1)), centerZ,
                    baseRadius + (topRadius - baseRadius) * t);
        }

        Shape shape = config.shape();
        double capRadius = config.capRadius().sample(random);
        int layers = shape.layers(capRadius);
        for (int k = 0; k <= layers; k++) {
            double outer = shape.radius(capRadius, k, layers);
            double inner = shape.radius(capRadius, k + shape.thickness(), layers);
            for (int x = (int) Math.floor(centerX - capRadius); x <= (int) Math.ceil(centerX + capRadius); x++) {
                for (int z = (int) Math.floor(centerZ - capRadius); z <= (int) Math.ceil(centerZ + capRadius); z++) {
                    double dx = x - centerX;
                    double dz = z - centerZ;
                    double distance = shape == Shape.CONE ? Math.max(Math.abs(dx), Math.abs(dz)) : Math.sqrt(dx * dx + dz * dz);
                    if (distance > outer || distance <= inner) {
                        continue;
                    }
                    boolean isRim = distance > outer - 1.2 && (k == 0 || shape == Shape.DISC && k == 1);
                    (isRim ? rim : cap).add(origin.offset(x, sign * (height - 1 + k), z));
                }
            }
            if (k < layers) {
                addDisc(stem, origin, sign, centerX, height - 1 + k, centerZ, 1.0);
            }
        }
        stem.removeAll(cap);
        stem.removeAll(rim);

        // Let the stem stand on uneven floor instead of floating above low spots.
        for (BlockPos pos : List.copyOf(stem)) {
            if (localY(pos, origin, sign) != 0) {
                continue;
            }
            for (int depth = 1; depth <= MAX_FOOT_DEPTH && level.getBlockState(pos.below(sign * depth)).isAir(); depth++) {
                stem.add(pos.below(sign * depth));
            }
        }

        int checked = 0;
        int blocked = 0;
        for (Set<BlockPos> part : List.of(stem, cap, rim)) {
            for (BlockPos pos : part) {
                if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) {
                    return false;
                }
                if (part == stem && localY(pos, origin, sign) < 2) {
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

    private static int localY(BlockPos pos, BlockPos origin, int sign) {
        return sign * (pos.getY() - origin.getY());
    }

    private static void addDisc(Set<BlockPos> blocks, BlockPos origin, int sign, double cx, int y, double cz, double radius) {
        for (int x = (int) Math.floor(cx - radius); x <= (int) Math.ceil(cx + radius); x++) {
            for (int z = (int) Math.floor(cz - radius); z <= (int) Math.ceil(cz + radius); z++) {
                double dx = x - cx;
                double dz = z - cz;
                if (dx * dx + dz * dz <= radius * radius + 0.25) {
                    blocks.add(origin.offset(x, sign * y, z));
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
        /** Stepped square pyramid. */
        CONE("cone", 1),
        /** Flat hat with a raised crown. */
        DISC("disc", 3);

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        private final String name;
        private final int thickness;

        Shape(String name, int thickness) {
            this.name = name;
            this.thickness = thickness;
        }

        /** Vertical shell thickness; a layer keeps only the ring outside the layer {@code thickness} above it. */
        int thickness() {
            return this.thickness;
        }

        int layers(double capRadius) {
            return switch (this) {
                case DOME -> Math.max(3, (int) Math.round(capRadius * 0.6));
                case UMBRELLA -> Math.max(2, (int) Math.round(capRadius * 0.5));
                case CONE -> Math.max(3, (int) Math.round(capRadius * 0.9));
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
                case CONE -> capRadius * (1 - k / (double) (layers + 1));
                case DISC -> capRadius - new int[]{0, 1, 3}[k];
            };
            return Math.max(radius, 1.5);
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
            int sway,
            boolean hanging
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
                Codec.intRange(0, 6).fieldOf("sway").forGetter(Config::sway),
                Codec.BOOL.fieldOf("hanging").forGetter(Config::hanging)
        ).apply(codec, Config::new));
    }
}
