package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import rearth.belts.items.BeltTileItem;
import rearth.belts.items.StretchPlan;

/**
 * The SimpleBelts fork's tile item (#393). A plain click with a stored start lays the fork's
 * stretch, which this is the plan of; without one it places a tile as vanilla would, and a
 * sneak-click stores a start and places nothing. Loaded only when the fork is.
 */
final class TilePlans {

    private TilePlans() {
    }

    static boolean isTile(Item item) {
        return item instanceof BeltTileItem;
    }

    @Nullable
    static PlacementPlan plan(Item item, BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return null;
        }
        BeltTileItem tile = (BeltTileItem) item;
        StretchPlan stretch = tile.stretch(context);
        if (stretch == null) {
            return Placements.vanillaPlan(tile, context);
        }
        if (stretch.tiles().isEmpty()) {
            return null;
        }
        List<PlacementPlan.Placed> blocks = stretch.tiles().stream()
                .map(placed -> new PlacementPlan.Placed(placed.pos(), placed.state()))
                .toList();
        List<BlockPos> replaces = stretch.tiles().stream()
                .filter(placed -> placed.action() != StretchPlan.Action.PLACE)
                .map(StretchPlan.Tile::pos)
                .toList();
        return new PlacementPlan(blocks, replaces, stretch.refused() ? refusal(stretch.refusal().reason()) : null);
    }

    private static PlacementPlan.Refusal refusal(StretchPlan.Reason reason) {
        return switch (reason) {
            case BEHIND_LOOK -> PlacementPlan.Refusal.BEHIND_LOOK;
            case BLOCKED -> PlacementPlan.Refusal.FOOTPRINT_BLOCKED;
            case NO_GROUND -> PlacementPlan.Refusal.NO_GROUND;
            case NOT_ENOUGH_TILES -> PlacementPlan.Refusal.NOT_ENOUGH_ITEMS;
            case NO_ROOM_TO_RETURN -> PlacementPlan.Refusal.NO_ROOM_TO_RETURN;
        };
    }
}
