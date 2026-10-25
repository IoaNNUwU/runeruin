package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.entities.Colossus;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ColossusRenderState extends LivingEntityRenderState {
    public float stride;
    public float walkWeight;
    public Colossus.Action action = Colossus.Action.NONE;
    public float actionSeconds;
}
