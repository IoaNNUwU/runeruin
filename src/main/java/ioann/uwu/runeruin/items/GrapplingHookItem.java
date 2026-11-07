package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.entities.GrapplingAnchor;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A click shoots the anchor on a chain ({@link GrapplingAnchor}), of the hook in the main hand first, then of the one
 * in the other hand. While an anchor holds onto a block, holding the button reels in the chains of the hooks in hand,
 * and a click while sneaking lets go of the lowest anchor out: the one left behind when climbing hook over hook. The
 * left button lets the chains out ({@code GrapplingHookControls} on the client).
 */
public class GrapplingHookItem extends Item {

    /** Blocks per tick of a shot anchor: it reaches the end of the chain before it drops much. */
    private static final float SPEED = 4.0F;

    public GrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        List<GrapplingAnchor> out = GrapplingAnchor.of(player);
        if (player.isSecondaryUseActive() && !out.isEmpty()) {
            if (!level.isClientSide()) {
                Collections.min(out, Comparator.comparingDouble(Entity::getY)).discard();
            }
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        if (GrapplingAnchor.of(player, hand) == null) {
            shoot(level, player, hand);
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND && player.getOffhandItem().is(this) && GrapplingAnchor.of(player, InteractionHand.OFF_HAND) == null) {
            return InteractionResult.PASS;
        }
        if (GrapplingAnchor.inHands(player).stream().anyMatch(GrapplingAnchor::hooksBlock)) {
            player.startUsingItem(hand);
        }
        return InteractionResult.CONSUME;
    }

    private void shoot(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            int slot = GrapplingAnchor.slotOf(player, hand);
            Projectile.spawnProjectileFromRotation(
                    (projectileLevel, owner, _) -> new GrapplingAnchor(projectileLevel, owner, slot),
                    serverLevel, stack, player, 0.0F, SPEED, 0.5F);
            stack.hurtAndBreak(1, player, hand);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        GrapplingAnchor.inHands(entity).forEach(anchor -> anchor.reelIn(entity));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }
}
