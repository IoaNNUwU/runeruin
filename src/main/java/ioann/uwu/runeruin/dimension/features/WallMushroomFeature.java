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
        double radiusSquared = radius * radius;
        int capHeight = 2 + random.nextInt(2);

        // Place only the curved upper shell of a flattened sphere, leaving its underside hollow.
        for (int x = minOffset; x <= maxOffset; x++) {
            for (int z = minOffset; z <= maxOffset; z++) {
                double dx = x - centerOffset;
                double dz = z - centerOffset;
                double distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radiusSquared) {
                    continue;
                }

                double domeProfile = Math.sqrt(1.0 - distanceSquared / radiusSquared);
                int yOffset = (int) Math.floor(capHeight * domeProfile);
                tryPlace(level, mutable.set(ox + x, oy + yOffset, oz + z), config, random, origin);
            }
        }

        return true;
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
