package ioann.uwu.runeruin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/** A mote that glows in the dark and falls for a long time, far enough to vanish in the abyss. */
public class VoidDustParticle extends SingleQuadParticle {

    public VoidDustParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.setSize(0.01F, 0.01F);
        // 0.004 blocks per tick squared: a slow start, then about four blocks per second.
        this.gravity = 0.1F;
        // Motes shed in the same tick must not fall as one clump.
        this.xd = 0.01 * (this.random.nextDouble() - 0.5);
        this.yd = -0.05 * this.random.nextDouble();
        this.zd = 0.01 * (this.random.nextDouble() - 0.5);
        this.lifetime = (int) (64.0F / Mth.randomBetween(this.random, 0.1F, 0.9F));
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    protected int getLightCoords(float a) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            this.remove();
        }
    }
}
