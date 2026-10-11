package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A piece of a structure that a person built in the game and saved with {@code /rrsave}: a template of
 * {@code data/runeruin/structure}, placed whole and turned as the structure asks. Air in the template clears
 * what the world has there; where the build has a structure void block, the world keeps its own block.
 */
public final class TemplatePiece extends TemplateStructurePiece {

    public TemplatePiece(StructureTemplateManager templates, Identifier template, BlockPos position, Rotation rotation, Mirror mirror) {
        super(RRStructurePieceTypes.TEMPLATE.get(), 0, templates, template, template.toString(), settings(rotation, mirror), position);
    }

    public TemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
        super(RRStructurePieceTypes.TEMPLATE.get(), tag, templates, template -> settings(
                tag.read("Rot", Rotation.LEGACY_CODEC).orElseThrow(), tag.read("Mirror", Mirror.LEGACY_CODEC).orElseThrow()));
    }

    /**
     * The variants of a piece: every template in a folder of {@code data/runeruin/structure}, in the order of
     * their names. One more template saved into the folder is one more variant, with no code to change.
     */
    public static List<Identifier> variants(StructureTemplateManager templates, String folder) {
        return templates.listTemplates()
                .filter(template -> template.getNamespace().equals(RR.MODID) && template.getPath().startsWith(folder + "/"))
                .sorted()
                .toList();
    }

    private static StructurePlaceSettings settings(Rotation rotation, Mirror mirror) {
        return new StructurePlaceSettings().setRotation(rotation).setMirror(mirror).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
        tag.store("Mirror", Mirror.LEGACY_CODEC, this.placeSettings.getMirror());
    }

    @Override
    protected void handleDataMarker(String markerId, BlockPos position, ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB) {
    }
}
