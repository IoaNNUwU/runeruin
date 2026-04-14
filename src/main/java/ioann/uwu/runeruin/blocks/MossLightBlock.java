package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class MossLightBlock extends Block {
    public MossLightBlock(Properties properties) {
        super(properties);
    }

    public static final MapCodec<MossLightBlock> CODEC = simpleCodec(MossLightBlock::new);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0 && level.environmentAttributes().getValue(EnvironmentAttributes.FIREFLY_BUSH_SOUNDS, pos) && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos) <= pos.getY()) {
            level.playLocalSound(pos, SoundEvents.FIREFLY_BUSH_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
        }

        if (level.getMaxLocalRawBrightness(pos) <= 13 && random.nextDouble() <= 0.7) {
            double fireflyX = (double)pos.getX() + random.nextDouble() * (double)10.0F - (double)5.0F;
            double fireflyY = (double)pos.getY() + random.nextDouble() * (double)5.0F;
            double fireflyZ = (double)pos.getZ() + random.nextDouble() * (double)10.0F - (double)5.0F;
            level.addParticle(ParticleTypes.FIREFLY, fireflyX, fireflyY, fireflyZ, 0.0F, 0.0F, 0.0F);
        }

    }
}
