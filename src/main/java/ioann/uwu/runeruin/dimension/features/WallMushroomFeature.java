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

public class WallMushroomFeature extends Feature<WallMushroomFeature.Config> {

    public WallMushroomFeature() {
        super(Config.CODEC);
    }

    // TODO: Find a flag or make properly aligned mushroom blocks
    private static final int FLAG = 1;

    @Override
    public boolean place(FeaturePlaceContext<WallMushroomFeature.Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        int diameter = config.diameter().sample(random);

        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {
            for (int z = -diameter / 2 + 1; z < diameter / 2; z++) {

                BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                if (level.getBlockState(blockPos).isAir()) {
                    level.setBlock(blockPos, blockState, FLAG);
                }
            }
        }

        int z = -diameter / 2;

        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {

            BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

            BlockState blockState = config.mushroomBlock().getState(level, random, origin);

            if (level.getBlockState(blockPos).isAir()) {
                level.setBlock(blockPos, blockState, FLAG);
            }
        }

        z = diameter / 2;

        for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {

            BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

            BlockState blockState = config.mushroomBlock().getState(level, random, origin);

            if (level.getBlockState(blockPos).isAir()) {
                level.setBlock(blockPos, blockState, FLAG);
            }
        }

        int x = -diameter / 2;

        for (z = -diameter / 2 + 1; z < diameter / 2; z++) {

            BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

            BlockState blockState = config.mushroomBlock().getState(level, random, origin);

            if (level.getBlockState(blockPos).isAir()) {
                level.setBlock(blockPos, blockState, FLAG);
            }
        }

        x = diameter / 2;

        for (z = -diameter / 2 + 1; z < diameter / 2; z++) {

            BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

            BlockState blockState = config.mushroomBlock().getState(level, random, origin);

            if (level.getBlockState(blockPos).isAir()) {
                level.setBlock(blockPos, blockState, FLAG);
            }
        }

        return true;
    }

    public record Config(BlockStateProvider mushroomBlock, IntProvider diameter) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(BlockStateProvider.CODEC.fieldOf("mushroom_block").forGetter(Config::mushroomBlock), IntProviders.codec(3, 7).fieldOf("diameter").forGetter(Config::diameter)).apply(codec, Config::new));
    }
}
