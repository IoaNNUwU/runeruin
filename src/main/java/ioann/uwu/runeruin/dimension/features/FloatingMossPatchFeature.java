package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.FloatingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A raft of floating moss, one block thick, in the top layer of the water above the origin. Its outline
 * is an oval of rows that all run the same way and end at random lengths, so two of its sides are ragged.
 * Plants grow on some of the moss.
 */
public class FloatingMossPatchFeature extends Feature<FloatingMossPatchFeature.Config> {

    /** The rows reach this far, and the connection pass one block further: the limit for a feature is 16. */
    private static final int MAX_REACH = 15;
    private static final int SURFACE_SCAN = 8;
    private static final float SHORTEST_ROW_END = 0.55F;

    public FloatingMossPatchFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        RandomSource random = context.random();

        BlockPos.MutableBlockPos pos = context.origin().mutable();
        for (int step = 0; step < SURFACE_SCAN && level.getBlockState(pos.above()).is(Blocks.WATER); step++) {
            pos.move(Direction.UP);
        }
        if (!isOpenWater(level, pos)) {
            return false;
        }
        BlockPos centre = pos.immutable();

        FloatingMossBlock block = (FloatingMossBlock) RRBlocks.FLOATING_MOSS.get();
        BlockState moss = block.defaultBlockState().setValue(FloatingMossBlock.WATERLOGGED, true);
        int width = config.width().sample(random);
        int length = config.length().sample(random);
        boolean rowsAlongX = random.nextBoolean();

        for (int across = -width; across <= width; across++) {
            double side = across / (width + 0.5);
            double reach = length * Math.sqrt(1 - side * side);
            int from = -rowEnd(reach, random);
            int to = rowEnd(reach, random);
            for (int along = from; along <= to; along++) {
                pos.setWithOffset(centre, rowsAlongX ? along : across, 0, rowsAlongX ? across : along);
                if (!isOpenWater(level, pos)) {
                    continue;
                }
                level.setBlock(pos, moss, Block.UPDATE_CLIENTS);
                if (random.nextFloat() < config.vegetationChance()) {
                    BlockPos above = pos.above();
                    BlockState plant = config.vegetation().getState(level, random, above);
                    if (plant.canSurvive(level, above)) {
                        level.setBlock(above, plant, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // Worldgen sends no shape updates, so the moss is joined by hand, to the moss of other rafts too.
        for (int dx = -MAX_REACH - 1; dx <= MAX_REACH + 1; dx++) {
            for (int dz = -MAX_REACH - 1; dz <= MAX_REACH + 1; dz++) {
                pos.setWithOffset(centre, dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (state.is(block)) {
                    BlockState connected = block.connect(state, level, pos);
                    if (connected != state) {
                        level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }

    private static int rowEnd(double reach, RandomSource random) {
        return (int) Math.round(reach * Math.min(1, SHORTEST_ROW_END + random.nextFloat() * (1 - SHORTEST_ROW_END)));
    }

    /** Still water with air above it: the only place where floating moss lies on the surface. */
    private static boolean isOpenWater(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource() && level.isEmptyBlock(pos.above());
    }

    public record Config(
            IntProvider width,
            IntProvider length,
            BlockStateProvider vegetation,
            float vegetationChance
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec -> codec.group(
                IntProviders.codec(1, MAX_REACH).fieldOf("width").forGetter(Config::width),
                IntProviders.codec(1, MAX_REACH).fieldOf("length").forGetter(Config::length),
                BlockStateProvider.CODEC.fieldOf("vegetation").forGetter(Config::vegetation),
                Codec.floatRange(0.0F, 1.0F).fieldOf("vegetation_chance").forGetter(Config::vegetationChance)
        ).apply(codec, Config::new));
    }
}
