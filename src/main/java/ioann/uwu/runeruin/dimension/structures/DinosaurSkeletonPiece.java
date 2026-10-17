package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.chunkgenerator.VoidGen;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class DinosaurSkeletonPiece extends StructurePiece {
    private final BlockPos origin;
    private final long seed;
    // Chunks of one structure are decorated on several threads at once: volatile hands each of them the finished plan.
    private volatile Map<BlockPos, BlockState> bones;

    public DinosaurSkeletonPiece(BlockPos origin, long seed, Map<BlockPos, BlockState> bones) {
        super(RRStructurePieceTypes.DINOSAUR_SKELETON_PIECE.get(), 0, BoundingBox.encapsulatingPositions(bones.keySet())
                .orElseThrow().moved(origin.getX(), origin.getY(), origin.getZ()));
        this.origin = origin;
        this.seed = seed;
        this.bones = bones;
    }

    public DinosaurSkeletonPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.DINOSAUR_SKELETON_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
        this.seed = tag.getLongOr("S", 0L);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("X", this.origin.getX());
        tag.putInt("Y", this.origin.getY());
        tag.putInt("Z", this.origin.getZ());
        tag.putLong("S", this.seed);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        Map<BlockPos, BlockState> bones = this.bones;
        if (bones == null) {
            this.bones = bones = DinosaurSkeletonGenerator.generate(this.seed);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Map.Entry<BlockPos, BlockState> bone : bones.entrySet()) {
            pos.setWithOffset(this.origin, bone.getKey());
            // Not under the Lost caves, and not into the ceiling of the Void where it rises into their floor.
            if (chunkBB.isInside(pos) && pos.getY() >= Const.LOST_CAVES_Y && !VoidGen.reaches(level, pos)) {
                level.setBlock(pos, bone.getValue(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
