---
status: accepted
---

# A Dismantle takes a span of a tagged family

#404 made the Dismantle a belt-only gesture, living in the SimpleBelts fork because it walks the
fork's transport line. Oritech's fluid pipes are laid in the same long runs and were still taken up
one block at a time (#431). A second pipe-only gesture would have been the second of many, one per
block kind, so the gesture is made generic instead.

**Decision.** A Dismantle takes up a span of one **Dismantle Family** in two sneak-clicks of the
Engineer's Pick.

- **A family is a block tag,** `planetaryfactory:dismantle/<family>`, plus an optional join rule
  registered in code. With no rule, two members are joined when they touch face to face. A family
  is chosen over one shared tag because a shared tag joins a pipe to the wall it touches, and over
  "same block as the start" because a pipe run is three Oritech blocks: the plain pipe, the
  connection block and the framed duct.
- **The span is the shortest path** through joined members of the start's family. Two equally
  short paths refuse the end rather than pick one. A run with no branches was rejected: clearing a
  trunk with tees would take one click pair per segment. A flood of everything joined was rejected
  too, because one click could take a base's whole plumbing. The preview shows which path a plan
  takes.
- **The first family is `dismantle/pipes`,** joined only where Oritech's connection between two
  pipes is open, which is what Oritech's own network follows.
- **The gesture belongs to `planetaryfactory:dismantles`,** holding both Picks. On a pipe it takes
  over Oritech's sneak-wrench pickup, which breaks one pipe; one pipe is now two sneak-clicks on it,
  or plain mining. The plain click still toggles a connection.
- **The rule lives in `planetaryfactory_core`,** its span Minecraft-free.

**Belts join later.** Belts are to become a family too, their line-following a join rule, rather
than stay a separate gesture. That waits for the belt fork to settle, so for now two
implementations sit side by side. The fork's own Dismantle keeps the belt tag
`belts:dismantles_belts` and its own start on the held stack. Each start is live only when aimed at
its own family. The migration has two constraints:

- The belt join rule must be **directed**, with the path taken along the flow either way. Undirected
  adjacency ties at opposite points of a belt ring, and the tie rule would refuse it.
- The two stored starts merge into one.

**Consequences.**

- Oritech's pipes hold no fluid (2.0.0-exp6: the interface entity keeps only target caches), so
  taking pipes up voids nothing and leaves the rest of the network nothing to keep.
- A new family is a tag and, if face adjacency is wrong for it, a join rule. Nothing else changes.
- The Deconstruction planner row in `docs/factorio-mechanics.md` stays `unargued`.

## Amended by Beltworks ADR 0011

Belts join as a family, but not through a join rule. The family Dismantle and Beltworks' belt
Dismantle both move into the Groundworks library (formerly placementpreview), and each family owns
its whole span rather than a join rule inside one shared search. The pipe family keeps this ADR's
shortest path with ties refused, now as a helper in Groundworks. The belt family keeps Beltworks'
line scan, which takes the flow either way, so the directed-rule constraint above no longer applies.
The two starts merge into one, as foreseen: one tool tag, `groundworks:dismantles`, and one stored
start and queue. A pass may take up belts and pipes together. `FamilyDismantle`, its Takeover and
`BeltClaim` leave the Pack, which supplies only the pipe family.
