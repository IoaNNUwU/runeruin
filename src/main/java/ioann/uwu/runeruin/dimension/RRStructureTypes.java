package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.BaobabStructure;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonStructure;
import ioann.uwu.runeruin.dimension.structures.GiantGobletStructure;
import ioann.uwu.runeruin.dimension.structures.HangingTracksStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRStructureTypes {

    public static final DeferredRegister<StructureType<?>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_TYPE, RR.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<GiantGobletStructure>> GIANT_GOBLET = REGISTRY.register("giant_goblet", () -> () -> GiantGobletStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<BaobabStructure>> BAOBAB = REGISTRY.register("baobab", () -> () -> BaobabStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<DinosaurSkeletonStructure>> DINOSAUR_SKELETON = REGISTRY.register("dinosaur_skeleton", () -> () -> DinosaurSkeletonStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<HangingTracksStructure>> HANGING_TRACKS = REGISTRY.register("hanging_tracks", () -> () -> HangingTracksStructure.CODEC);
}
