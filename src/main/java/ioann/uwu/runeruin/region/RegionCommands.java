package ioann.uwu.runeruin.region;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.TemplatePiece;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = RR.MODID)
public final class RegionCommands {
    private static final double PICK_RANGE = 128.0;
    private static final int HIGHLIGHT_INTERVAL = 8;
    private static final DustParticleOptions BOX_DUST = new DustParticleOptions(0xFFE14A, 0.65F);
    private static final DustParticleOptions POS1_DUST = new DustParticleOptions(0x3DDCFF, 1.1F);
    private static final DustParticleOptions POS2_DUST = new DustParticleOptions(0xFF4D9A, 1.1F);

    private static final Map<UUID, RegionSelection> SELECTIONS = new ConcurrentHashMap<>();

    private RegionCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerAliases(dispatcher, "rrpos1", cmd -> registerPos(cmd, 1));
        registerAliases(dispatcher, "rrpos2", cmd -> registerPos(cmd, 2));
        registerAliases(dispatcher, "rrexport", RegionCommands::registerExport);
        registerAliases(dispatcher, "rrsave", RegionCommands::registerSave);
        registerAliases(dispatcher, "rrload", RegionCommands::registerLoad);
        registerAliases(dispatcher, "rrgenerate", RegionCommands::registerGenerate);
        registerAliases(dispatcher, "rrclear", RegionCommands::registerClear);
    }

    /** Registers an operator command as {@code name} and as {@code /name}, so that {@code //name} works too. */
    public static void registerAliases(
        CommandDispatcher<CommandSourceStack> dispatcher,
        String name,
        Consumer<LiteralArgumentBuilder<CommandSourceStack>> builder
    ) {
        for (String alias : List.of(name, "/" + name)) {
            LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(alias).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
            builder.accept(cmd);
            dispatcher.register(cmd);
        }
    }

    private static void registerPos(LiteralArgumentBuilder<CommandSourceStack> cmd, int which) {
        cmd.executes(c -> setPos(c.getSource(), which, targetedOrFeet(c.getSource().getPlayerOrException())))
            .then(
                Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> setPos(c.getSource(), which, BlockPosArgument.getLoadedBlockPos(c, "pos")))
            );
    }

    private static void registerExport(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.executes(c -> export(c.getSource(), RegionExport.defaultName()))
            .then(
                Commands.argument("name", StringArgumentType.word())
                    .executes(c -> export(c.getSource(), RegionExport.sanitizeName(StringArgumentType.getString(c, "name"))))
            );
    }

    private static void registerSave(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.executes(c -> save(c.getSource(), null, false))
            .then(
                Commands.argument("name", IdentifierArgument.id())
                    .suggests(RegionCommands::suggestTemplates)
                    .executes(c -> save(c.getSource(), IdentifierArgument.getId(c, "name"), false))
                    .then(Commands.literal("entities").executes(c -> save(c.getSource(), IdentifierArgument.getId(c, "name"), true)))
            );
    }

    private static void registerLoad(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.then(
            Commands.argument("name", IdentifierArgument.id())
                .suggests(RegionCommands::suggestTemplates)
                .executes(c -> load(c.getSource(), IdentifierArgument.getId(c, "name"), c.getSource().getPlayerOrException().blockPosition().offset(2, 0, 2)))
                .then(
                    Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> load(c.getSource(), IdentifierArgument.getId(c, "name"), BlockPosArgument.getLoadedBlockPos(c, "pos")))
                )
        );
    }

    /** The templates of the mod for Tab, by the bare names that the commands take. */
    private static CompletableFuture<Suggestions> suggestTemplates(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(context.getSource().getLevel().getStructureManager().listTemplates()
            .filter(template -> template.getNamespace().equals(RR.MODID))
            .map(Identifier::getPath)
            .sorted(), builder);
    }

    private static void registerClear(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.executes(c -> {
            ServerPlayer player = c.getSource().getPlayerOrException();
            SELECTIONS.remove(player.getUUID());
            c.getSource().sendSuccess(() -> Component.translatable("commands.runeruin.region.clear"), false);
            return Command.SINGLE_SUCCESS;
        });
    }

    private static void registerGenerate(LiteralArgumentBuilder<CommandSourceStack> cmd) {
        cmd.executes(c -> generate(c.getSource()));
    }

    private static int setPos(CommandSourceStack source, int which, BlockPos pos) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceKey<Level> dimension = source.getLevel().dimension();
        RegionSelection selection = SELECTIONS.compute(player.getUUID(), (id, current) -> {
            if (current == null || !current.dimension().equals(dimension)) {
                return new RegionSelection(dimension);
            }
            return current;
        });
        if (which == 1) {
            selection.setPos1(pos);
        } else {
            selection.setPos2(pos);
        }

        String key = which == 1 ? "commands.runeruin.region.pos1" : "commands.runeruin.region.pos2";
        source.sendSuccess(
            () -> Component.translatable(key, pos.getX(), pos.getY(), pos.getZ()).append(selectionSummary(selection)),
            false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static Component selectionSummary(RegionSelection selection) {
        if (!selection.isComplete()) {
            return Component.translatable("commands.runeruin.region.incomplete").withStyle(ChatFormatting.GRAY);
        }
        BoundingBox box = selection.box();
        return Component.translatable(
            "commands.runeruin.region.size",
            box.getXSpan(),
            box.getYSpan(),
            box.getZSpan(),
            volume(box)
        ).withStyle(ChatFormatting.YELLOW);
    }

    private static long volume(BoundingBox box) {
        return (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    /** The player's selection when it is complete, in this dimension and not too big; else null and a failure message. */
    private static @Nullable BoundingBox checkedSelection(CommandSourceStack source) throws CommandSyntaxException {
        RegionSelection selection = SELECTIONS.get(source.getPlayerOrException().getUUID());
        if (selection == null || !selection.isComplete()) {
            source.sendFailure(Component.translatable("commands.runeruin.region.need_both"));
            return null;
        }
        if (!selection.dimension().equals(source.getLevel().dimension())) {
            source.sendFailure(Component.translatable("commands.runeruin.region.wrong_dimension"));
            return null;
        }
        BoundingBox box = selection.box();
        if (volume(box) > RegionExport.MAX_VOLUME) {
            source.sendFailure(Component.translatable("commands.runeruin.region.too_big", volume(box), RegionExport.MAX_VOLUME));
            return null;
        }
        return box;
    }

    private static int export(CommandSourceStack source, String name) throws CommandSyntaxException {
        BoundingBox box = checkedSelection(source);
        if (box == null) {
            return 0;
        }

        try {
            RegionExport.Result result = RegionExport.write(source.getLevel(), box, name);
            source.sendSuccess(() -> exportSuccess(result, box), false);
            return (int) volume(box);
        } catch (RegionExport.UnloadedChunkException e) {
            BlockPos pos = e.pos;
            source.sendFailure(Component.translatable("commands.runeruin.region.unloaded", pos.getX(), pos.getY(), pos.getZ()));
            return 0;
        } catch (IOException e) {
            RR.LOGGER.error("Failed to export region", e);
            source.sendFailure(Component.translatable("commands.runeruin.region.io_error", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    /**
     * Saves the selection as a structure template of the mod: what a person built becomes a piece of a
     * structure ({@code TemplatePiece}), and {@code /rrload} puts it back into a world. A structure void block in
     * the selection is left out: there the world keeps its own block. The game directory is shared by all
     * checkouts, so a game started with {@code runClient} writes a text file into the resources of the checkout
     * it was started from; any other game writes into its world, as a structure block does. A bare name is the
     * mod's; without a name the selection is saved as the template it was loaded from or last saved as.
     */
    private static int save(CommandSourceStack source, @Nullable Identifier name, boolean entities) throws CommandSyntaxException {
        BoundingBox box = checkedSelection(source);
        if (box == null) {
            return 0;
        }
        RegionSelection selection = SELECTIONS.get(source.getPlayerOrException().getUUID());
        if (name == null) {
            if (selection.template() == null) {
                source.sendFailure(Component.translatable("commands.runeruin.region.save_unnamed"));
                return 0;
            }
            name = selection.template();
            entities = selection.entities();
        }

        ServerLevel level = source.getLevel();
        Identifier id = RR.id(name.getPath());
        StructureTemplateManager templates = level.getStructureManager();
        StructureTemplate template = templates.getOrCreate(id);
        template.fillFromWorld(level, new BlockPos(box.minX(), box.minY(), box.minZ()),
            new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()), entities, List.of(Blocks.STRUCTURE_VOID));
        try {
            String checkout = System.getProperty("runeruin.checkout");
            Component where;
            if (checkout != null) {
                // Refuses a path such as ../x that would leave the structure folder, as the game does for its own.
                Path file = new TemplatePathFactory(Path.of(checkout, "src/main/resources"), PackType.SERVER_DATA)
                    .createAndValidatePathToStructure(id, StructureTemplateManager.RESOURCE_TEXT_STRUCTURE_LISTER);
                writeText(file, template);
                where = clickablePath(file);
            } else if (templates.save(id)) {
                where = Component.translatable("commands.runeruin.region.save_world");
            } else {
                throw new IOException(id.toString());
            }
            selection.setTemplate(id, entities);
            TemplatePiece.forgetVariants();
            source.sendSuccess(() -> Component.translatable("commands.runeruin.region.save", box.getXSpan(), box.getYSpan(), box.getZSpan(), id.toString(), where), false);
            return Command.SINGLE_SUCCESS;
        } catch (IOException | IdentifierException e) {
            RR.LOGGER.error("Failed to save template {}", id, e);
            source.sendFailure(Component.translatable("commands.runeruin.region.io_error", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    /**
     * Writes a template as text ({@code .snbt}), the way the game keeps its own templates in its sources: one
     * line per block, so that a change shows in a diff; {@code runData} turns it into the {@code .nbt} the game
     * loads. The seed that placing gave the loot of a container is left out: it changes with every placing.
     */
    public static void writeText(Path file, StructureTemplate template) throws IOException {
        CompoundTag tag = template.save(new CompoundTag());
        ListTag blocks = tag.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            blocks.getCompoundOrEmpty(i).getCompoundOrEmpty("nbt").remove("LootTableSeed");
        }
        NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, file, NbtUtils.structureToSnbt(tag));
    }

    /**
     * Puts a template of the mod into the world for a person to rebuild: unturned, with its lowest corner at
     * {@code corner}, and selects it, so that {@code /rrsave} without a name writes it back. The box is cleared
     * first, and where the template keeps the block of the world it gets a structure void block, which saving
     * leaves out again.
     */
    private static int load(CommandSourceStack source, Identifier name, BlockPos corner) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Identifier id = RR.id(name.getPath());
        StructureTemplate template = level.getStructureManager().get(id).orElse(null);
        if (template == null) {
            source.sendFailure(Component.translatable("commands.runeruin.region.load_missing", id.toString()));
            return 0;
        }
        StructurePlaceSettings asSaved = new StructurePlaceSettings().setKnownShape(true);
        BoundingBox box = template.getBoundingBox(asSaved, corner);
        if (box.minY() < level.getMinY() || box.maxY() > level.getMaxY()) {
            source.sendFailure(Component.translatable("commands.runeruin.region.generate_height", level.getMinY(), level.getMaxY()));
            return 0;
        }

        level.getEntitiesOfClass(Entity.class, AABB.of(box), entity -> !(entity instanceof Player)).forEach(Entity::discard);
        BlockState structureVoid = Blocks.STRUCTURE_VOID.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            level.setBlock(pos, structureVoid, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        template.placeInWorld(level, corner, corner, asSaved, level.getRandom(), Block.UPDATE_CLIENTS);

        RegionSelection selection = new RegionSelection(level.dimension());
        selection.setPos1(new BlockPos(box.minX(), box.minY(), box.minZ()));
        selection.setPos2(new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
        selection.setTemplate(id, !template.save(new CompoundTag()).getListOrEmpty("entities").isEmpty());
        SELECTIONS.put(player.getUUID(), selection);
        source.sendSuccess(() -> Component.translatable("commands.runeruin.region.load", id.toString(),
            box.getXSpan(), box.getYSpan(), box.getZSpan(), corner.getX(), corner.getY(), corner.getZ()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generate(CommandSourceStack source) throws CommandSyntaxException {
        BoundingBox box = checkedSelection(source);
        if (box == null) {
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (box.minY() < level.getMinY() || box.maxY() > level.getMaxY()) {
            source.sendFailure(Component.translatable("commands.runeruin.region.generate_height", level.getMinY(), level.getMaxY()));
            return 0;
        }

        try {
            PreviewWorld generated = HeadlessTerrainGenerator.generate(source.getServer(), level.getSeed(), box);
            int changed = insert(level, generated, box);
            int generatedBlocks = generated.placedCount();
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.runeruin.region.generate",
                    box.getXSpan(),
                    box.getYSpan(),
                    box.getZSpan(),
                    changed,
                    generatedBlocks
                ),
                false
            );
            return Math.max(Command.SINGLE_SUCCESS, changed);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (IOException e) {
            RR.LOGGER.error("Headless region generation failed", e);
            source.sendFailure(Component.translatable("commands.runeruin.region.generate_error", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int insert(ServerLevel level, PreviewWorld generated, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    pos.set(x, y, z);
                    BlockState state = generated.get(pos);
                    if (!level.getBlockState(pos).equals(state) && level.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    private static Component exportSuccess(RegionExport.Result result, BoundingBox box) {
        MutableComponent message = Component.translatable(
            "commands.runeruin.region.export",
            box.getXSpan(),
            box.getYSpan(),
            box.getZSpan(),
            clickablePath(result.jsonPath())
        );
        Path projection = result.txtPath();
        message.append(Component.literal("\n"));
        if (projection != null) {
            message.append(Component.translatable("commands.runeruin.region.export_projection", clickablePath(projection)));
        } else {
            message.append(Component.translatable("commands.runeruin.region.export_3d_hint").withStyle(ChatFormatting.GRAY));
        }
        if (result.snapshot().trimmedBlocks() > 0) {
            message.append(Component.literal("\n"));
            message.append(
                Component.translatable(
                    "commands.runeruin.region.export_trimmed",
                    result.snapshot().trimmedStates(),
                    result.snapshot().trimmedBlocks()
                ).withStyle(ChatFormatting.GOLD)
            );
        }
        return message;
    }

    /** The absolute path for chat; a click opens the file. */
    public static MutableComponent clickablePath(Path path) {
        Path absolute = path.toAbsolutePath();
        return Component.literal(absolute.toString())
            .withStyle(
                style -> style.withColor(ChatFormatting.AQUA)
                    .withUnderlined(Boolean.TRUE)
                    .withClickEvent(new ClickEvent.OpenFile(absolute))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.runeruin.region.open_file")))
            );
    }

    private static BlockPos targetedOrFeet(ServerPlayer player) {
        HitResult hit = player.pick(Math.max(PICK_RANGE, player.blockInteractionRange()), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return player.blockPosition();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % HIGHLIGHT_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            RegionSelection selection = SELECTIONS.get(player.getUUID());
            if (selection == null || !selection.dimension().equals(level.dimension())) {
                continue;
            }
            highlight(level, player, selection);
        }
    }

    private static void highlight(ServerLevel level, ServerPlayer player, RegionSelection selection) {
        if (selection.isComplete()) {
            BoundingBox box = selection.box();
            drawBox(level, player, BOX_DUST, box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        }
        drawBlock(level, player, POS1_DUST, selection.pos1());
        drawBlock(level, player, POS2_DUST, selection.pos2());
    }

    private static void drawBlock(ServerLevel level, ServerPlayer player, DustParticleOptions particle, @Nullable BlockPos pos) {
        if (pos != null) {
            drawBox(level, player, particle, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
    }

    private static void drawBox(
        ServerLevel level,
        ServerPlayer player,
        DustParticleOptions particle,
        int x0,
        int y0,
        int z0,
        int x1,
        int y1,
        int z1
    ) {
        edge(level, player, particle, x0, y0, z0, x1, y0, z0);
        edge(level, player, particle, x0, y0, z1, x1, y0, z1);
        edge(level, player, particle, x0, y1, z0, x1, y1, z0);
        edge(level, player, particle, x0, y1, z1, x1, y1, z1);
        edge(level, player, particle, x0, y0, z0, x0, y1, z0);
        edge(level, player, particle, x1, y0, z0, x1, y1, z0);
        edge(level, player, particle, x0, y0, z1, x0, y1, z1);
        edge(level, player, particle, x1, y0, z1, x1, y1, z1);
        edge(level, player, particle, x0, y0, z0, x0, y0, z1);
        edge(level, player, particle, x1, y0, z0, x1, y0, z1);
        edge(level, player, particle, x0, y1, z0, x0, y1, z1);
        edge(level, player, particle, x1, y1, z0, x1, y1, z1);
    }

    private static void edge(
        ServerLevel level,
        ServerPlayer player,
        DustParticleOptions particle,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4) {
            level.sendParticles(player, particle, true, true, x1, y1, z1, 1, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(length / Math.max(0.5, length / 24.0)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            level.sendParticles(player, particle, true, true, x1 + dx * t, y1 + dy * t, z1 + dz * t, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
