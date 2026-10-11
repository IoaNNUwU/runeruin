package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.entities.RREntityTypes;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemLore;
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

    public static final DeferredItem<Item> MOSSBERRY = REGISTRY.registerItem(
            "mossberry",
            p -> new BlockItem(RRBlocks.MOSSBERRY_BUSH.get(), p.useItemDescriptionPrefix()),
            p -> p.food(Foods.SWEET_BERRIES)
    );

    public static final DeferredItem<Item> POWDERED_MOSS_BUCKET = REGISTRY.registerItem(
            "powdered_moss_bucket",
            p -> new SolidBucketItem(RRBlocks.POWDERED_MOSS.get(), SoundEvents.BUCKET_EMPTY_POWDER_SNOW, p),
            p -> p.stacksTo(1).useItemDescriptionPrefix()
    );

    public static final DeferredItem<Item> RUNIC_MINECART = REGISTRY.registerItem(
            "runic_minecart",
            p -> new MinecartItem(RREntityTypes.RUNIC_MINECART.get(), p),
            p -> p.stacksTo(1).component(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item.runeruin.runic_minecart.tooltip")
                            .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false))
            )))
    );

    public static final DeferredItem<Item> MOSSY_PROPHET_SPAWN_EGG = REGISTRY.registerItem(
            "mossy_prophet_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(RREntityTypes.MOSSY_PROPHET.get())
    );

    public static final DeferredItem<Item> MOSSY_WARRIOR_SPAWN_EGG = REGISTRY.registerItem(
            "mossy_warrior_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(RREntityTypes.MOSSY_WARRIOR.get())
    );

    public static final DeferredItem<Item> SNAIL_SPAWN_EGG = REGISTRY.registerItem(
            "snail_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(RREntityTypes.SNAIL.get())
    );
}
