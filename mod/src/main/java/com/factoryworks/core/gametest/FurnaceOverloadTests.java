package com.factoryworks.core.gametest;

import static com.factoryworks.core.gametest.Faces.expectMoved;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceMenu;
import com.factoryworks.core.smelting.FurnaceSlots;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Automated insertion stops at the Overload Limit of the smelt; the hand does not (#518). */
final class FurnaceOverloadTests {

    private static final BlockPos FURNACE = new BlockPos(3, 1, 3);

    // Typed from Factorio: two smelts' worth on every tier, for iron and for the 5:1 steel smelt.
    private static final int IRON_LIMIT = 2;
    private static final int STEEL_LIMIT = 10;

    private static final Identifier IRON_PLATE_ID = Identifier.parse("ftbmaterials:iron_plate");

    private FurnaceOverloadTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        for (FurnaceTier tier : FurnaceTier.values()) {
            String name = tier.blockName();
            tests.test(name + "_input_stops_at_the_overload_limit", 20, helper -> inputStops(helper, tier));
            tests.test(name + "_screen_places_a_full_stack", 20, helper -> screenPlacesAFullStack(helper, tier));
            if (tier.burnsFuel()) {
                tests.test(name + "_fuel_is_not_capped", 20, helper -> fuelIsNotCapped(helper, tier));
            }
        }
    }

    private static void inputStops(GameTestHelper helper, FurnaceTier tier) {
        ItemResource rawIron = ItemResource.of(Items.RAW_IRON);
        ItemResource ironPlate = ItemResource.of(BuiltInRegistries.ITEM.getValue(IRON_PLATE_ID));
        ResourceHandler<ItemResource> face = place(helper, tier);
        expectMoved(helper, FURNACE, "raw iron into an empty input", IRON_LIMIT, face, (f, tx) -> f.insert(rawIron, 64, tx));
        expectMoved(helper, FURNACE, "raw iron at the limit", 0, face, (f, tx) -> f.insert(0, rawIron, 64, tx));

        furnace(helper).setItem(FurnaceSlots.INPUT, ItemStack.EMPTY);
        expectMoved(helper, FURNACE, "iron plates into an empty input", STEEL_LIMIT, face,
                (f, tx) -> f.insert(ironPlate, 64, tx));
        furnace(helper).setItem(FurnaceSlots.INPUT, ironPlate.toStack(STEEL_LIMIT - 1));
        expectMoved(helper, FURNACE, "iron plates one short of the limit", 1, face,
                (f, tx) -> f.insert(0, ironPlate, 64, tx));
        helper.succeed();
    }

    private static void fuelIsNotCapped(GameTestHelper helper, FurnaceTier tier) {
        ResourceHandler<ItemResource> face = place(helper, tier);
        expectMoved(helper, FURNACE, "coal into the fuel slot", 64, face,
                (f, tx) -> f.insert(ItemResource.of(Items.COAL), 64, tx));
        helper.succeed();
    }

    private static void screenPlacesAFullStack(GameTestHelper helper, FurnaceTier tier) {
        place(helper, tier);
        FurnaceBlockEntity furnace = furnace(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 64));
        FurnaceMenu menu = new FurnaceMenu(0, player.getInventory(), furnace, furnace.data());
        menu.quickMoveStack(player, hotbarFirst(menu, player));
        if (furnace.getItem(FurnaceSlots.INPUT).getCount() != 64) {
            helper.fail("a shift-click placed " + furnace.getItem(FurnaceSlots.INPUT) + ", expected 64 raw iron", FURNACE);
            return;
        }
        helper.succeed();
    }

    private static int hotbarFirst(FurnaceMenu menu, Player player) {
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() == 0) {
                return slot.index;
            }
        }
        throw new IllegalStateException("the furnace menu has no hotbar slot 0");
    }

    private static ResourceHandler<ItemResource> place(GameTestHelper helper, FurnaceTier tier) {
        helper.setBlock(FURNACE, PFBlocks.furnace(tier).get());
        return Faces.item(helper, FURNACE);
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper) {
        return helper.getBlockEntity(FURNACE, FurnaceBlockEntity.class);
    }
}
