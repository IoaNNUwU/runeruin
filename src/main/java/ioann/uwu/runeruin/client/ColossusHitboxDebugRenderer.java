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
import net.minecraft.util.Mth;
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
 * shockwave is its ring that hurts, a hollow cylinder as high as it hurts: red over the ground, orange over the
 * colossus, where it hurts only what stands on it, as high as the zone of the blow.</p>
 */
public class ColossusHitboxDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int RING_POSTS = 12;

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
                    double y = shockwave.origin().y;
                    if (!shockwave.onBody()) {
                        drawRing(shockwave, y - Colossus.Shockwave.GROUND_BELOW, y + Colossus.Shockwave.GROUND_ABOVE, ARGB.colorFromFloat(1.0F, 1.0F, 0.1F, 0.1F));
                    }
                    if (shockwave.reachesColossus()) {
                        drawRing(shockwave, shockwave.colossusBottom(), shockwave.colossusTop(), ARGB.colorFromFloat(0.8F, 1.0F, 0.55F, 0.0F));
                    }
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

    /**
     * The ring of a shockwave that hurts, as a hollow cylinder between two heights: its circles at the bottom, at the
     * top and where it started, and posts around it.
     */
    private static void drawRing(Colossus.Shockwave shockwave, double bottom, double top, int color) {
        Vec3 origin = shockwave.origin();
        float outer = shockwave.radius();
        float inner = Math.max(0.0F, outer - (float) Colossus.Shockwave.WIDTH);
        for (double y : new double[]{bottom, Mth.clamp(origin.y, bottom, top), top}) {
            Vec3 at = new Vec3(origin.x, y, origin.z);
            Gizmos.circle(at, outer, GizmoStyle.stroke(color));
            Gizmos.circle(at, inner, GizmoStyle.stroke(color));
        }
        for (int post = 0; post < RING_POSTS; post++) {
            double angle = Mth.TWO_PI * post / RING_POSTS;
            for (float radius : new float[]{inner, outer}) {
                double x = origin.x + Math.cos(angle) * radius;
                double z = origin.z + Math.sin(angle) * radius;
                Gizmos.line(new Vec3(x, bottom, z), new Vec3(x, top, z), color);
            }
        }
    }
}
