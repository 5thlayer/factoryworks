## Agent skills

The skills are mattpocock/skills, a git submodule at `.agents/mattpocock-skills` pinned to a release
tag. Upstream nests them by category, and Claude Code reads only `.claude/skills/<name>/SKILL.md`, so
`.agents/skills/<name>` is a symlink into the submodule for each skill the repo uses, and
`.claude/skills` is a symlink to `.agents/skills`. A fresh clone needs `git submodule update --init`
or the links dangle; every session runs it from `.claude/hooks/session-start.sh`, which also resets
the submodule to the commit the checkout records. Take a new release by checking out its tag in the
submodule and committing it before the session restarts, clears or compacts, or the hook undoes the
checkout; a skill moved upstream breaks its link, so check that `.claude/skills/*/SKILL.md` all
resolve.

### Code review

Review code only with the mattpocock `code-review` skill (`.claude/skills/code-review/`), which
checks a diff against this repo's standards and the originating ticket. Never use Claude Code's
built-in `/code-review`, `/review`, `/security-review` or `/simplify` in its place, even though the
built-in `/code-review` shares its name.

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Rejected scope

`.out-of-scope/` holds one file per rejected enhancement, so a `wontfix` keeps its reasoning and a
repeat request is recognised rather than re-argued. `/triage` reads it while gathering context. Only
rejected enhancements go there — never bugs, never something already built, never a deferral. A
Factorio mechanic the pack does not reproduce belongs in `docs/factorio-mechanics.md` instead, which
distinguishes `excluded` from `blocked`; a decision with a considered alternative belongs in an ADR.
See `.out-of-scope/README.md`.

### Domain docs

Single-context — `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Testing policy

Which check a feature warrants — and whether it warrants one at all — is decided by the claim the
feature makes, not ad hoc per ticket. Six claims, six answers, and a content ticket names its check
kind explicitly so that "no check" is a recorded decision. See `docs/testing/what-to-check.md`.

### Code comments

This overrides "match the surrounding comment density". Much existing code is over-commented, so
do not copy it.

A comment explains why the code is not the obvious code: a trap, a constraint from outside the
code, or a choice a reader would otherwise undo. Write the minimum that explains it, then name the
ADR or ticket that holds the reasoning, e.g. `(ADR-0074)`. Code that is obvious gets no comment.

Keep these out of comments:

- History, such as which ticket built something, what it replaced, or what was tried before. It
  belongs in the commit and the ADR.
- Rejected alternatives. They belong in the ADR.
- How a test was verified, such as "dropping X turns this red". It belongs in the commit.
- A restatement of what the next lines do.

When you edit code, trim the comments you touch to this rule. Leave comments elsewhere alone.

### Running the Python checks

`uv run --with pytest pytest tests/` runs **every** check under `tests/`. Most files are
`main()`-style scripts; `tests/conftest.py` wraps each as one test asserting exit 0, and fails the
run if any `test_*.py` produced no tests (#171). A single script runs directly too:
`uv run tests/pack/test_rig_assets.py`.

Three checks are in no batch and this command does not reach them: the GameTest harness,
`scripts/check-datapack-load.py` and the upload check, `python3 -m unittest discover scripts/tests`.
Whether an asset renders is a human's check in game, never an agent's (ADR-0119).

### GameTest harness

`./gradlew :factoryworks_core:runGameTestServer` is the only check that loads a world: headless,
no human, fails the command on a failed test. Tests live in the **main** source set under
`mod/src/main/java/com/factoryworks/core/gametest/`, since the game loads them. The run selects
`--tests factoryworks:*` (one wildcard pattern, not a list); the showcase scenes are under
`factoryworks_showcase:*`. The pack's `kubejs/` and server configs are linked into `mod/run/` by
Gradle tasks, so tests assert against the data the pack ships. Run it after editing anything under
`core/energy/`, `core/smelting/`, `core/fluid/`, `core/oil/`, `core/placement/`, `core/reach/`,
`core/dismantle/`, `core/stretch/`, `core/worldgen/` or `core/gametest/`. How it is wired, and what
each test class holds, is in `docs/testing/checks.md` § GameTest harness.

### Which check to run

Every check, what it asserts and the defect it exists for is in `docs/testing/checks.md`; read a
check's section there before editing it or the code it guards. Run the matching check after:

| Edit | Check |
|---|---|
| Sapros trees, stromatolites, its five biomes | `tests/flora/test_flora_data.py` |
| any block, blockstate, model, texture, lang key, loot table | `tests/pack/test_block_assets.py` |
| any texture, model, animation, `.bbmodel` or store image or clip | `tests/pack/test_art_provenance.py` |
| furnace tiers, `core/smelting/` | `test_furnace_assets.py`, `test_smelting_type.py`, `:factoryworks_core:test`, GameTest |
| `core/wreck/`, the wreck generators | `tests/pack/test_wreck_assets.py`, GameTest |
| a converter run | `test_recipe_names.py`, `test_recipe_convert.py`, `test_smelting_shape.py`, `test_recipe_duplication.py`, `test_hand_recipes.py`, `check-datapack-load.py` |
| corpus re-extract or `data/pack/item-map.json` | `test_replace_groups.py`, `test_building_tag.py`, `test_item_map.py`, `test_fuel_convert.py`, `test_overload_limit.py` |
| `core/placement/` | GameTest (`PlacementPlanTests`), then `check-datapack-load.py` if the platform moved |
| tree felling, after a dump refresh | `factorio-tree-extract.py`, then `build-tree-assets.py`; `test_tree_extract.py`, `test_pack_recipes.py` |
| `core/start/` or the spec's Opening | `tests/pack/test_starting_kit.py`, `:factoryworks_core:test` |
| `scripts/build-terra-worldgen.py` | `tests/worldgen/test_terra_spawning.py`, GameTest (`WorldgenFixtureTests`) |
| `core/worldgen/VanillaSpawning` | GameTest (`SpawningRuleTests`) |
| `scripts/build-terra-start.py` | `tests/worldgen/test_start_geometry.py` |
| `core/ore/`, the ore generators, the resource extractor | `test_resource_extract.py`, `test_ore_assets.py`, `test_outfield_worldgen.py`, `:factoryworks_core:test`, GameTest |
| a committed ADR with `supersedes:` | `scripts/adr-backlink-check.sh` (needs `gh`) |
| closing a ticket, editing the item map or `docs/factorio-mechanics.md` | `scripts/item-map-ticket-check.sh` (needs `gh`) |
| a jar update, live worldgen, the kit, the mechanic or creative lists | `tests/pack/test_obtainable_index.py` |
| removing a third-party content mod's references | `tests/pack/test_independence_guard.py` |
| an item or fluid face | `tests/pack/test_transfer_guards.py`, `test_capability_registration.py`, `test_energy_faces.py` |
| `core/fluid/`, `build-pump-assets.py`, `build-steam-assets.py` | `test_pump_assets.py`, `test_boiler_assets.py`, `:factoryworks_core:test`, GameTest |
| a fluid row, an Oritech update | `tests/pack/test_fluid_tints.py` |
| `SteamEngineSpec` | `:factoryworks_core:test` |
| `core/radar/` or its generator | `test_radar_assets.py`, `test_machine_extract.py`, `:factoryworks_core:test`, GameTest |
| `core/oil/`, the oil field, its generators | `test_pumpjack_assets.py`, `test_resource_extract.py`, GameTest |
| `scripts/factorio-enemy-extract.py` | `tests/factorio/test_enemy_extract.py` |
| an item model | `tests/pack/test_data_formats.py` |
| `scripts/pack-check.sh` | `tests/pack/test_pack_check.py` |
| `scripts/upload.py`, `scripts/release.sh` | `python3 -m unittest discover scripts/tests` |
| `scripts/sync-local-jars.py` | `tests/pack/test_sync_curseforge.py`, `tests/pack/test_local_jars.py` |
| a `.bbmodel`, `data/art/models/`, `build-model-assets.py` | `tests/pack/test_model_assets.py` |
| any edit to `kubejs/`, the dev runtime classpath | `scripts/check-datapack-load.py` |
| a reload listener, recipe serializer or item codec | `tests/pack/test_load_codecs.py` |
| `recipes.js`, `recipe_survivors.js`, the category map | `tests/factorio/test_recipe_sweep.py` |
| the hand-written Pick recipes | `tests/factorio/test_pack_recipes.py` |
| `stock-admissions.json`, `stock-substitutions.json`, a jar update | `tests/factorio/test_stock_recipes.py` |
| the gate table in `docs/spec/terra-progression.md` | `tests/factorio/test_tech_extract.py` |
| a new kind of file, third-party art, `REUSE.toml` | `tests/pack/test_licensing.py` |
| a lang entry, display name or quest | `tests/pack/test_licensing.py` (coined names) |

Generated output is never hand-edited: re-run its generator, whose `--check` its test runs.

### Factorio mechanic ledger

`docs/factorio-mechanics.md` lists every Factorio mechanic and what the pack does about it
(`planned`, `shipped`, `adapted`, `blocked`, `excluded`). Read it before deciding a mechanic is out
of scope, and update the rows a ticket touches (ADR-0028).

`docs/port/blocked-removals-26.1.2.md` lists every class the 26.1.2 port deleted, with the ticket
that restores it. Read it before concluding a mechanic was dropped.

### Pack manifest

The jar set is a packwiz manifest tracked in git (ADR-0024) — `pack.toml`, `index.toml` and one
`mods/*.pw.toml` per externally-sourced mod. `mods/*` is gitignored with `!mods/*.pw.toml` re-included;
never rewrite that as a bare `mods`, or the manifest silently stops being tracked. The local
Beltworks jar is an unmanaged hashed entry and `factoryworks_core` is not indexed at all.
`scripts/pack-check.sh` asserts the installed jars still match. See `docs/pack/packwiz-workflow.md`.

### Local jar check

Beltworks is a **local jar**: `data/pack/local-jars.json` pins the version the Pack runs, and
`scripts/sync-local-jars.py beltworks=<version>` writes the pin, copies that jar out of `~/.m2` into
`mods/`, refreshes the manifest and rebuilds the core mod (#465, ADR-0024). A row with a `curseforge`
project id (Beltworks, Craftworks) also gets `mods/<mod>.pw.toml` naming that version's CurseForge
file, and the jar itself is not indexed, so an export references it rather than bundling it (#532).
The pin never waits on CurseForge: while CurseForge does not list the file, the sync installs the jar,
removes the row's metafile and prints `pending <mod> <version>`, and a later plain sync fills it in.
FactoryWorks Core's `mods/factoryworks-core.pw.toml` follows `gradle.properties`' `mod_version`, the
version `scripts/release.sh` last released, the same way.
The build reads the same
table and names no Library (#475, ADR-0090): every pinned jar, and every jar its row `nests`, is on
the compile classpath and the dev runs, and each nested artifact's range is read from the jarjar
metadata into `neoforge.mods.toml` as `<artifact>_version_range`. So the Pack names no Groundworks
version, and adding a Library is a row and a sync. `tests/pack/test_local_jars.py` runs
the sync's `--check`: the jar in `mods/` is the pinned one, byte for byte `~/.m2`'s when `~/.m2`
holds it, and nests Groundworks; a newer version in `~/.m2` is named without failing. For a
`curseforge` row the metafile names the pinned file and project and hashes the installed jar, and
`index.toml` holds the metafile, not the jar; a row with no metafile is printed as pending and
passes. `--check --strict` fails on a pending row, and must pass before any export. The check
contacts nothing. Run it after
the sync or any change to `mods/`. Take a new Beltworks, Wireworks or Pipeworks with the sync, never by copying a jar.
Wireworks, the electric poles, is pinned the same way (#476). It has no CurseForge project yet, so
its jar is indexed by hash and negated in `.packwizignore`. Its Bindings are the Pack's:
`config/wireworks-server.toml` sets Factorio's supply areas and wire reaches; `kubejs/data/wireworks/tags/` puts the Picks in `wire_tools`
and the Pack's generators in their tag, held by `tests/pack/test_network_tags.py`;
and `FactoryWorksCore` states the pole Replace group.
Pipeworks, the pipes and the storage tank, is pinned the same way (#557, ADR-0110): no CurseForge
project, so indexed by hash and negated in `.packwizignore`. Core registers crude oil, the
Pumpjack's anchor and parts are its `FluidPort`s, and the pipe Dismantle Family and drag-laying
(`core/dismantle/PipeFamily`, `core/stretch/PipeworksPipeLegs`) are Pack Bindings over what its
pipes' arms report, and `kubejs/data/pipeworks/tags/` lets the Picks close a pipe's side (ADR-0125).
A change that crosses Groundworks, Beltworks, Wireworks, Pipeworks, Craftworks and the Pack goes through the
`release-train` skill (`skillworks:release-train`, from 5thlayer/skillworks), in that order: each
checkout is owned by the session working in it, and nothing is pushed without the user's word.
Wireworks nests Groundworks too, so a Groundworks release moves Beltworks' and Wireworks' ranges
together.

`-PsiblingBuilds` is for trying such a change in the Pack before a library is released (#466, #475).
It includes, as a composite, the checkout of every row of `local-jars.json` and of each jar the row
nests, except a row marked `"sibling": false` (a row whose tested binary no checkout reproduces;
none is now), or of only the rows named (`-PsiblingBuilds=craftworks`), each at `-P<name>Dir`, default
`~/minecraft_mods/<name>`. The compile and every dev run, `runGameTestServer` included, use those
checkouts and never their `mods/` jars. The build prints one `siblingBuilds:` line per checkout,
naming its version and HEAD.
`installToPack` refuses under it, and `scripts/check-datapack-load.py --sibling-builds` forwards it.
It never installs, and a green run under it proves nothing about the pinned jars: the change still
ships through the release train. A Groundworks checkout outside the range Beltworks nests it under
fails the build, naming the range, so a Groundworks minor needs Beltworks' range moved in its
checkout too.

### First-party mod

`factoryworks_core` is a Gradle subproject in `mod/`, built from the repo root with
`./gradlew :factoryworks_core:installToPack` — required after a fresh clone, since the jar
lands in the gitignored `mods/`. It owns mechanism only; ADR-0015 has the ownership table for
what goes in the mod, in KubeJS and in datapack JSON. See `mod/README.md`.
A Core change a player will notice adds its line under `## Unreleased` in `publish/core/changelog.md`
as it lands. Before releasing, tagging or uploading Core, read `docs/agents/releases.md`.
