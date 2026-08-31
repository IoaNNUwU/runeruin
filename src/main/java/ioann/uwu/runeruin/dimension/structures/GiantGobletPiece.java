package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final int MINI_RIM_THICKNESS = 3;
    /** Neck radius of a side cup — matches the arm tip so the hull meets the arm. */
    private static final float MINI_NECK_RADIUS = 2.0f;
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
    /** Packed (dx, dz) vein cells; filled on first chunk so later chunks reuse them. */
    private int[] mainVeins;
    private int[][] miniVeins;

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
        BlockState stem = RRBlocks.GIANT_GOBLET_STEM.get().defaultBlockState();
        BlockState bud = RRBlocks.GIANT_GOBLET_BUD.get().defaultBlockState();
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

        float hullMinR = mainHullMinR();
        placeMainGoblet(
                level, chunkBB, stem, bud, water,
                baseY, rimTopY, waterTopY, floorTopY, floorBottomY, hullBottomY, pillarTopY,
                innerRim, floorRim, spillInner, hullMinR, arms
        );
        placeVeins(
                level, chunkBB, stem, this.centerX, this.centerZ, mainVeins(),
                this.bowlRadius, floorTopY, hullMinR, OUTER_HULL_STEPS
        );
        placeArms(level, chunkBB, stem, arms);
        placeMiniBowls(level, chunkBB, stem, bud, water, arms);
    }

    private void placeMainGoblet(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockState stem,
            BlockState bud,
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
            float hullMinR,
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
                        set(level, pos.set(x, floorTopY, z), bud, chunkBB);
                    } else {
                        placeLipColumn(
                                level, chunkBB, pos, x, z, d2,
                                innerRim, this.bowlRadius, floorTopY, hullBottomY,
                                waterTopY, rimTopY, hullMinR, OUTER_HULL_STEPS, bud
                        );
                    }
                } else {
                    float r = (float) Math.sqrt(d2);
                    int floorY = floorYForDist(d2, floorRim, floorBottomY, floorTopY);
                    for (int y = hullBottomY; y <= floorY; y++) {
                        if (r <= outerRadiusAtY(y, floorTopY, this.bowlRadius, hullMinR, OUTER_HULL_STEPS)) {
                            set(level, pos.set(x, y, z), bud, chunkBB);
                        }
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

    private int[] mainVeins() {
        if (this.mainVeins == null) {
            float radius = this.bowlRadius * Mth.sqrt(19f / 20f) - 0.35f;
            this.mainVeins = WigglyDendrites.grow(this.seed, Math.max(6f, radius));
        }
        return this.mainVeins;
    }

    private int[] miniVeins(int index, int miniRadius) {
        if (this.miniVeins == null) {
            this.miniVeins = new int[8][];
        }
        if (this.miniVeins[index] == null) {
            float radius = miniRadius * Mth.sqrt(19f / 20f) - 0.35f;
            this.miniVeins[index] = WigglyDendrites.grow(this.seed + 17L * (index + 1), Math.max(5f, radius));
        }
        return this.miniVeins[index];
    }

    /**
     * Stamp sc_v4_wiggly cells onto the underside of a cup. One block per occupied
     * pixel, hanging one below the hull so the 1–2 px veins stay readable.
     */
    private void placeVeins(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockState stem,
            int cx,
            int cz,
            int[] packed,
            int bowlRadius,
            int floorTopY,
            float hullMinR,
            int hullSteps
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int p : packed) {
            int x = cx + WigglyDendrites.unpackDx(p);
            int z = cz + WigglyDendrites.unpackDz(p);
            if (x < chunkBB.minX() || x > chunkBB.maxX() || z < chunkBB.minZ() || z > chunkBB.maxZ()) {
                continue;
            }
            int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (!insideSmooth(d2, bowlRadius)) {
                continue;
            }
            float r = Mth.sqrt(d2);
            int y = cupUndersideY(r, floorTopY, bowlRadius, hullMinR, hullSteps) - 1;
            setWeb(level, pos.set(x, y, z), stem, chunkBB);
        }
    }

    private static int cupUndersideY(float r, int floorTopY, int bowlRadius, float minR, int hullSteps) {
        int hullBottomY = floorTopY - hullSteps;
        for (int y = hullBottomY; y <= floorTopY; y++) {
            if (r <= outerRadiusAtY(y, floorTopY, bowlRadius, minR, hullSteps)) {
                return y;
            }
        }
        return floorTopY;
    }

    private static void setWeb(WorldGenLevel level, BlockPos pos, BlockState stem, BoundingBox chunkBB) {
        if (!chunkBB.isInside(pos)) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.is(stem.getBlock())) {
            level.setBlock(pos, stem, 2);
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

    private void placeMiniBowls(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockState stem,
            BlockState bud,
            BlockState water,
            Arms arms
    ) {
        int hullSteps = miniHullSteps(arms.miniRadius);
        float hullMinR = miniHullMinR();
        for (int i = 0; i < arms.count; i++) {
            int cupX = this.centerX + Math.round(arms.cos[i] * (PILLAR_RADIUS + arms.reach));
            int cupZ = this.centerZ + Math.round(arms.sin[i] * (PILLAR_RADIUS + arms.reach));
            int rimTopY = arms.miniRimTopY[i];
            int floorTopY = rimTopY - RIM_PEAK_ABOVE_WATER - 1;
            placeBowl(
                    level, chunkBB, cupX, cupZ, rimTopY, arms.miniRadius,
                    MINI_FLOOR_STEPS, MINI_RIM_THICKNESS, hullMinR, hullSteps,
                    bud, water,
                    arms.miniSpillAngles[i], arms.miniSpillHalf[i]
            );
            placeVeins(
                    level, chunkBB, stem, cupX, cupZ, miniVeins(i, arms.miniRadius),
                    arms.miniRadius, floorTopY, hullMinR, hullSteps
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
            int rimThickness,
            float hullMinR,
            int hullSteps,
            BlockState piece,
            BlockState water,
            float[] spillAngles,
            float[] spillHalf
    ) {
        int waterTopY = rimTopY - RIM_PEAK_ABOVE_WATER;
        int floorTopY = rimTopY - RIM_PEAK_ABOVE_WATER - 1;
        int floorBottomY = floorTopY - floorSteps;
        int hullBottomY = floorTopY - hullSteps;
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
                                innerRim, radius, floorTopY, hullBottomY,
                                waterTopY, rimTopY, hullMinR, hullSteps, piece
                        );
                    }
                } else {
                    float r = (float) Math.sqrt(d2);
                    int floorY = floorYForDist(d2, floorRim, floorBottomY, floorTopY);
                    for (int y = hullBottomY; y <= floorY; y++) {
                        if (r <= outerRadiusAtY(y, floorTopY, radius, hullMinR, hullSteps)) {
                            set(level, pos.set(x, y, z), piece, chunkBB);
                        }
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
            int hullBottomY,
            int waterTopY,
            int peakY,
            float hullMinR,
            int hullSteps,
            BlockState piece
    ) {
        float r = (float) Math.sqrt(d2);
        int top = lipTopY(r, innerRim, radius, waterTopY, peakY);
        for (int y = hullBottomY; y <= top; y++) {
            if (y < waterTopY && r > outerRadiusAtY(y, floorTopY, radius, hullMinR, hullSteps)) {
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

    /**
     * Outer silhouette: neck-width at the hull bottom, then a delayed smoothstep
     * flare to the visible rim (preview_giant_goblet_yz_expected). Used by both
     * the main cup and the side cups.
     */
    private static float outerRadiusAtY(int y, int floorTopY, int bowlRadius, float minR, int hullSteps) {
        float maxR = bowlRadius * Mth.sqrt(19f / 20f);
        if (y >= floorTopY) {
            return maxR;
        }
        if (y == floorTopY - 1) {
            return maxR - 1.01f;
        }
        int hullBottomY = floorTopY - hullSteps;
        if (y < hullBottomY) {
            return 0f;
        }
        float t = (y - hullBottomY) / (float) hullSteps;
        float u = Mth.clamp((t - 1f / hullSteps) / (1f - 1f / hullSteps), 0f, 1f);
        float rise = u * u * (3f - 2f * u);
        float pad = t <= 0.01f ? 0f : 0.75f * Math.min(1f, (maxR - minR) / 28f);
        float extra = 1.25f * rise * Math.min(1f, (maxR - minR) / 28f);
        return minR + (maxR - minR) * rise + extra + pad;
    }

    private static float mainHullMinR() {
        return PILLAR_RADIUS * Mth.sqrt(19f / 20f) - 0.5f;
    }

    private static float miniHullMinR() {
        return MINI_NECK_RADIUS * Mth.sqrt(19f / 20f) - 0.25f;
    }

    private static int miniHullSteps(int miniRadius) {
        return Math.max(5, Math.round(OUTER_HULL_STEPS * miniRadius / 24f));
    }

    private static int miniStackHeight(int miniRadius) {
        return miniHullSteps(miniRadius) + RIM_PEAK_ABOVE_WATER + 1;
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

            int miniStack = miniStackHeight(miniRadius);
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

    /**
     * Runions space colonization matching {@code scripts/dla_ascii.py} {@code sc_v4_wiggly}
     * / {@code wiggly_kwargs}. Occupied cells are packed (dx, dz) from the cluster center.
     */
    private static final class WigglyDendrites {
        static final float STEP = 1.0f;
        static final float JITTER = 0.70f;
        static final float KILL = 1.42f;
        static final float INFLUENCE = 5.0f;
        static final float REF_RADIUS = 20.0f;
        static final float REF_ATTRACTORS = 680f;
        static final float REF_TARGET = 387f;
        static final float CENTER_MIX = 0.42f;
        static final float CORE_BOOST = 1.55f;
        static final float RIM_BOOST = 0.22f;
        static final float NEIGHBOR_POW = 2.0f;
        static final int[][] N4 = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        static int unpackDx(int packed) {
            return packed >> 16;
        }

        static int unpackDz(int packed) {
            return (short) packed;
        }

        static int pack(int dx, int dz) {
            return (dx << 16) | (dz & 0xffff);
        }

        static int[] grow(long seed, float radius) {
            RandomSource rng = RandomSource.create(seed);
            float s = Math.max(0.25f, radius / REF_RADIUS);
            int nAttractors = Math.max(24, Math.round(REF_ATTRACTORS * s * s));
            int target = Math.max(16, Math.round(REF_TARGET * s * s));
            int size = Math.max(8, 2 * Mth.ceil(radius + 3f));
            float cx = size / 2.0f;
            float cz = size / 2.0f;

            float[] ax = new float[nAttractors];
            float[] az = new float[nAttractors];
            for (int i = 0; i < nAttractors; i++) {
                float ang = rng.nextFloat() * Mth.TWO_PI;
                float r;
                if (rng.nextFloat() < CENTER_MIX) {
                    r = radius * (float) Math.pow(rng.nextFloat(), 1.2);
                } else {
                    r = radius * Mth.sqrt(rng.nextFloat());
                }
                ax[i] = cx + r * Mth.cos(ang);
                az[i] = cz + r * Mth.sin(ang);
            }
            int nAtt = nAttractors;

            int nodeCap = Math.max(64, target * 3);
            float[] nx = new float[nodeCap];
            float[] nz = new float[nodeCap];
            int nNodes = 1;
            nx[0] = cx;
            nz[0] = cz;

            boolean[][] occ = new boolean[size][size];
            int nOcc = raster(occ, size, cx, cz, cx, cz);

            float influence = Math.max(2.0f, INFLUENCE * (radius / 40.0f));
            float kill = Math.max(1.0f, KILL * (radius / 40.0f));
            float inf = influence;
            NodeIndex index = new NodeIndex(Math.max(inf, 2.0f));
            index.add(0, cx, cz);
            nAtt = killNear(ax, az, nAtt, nx, nz, 0, nNodes, kill);

            int maxIters = s <= 1.0f ? 1200 : 2500;
            for (int iter = 0; iter < maxIters; iter++) {
                if (nAtt == 0 || nOcc >= target) {
                    break;
                }
                int[] assignedCount = new int[nNodes];
                float[] vx = new float[nNodes];
                float[] vz = new float[nNodes];
                for (int a = 0; a < nAtt; a++) {
                    int best = -1;
                    float bestD = inf;
                    for (int n : index.query(ax[a], az[a], inf)) {
                        float d = Mth.sqrt((ax[a] - nx[n]) * (ax[a] - nx[n]) + (az[a] - nz[n]) * (az[a] - nz[n]));
                        if (d < bestD) {
                            bestD = d;
                            best = n;
                        }
                    }
                    if (best >= 0) {
                        vx[best] += ax[a] - nx[best];
                        vz[best] += az[a] - nz[best];
                        assignedCount[best]++;
                    }
                }
                int oldNodes = nNodes;
                boolean grew = false;
                boolean hitTarget = false;
                for (int n = 0; n < oldNodes; n++) {
                    if (assignedCount[n] == 0) {
                        continue;
                    }
                    float avx = vx[n] / assignedCount[n];
                    float avz = vz[n] / assignedCount[n];
                    float ang = (float) Math.atan2(avz, avx) + (rng.nextFloat() * 2f - 1f) * JITTER;
                    if (nNodes >= nodeCap) {
                        break;
                    }
                    float nnx = nx[n] + STEP * Mth.cos(ang);
                    float nnz = nz[n] + STEP * Mth.sin(ang);
                    nOcc += raster(occ, size, nx[n], nz[n], nnx, nnz);
                    nx[nNodes] = nnx;
                    nz[nNodes] = nnz;
                    nNodes++;
                    grew = true;
                    if (nOcc >= target) {
                        hitTarget = true;
                        break;
                    }
                }
                if (!grew) {
                    inf = Math.max(influence * 0.55f, inf * 0.90f);
                    if (inf <= influence * 0.56f) {
                        break;
                    }
                    continue;
                }
                for (int n = oldNodes; n < nNodes; n++) {
                    index.add(n, nx[n], nz[n]);
                }
                nAtt = killNear(ax, az, nAtt, nx, nz, oldNodes, nNodes, kill);
                if (hitTarget) {
                    break;
                }
            }

            fattenTo(occ, size, target, cx, cz, rng, radius + 1.2f, nOcc);
            trimTo(occ, size, target, rng);

            ArrayList<Integer> cells = new ArrayList<>();
            int icx = Math.round(cx);
            int icz = Math.round(cz);
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (occ[x][z]) {
                        cells.add(pack(x - icx, z - icz));
                    }
                }
            }
            int[] packed = new int[cells.size()];
            for (int i = 0; i < packed.length; i++) {
                packed[i] = cells.get(i);
            }
            return packed;
        }

        private static int countOcc(boolean[][] occ, int size) {
            int n = 0;
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (occ[x][z]) {
                        n++;
                    }
                }
            }
            return n;
        }

        private static int raster(boolean[][] occ, int size, float x0, float z0, float x1, float z1) {
            int ax = Math.round(x0);
            int az = Math.round(z0);
            int bx = Math.round(x1);
            int bz = Math.round(z1);
            int dx = Math.abs(bx - ax);
            int dz = -Math.abs(bz - az);
            int sx = ax < bx ? 1 : -1;
            int sz = az < bz ? 1 : -1;
            int err = dx + dz;
            int x = ax;
            int z = az;
            int added = 0;
            while (true) {
                if (x >= 0 && x < size && z >= 0 && z < size && !occ[x][z]) {
                    occ[x][z] = true;
                    added++;
                }
                if (x == bx && z == bz) {
                    break;
                }
                int e2 = 2 * err;
                if (e2 >= dz) {
                    err += dz;
                    x += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    z += sz;
                }
            }
            return added;
        }

        private static int killNear(
                float[] ax, float[] az, int nAtt,
                float[] nx, float[] nz, int from, int to,
                float kill
        ) {
            float r2 = kill * kill;
            int w = 0;
            for (int a = 0; a < nAtt; a++) {
                boolean dead = false;
                for (int n = from; n < to; n++) {
                    float dx = ax[a] - nx[n];
                    float dz = az[a] - nz[n];
                    if (dx * dx + dz * dz <= r2) {
                        dead = true;
                        break;
                    }
                }
                if (!dead) {
                    ax[w] = ax[a];
                    az[w] = az[a];
                    w++;
                }
            }
            return w;
        }

        private static void fattenTo(
                boolean[][] occ,
                int size,
                int target,
                float cx,
                float cz,
                RandomSource rng,
                float maxR,
                int nOcc
        ) {
            HashSet<Integer> peri = new HashSet<>();
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (!occ[x][z]) {
                        continue;
                    }
                    for (int[] d : N4) {
                        int nx = x + d[0];
                        int nz = z + d[1];
                        if (nx >= 0 && nx < size && nz >= 0 && nz < size && !occ[nx][nz]) {
                            peri.add(nx * size + nz);
                        }
                    }
                }
            }
            ArrayList<Integer> cells = new ArrayList<>();
            ArrayList<Float> weights = new ArrayList<>();
            while (nOcc < target && !peri.isEmpty()) {
                cells.clear();
                weights.clear();
                float total = 0f;
                for (int packed : peri) {
                    int x = packed / size;
                    int z = packed % size;
                    int nOccN = 0;
                    for (int[] d : N4) {
                        int nx = x + d[0];
                        int nz = z + d[1];
                        if (nx >= 0 && nx < size && nz >= 0 && nz < size && occ[nx][nz]) {
                            nOccN++;
                        }
                    }
                    float r = Mth.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                    float w = 0f;
                    if (r <= maxR) {
                        float t = Math.min(1f, r / Math.max(maxR, 1f));
                        float radial = CORE_BOOST * (1f - t) + RIM_BOOST * t;
                        w = (0.28f + (float) Math.pow(nOccN, NEIGHBOR_POW)) * radial;
                    }
                    cells.add(packed);
                    weights.add(w);
                    total += w;
                }
                if (total <= 0f) {
                    break;
                }
                float pick = rng.nextFloat() * total;
                float acc = 0f;
                int chosen = cells.get(0);
                for (int i = 0; i < cells.size(); i++) {
                    acc += weights.get(i);
                    if (pick <= acc) {
                        chosen = cells.get(i);
                        break;
                    }
                }
                peri.remove(chosen);
                int x = chosen / size;
                int z = chosen % size;
                occ[x][z] = true;
                nOcc++;
                for (int[] d : N4) {
                    int nx = x + d[0];
                    int nz = z + d[1];
                    if (nx >= 0 && nx < size && nz >= 0 && nz < size && !occ[nx][nz]) {
                        peri.add(nx * size + nz);
                    }
                }
            }
        }

        private static void trimTo(boolean[][] occ, int size, int target, RandomSource rng) {
            int nOcc = countOcc(occ, size);
            ArrayList<Integer> tips = new ArrayList<>();
            while (nOcc > target) {
                tips.clear();
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        if (!occ[x][z]) {
                            continue;
                        }
                        int n = 0;
                        for (int[] d : N4) {
                            int nx = x + d[0];
                            int nz = z + d[1];
                            if (nx >= 0 && nx < size && nz >= 0 && nz < size && occ[nx][nz]) {
                                n++;
                            }
                        }
                        if (n <= 1) {
                            tips.add(x * size + z);
                        }
                    }
                }
                if (tips.isEmpty()) {
                    for (int x = 0; x < size; x++) {
                        for (int z = 0; z < size; z++) {
                            if (occ[x][z]) {
                                tips.add(x * size + z);
                            }
                        }
                    }
                }
                if (tips.isEmpty()) {
                    break;
                }
                int chosen = tips.get(rng.nextInt(tips.size()));
                occ[chosen / size][chosen % size] = false;
                nOcc--;
            }
        }

        private static final class NodeIndex {
            final float cell;
            final Map<Long, List<Integer>> buckets = new HashMap<>();
            final ArrayList<Integer> scratch = new ArrayList<>(64);

            NodeIndex(float cell) {
                this.cell = Math.max(cell, 1f);
            }

            private long key(float x, float z) {
                int gx = Mth.floor(x / this.cell);
                int gz = Mth.floor(z / this.cell);
                return ((long) gx << 32) | (gz & 0xffffffffL);
            }

            void add(int i, float x, float z) {
                this.buckets.computeIfAbsent(key(x, z), k -> new ArrayList<>()).add(i);
            }

            List<Integer> query(float x, float z, float radius) {
                int gx = Mth.floor(x / this.cell);
                int gz = Mth.floor(z / this.cell);
                int r = (int) (radius / this.cell) + 1;
                this.scratch.clear();
                for (int ix = gx - r; ix <= gx + r; ix++) {
                    for (int iz = gz - r; iz <= gz + r; iz++) {
                        List<Integer> bucket = this.buckets.get(((long) ix << 32) | (iz & 0xffffffffL));
                        if (bucket != null) {
                            this.scratch.addAll(bucket);
                        }
                    }
                }
                return this.scratch;
            }
        }
    }
}
