package com.planetaryfactory.core.placement;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/** Rotate on a placed block (#405, ADR-0087): {@link PlacedTurn}'s rule over the world. */
public final class PlacedTurns {

    private static final boolean BELTS = ModList.get().isLoaded("beltworks");
    private static final List<PlacedTurn.Denial<BlockState>> DENIALS = BELTS ? BeltTurns.denials() : List.of();

    private PlacedTurns() {
    }

    /** Turns the block at {@code pos} a quarter, or says why it is not turned. */
    public static PlacedTurn.Verdict<BlockState> turn(Level level, BlockPos pos, boolean reverse) {
        BlockState state = level.getBlockState(pos);
        Rotation rotation = reverse ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        PlacedTurn.Contract<BlockState> own = state.getBlock() instanceof TurnsInPlace block
                ? current -> block.turnInPlace(current, level, pos, reverse)
                : null;
        PlacedTurn.Verdict<BlockState> verdict = PlacedTurn.decide(state, DENIALS, own,
                current -> fitted(current.rotate(level, pos, rotation), current, level, pos),
                turned -> BELTS ? BeltTurns.refitRefusal(level, pos, turned) : null);
        if (verdict instanceof PlacedTurn.Turned<BlockState>(BlockState turned)) {
            level.setBlock(pos, turned, Block.UPDATE_ALL);
        }
        return verdict;
    }

    // Vanilla's rotate leaves the neighbour update for later: a door or bed half turned alone
    // re-derives against its other half here, and a block that would no longer stand is left as it
    // was (ADR-0087).
    private static BlockState fitted(BlockState turned, BlockState original, Level level, BlockPos pos) {
        BlockState shaped = Block.updateFromNeighbourShapes(turned, level, pos);
        return shaped.isAir() || !shaped.canSurvive(level, pos) ? original : shaped;
    }
}
