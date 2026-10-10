package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.entities.RREntityTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(RR.MODID);

    static {
        // The wispberry was the mossberry once: worlds saved before the rename keep their berries.
        REGISTRY.addAlias(RR.id("moss_berry"), RR.id("wispberry"));
    }

    public static final DeferredItem<Item> RUNE_OF_SPACE = REGISTRY.registerItem(
            "rune_of_space",
            RuneOfSpaceItem::new,
            p -> p.food(
                    new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(1)
                            .saturationModifier(2f)
                            .build()
            )
    );

    public static final DeferredItem<Item> WISPBERRY = REGISTRY.registerItem(
            "wispberry",
            p -> new BlockItem(RRBlocks.WISPBERRY_BUSH.get(), p.useItemDescriptionPrefix()),
            p -> p.food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.3f)
                    .build(),
                    Consumable.builder()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(
                                    new MobEffectInstance(
                                            MobEffects.NIGHT_VISION,
                                            10 * 20,
                                            0
                                    )
                            ))
                            .build())
    );

    /** Picked from the plants of a moss layer and planted back on one; see {@code MossLayerBlock}. */
    public static final DeferredItem<Item> MOSSBERRY = REGISTRY.registerItem(
            "mossberry",
            Item::new,
            p -> p.food(Foods.SWEET_BERRIES)
    );

    public static final DeferredItem<Item> POWDERED_MOSS_BUCKET = REGISTRY.registerItem(
            "powdered_moss_bucket",
            p -> new SolidBucketItem(RRBlocks.POWDERED_MOSS.get(), SoundEvents.BUCKET_EMPTY_POWDER_SNOW, p),
            p -> p.stacksTo(1).useItemDescriptionPrefix()
    );

    public static final DeferredItem<Item> SNAIL_SPAWN_EGG = REGISTRY.registerItem(
            "snail_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(RREntityTypes.SNAIL.get())
    );
}
