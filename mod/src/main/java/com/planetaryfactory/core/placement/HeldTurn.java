package com.planetaryfactory.core.placement;

import com.planetaryfactory.core.PFDataComponents;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** The held stack's {@link QuarterTurn}, read and written on the stack itself (ADR-0083). */
public final class HeldTurn {

    private HeldTurn() {
    }

    public static QuarterTurn of(ItemStack stack) {
        return QuarterTurn.orNone(stack.get(PFDataComponents.QUARTER_TURN.get()));
    }

    public static boolean turns(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    /** Rotate, or Reverse Rotate, the stack; false and unchanged if it is not placeable. */
    public static boolean press(ItemStack stack, boolean reverse) {
        if (!turns(stack)) {
            return false;
        }
        QuarterTurn turned = reverse ? of(stack).reverseRotate() : of(stack).rotate();
        if (turned.equals(QuarterTurn.NONE)) {
            stack.remove(PFDataComponents.QUARTER_TURN.get());
        } else {
            stack.set(PFDataComponents.QUARTER_TURN.get(), turned);
        }
        return true;
    }

    /** The direction turned by the stack's offset; a vertical one is left alone. */
    public static Direction turn(ItemStack stack, Direction direction) {
        return turn(of(stack), direction);
    }

    public static Direction[] turn(ItemStack stack, Direction[] directions) {
        QuarterTurn turn = of(stack);
        if (turn.equals(QuarterTurn.NONE)) {
            return directions;
        }
        Direction[] turned = new Direction[directions.length];
        for (int i = 0; i < directions.length; i++) {
            turned[i] = turn(turn, directions[i]);
        }
        return turned;
    }

    private static Direction turn(QuarterTurn turn, Direction direction) {
        if (turn.equals(QuarterTurn.NONE) || direction.getAxis().isVertical()) {
            return direction;
        }
        QuarterTurn.Heading heading = QuarterTurn.Heading.fromDataValue(direction.get2DDataValue());
        return Direction.from2DDataValue(turn.turn(heading).ordinal());
    }
}
