package com.factoryworks.core.gametest;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.PFItems;
import com.factoryworks.core.energy.PoleTier;
import com.factoryworks.core.machine.AssemblingMachineBlockEntity;
import com.factoryworks.core.machine.AssemblingTier;
import com.factoryworks.core.machine.OilRefineryBlockEntity;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import io.github._5thlayer.beltworks.BlockContent;
import io.github._5thlayer.beltworks.model.BeltTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;

/**
 * Factories built for filming the Alpha's recruiting clips (#538). Each scene feeds itself, because
 * a test's callbacks stop when it succeeds and the scene is meant to keep running after.
 */
final class ShowcaseScenes {

    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("oritech", "fluid_pipe");

    private ShowcaseScenes() {
    }

    /** Each scene's instance is `data/factoryworks_showcase/test_instance/<scene>.json`. */
    static void defineBodies() {
        define("assembly_line", ShowcaseScenes::assemblyLine);
        define("steam_power", ShowcaseScenes::steamPower);
        define("oil", ShowcaseScenes::oil);
    }

    private static void define(String name, java.util.function.Consumer<GameTestHelper> body) {
        PFGameTestInstance.define(Identifier.fromNamespaceAndPath("factoryworks_showcase", name), body);
    }

    /** Copper plates become cable, and cable with iron plates becomes circuits. */
    private static void assemblyLine(GameTestHelper helper) {
        BlockPos out = new BlockPos(20, 1, 8);

        stockedChest(helper, new BlockPos(1, 1, 2), item("ftbmaterials:copper_plate"));
        loadingBelt(helper, new BlockPos(2, 1, 2), Direction.EAST, 10);

        ChassisFixture cable = ChassisFixture.assembling(AssemblingTier.ONE, new BlockPos(5, 1, 4), Direction.NORTH);
        cable.hold(helper, cable.placeWhole(helper, AssemblingMachineBlockEntity.class),
                "factoryworks:assembling/copper_cable");
        feeder(helper, new BlockPos(5, 1, 3), Direction.SOUTH);
        belt(helper, new BlockPos(6, 1, 6), Direction.EAST, 9);
        feeder(helper, new BlockPos(6, 1, 5), Direction.SOUTH);

        stockedChest(helper, new BlockPos(1, 1, 10), item("ftbmaterials:iron_plate"));
        loadingBelt(helper, new BlockPos(2, 1, 10), Direction.EAST, 12);

        ChassisFixture circuit = ChassisFixture.assembling(AssemblingTier.ONE, new BlockPos(12, 1, 8), Direction.NORTH);
        circuit.hold(helper, circuit.placeWhole(helper, AssemblingMachineBlockEntity.class),
                "factoryworks:assembling/electronic_circuit");
        feeder(helper, new BlockPos(12, 1, 7), Direction.SOUTH);
        feeder(helper, new BlockPos(13, 1, 9), Direction.NORTH);

        helper.setBlock(out, Blocks.CHEST);
        unloader(helper, out.west(), Direction.WEST);
        belt(helper, new BlockPos(15, 1, 8), Direction.EAST, 4);
        feeder(helper, new BlockPos(14, 1, 8), Direction.EAST);

        helper.setBlock(new BlockPos(10, 1, 5), PFBlocks.CREATIVE_POLE.get());
        succeedWhenChestHolds(helper, out, item("factoryworks:electronic_circuit"));
    }

    /** Water and coal make steam, steam makes power, and the power smelts iron. */
    private static void steamPower(GameTestHelper helper) {
        BlockPos pump = new BlockPos(2, 1, 3);
        BlockPos boiler = new BlockPos(4, 1, 3);
        helper.setBlock(pump, PFBlocks.OFFSHORE_PUMP.get());
        helper.setBlock(boiler, PFBlocks.BOILER.get());
        for (int i = 0; i < 2; i++) {
            PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(new BlockPos(6 + i, 1, 3)),
                    Direction.WEST);
        }
        extractingPipe(helper, pump.east(), Direction.WEST);
        extractingPipe(helper, boiler.east(), Direction.WEST);

        stockedChest(helper, new BlockPos(4, 1, 9), item("minecraft:coal"));
        unloader(helper, boiler.south(), Direction.SOUTH);
        belt(helper, new BlockPos(4, 1, 7), Direction.NORTH, 3);
        helper.setBlock(new BlockPos(4, 1, 8), loaderState(Direction.NORTH));

        helper.setBlock(new BlockPos(8, 1, 6), PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(new BlockPos(14, 1, 8), PFBlocks.pole(PoleTier.MEDIUM).get());

        BlockPos[] outs = {new BlockPos(12, 1, 6), new BlockPos(16, 1, 6)};
        for (BlockPos output : outs) {
            BlockPos furnace = output.south(2);
            helper.setBlock(furnace, PFBlocks.furnace(FurnaceTier.ELECTRIC).get());
            stockedChest(helper, furnace.south(2), item("minecraft:raw_iron"));
            feeder(helper, furnace.south(), Direction.NORTH);
            helper.setBlock(output, Blocks.CHEST);
            feeder(helper, furnace.north(), Direction.NORTH);
        }
        helper.succeedWhen(() -> {
            for (BlockPos output : outs) {
                if (holds(helper, output, item("ftbmaterials:iron_plate"))) {
                    return;
                }
            }
            throw helper.assertionException(outs[0], "no iron plate has reached an output chest");
        });
    }

    /** Crude from a Pumpjack is refined to gas, and gas with coal becomes plastic. */
    private static void oil(GameTestHelper helper) {
        BlockPos well = new BlockPos(3, 0, 4);
        helper.setBlock(well, PFBlocks.OIL_WELL.get());
        ((OilWellBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(well))).start(1_500_000);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.PUMPJACK.get()));
        BlockPos absoluteWell = helper.absolutePos(well);
        helper.useBlock(well, player, new BlockHitResult(
                Vec3.atCenterOf(absoluteWell).relative(Direction.UP, 0.5), Direction.UP, absoluteWell, false));

        BlockPos refineryAnchor = new BlockPos(8, 1, 3);
        ChassisFixture refinery = new ChassisFixture("Oil Refinery", PFBlocks.OIL_REFINERY_FOOTPRINT,
                refineryAnchor, Direction.NORTH);
        refinery.hold(helper, refinery.placeWhole(helper, OilRefineryBlockEntity.class),
                "factoryworks:oil_processing/basic_oil_processing");

        BlockPos plantAnchor = new BlockPos(11, 1, 3);
        ChassisFixture plant = new ChassisFixture("Chemical Plant", PFBlocks.CHEMICAL_PLANT_FOOTPRINT,
                plantAnchor, Direction.NORTH);
        plant.hold(helper, plant.placeWhole(helper, AssemblingMachineBlockEntity.class),
                "factoryworks:chemistry/plastic_bar");

        extractingPipe(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
        for (int x = 4; x <= 8; x++) {
            pipe(helper, new BlockPos(x, 1, 2));
        }

        extractingPipe(helper, refineryAnchor.above(4), Direction.DOWN);
        pipe(helper, new BlockPos(9, 5, 3));
        pipe(helper, new BlockPos(10, 5, 3));
        pipe(helper, new BlockPos(11, 5, 3));
        pipe(helper, new BlockPos(11, 4, 3));
        pipe(helper, new BlockPos(11, 3, 3));

        stockedChest(helper, new BlockPos(16, 1, 3), item("minecraft:coal"));
        unloader(helper, plantAnchor.east(), Direction.EAST);
        loadingBelt(helper, new BlockPos(15, 1, 3), Direction.WEST, 2);

        BlockPos out = new BlockPos(11, 1, 9);
        helper.setBlock(out, Blocks.CHEST);
        unloader(helper, out.north(), Direction.NORTH);
        belt(helper, plantAnchor.south(2), Direction.SOUTH, 3);
        helper.setBlock(plantAnchor.south(), loaderState(Direction.SOUTH));

        helper.setBlock(new BlockPos(6, 1, 7), PFBlocks.CREATIVE_POLE.get());
        succeedWhenChestHolds(helper, out, item("factoryworks:plastic_bar"));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static void stockedChest(GameTestHelper helper, BlockPos at, Item item) {
        helper.setBlock(at, Blocks.CHEST);
        Container chest = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(at));
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(item, 64));
        }
    }

    private static boolean holds(GameTestHelper helper, BlockPos at, Item item) {
        Container chest = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(at));
        return chest.hasAnyOf(java.util.Set.of(item));
    }

    private static void succeedWhenChestHolds(GameTestHelper helper, BlockPos at, Item item) {
        helper.succeedWhen(() -> {
            if (!holds(helper, at, item)) {
                throw helper.assertionException(at, "no " + BuiltInRegistries.ITEM.getKey(item) + " has reached the output chest");
            }
        });
    }

    /** {@code length} tiles flowing {@code flow} from {@code first}, laid downstream first. */
    private static void belt(GameTestHelper helper, BlockPos first, Direction flow, int length) {
        for (int i = length - 1; i >= 0; i--) {
            helper.setBlock(first.relative(flow, i), BlockContent.tileFor(BeltTier.BELT).defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, flow));
        }
    }

    /** A loader at {@code loader}, its inventory behind it, putting onto a belt flowing {@code flow}. */
    private static void loadingBelt(GameTestHelper helper, BlockPos loader, Direction flow, int length) {
        belt(helper, loader.relative(flow), flow, length);
        helper.setBlock(loader, loaderState(flow));
    }

    /** A loader facing {@code facing} at the belt that runs into it, emptying into the block behind. */
    private static void unloader(GameTestHelper helper, BlockPos at, Direction facing) {
        helper.setBlock(at, loaderState(facing));
    }

    private static BlockState loaderState(Direction facing) {
        return BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    /** Takes from the block behind it and puts into the block in front. */
    private static void feeder(GameTestHelper helper, BlockPos at, Direction facing) {
        helper.setBlock(at, BlockContent.FEEDER_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
    }

    private static void pipe(GameTestHelper helper, BlockPos at) {
        Block block = BuiltInRegistries.BLOCK.getValue(FLUID_PIPE);
        BlockPos absolute = helper.absolutePos(at);
        helper.getLevel().setBlockAndUpdate(absolute,
                ((AbstractPipeBlock) block).addConnectionStates(block.defaultBlockState(), helper.getLevel(), absolute, true));
    }

    /** A pipe that pulls from its {@code from} side, the side a player's click sets to extract. */
    private static void extractingPipe(GameTestHelper helper, BlockPos at, Direction from) {
        pipe(helper, at);
        BlockState state = helper.getBlockState(at);
        GenericPipeBlock pipe = (GenericPipeBlock) state.getBlock();
        helper.setBlock(at, state.setValue(pipe.directionToProperty(from), ExtractablePipeConnectionBlock.EXTRACT));
    }
}
