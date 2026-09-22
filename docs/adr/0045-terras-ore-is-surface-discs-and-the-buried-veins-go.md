---
status: accepted
supersedes: [57, 179]
---

# Terra's ore is surface discs everywhere, and the buried veins go

`#179` asked whether GregTech's buried veins belong in a Factorio-faithful pack. They do not, and
the reason is older than the ticket: **the veins never won an argument in the first place.** ADR-0019
put them there as the residue of vanilla worldgen — it decided flatness, cave removal and a surface
starting area, and left "everywhere else" as it found it, then commissioned prospecting to make the
leftovers legible. What looked like a decision was a default with an affordance built on top of it.

This ADR deletes Terra's ore veins and gives the whole planet one ore shape: the starting area's
flat disc, one block thick, flush with the topsoil, dealt outward by Factorio's own placement
numbers. It amends ADR-0019 (legibility, extraction, the prospecting prerequisite), amends ADR-0041
(the outfield amount arithmetic and its datum), and closes `#57`.

## Why the veins go

ADR-0043 settled that a mining rig works the **layer directly beneath it**, because a rig that
scanned downward would have ore in its area the player cannot see and the renderer cannot tint. That
is Factorio's rule and it is right. Its consequence, against buried veins, is that reaching ore means
digging down and placing the machine at depth.

That is the gesture ADR-0019 removed the world's reason for. It cut caves on the explicit grounds
that *"you do not dig because digging is not the verb, and a world with no caves is only cruel if
digging is how ore is found."* Then it kept ore buried. The pack therefore asks for the one gesture
whose supporting terrain it deleted, and it asks for it with a machine ADR-0043 designed to sit on
the surface.

**The complaint is the gesture, not the visibility.** Legibility — ADR-0019's second-ranked Factorio
feeling — has a fix that is not a worldgen change, and ADR-0019 commissioned it. The gesture does
not. A map layer over a buried vein still leaves the player digging a shaft to stand a rig at y 30.

**Fidelity is the tiebreaker, not the argument.** "Nauvis has no buried ore" does not settle this;
ADR-0019 broke fidelity deliberately once and was entitled to. The pack's fidelity decisions are
about numbers and names — ADR-0021's resource set, ADR-0028's row keys, ADR-0041's distance law —
not about geometry. The veins lose on the gesture. Factorio agreeing is a bonus.

## The decision

**Where the corpus disagrees with this ADR, the corpus wins.** This ADR was first argued from a
reading of Factorio's expressions that #317's extraction showed to be wrong in four places: the
spacing, the amplitude-then-radius crossover, the dropped ramp and the dropped edge. It is amended
in place, and a later disagreement is settled the same way: port Factorio's behaviour and amend the
text, unless Minecraft forbids it (#317).

**One ore shape on Terra.** A patch is a disc of one ore block, one block deep, landed on the
terrain surface, with Factorio's ragged edge. Terra registers **no ore veins at all**. The bedrock crude deposit is untouched: it
is a fluid, `adapted` under `#86`, and not an ore patch (ADR-0020 as amended).

**Outfield patches are worldgen; the starting area stays a stamp.** The starting area is stamped by
`planetaryfactory_core` at server start only because no `StructurePlacement` can see world spawn.
Outfield patches have no spawn anchor, so they are ordinary worldgen: one `structure_set` per
resource, which also makes them locatable — the thing the Radar will want.

**Spacing is extracted, not chosen.** Factorio adds spots until a region's target quantity is met,
so the spacing follows the *mean* spot size and not the spot count alone: one spot per
`1e6 × mean_factor / base_spots_per_km2` blocks², where `mean_factor` is the midpoint of
`random_spot_size_minimum`/`maximum`. That is 2.5 spots/km² at a mean factor of 1.125 for coal,
copper, iron and stone, ~671 blocks (~42 chunks), and 1.25 at a mean of 3 for uranium, ~1549 blocks
(~97 chunks). The generator derives
`spacing` from that field rather than carrying 40 as a literal, so a regeneration moves it, and
`separation` from the minimum distance `spot_noise` keeps between candidate spots,
`suggested_minimum_candidate_point_spacing` (~45 blocks, 3 chunks) (#320). **This is train distance, and deliberately so** — an outfield patch is not a belt run.

**Two laws, one after the other.** A spot's size and a tile's richness grow over different bands:

| | term | shape |
|---|---|---|
| density | fade `clamp((d − 150)/300, 0, 1)` × `1 + clamp((d − 300)/1300, 0, 1)` | zero to 150 blocks, full at 450, doubled by 1600, then flat |
| richness | `max((1000 + d)/2600, 1)` | flat to 1600 blocks, then linear forever |

A spot's quantity is `factor × 1e6 / base_spots_per_km2 × density(d)`, so it grows with density and
**stops growing at 1600 blocks**. Its radius is `min(32, regular_rq_factor × quantity^(1/3))` and its
blob amplitude `regular_blob_amplitude_multiplier × min(height(1600), height(d))`. Neither cap binds
at default settings (iron's typical spot is 22.9 blocks across its radius at 1600), so a far patch is
not bigger than a patch at 1600. Past 1600 only richness moves, and it moves **without limit**. That
is Factorio's, and the amount arithmetic's rising fallback is right rather than a bug.

**A patch's amount is uniform.** Each block of a disc holds `quantity × richness(d) / block count`,
with `d` the disc centre's distance from origin. Factorio's per-tile amount follows the spot's cone;
a uniform amount keeps the patch total Factorio's and lets a break read one number for the whole
disc. The spot's shape still shows in the edge, not in the amounts.

**Distance is measured from world origin, not from spawn.** Worldgen is isolated from level state by
construction — no `Structure` or `StructurePlacement` can see world spawn, custom ones included. The
radius law is worldgen, so its datum can only be `(0, 0)`; and one mechanic may not have two datums.
So ADR-0041's amount fallback is amended to measure from origin as well. The error is bounded by the
spawn-to-origin offset and lands inside the band where richness is flat anyway (1600 blocks), so
nothing observable changes near spawn and the divergence beyond is a few hundred blocks on a linear
ramp. The alternative — stamping outfield patches mod-side so they can see spawn — reintroduces the
unbounded saved data this ADR refuses below.

**The near-spawn exclusion and the ramp are the law.** Factorio suppresses regular patches inside
`starting_resource_placement_radius` (150) and fades them in over `regular_patch_fade_in_distance`
(300), full at 450. Both fall out of `density(d)`: a spot's quantity is zero inside 150 and small
until 450. No separate exclusion ships. A disc whose radius rounds below one block does not
generate.

**A procedural disc, not size-variant templates.** A continuous radius cannot come out of a jigsaw
pool dealing three fixed templates. Outfield patches are generated by a structure that fills a circle
at a computed radius, reusing the ground projection that walks a column past whatever grew there.
Each disc draws its own size factor, uniform between the resource's `random_spot_size_minimum` and
`maximum`, from the structure's seeded random. That is Factorio's `random_penalty_between`.

**The edge is Factorio's, on Minecraft's noise.** Ore sits where the spot's cone plus
`(octaves − 1/3) × blob_amplitude` is above zero. The three octaves have Factorio's input scales
(1/8, 1/24, 1/64) and weights (1, 1, 1.5), read from the corpus's `outfield_edge`. They are sampled
from Minecraft's `ImprovedNoise`, seeded by world seed and resource, because Factorio's
`basis_noise` is not published. The shape is Factorio's and the noise values are `adapted`. The
starting area keeps its template pools untouched, so the geometry check written for it still guards
what it was written for.

**Outfield patches carry no record of the pack's own.** The disc's structure piece stores its size
factor and its block count, counted when it generates, and a broken block reads them through the
structure manager. Vanilla already saves a structure's pieces with its chunk, so this adds no
saved data to the pack. The per-field census of the starting area exists solely to defeat vanilla's
silent dropping of an overlapping jigsaw child, which a spaced structure set does not suffer.

**Uranium stops being a special case.** The fallback that borrowed *"the smallest amount any other
field recorded"* existed because Factorio states no `starting_amount` for uranium. Under the
amplitude law it is dead weight: uranium has its own `base_density` (0.9) and `base_spots_per_km2`
(1.25), and the quantity expression takes both. It is derived like everything else, and it is
outfield-only, which is what `has_starting_area_placement = 0` means.

**Two amount arithmetics, and that is correct.** The starting fields take `starting_amount` over the
census block count; the outfield takes the laws. ADR-0041's comment commits to *"one arithmetic for
the whole planet"* and that commitment is retired rather than honoured. They are one arithmetic in
Factorio too: `starting_amount` is the starting patch's special case, `regular_*` is everywhere else,
and `starting_patches_split` is the seam. The pack currently fakes the second with the first. This
stops faking it.

## What this costs

**Prospecting stops being a verb, and the Radar becomes Factorio's Radar.** ADR-0019 called a
prospecting affordance *"a hard prerequisite of this ADR, not an enhancement"*, and that prerequisite
is discharged rather than met: a visible patch needs finding, not detecting. Exploration replaces
prospecting. The Radar reveals map at range — Factorio's actual Radar — instead of the ore-detection
meaning the pack had to invent for it because ore was hidden. `#57` closes. The Ore Finder Satellite
is to be cut (#322, amended below).

**GregTech's surface indicators become dead.** They marked buried veins and there are none. The
check that guards them goes with them, and both failure modes it was written for stop existing.

**The regeneration this needed is done.** #317 extracted the four per-resource arguments, the
spot's quantity and radius expressions, and the edge's octaves into `data/factorio/resource.json`,
with each resource's law tabulated against distance. No Factorio run was needed (amended below).

**What is deliberately not ported.** Nothing scales patch *count* with distance — `base_spots_per_km2`
is constant in Factorio too. Patches are land-only, confined by the land biome tag, which since #356
is every Terra biome but the Sea: the Shore is land, and the Sea is the water (ADR-0019).
Factorio also drops ore that would land on water, so the confinement is faithful as a rule and as an
outcome.

## How this is checked

Four claims, four checks. The first three launch no game:

- **The laws are Factorio's.** The resource extraction check re-derives each spot's quantity,
  radius, height, amplitude and spacing, and the edge's octaves, from the committed formulas, as it
  already does for the starting totals.
- **The amount arithmetic is right.** Unit tests under the mod's ore package cover the uniform
  amount from a stored factor and block count, richness read at the disc centre, and no cap.
- **The registries are what we said.** The worldgen registry check asserts Terra's ore vein registry
  is **empty** and that the resource structure sets are present — a stronger assertion than the
  fixture makes today.
- **A disc lands on the terrain.** `OutfieldDiscTests`, in the GameTest run, resolves each
  resource's structure set from a running server's registry and places a disc. It asserts every
  column holds exactly the shape's ore, one deep and flush with the ground and under a tree, that
  nothing generates inside 150 blocks or off the land, and that a break reads the disc's amount (#320).

The vein indicator check is deleted with the veins. What stays unchecked and is a world load: whether
discs land on real terrain across Terra's biomes, since the GameTest world is flat plains.

## Amended after building it (#321)

This ADR was written `provisional`. Three of its sentences stopped being true while it was built,
and one answer it named was wrong.
The decision itself is unchanged. It is `accepted` now that uranium, the last resource, generates
and every outfield patch in the tree is the one this ADR describes.

**The Ore Finder Satellite is cut rather than rehomed.** As written, this ADR said the satellite
"loses its stated job and needs a new one or needs cutting; that is its own ticket, not this ADR's
call." That ticket is #322, and its decision is to cut it. The live documents still name the
satellite until #322 lands. The decision rests on three grounds. Factorio has no such mechanic; its Radar is the only scanning it has. GCyR, the mod
that supplied the satellite, left with ADR-0060. And this ADR had already removed the hidden ore the
satellite existed to find, so no job was left to give it.

**The corpus blocker lapsed without a game launch.** The first text said a regeneration was required
before this could be built, because four constants
(`regular_blob_amplitude_maximum_distance`, `regular_rq_factor`, `random_spot_size_minimum` and
`maximum`) were not in the committed corpus. They were in the prototype dump already on disk the whole
time. #317 read them out of it with `scripts/factorio-resource-extract.py` and needed no Factorio run.
Nobody should schedule one for this.

**The disc-on-terrain question is answered.** The first text filed it as a world load no check could
reach, "the same class of question the starting area's geometry check cannot answer either." The
GameTest harness did not exist yet. #320's `OutfieldDiscTests` now answers it on the flat test
world, and it joins the three checks above. Uranium joins its set in #321. Only the biome spread
stays a world load. The first text read "Three claims, three checks, none of which launches the
game" and ended: "What stays unchecked and is a world load: whether a procedural disc lands on real
terrain across every biome, which is the same class of question the starting area's geometry check
cannot answer either."

**Uranium pays out FTB Materials' Raw Uranium.** The enum named `gtceu:raw_uranium`, which stopped
naming an item when GregTech left. No uranium block had been placed before this, so nothing noticed.
The drop is `ftbmaterials:uranium_raw_ore` (ADR-0061). A drop that resolves to nothing now fails
`tests/pack/test_ore_assets.py` and the disc GameTest.
