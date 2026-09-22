---
status: accepted
supersedes: [41]
---

# The Rocket Silo is first-party, and the launch does not wait on Oritech: Space Age

ADR-0060 deferred interplanetary travel "until Oritech ships a first-party space addon", and #340
held the launch in the same wait. The idea was that the addon's Rocket Assembler and Rocket Pad might
become the pack's launch. #41's answer was a GTCEu multiblock silo launching a GCyR rocket after 50
Rocket Part cycles, and it lost its whole substrate with ADR-0060.

**Decision (#278, #378).** The Rocket Silo is a `planetaryfactory_core` footprint machine on the
pack's own seam, as the Assembling Machine, the Steam Engine and the Radar are (ADR-0072, ADR-0077,
ADR-0079). It makes Rocket Parts and launches. Neither waits on an Oritech release. The `rocket-silo`
and `rocket-part` item-map rows name `planetaryfactory:rocket_silo` and
`planetaryfactory:rocket_part`, `blocked_by` #378.

#340 keeps travel, platforms and cargo. The launch is Terra's goal (#25). Holding it on an upstream
jar with no release date would leave Terra's progression without an end. The silo is a single
machine with a corpus behind it, so building it is the same work the pack has already done three
times.

What a launch *does* is not decided here. There is no travel, no platform, and no satellite, so a
launch has no destination yet. #378 owns that, and whether Factorio's continuous part production
survives.

## Considered Options

- **Wait for Oritech: Space Age's launch half** (#340's decision of 2026-09-21). Its rocket is
  assembled from blocks, rendered rising from its pad, and watchable, as `docs/gdd.md` §4 asks.
  Rejected: it is unreleased, and its rocket is a construction toy with a flight planner, not a silo
  that consumes a production line's output at a Factorio rate.
- **Borrow the addon's launch later, and build nothing now.** Rejected for the same reason: Terra's
  ladder would end in a gap.

## Consequences

- If the addon ships, it may still carry travel on top of this silo's launch. That is #340's call,
  and it no longer includes the launch.
- ADR-0006's GCyR `RocketEntity` and ADR-0060's deferral no longer describe the launch.
