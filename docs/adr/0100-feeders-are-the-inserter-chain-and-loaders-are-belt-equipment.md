---
status: accepted
supersedes: [347]
---

# Feeders are the inserter chain, and loaders are belt equipment

Beltworks 0.3.10 adds the **feeder**: one item at a time, head and tail each reaching 1–3 blocks,
either end a chest, a machine or a belt tile, and a head that picks up loose items. That is the job
Factorio's inserter does. ADR-0076 had given the inserter's recipes and technologies to the loader,
because until then the loader was the only block that could take them.

**Decision.** This amends ADR-0076's recipe and technology half. Its splitter half stands.

- The four feeder tiers take the corpus's inserter chain: `burner-inserter`, `inserter`,
  `fast-inserter` and `bulk-inserter` map to `feeder`, `improved_feeder`, `express_feeder` and
  `turbo_feeder`. Each is unlocked by that inserter's technology.
- Each feeder pays one whole swing of its inserter per item, tier 1 the burner inserter's, since a
  feeder carries one item a swing: the turbo feeder pays the bulk inserter's full 23,200 J, not the
  11,600 its two-item hand averages. The burner inserter's swing takes 5 spike ticks, like the
  inserter whose extension speed it shares; Beltworks states no rule for them. The figures are set
  in `config/beltworks-server.toml` under `feederJoulesPerItem`.
- The loaders take Factorio's own loader recipes, which the game ships hidden. The extractor keeps
  `loader`, `fast-loader` and `express-loader`, and the converter emits them like any other recipe.
  The tier-1 loader takes 5 `inserter`, which is a tier-2 feeder here, so it cannot be crafted
  before `electronics`. `turbo-loader` takes the turbo belt, which has no recipe, so the turbo
  loader has none either.
- Loader tier *n* unlocks with belt tier *n*: `logistics`, `logistics-2` and `logistics-3`.
- `long-handed-inserter` becomes `adapted`: the feeder's reach does its job. It has no recipe,
  because reach is set by key, not by tier.

**Considered: keep ADR-0076 and give the feeder a copy of the inserter's costs.** Rejected. Each
rung would then have two items of near-equal cost behind one technology, and the family that
actually behaves like the inserter would be the one without its name.

**Consequences.**

- ADR-0076's premise that Factorio has no loader recipe was wrong: the recipes are hidden, not
  absent. Nothing in the loader chain is authored by the Pack.
- The logistic science pack takes an `inserter`, so it now takes a tier-2 feeder.
- Loaders keep their current per-item energy, since Beltworks makes it configurable for no loader
  tier. That is a carry-over, not a decision, until 5thlayer/beltworks makes loader energy
  configurable per tier.
