package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.dimension.Const;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A rusty machine of the Stone Forest, taller than the Blooming caves: it stands in the open gaps between the
 * islands of the top layer and looks over their edge. It never jumps; when a player comes into reach, it beats its
 * fists on the ground in front of it or, at a player up on the top layer, down onto the islands, and from where a
 * fist hits a shockwave runs over the ground.
 *
 * <p>Its shape and its animations are the Blockbench project {@code colossus.bbmodel}. Every cube is a solid
 * {@link ColossusPart} that follows its bone through the animations ({@link ColossusRig}), so players can climb the
 * colossus and ride on it, turning with it. It walks only where nothing hangs over it, and it is slow to turn.</p>
 */
public class Colossus extends Monster {
    /** A pixel of the model is this many sixteenths of a block: the colossus stands about 97 blocks tall. */
    public static final float SCALE = 12.0F;
    /** How far the colossus goes in one loop of its walk, two strides: the feet stay where they step. */
    public static final float WALK_CYCLE = 56.0F;
    // Its speed in blocks a tick at the full swing of the walk.
    private static final float WALK_SPEED = 0.3F;
    // How far its body turns in a tick, in degrees: half a turn takes six seconds. Its head turns a little faster
    // and only so far from the body; a way that turns more than the last of these it turns to standing.
    private static final float TURN_SPEED = 1.5F;
    private static final float HEAD_TURN_SPEED = 3.0F;
    private static final int HEAD_TURN = 30;
    private static final float TURN_IN_PLACE = 45.0F;
    // Nothing may hang over this circle around it: a wide one where it spawns, a narrower one on its way.
    private static final int SPAWN_RADIUS = 24;
    private static final int WALK_RADIUS = 12;
    // Blocks lower than this over its feet leave the sky open: the boulders and monoliths of the Stone Forest.
    private static final int CLEARANCE = 24;
    private static final int SPAWN_SPACING = 160;
    // A target this high over its feet stands on the top layer and gets the fists from above.
    private static final double HIGH_TARGET = 30.0;
    // A slam starts when the target is this close to where the fists come down, and the shockwave reaches it.
    private static final double SLAM_TOLERANCE = 12.0;
    private static final int SLAM_COOLDOWN = 60;
    private static final float SHOCKWAVE_SPEED = 0.9F;
    private static final float SHOCKWAVE_RADIUS = 24.0F;

    /** What the body does besides walking; each action is the animation of the same name in the project. */
    public enum Action {
        NONE(""), SLAM_GROUND("slam_ground"), SLAM_HIGH("slam_high");

        public final String animation;

        Action(String animation) {
            this.animation = animation;
        }
    }

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(Colossus.class, EntityDataSerializers.BYTE);
    private static @Nullable ColossusRig rig;
    private static final double[] REACH = {Double.NaN, Double.NaN, Double.NaN};

    private final ColossusPart[] parts;
    private boolean partsPlaced;
    // How the body was turned when the cubes were placed last: what stands on them turns as far.
    private float placedBodyRot;
    private static @Nullable Colossus carryingLocalPlayer;
    private int carriedLocalPlayerAt;
    private float stride;
    private float strideO;
    private float walkWeight;
    private float walkWeightO;
    private int actionTicks;
    private int cooldown;
    private final List<Shockwave> shockwaves = new ArrayList<>();

    public Colossus(EntityType<? extends Colossus> type, Level level) {
        super(type, level);
        ColossusRig rig = rig();
        this.parts = new ColossusPart[rig.cubeCount()];
        for (int cube = 0; cube < this.parts.length; cube++) {
            this.parts[cube] = new ColossusPart(this, rig.cubeBone(cube), rig.cube(cube));
        }
        this.moveControl = new SlowTurnControl();
        this.xpReward = 100;
    }

    /** The bones of the project, read once for the server and the client alike. */
    public static ColossusRig rig() {
        if (rig == null) {
            rig = ColossusRig.load("colossus");
        }
        return rig;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 128.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 4.0);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RREntityTypes.COLOSSUS.get(), createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                RREntityTypes.COLOSSUS.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Colossus::checkColossusSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    /** On the floor of the Blooming caves, with the sky open around it and no other colossus near. */
    private static boolean checkColossusSpawnRules(EntityType<Colossus> type, ServerLevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!EntitySpawnReason.isSpawner(spawnReason)
                && (pos.getY() <= Const.BLOOMING_CAVES_Y || pos.getY() >= Const.BLOOMING_CAVES_CEILING_Y
                || !isOpen(level, pos.getX(), pos.getY(), pos.getZ(), SPAWN_RADIUS)
                || !level.getEntitiesOfClass(Colossus.class, new AABB(pos).inflate(SPAWN_SPACING)).isEmpty())) {
            return false;
        }
        return Mob.checkMobSpawnRules(type, level, spawnReason, pos, random);
    }

    /** Whether nothing hangs over a circle around a place on the ground; an unloaded column counts as closed. */
    public static boolean isOpen(LevelReader level, int x, int y, int z, int radius) {
        for (int ring = 0; ring <= 2; ring++) {
            int r = radius * ring / 2;
            for (int i = 0; i < (ring == 0 ? 1 : 8); i++) {
                int columnX = x + Math.round(Mth.cos(i * Mth.PI / 4) * r);
                int columnZ = z + Math.round(Mth.sin(i * Mth.PI / 4) * r);
                if (!level.hasChunk(SectionPos.blockToSectionCoord(columnX), SectionPos.blockToSectionCoord(columnZ))
                        || level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnX, columnZ) > y + CLEARANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SlamGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.02F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 64.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // It looks for players as high as it is tall: up on the top layer too.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            @Override
            protected AABB getTargetSearchArea(double followDistance) {
                return this.mob.getBoundingBox().inflate(followDistance);
            }
        });
    }

    /** Paths keep to the ground under the open sky: under an island the colossus would stand in the rock. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level) {
            @Override
            protected PathFinder createPathFinder(int maxVisitedNodes) {
                this.nodeEvaluator = new WalkNodeEvaluator() {
                    @Override
                    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
                        return isOpen(mob.level(), x, y, z, WALK_RADIUS) ? super.getPathTypeOfMob(context, x, y, z, mob) : PathType.BLOCKED;
                    }
                };
                return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ACTION, (byte) 0);
    }

    public Action getAction() {
        return Action.values()[this.entityData.get(DATA_ACTION)];
    }

    private void setAction(Action action) {
        this.entityData.set(DATA_ACTION, (byte) action.ordinal());
        this.actionTicks = 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ACTION.equals(accessor)) {
            this.actionTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        float moved = (float) Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        this.strideO = this.stride;
        this.stride += moved;
        this.walkWeightO = this.walkWeight;
        this.walkWeight += (Math.min(moved / WALK_SPEED, 1.0F) - this.walkWeight) * 0.25F;
        Action action = this.getAction();
        if (action != Action.NONE) {
            this.actionTicks++;
            if (!this.level().isClientSide()) {
                ColossusRig.Animation animation = rig().animation(action.animation);
                if (this.actionTicks == Math.round(animation.impact() * 20.0F)) {
                    this.impact();
                }
                if (this.actionTicks >= animation.length() * 20.0F) {
                    this.setAction(Action.NONE);
                    this.cooldown = SLAM_COOLDOWN;
                }
            }
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        this.updateParts();
        if (this.level() instanceof ServerLevel level) {
            this.shockwaves.removeIf(shockwave -> shockwave.tick(level, this));
        }
    }

    /** The pose of the bones: the idle movement, the walk as far as it walks and the action, as the model shows them. */
    public static float[][] pose(float ageInTicks, float stride, float walkWeight, Action action, float actionSeconds) {
        ColossusRig rig = rig();
        float[][] pose = rig.restPose();
        rig.apply(rig.animation("idle"), ageInTicks / 20.0F, 1.0F, pose);
        ColossusRig.Animation walk = rig.animation("walk");
        rig.apply(walk, walkSeconds(stride, walk.length()), walkWeight, pose);
        if (action != Action.NONE) {
            rig.apply(rig.animation(action.animation), actionSeconds, 1.0F, pose);
        }
        return pose;
    }

    /** Where the walk animation is after walking a distance. */
    public static float walkSeconds(float stride, float walkLength) {
        return stride / WALK_CYCLE * walkLength;
    }

    private ColossusRig.Posed posed() {
        return rig().posed(pose(this.tickCount, this.stride, this.walkWeight, this.getAction(), this.actionTicks / 20.0F), this.yBodyRot, SCALE);
    }

    /**
     * Moves the cubes to their bones and carries along what stands on them: to where its place on the cube is now,
     * turned as far as the body turned.
     */
    private void updateParts() {
        ColossusRig.Posed posed = this.posed();
        Vec3 at = this.position();
        float turn = Mth.wrapDegrees(this.yBodyRot - this.placedBodyRot);
        Set<Entity> found = new HashSet<>();
        for (int cube = 0; cube < this.parts.length; cube++) {
            ColossusPart part = this.parts[cube];
            List<Entity> riders = this.partsPlaced ? this.ridersOf(part, found) : List.of();
            List<Vector3f> places = riders.stream().map(rider -> part.toCube(rider.position())).toList();
            part.place(posed.bones()[part.bone], at, posed.cubes()[cube].move(at));
            for (int i = 0; i < riders.size(); i++) {
                this.carry(riders.get(i), part.toWorld(places.get(i)), turn);
            }
        }
        this.partsPlaced = true;
        this.placedBodyRot = this.yBodyRot;
    }

    /** What stands on a cube and no other cube yet, of what this side moves: the server its mobs, a client its player. */
    private List<Entity> ridersOf(ColossusPart part, Set<Entity> found) {
        List<Entity> riders = new ArrayList<>();
        for (Entity entity : this.level().getEntities(this, part.getBoundingBox().inflate(0.5),
                entity -> !(entity instanceof PartEntity<?>) && !entity.isPassenger() && part.carries(entity.position()))) {
            if (!found.add(entity)) {
                continue;
            }
            if (entity instanceof ServerPlayer player) {
                // Its client keeps it up on the colossus, where the server sees nothing under it.
                player.connection.resetFlyingTicks();
            }
            if (entity.isLocalInstanceAuthoritative()) {
                riders.add(entity);
            }
        }
        return riders;
    }

    private void carry(Entity rider, Vec3 to, float turn) {
        // A move along the cube is no landing, but the rider still stands: it can jump from there.
        boolean onGround = rider.onGround();
        rider.move(MoverType.SHULKER_BOX, to.subtract(rider.position()));
        rider.setOnGround(onGround);
        if (rider instanceof Player && this.level().isClientSide()) {
            // The camera shows the player's turn as it is, without easing it between ticks: the client turns its
            // player every frame (ColossusRiderView).
            carryingLocalPlayer = this;
            this.carriedLocalPlayerAt = this.tickCount;
        } else {
            rider.setYRot(rider.getYRot() + turn);
            rider.setYHeadRot(rider.getYHeadRot() + turn);
        }
    }

    /**
     * What an entity collides with in an area, with the cubes of every colossus there added; the area grows by the
     * height the entity steps up, which the game tries with the same shapes.
     */
    public static List<VoxelShape> withCubes(Level level, Entity entity, AABB area, List<VoxelShape> shapes) {
        AABB reach = area.expandTowards(0.0, entity.maxUpStep(), 0.0);
        List<VoxelShape> all = shapes;
        for (PartEntity<?> part : level.dragonParts()) {
            if (part instanceof ColossusPart cube && cube.getBoundingBox().intersects(reach) && cube.blocks(entity)) {
                if (all == shapes) {
                    all = new ArrayList<>(shapes);
                }
                cube.addCollisions(reach, all);
            }
        }
        return all;
    }

    /** The colossus the player of this client stood on in its last tick, if any. */
    public static @Nullable Colossus carryingLocalPlayer() {
        Colossus colossus = carryingLocalPlayer;
        return colossus != null && !colossus.isRemoved() && colossus.tickCount - colossus.carriedLocalPlayerAt <= 1 ? colossus : null;
    }

    /** Turns no faster than its body can: the way it walks and the place it looks at come round slowly. */
    private class SlowTurnControl extends MoveControl {
        SlowTurnControl() {
            super(Colossus.this);
        }

        @Override
        public void tick() {
            float before = this.mob.getYRot();
            super.tick();
            float wanted = this.mob.getYRot();
            this.mob.setYRot(Mth.approachDegrees(before, wanted, TURN_SPEED));
            if (Math.abs(Mth.degreesDifference(before, wanted)) > TURN_IN_PLACE) {
                this.mob.setSpeed(0.0F);
            }
        }
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                Colossus colossus = Colossus.this;
                colossus.yBodyRot = Mth.approachDegrees(colossus.yBodyRot, colossus.getYRot(), TURN_SPEED);
                colossus.yHeadRot = colossus.yBodyRot + Mth.clamp(Mth.wrapDegrees(colossus.yHeadRot - colossus.yBodyRot), -HEAD_TURN, HEAD_TURN);
            }
        };
    }

    @Override
    public int getMaxHeadYRot() {
        return HEAD_TURN;
    }

    @Override
    public int getHeadRotSpeed() {
        return (int) HEAD_TURN_SPEED;
    }

    /** The box around the whole colossus, for drawing it whenever any of it is in view. */
    public AABB getBodyBox() {
        AABB box = this.getBoundingBox();
        for (ColossusPart part : this.parts) {
            box = box.minmax(part.getBoundingBox());
        }
        return box;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(packet.getId() + i + 1);
        }
    }

    /** Where the fists of an action come down in front of the colossus: the distance forward from its feet. */
    public static double reach(Action action) {
        if (Double.isNaN(REACH[action.ordinal()])) {
            REACH[action.ordinal()] = measureReach(action);
        }
        return REACH[action.ordinal()];
    }

    private static double measureReach(Action action) {
        ColossusRig rig = rig();
        float[][] pose = rig.restPose();
        ColossusRig.Animation animation = rig.animation(action.animation);
        rig.apply(animation, animation.impact(), 1.0F, pose);
        ColossusRig.Posed posed = rig.posed(pose, 0.0F, SCALE);
        return (rig.boneBox(posed, rig.bone("right_hand")).getCenter().z + rig.boneBox(posed, rig.bone("left_hand")).getCenter().z) / 2.0;
    }

    /** Each fist hits the ground under it, or the top layer, and starts a shockwave there. */
    private void impact() {
        ServerLevel level = (ServerLevel) this.level();
        ColossusRig.Posed posed = this.posed();
        for (String hand : new String[]{"right_hand", "left_hand"}) {
            AABB fist = rig().boneBox(posed, rig().bone(hand)).move(this.position());
            Vec3 center = fist.getCenter();
            double surface = this.surfaceUnder(center.x, fist.minY, center.z);
            level.playSound(null, center.x, surface, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, surface + 1.0, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            this.shockwaves.add(new Shockwave(center.x, surface, center.z, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        }
    }

    /** The top of the ground a fist comes down on: the first solid block from a little over the fist downward. */
    private double surfaceUnder(double x, double fistY, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, fistY + 16.0, z).mutable();
        for (int i = 0; i < 56; i++, pos.move(0, -1, 0)) {
            BlockState below = this.level().getBlockState(pos.below());
            if (!below.getCollisionShape(this.level(), pos.below()).isEmpty()
                    && this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
                return pos.getY();
            }
        }
        return fistY;
    }

    /** A ring that runs out over the ground from where a fist hit, hurting and throwing what stands on that ground. */
    private static final class Shockwave {
        private final double x;
        private final double y;
        private final double z;
        private final float damage;
        private final Set<UUID> hit = new HashSet<>();
        private float radius;

        Shockwave(double x, double y, double z, float damage) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.damage = damage;
        }

        /** Runs the ring on by a tick; true once it has run out. */
        boolean tick(ServerLevel level, Colossus colossus) {
            this.radius += SHOCKWAVE_SPEED;
            AABB area = new AABB(this.x - this.radius - 1, this.y - 1.5, this.z - this.radius - 1, this.x + this.radius + 1, this.y + 2.5, this.z + this.radius + 1);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != colossus && entity.isAlive())) {
                double distance = Math.hypot(entity.getX() - this.x, entity.getZ() - this.z);
                // Only what stands on the ground the ring runs over: not what is above it, not what it has passed.
                if (distance > this.radius || distance < this.radius - 3.0 || entity.getY() > this.y + 2.0 || !this.hit.add(entity.getUUID())) {
                    continue;
                }
                entity.hurtServer(level, colossus.damageSources().mobAttack(colossus), this.damage);
                Vec3 away = new Vec3(entity.getX() - this.x, 0.0, entity.getZ() - this.z);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(1.4);
                entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, 0.6, away.z));
                entity.hurtMarked = true;
            }
            int points = Mth.ceil(this.radius * 2.0F);
            for (int i = 0; i < points; i++) {
                double angle = Mth.TWO_PI * i / points;
                double px = this.x + Math.cos(angle) * this.radius;
                double pz = this.z + Math.sin(angle) * this.radius;
                BlockState ground = level.getBlockState(BlockPos.containing(px, this.y - 1.0, pz));
                if (!ground.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), px, this.y + 0.1, pz, 2, 0.3, 0.1, 0.3, 0.15);
                }
            }
            return this.radius >= SHOCKWAVE_RADIUS;
        }
    }

    /**
     * Walks to where its fists come down on the target, turns to it and slams: onto the ground in front of it, or
     * from above onto a target up on the top layer.
     */
    private class SlamGoal extends Goal {
        private int repath;

        SlamGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = Colossus.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || Colossus.this.getAction() != Action.NONE;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            Colossus.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Colossus colossus = Colossus.this;
            LivingEntity target = colossus.getTarget();
            if (colossus.getAction() != Action.NONE || target == null) {
                return;
            }
            colossus.getLookControl().setLookAt(target, HEAD_TURN_SPEED, 30.0F);
            Action slam = target.getY() > colossus.getY() + HIGH_TARGET ? Action.SLAM_HIGH : Action.SLAM_GROUND;
            double reach = reach(slam);
            double dx = target.getX() - colossus.getX();
            double dz = target.getZ() - colossus.getZ();
            double distance = Math.max(Math.hypot(dx, dz), 1.0E-3);
            if (Math.abs(distance - reach) < SLAM_TOLERANCE) {
                colossus.getNavigation().stop();
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                colossus.setYRot(Mth.approachDegrees(colossus.getYRot(), yaw, TURN_SPEED));
                if (colossus.cooldown == 0 && Math.abs(Mth.degreesDifference(colossus.yBodyRot, yaw)) < 15.0F) {
                    colossus.setAction(slam);
                }
            } else if (--this.repath <= 0) {
                // To the place that far from the target on the line to it, nearer or back.
                this.repath = 20;
                colossus.getNavigation().moveTo(target.getX() - dx / distance * reach, colossus.getY(), target.getZ() - dz / distance * reach, 1.0);
            }
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 256.0 * getViewScale();
        return distance < range * range;
    }

    // Its head is up in the rock of the islands half the time; it does not choke there.
    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected float nextStep() {
        return this.moveDist + WALK_CYCLE / 4.0F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 4.0F, 0.3F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F;
    }

    public float getStride(float partialTicks) {
        return Mth.lerp(partialTicks, this.strideO, this.stride);
    }

    public float getWalkWeight(float partialTicks) {
        return Mth.lerp(partialTicks, this.walkWeightO, this.walkWeight);
    }

    public float getActionSeconds(float partialTicks) {
        return (this.actionTicks + partialTicks) / 20.0F;
    }
}
