package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/** A berry blown from a blowpipe: it hurts a little and gives the creature it hits the effect of its kind. */
public class BlowpipeBerry extends ThrowableItemProjectile {

    private static final float DAMAGE = 1.0F;
    private static final byte BURST = 3;

    public BlowpipeBerry(EntityType<? extends BlowpipeBerry> type, Level level) {
        super(type, level);
    }

    public BlowpipeBerry(Level level, LivingEntity shooter, ItemStack berry) {
        super(RREntityTypes.BLOWPIPE_BERRY.get(), shooter, level, berry);
    }

    /** The effect of a berry on the creature it hits; null for an item that a blowpipe does not shoot. */
    public static @Nullable MobEffectInstance effectOf(ItemStack berry) {
        if (berry.is(RRItems.MOSSBERRY)) {
            return new MobEffectInstance(MobEffects.POISON, 6 * 20);
        }
        if (berry.is(Items.GLOW_BERRIES) || berry.is(RRItems.WISPBERRY)) {
            return new MobEffectInstance(MobEffects.GLOWING, 10 * 20);
        }
        return null;
    }

    @Override
    protected Item getDefaultItem() {
        return RRItems.MOSSBERRY.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        MobEffectInstance effect = effectOf(this.getItem());
        if (this.level() instanceof ServerLevel level
                && hitResult.getEntity() instanceof LivingEntity target
                && target.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DAMAGE)
                && effect != null) {
            target.addEffect(effect, this.getEffectSource());
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.6F, 1.6F);
            this.level().broadcastEntityEvent(this, BURST);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == BURST) {
            var particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(this.getItem()));
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }
}
