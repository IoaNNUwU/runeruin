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

        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {
            for (int z = -diameter / 2 + 1; z < diameter / 2; z++) {
                tryPlace(level, mutable.set(ox + x, oy, oz + z), config, random, origin);
            }
        }

        int z = -diameter / 2;
        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {
            tryPlace(level, mutable.set(ox + x, oy, oz + z), config, random, origin);
        }

        z = diameter / 2;
        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {
            tryPlace(level, mutable.set(ox + x, oy, oz + z), config, random, origin);
        }

        int x = -diameter / 2;
        for (z = -diameter / 2 + 1; z < diameter / 2; z++) {
            tryPlace(level, mutable.set(ox + x, oy, oz + z), config, random, origin);
        }

        x = diameter / 2;
        for (z = -diameter / 2 + 1; z < diameter / 2; z++) {
            tryPlace(level, mutable.set(ox + x, oy, oz + z), config, random, origin);
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

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(BlockStateProvider.CODEC.fieldOf("mushroom_block").forGetter(Config::mushroomBlock), IntProviders.codec(3, 7).fieldOf("diameter").forGetter(Config::diameter)).apply(codec, Config::new));
    }
}
