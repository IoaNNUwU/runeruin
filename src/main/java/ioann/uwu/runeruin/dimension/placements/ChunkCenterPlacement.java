package ioann.uwu.runeruin.dimension.placements;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.dimension.RRPlacementModifierTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/** Places a feature at the center block of the chunk containing its input position. */
public class ChunkCenterPlacement extends PlacementModifier {

    public static final MapCodec<ChunkCenterPlacement> CODEC = MapCodec.unit(ChunkCenterPlacement::new);

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
        int centerX = SectionPos.sectionToBlockCoord(
                SectionPos.blockToSectionCoord(origin.getX()), SectionPos.SECTION_HALF_SIZE);
        int centerZ = SectionPos.sectionToBlockCoord(
                SectionPos.blockToSectionCoord(origin.getZ()), SectionPos.SECTION_HALF_SIZE);
        return Stream.of(new BlockPos(centerX, origin.getY(), centerZ));
    }

    @Override
    public PlacementModifierType<?> type() {
        return RRPlacementModifierTypes.CHUNK_CENTER.get();
    }
}
