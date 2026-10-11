package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.model.MossyRamModel;
import ioann.uwu.runeruin.entities.MossyRam;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

public class MossyRamRenderer extends MobRenderer<MossyRam, MossyRamRenderState, MossyRamModel> {
    private final Identifier texture;

    public MossyRamRenderer(EntityRendererProvider.Context context, ModelLayerLocation main, ModelLayerLocation moss, float shadowRadius) {
        super(context, new MossyRamModel(context.bakeLayer(main)), shadowRadius);
        String name = main.model().getPath();
        this.texture = RR.id("textures/entity/" + name + "/" + name + ".png");

        MossyRamModel mossModel = new MossyRamModel(context.bakeLayer(moss));
        Identifier mossTexture = RR.id("textures/entity/" + name + "/" + name + "_moss.png");
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, MossyRamRenderState state, float yRot, float xRot) {
                coloredCutoutModelCopyLayerRender(mossModel, mossTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
            }
        });
    }

    @Override
    public MossyRamRenderState createRenderState() {
        return new MossyRamRenderState();
    }

    @Override
    public void extractRenderState(MossyRam entity, MossyRamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnim = entity.getAttackAnim(partialTicks);
        state.crouch = entity.getCrouch(partialTicks);
        state.raise = entity.getRaise(partialTicks);
        state.spin = entity.getSpin(partialTicks);
    }

    @Override
    public Identifier getTextureLocation(MossyRamRenderState state) {
        return this.texture;
    }
}
