package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.HangingPlatformPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRStructureProcessors {

    public static final DeferredRegister<MapCodec<? extends StructureProcessor>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<HangingPlatformPiece.NetworkProcessor>> HANGING_TRACKS_NETWORK =
            REGISTRY.register("hanging_tracks_network", () -> HangingPlatformPiece.NetworkProcessor.CODEC);
}
