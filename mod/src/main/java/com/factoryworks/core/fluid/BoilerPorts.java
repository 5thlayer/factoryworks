package com.factoryworks.core.fluid;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import rearth.oritech.util.Geometry;

/** Which faces a Boiler port opens and what it adds to its segment (ADR-0114). */
final class BoilerPorts {

    private static final SteamChainCorpus CORPUS = SteamChainCorpus.get();

    private BoilerPorts() {
    }

    static long capacity(BoilerFootprint.Port port) {
        return switch (port) {
            case WATER -> CORPUS.boilerFluidBoxVolume(BoilerSpec.INPUT);
            case STEAM -> CORPUS.boilerFluidBoxVolume(BoilerSpec.OUTPUT);
        };
    }

    /** Water opens along the front row, steam backwards; local {@code x} runs backward from the front. */
    static boolean opens(BoilerFootprint.Port port, Direction facing, Direction face) {
        return switch (port) {
            case WATER -> face == world(facing, 0, -1) || face == world(facing, 0, 1);
            case STEAM -> face == world(facing, 1, 0);
        };
    }

    private static Direction world(Direction facing, int x, int z) {
        Vec3i offset = Geometry.rotatePosition(new Vec3i(x, 0, z), facing);
        return Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ());
    }
}
