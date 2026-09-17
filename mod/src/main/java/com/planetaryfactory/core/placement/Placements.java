package com.planetaryfactory.core.placement;

import com.planetaryfactory.core.PlanetaryFactoryCore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

/**
 * The one entry point to a {@link PlacementPlan} (#297, ADR-0069), and the vanilla plan every pack
 * block that places normally is served by.
 *
 * <h2>Every pack block gets a plan; no other mod's does</h2>
 *
 * <p>The gate is the <em>block</em>'s namespace, not the item's. It has to be: the mechanism is
 * generic and keyed on "this held item places a {@code planetaryfactory:} block", which is what
 * makes a new pack block previewable with no code at all. Other mods' placement refusals are
 * theirs, and owning them is unbounded.
 */
public final class Placements {

    private Placements() {
    }

    /**
     * What the held stack would do at this hit, or {@code null} if there is nothing to draw.
     *
     * <p>Safe on either side, and it reads the world without touching it: the client asks it every
     * frame (behind {@code PlacementPreview}'s cache) and the server asks it on the click.
     */
    @Nullable
    public static PlacementPlan planFor(Level level, @Nullable Player player, InteractionHand hand,
                                        ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem item) || !isPackBlock(item.getBlock())) {
            return null;
        }
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, stack, hit);
        return planFor(item, context);
    }

    /** The same, for a caller that already holds a context -- the item's own {@code place}. */
    @Nullable
    public static PlacementPlan planFor(BlockItem item, BlockPlaceContext context) {
        if (item instanceof PlansPlacement plans) {
            return plans.plan(context);
        }
        return vanillaPlan(item, context);
    }

    /** Whether this block is the pack's own, which is the whole of who gets a preview. */
    public static boolean isPackBlock(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(PlanetaryFactoryCore.NAMESPACE);
    }

    /**
     * The plan a plain {@link BlockItem} would carry out, asked of vanilla rather than restated.
     *
     * <p>This mirrors {@code BlockItem#place}'s decision chain exactly -- {@code canPlace}, then
     * {@code updatePlacementContext}, then {@code getStateForPlacement}, then survival and
     * obstruction -- because the whole value of deferring is that facing, replaceable blocks and
     * state survival come out right without the pack having an opinion. The two protected steps
     * ({@code BlockItem#canPlace}, {@code mustSurvive}) are reproduced here rather than reached
     * through an accessor mixin; no pack block overrides either, and a mixin for two lines would be
     * a second thing to keep in step.
     *
     * <p>Where vanilla would refuse before there is even a position -- an unplaceable context, a
     * null state -- there is no plan, so nothing is drawn. Where it refuses <em>at</em> a position,
     * that position draws red.
     */
    @Nullable
    public static PlacementPlan vanillaPlan(BlockItem item, BlockPlaceContext context) {
        if (!item.getBlock().isEnabled(context.getLevel().enabledFeatures()) || !context.canPlace()) {
            return null;
        }
        BlockPlaceContext updated = item.updatePlacementContext(context);
        if (updated == null) {
            return null;
        }
        BlockState state = item.getBlock().getStateForPlacement(updated);
        if (state == null) {
            return null;
        }
        BlockPos pos = updated.getClickedPos();
        if (!survives(state, updated, pos)) {
            return PlacementPlan.refused(pos, state, PlacementPlan.Refusal.VANILLA);
        }
        return PlacementPlan.accepted(pos, state);
    }

    /**
     * The block the ray actually hit, which is not always the block a plan is about.
     *
     * <p>{@code BlockPlaceContext} answers {@code getClickedPos} with where the block would
     * <em>go</em> -- the hit position itself when it is replaceable, the neighbour across the hit
     * face otherwise -- and that is the right answer for placing and the wrong one for a rule about
     * what is being aimed at, like the pole's column. {@code UseOnContext#getHitResult} is
     * protected, so this undoes the same step it took.
     */
    public static BlockPos aimedPos(BlockPlaceContext context) {
        BlockPos clicked = context.getClickedPos();
        return context.replacingClickedOnBlock()
                ? clicked
                : clicked.relative(context.getClickedFace().getOpposite());
    }

    /** Vanilla's own two conditions for "this state may stand here". */
    private static boolean survives(BlockState state, BlockPlaceContext context, BlockPos pos) {
        Level level = context.getLevel();
        return state.canSurvive(level, pos)
                && level.isUnobstructed(state, pos, CollisionContext.placementContext(context.getPlayer()));
    }
}
