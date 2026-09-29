---
status: accepted
---

# The pack is FactoryWorks, an overhaul modpack, and its mod is FactoryWorks Core

The pack is unreleased and has never had a name chosen on purpose. "PlanetaryFactory" promises the
interplanetary scope, and the first release is Terra alone: every other body is parked (ADR-0060)
and travel waits on #340. Meanwhile the mechanics that left the pack became mods of their own under
one family name -- Beltworks, Craftworks and Groundworks inside Beltworks (ADR-0090) -- and the
first two are published. #301 already bars "Factorio" and its coined names from the pack's name.

**Decision.**

- **The name is FactoryWorks, and the tagline is "the factory just works".** "Works" carries the
  family's shared promise: tech mods with the friction taken out, true to Factorio. The capital W
  is deliberate. A Library is spelled with a lowercase w (Beltworks); the pack is not a Library, it
  is what the Libraries add up to. The slug is `factoryworks` on CurseForge and on Modrinth, both
  free when checked on 2026-09-29.
- **FactoryWorks is an overhaul modpack**, in the sense RLCraft is one. The rules are first-party:
  the core mod, the Libraries and the Researchd fork. Third-party mods supply parts (Oritech's
  models and entities, FTB Materials' items, FTB Chunks' map), libraries, or quality of life. The
  pack removes every stock recipe (ADR-0034), vanilla spawning (ADR-0093), the Nether and the End
  (ADR-0094) and the crafting grid (ADR-0066). It is distributed as a modpack, not as a mod other
  packs build on.
- **The core mod is FactoryWorks Core**, the pack's **Binding** in ADR-0090's terms. It is published
  as a CurseForge project so the pack's manifest names it by project and file id, and its page says
  it is required by the modpack and not meant on its own. This amends ADR-0024's rule that the core
  mod stays out of the index; how the jar reaches the manifest is #70's.
- **The rename reaches every identifier.** The registry namespace `planetaryfactory` becomes
  `factoryworks`, the mod id `planetaryfactory_core` becomes `factoryworks_core`, the package
  `com.planetaryfactory` becomes `com.factoryworks`, and the GitHub repository `planetary-factory`
  becomes `factoryworks`. The split between the pack's namespace and the mod's id is kept, so the
  rename is a substitution and not a redesign. The live docs follow, ADRs included, since they are
  state. Closed tickets and commits keep the old ids as history. It lands before the first upload
  (#70) and before the repository goes public (#306), as a pull request with nothing else open.

**Considered: keep PlanetaryFactory.** Rejected. It names a destination the first release does not
reach, and milestones reach Minecraft channels before the pack is complete (#301).

**Considered: a name for the scope, the loop or the frontier.** Starworks and Orbitworks named the
scope, and overpromise for the same reason. Chainworks and Lineworks named the production loop and
read as a chain-block mod and a drawing tool. Outworks named the push outward and is a word few
players know. FactoryWorks names the loop and is true at every release.

**Considered: Factoryworks, lowercase.** Rejected. It would read as one more Library.

**Considered: a mod that packs are built on**, as TerraFirmaCraft is. Rejected. The core mod is a
Binding, which by ADR-0090's definition makes sense only with Factorio's rules. The pack's recipes,
worldgen and sweep live in `kubejs/`, not in the jar. A mechanic that is reusable leaves as a Library
through ADR-0090's gate, as the pole network is about to (#476), so no second route is needed.

**Considered: rename the display name only.** Rejected. The pack ships Better Advanced Tooltips, so
every tooltip would show `planetaryfactory:`, and a published FactoryWorks Core with the mod id
`planetaryfactory_core` confuses pack authors and crash reports. After the first release a namespace
change loses every placed block and held item of the old namespace in every world. Before it, the
cost is one diff.

**Consequences.**

- "Factory" is a crowded search term. Feed the Factory pitches Factorio-style play, and Forever
  Factory is AmmoniumX's, a collaborator's; AmmoniumX hears of the name before it is announced
  (#308). The store page has to set the pack apart in its first line.
- Beltworks and Craftworks are CurseForge projects, so the manifest names them by id with no
  bundled jar. The jars #70 still has to route are FactoryWorks Core, the Researchd fork and Porting
  Dead Libs.
- A future Library may not take the pack's name, and the names the pole network would want
  (Gridworks, Wireworks, Powerworks) are left free for it.
