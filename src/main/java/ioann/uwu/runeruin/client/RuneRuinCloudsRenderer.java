package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.Config;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import org.joml.Matrix4fc;

public class RuneRuinCloudsRenderer implements CustomCloudsRenderer {

    @Override
    public boolean renderClouds(LevelRenderState levelRenderState, Vec3 camPos, CloudStatus cloudStatus, int cloudColor, float cloudHeight, int cloudRange, Matrix4fc modelViewMatrix) {

        if (levelRenderState.customCloudsRenderer != null) {
            CloudRenderer cloudRenderer = Minecraft.getInstance().levelRenderer.getCloudRenderer();

            cloudRenderer.render(
                    cloudColor,
                    cloudStatus,
                    450.33f,
                    cloudRange,
                    camPos,
                    levelRenderState.gameTime * 2 + 40000,
                    DeltaTracker.ZERO.getGameTimeDeltaPartialTick(true)
            );

            cloudRenderer.endFrame();

            cloudRenderer.render(
                    cloudColor,
                    cloudStatus,
                    650.33f,
                    cloudRange,
                    camPos,
                    levelRenderState.gameTime + 2000,
                    DeltaTracker.ONE.getGameTimeDeltaPartialTick(true)
            );

            if (Config.RENDER_CLOUDS_BELOW_TOP_LAYER.getAsBoolean()) {

                cloudRenderer.endFrame();

                cloudRenderer.render(
                        cloudColor,
                        cloudStatus,
                        285.33f,
                        cloudRange,
                        camPos,
                        levelRenderState.gameTime,
                        DeltaTracker.ZERO.getGameTimeDeltaPartialTick(true)
                );
            }
        }
        return true;
    }
}
