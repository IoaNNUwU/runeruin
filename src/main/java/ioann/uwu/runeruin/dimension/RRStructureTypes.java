package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.dimension.structures.GiantGobletStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RRStructureTypes {

    public static final DeferredRegister<StructureType<?>> REGISTRY = DeferredRegister.create(Registries.STRUCTURE_TYPE, RR.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_REGISTRY = DeferredRegister.create(Registries.STRUCTURE_PIECE, RR.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<GiantGobletStructure>> GIANT_GOBLET = REGISTRY.register("giant_goblet", () -> () -> GiantGobletStructure.CODEC);

    /*
    public static final DeferredHolder<StructurePieceType, StructurePieceType> GIANT_GOBLET_PIECE = PIECE_REGISTRY.register(
            "giant_goblet_piece", new Supplier<StructurePieceType>() {
                @Override
                public StructurePieceType get() {
                    return new GiantGobletPiece();
                }
            }
    );
     */
}
