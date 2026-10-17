package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

public class MonolithFeature extends Feature<MonolithFeature.Config> {

    public MonolithFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        BlockState block = config.stoneBlock.getState(level, random, origin);

        int minRadius = config.minRadius.sample(random);
        int maxRadius = config.maxRadius.sample(random);
        int radius = random.nextIntBetweenInclusive(minRadius, maxRadius);

        GeometryUtils.smoothCyl(level, origin.below(3), block, radius, 30);

        return true;
    }

    public record Config(
            BlockStateProvider stoneBlock,
            IntProvider minRadius,
            IntProvider maxRadius
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("stone_block").forGetter(Config::stoneBlock),
                        IntProviders.codec(3, 32).fieldOf("min_radius").forGetter(Config::minRadius),
                        IntProviders.codec(3, 32).fieldOf("max_radius").forGetter(Config::maxRadius)
                ).apply(codec, Config::new)
        );
    }
}
