package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.RustyColossusRenderState;
import ioann.uwu.runeruin.entities.ColossusRig;
import ioann.uwu.runeruin.entities.RustyColossus;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The Rusty Colossus: a machine of rusty plates, its arms and legs bending at the elbows and knees. Its shape and
 * animations are the Blockbench project {@code rusty_colossus.bbmodel}, the animations exported into
 * {@link RustyColossusAnimation}; a second layer, the same boxes a little larger, carries the moss of its shoulders,
 * head and chest.
 *
 * <p>The server moves the solid parts of the colossus with the animations of the project itself
 * ({@link ColossusRig}). So that both play the same movement, building the model compares the exported class with
 * the project and fails when the animations were changed in Blockbench but not exported.</p>
 */
public class RustyColossusModel extends EntityModel<RustyColossusRenderState> {
    public static final ModelLayerLocation MAIN = new ModelLayerLocation(RR.id("rusty_colossus"), "main");
    public static final ModelLayerLocation MOSS = new ModelLayerLocation(RR.id("rusty_colossus"), "moss");
    public static final CubeDeformation MOSS_DEPTH = new CubeDeformation(0.25F);

    private static final Map<String, AnimationDefinition> EXPORTED = Map.of(
            "walk", RustyColossusAnimation.walk,
            "idle", RustyColossusAnimation.idle,
            "slam_high", RustyColossusAnimation.slam_high,
            "slam_ground", RustyColossusAnimation.slam_ground);

    private final ModelPart head;
    private final KeyframeAnimation idle;
    private final KeyframeAnimation walk;
    private final float walkLength;
    private final KeyframeAnimation slamHigh;
    private final KeyframeAnimation slamGround;

    public RustyColossusModel(ModelPart root) {
        super(root);
        this.head = root.getChild("whole").getChild("body").getChild("head");
        this.idle = RustyColossusAnimation.idle.bake(root);
        this.walk = RustyColossusAnimation.walk.bake(root);
        this.walkLength = RustyColossusAnimation.walk.lengthInSeconds();
        this.slamHigh = RustyColossusAnimation.slam_high.bake(root);
        this.slamGround = RustyColossusAnimation.slam_ground.bake(root);
    }

    public static LayerDefinition create(CubeDeformation deformation) {
        return BlockbenchModel.layer("rusty_colossus", deformation, root -> {
            matchProject(root);
            return new RustyColossusModel(root);
        });
    }

    @Override
    public void setupAnim(RustyColossusRenderState state) {
        super.setupAnim(state);
        this.idle.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        this.walk.apply((long) (RustyColossus.walkSeconds(state.stride, this.walkLength) * 1000.0F), state.walkWeight);
        switch (state.action) {
            case SLAM_HIGH -> this.slamHigh.apply((long) (state.actionSeconds * 1000.0F), 1.0F);
            case SLAM_GROUND -> this.slamGround.apply((long) (state.actionSeconds * 1000.0F), 1.0F);
            case NONE -> {
            }
        }
        this.head.yRot += Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
    }

    /** Fails unless every exported animation moves every bone as the animation of the same name in the project. */
    private static void matchProject(ModelPart root) {
        ColossusRig rig = RustyColossus.rig();
        Function<String, @Nullable ModelPart> parts = root.createPartLookup();
        for (Map.Entry<String, AnimationDefinition> entry : EXPORTED.entrySet()) {
            ColossusRig.Animation project = rig.animation(entry.getKey());
            KeyframeAnimation exported = entry.getValue().bake(root);
            for (int step = 0; step <= 40; step++) {
                // The game plays an animation in whole milliseconds.
                long millis = (long) (project.length() * step / 40.0F * 1000.0F);
                float seconds = millis / 1000.0F;
                float[][] pose = rig.restPose();
                rig.apply(project, seconds, 1.0F, pose);
                root.getAllParts().forEach(ModelPart::resetPose);
                exported.apply(millis, 1.0F);
                for (int bone = 0; bone < rig.boneCount(); bone++) {
                    ModelPart part = Objects.requireNonNull(parts.apply(rig.boneName(bone)), rig.boneName(bone));
                    PartPose rest = part.getInitialPose();
                    float[] moved = {part.x - rest.x(), part.y - rest.y(), part.z - rest.z(),
                            part.xRot - rest.xRot(), part.yRot - rest.yRot(), part.zRot - rest.zRot()};
                    for (int i = 0; i < 6; i++) {
                        if (Math.abs(moved[i] - pose[bone][i]) > 1.0E-3F) {
                            throw new IllegalStateException("the animation '" + entry.getKey() + "' moves the bone '"
                                    + rig.boneName(bone) + "' otherwise than RustyColossusAnimation does at " + seconds
                                    + " s: export the animations again (File, Export, Export Modded Entity Animations)");
                        }
                    }
                }
            }
        }
        root.getAllParts().forEach(ModelPart::resetPose);
    }
}
