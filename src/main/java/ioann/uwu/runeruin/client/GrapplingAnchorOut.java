package ioann.uwu.runeruin.client;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.entities.GrapplingAnchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Whether the anchor of the grappling hook in the hand is out: then the hook shows none at its muzzle. */
public record GrapplingAnchorOut() implements ConditionalItemModelProperty {

    public static final MapCodec<GrapplingAnchorOut> MAP_CODEC = MapCodec.unit(new GrapplingAnchorOut());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        if (owner == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (owner.getItemInHand(hand) == stack && GrapplingAnchor.of(owner, hand) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MapCodec<GrapplingAnchorOut> type() {
        return MAP_CODEC;
    }
}
