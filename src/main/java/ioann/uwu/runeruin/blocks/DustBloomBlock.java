package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.particles.RRParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A flower under a ceiling that sheds glowing dust, like a spore blossom without the cloud around it. */
public class DustBloomBlock extends Block {
    public static final MapCodec<DustBloomBlock> CODEC = simpleCodec(DustBloomBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 9.0, 16.0);
    // The client ticks a nearby block about once in three seconds: several motes keep the stream visible.
    private static final int DUST_PER_TICK = 4;

    public DustBloomBlock(Properties properties) {
        super(properties);
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
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
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
        if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < DUST_PER_TICK; i++) {
            level.addParticle(
                    RRParticleTypes.VOID_DUST.get(),
                    pos.getX() + 0.25 + random.nextDouble() * 0.5,
                    pos.getY() + 0.5 + random.nextDouble() * 0.2,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }
}
