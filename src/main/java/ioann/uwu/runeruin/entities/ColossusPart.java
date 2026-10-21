package ioann.uwu.runeruin.entities;

import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * One cube of the Colossus, turned and moved with its bone. Its box, which the game keeps square to the blocks, is
 * where a hit lands; what stands on the cube collides with the cube itself (ColossusCollisionMixin), so players can
 * climb the colossus and stand on its shoulders. A hit on it hurts the colossus.
 */
public class ColossusPart extends PartEntity<Colossus> {
    // A block of the world, in pixels of the model.
    private static final float BLOCK = 16.0F / Colossus.SCALE;

    public final int bone;
    // The cube in the space of its bone, in pixels: min x, y, z, max x, y, z. Up is towards the smaller y.
    private final float[] cube;
    // From the space of the bone to the world relative to the origin, and back.
    private final Matrix4f toWorld = new Matrix4f();
    private final Matrix4f toCube = new Matrix4f();
    private Vec3 origin = Vec3.ZERO;

    public ColossusPart(Colossus parent, int bone, float[] cube) {
        super(parent);
        this.bone = bone;
        this.cube = cube;
        this.noPhysics = true;
    }

    /** Puts the cube where its bone is: {@code bone} takes the bone's space in sixteenths of pixels to the world. */
    void place(Matrix4f bone, Vec3 origin, AABB box) {
        this.toWorld.set(bone).scale(1.0F / 16.0F);
        this.toWorld.invert(this.toCube);
        this.origin = origin;
        this.setPos(box.getCenter().x, box.minY, box.getCenter().z);
        this.setBoundingBox(box);
    }

    /** A point of the world in the space of the cube, in pixels. */
    Vector3f toCube(Vec3 point) {
        return this.toCube.transformPosition((float) (point.x - this.origin.x), (float) (point.y - this.origin.y), (float) (point.z - this.origin.z), new Vector3f());
    }

    /** A point of the space of the cube in the world. */
    Vec3 toWorld(Vector3f point) {
        Vector3f world = this.toWorld.transformPosition(point, new Vector3f());
        return this.origin.add(world.x, world.y, world.z);
    }

    /** Whether something stands on the top of the cube with its feet: over the top face, less than half a block from it. */
    boolean carries(Vec3 feet) {
        Vector3f point = this.toCube(feet);
        float margin = 0.3F * BLOCK;
        return point.x > this.cube[0] - margin && point.x < this.cube[3] + margin
                && point.z > this.cube[2] - margin && point.z < this.cube[5] + margin
                && Math.abs(point.y - this.cube[1]) < 0.5F * BLOCK;
    }

    /**
     * Whether the cube stops another entity. Not a player on the server: a player stands on the cubes as the client sees
     * them, which run a tick apart from the server's, and the server would take every step against its own cubes for a
     * collision and pull the player back. A moving happy ghast leaves its players to their clients in the same way.
     */
    public boolean blocks(Entity other) {
        return !this.is(other) && (this.level().isClientSide() || !(other instanceof Player));
    }

    /**
     * Adds the cube where it meets an area, as boxes of a block of the world turned with it: the game collides with
     * boxes square to the blocks only, and one box around a turned cube would stand out of it like a wall of air.
     */
    public void addCollisions(AABB area, List<VoxelShape> shapes) {
        float[] local = bounds(this.toCube, area.move(this.origin.scale(-1.0)));
        int[] from = new int[3];
        int[] to = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            from[axis] = Math.max(0, (int) Math.floor((local[axis] - this.cube[axis]) / BLOCK));
            to[axis] = Math.min((int) Math.ceil((this.cube[axis + 3] - this.cube[axis]) / BLOCK), (int) Math.ceil((local[axis + 3] - this.cube[axis]) / BLOCK));
        }
        for (int x = from[0]; x < to[0]; x++) {
            for (int y = from[1]; y < to[1]; y++) {
                for (int z = from[2]; z < to[2]; z++) {
                    float[] cell = bounds(this.toWorld, new AABB(
                            this.cube[0] + x * BLOCK, this.cube[1] + y * BLOCK, this.cube[2] + z * BLOCK,
                            Math.min(this.cube[3], this.cube[0] + (x + 1) * BLOCK),
                            Math.min(this.cube[4], this.cube[1] + (y + 1) * BLOCK),
                            Math.min(this.cube[5], this.cube[2] + (z + 1) * BLOCK)));
                    shapes.add(Shapes.create(new AABB(cell[0], cell[1], cell[2], cell[3], cell[4], cell[5]).move(this.origin)));
                }
            }
        }
    }

    /** The eight corners of the cube in the world, for drawing it: bit 1 of an index is x, bit 2 y, bit 4 z. */
    public Vec3[] corners() {
        Vec3[] corners = new Vec3[8];
        for (int c = 0; c < 8; c++) {
            corners[c] = this.toWorld(new Vector3f(this.cube[(c & 1) == 0 ? 0 : 3], this.cube[(c & 2) == 0 ? 1 : 4], this.cube[(c & 4) == 0 ? 2 : 5]));
        }
        return corners;
    }

    /** The bounds of a box taken through a matrix: min x, y, z, max x, y, z. */
    private static float[] bounds(Matrix4f matrix, AABB box) {
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Vector3f point = new Vector3f();
        for (int c = 0; c < 8; c++) {
            matrix.transformPosition((float) ((c & 1) == 0 ? box.minX : box.maxX), (float) ((c & 2) == 0 ? box.minY : box.maxY),
                    (float) ((c & 4) == 0 ? box.minZ : box.maxZ), point);
            for (int axis = 0; axis < 3; axis++) {
                bounds[axis] = Math.min(bounds[axis], point.get(axis));
                bounds[axis + 3] = Math.max(bounds[axis + 3], point.get(axis));
            }
        }
        return bounds;
    }

    // The game collides with the cube through addCollisions, not with this box.
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return !this.isInvulnerableToBase(source) && this.getParent().hurtServer(level, source, damage);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || this.getParent() == other;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.0F, 1.0F);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
