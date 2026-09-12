package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
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

        int ballExtent = radius + 2;
        for (int x = -ballExtent; x <= ballExtent; x++) {
            for (int z = -ballExtent; z <= ballExtent; z++) {
                for (int y = -trunkLength - radius * 2 - 2; y <= -trunkLength + 2; y++) {
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
                toPlace = air;
            }
            int flags = toPlace.isAir() ? GeometryUtils.BULK_FLAG : GeometryUtils.SOLID_FLAG;
            level.setBlock(mutable.set(x, y, z), toPlace, flags);
        });

        placeBranchWeb(level, center, radius, trunkBlock, random);

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

    /** Builds a lightly irregular geodesic lattice just outside the ball's shell. */
    private static void placeBranchWeb(WorldGenLevel level, BlockPos center, int radius, BlockState branchBlock, RandomSource random) {
        double shellRadius = radius + 0.35;
        int siteCount = Math.max(24, Math.min(72, (int) Math.round(shellRadius * shellRadius * 0.45)));
        UnitVector[] sites = fibonacciSphere(siteCount, random);
        Set<Long> edges = new LinkedHashSet<>();
        for (int i = 0; i < sites.length; i++) {
            for (int neighbor : closestSites(sites, i, 4)) {
                int low = Math.min(i, neighbor);
                int high = Math.max(i, neighbor);
                edges.add(((long) low << 32) | (high & 0xffffffffL));
            }
        }

        Map<BlockPos, Direction.Axis> branches = new HashMap<>();
        int outerRadiusSquared = (radius + 2) * (radius + 2);
        for (long edge : edges) {
            UnitVector start = sites[(int) (edge >>> 32)];
            UnitVector end = sites[(int) edge];
            double dot = clamp(start.dot(end), -1.0, 1.0);
            double angle = Math.acos(dot);
            if (angle < 1.0e-4) {
                continue;
            }

            UnitVector side = start.cross(end).normalized();
            double sinAngle = Math.sin(angle);
            double phase = random.nextDouble() * Math.PI * 2.0;
            int samples = Math.max(4, (int) Math.ceil(angle * shellRadius * 2.5));
            int previousX = Integer.MIN_VALUE;
            int previousY = Integer.MIN_VALUE;
            int previousZ = Integer.MIN_VALUE;
            for (int sample = 0; sample <= samples; sample++) {
                double t = sample / (double) samples;
                UnitVector point;
                if (sinAngle < 1.0e-5) {
                    point = start;
                } else {
                    double a = Math.sin((1.0 - t) * angle) / sinAngle;
                    double b = Math.sin(t * angle) / sinAngle;
                    point = start.scale(a).add(end.scale(b));
                }

                double wobble = Math.sin(Math.PI * t) * Math.sin(Math.PI * 2.0 * t + phase) * 0.045;
                point = point.add(side.scale(wobble)).normalized();

                int x = center.getX() + (int) Math.round(point.x * shellRadius);
                int y = center.getY() + (int) Math.round(point.y * shellRadius);
                int z = center.getZ() + (int) Math.round(point.z * shellRadius);
                if (x == previousX && y == previousY && z == previousZ) {
                    continue;
                }

                Direction.Axis axis = previousX == Integer.MIN_VALUE
                        ? Direction.Axis.Y
                        : dominantAxis(x - previousX, y - previousY, z - previousZ);
                previousX = x;
                previousY = y;
                previousZ = z;
                int dx = x - center.getX();
                int dy = y - center.getY();
                int dz = z - center.getZ();
                int distanceSquared = dx * dx + dy * dy + dz * dz;
                if (distanceSquared <= radius * radius || distanceSquared > outerRadiusSquared) {
                    continue;
                }

                branches.putIfAbsent(new BlockPos(x, y, z), axis);
            }
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean isLog = branchBlock.getBlock() instanceof RotatedPillarBlock;
        for (Map.Entry<BlockPos, Direction.Axis> branch : branches.entrySet()) {
            BlockState state = isLog
                    ? branchBlock.setValue(RotatedPillarBlock.AXIS, branch.getValue())
                    : branchBlock;
            BlockPos pos = branch.getKey();
            level.setBlock(mutable.set(pos.getX(), pos.getY(), pos.getZ()), state, GeometryUtils.SOLID_FLAG);
        }
    }

    private static UnitVector[] fibonacciSphere(int count, RandomSource random) {
        UnitVector[] sites = new UnitVector[count];
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        double yaw = random.nextDouble() * Math.PI * 2.0;
        double tilt = random.nextDouble() * Math.PI * 2.0;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosTilt = Math.cos(tilt);
        double sinTilt = Math.sin(tilt);

        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double ring = Math.sqrt(1.0 - y * y);
            double angle = goldenAngle * i;
            double x = ring * Math.cos(angle);
            double z = ring * Math.sin(angle);

            // Rotate the evenly spaced sites so each generated ball has a distinct orientation.
            double tiltedY = y * cosTilt - z * sinTilt;
            double tiltedZ = y * sinTilt + z * cosTilt;
            sites[i] = new UnitVector(
                    x * cosYaw - tiltedZ * sinYaw,
                    tiltedY,
                    x * sinYaw + tiltedZ * cosYaw
            );
        }
        return sites;
    }

    private static int[] closestSites(UnitVector[] sites, int source, int count) {
        int[] closest = new int[count];
        double[] scores = new double[count];
        for (int i = 0; i < count; i++) {
            closest[i] = -1;
            scores[i] = -Double.MAX_VALUE;
        }

        for (int candidate = 0; candidate < sites.length; candidate++) {
            if (candidate == source) {
                continue;
            }
            double score = sites[source].dot(sites[candidate]);
            for (int slot = 0; slot < count; slot++) {
                if (score <= scores[slot]) {
                    continue;
                }
                for (int shift = count - 1; shift > slot; shift--) {
                    scores[shift] = scores[shift - 1];
                    closest[shift] = closest[shift - 1];
                }
                scores[slot] = score;
                closest[slot] = candidate;
                break;
            }
        }
        return closest;
    }

    private static Direction.Axis dominantAxis(int dx, int dy, int dz) {
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);
        int absZ = Math.abs(dz);
        if (absX >= absY && absX >= absZ) {
            return Direction.Axis.X;
        }
        return absY >= absZ ? Direction.Axis.Y : Direction.Axis.Z;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record UnitVector(double x, double y, double z) {
        UnitVector add(UnitVector other) {
            return new UnitVector(x + other.x, y + other.y, z + other.z);
        }

        UnitVector scale(double factor) {
            return new UnitVector(x * factor, y * factor, z * factor);
        }

        UnitVector cross(UnitVector other) {
            return new UnitVector(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        double dot(UnitVector other) {
            return x * other.x + y * other.y + z * other.z;
        }

        UnitVector normalized() {
            double length = Math.sqrt(dot(this));
            return length < 1.0e-8 ? this : scale(1.0 / length);
        }
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
