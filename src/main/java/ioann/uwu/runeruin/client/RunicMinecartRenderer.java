package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.Identifier;

/** The vanilla minecart model; {@code MinecartTextureMixin} gives it its texture. */
public class RunicMinecartRenderer extends MinecartRenderer {

    public static final Identifier TEXTURE = RR.id("textures/entity/minecart/runic_minecart.png");

    public RunicMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }
}
