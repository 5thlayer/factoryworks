# Can a Blockbench export be held to its `.bbmodel` headlessly?

**Answer in one line: not by re-running Blockbench. It has no CLI, its Electron binary refuses to
run as Node, and the GeckoLib plugin only patches Blockbench's own Bedrock codec, so it cannot run
on its own. A Python check that compares structure is the realistic option. It parses the
`.bbmodel` and asserts that the export carries the same bones, pivots, cubes, texture size,
identifier, animations and keyframe times, and that the export settings below are pinned. Two
plugin defaults must be pinned in the template, because GeckoLib 5.5.2 reads their output wrongly
and gives no error.**

Ticket #570, part of map #567. The sources read were these:

- Blockbench 5.2.1's own `app.asar`, unpacked to `~/minecraft_mods/blockbench-5.2.1-asar/`. The
  renderer is the minified `dist/bundle.js`, so the identifiers quoted below are minified names.
- The installed plugin at `~/Library/Application Support/Blockbench/plugins/geckolib.js`, which is
  version 4.2.5. That is also the newest version in `JannisX11/blockbench-plugins` `plugins.json`.
- `mods/geckolib-neoforge-26.1.2-5.5.2.jar`, read with `javap`.
- The Blockbench wiki, the upstream issue trackers, and vanilla 26.1.2 source at
  `~/minecraft_mods/mc-26.1.2.109-src`.

## 1. The `.bbmodel` format: parseable, not specified

- **It is JSON with a version number.** `meta.format_version` is written from the constant
  `S6="5.0"`, alongside `model_format` and `box_uv` (`bundle.js`, the bbmodel codec's `compile`).
  When Blockbench loads an older file it migrates it in steps. The migrations branch on `< "3.2"`,
  `< "4.5"`, `< "4.10"` and `< "5.0"`. A file newer than `S6` is refused with
  `message.newer_project_format_version`.
- **There is no official specification.** The Blockbench wiki page on the format
  (<https://www.blockbench.net/wiki/docs/bbmodel>) says this outright. It calls the format mainly
  internal, warns that it "may be subject to breaking changes", lists only the breaking changes, and
  points to the source code as the specification.
  - **5.0 (October 2025)** split `groups` out of `outliner`.
  - **5.0 also changed the sign of keyframe values.** Position X, and rotation X and Y, used to be
    inverted and no longer are.
  - **4.10** moved relative texture paths.
  - **4.9** moved UV size onto each texture.
- **For Python this is fine, within limits.** A reader can take `elements`, `groups`, `outliner`,
  `textures`, `animations` and `resolution` by key. It should assert `meta.format_version == "5.0"`
  and fail if the version is anything else, rather than guess at a file a future Blockbench writes.
  The structural check below needs only that much. Reproducing Blockbench's export maths is a
  different job; §3 covers it.
- **Two things get into the source that must not be committed:**
  - **Machine paths from the plugin.** Its project property `geckolib_filepath_cache` holds the
    absolute paths of the last model and animation exports. Its setting "Remember file export
    locations" defaults to `true`, and the property is not marked `export: false`. Blockbench
    therefore copies it into the `.bbmodel` on every save. The loop that does this is
    `for (a in ModelProject.properties) …export!=!1&&…copy(Project,e)`. The result is
    `/Users/<name>/…` paths in a committed file.
  - **Texture paths, depending on a setting.** Blockbench's `export_asset_paths` setting defaults to
    `relative`, but `absolute` or `both` write absolute texture paths.

  The check should reject both.

## 2. A headless or scriptable export does not exist today

- **No CLI.** `electron/main.js` reads only `--userData <dir>` from `process.argv`, which picks the
  profile directory. The renderer opens `process.argv.last()` as a file to load. No export path is
  driven by arguments. Upstream's request for a headless conversion CLI,
  [JannisX11/blockbench#1764](https://github.com/JannisX11/blockbench/issues/1764), names exactly
  `.bbmodel → .geo.json / .animation.json`. It has been open since February 2023. The maintainer's
  only answer points to a third-party converter (below).
- **No Node mode.** The bundled Electron is 43.4.0. Its fuse wire in `Electron Framework` reads
  `001100011`, and fuse 0 is RunAsNode, so RunAsNode is **off**.
  - Running `ELECTRON_RUN_AS_NODE=1 …/Blockbench -e …` launched the full GUI app. It ran its update
    check and was then killed.
  - `NodeOptions` and `NodeCliInspectArguments` are on.
  - `OnlyLoadAppFromAsar` and `EmbeddedAsarIntegrityValidation` are off.
- **The renderer cannot run without Electron.** `dist/bundle.js` is the desktop build. It sets
  `isApp:!0` and requires `electron`, `@electron/remote` and `node:fs` at the top. It cannot be
  loaded as a plain page in headless Chrome. Running the web build headless would mean building
  Blockbench from source (GPL-3.0, a Node toolchain this machine does not have) and driving it with
  Playwright. That is a CI project in its own right.
- **Electron could be scripted, but not headlessly.** It can be started with Chromium's
  `--remote-debugging-port` and a scratch `--userData`, then driven over CDP to open a file and call
  the codecs' `compile()`. That still has three problems:
  - **It opens a window.** macOS has no headless Electron, and an agent session has no display.
  - **It collides with the user's own Blockbench.** `main.js` takes `requestSingleInstanceLock()`,
    so a second launch quits and passes its arguments to the Blockbench the user already has open.
  - **It needs the plugin installed** into that profile.

  This is possible as a manual tool. It is not possible as a check in
  `uv run --with pytest pytest tests/`.

## 3. How the GeckoLib plugin exports, and whether it can be reproduced

- **The plugin is a set of monkeypatches, not a codec.** Version 4.2.5 (MIT, `min_version` 5.0.0,
  `max_version` 6.0.0) patches the following:
  - **`Blockbench.export`** (`monkeypatchBlockbenchExport`), only to choose file names and to cache
    export paths.
  - **The Bedrock model codec's compile event** (`onBedrockCompile`). It deletes
    `item_display_transforms` and rewrites some `format_version` values (see §4).
  - **`Animator.buildFile`** (`monkeypatchAnimatorBuildFile`). It keeps bezier keyframes as bezier
    instead of baking them, and stamps `geckolib_format_version: 2`.
  - **`Keyframe.prototype.compileBedrockKeyframe` and `getLerp`.** These handle GeckoLib's easing
    plus `easingArgs`, the `pre`/`post` and catmull-rom shapes, and Molang inversion on position X
    and rotation X/Y.

  The geometry itself comes from Blockbench's built-in `Codec("bedrock")` `compile`, through pivots,
  cube origins and sizes, UV, `inflate`, `mirror` and locators. **Neither half can run without
  Blockbench's object model:** `Project`, `Outliner`, `Group`, `Cube`, `Animator`, `Keyframe` and
  `settings`.
- **A Python re-implementation is possible, but it drifts.** There is prior art.
  `Bedrock-OSS/regolith-filters/blockbench_convert/blockbench_convert.js` is about 400 lines of Node
  ported from Blockbench's `bedrock.js` compile, and is the converter the maintainer pointed to.
  - It was last touched in July 2023, before 5.0.
  - It hard-codes `format_version: "1.12.0"`.
  - It predates the keyframe sign change.

  It is, in other words, already wrong for 5.x files. A Pack port would have to follow both
  Blockbench's codec and the plugin's patches across every upgrade.
  - **The output depends on settings.** Examples are the bake-bezier setting,
    `export_groups`, `java_export_pivots`, `credit`, and whether the display panel shows (§4).
  - **The output depends on formatting.** `autoStringify` and `oneLiner` decide the layout.

  A byte-for-byte check would fail whenever someone's Blockbench preferences differ.
- **What can be checked reliably** is semantic equality on the fields the game reads. Compare
  parsed JSON, never bytes.
  - **Geometry:**
    - The identifier, `geometry.<model_identifier>`.
    - `texture_width` and `texture_height`.
    - The set of bone names and each bone's parent.
    - Each bone's pivot and rotation. Bedrock negates X on export, and the check can derive that the
      same way.
    - The cube count per bone.
    - Each cube's size and its origin, or its `from`/`to`.
    - Whether each cube is box UV or per-face UV.
  - **Animations:**
    - The set of animation names.
    - `loop` and `animation_length`.
    - The bones and channels that carry keyframes.
    - The keyframe timestamps.
    - The easing names.

  These are plain functions of the `.bbmodel` fields. Comparing values, rather than recomputing
  Blockbench's formulas, catches what matters most, which is an export left behind after an edit.
  It can still miss a pure UV tweak or a value-only keyframe change unless values are compared too.
  - **Value comparison is fine where Blockbench's mapping is simple:** pivots, positions and
    keyframe vectors with the 5.0 sign rule.
  - **It is not worth it where the mapping is not:** box-UV unwrapping and Molang strings.
- **Weaker options that were rejected:**
  - **File modification times are meaningless in git**, because a checkout resets them.
  - **A hash of the source recorded in the export.** Blockbench records none. A Pack-side plugin
    could stamp one on the compile event, and GeckoLib's Gson ignores unknown root keys. But it
    hashes the project at export time, not the `.bbmodel` as saved. Exporting before saving, or
    saving after exporting, makes it disagree with no change in content. It also adds a third
    plugin to keep up.
- **Vanilla Java block and item models** come from the codec built into Blockbench. Its `compile`
  covers the following:
  - elements' `from` and `to`, with `inflate`;
  - `rotation`, either `angle`/`axis` or free `x`/`y`/`z` (from 1.21.11 on);
  - face `uv`, scaled by `16 / resolution`, with `rotation`, `cullface` and `tintindex`;
  - the `textures` map, `display`, `groups` (behind a setting), `credit` and `parent`.

  It is about 150 lines, carries no plugin patches, and needs no animation. A Python port that
  reproduces the parsed JSON exactly is realistic here, but it still depends on settings: `credit`,
  `export_groups` and `java_export_pivots`. The static path is "documented, not proven" on map
  #567, so use the same structural check as for GeckoLib. Port the codec only if static models
  multiply.

## 4. The installed plugin's output against GeckoLib 5.5.2

- **Geometry `format_version`.** GeckoLib's `ModelFormatVersion` enum knows `1.12.0`, `1.14.0`,
  `1.16.0`, `1.19.30` and `1.21.0`, and supports all five. In
  `GeckoLibGsonLoader.deserializeJsonGeometry`, an unknown version only logs a warning,
  `Unknown geo model format version: '{}'. This may not work correctly`, and the model loads anyway.
  Blockbench 5.2.1 picks the version in `tB()`:

  | Model | Version chosen |
  |---|---|
  | display panel on, and a display slot set | `1.21.110` |
  | non-box-UV cube with a rotated face | `1.21.0` |
  | a group with `bedrock_binding` | `1.16.0` |
  | anything else | `1.12.0` |

  The plugin's `onBedrockCompile` rewrites `1.14.0`, `1.21.0` and `1.21.20` to `1.12.0`. It does
  **not** rewrite `1.21.110`. The plugin turns on `Format.display_mode` for item-type models, or when
  its "always show display" setting is on. A GeckoLib item model with any display transform
  therefore exports `1.21.110`, and that warning is logged on every resource reload. It is harmless
  but noisy, and the plugin also strips `item_display_transforms`. The check can assert a version
  from the five above.
- **Animation `format_version`.** Blockbench writes the format version of Bedrock's animation file.
  GeckoLib 5.5.2's `ActorAnimations` reads the field but does not validate it.
- **Bezier keyframes play as linear in game, and GeckoLib reports nothing.** The plugin's setting
  "Bake in bezier keyframes" defaults to `false`. With it off, a bezier keyframe exports as
  `{vector, easing: "bezier", left, left_time, right, right_time}` (`geckolibGetArray`). GeckoLib
  5.5.2 does not know the word:
  - `EasingType` registers `linear`, `none`, `step`, `catmullrom` and the 30 `easein*`/`easeout*`
    names. None of them is `bezier`.
  - `EasingType.fromString` is `getOrDefault(name.toLowerCase(), LINEAR)`.
  - No class in the jar contains `bezier` or `left_time`.
  - Upstream `bernie-g/geckolib` main is the same. Its only mention of bezier is a doc link in
    `EasingType.java`.

  **Turn "Bake in bezier keyframes" on, or don't use bezier interpolation.** The check should reject
  `"easing": "bezier"` in a committed `.animation.json`.
- **`geckolib_format_version: 2`**, which the plugin stamps on animation files when it does not bake
  bezier, is read nowhere in the 5.5.2 jar. The string does not occur in it. It is inert.
- **The plugin's version range fits.** 4.2.5 declares Blockbench 5.0.0 to 6.0.0, and 5.2.1 is
  inside that range. It is also the newest version published.
- **The Java block or item model version must be pinned.** Blockbench's default setting
  `default_java_block_version: "latest"` resolves to `"26.3"` for a new Java block project. That
  enables `shade_direction_override`, a 26.3 field. The export also writes
  `"format_version": "<java_block_version>"` at the root. Vanilla 26.1.2's `CuboidModel.Deserializer`
  reads only the keys it knows, so the root field is ignored. A 26.3-only face property would also be
  dropped without any error. Blockbench's own option for the Pack's version is
  `"1.21.11": "1.21.11 - 26.2"`. Pin `java_block_version: "1.21.11"` in the template, and have the
  check assert it in both the `.bbmodel` and the export.

## Recommendations

1. **Write a structural `--check` in Python, not a reproduction.** Name it something like
   `scripts/check-bbmodel-exports.py --check`, with a `tests/pack/` wrapper. It should do all of the
   following:
   - pair each committed `.bbmodel` with its exports by `model_identifier` or path convention;
   - assert `meta.format_version == "5.0"`;
   - compare the fields listed in §3, as parsed JSON, failing with the field that drifted.

   Bones, cubes, pivots, animation names and timestamps catch the realistic failure, an edit left
   unexported. The cost is that value-only UV or Molang edits go unseen.
2. **Have the same check enforce the export settings the Pack relies on:**
   - no `"easing": "bezier"`;
   - a geo `format_version` from GeckoLib's five;
   - `java_block_version == "1.21.11"` for static models;
   - no absolute paths in the `.bbmodel`, meaning `geckolib_filepath_cache` and texture `path`.
3. **Pin the settings in the workflow doc, and in the template where Blockbench stores them per
   project:**
   - "Bake in bezier keyframes" on;
   - "Remember file export locations" off, or strip the cache before commit;
   - `export_asset_paths` set to `relative`;
   - the Java version set to `1.21.11`.
4. **Don't build a headless Blockbench now.** The CDP-driven Electron route and the web build under
   Playwright both work in principle. Both cost a window or a Node toolchain, and both would be the
   only check in the Pack that needs either. Revisit if upstream #1764 lands a CLI, or if models
   multiply to the point where a re-export pass is wanted more than a drift check.
5. **Port Blockbench's Java block codec to Python only if static models become common.** Its
   ~150 lines make it the one export where full reproduction is cheap.
