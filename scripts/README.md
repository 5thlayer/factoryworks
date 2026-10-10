# `scripts/`

Build and extraction tooling for the pack. Nothing here decides anything — a
decision is a diff to a design document (an ADR, or a `data/` file). These
scripts only transform committed inputs into generated outputs, or assert that
what is installed still matches what is tracked.

Generated output is **never hand-edited**. Re-run the script.

Python scripts run under `uv` (`uv run scripts/<name>.py`). Most take `--check`
for a no-write CI mode; the matching `tests/` entry calls it.

---

The Factorio corpus, its extractors and converters live in a private repository (ADR-0103, #605).
The Pack's guard (`tests/pack/test_independence_guard.py`) fails if any of them returns.

## NBT

| Script | Produces |
|---|---|

## Other asset generators

| Script | Produces |
|---|---|
| `gen-flora-textures.py` | placeholder 16×16 flora sprites for Sapros's trees (stdlib, meant to be redrawn) |
| `build-filter-pack.sh` | rebuilds `kubejs/data/<name>.zip` from `packs/<name>/` (pack.mcmeta filter sections) |
| `jar-registry-extract.py` | the installed jars' item and fluid ids, block loot and features → `data/jars/` |

## Manifest + config integrity checks

| Script | Asserts |
|---|---|
| `pack-check.sh` | installed jars match the packwiz manifest (ADR-0024) |
| `config-orphans.py` | no `config/` entry belongs to an uninstalled mod |
| `adr-backlink-check.sh` | every ticket an ADR `supersedes:` carries an `ADR-00NN` comment (needs authenticated `gh`) |
| `art-worklist.py` | prints the stand-in, vendored and unknown art in `data/pack/art-provenance.json` (ADR-0122), and exits 1 if that manifest and the shipped art disagree; `art_provenance.py` is its library, shared with `tests/pack/test_art_provenance.py` |

## Launch + diagnostics

| Script | Does |
|---|---|
| `bootstrap.py` | a fresh machine: links the CurseForge instance, downloads the CurseForge jars, puts the local jars into `~/.m2` (`docs/pack/packwiz-workflow.md`) |
| `launch.py` | assembles and runs the CLI launch command for this instance (no `--help`; unknown args pass to the game) |
| `check-launch.sh` | greps the most recent launch log for mod-loading failures |
