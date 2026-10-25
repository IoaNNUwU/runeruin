package ioann.uwu.runeruin.client;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.entities.GrapplingAnchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Whether the anchor of the grappling hook in the hand is out: then the hook shows none at its muzzle. */
public record GrapplingAnchorOut() implements ConditionalItemModelProperty {

    public static final MapCodec<GrapplingAnchorOut> MAP_CODEC = MapCodec.unit(new GrapplingAnchorOut());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return owner instanceof Player player && GrapplingAnchor.of(player) != null
                && player.getItemHeldByArm(GrapplingAnchorRenderer.holdingArm(player)) == stack;
    }

    @Override
    public MapCodec<GrapplingAnchorOut> type() {
        return MAP_CODEC;
    }
}
