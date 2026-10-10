package ioann.uwu.runeruin.dimension.structures;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.RRStructureTypes;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesAndLostCavesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;

public class DinosaurSkeletonStructure extends Structure {

    public static final MapCodec<DinosaurSkeletonStructure> CODEC = simpleCodec(DinosaurSkeletonStructure::new);

    public static final double MIN_BURIED_SHARE = 0.2;
    public static final double MAX_BURIED_SHARE = 0.5;

    public DinosaurSkeletonStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        long seed = ctx.random().nextLong();
        Map<BlockPos, BlockState> bones = DinosaurSkeletonGenerator.generate(seed);

        RandomState randomState = ctx.randomState();
        OptionalInt y = buriedOriginY(bones, x, z, (floorX, floorZ) -> floorY(floorX, floorZ, randomState), ctx.random());
        if (y.isEmpty()) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y.getAsInt(), z);
        return Optional.of(new GenerationStub(
                origin, builder -> builder.addPiece(new DinosaurSkeletonPiece(origin, seed, bones))));
    }

    /** The top block of the lost caves floor. The terrain takes its noise 8 blocks away from the column it fills. */
    public static int floorY(int x, int z, RandomState randomState) {
        return DeepCavesAndLostCavesGen.lostCavesFloorY(x + 8, z + 8, randomState);
    }

    /**
     * The height of the skeleton's origin at which a random share of its blocks, from
     * {@link #MIN_BURIED_SHARE} to {@link #MAX_BURIED_SHARE}, lies in the floor; empty if the floor allows none.
     */
    public static OptionalInt buriedOriginY(
            Map<BlockPos, BlockState> bones, int x, int z, IntBinaryOperator floorY, RandomSource random
    ) {
        // How far each block is above the floor with the origin at Y 0. Lowering the origin buries them in this order.
        int[] aboveFloor = bones.keySet().stream()
                .mapToInt(pos -> pos.getY() - floorY.applyAsInt(x + pos.getX(), z + pos.getZ()))
                .sorted()
                .toArray();

        double wanted = MIN_BURIED_SHARE + random.nextDouble() * (MAX_BURIED_SHARE - MIN_BURIED_SHARE);
        OptionalInt best = OptionalInt.empty();
        double bestMiss = Double.MAX_VALUE;
        for (int i = 0; i < aboveFloor.length; i++) {
            if (i + 1 < aboveFloor.length && aboveFloor[i + 1] == aboveFloor[i]) {
                continue;
            }
            double share = (i + 1) / (double) aboveFloor.length;
            if (share >= MIN_BURIED_SHARE && share <= MAX_BURIED_SHARE && Math.abs(share - wanted) < bestMiss) {
                best = OptionalInt.of(-aboveFloor[i]);
                bestMiss = Math.abs(share - wanted);
            }
        }
        return best;
    }

    /** The share of the skeleton's blocks that lie in the floor with its origin at this height. */
    public static double buriedShare(Map<BlockPos, BlockState> bones, BlockPos origin, IntBinaryOperator floorY) {
        long buried = bones.keySet().stream()
                .filter(pos -> origin.getY() + pos.getY() <= floorY.applyAsInt(origin.getX() + pos.getX(), origin.getZ() + pos.getZ()))
                .count();
        return buried / (double) bones.size();
    }

    @Override
    public StructureType<?> type() {
        return RRStructureTypes.DINOSAUR_SKELETON.get();
    }
}
