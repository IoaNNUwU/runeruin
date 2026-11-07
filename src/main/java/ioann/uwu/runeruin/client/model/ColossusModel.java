package ioann.uwu.runeruin.client.model;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.ColossusRenderState;
import ioann.uwu.runeruin.entities.ColossusRig;
import ioann.uwu.runeruin.entities.Colossus;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.jspecify.annotations.Nullable;

/**
 * The Colossus: a giant of stone bricks, its arms and legs bending at the elbows and knees, moss growing on its head
 * and body and white crystals out of it. Its shape and animations are the Blockbench project {@code colossus.bbmodel},
 * the animations exported into {@link ColossusAnimation}.
 *
 * <p>The server moves the solid parts of the colossus with the animations of the project itself
 * ({@link ColossusRig}). So that both play the same movement, building the model compares the exported class with
 * the project and fails when the animations were changed in Blockbench but not exported.</p>
 */
public class ColossusModel extends EntityModel<ColossusRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RR.id("colossus"), "main");

    private static final Map<String, AnimationDefinition> EXPORTED = Map.of(
            "walk", ColossusAnimation.walk,
            "idle", ColossusAnimation.idle,
            "slam_high", ColossusAnimation.slam_high,
            "slam_ground", ColossusAnimation.slam_ground,
            "slam_head", ColossusAnimation.slam_head,
            "slam_face", ColossusAnimation.slam_face,
            "slam_shoulders", ColossusAnimation.slam_shoulders,
            "stomp_right", ColossusAnimation.stomp_right,
            "stomp_left", ColossusAnimation.stomp_left);

    private final KeyframeAnimation idle;
    private final KeyframeAnimation walk;
    private final float walkLength;
    private final Map<Colossus.Action, KeyframeAnimation> actions = new EnumMap<>(Colossus.Action.class);
    // The crystals in their order in the project: each is a part of its own, as a turned cube.
    private final ModelPart[] crystals;
    // Draws only the crystals that flash red after a hit, over the model that draws the rest.
    private final boolean flashing;

    public ColossusModel(ModelPart root, boolean flashing) {
        super(root);
        this.idle = ColossusAnimation.idle.bake(root);
        this.walk = ColossusAnimation.walk.bake(root);
        this.walkLength = ColossusAnimation.walk.lengthInSeconds();
        for (Colossus.Action action : Colossus.Action.values()) {
            if (action != Colossus.Action.NONE) {
                this.actions.put(action, Objects.requireNonNull(EXPORTED.get(action.animation), action.animation).bake(root));
            }
        }
        this.crystals = crystals(root);
        this.flashing = flashing;
        if (flashing) {
            Set<ModelPart> crystals = Set.of(this.crystals);
            root.getAllParts().forEach(part -> part.skipDraw = !crystals.contains(part));
        }
    }

    public static LayerDefinition create() {
        return BlockbenchModel.layer("colossus", root -> {
            matchProject(root);
            return new ColossusModel(root, false);
        });
    }

    private static ModelPart[] crystals(ModelPart root) {
        ColossusRig rig = Colossus.rig();
        Function<String, @Nullable ModelPart> parts = root.createPartLookup();
        ModelPart[] crystals = new ModelPart[rig.crystalCount()];
        for (int crystal = 0; crystal < crystals.length; crystal++) {
            String bone = rig.boneName(rig.cubeBone(rig.crystalCube(crystal)));
            String part = rig.crystalPart(crystal);
            if (part == null) {
                throw new IllegalStateException("a crystal in the bone '" + bone
                        + "' has no rotation of its own: turn it a little, so that the game can draw it apart from the bone");
            }
            crystals[crystal] = Objects.requireNonNull(parts.apply(bone), bone).getChild(part);
        }
        return crystals;
    }

    @Override
    public void setupAnim(ColossusRenderState state) {
        super.setupAnim(state);
        this.idle.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        this.walk.apply((long) (Colossus.walkSeconds(state.stride, this.walkLength) * 1000.0F), state.walkWeight);
        KeyframeAnimation action = this.actions.get(state.action);
        if (action != null) {
            action.apply((long) (state.actionSeconds * 1000.0F), 1.0F);
        }
        for (int crystal = 0; crystal < this.crystals.length; crystal++) {
            this.crystals[crystal].visible = (state.wholeCrystals >> crystal & 1) != 0
                    && ((state.flashingCrystals >> crystal & 1) != 0) == this.flashing;
        }
    }

    /** Fails unless every exported animation moves every bone as the animation of the same name in the project. */
    private static void matchProject(ModelPart root) {
        ColossusRig rig = Colossus.rig();
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
                                    + rig.boneName(bone) + "' otherwise than ColossusAnimation does at " + seconds
                                    + " s: export the animations again (File, Export, Export Modded Entity Animations)");
                        }
                    }
                }
            }
        }
        root.getAllParts().forEach(ModelPart::resetPose);
    }
}
