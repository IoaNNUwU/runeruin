package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MiniVolcanoFeature extends Feature<MiniVolcanoFeature.Config> {
    public MiniVolcanoFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        int radius = ctx.config().radius().sample(ctx.random());
        int rimRadius = Math.max(4, radius / 2);
        int height = radius - rimRadius + 2;
        int poolRadius = rimRadius - 1;
        int poolLimit = diskLimit(poolRadius);
        BlockPos origin = ctx.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState tuff = ctx.config().stone().getState(ctx.level(), ctx.random(), origin);

        for (int y = 0; y <= height; y++) {
            int layerRadius = Math.min(radius, rimRadius + height - y);
            int layerLimit = diskLimit(layerRadius);
            for (int x = -layerRadius; x <= layerRadius; x++) {
                for (int z = -layerRadius; z <= layerRadius; z++) {
                    int distance2 = x * x + z * z;
                    if (distance2 > layerLimit) {
                        continue;
                    }
                    BlockState state = tuff;
                    if (distance2 <= poolLimit && y == height) {
                        state = Blocks.AIR.defaultBlockState();
                    } else if (distance2 <= poolLimit && y >= height - 2) {
                        state = Blocks.WATER.defaultBlockState();
                    } else if (y == height - 3 && distance2 <= poolLimit
                            && (distance2 == 0 || ctx.random().nextFloat() < 0.3F)) {
                        state = Blocks.MAGMA_BLOCK.defaultBlockState();
                    }
                    ctx.level().setBlock(pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z), state, Block.UPDATE_CLIENTS);
                }
            }
        }

        return true;
    }

    private static int diskLimit(int radius) {
        return radius * radius + radius / 2;
    }

    public record Config(BlockStateProvider stone, IntProvider radius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                BlockStateProvider.CODEC.fieldOf("stone").forGetter(Config::stone),
                IntProviders.codec(5, 32).fieldOf("radius").forGetter(Config::radius)
        ).apply(codec, Config::new));
    }
}
