package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.structures.HangingTracksStructure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CeilingBlockVineFeature extends Feature<CeilingBlockVineFeature.Config> {

    public CeilingBlockVineFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        BlockState ceilingBlock = config.placeOn.getState(level, random, origin);

        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);

        float maxLength = config.maxLength.sample(random);
        int height = (int) (maxLength / 2 + (maxLength / 2) * random.nextFloat());

        if (isValidPlacement(level, origin, ceilingBlock, trunkBlock, height, mutable)) {

            // --- Roots ---
            for (int y = origin.getY() - 1; y <= origin.getY() + 1; y++) {
                for (int x = origin.getX() - 1; x <= origin.getX() + 2; x++) {
                    for (int z = origin.getZ(); z <= origin.getZ() + 1; z++) {
                        setTrunk(level, mutable, x, y, z, trunkBlock);
                    }
                }
                for (int x = origin.getX(); x <= origin.getX() + 1; x++) {
                    int z = origin.getZ() - 1;
                    setTrunk(level, mutable, x, y, z, trunkBlock);

                    z = origin.getZ() + 2;
                    setTrunk(level, mutable, x, y, z, trunkBlock);
                }
            }

            for (int y = origin.getY() + 1; y < origin.getY() + 4; y++) {
                for (int x = origin.getX(); x < origin.getX() + 2; x++) {
                    for (int z = origin.getZ(); z < origin.getZ() + 2; z++) {
                        setTrunk(level, mutable, x, y, z, trunkBlock);
                    }
                }
            }

            List<BlockPos> additionalRoots = List.of(
                    new BlockPos(origin.getX() - 1, origin.getY() - 2, origin.getZ() + 1),
                    new BlockPos(origin.getX() + 1, origin.getY() - 2, origin.getZ() - 1),
                    new BlockPos(origin.getX(), origin.getY() - 2, origin.getZ() + 2),
                    new BlockPos(origin.getX() + 2, origin.getY() - 2, origin.getZ() + 1)
            );

            for (BlockPos blockPos : additionalRoots) {
                setTrunk(level, mutable, blockPos.getX(), blockPos.getY(), blockPos.getZ(), trunkBlock);
                setTrunk(level, mutable, blockPos.getX(), blockPos.getY() + 4, blockPos.getZ(), trunkBlock);
            }

            boolean mirror = random.nextBoolean();
            boolean rotate = random.nextBoolean();

            // --- Trunk ---
            int segmentHeight = random.nextIntBetweenInclusive(5, 8);
            int nSegments = height / segmentHeight;

            for (int nSeg = 0; nSeg < nSegments; nSeg++) {

                int xOffset;
                int zOffset;
                if (nSeg % 2 == 0) {
                    xOffset = 0;
                    zOffset = 0;
                } else {
                    xOffset = mirror ? -1 : 1;
                    zOffset = rotate ? -1 : 1;
                }

                // --- Main segment body ---
                for (int y = 1; y <= segmentHeight; y++) {
                    int yy = y + segmentHeight * nSeg;
                    int py = origin.getY() - yy;
                    setTrunk(level, mutable, origin.getX() + xOffset, py, origin.getZ() + zOffset, trunkBlock);
                    setTrunk(level, mutable, origin.getX() + xOffset + 1, py, origin.getZ() + zOffset, trunkBlock);
                    setTrunk(level, mutable, origin.getX() + xOffset, py, origin.getZ() + zOffset + 1, trunkBlock);
                    setTrunk(level, mutable, origin.getX() + xOffset + 1, py, origin.getZ() + zOffset + 1, trunkBlock);
                }

                int x0 = origin.getX() + xOffset;
                int z0 = origin.getZ() + zOffset;

                // --- Additional 2 blocks on top and bottom of the segment for smoothness: one diagonal of it ---
                boolean antiDiagonal = rotate == mirror;
                for (int y : List.of(segmentHeight * nSeg, segmentHeight * (nSeg + 1) + 1)) {
                    setTrunk(level, mutable, x0 + 1, origin.getY() - y, antiDiagonal ? z0 : z0 + 1, trunkBlock);
                    setTrunk(level, mutable, x0, origin.getY() - y, antiDiagonal ? z0 + 1 : z0, trunkBlock);
                }

                // --- Thorns in the middle of the segment: a diagonal pair at one of its corners ---
                boolean odd = nSeg % 2 == 1;
                boolean west = mirror == odd;
                boolean south = rotate != odd;
                for (int y : List.of((segmentHeight * nSeg) + segmentHeight / 2, (segmentHeight * nSeg) + segmentHeight / 2 + 1)) {
                    setTrunk(level, mutable, west ? x0 - 1 : x0 + 2, origin.getY() - y, south ? z0 + 1 : z0, trunkBlock);
                    setTrunk(level, mutable, west ? x0 : x0 + 1, origin.getY() - y, south ? z0 + 2 : z0 - 1, trunkBlock);
                }
            }

            // --- Tip of the vine: a column at one corner of the last segment, shorter ones beside it and a thorn
            // under the opposite corner ---
            int tipX;
            int tipZ;
            int stepX;
            int stepZ;
            if (nSegments % 2 == 0) {
                tipX = mirror ? 0 : 1;
                tipZ = rotate ? 0 : 1;
                stepX = 1 - 2 * tipX;
                stepZ = 1 - 2 * tipZ;
            } else {
                tipX = tipZ = mirror ? 0 : 1;
                stepX = stepZ = mirror ? -1 : 1;
            }
            int tx = origin.getX() + tipX;
            int ty = origin.getY() - segmentHeight * nSegments;
            int tz = origin.getZ() + tipZ;
            for (int y = 0; y < 6; y++) {
                setTrunk(level, mutable, tx, ty - y, tz, trunkBlock);
            }
            for (int y = 0; y < 4; y++) {
                setTrunk(level, mutable, tx + stepX, ty - y, tz, trunkBlock);
                setTrunk(level, mutable, tx, ty - y, tz + stepZ, trunkBlock);
            }
            for (int y = 0; y < 2; y++) {
                setTrunk(level, mutable, tx + stepX, ty - 1 - y, tz + stepZ, trunkBlock);
            }

            // --- Berries ---
            List<BlockStateProvider> berryBlocks = config.berryBlocks;
            if (berryBlocks.isEmpty()) {
                return true;
            }

            int rand = random.nextIntBetweenInclusive(0, berryBlocks.size() - 1);
            BlockState berryBlock = config.berryBlocks.get(rand).getState(level, random, origin);

            for (int nSeg = 0; nSeg < nSegments; nSeg++) {

                int xOffset;
                int zOffset;
                if (nSeg % 2 == 0) {
                    xOffset = 0;
                    zOffset = 0;
                } else {
                    xOffset = mirror ? -1 : 1;
                    zOffset = rotate ? -1 : 1;
                }

                for (int y = 1; y < segmentHeight; y = y + 2) {
                    Direction dir = Direction.getRandom(random);

                    for (int i = 0; i < 5; i++) {
                        mutable.set(
                                origin.getX() + xOffset + dir.getStepX() * i,
                                origin.getY() - nSeg * segmentHeight - y + dir.getStepY() * i,
                                origin.getZ() + zOffset + dir.getStepZ() * i
                        );

                        if (level.getBlockState(mutable).isAir()) {
                            level.setBlock(mutable, berryBlock, GeometryUtils.BULK_FLAG);
                            break;
                        }
                    }
                }
            }

            return true;
        }
        return false;
    }

    private static void setTrunk(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutable,
            int x,
            int y,
            int z,
            BlockState trunkBlock
    ) {
        level.setBlock(mutable.set(x, y, z), trunkBlock, GeometryUtils.BULK_FLAG);
    }

    private static boolean isValidPlacement(
            WorldGenLevel level,
            BlockPos origin,
            BlockState ceilingBlock,
            BlockState trunkBlock,
            int height,
            BlockPos.MutableBlockPos mutable
    ) {
        if (!FeatureScans.isHangingTrunkAnchor(level, origin, ceilingBlock, trunkBlock, mutable)) {
            return false;
        }

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        for (int y = 1; y < height; y++) {
            if (!level.getBlockState(mutable.set(ox, oy - y, oz)).isAir()) {
                return false;
            }
        }

        // Like the inverted trees, the vine keeps out of the way of hanging tracks that are not built yet.
        return !HangingTracksStructure.reaches(level, new BoundingBox(ox - 3, oy - height - 2, oz - 3, ox + 4, oy, oz + 4));
    }

    public record Config(
            BlockStateProvider placeOn,
            BlockStateProvider trunkBlock,
            List<BlockStateProvider> berryBlocks,
            IntProvider maxLength
    ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("place_on").forGetter(Config::placeOn),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                        Codec.list(BlockStateProvider.CODEC).fieldOf("berry_blocks").forGetter(Config::berryBlocks),
                        InvertedTreeFeature.Config.MAX_LENGTH.fieldOf("max_length").forGetter(Config::maxLength)
                ).apply(codec, Config::new));
    }
}
