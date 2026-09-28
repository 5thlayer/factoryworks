---
status: accepted
supersedes: [8]
---

# Terra's day is Nauvis's seven minutes

Factorio's Nauvis turns once every 25,200 ticks, 7 minutes, and Minecraft's overworld every 20.
The difference is not cosmetic. A Solar Panel's night is what an Accumulator must carry, so the day's
length sets how many Accumulators a panel needs: Factorio's 0.84 on Nauvis, about 2.4 on a vanilla
day. A player who plans solar with Factorio's ratio runs dry every night.

#8 asked for each body's day at its Factorio length and wrote Terra's as "unchanged at vanilla 20
minutes", because in 1.21.1 no data could scale a day: a `dimension_type` could pin time and nothing
else. 26.1 moves the day into data. A dimension type names its `timelines`, and a timeline states
its `period_ticks` and keyframes every time-of-day value on it, the sky light that `isBrightOutside`
reads included.

**Decision.** Terra runs Nauvis's day: 8,400 Minecraft ticks. Terra's dimension type names timelines
of its own, and a generator writes them from vanilla's `day` and `moon` timelines out of the client
jar, every keyframe and time marker scaled by 8400/24000. Nothing in them is chosen by hand, so the
sky, its colour, the fog, the markers `/time set` uses and every behaviour keyed to the time of day
stay in step with each other, and a jar update arrives as a diff.

## Consequences

- #8's "Terra is unchanged" criterion is overridden. The other bodies' lengths stay #8's, each
  scaled from Nauvis's 7 minutes rather than from vanilla's 20.
- Every day-keyed vanilla behaviour runs 2.86 times as often: beds, bees, the moon's phases. None
  of it is a mechanic the pack reproduces, and no mob spawns on Terra (ADR-0093).
- The Solar Panel's output is read against the fraction of the day, so it is right at any period,
  and does not wait on this.
- The ledger's day-and-night row stops being a vanilla mechanic the pack inherits and becomes one it
  adapts.
