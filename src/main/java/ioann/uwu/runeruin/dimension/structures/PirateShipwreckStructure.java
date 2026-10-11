package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

public class PirateShipwreckStructure extends Structure {

    public static final MapCodec<PirateShipwreckStructure> CODEC = simpleCodec(PirateShipwreckStructure::new);

    /** The floor under the ship rises no higher than this above the bottom of its keel: the gun deck stays clear. */
    public static final int MAX_FLOOR_ABOVE_KEEL = 4;
    // The biomes are checked this far around the ship, to keep it out from under the baobabs next door,
    // and above the highest floor: inside the floor a column may be in the biomes of the Deep caves ceiling.
    private static final int BIOME_MARGIN = 16;
    private static final int BIOME_Y = Const.BLOOMING_CAVES_Y + Const.TERRAIN_HEIGHT + 1;

    public PirateShipwreckStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        RandomState randomState = ctx.randomState();
        return lay(
                ctx.random().nextLong(), x, z,
                (floorX, floorZ) -> floorY(floorX, floorZ, randomState),
                pos -> ctx.validBiome().test(ctx.biomeSource().getNoiseBiome(
                        QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()), randomState.sampler()))
        ).map(piece -> new GenerationStub(piece.origin().atY(BIOME_Y), builder -> builder.addPiece(piece)));
    }

    /** The ship of this seed where it comes to rest around this block, if the floor holds it and all of it lies in its biomes. */
    public static Optional<PirateShipwreckPiece> lay(long seed, int x, int z, IntBinaryOperator floorY, Predicate<BlockPos> inBiome) {
        PirateShipwreckGenerator.Ship ship = PirateShipwreckGenerator.generate(seed);
        OptionalInt y = restingY(ship, x, z, floorY);
        if (y.isEmpty()) {
            return Optional.empty();
        }
        PirateShipwreckPiece piece = new PirateShipwreckPiece(new BlockPos(x, y.getAsInt(), z), seed, ship.blocks());
        // The structure itself is checked in the middle only.
        BoundingBox box = piece.getBoundingBox().inflatedBy(BIOME_MARGIN);
        for (int cornerX = box.minX(); cornerX <= box.maxX(); cornerX += box.getXSpan() - 1) {
            for (int cornerZ = box.minZ(); cornerZ <= box.maxZ(); cornerZ += box.getZSpan() - 1) {
                if (!inBiome.test(new BlockPos(cornerX, BIOME_Y, cornerZ))) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(piece);
    }

    /** The top block of the blooming caves floor. The terrain takes its noise 8 blocks away from the column it fills. */
    public static int floorY(int x, int z, RandomState randomState) {
        return TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x + 8, z + 8, randomState);
    }

    /**
     * The height of the ship's origin at which the whole keel lies in the floor, at least a block deep; empty
     * if the floor there is too uneven: it would bury the gun deck, or the hull would cut through the floor
     * into the Deep caves.
     */
    public static OptionalInt restingY(PirateShipwreckGenerator.Ship ship, int x, int z, IntBinaryOperator floorY) {
        int y = Integer.MAX_VALUE;
        for (BlockPos keel : ship.keel()) {
            y = Math.min(y, floorY.applyAsInt(x + keel.getX(), z + keel.getZ()) - keel.getY() - 1);
        }

        // The lowest block of every column of the ship.
        Map<Long, Integer> lowest = new HashMap<>();
        for (BlockPos pos : ship.blocks().keySet()) {
            lowest.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY(), Math::min);
        }
        for (Map.Entry<Long, Integer> column : lowest.entrySet()) {
            int floor = floorY.applyAsInt(x + BlockPos.getX(column.getKey()), z + BlockPos.getZ(column.getKey()));
            if (floor - y > MAX_FLOOR_ABOVE_KEEL || floor - (y + column.getValue()) > DeepCavesGen.CRUST) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(y);
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.PIRATE_SHIPWRECK.get();
    }
}
