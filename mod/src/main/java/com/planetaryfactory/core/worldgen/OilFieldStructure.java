package com.planetaryfactory.core.worldgen;

import com.mojang.serialization.MapCodec;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.oil.OilField;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/** A crude-oil field: crude's outfield shape, with a well on the columns its draw passes (ADR-0081). */
public final class OilFieldStructure extends Structure {

    public static final MapCodec<OilFieldStructure> CODEC = simpleCodec(OilFieldStructure::new);

    public OilFieldStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        ImprovedNoise noise = new ImprovedNoise(new XoroshiroRandomSource(context.seed()).forkPositional()
                .fromHashOf(Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "outfield/crude_oil")));
        OilField field = OilField.of(x, z, (scaledX, scaledZ) -> noise.noise(scaledX, 0, scaledZ),
                OutfieldDiscStructure.land(context, y));
        if (field.shape().isEmpty()) {
            return Optional.empty();
        }
        List<OilField.Well> wells = field.draw(context.random()::nextDouble);
        if (wells.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(
                new OilFieldPiece(x, z, field.shape().reach(), wells, context.heightAccessor()))));
    }

    @Override
    public StructureType<?> type() {
        return PFWorldgen.OIL_FIELD.get();
    }
}
