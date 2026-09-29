package com.factoryworks.core.gametest;

import com.mojang.logging.LogUtils;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.oil.OilField;
import com.factoryworks.core.oil.OilFieldSource;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.ore.OreResource;
import com.factoryworks.core.ore.OutfieldDisc;
import com.factoryworks.core.worldgen.OilFieldStructure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;

/**
 * Terra's crude-oil fields, resolved out of the server's registry and placed the way
 * {@link OutfieldDiscTests} places a disc (ADR-0081): wells spaced by the collision box, each holding
 * its drawn amount, flush with the ground, never on an ore column, never near origin, never off the
 * land.
 */
final class OilFieldTests {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier SET = Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "oil_field");
    private static final int HOLE = 150;
    /** About 2,300 blocks out, clear of the outfield disc tests' chunks. */
    private static final ChunkPos FAR = new ChunkPos(144, 20);
    private static final ChunkPos UNDER_IRON = new ChunkPos(144, 60);

    private OilFieldTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("oil_field_places_its_wells", 20, OilFieldTests::fieldPlaces);
        tests.test("oil_field_skips_ore_columns", 20, OilFieldTests::fieldSkipsOre);
        tests.test("oil_field_nothing_in_the_hole", 20, OilFieldTests::nothingInTheHole);
        tests.test("oil_field_nothing_off_the_land", 20, OilFieldTests::nothingOffTheLand);
    }

    private static void fieldPlaces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Structure> structure = resolve(helper);
        if (structure == null) {
            return;
        }
        StructureStart start = OutfieldDiscTests.generate(level, structure, FAR);
        if (!start.isValid() || !(start.getPieces().getFirst() instanceof OilFieldSource field)) {
            helper.fail("no oil field generated at " + FAR + ", " + OutfieldDiscTests.distance(FAR) + " from origin");
            return;
        }
        BoundingBox box = start.getBoundingBox();
        OutfieldDiscTests.loadChunks(level, box);
        OutfieldDiscTests.place(level, structure.value(), start, FAR, box);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, box.minX(), box.minZ()) - 1;

        List<BlockPos> placed = wellsIn(level, box, ground, helper);
        if (placed == null) {
            return;
        }
        if (placed.size() != field.wells().size()) {
            helper.fail("the field drew " + field.wells().size() + " wells and placed " + placed.size());
            return;
        }
        for (OilField.Well well : field.wells()) {
            BlockPos pos = new BlockPos(well.x(), ground, well.z());
            if (!(level.getBlockEntity(pos) instanceof OilWellBlockEntity entity)
                    || entity.amount() != well.amount() || entity.initial() != well.amount()) {
                helper.fail("the well at " + pos + " does not hold its drawn " + well.amount());
                return;
            }
            for (OilField.Well other : field.wells()) {
                if (other != well && Math.max(Math.abs(other.x() - well.x()), Math.abs(other.z() - well.z()))
                        < OilField.spacing()) {
                    helper.fail("wells at " + well + " and " + other + " stand closer than " + OilField.spacing());
                    return;
                }
            }
        }
        long total = OilField.total(field.wells());
        LOGGER.info("OIL field at {}, {}: {} wells, {} in all, {}% summed yield",
                field.centreX(), field.centreZ(), placed.size(), total, total * 100 / 300_000);
        helper.succeed();
    }

    /** An iron disc on the same centre keeps its ore; the field's wells stand only where no ore does. */
    private static void fieldSkipsOre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Structure> structure = resolve(helper);
        Holder<Structure> iron = OutfieldDiscTests.resolve(helper, OreResource.IRON);
        if (structure == null || iron == null) {
            return;
        }
        StructureStart disc = OutfieldDiscTests.generate(level, iron, UNDER_IRON);
        StructureStart start = OutfieldDiscTests.generate(level, structure, UNDER_IRON);
        if (!disc.isValid() || !start.isValid()
                || !(disc.getPieces().getFirst() instanceof OutfieldDisc.Source ore)
                || !(start.getPieces().getFirst() instanceof OilFieldSource field)) {
            helper.fail("no iron disc and oil field generated together at " + UNDER_IRON);
            return;
        }
        BoundingBox box = start.getBoundingBox();
        OutfieldDiscTests.loadChunks(level, disc.getBoundingBox());
        OutfieldDiscTests.loadChunks(level, box);
        // The disc first: a well that ignored it would then replace its ore, where one placed first
        // would be overwritten by it and pass unseen.
        OutfieldDiscTests.place(level, iron.value(), disc, UNDER_IRON, disc.getBoundingBox());
        OutfieldDiscTests.place(level, structure.value(), start, UNDER_IRON, box);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, box.minX(), box.minZ()) - 1;

        List<OilField.Well> turnedAway = field.wells().stream().filter(w -> ore.covers(w.x(), w.z())).toList();
        if (turnedAway.isEmpty()) {
            helper.fail("no well of the field fell on the disc, so nothing was turned away");
            return;
        }
        List<BlockPos> placed = wellsIn(level, box, ground, helper);
        if (placed == null) {
            return;
        }
        if (placed.size() != field.wells().size() - turnedAway.size()) {
            helper.fail(placed.size() + " wells stand where " + (field.wells().size() - turnedAway.size())
                    + " of " + field.wells().size() + " are off the disc");
            return;
        }
        for (BlockPos pos : placed) {
            if (ore.covers(pos.getX(), pos.getZ())) {
                helper.fail("a well stands on the disc's column at " + pos);
                return;
            }
        }
        helper.succeed();
    }

    private static void nothingInTheHole(GameTestHelper helper) {
        Holder<Structure> structure = resolve(helper);
        if (structure == null) {
            return;
        }
        int reach = SectionPos.blockToSectionCoord(HOLE) + 1;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                ChunkPos chunk = new ChunkPos(x, z);
                if (OutfieldDiscTests.distance(chunk) <= HOLE
                        && OutfieldDiscTests.generate(helper.getLevel(), structure, chunk).isValid()) {
                    helper.fail("an oil field generated at " + chunk + ", " + OutfieldDiscTests.distance(chunk));
                    return;
                }
            }
        }
        helper.succeed();
    }

    /** The flat world is plains, which the land tag does not hold. */
    private static void nothingOffTheLand(GameTestHelper helper) {
        Holder<Structure> structure = resolve(helper);
        if (structure == null) {
            return;
        }
        if (OutfieldDiscTests.generate(helper.getLevel(), structure, FAR, structure.value().biomes()::contains).isValid()) {
            helper.fail("an oil field generated on the flat world's plains");
            return;
        }
        helper.succeed();
    }

    /** Every oil well in the box, each flush with the ground; null after failing the test. */
    private static List<BlockPos> wellsIn(ServerLevel level, BoundingBox box, int ground, GameTestHelper helper) {
        BlockState well = PFBlocks.OIL_WELL.get().defaultBlockState();
        List<BlockPos> placed = new ArrayList<>();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = ground - 4; y <= ground + 5; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).equals(well)) {
                        if (y != ground) {
                            helper.fail("a well is not flush at " + pos + ": the terrain is at " + ground);
                            return null;
                        }
                        placed.add(pos);
                    }
                }
            }
        }
        return placed;
    }

    private static Holder<Structure> resolve(GameTestHelper helper) {
        var set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceKey.create(Registries.STRUCTURE_SET, SET));
        if (set.isEmpty()) {
            helper.fail("no structure set " + SET);
            return null;
        }
        StructureSet value = set.get().value();
        if (!(value.placement() instanceof RandomSpreadStructurePlacement)) {
            helper.fail(SET + " is not a random spread");
            return null;
        }
        Holder<Structure> structure = value.structures().getFirst().structure();
        if (!(structure.value() instanceof OilFieldStructure)) {
            helper.fail(SET + " holds " + structure.value() + ", not an oil field");
            return null;
        }
        if (!structure.value().biomes().unwrapKey().map(OutfieldDiscTests.LAND::equals).orElse(false)) {
            helper.fail(SET + " is confined to " + structure.value().biomes() + ", not Terra's land");
            return null;
        }
        return structure;
    }
}
