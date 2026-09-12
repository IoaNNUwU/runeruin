package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.RR;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RREntityTypes {
    public static final DeferredRegister.Entities REGISTRY = DeferredRegister.createEntities(RR.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Snail>> SNAIL = REGISTRY.registerEntityType(
            "snail",
            Snail::new,
            MobCategory.CREATURE,
            builder -> builder.sized(0.75F, 1.4F).eyeHeight(1.0F).clientTrackingRange(8).noLootTable()
    );

    private RREntityTypes() {
    }
}
