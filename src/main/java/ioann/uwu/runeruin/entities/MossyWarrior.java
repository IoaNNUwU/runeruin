package ioann.uwu.runeruin.entities;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A small fighter of the Mossy Prophet: somersaults onto its target from a distance and punches up close. The
 * Prophet throws it at a target as well.
 */
public class MossyWarrior extends MossyRam {
    private static final double SPIN_JUMP_MIN_DISTANCE = 3.0;
    private static final double SPIN_JUMP_MAX_DISTANCE = 8.0;
    // Two blocks up and down again.
    private static final double SPIN_JUMP_UP_SPEED = 0.55;
    private static final int SPIN_JUMP_FLIGHT_TICKS = 14;
    private static final float SPIN_JUMP_DAMAGE = 5.0F;

    // From the hands of the Prophet, three blocks above the ground, down onto the target.
    private static final double THROW_UP_SPEED = 0.35;
    private static final int THROW_FLIGHT_TICKS = 15;
    private static final float THROW_DAMAGE = 8.0F;

    private int nextSpinJump;
    // The horizontal speed of a somersault under way and what it does to the target.
    private Vec3 flight;
    private float flightDamage;

    public MossyWarrior(EntityType<? extends MossyWarrior> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new SpinJumpGoal());
    }

    /** Flies at the target from where it is, the hands of the Prophet, and goes on fighting it after landing. */
    public void throwAt(LivingEntity target) {
        this.setTarget(target);
        this.somersault(target, THROW_UP_SPEED, THROW_FLIGHT_TICKS, THROW_DAMAGE);
    }

    private void somersault(LivingEntity target, double upSpeed, int flightTicks, float damage) {
        this.flight = this.jumpAt(target.position(), upSpeed, flightTicks);
        this.flightDamage = damage;
        this.setAction(Action.SPIN);
        this.playSound(SoundEvents.GOAT_LONG_JUMP, 1.0F, 1.5F);
    }

    /** Keeps a somersault on its way until it meets the target or the ground, also one the Prophet started. */
    private class SpinJumpGoal extends Goal {
        SpinJumpGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (MossyWarrior.this.flight != null) {
                return true;
            }
            LivingEntity target = MossyWarrior.this.getTarget();
            if (target == null || !MossyWarrior.this.onGround() || MossyWarrior.this.tickCount < MossyWarrior.this.nextSpinJump) {
                return false;
            }
            double distance = MossyWarrior.this.distanceTo(target);
            return distance >= SPIN_JUMP_MIN_DISTANCE && distance <= SPIN_JUMP_MAX_DISTANCE
                    && MossyWarrior.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return !MossyWarrior.this.onGround();
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            if (MossyWarrior.this.flight == null) {
                MossyWarrior.this.somersault(MossyWarrior.this.getTarget(), SPIN_JUMP_UP_SPEED, SPIN_JUMP_FLIGHT_TICKS, SPIN_JUMP_DAMAGE);
            }
        }

        @Override
        public void tick() {
            Vec3 flight = MossyWarrior.this.flight;
            LivingEntity target = MossyWarrior.this.getTarget();
            if (flight == null || target == null) {
                return;
            }
            MossyWarrior.this.keepFlying(flight);
            if (MossyWarrior.this.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
                target.hurtServer((ServerLevel) MossyWarrior.this.level(), MossyWarrior.this.damageSources().mobAttack(MossyWarrior.this), MossyWarrior.this.flightDamage);
                // Bounces off its target.
                MossyWarrior.this.setDeltaMovement(-flight.x * 0.5, 0.3, -flight.z * 0.5);
                MossyWarrior.this.flight = null;
            }
        }

        @Override
        public void stop() {
            MossyWarrior.this.flight = null;
            MossyWarrior.this.setAction(Action.NONE);
            MossyWarrior.this.nextSpinJump = MossyWarrior.this.tickCount + 40 + MossyWarrior.this.random.nextInt(40);
        }
    }
}
