---
status: accepted, in part superseded by ADR-0065
---

# The Crafting Plan queues on each press, with no Select Amount and no Start

ADR-0038 put three steps between EMI's **Fill Recipe** and the queue: Select Amount, the Crafting
Plan, and Start. That made the player commit to a count before seeing what it cost, and a single
craft took five clicks (#287).

**Decision.** **Fill Recipe** opens the Crafting Plan directly, for one craft, and moves the cursor
onto its **+1** button. The dialog has **+1**, **+5**, **all** and **Close**:

- Each count button **queues that many straight away**. The server resolves the plan again and takes
  the whole raw cost in the same step.
- A button is **greyed out when the inventory can't cover its count**. The limit is the resolver's
  `largestAffordable` (`CraftButtons`).
- The dialog **stays open**. The plan and the button states are resolved again after every press and
  on the queue's sync cadence. The first few queue rows are shown under the plan.
- Opening the dialog queues nothing, and **Close** leaves the queue running. Cancelling stays on the
  panel, which already lists the queue.

**What survives from ADR-0038.** The plan is still server truth, and the whole raw cost is still paid
at once. A queued plan is still never resolved again. Only what the dialog shows is recalculated.

**What goes.**

- **The typed amount field.** Repeated **+5** and **all** cover it.
- **The pending plan held between the dialog and Start.** With nothing standing between resolve and
  enqueue, there is no stale plan for a click to pay for.
- **The plan id Start quoted back.** It guarded exactly that stale plan, so it goes with it.

**Consequence.** The cursor move needs `MouseHandler.xpos` and `ypos` opened by an access
transformer. GLFW reports no motion for a programmatic cursor move, so a click made before the mouse
moves would otherwise land at the old position.
