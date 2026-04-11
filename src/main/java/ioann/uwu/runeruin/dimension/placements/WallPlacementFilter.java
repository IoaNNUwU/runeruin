package ioann.uwu.runeruin.dimension.placements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.List;

public class WallPlacementFilter extends PlacementFilter {

    private final List<BlockState> placeOn;
    private final List<BlockState> forbidNearby;

    public WallPlacementFilter(List<BlockState> placeOn, List<BlockState> forbidNearby) {
        this.placeOn = placeOn;
        this.forbidNearby = forbidNearby;
    }

    public List<BlockState> getPlaceOn() {
        return placeOn;
    }

    public List<BlockState> getForbidNearby() {
        return forbidNearby;
    }

    @Override
    protected boolean shouldPlace(PlacementContext placementContext, RandomSource randomSource, BlockPos origin) {

        WorldGenLevel level = placementContext.getLevel();

        BlockState originBlock = level.getBlockState(origin);

        if (placeOn.stream().noneMatch(block -> originBlock.is(block.getBlock()))) {
            return false;
        }

        var potentialNeighbours = List.of(
                origin.north().above(),
                origin.north().below(),
                origin.south().above(),
                origin.south().below(),
                origin.west().above(),
                origin.west().below(),
                origin.east().above(),
                origin.east().below()
        );

        boolean anyForbiddenBlocks = potentialNeighbours.stream()
                .map(level::getBlockState)
                .anyMatch(neighbour -> forbidNearby.stream()
                        .anyMatch(forbiddenBlockState -> forbiddenBlockState.is(neighbour.getBlock()))
                );

        if (anyForbiddenBlocks) {
            return false;
        }

        List<BlockPos> surrounding = List.of(
                origin.north(),
                origin.south(),
                origin.west(),
                origin.east()
        );

        long holeCount = surrounding.stream().filter(level::isEmptyBlock).count();

        long blockCount = surrounding.stream()
                .map(level::getBlockState)
                .filter(neighbour -> placeOn.stream()
                        .anyMatch(allowedBlockState -> allowedBlockState.is(neighbour.getBlock()))
                ).count();

        return blockCount > 0 && holeCount > 0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return RRPlacementModifierTypes.WALL_FILTER.get();
    }

    static {
        Codec<WallPlacementFilter> a = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockState.CODEC.listOf().fieldOf("place_on").forGetter(WallPlacementFilter::getPlaceOn),
                        BlockState.CODEC.listOf().fieldOf("forbid_nearby").forGetter(WallPlacementFilter::getForbidNearby)
                ).apply(codec, WallPlacementFilter::new));

        CODEC = a.fieldOf("wall_placement");
    }

    public static final MapCodec<WallPlacementFilter> CODEC;
}
