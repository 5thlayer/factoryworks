package com.factoryworks.core.machine.footprint;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.block.MultiblockMachine;

/**
 * The anchor of a {@link FootprintMachine}: the block holding the Oritech block entity.
 *
 * <p>Extends Oritech's {@link MultiblockMachine} so the {@code ASSEMBLED} property is declared:
 * Oritech's block and controller code reads it unguarded, and a state without it would throw. The
 * anchor is placed with it already set (ADR-0071).
 */
public abstract class FootprintAnchorBlock extends MultiblockMachine implements FootprintTurn {

    private final Supplier<FootprintMachine> machine;

    protected FootprintAnchorBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties);
        this.machine = machine;
    }

    public FootprintMachine machine() {
        return machine.get();
    }

    /** Replaces Oritech's machine tooltip, which describes Oritech's machine rather than this one (#515). */
    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag,
                             DataComponentGetter components) {
        for (MachineTooltip.Line line : tooltipLines()) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }

    protected abstract List<MachineTooltip.Line> tooltipLines();

    /**
     * The anchor going takes its parts with it. Its own item comes from its loot table, and its
     * inventory from Oritech's {@code playerWillDestroy} -- or from the teardown when a part was
     * what the player broke.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        machine().teardown(level, pos, state.getValue(FACING), pos);
    }
}
