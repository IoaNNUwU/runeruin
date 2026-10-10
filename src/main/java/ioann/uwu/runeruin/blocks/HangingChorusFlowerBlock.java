package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The vanilla chorus flower turned upside down: the same growth and the same tree, but downwards from
 * void stone. Keep it in step with {@code ChorusFlowerBlock} when that one changes.
 */
public class HangingChorusFlowerBlock extends Block {
    public static final MapCodec<HangingChorusFlowerBlock> CODEC = simpleCodec(HangingChorusFlowerBlock::new);
    public static final int DEAD_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;

    public HangingChorusFlowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<HangingChorusFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < DEAD_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        if (!level.isEmptyBlock(below) || below.getY() < level.getMinY()) {
            return;
        }

        int age = state.getValue(AGE);
        boolean growDown = false;
        boolean pillarOnSupport = false;
        BlockState aboveState = level.getBlockState(pos.above());
        if (HangingChorusPlantBlock.supports(aboveState)) {
            growDown = true;
        } else if (aboveState.is(RRBlocks.HANGING_CHORUS_PLANT)) {
            int height = 1;

            for (int i = 0; i < 4; i++) {
                BlockState testState = level.getBlockState(pos.above(height + 1));
                if (!testState.is(RRBlocks.HANGING_CHORUS_PLANT)) {
                    pillarOnSupport = HangingChorusPlantBlock.supports(testState);
                    break;
                }

                height++;
            }

            if (height < 2 || height <= random.nextInt(pillarOnSupport ? 5 : 4)) {
                growDown = true;
            }
        } else if (aboveState.isAir()) {
            growDown = true;
        }

        if (growDown && allNeighborsEmpty(level, below, null) && level.isEmptyBlock(pos.below(2))) {
            setPlant(level, pos);
            this.placeGrownFlower(level, below, age);
        } else if (age < 4) {
            int branchAttempts = random.nextInt(4);
            if (pillarOnSupport) {
                branchAttempts++;
            }

            boolean createdBranch = false;

            for (int i = 0; i < branchAttempts; i++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos target = pos.relative(direction);
                if (level.isEmptyBlock(target) && level.isEmptyBlock(target.above()) && allNeighborsEmpty(level, target, direction.getOpposite())) {
                    this.placeGrownFlower(level, target, age + 1);
                    createdBranch = true;
                }
            }

            if (createdBranch) {
                setPlant(level, pos);
            } else {
                this.placeDeadFlower(level, pos);
            }
        } else {
            this.placeDeadFlower(level, pos);
        }
    }

    private void placeGrownFlower(Level level, BlockPos pos, int age) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, age), 2);
        level.levelEvent(1033, pos, 0);
    }

    private void placeDeadFlower(Level level, BlockPos pos) {
        level.setBlock(pos, this.defaultBlockState().setValue(AGE, DEAD_AGE), 2);
        level.levelEvent(1034, pos, 0);
    }

    private static void setPlant(LevelAccessor level, BlockPos pos) {
        level.setBlock(pos, HangingChorusPlantBlock.stateWithConnections(level, pos), 2);
    }

    private static boolean allNeighborsEmpty(LevelReader level, BlockPos pos, @Nullable Direction ignore) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != ignore && !level.isEmptyBlock(pos.relative(direction))) {
                return false;
            }
        }

        return true;
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
        if (directionToNeighbour != Direction.DOWN && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        if (aboveState.is(RRBlocks.HANGING_CHORUS_PLANT) || HangingChorusPlantBlock.supports(aboveState)) {
            return true;
        }
        if (!aboveState.isAir()) {
            return false;
        }

        boolean oneNeighbor = false;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(RRBlocks.HANGING_CHORUS_PLANT)) {
                if (oneNeighbor) {
                    return false;
                }

                oneNeighbor = true;
            } else if (!neighbor.isAir()) {
                return false;
            }
        }

        return oneNeighbor;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** A whole tree hanging from {@code target}, for world generation: its flowers are already dead. */
    public static void generatePlant(LevelAccessor level, BlockPos target, RandomSource random, int maxHorizontalSpread) {
        setPlant(level, target);
        growTreeRecursive(level, target, random, target, maxHorizontalSpread, 0);
    }

    private static void growTreeRecursive(LevelAccessor level, BlockPos current, RandomSource random, BlockPos startPos, int maxHorizontalSpread, int depth) {
        int height = random.nextInt(4) + 1;
        if (depth == 0) {
            height++;
        }

        for (int i = 0; i < height; i++) {
            BlockPos target = current.below(i + 1);
            if (!allNeighborsEmpty(level, target, null)) {
                return;
            }

            setPlant(level, target);
            setPlant(level, target.above());
        }

        boolean placedStem = false;
        if (depth < 4) {
            int stems = random.nextInt(4);
            if (depth == 0) {
                stems++;
            }

            for (int i = 0; i < stems; i++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos target = current.below(height).relative(direction);
                if (Math.abs(target.getX() - startPos.getX()) < maxHorizontalSpread
                        && Math.abs(target.getZ() - startPos.getZ()) < maxHorizontalSpread
                        && level.isEmptyBlock(target)
                        && level.isEmptyBlock(target.above())
                        && allNeighborsEmpty(level, target, direction.getOpposite())) {
                    placedStem = true;
                    setPlant(level, target);
                    setPlant(level, target.relative(direction.getOpposite()));
                    growTreeRecursive(level, target, random, startPos, maxHorizontalSpread, depth + 1);
                }
            }
        }

        if (!placedStem) {
            level.setBlock(current.below(height), RRBlocks.HANGING_CHORUS_FLOWER.get().defaultBlockState().setValue(AGE, DEAD_AGE), 2);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        BlockPos pos = blockHit.getBlockPos();
        if (level instanceof ServerLevel serverLevel && projectile.mayInteract(serverLevel, pos) && projectile.mayBreak(serverLevel)) {
            level.destroyBlock(pos, true, projectile);
        }
    }
}
