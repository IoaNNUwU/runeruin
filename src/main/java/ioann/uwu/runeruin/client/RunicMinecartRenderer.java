package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.Identifier;

/** The vanilla minecart with a wheel at each corner; {@code MinecartTextureMixin} gives it its texture. */
public class RunicMinecartRenderer extends MinecartRenderer {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(RR.id("runic_minecart"), "main");
    public static final Identifier TEXTURE = RR.id("textures/entity/minecart/runic_minecart.png");

    public RunicMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, LAYER);
    }

    public static LayerDefinition createBodyLayer() {
        return MinecartModel.createBodyLayer().apply(mesh -> {
            PartDefinition root = mesh.getRoot();
            // Flat against the long sides, one pixel below the floor: down to the rails.
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    root.addOrReplaceChild(
                            "wheel_" + (x < 0 ? "front" : "back") + (z < 0 ? "_left" : "_right"),
                            CubeListBuilder.create().texOffs(44, 0).addBox(-2.0F, -2.0F, -0.5F, 4.0F, 4.0F, 1.0F),
                            PartPose.offset(x * 6.0F, 4.0F, z * 8.5F)
                    );
                }
            }
            return mesh;
        });
    }
}
