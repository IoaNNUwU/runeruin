package ioann.uwu.runeruin.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** What the Mossy Prophet and its Mossy Warriors share: the flock, the jumps and the poses of their attacks. */
public abstract class MossyRam extends Monster {
    /** What the body does besides walking and punching; the client turns it into a pose. */
    public enum Action { NONE, CROUCH, RAISE, HOLD, SPIN }

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(MossyRam.class, EntityDataSerializers.BYTE);
    private static final float SPIN_SPEED = 0.9F;

    // Client only: how far the pose of an action has come, and the same a tick ago.
    private float crouch;
    private float crouchO;
    private float raise;
    private float raiseO;
    private float hold;
    private float holdO;
    private float spin;
    private float spinO;

    protected MossyRam(EntityType<? extends MossyRam> type, Level level) {
        super(type, level);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RREntityTypes.MOSSY_PROPHET.get(), MossyProphet.createAttributes().build());
        event.put(RREntityTypes.MOSSY_WARRIOR.get(), MossyWarrior.createAttributes().build());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, MossyRam.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ACTION, (byte) 0);
    }

    public Action getAction() {
        return Action.values()[this.entityData.get(DATA_ACTION)];
    }

    protected void setAction(Action action) {
        this.entityData.set(DATA_ACTION, (byte) action.ordinal());
    }

    /**
     * Jumps to come down on a point after {@code flightTicks}. Returns the horizontal speed, which
     * {@link #keepFlying} has to restore every tick: air drag would stop the jump halfway.
     */
    protected Vec3 jumpAt(Vec3 point, double upSpeed, int flightTicks) {
        Vec3 flat = new Vec3(point.x - this.getX(), 0.0, point.z - this.getZ()).scale(1.0 / flightTicks);
        this.setDeltaMovement(flat.x, upSpeed, flat.z);
        return flat;
    }

    protected void keepFlying(Vec3 flat) {
        this.setDeltaMovement(flat.x, this.getDeltaMovement().y, flat.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            Action action = this.getAction();
            this.crouchO = this.crouch;
            this.crouch += ((action == Action.CROUCH ? 1.0F : 0.0F) - this.crouch) * 0.5F;
            this.raiseO = this.raise;
            this.raise += ((action == Action.RAISE ? 1.0F : 0.0F) - this.raise) * 0.4F;
            this.holdO = this.hold;
            this.hold += ((action == Action.HOLD ? 1.0F : 0.0F) - this.hold) * 0.4F;
            // A somersault stops at once: unwinding it would turn the mob backwards through the ground.
            this.spinO = action == Action.SPIN ? this.spin : 0.0F;
            this.spin = action == Action.SPIN ? this.spin + SPIN_SPEED : 0.0F;
        }
    }

    public float getCrouch(float partialTicks) {
        return Mth.lerp(partialTicks, this.crouchO, this.crouch);
    }

    public float getRaise(float partialTicks) {
        return Mth.lerp(partialTicks, this.raiseO, this.raise);
    }

    public float getHold(float partialTicks) {
        return Mth.lerp(partialTicks, this.holdO, this.hold);
    }

    public float getSpin(float partialTicks) {
        return Mth.lerp(partialTicks, this.spinO, this.spin);
    }

    // Their jumps are attacks.
    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.GOAT_STEP, 0.15F, 1.0F);
    }
}
