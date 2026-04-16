package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.GeometryUtils;
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

import java.util.List;

public class CeilingBallFeature extends Feature<CeilingBallFeature.Config> {

    public CeilingBallFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);
        BlockState ballBlock = config.ballBlock.getState(level, random, origin);

        float maxLength = config.maxTrunkLength.sample(random);
        int trunkLength = (int) (maxLength / 4 + (maxLength / 4 * 3) * random.nextFloat());

        float maxRadius = config.maxRadius.sample(random);
        int radius = (int) (maxRadius / 2 + (maxRadius / 2) * random.nextFloat());

        // --- Check if there is enough space ---

        List<BlockPos> noBlocks = List.of(
                origin.above(3).north(5),
                origin.above(3).south(5),
                origin.above(3).east(5),
                origin.above(3).west(5)
        );

        for (BlockPos blockPos : noBlocks) {
            if (level.isEmptyBlock(blockPos)) {
                return false;
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -trunkLength; y < -2; y++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    if (!level.isEmptyBlock(new BlockPos(xx, yy, zz))) {
                        return false;
                    }
                }
            }
        }

        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                for (int y = -trunkLength - radius * 2; y < -trunkLength; y++) {
                    int xx = origin.getX() + x;
                    int zz = origin.getZ() + z;
                    int yy = origin.getY() + y;

                    if (!level.isEmptyBlock(new BlockPos(xx, yy, zz))) {
                        return false;
                    }
                }
            }
        }

        int trunkDiameter = 3;

        // --- Roots ---
        for (int x = -2; x < trunkDiameter; x++) {
            for (int z = -1; z < trunkDiameter - 1; z++) {
                for (int y = 0; y < trunkDiameter; y++) {
                    int xx = origin.getX() + x - trunkDiameter / 2 + 1;
                    int yy = origin.getY() + y - trunkDiameter / 2;
                    int zz = origin.getZ() + z - trunkDiameter / 2 + 1;

                    level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
                }
            }
        }
        for (int x = -1; x < trunkDiameter - 1; x++) {
            for (int z = -2; z < trunkDiameter; z++) {
                for (int y = 0; y < trunkDiameter; y++) {
                    int xx = origin.getX() + x - trunkDiameter / 2 + 1;
                    int yy = origin.getY() + y - trunkDiameter / 2;
                    int zz = origin.getZ() + z - trunkDiameter / 2 + 1;

                    level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
                }
            }
        }
        for (int z = -1; z < trunkDiameter - 1; z++) {
            for (int x = -1; x < trunkDiameter - 1; x++) {
                int xx = origin.getX() + x - trunkDiameter / 2 + 1;
                int zz = origin.getZ() + z - trunkDiameter / 2 + 1;
                int yy = origin.getY() + 1;

                level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
            }
        }

        for (int x = -2; x < trunkDiameter; x++) {
            for (int z = -1; z < trunkDiameter - 1; z++) {
                int xx = origin.getX() + x - trunkDiameter / 2 + 1;
                int zz = origin.getZ() + z - trunkDiameter / 2 + 1;
                int yy = origin.getY() - trunkDiameter / 2 + 2;

                if (random.nextBoolean()) {
                    level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
                }
            }
        }
        for (int x = -1; x < trunkDiameter - 1; x++) {
            for (int z = -2; z < trunkDiameter; z++) {
                int xx = origin.getX() + x - trunkDiameter / 2 + 1;
                int zz = origin.getZ() + z - trunkDiameter / 2 + 1;
                int yy = origin.getY() - trunkDiameter / 2 + 2;

                if (random.nextBoolean()) {
                    level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
                }
            }
        }
        // --- Additional Trunk blocks ---
        BlockPos yOrigin1 = origin.below(2);
        BlockPos yOrigin2 = origin.below(3);
        List<BlockPos> additionalTrunkBlocks = List.of(
                yOrigin1.north(2),
                yOrigin1.south(2),
                yOrigin1.east(2),
                yOrigin1.west(2),
                yOrigin1.north().west(),
                yOrigin1.north().east(),
                yOrigin1.south().west(),
                yOrigin1.south().east(),
                yOrigin2.north().west(),
                yOrigin2.north().east(),
                yOrigin2.south().west(),
                yOrigin2.south().east()
        );
        for (BlockPos block : additionalTrunkBlocks) {
            level.setBlock(block, trunkBlock, 1);
        }

        // --- Trunk ---

        for (int y = 0; y < trunkLength; y++) {
            BlockPos yOrigin = origin.below(y);

            List<BlockPos> yBlocksToSet = List.of(
                    yOrigin,
                    yOrigin.north(),
                    yOrigin.south(),
                    yOrigin.west(),
                    yOrigin.east()
            );

            for (BlockPos block : yBlocksToSet) {
                level.setBlock(block, trunkBlock, 1);
            }
        }

        // --- Sphere ---

        GeometryUtils.sphere(
                level,
                origin.below(trunkLength + radius),
                () -> random.nextBoolean() ? trunkBlock : Blocks.AIR.defaultBlockState(),
                radius + 1
        );

        GeometryUtils.sphere(
                level,
                origin.below(trunkLength + radius),
                () -> ballBlock,
                radius
        );

        return true;
    }

    public record Config(
            BlockStateProvider trunkBlock,
            BlockStateProvider ballBlock,
            IntProvider maxTrunkLength,
            IntProvider maxRadius
    ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                        BlockStateProvider.CODEC.fieldOf("ball_block").forGetter(Config::ballBlock),
                        IntProviders.CODEC.fieldOf("max_trunk_length").forGetter(Config::maxTrunkLength),
                        IntProviders.CODEC.fieldOf("max_radius").forGetter(Config::maxRadius)
                ).apply(codec, Config::new)
        );

    }
}
