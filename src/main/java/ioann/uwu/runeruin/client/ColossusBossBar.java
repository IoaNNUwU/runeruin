package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.entities.Colossus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * The health bar of a Colossus, split into stages like the bar of a raid: one for each crystal, which empties only
 * as that crystal is hit. The bar has the id of its colossus, which the client draws it from.
 *
 * <p>The game draws the next boss bar where this one is, because a bar drawn instead of the game's does not move the
 * next one down (NeoForge's BossEventProgress); two bosses at once are rare.</p>
 */
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public final class ColossusBossBar {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("boss_bar/white_background");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("boss_bar/white_progress");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    // The line between two stages, in the dark grey of the frame of the bar.
    private static final int BORDER = 0xFF262626;

    private ColossusBossBar() {
    }

    @SubscribeEvent
    static void drawStages(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Minecraft minecraft = Minecraft.getInstance();
        LerpingBossEvent bar = event.getBossEvent();
        if (minecraft.level == null || !(minecraft.level.getEntity(bar.getId()) instanceof Colossus colossus)) {
            return;
        }
        event.setCanceled(true);
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int x = event.getX();
        int y = event.getY();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, WIDTH, HEIGHT, 0, 0, x, y, WIDTH, HEIGHT);
        int stages = Colossus.rig().crystalCount();
        for (int stage = 0; stage < stages; stage++) {
            int from = WIDTH * stage / stages;
            int left = Mth.lerpDiscrete(colossus.crystal(stage), 0, WIDTH * (stage + 1) / stages - from);
            if (left > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, WIDTH, HEIGHT, from, 0, x + from, y, left, HEIGHT);
            }
            if (stage > 0) {
                graphics.fill(x + from, y, x + from + 1, y + HEIGHT, BORDER);
            }
        }
        Component name = bar.getName();
        graphics.text(minecraft.font, name, x + WIDTH / 2 - minecraft.font.width(name) / 2, y - 9, -1);
    }
}
