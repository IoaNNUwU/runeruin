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

        int minSurrounding = config.minSubMonoliths.sample(random);
        int maxSurrounding = config.maxSubMonoliths.sample(random);
        int surrounding = random.nextIntBetweenInclusive(minSurrounding, maxSurrounding);

        GeometryUtils.smoothCyl(
                level,
                new BlockPos(origin.getX(), origin.getY() - 3, origin.getZ()),
                block,
                radius,
                30
        );

        return true;
    }

    public record Config(
            BlockStateProvider stoneBlock,
            BlockStateProvider innerBlock,
            IntProvider minRadius,
            IntProvider maxRadius,
            IntProvider minSubMonoliths,
            IntProvider maxSubMonoliths
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("stone_block").forGetter(Config::stoneBlock),
                        BlockStateProvider.CODEC.fieldOf("inner_block").forGetter(Config::innerBlock),
                        IntProviders.codec(3, 32).fieldOf("min_radius").forGetter(Config::minRadius),
                        IntProviders.codec(3, 32).fieldOf("max_radius").forGetter(Config::maxRadius),
                        IntProviders.codec(3, 32).fieldOf("min_sub_monoliths").forGetter(Config::minSubMonoliths),
                        IntProviders.codec(3, 32).fieldOf("max_sub_monoliths").forGetter(Config::maxSubMonoliths)
                ).apply(codec, Config::new)
        );
    }
}
