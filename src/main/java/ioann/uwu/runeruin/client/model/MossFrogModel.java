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

/**
 * A sitting frog: the back slopes down from the shoulders to the rump, the muzzle slopes down to the lips and the
 * hind legs lie folded along the body. The moss parts lie on the head and the back as a second layer with a
 * fringe hanging from its edges; only the boss wears them.
 */
public class MossFrogModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(RR.id("moss_frog"), "main");

    /** How far the body leans back; the head and the arms turn the other way to stay level. */
    private static final float BODY_PITCH = -0.35F;

    public MossFrogModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -8.0F, -14.0F, 14.0F, 8.0F, 10.0F)
                        .texOffs(0, 18).addBox(-6.0F, -7.0F, -4.0F, 12.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 6.0F, BODY_PITCH, 0.0F, 0.0F)
        );
        PartDefinition bodyMoss = body.addOrReplaceChild(
                "moss",
                CubeListBuilder.create()
                        .texOffs(0, 98).addBox(-7.5F, -9.5F, -14.5F, 15.0F, 2.0F, 11.0F)
                        .texOffs(0, 112).addBox(-6.5F, -8.5F, -3.5F, 13.0F, 2.0F, 7.0F)
                        .texOffs(56, 98).addBox(7.6F, -8.5F, -14.5F, 0.0F, 6.0F, 11.0F)
                        .texOffs(78, 98).addBox(-7.6F, -8.5F, -14.5F, 0.0F, 6.0F, 11.0F)
                        .texOffs(100, 98).addBox(6.6F, -7.5F, -3.5F, 0.0F, 5.0F, 7.0F)
                        .texOffs(114, 98).addBox(-6.6F, -7.5F, -3.5F, 0.0F, 5.0F, 7.0F)
                        .texOffs(44, 116).addBox(-6.5F, -7.5F, 3.6F, 13.0F, 5.0F, 0.0F),
                PartPose.ZERO
        );

        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-7.0F, -4.0F, -5.0F, 14.0F, 6.0F, 8.0F)
                        // The upper jaw under the sloping snout.
                        .texOffs(64, 60).addBox(-6.0F, -1.0F, -11.0F, 12.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, -13.0F, 0.25F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "snout",
                CubeListBuilder.create().texOffs(0, 46).addBox(-6.0F, 0.0F, -7.0F, 12.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, -5.0F, 0.4F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create().texOffs(0, 56).addBox(-6.5F, 0.0F, -13.0F, 13.0F, 2.0F, 13.0F),
                PartPose.offset(0.0F, 1.0F, 1.5F)
        );
        head.addOrReplaceChild(
                "throat",
                CubeListBuilder.create().texOffs(0, 71).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 3.0F, -3.0F)
        );
        head.addOrReplaceChild(
                "left_eye",
                CubeListBuilder.create().texOffs(100, 60).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(5.5F, -4.0F, -2.0F)
        );
        head.addOrReplaceChild(
                "right_eye",
                CubeListBuilder.create().texOffs(100, 69).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(-5.5F, -4.0F, -2.0F)
        );
        head.addOrReplaceChild(
                "moss",
                CubeListBuilder.create()
                        .texOffs(0, 84).addBox(-7.5F, -5.5F, -5.5F, 15.0F, 2.0F, 9.0F)
                        .texOffs(48, 84).addBox(7.6F, -4.5F, -5.5F, 0.0F, 5.0F, 9.0F)
                        .texOffs(66, 84).addBox(-7.6F, -4.5F, -5.5F, 0.0F, 5.0F, 9.0F)
                        .texOffs(84, 84).addBox(-7.5F, -4.5F, -5.6F, 15.0F, 3.0F, 0.0F),
                PartPose.ZERO
        );

        addArm(body, "left", 1.0F, 64, 64, 88, 0);
        addArm(body, "right", -1.0F, 76, 72, 88, 6);
        addLeg(root, "left", 1.0F, 48, 48, 52);
        addLeg(root, "right", -1.0F, 78, 72, 88);

        return LayerDefinition.create(mesh, 128, 128);
    }

    /** The elbow points back and out, the forearm comes forward again and the hand lies flat on the ground. */
    private static void addArm(PartDefinition body, String side, float sign, int armU, int forearmU, int handU, int handV) {
        PartDefinition arm = body.addOrReplaceChild(
                side + "_arm",
                CubeListBuilder.create().texOffs(armU, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(sign * 6.5F, -2.0F, -11.0F, 0.35F - BODY_PITCH, 0.0F, sign * -0.2F)
        );
        PartDefinition forearm = arm.addOrReplaceChild(
                side + "_forearm",
                CubeListBuilder.create().texOffs(forearmU, 7).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, -0.75F, 0.0F, sign * 0.2F)
        );
        forearm.addOrReplaceChild(
                side + "_hand",
                CubeListBuilder.create().texOffs(handU, handV).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 0.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 4.2F, 0.0F, 0.44F, sign * -0.3F, sign * -0.14F)
        );
    }

    /** The thigh runs forward and out to the knee, the shin back along the ground and the foot forward again. */
    private static void addLeg(PartDefinition root, String side, float sign, int thighU, int shinU, int footU) {
        PartDefinition thigh = root.addOrReplaceChild(
                side + "_leg",
                CubeListBuilder.create().texOffs(thighU, 18).addBox(sign > 0.0F ? 0.0F : -4.0F, -2.5F, -10.0F, 4.0F, 5.0F, 11.0F),
                PartPose.offsetAndRotation(sign * 6.0F, -5.0F, 6.0F, 0.2F, sign * -0.35F, 0.0F)
        );
        PartDefinition shin = thigh.addOrReplaceChild(
                side + "_shin",
                CubeListBuilder.create().texOffs(shinU, 34).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 9.0F),
                PartPose.offsetAndRotation(sign * 2.0F, 0.5F, -9.0F, -0.32F, sign * 0.2F, 0.0F)
        );
        shin.addOrReplaceChild(
                side + "_foot",
                CubeListBuilder.create().texOffs(footU, 46).addBox(-3.5F, 0.0F, -10.0F, 7.0F, 0.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 1.67F, 8.5F, 0.12F, sign * -0.15F, sign * -0.06F)
        );
    }
}
