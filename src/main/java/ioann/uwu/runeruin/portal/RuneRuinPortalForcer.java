package ioann.uwu.runeruin.portal;

import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_Y;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRDimension;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Locates / builds RuneRuin portals. Mirrors {@link net.minecraft.world.level.portal.PortalForcer}
 * but uses our POI, arcane-stone frames, and (in RuneRuin) only the top layer.
 * <p>
 * Vanilla {@code PortalForcer} cannot be reused as-is: it hardcodes {@code PoiTypes.NETHER_PORTAL}
 * and {@code Blocks.OBSIDIAN}/{@code NETHER_PORTAL}.
 */
public class RuneRuinPortalForcer {
    private static final int RUNERUIN_PORTAL_RADIUS = 16;
    private static final int OVERWORLD_PORTAL_RADIUS = 128;
    private final ServerLevel level;

    public RuneRuinPortalForcer(ServerLevel level) {
        this.level = level;
    }

    public Optional<BlockPos> findClosestPortalPosition(BlockPos approximateExitPos, boolean toRuneRuin, WorldBorder worldBorder) {
        PoiManager poiManager = this.level.getPoiManager();
        int radius = toRuneRuin ? RUNERUIN_PORTAL_RADIUS : OVERWORLD_PORTAL_RADIUS;
        poiManager.ensureLoadedAndValid(this.level, approximateExitPos, radius);
        return poiManager.getInSquare(type -> type.is(RRPoiTypes.RUNE_RUIN_PORTAL.getKey()), approximateExitPos, radius, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(worldBorder::isWithinBounds)
                .filter(pos -> this.level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .filter(this::isValidExitPortalY)
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(approximateExitPos)).thenComparingInt(Vec3i::getY));
    }

    /**
     * In RuneRuin, only top-layer portals count as exit targets (caves are ignored).
     */
    private boolean isValidExitPortalY(BlockPos pos) {
        if (this.level.dimension() != RRDimension.LEVEL) {
            return true;
        }
        return pos.getY() >= TOP_LAYER_Y;
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos origin, Direction.Axis portalAxis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, portalAxis);
        double closestFullDistanceSqr = -1.0;
        BlockPos closestFullPosition = null;
        double closestPartialDistanceSqr = -1.0;
        BlockPos closestPartialPosition = null;
        WorldBorder worldBorder = this.level.getWorldBorder();
        int maxPlaceableY = Math.min(this.level.getMaxY(), this.level.getMinY() + this.level.getLogicalHeight() - 1);
        int minSearchY = this.minPortalSearchY();
        BlockPos.MutableBlockPos mutable = origin.mutable();

        for (BlockPos.MutableBlockPos columnPos : BlockPos.spiralAround(origin, 16, Direction.EAST, Direction.SOUTH)) {
            int height = Math.min(maxPlaceableY, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnPos.getX(), columnPos.getZ()));
            if (worldBorder.isWithinBounds(columnPos) && worldBorder.isWithinBounds(columnPos.move(direction, 1))) {
                columnPos.move(direction.getOpposite(), 1);

                for (int y = height; y >= minSearchY; y--) {
                    columnPos.setY(y);
                    if (this.canPortalReplaceBlock(columnPos)) {
                        int firstEmptyY = y;

                        while (y > minSearchY && this.canPortalReplaceBlock(columnPos.move(Direction.DOWN))) {
                            y--;
                        }

                        if (y + 4 <= maxPlaceableY) {
                            int deltaY = firstEmptyY - y;
                            if (deltaY <= 0 || deltaY >= 3) {
                                columnPos.setY(y);
                                if (this.canHostFrame(columnPos, mutable, direction, 0)) {
                                    double distance = origin.distSqr(columnPos);
                                    if (this.canHostFrame(columnPos, mutable, direction, -1)
                                            && this.canHostFrame(columnPos, mutable, direction, 1)
                                            && (closestFullDistanceSqr == -1.0 || closestFullDistanceSqr > distance)) {
                                        closestFullDistanceSqr = distance;
                                        closestFullPosition = columnPos.immutable();
                                    }

                                    if (closestFullDistanceSqr == -1.0 && (closestPartialDistanceSqr == -1.0 || closestPartialDistanceSqr > distance)) {
                                        closestPartialDistanceSqr = distance;
                                        closestPartialPosition = columnPos.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (closestFullDistanceSqr == -1.0 && closestPartialDistanceSqr != -1.0) {
            closestFullPosition = closestPartialPosition;
            closestFullDistanceSqr = closestPartialDistanceSqr;
        }

        if (closestFullDistanceSqr == -1.0) {
            int minStartY = Math.max(minSearchY, this.level.dimension() == RRDimension.LEVEL ? TOP_LAYER_Y : 70);
            int maxStartY = maxPlaceableY - 9;
            if (maxStartY < minStartY) {
                return Optional.empty();
            }

            closestFullPosition = new BlockPos(
                    origin.getX() - direction.getStepX() * 1,
                    Mth.clamp(origin.getY(), minStartY, maxStartY),
                    origin.getZ() - direction.getStepZ() * 1
            ).immutable();
            closestFullPosition = worldBorder.clampToBounds(closestFullPosition);
            Direction clockWise = direction.getClockWise();

            for (int box = -1; box < 2; box++) {
                for (int width = 0; width < 2; width++) {
                    for (int height = -1; height < 3; height++) {
                        BlockState blockState = height < 0
                                ? RRBlocks.ARCANE_STONE.get().defaultBlockState()
                                : Blocks.AIR.defaultBlockState();
                        mutable.setWithOffset(
                                closestFullPosition,
                                width * direction.getStepX() + box * clockWise.getStepX(),
                                height,
                                width * direction.getStepZ() + box * clockWise.getStepZ()
                        );
                        this.level.setBlockAndUpdate(mutable, blockState);
                    }
                }
            }
        }

        for (int width = -1; width < 3; width++) {
            for (int height = -1; height < 4; height++) {
                if (width == -1 || width == 2 || height == -1 || height == 3) {
                    mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
                    this.level.setBlock(mutable, RRBlocks.ARCANE_STONE.get().defaultBlockState(), 3);
                }
            }
        }

        BlockState portalBlockState = RRBlocks.RUNE_RUIN_PORTAL.get().defaultBlockState().setValue(RuneRuinPortalBlock.AXIS, portalAxis);

        for (int width = 0; width < 2; width++) {
            for (int height = 0; height < 3; height++) {
                mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
                this.level.setBlock(mutable, portalBlockState, 18);
            }
        }

        return Optional.of(new BlockUtil.FoundRectangle(closestFullPosition.immutable(), 2, 3));
    }

    private int minPortalSearchY() {
        if (this.level.dimension() == RRDimension.LEVEL) {
            return TOP_LAYER_Y;
        }
        return this.level.getMinY();
    }

    private boolean canPortalReplaceBlock(BlockPos.MutableBlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        return blockState.canBeReplaced() && blockState.getFluidState().isEmpty();
    }

    private boolean canHostFrame(BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction, int offset) {
        Direction clockWise = direction.getClockWise();

        for (int width = -1; width < 3; width++) {
            for (int height = -1; height < 4; height++) {
                mutable.setWithOffset(
                        origin,
                        direction.getStepX() * width + clockWise.getStepX() * offset,
                        height,
                        direction.getStepZ() * width + clockWise.getStepZ() * offset
                );
                if (height < 0 && !this.level.getBlockState(mutable).isSolid()) {
                    return false;
                }

                if (height >= 0 && !this.canPortalReplaceBlock(mutable)) {
                    return false;
                }
            }
        }

        return true;
    }
}
