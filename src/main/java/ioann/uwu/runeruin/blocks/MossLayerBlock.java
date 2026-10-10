package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.items.RRItems;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Moss stacked like snow layers. Mossberries grow on top of it, each in a place of its own: they glow, prick,
 * are picked by hand and planted back, and bone meal adds more.
 */
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

    /** Dry twigs lying under the plants, just above the moss. */
    public static final BooleanProperty TWIGS = BooleanProperty.create("twigs");

    public MossLayerBlock(Properties properties) {
        super(properties);
        BlockState bare = this.defaultBlockState().setValue(TWIGS, false);
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

    private static int plantCount(BlockState state) {
        return (int) PLANTS.stream().filter(plant -> state.getValue(plant.property())).count();
    }

    /** The free places in random order. */
    private static List<Plant> freePlaces(BlockState state, RandomSource random) {
        return Util.toShuffledList(PLANTS.stream().filter(plant -> !state.getValue(plant.property())), random);
    }

    /** No plants, no light; a full block glows like a ripe berry bush. */
    public static int lightLevel(BlockState state) {
        int plants = plantCount(state);
        return plants == 0 ? 0 : plants + 2;
    }

    /** A picked block gives fewer berries than it had plants: three at most. */
    private static ItemStack berries(BlockState state, RandomSource random) {
        return new ItemStack(RRItems.MOSSBERRY.get(), Math.min(plantCount(state), 1 + random.nextInt(3)));
    }

    /** A mossberry in the hand is planted in one of the smallest free places. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(RRItems.MOSSBERRY) && plantCount(state) < PLANTS.size()) {
            if (level instanceof ServerLevel serverLevel) {
                List<Plant> free = freePlaces(state, serverLevel.getRandom());
                free.sort(Comparator.comparingInt(Plant::size));
                serverLevel.setBlock(pos, state.setValue(free.getFirst().property(), true), Block.UPDATE_CLIENTS);
                serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        // Bone meal and more moss do their own work instead of picking the berries.
        return stack.is(Items.BONE_MEAL) || stack.is(this.asItem())
                ? InteractionResult.PASS
                : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** One click picks every plant; the twigs stay. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (plantCount(state) == 0) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            Block.popResource(serverLevel, pos, berries(state, serverLevel.getRandom()));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            BlockState picked = state;
            for (Plant plant : PLANTS) {
                picked = picked.setValue(plant.property(), false);
            }
            serverLevel.setBlock(pos, picked, Block.UPDATE_CLIENTS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }

    /** A broken block drops its berries as if they were picked, on top of the moss from the loot table. */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (plantCount(state) > 0) {
            Block.popResource(level, pos, berries(state, level.getRandom()));
        }
    }

    /** The plants prick whoever moves over them, as a sweet berry bush does, but do not slow anyone down. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof LivingEntity && level instanceof ServerLevel serverLevel && plantCount(state) > 0) {
            Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
            if (Math.abs(movement.x()) >= 0.003F || Math.abs(movement.z()) >= 0.003F) {
                entity.hurtServer(serverLevel, level.damageSources().sweetBerryBush(), 1.0F);
            }
        }
    }

    /** Mobs walk around the plants as they walk around a sweet berry bush. */
    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return plantCount(state) > 0 ? PathType.DAMAGING : super.getBlockPathType(state, level, pos, mob);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (nearState.is(this) && plantCount(nearState) < PLANTS.size()) {
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
            List<Plant> free = freePlaces(nearState, random);
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
            // Half of the blocks that grow something also get twigs.
            if (grownState != nearState && random.nextBoolean()) {
                grownState = grownState.setValue(TWIGS, true);
            }
            level.setBlock(near, grownState, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TWIGS);
        PLANTS.forEach(plant -> builder.add(plant.property()));
    }
}
