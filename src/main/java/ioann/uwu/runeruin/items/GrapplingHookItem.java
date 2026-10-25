package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.entities.GrapplingAnchor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Drawn and released like a bow, shoots its anchor on a chain ({@link GrapplingAnchor}); used again, pulls the
 * anchor back.
 */
public class GrapplingHookItem extends Item {

    /** Blocks per tick of an anchor shot at full draw: it reaches the end of the chain before it drops much. */
    private static final float MAX_SPEED = 2.5F;

    public GrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        GrapplingAnchor anchor = GrapplingAnchor.of(player);
        if (anchor != null) {
            if (!level.isClientSide()) {
                anchor.discard();
            }
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        float power = BowItem.getPowerForTime(getUseDuration(stack, entity) - remainingTime);
        if (power < 0.1F || !(entity instanceof Player player)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(
                    (projectileLevel, owner, _) -> new GrapplingAnchor(projectileLevel, owner),
                    serverLevel, stack, player, 0.0F, power * MAX_SPEED, 0.5F);
            stack.hurtAndBreak(1, player, player.getUsedItemHand());
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                1.0F, 0.8F + power * 0.2F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }
}
