package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.items.RRItems;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * The anchor of the grappling hook on its chain. It flies like an arrow and hooks the first block or creature it hits.
 * A block holds the owner on a chain of the length it caught at: the owner swings under it like a pendulum, reels the
 * chain in and lets it out, and reeled in to the end climbs onto the top of the block if there is room. A creature is
 * pulled to the owner, then the anchor comes back.
 *
 * <p>The anchor stays out while a grappling hook is in the inventory slot it was shot from, so an owner can hang on
 * two. The chain and the pulls move the owner on the side that moves it anyway: the client of a player, the server for
 * a mob; only that side knows how long the chain is now. The server lets go by removing the anchor.
 */
public class GrapplingAnchor extends Projectile {

    /** Blocks of chain: an anchor that flies further comes back, and a chain lets out no more. */
    public static final double CHAIN_LENGTH = 64.0;
    /** Blocks of chain left reeled in to the end. */
    private static final double MIN_LENGTH = 1.5;
    /** Blocks per tick of chain reeled in or let out. */
    private static final double REEL_SPEED = 0.3;
    /** What the swing on a taut chain keeps of its speed per tick, instead of the air drag of the owner. */
    private static final double SWING_DAMPING = 0.985;
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
    /** The length of the chain when the anchor caught. */
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(GrapplingAnchor.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SLOT =
            SynchedEntityData.defineId(GrapplingAnchor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(GrapplingAnchor.class, EntityDataSerializers.BOOLEAN);

    // The anchors each owner has out: meanwhile their hooks fire none and show none at the muzzle. A map for each side,
    // because entities with one id are equal, and in singleplayer the client gives its entities the ids of the server.
    private static final Map<Entity, CopyOnWriteArrayList<GrapplingAnchor>> ON_CLIENT = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Entity, CopyOnWriteArrayList<GrapplingAnchor>> ON_SERVER = Collections.synchronizedMap(new WeakHashMap<>());

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    private @Nullable Entity hookedEntity;
    private double length = CHAIN_LENGTH;
    private int pullTicks;
    private int stuckTicks;
    private double closestDistance = Double.MAX_VALUE;

    public GrapplingAnchor(EntityType<? extends GrapplingAnchor> type, Level level) {
        super(type, level);
    }

    /** An anchor shot from the hook in the given inventory slot of the owner ({@link #slotOf}). */
    public GrapplingAnchor(Level level, LivingEntity owner, int slot) {
        this(RREntityTypes.GRAPPLING_ANCHOR.get(), level);
        getEntityData().set(DATA_SLOT, slot);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    /** The inventory slot of the hook in the hand; a creature other than a player has the main hand at slot 0. */
    public static int slotOf(LivingEntity owner, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND
                : owner instanceof Player player ? player.getInventory().getSelectedSlot() : 0;
    }

    /** All the anchors the owner has out. */
    public static List<GrapplingAnchor> of(Entity owner) {
        List<GrapplingAnchor> anchors = byOwner(owner.level()).get(owner);
        return anchors == null ? List.of() : anchors.stream().filter(anchor -> !anchor.isRemoved()).toList();
    }

    private static Map<Entity, CopyOnWriteArrayList<GrapplingAnchor>> byOwner(Level level) {
        return level.isClientSide() ? ON_CLIENT : ON_SERVER;
    }

    /** The anchor out of the hook in the hand of the owner. */
    public static @Nullable GrapplingAnchor of(LivingEntity owner, InteractionHand hand) {
        int slot = slotOf(owner, hand);
        return of(owner).stream().filter(anchor -> anchor.getSlot() == slot).findFirst().orElse(null);
    }

    /** The anchors out of the hooks in both hands of the owner. */
    public static List<GrapplingAnchor> inHands(LivingEntity owner) {
        return Stream.of(InteractionHand.values()).map(hand -> of(owner, hand)).filter(Objects::nonNull).toList();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_HOOKED_ENTITY, 0);
        entityData.define(DATA_HOOKED_BLOCK, Optional.empty());
        entityData.define(DATA_LENGTH, (float) CHAIN_LENGTH);
        entityData.define(DATA_SLOT, 0);
        entityData.define(DATA_CLIMBING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_HOOKED_ENTITY.equals(accessor)) {
            int id = getEntityData().get(DATA_HOOKED_ENTITY);
            hookedEntity = id > 0 ? level().getEntity(id - 1) : null;
        } else if (DATA_LENGTH.equals(accessor)) {
            length = getEntityData().get(DATA_LENGTH);
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
        if (!(getOwner() instanceof LivingEntity owner) || !level().isClientSide() && shouldReturn(owner)) {
            discard();
            return;
        }

        Optional<BlockPos> block = getEntityData().get(DATA_HOOKED_BLOCK);
        if (hookedEntity != null) {
            setPos(hookedEntity.getX(), hookedEntity.getY(0.8), hookedEntity.getZ());
            pull(hookedEntity, owner.position(), CREATURE_STOP);
        } else if (block.isPresent()) {
            Vec3 top = getEntityData().get(DATA_CLIMBING) ? climbTarget(block.get(), owner) : null;
            if (top != null) {
                pull(owner, top, OWNER_STOP);
            } else {
                hold(owner);
            }
        } else {
            fly();
        }
    }

    /** On the server: whether the hook left its slot, the owner went too far or the hooked thing is gone. */
    private boolean shouldReturn(LivingEntity owner) {
        if (!owner.isAlive() || owner.level() != level() || !hookInSlot(owner)) {
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

    private boolean hookInSlot(LivingEntity owner) {
        int slot = getSlot();
        ItemStack stack = owner instanceof Player player ? player.getInventory().getItem(slot)
                : owner.getItemInHand(slot == Inventory.SLOT_OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return stack.is(RRItems.GRAPPLING_HOOK);
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
        if (getOwner() != null) {
            getEntityData().set(DATA_LENGTH, (float) Mth.clamp(chainEnd(getOwner()).distanceTo(hit.getLocation()), MIN_LENGTH, CHAIN_LENGTH));
        }
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

    /** Where the chain holds the owner: about the hand. */
    private static Vec3 chainEnd(Entity owner) {
        return new Vec3(owner.getX(), owner.getY(0.7), owner.getZ());
    }

    /**
     * Keeps the owner within the length of the chain: on a taut chain it moves only across the chain, and what is out
     * beyond its length is pulled back. On a taut chain the swing keeps its speed in the air but for {@link #SWING_DAMPING}, so it dies down slowly.
     */
    private void hold(LivingEntity owner) {
        if (!owner.isLocalInstanceAuthoritative()) {
            return;
        }
        Vec3 motion = owner.getDeltaMovement();
        Vec3 offset = chainEnd(owner).subtract(position());
        if (offset.add(motion).length() <= length) {
            return;
        }
        owner.resetFallDistance();
        if (!owner.onGround()) {
            // The motion has just lost the air drag of the last tick: give it back.
            motion = motion.multiply(SWING_DAMPING / LivingEntity.BASE_HORIZONTAL_AIR_DRAG, SWING_DAMPING / LivingEntity.BASE_VERTICAL_AIR_DRAG,
                    SWING_DAMPING / LivingEntity.BASE_HORIZONTAL_AIR_DRAG);
        }
        // All of the motion along the chain goes: what the last pull back left of it would pump the swing up.
        Vec3 out = offset.normalize();
        motion = motion.subtract(out.scale(motion.dot(out)));
        Vec3 next = offset.add(motion);
        double excess = next.length() - length;
        if (excess > 0.0) {
            motion = motion.subtract(next.normalize().scale(Math.min(excess, PULL_SPEED)));
        }
        owner.setDeltaMovement(motion);
    }

    /** Reels the chain in, on both sides; reeled in to the end, the server lets the owner climb onto the block. */
    public void reelIn(LivingEntity owner) {
        Optional<BlockPos> block = getEntityData().get(DATA_HOOKED_BLOCK);
        if (block.isEmpty()) {
            return;
        }
        length = Math.max(MIN_LENGTH, length - REEL_SPEED);
        if (!level().isClientSide() && chainEnd(owner).distanceTo(position()) < MIN_LENGTH + 0.5 && climbTarget(block.get(), owner) != null) {
            getEntityData().set(DATA_CLIMBING, true);
        }
    }

    /** Lets the chain out on the side that holds the owner. */
    public void letOut() {
        if (hooksBlock()) {
            length = Math.min(CHAIN_LENGTH, length + REEL_SPEED);
        }
    }

    /**
     * Where the feet of the owner go when it climbs: just above the top of the hooked block, if it can stand there.
     * An anchor under a block hangs from it and has nowhere to climb.
     */
    private @Nullable Vec3 climbTarget(BlockPos block, Entity owner) {
        if (getY() < block.getY() + 1.0E-3) {
            return null;
        }
        VoxelShape shape = level().getBlockState(block).getCollisionShape(level(), block);
        Vec3 top = Vec3.atBottomCenterOf(block).add(0.0, (shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y)) + 0.1, 0.0);
        return level().noCollision(owner, owner.getDimensions(Pose.STANDING).makeBoundingBox(top)) ? top : null;
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

    public boolean hooksBlock() {
        return getEntityData().get(DATA_HOOKED_BLOCK).isPresent();
    }

    /** The inventory slot of the hook the anchor was shot from. */
    public int getSlot() {
        return getEntityData().get(DATA_SLOT);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (owner != null) {
            byOwner(level()).computeIfAbsent(owner, _ -> new CopyOnWriteArrayList<>()).addIfAbsent(this);
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
            byOwner(level()).computeIfPresent(owner, (_, anchors) -> {
                anchors.remove(this);
                return anchors.isEmpty() ? null : anchors;
            });
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
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
