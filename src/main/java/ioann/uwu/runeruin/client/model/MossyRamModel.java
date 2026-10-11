package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.MossyRamRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Mossy Prophet and the Mossy Warrior: a boxy body on short legs with a small ram's head on top of its front.
 * Each has a second layer for the moss, the same boxes a little larger: the moss lies on the body like wool on a
 * sheep, and the texture decides how far it hangs down the sides.
 */
public class MossyRamModel extends EntityModel<MossyRamRenderState> {
    public static final ModelLayerLocation PROPHET = new ModelLayerLocation(RR.id("mossy_prophet"), "main");
    public static final ModelLayerLocation PROPHET_MOSS = new ModelLayerLocation(RR.id("mossy_prophet"), "moss");
    public static final ModelLayerLocation WARRIOR = new ModelLayerLocation(RR.id("mossy_warrior"), "main");
    public static final ModelLayerLocation WARRIOR_MOSS = new ModelLayerLocation(RR.id("mossy_warrior"), "moss");

    public static final CubeDeformation PROPHET_MOSS_DEPTH = new CubeDeformation(1.0F);
    public static final CubeDeformation WARRIOR_MOSS_DEPTH = new CubeDeformation(0.5F);

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public MossyRamModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createProphet(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -14.0F, -12.0F, 32.0F, 28.0F, 24.0F, deformation),
                PartPose.offset(0.0F, 2.0F, 0.0F)
        );
        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(64, 52).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 8.0F, deformation)
                        .texOffs(100, 52).addBox(-3.0F, -5.0F, -11.0F, 6.0F, 5.0F, 5.0F, deformation),
                PartPose.offset(0.0F, -14.0F, -8.0F)
        );
        for (boolean left : new boolean[] {false, true}) {
            // Curled into a ring in the side view: from the top of the head to the back, down, forward again and
            // up to the tip, each bar further out than the one before: from the front the horn sweeps aside.
            CubeListBuilder horn = CubeListBuilder.create().mirror(left);
            bar(horn, left, 0, 82, -2.0F, -5.0F, -5.0F, 5.0F, 5.0F, 17.0F, deformation);
            bar(horn, left, 44, 82, 1.0F, 0.0F, 7.0F, 5.0F, 10.0F, 5.0F, deformation);
            bar(horn, left, 64, 82, 4.0F, 5.0F, -5.0F, 5.0F, 5.0F, 12.0F, deformation);
            bar(horn, left, 98, 82, 7.0F, 1.0F, -5.0F, 4.0F, 4.0F, 4.0F, deformation);
            head.addOrReplaceChild(left ? "left_horn" : "right_horn", horn, PartPose.offset(left ? 5.0F : -5.0F, -10.0F, 0.0F));
        }
        body.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(0, 52).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 22.0F, 8.0F, deformation),
                PartPose.offset(-20.0F, -10.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(0, 52).mirror().addBox(-4.0F, -2.0F, -4.0F, 8.0F, 22.0F, 8.0F, deformation),
                PartPose.offset(20.0F, -10.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(32, 52).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation),
                PartPose.offset(-8.0F, 16.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(32, 52).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 8.0F, 8.0F, deformation),
                PartPose.offset(8.0F, 16.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    /** The Prophet at a third of its size, with fewer boxes: horns of three bars and no tips. */
    public static LayerDefinition createWarrior(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -4.0F, 10.0F, 8.0F, 8.0F, deformation),
                PartPose.offset(0.0F, 18.0F, 0.0F)
        );
        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-3.0F, -6.0F, -4.0F, 6.0F, 6.0F, 5.0F, deformation)
                        .texOffs(36, 11).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 3.0F, 2.0F, deformation),
                PartPose.offset(0.0F, -4.0F, -2.0F)
        );
        for (boolean left : new boolean[] {false, true}) {
            CubeListBuilder horn = CubeListBuilder.create().mirror(left);
            bar(horn, left, 24, 16, 0.0F, -1.0F, -2.0F, 2.0F, 2.0F, 7.0F, deformation);
            bar(horn, left, 42, 16, 1.0F, 1.0F, 3.0F, 2.0F, 4.0F, 2.0F, deformation);
            bar(horn, left, 50, 16, 2.0F, 3.0F, -1.0F, 2.0F, 2.0F, 4.0F, deformation);
            head.addOrReplaceChild(left ? "left_horn" : "right_horn", horn, PartPose.offset(left ? 3.0F : -3.0F, -6.0F, -1.0F));
        }
        body.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F, deformation),
                PartPose.offset(-6.5F, -2.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F, deformation),
                PartPose.offset(6.5F, -2.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F, deformation),
                PartPose.offset(-2.5F, 22.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(12, 16).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F, deformation),
                PartPose.offset(2.5F, 22.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    /** A bar of a horn that starts {@code out} away from the side of the head. */
    private static void bar(CubeListBuilder horn, boolean left, int u, int v, float out, float y, float z, float width, float height, float depth,
                            CubeDeformation deformation) {
        horn.texOffs(u, v).addBox(left ? out : -out - width, y, z, width, height, depth, deformation);
    }

    @Override
    public void setupAnim(MossyRamRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float step = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
        this.rightLeg.xRot = step * 1.2F;
        this.leftLeg.xRot = -step * 1.2F;
        // A punch: both fists go up in front of the body and come down.
        float punch = Mth.sin(state.attackAnim * Mth.PI) * 2.2F;
        this.rightArm.xRot = -step * 0.6F - punch;
        this.leftArm.xRot = step * 0.6F - punch;

        // The arms go up and apart to call warriors and through a leap; holding a warrior, they go on past
        // the vertical and lean in over the head.
        float lift = state.raise * 2.5F + state.hold * 3.66F;
        this.rightArm.zRot = lift;
        this.leftArm.zRot = -lift;
        // The body sinks onto the legs before a leap and after it.
        this.body.y += state.crouch * 4.0F;
        this.body.xRot = state.crouch * 0.1F;

        // A somersault, forward around the middle of the body.
        float middle = this.body.getInitialPose().y();
        this.root.xRot = state.spin;
        this.root.y = middle * (1.0F - Mth.cos(state.spin));
        this.root.z = -middle * Mth.sin(state.spin);
    }
}
