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

    public static final DeferredHolder<EntityType<?>, EntityType<MossyProphet>> MOSSY_PROPHET = REGISTRY.registerEntityType(
            "mossy_prophet",
            MossyProphet::new,
            MobCategory.MONSTER,
            // A warrior it lifts sits over the horns, between the raised hands.
            builder -> builder.sized(2.3F, 3.0F).eyeHeight(2.6F).passengerAttachments(3.2F).clientTrackingRange(10).noLootTable()
    );

    public static final DeferredHolder<EntityType<?>, EntityType<MossyWarrior>> MOSSY_WARRIOR = REGISTRY.registerEntityType(
            "mossy_warrior",
            MossyWarrior::new,
            MobCategory.MONSTER,
            builder -> builder.sized(0.7F, 1.0F).eyeHeight(0.8F).clientTrackingRange(8).noLootTable()
    );

    // Sized like the vanilla minecart.
    public static final DeferredHolder<EntityType<?>, EntityType<RunicMinecart>> RUNIC_MINECART = REGISTRY.registerEntityType(
            "runic_minecart",
            RunicMinecart::new,
            MobCategory.MISC,
            builder -> builder.noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );

    private RREntityTypes() {
    }
}
