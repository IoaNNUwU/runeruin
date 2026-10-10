package ioann.uwu.runeruin.loottables;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class RRLootTables {

    public static final ResourceKey<LootTable> HARVEST_WISPBERRY = RR.resourceKey(Registries.LOOT_TABLE, "harvest/wispberry");

    public static final ResourceKey<LootTable> HANGING_TRACKS_SUPPLY = RR.resourceKey(Registries.LOOT_TABLE, "chests/hanging_tracks_supply");
    public static final ResourceKey<LootTable> HANGING_TRACKS_TREASURE = RR.resourceKey(Registries.LOOT_TABLE, "chests/hanging_tracks_treasure");
}
