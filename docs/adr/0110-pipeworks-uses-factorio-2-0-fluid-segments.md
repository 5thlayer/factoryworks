---
status: accepted
---

# Pipeworks uses Factorio 2.0's fluid segments

A connected run of pipes, tanks and machine fluid ports is one fluid segment: a single fluid box
holding one fluid, its capacity the sum of its parts, and flow inside it instant. Throughput is
limited only by the pumps and by the endpoints that fill and drain the segment. This is Factorio 2.0's
model (ADR-0109).

**Considered.** Per-block pressure and flow, as in Factorio 1.1 and Oritech. Rejected: it is not the
Factorio the Pack ports, it makes throughput depend on run length in ways players have to tune
around, and it costs a tick update per pipe where a segment costs one per network.

**Consequences.** Mixing two fluids in one segment is refused at placement, as in Factorio. The
in-line Pump (#293) is what separates and caps segments, so it belongs to Pipeworks.
