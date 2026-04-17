package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
        int trunkLength = (int) (maxLength / 4 + (maxLength * 3 / 4) * random.nextFloat());

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

        // --- Additional blocks at the bottom of the Trunk ---
        BlockPos trunkBottom = origin.below(trunkLength - 1);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    int xx = trunkBottom.getX() + x;
                    int zz = trunkBottom.getZ() + z;
                    int yy = trunkBottom.getY() + y;

                    level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
                }
            }
        }

        List<BlockPos> moreBlocks;
        if (random.nextBoolean()) {
            moreBlocks = List.of(
                    trunkBottom.north(2).west(1),
                    trunkBottom.north(1).west(2),
                    trunkBottom.north(1).west(1).above(2),
                    trunkBottom.north(-2).west(-1),
                    trunkBottom.north(-1).west(-2),
                    trunkBottom.north(-1).west(-1).above(2)
            );
        } else {
            moreBlocks = List.of(
                    trunkBottom.north(2).west(-1),
                    trunkBottom.north(1).west(-2),
                    trunkBottom.north(1).west(-1).above(2),
                    trunkBottom.north(-2).west(1),
                    trunkBottom.north(-1).west(2),
                    trunkBottom.north(-1).west(1).above(2)
            );
        }
        for (BlockPos blockPos : moreBlocks) {
            level.setBlock(blockPos, trunkBlock, 1);
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

        // --- Additional thorns in the middle of the trunk ---

        int segmentLength = 7;
        int segmentCount = trunkLength / segmentLength;
        for (int nSegment = 0; nSegment < segmentCount; nSegment++) {
            BlockPos yOrigin = origin.below(nSegment * segmentLength + segmentLength / 2 - trunkLength % segmentCount);

            List<BlockPos> additionalBlocks;
            if (random.nextBoolean()) {
                additionalBlocks = List.of(
                        yOrigin.north().west(),
                        yOrigin.north().west().above(),
                        yOrigin.north().east(),
                        yOrigin.north().east().below(),
                        yOrigin.south().west(),
                        yOrigin.south().west().above(),
                        yOrigin.south().east(),
                        yOrigin.south().east().below()
                );
            } else {
                additionalBlocks = List.of(
                        yOrigin.north().west(),
                        yOrigin.north().west().below(),
                        yOrigin.north().east(),
                        yOrigin.north().east().above(),
                        yOrigin.south().west(),
                        yOrigin.south().west().above(),
                        yOrigin.south().east(),
                        yOrigin.south().east().below()
                );
            }

            for (BlockPos blockPos : additionalBlocks) {
                level.setBlock(blockPos, trunkBlock, 1);
            }
        }


        // --- Sphere ---

        BlockPos center = origin.below(trunkLength + radius);

        GeometryUtils.sphere(
                level,
                center,
                () -> random.nextBoolean() ? trunkBlock : Blocks.AIR.defaultBlockState(),
                radius + 1
        );
        GeometryUtils.sphere(
                level,
                center,
                () -> ballBlock,
                radius
        );

        int spawnerRand = random.nextIntBetweenInclusive(0, 4);

        switch (spawnerRand) {
            case 0, 1 -> {
                // --- Generate spawner room ---
                GeometryUtils.sphere(level, center, () -> trunkBlock, radius - 1);

                Supplier<BlockState> cobwebToPlace = () -> random.nextBoolean()
                        ? Blocks.COBWEB.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();

                GeometryUtils.sphere(level, center, cobwebToPlace, radius - 2);
                GeometryUtils.sphere(level, center, Blocks.AIR::defaultBlockState, radius - 3);

                BlockState spawnerBlockState = Blocks.SPAWNER.defaultBlockState();

                level.setBlock(center, spawnerBlockState, 1);
                BlockEntity blockEntity = level.getBlockEntity(center);
                if (blockEntity instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId(EntityType.CAVE_SPIDER, random);
                } else {
                    RR.LOGGER.warn("SpawnerBlockEntity from generated spawner is unaccessible");
                }

                for (int y = 1; y < radius - 2; y++) {
                    level.setBlock(center.above(y), Blocks.IRON_CHAIN.defaultBlockState(), Block.UPDATE_ALL);
                }

                BlockState bar = Blocks.IRON_BARS.defaultBlockState();

                var north = CrossCollisionBlock.NORTH;
                var south = CrossCollisionBlock.SOUTH;
                var west = CrossCollisionBlock.WEST;
                var east = CrossCollisionBlock.EAST;

                if (radius > 4) {
                    for (int y = -1; y <= 1; y++) {
                        BlockPos yCenter = center.above(y);

                        level.setBlock(yCenter.east(), bar.setValue(south, true).setValue(north, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.west(), bar.setValue(south, true).setValue(north, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.north(), bar.setValue(west, true).setValue(east, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.south(), bar.setValue(west, true).setValue(east, true), Block.UPDATE_ALL);

                        level.setBlock(yCenter.east().north(), bar.setValue(south, true).setValue(west, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.east().south(), bar.setValue(north, true).setValue(west, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.west().north(), bar.setValue(south, true).setValue(east, true), Block.UPDATE_ALL);
                        level.setBlock(yCenter.west().south(), bar.setValue(north, true).setValue(east, true), Block.UPDATE_ALL);
                    }

                    level.setBlock(center.above(), trunkBlock, Block.UPDATE_ALL);
                    level.setBlock(center.below(), trunkBlock, Block.UPDATE_ALL);
                }
            }
            case 2 -> {
                // --- Generate empty room ---
                Supplier<BlockState> blockToPlace = () -> random.nextBoolean() ? trunkBlock : Blocks.AIR.defaultBlockState();

                GeometryUtils.sphere(level, center, blockToPlace, radius - 1);
                GeometryUtils.sphere(level, center, Blocks.AIR::defaultBlockState, radius - 2);
            }
            case 3, 4 -> {
                GeometryUtils.sphere(level, center, Blocks.STONE::defaultBlockState, radius - 1);
                // TODO: Generate extensive amount of custom ores
            }
        }

        // --- Additional blocks at the bottom of the Trunk ---
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int xx = trunkBottom.getX() + x;
                int zz = trunkBottom.getZ() + z;
                int yy = trunkBottom.getY() - 1;

                level.setBlock(new BlockPos(xx, yy, zz), trunkBlock, 1);
            }
        }

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
