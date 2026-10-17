package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.Config;
import ioann.uwu.runeruin.RR;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.joml.Matrix4fc;

/**
 * Layers of vanilla clouds over Rune Ruin, each drawn by a {@link CloudRenderer} of its own: one renderer draws once
 * per frame. Its uniform buffer turns through three slots, once per draw and once more after the frame is submitted,
 * so a third draw in one frame waits on a fence of the frame it is building and the game crashes.
 */
public class RuneRuinCloudsRenderer implements CustomCloudsRenderer {

    /** Created while the game starts, on the render thread: a {@link CloudRenderer} allocates GPU buffers. */
    public static final RuneRuinCloudsRenderer INSTANCE = new RuneRuinCloudsRenderer();

    private record Layer(String name, float height, int speed, long timeOffset, BooleanSupplier shown, CloudRenderer clouds) {

        Layer(String name, float height, int speed, long timeOffset, BooleanSupplier shown) {
            this(name, height, speed, timeOffset, shown, new CloudRenderer());
        }
    }

    private final List<Layer> layers = List.of(
            new Layer("lower_sky_clouds", 450.33f, 2, 40000, () -> true),
            new Layer("upper_sky_clouds", 650.33f, 1, 2000, () -> true),
            new Layer("blooming_caves_clouds", 285.33f, 1, 0, Config.RENDER_CLOUDS_BELOW_TOP_LAYER::getAsBoolean)
    );

    private RuneRuinCloudsRenderer() {}

    /** Each renderer loads the cloud texture itself, as the game's own does. */
    public void addReloadListeners(AddClientReloadListenersEvent event) {
        layers.forEach(layer -> event.addListener(RR.id(layer.name), layer.clouds));
    }

    /** After the frame is submitted, as the game does for its own clouds. */
    public void endFrame() {
        layers.forEach(layer -> layer.clouds.endFrame());
    }

    @Override
    public boolean renderClouds(LevelRenderState levelRenderState, Vec3 camPos, CloudStatus cloudStatus, int cloudColor, float cloudHeight, int cloudRange, Matrix4fc modelViewMatrix) {
        // The clouds move by whole ticks of gameTime; the partial tick moves them smoothly between two ticks.
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Layer layer : layers) {
            if (layer.shown.getAsBoolean()) {
                layer.clouds.render(cloudColor, cloudStatus, layer.height, cloudRange, camPos,
                        levelRenderState.gameTime * layer.speed + layer.timeOffset, partialTick * layer.speed);
            }
        }
        return true;
    }
}
