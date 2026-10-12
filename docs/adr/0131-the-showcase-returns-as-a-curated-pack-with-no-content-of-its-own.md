---
status: accepted
---

# The Showcase returns as a curated pack with no content of its own

ADR-0128's amendment (#663) retired the Showcase: with no content of its own, a pack only curated
jars and configs each mod already decides. But the suite still needs one download that puts every
Module in a world together, beside the third-party mods a player would run them with. A pack author
reading eight READMEs does not see that, and nothing else tests that the Modules load together.

**Decision.** The **Showcase** returns as a CurseForge modpack, `5thlayer/factoryworks-showcase`.

- **It curates, and authors nothing.** It holds the suite's Modules, the third-party mods chosen to
  play beside them, and their configs. It adds no recipes, datapacks, scripts, quests or art. A
  pack with content of its own stays out of scope (`.out-of-scope/custom-content-modpack.md`).
- **KubeJS is installed with no scripts**, so a later tweak needs no new jar. A script it ships is a
  new decision, since it would be the Showcase's first content.
- **The manifest is packwiz (ADR-0024).** Every jar is a CurseForge metafile by project and file id,
  so the export bundles no jar and avoids CurseForge's review of jars from elsewhere.
- **Configs ship only in file types CurseForge allows in `overrides/`.** The import warns about
  any other type (`.json5`, `.jexl`, `.md`, `.css`) as possibly harmful, so `.packwizignore`
  excludes them. Each was a default its mod writes again on first launch.
- **CurseForge only, for now.** Modrinth has not yet approved the Modules, and its export can only
  name jars hosted there.
- **It is not on the release train.** Nothing requires it, so it releases on its own once the
  Module files it names are published, and no Module waits on it.

**Considered: no pack, a list of mods in each README (#663).** Rejected: it shows no one the suite
working together, and leaves no check that it loads together.

**Consequences.**

- ADR-0128's amendment is superseded; the Showcase lives in the repository ADR-0128 first named.
- The Showcase is not the old Pack: none of the Pack's ADRs about its content come back with it.
