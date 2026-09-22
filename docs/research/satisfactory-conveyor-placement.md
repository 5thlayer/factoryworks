# How Satisfactory constrains conveyor placement without a tile grid

**Question** (#364, for #362): Satisfactory lays belts in 3D between machines at any height, with
free curves and no undergrounds. How does it keep that readable, and what should the SimpleBelts
fork take from it: quantise the spline to the block grid, or constrain it?
**Retrieved**: 2026-09-22, against Satisfactory 1.1 / 1.2 (the wiki's history runs to Patch 1.2.4.0).

**Sources.** The official wiki is `satisfactory.wiki.gg` (the official one since the move off
Fandom). Its patch pages transcribe Coffee Stain's own notes and link them. Coffee Stain's Q&A site
(`questions.satisfactorygame.com`) and `satisfactorygame.com/updates/*` are single-page apps that
return no text to a fetch, so patch notes are cited through the wiki's transcriptions, which name
the official post. The Fandom mirror returned HTTP 402 and is not used. A claim marked
**lower trust** rests on a Steam community thread and nothing primary.

**This is research, not a decision.** The recommendation at the end goes back to #362, and the ADR
is written there.

## Scale

Satisfactory measures in metres and its belt is **2 m wide and 1 m tall**
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). A foundation is **8 m × 8 m**, 1, 2 or 4 m
tall (<https://satisfactory.wiki.gg/wiki/Foundations>). The fork's belt is about one block wide, so
comparing in **belt widths** is fairer than in metres: one Satisfactory metre is about half a block.
Both conversions are given where a number is carried over.

## Where the fork stands

These were checked against `~/minecraft_mods/simplebelts-src` on branch `planetaryfactory`, with
paths relative to `common/src/main/java/rearth/belts/`.

- A belt is a chain of cubic Hermite segments (`util/SplineUtil.java:147`). Its control points are a
  start loader, the `conveyor_support` blocks clicked in order, and an end loader
  (`items/BeltItem.java:103-118`, `:140-148`).
- Every tangent is horizontal and axis-aligned. A support has `HORIZONTAL_FACING`
  (`blocks/ConveyorSupportBlock.java:43`), and `getPointPairs` picks that axis's sign by whichever
  end is nearer the previous point (`util/SplineUtil.java:105-133`). A belt's end dropped on a floor
  or ceiling takes the player's horizontal facing (`items/BeltItem.java:125-127`).
- Length is three chords per segment (`util/SplineUtil.java:74-87`). Cost is
  `ceilDiv(round(length / 0.125), 8)` (`model/BeltCost.java:13-18`), which is `ceil(length)` taken
  on whole 1/8-block slots.
- The preview only warns. A dot turns orange when the unit direction between samples about 0.1
  blocks apart changes by more than 0.25, and red above 0.43
  (`client/renderers/BeltOutlineRenderer.java:115-122`, `:170`). That works out to roughly 14° and
  25° per tenth of a block, so a turn radius near **0.4 and 0.23 blocks**. The spline's parameter is
  not arc length, so these figures are approximate. Nothing is refused on any geometry.

## 1. The placement gesture

**Endpoints snap to ports and poles.** A belt is built between two connection points: a building's
port, an existing belt end, or a pole. "When building between poles, the first snapping point will
always be the input and the second the output, unless the first point is connecting to an input of
another building or conveyor belt" (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). A port's
direction is fixed, and the wiki's tips note that outputs show green arrows and inputs orange bars
(same page). "Conveyor Belts have to be always constructed second, after the buildings they are
connected to" (same page).

**A pole is placed for you.** "Placing down a Conveyor Belt on the ground automatically places a
Conveyor Pole with it" (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). Patch 0.8.0.0 (Update
8) "Added automatic placement of a Conveyor or Pipe pole when not building a Belt or Pipe from an
existing connection point" (<https://satisfactory.wiki.gg/wiki/Patch_0.8.0.0>, transcribing
<https://www.satisfactorygame.com/updates/update-8>). A belt can end on top of a Constructor
"provided there is a valid spot on which a conveyor pole can be automatically placed"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>).

**The supports come in fixed heights, on fixed surfaces**
(<https://satisfactory.wiki.gg/wiki/Conveyor_Poles>):

| support | height | attaches to |
| --- | --- | --- |
| Conveyor Pole | 1 to 7 m in 2 m steps | ground or foundation |
| Stackable Conveyor Pole | 3 m, then 2 m per stacked pole (2n+1) | ground, then on itself, indefinitely |
| Conveyor Wall Mount | 1 m, fixed | sides of walls and foundations only |
| Conveyor Ceiling Mount | 2n+1 m | ceilings and other ceiling mounts |
| Conveyor Wall Hole | 2 m, fixed | walls only, placed freely on the wall (Patch 1.1.0.0) |

Since Patch 0.7.0.0 poles "contextually switch between floor, wall and ceiling attachments based on
where the player aims while building" (<https://satisfactory.wiki.gg/wiki/Conveyor_Poles>). Pole
height and rotation became adjustable while building in Patch 0.3 (same page).

**Build modes.** Belts have three, cycled with R (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>):

- **Default**: the wiki says only "The default build mode." Its shape is a free curve between the
  two endpoint tangents.
- **Straight**: "only constructs belts with straight lines and 90° turns, except when placing a belt
  in such a way that in and output are in the same direction but a few meters apart in which case it
  creates an S-shape." It arrived in Patch 1.0: "You can now build 90º turn conveyor belts in a
  straight shape, instead of the usual curved" (<https://satisfactory.wiki.gg/wiki/Patch_1.0>,
  transcribing <https://www.satisfactorygame.com/updates/1-0>).
- **Curve**: "curved in a way similar to railways, according to the position and orientation of the
  starting and ending points." The belt page dates it to Patch 1.1.0.0. Coffee Stain's 1.1 notes
  describe the Curved mode in the paragraph about pipes ("we have now added it for Pipes ... we also
  included a Curved build mode") (<https://satisfactory.wiki.gg/wiki/Patch_1.1.0.0>, transcribing
  <https://store.steampowered.com/news/app/526870/view/520835140699555676>). **The sources
  disagree slightly**: the official note puts Curve beside pipes, and the wiki's belt page counts
  it as a belt mode too.

**Horizontal-to-Vertical is a pipe mode, not a belt mode.** The ticket names it, but it appears only
among the pipeline's six modes: Auto, Auto 2D, Noodle, Horizontal to Vertical, Curve and Straight
(<https://satisfactory.wiki.gg/wiki/Pipelines>). Its description is "maintain the same level as the
starting point (traveling horizontally), then turns vertically when approaching the ending point"
(same page). Belts get the same split from a rule instead of a mode (see section 2).

Straight and Curve came five years after Early Access began, as **optional generators**. They
reshape a free placement. They do not restrict the default gesture.

**Placement aids around the belt.** Holding Ctrl shows guidelines: "Solid color (when aligning a
building from the sides) and Gradient (when snapping buildables from an input or output)"
(<https://satisfactory.wiki.gg/wiki/Patch_1.0>). Nudging (H) locks a hologram and moves it 1 m per
key press, or 0.5 m with Ctrl, and since Patch 1.1.0.0 it moves vertically with no distance limit
(<https://satisfactory.wiki.gg/wiki/Build_Gun>).

**Carries over?**

- *Ports and poles as the only control points:* **maps onto the model.** Loaders are the ports and
  `conveyor_support` is the pole. The fork is already tighter, since its supports face only the four
  horizontal directions, where a Satisfactory pole rotates freely.
- *Pole auto-placed at a free end:* **maps.** The fork already makes a loader at a free end, which
  is #354's charge.
- *Wall mount, ceiling mount, stackable pole:* **maps, with a new block state at most.** In Minecraft
  any block face is a wall or a ceiling, so a support that hangs from the block above or stands on a
  wall is a placement rule for `conveyor_support`, not a new mechanic.
- *Straight and Curve modes:* **maps.** Both are path generators between the same control points.
  They need a mode on the belt item and no new block.
- *Nudging, and free rotation of poles:* **needs what Minecraft lacks**, namely off-grid placement.
  The block grid removes the need.

## 2. The limits it enforces

From <https://satisfactory.wiki.gg/wiki/Conveyor_Belts> unless noted:

| limit | Satisfactory | in belt widths | in blocks (1 m ≈ ½ block) |
| --- | --- | --- | --- |
| segment length | "~0.5 up to 56 meters", Euclidean, "exactly seven Foundations" | 0.25 to 28 | ~0.25 to 28 |
| minimum turn radius | 2 m | 1 | ~1 |
| maximum slope | 35°, `arctan z/√(x²+y²)` | same | same |
| slope while turning | not allowed: "They will do one, then the other" | | |
| steepest short rise | 31 m up over 45 m across | | |

**The length changed once.** Patch 0.3.6 "Increased maximum length from 48 to 56 meters"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>, History). No other change to length, radius or
slope appears in the belt page's history, which runs from Patch 0.1.14 to 1.2.4.0. The 31 m / 45 m
figure is consistent with the others: arctan(31/45) is 34.6°, and the chord is 54.6 m, under 56.

**A belt never turns and climbs at once.** "Conveyor Belts cannot incline along the Z-axis while
turning in the XY plane. They will do one, then the other"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). Every Satisfactory curve is therefore flat,
and every slope is a straight ramp. That is the belt's version of the pipe's Horizontal-to-Vertical
mode.

**Vertical runs are a different buildable, the Conveyor Lift**
(<https://satisfactory.wiki.gg/wiki/Conveyor_Lifts>):

- It is strictly vertical, between 4 m and 48 m tall, adjusted "in one-meter increments".
- Lifts stack end to end without limit, and a Conveyor Lift Floor Hole lets one pass through a
  foundation. Floor Holes and the Reversed mode came in Patch 0.5.0.0.
- "The starting point of the lift snaps to an existing Belt, but the ending point does not."
- R reverses its direction unless a connection has fixed it.
- Throughput does not depend on height.

A belt climbs at most 35°. Anything steeper is a lift, so no belt ever needs a slope rule beyond the
35° cap.

**Carries over?**

- *Maximum segment length:* **maps.** A segment is support to support, and the fork has no cap.
  56 m is about 28 blocks. SimpleBelts' reach is a separate question (#341).
- *Minimum turn radius:* **maps**, as a refusal on the spline's curvature. Where a belt uses its
  tangent length is where the rule lands. Today's orange and red preview dots fire at a radius of
  about 0.4 and 0.23 blocks, well below Satisfactory's one belt width.
- *Maximum slope, and no turning while climbing:* **maps.** Every fork tangent is horizontal, so a
  Hermite segment between two supports at different heights is a vertical S-curve whose steepest
  point is in the middle. A cap can be checked on the sampled curve. "One, then the other" is a
  generator rule: turn flat at a support and climb only on a straight run.
- *The lift:* **needs a new block.** A vertical belt in Minecraft is a column of blocks with a
  bottom and a top port, much like the pole column of #297. Nothing in the loader-support-spline
  model can express a 90° climb, since its tangents are horizontal.
- *Floor hole:* **needs what Minecraft lacks, and the pack does not need it.** Minecraft has no thin
  foundation to pass through, so the lift column is itself the hole.

## 3. Refuse or bend

Satisfactory does both, and chooses by what kind of limit it is.

- **Geometry the chosen mode cannot draw is refused.** Default builds the free curve, and the belt
  turns red when the curve is impossible. Patch 0.8.0.0 "Moved validation check for building
  Conveyors and Pipes to final placement step" (<https://satisfactory.wiki.gg/wiki/Patch_0.8.0.0>),
  so the check runs once both ends are chosen. The hologram is blue when valid, red for hard
  clearance and yellow for soft (<https://satisfactory.wiki.gg/wiki/Build_Gun>).
- **Mode rules reshape the placement.** Straight turns a free placement into straight runs, 90°
  turns and an S where the ends are parallel. Turning and climbing together becomes one after the
  other (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). The player picks the endpoints and
  the game picks the path.
- **In the modes that reshape, a turn too tight for them is refused.** A player report says Straight
  and Curve refuse corners that Default accepts, with the message "invalid belt shape", after 1.1
  (<https://steamcommunity.com/app/526870/discussions/0/601907762377559337/>). **Lower trust**: it
  is one community thread with no developer reply, and the wiki does not state it. Patch 1.2.4.0's
  fix of belts "not properly building straight after previously building a segment with a 90 degree
  bend while using Straight belt build mode" shows the mode's path is computed
  (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>, History).
- **Terrain can refuse the pole.** "The floor is too steep" is refused when the ground under the
  auto-placed pole is too uneven (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>).

**Carries over?** **Maps.** ADR-0069's placement plan already has both outcomes: a plan is either a
shape or a refusal. A generator mode returns a reshaped plan. A limit the plan breaks returns a
refusal, and the preview draws it red. The fork's preview today is Default mode with no refusal.

## 4. Obstruction and crossing

**Belts collide with nothing.** Since Patch 0.5.0.0, soft clearance covers "cosmetic and structural
buildable's as well as Power Poles, Splitters, Conveyor Belts and Pipes", which "can now be built
overlapping with any other buildings". Hard clearance covers "all buildings that have functionality
such as Constructors, Assemblers, The HUB", which "cannot be built overlapping with each other"
(<https://satisfactory.wiki.gg/wiki/Build_clearance>). The belt page agrees: belts "can also
intersect each other, some non-colliding entities, and terrain freely"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). Patch 1.1.0.0 gave Conveyor Poles soft
clearance too (<https://satisfactory.wiki.gg/wiki/Patch_1.1.0.0>). What remains is a yellow
hologram, a warning and not a refusal (<https://satisfactory.wiki.gg/wiki/Build_Gun>).

So Satisfactory checks **no clearance on a belt's path**. It prevents nothing, and a belt through a
machine or another belt is legal. Crossing is solved the way the pack solves it: one belt goes over
the other, and nothing stops it going through instead. The splitter is the exception in practice.
The wiki advises building parallel belts before a splitter or merger because their collision boxes
are large (<https://satisfactory.wiki.gg/wiki/Conveyor_Merger>).

**Carries over?** **Maps, with nothing to build.** It is the fork's current behaviour, and it keeps
undergrounds `excluded`. It also means Satisfactory is no evidence for #341's obstruction check: the
game has none for belts.

## 5. Readability: mechanics and habits

Satisfactory's belts can curve freely and pass through anything. What keeps a base legible is where
their **ends** can be, not the belts themselves.

Mechanics:

- **A grid for control points.** Foundations snap to each other, and with Ctrl since Patch 0.5.0.8
  to a world grid "snapped to the nearest 800 cm in x-z direction and nearest 100 cm in y-direction"
  (<https://satisfactory.wiki.gg/wiki/Foundations>). Patch 0.7.0.0 cut the vertical step from 200 to
  100 (same page). Machines, poles and mounts stand on that grid, so a belt's two ends are almost
  always grid-aligned points facing along grid axes. The 56 m cap is "exactly seven Foundations"
  (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>).
- **Discrete support heights.** Pole heights step by 2 m and lifts by 1 m, so parallel belts sit at
  the same few heights (<https://satisfactory.wiki.gg/wiki/Conveyor_Poles>,
  <https://satisfactory.wiki.gg/wiki/Conveyor_Lifts>).
- **Flat curves, straight slopes, vertical lifts.** A belt never turns and climbs at once, and a
  climb steeper than 35° is a lift (section 2).
- **Straight mode**, from 1.0, for Factorio-style runs and 90° corners (section 1).
- **Guidelines and nudging** for alignment (section 1).

Habits: busses, manifolds, belt floors, and the rule of never crossing through a machine are
conventions. Nothing enforces them. The wiki pages cited here document none of them, and they are
not sourced here beyond that.

**Carries over?** **Mostly free.** Minecraft's block grid is a world grid for every control point,
at a finer step than Satisfactory's 8 m, and the fork's supports face only four directions where
foundations rotate in 45° or 5° steps (<https://satisfactory.wiki.gg/wiki/Foundations>). The pack
already has everything Satisfactory uses for readability except the flat-turn rule, Straight mode
and the lift. Free-placed and freely rotated foundations **need what Minecraft lacks**, and the pack
does not want them.

## 6. Cost and capacity

**Cost is per length, rounded up per segment.** "The building cost is roughly 0.5 materials per
meter, rounded up to nearest integer", in one ingredient per mark (Iron Plate for Mk.1, then
Reinforced Iron Plate, Steel Beam, Encased Industrial Beam, Alclad Aluminum Sheet, and Ficsite
Trigon plus Time Crystal for Mk.6) (<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). A turn is
paid as its arc length. There is no fixed charge per turn. Poles are paid separately at 1 Iron Rod,
1 Iron Plate and 1 Concrete, doubled for the stackable and mounted kinds
(<https://satisfactory.wiki.gg/wiki/Conveyor_Poles>).

**A lift costs by height, rounded to an even number of metres.** It is "1 x base cost per meter, but
the height used in calculating the cost is rounded up to the nearest even integer"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Lifts>), so a 9 m or 10 m Mk.1 lift costs 10 plates.

**Only what a belt holds depends on its exact length.** "A single item occupies roughly 1.186 meters
on a belt. 843 items fit on a belt 1000 meters long"
(<https://satisfactory.wiki.gg/wiki/Conveyor_Belts>). Throughput is per mark and independent of
length, and a lift's does not depend on height (<https://satisfactory.wiki.gg/wiki/Conveyor_Lifts>).

| | Satisfactory | the fork |
| --- | --- | --- |
| cost | `ceil(0.5 × length m)` per segment | `ceil(length)` per belt, on 1/8 slots (#346) |
| per belt width | about 1 ingredient per belt width | 1 item per block, which is about 1 per width |
| turn | its arc length | its sampled length |
| capacity | about 0.84 items per m, about 1.7 per belt width | 8 per block (#344) |
| vertical | lift, cost by even metres of height | none |

**Carries over?** **Maps as it is.** The fork's rule is Satisfactory's at the same rate per belt
width, rounded per belt rather than per segment. Satisfactory shows that per-length cost with
rounding up is enough. The layout does not need whole numbers for the cost to be legible. The eight
items per block are Factorio's density, and nothing here argues against them.

## 7. What carries over, collected

| finding | carries over? |
| --- | --- |
| ends snap to ports and poles, on fixed facings | maps (already true) |
| pole auto-placed at a free end | maps (the loader at a free end, #354) |
| wall and ceiling supports | maps (a placement rule for `conveyor_support`) |
| Straight and Curve modes | maps (a path generator in the belt item and the plan) |
| minimum turn radius 2 m, refused | maps (a curvature refusal in the plan) |
| maximum segment 56 m | maps (a cap between control points) |
| maximum slope 35° | maps (checked on the sampled curve) |
| no turning while climbing | maps (a generator rule) |
| vertical Conveyor Lift | needs a new block |
| floor hole, free foundations, nudging | needs what Minecraft lacks, and is not needed |
| belts clip through belts and buildings | maps (current behaviour, keeps undergrounds out) |
| cost per length, rounded up | maps (current behaviour) |

## Recommendation for #362

**1. Constrain; do not quantise the belt.** Satisfactory keeps the free curve and quantises its
**control points**: ports, poles and mounts stand on a world grid at fixed facings and fixed heights.
It then refuses the geometry a real belt could not take. The fork already has the grid half, since
every loader and support is a block with a horizontal axis-aligned facing. What it lacks is the
refusal. Satisfactory's grid-shaped belt is an opt-in mode, added late (Straight in 1.0, Curve in
1.1): **snap as a generator, refuse as a limit.**

- Keep the free spline as the default.
- Refuse, in the placement plan, a turn tighter than about one belt width (Satisfactory's 2 m, so
  about one block), a segment longer than a cap (Satisfactory's 56 m is about 28 blocks), and a
  slope above a cap (Satisfactory's 35°).
- Add a Straight mode later as a generator. It lays axis runs between the same supports with 90°
  turns of a fixed radius, and an S where the two ends are parallel. It is what #360 and #361 want,
  and it can be offered without taking the spline away.

**2. A turn counts as its arc length.** Satisfactory charges no fixed amount per turn, and cost is
per metre rounded up. The fork's `ceil(length)` already is this rule, so it needs no change. If
Straight mode fixes the turn radius, each 90° turn has a known arc length and the cost of a Straight
belt can be read off the layout without being made a whole number by fiat.

**3. Slopes: flat turns, capped straight ramps, and a lift for the rest.** A Satisfactory belt turns
or climbs, never both, and never above 35°. Anything steeper is a vertical lift. For the fork:

- Refuse a segment whose sampled slope exceeds the cap.
- In a generated mode, turn only on the level at a support and climb only on straight runs.
- Leave vertical runs to a new lift block, to be ticketed separately. Until one exists, the slope cap
  is the vertical rule.

None of this touches crossing. Satisfactory belts pass over and through each other freely, which
keeps undergrounds `excluded` as `docs/factorio-mechanics.md` has them.

## Not sourced primarily

- That Straight and Curve refuse corners Default accepts, with "invalid belt shape": one Steam
  thread only.
- The Default mode's shape. The wiki says only "The default build mode."
- Whether Curve is a belt mode as well as a pipe mode. The wiki's belt page says so, and Coffee
  Stain's 1.1 note names it only under pipes.
- Coffee Stain's Q&A patch posts and `satisfactorygame.com/updates/*` could not be read directly.
  Their text is cited through the wiki's transcriptions, which link them.
