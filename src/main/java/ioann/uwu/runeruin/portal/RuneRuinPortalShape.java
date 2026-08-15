package ioann.uwu.runeruin.portal;

import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

/**
 * Arcane-stone frame detector. Cannot reuse {@link net.minecraft.world.level.portal.PortalShape} for
 * find/fill: vanilla hardcodes {@code isPortalFrame} + {@code Blocks.NETHER_PORTAL}. Public helpers
 * like {@link net.minecraft.world.level.portal.PortalShape#findCollisionFreePosition} are reused from
 * {@link RuneRuinPortalBlock} instead.
 */
public class RuneRuinPortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBehaviour.StatePredicate FRAME =
            (state, level, pos) -> state.is(RRBlocks.ARCANE_STONE);
    private final Direction.Axis axis;
    private final Direction rightDir;
    private final int numPortalBlocks;
    private final BlockPos bottomLeft;
    private final int height;
    private final int width;

    private RuneRuinPortalShape(Direction.Axis axis, int portalBlockCount, Direction rightDir, BlockPos bottomLeft, int width, int height) {
        this.axis = axis;
        this.numPortalBlocks = portalBlockCount;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    public static Optional<RuneRuinPortalShape> findEmptyPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis preferredAxis) {
        return findPortalShape(level, pos, shape -> shape.isValid() && shape.numPortalBlocks == 0, preferredAxis);
    }

    public static Optional<RuneRuinPortalShape> findPortalShape(
            LevelAccessor level, BlockPos pos, Predicate<RuneRuinPortalShape> isValid, Direction.Axis preferredAxis
    ) {
        Optional<RuneRuinPortalShape> firstAxis = Optional.of(findAnyShape(level, pos, preferredAxis)).filter(isValid);
        if (firstAxis.isPresent()) {
            return firstAxis;
        }

        Direction.Axis otherAxis = preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return Optional.of(findAnyShape(level, pos, otherAxis)).filter(isValid);
    }

    public static RuneRuinPortalShape findAnyShape(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = calculateBottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new RuneRuinPortalShape(axis, 0, rightDir, pos, 0, 0);
        }

        int width = calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return new RuneRuinPortalShape(axis, 0, rightDir, bottomLeft, 0, 0);
        }

        MutableInt portalBlockCountOutput = new MutableInt();
        int height = calculateHeight(level, bottomLeft, rightDir, width, portalBlockCountOutput);
        return new RuneRuinPortalShape(axis, portalBlockCountOutput.intValue(), rightDir, bottomLeft, width, height);
    }

    private static @Nullable BlockPos calculateBottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - 21);

        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction leftDir = rightDir.getOpposite();
        int edge = getDistanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        return edge < 0 ? null : pos.relative(leftDir, edge);
    }

    private static int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction rightDir) {
        int width = getDistanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
    }

    private static int getDistanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for (int width = 0; width <= MAX_WIDTH; width++) {
            blockPos.set(pos).move(direction, width);
            BlockState blockState = level.getBlockState(blockPos);
            if (!isEmpty(blockState)) {
                if (FRAME.test(blockState, level, blockPos)) {
                    return width;
                }
                break;
            }

            BlockState belowState = level.getBlockState(blockPos.move(Direction.DOWN));
            if (!FRAME.test(belowState, level, blockPos)) {
                break;
            }
        }

        return 0;
    }

    private static int calculateHeight(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, MutableInt portalBlockCount) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int height = getDistanceUntilTop(level, bottomLeft, rightDir, pos, width, portalBlockCount);
        return height >= MIN_HEIGHT && height <= MAX_HEIGHT && hasTopFrame(level, bottomLeft, rightDir, pos, width, height) ? height : 0;
    }

    private static boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos pos, int width, int height) {
        for (int i = 0; i < width; i++) {
            BlockPos.MutableBlockPos framePos = pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
            if (!FRAME.test(level.getBlockState(framePos), level, framePos)) {
                return false;
            }
        }

        return true;
    }

    private static int getDistanceUntilTop(
            BlockGetter level, BlockPos bottomLeft, Direction rightDir, BlockPos.MutableBlockPos pos, int width, MutableInt portalBlockCount
    ) {
        for (int height = 0; height < MAX_HEIGHT; height++) {
            pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1);
            if (!FRAME.test(level.getBlockState(pos), level, pos)) {
                return height;
            }

            pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, width);
            if (!FRAME.test(level.getBlockState(pos), level, pos)) {
                return height;
            }

            for (int i = 0; i < width; i++) {
                pos.set(bottomLeft).move(Direction.UP, height).move(rightDir, i);
                BlockState state = level.getBlockState(pos);
                if (!isEmpty(state)) {
                    return height;
                }

                if (state.is(RRBlocks.RUNE_RUIN_PORTAL)) {
                    portalBlockCount.increment();
                }
            }
        }

        return MAX_HEIGHT;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(RRBlocks.RUNE_RUIN_PORTAL);
    }

    public boolean isValid() {
        return this.width >= MIN_WIDTH && this.width <= MAX_WIDTH && this.height >= MIN_HEIGHT && this.height <= MAX_HEIGHT;
    }

    public void createPortalBlocks(LevelAccessor level) {
        this.placePortalBlocks(level, false);
    }

    /**
     * Lights the portal briefly as {@code unstable}, then schedules a shatter (invalid location feedback).
     * No explosion and no player damage.
     */
    public void createUnstablePortalBlocks(Level level, int shatterDelayTicks) {
        this.placePortalBlocks(level, true);
        level.scheduleTick(this.bottomLeft.immutable(), RRBlocks.RUNE_RUIN_PORTAL.get(), shatterDelayTicks);
        level.playSound(
                null,
                this.bottomLeft,
                SoundEvents.PORTAL_TRIGGER,
                SoundSource.BLOCKS,
                0.85F,
                0.45F
        );
    }

    private void placePortalBlocks(LevelAccessor level, boolean unstable) {
        BlockState portalState = RRBlocks.RUNE_RUIN_PORTAL.get().defaultBlockState()
                .setValue(RuneRuinPortalBlock.AXIS, this.axis)
                .setValue(RuneRuinPortalBlock.UNSTABLE, unstable);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> level.setBlock(pos, portalState, 18));
    }

    /**
     * Breaks every portal block in this shape with destroy particles and glass sounds — no explosion/damage.
     */
    public void shatterPortalBlocks(Level level) {
        BlockPos max = this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1);
        boolean playedBreakSound = false;
        for (BlockPos pos : BlockPos.betweenClosed(this.bottomLeft, max)) {
            BlockPos immutable = pos.immutable();
            BlockState state = level.getBlockState(immutable);
            if (!state.is(RRBlocks.RUNE_RUIN_PORTAL)) {
                continue;
            }
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, immutable, Block.getId(state));
            // Client update only — avoid neighbor cascades mid-shatter.
            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        immutable.getX() + 0.5,
                        immutable.getY() + 0.5,
                        immutable.getZ() + 0.5,
                        8,
                        0.3,
                        0.4,
                        0.3,
                        0.5
                );
            }
            if (!playedBreakSound) {
                level.playSound(null, immutable, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.9F, 0.9F);
                playedBreakSound = true;
            }
        }
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }
}
