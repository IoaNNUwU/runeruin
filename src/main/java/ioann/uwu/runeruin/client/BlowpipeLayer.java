package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.items.BlowpipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A player who blows a blowpipe holds it in the mouth: the pipe follows the head, placed by the "head" display of
 * {@code item/blowpipe_blowing}, and the arm reaches for it. Meanwhile the item definition draws nothing in the hand.
 */
public class BlowpipeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final ContextKey<ItemStackRenderState> PIPE = new ContextKey<>(RR.id("blowpipe_at_mouth"));

    /** Where the hand grips the pipe: under it, in front of the mouth, in model pixels from the neck along the head. */
    private static final Vector3f GRIP = new Vector3f(0.0F, 1.4F, -7.0F);

    /** The pose of the arm that blows, added to the vanilla ones by {@code META-INF/enumextensions.json}. */
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class, false, false, (IArmPoseTransformer) BlowpipeLayer::reachForPipe);

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack blowpipe) {
            return entity.getUseItemRemainingTicks() > 0 && entity.getUsedItemHand() == hand ? ARM_POSE.getValue() : null;
        }
    };

    /** Resolves the pipe for the head while the player blows. */
    public static final AvatarRenderStateModifier STATE_MODIFIER = new AvatarRenderStateModifier() {
        @Override
        public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
            if (avatar.isUsingItem() && avatar.getUseItem().getItem() instanceof BlowpipeItem) {
                ItemStackRenderState pipe = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForLiving(pipe, avatar.getUseItem(), ItemDisplayContext.HEAD, avatar);
                state.setRenderData(PIPE, pipe);
            }
        }
    };

    public BlowpipeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state,
                       float yRot, float xRot) {
        ItemStackRenderState pipe = state.getRenderData(PIPE);
        if (pipe == null || pipe.isEmpty()) {
            return;
        }
        // As vanilla puts an item on the head.
        poseStack.pushPose();
        this.getParentModel().root().translateAndRotate(poseStack);
        this.getParentModel().translateToHead(poseStack);
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
        pipe.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }

    // Points the arm at the grip wherever the head turns. The model crouches only after posing the arms, so the
    // crouch of HumanoidModel.setupAnim is taken in advance: the head drops by 4.2, the arms by 3.2 and tilt by 0.4.
    private static void reachForPipe(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart head = model.head;
        ModelPart limb = model.getArm(arm);
        float crouch = state.isCrouching ? 1.0F : 0.0F;
        Vector3f toGrip = new Vector3f(GRIP).rotateX(head.xRot).rotateY(head.yRot)
                .add(head.x, head.y + 4.2F * crouch, head.z)
                .sub(limb.x, limb.y + 3.2F * crouch, limb.z);
        limb.xRot = (float) Mth.atan2(-Math.sqrt(toGrip.x * toGrip.x + toGrip.z * toGrip.z), toGrip.y) - 0.4F * crouch;
        limb.yRot = (float) Mth.atan2(-toGrip.x, -toGrip.z);
    }
}
