---
status: superseded by ADR-0084
---

# Belts are bounded splines between grid-aligned supports

The fork's belt is a free spline from loader to loader through any supports, so a base can grow
into curves that cross at any angle and that nobody can read (#362). Factorio's answer is the tile:
straight runs, 90° turns, one belt per tile. Factorio is flat, though, and the pack's belts climb
over each other, which is why undergrounds are `excluded` (ADR-0044). Satisfactory has the pack's
problem in three dimensions and solves it another way: free curves, bounded, between points that
snap to a grid (`docs/research/satisfactory-conveyor-placement.md`).

**Decision.** A belt stays a spline, and its control points stay where they already are, on the
block grid and facing along an axis. Each **span**, the stretch between two consecutive supports or
ends, is bounded:

| bound | limit | Satisfactory's |
| --- | --- | --- |
| turn radius | at least 1 block | 2 m, one belt width |
| slope | at most 35° at the span's steepest point | 35° |
| turn and climb | a span that climbs is straight seen from above; any other span is level | "one, then the other" |
| reach | at most 32 blocks, two chunks, on the larger of the X and Z distance between the ends | 56 m |

The slope is read at the steepest point of the curve as drawn, not on the line between the ends. A
span's tangents are horizontal, so a climb is an S whose middle is at least twice as steep as the
line between its ends. A short span is steeper still, because upstream scales its tangents from the
curve's length, and a climbing curve is longer than its horizontal run. Between two loaders a rise
of one block therefore needs four blocks of run: over three, the line's slope doubled is 33.7° but
the curve reaches 35.4°.

A layout outside the bounds is **refused, never bent to fit**. Each bound has its own refusal
message. The rule is Minecraft-free geometry in the fork, applied where every belt is created, so
the preview, the click and the GameTest fixtures all go through it. That is ADR-0069's
plan-then-execute on the fork's side. Cost stays `ceil(length)`, and a turn costs its own arc length
(#346).

A **support is a belt end** as well as a waypoint (#366). It has one slot in and one slot out, and a
dead end backs up. Supports are free and placed only by the belt item, because Factorio's belt costs
its tiles and nothing else. Breaking any support a belt uses breaks that belt, which is not what
Satisfactory does, because a belt floating over a missing support reads as a bug.

**Considered: quantise to the grid, as Factorio does.** Rejected. The grid already holds every
control point, so only the curve between them is free, and bounding it is enough to make a layout
readable. Straight-and-corner paths would move the fork furthest from upstream, and slopes would
still need a rule Factorio does not have.

**Considered: bend an out-of-bounds layout to fit.** Deferred rather than rejected. Satisfactory's
Straight and Curve modes are an opt-in layer over the refusal rule (#365).

**Considered: a Conveyor Lift for vertical runs.** Rejected. Height is gained along a 35° climb or not
at all.

**Consequences.**

- The 64-block capacity fixture (#344) is a single span today and gains a support at its midpoint.
- Obstruction and crossing are still unchecked (#341), so a belt still crosses another by going over
  it, and ADR-0044's case for excluding undergrounds stands.
