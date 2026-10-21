package ioann.uwu.runeruin;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.creativetab.RRCreativeModeTabs;
import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.RRFeatures;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.RRStructureProcessors;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import ioann.uwu.runeruin.entities.MossyRam;
import ioann.uwu.runeruin.entities.RREntityTypes;
import ioann.uwu.runeruin.entities.RustyColossus;
import ioann.uwu.runeruin.entities.Snail;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.particles.RRParticleTypes;
import ioann.uwu.runeruin.portal.RRPoiTypes;
import ioann.uwu.runeruin.preview.RRGameTests;
import java.nio.file.Path;
import net.minecraft.core.dispenser.MinecartDispenseItemBehavior;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

@Mod(RR.MODID)
public class RuneRuinMod {

    public RuneRuinMod(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::addBlockEntityTypes);
        modEventBus.addListener(this::addDispenserBehaviors);
        modEventBus.addListener(MossyRam::registerAttributes);
        modEventBus.addListener(RustyColossus::registerAttributes);
        modEventBus.addListener(RustyColossus::registerSpawnPlacements);
        modEventBus.addListener(Snail::registerAttributes);
        modEventBus.addListener(Snail::registerSpawnPlacements);
        modEventBus.addListener(StoneForest::registerSpawnPlacements);

        RRParticleTypes.REGISTRY.register(modEventBus);
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
        RRStructureProcessors.REGISTRY.register(modEventBus);
        RRGameTests.TEST_FUNCTIONS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);

        // A game started with runClient reads the templates that /rrsave writes as text straight from the checkout,
        // through the source the game has for its own text templates: the latest save, before runData converts it.
        String checkout = System.getProperty("runeruin.checkout");
        if (checkout != null) {
            StructureUtils.testStructuresSourceDir = Path.of(checkout, "src/main/resources");
        }
    }

    private void addDispenserBehaviors(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerBehavior(
                RRItems.RUNIC_MINECART,
                new MinecartDispenseItemBehavior(RREntityTypes.RUNIC_MINECART.get())
        ));
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
