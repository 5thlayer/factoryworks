# How the licensing check treats a hand-made model and its Stand-in listing

Research for #571, part of the Blockbench map #567.

**Answer in one line: the licensing check is keyed on paths, not extensions, so a `.geo.json`,
`.animation.json`, JSON model or PNG under `kubejs/assets/**` or `mod/src/main/resources/assets/**`
already passes as the Pack's CC BY 4.0 work with no change. The `.bbmodel` source is the one file
that needs a decision: under `data/art/` or at a new top-level folder no annotation reaches it and
the check fails; under `mod/` outside `resources/assets/` it silently becomes LGPL code. No Stand-in
list exists yet. The glossary promises one, and #564 plans it as `data/art/provenance.json`, a
test-held row per shipped asset. A hand-made model enters as rows for its export and textures that
name the `.bbmodel` as their source. Coined-name rules reach no model file, but ADR-0103's asset
rule does: no Factorio sprite may sit in the `.bbmodel` as a reference image, because Blockbench
embeds reference images in the saved file.**

## 1. How the licensing check treats each new file kind

### The rule is a glob map, and extensions play no part

- `REUSE.toml` is the whole licence map. No file carries an SPDX header, and the last matching
  annotation wins, except for one marked `precedence = "override"` (`REUSE.toml:3-6`).
- `tests/pack/test_licensing.py` reimplements `reuse lint`'s rule because that tool needs libmagic.
  `_glob` (`:41-57`) converts REUSE globs, `_licence_of` (`:75-83`) picks the deciding annotation,
  and `_tracked` (`:67-72`) walks `git ls-files`. The check that matters here is
  `test_every_tracked_file_has_a_licence_and_a_holder` (`:240-243`). It fails when a tracked file
  matches no annotation, or matches one with no `SPDX-License-Identifier` or
  `SPDX-FileCopyrightText`. CI runs the real `reuse lint` as well
  (`.github/workflows/reuse.yml`, `fsfe/reuse-action@v6`).
- Neither the test nor `REUSE.toml` has an extension allowlist. "A new kind of file" in
  `CLAUDE.md:110` and in `docs/testing/checks.md:1041-1042` ("Run it after adding a file of a new
  kind") means a file at a path no glob covers yet. ADR-0102 states the intent: "A new file no
  annotation reaches fails `tests/pack/test_licensing.py`, so a new kind of file gets a licence
  when it is added rather than after" (`docs/adr/0102-…md:70-71`).

### What each candidate path resolves to today

These were resolved with the test's own `_licence_of` on the current `REUSE.toml`. GeckoLib 5
reads `assets/<ns>/geckolib/models/*.geo.json` and `assets/<ns>/geckolib/animations/*.animation.json`,
the layout in Oritech's jar (`~/minecraft_mods/oritech-2.0.0-exp6-jar/assets/oritech/geckolib/`).

| Path | Result |
|---|---|
| `kubejs/assets/factoryworks/geckolib/models/x.geo.json` (also `geckolib/animations/`, `models/block/x.json`, `textures/block/x.png`) | CC-BY-4.0, 2026 5thlayer, via `kubejs/assets/**` (`REUSE.toml:40`) |
| `mod/src/main/resources/assets/factoryworks/…` (same kinds) | CC-BY-4.0, 2026 5thlayer, via `mod/src/main/resources/assets/**` (`REUSE.toml:46`), which follows and so beats `mod/**` (`:12`) |
| `data/art/x.bbmodel`, `data/art/models/x.bbmodel`, a new `data/art/x.png` | **None: fails the test.** `data/art/` has no general rule; only named third-party files are mapped (`REUSE.toml:61-67`, `:94`) |
| `art/x.bbmodel`, `models/x.bbmodel` (new top-level folder) | **None: fails the test** |
| `mod/art/x.bbmodel`, `mod/src/main/bbmodel/x.bbmodel` | **LGPL-3.0-only, which is wrong and passes silently.** `mod/**` is the code rule (`REUSE.toml:12`), and ADR-0102 puts textures and assets under CC BY (`docs/adr/0102-…md:20-21`) |
| a new PNG under `textures/block/electric_furnace/`, `burner_mining_drill/`, `electric_mining_drill/` or `textures/block/ore/` | **Inherits a third-party holder and licence** (Futureazoo CC-BY-NC-SA, or Malcolm Riley) from a directory glob (`REUSE.toml:58`, `:90-92`). A hand-made texture must not be placed in those folders |

### What must change

1. **The exports and textures need nothing**, as long as they sit in `geckolib/`, `models/` or
   `textures/` subfolders under one of the two asset roots and not in the vendored-art folders
   listed above.
2. **The `.bbmodel` needs a home and a glob.** It is the Pack's own content, so CC BY 4.0, 5thlayer
   (ADR-0102 `:20-21`). Either add its folder to the second annotation (`REUSE.toml:35-52`), or
   rely on a parent glob that already reaches it. Two constraints apply:
   - `data/` is pack content that packwiz indexes. It is not in `.packwizignore`, whose comment
     reads "The index covers pack *content* only: … data/" (`.packwizignore:7-8`), and `index.toml`
     already lists 11 `data/art/` files. A `.bbmodel` under `data/art/` would therefore ship in the
     upload unless it is ignored there. That is harmless, but it is noise.
   - Put a source-art glob such as `data/art/models/**` or `data/art/**/*.bbmodel` in the general
     CC BY annotation, ahead of the third-party ones. A broad `data/art/**` there is safe too, since
     the Malcolm Riley and Futureazoo annotations come later and still win for their named files.
     Never let the source sit under `mod/` outside `resources/assets/`.
3. **No extension allowlist is needed or exists.** If the map wants to fail closed on a `.bbmodel`
   that lands somewhere it should not, that is a new assertion in the test, for example that every
   tracked `*.bbmodel` resolves to CC-BY-4.0 with 5thlayer as holder. It would be a new boundary
   test in the style of `test_the_packs_own_work_is_under_the_packs_licences` (`:281-295`), not a
   REUSE change.
4. **A `.bbmodel` embeds its textures.** In Blockbench 5.2.1, `embed_textures` defaults to `true`.
   On save, every texture's PNG goes into the file as a data URL (`source`, `internal: true`). The
   absolute `path` is dropped and only `relative_path` is kept under the default
   `export_asset_paths: "relative"` (`~/minecraft_mods/blockbench-5.2.1-asar/dist/bundle.js`,
   setting at offset ~6994983, bbmodel compile at ~8592993-8595400). Because the licence is mapped
   by the `.bbmodel`'s path, a source that embeds a vendored texture (Futureazoo's NC art, or the
   ArtOfTecharium CC BY-NC models ADR-0109 vendors) would carry that texture under the Pack's
   CC BY. A hand-made model should embed only Pack-drawn textures, or the source must take the
   vendored annotation as well. `minify_bbmodel` also defaults to `true` (same offset), which writes
   the source as a single JSON line and makes diffs useless. Which settings the template fixes is
   the workflow doc's question.

## 2. Where Stand-in art is listed today, and how a model enters

- **The definition** (CONTEXT.md, glossary "Shipping the pack", `CONTEXT.md:618-620` on `main`)
  reads: "A shipped texture, model or animation meant to be replaced: a placeholder, a procedurally
  generated sprite, or an AI-generated one. … Every stand-in is listed so it can be commissioned or
  redrawn." Vendored art follows it (`:622-624`). ADR-0109 adds stand-ins for FTB Materials' and
  Railcraft's sprites (`docs/adr/0109-…md:35`).
- **No list exists.** The repository has no file, table or test that enumerates stand-ins.
  `REUSE.toml` and `NOTICE` record licence and credit, not finality. `NOTICE` lists only
  third-party copies (`NOTICE:1-9`), and `test_notice_credits_resolve_to_the_licence_notice_names`
  (`test_licensing.py:303-320`) only holds `NOTICE` and `REUSE.toml` to each other. A stand-in
  today is marked only in prose:
  - `scripts/README.md:77` (`gen-flora-textures.py`: "placeholder … meant to be redrawn")
  - `scripts/gen-flora-textures.py:1`
  - `scripts/build-radar-assets.py:8` ("The model is a placeholder until #367")
  - `scripts/build-pumpjack-assets.py:10`
  - `scripts/build-pump-assets.py:45`
- **The planned list is #564**, which is open. It adds `data/art/provenance.json` with one row per
  shipped PNG "(and model/animation)" under `kubejs/assets/**` and
  `mod/src/main/resources/assets/**`, with these kinds:
  - `drawn`
  - `vendored` (source, licence)
  - `stand-in` (`placeholder` | `procgen` | `ai`, plus the generating script)
  - `unknown`

  A test fails on an asset with no row, or a row naming a missing file. A script prints the
  worklist. #565 (blocked by #564) classifies the `unknown` rows. #574, the Steam Engine proof,
  says it will "list it as Stand-in art".
- **How a hand-made model enters.** Under #564's schema, the shipped files are the rows:
  - the `.geo.json`
  - the `.animation.json`
  - each texture PNG
  - any JSON block/item model

  The `.bbmodel` is outside the manifest's two roots, so it is not a row of its own. Each row is
  `stand-in`, and it names the `.bbmodel` where a procgen row names its generating script. That
  makes the source the "how to regenerate" pointer and keeps "marked final" a one-field change, to
  `drawn`. #564's sub-kinds do not fit a hand-authored, not-yet-final model: it is not a
  placeholder cube, procgen or AI. #564 needs a fourth sub-kind (`hand`, or `blockout`), or #567
  must decide that `placeholder` covers it. Until #564 lands there is nowhere to list it, so #574
  either depends on #564 or records its rows in #564's first pass.

## 3. Coined-name and attribution rules that apply

- **The coined-name scan does not reach model files.** `COINED_TERMS` (`test_licensing.py:27-38`)
  scans lang values, FTB Quests text, `.displayName(...)` literals and research names
  (`_player_text`, `:220-225`; scope in `docs/testing/checks.md:1044-1060`). Bone names, animation
  names (`animation.steam_engine.run`), the `.bbmodel`'s `name` and `model_identifier`, and file
  names are not player-read strings. Like registry ids and lang keys, they fall under ADR-0103's
  exemption for functional names (`docs/adr/0103-…md:24-30`). The model's display name lives in
  lang, which the scan does cover.
- **The asset rule is the binding constraint, and no check holds it.** ADR-0103 rules: "The pack
  ships no Wube art, sound or game text, and no wiki text or image, not even as a placeholder"
  (`docs/adr/0103-…md:31-34`). It also notes that no check can see a Wube image, so "That rule is
  held by this ADR and by review" (`:48-50`). Blockbench saves a project's reference images inside
  the `.bbmodel`, converting a file-path PNG or JPG to a data URL (`ReferenceImage.getSaveCopy`;
  bbmodel compile `if(!i.backup&&i.reference_images!=!1)…e.reference_images=m`, `bundle.js`
  ~8596350). A Factorio sprite or wiki render used as a modelling reference would therefore be
  committed and shipped inside the source. The workflow must say to remove reference images before
  saving, or to keep them in a separate scratch project. #567's note "original geometry and
  textures" is the same rule.
- **Attribution.** A hand-made model is 5thlayer's and needs no `NOTICE` entry. ADR-0102's rule
  that third-party art takes a `NOTICE` entry and a `REUSE.toml` exception, held to each other by
  the test (`docs/adr/0102-…md:73-74`), applies only when a model reuses vendored geometry or
  textures, such as Oritech's CC0 assets or ArtOfTecharium's CC BY-NC models (ADR-0109 `:33-35`).
  The `NOTICE` cross-check regex matches only `.png` files under `kubejs/` or `data/`
  (`test_licensing.py:311`). A vendored `.geo.json` or `.bbmodel` credited in `NOTICE` would
  therefore not be cross-checked until that regex is widened.

## Sources

- `REUSE.toml`, `tests/pack/test_licensing.py`, `.github/workflows/reuse.yml`, `.packwizignore`,
  `index.toml`, `NOTICE`
- `CLAUDE.md:110-113`, `docs/testing/checks.md:1033-1060`
- `docs/adr/0102-code-is-lgpl-the-packs-content-is-cc-by-and-wubes-data-is-neither.md`,
  `docs/adr/0103-the-pack-ships-nothing-wube-owns-and-earns-nothing.md`,
  `docs/adr/0109-the-pack-depends-on-no-third-party-content-mod.md` (on `main`)
- `CONTEXT.md` § Shipping the pack (on `main`)
- Issues #564, #565, #567, #574
- Blockbench 5.2.1 bundle, `~/minecraft_mods/blockbench-5.2.1-asar/dist/bundle.js`
- Oritech's GeckoLib layout, `~/minecraft_mods/oritech-2.0.0-exp6-jar/assets/oritech/geckolib/`
