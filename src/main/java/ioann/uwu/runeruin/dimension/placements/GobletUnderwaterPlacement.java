package ioann.uwu.runeruin.dimension.placements;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_Y;

/** Finds actual water cells resting on Goblet bowl blocks, regardless of overhead terrain. */
public class GobletUnderwaterPlacement extends PlacementModifier {

    public static final MapCodec<GobletUnderwaterPlacement> CODEC = MapCodec.unit(GobletUnderwaterPlacement::new);

    private static final int MIN_SUPPORT_Y = LOST_CAVES_Y;
    // Scan the full stacked-cave band; the bud + water check keeps placements on goblets only.
    private static final int MAX_SUPPORT_Y = DEEP_CAVES_CEILING_Y;

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        var level = context.getLevel();
        var positions = Stream.<BlockPos>builder();
        var supportPos = new BlockPos.MutableBlockPos(origin.getX(), MIN_SUPPORT_Y, origin.getZ());

        for (int y = MIN_SUPPORT_Y; y <= MAX_SUPPORT_Y; y++) {
            supportPos.setY(y);
            if (level.getBlockState(supportPos).is(RRBlocks.GIANT_GOBLET_BUD.get())) {
                BlockPos waterPos = supportPos.above();
                if (level.getBlockState(waterPos).is(Blocks.WATER)) {
                    positions.add(waterPos);
                }
            }
        }

        return positions.build();
    }

    @Override
    public PlacementModifierType<?> type() {
        return RRPlacementModifierTypes.GOBLET_UNDERWATER.get();
    }
}
