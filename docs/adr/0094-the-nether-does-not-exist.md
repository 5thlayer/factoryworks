---
status: accepted
---

# The Nether does not exist

ADR-0007 kept the Nether and the End as dimensions and stripped GregTech's veins from them, so the
planetary progression would not have a free parallel endgame behind two portals. GregTech left with
ADR-0060, which took the whole premise away: there are no veins to strip. What was left was a portal
reachable from spawn, before any science pack, since Terra is `minecraft:overworld` (ADR-0019).

**Decision (#124, applied by #141).** The Nether is not a dimension of this pack. The world preset
`kubejs/data/minecraft/world_preset/normal.json`, which `scripts/build-terra-worldgen.py` writes,
declares `minecraft:overworld` and `minecraft:the_end` and nothing else. A preset lists its
dimensions whole, so a dimension it leaves out is not created in a new world.

Two rules come with it, both #124's:

- **No Factorio ingredient is satisfied through the Nether**, because there is nothing to satisfy it
  through. ADR-0021 cut gold and redstone as patches, and a portal would only be a different door to
  the same items. Nether quartz goes with them.
- **Terra's input alphabet is its four ores plus what its surface grows**, and the rule binds every
  pack-authored recipe, not only the converter's. This extends ADR-0021's patches-and-items line to
  dimensions. ADR-0017's "every new recipe is checked against this table" is the precedent for a rule
  that binds at the recipe.

The Nether's sky is a separate thing. The parked `kubejs/parked/data/planetaryfactory/dimension_type/ignus.json`
borrows it with `"effects": "minecraft:the_nether"`, a 1.21.1 field that 26.1's `dimension_type` no
longer has: a dimension's sky and fog are its `minecraft:visual/*` attributes. When #12 brings Ignus
back, its sky is restated in that form.

**Considered.**

- *Keep the Nether, as ADR-0007 did.* It rejected closing the portals as the most expensive option,
  for the blaze powder, netherite and ender pearls every recipe in the stack assumed. That cost was
  never real: the pack sweeps stock recipes and authors its own (ADR-0034), so the recipes that
  needed patching do not ship. Kept, the Nether would be a place the progression counts for nothing,
  reachable at rung 0.
- *Amend ADR-0007* instead of superseding it. Its argument is about GregTech's veins, which no longer
  exist, so an amendment would leave a record whose whole reasoning is dead.

**Consequences.**

- A world created before this keeps its Nether. The pack is pre-release, so no migration is written.
- Only the `normal` preset is the pack's. Vanilla's other presets still declare a Nether, and the
  world-preset tags only hide them from the create-world screen; a server's `level-type` can still
  name one.
- Vanilla mechanics that route through the Nether go with it: brewing's blaze-powder fuel, netherite
  gear, and the eyes of ender that open the End. None is authored around here.
- Circuits with no redstone are #119's question.
- **The End is still declared, and not decided here.** It is unreachable in practice: its portal needs
  eyes of ender, and no mob spawns to drop a pearl (ADR-0093). Nor does a stronghold generate to hold
  one: its biome tag is `#minecraft:is_overworld`, which names only vanilla biomes, not Terra's.
  Whether the End goes for the same reasons is a separate decision.
