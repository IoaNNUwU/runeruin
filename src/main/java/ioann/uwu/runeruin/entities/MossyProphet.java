package ioann.uwu.runeruin.entities;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A mini-boss: a huge ram overgrown with moss. Leaps onto its target, sending a shockwave over the ground from
 * where it lands, calls Mossy Warriors when few of them are left around it, and throws those at hand at the target.
 */
public class MossyProphet extends MossyRam {
    private static final double LEAP_MIN_DISTANCE = 4.0;
    private static final double LEAP_MAX_DISTANCE = 14.0;
    private static final int LEAP_WINDUP_TICKS = 15;
    // Six blocks up and down again.
    private static final double LEAP_UP_SPEED = 1.0;
    private static final int LEAP_FLIGHT_TICKS = 25;
    private static final int LEAP_RECOVERY_TICKS = 10;

    private static final double SHOCKWAVE_RADIUS = 7.0;
    private static final double SHOCKWAVE_SPEED = 0.7;
    private static final float SHOCKWAVE_DAMAGE = 8.0F;

    private static final int SUMMON_TICKS = 30;
    private static final int SUMMON_INTERVAL = 200;
    private static final int ENOUGH_WARRIORS = 3;
    private static final double WARRIOR_COUNT_RANGE = 16.0;

    private static final double THROW_MIN_DISTANCE = 5.0;
    private static final double THROW_MAX_DISTANCE = 16.0;
    private static final double THROW_GRAB_RANGE = 6.0;
    private static final int THROW_WINDUP_TICKS = 12;

    private int nextLeap;
    private int nextSummon;
    private int nextThrow;
    private Vec3 shockwaveCenter;
    private double shockwaveReach;

    public MossyProphet(EntityType<? extends MossyProphet> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new SummonGoal());
        this.goalSelector.addGoal(2, new ThrowGoal());
        this.goalSelector.addGoal(3, new LeapGoal());
    }

    // A mob that rides a mob steers it; the warrior in the hands of the Prophet does not.
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.shockwaveCenter != null) {
            this.spreadShockwave(level);
        }
    }

    /** One tick of the shockwave: a ring that widens over the ground and throws up whoever stands in its way. */
    private void spreadShockwave(ServerLevel level) {
        Vec3 center = this.shockwaveCenter;
        double inner = this.shockwaveReach;
        double outer = this.shockwaveReach = inner + SHOCKWAVE_SPEED;
        AABB area = new AABB(center, center).inflate(outer, 1.5, outer);
        // Whoever is in the air when the ring passes, a jumping player as well, is spared.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e.onGround() && !(e instanceof MossyRam))) {
            Vec3 away = new Vec3(entity.getX() - center.x, 0.0, entity.getZ() - center.z);
            double distance = away.length();
            if (distance >= inner && distance < outer && entity.hurtServer(level, this.damageSources().mobAttack(this), SHOCKWAVE_DAMAGE)) {
                // Thrown up and away, in place of the knockback of the hit: together they would add up to a fall that hurts.
                away = distance < 1.0E-3 ? Vec3.ZERO : away.scale(0.4 / distance);
                entity.setDeltaMovement(away.x, 0.6, away.z);
                entity.hurtMarked = true;
            }
        }
        for (int i = 0, dust = (int) (outer * 5.0); i < dust; i++) {
            double angle = Math.PI * 2.0 * i / dust;
            double x = center.x + Math.cos(angle) * outer;
            double z = center.z + Math.sin(angle) * outer;
            BlockState ground = level.getBlockState(BlockPos.containing(x, center.y - 0.5, z));
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground), x, center.y + 0.1, z, 2, 0.1, 0.0, 0.1, 0.1);
            }
        }
        if (outer >= SHOCKWAVE_RADIUS) {
            this.shockwaveCenter = null;
        }
    }

    private void summonWarriors(ServerLevel level) {
        for (int i = 3 + this.random.nextInt(3); i > 0; i--) {
            MossyWarrior warrior = RREntityTypes.MOSSY_WARRIOR.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 2.0 + this.random.nextDouble() * 1.5;
            warrior.snapTo(this.getX() + Math.cos(angle) * distance, this.getY(), this.getZ() + Math.sin(angle) * distance, this.getYRot(), 0.0F);
            // A wall or a ledge in the way: the warrior comes out from under the Prophet.
            if (!level.noCollision(warrior)) {
                warrior.snapTo(this.position(), this.getYRot(), 0.0F);
            }
            warrior.finalizeSpawn(level, level.getCurrentDifficultyAt(warrior.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            warrior.setTarget(this.getTarget());
            level.addFreshEntity(warrior);
            warrior.spawnAnim();
        }
    }

    private class LeapGoal extends Goal {
        private LivingEntity target;
        private Vec3 flat;
        private int time;
        private int landed;

        LeapGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.target = MossyProphet.this.getTarget();
            if (this.target == null || !MossyProphet.this.onGround() || MossyProphet.this.tickCount < MossyProphet.this.nextLeap) {
                return false;
            }
            double distance = MossyProphet.this.distanceTo(this.target);
            return distance >= LEAP_MIN_DISTANCE && distance <= LEAP_MAX_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.landed == 0 || this.time < this.landed + LEAP_RECOVERY_TICKS;
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
            this.time = 0;
            this.landed = 0;
            MossyProphet.this.getNavigation().stop();
            MossyProphet.this.setAction(Action.CROUCH);
            MossyProphet.this.playSound(SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 2.0F, 0.6F);
        }

        @Override
        public void tick() {
            this.time++;
            if (this.time < LEAP_WINDUP_TICKS) {
                MossyProphet.this.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            } else if (this.time == LEAP_WINDUP_TICKS) {
                this.flat = MossyProphet.this.jumpAt(this.target.position(), LEAP_UP_SPEED, LEAP_FLIGHT_TICKS);
                MossyProphet.this.setAction(Action.RAISE);
                MossyProphet.this.playSound(SoundEvents.GOAT_LONG_JUMP, 2.0F, 0.6F);
            } else if (this.landed == 0) {
                // Water, or a hole it never comes out of, ends the leap without a shockwave.
                if (MossyProphet.this.onGround()) {
                    this.land();
                } else if (MossyProphet.this.isInWater() || this.time > LEAP_WINDUP_TICKS + LEAP_FLIGHT_TICKS * 4) {
                    this.landed = this.time - LEAP_RECOVERY_TICKS;
                } else {
                    MossyProphet.this.keepFlying(this.flat);
                }
            }
        }

        private void land() {
            this.landed = this.time;
            MossyProphet.this.setDeltaMovement(Vec3.ZERO);
            MossyProphet.this.setAction(Action.CROUCH);
            MossyProphet.this.shockwaveCenter = MossyProphet.this.position();
            MossyProphet.this.shockwaveReach = 0.0;
            MossyProphet.this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0F, 0.7F);
            MossyProphet.this.level().levelEvent(LevelEvent.PARTICLES_SMASH_ATTACK, MossyProphet.this.getOnPos(), 750);
        }

        @Override
        public void stop() {
            MossyProphet.this.setAction(Action.NONE);
            MossyProphet.this.nextLeap = MossyProphet.this.tickCount + 100 + MossyProphet.this.random.nextInt(60);
        }
    }

    /** Lifts a warrior that stands close by over its head and throws it at the target. */
    private class ThrowGoal extends Goal {
        private LivingEntity target;
        private MossyWarrior warrior;
        private int time;

        ThrowGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.target = MossyProphet.this.getTarget();
            if (this.target == null || !MossyProphet.this.onGround() || MossyProphet.this.tickCount < MossyProphet.this.nextThrow) {
                return false;
            }
            double distance = MossyProphet.this.distanceTo(this.target);
            if (distance < THROW_MIN_DISTANCE || distance > THROW_MAX_DISTANCE) {
                return false;
            }
            AABB reach = MossyProphet.this.getBoundingBox().inflate(THROW_GRAB_RANGE);
            this.warrior = MossyProphet.this.level()
                    .getEntitiesOfClass(MossyWarrior.class, reach, near -> near.isAlive() && near.onGround() && near.getAction() == Action.NONE)
                    .stream()
                    .min(Comparator.comparingDouble(near -> MossyProphet.this.distanceToSqr(near)))
                    .orElse(null);
            return this.warrior != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.time < THROW_WINDUP_TICKS && this.warrior.isAlive() && this.target.isAlive();
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
            this.time = 0;
            MossyProphet.this.getNavigation().stop();
            MossyProphet.this.setAction(Action.RAISE);
            this.warrior.startRiding(MossyProphet.this, true, false);
            MossyProphet.this.playSound(SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 2.0F, 0.8F);
        }

        @Override
        public void tick() {
            MossyProphet.this.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            if (++this.time == THROW_WINDUP_TICKS) {
                // Dismounting puts the warrior on the ground beside the Prophet: back up into the hands.
                Vec3 hands = this.warrior.position();
                this.warrior.stopRiding();
                this.warrior.snapTo(hands, MossyProphet.this.getYRot(), 0.0F);
                this.warrior.throwAt(this.target);
                MossyProphet.this.swing(InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void stop() {
            if (this.warrior.getVehicle() == MossyProphet.this) {
                this.warrior.stopRiding();
            }
            MossyProphet.this.setAction(Action.NONE);
            MossyProphet.this.nextThrow = MossyProphet.this.tickCount + 80 + MossyProphet.this.random.nextInt(40);
        }
    }

    private class SummonGoal extends Goal {
        private int time;

        SummonGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (MossyProphet.this.getTarget() == null || !MossyProphet.this.onGround() || MossyProphet.this.tickCount < MossyProphet.this.nextSummon) {
                return false;
            }
            AABB area = MossyProphet.this.getBoundingBox().inflate(WARRIOR_COUNT_RANGE);
            return MossyProphet.this.level().getEntitiesOfClass(MossyWarrior.class, area, LivingEntity::isAlive).size() < ENOUGH_WARRIORS;
        }

        @Override
        public boolean canContinueToUse() {
            return this.time < SUMMON_TICKS;
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
            this.time = 0;
            MossyProphet.this.getNavigation().stop();
            MossyProphet.this.setAction(Action.RAISE);
            MossyProphet.this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.6F);
        }

        @Override
        public void tick() {
            if (++this.time == SUMMON_TICKS) {
                MossyProphet.this.summonWarriors((ServerLevel) MossyProphet.this.level());
            }
        }

        @Override
        public void stop() {
            MossyProphet.this.setAction(Action.NONE);
            MossyProphet.this.nextSummon = MossyProphet.this.tickCount + SUMMON_INTERVAL;
        }
    }
}
