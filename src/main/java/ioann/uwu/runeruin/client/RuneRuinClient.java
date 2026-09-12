package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.client.model.SnailModel;
import ioann.uwu.runeruin.datagen.DatagenBiomeTagProvider;
import ioann.uwu.runeruin.entities.RREntityTypes;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.data.event.GatherDataEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RR.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public class RuneRuinClient {
    public RuneRuinClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
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
