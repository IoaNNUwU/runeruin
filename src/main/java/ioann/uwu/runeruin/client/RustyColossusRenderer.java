package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.model.RustyColossusModel;
import ioann.uwu.runeruin.entities.RustyColossus;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** The colossus at {@link RustyColossus#SCALE} times the size of its model, with its moss and its glowing eyes. */
public class RustyColossusRenderer extends MobRenderer<RustyColossus, RustyColossusRenderState, RustyColossusModel> {
    private static final Identifier TEXTURE = RR.id("textures/entity/rusty_colossus/rusty_colossus.png");
    private static final Identifier MOSS_TEXTURE = RR.id("textures/entity/rusty_colossus/rusty_colossus_moss.png");
    private static final RenderType EYES = RenderTypes.eyes(RR.id("textures/entity/rusty_colossus/rusty_colossus_eyes.png"));

    public RustyColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new RustyColossusModel(context.bakeLayer(RustyColossusModel.MAIN)), 12.0F);
        RustyColossusModel mossModel = new RustyColossusModel(context.bakeLayer(RustyColossusModel.MOSS));
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, RustyColossusRenderState state, float yRot, float xRot) {
                coloredCutoutModelCopyLayerRender(mossModel, MOSS_TEXTURE, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
            }
        });
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public RustyColossusRenderState createRenderState() {
        return new RustyColossusRenderState();
    }

    @Override
    public void extractRenderState(RustyColossus entity, RustyColossusRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.stride = entity.getStride(partialTicks);
        state.walkWeight = entity.getWalkWeight(partialTicks);
        state.action = entity.getAction();
        state.actionSeconds = entity.getActionSeconds(partialTicks);
    }

    @Override
    protected void scale(RustyColossusRenderState state, PoseStack poseStack) {
        poseStack.scale(RustyColossus.SCALE, RustyColossus.SCALE, RustyColossus.SCALE);
    }

    // Its box is only its feet: the colossus is drawn while any of its body is in view.
    @Override
    protected AABB getBoundingBoxForCulling(RustyColossus entity) {
        return entity.getBodyBox();
    }

    @Override
    public Identifier getTextureLocation(RustyColossusRenderState state) {
        return TEXTURE;
    }
}
