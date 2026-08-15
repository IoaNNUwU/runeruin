package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class CeilingBallFeature extends Feature<CeilingBallFeature.Config> {

    private static final int[][] CEILING_ANCHORS = {
            {0, 3, -5},
            {0, 3, 5},
            {5, 3, 0},
            {-5, 3, 0}
    };

    private static final int[][] TRUNK_CROSS = {
            {0, 0},
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
    };

    private static final int[][] EXTRA_TRUNK = {
            {0, -2, -2},
            {0, -2, 2},
            {2, -2, 0},
            {-2, -2, 0},
            {-1, -2, -1},
            {1, -2, -1},
            {-1, -2, 1},
            {1, -2, 1},
            {-1, -3, -1},
            {1, -3, -1},
            {-1, -3, 1},
            {1, -3, 1}
    };

    private static final int[][] TRUNK_BOTTOM_A = {
            {-1, 0, -2},
            {-2, 0, -1},
            {-1, 2, -1},
            {1, 0, 2},
            {2, 0, 1},
            {1, 2, 1}
    };

    private static final int[][] TRUNK_BOTTOM_B = {
            {1, 0, -2},
            {2, 0, -1},
            {1, 2, -1},
            {-1, 0, 2},
            {-2, 0, 1},
            {-1, 2, 1}
    };

    private static final int[][] THORNS_A = {
            {-1, 0, -1},
            {-1, 1, -1},
            {1, 0, -1},
            {1, -1, -1},
            {-1, 0, 1},
            {-1, -1, 1},
            {1, 0, 1},
            {1, 1, 1}
    };

    private static final int[][] THORNS_B = {
            {-1, 0, -1},
            {-1, -1, -1},
            {1, 0, -1},
            {1, 1, -1},
            {-1, 0, 1},
            {-1, 1, 1},
            {1, 0, 1},
            {1, -1, 1}
    };

    public CeilingBallFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);
        BlockState ballBlock = config.ballBlock.getState(level, random, origin);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState cobweb = Blocks.COBWEB.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();

        float maxLength = config.maxTrunkLength.sample(random);
        int trunkLength = (int) (maxLength / 4 + (maxLength * 3 / 4) * random.nextFloat());

        float maxRadius = config.maxRadius.sample(random);
        int radius = (int) (maxRadius / 2 + (maxRadius / 2) * random.nextFloat());

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        for (int[] offset : CEILING_ANCHORS) {
            if (level.isEmptyBlock(mutable.set(ox + offset[0], oy + offset[1], oz + offset[2]))) {
                return false;
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -trunkLength; y < -2; y++) {
                    if (!level.isEmptyBlock(mutable.set(ox + x, oy + y, oz + z))) {
                        return false;
                    }
                }
            }
        }

        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                for (int y = -trunkLength - radius * 2; y < -trunkLength; y++) {
                    if (!level.isEmptyBlock(mutable.set(ox + x, oy + y, oz + z))) {
                        return false;
                    }
                }
            }
        }

        int trunkDiameter = 3;

        for (int x = -2; x < trunkDiameter; x++) {
            for (int z = -1; z < trunkDiameter - 1; z++) {
                for (int y = 0; y < trunkDiameter; y++) {
                    int xx = ox + x - trunkDiameter / 2 + 1;
                    int yy = oy + y - trunkDiameter / 2;
                    int zz = oz + z - trunkDiameter / 2 + 1;
                    setSolid(level, mutable, xx, yy, zz, trunkBlock);
                }
            }
        }
        for (int x = -1; x < trunkDiameter - 1; x++) {
            for (int z = -2; z < trunkDiameter; z++) {
                for (int y = 0; y < trunkDiameter; y++) {
                    int xx = ox + x - trunkDiameter / 2 + 1;
                    int yy = oy + y - trunkDiameter / 2;
                    int zz = oz + z - trunkDiameter / 2 + 1;
                    setSolid(level, mutable, xx, yy, zz, trunkBlock);
                }
            }
        }
        for (int z = -1; z < trunkDiameter - 1; z++) {
            for (int x = -1; x < trunkDiameter - 1; x++) {
                int xx = ox + x - trunkDiameter / 2 + 1;
                int zz = oz + z - trunkDiameter / 2 + 1;
                setSolid(level, mutable, xx, oy + 1, zz, trunkBlock);
            }
        }

        for (int x = -2; x < trunkDiameter; x++) {
            for (int z = -1; z < trunkDiameter - 1; z++) {
                if (random.nextBoolean()) {
                    int xx = ox + x - trunkDiameter / 2 + 1;
                    int zz = oz + z - trunkDiameter / 2 + 1;
                    int yy = oy - trunkDiameter / 2 + 2;
                    setSolid(level, mutable, xx, yy, zz, trunkBlock);
                }
            }
        }
        for (int x = -1; x < trunkDiameter - 1; x++) {
            for (int z = -2; z < trunkDiameter; z++) {
                if (random.nextBoolean()) {
                    int xx = ox + x - trunkDiameter / 2 + 1;
                    int zz = oz + z - trunkDiameter / 2 + 1;
                    int yy = oy - trunkDiameter / 2 + 2;
                    setSolid(level, mutable, xx, yy, zz, trunkBlock);
                }
            }
        }

        for (int[] offset : EXTRA_TRUNK) {
            setSolid(level, mutable, ox + offset[0], oy + offset[1], oz + offset[2], trunkBlock);
        }

        int bx = ox;
        int by = oy - (trunkLength - 1);
        int bz = oz;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    setSolid(level, mutable, bx + x, by + y, bz + z, trunkBlock);
                }
            }
        }

        int[][] moreBlocks = random.nextBoolean() ? TRUNK_BOTTOM_A : TRUNK_BOTTOM_B;
        for (int[] offset : moreBlocks) {
            setSolid(level, mutable, bx + offset[0], by + offset[1], bz + offset[2], trunkBlock);
        }

        for (int y = 0; y < trunkLength; y++) {
            int yy = oy - y;
            for (int[] offset : TRUNK_CROSS) {
                setSolid(level, mutable, ox + offset[0], yy, oz + offset[1], trunkBlock);
            }
        }

        int segmentLength = 7;
        int segmentCount = trunkLength / segmentLength;
        for (int nSegment = 0; nSegment < segmentCount; nSegment++) {
            int yy = oy - (nSegment * segmentLength + segmentLength / 2 - trunkLength % segmentCount);
            int[][] thorns = random.nextBoolean() ? THORNS_A : THORNS_B;
            for (int[] offset : thorns) {
                setSolid(level, mutable, ox + offset[0], yy + offset[1], oz + offset[2], trunkBlock);
            }
        }

        BlockPos center = origin.below(trunkLength + radius);
        int spawnerRand = random.nextIntBetweenInclusive(0, 4);

        GeometryUtils.forEachInSphere(center, radius + 1, (x, y, z, d2) -> {
            BlockState toPlace;
            if (GeometryUtils.insideSphere(d2, radius - 3)) {
                toPlace = switch (spawnerRand) {
                    case 0, 1, 2 -> air;
                    default -> stone;
                };
            } else if (GeometryUtils.insideSphere(d2, radius - 2)) {
                toPlace = switch (spawnerRand) {
                    case 0, 1 -> random.nextBoolean() ? cobweb : air;
                    case 2 -> air;
                    default -> stone;
                };
            } else if (GeometryUtils.insideSphere(d2, radius - 1)) {
                toPlace = switch (spawnerRand) {
                    case 0, 1 -> trunkBlock;
                    case 2 -> random.nextBoolean() ? trunkBlock : air;
                    default -> stone;
                };
            } else if (GeometryUtils.insideSphere(d2, radius)) {
                toPlace = ballBlock;
            } else {
                toPlace = random.nextBoolean() ? trunkBlock : air;
            }
            int flags = toPlace.isAir() ? GeometryUtils.BULK_FLAG : GeometryUtils.SOLID_FLAG;
            level.setBlock(mutable.set(x, y, z), toPlace, flags);
        });

        switch (spawnerRand) {
            case 0, 1 -> {
                BlockState spawnerBlockState = Blocks.SPAWNER.defaultBlockState();
                level.setBlock(center, spawnerBlockState, Block.UPDATE_ALL);
                BlockEntity blockEntity = level.getBlockEntity(center);
                if (blockEntity instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId(EntityTypes.CAVE_SPIDER, random);
                } else {
                    RR.LOGGER.warn("SpawnerBlockEntity generated in CeilingBallFeature is unaccessible");
                }

                BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
                for (int y = 1; y < radius - 2; y++) {
                    level.setBlock(mutable.set(center.getX(), center.getY() + y, center.getZ()), chain, Block.UPDATE_ALL);
                }

                BlockState bar = Blocks.IRON_BARS.defaultBlockState();

                var north = CrossCollisionBlock.NORTH;
                var south = CrossCollisionBlock.SOUTH;
                var west = CrossCollisionBlock.WEST;
                var east = CrossCollisionBlock.EAST;

                if (radius > 6) {
                    int cx = center.getX();
                    int cy = center.getY();
                    int cz = center.getZ();
                    for (int y = -1; y <= 1; y++) {
                        int yy = cy + y;
                        level.setBlock(mutable.set(cx + 1, yy, cz), bar.setValue(south, true).setValue(north, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx - 1, yy, cz), bar.setValue(south, true).setValue(north, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx, yy, cz - 1), bar.setValue(west, true).setValue(east, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx, yy, cz + 1), bar.setValue(west, true).setValue(east, true), Block.UPDATE_ALL);

                        level.setBlock(mutable.set(cx + 1, yy, cz - 1), bar.setValue(south, true).setValue(west, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx + 1, yy, cz + 1), bar.setValue(north, true).setValue(west, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx - 1, yy, cz - 1), bar.setValue(south, true).setValue(east, true), Block.UPDATE_ALL);
                        level.setBlock(mutable.set(cx - 1, yy, cz + 1), bar.setValue(north, true).setValue(east, true), Block.UPDATE_ALL);
                    }

                    setSolid(level, mutable, cx, cy + 1, cz, trunkBlock);

                    BlockPos chestPos = center.below();
                    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                    BlockEntity chestBlockEntity = level.getBlockEntity(chestPos);

                    if (chestBlockEntity instanceof ChestBlockEntity chest) {
                        chest.setLootTable(BuiltInLootTables.ABANDONED_MINESHAFT, random.nextLong());
                    } else {
                        RR.LOGGER.warn("ChestBlockEntity generated in CeilingBallFeature is unaccessible");
                    }
                }
            }
            default -> {
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                setSolid(level, mutable, bx + x, by - 1, bz + z, trunkBlock);
            }
        }

        return true;
    }

    private static void setSolid(WorldGenLevel level, BlockPos.MutableBlockPos mutable, int x, int y, int z, BlockState state) {
        level.setBlock(mutable.set(x, y, z), state, GeometryUtils.SOLID_FLAG);
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
