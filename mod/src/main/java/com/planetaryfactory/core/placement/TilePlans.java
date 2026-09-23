package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;
import rearth.belts.items.BeltTileItem;
import rearth.belts.items.StretchPlan;

/**
 * The SimpleBelts fork's tile item (#393). With a stored start, a click lays the fork's stretch and
 * a sneak-click adds a corner where it would end, so both are drawn as that stretch; without one a
 * click places a tile as vanilla would and a sneak-click stores a start, drawn as the tile there
 * facing the look. Loaded only when the fork is.
 */
final class TilePlans {

    private TilePlans() {
    }

    static boolean isTile(Item item) {
        return item instanceof BeltTileItem;
    }

    @Nullable
    static PlacementPlan plan(Item item, BlockPlaceContext context) {
        BeltTileItem tile = (BeltTileItem) item;
        StretchPlan stretch = tile.stretch(context);
        if (stretch == null) {
            boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
            if (!sneaking) {
                return Placements.vanillaPlan(tile, context);
            }
            BlockState start = tile.getBlock().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
            return new PlacementPlan(List.of(new PlacementPlan.Placed(BeltTileItem.aimedTile(context), start)), List.of(), null);
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
