---
status: accepted
supersedes: [327]
---

# The recipe viewer sets the Held recipe, and the machine's screen draws it

ADR-0071 says the Held recipe "is set through a pack-owned screen widget and through EMI's Fill
Recipe". #327 built the widget: a list of every assembling recipe on the machine's own screen. It
works, and it is a second recipe browser beside EMI's -- two places to find a recipe, two paths to
set one, and the one the pack's Fill Recipe integration was meant to make redundant. This ADR
decides which of the two stays and what the screen shows instead. It amends ADR-0071's setter
sentence only; the Held recipe, the pack's craft cycle and the stall rules stand.

**The reference is Modern Industrialization, not Factorio.** Factorio picks a recipe from a grid on
the machine, because Factorio has no recipe viewer to defer to. MI does, and read against
`~/minecraft_mods/modern-industrialization-src` at `e0b405a` its machine screen has **no recipe
list at all**. EMI's Fill Recipe on an MI machine does not move items: with the machine's screen on
top it sends the recipe id and the server **locks the machine's slots** to it
(`MachineRecipeHandler`, `CrafterComponent.lockRecipe`). The button is lit with an empty inventory,
because locking needs no items. A locked, empty slot draws the item it wants, so the recipe is
visible on the machine without a ghost feature. Every slot the recipe does not use is locked to
*nothing* (`lockAll`), which is what stops a pipe filling the machine with an item it cannot use.
Each slot carries a player-set capacity (`adjustedCapacity`). And a machine that cannot start says
why, and what to do, in a problems panel (`MachineProblemsDisplay`).

**Decision.** The recipe viewer is the only place a recipe is picked, and the machine's screen is
where the Held recipe is **shown**:

- **EMI's Fill Recipe sets the Held recipe** on the open machine, lit with an empty inventory. The
  server refuses a recipe the machine cannot hold, with a message, through the same path the
  widget used.
- **The in-screen recipe list is removed.** A **clear** button stays, so a recipe can be removed
  without a viewer.
- **The Held recipe is drawn in the slots**: each ingredient ghosted in its input slot with its
  count, the product in the output slot, a tag ingredient cycling through its members. A ghost
  disappears under a real stack.
- **Input slots the Held recipe does not use accept nothing**, MI's lock-empty rule, on the face
  #329 builds.
- **Each input slot has a player-set capacity**, capping what a pipe loads into it.
- **The screen states the stall**: one line from `AssemblingStall`, naming what is wrong and what
  fixes it.

**Considered: keep the list and add ghosts (Factorio's model).** Rejected. It is faithful to
Factorio's screen, but the pack ships EMI and JEI, and a second recipe browser inside one machine
duplicates the first while missing its search, its uses-and-recipes navigation and its bookmarks.
The ghosts and the stall line are kept; they are what the list was not.

**Considered: MI's storage model -- no Held recipe, slots locked to items, the recipe inferred from
the inputs.** Rejected, and this is the part worth recording, because MI's UI and MI's storage are
separable and only the storage is refused.

- *Counts are not the reason.* An MI lock covers amounts: `handleLocking` locks as many slots as the
  recipe's count needs, and each has an adjustable capacity. That objection was raised and was
  wrong.
- *Tags are the reason.* A lock names one `Item`, so a tag ingredient must pick one member -- the
  item the player carries, then AlmostUnified's target, then the first in the tag. A machine locked
  for a wooden chest with oak logs then refuses birch, where Factorio's wood is wood. That is 7 of
  the 42 emitted assembling recipes today, every one early game: four log tags and
  `#c:raw_materials/iron`. A Held recipe keeps the ingredient as a tag.
- *An empty machine knows its recipe.* With a recipe id, "locked by research" and the other stalls
  of #328 are exact before any item arrives. With inference, a machine with no inputs has no
  recipe, so a research lock is reported only once items are in it.
- *Components.* `lockedInstance` is a bare `Item`, so a lock cannot tell apart two stacks that
  differ by a data component, which is how Researchd's science packs differ (ADR-0052). No
  emitted assembling recipe has one today, so this is a later cost, not a present one.
- *No ambiguity to resolve.* MI needs output locks to choose between recipes that share inputs. A
  Held recipe is never ambiguous.

**Consequences.**

- **The machine needs a recipe viewer to be configured.** EMI and JEI both ship in the pack, so
  this is a pack dependency now stated rather than a gap. Fill Recipe is EMI's; JEI's transfer
  handler is not built, and a JEI-only player can clear a recipe but not set one until it is.
- **Fill Recipe must work before the list goes.** It is broken today, and the list is then the
  only setter; #330 lands first.
- **`AssemblingMachineRecipes.choices`** loses its screen reader. What it guarantees -- every
  emitted recipe is one the machine can hold -- is still asserted by #327's GameTest, which moves
  to the Fill Recipe path.
- **A Held recipe copies as one id**, so the configuration card the mechanic ledger names stays a
  small mechanic when it is built. Slot capacities travel with it.
- `docs/factorio-mechanics.md`'s "Recipe selection in a machine" row is rewritten against this
  surface.
