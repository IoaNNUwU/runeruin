package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.Config;
import ioann.uwu.runeruin.RR;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = RR.MODID)
public class GlowingMossEvents {

    private static final int ACTIVATION_RADIUS = 2;
    private static final int ACTIVATION_VERTICAL_RANGE = 2;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!Config.GLOWING_MOSS_DYNAMIC_LIGHT.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Player player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }

            BlockPos center = player.blockPosition();
            int minY = Math.max(level.getMinY(), center.getY() - ACTIVATION_VERTICAL_RANGE);
            int maxY = Math.min(level.getMaxY(), center.getY() + ACTIVATION_VERTICAL_RANGE);

            for (int dx = -ACTIVATION_RADIUS; dx <= ACTIVATION_RADIUS; dx++) {
                for (int dz = -ACTIVATION_RADIUS; dz <= ACTIVATION_RADIUS; dz++) {
                    for (int y = minY; y <= maxY; y++) {
                        mutable.set(center.getX() + dx, y, center.getZ() + dz);
                        BlockState state = level.getBlockState(mutable);
                        if (!GlowingMossBlock.isGlowingMoss(state)) {
                            continue;
                        }
                        if (state.getValue(GlowingMossBlock.TARGET_LIGHT_LEVEL) != GlowingMossBlock.MAX_LIGHT) {
                            GlowingMossBlock.setTargetLightLevel(level, mutable, state, GlowingMossBlock.MAX_LIGHT);
                        }
                    }
                }
            }
        }
    }
}
