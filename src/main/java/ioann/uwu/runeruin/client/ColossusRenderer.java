package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.model.ColossusModel;
import ioann.uwu.runeruin.entities.Colossus;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * The colossus at {@link Colossus#SCALE} times the size of its model, with its glowing eyes. A hit never turns it red,
 * only the crystal that was hit: a second model draws that crystal alone, red.
 */
public class ColossusRenderer extends MobRenderer<Colossus, ColossusRenderState, ColossusModel> {
    private static final Identifier TEXTURE = RR.id("textures/entity/colossus/colossus.png");
    private static final RenderType EYES = RenderTypes.eyes(RR.id("textures/entity/colossus/colossus_eyes.png"));

    public ColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new ColossusModel(context.bakeLayer(ColossusModel.LAYER), false), 12.0F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
        ColossusModel flashing = new ColossusModel(context.bakeLayer(ColossusModel.LAYER), true);
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ColossusRenderState state, float yRot, float xRot) {
                if (state.flashingCrystals != 0) {
                    collector.submitModel(flashing, state, poseStack, flashing.renderType(TEXTURE), lightCoords,
                            OverlayTexture.pack(0.0F, true), state.outlineColor, null);
                }
            }
        });
    }

    @Override
    public ColossusRenderState createRenderState() {
        return new ColossusRenderState();
    }

    @Override
    public void extractRenderState(Colossus entity, ColossusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.stride = entity.getStride(partialTicks);
        state.walkWeight = entity.getWalkWeight(partialTicks);
        state.action = entity.getAction();
        state.actionSeconds = entity.getActionSeconds(partialTicks);
        state.hasRedOverlay = false;
        state.wholeCrystals = 0;
        state.flashingCrystals = 0;
        for (int crystal = 0; crystal < Colossus.rig().crystalCount(); crystal++) {
            state.wholeCrystals |= entity.crystal(crystal) > 0.0F ? 1 << crystal : 0;
            state.flashingCrystals |= entity.isFlashing(crystal) ? 1 << crystal : 0;
        }
    }

    @Override
    protected void scale(ColossusRenderState state, PoseStack poseStack) {
        poseStack.scale(Colossus.SCALE, Colossus.SCALE, Colossus.SCALE);
    }

    // Its box is only its feet: the colossus is drawn while any of its body is in view.
    @Override
    protected AABB getBoundingBoxForCulling(Colossus entity) {
        return entity.getBodyBox();
    }

    @Override
    public Identifier getTextureLocation(ColossusRenderState state) {
        return TEXTURE;
    }
}
