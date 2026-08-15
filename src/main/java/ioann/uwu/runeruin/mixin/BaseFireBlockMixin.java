package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRDimension;
import ioann.uwu.runeruin.portal.RuneRuinPortalShape;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {

    private static final int FAILED_IGNITION_SHATTER_DELAY = 4;

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void runeruin$trySpawnRuneRuinPortal(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci
    ) {
        if (oldState.is(state.getBlock())) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD && level.dimension() != RRDimension.LEVEL) {
            return;
        }

        Optional<RuneRuinPortalShape> shape = RuneRuinPortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
        if (shape.isEmpty()) {
            return;
        }

        // In RuneRuin below the top layer: brief portal flash, then shatter (no explosion / damage).
        if (level.dimension() == RRDimension.LEVEL && pos.getY() < Const.TOP_LAYER_Y) {
            if (!level.isClientSide()) {
                shape.get().createUnstablePortalBlocks(level, FAILED_IGNITION_SHATTER_DELAY);
            }
            ci.cancel();
            return;
        }

        shape.get().createPortalBlocks(level);
        ci.cancel();
    }
}
