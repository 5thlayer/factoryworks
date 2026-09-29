package com.factoryworks.core.placement;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * A block with a facing, an axis or a sixteen-way rotation. Vanilla cannot say whether a block's
 * placement reads the look, so a block with one of those is taken to (ADR-0087, #450).
 */
public final class Oriented {

    private Oriented() {
    }

    public static boolean is(Block block) {
        return block.defaultBlockState().getProperties().stream().anyMatch(Oriented::orients);
    }

    private static boolean orients(Property<?> property) {
        return property.getValueClass() == Direction.class || property.getValueClass() == Direction.Axis.class
                || property == BlockStateProperties.ROTATION_16;
    }
}
