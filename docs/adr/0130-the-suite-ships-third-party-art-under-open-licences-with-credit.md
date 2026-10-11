---
status: accepted
---

# The suite ships third-party art under open licences, with credit

ADR-0122 held that FactoryWorks and its Modules ship only art they made, and that borrowed art
leaves as it is replaced. In practice the suite's art comes from two places it cannot cheaply
redraw: art collections (Malcolm Riley's unused-textures, which Fieldworks already ships under its
own ADR 0006) and other mods (Pipeworks' pipe and tank are Oritech's CC0 models; Beltworks' belts
are re-authored from Simple Conveyor Belts' art). Replacing them buys nothing a player sees.

**Decision.** The suite and its Modules may ship third-party art under any open licence that allows
redistribution. Each repo credits it in its `NOTICE` and assigns its licence in `REUSE.toml`. That
holds whether the art comes from an art collection or from another mod. Where nothing of our own
exists and nothing open is worth taking, the asset stays Stand-in art. Minecraft's own textures
remain the platform, not borrowed art.

**Considered: ship only art the suite made (ADR-0122).** Rejected: a licence that allows
redistribution already settles whether the art may be used, credit settles whose it is, and
redrawing working art to avoid saying so costs effort and improves nothing.

**Consequences.**

- This supersedes ADR-0122. Its amendment of ADR-0102 no longer applies, so ADR-0102's licensing
  stands as written. Its supersession of ADR-0109's art paragraph is undone: ADR-0109's rule that
  the Pack depends on no third-party content mod is unchanged, and third-party art may be shipped
  under this rule.
- Vendored art is shipped for good, not until replaced. The provenance manifest (#564) keeps its
  `vendored` kind to list it.
- A licence that does not allow redistribution, or restricts it to a non-commercial use the suite
  cannot honour, still keeps art out.
- beltworks#96, pipeworks#14 and libworks#7, which tracked replacing borrowed art, are closed as
  not planned.
