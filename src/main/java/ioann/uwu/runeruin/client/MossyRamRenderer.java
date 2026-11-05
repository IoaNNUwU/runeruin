package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.model.MossyRamModel;
import ioann.uwu.runeruin.entities.MossyRam;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class MossyRamRenderer extends MobRenderer<MossyRam, MossyRamRenderState, MossyRamModel> {
    private final Identifier texture;

    public MossyRamRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                            Function<ModelPart, MossyRamModel> model, float shadowRadius) {
        super(context, model.apply(context.bakeLayer(layer)), shadowRadius);
        String name = layer.model().getPath();
        this.texture = RR.id("textures/entity/" + name + "/" + name + ".png");
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
        state.hold = entity.getHold(partialTicks);
        state.spin = entity.getSpin(partialTicks);
    }

    @Override
    public Identifier getTextureLocation(MossyRamRenderState state) {
        return this.texture;
    }
}
