package ioann.uwu.runeruin;

import ioann.uwu.runeruin.blocks.RRBlocks;

import ioann.uwu.runeruin.creativetab.RRCreativeModeTabs;
import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.RRFeatures;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RR.MODID)
public class RuneRuinMod {

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RuneRuinMod(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);

        RRBlocks.REGISTRY.register(modEventBus);
        RRItems.REGISTRY.register(modEventBus);
        RRCreativeModeTabs.REGISTRY.register(modEventBus);

        RRBiomeSource.REGISTRY.register(modEventBus);
        RRChunkGenerator.REGISTRY.register(modEventBus);

        RRPlacementModifierTypes.REGISTRY.register(modEventBus);
        RRFeatures.REGISTRY.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (RuneRuin) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this); // TODO !!

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

        if (Config.RENDER_CLOUDS_BELOW_TOP_LAYER.getAsBoolean()) {
            RR.LOGGER.info("RENDER CLOUDS");
        } else {
            RR.LOGGER.info("DON'T RENDER CLOUDS");
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        RR.LOGGER.info("HELLO from server starting");
    }
}
