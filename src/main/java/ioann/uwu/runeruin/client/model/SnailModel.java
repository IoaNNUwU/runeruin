package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SnailModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(RR.id("snail"), "main");

    public SnailModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 8.0F, -8.0F, 8.0F, 16.0F, 16.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 36).addBox(-4.0F, 12.0F, -12.0F, 8.0F, 8.0F, 4.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "left_antenna",
                CubeListBuilder.create().texOffs(24, 36).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 12.0F, -11.0F, 0.0F, 0.0F, -0.35F)
        );
        root.addOrReplaceChild(
                "right_antenna",
                CubeListBuilder.create().texOffs(34, 36).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, 12.0F, -11.0F, 0.0F, 0.0F, 0.35F)
        );

        root.addOrReplaceChild(
                "left_eye",
                CubeListBuilder.create().texOffs(44, 36).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-4.1F, 4.0F, -11.5F)
        );
        root.addOrReplaceChild(
                "right_eye",
                CubeListBuilder.create().texOffs(44, 42).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(4.1F, 4.0F, -11.5F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }
}
