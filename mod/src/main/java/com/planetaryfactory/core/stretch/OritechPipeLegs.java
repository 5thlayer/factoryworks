package com.planetaryfactory.core.stretch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.groundworks.Leg;
import io.github._5thlayer.groundworks.LegBuilder;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Stretches;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;

/** Oritech's fluid pipes laid by Stretch (#452): a rise climbs straight up in place, then the leg runs level. */
public final class OritechPipeLegs implements LegBuilder {

    private static final String BLOCKED_KEY = "message.planetaryfactory.stretch.pipe_blocked";

    enum Blocked implements Refusal {
        BLOCKED
    }

    private OritechPipeLegs() {
    }

    public static void register() {
        Stretches.register(new OritechPipeLegs());
    }

    @Override
    public boolean claims(Item item) {
        return item instanceof BlockItem block && block.getBlock() instanceof FluidPipeBlock;
    }

    @Override
    public PlacementPlan build(Level level, Item item, Leg leg) {
        FluidPipeBlock pipe = (FluidPipeBlock) ((BlockItem) item).getBlock();
        List<BlockPos> positions = positions(leg);
        Set<BlockPos> laid = Set.copyOf(positions);
        List<PlacementPlan.Placed> blocks = new ArrayList<>();
        Refusal refusal = null;
        for (BlockPos pos : positions) {
            blocks.add(new PlacementPlan.Placed(pos, opened(pipe, level, pos, laid)));
            if (refusal == null && (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced())) {
                refusal = new Refusal.At(Blocked.BLOCKED, pos);
            }
        }
        return new PlacementPlan(blocks, List.of(), refusal);
    }

    @Override
    public Component message(Refusal refusal) {
        return Component.translatable(BLOCKED_KEY);
    }

    private static List<BlockPos> positions(Leg leg) {
        List<BlockPos> positions = new ArrayList<>();
        int step = Integer.signum(leg.rise());
        for (int dy = 0; dy != leg.rise(); dy += step) {
            positions.add(leg.from().above(dy));
        }
        for (Leg.Column column : leg.route()) {
            positions.add(new BlockPos(column.x(), leg.from().getY() + leg.rise(), column.z()));
        }
        return positions;
    }

    // Beside a machine the pipe is planned plain: Oritech's placement swaps it for a connection pipe and
    // rejoins its neighbours, which a planned connection pipe would skip (#452).
    private static BlockState opened(FluidPipeBlock pipe, Level level, BlockPos pos, Set<BlockPos> laid) {
        BlockState state = pipe.addFluidState(pipe.defaultBlockState(), pos, level);
        for (Direction side : Direction.values()) {
            boolean open = laid.contains(pos.relative(side)) || pipe.shouldConnect(state, side, pos, level, true);
            state = state.setValue(pipe.directionToProperty(side), open ? GenericPipeBlock.CONNECTION : GenericPipeBlock.NO_CONNECTION);
        }
        return pipe.addStraightState(state);
    }
}
