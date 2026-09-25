package com.planetaryfactory.core.placement;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.blocks.BeltWedgeBlock;
import io.github._5thlayer.beltworks.blocks.SplitterBlock;

/** The belts fork's blocks whose vanilla turn is wrong (#405). Loaded only when the fork is. */
final class BeltTurns {

    private static final String SLOPE = "message.planetaryfactory.rotate.slope";

    private BeltTurns() {
    }

    static List<PlacedTurn.Denial<BlockState>> denials() {
        return List.of(
                // One half turned alone splits the splitter; it turns whole with #407.
                new PlacedTurn.Denial<>(state -> state.getBlock() instanceof SplitterBlock,
                        "message.planetaryfactory.rotate.splitter"),
                new PlacedTurn.Denial<>(state -> state.getBlock() instanceof BeltTileBlock
                        && state.getValue(BeltTileBlock.PITCH) != BeltTileBlock.PitchState.LEVEL, SLOPE),
                // Only its slope's tile keeps a wedge, so a wedge turned alone is never put right.
                new PlacedTurn.Denial<>(state -> state.getBlock() instanceof BeltWedgeBlock, SLOPE));
    }

    /**
     * Why a tile turned to {@code turned} would be refused if it were placed so: a corner it would
     * slope (#419) or a wedge with no room (#420), since the turn reshapes the tiles around it as a
     * placement does.
     */
    static @Nullable String refitRefusal(Level level, BlockPos pos, BlockState turned) {
        if (!(turned.getBlock() instanceof BeltTileBlock tile)) {
            return null;
        }
        BeltTileBlock.Reshape reshape = BeltTileBlock.reshape(level, Map.of(BeltTileBlock.spot(pos),
                BeltTileBlock.travel(turned.getValue(HorizontalDirectionalBlock.FACING))), tile);
        return reshape.refused() ? reshape.refusal().messageKey() : null;
    }
}
