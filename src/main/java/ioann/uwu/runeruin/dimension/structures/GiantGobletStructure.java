package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;

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

        ChunkPos chunk = ctx.chunkPos();
        BoundingBox boundingBox = new BoundingBox(
                chunk.getMinBlockX(),
                0,
                chunk.getMinBlockZ(),
                chunk.getMaxBlockX(),
                400,
                chunk.getMaxBlockZ()
        );

        var piece = new OceanMonumentPieces.OceanMonumentWingRoom(Direction.NORTH, boundingBox, 3000);

        builder.addPiece(piece);
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.GIANT_GOBLET.get();
    }
}
