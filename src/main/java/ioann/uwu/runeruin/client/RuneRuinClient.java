package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.Config;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.client.model.MossyRamModel;
import ioann.uwu.runeruin.client.model.SnailModel;
import ioann.uwu.runeruin.dimension.RRDimension;
import ioann.uwu.runeruin.entities.RREntityTypes;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.particles.RRParticleTypes;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.FlipFrameEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@Mod(value = RR.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public class RuneRuinClient {
    private static final float POWDERED_MOSS_FOG_END = 2.2F;

    public RuneRuinClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        // The config screen behind the mod's button in the Mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void renderPowderedMossFog(ViewportEvent.RenderFog event) {
        if (!isCameraInPowderedMoss(event.getCamera())) {
            return;
        }

        event.setNearPlaneDistance(0.25F);
        event.setFarPlaneDistance(POWDERED_MOSS_FOG_END);
        event.getFogData().skyEnd = POWDERED_MOSS_FOG_END;
        event.getFogData().cloudEnd = POWDERED_MOSS_FOG_END;
    }

    @SubscribeEvent
    static void colorPowderedMossFog(ViewportEvent.ComputeFogColor event) {
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
        event.registerCloudRenderer(RRDimension.CLOUDS_ID, RuneRuinCloudsRenderer.INSTANCE);
    }

    @SubscribeEvent
    static void addReloadListeners(AddClientReloadListenersEvent event) {
        RuneRuinCloudsRenderer.INSTANCE.addReloadListeners(event);
    }

    @SubscribeEvent
    static void endFrame(FlipFrameEvent event) {
        RuneRuinCloudsRenderer.INSTANCE.endFrame();
    }

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Cherry petal motion at vanilla leaf size.
        event.registerSpriteSet(RRParticleTypes.ELDEN_LEAVES.get(), sprites -> (_, level, x, y, z, _, _, _, random) ->
                new FallingLeavesParticle(level, x, y, z, sprites.get(random), 0.25F, 2.0F, false, true, 2.0F, 0.0F));
        event.registerSpriteSet(RRParticleTypes.VOID_DUST.get(), sprites -> (_, level, x, y, z, _, _, _, random) ->
                new VoidDustParticle(level, x, y, z, sprites.get(random)));
        // The end rod glitter, white to the end instead of fading to warm.
        event.registerSpriteSet(RRParticleTypes.STAR.get(), sprites -> (_, level, x, y, z, xd, yd, zd, _) -> {
            EndRodParticle star = new EndRodParticle(level, x, y, z, xd, yd, zd, sprites);
            star.setFadeColor(0xFFFFFF);
            return star;
        });
    }

    @SubscribeEvent
    static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SnailModel.LAYER_LOCATION, SnailModel::createBodyLayer);
        event.registerLayerDefinition(MossyRamModel.PROPHET, () -> MossyRamModel.createProphet(CubeDeformation.NONE));
        event.registerLayerDefinition(MossyRamModel.PROPHET_MOSS, () -> MossyRamModel.createProphet(MossyRamModel.PROPHET_MOSS_DEPTH));
        event.registerLayerDefinition(MossyRamModel.WARRIOR, () -> MossyRamModel.createWarrior(CubeDeformation.NONE));
        event.registerLayerDefinition(MossyRamModel.WARRIOR_MOSS, () -> MossyRamModel.createWarrior(MossyRamModel.WARRIOR_MOSS_DEPTH));
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RREntityTypes.SNAIL.get(), SnailRenderer::new);
        event.registerEntityRenderer(RREntityTypes.MOSSY_PROPHET.get(),
                context -> new MossyRamRenderer(context, MossyRamModel.PROPHET, MossyRamModel.PROPHET_MOSS, MossyRamModel::prophet, 1.3F));
        event.registerEntityRenderer(RREntityTypes.MOSSY_WARRIOR.get(),
                context -> new MossyRamRenderer(context, MossyRamModel.WARRIOR, MossyRamModel.WARRIOR_MOSS, MossyRamModel::warrior, 0.4F));
        event.registerEntityRenderer(RREntityTypes.RUNIC_MINECART.get(), RunicMinecartRenderer::new);
        // The berry's own item at half the size of a snowball.
        event.registerEntityRenderer(RREntityTypes.BLOWPIPE_BERRY.get(), context -> new ThrownItemRenderer<>(context, 0.5F, false));
    }

    @SubscribeEvent
    static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            renderer.addLayer(new BlowpipeLayer(renderer));
        }
    }

    @SubscribeEvent
    static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(BlowpipeLayer.STATE_MODIFIER);
    }

    @SubscribeEvent
    static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(BlowpipeLayer.ITEM_EXTENSIONS, RRItems.BLOWPIPE.get());
    }

    @SubscribeEvent
    static void registerBlockTintSources(RegisterColorHandlersEvent.BlockTintSources event) {
        // Use the biome foliage color for every part, including the item model's
        // in-hand tint via the matching datagen tint source.
        event.register(
                List.of(BlockTintSources.foliage()),
                RRBlocks.BIG_LILY_PAD.get(),
                RRBlocks.WATER_LILY_ROOT.get(),
                RRBlocks.WATER_LILY_LEAF.get(),
                RRBlocks.WATER_LILY_FLOWER.get(),
                RRBlocks.BAOBAB_LEAVES.get()
        );
    }

    @SubscribeEvent
    static void addWaterLilyStems(ModelEvent.ModifyBakingResult event) {
        var sprite = event.getTextureGetter().apply(WaterLilyStemTexture.SPRITE);
        for (var state : RRBlocks.WATER_LILY_ROOT.get().getStateDefinition().getPossibleStates()) {
            event.getBakingResult().blockStateModels().computeIfPresent(state, (_, model) -> new WaterLilyRootModel(model, sprite));
        }
    }
}
