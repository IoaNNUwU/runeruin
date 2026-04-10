package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

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

        int diameter = config.diameter().sample(random);

        if (isValidWallMushroomPlacement(level, origin)) {

            for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {
                for (int z = -diameter / 2 + 1; z < diameter / 2; z++) {

                    BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                    BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                    if (level.getBlockState(blockPos).isAir()) {
                        level.setBlock(blockPos, blockState, 1);
                    }
                }
            }

            int z = -diameter / 2;

            for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {

                BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                if (level.getBlockState(blockPos).isAir()) {
                    level.setBlock(blockPos, blockState, 1);
                }
            }

            z = diameter / 2;

            for (int x = -diameter / 2 + 1; x < diameter / 2; x++) {

                BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                if (level.getBlockState(blockPos).isAir()) {
                    level.setBlock(blockPos, blockState, 1);
                }
            }

            int x = -diameter / 2;

            for (z = -diameter / 2 + 1; z < diameter / 2; z++) {

                BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                if (level.getBlockState(blockPos).isAir()) {
                    level.setBlock(blockPos, blockState, 1);
                }
            }

            x = diameter / 2;

            for (z = -diameter / 2 + 1; z < diameter / 2; z++) {

                BlockPos blockPos = new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z);

                BlockState blockState = config.mushroomBlock().getState(level, random, origin);

                if (level.getBlockState(blockPos).isAir()) {
                    level.setBlock(blockPos, blockState, 1);
                }
            }

            return true;
        }
        return false;
    }

    private static boolean isValidWallMushroomPlacement(WorldGenLevel level, BlockPos origin) {

        if (!level.getBlockState(origin).is(Tags.Blocks.STONES)) {
            return false;
        }

        var noMushroomsNeighbours = List.of(
                origin.north().above(),
                origin.north().below(),
                origin.south().above(),
                origin.south().below(),
                origin.west().above(),
                origin.west().below(),
                origin.east().above(),
                origin.east().below()
        );

        for (BlockPos blockPos : noMushroomsNeighbours) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.RED_MUSHROOM_BLOCK) || blockState.is(Blocks.BROWN_MUSHROOM_BLOCK)) {
                return false;
            }
        }

        int holeCount = 0;

        if (level.isEmptyBlock(origin.north())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.south())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.west())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.east())) {
            holeCount++;
        }

        int blockCount = 0;

        if (level.getBlockState(origin.north()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.south()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.east()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.west()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }

        return blockCount > 0 && holeCount > 0;
    }

    public record Config(
            BlockStateProvider mushroomBlock,
            IntProvider diameter
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("mushroom_block").forGetter(Config::mushroomBlock),
                        IntProviders.codec(3, 7).fieldOf("diameter").forGetter(Config::diameter)
                ).apply(codec, Config::new));
    }
}
