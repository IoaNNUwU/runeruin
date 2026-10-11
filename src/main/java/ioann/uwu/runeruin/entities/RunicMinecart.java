package ioann.uwu.runeruin.entities;

import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A minecart that keeps its speed on any rail while someone rides it ({@code RunicRailMixin}). */
public class RunicMinecart extends Minecart {

    public RunicMinecart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return RRItems.RUNIC_MINECART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RRItems.RUNIC_MINECART.get());
    }
}
