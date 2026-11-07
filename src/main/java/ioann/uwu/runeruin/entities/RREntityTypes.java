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

    // The box is only its feet, which walk on the ground; the solid parts around its bones are its body.
    public static final DeferredHolder<EntityType<?>, EntityType<Colossus>> COLOSSUS = REGISTRY.registerEntityType(
            "colossus",
            Colossus::new,
            MobCategory.MONSTER,
            builder -> builder.sized(4.0F, 8.0F).eyeHeight(7.0F).fireImmune().clientTrackingRange(16).noLootTable()
    );

    // Sized and tracked like a snowball.
    public static final DeferredHolder<EntityType<?>, EntityType<BlowpipeBerry>> BLOWPIPE_BERRY = REGISTRY.registerEntityType(
            "blowpipe_berry",
            BlowpipeBerry::new,
            MobCategory.MISC,
            builder -> builder.noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
    );

    // Sized like the vanilla minecart.
    public static final DeferredHolder<EntityType<?>, EntityType<RunicMinecart>> RUNIC_MINECART = REGISTRY.registerEntityType(
            "runic_minecart",
            RunicMinecart::new,
            MobCategory.MISC,
            builder -> builder.noLootTable().sized(0.98F, 0.7F).passengerAttachments(0.1875F).clientTrackingRange(8)
    );

    // Sized and synced like the fishing bobber: it comes back when its owner leaves, so it is never saved.
    public static final DeferredHolder<EntityType<?>, EntityType<GrapplingAnchor>> GRAPPLING_ANCHOR = REGISTRY.registerEntityType(
            "grappling_anchor",
            GrapplingAnchor::new,
            MobCategory.MISC,
            builder -> builder.noLootTable().noSave().noSummon().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5)
    );

    private RREntityTypes() {
    }
}
