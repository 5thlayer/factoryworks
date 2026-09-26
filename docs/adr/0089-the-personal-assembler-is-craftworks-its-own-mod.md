---
status: accepted
---

# The Personal Assembler is Craftworks, a mod of its own

The planner that ADR-0038 describes, and that ADR-0064 to ADR-0066 refined, is not specific to
Factorio: any pack could use a queue that chain-crafts, reserves every input up front and refunds on
cancel. The code under `core/assembler/` also hard-wires this pack's choices into Java: the recipe
type, the `category: crafting` filter, Researchd as the lock, and the inventory screen as the only
surface.

**Decision (#291).** The mechanic moves to **Craftworks** (`craftworks`, 5thlayer/craftworks), and the
pack depends on it the way it depends on Beltworks: a local jar pinned in `data/pack/local-jars.json`
as `craftworks=<version>` (Maven `io.github.5thlayer:craftworks`). Craftworks' own ADRs 0006 and 0007
and its spec, 5thlayer/craftworks#2, record its side. For the pack, it means the following.

- **Recipes.** Craftworks plans only with its own recipe type, `craftworks:assembling`, and accepts
  no foreign types. A recipe carries `ingredients` (counted, tags allowed), `result`, `time` in whole
  ticks (default 10) and `priority` (default 0). It takes items only. Because a recipe id has one
  type, every recipe in ADR-0063's hand set becomes two recipes with two ids: the converter emits a
  `craftworks:assembling` copy beside `planetaryfactory:assembling/<name>`. This amends ADR-0063,
  under which the hand and the Assembling Machines shared one file. The machine's recipe and its id
  are unchanged, so research unlocks keep their keys.
- **Locks.** Craftworks locks by `lockSource` (`none` or `recipeBook`) and by hooks registered through
  its Java API or a KubeJS event. A recipe is Locked if any of them says so. The pack sets `none` and,
  once Researchd returns (#260), registers a hook that maps a hand recipe's id to the machine id its
  research unlocks key on (ADR-0063).
- **Screen.** Craftworks always removes the 2x2 grid and draws the queue in its place, which is
  ADR-0066's behaviour. There is no access gate and no item: every player has the Assembler.
- **Vanilla recipes.** Craftworks ships vanilla's crafting recipes, converted in place, as a built-in
  datapack. The pack turns it off with Craftworks' server config flag, since ADR-0034's sweep admits
  no stock recipe that has not been re-authored.

**Sequencing.** Craftworks is built to parity in its own repo first. Parity means its core, the recipe
type, the queue held on the player (with the HUD and the refund on death), the inventory screen with
the Crafting Plan screen, and EMI's Fill Recipe. JEI, KubeJS, the Lock API and the vanilla datapack
do not block the switch. The pack then switches in one change: it deletes `core/assembler/` and its
tests, adds the jar row, emits the hand copies and configures the mod. It goes through the release
train like Beltworks. The pack has no public release, so the switch migrates nothing: its notes tell
a player to empty the Assembler queue before updating, and the old queue attachment is dropped.

**Considered.**

- *A subproject in this repo.* Rejected: a public mod needs its own issues, releases and CI, and this
  repo's checks and ADRs are about the Factorio pack.
- *The pack keeps its own copy.* Rejected: two copies of ADR-0038's pause and refund rules would drift.
- *Craftworks admits `planetaryfactory:assembling` by a config predicate*, as #291's first draft had
  it, so the hand and the machine keep one file. Rejected in Craftworks' ADRs: a planner that reads
  foreign types has to guess each type's shape, and only its own type guarantees items-only recipes
  with counts.

**Consequences.**

- `test_hand_resolver.py` and `test_research_unlocks.py` read the `craftworks:assembling` copies, and
  a check holds each copy to its machine recipe, so the two ids cannot drift apart.
- The unit tests under `mod/src/test/java/com/planetaryfactory/core/assembler/` move to Craftworks'
  repo, along with the rules they hold.
- `CLAUDE.md`'s "Assembler queue and resolver check" section is rewritten for what stays in the pack.
