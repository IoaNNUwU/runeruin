package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Decorative Arcane Stone used for the distinct parts of a RuneRuin portal frame. */
public class ArcaneStonePortalBlock extends Block {
    public static final MapCodec<ArcaneStonePortalBlock> CODEC = simpleCodec(ArcaneStonePortalBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<MiddleSide> MIDDLE_SIDE = EnumProperty.create("middle_side", MiddleSide.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public ArcaneStonePortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, Part.CORNER)
                .setValue(MIDDLE_SIDE, MiddleSide.SINGLE)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(RRBlocks.ARCANE_STONE.get());
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(RRBlocks.ARCANE_STONE.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, MIDDLE_SIDE, AXIS);
    }

    public enum Part implements StringRepresentable {
        CORNER("corner"),
        COLUMN("column"),
        MIDDLE("middle"),
        VERT_COLUMN("vert_column");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /** Identifies the one-block medallion or each half of an even-width medallion. */
    public enum MiddleSide implements StringRepresentable {
        SINGLE("single"),
        LEFT("left"),
        RIGHT("right");

        private final String name;

        MiddleSide(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
