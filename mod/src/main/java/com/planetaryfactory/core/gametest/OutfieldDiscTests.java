package com.planetaryfactory.core.gametest;

import com.mojang.logging.LogUtils;
import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.ore.OreBlock;
import com.planetaryfactory.core.ore.OreDelta;
import com.planetaryfactory.core.ore.OreFields;
import com.planetaryfactory.core.ore.OreMining;
import com.planetaryfactory.core.ore.OreResource;
import com.planetaryfactory.core.ore.OutfieldDisc;
import com.planetaryfactory.core.ore.OutfieldLaw;
import com.planetaryfactory.core.radar.ChartDeliveries;
import com.planetaryfactory.core.radar.MarkerDelivery;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.RadarChartData;
import com.planetaryfactory.core.radar.Sector;
import com.planetaryfactory.core.worldgen.OutfieldDiscStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;

/**
 * Terra's outfield discs, resolved out of the server's registry and placed (#320, ADR-0045).
 *
 * <p>The disc is generated far enough out that the flat GameTest world never overlaps another
 * test, with a tree standing on its centre, and placed chunk by chunk the way {@code /place} does.
 * The start is also recorded in its chunk, which {@code /place} skips and worldgen does not, so a
 * broken block reads its amount through the same structure manager a real world would.
 */
final class OutfieldDiscTests {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final OreResource[] RESOURCES = {
        OreResource.COAL, OreResource.COPPER, OreResource.IRON, OreResource.STONE, OreResource.URANIUM};

    private static final TagKey<Biome> LAND = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "terra_land"));

    private static final int HOLE = 150;

    private OutfieldDiscTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        for (int i = 0; i < RESOURCES.length; i++) {
            OreResource resource = RESOURCES[i];
            ChunkPos far = new ChunkPos(100 + 8 * i, 100);
            tests.test("outfield_" + resource.key() + "_disc_places", 20, helper -> discPlaces(helper, resource, far));
            tests.test("outfield_" + resource.key() + "_nothing_in_the_hole", 20,
                    helper -> nothingInTheHole(helper, resource));
            tests.test("outfield_" + resource.key() + "_nothing_off_the_land", 20,
                    helper -> nothingOffTheLand(helper, resource, far));
        }
        tests.test("outfield_last_block_removes_its_marker", 20,
                helper -> lastBlockRemovesItsMarker(helper, OreResource.IRON, new ChunkPos(100, 140)));
    }

    /**
     * A patch's marker is sent with what it holds, and exactly once more with nothing when its last
     * block goes (#371). The first block is mined out and the last is mined to the end, so a mined-out
     * block counted twice ends the patch a block early; the rest are broken outright, the route by
     * which a patch loses units nobody drew.
     */
    private static void lastBlockRemovesItsMarker(GameTestHelper helper, OreResource resource, ChunkPos chunk) {
        ServerLevel level = helper.getLevel();
        Holder<Structure> structure = resolve(helper, resource);
        if (structure == null) {
            return;
        }
        StructureStart start = generate(level, structure, chunk);
        if (!start.isValid() || !(start.getPieces().getFirst() instanceof OutfieldDisc.Source source)) {
            helper.fail(resource.key() + " generated no disc at " + chunk);
            return;
        }
        OutfieldDisc disc = source.disc();
        BoundingBox box = start.getBoundingBox();
        loadChunks(level, box);
        place(level, structure.value(), start, chunk, box);
        List<BlockPos> ores = new ArrayList<>();
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, box.minX(), box.minZ()) - 1;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, ground, z);
                if (level.getBlockState(pos).getBlock() instanceof OreBlock) {
                    ores.add(pos);
                }
            }
        }

        if (ores.size() < 2) {
            helper.fail(resource.key() + "'s disc at " + chunk + " placed " + ores.size() + " blocks");
            return;
        }
        UUID player = UUID.randomUUID();
        RadarChartData data = RadarChartData.get(level.getServer());
        try {
            lastBlockRemovesItsMarker(helper, level, data, player, chunk, disc, ores);
        } finally {
            data.logout(player);
        }
    }

    private static void lastBlockRemovesItsMarker(GameTestHelper helper, ServerLevel level, RadarChartData data,
            UUID player, ChunkPos chunk, OutfieldDisc disc, List<BlockPos> ores) {
        String dimension = level.dimension().identifier().toString();
        MarkerDelivery.Amounts amounts = ChartDeliveries.amounts(level.getServer());
        data.observe(player, player, dimension);
        Sector sector = Sector.ofBlock(chunk.getMinBlockX(), chunk.getMinBlockZ());
        List<PatchMarker> found = ChartDeliveries.patchesStartedIn(level, sector,
                List.of(level.getChunk(chunk.x(), chunk.z())));
        data.chart(player, dimension, sector, found);
        List<MarkerDelivery.Update> charted = data.takeMarkers(player, amounts);
        if (found.size() != 1 || charted.size() != 1 || charted.getFirst().amount() != disc.total()) {
            helper.fail("charting the disc's sector found " + found + " and sent " + charted
                    + ", for a disc holding " + disc.total());
            return;
        }
        PatchMarker marker = found.getFirst();

        BlockPos first = ores.removeFirst();
        BlockPos last = ores.removeLast();
        OreBlock ore = (OreBlock) level.getBlockState(last).getBlock();
        for (int unit = 0; unit < disc.amountPerBlock(); unit++) {
            OreMining.draw(level, ore, first);
        }
        for (BlockPos pos : ores) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        data.rescanned(player, dimension, List.of(sector));
        List<MarkerDelivery.Update> broken = data.takeMarkers(player, amounts);
        if (!broken.equals(List.of(new MarkerDelivery.Update(marker, disc.amountPerBlock())))) {
            helper.fail("with one block of " + disc.blockCount() + " left, a re-scan sent " + broken);
            return;
        }

        for (int unit = 0; unit < disc.amountPerBlock(); unit++) {
            if (!data.takeMarkers(player, amounts).isEmpty()) {
                helper.fail("a removal was sent with " + (disc.amountPerBlock() - unit) + " units left");
                return;
            }
            OreMining.draw(level, ore, last);
        }
        List<MarkerDelivery.Update> removal = data.takeMarkers(player, amounts);
        data.rescanned(player, dimension, List.of(sector));
        List<MarkerDelivery.Update> after = data.takeMarkers(player, amounts);
        if (!level.getBlockState(last).is(Blocks.STONE)
                || !removal.equals(List.of(new MarkerDelivery.Update(marker, 0))) || !after.isEmpty()) {
            helper.fail("mining out the last block left " + level.getBlockState(last) + ", sent " + removal
                    + " and then " + after);
            return;
        }
        helper.succeed();
    }

    private static void discPlaces(GameTestHelper helper, OreResource resource, ChunkPos chunk) {
        ServerLevel level = helper.getLevel();
        Holder<Structure> structure = resolve(helper, resource);
        if (structure == null) {
            return;
        }
        StructureStart start = generate(level, structure, chunk);
        if (!start.isValid() || !(start.getPieces().getFirst() instanceof OutfieldDisc.Source source)) {
            helper.fail(resource.key() + " generated no disc at " + chunk + ", " + distance(chunk) + " from origin");
            return;
        }
        OutfieldDisc disc = source.disc();
        BoundingBox box = start.getBoundingBox();
        loadChunks(level, box);

        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, box.minX(), box.minZ()) - 1;
        BlockPos trunk = new BlockPos(disc.centreX(), ground + 1, disc.centreZ());
        for (int y = 0; y < 3; y++) {
            level.setBlockAndUpdate(trunk.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        level.setBlockAndUpdate(trunk.above(3), Blocks.OAK_LEAVES.defaultBlockState());

        int fieldsBefore = level.getDataStorage().computeIfAbsent(OreFields.TYPE).fields().size();
        place(level, structure.value(), start, chunk, box);

        BlockState ore = PFBlocks.ore(resource).get().defaultBlockState();
        int placed = 0;
        double farthest = 0;
        BlockPos farthestOre = null;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int ores = 0;
                for (int y = ground - 4; y <= ground + 5; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof OreBlock)) {
                        continue;
                    }
                    ores++;
                    if (state != ore) {
                        helper.fail(resource.key() + "'s disc placed " + state + " at " + pos);
                        return;
                    }
                    if (y != ground) {
                        helper.fail(resource.key() + "'s disc is not flush at " + pos + ": the terrain is at " + ground);
                        return;
                    }
                    double r = Math.hypot(x - disc.centreX(), z - disc.centreZ());
                    if (r >= farthest) {
                        farthest = r;
                        farthestOre = pos;
                    }
                }
                if (ores > 1) {
                    helper.fail(resource.key() + "'s disc is " + ores + " deep at " + x + ", " + z);
                    return;
                }
                if ((ores == 1) != source.covers(x, z)) {
                    helper.fail(resource.key() + "'s disc " + (ores == 1 ? "placed" : "missed") + " " + x + ", " + z);
                    return;
                }
                placed += ores;
            }
        }
        if (placed != disc.blockCount()) {
            helper.fail(resource.key() + "'s piece stored " + disc.blockCount() + " blocks and " + placed + " were placed");
            return;
        }
        if (!level.getBlockState(trunk.below()).is(ore.getBlock()) || !level.getBlockState(trunk).is(Blocks.OAK_LOG)) {
            helper.fail(resource.key() + "'s disc did not land under the tree at " + trunk);
            return;
        }

        double radius = OutfieldLaw.of(resource).radius(disc.sizeFactor(), disc.distance());
        if (farthest < 0.5 * radius || farthest > 1.8 * radius) {
            helper.fail(resource.key() + "'s disc reaches " + farthest + " for a law radius of " + radius
                    + " at size " + disc.sizeFactor() + ", " + disc.distance() + " from origin");
            return;
        }

        OreFields fields = level.getDataStorage().computeIfAbsent(OreFields.TYPE);
        if (fields.fields().size() != fieldsBefore || fields.startingAmount(resource, farthestOre).isPresent()) {
            helper.fail(resource.key() + "'s disc was recorded in the pack's saved data");
            return;
        }
        int amount = disc.amountPerBlock();
        OreDelta.Draw draw = OreMining.draw(level, (OreBlock) ore.getBlock(), farthestOre);
        if (amount <= 0 || draw.paid() != 1 || draw.remaining() != amount - 1) {
            helper.fail(resource.key() + " at " + farthestOre + " drew " + draw + " from a disc holding " + amount + " a block");
            return;
        }
        if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(resource.drop()))) {
            helper.fail(resource.key() + " pays out " + resource.drop() + ", which no mod registers, so every draw pays air");
            return;
        }
        LOGGER.info("OUTFIELD {} disc at {}, {}: size {}, {} blocks, law radius {}, reaches {}, {} a block",
                resource.key(), disc.centreX(), disc.centreZ(), disc.sizeFactor(), placed, radius, farthest, amount);
        helper.succeed();
    }

    private static void nothingInTheHole(GameTestHelper helper, OreResource resource) {
        Holder<Structure> structure = resolve(helper, resource);
        if (structure == null) {
            return;
        }
        int reach = SectionPos.blockToSectionCoord(HOLE) + 1;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (distance(chunk) <= HOLE && generate(helper.getLevel(), structure, chunk).isValid()) {
                    helper.fail(resource.key() + " generated at " + chunk + ", " + distance(chunk) + " from origin");
                    return;
                }
            }
        }
        helper.succeed();
    }

    /** The flat world is plains, which the land tag does not hold. */
    private static void nothingOffTheLand(GameTestHelper helper, OreResource resource, ChunkPos chunk) {
        Holder<Structure> structure = resolve(helper, resource);
        if (structure == null) {
            return;
        }
        if (generate(helper.getLevel(), structure, chunk, structure.value().biomes()::contains).isValid()) {
            helper.fail(resource.key() + " generated on the flat world's plains");
            return;
        }
        helper.succeed();
    }

    private static Holder<Structure> resolve(GameTestHelper helper, OreResource resource) {
        Identifier id = Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "outfield_" + resource.key());
        var set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceKey.create(Registries.STRUCTURE_SET, id));
        if (set.isEmpty()) {
            helper.fail("no structure set " + id);
            return null;
        }
        StructureSet value = set.get().value();
        if (!(value.placement() instanceof RandomSpreadStructurePlacement)) {
            helper.fail(id + " is not a random spread");
            return null;
        }
        Holder<Structure> structure = value.structures().getFirst().structure();
        if (!(structure.value() instanceof OutfieldDiscStructure disc) || disc.resource() != resource) {
            helper.fail(id + " holds " + structure.value() + ", not " + resource.key() + "'s disc");
            return null;
        }
        if (!structure.value().biomes().unwrapKey().map(LAND::equals).orElse(false)) {
            helper.fail(id + " is confined to " + structure.value().biomes() + ", not " + LAND.location());
            return null;
        }
        return structure;
    }

    private static StructureStart generate(ServerLevel level, Holder<Structure> structure, ChunkPos chunk) {
        // The flat GameTest world has no Terra biome; the tag is asserted in resolve().
        return generate(level, structure, chunk, biome -> true);
    }

    private static StructureStart generate(ServerLevel level, Holder<Structure> structure, ChunkPos chunk,
            Predicate<Holder<Biome>> validBiome) {
        var generator = level.getChunkSource().getGenerator();
        return structure.value().generate(structure, level.dimension(), level.registryAccess(), generator,
                generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                level.getSeed(), chunk, 0, level, validBiome);
    }

    private static void loadChunks(ServerLevel level, BoundingBox box) {
        for (int x = SectionPos.blockToSectionCoord(box.minX()); x <= SectionPos.blockToSectionCoord(box.maxX()); x++) {
            for (int z = SectionPos.blockToSectionCoord(box.minZ()); z <= SectionPos.blockToSectionCoord(box.maxZ()); z++) {
                level.getChunk(x, z);
            }
        }
    }

    private static void place(ServerLevel level, Structure structure, StructureStart start, ChunkPos origin,
            BoundingBox box) {
        level.getChunk(origin.x(), origin.z()).setStartForStructure(structure, start);
        for (int x = SectionPos.blockToSectionCoord(box.minX()); x <= SectionPos.blockToSectionCoord(box.maxX()); x++) {
            for (int z = SectionPos.blockToSectionCoord(box.minZ()); z <= SectionPos.blockToSectionCoord(box.maxZ()); z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                level.getChunk(x, z).addReferenceForStructure(structure, origin.pack());
                start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        level.getRandom(), new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                                chunk.getMaxBlockX(), level.getMaxY(), chunk.getMaxBlockZ()), chunk);
            }
        }
    }

    private static double distance(ChunkPos chunk) {
        return Math.hypot(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
    }
}
