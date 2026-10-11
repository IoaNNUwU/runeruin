package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.dimension.RRStructures;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesAndLostCavesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_Y;

public class GiantGobletStructure extends Structure {

    public static final MapCodec<GiantGobletStructure> CODEC = simpleCodec(GiantGobletStructure::new);

    private static final int HEIGHT_JITTER = 20;
    private static final int WIDTH_JITTER = 10;
    private static final int SAMPLE_STEP = 6;
    private static final int RIM_SAMPLES = 16;

    public GiantGobletStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos chunkPos = ctx.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();

        int height = (DEEP_CAVES_Y - LOST_CAVES_Y) + ctx.random().nextInt(HEIGHT_JITTER * 2 + 1) - HEIGHT_JITTER;
        int minRadius = GiantGobletPiece.PILLAR_RADIUS + GiantGobletPiece.RIM_THICKNESS + 3;
        int bowlRadius = Math.max(height / 2 + ctx.random().nextInt(WIDTH_JITTER * 2 + 1) - WIDTH_JITTER, minRadius);

        if (!hasRoomForBowl(ctx.randomState(), centerX, centerZ, height, bowlRadius)) {
            return Optional.empty();
        }

        long seed = ctx.random().nextLong();
        int biomeY = LOST_CAVES_Y + 8;
        return Optional.of(new GenerationStub(
                new BlockPos(centerX, biomeY, centerZ),
                builder -> builder.addPiece(new GiantGobletPiece(centerX, centerZ, height, bowlRadius, seed))
        ));
    }

    private static boolean hasRoomForBowl(RandomState randomState, int centerX, int centerZ, int height, int bowlRadius) {
        int bowlMinY = GiantGobletPiece.bowlBottomY(height);
        int bowlMaxY = GiantGobletPiece.bowlTopY(height);
        int extent = GiantGobletPiece.generationRadius(bowlRadius);
        int r2 = extent * extent;

        if (DeepCavesAndLostCavesGen.interLayerTerrainOverlaps(centerX, centerZ, bowlMinY, bowlMaxY, randomState)) {
            return false;
        }

        for (int dx = -extent; dx <= extent; dx += SAMPLE_STEP) {
            for (int dz = -extent; dz <= extent; dz += SAMPLE_STEP) {
                if (dx * dx + dz * dz > r2) {
                    continue;
                }
                if (DeepCavesAndLostCavesGen.interLayerTerrainOverlaps(centerX + dx, centerZ + dz, bowlMinY, bowlMaxY, randomState)) {
                    return false;
                }
            }
        }

        for (int i = 0; i < RIM_SAMPLES; i++) {
            double angle = (Math.PI * 2 * i) / RIM_SAMPLES;
            int x = centerX + (int) Math.round(Math.cos(angle) * extent);
            int z = centerZ + (int) Math.round(Math.sin(angle) * extent);
            if (DeepCavesAndLostCavesGen.interLayerTerrainOverlaps(x, z, bowlMinY, bowlMaxY, randomState)) {
                return false;
            }
        }

        return true;
    }

    /**
     * The goblets that reach into the chunk of this block, built or not: a goblet is known before any of its
     * chunks is decorated. Only chunk generation can ask; anything placed later sees the blocks.
     */
    public static List<GiantGobletPiece> inChunkOf(WorldGenLevel level, BlockPos pos) {
        if (!(level instanceof WorldGenRegion region)) {
            return List.of();
        }
        Structure goblet = region.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(RRStructures.GIANT_GOBLET);
        StructureManager structures = region.getLevel().structureManager().forWorldGenRegion(region);
        List<GiantGobletPiece> goblets = new ArrayList<>();
        for (StructureStart start : structures.startsForStructure(SectionPos.of(pos), goblet)) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece instanceof GiantGobletPiece gobletPiece) {
                    goblets.add(gobletPiece);
                }
            }
        }
        return goblets;
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.GIANT_GOBLET.get();
    }
}
