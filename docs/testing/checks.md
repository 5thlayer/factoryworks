# Checks

What each check asserts, why, and the defect it exists for. `CLAUDE.md` indexes them by when to run each.

## Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree and surface data are internally consistent
— features, loot tables, blockstates, textures and lang against what is actually registered, plus
which marshland carries which tree and that no stromatolite drops ore — with no game launch. Run it
after any edit to the trees, the stromatolites or the five biomes. The worldgen half is read from
`kubejs/parked/` while Sapros is parked (ADR-0060).

## Starting kit check

The Showcase's starting kit is a KubeJS script, `kubejs/server_scripts/starting_kit.js`, that gives
each player a few of the suite's items once, on first join, marked by a stage that survives death
(ADR-0127). `tests/pack/test_starting_kit.py` is a static data check, no game launch: every id the
script grants is vanilla's or resolves to an item definition in the installed jar of a suite mod,
and a count is positive. An id that names nothing is a silent empty slot. Run it after editing the
script. Whether the kit arrives, and arrives once, is a world load.

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
pointed at a new open ticket -- never at the reopened old one. It needs an authenticated `gh`, so run it
after closing a ticket or editing the item map; it is in no batch. The mechanic ledger's half of
this check left with the ledger (ADR-0126, #605).

## Jar registry check

`scripts/jar-registry-extract.py` writes to `data/jars/` every item and fluid id the client jar and
`mods/` register, every block loot table reduced to its entries and conditions, and every placed
and configured feature reduced to the block states it places and the features it names. The pack's
own jar is excluded. `tests/pack/test_jar_registry.py` runs its `--check`, which re-extracts and
diffs when the jars are on disk, so a jar update arrives as a diff to review. Run it after a jar
update. `test_item_map.py` reads the item and fluid ids.

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
the gain in the same commit and nothing can later grow back into the headroom. It also fails on any tracked file under `data/factorio/`, `docs/research/`, `docs/spec/`,
`docs/factorio-mechanics.md` or named `scripts/factorio-*`, with no exemption (ADR-0126, #605): the corpus
and what reads it live in a private repository. Run it after
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

## Oritech spring check

Crude is infinite and the oil well is its only source (#377, ADR-0081).
`tests/pack/test_oritech_springs.py` asserts Oritech's two `oil_spring` biome modifiers are
overridden with a no-op -- NeoForge 26.1 has `none` for structure modifiers only.

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

## Item map check

`tests/pack/test_item_map.py` holds ADR-0109: the Pack owns every material form, and a tech
mod supplies machines. It asserts every `data/pack/item-map.json` target resolves against the
installed jars (the pack's own via its lang and KubeJS's `event.create`, vanilla via the client jar
when present), that no row names a mod ADR-0060 removed, that the eight material-form rows are
authored `factoryworks:` items, and that no emitted recipe or item tag names a `c:` tag more than one installed jar
populates -- with AlmostUnified gone, `#c:ingots/steel` accepts three items and is not a decision.
A row whose target cannot resolve yet sits in `DEFERRED` with the ticket that owns it, and must
carry `blocked_by` with that ticket so the converter emits nothing naming it; a stale entry
fails, so delete one as its row resolves. Run it after editing the item map.

## Licence check

Which licence covers which file is `REUSE.toml`'s, with the texts in `LICENSES/` (#302,
ADR-0102): code LGPL-3.0-only, the Pack's content CC BY 4.0, and third-party files under their own. No file carries an SPDX header. `tests/pack/test_licensing.py`
implements `reuse lint`'s rule, since that tool needs libmagic and CI runs it, and holds the
boundary a glob edit can silently move: each art
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

