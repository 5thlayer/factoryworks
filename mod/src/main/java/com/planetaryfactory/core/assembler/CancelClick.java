package com.planetaryfactory.core.assembler;

/**
 * How many crafts a click on a queue icon cancels (#290): left one, right five, Shift all, as
 * Factorio's hand-craft queue does. Any other button cancels nothing, so a stray middle click on the
 * inventory screen never throws a craft away.
 *
 * <p>A count beyond what the row has left cancels what is there; {@link AssemblerQueue} caps it.
 */
public final class CancelClick {

    private CancelClick() {
    }

    public static int crafts(int button, boolean shift) {
        if (button != 0 && button != 1) return 0;
        if (shift) return AssemblerQueue.ALL;
        return button == 0 ? 1 : 5;
    }
}
