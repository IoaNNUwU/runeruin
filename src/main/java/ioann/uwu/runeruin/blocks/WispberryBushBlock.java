package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.loottables.RRLootTables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

public class WispberryBushBlock extends VegetationBlock implements BonemealableBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SHAPE_SAPLING = Block.column(8.0F, 0.0F, 6.0F);
    private static final VoxelShape SHAPE_GROWN = Block.column(14.0F, 0.0F, 12.0F);
    // Only the middle of the bush holds whoever walks into it: its edge can be brushed past.
    private static final VoxelShape CORE_SAPLING = Block.column(4.0F, 0.0F, 6.0F);
    private static final VoxelShape CORE_GROWN = Block.column(8.0F, 0.0F, 10.0F);

    public WispberryBushBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(AGE, 0));
    }

    public static final MapCodec<WispberryBushBlock> CODEC = simpleCodec(WispberryBushBlock::new);

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return RRItems.WISPBERRY.toStack();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SHAPE_SAPLING : SHAPE_GROWN;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(AGE) == 0 ? CORE_SAPLING : CORE_GROWN;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 3;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        if (age < 3 && CommonHooks.canCropGrow(level, pos, state, random.nextInt(5) == 0)) {
            BlockState newState = state.setValue(AGE, age + 1);
            level.setBlock(pos, newState, 2);
            CommonHooks.fireCropGrowPost(level, pos, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
        }
    }

    /** The bush holds whoever walks into it as a cobweb does, and does not hurt. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof LivingEntity && !entity.is(EntityTypes.FROG) && !entity.is(EntityTypes.BOGGED)) {
            entity.makeStuckInBlock(state, new Vec3(0.25, 0.05F, 0.25));
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        boolean isMaxAge = age == 3;

        return !isMaxAge && itemStack.is(Items.BONE_MEAL) ?
                InteractionResult.PASS
                : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

        if (state.getValue(AGE) > 1) {
            if (level instanceof ServerLevel serverLevel) {

                Block.dropFromBlockInteractLootTable(serverLevel,
                        RRLootTables.HARVEST_WISPBERRY,
                        state,
                        level.getBlockEntity(pos),
                        null,
                        player,
                        (serverlvl, itemStack) -> Block.popResource(serverlvl, pos, itemStack));

                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);

                BlockState newState = state.setValue(AGE, 1);
                serverLevel.setBlock(pos, newState, 2);
                serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 3 && level.getBlockState(pos.above()).isAir() && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        return true;
    }

    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int newAge = Math.min(3, state.getValue(AGE) + 1);
        level.setBlock(pos, state.setValue(AGE, newAge), 2);
    }

    public static int getLightLevel(BlockState state) {
        int lightLevel;
        switch (state.getValue(AGE)) {
            case 0 -> lightLevel = 0;
            case 1 -> lightLevel = 3;
            case 2 -> lightLevel = 6;
            default -> lightLevel = 9;
        }
        return lightLevel;
    }
}
