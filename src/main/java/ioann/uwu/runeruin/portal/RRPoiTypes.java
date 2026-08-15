package ioann.uwu.runeruin.portal;

import com.google.common.collect.ImmutableSet;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRPoiTypes {

    public static final DeferredRegister<PoiType> REGISTRY = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, RR.MODID);

    public static final DeferredHolder<PoiType, PoiType> RUNE_RUIN_PORTAL = REGISTRY.register(
            "rune_ruin_portal",
            () -> new PoiType(
                    ImmutableSet.copyOf(RRBlocks.RUNE_RUIN_PORTAL.get().getStateDefinition().getPossibleStates()),
                    0,
                    1
            )
    );
}
