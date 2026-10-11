package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Map;

public final class PirateShipwreckPiece extends StructurePiece {
    private final BlockPos origin;
    private final long seed;
    private Map<BlockPos, BlockState> blocks;

    public PirateShipwreckPiece(BlockPos origin, long seed, Map<BlockPos, BlockState> blocks) {
        super(RRStructurePieceTypes.PIRATE_SHIPWRECK_PIECE.get(), 0, BoundingBox.encapsulatingPositions(blocks.keySet())
                .orElseThrow().moved(origin.getX(), origin.getY(), origin.getZ()));
        this.origin = origin;
        this.seed = seed;
        this.blocks = blocks;
    }

    public PirateShipwreckPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.PIRATE_SHIPWRECK_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getIntOr("X", 0), tag.getIntOr("Y", 0), tag.getIntOr("Z", 0));
        this.seed = tag.getLongOr("S", 0L);
    }

    /** The bottom of the keel under the main mast. */
    public BlockPos origin() {
        return this.origin;
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
        if (this.blocks == null) {
            this.blocks = PirateShipwreckGenerator.generate(this.seed).blocks();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Map.Entry<BlockPos, BlockState> block : this.blocks.entrySet()) {
            pos.setWithOffset(this.origin, block.getKey());
            if (!chunkBB.isInside(pos)) {
                continue;
            }
            // The masts end at the ceiling of the cave instead of going on inside it.
            if (pos.getY() >= TopLayerAndBloomingCavesGen.CEILING_LOWEST_Y && !level.isEmptyBlock(pos)) {
                continue;
            }
            level.setBlock(pos, block.getValue(), Block.UPDATE_CLIENTS);
            // Fences and panes join their neighbours once the chunk is finished, as those of vanilla structures do.
            if (block.getValue().getBlock() instanceof CrossCollisionBlock && level instanceof WorldGenRegion) {
                level.getChunk(pos).markPosForPostProcessing(pos);
            }
        }
    }
}
