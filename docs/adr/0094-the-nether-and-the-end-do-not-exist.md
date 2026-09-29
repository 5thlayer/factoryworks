---
status: accepted
---

# The Nether and the End do not exist

ADR-0007 kept the Nether and the End as dimensions and stripped GregTech's veins from them, so the
planetary progression would not have a free parallel endgame behind two portals. GregTech left with
ADR-0060, which took the whole premise away: there are no veins to strip. When #124 was decided, a
Nether portal was reachable from spawn, before any science pack, since Terra is `minecraft:overworld`
(ADR-0019). Neither portal is reachable now:

- **The Nether's.** Nothing on Terra yields obsidian, lava, flint or fire, so no portal can be built
  or lit.
- **The End's.** It needs eyes of ender, and no mob spawns to drop a pearl (ADR-0093). No stronghold
  generates to hold the frame either: its biome tag is `#minecraft:is_overworld`, which names only
  vanilla biomes, not Terra's.

What was left was two dimensions nobody can enter, declared for nothing, and Factorio has neither.

**Decision (#124, applied by #141).** Terra is the pack's only vanilla dimension. The world preset
`kubejs/data/minecraft/world_preset/normal.json`, which `scripts/build-terra-worldgen.py` writes,
declares `minecraft:overworld` and nothing else. A preset lists its dimensions whole, so a dimension
it leaves out is not created in a new world. #124 decided the Nether; the End goes for the same
reasons, which #124 left open.

Two rules come with it, both #124's:

- **No Factorio ingredient is satisfied through another dimension**, because there is none to satisfy
  it through. ADR-0021 cut gold and redstone as patches, and a portal would only be a different door
  to the same items. Nether quartz goes with them.
- **Terra's input alphabet is its four ores plus what its surface grows**, and the rule binds every
  pack-authored recipe, not only the converter's. This extends ADR-0021's patches-and-items line to
  dimensions. ADR-0017's "every new recipe is checked against this table" is the precedent for a rule
  that binds at the recipe.

The Nether's sky is a separate thing. The parked `kubejs/parked/data/factoryworks/dimension_type/ignus.json`
borrows it with `"effects": "minecraft:the_nether"`, a 1.21.1 field that 26.1's `dimension_type` no
longer has: a dimension's sky and fog are its `minecraft:visual/*` attributes. When #12 brings Ignus
back, its sky is restated in that form.

**Considered.**

- *Keep both, as ADR-0007 did.* It rejected closing the portals as the most expensive option, for the
  blaze powder, netherite and ender pearls every recipe in the stack assumed. That cost was never
  real: the pack sweeps stock recipes and authors its own (ADR-0034), so the recipes that needed
  patching do not ship. Kept, each dimension would be a place the progression counts for nothing, and
  one a single obtainable portal ingredient would open.
- *Remove the Nether and leave the End*, as #124 did. The End is as unreachable as the Nether, so
  keeping it only leaves the question open.
- *Amend ADR-0007* instead of superseding it. Its argument is about GregTech's veins, which no longer
  exist, so an amendment would leave a record whose whole reasoning is dead.

**Consequences.**

- A world created before this keeps both dimensions. The pack is pre-release, so no migration is
  written.
- Only the `normal` preset is the pack's. Vanilla's other presets still declare both, and the
  world-preset tags only hide them from the create-world screen; a server's `level-type` can still
  name one.
- Vanilla mechanics that route through either go with them: brewing's blaze-powder fuel, netherite
  gear, the dragon, elytra and shulker boxes. None is authored around here.
- Circuits with no redstone are #119's question.
