package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.dimension.placements.WallPlacementFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AshenMushroomClusterFeature extends Feature<WallMushroomFeature.Config> {

    private static final int HORIZONTAL_RADIUS = 2;
    private static final int MIN_VERTICAL_SPACING = 5;
    private static final int MAX_VERTICAL_SPACING = 7;
    private static final WallPlacementFilter WALL_FILTER = WallPlacementFilter.ashenMushroom();

    public AshenMushroomClusterFeature() {
        super(WallMushroomFeature.Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<WallMushroomFeature.Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int targetCount = 3 + random.nextInt(3);
        if (!placeAt(level, context.config(), origin, random)) {
            return false;
        }

        List<BlockPos> placed = new ArrayList<>(targetCount);
        Set<BlockPos> attempted = new HashSet<>();
        placed.add(origin);
        attempted.add(origin);

        for (int attempt = 0; attempt < targetCount * 16 && placed.size() < targetCount; attempt++) {
            BlockPos parent = placed.get(random.nextInt(placed.size()));
            int dx = random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            int dy = MIN_VERTICAL_SPACING + random.nextInt(MAX_VERTICAL_SPACING - MIN_VERTICAL_SPACING + 1);
            if (random.nextBoolean()) {
                dy = -dy;
            }
            int dz = random.nextInt(HORIZONTAL_RADIUS * 2 + 1) - HORIZONTAL_RADIUS;
            if (dx * dx + dz * dz > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                continue;
            }

            BlockPos candidate = parent.offset(dx, dy, dz);
            if (!attempted.add(candidate) || !WALL_FILTER.canPlaceAt(level, candidate)) {
                continue;
            }
            if (placeAt(level, context.config(), candidate, random)) {
                placed.add(candidate);
            }
        }

        return !placed.isEmpty();
    }

    private static boolean placeAt(WorldGenLevel level, WallMushroomFeature.Config config, BlockPos origin, RandomSource random) {
        return WALL_FILTER.canPlaceAt(level, origin) && WallMushroomFeature.placeAt(level, config, origin, random);
    }
}
