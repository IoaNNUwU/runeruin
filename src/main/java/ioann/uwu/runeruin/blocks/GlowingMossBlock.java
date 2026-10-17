package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class GlowingMossBlock extends Block implements BonemealableBlock {

    private static final int NEIGHBOR_LIGHT_INCREASE_COUNT = 2;
    private static final int NEIGHBOR_LIGHT_DECREASE_COUNT = 4;

    public static final int PLACEMENT_LIGHT_MIN = 4;
    public static final int PLACEMENT_LIGHT_MAX = 5;
    public static final int MAX_LIGHT = 15;

    public static final IntegerProperty MIN_LIGHT = IntegerProperty.create("min_light", PLACEMENT_LIGHT_MIN, MAX_LIGHT);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", PLACEMENT_LIGHT_MIN, MAX_LIGHT);
    public static final IntegerProperty TARGET_LIGHT_LEVEL = IntegerProperty.create("target_light_level", PLACEMENT_LIGHT_MIN, MAX_LIGHT);
    public static final MapCodec<GlowingMossBlock> CODEC = simpleCodec(GlowingMossBlock::new);

    public GlowingMossBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MIN_LIGHT, PLACEMENT_LIGHT_MIN)
                .setValue(LIGHT, PLACEMENT_LIGHT_MIN)
                .setValue(TARGET_LIGHT_LEVEL, PLACEMENT_LIGHT_MIN));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MIN_LIGHT, LIGHT, TARGET_LIGHT_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateForPlacement(this.defaultBlockState(), context.getLevel().getRandom());
    }

    public static BlockState stateForPlacement(BlockState base, RandomSource random) {
        int minLight = randomMinLight(random);
        return base.setValue(MIN_LIGHT, minLight).setValue(LIGHT, minLight).setValue(TARGET_LIGHT_LEVEL, minLight);
    }

    public static int randomMinLight(RandomSource random) {
        return PLACEMENT_LIGHT_MIN + random.nextInt(PLACEMENT_LIGHT_MAX - PLACEMENT_LIGHT_MIN + 1);
    }

    public static int getLightLevel(BlockState state) {
        return state.getValue(LIGHT);
    }

    public static boolean isGlowingMoss(BlockState state) {
        return state.getBlock() instanceof GlowingMossBlock || state.getBlock() instanceof GlowingMossCarpetBlock;
    }

    public static void setTargetLightLevel(ServerLevel level, BlockPos pos, BlockState state, int target) {
        level.setBlock(pos, state.setValue(TARGET_LIGHT_LEVEL, target), Block.UPDATE_CLIENTS);
    }

    public static void tickLight(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!Config.GLOWING_MOSS_DYNAMIC_LIGHT.get()) {
            return;
        }

        int light = state.getValue(LIGHT);
        BlockState next = light < state.getValue(TARGET_LIGHT_LEVEL) ? brighter(state) : dimmer(state);
        if (next == state) {
            return;
        }
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        if (next.getValue(LIGHT) > light) {
            changeRandomNeighbors(level, pos, state, random, NEIGHBOR_LIGHT_INCREASE_COUNT,
                    neighbor -> neighbor.getValue(LIGHT) < neighbor.getValue(TARGET_LIGHT_LEVEL), GlowingMossBlock::brighter);
        } else if (next.getValue(LIGHT) < light) {
            changeRandomNeighbors(level, pos, state, random, NEIGHBOR_LIGHT_DECREASE_COUNT,
                    neighbor -> neighbor.getValue(LIGHT) > neighbor.getValue(MIN_LIGHT), GlowingMossBlock::dimmer);
        }
    }

    /** One step towards the target, which itself sinks back towards the minimum. Only for a light below its target. */
    private static BlockState brighter(BlockState state) {
        int target = state.getValue(TARGET_LIGHT_LEVEL);
        return state.setValue(LIGHT, state.getValue(LIGHT) + 1)
                .setValue(TARGET_LIGHT_LEVEL, Math.max(target - 1, state.getValue(MIN_LIGHT)));
    }

    /** One step down to the minimum; a target the light has reached sinks with it. */
    private static BlockState dimmer(BlockState state) {
        int minLight = state.getValue(MIN_LIGHT);
        int light = state.getValue(LIGHT);
        int target = state.getValue(TARGET_LIGHT_LEVEL);
        return state.setValue(LIGHT, Math.max(light - 1, minLight))
                .setValue(TARGET_LIGHT_LEVEL, light == target ? Math.max(target - 1, minLight) : target);
    }

    /** Changes up to {@code count} random glowing moss neighbors for which {@code applies} holds. */
    private static void changeRandomNeighbors(ServerLevel level, BlockPos pos, BlockState sourceState, RandomSource random,
                                              int count, Predicate<BlockState> applies, UnaryOperator<BlockState> change) {
        List<BlockPos> candidates = collectGlowingMossNeighbors(level, pos, sourceState, applies);
        int spreadCount = Math.min(count, candidates.size());
        for (int i = 0; i < spreadCount; i++) {
            BlockPos chosen = candidates.remove(random.nextInt(candidates.size()));
            BlockState chosenState = level.getBlockState(chosen);
            if (applies.test(chosenState)) {
                level.setBlock(chosen, change.apply(chosenState), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static List<BlockPos> collectGlowingMossNeighbors(ServerLevel level, BlockPos pos, BlockState sourceState, Predicate<BlockState> predicate) {
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        List<BlockPos> candidates = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            tryAddGlowingMossNeighbor(candidates, level, neighborPos.setWithOffset(pos, direction), predicate);
        }

        if (sourceState.getBlock() instanceof GlowingMossCarpetBlock) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                tryAddGlowingMossNeighbor(candidates, level, neighborPos.setWithOffset(pos.below(), direction), predicate);
            }
        } else if (sourceState.getBlock() instanceof GlowingMossBlock) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                tryAddGlowingMossNeighbor(candidates, level, neighborPos.setWithOffset(pos, direction).move(Direction.UP), predicate);
            }
        }

        return candidates;
    }

    private static void tryAddGlowingMossNeighbor(List<BlockPos> candidates, ServerLevel level, BlockPos pos, Predicate<BlockState> predicate) {
        BlockState neighborState = level.getBlockState(pos);
        if (isGlowingMoss(neighborState) && predicate.test(neighborState)) {
            candidates.add(pos.immutable());
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tickLight(state, level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.registryAccess()
                .lookup(Registries.CONFIGURED_FEATURE)
                .flatMap(registry -> registry.get(CaveFeatures.MOSS_PATCH_BONEMEAL))
                .ifPresent(mossPatch -> mossPatch.value().place(level, level.getChunkSource().getGenerator(), random, pos.above()));
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
