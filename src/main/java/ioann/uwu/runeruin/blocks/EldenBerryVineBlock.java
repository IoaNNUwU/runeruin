package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EldenBerryVineBlock extends Block {
    public static final MapCodec<EldenBerryVineBlock> CODEC = simpleCodec(EldenBerryVineBlock::new);
    public static final BooleanProperty BERRIES = BlockStateProperties.BERRIES;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public EldenBerryVineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BERRIES, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(BERRIES)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), serverLevel.getRandom().nextInt(1, 4));
            float pitch = Mth.randomBetween(serverLevel.getRandom(), 0.8F, 1.2F);
            serverLevel.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
            BlockState newState = state.setValue(BERRIES, false);
            serverLevel.setBlock(pos, newState, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BERRIES);
    }
}
