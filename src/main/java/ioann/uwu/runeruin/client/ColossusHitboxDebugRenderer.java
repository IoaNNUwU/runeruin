package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.entities.Colossus;
import ioann.uwu.runeruin.entities.ColossusPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The parts of the Colossus with the hitboxes (F3+B), in the colour vanilla gives the parts of the Ender Dragon, the
 * only ones it draws: faint the box where a hit lands, bright the turned cube that collides.
 */
public class ColossusHitboxDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public ColossusHitboxDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        if (this.minecraft.level == null || !this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
            return;
        }
        for (Entity entity : this.minecraft.level.entitiesForRendering()) {
            if (entity instanceof Colossus colossus && frustum.isVisible(colossus.getBodyBox())) {
                for (PartEntity<?> part : colossus.getParts()) {
                    AABB box = part.getBoundingBox();
                    if (part instanceof ColossusPart cube && frustum.isVisible(box)) {
                        Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.colorFromFloat(0.35F, 0.25F, 1.0F, 0.0F)));
                        // The cube itself, which is what collides.
                        Vec3[] corners = cube.corners();
                        for (int c = 0; c < 8; c++) {
                            for (int bit = 1; bit < 8; bit <<= 1) {
                                if ((c & bit) == 0) {
                                    Gizmos.line(corners[c], corners[c | bit], ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
