---
status: accepted
---

# Pack machine models are unanimated vanilla JSON and show status by texture

A model the Pack authors is a vanilla block model, made in Blockbench. Nothing in it moves: a
machine shows what it is doing only by swapping its textures, between three looks, idle, working and
problem. Problem covers every reason a Factorio machine stops (no power, low power, no input, output
full), since Factorio's alert icons already say which one. A working texture may be animated with a
`.mcmeta` frame strip.

The art follows modern tech mods' practice (#568):

- **16 px per block, fixed.** A bigger model gets a bigger sheet (32, 64 or 128 square), never a
  finer grid. Trim may stretch below 16; nothing goes above it.
- **Cubes on the 1/16 grid**, rotated only in vanilla's 22.5° steps.
- **Lights on their own layer**: lamps, fire and status lights are overlay elements drawn at full
  brightness, so the body stays lit by the world.
- **Factorio's mood, not its sprites** (ADR-0103): warm, desaturated industrial metal in a narrow,
  dark value range, with small saturated accents that carry meaning.
- **A shared parts kit**: fluid port, power connection, status light, hazard stripe, each looking
  the same on every machine.

**Considered.** GeckoLib keyframe animation, as Oritech's machines have: rejected, since animating is
beyond what the Pack can author today, and a GeckoLib machine renders every frame, a cost that grows
with a Factorio base's hundreds of machines. Baked JSON parts moved by a block-entity renderer, as
AE2 and Mekanism do: rejected for now, since motion would then be Java to write per machine. Either
can return for a single machine without changing the rest.

**Consequences.** GeckoLib stays in the Pack (ADR-0109) only for the vendored Oritech models, until
each is replaced. A vanilla model can reach only one block beyond its own on each side, which fits a
machine up to 3x3; a larger footprint needs a decision of its own when its model is made. Because
Blockbench's vanilla exporter is small, a check can re-export the `.bbmodel` and compare it exactly
(#570).
