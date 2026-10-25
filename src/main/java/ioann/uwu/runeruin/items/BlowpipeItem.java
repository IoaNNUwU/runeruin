package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.entities.BlowpipeBerry;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Shoots berries ({@link BlowpipeBerry#effectOf}). As with a bow, the player holds use and lets go: the pipe goes
 * to the mouth, and the longer the breath up to {@link #FULL_BREATH}, the faster the berry flies.
 */
public class BlowpipeItem extends ProjectileWeaponItem {

    private static final int FULL_BREATH = 10;
    private static final float FULL_SPEED = 2.5F;
    private static final Predicate<ItemStack> BERRIES = berry -> BlowpipeBerry.effectOf(berry) != null;

    public BlowpipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getProjectile(player.getItemInHand(hand)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack blowpipe, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        ItemStack berry = player.getProjectile(blowpipe);
        float breath = Math.min(1.0F, (this.getUseDuration(blowpipe, entity) - remainingTime) / (float) FULL_BREATH);
        if (berry.isEmpty() || breath < 0.2F) {
            return false;
        }

        List<ItemStack> berries = draw(blowpipe, berry, player);
        if (level instanceof ServerLevel serverLevel && !berries.isEmpty()) {
            this.shoot(serverLevel, player, player.getUsedItemHand(), blowpipe, berries, breath * FULL_SPEED, 1.0F, false, null);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS,
                1.0F, 1.0F + breath * 0.4F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack berry, boolean isCrit) {
        return new BlowpipeBerry(level, shooter, berry);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile berry, int index, float speed, float uncertainty, float angle,
                                   @Nullable LivingEntity target) {
        berry.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, speed, uncertainty);
    }

    @Override
    public int getUseDuration(ItemStack blowpipe, LivingEntity user) {
        return 72000;
    }

    /** The arm raises the pipe to the mouth, as it does a goat horn. */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack blowpipe) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return BERRIES;
    }

    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack blowpipe) {
        return RRItems.MOSSBERRY.toStack();
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }
}
