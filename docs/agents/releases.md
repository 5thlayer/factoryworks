# Releasing FactoryWorks Core

FactoryWorks Core (`mod/`, ADR-0101) is released from this checkout to the local maven repository (`~/.m2`), tagged, and uploaded to Modrinth and CurseForge. A release that has to reach or follow a Library goes through the `release-train` skill, where Core runs after the Libraries and before the Pack.

## The changelog

A Core change a player will notice adds its line under `## Unreleased` in `publish/core/changelog.md` in the commit that makes it. Write it in plain words, in the glossary's terms, one line per change, as the 0.1.1 entries are. A release ships what Unreleased lists, and its section becomes the upload's notes on both sites, so the changelog is written as the work lands and never reconstructed from commits. A change no player sees, such as a test, a refactor or tooling, adds no line.

## Cutting a release

`scripts/release.sh <version>`, from a clean `main`. Below 1.0 an addition or a fix is the next patch, and a breaking change the next minor. The script's header has the details. In short, it:

- refuses an empty Unreleased, a version already tagged or in `~/.m2`, and a dirty tree;
- sets `mod_version`, turns `## Unreleased` into `## <version>` and runs `:factoryworks_core:build`;
- commits `chore: release FactoryWorks Core <version>`, publishes to `~/.m2` and tags `core-v<version>` with the jar's sha256.

It pushes nothing and uploads nothing unless given `--upload`. An upload is public and permanent, so it waits on the user's word: run `scripts/upload.py <version>` once the user says so. Its header covers the tokens, `--site` and `--dry-run`.

After a release, a plain `scripts/sync-local-jars.py` moves `mods/factoryworks-core.pw.toml` to the new CurseForge file once CurseForge lists it. Until then the Core reference is pending, and `--check --strict`, the gate before any export, fails.

## A published version is final

A version in `~/.m2`, on Modrinth or on CurseForge never changes. A fix is the next patch.

## Tags

`git tag -l 'core-v*' -n9` lists the releases. `core-v0.1.1` points at a commit outside `main`'s history, because `main` was rewritten after it was tagged. `main`'s own `481286d9` has the same tree, so count 0.1.1's successors from there.
