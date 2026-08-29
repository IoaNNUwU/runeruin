package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
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

    public static final int PILLAR_RADIUS = 8;
    public static final int STEM_PINCH = 2;
    public static final int FLOOR_STEPS = 3;
    public static final int FLOOR_THICKNESS = 2;
    public static final int RIM_THICKNESS = 5;

    private static final int MINI_FLOOR_STEPS = 3;
    private static final int MINI_FLOOR_THICKNESS = 2;
    private static final int MINI_RIM_THICKNESS = 3;
    private static final float MINI_DISTANCE_SCALE = 1.5f;
    private static final int FALL_GAP = 3;
    private static final float ARM_RISE_SCALE = 0.32f;
    private static final int ARM_RISE_MIN = 5;
    private static final int RIM_PEAK_ABOVE_WATER = 1;
    /** Outer hull from stem meet to the water-line floor (see preview_giant_goblet_yz_expected). */
    private static final int OUTER_HULL_STEPS = 7;

    private final int centerX;
    private final int centerZ;
    private final int height;
    private final int bowlRadius;
    private final long seed;

    public static int bowlTopY(int height) {
        return Const.LOST_CAVES_Y + height;
    }

    public static int bowlBottomY(int height) {
        return outerFloorY(height) - OUTER_HULL_STEPS;
    }

    /** Top of the outermost floor step: one water block sits on it under the rim. */
    public static int outerFloorY(int height) {
        return bowlTopY(height) - RIM_PEAK_ABOVE_WATER - 1;
    }

    /**
     * Mini-cup radius: smaller than the main bowl, but wide enough that a rim spill
     * at {@code ~bowlRadius} still lands in the inner water, not on the mini-rim.
     */
    public static int miniBowlRadius(int bowlRadius) {
        int fitted = Math.max(8, Math.round((bowlRadius - 6) / 2.4f));
        return Math.min(fitted, Math.max(8, bowlRadius * 2 / 5));
    }

    public static int generationRadius(int bowlRadius) {
        int mini = miniBowlRadius(bowlRadius);
        return Math.max(bowlRadius, PILLAR_RADIUS + Math.round((MINI_DISTANCE_SCALE + 1f) * mini) + 2);
    }

    public GiantGobletPiece(int centerX, int centerZ, int height, int bowlRadius, long seed) {
        super(RRStructurePieceTypes.GIANT_GOBLET_PIECE.get(), 0, boundingBox(centerX, centerZ, height, bowlRadius));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.height = height;
        this.bowlRadius = bowlRadius;
        this.seed = seed;
    }

    public GiantGobletPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.GIANT_GOBLET_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CX", 0);
        this.centerZ = tag.getIntOr("CZ", 0);
        this.height = tag.getIntOr("H", Const.DEEP_CAVES_Y - Const.LOST_CAVES_Y);
        this.bowlRadius = tag.getIntOr("R", this.height / 2);
        this.seed = tag.getLongOr("S", defaultSeed(this.centerX, this.centerZ, this.height, this.bowlRadius));
    }

    private static BoundingBox boundingBox(int centerX, int centerZ, int height, int bowlRadius) {
        int extent = generationRadius(bowlRadius);
        return new BoundingBox(
                centerX - extent,
                Const.LOST_CAVES_Y,
                centerZ - extent,
                centerX + extent,
                bowlTopY(height),
                centerZ + extent
        );
    }

    private static long defaultSeed(int x, int z, int height, int bowlRadius) {
        return x * 341873128712L + z * 132897987541L + height * 31L + bowlRadius;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", this.centerX);
        tag.putInt("CZ", this.centerZ);
        tag.putInt("H", this.height);
        tag.putInt("R", this.bowlRadius);
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
        BlockState piece = RRBlocks.GIANT_GOBLET_PIECE.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        int baseY = Const.LOST_CAVES_Y;
        int rimTopY = bowlTopY(this.height);
        int waterTopY = rimTopY - RIM_PEAK_ABOVE_WATER;
        int floorTopY = outerFloorY(this.height);
        int floorBottomY = floorTopY - FLOOR_STEPS;
        int hullBottomY = floorTopY - OUTER_HULL_STEPS;
        int pillarTopY = hullBottomY - 1;
        int innerRim = Math.max(this.bowlRadius - RIM_THICKNESS, 0);
        int floorRim = innerRim;
        int spillInner = Math.max(innerRim - 1, 0);

        Arms arms = Arms.create(this.seed, this.bowlRadius, baseY, pillarTopY);

        placeMainGoblet(
                level, chunkBB, piece, water,
                baseY, rimTopY, waterTopY, floorTopY, floorBottomY, hullBottomY, pillarTopY,
                innerRim, floorRim, spillInner, arms
        );
        placeArms(level, chunkBB, piece, arms);
        placeMiniBowls(level, chunkBB, piece, water, arms);
    }

    private void placeMainGoblet(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockState piece,
            BlockState water,
            int baseY,
            int rimTopY,
            int waterTopY,
            int floorTopY,
            int floorBottomY,
            int hullBottomY,
            int pillarTopY,
            int innerRim,
            int floorRim,
            int spillInner,
            Arms arms
    ) {
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

                boolean spillway = arms.isSpillway(x - this.centerX, z - this.centerZ)
                        && !insideSmooth(d2, spillInner);

                if (!insideSmooth(d2, innerRim) || spillway) {
                    if (spillway) {
                        set(level, pos.set(x, floorTopY, z), piece, chunkBB);
                    } else {
                        placeLipColumn(
                                level, chunkBB, pos, x, z, d2,
                                innerRim, this.bowlRadius, floorTopY, floorBottomY, hullBottomY, floorRim,
                                waterTopY, rimTopY, piece
                        );
                    }
                } else {
                    float r = (float) Math.sqrt(d2);
                    int floorY = floorYForDist(d2, floorRim, floorBottomY, floorTopY);
                    for (int y = hullBottomY; y <= floorY; y++) {
                        if (r <= outerRadiusAtY(y, floorRim, floorBottomY, floorTopY, this.bowlRadius)) {
                            set(level, pos.set(x, y, z), piece, chunkBB);
                        }
                    }
                    for (int y = floorY + 1; y <= waterTopY; y++) {
                        set(level, pos.set(x, y, z), water, chunkBB);
                    }
                }

                float stemJitter = columnJitter(x - this.centerX, z - this.centerZ);
                for (int y = baseY; y <= pillarTopY; y++) {
                    if (insideSmooth(d2, stemRadiusAt(y, baseY, pillarTopY) + stemJitter)) {
                        set(level, pos.set(x, y, z), piece, chunkBB);
                    }
                }
            }
        }
    }

    private void placeArms(WorldGenLevel level, BoundingBox chunkBB, BlockState piece, Arms arms) {
        int pad = 4;
        int minX = Math.max(chunkBB.minX(), this.centerX - arms.extent() - pad);
        int maxX = Math.min(chunkBB.maxX(), this.centerX + arms.extent() + pad);
        int minZ = Math.max(chunkBB.minZ(), this.centerZ - arms.extent() - pad);
        int maxZ = Math.min(chunkBB.maxZ(), this.centerZ + arms.extent() + pad);
        int minY = arms.minAttachY() - pad;
        int maxY = arms.maxArmTop() + pad;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                float dx = x - this.centerX + 0.5f;
                float dz = z - this.centerZ + 0.5f;
                for (int i = 0; i < arms.count; i++) {
                    float ux = arms.cos[i];
                    float uz = arms.sin[i];
                    float along = dx * ux + dz * uz;
                    float side = dx * -uz + dz * ux;
                    float s = along - PILLAR_RADIUS;
                    if (s < -2f || s > arms.reach + 2f || Math.abs(side) > 4f) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        if (insideArm(s, y, side, arms, arms.attachY[i])) {
                            set(level, pos.set(x, y, z), piece, chunkBB);
                        }
                    }
                }
            }
        }
    }

    private void placeMiniBowls(WorldGenLevel level, BoundingBox chunkBB, BlockState piece, BlockState water, Arms arms) {
        for (int i = 0; i < arms.count; i++) {
            int cupX = this.centerX + Math.round(arms.cos[i] * (PILLAR_RADIUS + arms.reach));
            int cupZ = this.centerZ + Math.round(arms.sin[i] * (PILLAR_RADIUS + arms.reach));
            placeBowl(
                    level, chunkBB, cupX, cupZ,
                    arms.miniRimTopY[i], arms.miniRadius,
                    MINI_FLOOR_STEPS, MINI_FLOOR_THICKNESS, MINI_RIM_THICKNESS,
                    piece, water,
                    arms.miniSpillAngles[i], arms.miniSpillHalf[i]
            );
        }
    }

    private static void placeBowl(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int cx,
            int cz,
            int rimTopY,
            int radius,
            int floorSteps,
            int floorThickness,
            int rimThickness,
            BlockState piece,
            BlockState water,
            float[] spillAngles,
            float[] spillHalf
    ) {
        int waterTopY = rimTopY - RIM_PEAK_ABOVE_WATER;
        int floorTopY = rimTopY - RIM_PEAK_ABOVE_WATER - 1;
        int floorBottomY = floorTopY - floorSteps;
        int hullBottomY = floorBottomY - floorThickness + 1;
        int innerRim = Math.max(radius - rimThickness, 0);
        int floorRim = innerRim;
        int spillInner = Math.max(innerRim - 1, 0);

        int minX = Math.max(chunkBB.minX(), cx - radius);
        int maxX = Math.min(chunkBB.maxX(), cx + radius);
        int minZ = Math.max(chunkBB.minZ(), cz - radius);
        int maxZ = Math.min(chunkBB.maxZ(), cz + radius);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            int dx2 = (x - cx) * (x - cx);
            for (int z = minZ; z <= maxZ; z++) {
                int d2 = dx2 + (z - cz) * (z - cz);
                if (!insideSmooth(d2, radius)) {
                    continue;
                }
                boolean spillway = hitsSpill(x - cx, z - cz, spillAngles, spillHalf)
                        && !insideSmooth(d2, spillInner);
                if (!insideSmooth(d2, innerRim) || spillway) {
                    if (spillway) {
                        set(level, pos.set(x, floorTopY, z), piece, chunkBB);
                    } else {
                        placeLipColumn(
                                level, chunkBB, pos, x, z, d2,
                                innerRim, radius, floorTopY, floorBottomY, hullBottomY, floorRim,
                                waterTopY, rimTopY, piece
                        );
                    }
                } else {
                    int floorY = floorYForDist(d2, floorRim, floorBottomY, floorTopY);
                    for (int y = floorY - floorThickness + 1; y <= floorY; y++) {
                        set(level, pos.set(x, y, z), piece, chunkBB);
                    }
                    for (int y = floorY + 1; y <= waterTopY; y++) {
                        set(level, pos.set(x, y, z), water, chunkBB);
                    }
                }
            }
        }
    }

    /**
     * Lip: water-level step, +1 peak (two blocks wide, inset from the outer
     * edge), then a solid hull down to {@code hullBottomY}.
     */
    private static void placeLipColumn(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockPos.MutableBlockPos pos,
            int x,
            int z,
            int d2,
            int innerRim,
            int radius,
            int floorTopY,
            int floorBottomY,
            int hullBottomY,
            int floorRim,
            int waterTopY,
            int peakY,
            BlockState piece
    ) {
        float r = (float) Math.sqrt(d2);
        int top = lipTopY(r, innerRim, radius, waterTopY, peakY);
        for (int y = hullBottomY; y <= top; y++) {
            if (y < waterTopY && r > outerRadiusAtY(y, floorRim, floorBottomY, floorTopY, radius)) {
                continue;
            }
            if (insideSmooth(d2, radius)) {
                set(level, pos.set(x, y, z), piece, chunkBB);
            }
        }
    }

    private static int lipTopY(float r, int innerRim, int radius, int waterTopY, int peakY) {
        float span = Math.max(1f, radius - innerRim);
        float t = Mth.clamp((r - innerRim) / span, 0f, 1f);
        int peakH = peakY - waterTopY;
        float peakT = 0.18f;
        float h;
        if (t <= peakT) {
            float u = t / peakT;
            h = peakH * u * u * (3f - 2f * u);
        } else {
            float u = (t - peakT) / (1f - peakT);
            h = peakH * (1f - u * u * (3f - 2f * u));
        }
        return waterTopY + Math.round(h);
    }

    /** Inner water/floor edge at {@code y} (smallest r whose floor is at least y). */
    private static float innerRadiusAtY(int y, int floorRim, int floorBottomY, int floorTopY) {
        int span = floorTopY - floorBottomY;
        if (span <= 0 || y <= floorBottomY) {
            return 0f;
        }
        float sm = (y - floorBottomY - 0.5f) / (float) span;
        if (sm >= 1f) {
            return floorRim;
        }
        if (sm <= 0f) {
            return 0f;
        }
        return inverseSmoothstep(sm) * floorRim;
    }

    /**
     * Outer silhouette of the main bowl: stem-width at the hull bottom, then a
     * delayed smoothstep flare to the visible rim (preview_giant_goblet_yz_expected).
     * Mini cups keep the old inner-dish-raised-by-2 hull.
     */
    private static float outerRadiusAtY(int y, int floorRim, int floorBottomY, int floorTopY, int bowlRadius) {
        float maxR = bowlRadius * Mth.sqrt(19f / 20f);
        if (bowlRadius <= 20) {
            if (y >= floorTopY) {
                return maxR - 1.01f;
            }
            return innerRadiusAtY(y + 2, floorRim, floorBottomY, floorTopY);
        }
        if (y >= floorTopY) {
            return maxR;
        }
        if (y == floorTopY - 1) {
            return maxR - 1.01f;
        }
        int hullBottomY = floorTopY - OUTER_HULL_STEPS;
        if (y < hullBottomY) {
            return 0f;
        }
        float t = (y - hullBottomY) / (float) OUTER_HULL_STEPS;
        float u = Mth.clamp((t - 1f / OUTER_HULL_STEPS) / (1f - 1f / OUTER_HULL_STEPS), 0f, 1f);
        float rise = u * u * (3f - 2f * u);
        float minR = PILLAR_RADIUS * Mth.sqrt(19f / 20f) - 0.5f;
        float pad = t <= 0.01f ? 0f : 0.75f;
        return minR + (maxR - minR) * rise + 1.25f * rise + pad;
    }

    private static float inverseSmoothstep(float s) {
        float t = s;
        for (int i = 0; i < 6; i++) {
            float f = t * t * (3f - 2f * t) - s;
            float df = 6f * t * (1f - t);
            if (Math.abs(df) < 1e-5f) {
                break;
            }
            t = Mth.clamp(t - f / df, 0f, 1f);
        }
        return t;
    }

    private static boolean hitsSpill(int dx, int dz, float[] angles, float[] half) {
        if (angles == null || angles.length == 0 || (dx == 0 && dz == 0)) {
            return false;
        }
        float angle = (float) Math.atan2(dz, dx);
        for (int i = 0; i < angles.length; i++) {
            if (angularDistance(angle, angles[i]) <= half[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideArm(float s, int y, float side, Arms arms, int attachY) {
        float radius = Mth.lerp(Mth.clamp(s / arms.reach, 0f, 1f), 2.8f, 2.0f);
        if (Math.abs(side) > radius + 0.35f) {
            return false;
        }

        float cx = 0f;
        float cy = attachY + arms.arcR;
        float vx = s - cx;
        float vy = y - cy;
        float ang = (float) Math.atan2(vx, -vy);

        float closestS;
        float closestY;
        if (ang < 0f) {
            closestS = 0f;
            closestY = attachY;
        } else if (ang > arms.arcAngle) {
            closestS = arms.reach;
            closestY = attachY + arms.rise;
        } else {
            float len = Mth.sqrt(vx * vx + vy * vy);
            if (len < 1e-3f) {
                return false;
            }
            float scale = arms.arcR / len;
            closestS = vx * scale;
            closestY = cy + vy * scale;
        }

        float ds = s - closestS;
        float dy = y - closestY;
        return ds * ds + dy * dy + side * side <= radius * radius;
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
     * Shallow inner dish: water is 4 deep at the center and pinches to a
     * one-block film under the rim (preview_giant_goblet_yz_expected).
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
        int span = floorTopY - floorBottomY;
        int waterBands;
        if (t < 0.25f) {
            waterBands = span;
        } else if (t < 0.60f) {
            waterBands = Math.max(span - 1, 0);
        } else if (t < 0.85f) {
            waterBands = Math.max(span - 2, 0);
        } else {
            waterBands = 0;
        }
        return floorTopY - waterBands;
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

    /**
     * Side cups sit {@code 1.5 × miniRadius} out from the stem on a shallow arc
     * (gentle lift, not a 90° Γ). Rim holes are independent of the arms.
     */
    private static final class Arms {
        final int count;
        final int miniRadius;
        final int reach;
        final int rise;
        final float arcR;
        final float arcAngle;
        final float[] cos;
        final float[] sin;
        final int[] attachY;
        final int[] miniRimTopY;
        final float[] spillAngles;
        final float[] spillHalfAngle;
        final float[][] miniSpillAngles;
        final float[][] miniSpillHalf;

        static Arms create(long seed, int bowlRadius, int baseY, int pillarTopY) {
            RandomSource random = RandomSource.create(seed);
            int miniRadius = miniBowlRadius(bowlRadius);
            int reach = Math.max(6, Math.round(MINI_DISTANCE_SCALE * miniRadius));
            int rise = Math.max(ARM_RISE_MIN, Math.round(reach * ARM_RISE_SCALE));
            float arcAngle = 2f * (float) Math.atan(rise / (float) reach);
            float arcR = reach / Mth.sin(arcAngle);

            int miniStack = MINI_FLOOR_STEPS + MINI_FLOOR_THICKNESS + RIM_PEAK_ABOVE_WATER;
            int highAttach = Math.max(baseY + 8, pillarTopY - rise - miniStack - FALL_GAP);
            int lowAttach = baseY + 8 + Math.max(0, (highAttach - (baseY + 8)) / 4);

            int count = bowlRadius < 28 ? 2 : 2 + random.nextInt(3);
            float start = random.nextFloat() * Mth.TWO_PI;
            float[] cos = new float[count];
            float[] sin = new float[count];
            int[] attachY = staggeredAttachYs(random, count, lowAttach, highAttach);
            int[] miniRimTopY = new int[count];
            float[][] miniSpillAngles = new float[count][];
            float[][] miniSpillHalf = new float[count][];
            int miniHoleCount = Math.max(2, miniRadius / 6);
            for (int i = 0; i < count; i++) {
                float angle = start + i * (Mth.TWO_PI / count) + (random.nextFloat() - 0.5f) * 0.2f;
                cos[i] = Mth.cos(angle);
                sin[i] = Mth.sin(angle);
                miniRimTopY[i] = attachY[i] + rise + miniStack;
                miniSpillAngles[i] = new float[miniHoleCount];
                miniSpillHalf[i] = new float[miniHoleCount];
                float miniStart = random.nextFloat() * Mth.TWO_PI;
                float miniSlot = Mth.TWO_PI / miniHoleCount;
                for (int h = 0; h < miniHoleCount; h++) {
                    miniSpillAngles[i][h] = miniStart + h * miniSlot + (random.nextFloat() - 0.5f) * miniSlot * 0.7f;
                    float holeWidth = 1.5f + random.nextFloat();
                    miniSpillHalf[i][h] = holeWidth * 0.5f / Math.max(miniRadius, 1);
                }
            }

            int spillCount = Math.max(4, bowlRadius / 5);
            float[] spillAngles = new float[spillCount];
            float[] spillHalfAngle = new float[spillCount];
            float spillStart = random.nextFloat() * Mth.TWO_PI;
            float slot = Mth.TWO_PI / spillCount;
            for (int i = 0; i < spillCount; i++) {
                spillAngles[i] = spillStart + i * slot + (random.nextFloat() - 0.5f) * slot * 0.7f;
                float holeWidth = 2f + random.nextFloat();
                spillHalfAngle[i] = holeWidth * 0.5f / Math.max(bowlRadius, 1);
            }
            return new Arms(
                    count, miniRadius, reach, rise, arcR, arcAngle, cos, sin, attachY, miniRimTopY,
                    spillAngles, spillHalfAngle, miniSpillAngles, miniSpillHalf
            );
        }

        private Arms(
                int count,
                int miniRadius,
                int reach,
                int rise,
                float arcR,
                float arcAngle,
                float[] cos,
                float[] sin,
                int[] attachY,
                int[] miniRimTopY,
                float[] spillAngles,
                float[] spillHalfAngle,
                float[][] miniSpillAngles,
                float[][] miniSpillHalf
        ) {
            this.count = count;
            this.miniRadius = miniRadius;
            this.reach = reach;
            this.rise = rise;
            this.arcR = arcR;
            this.arcAngle = arcAngle;
            this.cos = cos;
            this.sin = sin;
            this.attachY = attachY;
            this.miniRimTopY = miniRimTopY;
            this.spillAngles = spillAngles;
            this.spillHalfAngle = spillHalfAngle;
            this.miniSpillAngles = miniSpillAngles;
            this.miniSpillHalf = miniSpillHalf;
        }

        int extent() {
            return PILLAR_RADIUS + reach + miniRadius;
        }

        int minAttachY() {
            int min = this.attachY[0];
            for (int y : this.attachY) {
                min = Math.min(min, y);
            }
            return min;
        }

        int maxArmTop() {
            int max = this.attachY[0];
            for (int y : this.attachY) {
                max = Math.max(max, y);
            }
            return max + this.rise;
        }

        boolean isSpillway(int dx, int dz) {
            return hitsSpill(dx, dz, this.spillAngles, this.spillHalfAngle);
        }
    }

    private static int[] staggeredAttachYs(RandomSource random, int count, int low, int high) {
        int[] ys = new int[count];
        if (count <= 1 || high <= low) {
            ys[0] = high;
            return ys;
        }
        int span = high - low;
        int jitter = Math.max(1, span / (count * 4));
        for (int i = 0; i < count; i++) {
            float t = i / (float) (count - 1);
            ys[i] = Mth.clamp(low + Math.round(t * span) + random.nextInt(jitter * 2 + 1) - jitter, low, high);
        }
        for (int i = count - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = ys[i];
            ys[i] = ys[j];
            ys[j] = tmp;
        }
        return ys;
    }

    private static float angularDistance(float a, float b) {
        float d = Math.abs(a - b) % Mth.TWO_PI;
        if (d > Mth.PI) {
            d = Mth.TWO_PI - d;
        }
        return d;
    }
}
