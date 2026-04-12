package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
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
        WorldGenLevel level = ctx.level();

        int minRadius = ctx.config().minRadius().sample(ctx.random());
        int maxRadius = ctx.config().maxRadius().sample(ctx.random());
        int radius = ctx.random().nextIntBetweenInclusive(minRadius, maxRadius);

        BlockPos origin = ctx.origin();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    if (origin.distToCenterSqr(xx, yy, zz) < radius * radius) {
                        level.setBlock(new BlockPos(xx, yy, zz), block, 1);
                    }
                }
            }
        }

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
