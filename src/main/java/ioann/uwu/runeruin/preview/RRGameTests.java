package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.region.RegionExport;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = RR.MODID)
public final class RRGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, RR.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PREVIEW_GIANT_GOBLET =
        TEST_FUNCTIONS.register("preview_giant_goblet", () -> RRGameTests::previewGiantGoblet);

    private RRGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(RR.id("preview"));
        event.registerTest(
            RR.id("preview_giant_goblet"),
            new FunctionGameTestInstance(
                PREVIEW_GIANT_GOBLET.getKey(),
                new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)
            )
        );
    }

    private static void previewGiantGoblet(GameTestHelper helper) {
        try {
            PreviewArgs args = new PreviewArgs(RegionExport.resolveExportDir(helper.getLevel().getServer()), java.util.Map.of())
                .with("seed", "1");
            PreviewJobs.Result result = PreviewCatalog.require("giant_goblet").run(args);
            helper.assertTrue(result.count("runeruin:giant_goblet_piece") > 100, "giant goblet placed too few piece blocks");
            helper.assertTrue(result.count("minecraft:water") > 100, "giant goblet placed too little water");
            helper.succeed();
        } catch (Exception e) {
            helper.fail(e.toString());
        }
    }
}
