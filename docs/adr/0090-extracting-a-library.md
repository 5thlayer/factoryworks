---
status: accepted
---

# Extracting a Library

Three mechanics have left the Pack as mods of their own: Beltworks (forked from SimpleBelts),
Groundworks (carved out of `core/placement/`) and Craftworks (carved out of `core/assembler/`,
ADR-0089). Each extraction re-decided the same questions, and several were decided twice: the Pack
consumed Beltworks from mavenLocal before #465 pinned it, and the Pack's belt GameTests ran beside
Beltworks' own until #438 deleted them. The electric pole network is next (#476).

A **Library** is a 5thlayer mod the Pack consumes. A **Binding** is the Pack's code that configures a
Library for Factorio's rules, with the tests asserting that configuration. Both terms are defined in
5thlayer/skillworks, which holds the `extract-library` procedure; this ADR holds only its rules.

**Decision.**

- **The gate.** A mechanic is extracted only if all five hold, otherwise it stays in
  `factoryworks_core`:
  1. it makes sense without Factorio's rules;
  2. few Pack classes depend on it;
  3. its tests move without the Pack's corpus;
  4. it works standalone, in a game with no Pack;
  5. it gives the modding community a mechanic no other mod has.
- **Consumption.** The Pack consumes every Library only as a local jar pinned in
  `data/pack/local-jars.json` and copied by `scripts/sync-local-jars.py` (#465). The build reads that
  file rather than naming a Library (#475). A fork we did not write, such as Researchd's, is
  consumed the same way without being a Library.
- **Tests.** A Library's tests move with its code. The commit that switches the Pack to the Library
  deletes the Pack's copies of both.
- **Bindings stay.** A Binding and its GameTests stay in the Pack, under the `factoryworks:*`
  selector (#448), because they assert the Pack's settings, which a Library's own run rules out.

**Not decided here.** Naming is not a rule: the skill fixes a name before the first commit because
renaming later cost two rounds of churn, and lists what a rename touches, but a Library may be
renamed. How a Library is built, released and versioned is 5thlayer/libworks', the template every
Library starts from.
