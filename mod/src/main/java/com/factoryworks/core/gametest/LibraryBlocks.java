package com.factoryworks.core.gametest;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Libraries' registered blocks, named by registry id as the Pack's own recipes and data name them
 * (#627), so a Library release that renames a class cannot break a Pack check. The pipe's arms are
 * its block state, which is what its model draws.
 */
final class LibraryBlocks {

    private LibraryBlocks() {
    }

    static Block block(String id) {
        Identifier key = Identifier.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            throw new IllegalStateException("no block is registered as " + id);
        }
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    static Block smallPole() {
        return block("wireworks:small_pole");
    }

    static Block mediumPole() {
        return block("wireworks:medium_pole");
    }

    static Block largePole() {
        return block("wireworks:large_pole");
    }

    static Block creativePole() {
        return block("wireworks:creative_pole");
    }

    static Block pipe() {
        return block("pipeworks:pipe");
    }

    static Block storageTank() {
        return block("pipeworks:storage_tank");
    }

    /** Whether {@code state} is a pipe drawing an arm on {@code side}. */
    static boolean pipeIsOpen(BlockState state, Direction side) {
        return state.is(pipe()) && state.getValue(arm(side));
    }

    /** {@code state} with the arm on {@code side} shut and every other arm as it was. */
    static BlockState pipeShut(BlockState state, Direction side) {
        return state.setValue(arm(side), false);
    }

    private static BooleanProperty arm(Direction side) {
        return switch (side) {
            case NORTH -> BlockStateProperties.NORTH;
            case EAST -> BlockStateProperties.EAST;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case UP -> BlockStateProperties.UP;
            case DOWN -> BlockStateProperties.DOWN;
        };
    }
}
