package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.DripstoneUtils;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

public class MossySpikeFeature extends Feature<LargeDripstoneConfiguration> {
    public MossySpikeFeature() {
        super(LargeDripstoneConfiguration.CODEC);
    }

    public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        LargeDripstoneConfiguration config = (LargeDripstoneConfiguration)context.config();
        RandomSource random = context.random();
        if (!level.isStateAtPosition(origin, DripstoneUtils::isEmptyOrWater)) {
            return false;
        } else {

            Predicate<BlockState> isDripstoneCapable = block -> block.is(Tags.Blocks.STONES) || block.is(Blocks.MOSS_BLOCK);

            Optional<Column> column = Column.scan(level, origin, config.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, isDripstoneCapable);
            if (!column.isEmpty() && column.get() instanceof Column.Range) {
                Column.Range columnRange = (Column.Range)column.get();
                if (columnRange.height() < 4) {
                    return false;
                } else {
                    int maxColumnRadiusBasedOnColumnHeight = (int)((float)columnRange.height() * config.maxColumnRadiusToCaveHeightRatio);
                    int maxColumnRadius = Mth.clamp(maxColumnRadiusBasedOnColumnHeight, config.columnRadius.minInclusive(), config.columnRadius.maxInclusive());
                    int radius = Mth.randomBetweenInclusive(random, config.columnRadius.minInclusive(), maxColumnRadius);
                    LargeDripstone stalactite = makeDripstone(origin.atY(columnRange.ceiling() - 1), false, random, radius, config.stalactiteBluntness, config.heightScale);
                    LargeDripstone stalagmite = makeDripstone(origin.atY(columnRange.floor() + 1), true, random, radius, config.stalagmiteBluntness, config.heightScale);
                    WindOffsetter wind;
                    if (stalactite.isSuitableForWind(config) && stalagmite.isSuitableForWind(config)) {
                        wind = new WindOffsetter(origin.getY(), random, config.windSpeed);
                    } else {
                        wind = WindOffsetter.noWind();
                    }

                    boolean stalactiteBaseEmbeddedInStone = stalactite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
                    boolean stalagmiteBaseEmbeddedInStone = stalagmite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
                    if (stalactiteBaseEmbeddedInStone) {
                        stalactite.placeBlocks(level, random, wind);
                    }

                    if (stalagmiteBaseEmbeddedInStone) {
                        stalagmite.placeBlocks(level, random, wind);
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static LargeDripstone makeDripstone(BlockPos root, boolean pointingUp, RandomSource random, int radius, FloatProvider bluntness, FloatProvider heightScale) {
        return new LargeDripstone(root, pointingUp, radius, (double)bluntness.sample(random), (double)heightScale.sample(random));
    }

    private void placeDebugMarkers(WorldGenLevel level, BlockPos origin, Column.Range range, WindOffsetter wind) {
        level.setBlock(wind.offset(origin.atY(range.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        level.setBlock(wind.offset(origin.atY(range.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        for(BlockPos.MutableBlockPos pos = origin.atY(range.floor() + 2).mutable(); pos.getY() < range.ceiling() - 1; pos.move(Direction.UP)) {
            BlockPos windAdjustedPos = wind.offset(pos);
            if (level.isStateAtPosition(windAdjustedPos, DripstoneUtils::isEmptyOrWater) || level.getBlockState(windAdjustedPos).is(Blocks.DRIPSTONE_BLOCK)) {
                level.setBlock(windAdjustedPos, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
            }
        }

    }

    private static final class LargeDripstone {
        private BlockPos root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        private LargeDripstone(BlockPos root, boolean pointingUp, int radius, double bluntness, double scale) {
            this.root = root;
            this.pointingUp = pointingUp;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
        }

        private int getHeight() {
            return this.getHeightAtRadius(0.0F);
        }

        private int getMinY() {
            return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
        }

        private int getMaxY() {
            return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
        }

        private boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel level, WindOffsetter wind) {
            while(this.radius > 1) {
                BlockPos.MutableBlockPos newRoot = this.root.mutable();
                int maxTries = Math.min(10, this.getHeight());

                for(int i = 0; i < maxTries; ++i) {
                    if (level.getBlockState(newRoot).is(Blocks.LAVA)) {
                        return false;
                    }

                    if (isCircleMostlyEmbeddedInStone(level, wind.offset(newRoot), this.radius)) {
                        this.root = newRoot;
                        return true;
                    }

                    newRoot.move(this.pointingUp ? Direction.DOWN : Direction.UP);
                }

                this.radius /= 2;
            }

            return false;
        }

        private int getHeightAtRadius(float checkRadius) {
            return (int)getDripstoneHeight((double)checkRadius, (double)this.radius, this.scale, this.bluntness);
        }

        private void placeBlocks(WorldGenLevel level, RandomSource random, WindOffsetter wind) {
            for(int dx = -this.radius; dx <= this.radius; ++dx) {
                for(int dz = -this.radius; dz <= this.radius; ++dz) {
                    float currentRadius = Mth.sqrt((float)(dx * dx + dz * dz));
                    if (!(currentRadius > (float)this.radius)) {
                        int height = this.getHeightAtRadius(currentRadius);
                        if (height > 0) {
                            if ((double)random.nextFloat() < 0.2) {
                                height = (int)((float)height * Mth.randomBetween(random, 0.8F, 1.0F));
                            }

                            BlockPos.MutableBlockPos pos = this.root.offset(dx, 0, dz).mutable();
                            boolean hasBeenOutOfStone = false;
                            int maxY = this.pointingUp ? level.getHeight(Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) : Integer.MAX_VALUE;

                            for(int i = 0; i < height && pos.getY() < maxY; ++i) {
                                BlockPos windAdjustedPos = wind.offset(pos);
                                if (level.isStateAtPosition(windAdjustedPos, DripstoneUtils::isEmptyOrWaterOrLava)) {
                                    hasBeenOutOfStone = true;
                                    Block block = SharedConstants.DEBUG_LARGE_DRIPSTONE ? Blocks.GLASS : Blocks.DRIPSTONE_BLOCK;
                                    level.setBlock(windAdjustedPos, block.defaultBlockState(), 2);
                                } else if (hasBeenOutOfStone && level.getBlockState(windAdjustedPos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                                    break;
                                }

                                pos.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                            }
                        }
                    }
                }
            }

        }

        private boolean isSuitableForWind(LargeDripstoneConfiguration config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= (double)config.minBluntnessForWind;
        }
    }

    private static final class WindOffsetter {
        private final int originY;
        private final @Nullable Vec3 windSpeed;

        private WindOffsetter(int originY, RandomSource random, FloatProvider windSpeedRange) {
            this.originY = originY;
            float speed = windSpeedRange.sample(random);
            float direction = Mth.randomBetween(random, 0.0F, (float)Math.PI);
            this.windSpeed = new Vec3((double)(Mth.cos((double)direction) * speed), (double)0.0F, (double)(Mth.sin((double)direction) * speed));
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
        }

        private static WindOffsetter noWind() {
            return new WindOffsetter();
        }

        private BlockPos offset(BlockPos pos) {
            if (this.windSpeed == null) {
                return pos;
            } else {
                int dy = this.originY - pos.getY();
                Vec3 totalWindAdjust = this.windSpeed.scale((double)dy);
                return pos.offset(Mth.floor(totalWindAdjust.x), 0, Mth.floor(totalWindAdjust.z));
            }
        }
    }

    static double getDripstoneHeight(double xzDistanceFromCenter, double dripstoneRadius, double scale, double bluntness) {
        if (xzDistanceFromCenter < bluntness) {
            xzDistanceFromCenter = bluntness;
        }

        double cutoff = 0.384;
        double r = xzDistanceFromCenter / dripstoneRadius * 0.384;
        double part1 = (double)0.75F * Math.pow(r, 1.3333333333333333);
        double part2 = Math.pow(r, 0.6666666666666666);
        double part3 = 0.3333333333333333 * Math.log(r);
        double heightRelativeToMaxRadius = scale * (part1 - part2 - part3);
        heightRelativeToMaxRadius = Math.max(heightRelativeToMaxRadius, (double)0.0F);
        return heightRelativeToMaxRadius / 0.384 * dripstoneRadius;
    }

    static boolean isCircleMostlyEmbeddedInStone(WorldGenLevel level, BlockPos center, int xzRadius) {

        if (level.isStateAtPosition(center, DripstoneUtils::isEmptyOrWaterOrLava)) {
            return false;
        } else {

            float arcLength = 6.0F;
            float angleIncrement = 6.0F / (float)xzRadius;

            for(float angle = 0.0F; angle < ((float)Math.PI * 2F); angle += angleIncrement) {
                int dx = (int)(Mth.cos((double)angle) * (float)xzRadius);
                int dz = (int)(Mth.sin((double)angle) * (float)xzRadius);

                if (level.isStateAtPosition(center.offset(dx, 0, dz), DripstoneUtils::isEmptyOrWaterOrLava)) {
                    return false;
                }
            }

            return true;
        }
    }
}
