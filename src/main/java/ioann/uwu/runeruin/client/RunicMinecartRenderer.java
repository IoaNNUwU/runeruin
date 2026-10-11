package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ioann.uwu.runeruin.RR;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The vanilla minecart model with its own texture. The vanilla renderer draws its texture, a private constant,
 * in the same method that poses the minecart, so {@link #submit} repeats that method (Minecraft 26.2) without
 * the block a minecart can carry: a runic minecart has none.
 */
public class RunicMinecartRenderer extends MinecartRenderer {

    private static final Identifier TEXTURE = RR.id("textures/entity/minecart/runic_minecart.png");

    public RunicMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @Override
    public void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // EntityRenderer.submit: out of reach behind the vanilla method.
        if (state.leashStates != null) {
            for (EntityRenderState.LeashState leashState : state.leashStates) {
                collector.submitLeash(poseStack, leashState);
            }
        }
        this.submitNameDisplay(state, poseStack, collector, camera);

        poseStack.pushPose();
        long seed = state.offsetSeed;
        float offsetX = (((float) (seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetY = (((float) (seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float offsetZ = (((float) (seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(offsetX, offsetY, offsetZ);
        if (state.isNewRender) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-state.xRot));
            poseStack.translate(0.0F, 0.375F, 0.0F);
        } else {
            poseOnOldRails(state, poseStack);
        }

        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
    }

    /** AbstractMinecartRenderer.oldRender. */
    private static void poseOnOldRails(MinecartRenderState state, PoseStack poseStack) {
        float xRot = state.xRot;
        float rotation = state.yRot;
        if (state.posOnRail != null && state.frontPos != null && state.backPos != null) {
            Vec3 frontPos = state.frontPos;
            Vec3 backPos = state.backPos;
            poseStack.translate(state.posOnRail.x - state.x, (frontPos.y + backPos.y) / 2.0 - state.y, state.posOnRail.z - state.z);
            Vec3 direction = backPos.add(-frontPos.x, -frontPos.y, -frontPos.z);
            if (direction.length() != 0.0) {
                direction = direction.normalize();
                rotation = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI);
                xRot = (float) (Math.atan(direction.y) * 73.0);
            }
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-xRot));
    }
}
