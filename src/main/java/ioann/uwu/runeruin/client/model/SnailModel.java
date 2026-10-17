package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** The snail; its shape is the Blockbench project {@code snail.bbmodel}. */
public class SnailModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(RR.id("snail"), "main");

    public SnailModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return BlockbenchModel.layer("snail", SnailModel::new);
    }
}
