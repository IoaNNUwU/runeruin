package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRDataComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RR.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceKey<Level>>> LAST_DIMENSION = REGISTRY.register(
            "last_dimension", () -> {
                DataComponentType.Builder<ResourceKey<Level>> builder = DataComponentType.builder();
                builder.persistent(ResourceKey.codec(Registries.DIMENSION));
                return builder.build();
            }
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Vec3>> LAST_POSITION = REGISTRY.register(
            "last_pos", () -> {
                DataComponentType.Builder<Vec3> builder = DataComponentType.builder();
                builder.persistent(Vec3.CODEC);
                return builder.build();
            }
    );
}
