package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.MossyRamRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * The Mossy Prophet and the Mossy Warrior: a boxy body on short legs with a small ram's head on top of its front.
 * Their shapes are the Blockbench projects {@code mossy_prophet.bbmodel} and {@code mossy_warrior.bbmodel}; this
 * class moves their bones. Each has a second layer for the moss, the same boxes a little larger: the moss lies
 * on the body like wool on a sheep, and the texture decides how far it hangs down the sides.
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
        return BlockbenchModel.layer("mossy_prophet", deformation, MossyRamModel::new);
    }

    /** The Prophet at a third of its size, with fewer boxes: horns of three bars and no tips. */
    public static LayerDefinition createWarrior(CubeDeformation deformation) {
        return BlockbenchModel.layer("mossy_warrior", deformation, MossyRamModel::new);
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
