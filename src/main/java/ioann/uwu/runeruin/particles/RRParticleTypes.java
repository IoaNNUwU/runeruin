package ioann.uwu.runeruin.particles;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RRParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, RR.MODID);

    /** Vanilla leaf sprites in the Elden palette that fall like cherry petals; provider in {@code RuneRuinClient}. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELDEN_LEAVES = REGISTRY.register(
            "elden_leaves",
            () -> new SimpleParticleType(false)
    );

    /** A glowing mote that falls into the abyss, shed by dust blooms and by the Void itself. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_DUST = REGISTRY.register(
            "void_dust",
            () -> new SimpleParticleType(false)
    );

    private RRParticleTypes() {
    }
}
