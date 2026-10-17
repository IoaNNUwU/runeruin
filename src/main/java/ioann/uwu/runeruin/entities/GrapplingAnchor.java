package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.items.RRItems;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * The anchor of the grappling hook on its chain. It flies like an arrow and hooks the first block or creature it hits:
 * a block pulls the owner onto its top, or up to the anchor where there is no room on top; a creature is pulled to the
 * owner. Then the anchor comes back.
 *
 * <p>A pull moves the pulled entity on the side that moves it anyway: the client of a player, the server for a mob.
 * The server ends the pull by removing the anchor.
 */
public class GrapplingAnchor extends Projectile {

    /** Blocks of chain: an anchor that flies further comes back. */
    public static final double CHAIN_LENGTH = 32.0;
    private static final double GRAVITY = 0.05;
    /** Blocks per tick. */
    private static final double PULL_SPEED = 1.2;
    /** Where a pulled creature stops in front of the owner. */
    private static final double CREATURE_STOP = 2.5;
    private static final double OWNER_STOP = 0.75;
    private static final int MAX_PULL_TICKS = 60;
    /** Ticks without getting closer before a pull gives up: something is in the way. */
    private static final int MAX_STUCK_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY =
            SynchedEntityData.defineId(GrapplingAnchor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_HOOKED_BLOCK =
            SynchedEntityData.defineId(GrapplingAnchor.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    // The anchor each owner has out, on both sides: meanwhile the hook fires none and shows none at its muzzle.
    private static final Map<Entity, GrapplingAnchor> BY_OWNER = Collections.synchronizedMap(new WeakHashMap<>());

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    private @Nullable Entity hookedEntity;
    private int pullTicks;
    private int stuckTicks;
    private double closestDistance = Double.MAX_VALUE;

    public GrapplingAnchor(EntityType<? extends GrapplingAnchor> type, Level level) {
        super(type, level);
    }

    public GrapplingAnchor(Level level, LivingEntity owner) {
        this(RREntityTypes.GRAPPLING_ANCHOR.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    public static @Nullable GrapplingAnchor of(Entity owner) {
        GrapplingAnchor anchor = BY_OWNER.get(owner);
        return anchor == null || anchor.isRemoved() ? null : anchor;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_HOOKED_ENTITY, 0);
        entityData.define(DATA_HOOKED_BLOCK, Optional.empty());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_HOOKED_ENTITY.equals(accessor)) {
            int id = getEntityData().get(DATA_HOOKED_ENTITY);
            hookedEntity = id > 0 ? level().getEntity(id - 1) : null;
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        interpolation.interpolate();
        super.tick();
        Entity owner = getOwner();
        if (owner == null || !level().isClientSide() && shouldReturn(owner)) {
            discard();
            return;
        }

        Optional<BlockPos> block = getEntityData().get(DATA_HOOKED_BLOCK);
        if (hookedEntity != null) {
            setPos(hookedEntity.getX(), hookedEntity.getY(0.8), hookedEntity.getZ());
            pull(hookedEntity, owner.position(), CREATURE_STOP);
        } else if (block.isPresent()) {
            pull(owner, pullTarget(block.get(), owner), OWNER_STOP);
        } else {
            fly();
        }
    }

    /** On the server: whether the owner let go of the hook, went too far or the hooked thing is gone. */
    private boolean shouldReturn(Entity owner) {
        if (!owner.isAlive() || owner.level() != level() || !(owner instanceof LivingEntity living)
                || !living.getMainHandItem().is(RRItems.GRAPPLING_HOOK) && !living.getOffhandItem().is(RRItems.GRAPPLING_HOOK)) {
            return true;
        }
        Optional<BlockPos> block = getEntityData().get(DATA_HOOKED_BLOCK);
        if (hookedEntity != null && !hookedEntity.isAlive() || block.isPresent() && level().getBlockState(block.get()).isAir()) {
            return true;
        }
        // A pulled owner can fall short of the anchor: hooked, the chain gives way only well beyond its length.
        double reach = hookedEntity != null || block.isPresent() ? CHAIN_LENGTH * 1.5 : CHAIN_LENGTH;
        return distanceToSqr(owner) > reach * reach;
    }

    private void fly() {
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            // The client stops here too and waits for the server to hook it.
            setPos(hit.getLocation());
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide() && !EventHooks.onProjectileImpact(this, hit)) {
                onHit(hit);
            }
            return;
        }
        setPos(position().add(getDeltaMovement()));
        updateRotation();
        setDeltaMovement(getDeltaMovement().scale(0.99).subtract(0.0, GRAVITY, 0.0));
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // An anchor shot upwards falls back past its owner.
        return super.canHitEntity(entity) && !ownedBy(entity);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        getEntityData().set(DATA_HOOKED_BLOCK, Optional.of(hit.getBlockPos()));
        playSound(SoundEvents.CHAIN_PLACE, 1.0F, 0.8F);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        hookedEntity = hit.getEntity();
        getEntityData().set(DATA_HOOKED_ENTITY, hookedEntity.getId() + 1);
        playSound(SoundEvents.CHAIN_HIT, 1.0F, 0.8F);
    }

    /**
     * Where the feet of the owner go: just above the top of the hooked block if they can stand there, else under the
     * anchor. An anchor under a block hangs from it.
     */
    private Vec3 pullTarget(BlockPos block, Entity owner) {
        VoxelShape shape = level().getBlockState(block).getCollisionShape(level(), block);
        Vec3 top = Vec3.atBottomCenterOf(block).add(0.0, (shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y)) + 0.1, 0.0);
        boolean underneath = getY() < block.getY() + 1.0E-3;
        if (!underneath && level().noCollision(owner, owner.getDimensions(Pose.STANDING).makeBoundingBox(top))) {
            return top;
        }
        return position().subtract(0.0, owner.getBbHeight() * 0.8, 0.0);
    }

    /** Moves the pulled entity to the target; the server ends the pull once it is there or gets no closer. */
    private void pull(Entity pulled, Vec3 target, double stop) {
        pulled.resetFallDistance();
        Vec3 offset = target.subtract(pulled.position());
        double distance = offset.length();
        if (pulled.isLocalInstanceAuthoritative()) {
            Vec3 motion = offset.scale(Math.min(PULL_SPEED, distance * 0.5) / Math.max(distance, 1.0E-4));
            // Pressed against a wall below the target, the pull climbs it: this lifts the owner over the edge.
            if (pulled.horizontalCollision && offset.y > -0.5) {
                motion = new Vec3(motion.x, Math.max(motion.y, 0.5), motion.z);
            }
            pulled.setDeltaMovement(motion);
        }
        if (level().isClientSide()) {
            return;
        }
        if (distance < closestDistance - 0.1) {
            closestDistance = distance;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        if (distance < stop || stuckTicks > MAX_STUCK_TICKS || ++pullTicks > MAX_PULL_TICKS) {
            discard();
        }
    }

    public @Nullable Entity getHookedEntity() {
        return hookedEntity;
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (owner != null) {
            BY_OWNER.put(owner, this);
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // An owner the client does not know leaves the chain nowhere to start.
        if (getOwner() == null) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        forget();
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        forget();
    }

    private void forget() {
        Entity owner = getOwner();
        if (owner != null) {
            BY_OWNER.remove(owner, this);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0 * 64.0;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }
}
