# What the 26.1.2 port took out, and who owns putting it back

ADR-0060 moved the pack to Minecraft 26.1.2 and took GregTech out. #268 is the ticket that made
`planetaryfactory_core` compile again, and it carries an explicit escape hatch: a call site whose
dependency is gone is either ported to the replacement, or **removed behind a note naming the ticket
that owns it**. This file is that note, written once here rather than as a TODO comment in a file
that no longer exists.

Nothing here is a decision to drop a feature. Every row is code whose dependency does not exist on
26.1.2 *yet*, and the git history holds the 1.21.1 implementation: `git log --diff-filter=D` finds
the removing commit, and the file one commit before it is the thing to port.

## Removed because GregTech is gone (ADR-0060)

GTCEu is not installed and no `com.gregtechceu` reference may survive under `mod/src`. Oritech is
the replacement chassis, and its API is a different shape — `rearth.oritech`, not a renamed
`com.gregtechceu.gtceu`. Renaming an import cannot port these; each is a re-implementation.

| removed | what it was | owner |
| --- | --- | --- |
| `core/machine/SimpleMachine.java` | The chassis under every single-block machine the pack registers — `SimpleTieredMachine` minus the programmed-circuit configurator and the charger slot. Both removals are GregTech-specific and neither idiom exists in Oritech. | #262 |
| `core/research/client/IdleMachineLockNote.java` | Why an idle machine is idle, searched out of GregTech's recipe trie with `RecipeHelper.matchContents`. Blocked twice over — it also needs Researchd. #251 already owns a Jade provider saying why a machine is doing nothing. | #251 |
| `core/mixin/gtceu/RecipeLogicMixin.java` | The research lock's refusal, injected into `RecipeLogic.matchRecipe`. | #262 |
| `core/mixin/gtceu/RecipeLogicStatusMixin.java` | The idle note's three-times-a-frame hook into the machine screen. | #251 |

Both mixins named GregTech classes by target string, so their entries left
`planetaryfactory_core.mixins.json` with them: a mixin naming a class that is not on the classpath
fails the *load*, not the build, which is the one failure mode a green compile would have hidden.

## Removed because Researchd is not ported yet (#260)

When these went, the pack's Researchd fork was still on 1.21.1, so every file importing
`com.portingdeadmods.researchd` had to go. The fork is now on 26.1 (`~/minecraft_mods/researchd`)
and pinned in `data/pack/local-jars.json`, so nothing blocks them any more; #260 owns restoring
them, and none needs redesigning — they are ports, not rewrites.

| removed | what it was |
| --- | --- |
| `core/research/ResearchLocks.java` | The index of which research unlocks which recipe, read out of Researchd's `ResearchManager` and cached against its identity. |
| `core/research/client/LockedByResearchLines.java` | The tooltip lines naming the researches that would unlock a recipe. |
| `core/research/client/LockedRecipeNote.java` | The recipe-viewer annotation itself (#75). |
| `core/compat/emi/LockedRecipeEmiNote.java` | EMI's half of that annotation. |
| `core/compat/jei/LockedRecipeJeiDecorator.java` | JEI's half. |

**The rules survived.** `RecipeLockLookup`, `MachineLockStatus`, `RecipeResearchIndex` and
`LockBypassLog` import no Researchd type — they are the Minecraft-free logic with the unit tests
that hold it, and they are untouched. What went is only the glue that reads the running game. That
is the testing policy working as intended: the part worth keeping was the part that was checkable.

## What a player loses meanwhile

- **No research annotations.** Researchd loads the tree, and Craftworks' Personal Assembler
  (ADR-0089) and the Assembling Machine ask it what is Locked for the placing team, but no recipe is
  annotated in EMI or JEI and no machine names the research it waits on.
- **No machine chassis**, so none of Terra's Assembling Machines or its Chemical Plant is registered.

## The order to put it back in

1. **#260** — port the Researchd fork. It unblocks five files on its own, all of them
   pure ports.
2. **#262** — the Oritech chassis. `SimpleMachine` waits on what that decides.
3. **#251** — the Jade provider, which is where the idle note's job now lives.
