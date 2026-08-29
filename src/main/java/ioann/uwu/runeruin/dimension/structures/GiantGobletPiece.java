package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class GiantGobletPiece extends StructurePiece {

    public static final int PILLAR_RADIUS = 8;
    public static final int STEM_PINCH = 2;
    public static final int FLOOR_STEPS = 6;
    public static final int FLOOR_THICKNESS = 2;
    public static final int RIM_THICKNESS = 3;

    private final int centerX;
    private final int centerZ;
    private final int height;
    private final int bowlRadius;

    public static int bowlTopY(int height) {
        return Const.LOST_CAVES_Y + height - 1;
    }

    public static int bowlBottomY(int height) {
        return outerFloorY(height) - FLOOR_STEPS - (FLOOR_THICKNESS - 1);
    }

    /** Top of the outermost floor step: one water block sits on it under the rim. */
    public static int outerFloorY(int height) {
        return bowlTopY(height) - 2;
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
        BlockState stem = Blocks.PALE_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState cup = Blocks.PALE_OAK_WOOD.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        int baseY = Const.LOST_CAVES_Y;
        int rimTopY = bowlTopY(this.height);
        int waterTopY = rimTopY - 1;
        int floorTopY = outerFloorY(this.height);
        int floorBottomY = floorTopY - FLOOR_STEPS;
        int pillarTopY = floorBottomY - FLOOR_THICKNESS + 1;
        int innerRim = Math.max(this.bowlRadius - RIM_THICKNESS, 0);
        int floorRim = Math.max(innerRim - 1, 0);
        int lastCircle = Math.max(this.bowlRadius - 1, 0);

        int minX = Math.max(chunkBB.minX(), this.centerX - this.bowlRadius);
        int maxX = Math.min(chunkBB.maxX(), this.centerX + this.bowlRadius);
        int minZ = Math.max(chunkBB.minZ(), this.centerZ - this.bowlRadius);
        int maxZ = Math.min(chunkBB.maxZ(), this.centerZ + this.bowlRadius);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            int dx2 = (x - this.centerX) * (x - this.centerX);
            for (int z = minZ; z <= maxZ; z++) {
                int d2 = dx2 + (z - this.centerZ) * (z - this.centerZ);

                if (!insideSmooth(d2, this.bowlRadius)) {
                    continue;
                }

                if (!insideSmooth(d2, innerRim)) {
                    int wallBottomY = insideSmooth(d2, lastCircle)
                            ? floorTopY - FLOOR_THICKNESS + 1
                            : floorTopY;
                    for (int y = wallBottomY; y <= rimTopY; y++) {
                        set(level, pos.set(x, y, z), cup, chunkBB);
                    }
                } else {
                    int floorY = floorYForDist(d2, floorRim, floorBottomY, floorTopY);
                    for (int y = floorY - FLOOR_THICKNESS + 1; y <= floorY; y++) {
                        set(level, pos.set(x, y, z), cup, chunkBB);
                    }
                    for (int y = floorY + 1; y <= waterTopY; y++) {
                        set(level, pos.set(x, y, z), water, chunkBB);
                    }
                }

                float stemJitter = columnJitter(x - this.centerX, z - this.centerZ);
                for (int y = baseY; y <= pillarTopY; y++) {
                    if (insideSmooth(d2, stemRadiusAt(y, baseY, pillarTopY) + stemJitter)) {
                        set(level, pos.set(x, y, z), stem, chunkBB);
                    }
                }
            }
        }
    }

    private static float stemRadiusAt(int y, int baseY, int pillarTopY) {
        if (pillarTopY <= baseY) {
            return PILLAR_RADIUS;
        }
        float t = (y - baseY) / (float) (pillarTopY - baseY);
        float pinch = 4f * t * (1f - t);
        return Math.max(1f, PILLAR_RADIUS - pinch * STEM_PINCH);
    }

    /**
     * Smooth concave bowl: deepest at the center, one water block at {@code floorRim}.
     * The leftover ring under the wall sits at {@code floorTopY}.
     */
    private static int floorYForDist(int d2, int floorRim, int floorBottomY, int floorTopY) {
        if (floorRim <= 0) {
            return floorTopY;
        }
        float dist = (float) Math.sqrt(d2);
        if (dist >= floorRim) {
            return floorTopY;
        }
        float t = dist / floorRim;
        float rise = t * t * (3f - 2f * t);
        int span = (floorTopY - 1) - floorBottomY;
        return floorBottomY + Math.round(rise * span);
    }

    private static float columnJitter(int dx, int dz) {
        int h = dx * 374761393 + dz * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h >>> 8) & 255) / 255f * 0.7f - 0.35f;
    }

    private static boolean insideSmooth(int d2, float radius) {
        if (radius <= 0f) {
            return false;
        }
        return d2 * 20 < radius * radius * 19;
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
