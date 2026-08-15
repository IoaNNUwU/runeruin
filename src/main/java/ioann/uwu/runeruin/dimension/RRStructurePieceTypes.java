package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRStructurePieceTypes {

    public static final DeferredRegister<StructurePieceType> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_PIECE, RR.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> GIANT_GOBLET_PIECE = REGISTRY.register("giant_goblet_piece", () -> (StructurePieceType.ContextlessType) GiantGobletPiece::new);
}
