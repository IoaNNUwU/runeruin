package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class BoulderFeature extends Feature<BoulderFeature.Config> {

    public BoulderFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        BlockState block = ctx.config().stoneBlock().getState(ctx.level(), ctx.random(), ctx.origin());

        int minRadius = ctx.config().minRadius().sample(ctx.random());
        int maxRadius = ctx.config().maxRadius().sample(ctx.random());
        int radius = ctx.random().nextIntBetweenInclusive(minRadius, maxRadius);

        GeometryUtils.sphere(ctx.level(), ctx.origin(), block, radius);

        return true;
    }

    public record Config(
            BlockStateProvider stoneBlock,
            BlockStateProvider mossBlock,
            IntProvider minRadius,
            IntProvider maxRadius
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("stone_block").forGetter(Config::stoneBlock),
                        BlockStateProvider.CODEC.fieldOf("moss_block").forGetter(Config::mossBlock),
                        IntProviders.codec(5, 32).fieldOf("min_radius").forGetter(Config::minRadius),
                        IntProviders.codec(5, 32).fieldOf("max_radius").forGetter(Config::maxRadius)
                ).apply(codec, BoulderFeature.Config::new));

    }
}
