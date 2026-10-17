package ioann.uwu.runeruin.dimension.placements;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class WallPlacementFilter extends PlacementFilter {

    public static final MapCodec<WallPlacementFilter> CODEC = RecordCodecBuilder.<WallPlacementFilter>create(codec -> codec.group(
            BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("place_on").forGetter(filter -> filter.placeOn),
            BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("forbid_nearby").forGetter(filter -> filter.forbidNearby)
    ).apply(codec, WallPlacementFilter::new)).fieldOf("wall_placement");

    private final List<Block> placeOn;
    private final List<Block> forbidNearby;

    public WallPlacementFilter(List<Block> placeOn, List<Block> forbidNearby) {
        this.placeOn = placeOn;
        this.forbidNearby = forbidNearby;
    }

    public static WallPlacementFilter ashenMushroom() {
        return new WallPlacementFilter(
                List.of(Blocks.STONE, Blocks.DEEPSLATE),
                List.of(Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, RRBlocks.ASHEN_MUSHROOM_BLOCK.get())
        );
    }

    @Override
    protected boolean shouldPlace(PlacementContext placementContext, RandomSource randomSource, BlockPos origin) {
        return canPlaceAt(placementContext.getLevel(), origin);
    }

    public boolean canPlaceAt(WorldGenLevel level, BlockPos origin) {
        if (!placeOn.contains(level.getBlockState(origin).getBlock())) {
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

        if (potentialNeighbours.stream().anyMatch(pos -> forbidNearby.contains(level.getBlockState(pos).getBlock()))) {
            return false;
        }

        List<BlockPos> surrounding = List.of(
                origin.north(),
                origin.south(),
                origin.west(),
                origin.east()
        );

        long holeCount = surrounding.stream().filter(level::isEmptyBlock).count();

        long blockCount = surrounding.stream().filter(pos -> placeOn.contains(level.getBlockState(pos).getBlock())).count();

        return blockCount > 0 && holeCount > 0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return RRPlacementModifierTypes.WALL_FILTER.get();
    }
}
