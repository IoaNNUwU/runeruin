package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.entities.Colossus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Turns the player who stands on a Colossus with its body, a little every frame. The camera takes the player's turn
 * as it is, without easing it between ticks as it eases the body of the colossus, so a turn once a tick would jerk.
 */
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public final class ColossusRiderView {
    private static @Nullable Colossus colossus;
    private static float bodyRot;

    private ColossusRiderView() {
    }

    @SubscribeEvent
    static void turnWithColossus(RenderFrameEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        Colossus carrying = player == null ? null : Colossus.carryingLocalPlayer();
        if (carrying == null) {
            colossus = null;
            return;
        }
        float body = Mth.rotLerp(event.getPartialTick().getGameTimeDeltaPartialTick(true), carrying.yBodyRotO, carrying.yBodyRot);
        if (carrying == colossus) {
            float turn = Mth.wrapDegrees(body - bodyRot);
            player.setYRot(player.getYRot() + turn);
            player.yRotO += turn;
            player.setYHeadRot(player.getYHeadRot() + turn);
        }
        colossus = carrying;
        bodyRot = body;
    }
}
