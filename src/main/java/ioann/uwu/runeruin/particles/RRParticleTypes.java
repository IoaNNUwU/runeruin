package ioann.uwu.runeruin.particles;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RRParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, RR.MODID);

    /** Yellow vanilla leaf sprites that fall like cherry petals; provider in {@code RuneRuinClient}. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELDEN_LEAVES = REGISTRY.register(
            "elden_leaves",
            () -> new SimpleParticleType(false)
    );

    private RRParticleTypes() {
    }
}
