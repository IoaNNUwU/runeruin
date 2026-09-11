package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A lily pad that grows into a single connected 2x2 or 3x3 shape.
 *
 * <p>Every cell is still a block in the world. The {@link Part} state is the
 * ownership marker for the shape, which is important: a formed 2x2 pad must
 * not be treated as four ordinary pads when a new pad is placed beside it.</p>
 */
public class BigLilyPadBlock extends VegetationBlock {
    public static final MapCodec<BigLilyPadBlock> CODEC = simpleCodec(BigLilyPadBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SINGLE_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    public BigLilyPadBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.SINGLE).setValue(FACING, Direction.NORTH));
    }

    /** Returns the state part for a world-generated pad cell of the given size. */
    public static Part partAt(int size, int x, int z) {
        return switch (size) {
            case 1 -> Part.SINGLE;
            case 2 -> Part.smallAt(x, z);
            case 3 -> Part.largeAt(x, z);
            default -> throw new IllegalArgumentException("Lily pad size must be 1, 2, or 3");
        };
    }

    @Override
    public MapCodec<BigLilyPadBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise
    ) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        if (level instanceof ServerLevel && entity instanceof AbstractBoat) {
            this.destroyOtherParts(level, pos, state, entity);
            level.destroyBlock(pos, true, entity);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.destroyOtherParts(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // FACING is the direction of the texture's notch. The player's
        // horizontal look direction therefore puts the notch in front of them.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        FluidState fluidAbove = level.getFluidState(pos.above());
        return (fluidState.is(FluidTags.SUPPORTS_LILY_PAD) || state.is(BlockTags.SUPPORTS_LILY_PAD))
                && fluidAbove.is(Fluids.EMPTY);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Part part = state.getValue(PART);
        if (part == Part.SINGLE) {
            return SINGLE_SHAPE;
        }

        // The collision shape follows the outside edge of the combined pad.
        // The model supplies the actual pixel-perfect, transparent outline.
        double minX = part.x() == 0 ? 1.0 : 0.0;
        double maxX = part.x() == part.gridSize() - 1 ? 15.0 : 16.0;
        double minZ = part.z() == 0 ? 1.0 : 0.0;
        double maxZ = part.z() == part.gridSize() - 1 ? 15.0 : 16.0;
        return Block.box(minX, 0.0, minZ, maxX, 1.5, maxZ);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (!level.isClientSide()) {
            this.mergeAround(level, pos, state.getValue(FACING));
        }
    }

    private void mergeAround(Level level, BlockPos placedPos, Direction facing) {
        // Check the larger shape first. Otherwise a 3x3 made from nine
        // ordinary pads could be consumed as a smaller 2x2 during this pass.
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -2; dz <= 0; dz++) {
                if (this.tryMerge3x3(level, placedPos.offset(dx, 0, dz), facing)) {
                    return;
                }
            }
        }

        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                if (this.tryMerge2x2(level, placedPos.offset(dx, 0, dz), facing)) {
                    return;
                }
            }
        }
    }

    private boolean tryMerge2x2(Level level, BlockPos origin, Direction facing) {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                BlockState state = level.getBlockState(origin.offset(x, 0, z));
                // This exact state check deliberately excludes every already
                // formed 2x2/3x3 part from being used as a new singleton.
                if (!this.isPart(state, Part.SINGLE)) {
                    return false;
                }
            }
        }

        this.write2x2(level, origin, facing);
        return true;
    }

    private boolean tryMerge3x3(Level level, BlockPos origin, Direction facing) {
        if (this.matches3x3OfSingles(level, origin)) {
            this.write3x3(level, origin, facing);
            return true;
        }

        // A complete small pad may occupy any of the four 2x2 quadrants of
        // the future 3x3 pad. Its four states must match their exact local
        // positions; merely checking the block type is not sufficient.
        for (int smallX = 0; smallX < 2; smallX++) {
            for (int smallZ = 0; smallZ < 2; smallZ++) {
                if (this.matches3x3With2x2(level, origin, smallX, smallZ)) {
                    this.write3x3(level, origin, facing);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matches3x3OfSingles(Level level, BlockPos origin) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (!this.isPart(level.getBlockState(origin.offset(x, 0, z)), Part.SINGLE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matches3x3With2x2(Level level, BlockPos origin, int smallX, int smallZ) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                Part expected = x >= smallX && x < smallX + 2 && z >= smallZ && z < smallZ + 2
                        ? Part.smallAt(x - smallX, z - smallZ)
                        : Part.SINGLE;
                if (!this.isPart(level.getBlockState(origin.offset(x, 0, z)), expected)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPart(BlockState state, Part expected) {
        return state.is(this) && state.getValue(PART) == expected;
    }

    private void write2x2(Level level, BlockPos origin, Direction facing) {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                this.setPart(level, origin.offset(x, 0, z), Part.smallAt(x, z), facing);
            }
        }
    }

    private void write3x3(Level level, BlockPos origin, Direction facing) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                this.setPart(level, origin.offset(x, 0, z), Part.largeAt(x, z), facing);
            }
        }
    }

    private void setPart(Level level, BlockPos pos, Part part, Direction facing) {
        level.setBlock(
                pos,
                this.defaultBlockState().setValue(PART, part).setValue(FACING, facing),
                Block.UPDATE_ALL
        );
    }

    private void destroyOtherParts(Level level, BlockPos pos, BlockState state, Entity breaker) {
        Part part = state.getValue(PART);
        if (part == Part.SINGLE) {
            return;
        }

        int gridSize = part.gridSize();
        BlockPos origin = pos.offset(-part.x(), 0, -part.z());
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                BlockPos partPos = origin.offset(x, 0, z);
                if (partPos.equals(pos)) {
                    continue;
                }

                Part expected = gridSize == 2 ? Part.smallAt(x, z) : Part.largeAt(x, z);
                if (this.isPart(level.getBlockState(partPos), expected, state.getValue(FACING))) {
                    level.destroyBlock(partPos, true, breaker);
                }
            }
        }
    }

    private boolean isPart(BlockState state, Part expected, Direction facing) {
        return this.isPart(state, expected) && state.getValue(FACING) == facing;
    }

    public enum Part implements StringRepresentable {
        SINGLE("single", 1, 0, 0),

        SMALL_NORTH_WEST("small_north_west", 2, 0, 0),
        SMALL_NORTH_EAST("small_north_east", 2, 1, 0),
        SMALL_SOUTH_WEST("small_south_west", 2, 0, 1),
        SMALL_SOUTH_EAST("small_south_east", 2, 1, 1),

        LARGE_NORTH_WEST("large_north_west", 3, 0, 0),
        LARGE_NORTH("large_north", 3, 1, 0),
        LARGE_NORTH_EAST("large_north_east", 3, 2, 0),
        LARGE_WEST("large_west", 3, 0, 1),
        LARGE_CENTER("large_center", 3, 1, 1),
        LARGE_EAST("large_east", 3, 2, 1),
        LARGE_SOUTH_WEST("large_south_west", 3, 0, 2),
        LARGE_SOUTH("large_south", 3, 1, 2),
        LARGE_SOUTH_EAST("large_south_east", 3, 2, 2);

        private final String name;
        private final int gridSize;
        private final int x;
        private final int z;

        Part(String name, int gridSize, int x, int z) {
            this.name = name;
            this.gridSize = gridSize;
            this.x = x;
            this.z = z;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int gridSize() {
            return this.gridSize;
        }

        public int x() {
            return this.x;
        }

        public int z() {
            return this.z;
        }

        private static Part smallAt(int x, int z) {
            return switch (z * 2 + x) {
                case 0 -> SMALL_NORTH_WEST;
                case 1 -> SMALL_NORTH_EAST;
                case 2 -> SMALL_SOUTH_WEST;
                case 3 -> SMALL_SOUTH_EAST;
                default -> throw new IllegalArgumentException("2x2 part coordinates must be in [0, 1]");
            };
        }

        private static Part largeAt(int x, int z) {
            return switch (z * 3 + x) {
                case 0 -> LARGE_NORTH_WEST;
                case 1 -> LARGE_NORTH;
                case 2 -> LARGE_NORTH_EAST;
                case 3 -> LARGE_WEST;
                case 4 -> LARGE_CENTER;
                case 5 -> LARGE_EAST;
                case 6 -> LARGE_SOUTH_WEST;
                case 7 -> LARGE_SOUTH;
                case 8 -> LARGE_SOUTH_EAST;
                default -> throw new IllegalArgumentException("3x3 part coordinates must be in [0, 2]");
            };
        }
    }
}
