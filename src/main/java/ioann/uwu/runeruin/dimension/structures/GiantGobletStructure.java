package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class GiantGobletStructure extends Structure {

    public static final MapCodec<GiantGobletStructure> CODEC = simpleCodec(GiantGobletStructure::new);

    public GiantGobletStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        return onLostCaves(ctx, builder -> generatePieces(builder, ctx));
    }

    protected static Optional<GenerationStub> onLostCaves(GenerationContext context, Consumer<StructurePiecesBuilder> generator) {
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMiddleBlockX();
        int blockZ = chunkPos.getMiddleBlockZ();

        int blockY = Const.LOST_CAVES_Y;

        return Optional.of(new GenerationStub(new BlockPos(blockX, blockY, blockZ), generator));
    }

    private static void generatePieces(StructurePiecesBuilder builder, Structure.GenerationContext ctx) {

        ChunkPos origin = ctx.chunkPos();

        List<ChunkPos> chunks = List.of(
                new ChunkPos(origin.x(), origin.z() + 1),
                new ChunkPos(origin.x() + 1, origin.z()),
                new ChunkPos(origin.x(), origin.z() - 1),
                new ChunkPos(origin.x() - 1, origin.z())
        );

        for (ChunkPos chunk : chunks) {
            BoundingBox boundingBox = getBoundingBox(chunk);

            builder.addPiece(new GiantGobletPiece(0, boundingBox));
        }
    }

    private static BoundingBox getBoundingBox(ChunkPos chunk) {
        return new BoundingBox(
                chunk.getMinBlockX(),
                0,
                chunk.getMinBlockZ(),
                chunk.getMaxBlockX(),
                400,
                chunk.getMaxBlockZ()
        );
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.GIANT_GOBLET.get();
    }
}
