package com.factoryworks.core.stretch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.groundworks.Leg;
import io.github._5thlayer.groundworks.LegBuilder;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.Stretches;
import io.github._5thlayer.pipeworks.FluidSegments;
import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.block.FluidPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Pipeworks' pipes laid by Stretch (#452): a rise climbs straight up in place, then the leg runs level. */
public final class PipeworksPipeLegs implements LegBuilder {

    private static final String BLOCKED_KEY = "message.factoryworks.stretch.pipe_blocked";

    enum Blocked implements Refusal {
        BLOCKED
    }

    private PipeworksPipeLegs() {
    }

    public static void register() {
        Stretches.register(new PipeworksPipeLegs());
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

    // The arms the pipe will draw once Pipeworks joins it, so the plan equals what the click lays (ADR-0110).
    private static BlockState opened(FluidPipeBlock pipe, Level level, BlockPos pos, Set<BlockPos> laid) {
        return pipe.withLinks(pipe.defaultBlockState(), side -> {
            BlockPos beside = pos.relative(side);
            return laid.contains(beside) || opensTowards(level, beside, side.getOpposite());
        });
    }

    private static boolean opensTowards(Level level, BlockPos pos, Direction face) {
        return level.getBlockState(pos).getBlock() instanceof FluidSegments.SegmentBlock
                || level.getBlockEntity(pos) instanceof FluidPort port && port.connectsOn(face);
    }
}
