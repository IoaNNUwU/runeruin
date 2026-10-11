package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRBiomeTags;
import ioann.uwu.runeruin.dimension.structures.PirateShipwreckPiece;
import ioann.uwu.runeruin.dimension.structures.PirateShipwreckStructure;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class PirateShipwreckPreviewJob implements PreviewJob {
    private static final int GROUND_Y = Const.BLOOMING_CAVES_Y + 8;
    private static final int GROUND_MARGIN = 6;
    // How many chunks around the asked one are tried for a place that takes the ship.
    private static final int SEARCH_CHUNKS = 12;

    @Override
    public String id() {
        return "pirate_shipwreck";
    }

    @Override
    public String description() {
        return "Pirate shipwreck structure on a flat floor. params: terrain=real with x and z (the floor and the biomes "
                + "of the Blooming caves for the seed, at the nearest chunk that takes the ship)";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        IntBinaryOperator floorY = (x, z) -> GROUND_Y;
        return export(args, PirateShipwreckStructure.lay(args.seed(), 0, 0, floorY, pos -> true).orElseThrow(), floorY, "flat");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        if (!args.get("terrain", "flat").equals("real")) {
            return run(args);
        }
        RandomState randomState = HeadlessTerrainGenerator.randomState(server, args.seed());
        BiomeSource biomes = HeadlessTerrainGenerator.generator(server).getBiomeSource();
        IntBinaryOperator floorY = (x, z) -> PirateShipwreckStructure.floorY(x, z, randomState);
        Predicate<BlockPos> inBiome = pos -> biomes.getNoiseBiome(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()), randomState.sampler()
        ).is(RRBiomeTags.HAS_PIRATE_SHIPWRECK);

        int chunkX = args.getInt("x", 0) >> 4;
        int chunkZ = args.getInt("z", 0) >> 4;
        // The share of chunks that take the ship tells how many of its places in a world stay empty.
        int taken = 0;
        int nearestDistance = Integer.MAX_VALUE;
        PirateShipwreckPiece nearest = null;
        for (int dx = -SEARCH_CHUNKS; dx <= SEARCH_CHUNKS; dx++) {
            for (int dz = -SEARCH_CHUNKS; dz <= SEARCH_CHUNKS; dz++) {
                Optional<PirateShipwreckPiece> piece = PirateShipwreckStructure.lay(
                        args.seed(), (chunkX + dx << 4) + 8, (chunkZ + dz << 4) + 8, floorY, inBiome);
                if (piece.isEmpty()) {
                    continue;
                }
                taken++;
                if (dx * dx + dz * dz < nearestDistance) {
                    nearestDistance = dx * dx + dz * dz;
                    nearest = piece.get();
                }
            }
        }
        int chunks = (SEARCH_CHUNKS * 2 + 1) * (SEARCH_CHUNKS * 2 + 1);
        if (nearest == null) {
            throw new IOException("None of " + chunks + " chunks around takes the ship");
        }
        return export(args, nearest, floorY, "real, " + taken + " of " + chunks + " chunks around take the ship");
    }

    private PreviewJobs.Result export(PreviewArgs args, PirateShipwreckPiece piece, IntBinaryOperator floorY, String terrain) throws IOException {
        PreviewWorld world = PreviewWorld.create(args.seed());
        BoundingBox ground = piece.getBoundingBox().inflatedBy(GROUND_MARGIN);
        for (int x = ground.minX(); x <= ground.maxX(); x++) {
            for (int z = ground.minZ(); z <= ground.maxZ(); z++) {
                world.fillBox(new BoundingBox(x, Const.BLOOMING_CAVES_Y, z, x, floorY.applyAsInt(x, z), z), Blocks.STONE.defaultBlockState());
            }
        }
        PreviewJobs.placePieceAcrossChunks(piece, world);

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_pirate_shipwreck"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + args.seed(),
                "terrain: " + terrain,
                "origin: " + piece.origin().toShortString()
        ));
    }
}
