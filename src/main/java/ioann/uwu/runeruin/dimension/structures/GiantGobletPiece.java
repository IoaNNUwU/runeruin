package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class GiantGobletPiece extends StructurePiece {

    public static final int PILLAR_RADIUS = 5;
    public static final int RIM_HEIGHT = 3;
    public static final int FLOOR_HEIGHT = 2;
    public static final int RIM_THICKNESS = 2;

    private final int centerX;
    private final int centerZ;
    private final int height;
    private final int bowlRadius;

    public static int bowlTopY(int height) {
        return Const.LOST_CAVES_Y + height - 1;
    }

    public static int bowlBottomY(int height) {
        return bowlTopY(height) - RIM_HEIGHT - FLOOR_HEIGHT + 1;
    }

    public GiantGobletPiece(int centerX, int centerZ, int height, int bowlRadius) {
        super(RRStructurePieceTypes.GIANT_GOBLET_PIECE.get(), 0, boundingBox(centerX, centerZ, height, bowlRadius));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.height = height;
        this.bowlRadius = bowlRadius;
    }

    public GiantGobletPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.GIANT_GOBLET_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.height = tag.getIntOr("H", Const.DEEP_CAVES_Y - Const.LOST_CAVES_Y);
        this.bowlRadius = tag.getIntOr("R", this.height / 2);
    }

    private static BoundingBox boundingBox(int centerX, int centerZ, int height, int bowlRadius) {
        return new BoundingBox(
                centerX - bowlRadius,
                Const.LOST_CAVES_Y,
                centerZ - bowlRadius,
                centerX + bowlRadius,
                bowlTopY(height),
                centerZ + bowlRadius
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", this.centerX);
        tag.putInt("CZ", this.centerZ);
        tag.putInt("H", this.height);
        tag.putInt("R", this.bowlRadius);
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
        BlockState pillar = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState floor = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState rim = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        int baseY = Const.LOST_CAVES_Y;
        int rimTopY = bowlTopY(this.height);
        int rimBottomY = rimTopY - RIM_HEIGHT + 1;
        int floorTopY = rimBottomY - 1;
        int floorBottomY = floorTopY - FLOOR_HEIGHT + 1;
        int pillarTopY = floorBottomY - 1;

        int pillarR2 = PILLAR_RADIUS * PILLAR_RADIUS;
        int bowlR2 = this.bowlRadius * this.bowlRadius;
        int innerRim = Math.max(this.bowlRadius - RIM_THICKNESS, 0);
        int innerRim2 = innerRim * innerRim;

        int minX = Math.max(chunkBB.minX(), this.centerX - this.bowlRadius);
        int maxX = Math.min(chunkBB.maxX(), this.centerX + this.bowlRadius);
        int minZ = Math.max(chunkBB.minZ(), this.centerZ - this.bowlRadius);
        int maxZ = Math.min(chunkBB.maxZ(), this.centerZ + this.bowlRadius);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            int dx2 = (x - this.centerX) * (x - this.centerX);
            for (int z = minZ; z <= maxZ; z++) {
                int d2 = dx2 + (z - this.centerZ) * (z - this.centerZ);

                if (d2 <= pillarR2) {
                    for (int y = baseY; y <= pillarTopY; y++) {
                        set(level, pos.set(x, y, z), pillar, chunkBB);
                    }
                }

                if (d2 > bowlR2) {
                    continue;
                }

                for (int y = floorBottomY; y <= floorTopY; y++) {
                    set(level, pos.set(x, y, z), floor, chunkBB);
                }

                if (d2 > innerRim2) {
                    for (int y = rimBottomY; y <= rimTopY; y++) {
                        set(level, pos.set(x, y, z), rim, chunkBB);
                    }
                } else {
                    for (int y = floorTopY + 1; y < rimTopY; y++) {
                        set(level, pos.set(x, y, z), water, chunkBB);
                    }
                }
            }
        }
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state, BoundingBox chunkBB) {
        if (chunkBB.isInside(pos)) {
            level.setBlock(pos, state, 2);
            var fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                level.scheduleTick(pos, fluid.getType(), 0);
            }
        }
    }
}
