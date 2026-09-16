package com.planetaryfactory.core.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * Registers the Assembler's fill handler against the player's inventory screen (#290).
 *
 * <p>A plugin and not a mixin, unlike this package's other resident: {@code addRecipeHandler} is a
 * supported seam that runs unconditionally, where the decorator API behind the lock badge is gated
 * on a config flag that is off for players.
 *
 * <p>{@code EmiRecipeFiller.handlers} is keyed by {@code MenuType}, so Fill Recipe reaches the
 * Assembler from the inventory screen and nowhere else. EMI's own 2x2 handler sits under the same key,
 * but it claims only vanilla's crafting category, and the hand set is the pack's assembling one.
 */
@EmiEntrypoint
public final class PersonalAssemblerEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // EMI keys the player's own inventory under a null menu type: InventoryMenu has none.
        registry.addRecipeHandler(null, new PersonalAssemblerEmiHandler());
    }
}
