package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
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

public final class BaobabPiece extends StructurePiece {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int height;
    private final long seed;
    private final int[] groundHeights;
    private transient Map<BlockPos, BlockState> treePlan;

    public BaobabPiece(int centerX, int centerZ, int radius, int height, long seed, int[] groundHeights) {
        super(RRStructurePieceTypes.BAOBAB_PIECE.get(), 0, boundingBox(centerX, centerZ, groundHeights));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.height = height;
        this.seed = seed;
        this.groundHeights = groundHeights.clone();
    }

    public BaobabPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.BAOBAB_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.radius = tag.getIntOr("R", 28);
        this.height = tag.getIntOr("H", 32);
        this.seed = tag.getLongOr("S", defaultSeed(this.centerX, this.centerZ));
        this.groundHeights = tag.getIntArray("G").orElseGet(BaobabPiece::defaultGroundHeights);
    }

    private static BoundingBox boundingBox(int centerX, int centerZ, int[] groundHeights) {
        int extent = BaobabTreeGenerator.MAX_HORIZONTAL_EXTENT;
        int centerGroundY = groundHeights[BaobabTreeGenerator.groundProfileIndex(0, 0)];
        return new BoundingBox(
                centerX - extent,
                centerGroundY - (Const.TERRAIN_HEIGHT - Const.TERRAIN_MIN_HEIGHT) - 1,
                centerZ - extent,
                centerX + extent,
                centerGroundY + 1 + BaobabTreeGenerator.MAX_TREE_HEIGHT + 8,
                centerZ + extent
        );
    }

    private static long defaultSeed(int x, int z) {
        return x * 341873128712L + z * 132897987541L;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", this.centerX);
        tag.putInt("CZ", this.centerZ);
        tag.putInt("R", this.radius);
        tag.putInt("H", this.height);
        tag.putLong("S", this.seed);
        tag.putIntArray("G", this.groundHeights);
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
        if (this.groundHeights.length != BaobabTreeGenerator.groundProfileLength()) {
            return;
        }

        Map<BlockPos, BlockState> plan = this.treePlan();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Map.Entry<BlockPos, BlockState> entry : plan.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!chunkBB.isInside(pos) || level.isOutsideBuildHeight(pos.getY())) {
                continue;
            }

            if (!BaobabTreeGenerator.canTreeReplace(level.getBlockState(pos))) {
                continue;
            }
            level.setBlock(mutable.set(pos), entry.getValue(), Block.UPDATE_CLIENTS);
        }
    }

    private Map<BlockPos, BlockState> treePlan() {
        if (this.treePlan == null) {
            this.treePlan = BaobabTreeGenerator.generate(
                    this.centerX,
                    this.centerZ,
                    this.radius,
                    this.height,
                    this.seed,
                    new BaobabTreeGenerator.GroundProfile(this.centerX, this.centerZ, this.groundHeights),
                    RRBlocks.BAOBAB_WOOD.get().defaultBlockState(),
                    RRBlocks.BAOBAB_LEAVES.get().defaultBlockState()
            );
        }
        return this.treePlan;
    }

    private static int[] defaultGroundHeights() {
        int[] values = new int[BaobabTreeGenerator.groundProfileLength()];
        java.util.Arrays.fill(values, Const.BLOOMING_CAVES_Y + Const.TERRAIN_HEIGHT);
        return values;
    }
}
