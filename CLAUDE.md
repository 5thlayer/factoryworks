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
Factorio mechanic the pack does not reproduce belongs in the mechanic ledger instead (private,
see "Factorio corpus" below), which distinguishes `excluded` from `blocked`; a decision with a considered alternative belongs in an ADR.
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
`uv run tests/pack/test_licensing.py`.

Whether an asset renders is a human's check in game, never an agent's (ADR-0119).

### Which check to run

Every check, what it asserts and the defect it exists for is in `docs/testing/checks.md`; read a
check's section there before editing it or the code it guards. Run the matching check after:

| Edit | Check |
|---|---|
| any texture, model, animation, `.bbmodel` or store image or clip | `tests/pack/test_art_provenance.py` |
| `kubejs/server_scripts/starting_kit.js` | `tests/pack/test_starting_kit.py` |
| a committed ADR with `supersedes:` | `scripts/adr-backlink-check.sh` (needs `gh`) |
| a jar update | `tests/pack/test_jar_registry.py` |
| removing a third-party content mod's references, or any corpus path returning | `tests/pack/test_independence_guard.py` |
| Oritech's oil springs | `tests/pack/test_oritech_springs.py` |
| an item model | `tests/pack/test_data_formats.py` |
| `scripts/pack-check.sh` | `tests/pack/test_pack_check.py` |
| `scripts/sync-local-jars.py` | `tests/pack/test_sync_curseforge.py`, `tests/pack/test_local_jars.py` |
| a `.bbmodel`, `data/art/models/`, `build-model-assets.py` | `tests/pack/test_model_assets.py` |
| a new kind of file, third-party art, `REUSE.toml` | `tests/pack/test_licensing.py` |
| a lang entry, display name or quest | `tests/pack/test_licensing.py` (coined names) |

Generated output is never hand-edited: re-run its generator, whose `--check` its test runs.

### Factorio corpus

No public repository holds or reads Wube's data (ADR-0103, ADR-0126, #605). The corpus
(`data/factorio/`), its extractors and converters (`scripts/factorio-*`), the mechanic ledger,
`docs/research/` and `docs/spec/` are in the private repository `adamico/factoryworks-corpus`, and
`tests/pack/test_independence_guard.py` fails if any of them returns here, as it does for the item map and the three converter tables (`data/pack/{item-map,category-map,recipe-overrides,subgroup-owner}.json`). A ticket that needs
them is worked from that repository. Neither this file nor any Pack check points into it.

`docs/port/blocked-removals-26.1.2.md` lists every class the 26.1.2 port deleted, with the ticket
that restores it. Read it before concluding a mechanic was dropped.

### Pack manifest

The jar set is a packwiz manifest tracked in git (ADR-0024) — `pack.toml`, `index.toml` and one
`mods/*.pw.toml` per externally-sourced mod. `mods/*` is gitignored with `!mods/*.pw.toml` re-included;
never rewrite that as a bare `mods`, or the manifest silently stops being tracked. The local
Beltworks jar is an unmanaged hashed entry.
`scripts/pack-check.sh` asserts the installed jars still match. See `docs/pack/packwiz-workflow.md`.

### Local jar check

Beltworks is a **local jar**: `data/pack/local-jars.json` pins the version the Pack runs, and
`scripts/sync-local-jars.py beltworks=<version>` writes the pin, copies that jar out of `~/.m2` into
`mods/` and refreshes the manifest (#465, ADR-0024). A row with a `curseforge`
project id (Beltworks, Craftworks) also gets `mods/<mod>.pw.toml` naming that version's CurseForge
file, and the jar itself is not indexed, so an export references it rather than bundling it (#532).
The pin never waits on CurseForge: while CurseForge does not list the file, the sync installs the jar,
removes the row's metafile and prints `pending <mod> <version>`, and a later plain sync fills it in.
Adding a Library is a row and a sync.
`tests/pack/test_local_jars.py` runs
the sync's `--check`: the jar in `mods/` is the pinned one, byte for byte `~/.m2`'s when `~/.m2`
holds it; a newer version in `~/.m2` is named without failing. For a
`curseforge` row the metafile names the pinned file and project and hashes the installed jar, and
`index.toml` holds the metafile, not the jar; a row with no metafile is printed as pending and
passes. `--check --strict` fails on a pending row, and must pass before any export. The check
contacts nothing. Run it after
the sync or any change to `mods/`. Take a new Beltworks, Wireworks or Pipeworks with the sync, never by copying a jar.
Wireworks, the electric poles, is pinned the same way (#476). It has no CurseForge project yet, so
its jar is indexed by hash and negated in `.packwizignore`. Its Bindings are the Pack's:
`config/wireworks-server.toml` sets Factorio's supply areas and wire reaches; `kubejs/data/wireworks/tags/` puts the Picks in `wire_tools`
and the Pack's generators in their tag, held by `tests/pack/test_network_tags.py`.
Pipeworks, the pipes and the storage tank, and Fieldworks, the ore patches and oil wells, are pinned
the same way (#557, ADR-0110, #609): no CurseForge project, so indexed by hash and negated in
`.packwizignore`. `kubejs/data/pipeworks/tags/` lets the
Picks close a pipe's side (ADR-0125).
A change that crosses Groundworks, Beltworks, Wireworks, Pipeworks, Craftworks and the Pack goes through the
`release-train` skill (`skillworks:release-train`, from 5thlayer/skillworks), in that order: each
checkout is owned by the session working in it, and nothing is pushed without the user's word.
Beltworks and Wireworks require Groundworks rather than nesting it, so a Groundworks minor moves
their ranges together.

### First-party mod

`factoryworks_core` left this repo (ADR-0128, `docs/port/mod-audit.md`): its source is at commit
`fb05f50`, and each port ticket names the rows it takes from there. Fieldworks replaces its jar
(#609); the Pack's KubeJS still registers `factoryworks:` items until the Showcase moves out (#663).
