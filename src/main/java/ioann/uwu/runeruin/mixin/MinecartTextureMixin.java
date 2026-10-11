package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.client.RunicMinecartRenderer;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Every minecart renderer draws the vanilla minecart texture: the renderer has no way to choose another. */
@Mixin(AbstractMinecartRenderer.class)
public abstract class MinecartTextureMixin {

    @ModifyArg(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 3
    )
    private Identifier runeruin$runicMinecartTexture(Identifier texture) {
        return (Object) this instanceof RunicMinecartRenderer ? RunicMinecartRenderer.TEXTURE : texture;
    }
}
