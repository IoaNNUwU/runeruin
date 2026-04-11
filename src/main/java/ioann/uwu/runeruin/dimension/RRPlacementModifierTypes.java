package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.placements.WallPlacementFilter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRPlacementModifierTypes {

    public static final DeferredRegister<PlacementModifierType<?>> REGISTRY = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, RR.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<WallPlacementFilter>> WALL_FILTER = REGISTRY.register(
            "wall_placement_filter",
            () -> () -> WallPlacementFilter.CODEC
    );
}
