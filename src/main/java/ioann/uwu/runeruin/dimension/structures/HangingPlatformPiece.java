package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import ioann.uwu.runeruin.dimension.structures.HangingPiece.Wood;
import ioann.uwu.runeruin.dimension.structures.HangingTracksLayout.Port;
import ioann.uwu.runeruin.loottables.RRLootTables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

/**
 * A platform of the hanging tracks that a person built: a template of {@link #PLATFORMS}, where a track ends and
 * its rails run on, or of {@link #DUNGEONS}, hanging beside the tracks. The jigsaw block of the template is its
 * entrance: the template turns so that the jigsaw faces the track and stands where the track ends. A chain that
 * reaches the top of the template goes on up to the ceiling.
 */
public final class HangingPlatformPiece extends TemplatePiece implements HangingTracksLayout.Part {

    public static final String PLATFORMS = "hanging_tracks/platform";
    public static final String DUNGEONS = "hanging_tracks/dungeon";

    private final Wood wood;

    private HangingPlatformPiece(StructureTemplateManager templates, Identifier template, BlockPos position, Rotation rotation, Mirror mirror, Wood wood) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), templates, template, position,
                settings(rotation, mirror).addProcessor(new NetworkProcessor(wood)));
        this.wood = wood;
        this.boundingBox = upToCeiling(this.boundingBox);
    }

    public HangingPlatformPiece(StructureTemplateManager templates, CompoundTag tag) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), templates, tag,
                settings -> settings.addProcessor(new NetworkProcessor(Wood.values()[tag.getIntOr("Wood", 0)])));
        this.wood = Wood.values()[tag.getIntOr("Wood", 0)];
        this.boundingBox = upToCeiling(this.boundingBox);
    }

    /**
     * The platform of a template, turned so that its jigsaw faces back along the track and stands at
     * {@code port}, where the track ends. Null when the template has no jigsaw facing sideways.
     */
    public static @Nullable HangingPlatformPiece create(StructureTemplateManager templates, Identifier template, Port port, Mirror mirror, Wood wood) {
        List<StructureTemplate.StructureBlockInfo> jigsaws = templates.getOrCreate(template)
                .filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
        if (jigsaws.isEmpty()) {
            return null;
        }
        StructureTemplate.StructureBlockInfo jigsaw = jigsaws.getFirst();
        Direction front = mirror.mirror(JigsawBlock.getFrontFacing(jigsaw.state()));
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(front) == port.facing().getOpposite()) {
                BlockPos entrance = StructureTemplate.transform(jigsaw.pos(), mirror, rotation, BlockPos.ZERO);
                BlockPos position = new BlockPos(port.x(), port.y(), port.z()).subtract(entrance);
                return new HangingPlatformPiece(templates, template, position, rotation, mirror, wood);
            }
        }
        return null;
    }

    /** The piece reaches up to the ceiling, where its chains hang from. */
    private static BoundingBox upToCeiling(BoundingBox box) {
        return new BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), Math.max(box.maxY(), DeepCavesGen.HIGHEST_Y), box.maxZ());
    }

    private BoundingBox templateBox() {
        return this.template.getBoundingBox(this.placeSettings, this.templatePosition);
    }

    @Override
    public BoundingBox footprint() {
        BoundingBox box = templateBox();
        return new BoundingBox(box.minX(), lowestDeck(), box.minZ(), box.maxX(), DeepCavesGen.HIGHEST_Y, box.maxZ());
    }

    /** The deck the track runs onto: the height of the jigsaw. */
    @Override
    public int lowestDeck() {
        return this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.JIGSAW).getFirst().pos().getY();
    }

    @Override
    public int top() {
        return templateBox().maxY();
    }

    @Override
    public int bottom() {
        return templateBox().minY();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("Wood", this.wood.ordinal());
    }

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
        super.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
        // The settings keep to the chunk now, and so does the list of chains.
        int top = templateBox().maxY();
        for (StructureTemplate.StructureBlockInfo chain : this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.IRON_CHAIN)) {
            BlockPos.MutableBlockPos cursor = chain.pos().mutable();
            if (cursor.getY() != top) {
                continue;
            }
            while (cursor.move(Direction.UP).getY() <= DeepCavesGen.HIGHEST_Y && level.getBlockState(cursor).isAir()) {
                level.setBlock(cursor, chain.state(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Builds a template the way its network does: in the wood of the network, with supplies in every empty chest and barrel. */
    public record NetworkProcessor(Wood wood) implements StructureProcessor {
        public static final MapCodec<NetworkProcessor> CODEC = Codec.INT
                .xmap(wood -> new NetworkProcessor(Wood.values()[wood]), processor -> processor.wood().ordinal())
                .fieldOf("wood");

        @Override
        public StructureTemplate.StructureBlockInfo processBlock(
                LevelReader level,
                BlockPos targetPosition,
                BlockPos referencePos,
                BlockPos templateRelativePos,
                StructureTemplate.StructureBlockInfo info,
                StructurePlaceSettings settings
        ) {
            BlockState state = this.wood.of(info.state());
            CompoundTag nbt = info.nbt();
            boolean empty = nbt == null || !nbt.contains("LootTable") && nbt.getListOrEmpty("Items").isEmpty();
            if ((state.is(Blocks.CHEST) || state.is(Blocks.BARREL)) && empty) {
                nbt = nbt == null ? new CompoundTag() : nbt.copy();
                nbt.putString("LootTable", RRLootTables.HANGING_TRACKS_SUPPLY.identifier().toString());
            }
            return new StructureTemplate.StructureBlockInfo(info.pos(), state, nbt);
        }

        @Override
        public MapCodec<NetworkProcessor> codec() {
            return CODEC;
        }
    }
}
