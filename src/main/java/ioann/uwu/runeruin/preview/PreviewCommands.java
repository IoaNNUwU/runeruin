package ioann.uwu.runeruin.preview;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.region.RegionExport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = RR.MODID)
public final class PreviewCommands {
    public static final String PROPERTY_JOB = "runeruin.preview";

    private static final SuggestionProvider<CommandSourceStack> JOB_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("list");
        for (PreviewJob job : PreviewCatalog.all()) {
            builder.suggest(job.id());
        }
        return builder.buildFuture();
    };

    private PreviewCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerAliases(dispatcher, "rrpreview", "/rrpreview", PreviewCommands::registerPreview);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        String job = System.getProperty(PROPERTY_JOB, "").trim();
        if (job.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Path dir = RegionExport.resolveExportDir(server);
        try {
            if ("list".equals(job)) {
                Path path = PreviewCatalog.writeList(dir);
                RR.LOGGER.info("Preview job list -> {}", path.toAbsolutePath());
            } else {
                for (PreviewArgs args : PreviewArgs.fromSystem(dir).perSeed()) {
                    PreviewJobs.Result result = PreviewCatalog.require(job).run(args, server);
                    RR.LOGGER.info("Headless preview '{}' finished: {} blocks -> {}", job, result.world().placedCount(), result.jsonPath().toAbsolutePath());
                }
            }
        } catch (Exception e) {
            RR.LOGGER.error("Headless preview '{}' failed. Known jobs:\n{}", job, PreviewCatalog.listText(), e);
            // Exit non-zero so runPreview fails. Not on this thread: the server shutdown hook joins it.
            new Thread(() -> System.exit(1), "Preview failure exit").start();
            return;
        }
        server.halt(false);
    }

    private static void registerAliases(
        CommandDispatcher<CommandSourceStack> dispatcher,
        String name,
        String slashName,
        java.util.function.Consumer<LiteralArgumentBuilder<CommandSourceStack>> builder
    ) {
        LiteralArgumentBuilder<CommandSourceStack> a = Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal(slashName).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        builder.accept(a);
        builder.accept(b);
        dispatcher.register(a);
        dispatcher.register(b);
    }

    private static void registerPreview(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.then(Commands.literal("list").executes(c -> list(c.getSource())))
            .then(Commands.literal("place").then(job(true)))
            .then(job(false));
    }

    /** {@code <job> [seed] [params]}; with {@code place}, what the job builds is also built beside the player. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> job(boolean place) {
        return Commands.argument("job", StringArgumentType.word())
            .suggests(JOB_SUGGESTIONS)
            .executes(c -> run(c.getSource(), StringArgumentType.getString(c, "job"), PreviewJobs.DEFAULT_SEED, Map.of(), place))
            .then(
                Commands.argument("seed", LongArgumentType.longArg())
                    .executes(c -> run(c.getSource(), StringArgumentType.getString(c, "job"), LongArgumentType.getLong(c, "seed"), Map.of(), place))
                    .then(
                        Commands.argument("params", StringArgumentType.greedyString())
                            .executes(c -> run(
                                c.getSource(),
                                StringArgumentType.getString(c, "job"),
                                LongArgumentType.getLong(c, "seed"),
                                PreviewArgs.parseTrailing(StringArgumentType.getString(c, "params")),
                                place
                            ))
                    )
            );
    }

    private static int list(CommandSourceStack source) {
        try {
            Path path = PreviewCatalog.writeList(RegionExport.resolveExportDir(source.getServer()));
            source.sendSuccess(() -> Component.literal(PreviewCatalog.listText()).append(clickablePath(path)), false);
            return PreviewCatalog.all().size();
        } catch (IOException e) {
            source.sendFailure(Component.translatable("commands.runeruin.preview.io_error", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int run(CommandSourceStack source, String jobId, long seed, Map<String, String> extra, boolean place) throws CommandSyntaxException {
        if ("list".equals(jobId)) {
            return list(source);
        }
        try {
            Path dir = RegionExport.resolveExportDir(source.getServer());
            PreviewArgs args = new PreviewArgs(dir, extra).with("seed", Long.toString(seed));
            PreviewJobs.Result result = PreviewCatalog.require(jobId).run(args, source.getServer());
            source.sendSuccess(() -> success(jobId, result), false);
            if (place) {
                int placed = place(source, result.world());
                source.sendSuccess(() -> Component.translatable("commands.runeruin.preview.placed", placed), false);
            }
            return Math.max(1, result.world().placedCount());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (IOException e) {
            RR.LOGGER.error("Preview export failed", e);
            source.sendFailure(Component.translatable("commands.runeruin.preview.io_error", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    /**
     * Builds what a job built, without the ground the job prepared for it, two blocks from the player: a draft
     * that a person rebuilds by hand and saves as a template with {@code /rrsave}.
     */
    private static int place(CommandSourceStack source, PreviewWorld world) throws CommandSyntaxException {
        BoundingBox box = world.writtenBox();
        if (box == null) {
            return 0;
        }
        BlockPos corner = source.getPlayerOrException().blockPosition().offset(2 - box.minX(), -box.minY(), 2 - box.minZ());
        int placed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = world.get(pos);
            if (!state.isAir() && !world.isPrepared(pos) && source.getLevel().setBlock(pos.offset(corner), state, Block.UPDATE_CLIENTS)) {
                placed++;
            }
        }
        return placed;
    }

    private static Component success(String jobId, PreviewJobs.Result result) {
        MutableComponent message = Component.translatable(
            "commands.runeruin.preview.done",
            jobId,
            result.world().seed(),
            result.world().placedCount(),
            clickablePath(result.jsonPath())
        );
        for (Path file : result.files()) {
            if (file.getFileName().toString().endsWith(".txt")) {
                message.append(Component.literal("\n"));
                message.append(clickablePath(file).withStyle(ChatFormatting.AQUA));
            }
        }
        return message;
    }

    private static MutableComponent clickablePath(Path path) {
        Path absolute = path.toAbsolutePath();
        return Component.literal(absolute.toString())
            .withStyle(
                style -> style.withColor(ChatFormatting.AQUA)
                    .withUnderlined(Boolean.TRUE)
                    .withClickEvent(new ClickEvent.OpenFile(absolute))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.runeruin.region.open_file")))
            );
    }
}
