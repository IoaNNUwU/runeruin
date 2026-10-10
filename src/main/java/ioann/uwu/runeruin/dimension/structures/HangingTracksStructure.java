package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.dimension.RRStructures;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesAndLostCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Abandoned minecart tracks and small dungeons that hang on chains under the ceiling of the Deep caves. */
public final class HangingTracksStructure extends Structure {
    public static final MapCodec<HangingTracksStructure> CODEC = simpleCodec(HangingTracksStructure::new);

    /** Nothing of a network is lower than this: the lowest deck under the thickest ceiling, and what hangs under it. */
    private static final int LOWEST_Y = Const.DEEP_CAVES_CEILING_Y - Const.CEILING_TERRAIN_HEIGHT
            - HangingTracksLayout.MAX_GAP - HangingPiece.BELOW_DECK - 1;

    public HangingTracksStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos chunkPos = ctx.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        List<HangingPiece> pieces = HangingTracksLayout.generate(centerX, centerZ, ctx.random(), terrain(ctx.randomState()));
        if (pieces.isEmpty()) {
            return Optional.empty();
        }
        // The biome is checked at this block: inside the ceiling, where the ceiling biomes are.
        return Optional.of(new GenerationStub(
                new BlockPos(centerX, Const.DEEP_CAVES_CEILING_Y - 1, centerZ),
                builder -> pieces.forEach(builder::addPiece)
        ));
    }

    /** The ceiling and the floor of the Deep caves, as the terrain generator shapes them. */
    public static HangingTracksLayout.Terrain terrain(RandomState randomState) {
        return new HangingTracksLayout.Terrain() {
            @Override
            public int ceilingY(int x, int z) {
                return DeepCavesGen.ceilingSurfaceY(x, z, randomState);
            }

            @Override
            public boolean floorReaches(int x, int z, int y) {
                return DeepCavesAndLostCavesGen.interLayerTerrainOverlaps(x, z, y, Const.DEEP_CAVES_CEILING_Y, randomState);
            }
        };
    }

    /**
     * Whether a piece of hanging tracks reaches into this box, built or not: the layout of a network is known
     * before any of its chunks is decorated. Only chunk generation can ask; a tree planted later sees the blocks.
     */
    public static boolean reaches(WorldGenLevel level, BoundingBox box) {
        if (box.maxY() < LOWEST_Y || box.minY() > Const.DEEP_CAVES_CEILING_Y || !(level instanceof WorldGenRegion region)) {
            return false;
        }
        Structure tracks = region.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(RRStructures.HANGING_TRACKS);
        StructureManager structures = region.getLevel().structureManager().forWorldGenRegion(region);
        for (int chunkX = SectionPos.blockToSectionCoord(box.minX()); chunkX <= SectionPos.blockToSectionCoord(box.maxX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(box.minZ()); chunkZ <= SectionPos.blockToSectionCoord(box.maxZ()); chunkZ++) {
                for (StructureStart start : structures.startsForStructure(SectionPos.of(chunkX, 0, chunkZ), tracks)) {
                    for (StructurePiece piece : start.getPieces()) {
                        if (piece.getBoundingBox().intersects(box)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.HANGING_TRACKS.get();
    }
}
