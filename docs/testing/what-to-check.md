# What to check

Before a feature is built, one question has to be answered: **does this need a check, and of what
kind?** This document answers it. It exists so that the answer is looked up rather than re-argued,
and so that "no check" is a decision somebody recorded instead of a step somebody skipped.

The document is keyed on the **claim** a feature makes, not on the harnesses a Module happens to
own. Harnesses come and go; the claims do not. Find the claim, read off the check. It guides tickets
in each Module's repo, and the checks themselves live in that Module: GameTests, unit tests and
static data checks.

## The six claims

| The feature claims | The check |
|---|---|
| This data parses, and this thing is registered | **Nothing.** Load-time facts are free. |
| Cross-file references resolve | **Static data check**, a test with no game launch. |
| This is emitted into a world | **Fixture row** in a worldgen GameTest. |
| This Module logic computes something | **Unit test**, run without Minecraft. |
| This block or entity behaves in-world | **Nothing** if vanilla by construction, else **GameTest**. |
| This looks or feels right | **Human on delivery.** |

### Load-time facts are free

Malformed datapack JSON fails the load, loudly, with the offending file named. A biome, a feature
or a noise setting that does not parse never reaches a world, and no check a Module could write
would tell you anything the crash log does not.

**The trap: "a codec catches it" is true for malformed and false for absent.** A codec validates
the files it is handed. It has nothing to say about a file that was never written, a biome that
parses perfectly and is never placed, or a vein registered against a dimension nobody visits.
Absence is the failure mode this row does not cover, and the two rows below are where it is caught.

### Cross-file references resolve

A Module's content is spread across its registries, datapack JSON, loot tables, blockstates, model
and texture files and lang. Each of those parses in isolation. Nothing in Minecraft's loading checks
that they agree with each other: a lang key naming a block that was never registered, a blockstate
pointing at a texture that does not exist, a loot table for a block that dropped out of the mod. All
of these load without complaint and fail in front of a player.

That is what a static data check catches. It reads the registered ids out of the mod source, reads
the ids out of the datapack and assets, and asserts that every reference on either side resolves,
plus the facts that no file could contradict on its own. It launches no game and needs no JVM.

**These checks grow by assertion, not by fixture.** Adding a feature to a static data check means
writing the assertion. This is the opposite of the fixture row below; do not read the fixture
check's "a new body adds data, not code" rule as covering both.

**Block hops are one generic walker.** A check that walks blockstate, model parents, textures, lang
key, item model and loot table for every block a Module registers fails on a registration it cannot
name, rather than skipping it. The judgement assertions stay in each machine's own file: the ones no
generic walker could make.

A hop that is present but rejected by the game is seen only in the client, by a human (ADR-0119); a
hop that is simply absent is logged nowhere, so it needs the static walker.

### This is emitted into a world

A registry that loaded is not a world that contains anything. Ore patches, oil wells, worldgen
layers and biomes all have a state in which they parse, register, and are then never placed. The
check is a launch into a fresh world that asserts against what the game actually loaded and, for
biomes, actually located. In a Module's repo that is a worldgen GameTest.

**The client is a second world, and a human checks it (ADR-0119).** A registered block is not a
block anybody can see, and it is read by a client that no server run starts. So *this renders at
all* is checked by a human in game on delivery, with *this looks or feels right*. A missing item
model definition is logged nowhere, so that half stays static.

**A fixture row is unconditional.** Every body-level worldgen fact gets one; there is no judgement
call about whether a given patch is important enough. The launch happens regardless and rows are
free after the first, so the cost of the rule is nil and the cost of re-arguing it per ticket is
not. A new body adds a fixture entry; it does not add harness code.

**A recipe that is removed is the same claim read backwards.** A removal is a claim about the loaded
recipe manager, which exists only in a running JVM: nothing static can see that a stock recipe
survived a removal. A removal therefore splits in two: a static check that the files agree with each
other, and a world load with 0 failed recipes and no route to the removed recipe in the recipe
viewer, asserted by a human on delivery. A removal of a *surface*, such as a crafting grid, is not
seen by a recipe dump at all, since the recipes it lists are still there and unreachable; its check
is a human on delivery, with the steps in the ticket.

### This Module logic computes something

Where a Module computes rather than declares, such as a sampler, a state transition or anything with
arithmetic in it, the check is an ordinary unit test, run without Minecraft.

The line is between a Module's logic and game features. A subclass that returns a feature holder is
testing Minecraft, not the Module, and gets nothing.

### This block or entity behaves in-world

Most of what a Module ships is vanilla by construction: a real `SaplingBlock` grows the way a
sapling grows, and a `TreeGrower` subclass hands back a feature holder. Checking those tests
Minecraft. They get nothing.

**A GameTest is warranted when all three hold.** The behaviour

1. runs on world state: ticking, neighbour updates, block interaction; **and**
2. is not inherited unchanged from a vanilla superclass; **and**
3. cannot be reached from a plain JVM unit test without a server.

GameTests run headless: no display, no human, and the command fails when a test fails. They build
the mod, resolve the Minecraft artifacts and boot a server, which takes seconds rather than
milliseconds, so they belong to a change that touched mechanism, not to a docs edit.

The shape of that decision generalises. Most of what looks like in-world behaviour is arithmetic
that has no business needing a server, and it should be written to be reachable without one:
geometry, unit conversion and rationing are Minecraft-free classes with unit tests. What is left
over, such as a capability lookup finding a machine, or inserting energy making it run, is the part
a GameTest exists for. **Split first, then check what remains**; the residue is usually much
smaller than the feature.

**A Module tests its own machines, and another Module only through what it documents.** What a
Module's own machine does, such as a pole's network, a pipe's segments, a dismantle gesture or an
assembler's craft, is that Module's to test in its own run. A check in one Module reaches another
only through what that Module documents: a block or item by registry id, a tag, a capability, a
config key or its named API. A release of the other Module then breaks the check only when it breaks
what it promised.

### This looks or feels right

Sky colour, ground stone, whether a patch yields the intended ore under its intended name, whether a
loop is any fun. Some of this is genuinely unautomatable and some of it merely has not been
automated; the document does not pretend to know which in advance. Either way the check is a human,
on delivery, running the steps the ticket lists.

**Human steps live in the ticket, and nowhere else.** They are written in its `## Checks` section
and asserted when it closes. There is no standing manual-check document: one that nobody runs goes
stale the day it is written, and a per-ticket list is verifiable at the moment it matters.

## What a ticket must do

**A ticket that changes a Module's content carries a `## Checks` section**, one line per claim it
makes:

```markdown
## Checks

- emitted → fixture rows for the new ore patches in the worldgen GameTest
- references → assertions that the loot tables and lang entries resolve
- in-world behaviour → none; the sapling is a real `SaplingBlock`, vanilla by construction
- looks right → human: find a patch and confirm it carries the intended ore
```

Naming the claim and then writing `none` is the point. It makes an unchecked feature a decision
with a reason attached, which is reviewable, instead of an omission, which is invisible.

**Docs and process tickets omit the section.** A hollow `Checks: none` on every meta-ticket trains
readers to skip the section on the tickets where it matters.

**When no seam exists, the ticket does not block.** Fall back to a human check on delivery and say
so. If the fact is not humanly observable either, record `unchecked — <reason>` and ship. A policy
that stops work gets routed around, and the missing seam is a ticket of its own.
