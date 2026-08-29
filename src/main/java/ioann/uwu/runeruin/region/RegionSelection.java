package ioann.uwu.runeruin.region;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public final class RegionSelection {
    private final ResourceKey<Level> dimension;
    private @Nullable BlockPos pos1;
    private @Nullable BlockPos pos2;

    public RegionSelection(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public @Nullable BlockPos pos1() {
        return pos1;
    }

    public @Nullable BlockPos pos2() {
        return pos2;
    }

    public void setPos1(BlockPos pos) {
        this.pos1 = pos.immutable();
    }

    public void setPos2(BlockPos pos) {
        this.pos2 = pos.immutable();
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public BoundingBox box() {
        BlockPos a = pos1;
        BlockPos b = pos2;
        if (a == null || b == null) {
            throw new IllegalStateException("Selection is incomplete");
        }
        return BoundingBox.fromCorners(a, b);
    }
}
