package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EldenVinesBlock extends Block implements BonemealableBlock {
    public static final MapCodec<EldenVinesBlock> CODEC = simpleCodec(EldenVinesBlock::new);
    public static final BooleanProperty ORB = BooleanProperty.create("orb");
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);

    public EldenVinesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ORB, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getLightLevel(BlockState state) {
        return state.getValue(ORB) ? 14 : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        return aboveState.is(this)
                || aboveState.is(RRBlocks.ELDEN_LEAVES.get())
                || aboveState.is(RRBlocks.ELDEN_LOG.get())
                || aboveState.is(RRBlocks.ELDEN_WOOD.get());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(ORB)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), serverLevel.getRandom().nextInt(1, 4));
            float pitch = Mth.randomBetween(serverLevel.getRandom(), 0.8F, 1.2F);
            serverLevel.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
            BlockState newState = state.setValue(ORB, false);
            serverLevel.setBlock(pos, newState, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORB);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!state.getValue(ORB)) {
            level.setBlock(pos, state.setValue(ORB, true), 2);
        }

        if (random.nextBoolean()) {
            List<BlockPos> growableNeighbours = new ArrayList<>();
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    if (xOffset == 0 && zOffset == 0) {
                        continue;
                    }

                    for (int yOffset = -1; yOffset <= 1; yOffset++) {
                        BlockPos neighbourPos = pos.offset(xOffset, yOffset, zOffset);
                        if (level.isEmptyBlock(neighbourPos)
                                && this.defaultBlockState().canSurvive(level, neighbourPos)) {
                            growableNeighbours.add(neighbourPos);
                        }
                    }
                }
            }

            if (!growableNeighbours.isEmpty()) {
                BlockPos growPos = growableNeighbours.get(random.nextInt(growableNeighbours.size()));
                level.setBlock(growPos, this.defaultBlockState(), 2);
            }
        }

        if (random.nextBoolean()) {
            BlockPos bottomPos = pos;
            while (bottomPos.getY() > level.getMinY() && level.getBlockState(bottomPos.below()).is(this)) {
                bottomPos = bottomPos.below();
            }

            BlockPos growDownPos = bottomPos.below();
            if (growDownPos.getY() >= level.getMinY()
                    && level.isEmptyBlock(growDownPos)
                    && this.defaultBlockState().canSurvive(level, growDownPos)) {
                level.setBlock(growDownPos, this.defaultBlockState(), 2);
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos belowPos = pos.below();
        if (!level.getGameRules().get(GameRules.SPREAD_VINES)
                || pos.getY() <= level.getMinY()
                || !level.isEmptyBlock(belowPos)
                || !level.isAreaLoaded(pos, 4)
                || random.nextInt(4) != 0
                || !canGrowNearby(level, pos)) {
            return;
        }

        BlockState newVine = this.defaultBlockState().setValue(ORB, random.nextInt(5) == 0);
        level.setBlock(belowPos, newVine, 2);
    }

    private boolean canGrowNearby(BlockGetter level, BlockPos pos) {
        int remainingVines = 5;
        for (BlockPos nearbyPos : BlockPos.betweenClosed(
                pos.getX() - 4,
                pos.getY() - 1,
                pos.getZ() - 4,
                pos.getX() + 4,
                pos.getY() + 1,
                pos.getZ() + 4
        )) {
            if (level.getBlockState(nearbyPos).is(this) && --remainingVines <= 0) {
                return false;
            }
        }
        return true;
    }
}
