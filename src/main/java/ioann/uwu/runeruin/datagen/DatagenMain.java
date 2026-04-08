package ioann.uwu.runeruin.datagen;


import ioann.uwu.runeruin.RR;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = RR.MODID)
public class DatagenMain {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        gen.addProvider(true, new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(DatagenBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider
        ));
        gen.addProvider(true, new DatagenModelProvider(packOutput));
        gen.addProvider(true, new DatagenBlockTagProvider(packOutput, lookupProvider));

        gen.addProvider(true, new DatagenWorldGenProvider(packOutput, lookupProvider));
    }
}
