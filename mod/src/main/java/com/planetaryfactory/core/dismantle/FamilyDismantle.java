package com.planetaryfactory.core.dismantle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.planetaryfactory.core.PFDataComponents;
import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * Taking up a span of one Dismantle Family in two sneak-clicks of an item in {@link #DISMANTLES}
 * (#431, ADR-0086). A family is a block tag under {@code planetaryfactory:dismantle/}; the first
 * click on a member stores the start on the held stack, the second takes up the {@link DismantleSpan}
 * to the aimed block. A start whose block has left every family is no start.
 */
public final class FamilyDismantle {

    public static final TagKey<Item> DISMANTLES = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "dismantles"));

    private static final String FAMILY_PREFIX = "dismantle/";

    private static final Map<Identifier, JoinRule> JOIN_RULES = new HashMap<>();

    private static final boolean BELTS = ModList.get().isLoaded("beltworks");

    static {
        if (ModList.get().isLoaded("oritech")) {
            JOIN_RULES.put(Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "dismantle/pipes"),
                    new OritechPipeJoin());
        }
    }

    private FamilyDismantle() {
    }

    public static boolean dismantles(ItemStack stack) {
        return stack.is(DISMANTLES);
    }

    /** The family {@code state} belongs to, or none. */
    public static Optional<TagKey<Block>> familyOf(BlockState state) {
        return state.typeHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals(PlanetaryFactoryCore.NAMESPACE)
                        && tag.location().getPath().startsWith(FAMILY_PREFIX))
                .findFirst();
    }

    /** The stored start, or null when none is stored, it is in another dimension, or its block has left every family. */
    public static @Nullable BlockPos liveStart(Level level, ItemStack held) {
        GlobalPos start = held.get(PFDataComponents.DISMANTLE_START.get());
        if (start == null || !start.dimension().equals(level.dimension())) {
            return null;
        }
        return familyOf(level.getBlockState(start.pos())).isPresent() ? start.pos() : null;
    }

    /** What a sneak-click at {@code aimed} would take up, or null when {@code held} has no live start. */
    public static @Nullable DismantlePlan plan(Level level, ItemStack held, BlockPos aimed) {
        if (!dismantles(held)) {
            return null;
        }
        BlockPos start = liveStart(level, held);
        if (start == null) {
            return null;
        }
        TagKey<Block> family = familyOf(level.getBlockState(start)).orElseThrow();
        DismantleSpan.Result<BlockPos> span = DismantleSpan.between(start, aimed.immutable(),
                new LevelFamily(level, family, JOIN_RULES.getOrDefault(family.location(), JoinRule.TOUCHING)));
        return new DismantlePlan(span.path(), span.refusal());
    }

    /** Whether a sneak-click at {@code pos} is this gesture's rather than Groundworks' belt family's. */
    public static boolean claims(Level level, BlockPos pos, ItemStack held) {
        return !(BELTS && BeltClaim.claims(level, pos, held));
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!player.isShiftKeyDown() || !dismantles(held)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!claims(level, pos, held)) {
            return;
        }
        DismantlePlan plan = plan(level, held, pos);
        if (plan == null) {
            if (familyOf(level.getBlockState(pos)).isEmpty()) {
                return;
            }
            if (!level.isClientSide()) {
                held.set(PFDataComponents.DISMANTLE_START.get(), GlobalPos.of(level.dimension(), pos.immutable()));
                tell(player, Component.translatable("message.planetaryfactory.dismantle.started"));
            }
        } else if (!level.isClientSide()) {
            if (plan.refused()) {
                tell(player, plan.message());
            } else {
                execute(plan, (ServerLevel) level, held, player);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** A sneak-use in the air clears the stored start. */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!player.isShiftKeyDown() || !dismantles(held) || !held.has(PFDataComponents.DISMANTLE_START.get())) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            held.remove(PFDataComponents.DISMANTLE_START.get());
            tell(player, Component.translatable("message.planetaryfactory.dismantle.cleared"));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void execute(DismantlePlan plan, ServerLevel level, ItemStack held, Player player) {
        held.remove(PFDataComponents.DISMANTLE_START.get());
        List<ItemStack> handed = new ArrayList<>();
        for (BlockPos pos : plan.blocks()) {
            BlockState state = level.getBlockState(pos);
            if (!player.hasInfiniteMaterials()) {
                handed.addAll(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, held));
            }
            level.destroyBlock(pos, false, player);
        }
        for (ItemStack stack : handed) {
            if (!stack.isEmpty() && !player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }

    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
