package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.Config;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

    public static final int DEFAULT_MIN_LIGHT = 4;
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
                .setValue(MIN_LIGHT, DEFAULT_MIN_LIGHT)
                .setValue(LIGHT, DEFAULT_MIN_LIGHT)
                .setValue(TARGET_LIGHT_LEVEL, DEFAULT_MIN_LIGHT));
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

    public static void tickLight(BlockState state, ServerLevel level, BlockPos pos) {
        if (!Config.GLOWING_MOSS_DYNAMIC_LIGHT.get()) {
            return;
        }

        int minLight = state.getValue(MIN_LIGHT);
        int light = state.getValue(LIGHT);
        int target = state.getValue(TARGET_LIGHT_LEVEL);

        int nextLight = light;
        int nextTarget = target;

        if (light < target) {
            nextLight = light + 1;
        } else if (light == target) {
            nextLight = Math.max(light - 1, minLight);
            nextTarget = Math.max(target - 1, minLight);
        }

        if (nextLight != light || nextTarget != target) {
            level.setBlock(pos, state.setValue(LIGHT, nextLight).setValue(TARGET_LIGHT_LEVEL, nextTarget), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tickLight(state, level, pos);
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
