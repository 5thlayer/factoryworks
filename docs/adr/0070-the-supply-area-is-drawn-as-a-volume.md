---
status: accepted
---

# The supply area is drawn as a volume, in wireframe

A pole's supply area is invisible by construction -- that invisibility is the mechanic -- and #147
answered it with text: an item tooltip stating the footprint and a Jade line stating what is being
fed. Neither answers "where exactly does this area land on this terrain", which is a
placement-precision question the numbers cannot resolve, especially now that a pole's height no
longer moves its footprint (#147) and a base may sit below a roof, on a slope, or straddling a
ledge.

#158 brings that in as the third thing the **Placement Preview** shows, after ADR-0069's translucent
block and #298's wires.

## Decision

**The area is drawn as its volume, not as a surface.** The overlay is the box the area actually
occupies -- the tier's footprint by the vertical band -- anchored at the base. Minecraft is three
dimensional and the area is a three dimensional region; a 2D overlay has to answer "at what height"
for every column, and every answer is wrong somewhere.

**It is one axis-aligned box, drawn as edges only, with no fill.** The player is almost always
*inside* the volume: the band is base +/- 2 and a player stands on the same ground the pole does, so
their eyes sit inside the box for any pole they are next to. A filled box seen from inside is a
full-screen colour wash over exactly the machines being positioned, and an 18-wide substation seen
from outside is a translucent wall between the player and their own base. Vanilla draws the
structure block's bounding box as edges only, at the same scale, for the same reason.

**The edges draw through terrain.** The box's lower edges sit two blocks below the base, so on flat
ground they are inside the terrain; depth-tested, the box reads as an open-bottomed cage and the
ground extent -- the thing the overlay exists to show -- is the part that goes missing. Drawing
through terrain costs nothing extra: it is the same twelve segments and the same arithmetic, with
the depth test off.

**Bright yellow, one state, never changing.** Yellow is Factorio's own electric-network overlay and
is unused by every other pole gesture, which is what makes it available: green, orange, red and
brown are the Engineer's Pick's wire outcomes (ADR-0068) and white and red are ADR-0069's accepted
and refused. The box describes an *area*, not an outcome, so it does not turn red on a refused
placement -- ADR-0069's red block already says that -- and it will not react to network load when
#157 lands. Whether machines in the area are being fed is the Jade line's answer, on the machine.
This is #298's argument unchanged.

**The geometry is pure, and there is one of it.** The box is a function of the base position and the
tier -- `minOffset`, `maxOffset` and `verticalRadius`, which `SupplyArea` and `PoleTier` already
hold and already unit-test. One builder answers for the box and two call sites draw it: the held
item's preview and the placed pole's renderer. Neither reads the world for the geometry; the preview
reads one block, to ask whether the pole below is the same pole.

**An extension draws no box from the hand.** A pole extending a column joins a placed pole the
player is by definition looking at, whose own renderer is already drawing that exact box, so the
preview would only be submitting identical geometry twice a frame. The consequence that matters is
the one that made this a rule rather than an optimisation: a held pole must *not* walk down to
whatever column happens to be below it, because the aim does not have to land on a pole to end up
above one -- a snow layer or a plant on a column's top segment is replaceable, so a small pole aimed
at it stands on top of a medium column. Anchoring its box to that column's base would draw it two
blocks below where the pole would stand, which is the preview lying about the one thing the overlay
exists to show.

**Both triggers, but a placed pole shows its box only while looked at.** A held pole reuses
ADR-0069's target and cache, teaching before commitment. A placed pole already has a per-frame
client renderer -- the one drawing its wires -- so the placed case is an existing surface rather
than a new integration. Drawing every loaded pole's box would carpet a built base in overlapping
wireframes, which is the opposite of legible and is not what Factorio does; so it is the aimed pole
alone, and not the poles it is wired to. "Do my two poles cover the gap between them" is answered by
aiming at each in turn, and if it needs a better answer that is a ticket about network coverage, not
this overlay.

## Amended: the machines are outlined too

The box shipped and the human check this ADR asks for found it **unreadable in a built base**. The
diagnosis is that a player reading the box wants to know which machines are inside it, and no box
shape can tell them: membership depends on the vertical band, and the box has no face at a machine's
height. The limit recorded under Consequences below -- "the overlay says where the footprint lands,
not whether a given block is powered" -- was the problem rather than an acceptable cost, so it is
reversed here.

**Every block the pole reaches is outlined, in the same yellow.** The box still answers "where does
the footprint land" and the outlines answer "what is in it", which is the question that was actually
being asked of the box.

**The outlines are the pole's own scan, not a capability sweep.** `SupplyAreaScan` is extracted from
the pole's block entity so the server and the overlay run one piece of code, for the reason ADR-0069
gives about placement: a second implementation drifts, and the ways this one would drift are not
guessable. A slave Steam Engine resolves to its master, which may stand outside the area entirely,
and a machine's hull block answers its controller's face and is never a consumer in its own right
(#292). A naive "has an Energy capability" check gets both wrong and outlines blocks the network
never feeds -- the overlay lying again, only more precisely. An owner outside the box is outlined
where it stands, because that is the block the network draws.

**One colour for all three roles.** A consumer, a generator and an accumulator are all things this
pole is connected to; which is which is the Jade line's answer, on the machine, and a second colour
here would collide with the wire gestures' palette for no gain.

**Cached, not asked per frame.** The scan is a capability lookup per block and a substation's area is
1,620 of them -- a cost the pole's own scan already refuses to pay every tick, and a renderer runs
sixty times a second rather than twenty. The answer is kept for the pole's own rescan interval of two
seconds, for the pole's own reason: machines do not appear and vanish every tick. One entry is
enough, because only one box is ever drawn in a frame.

Rejected alongside it: **filling the box's top face**, which reads as the obvious fix and answers the
wrong question. It makes the extent easier to read and says nothing about which machines are in the
band, and the top face sits at base+3, a plane above the player's head.

## Considered options

**A 2D overlay draped per column**, resting on the highest block in each column, which is how #158
was originally written. It was rejected for what it cannot say. A column with nothing solid inside
the band gets no quad, so the sheet has a hole wherever terrain falls away -- and a hole reads as
"the area stops here", which is false: a platform built out over that drop is inside the band and
would be powered. That is the overlay misstating the pole's reach in precisely the straddled-ledge
case the ticket wants checked. Filling the hole at the band's floor fixes that but not the converse:
a column covering two levels at once, a machine on a platform and a floor below it, can only draw
the upper, leaving a powered floor invisible. Both failures are the surface model's, not the
lookup's, and the volume has neither.

**A heightmap for the per-column lookup**, had the sheet survived. Worth recording because it is the
obvious implementation and it is wrong: a heightmap is per chunk column and ignores the pole's own y,
so a pole in a mineshaft would draw its area on the mountain above it.

**A Jade probe-style outline on the placed block**, raised as #147's Q8 and deferred to #158 rather
than decided. It is folded in here as redundant: a placed pole showing its box while looked at is
the same fact on the same gesture, and a probe outline would be a second rendering of it.

## Consequences

ADR-0036's rationale for the vertical band said the band is shallow "so the area still reads as a
footprint on the ground". The *sizing* argument survives untouched and is the better half of it --
two blocks covers a machine on the pole's floor, one sunk into it and one on a platform above,
without powering the floor below through the ceiling -- but the footprint clause is retired here,
along with the same sentence in `PoleTier.VERTICAL_RADIUS`'s javadoc. The number does not change.

The overlay says where the footprint lands, not whether a given block is powered. It carries no new
arithmetic: the box's bounds are `SupplyArea`'s and `PoleTier`'s, both already unit-tested including
the substation's half-block offset, and the base walk is `PoleColumn`'s. So #158 warrants no new
check, which is a recorded decision rather than an omission: the only claim left is "looks right",
and that is the human check on delivery.

The one exception is a single assertion folded into `SupplyAreaTest` -- that the bounds enclose
exactly what `covers` admits and `forEachOffset` walks. It is there because the bounds are a second
statement of the region and the overlay is the only thing that reads them: lose an offset and the
box understates the pole's reach, enclose one it does not cover and the box overstates it, and
neither shows anywhere else. No new test *file* was added, and the box's numbers are not restated --
they are `PoleTier`'s and are asserted there.
