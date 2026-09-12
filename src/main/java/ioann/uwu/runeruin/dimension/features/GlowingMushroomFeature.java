package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlowingMushroomFeature extends Feature<GlowingMushroomFeature.Config> {

    private static final double CAP_SHELL_THICKNESS = 2.0;

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

        int capDiameter = context.config().capDiameter().sample(random);
        int capHeight = (capDiameter + 1) / 2;
        int stemHeight = context.config().stemHeight().sample(random);
        int stemBaseDiameter = 3 + random.nextInt(2);
        int stemTopDiameter = 3 + random.nextInt(2);
        int stemWaistDiameter = Math.min(
                context.config().stemWaistDiameter().sample(random),
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
        Set<BlockPos> stemPositions = new HashSet<>(stem);
        List<BlockPos> cap = createCapPositions(base, stemHeight, capDiameter, capHeight, stemPositions);
        if (!canPlace(level, stem) || !canPlace(level, cap)) {
            return false;
        }

        BlockState stemState = context.config().stem().getState(level, random, base);
        BlockState capState = context.config().cap().getState(level, random, base);
        for (BlockPos pos : stem) {
            level.setBlock(pos, stemState, GeometryUtils.BULK_FLAG);
        }
        for (BlockPos pos : cap) {
            level.setBlock(pos, capState, GeometryUtils.BULK_FLAG);
        }
        return true;
    }

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

    private static List<BlockPos> createCapPositions(
            BlockPos base,
            int stemHeight,
            int diameter,
            int height,
            Set<BlockPos> stemPositions
    ) {
        List<BlockPos> positions = new ArrayList<>();
        double radius = diameter / 2.0;
        boolean evenDiameter = diameter % 2 == 0;
        int minOffset = -(diameter / 2);
        int maxOffset = (diameter - 1) / 2;

        for (int layer = 0; layer <= height; layer++) {
            double fraction = layer / (double) height;
            double layerRadius = radius * Math.sqrt(Math.max(0.0, 1.0 - fraction * fraction));
            // Keep the apex wide enough to cover the inner stalk: 4-wide when even, 3x3 when odd.
            if (evenDiameter) {
                layerRadius = Math.max(layerRadius, 2.0);
            } else if (layer == height) {
                layerRadius = Math.max(layerRadius, Math.sqrt(2.0));
            }

            double centerOffset = evenDiameter ? 0.5 : 0.0;
            // Fill the penultimate layer as a crown so the hollow shell cannot open at its top.
            double innerRadius = layer == height - 1
                    ? 0.0
                    : Math.max(0.0, layerRadius - CAP_SHELL_THICKNESS);
            double innerRadiusSquared = innerRadius * innerRadius;
            for (int x = minOffset; x <= maxOffset; x++) {
                double centeredX = x + centerOffset;
                for (int z = minOffset; z <= maxOffset; z++) {
                    double centeredZ = z + centerOffset;
                    double distanceSquared = centeredX * centeredX + centeredZ * centeredZ;
                    if (distanceSquared <= layerRadius * layerRadius + 1.0e-6
                            && (innerRadius == 0.0 || distanceSquared >= innerRadiusSquared)) {
                        BlockPos pos = base.offset(x, stemHeight + layer, z);
                        if (!stemPositions.contains(pos)) {
                            positions.add(pos);
                        }
                    }
                }
            }
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
            if (!existing.isAir() && (!existing.canBeReplaced() || !existing.getFluidState().isEmpty())) {
                return false;
            }
        }
        return true;
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
