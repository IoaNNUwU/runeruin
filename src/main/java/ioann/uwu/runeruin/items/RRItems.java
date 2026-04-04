package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(RR.MODID);

    public static final DeferredItem<Item> RUNE_OF_SPACE = REGISTRY.registerSimpleItem("rune_of_space", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));
}
