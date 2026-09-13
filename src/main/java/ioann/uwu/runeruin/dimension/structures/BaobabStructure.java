package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;

public final class BaobabStructure extends Structure {
    public static final MapCodec<BaobabStructure> CODEC = simpleCodec(BaobabStructure::new);

    public BaobabStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos chunkPos = ctx.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        BaobabTreeGenerator.GroundProfile ground = BaobabTreeGenerator.sampleGroundProfile(
                centerX, centerZ, ctx.randomState());
        int baseY = ground.groundYAt(centerX, centerZ) + 1;

        int radius = ctx.random().nextInt(20, 41);
        int height = BaobabTreeGenerator.sampleHeight(radius, ctx.random());
        long seed = ctx.random().nextLong();

        return Optional.of(new GenerationStub(
                new BlockPos(centerX, baseY, centerZ),
                builder -> builder.addPiece(new BaobabPiece(
                        centerX, centerZ, radius, height, seed, ground.copyHeights()))
        ));
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.BAOBAB.get();
    }
}
