---
status: accepted
---

# Placement is a plan, and the preview draws it

> **Belt part moved to Beltworks.** How a belt piece plans its placement now lives in Beltworks
> (5thlayer/beltworks), as its ADR 0006. The plan and preview seam for the pack's own blocks
> stays here.
>
> **The mechanism moved to placementpreview** (#446). The plan, the vanilla plan, the opt-in and
> the drawing are the 5thlayer/placementpreview library's, per its ADR 0001 and Beltworks' ADR 0010,
> so a belt piece and a pack block preview through one renderer. The decision below stands; the pack
> keeps its own items' plans, its refusals and what it draws beside a plan.
>
> **Amended by #450: every oriented block is drawn, not only the pack's.** The opt-in widens from
> the pack's namespace to any block with a facing, an axis or a sixteen-way rotation, so held stairs,
> a chest or another mod's machine draw a preview, since the library's Rotate the Plan turns only what
> is drawn (5thlayer/groundworks ADR 0003). Those blocks get vanilla's plan and its refusals only; the
> pack owns no other mod's placement rules. Two quirks are accepted: a two-part block such as a door
> or bed draws one half, and a block that sets its state after placement may draw in a state it will
> not keep. A block from another mod with no orientation, such as stone, still draws nothing.

Factorio draws what placing a block would do before the click. #297 brings that in as the
**Placement Preview**, and #158 (the supply area) and #298 (a pole's wires) draw on top of it. The
obvious shape -- a client renderer that works out where the block would land -- is a second copy of
every placement rule the pack has, and the two drift silently: a preview that lies is worse than no
preview, because a player builds against it.

## Decision

**Placement is computed as a plan, and executed separately.** A **Placement Plan** is what a held
item would do at an aimed spot: the positions it would fill and the blockstate at each, plus a
refusal or none. Every pack item that places a block answers for its own plan, in one interface
under `core/placement/`. Placing then *executes* a plan -- build it, refuse if it is refused,
otherwise put the blocks down -- so there is one rule and the preview asks the same question the
click does. A plain `BlockItem` defers to vanilla's `BlockPlaceContext`, which is what gives facing,
replaceable blocks and "can this state survive here" without the pack restating any of it.

We rejected a client-side registry of preview rules keyed by item. It needs no refactor and is the
drift this decision exists to prevent.

**Every pack block gets one; no other mod's does.** The mechanism is generic and keyed on the held
item placing a `planetaryfactory:` block. Other mods' placement refusals are theirs, and owning them
is unbounded. The pole's column rules and the rig's footprint are the only two plans that are not
vanilla's today.

The pack's SimpleBelts fork is the one exception, because the pack owns that fork. Its belt item
answers to the same contract, that the click executes the plan the preview draws, through a plan type
of its own, since the fork cannot depend on the core (#372). Its splitter item is the other way
round: the core answers for its plan, both halves faced or refused whole, built from the same two
methods the fork's click calls, so the fork stays free of the pack and the core previews it (#355).

**A multiblock is one plan and refuses as a whole.** The rig places its anchor and every part in one
gesture, so a single blocked part refuses the placement, and the whole footprint draws red. Drawing
one part red and the rest translucent would promise a partial placement the game never performs.
Oritech's multiblocks are not the pack's blocks and get no plan.

**A refusal is any reason placing would fail**, vanilla's as much as the pack's. Where the aim yields
no target at all there is no plan and nothing is drawn, which is most of vanilla's refusals: the ray
hits a block and placement simply picks another spot rather than refusing one.

**The preview is drawn, unconditionally, from the client's own plan.** The block's own model at each
of the plan's positions -- not a wireframe box, which cannot show the facing the plan just decided;
a box is the fallback only for a block whose model will not render out of context, if one turns up --
translucent, tinted red when the plan is refused, on
`RenderLevelStageEvent.AfterTranslucentBlocks` -- the same kind of hook the wire renderer already
uses, and no mixin. It is shown whenever a previewable item is in the main hand and the aim hits a
block in reach, as in Factorio, with no keybind and no toggle: a build preview behind a setting is a
build preview nobody finds. The client builds the plan locally rather than asking the server, the way
it draws wires locally; the authoritative answer is still the server's on the click, and a stale
chunk resolves next tick. The plan is cached on the item, the aimed position, the facing and the hit
face, so a rig's few hundred block checks do not run every frame.

A multiblock draws only the faces on the outside of its plan: a model quad whose cull face touches
another block of the same plan is skipped, the way the placed blocks cull each other, and so is one
the world already hides, such as a rig's underside on solid ground (#311). The
outside follows the footprint, not its bounding box, so a hollow or concave footprint keeps the faces
around its hole -- the preview promises the shape that will stand. A quad with no cull face always
draws, so a part whose model is not a full cube can still show inner faces.

Vanilla's white block outline stays as it is. It marks what the player is aiming at, which is still
true, and suppressing it would be a second render hook for a problem nobody has reported.

## Consequences

- `SupplyAreaPoleItem`/`SupplyAreaPoleBlock#useItemOn` and `RigBlockItem` are refactored plan-first.
  That refactor is the bulk of #297; the renderer is thin on top of it.
- #158 draws the supply area at the plan's position and #298 the wires the plan's pole would add.
  Neither chooses its own trigger or target.
- A new pack block that places unusually must answer for its plan, or it silently previews as
  vanilla would place it.
- Checks: the geometry under a plan is Minecraft-free and already tested (`RigGeometry`, the pole's
  column rule). The plan itself is a GameTest -- built in a world, asked for a plan, asserted against
  what placing actually does -- because a plan that disagrees with placement is the defect this
  decision exists to prevent, and it is server-side and so within a GameTest's reach. Whether the
  preview draws correctly is a human check on delivery.
- The hook is `SubmitCustomGeometryEvent`, not `RenderLevelStageEvent.AfterTranslucentBlocks`. 26.1
  moved level rendering behind the submit-node collector and that event hands out no collector;
  NeoForge's own answer is the submit event, and the translucent render type still puts the quads in
  the translucent pass. Still no mixin, which is what this decision was about.
- Not included: a preview for other mods' blocks, fast replace (a different-tier pole aimed at a pole
  stays refused), and rotating a preview before placing.
