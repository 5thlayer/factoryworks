package com.factoryworks.core.dismantle;

import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.FactoryWorksCore;
import io.github._5thlayer.groundworks.DismantleFamily;
import io.github._5thlayer.groundworks.DismantleSpan;
import io.github._5thlayer.groundworks.Dismantles;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ShortestPath;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The pipes as a Dismantle Family, whose span is the shortest joined path between its ends (ADR-0086). */
public final class PipeFamily implements DismantleFamily {

    public static final TagKey<Block> PIPES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "dismantle/pipes"));

    private final JoinRule rule;

    private PipeFamily(JoinRule rule) {
        this.rule = rule;
    }

    public static void register() {
        Dismantles.register(new PipeFamily(new PipeworksPipeJoin()));
    }

    @Override
    public boolean claims(BlockState state) {
        return state.is(PIPES);
    }

    @Override
    public DismantleSpan span(Level level, BlockPos start, BlockPos end) {
        ShortestPath.Result<BlockPos> path = ShortestPath.between(start, end, new ShortestPath.Graph<>() {
            @Override
            public boolean member(BlockPos pos) {
                // A span stops at a chunk's edge rather than loading the next one (ADR-0086).
                return level.isLoaded(pos) && claims(level.getBlockState(pos));
            }

            @Override
            public List<BlockPos> neighbours(BlockPos pos) {
                List<BlockPos> around = new ArrayList<>(6);
                for (Direction side : Direction.values()) {
                    around.add(pos.relative(side));
                }
                return around;
            }

            @Override
            public boolean joined(BlockPos a, BlockPos b) {
                return rule.joined(level, a, b);
            }
        });
        return path.refusal() == null ? DismantleSpan.taking(path.path()) : DismantleSpan.refused(path.refusal());
    }

    @Override
    public Component message(Refusal refusal) {
        if (!(refusal instanceof ShortestPath.Refused refused)) {
            throw new IllegalArgumentException("the pipe family never refuses with " + refusal);
        }
        return Component.translatable(switch (refused) {
            case OUTSIDE_FAMILY -> "message.factoryworks.dismantle.outside_family";
            case NOT_JOINED -> "message.factoryworks.dismantle.not_joined";
            case TIED -> "message.factoryworks.dismantle.tied";
        });
    }
}
