package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.entities.GrapplingAnchor;
import ioann.uwu.runeruin.items.RRItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jspecify.annotations.Nullable;

/**
 * While a grappling hook in the main hand or its pair in the other one has its anchor in a block, the left mouse button
 * lets out their chains instead of hitting. Only the client of the player knows how long its chains are.
 */
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public class GrapplingHookControls {

    @SubscribeEvent
    static void letOutInsteadOfHitting(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && !hookedChains(Minecraft.getInstance().player).isEmpty()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    static void letOut(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyAttack.isDown()) {
            hookedChains(minecraft.player).forEach(GrapplingAnchor::letOut);
        }
    }

    private static List<GrapplingAnchor> hookedChains(@Nullable Player player) {
        if (player == null || !player.getMainHandItem().is(RRItems.GRAPPLING_HOOK)) {
            return List.of();
        }
        return GrapplingAnchor.inHands(player).stream().filter(GrapplingAnchor::hooksBlock).toList();
    }
}
