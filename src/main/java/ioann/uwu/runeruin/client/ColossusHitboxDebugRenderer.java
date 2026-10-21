package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.entities.Colossus;
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
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The boxes of the parts of the Colossus with the hitboxes (F3+B): vanilla draws the parts of the Ender Dragon only.
 * They are drawn in the colour vanilla gives the dragon's parts.
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
                    if (frustum.isVisible(box)) {
                        Gizmos.cuboid(box, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F)));
                    }
                }
            }
        }
    }
}
