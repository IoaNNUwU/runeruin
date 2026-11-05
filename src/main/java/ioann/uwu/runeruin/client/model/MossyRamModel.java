package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.MossyRamRenderState;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * The Mossy Prophet and the Mossy Warrior: a boxy body on short legs with a small ram's head on top of its front.
 * Their shapes and animations are the Blockbench projects {@code mossy_prophet.bbmodel} and
 * {@code mossy_warrior.bbmodel}; the animations are exported from Blockbench into {@link MossyProphetAnimation}
 * and {@link MossyWarriorAnimation}. The moss is boxes of the project too, in the bones it grows on: a coat over the
 * back, lumps, a tuft on the head, a beard and moss on the shoulders; the texture decides how far it hangs down.
 *
 * <p>An animation follows the progress of a movement that the mob counts: its first frame is the start of the
 * movement, its last frame the full pose. The walk loops with the distance walked.</p>
 */
public class MossyRamModel extends EntityModel<MossyRamRenderState> {
    public static final ModelLayerLocation PROPHET = new ModelLayerLocation(RR.id("mossy_prophet"), "main");
    public static final ModelLayerLocation WARRIOR = new ModelLayerLocation(RR.id("mossy_warrior"), "main");

    /** A second of the walk is one stride, the swing of the vanilla mobs' legs: {@code cos(0.6662 * distance)}. */
    private static final float STRIDES = 1000.0F * 0.6662F / (50.0F * Mth.TWO_PI);

    private final ModelPart head;
    private final KeyframeAnimation walk;
    private final Movement punch;
    private final Movement raise;
    private final Movement hold;
    private final Movement crouch;
    private final Movement somersault;

    private MossyRamModel(ModelPart root, AnimationDefinition walk, AnimationDefinition punch, AnimationDefinition raise,
                          AnimationDefinition hold, AnimationDefinition crouch, AnimationDefinition somersault) {
        super(root);
        this.head = root.getChild("whole").getChild("body").getChild("head");
        this.walk = walk.bake(root);
        this.punch = new Movement(root, punch);
        this.raise = new Movement(root, raise);
        this.hold = new Movement(root, hold);
        this.crouch = new Movement(root, crouch);
        this.somersault = new Movement(root, somersault);
    }

    public static MossyRamModel prophet(ModelPart root) {
        return new MossyRamModel(root, MossyProphetAnimation.walk, MossyProphetAnimation.punch, MossyProphetAnimation.raise,
                MossyProphetAnimation.hold, MossyProphetAnimation.crouch, MossyProphetAnimation.somersault);
    }

    /** The Prophet at a third of its size, with fewer boxes. */
    public static MossyRamModel warrior(ModelPart root) {
        return new MossyRamModel(root, MossyWarriorAnimation.walk, MossyWarriorAnimation.punch, MossyWarriorAnimation.raise,
                MossyWarriorAnimation.hold, MossyWarriorAnimation.crouch, MossyWarriorAnimation.somersault);
    }

    public static LayerDefinition createProphet() {
        return BlockbenchModel.layer("mossy_prophet", MossyRamModel::prophet);
    }

    public static LayerDefinition createWarrior() {
        return BlockbenchModel.layer("mossy_warrior", MossyRamModel::warrior);
    }

    @Override
    public void setupAnim(MossyRamRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.walk.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, STRIDES, 1.0F);
        this.punch.show(state.attackAnim);
        this.raise.show(state.raise);
        this.hold.show(state.hold);
        this.crouch.show(state.crouch);
        this.somersault.show(state.spin / Mth.TWO_PI);
    }

    /** An animation shown at the progress of a movement: 0 is its first frame, 1 its last. */
    private record Movement(KeyframeAnimation animation, float length) {
        Movement(ModelPart root, AnimationDefinition definition) {
            this(definition.bake(root), definition.lengthInSeconds());
        }

        void show(float progress) {
            this.animation.apply((long) (progress * this.length * 1000.0F), 1.0F);
        }
    }
}
