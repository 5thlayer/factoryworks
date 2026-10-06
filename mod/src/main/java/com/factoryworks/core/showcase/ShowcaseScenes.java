package com.factoryworks.core.showcase;

import io.github._5thlayer.craftworks.machine.AssemblerBlockEntity;
import io.github._5thlayer.craftworks.machine.AssemblerTier;
import io.github._5thlayer.craftworks.machine.Assemblers;
import io.github._5thlayer.craftworks.machine.ChemicalPlantBlockEntity;
import io.github._5thlayer.craftworks.machine.ChemicalPlants;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.wireworks.WireworksRegistries;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.fluid.BoilerFootprint;
import io.github._5thlayer.wireworks.PoleTier;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.HeldRecipe;
import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import io.github._5thlayer.beltworks.BlockContent;
import io.github._5thlayer.beltworks.model.BeltTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import io.github._5thlayer.pipeworks.PipeworksRegistries;

/**
 * Factories built for filming the Alpha's recruiting clips (#538), each on a smooth-stone floor
 * {@link #WIDTH} by {@link #DEPTH} whose north-west corner is the origin. Each scene feeds itself
 * from stocked chests, so it keeps running with nothing driving it.
 */
public final class ShowcaseScenes {

    public static final int WIDTH = 24;
    public static final int HEIGHT = 8;
    public static final int DEPTH = 16;

    /** Where a scene's product arrives, relative to its origin. */
    public record Product(List<BlockPos> chests, Item item) {

        public boolean arrived(ServerLevel level, BlockPos origin) {
            for (BlockPos chest : chests) {
                if (level.getBlockEntity(origin.offset(chest)) instanceof Container container
                        && container.hasAnyOf(Set.of(item))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final Map<String, Function<Site, Product>> SCENES = Map.of(
            "assembly_line", ShowcaseScenes::assemblyLine,
            "steam_power", ShowcaseScenes::steamPower,
            "oil", ShowcaseScenes::oil);

    private ShowcaseScenes() {
    }

    /** Clears the scene's box, lays its floor and builds it. */
    public static Product build(String scene, ServerLevel level, BlockPos origin) {
        Site site = new Site(level, origin);
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 1; y < HEIGHT; y++) {
                    site.set(new BlockPos(x, y, z), Blocks.AIR);
                }
                site.set(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE);
            }
        }
        return SCENES.get(scene).apply(site);
    }

    /** A scene's blocks, placed relative to its origin. */
    record Site(ServerLevel level, BlockPos origin) {

        BlockPos at(BlockPos relative) {
            return origin.offset(relative);
        }

        void set(BlockPos relative, BlockState state) {
            level.setBlockAndUpdate(at(relative), state);
        }

        void set(BlockPos relative, Block block) {
            set(relative, block.defaultBlockState());
        }

        <T> T blockEntity(BlockPos relative, Class<T> type) {
            return type.cast(level.getBlockEntity(at(relative)));
        }
    }

    /** Copper plates become cable, and cable with iron plates becomes circuits. */
    private static Product assemblyLine(Site site) {
        BlockPos out = new BlockPos(20, 1, 8);

        stockedChest(site, new BlockPos(1, 1, 2), item("factoryworks:copper_plate"));
        loadingBelt(site, new BlockPos(2, 1, 2), Direction.EAST, 10);

        assembling(site, new BlockPos(5, 1, 5), "factoryworks:assembling/copper_cable");
        feeder(site, new BlockPos(5, 1, 3), Direction.SOUTH);
        feeder(site, new BlockPos(7, 1, 5), Direction.EAST);
        belt(site, new BlockPos(8, 1, 5), Direction.EAST, 7);
        feeder(site, new BlockPos(13, 1, 6), Direction.SOUTH);

        stockedChest(site, new BlockPos(1, 1, 11), item("factoryworks:iron_plate"));
        loadingBelt(site, new BlockPos(2, 1, 11), Direction.EAST, 12);

        assembling(site, new BlockPos(13, 1, 8), "factoryworks:assembling/electronic_circuit");
        feeder(site, new BlockPos(13, 1, 10), Direction.NORTH);

        site.set(out, Blocks.CHEST);
        unloader(site, out.west(), Direction.WEST);
        belt(site, new BlockPos(16, 1, 8), Direction.EAST, 3);
        feeder(site, new BlockPos(15, 1, 8), Direction.EAST);

        site.set(new BlockPos(7, 1, 7), WireworksRegistries.CREATIVE_POLE.get());
        site.set(new BlockPos(11, 1, 7), WireworksRegistries.CREATIVE_POLE.get());
        return new Product(List.of(out), item("factoryworks:electronic_circuit"));
    }

    /** Water and coal make steam, steam makes power, and the power smelts iron. */
    private static Product steamPower(Site site) {
        BlockPos pump = new BlockPos(2, 1, 1);
        BlockPos boiler = new BlockPos(3, 1, 3);
        site.set(pump, PFBlocks.OFFSHORE_PUMP.get());
        PFBlocks.BOILER_FOOTPRINT.placeAll(site.level(), site.at(boiler), boilerFacingEast());
        for (int i = 0; i < 2; i++) {
            PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(site.level(), site.at(new BlockPos(6 + i, 1, 3)), Direction.WEST);
        }
        pipe(site, pump.east());
        pipe(site, new BlockPos(5, 1, 3));

        stockedChest(site, new BlockPos(4, 1, 9), item("minecraft:coal"));
        unloader(site, new BlockPos(4, 1, 5), Direction.SOUTH);
        belt(site, new BlockPos(4, 1, 7), Direction.NORTH, 2);
        site.set(new BlockPos(4, 1, 8), loaderState(Direction.NORTH));

        site.set(new BlockPos(8, 1, 6), WireworksRegistries.pole(PoleTier.SMALL).get());
        site.set(new BlockPos(14, 1, 8), WireworksRegistries.pole(PoleTier.MEDIUM).get());

        List<BlockPos> outs = List.of(new BlockPos(12, 1, 6), new BlockPos(16, 1, 6));
        for (BlockPos output : outs) {
            BlockPos furnace = output.south(2);
            site.set(furnace, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
            stockedChest(site, furnace.south(2), item("minecraft:raw_iron"));
            feeder(site, furnace.south(), Direction.NORTH);
            site.set(output, Blocks.CHEST);
            feeder(site, furnace.north(), Direction.NORTH);
        }
        return new Product(outs, item("factoryworks:iron_plate"));
    }

    /** The facing whose Boiler steam port stands east of its anchor, so the steam leaves towards the engines. */
    private static Direction boilerFacingEast() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (PFBlocks.BOILER_FOOTPRINT.positions(BlockPos.ZERO, facing).get(BoilerFootprint.STEAM_PART).equals(BlockPos.ZERO.east())) {
                return facing;
            }
        }
        throw new IllegalStateException("no facing puts a Boiler's steam port to the east");
    }

    /** Crude from a Pumpjack is refined to gas, and gas with coal becomes plastic. */
    private static Product oil(Site site) {
        BlockPos well = new BlockPos(3, 0, 4);
        site.set(well, PFBlocks.OIL_WELL.get());
        site.blockEntity(well, OilWellBlockEntity.class).start(1_500_000);
        PFBlocks.PUMPJACK_FOOTPRINT.placeAll(site.level(), site.at(well.above()), Direction.NORTH);

        BlockPos refinery = new BlockPos(8, 1, 3);
        placeHolding(site, PFBlocks.OIL_REFINERY_FOOTPRINT, refinery,
                "factoryworks:oil_processing/basic_oil_processing");
        BlockPos plant = new BlockPos(14, 1, 8);
        chemicalPlant(site, plant, "factoryworks:chemistry/plastic_bar");

        for (int x = 3; x <= 8; x++) {
            pipe(site, new BlockPos(x, 1, 2));
        }

        // The refinery's gas runs along the roof, down at x=13 and along the floor to the plant's
        // north-west connection.
        for (int x = 8; x <= 13; x++) {
            pipe(site, new BlockPos(x, 5, 3));
        }
        for (int y = 1; y <= 4; y++) {
            pipe(site, new BlockPos(13, y, 3));
        }
        for (int z = 4; z <= 6; z++) {
            pipe(site, new BlockPos(13, 1, z));
        }

        stockedChest(site, new BlockPos(20, 1, 8), item("minecraft:coal"));
        loadingBelt(site, new BlockPos(19, 1, 8), Direction.WEST, 2);
        unloader(site, plant.east(2), Direction.EAST);

        BlockPos out = new BlockPos(14, 1, 15);
        site.set(out, Blocks.CHEST);
        unloader(site, out.north(), Direction.NORTH);
        belt(site, plant.south(3), Direction.SOUTH, 3);
        site.set(plant.south(2), loaderState(Direction.SOUTH));

        site.set(new BlockPos(11, 1, 7), WireworksRegistries.CREATIVE_POLE.get());
        site.set(new BlockPos(6, 1, 7), WireworksRegistries.CREATIVE_POLE.get());
        return new Product(List.of(out), item("factoryworks:plastic_bar"));
    }

    private static void assembling(Site site, BlockPos origin, String recipe) {
        Footprint footprint = Assemblers.footprint(AssemblerTier.ONE);
        List<BlockPos> blocks = footprint.positions(site.at(origin), Direction.NORTH);
        for (int i = 0; i < blocks.size(); i++) {
            site.level().setBlockAndUpdate(blocks.get(i), footprint.stateAt(i, Direction.NORTH));
        }
        AssemblerBlockEntity machine = site.blockEntity(origin, AssemblerBlockEntity.class);
        machine.setHeldRecipe(Identifier.parse(recipe), FakePlayerFactory.getMinecraft(site.level()));
        if (machine.heldRecipe().isEmpty()) {
            throw new IllegalStateException(recipe + " was not held by the Assembler at " + site.at(origin));
        }
    }

    private static void placeHolding(Site site, FootprintMachine footprint, BlockPos anchor, String recipe) {
        footprint.placeAll(site.level(), site.at(anchor), Direction.NORTH);
        AssemblingMachineBlockEntity machine = site.blockEntity(anchor, AssemblingMachineBlockEntity.class);
        machine.setHeldRecipe(HeldRecipe.of(recipe), FakePlayerFactory.getMinecraft(site.level()));
        if (!machine.heldRecipeResolves()) {
            throw new IllegalStateException(recipe + " does not resolve on the machine at " + site.at(anchor));
        }
    }

    private static void chemicalPlant(Site site, BlockPos origin, String recipe) {
        Footprint footprint = ChemicalPlants.footprint();
        List<BlockPos> blocks = footprint.positions(site.at(origin), Direction.NORTH);
        for (int i = 0; i < blocks.size(); i++) {
            site.level().setBlockAndUpdate(blocks.get(i), footprint.stateAt(i, Direction.NORTH));
        }
        ChemicalPlantBlockEntity machine = site.blockEntity(origin, ChemicalPlantBlockEntity.class);
        machine.setHeldRecipe(Identifier.parse(recipe), FakePlayerFactory.getMinecraft(site.level()));
        if (machine.heldRecipe().isEmpty()) {
            throw new IllegalStateException(recipe + " was not held by the Chemical Plant at " + site.at(origin));
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static void stockedChest(Site site, BlockPos at, Item item) {
        site.set(at, Blocks.CHEST);
        Container chest = site.blockEntity(at, Container.class);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(item, 64));
        }
    }

    /** {@code length} tiles flowing {@code flow} from {@code first}, laid downstream first. */
    private static void belt(Site site, BlockPos first, Direction flow, int length) {
        for (int i = length - 1; i >= 0; i--) {
            site.set(first.relative(flow, i), BlockContent.tileFor(BeltTier.BELT).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, flow));
        }
    }

    /** A loader at {@code loader}, its inventory behind it, putting onto a belt flowing {@code flow}. */
    private static void loadingBelt(Site site, BlockPos loader, Direction flow, int length) {
        belt(site, loader.relative(flow), flow, length);
        site.set(loader, loaderState(flow));
    }

    /** A loader facing {@code facing} at the belt that runs into it, emptying into the block behind. */
    private static void unloader(Site site, BlockPos at, Direction facing) {
        site.set(at, loaderState(facing));
    }

    private static BlockState loaderState(Direction facing) {
        return BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    /** Takes from the block behind it and puts into the block in front. */
    private static void feeder(Site site, BlockPos at, Direction facing) {
        site.set(at, BlockContent.FEEDER_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
    }

    private static void pipe(Site site, BlockPos at) {
        site.set(at, PipeworksRegistries.PIPE.get());
    }
}
