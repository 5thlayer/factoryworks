package com.planetaryfactory.core.compat.emi;

import com.planetaryfactory.core.assembler.AssemblerPanelMenu;
import com.planetaryfactory.core.assembler.FillRequest;
import com.planetaryfactory.core.assembler.HandRecipeSet;
import com.planetaryfactory.core.network.FillRecipePacket;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * EMI's {@code + Fill Recipe}, pointed at the Personal Assembler (ADR-0038, #160).
 *
 * <p><b>{@link EmiRecipeHandler} directly, never {@code StandardRecipeHandler}.</b> That is the whole
 * point of this class. A standard handler moves ingredients into a crafting grid, and its default
 * {@code canCraft} checks the player's inventory against the recipe -- so EMI greys the button out
 * when an ingredient is missing. We have no grid and move nothing, and a missing ingredient is
 * exactly the case the Crafting Plan exists to name. Greying the button there would hide the answer
 * behind the question.
 *
 * <p>Verified against {@code emi-1.1.24+1.21.1+neoforge.jar} rather than assumed:
 * {@code RecipeFillButtonWidget} sets {@code canFill = supportsRecipe(recipe) && canCraft(recipe,
 * ctx)} and that single field drives both the greyed texture and the click gate. {@code canCraft} is
 * ours, so returning true unconditionally keeps the button lit with the ingredients absent.
 *
 * <p>{@code craft()} sends and returns. It opens no screen, because {@code
 * EmiRecipeFiller.performFill} calls {@code Minecraft.setScreen(handledScreen)} the moment it
 * returns true -- anything opened synchronously here loses that race. The server queues, or
 * opens the Crafting Plan when the request cannot be queued (ADR-0065).
 *
 * <p>That same {@code setScreen} is why a queueing click returns <b>false</b>: true would close EMI's
 * recipe screen after every craft, and the player would reopen it to queue the next one. False
 * skips the {@code setScreen} and EMI's button sound with it, so the sound is played here.
 */
public final class PersonalAssemblerEmiHandler implements EmiRecipeHandler<AssemblerPanelMenu> {

    /**
     * The player's stacks, built here rather than asked for.
     *
     * <p>{@code EmiPlayerInventory.of(player)} would be the obvious call and is a stack overflow:
     * read out of the jar, {@code of} is EMI's <em>dispatcher</em> -- it looks up the handlers
     * registered for the currently open screen and returns {@code handlers.get(0).getInventory(...)},
     * which is this method. It only reaches {@code new EmiPlayerInventory(player)} when no handler is
     * registered at all, so it works everywhere except inside a handler.
     *
     * <p>It crashes the moment the panel opens, not on a button press: EMI builds the inventory to
     * work out what is craftable as soon as a screen with a handler comes up.
     */
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<AssemblerPanelMenu> screen) {
        Player player = Minecraft.getInstance().player;
        return player == null ? new EmiPlayerInventory(List.of()) : new EmiPlayerInventory(player);
    }

    /**
     * A recipe the Assembler can actually plan, and no other.
     *
     * <p>The hand-craftable set -- first category {@code crafting}, minus the eleven Factorio
     * withholds (#88) -- is a fact about the loaded recipes and therefore server truth, so it is
     * synced as ids and read here from {@link HandRecipeSet}. Smelting raw copper is a furnace's
     * job, and offering to hand-craft it could only ever end in a dialog that says no.
     *
     * <p>This used to be every recipe EMI could name, on the argument that a refusal one screen
     * later says more than a missing button. That holds for a recipe the Assembler <em>does</em>
     * make and the player cannot afford -- which is the Crafting Plan's whole job, and why
     * {@link #canCraft} still returns true unconditionally. It does not hold for a recipe no
     * Assembler will ever make: there the dialog has nothing to teach, and the button is a dead end
     * the player has to open to discover.
     */
    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        Identifier id = recipe.getId();
        return id != null && HandRecipeSet.contains(id.toString());
    }

    /**
     * Always, and this is the non-standard half of the contract.
     *
     * <p>The button must stay enabled when the player lacks the ingredients, because showing what is
     * missing is the Crafting Plan's entire job. Which recipes get a button at all is
     * {@link #supportsRecipe}'s question, and a different one.
     */
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<AssemblerPanelMenu> context) {
        return true;
    }

    /**
     * Names the four clicks under EMI's own "Fill Recipe" line, or the shortcut cannot be discovered.
     *
     * <p>EMI asks this only of the handler for the open screen, so the lines appear on the Assembler's
     * screen and nowhere else.
     */
    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<AssemblerPanelMenu> context) {
        return List.of("left", "right", "shift", "middle").stream()
                .map(key -> ClientTooltipComponent.create(
                        Component.translatable("planetaryfactory_core.assembler.fill." + key).getVisualOrderText()))
                .toList();
    }

    /**
     * Sends the recipe and what the click asked for, and gets out of EMI's way (#288, ADR-0065).
     *
     * <p>Left queues one, right five, Shift all, middle opens the Crafting Plan. The button comes from
     * {@link FillClick}; EMI reports Shift itself, as an amount of {@code Integer.MAX_VALUE}. Whether the
     * inventory covers the request is the server's call, and it opens the plan when it does not.
     *
     * <p>Only a request for the plan returns true and hands the screen back to the panel; a queueing
     * click returns false so EMI's recipe screen stays open for the next one (see the class doc).
     */
    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<AssemblerPanelMenu> context) {
        Identifier id = recipe.getId();
        if (id == null) return false;
        FillRequest request = FillRequest.of(FillClick.button(), context.getAmount() == Integer.MAX_VALUE);
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new FillRecipePacket(id, request));
        if (request == FillRequest.PLAN) return true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return false;
    }
}
