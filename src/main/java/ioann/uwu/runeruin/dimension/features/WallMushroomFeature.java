package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class WallMushroomFeature extends Feature<WallMushroomFeature.Config> {

    private static final double CAP_SHELL_THICKNESS = 2.5;
    private static final double SMALL_CAP_TAPER = 0.25;
    private static final double LARGE_CAP_TAPER = 0.40;
    private static final String[] RADIUS_TWO_FOOTPRINT = {
            ".###.",
            "#####",
            "#####",
            "#####",
            ".###."
    };
    private static final String[] RADIUS_THREE_FOOTPRINT = {
            "..###..",
            ".#####.",
            "#######",
            "#######",
            "#######",
            ".#####.",
            "..###.."
    };

    public WallMushroomFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<WallMushroomFeature.Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int diameter = config.diameter().sample(random);
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        int minOffset = -(diameter / 2);
        int maxOffset = minOffset + diameter - 1;
        double centerOffset = (minOffset + maxOffset) / 2.0;
        double radius = (diameter - 1) / 2.0;
        boolean smallCap = radius <= 3.5;
        boolean tinyCap = radius <= 1.5;
        double baseRadius = radius <= 1.0
                ? Math.sqrt(2.0)
                : (tinyCap && diameter % 2 == 0 ? 2.0 : radius);
        double apexRadius = tinyCap ? 1.0 : Math.min(radius, diameter % 2 == 0 ? 2.0 : Math.sqrt(2.0));
        int capLayers = diameter == 5
                ? 1
                : radius <= 3.0
                ? 2
                : radius <= 5.0
                ? 3
                : 3 + random.nextInt(2);
        double capTaper = smallCap ? SMALL_CAP_TAPER : LARGE_CAP_TAPER;

        // Build the dome as horizontal shell layers so the lower rim has real thickness.
        for (int yOffset = 0; yOffset < capLayers; yOffset++) {
            if (diameter == 5 || (diameter == 7 && yOffset == 0)) {
                String[] footprint = diameter == 5 ? RADIUS_TWO_FOOTPRINT : RADIUS_THREE_FOOTPRINT;
                placeFootprint(level, mutable, config, random, origin, ox, oy + yOffset, oz, minOffset, footprint);
                continue;
            }

            double progress = yOffset / (double) (capLayers - 1);
            double outerRadius = baseRadius * (1.0 - capTaper * progress * progress);
            outerRadius = Math.min(baseRadius, Math.max(outerRadius, apexRadius));
            // At small scales a thin ring aliases into separate pixels, so use a solid rounded cap.
            double innerRadius = smallCap ? 0.0 : Math.max(0.0, outerRadius - CAP_SHELL_THICKNESS);

            // Fill only the crown; keeping the lower layers as a shell avoids a broad, boxy plateau.
            if (!smallCap && yOffset == capLayers - 1) {
                innerRadius = 0.0;
            }

            double outerRadiusSquared = outerRadius * outerRadius;
            double innerRadiusSquared = innerRadius * innerRadius;
            for (int x = minOffset; x <= maxOffset; x++) {
                double dx = x - centerOffset;
                for (int z = minOffset; z <= maxOffset; z++) {
                    double dz = z - centerOffset;
                    double distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared > outerRadiusSquared + 1.0e-6
                            || (innerRadius > 0.0 && distanceSquared < innerRadiusSquared - 1.0e-6)) {
                        continue;
                    }

                    tryPlace(level, mutable.set(ox + x, oy + yOffset, oz + z), config, random, origin);
                }
            }
        }

        return true;
    }

    private static void placeFootprint(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutable,
            Config config,
            RandomSource random,
            BlockPos origin,
            int ox,
            int oy,
            int oz,
            int minOffset,
            String[] footprint
    ) {
        for (int z = 0; z < footprint.length; z++) {
            for (int x = 0; x < footprint[z].length(); x++) {
                if (footprint[z].charAt(x) == '#') {
                    tryPlace(
                            level,
                            mutable.set(ox + minOffset + x, oy, oz + minOffset + z),
                            config,
                            random,
                            origin
                    );
                }
            }
        }
    }

    private static void tryPlace(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            Config config,
            RandomSource random,
            BlockPos origin
    ) {
        BlockState blockState = config.mushroomBlock().getState(level, random, origin);
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, blockState, GeometryUtils.BULK_FLAG);
        }
    }

    public record Config(BlockStateProvider mushroomBlock, IntProvider diameter) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                BlockStateProvider.CODEC.fieldOf("mushroom_block").forGetter(Config::mushroomBlock),
                IntProviders.codec(3, 15).fieldOf("diameter").forGetter(Config::diameter)
        ).apply(codec, Config::new));
    }
}
