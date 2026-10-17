package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.structures.HangingTracksStructure;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpeleothemUtils;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/** A stalactite and a stalagmite shaped like vanilla large dripstone; where they grow through moss, they keep a cap of it. */
public class MossySpikeFeature extends Feature<MossySpikeFeature.Config> {

    public MossySpikeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Config config = context.config();
        RandomSource random = context.random();
        if (!level.isStateAtPosition(origin, SpeleothemUtils::isEmptyOrWater)) {
            return false;
        }

        BlockState mainBlock = config.block.getState(level, random, origin);
        Optional<Column> column = Column.scan(level, origin, config.floorToCeilingSearchRange, MossySpikeFeature::isInsideColumn, MossySpikeFeature::isSpikeBase);
        if (column.isEmpty() || !(column.get() instanceof Column.Range columnRange) || columnRange.height() < 4) {
            return false;
        }

        int maxColumnRadiusBasedOnColumnHeight = (int) (columnRange.height() * config.maxColumnRadiusToCaveHeightRatio);
        int maxColumnRadius = Mth.clamp(maxColumnRadiusBasedOnColumnHeight, config.columnRadius.minInclusive(), config.columnRadius.maxInclusive());
        int maxReach = maxReach(origin);
        int radius = Math.min(maxReach, Mth.randomBetweenInclusive(random, config.columnRadius.minInclusive(), maxColumnRadius));

        // Hanging tracks cut their way through the spikes of their own chunk. A spike of a chunk that is
        // decorated later would grow back into them, so it keeps out of their way, like the inverted trees.
        if (HangingTracksStructure.reaches(level, new BoundingBox(
                origin.getX() - radius, columnRange.floor(), origin.getZ() - radius,
                origin.getX() + radius, columnRange.ceiling(), origin.getZ() + radius))) {
            return false;
        }

        LargeDripstone stalactite = makeDripstone(origin.atY(columnRange.ceiling() - 1), false, random, radius, config.stalactiteBluntness, config.heightScale);
        LargeDripstone stalagmite = makeDripstone(origin.atY(columnRange.floor() + 1), true, random, radius, config.stalagmiteBluntness, config.heightScale);

        WindOffsetter wind = stalactite.isSuitableForWind(config) && stalagmite.isSuitableForWind(config)
                ? new WindOffsetter(origin.getY(), random, config.windSpeed, maxReach - radius)
                : WindOffsetter.noWind();

        boolean stalactiteBaseEmbeddedInStone = stalactite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
        boolean stalagmiteBaseEmbeddedInStone = stalagmite.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(level, wind);
        if (stalactiteBaseEmbeddedInStone) {
            stalactite.placeBlocks(level, random, wind, mainBlock, columnRange);
        }
        if (stalagmiteBaseEmbeddedInStone) {
            stalagmite.placeBlocks(level, random, wind, mainBlock, columnRange);
        }
        return true;
    }

    /**
     * How far from the origin along X and Z the spike may write. A feature may change only the chunk of
     * its origin and the eight around it: 16 blocks from an origin on a chunk edge, more towards the middle.
     * Keeping the radius and the wind shift within this takes the place of checking every block.
     */
    private static int maxReach(BlockPos origin) {
        int x = SectionPos.sectionRelative(origin.getX());
        int z = SectionPos.sectionRelative(origin.getZ());
        return 16 + Math.min(Math.min(x, 15 - x), Math.min(z, 15 - z));
    }

    private static boolean isInsideColumn(BlockState state) {
        return SpeleothemUtils.isEmptyOrWater(state) || isMossCarpet(state);
    }

    private static boolean isMossCarpet(BlockState state) {
        return state.is(RRBlocks.GLOWING_MOSS_CARPET.get())
                || state.is(RRBlocks.DEEP_MOSS_LAYER.get())
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.PALE_MOSS_CARPET);
    }

    private static boolean canReplaceWithSpike(BlockState state) {
        return SpeleothemUtils.isEmptyOrWaterOrLava(state)
                || isMossCarpet(state)
                || state.is(BlockTags.MOSS_BLOCKS);
    }

    private static boolean canReplaceWithMossCap(BlockState state) {
        return SpeleothemUtils.isEmptyOrWaterOrLava(state) || isMossCarpet(state);
    }

    private static boolean isSpikeBase(BlockState state) {
        return state.is(Tags.Blocks.STONES)
                || state.is(BlockTags.MOSS_BLOCKS)
                || state.is(RRBlocks.MOSS_LIGHT.get());
    }

    private static LargeDripstone makeDripstone(BlockPos root, boolean pointingUp, RandomSource random, int radius, FloatProvider bluntness, FloatProvider heightScale) {
        return new LargeDripstone(root, pointingUp, radius, bluntness.sample(random), heightScale.sample(random));
    }

    private static double getDripstoneHeight(double xzDistanceFromCenter, double dripstoneRadius, double scale, double bluntness) {
        double cutoff = 0.384;
        double r = Math.max(xzDistanceFromCenter, bluntness) / dripstoneRadius * cutoff;
        double part1 = 0.75 * Math.pow(r, 4.0 / 3.0);
        double part2 = Math.pow(r, 2.0 / 3.0);
        double part3 = (1.0 / 3.0) * Math.log(r);
        double heightRelativeToMaxRadius = Math.max(scale * (part1 - part2 - part3), 0.0);
        return heightRelativeToMaxRadius / cutoff * dripstoneRadius;
    }

    private static boolean isCircleMostlyEmbeddedInStone(WorldGenLevel level, BlockPos center, int xzRadius) {
        if (level.isStateAtPosition(center, MossySpikeFeature::canReplaceWithSpike)) {
            return false;
        }

        float arcLength = 6.0F;
        float angleIncrement = arcLength / xzRadius;
        for (float angle = 0.0F; angle < Mth.TWO_PI; angle += angleIncrement) {
            int dx = (int) (Mth.cos(angle) * xzRadius);
            int dz = (int) (Mth.sin(angle) * xzRadius);
            if (level.isStateAtPosition(center.offset(dx, 0, dz), MossySpikeFeature::canReplaceWithSpike)) {
                return false;
            }
        }
        return true;
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

        private boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel level, WindOffsetter wind) {
            while (this.radius > 1) {
                BlockPos.MutableBlockPos newRoot = this.root.mutable();
                int maxTries = Math.min(10, this.getHeight());
                for (int i = 0; i < maxTries; i++) {
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
            return (int) getDripstoneHeight(checkRadius, this.radius, this.scale, this.bluntness);
        }

        private void placeBlocks(WorldGenLevel level, RandomSource random, WindOffsetter wind, BlockState block, Column.Range columnRange) {
            Direction growthDirection = this.pointingUp ? Direction.UP : Direction.DOWN;
            int maxY = this.pointingUp ? columnRange.ceiling() + 3 : Integer.MAX_VALUE;
            for (int dx = -this.radius; dx <= this.radius; dx++) {
                for (int dz = -this.radius; dz <= this.radius; dz++) {
                    float currentRadius = Mth.sqrt(dx * dx + dz * dz);
                    if (currentRadius > this.radius) {
                        continue;
                    }
                    int height = this.getHeightAtRadius(currentRadius);
                    if (height <= 0) {
                        continue;
                    }
                    if (random.nextFloat() < 0.2) {
                        height = (int) (height * Mth.randomBetween(random, 0.8F, 1.0F));
                    }

                    BlockPos.MutableBlockPos pos = this.root.offset(dx, 0, dz).mutable();
                    boolean hasBeenOutOfStone = false;
                    BlockState mossBase = null;
                    BlockPos mossCapPos = null;
                    int heightFromMoss = 0;
                    for (int i = 0; i < height && pos.getY() < maxY; i++) {
                        BlockPos windAdjustedPos = wind.offset(pos);
                        BlockState state = level.getBlockState(windAdjustedPos);
                        if (canReplaceWithSpike(state)) {
                            hasBeenOutOfStone = true;
                            if (mossBase == null && state.is(BlockTags.MOSS_BLOCKS)) {
                                mossBase = state;
                                heightFromMoss = 1;
                            } else if (mossBase != null) {
                                heightFromMoss++;
                            }

                            level.setBlock(windAdjustedPos, block, Block.UPDATE_CLIENTS);
                            if (mossBase != null) {
                                mossCapPos = wind.offset(pos.relative(growthDirection)).immutable();
                            }
                        } else if (hasBeenOutOfStone && state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                            break;
                        }
                        pos.move(growthDirection);
                    }

                    if (mossCapPos != null
                            && isMossTransitionColumn(heightFromMoss, random)
                            && level.isStateAtPosition(mossCapPos, MossySpikeFeature::canReplaceWithMossCap)) {
                        level.setBlock(mossCapPos, mossBase, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        private static boolean isMossTransitionColumn(int heightFromMoss, RandomSource random) {
            return heightFromMoss == 1 || heightFromMoss == 2 && random.nextFloat() < 0.66F;
        }

        private boolean isSuitableForWind(Config config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= config.minBluntnessForWind;
        }
    }

    private static final class WindOffsetter {
        private final int originY;
        private final @Nullable Vec3 windSpeed;
        private final int maxShift;

        private WindOffsetter(int originY, RandomSource random, FloatProvider windSpeedRange, int maxShift) {
            this.originY = originY;
            this.maxShift = maxShift;
            float speed = windSpeedRange.sample(random);
            float direction = Mth.randomBetween(random, 0.0F, Mth.PI);
            this.windSpeed = new Vec3(Mth.cos(direction) * speed, 0.0, Mth.sin(direction) * speed);
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
            this.maxShift = 0;
        }

        private static WindOffsetter noWind() {
            return new WindOffsetter();
        }

        private BlockPos offset(BlockPos pos) {
            if (this.windSpeed == null) {
                return pos;
            }
            Vec3 totalWindAdjust = this.windSpeed.scale(this.originY - pos.getY());
            return pos.offset(
                    Mth.clamp(Mth.floor(totalWindAdjust.x), -this.maxShift, this.maxShift),
                    0,
                    Mth.clamp(Mth.floor(totalWindAdjust.z), -this.maxShift, this.maxShift)
            );
        }
    }

    public record Config(
            int floorToCeilingSearchRange,
            IntProvider columnRadius,
            FloatProvider heightScale,
            float maxColumnRadiusToCaveHeightRatio,
            FloatProvider stalactiteBluntness,
            FloatProvider stalagmiteBluntness,
            FloatProvider windSpeed,
            int minRadiusForWind,
            float minBluntnessForWind,
            BlockStateProvider block
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").orElse(30).forGetter(Config::floorToCeilingSearchRange),
                IntProviders.codec(1, 60).fieldOf("column_radius").forGetter(Config::columnRadius),
                FloatProviders.codec(0.0F, 20.0F).fieldOf("height_scale").forGetter(Config::heightScale),
                Codec.floatRange(0.1F, 1.0F).fieldOf("max_column_radius_to_cave_height_ratio").forGetter(Config::maxColumnRadiusToCaveHeightRatio),
                FloatProviders.codec(0.1F, 10.0F).fieldOf("stalactite_bluntness").forGetter(Config::stalactiteBluntness),
                FloatProviders.codec(0.1F, 10.0F).fieldOf("stalagmite_bluntness").forGetter(Config::stalagmiteBluntness),
                FloatProviders.codec(0.0F, 2.0F).fieldOf("wind_speed").forGetter(Config::windSpeed),
                Codec.intRange(0, 100).fieldOf("min_radius_for_wind").forGetter(Config::minRadiusForWind),
                Codec.floatRange(0.0F, 5.0F).fieldOf("min_bluntness_for_wind").forGetter(Config::minBluntnessForWind),
                BlockStateProvider.CODEC.fieldOf("main_block").forGetter(Config::block)
        ).apply(codec, Config::new));
    }
}
