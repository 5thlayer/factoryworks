package com.factoryworks.core.showcase;

import io.github._5thlayer.wireworks.WireworksRegistries;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.factoryworks.core.PFBlocks;
import io.github._5thlayer.wireworks.PoleTier;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingTier;
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
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;

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

    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("oritech", "fluid_pipe");

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

        assembling(site, new BlockPos(5, 1, 4), "factoryworks:assembling/copper_cable");
        feeder(site, new BlockPos(5, 1, 3), Direction.SOUTH);
        belt(site, new BlockPos(6, 1, 6), Direction.EAST, 9);
        feeder(site, new BlockPos(6, 1, 5), Direction.SOUTH);

        stockedChest(site, new BlockPos(1, 1, 10), item("factoryworks:iron_plate"));
        loadingBelt(site, new BlockPos(2, 1, 10), Direction.EAST, 12);

        assembling(site, new BlockPos(12, 1, 8), "factoryworks:assembling/electronic_circuit");
        feeder(site, new BlockPos(12, 1, 7), Direction.SOUTH);
        feeder(site, new BlockPos(13, 1, 9), Direction.NORTH);

        site.set(out, Blocks.CHEST);
        unloader(site, out.west(), Direction.WEST);
        belt(site, new BlockPos(15, 1, 8), Direction.EAST, 4);
        feeder(site, new BlockPos(14, 1, 8), Direction.EAST);

        site.set(new BlockPos(10, 1, 5), WireworksRegistries.CREATIVE_POLE.get());
        return new Product(List.of(out), item("factoryworks:electronic_circuit"));
    }

    /** Water and coal make steam, steam makes power, and the power smelts iron. */
    private static Product steamPower(Site site) {
        BlockPos pump = new BlockPos(2, 1, 3);
        BlockPos boiler = new BlockPos(4, 1, 3);
        site.set(pump, PFBlocks.OFFSHORE_PUMP.get());
        site.set(boiler, PFBlocks.BOILER.get());
        for (int i = 0; i < 2; i++) {
            PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(site.level(), site.at(new BlockPos(6 + i, 1, 3)), Direction.WEST);
        }
        extractingPipe(site, pump.east(), Direction.WEST);
        extractingPipe(site, boiler.east(), Direction.WEST);

        stockedChest(site, new BlockPos(4, 1, 9), item("minecraft:coal"));
        unloader(site, boiler.south(), Direction.SOUTH);
        belt(site, new BlockPos(4, 1, 7), Direction.NORTH, 3);
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

    /** Crude from a Pumpjack is refined to gas, and gas with coal becomes plastic. */
    private static Product oil(Site site) {
        BlockPos well = new BlockPos(3, 0, 4);
        site.set(well, PFBlocks.OIL_WELL.get());
        site.blockEntity(well, OilWellBlockEntity.class).start(1_500_000);
        PFBlocks.PUMPJACK_FOOTPRINT.placeAll(site.level(), site.at(well.above()), Direction.NORTH);

        BlockPos refinery = new BlockPos(8, 1, 3);
        placeHolding(site, PFBlocks.OIL_REFINERY_FOOTPRINT, refinery,
                "factoryworks:oil_processing/basic_oil_processing");
        BlockPos plant = new BlockPos(11, 1, 3);
        placeHolding(site, PFBlocks.CHEMICAL_PLANT_FOOTPRINT, plant, "factoryworks:chemistry/plastic_bar");

        // Pipeworks carries the crude off the Pumpjack; Oritech's pipe takes it from the last of them to the Refinery.
        for (int x = 3; x <= 6; x++) {
            site.set(new BlockPos(x, 1, 2), PipeworksRegistries.PIPE.get());
        }
        extractingPipe(site, new BlockPos(7, 1, 2), Direction.WEST);
        pipe(site, new BlockPos(8, 1, 2));

        extractingPipe(site, refinery.above(4), Direction.DOWN);
        pipe(site, new BlockPos(9, 5, 3));
        pipe(site, new BlockPos(10, 5, 3));
        pipe(site, new BlockPos(11, 5, 3));
        pipe(site, new BlockPos(11, 4, 3));
        pipe(site, new BlockPos(11, 3, 3));

        stockedChest(site, new BlockPos(16, 1, 3), item("minecraft:coal"));
        unloader(site, plant.east(), Direction.EAST);
        loadingBelt(site, new BlockPos(15, 1, 3), Direction.WEST, 2);

        BlockPos out = new BlockPos(11, 1, 9);
        site.set(out, Blocks.CHEST);
        unloader(site, out.north(), Direction.NORTH);
        belt(site, plant.south(2), Direction.SOUTH, 3);
        site.set(plant.south(), loaderState(Direction.SOUTH));

        site.set(new BlockPos(6, 1, 7), WireworksRegistries.CREATIVE_POLE.get());
        return new Product(List.of(out), item("factoryworks:plastic_bar"));
    }

    private static void assembling(Site site, BlockPos anchor, String recipe) {
        placeHolding(site, PFBlocks.assemblingFootprint(AssemblingTier.ONE), anchor, recipe);
    }

    private static void placeHolding(Site site, FootprintMachine footprint, BlockPos anchor, String recipe) {
        footprint.placeAll(site.level(), site.at(anchor), Direction.NORTH);
        AssemblingMachineBlockEntity machine = site.blockEntity(anchor, AssemblingMachineBlockEntity.class);
        machine.setHeldRecipe(HeldRecipe.of(recipe), FakePlayerFactory.getMinecraft(site.level()));
        if (!machine.heldRecipeResolves()) {
            throw new IllegalStateException(recipe + " does not resolve on the machine at " + site.at(anchor));
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
        Block block = BuiltInRegistries.BLOCK.getValue(FLUID_PIPE);
        site.level().setBlockAndUpdate(site.at(at), ((AbstractPipeBlock) block)
                .addConnectionStates(block.defaultBlockState(), site.level(), site.at(at), true));
    }

    /** A pipe that pulls from its {@code from} side, the side a player's click sets to extract. */
    private static void extractingPipe(Site site, BlockPos at, Direction from) {
        pipe(site, at);
        BlockState state = site.level().getBlockState(site.at(at));
        GenericPipeBlock pipe = (GenericPipeBlock) state.getBlock();
        site.set(at, state.setValue(pipe.directionToProperty(from), ExtractablePipeConnectionBlock.EXTRACT));
    }
}
