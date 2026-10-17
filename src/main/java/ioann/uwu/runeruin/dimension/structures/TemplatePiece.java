package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * A piece of a structure that a person built in the game and saved with {@code /rrsave}: a template of
 * {@code data/runeruin/structure}, placed whole and turned as the structure asks. Air in the template clears
 * what the world has there; where the build has a structure void block, the world keeps its own block. Blocks
 * keep the shapes they were saved with, and a jigsaw block turns into the block its "Turns into" names.
 */
@EventBusSubscriber(modid = RR.MODID)
public class TemplatePiece extends TemplateStructurePiece {

    private static final Map<String, List<Identifier>> VARIANTS = new ConcurrentHashMap<>();

    public TemplatePiece(StructureTemplateManager templates, Identifier template, BlockPos position, Rotation rotation, Mirror mirror) {
        this(RRStructurePieceTypes.TEMPLATE.get(), templates, template, position, settings(rotation, mirror));
    }

    public TemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
        this(RRStructurePieceTypes.TEMPLATE.get(), templates, tag, UnaryOperator.identity());
    }

    protected TemplatePiece(StructurePieceType type, StructureTemplateManager templates, Identifier template, BlockPos position, StructurePlaceSettings settings) {
        super(type, 0, templates, template, template.toString(), settings, position);
    }

    /** Loads a saved piece; {@code more} adds the processors of a subclass to the turn it was saved with. */
    protected TemplatePiece(StructurePieceType type, StructureTemplateManager templates, CompoundTag tag, UnaryOperator<StructurePlaceSettings> more) {
        super(type, tag, templates, template -> more.apply(settings(
                tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), tag.read("Mirror", Mirror.LEGACY_CODEC).orElseThrow())));
    }

    /**
     * The variants of a piece: every template in a folder of {@code data/runeruin/structure}, in the order of
     * their names. One more template saved into the folder is one more variant, with no code to change.
     * Listing the templates walks every data pack, so a folder is listed once until {@link #forgetVariants}.
     */
    public static List<Identifier> variants(StructureTemplateManager templates, String folder) {
        return VARIANTS.computeIfAbsent(folder, _ -> templates.listTemplates()
                .filter(template -> template.getNamespace().equals(RR.MODID) && template.getPath().startsWith(folder + "/"))
                .sorted()
                .toList());
    }

    /** Lists the folders anew: when the data packs load or reload, and after a person saved a template. */
    public static void forgetVariants() {
        VARIANTS.clear();
    }

    @SubscribeEvent
    public static void addReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(RR.id("template_variants"), (ResourceManagerReloadListener) _ -> forgetVariants());
    }

    protected static StructurePlaceSettings settings(Rotation rotation, Mirror mirror) {
        return new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).addProcessor(JigsawReplacementProcessor.INSTANCE);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        tag.store("Mirror", Mirror.LEGACY_CODEC, this.placeSettings.getMirror());
    }

    /** Places the part of the template inside the chunk; unlike the vanilla piece, writes nothing outside it. */
    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        this.placeSettings.setBoundingBox(chunkBB);
        this.template.placeInWorld(level, this.templatePosition, referencePos, this.placeSettings, random, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
    }
}
