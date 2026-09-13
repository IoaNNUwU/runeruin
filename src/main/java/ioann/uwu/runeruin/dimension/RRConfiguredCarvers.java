package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

import static ioann.uwu.runeruin.dimension.Const.*;

public class RRConfiguredCarvers {

    public static final ResourceKey<ConfiguredWorldCarver<?>> TOP_LAYER_CAVES = RR.resourceKey(Registries.CONFIGURED_CARVER, "top_layer_caves");

    public static void bootstrap(BootstrapContext<ConfiguredWorldCarver<?>> ctx) {
        HolderGetter<Block> blocks = ctx.lookup(Registries.BLOCK);
        HolderSet<Block> replaceable = blocks.getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES);
        int minY = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_OFFSET + 1;
        int maxY = TOP_LAYER_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_OFFSET + TOP_LAYER_TERRAIN_HEIGHT - 8;

        ctx.register(TOP_LAYER_CAVES, WorldCarver.CAVE.configured(new CaveCarverConfiguration(
                0.24f,
                UniformHeight.of(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)),
                UniformFloat.of(0.8415f, 1.309f),
                VerticalAnchor.aboveBottom(8),
                replaceable,
                UniformFloat.of(0.675f, 1.2f),
                UniformFloat.of(0.6545f, 1.2155f),
                UniformFloat.of(-1f, -0.4f)
        )));
    }
}
