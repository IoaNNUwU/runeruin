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
 *
 * <p>Its attacks too. Yellow, the zones where a target gets a blow onto the colossus, red the one it strikes now. Each
 * shockwave is its ring that hurts, between two circles, as high as it hurts: red over the ground, orange over the
 * colossus, where it hurts only what stands on it.</p>
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
                for (Colossus.Zone zone : Colossus.zones()) {
                    boolean striking = zone.action() == colossus.getAction();
                    drawTurned(colossus, zone.box(), striking ? ARGB.colorFromFloat(1.0F, 1.0F, 0.1F, 0.1F) : ARGB.colorFromFloat(0.6F, 1.0F, 0.9F, 0.1F));
                }
                for (Colossus.Shockwave shockwave : colossus.getShockwaves()) {
                    if (!shockwave.onBody()) {
                        drawRing(shockwave, -Colossus.Shockwave.GROUND_BELOW, Colossus.Shockwave.GROUND_ABOVE, ARGB.colorFromFloat(1.0F, 1.0F, 0.1F, 0.1F));
                    }
                    drawRing(shockwave, -shockwave.colossusBelow(), shockwave.colossusAbove(), ARGB.colorFromFloat(0.8F, 1.0F, 0.55F, 0.0F));
                }
            }
        }
    }

    /** A box given as if the colossus faced +z, turned with it. */
    private static void drawTurned(Colossus colossus, AABB box, int color) {
        Vec3[] corners = new Vec3[8];
        for (int c = 0; c < 8; c++) {
            corners[c] = colossus.toWorld(new Vec3((c & 1) == 0 ? box.minX : box.maxX, (c & 2) == 0 ? box.minY : box.maxY, (c & 4) == 0 ? box.minZ : box.maxZ));
        }
        for (int c = 0; c < 8; c++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                if ((c & bit) == 0) {
                    Gizmos.line(corners[c], corners[c | bit], color);
                }
            }
        }
    }

    /** The ring of a shockwave that hurts, from below to above where it started. */
    private static void drawRing(Colossus.Shockwave shockwave, double below, double above, int color) {
        float outer = shockwave.radius();
        float inner = Math.max(0.0F, outer - (float) Colossus.Shockwave.WIDTH);
        for (double dy : new double[]{below, above}) {
            Vec3 at = shockwave.origin().add(0.0, dy, 0.0);
            Gizmos.circle(at, outer, GizmoStyle.stroke(color));
            Gizmos.circle(at, inner, GizmoStyle.stroke(color));
        }
        Gizmos.line(shockwave.origin().add(0.0, below, 0.0), shockwave.origin().add(0.0, above, 0.0), color);
    }
}
