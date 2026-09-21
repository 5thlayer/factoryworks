package com.planetaryfactory.core.worldgen;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The worldgen pieces the pack's data refers to by id.
 *
 * <p>The processor that puts Terra's starting patches on the ground rather than on the canopy, and
 * the outfield disc's structure type and piece. They are registered here rather than in the
 * datapack because they are code -- ADR-0015's line.
 */
public final class PFWorldgen {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<GroundProcessor>>
            GROUND_PROCESSOR = PROCESSORS.register("ground", () -> () -> GroundProcessor.CODEC);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<StructureType<?>, StructureType<OutfieldDiscStructure>> OUTFIELD_DISC =
            STRUCTURE_TYPES.register("outfield_disc", () -> () -> OutfieldDiscStructure.CODEC);

    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, PlanetaryFactoryCore.NAMESPACE);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> OUTFIELD_DISC_PIECE =
            PIECES.register("outfield_disc", () -> (StructurePieceType.ContextlessType) OutfieldDiscPiece::new);

    private PFWorldgen() {
    }

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
