---
status: accepted
supersedes: [287]
---

# Fill Recipe on the Assembler queues without opening the Crafting Plan

ADR-0064 made **Fill Recipe** open the Crafting Plan with the cursor on **+1**, so one craft still
took two clicks. Factorio crafts straight from the recipe, with a click and a modifier (#288).

**Decision.** On the Personal Assembler's screen, EMI's **Fill Recipe** on a recipe the Assembler can
make queues directly:

- **Left-click** queues 1. **Right-click** queues 5.
- **Shift + click** queues as many as the inventory covers, whichever button is used.
- **Middle click** opens the Crafting Plan and queues nothing.

The client sends only the recipe and the request. The server resolves the plan the way the dialog's
buttons do, and queues it only if the inventory covers the count. **all** is computed on the server.

**A craft that cannot start opens the Crafting Plan.** That covers a count the inventory can't cover,
a Missing leaf and a Locked recipe, and the plan names the reason. A request for 5 never becomes 3;
only Shift means "as many as possible". A queue that succeeds gives no feedback beyond the button
sound and the queue updating, and **EMI's recipe screen stays open**, so the next craft is one more
click. EMI closes that screen whenever the handler reports a craft, so a queueing click reports none
and plays the sound itself.

**Scope.** Nothing about EMI's Fill Recipe changes on any other screen. EMI calls the pack's handler
only for `AssemblerPanelMenu` and only for recipes in the hand set. Everywhere else EMI's own
behaviour stands, for every button.

**Reading the button.** EMI's handler API carries no mouse button: `EmiCraftContext` has a type, a
destination and an amount, and it encodes Shift only as an amount of `Integer.MAX_VALUE`.
`RecipeScreen` passes every button to the widget, and `RecipeFillButtonWidget.mouseClicked` ignores
the button, so the pack records the button there. The recording never alters, consumes or reroutes
the click. The button is taken as Minecraft delivers it to the screen, not from the physical mouse,
so each OS's conversions apply with no platform code in the pack. On macOS, `MouseHandler` turns
Ctrl + left-click into a right-click.

**Rejected.**

- **Polling GLFW inside `craft()`.** It reads the physical button, which is wrong wherever Minecraft
  converts it.
- **A Ctrl modifier for 5.** Minecraft reads Ctrl as Cmd on macOS, and Ctrl + left-click is already a
  right-click there. Two paths to one count would need a branch per platform.

**EMI's craft hotkeys** reach the same handler with no click behind them. They read as a left-click,
or as Shift when EMI reports "all": craft-one queues 1, craft-all queues all.

**What survives from ADR-0064.** The dialog is unchanged: **+1**, **+5**, **all** and **Close**,
queueing on each press. It is no longer the way in; it opens on demand or when a craft can't start.
**The cursor warp onto +1 goes**, with the access transformer that existed only for it. The dialog now
opens mostly after a refusal, and a warp there would put a lit **+1** under a player who just asked
for 5, so a repeated click would quietly queue 1.

**Consequences.**

- **all is capped** at `PlanResolver.MAX_CRAFTS`, as every queue already is.
- **Tooltip.** The Fill Recipe tooltip lists the four actions, on the Assembler's screen only.
- **Checks.** The rule that turns a request and an affordability result into "queue N" or "open the
  plan" is a Minecraft-free unit test. Whether each button reaches the handler, and that Fill Recipe
  is unchanged elsewhere, is a world load.
