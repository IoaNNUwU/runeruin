package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(RR.MODID);

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

    public static final DeferredItem<Item> MOSS_BERRY = REGISTRY.registerItem(
            "moss_berry",
            p -> new BlockItem(RRBlocks.MOSS_BERRY_BUSH.get(), p.useItemDescriptionPrefix()),
            p -> p.food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.1f)
                    .build(),
                    Consumable.builder()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(
                                    new MobEffectInstance(
                                            MobEffects.POISON,
                                            5 * 20,
                                            0
                                    )
                            ))
                            .build())
    );
}
