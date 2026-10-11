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

/** A small fighter of the Mossy Prophet: somersaults onto its target from a distance and punches up close. */
public class MossyWarrior extends MossyRam {
    private static final double SPIN_JUMP_MIN_DISTANCE = 3.0;
    private static final double SPIN_JUMP_MAX_DISTANCE = 8.0;
    // Two blocks up and down again.
    private static final double SPIN_JUMP_UP_SPEED = 0.55;
    private static final int SPIN_JUMP_FLIGHT_TICKS = 14;
    private static final float SPIN_JUMP_DAMAGE = 5.0F;

    private int nextSpinJump;

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

    private class SpinJumpGoal extends Goal {
        private LivingEntity target;
        private Vec3 flat;
        private boolean hit;

        SpinJumpGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            this.target = MossyWarrior.this.getTarget();
            if (this.target == null || !MossyWarrior.this.onGround() || MossyWarrior.this.tickCount < MossyWarrior.this.nextSpinJump) {
                return false;
            }
            double distance = MossyWarrior.this.distanceTo(this.target);
            return distance >= SPIN_JUMP_MIN_DISTANCE && distance <= SPIN_JUMP_MAX_DISTANCE
                    && MossyWarrior.this.getSensing().hasLineOfSight(this.target);
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
            this.hit = false;
            this.flat = MossyWarrior.this.jumpAt(this.target.position(), SPIN_JUMP_UP_SPEED, SPIN_JUMP_FLIGHT_TICKS);
            MossyWarrior.this.setAction(Action.SPIN);
            MossyWarrior.this.playSound(SoundEvents.GOAT_LONG_JUMP, 1.0F, 1.5F);
        }

        @Override
        public void tick() {
            if (this.hit) {
                return;
            }
            MossyWarrior.this.keepFlying(this.flat);
            if (MossyWarrior.this.getBoundingBox().inflate(0.2).intersects(this.target.getBoundingBox())) {
                this.hit = true;
                this.target.hurtServer((ServerLevel) MossyWarrior.this.level(), MossyWarrior.this.damageSources().mobAttack(MossyWarrior.this), SPIN_JUMP_DAMAGE);
                // Bounces off its target.
                MossyWarrior.this.setDeltaMovement(-this.flat.x * 0.5, 0.3, -this.flat.z * 0.5);
            }
        }

        @Override
        public void stop() {
            MossyWarrior.this.setAction(Action.NONE);
            MossyWarrior.this.nextSpinJump = MossyWarrior.this.tickCount + 40 + MossyWarrior.this.random.nextInt(40);
        }
    }
}
