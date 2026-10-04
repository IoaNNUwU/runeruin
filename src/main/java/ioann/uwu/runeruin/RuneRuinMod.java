package ioann.uwu.runeruin;

import ioann.uwu.runeruin.blocks.RRBlocks;

import ioann.uwu.runeruin.creativetab.RRCreativeModeTabs;
import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.RRFeatures;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.entities.RREntityTypes;
import ioann.uwu.runeruin.entities.Snail;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.preview.RRGameTests;
import ioann.uwu.runeruin.portal.RRPoiTypes;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RR.MODID)
public class RuneRuinMod {

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RuneRuinMod(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::addBlockEntityTypes);
        modEventBus.addListener(Snail::registerAttributes);
        modEventBus.addListener(Snail::registerSpawnPlacements);

        RRBlocks.REGISTRY.register(modEventBus);
        RRItems.REGISTRY.register(modEventBus);
        RREntityTypes.REGISTRY.register(modEventBus);
        RRPoiTypes.REGISTRY.register(modEventBus);
        RRCreativeModeTabs.REGISTRY.register(modEventBus);

        RRBiomeSource.REGISTRY.register(modEventBus);
        RRChunkGenerator.REGISTRY.register(modEventBus);

        RRPlacementModifierTypes.REGISTRY.register(modEventBus);
        RRFeatures.REGISTRY.register(modEventBus);

        RRStructureTypes.REGISTRY.register(modEventBus);
        RRStructurePieceTypes.REGISTRY.register(modEventBus);
        RRGameTests.TEST_FUNCTIONS.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addBlockEntityTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(
                BlockEntityTypes.SIGN,
                RRBlocks.ELDEN_SIGN.get(),
                RRBlocks.ELDEN_WALL_SIGN.get(),
                RRBlocks.INVERTED_TREE_SIGN.get(),
                RRBlocks.INVERTED_TREE_WALL_SIGN.get()
        );
        event.modify(
                BlockEntityTypes.HANGING_SIGN,
                RRBlocks.ELDEN_HANGING_SIGN.get(),
                RRBlocks.ELDEN_WALL_HANGING_SIGN.get(),
                RRBlocks.INVERTED_TREE_HANGING_SIGN.get(),
                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.get()
        );
    }
}
