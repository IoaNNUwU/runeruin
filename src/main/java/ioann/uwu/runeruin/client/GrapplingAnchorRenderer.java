package ioann.uwu.runeruin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.entities.GrapplingAnchor;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The anchor, its model {@code item/grappling_anchor} turned along its flight, and the chain from the hand of its
 * owner to the eye of the anchor, drawn as the vanilla chain block draws it: two crossed strips of its texture.
 */
public class GrapplingAnchorRenderer extends EntityRenderer<GrapplingAnchor, GrapplingAnchorRenderer.State> {

    private static final RenderType CHAIN = RenderTypes.entityCutout(Identifier.withDefaultNamespace("textures/block/iron_chain.png"));
    /** The vanilla chain block at three quarters of its size: blocks of chain per repeat of the texture, and its width. */
    private static final float LINK_LENGTH = 0.75F;
    private static final float CHAIN_WIDTH = 3.0F / 16.0F * LINK_LENGTH;
    /**
     * Points of the anchor model, in model units: the crown that leads the flight sits where the anchor hit, a little
     * into the block, and the chain is fastened to the eye.
     */
    private static final Vector3f CROWN = new Vector3f(8.0F, 10.0F, 1.5F);
    private static final Vector3f EYE = new Vector3f(8.0F, 10.0F, 9.0F);

    private final ItemModelResolver itemModelResolver;
    private final ItemStack anchor = RRItems.GRAPPLING_HOOK.toStack();

    public GrapplingAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.anchor.set(DataComponents.ITEM_MODEL, RR.id("grappling_anchor"));
    }

    @Override
    public boolean shouldRender(GrapplingAnchor entity, Frustum culler, double camX, double camY, double camZ) {
        return super.shouldRender(entity, culler, camX, camY, camZ) && entity.getOwner() instanceof Player;
    }

    @Override
    protected boolean affectedByCulling(GrapplingAnchor entity) {
        // The chain reaches far out of the box of the anchor.
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GrapplingAnchor entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        itemModelResolver.updateForNonLiving(state.anchor, anchor, ItemDisplayContext.NONE, entity);
        // The model leads with -z; the turn brings it to +z, then along the flight.
        state.rotation = Axis.YP.rotationDegrees(entity.getYRot(partialTicks))
                .mul(Axis.XP.rotationDegrees(-entity.getXRot(partialTicks)))
                .mul(Axis.YP.rotationDegrees(180.0F));
        state.chainStart = state.rotation.transform(new Vector3f(EYE).sub(CROWN).div(16.0F));
        if (entity.getOwner() instanceof Player owner) {
            Vec3 hand = handPosition(owner, partialTicks);
            state.chainEnd = hand.subtract(entity.getPosition(partialTicks)).toVector3f();
            BlockPos handPos = BlockPos.containing(hand);
            state.handLight = LightCoordsUtil.pack(
                    owner.level().getBrightness(LightLayer.BLOCK, handPos), owner.level().getBrightness(LightLayer.SKY, handPos));
        } else {
            state.chainEnd = state.chainStart;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(state.rotation);
        // The item renderer draws a model point p at p / 16 - 0.5.
        poseStack.translate(0.5F - CROWN.x / 16.0F, 0.5F - CROWN.y / 16.0F, 0.5F - CROWN.z / 16.0F);
        state.anchor.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();

        collector.submitCustomGeometry(poseStack, CHAIN, (pose, buffer) -> {
            Vector3f along = new Vector3f(state.chainEnd).sub(state.chainStart);
            float length = along.length();
            if (length < 1.0E-3F) {
                return;
            }
            Vector3f side = new Vector3f(along).cross(0.0F, 1.0F, 0.0F);
            if (side.lengthSquared() < 1.0E-6F * length * length) {
                side.set(1.0F, 0.0F, 0.0F);
            }
            side.normalize(CHAIN_WIDTH / 2.0F);
            Vector3f otherSide = new Vector3f(along).cross(side).normalize(CHAIN_WIDTH / 2.0F);
            strip(buffer, pose, state, along, length, side, otherSide, 0.0F);
            strip(buffer, pose, state, along, length, otherSide, side, 3.0F / 16.0F);
        });
        super.submit(state, poseStack, collector, camera);
    }

    /** One strip of the chain, a texture repeat per link length; u0 is the column of the chain texture it shows. */
    private static void strip(VertexConsumer buffer, PoseStack.Pose pose, State state, Vector3f along, float length,
                              Vector3f side, Vector3f normal, float u0) {
        float u1 = u0 + 3.0F / 16.0F;
        int links = Mth.ceil(length / LINK_LENGTH);
        for (int i = 0; i < links; i++) {
            float t0 = i * LINK_LENGTH / length;
            float t1 = Math.min(1.0F, (i + 1) * LINK_LENGTH / length);
            float v1 = (t1 - t0) * length / LINK_LENGTH;
            int light = lerpLight((t0 + t1) / 2.0F, state.lightCoords, state.handLight);
            Vector3f p0 = new Vector3f(along).mul(t0).add(state.chainStart);
            Vector3f p1 = new Vector3f(along).mul(t1).add(state.chainStart);
            vertex(buffer, pose, new Vector3f(p0).sub(side), u0, 0.0F, light, normal);
            vertex(buffer, pose, new Vector3f(p0).add(side), u1, 0.0F, light, normal);
            vertex(buffer, pose, new Vector3f(p1).add(side), u1, v1, light, normal);
            vertex(buffer, pose, new Vector3f(p1).sub(side), u0, v1, light, normal);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f point, float u, float v, int light, Vector3f normal) {
        buffer.addVertex(pose, point.x, point.y, point.z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static int lerpLight(float t, int from, int to) {
        return LightCoordsUtil.pack(
                Math.round(Mth.lerp(t, LightCoordsUtil.block(from), LightCoordsUtil.block(to))),
                Math.round(Mth.lerp(t, LightCoordsUtil.sky(from), LightCoordsUtil.sky(to))));
    }

    /** The arm that holds the hook: the main one unless only the other hand has it. */
    public static HumanoidArm holdingArm(Player owner) {
        return owner.getMainHandItem().is(RRItems.GRAPPLING_HOOK) ? owner.getMainArm() : owner.getMainArm().getOpposite();
    }

    /** The muzzle of the hook in the hand, as FishingHookRenderer finds the tip of a fishing rod (Minecraft 26.2). */
    private Vec3 handPosition(Player owner, float partialTicks) {
        float swing = Mth.sin(Mth.sqrt(owner.getAttackAnim(partialTicks)) * (float) Math.PI);
        int side = holdingArm(owner) == HumanoidArm.RIGHT ? 1 : -1;
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().player
                && entityRenderDispatcher.camera != null) {
            float fov = entityRenderDispatcher.options.fov().get().intValue();
            Vec3 view = entityRenderDispatcher.camera.getNearPlane(fov)
                    .getPointOnPlane(side * 0.45F, -0.35F)
                    .scale(960.0 / fov)
                    .yRot(swing * 0.5F)
                    .xRot(-swing * 0.7F);
            return owner.getEyePosition(partialTicks).add(view);
        }
        float yRot = Mth.lerp(partialTicks, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yRot);
        double cos = Mth.cos(yRot);
        float scale = owner.getScale();
        double right = side * 0.35 * scale;
        double forward = 0.8 * scale;
        double y = (owner.isCrouching() ? -0.1875 : 0.0) - 0.45 * scale;
        return owner.getEyePosition(partialTicks).add(-cos * right - sin * forward, y, -sin * right + cos * forward);
    }

    public static class State extends EntityRenderState {
        final ItemStackRenderState anchor = new ItemStackRenderState();
        Quaternionf rotation = new Quaternionf();
        Vector3f chainStart = new Vector3f();
        Vector3f chainEnd = new Vector3f();
        int handLight;
    }
}
