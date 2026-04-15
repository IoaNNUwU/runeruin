package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.dimension.RRDimension;
import ioann.uwu.runeruin.dimension.biomes.bloomingcaves.StoneForest;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slime.class)
public class SlimeMixin {

    @Inject(method = "checkSlimeSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void checkSlimeSpawnRules(EntityType<Slime> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {

        var server = level.getServer();
        if (server != null) {
            var runeRuinDimension = server.getLevel(RRDimension.LEVEL);
            if (runeRuinDimension != null) {
                if (StoneForest.checkSlimeSpawnRulesInRuneRuinDimension(type, level, spawnReason, pos, random)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
