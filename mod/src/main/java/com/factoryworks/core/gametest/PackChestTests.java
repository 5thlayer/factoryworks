package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.chest.ChestTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The Iron and Steel Chests hold exactly their slots, through the face and through a reload (#540). */
final class PackChestTests {

    private static final BlockPos AT = new BlockPos(2, 1, 3);

    private PackChestTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        for (ChestTier tier : ChestTier.values()) {
            tests.test(tier.blockName() + "_face_holds_exactly_its_slots", 20, helper -> faceHoldsSlots(helper, tier));
            tests.test(tier.blockName() + "_full_survives_its_save_hook", 20, helper -> fullSurvivesSave(helper, tier));
        }
    }

    private static void faceHoldsSlots(GameTestHelper helper, ChestTier tier) {
        ResourceHandler<ItemResource> face = placeChest(helper, tier);
        List<ItemResource> items = distinctItems(tier.slots() + 1);

        int landed = 0;
        for (ItemResource item : items) {
            try (Transaction tx = Transaction.openRoot()) {
                landed += face.insert(item, 1, tx);
                tx.commit();
            }
        }
        helper.assertValueEqual(face.size(), tier.slots(), "slots on the face");
        helper.assertValueEqual(landed, tier.slots(), "stacks that landed of " + items.size());

        int taken = 0;
        for (ItemResource item : items) {
            try (Transaction tx = Transaction.openRoot()) {
                taken += face.extract(item, 1, tx);
                tx.commit();
            }
        }
        helper.assertValueEqual(taken, tier.slots(), "stacks handed back");
        helper.succeed();
    }

    private static void fullSurvivesSave(GameTestHelper helper, ChestTier tier) {
        ResourceHandler<ItemResource> face = placeChest(helper, tier);
        List<ItemResource> items = distinctItems(tier.slots());
        for (ItemResource item : items) {
            try (Transaction tx = Transaction.openRoot()) {
                face.insert(item, 1, tx);
                tx.commit();
            }
        }

        BlockPos absolute = helper.absolutePos(AT);
        BlockState state = helper.getLevel().getBlockState(absolute);
        BlockEntity original = helper.getLevel().getBlockEntity(absolute);
        var registries = helper.getLevel().registryAccess();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        original.saveWithFullMetadata(out);
        BlockEntity reloaded = BlockEntity.loadStatic(absolute, state, out.buildResult(), registries);

        helper.assertTrue(reloaded != null, "the saved chest loads again");
        for (int slot = 0; slot < tier.slots(); slot++) {
            ItemStack want = items.get(slot).toStack(1);
            ItemStack got = ((net.minecraft.world.Container) reloaded).getItem(slot);
            if (!ItemStack.matches(want, got)) {
                helper.fail("slot " + slot + " reloaded as " + got + ", not " + want, AT);
                return;
            }
        }
        helper.succeed();
    }

    private static ResourceHandler<ItemResource> placeChest(GameTestHelper helper, ChestTier tier) {
        helper.setBlock(AT, PFBlocks.chest(tier).get().defaultBlockState());
        ResourceHandler<ItemResource> face = helper.getLevel()
                .getCapability(Capabilities.Item.BLOCK, helper.absolutePos(AT), null);
        helper.assertTrue(face != null, tier.blockName() + " has an item face");
        return face;
    }

    private static List<ItemResource> distinctItems(int count) {
        List<ItemResource> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                items.add(ItemResource.of(new ItemStack(item)));
            }
            if (items.size() == count) {
                break;
            }
        }
        return items;
    }
}
