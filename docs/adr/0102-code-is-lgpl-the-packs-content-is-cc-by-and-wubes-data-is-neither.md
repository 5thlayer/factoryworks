---
status: accepted
---

# Code is LGPL-3.0-only, the Pack's content is CC BY 4.0, and Wube's data is neither

The repository goes public (#306) and the Pack is uploaded (#70), and until now it stated no licence
of its own: `NOTICE` lists what was copied in and says nothing of what the rest is (#302). The
store pages already say LGPL for the code and CC BY for the assets (`publish/description.md`), so
this ADR makes the repository say the same. It records the licences only. The rest of #302's policy
-- audience, fidelity promise, naming and asset rules, monetisation, launch sequence, the commitment
on Wube's requests and the citation of Wube's copyright terms -- is not decided here; it is
ADR-0103's.

**Decision.**

- **Code is LGPL-3.0-only.** FactoryWorks Core, its build, the scripts and checks, the KubeJS
  scripts, and the manifests and configs a program reads as instructions. `neoforge.mods.toml`
  declares `LGPL-3.0-only` to match.
- **The Pack's own content is CC BY 4.0.** Docs, the datapack and resource JSON, the item and
  decision tables under `data/pack/`, textures, structures, quests, and the store pages.
- **Wube's Factorio data is under neither.** `data/factorio/*.json` and the script the tech
  extractor rewrites from it, `kubejs/server_scripts/factorio_tech_data.js`, are recorded as
  `LicenseRef-Wube-Factorio-Data`: Wube's, not licensed here, not offered for reuse. The Pack's own
  generators and the recipes, tables and resources they emit are the Pack's work and are licensed
  by their kind. `data/factorio/README.md` states this.
- **Everything else keeps its own licence.** The Electric Furnace art is CC BY-NC-SA 4.0, the ore
  art CC BY 4.0, Konkrete's translations Apache-2.0, the vendored skills under `.agents/` MIT,
  Core's release tooling (`scripts/release.sh`, `scripts/upload.py` and its tests,
  `publish/upload.env`) MIT as the 5thlayer/libworks template it is copied from, and the Gradle
  wrapper Apache-2.0. The store art's backdrop includes Minecraft's textures, which stay
  Mojang's.
- **The map is `REUSE.toml` and `LICENSES/`,** laid out as Beltworks has them, checked by
  `reuse lint` in CI and by `tests/pack/test_licensing.py`, which implements the same rule for a
  machine without libmagic.
- **The upload carries the texts.** The core mod's jar bundles `LICENSE`, `NOTICE` and
  `LICENSES/`, as Beltworks' does, since the jar is published on its own. The Pack's manifest
  indexes the same files and `REUSE.toml`, so the modpack carries them too; `.packwizignore` does
  not exclude them.
- **No file carries an SPDX header.** Beltworks and the Libraries mark their Java files. Nothing in
  the core mod is a fork, so one licence covers each tree, a glob is the whole map, and 400
  identical headers would say nothing more. The cost is that a file copied
  out of the repository carries no mark; the store pages and `LICENSE` say what the mod is.

**Why LGPL here and MIT in Beltworks.** Beltworks rejected LGPL (its ADR-0001) because every
modpack redistributing a Library would owe a pointer to its source, and a Library exists to be
reused. FactoryWorks Core is a Binding (ADR-0101): it makes sense only with Factorio's rules, so
nothing is lost by asking that a changed copy of it stay open. A mechanic worth reusing leaves as a
Library through ADR-0090's gate and takes that Library's licence.

**Beside the 5thlayer mods.** Beltworks (MIT code, CC BY 4.0 assets and Rearth's code), Groundworks
and Craftworks (MIT) are separate jars the Pack depends on, and the core mod compiles against them.
MIT and CC BY 4.0 place no condition an LGPL-3.0-only mod cannot meet on being depended on.
Respoiled is parked until #481 and is not in the Pack, so its licence is checked when it returns.

**Open: Researchd and Porting Dead Libs.** This ADR does not call them compatible. Both are under
Porting Dead Mods' Common Sense License 1.0, which allows redistribution only of a fork altered by
about 50% or more, at the original author's judgement, and requires prominent credit. Its §3 lifts
the restriction if upstream goes six months without a commit or is set read-only, and neither is
true today. The Researchd fork the Pack pins (`1.3.4-26.1`, branch `26.1`) is a port and Porting
Dead Libs ships unchanged, so as the licence reads today neither can be uploaded inside the Pack. That
blocks #70 and is #524's, which chooses the route: written permission, upstreaming the port, or
leaving Researchd. When it is chosen, this ADR is amended with the licence of both jars and the
route taken.

**Noted, not decided.** The Electric Furnace art is NonCommercial, which the Pack's own licences are
not, and the Pack is uploaded to platforms that pay authors. `NOTICE` keeps it credited and REUSE
keeps it apart; whether to keep it in the upload is for #70.

**Consequences.**

- A new file no annotation reaches fails `tests/pack/test_licensing.py`, so a new kind of file
  gets a licence when it is added rather than after.
- The copyright holder in `REUSE.toml` is 5thlayer, as in the Libraries.
- Third-party art added to the repository takes a `NOTICE` entry and a `REUSE.toml` exception, the
  test holding the two to each other.

## Amended by ADR-0122

No third-party art is added. Each `NOTICE` art entry and `REUSE.toml` exception goes when its
asset is replaced with FactoryWorks' own.
