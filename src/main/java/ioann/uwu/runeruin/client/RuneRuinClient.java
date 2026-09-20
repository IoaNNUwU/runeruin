package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.client.model.SnailModel;
import ioann.uwu.runeruin.datagen.DatagenBiomeTagProvider;
import ioann.uwu.runeruin.entities.RREntityTypes;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.data.event.GatherDataEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RR.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public class RuneRuinClient {
    private static final float POWDERED_MOSS_FOG_END = 2.2F;

    public RuneRuinClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.addListener(RuneRuinClient::renderPowderedMossFog);
            NeoForge.EVENT_BUS.addListener(RuneRuinClient::colorPowderedMossFog);
        });
    }

    private static void renderPowderedMossFog(ViewportEvent.RenderFog event) {
        if (!isCameraInPowderedMoss(event.getCamera())) {
            return;
        }

        event.setNearPlaneDistance(0.25F);
        event.setFarPlaneDistance(POWDERED_MOSS_FOG_END);
        event.getFogData().skyEnd = POWDERED_MOSS_FOG_END;
        event.getFogData().cloudEnd = POWDERED_MOSS_FOG_END;
    }

    private static void colorPowderedMossFog(ViewportEvent.ComputeFogColor event) {
        if (!isCameraInPowderedMoss(event.getCamera())) {
            return;
        }

        event.setRed(0.035F);
        event.setGreen(0.09F);
        event.setBlue(0.045F);
    }

    private static boolean isCameraInPowderedMoss(Camera camera) {
        var entity = camera.entity();
        return entity != null && entity.level().getBlockState(camera.blockPosition()).is(RRBlocks.POWDERED_MOSS.get());
    }

    @SubscribeEvent
    static void registerCustomEnvironmentEffectRenderer(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerCloudRenderer(Renderers.RUNE_RUIN_CLOUDS_ID, new RuneRuinCloudsRenderer());
    }

    @SubscribeEvent
    static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SnailModel.LAYER_LOCATION, SnailModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RREntityTypes.SNAIL.get(), SnailRenderer::new);
    }

    @SubscribeEvent
    static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        // Use the biome foliage color for every part, including the item model's
        // in-hand tint via the matching datagen tint source.
        event.register(
                List.of(BlockTintSources.foliage()),
                RRBlocks.BIG_LILY_PAD.get(),
                RRBlocks.BAOBAB_LEAVES.get()
        );
    }
}
