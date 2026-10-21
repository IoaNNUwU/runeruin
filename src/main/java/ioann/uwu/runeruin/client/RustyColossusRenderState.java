package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.entities.RustyColossus;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class RustyColossusRenderState extends LivingEntityRenderState {
    public float stride;
    public float walkWeight;
    public RustyColossus.Action action = RustyColossus.Action.NONE;
    public float actionSeconds;
}
