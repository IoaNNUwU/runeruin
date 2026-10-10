package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Moss stacked like snow layers; small plants grow on top of it, each in a place of its own, bone meal adds more. */
public class MossLayerBlock extends SnowLayerBlock implements BonemealableBlock {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(MossLayerBlock::new);

    /**
     * A place a plant can stand in: where its cube is and how big, in pixels. {@code lift} (centre above the moss)
     * and {@code tilt} (degrees outwards) only shape the model.
     */
    public record Plant(BooleanProperty property, double x, double z, int size, double lift, double tilt) {}

    /** One big cube, three medium ones around it and three small ones further out: together they make a dome. */
    public static final List<Plant> PLANTS = List.of(
            plant(0, 8.5, 7.5, 4, 2, 8),
            plant(1, 4.5, 7, 3, 1, 30),
            plant(2, 11, 5.5, 3, 0.7, 30),
            plant(3, 8.5, 11.5, 3, 1.3, 30),
            plant(4, 12.5, 9.5, 2, 0.5, 50),
            plant(5, 4, 11, 2, 0.3, 50),
            plant(6, 7, 3, 2, 0.8, 50)
    );

    public MossLayerBlock(Properties properties) {
        super(properties);
        BlockState bare = this.defaultBlockState();
        for (Plant plant : PLANTS) {
            bare = bare.setValue(plant.property(), false);
        }
        this.registerDefaultState(bare);
    }

    private static Plant plant(int number, double x, double z, int size, double lift, double tilt) {
        return new Plant(BooleanProperty.create("plant_" + number), x, z, size, lift, tilt);
    }

    @Override
    public MapCodec<SnowLayerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        return Block.isFaceFull(belowState.getCollisionShape(level, pos.below()), Direction.UP)
                || belowState.is(this) && belowState.getValue(LAYERS) == MAX_HEIGHT;
    }

    /** Unlike snow, a single layer gives way only to another layer of moss. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(this.asItem())
                && state.getValue(LAYERS) < MAX_HEIGHT
                && (!context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (nearState.is(this) && PLANTS.stream().anyMatch(plant -> !nearState.getValue(plant.property()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (!nearState.is(this)) {
                continue;
            }
            List<Plant> free = Util.toShuffledList(PLANTS.stream().filter(plant -> !nearState.getValue(plant.property())), random);
            int grown;
            if (near.equals(pos)) {
                // One or two on the block itself, any of the smallest that are missing.
                free.sort(Comparator.comparingInt(Plant::size));
                grown = 1 + random.nextInt(2);
            } else {
                // At most one on each moss layer around it, in the place nearest to the block with the bone meal.
                double fromX = (pos.getX() - near.getX()) * 16 + 8;
                double fromZ = (pos.getZ() - near.getZ()) * 16 + 8;
                free.sort(Comparator.comparingDouble(plant -> Mth.square(plant.x() - fromX) + Mth.square(plant.z() - fromZ)));
                grown = random.nextInt(2);
            }
            BlockState grownState = nearState;
            for (Plant plant : free.subList(0, Math.min(grown, free.size()))) {
                grownState = grownState.setValue(plant.property(), true);
            }
            level.setBlock(near, grownState, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        PLANTS.forEach(plant -> builder.add(plant.property()));
    }
}
