package com.planetaryfactory.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.ore.OutfieldDisc;
import com.planetaryfactory.core.ore.OutfieldLaw;
import com.planetaryfactory.core.ore.OutfieldShape;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * One resource's outfield patch: a disc at the radius Factorio's law gives its own size factor at
 * its centre's distance from origin (ADR-0045).
 *
 * <p>Nothing excludes the ground near origin: a disc there has no quantity, so its radius rounds
 * below one block and nothing generates.
 */
public final class OutfieldDiscStructure extends Structure {

    private static final Codec<OreResource> RESOURCE = Codec.STRING.comapFlatMap(
            key -> {
                try {
                    return DataResult.success(OreResource.of(key));
                } catch (IllegalArgumentException unknown) {
                    return DataResult.error(unknown::getMessage);
                }
            },
            OreResource::key);

    public static final MapCodec<OutfieldDiscStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            RESOURCE.fieldOf("resource").forGetter(structure -> structure.resource))
            .apply(instance, OutfieldDiscStructure::new));

    private final OreResource resource;

    public OutfieldDiscStructure(StructureSettings settings, OreResource resource) {
        super(settings);
        this.resource = resource;
    }

    public OreResource resource() {
        return resource;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        double sizeFactor = OutfieldLaw.of(resource).sizeFactor(context.random().nextDouble());
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        ImprovedNoise noise = new ImprovedNoise(new XoroshiroRandomSource(context.seed()).forkPositional()
                .fromHashOf(Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "outfield/" + resource.key())));
        OutfieldShape shape = OutfieldShape.of(OutfieldLaw.of(resource), sizeFactor, x, z,
                (scaledX, scaledZ) -> noise.noise(scaledX, 0, scaledZ), land(context, y));
        if (shape.isEmpty()) {
            return Optional.empty();
        }
        OutfieldDisc disc = new OutfieldDisc(resource, sizeFactor, shape.blockCount(), x, z);
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> builder.addPiece(
                new OutfieldDiscPiece(disc, shape, context.heightAccessor()))));
    }

    /**
     * Each column's biome, read at the centre's height, against the structure's own predicate:
     * vanilla asks only at the stub, so a coastal disc would otherwise run onto the seabed.
     */
    static OutfieldShape.Land land(GenerationContext context, int y) {
        Map<Long, Boolean> quarts = new HashMap<>();
        int quartY = QuartPos.fromBlock(y);
        return (x, z) -> quarts.computeIfAbsent(ChunkPos.pack(QuartPos.fromBlock(x), QuartPos.fromBlock(z)),
                key -> context.validBiome().test(context.biomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), context.randomState().sampler())));
    }

    @Override
    public StructureType<?> type() {
        return PFWorldgen.OUTFIELD_DISC.get();
    }
}
