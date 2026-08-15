package ioann.uwu.runeruin.portal;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRDimension;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Nether-portal analogue linking Overworld and {@link RRDimension#LEVEL}.
 * Arrival / exit-portal creation in RuneRuin targets the top layer ({@link Const#TOP_LAYER_Y}+).
 */
public class RuneRuinPortalBlock extends Block implements Portal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<RuneRuinPortalBlock> CODEC = simpleCodec(RuneRuinPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** True while showing a failed-ignition flash that will shatter (no teleport). */
    public static final BooleanProperty UNSTABLE = BooleanProperty.create("unstable");
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

    /** Preferred Y when searching / creating a portal in the Overworld. */
    public static final int OVERWORLD_PORTAL_Y = 64;

    @Override
    public MapCodec<RuneRuinPortalBlock> codec() {
        return CODEC;
    }

    public RuneRuinPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(UNSTABLE, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
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
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        return !wrongAxis && !neighbourState.is(this) && !RuneRuinPortalShape.findAnyShape(level, pos, axis).isComplete()
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(UNSTABLE)) {
            return;
        }
        // One scheduled tick on the origin block shatters the whole unstable portal.
        RuneRuinPortalShape.findAnyShape(level, pos, state.getValue(AXIS)).shatterPortalBlocks(level);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (state.getValue(UNSTABLE)) {
            return;
        }
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player player
                ? Math.max(
                        0,
                        level.getGameRules()
                                .get(player.getAbilities().invulnerable
                                        ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                                        : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)
                )
                : 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> current = currentLevel.dimension();
        ResourceKey<Level> targetKey;
        if (current == RRDimension.LEVEL) {
            targetKey = Level.OVERWORLD;
        } else if (current == Level.OVERWORLD) {
            targetKey = RRDimension.LEVEL;
        } else {
            return null;
        }

        ServerLevel newLevel = currentLevel.getServer().getLevel(targetKey);
        if (newLevel == null) {
            return null;
        }

        boolean toRuneRuin = newLevel.dimension() == RRDimension.LEVEL;
        WorldBorder newWorldBorder = newLevel.getWorldBorder();
        double teleportationScale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), newLevel.dimensionType());
        int preferredY = toRuneRuin ? Const.TOP_LAYER_PORTAL_Y : OVERWORLD_PORTAL_Y;
        preferredY = Mth.clamp(preferredY, newLevel.getMinY(), newLevel.getMaxY());
        BlockPos approximateExitPos = newWorldBorder.clampToBounds(
                entity.getX() * teleportationScale,
                preferredY,
                entity.getZ() * teleportationScale
        );
        return this.getExitPortal(newLevel, entity, portalEntryPos, approximateExitPos, toRuneRuin, newWorldBorder);
    }

    private @Nullable TeleportTransition getExitPortal(
            ServerLevel newLevel,
            Entity entity,
            BlockPos portalEntryPos,
            BlockPos approximateExitPos,
            boolean toRuneRuin,
            WorldBorder worldBorder
    ) {
        RuneRuinPortalForcer forcer = new RuneRuinPortalForcer(newLevel);
        Optional<BlockPos> exitPortalPos = forcer.findClosestPortalPosition(approximateExitPos, toRuneRuin, worldBorder);
        BlockUtil.FoundRectangle exitPortal;
        TeleportTransition.PostTeleportTransition post;
        if (exitPortalPos.isPresent()) {
            BlockPos pos = exitPortalPos.get();
            BlockState portalState = newLevel.getBlockState(pos);
            exitPortal = BlockUtil.getLargestRectangleAround(
                    pos,
                    portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS),
                    21,
                    Direction.Axis.Y,
                    21,
                    blockPos -> newLevel.getBlockState(blockPos) == portalState
            );
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(pos));
        } else {
            Direction.Axis sourcePortalAxis = entity.level().getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> createdExit = forcer.createPortal(approximateExitPos, sourcePortalAxis);
            if (createdExit.isEmpty()) {
                LOGGER.error("Unable to create a RuneRuin portal, likely target out of worldborder");
                return null;
            }

            exitPortal = createdExit.get();
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        }

        return getDimensionTransitionFromExit(entity, portalEntryPos, exitPortal, newLevel, post);
    }

    private static TeleportTransition getDimensionTransitionFromExit(
            Entity entity,
            BlockPos portalEntryPos,
            BlockUtil.FoundRectangle exitPortal,
            ServerLevel newLevel,
            TeleportTransition.PostTeleportTransition postTeleportTransition
    ) {
        BlockState blockState = entity.level().getBlockState(portalEntryPos);
        Direction.Axis axis;
        Vec3 offset;
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            BlockUtil.FoundRectangle portalArea = BlockUtil.getLargestRectangleAround(
                    portalEntryPos, axis, 21, Direction.Axis.Y, 21, pos -> entity.level().getBlockState(pos) == blockState
            );
            offset = entity.getRelativePortalPosition(axis, portalArea);
        } else {
            axis = Direction.Axis.X;
            offset = new Vec3(0.5, 0.0, 0.0);
        }

        return createDimensionTransition(newLevel, exitPortal, axis, offset, entity, postTeleportTransition);
    }

    private static TeleportTransition createDimensionTransition(
            ServerLevel newLevel,
            BlockUtil.FoundRectangle foundRectangle,
            Direction.Axis portalAxis,
            Vec3 offset,
            Entity entity,
            TeleportTransition.PostTeleportTransition postTeleportTransition
    ) {
        BlockPos bottomLeft = foundRectangle.minCorner;
        BlockState blockState = newLevel.getBlockState(bottomLeft);
        Direction.Axis axis = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double width = foundRectangle.axis1Size;
        double height = foundRectangle.axis2Size;
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        int outputRotation = portalAxis == axis ? 0 : 90;
        double offsetRight = dimensions.width() / 2.0 + (width - dimensions.width()) * offset.x();
        double offsetUp = (height - dimensions.height()) * offset.y();
        double offsetForward = 0.5 + offset.z();
        boolean xAligned = axis == Direction.Axis.X;
        Vec3 targetPos = new Vec3(
                bottomLeft.getX() + (xAligned ? offsetRight : offsetForward),
                bottomLeft.getY() + offsetUp,
                bottomLeft.getZ() + (xAligned ? offsetForward : offsetRight)
        );
        // Reuse vanilla helper — identical to NetherPortalBlock.
        Vec3 collisionFreePos = PortalShape.findCollisionFreePosition(targetPos, newLevel, entity, dimensions);
        return new TeleportTransition(
                newLevel, collisionFreePos, Vec3.ZERO, outputRotation, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), postTeleportTransition
        );
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false
            );
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double xa = (random.nextFloat() - 0.5) * 0.5;
            double ya = (random.nextFloat() - 0.5) * 0.5;
            double za = (random.nextFloat() - 0.5) * 0.5;
            int flip = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = pos.getX() + 0.5 + 0.25 * flip;
                xa = random.nextFloat() * 2.0F * flip;
            } else {
                z = pos.getZ() + 0.5 + 0.25 * flip;
                za = random.nextFloat() * 2.0F * flip;
            }

            level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, UNSTABLE);
    }
}
