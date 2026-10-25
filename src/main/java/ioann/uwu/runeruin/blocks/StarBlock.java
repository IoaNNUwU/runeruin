package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.particles.RRParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A star over the abyss of the Void: invisible, only its glitter gives it away. It drops stardust and is never placed back. */
public class StarBlock extends Block {
    public static final MapCodec<StarBlock> CODEC = simpleCodec(StarBlock::new);
    private static final VoxelShape SHAPE = Block.cube(6.0);
    // The client ticks a nearby block about once in three seconds, and a glitter lives about as long.
    private static final int GLITTER_PER_TICK = 3;

    public StarBlock(Properties properties) {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < GLITTER_PER_TICK; i++) {
            level.addParticle(
                    RRParticleTypes.STAR.get(),
                    pos.getX() + 0.5 + random.nextGaussian() * 0.15,
                    pos.getY() + 0.5 + random.nextGaussian() * 0.15,
                    pos.getZ() + 0.5 + random.nextGaussian() * 0.15,
                    random.nextGaussian() * 0.005,
                    random.nextGaussian() * 0.005,
                    random.nextGaussian() * 0.005
            );
        }
    }
}
