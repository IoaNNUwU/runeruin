package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.blocks.WaterLilyLeafBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redraws the root of a water lily whose leaf appears or disappears. A mixin, because the client has no event for a
 * changed block, and NeoForge's {@code IBlockExtension.onBlockStateChange} is called only on the new block, so a
 * removed leaf never learns that it is gone.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow
    @Final
    private LevelExtractor levelExtractor;

    // A water lily stem is drawn by its root, which may be in another section than the leaf.
    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void runeruin$redrawWaterLilyRoot(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        WaterLilyLeafBlock.rootOf(pos, oldState).ifPresent(root -> this.levelExtractor.blockChanged(root, 0));
        WaterLilyLeafBlock.rootOf(pos, newState).ifPresent(root -> this.levelExtractor.blockChanged(root, 0));
    }
}
