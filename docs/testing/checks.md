# Checks

What each check asserts, why, and the defect it exists for. `CLAUDE.md` indexes them by when to run each.

## Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree and surface data are internally consistent
— features, loot tables, blockstates, textures and lang against what is actually registered, plus
which marshland carries which tree and that no stromatolite drops ore — with no game launch. Run it
after any edit to the trees, the stromatolites or the five biomes. The worldgen half is read from
`kubejs/parked/` while Sapros is parked (ADR-0060).

## Recipe name check

The corpus holds no Wube text (ADR-0103, #303), so a chemistry or oil-processing recipe is named from what
`data/factorio/recipe.json` holds (#490). Factorio names a recipe after its main product unless it
is not one product under its own name -- advanced oil processing has three results, heavy oil
cracking one that is not its id -- and then it names the recipe itself. `scripts/build-recipe-names.py`
writes a `recipe.factoryworks.<type>.<name>` key for every emitted chemistry and oil
processing recipe: for such a recipe its id read as words (`heavy-oil-cracking` is "Heavy oil
cracking"), and `%s` for every other, which is filled with the product.
`tests/pack/test_recipe_names.py` runs the `--check` and re-derives both halves from the corpus,
holding the keys to the emitted recipes both ways. Run it after any converter run.

## Building tag check

What the player breaks at full Reach (16) rather than vanilla's 4.5 is the
`factoryworks:buildings` block tag (#413), hand-owned data since #599. `tests/pack/test_building_tag.py`
names the families the rule exists for and refuses anything a player digs up close.
Run them after re-extracting the corpus or editing the item map.

## Felling check

A tree is one entity holding an amount, and one gesture takes it whole (ADR-0051).
`tests/factorio/test_tree_extract.py` re-derives the rate from the corpus and names the three
prototypes the discriminant must exclude, each of which yields a different plausible-looking wrong
number. `tests/factorio/test_pack_recipes.py` carries the `fellable` tag: a tag whose JSON is
missing resolves to an empty tag rather than an error, and every tree silently stops felling.
Re-run `scripts/factorio-tree-extract.py` after a dump refresh. Whether a tree falls in a running
game is a world load.

## Starting kit check

The Showcase's starting kit is a KubeJS script, `kubejs/server_scripts/starting_kit.js`, that gives
each player a few of the suite's items once, on first join, marked by a stage that survives death
(ADR-0127). `tests/pack/test_starting_kit.py` is a static data check, no game launch: every id the
script grants is vanilla's or resolves to an item definition in the installed jar of a suite mod,
and a count is positive. An id that names nothing is a silent empty slot. Run it after editing the
script. Whether the kit arrives, and arrives once, is a world load.

## Fuel table check

What a burner furnace burns is generated datapack JSON, not Forge's burn table (ADR-0047).
`scripts/factorio-fuel-convert.py` joins `data/factorio/fuel.json` onto `data/pack/item-map.json`
into `kubejs/data/factoryworks/fuel/`, and nothing is decided in the script: a fuel with no
item-map row, an `undecided` one or a fluid is a *recorded skip*, printed with its reason.
`tests/factorio/test_fuel_convert.py` asserts every decided fuel has a row and nothing else does,
that `uranium-fuel-cell` fails on category as well as on its row, that coal's row still buys 888
whole ticks at the Stone Furnace's own 4,500 J/t, and that `wood` arrives as the tag
`minecraft:logs`. Run it after re-extracting the corpus or editing the item map. Whether a furnace burns a log in a running game is a world
load. See `docs/testing/fuel-table-check.md`.

## Hand recipe check

The Personal Assembler is Craftworks, a local jar (ADR-0089), and its rules are tested in its repo.
It plans only a `craftworks:assembling` recipe whose `hand_craftable` is true, with no fluid and one
result, and the recipes the Assemblers hold are the same recipes (ADR-0118): the converter writes the
flag for a first Factorio category of `crafting`, as do the stock re-authoring, and the two Pick
recipes are written with it. `config/craftworks-server.toml` names no Lock source, so every recipe
is unlocked from the start (ADR-0126). `tests/factorio/test_hand_recipes.py`
re-derives the set from the corpus, holds every emitted recipe to it, and asserts the
`factoryworks:hand/*` copies, their generator and `withHandCopies` are gone.

`tests/factorio/test_hand_resolver.py` is the corpus half: all 113 category-`crafting` recipes
resolve to plans bottoming out in the 21 known leaves, no item has two hand recipes (the resolver
picks a route with no cost model), and there are no cycles. It reads `data/factorio/recipe.json` and
fails the day a regeneration adds a recipe nothing hand-makes.

## Terra spawning check

Terra spawns no vanilla mob on its own (#480, ADR-0093). `tests/worldgen/test_terra_spawning.py`
runs `scripts/build-terra-worldgen.py --check`, then asserts every biome the live dimension and world
preset name has empty spawner lists and that the noise settings' `disable_mob_generation` is on.
Whether a night on Terra passes with no mob is a human check on delivery. Run it after editing the
generator.

## Starting-area geometry check

`tests/worldgen/test_start_geometry.py` asserts Terra's starting area can actually deal all four
ore fields: every hub connector sits on the face it points out of, and no two fields overlap each
other or the hub, for every hub variant against every combination of size variants. Vanilla drops
an overlapping jigsaw child silently, so this failure ships as "three patches instead of four" on
some seeds and nothing in a log. Run it after any edit to `scripts/build-terra-start.py`; it runs
the generator's `--check` and reads the generated `.nbt` files.

It also holds the water pool at every hub: a few blocks from the centre, where the spawn point is,
one block deep, its columns cleared above, and clear of every connector and every field (ADR-0050).

It is the only check standing behind the opening, and it cannot see the opening being *absent*:
the pools, the processor list and the hub's jigsaw names are referenced from
`TerraStartingArea` by string, with no compiler or test relationship to the datapack. #313 shipped
with those five files parked, which reached a new world as no hub, no water and no patches, and
one `No template pool` line at server start. No check was added for it (#313's own decision); the
symptom is a new world.

## ADR back-links

An ADR that contradicts a closed ticket's stated answer declares it as `supersedes: [55, 62]` in
frontmatter, and each named ticket gets a comment containing the literal `ADR-00NN`. Tickets are the
route and the ADRs are the state; without the back-link a closed ticket keeps asserting an answer an
ADR has overridden. Run `scripts/adr-backlink-check.sh` after committing an ADR that declares the
key — it needs an authenticated `gh`, so it is not part of any offline check. See
`docs/agents/domain.md`.

## Item-map ticket check

An `undecided` item-map row is a recorded skip only while the ticket it names is open, and a
`blocked_by` only while its blocker is (#278). A closed one leaves the converter skipping the row
for good behind a pointer that looks live. `scripts/item-map-ticket-check.sh` fails every row whose
`ticket` or `blocked_by` names a closed or missing issue, with that issue's title. When a ticket
closes, each row naming it is rewritten to a target, made `not_emitted` or `native_mechanic`, or
pointed at a new open ticket -- never at the reopened old one. The same command checks
`docs/factorio-mechanics.md` (#379): a `planned` or `blocked` section must name at least one open
issue in its `ticket` field, which is prose keeping closed refs as history; `owner` and the inline
sub-rule verdicts are not read. A failing section is re-verdicted or pointed at a new open ticket
the same way. It needs an authenticated `gh`, so run it after closing a ticket or editing either
file; it is in no batch.

## Obtainable index check

EMI's index lists only **Obtainable** items and fluids, as an allowlist (#173, ADR-0088).
`scripts/jar-registry-extract.py` writes to `data/jars/` every item and fluid id the client jar and
`mods/` register, every block loot table reduced to its entries and conditions, and
every placed and configured feature reduced to the block states it places and the features it names.
The pack's own jar is excluded. Its `--check` re-extracts and diffs when the jars are on disk, so a
jar update arrives as a diff to review. `scripts/build-obtainable-index.py` reads only committed
files and writes `kubejs/assets/emi/index/stacks/obtainable.json`: a `filters` entry matching every
id, then `added` naming every emitted recipe's output, every starting-kit item, every row of
`data/pack/mechanic-obtainable.json` and every drop of a block the live worldgen places. EMI reads
the file only under the `emi` namespace, applies `filters` before `added`, and skips an `added`
entry that is a bare string, so each is a `{"stack": ...}` object. A stack with components is
listed by its `componentChanges`, since EMI hides a variant not listed. A mechanic row is
`{id, mechanic, why, owner}`, for what a mechanic produces with no recipe and no data source, and
its `owner` is the ADR that makes it permanent.

The worldgen walk (#454) starts at each dimension under `kubejs/data/`, never `kubejs/parked/`: the
noise settings' default block and fluid and its surface rule, each biome's features followed from
placed to configured and on through the features they name, and the palettes of the live template
pools' templates, which are the starting area's. A feature type whose blocks are not all in its
config is in the generator's `IMPLICIT`, and a walked type in neither it nor `DATA_DRIVEN` fails
the run. Drops resolve to a fixpoint: a `match_tool` condition passes only when an Obtainable item
satisfies it, and any other tool predicate, silk touch included, is satisfied by nothing. So a grass
block drops dirt and not itself. A loot table under `kubejs/data/` replaces the jar's: every plant
the live worldgen places, leaves included, has an empty one, and gravel drops no flint (ADR-0092).
Each drop's block and loot table are written to
`kubejs/assets/factoryworks/obtainable/sources.json` for EMI's Where it is found (ADR-0091).

`tests/pack/test_obtainable_index.py` runs both `--check`s, holds every listed stack to an id the
corpus or the pack registers, and fails a mechanic row naming nothing, naming no ADR, or one the derivation already
covers. It holds the drops to #454's named ids and to terrain and logs alone, so a plant new to the
live worldgen fails until its loot table is replaced. It holds the loot rule to three cases, and
asserts that no block only a parked body places is a source. No mob drop is derived, since no mob spawns (ADR-0093). Run it after a
jar update, a converter run, an edit to the live worldgen, or an edit to the kit or the mechanic
list. Whether EMI shows exactly the allowlist is a human check: F3+T on a running client.

`data/pack/creative-listed.json` (ADR-0105, #539) adds the Pack's own creative test items, `{id, why}`
and `factoryworks:` only, to the emitted index and to nothing else: they are not Obtainable, so no
derivation reads them and `test_obtainable_index.py` fails a recipe that takes one, a row naming no
registered item, and a row already Obtainable.

## Independence guard

The Pack depends on no third-party content mod (ADR-0109), and `tests/pack/test_independence_guard.py`
is the ratchet that holds the removal slices of #566 to it. For each namespace in
`data/pack/independence-baseline.json`'s `forbidden` list (`railcraft`, `oritech`, `ftbmaterials`,
`researchd`, `portingdeadlibs`) it counts references across shipped data and compares the count to
that file's `baseline`. Static; no game launch. `researchd` and `portingdeadlibs` stay forbidden
though both mods are gone (ADR-0126), so neither grows back; Craftworks' generated comment in
`config/craftworks-server.toml` still names its `researchd` Lock source.

The count is the sum of three things. Each `<ns>:` occurrence in a text file under `kubejs/` (not
`kubejs/parked/`, which is never loaded), `config/`, `data/pack/*.json`
(not the baseline file) and `mods/*.pw.toml`, and in `index.toml`. Each of those files whose path,
lowercased with `-` and `_` removed, contains the namespace, so `mods/ftb-materials.pw.toml` and
`config/oritech-common.toml` count once each. Each `index.toml` `file = "..."` line whose path matches
the same way. `data/jars/` is an extract of the installed jars, not shipped data, and is never read. Only
tracked files count, since the game writes untracked client configs that would make the count
differ between checkouts.

A count above its baseline fails: a new reference to a mod the Pack is leaving. A count below it
fails too, naming the number to lower the baseline to, so a slice that removes references records
the gain in the same commit and nothing can later grow back into the headroom. Run it after
removing a third-party content mod's references, or adding anything that names one.

## Blockbench model check

`tests/pack/test_model_assets.py` runs `scripts/build-model-assets.py --check`, which regenerates every
model and texture from `data/art/models/*/*.bbmodel` and compares byte for byte (ADR-0112). It exists
because nobody exports from Blockbench: a hand-exported or hand-edited model is stale here. The same
run refuses what the exporter cannot see:

- a Blockbench format other than 5.2.1's "5.0", a model format other than `java_block`, or a Java
  block version other than 1.21.11;
- an embedded texture or a saved reference image, since whatever a `.bbmodel` saves ships
  (ADR-0103);
- an absolute path, a path outside `data/art/models/`, or a file or folder name the game cannot
  load, and a model not named after its folder;
- a cube off the 1/16 grid, inflated, beyond -16..32, or rotated off one axis's 22.5° steps up to 45°;
- a model path that exists but carries no `credit` naming this generator, so a hand-made model is
  never overwritten; a generated model whose `.bbmodel` is gone; and a stray file in a generated
  texture folder.

ADR-0111's art rules, 16 px per block and a status light on every model, are checked by eye on
delivery, not here. The test exports the committed template as a machine, asserting the three status
children name the kit's lamps, and breaks each rule once on a copy to prove the generator names it. The template is held
to the rules but never exported. A whole texture folder left by a deleted machine is not caught. Run it
after editing a `.bbmodel`, anything under `data/art/models/`, or the generator.

## Radar check

`tests/factorio/test_machine_extract.py` holds the Radar's `radars` row (#368, ADR-0079) against
the dump when it is on disk and re-derives 33.3 s per sector.

## Oritech spring check

Crude is infinite and the oil well is its only source (#377, ADR-0081).
`tests/pack/test_oritech_springs.py` asserts Oritech's two `oil_spring` biome modifiers are
overridden with a no-op -- NeoForge 26.1 has `none` for structure modifiers only.

## Enemy corpus check

`tests/factorio/test_enemy_extract.py` holds the eighth extractor's output — the units,
nests, turrets, walls, map-settings coefficients and per-entity emission rates ADR-0055 is
argued in. It **re-derives** rather than trusts, the way `test_resource_extract.py` does:
the evolution factor is stepped through Factorio's own published update and checked against
the closed form of the same differential equation, so a hand-edited `time_factor` fails here
and nowhere in a running game; a nest's absorption is compared against the Boiler's own
emission rather than against a literal; and each unit's `damage_per_shot` is recomputed from
its `damages` and its `damage_modifier`. Four prototypes are the walk's controls, each of
which yields a different plausible-looking wrong number: a premature wriggler's
`source_effects` hold a *negative* damage the attacker pays itself, a small spitter's damage
is 1 in a `stream` prototype and 12 in the game, a laser turret's is in a `beam` prototype
and reads as none if the reference is not followed, and a gun turret genuinely has none
because a magazine decides it — and the magazines are extracted too, so the turrets that
state no damage still have one in the corpus. Run it after re-running `scripts/factorio-enemy-extract.py`.
Nothing consumes this corpus yet; ADR-0055's arithmetic is filed against later tickets.

## Factorio mechanic ledger

`docs/factorio-mechanics.md` is the tracked list of every Factorio mechanic — base game and Space
Age — and what the pack does about it: one of `planned`, `shipped`, `adapted`, `blocked`,
`excluded`, never `undecided`. Read it before deciding a mechanic is out of scope, and update the
rows a ticket touches; a mechanic dropped without a row is exactly the failure it exists to catch.
It is not derived from `data/pack/subgroup-owner.json` and does not derive it — `not_emitted` there
is never evidence for `excluded` here — and it places nothing on a progression ladder, which is
#25's call. Row keys are Factorio's names by declared exception (ADR-0028).

## Recipe conversion

`scripts/factorio-recipe-convert.py` turns the extracted corpus into `craftworks:assembling` (ADR-0118) and `factoryworks:smelting` recipe JSON under `kubejs/data/factoryworks/recipe/` (#279), reading five committed data files: the corpus, the category
map, the subgroup owners, `data/pack/item-map.json` and `data/pack/recipe-overrides.json`. Nothing is
decided in the script — a decision is a diff to a design document. Generated output is never
hand-edited; re-run the converter. A Factorio name with no item-map row is a hard failure, while an
`undecided` row is a recorded skip. So is a row carrying `blocked_by`, the ticket that makes its
target loadable, such as a machine #277 has not chosen, and a machine
whose `recipe_type` is still null (the Centrifuge and the Rocket Silo). `--awaited` prints
those deferred recipes by the id they will load under, which is how the duplication check tells a
deferral from a typo.
`tests/factorio/test_recipe_convert.py` is the static check and runs the converter's `--check`. See
`docs/testing/recipe-conversion-check.md`.

## 26.1 data-format check

`tests/pack/test_data_formats.py` is the check kind #273 exists to add. Every generator here has a
`--check` that re-runs the generator and diffs its own output — self-consistent by construction and
blind to a shape Minecraft rejects — and every asset-hop check walks blockstate to model to texture
without asking whether the game reads any of them. This one asserts the shape **the game parses**,
against no generator, over every live tree at once, so a format landing in a subtree nobody thought
about still fails here. It holds four shapes: that every `models/item/X.json` has an
`assets/<ns>/items/X.json` pointing at it (26.1 resolves an item's model through that definition,
and a missing one is the black-and-magenta missing model in inventory, hand and EMI with nothing in
any log — the pack shipped the port with fifteen item models and zero definitions), that no
definition is an orphan, that every ingredient in every live recipe is a string rather than 1.21.1's
object, and that no namespace holds a pre-1.21.2 plural directory (`loot_tables/`, `tags/items/`),
which the game does not walk at all. `kubejs:oil_refinery` is a recorded deferral, not a silent skip:
it is dead with ADR-0060 and re-derived against the chassis #486 builds. A sized ingredient
(`{"ingredient": ..., "count": n}`) is read through to the string inside it.

`scripts/build-item-definitions.py` is the definitions' single owner — one generator rather than a
line in each asset generator, because a definition is not a decision about the Boiler or the rig but
the same three fields mechanically derived from the model beside it, and half the pack's item models
are hand-written with no generator to add the line to. An item wanting a tint, a range dispatch or a
condition stops being this script's and becomes its subject generator's. Run its `--check` (the test
does) after adding any item model. Whether the emitted files actually load is a datapack-load run
with zero `Couldn't parse data file` lines.

## Pack check restore

`tests/pack/test_pack_check.py` runs `scripts/pack-check.sh` in a scratch repo with a stand-in
`packwiz` whose refresh rewrites the manifest and adds a metafile (#625). A failed check must leave
the manifest exactly as it was before the run: unstaged, staged and untracked edits a sync left are
kept, the staged diff is unchanged, and the metafile the refresh wrote is gone. It exists because
the restore once ran `git checkout` over `mods/`, wiping a sync that had not been committed.
It also asserts that a jar `data/pack/local-jars.json` pins is not STRAY without a metafile, since
the sync leaves a pending CurseForge reference that way, while an unpinned jar still is.
Run it after editing `scripts/pack-check.sh`.

## Sync CurseForge references

`tests/pack/test_sync_curseforge.py` runs `scripts/sync-local-jars.py` over a scratch `~/.m2` and
`mods/`, with stand-ins for CurseForge's listing and `packwiz`. A listed file gets its
metafile. An unlisted or unreachable one still pins and installs the jar, removes the older
metafile and is reported pending. Plain `--check` passes on a pending row and `--check --strict`
fails. A later plain sync fills the reference in, and a metafile that already names the pin is not
queried again. It exists so the Pack can take and test a Library released to `~/.m2` before the jar
is uploaded, without ever exporting an older CurseForge file than its pin. Run it after editing
`scripts/sync-local-jars.py`.

## Emitted smelt shape check

`tests/factorio/test_smelting_shape.py` asserts the four emitted `factoryworks:smelting`
recipes are shaped the way **26.1** parses an ingredient: a string, `#`-prefixed for a tag, where
1.21.1 took `{"item": ...}`. The old shape does not crash — it is one `Couldn't parse data file`
line at datapack load and the recipe is then absent from the manager, which reaches a player as a
furnace that holds the item, holds power and never smelts. It shipped that way through the port and
cost #266's in-world check. `test_recipe_convert.py` could not see it: it runs the converter's
`--check`, which re-runs the converter and compares the output to what the converter would emit —
self-consistent by construction and blind to a shape Minecraft rejects. The assembling recipes'
shape is `test_data_formats.py`'s. Run it after any
converter change. A KubeJS reload is enough to see the fix in a running game — no restart.

## Stock-recipe sweep

`kubejs/server_scripts/recipes.js` removes every recipe the pack does not admit by name, and
`recipe_survivors.js` is the allowlist it negates (ADR-0034: a stock recipe ships only if a
decision names it and names the surface it is crafted on). A survivor is a *surface*, not a
recipe, and its filter's recipe type must be the one `data/pack/category-map.json` registers for
that machine — so a machine landing later without a survivor entry fails
`tests/factorio/test_recipe_sweep.py` rather than having its recipes swept in silence. Run that
check after editing either script, the category map or the emitted recipes; whether the sweep
removed the right things in a running game is a world load, not a static check.

## Recipe duplication check

`tests/factorio/test_recipe_duplication.py` asserts no item is made by two emitted recipes unless
`MULTI_ROUTE` names it and says what the second route earns. Every other recipe check owns one
subtree and one input table, which is the right shape for "did this converter do its job" and blind
to the question none of them can ask: whether two converters, or one converter twice, made the same
item. Two routes to one block fails no schema, appears in no log and loads perfectly — it reaches
the player as two EMI entries for the same thing, and if both are `hand_craftable` the
Personal Assembler's resolver has no cost model to choose between them. It shipped once, when
Create's two gearbox conversions and the large cogwheel's second route were emitted alongside the
direct recipes they duplicate and every subtree-local check passed. One item legitimately has a
second route: solid fuel, which Factorio makes from each of its three oils, and that is a row
with its reason, all three of whose routes are `factoryworks:chemistry` recipes (#488). The file-path
invariant it used to hold existed because GregTech re-registered every GTRecipe under its type's
path (#87); the pack's own types are re-registered by nothing, and the rule left with GregTech (#279).

Run it after any converter change. It does not assert the routes are balanced; costing is a
decision.

## Hand-written recipe check

`kubejs/data/factoryworks/recipe/assembling/pack/` is the one subtree no converter generates: ADR-0039's
two Engineer's Pick recipes, which the corpus can never author because Factorio has no mining-tool
prototype. `tests/factorio/test_pack_recipes.py` is what holds them, since every other recipe here
is checked against the corpus and these are checked against nothing otherwise — that the
converter still lists `pack` as foreign, which its own check reads from it rather than restating (a
run that forgets deletes them, and the sweep leaves no stock pickaxe to fall back on), that both land on a surface
`recipe_survivors.js` admits and carry `category: crafting` and `hand_craftable` so the Personal Assembler
plans them at rung 0, that the steel recipe consumes the iron pick, and that each Pick
has its model, texture, lang key, `c:tools/wrench` and `groundworks:dismantles`, the two tags that
carry its verbs, that `groundworks:dismantles` holds nothing else (#448). Both sprites are vanilla's own — the Iron Pick's `iron_pickaxe` and
the Steel Pick's `netherite_pickaxe` (#241, applied on #323). The Steel Pick used to wear GTCEu's
Damascus Steel pickaxe, flattened by a generator because GT's tool art is three greyscale layers
that only become a material under a colour handler our item never reaches; GregTech left with
ADR-0060 and took the source with it, so `scripts/build-pick-textures.py` and its `--check` are
gone rather than restated. The two Picks are named in the check. Whether the Pick mines every block
class is a world load. See
`docs/testing/hand-written-recipe-check.md`.

## Stock recipe re-authoring check

A stock recipe the pack keeps is re-authored, never admitted as shipped (ADR-0034's tail).
`scripts/stock-recipe-convert.py` reads each recipe `data/pack/stock-admissions.json` admits out of
the installed jar, flattens a shaped pattern with every count kept, swaps each ingredient through
`data/pack/stock-substitutions.json` and writes a hand recipe under
`kubejs/data/factoryworks/recipe/assembling/stock/`. An ingredient in neither table, a table row
nothing reads, and a recipe no jar or two jars ship each fail the line (#442). An admission can
carry a `rewrite` instead of being flattened: its ingredients, all `keep` rows, and its yield,
chosen and recorded with a reason, and only the output is read from the jar (#444).
An `author` row is a recipe no jar ships, written whole from its row on the machine it names;
sand is ground on an Assembler and smelted to glass under `recipe/smelting/stock/` (#445).
`tests/factorio/test_stock_recipes.py` runs the `--check`, holds each ingredient to an item another
pack recipe makes or a `keep` row, each output to an item a jar defines, each recipe to the
machine's input slots, and the union of every emitted hand recipe to no cycle; a second hand route
is `test_recipe_duplication.py`'s. It also holds the wooden stairs to one per species Terra's biomes
grow, read out of the biome files, and the subtree to no wall. Run it after editing either file or
after a jar update. Whether the filter appears in EMI with a route to follow is a human check on
delivery.

## Item map check

`tests/pack/test_item_map.py` holds ADR-0109: the Pack owns every material form, and a tech
mod supplies machines. It asserts every `data/pack/item-map.json` target resolves against the
installed jars (the pack's own via its lang and KubeJS's `event.create`, vanilla via the client jar
when present), that no row names a mod ADR-0060 removed, that the eight material-form rows are
authored `factoryworks:` items, and that no emitted recipe or item tag names a `c:` tag more than one installed jar
populates -- with AlmostUnified gone, `#c:ingots/steel` accepts three items and is not a decision.
A row whose target cannot resolve yet sits in `DEFERRED` with the ticket that owns it, and must
carry `blocked_by` with that ticket so the converter emits nothing naming it; a stale entry
fails, so delete one as its row resolves. Run it after editing the item map or re-running a converter.

## Factorio tech tree

The Showcase has no research (ADR-0126), but Terra's arc is measured against Factorio's tech tree,
extracted rather than transcribed (ADR-0022). `data/factorio/technology.json` and
`science_packs.json` are the committed reference. Regeneration and provenance are in
`data/factorio/README.md`. `tests/factorio/test_tech_extract.py` asserts the pruned tree is still a
valid tree: the extractor drops the infinite, formula-costed and upgrade technologies, re-points a
dropped node's children to its nearest surviving ancestors, and collapses generated reverse-crafts,
and the committed tree has no duplicate, dangling or self-referential prerequisite and every
technology is reachable from a root. It also reads the gate table in
`docs/spec/terra-progression.md` and holds each gate to its cost in the corpus, the launch to no
production pack and the reactor to a branch the silo does not require (ADR-0097). Run it after
re-extracting or after editing that table.

## Licence check

Which licence covers which file is `REUSE.toml`'s, with the texts in `LICENSES/` (#302,
ADR-0102): code LGPL-3.0-only, the Pack's content CC BY 4.0, Wube's corpus under neither, and
third-party files under their own. No file carries an SPDX header. `tests/pack/test_licensing.py`
implements `reuse lint`'s rule, since that tool needs libmagic and CI runs it, and holds the
boundaries a glob edit can silently move: the corpus never under the Pack's licences, each art
credit in `NOTICE` resolving to the licence `NOTICE` names, and `.packwizignore` leaving the licence
texts in the upload. Run it after adding a file of a new kind, any third-party art, or an edit to
`REUSE.toml`.

## Art provenance check

`data/pack/art-provenance.json` has one row per shipped texture, model and animation, saying whose it
is (#564, ADR-0122): `drawn`; `vendored`, with its `source` and `licence`; `stand-in`, with a
`subkind` of `placeholder`, `procgen` or `ai` and its `generator`; or `unknown`, for what no
`REUSE.toml` entry, `NOTICE` credit or generator has yet classified. `tests/pack/test_art_provenance.py`
holds the manifest to the files. What counts as shipped is `scripts/art_provenance.py`'s `SHIPPED`: every
file under a `textures/`, `models/` or `sounds/` folder of `kubejs/assets/*/` (so each
`.png.mcmeta` animation and each model JSON), the images and `.bbmodel`
sources under `data/art/`, and the images and clips under `publish/`. Blockstates, item
definitions and lang only point at art and have no row. The check fails on:

- a shipped asset with no row, including one not yet `git add`ed;
- a row whose file is missing, or is not a shipped asset;
- a malformed row: a kind outside the four, a field the kind does not take, a `vendored` row without
  `source` and `licence`, a `stand-in` row without a `subkind` and `generator`, a `procgen` or
  `placeholder` generator that is not a file in the repo (an `ai` one names the model), a path that is
  duplicated or out of order.

It also runs those rules on made-up rows, so a rule that stopped firing fails here instead of passing
vacuously. It does not compare a row to `REUSE.toml` or `NOTICE`: a `vendored` row's `licence` is the
row's own claim, and `test_licensing.py` still holds the credits.

The manifest is kept by hand, one row per line, sorted by path, and its `note` is free text. A row's
kind is the decision, so changing it is a diff a reviewer reads. Add the row in the commit that adds the
file; a file nobody has looked at is `unknown`. When a vendored asset is replaced, its row goes with
it (ADR-0122: `vendored` stays only to list what is left to replace).

`scripts/art-worklist.py` prints what is left: the stand-ins by subkind and generator, every vendored
row by licence with the non-commercial ones first, and the `unknown` rows, after a count line for all
four kinds. It exits 1 after printing if the manifest and the files disagree. Run the check after
adding, renaming or deleting any texture, model, animation, store image or clip, or a `.bbmodel`.

## Coined-name check

No string a player reads names a coined Factorio term (#304, ADR-0103). `COINED_TERMS` in
`tests/pack/test_licensing.py` is a recorded deny-list, term to reason, matched as whole words,
case-insensitive, plural included; a term that matches nothing is the passing state, so nothing
there goes stale. It scans every value of every shipped lang file (`kubejs/assets/*/lang/` and the
mod's), skipping keys that start with `_`; the literal argument of each `.displayName(...)` in
`kubejs/startup_scripts/`, with comments ignored. Registry ids and lang keys are exempt, and
`kubejs/parked/` and `publish/` are out of scope. A source that yields no strings fails, so a
scanner that stops matching cannot pass by finding nothing. Run it after adding a lang entry, a
display name or a quest.

