package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.network.HoldRecipePacket;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * EMI's {@code + Fill Recipe}, pointed at an open Assembling Machine (#330, ADR-0073): it sets the
 * machine's Held recipe and moves no items.
 *
 * <p>{@link EmiRecipeHandler} directly, for {@link PersonalAssemblerEmiHandler}'s reason: a standard
 * handler moves ingredients and greys the button when they are missing, and holding a recipe needs
 * none -- MI's locking branch, where {@code canCraft} asks whether the recipe can be held, not
 * whether the player has the items. Unlike the Assembler's, a press carries no quantity, so
 * {@link FillClick} is not read.
 *
 * <p>Every assembling recipe gets the button, locked ones included: whether the machine may hold it
 * is server truth, and the server refuses with a message ({@code AssemblingMachineMenu.request})
 * rather than the button hiding the reason. {@code craft} returns true, so EMI hands the screen back
 * to the machine, where the Held recipe is shown.
 */
public final class AssemblingMachineEmiHandler implements EmiRecipeHandler<AssemblingMachineMenu> {

    /** Built here, not asked for: {@code EmiPlayerInventory.of} dispatches back to this method. */
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<AssemblingMachineMenu> screen) {
        Player player = Minecraft.getInstance().player;
        return player == null ? new EmiPlayerInventory(List.of()) : new EmiPlayerInventory(player);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == AssemblingEmiPlugin.ASSEMBLING && recipe.getId() != null;
    }

    /** Always: holding a recipe takes no items, so an empty inventory keeps the button lit. */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<AssemblingMachineMenu> context) {
        return true;
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe,
            EmiCraftContext<AssemblingMachineMenu> context) {
        return List.of(ClientTooltipComponent.create(
                Component.translatable("planetaryfactory_core.assembling_machine.fill").getVisualOrderText()));
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<AssemblingMachineMenu> context) {
        Identifier id = recipe.getId();
        if (id == null) return false;
        ClientPacketDistributor.sendToServer(new HoldRecipePacket(id));
        return true;
    }
}
