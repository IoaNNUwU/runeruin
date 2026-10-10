package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.items.RRItems;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

/**
 * A small bush on moss that grows mossberries, each in a place of its own. The berries glow, prick and are
 * picked by hand; they grow again by themselves, and bone meal adds more and spreads bushes over the moss around.
 */
public class MossberryBushBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<MossberryBushBlock> CODEC = simpleCodec(MossberryBushBlock::new);

    /**
     * A place a berry can grow in: where its cube is and how big, in pixels. {@code lift} (centre above the
     * ground) and {@code tilt} (degrees outwards) only shape the model.
     */
    public record Berry(BooleanProperty property, double x, double z, int size, double lift, double tilt) {}

    /**
     * One big cube, three medium ones around it and three small ones further out: together they make a dome.
     * The model turns each berry to face away from the middle, so none may stand on a diagonal of the block:
     * there its spike planes would lie in the planes of the crossed bush and flicker.
     */
    public static final List<Berry> BERRIES = List.of(
            berry(0, 8, 8, 4, 2, 8),
            berry(1, 4.5, 7, 3, 1, 30),
            berry(2, 11.5, 6.5, 3, 0.7, 30),
            berry(3, 8.5, 11.5, 3, 1.3, 30),
            berry(4, 12.5, 9.5, 2, 0.5, 50),
            berry(5, 3.5, 10, 2, 0.3, 50),
            berry(6, 7, 3, 2, 0.8, 50)
    );

    /** Dry twigs lying around the bush, just above the ground. */
    public static final BooleanProperty TWIGS = BooleanProperty.create("twigs");

    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 6.0);

    public MossberryBushBlock(Properties properties) {
        super(properties);
        BlockState bare = this.defaultBlockState().setValue(TWIGS, false);
        for (Berry berry : BERRIES) {
            bare = bare.setValue(berry.property(), false);
        }
        this.registerDefaultState(bare);
    }

    private static Berry berry(int number, double x, double z, int size, double lift, double tilt) {
        return new Berry(BooleanProperty.create("berry_" + number), x, z, size, lift, tilt);
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Only moss: any moss block, deep moss among them, and a deep moss layer of full height. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.MOSS_BLOCKS)
                || state.is(RRBlocks.DEEP_MOSS_LAYER) && state.getValue(DeepMossLayerBlock.LAYERS) == DeepMossLayerBlock.MAX_HEIGHT;
    }

    private static int berryCount(BlockState state) {
        return (int) BERRIES.stream().filter(berry -> state.getValue(berry.property())).count();
    }

    /** One more berry: any of the smallest that are missing. */
    private static BlockState grow(BlockState state, RandomSource random) {
        List<Berry> free = Util.toShuffledList(BERRIES.stream().filter(berry -> !state.getValue(berry.property())), random);
        free.sort(Comparator.comparingInt(Berry::size));
        return free.isEmpty() ? state : state.setValue(free.getFirst().property(), true);
    }

    /** No berries, no light; a full bush glows like a ripe wispberry bush. */
    public static int lightLevel(BlockState state) {
        int berries = berryCount(state);
        return berries == 0 ? 0 : berries + 2;
    }

    /** A picked bush gives fewer berries than it had: three at most. */
    private static ItemStack picked(BlockState state, RandomSource random) {
        return new ItemStack(RRItems.MOSSBERRY.get(), Math.min(berryCount(state), 1 + random.nextInt(3)));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return berryCount(state) < BERRIES.size();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(5) == 0)) {
            BlockState grown = grow(state, random);
            level.setBlock(pos, grown, Block.UPDATE_CLIENTS);
            CommonHooks.fireCropGrowPost(level, pos, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(grown));
        }
    }

    /** Bone meal does its own work instead of picking the berries. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return stack.is(Items.BONE_MEAL) ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** One click picks every berry; the bush and its twigs stay. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (berryCount(state) == 0) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            Block.popResource(serverLevel, pos, picked(state, serverLevel.getRandom()));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F);
            BlockState bare = state;
            for (Berry berry : BERRIES) {
                bare = bare.setValue(berry.property(), false);
            }
            serverLevel.setBlock(pos, bare, Block.UPDATE_CLIENTS);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, bare));
        }
        return InteractionResult.SUCCESS;
    }

    /** A broken bush drops its berries as if they were picked; the bush itself is lost. */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (berryCount(state) > 0) {
            Block.popResource(level, pos, picked(state, level.getRandom()));
        }
    }

    /** The berries prick whoever moves through them, as a sweet berry bush does, but do not slow anyone down. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof LivingEntity && level instanceof ServerLevel serverLevel && berryCount(state) > 0) {
            Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
            if (Math.abs(movement.x()) >= 0.003F || Math.abs(movement.z()) >= 0.003F) {
                entity.hurtServer(serverLevel, level.damageSources().sweetBerryBush(), 1.0F);
            }
        }
    }

    /** Mobs walk around the berries as they walk around a sweet berry bush. */
    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return berryCount(state) > 0 ? PathType.DAMAGING : super.getBlockPathType(state, level, pos, mob);
    }

    /** Free moss next to a bush: bone meal can start a new bush there. */
    private boolean canSpreadTo(LevelReader level, BlockPos pos, BlockState state) {
        return state.isAir() && this.defaultBlockState().canSurvive(level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (nearState.is(this) ? berryCount(nearState) < BERRIES.size() : canSpreadTo(level, near, nearState)) {
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
            BlockState grown;
            if (near.equals(pos)) {
                // One or two berries on the bush itself.
                grown = grow(nearState, random);
                if (random.nextBoolean()) {
                    grown = grow(grown, random);
                }
            } else if (random.nextBoolean() && (nearState.is(this) || this.canSpreadTo(level, near, nearState))) {
                // At most one around it, on a bush or on free moss, where it starts a new bush:
                // in the place nearest to the bush with the bone meal.
                BlockState bush = nearState.is(this) ? nearState : this.defaultBlockState();
                double fromX = (pos.getX() - near.getX()) * 16 + 8;
                double fromZ = (pos.getZ() - near.getZ()) * 16 + 8;
                grown = BERRIES.stream()
                        .filter(berry -> !bush.getValue(berry.property()))
                        .min(Comparator.comparingDouble(berry -> Mth.square(berry.x() - fromX) + Mth.square(berry.z() - fromZ)))
                        .map(berry -> bush.setValue(berry.property(), true))
                        .orElse(bush);
            } else {
                continue;
            }
            // Half of the bushes that grow something also get twigs.
            if (grown != nearState && random.nextBoolean()) {
                grown = grown.setValue(TWIGS, true);
            }
            level.setBlock(near, grown, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TWIGS);
        BERRIES.forEach(berry -> builder.add(berry.property()));
    }
}
