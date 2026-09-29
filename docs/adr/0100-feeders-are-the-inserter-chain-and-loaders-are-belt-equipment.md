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
- Each feeder pays its inserter's swing energy per item, and tier 1 pays the burner inserter's.
  These are set in `config/beltworks-server.toml` under `feederJoulesPerItem`.
- The loaders are hand-written in `recipe/assembling/pack/`, in the shape of Factorio 1.1's hidden
  loaders: 5 feeders, 5 electronic circuits, 5 iron gear wheels, 5 iron plates and 5 belts, all of
  the loader's tier. Tier 1 has no Factorio counterpart and takes no circuit.
- Loader tier *n* unlocks with belt tier *n*, and tier 1 at the start.
- `long-handed-inserter` becomes `adapted`: the feeder's reach does its job. It has no recipe,
  because reach is set by key, not by tier.

**Considered: keep ADR-0076 and give the feeder a copy of the inserter's costs.** Rejected. Each
rung would then have two items of near-equal cost behind one technology, and the family that
actually behaves like the inserter would be the one without its name.

**Consequences.**

- The loader chain is authored by the Pack, which ADR-0076 had avoided. Its figures come from
  Factorio 1.1's data, not from a choice.
- Loaders keep their current per-item energy, since Beltworks makes it configurable for no loader
  tier. That is a carry-over, not a decision, until 5thlayer/beltworks makes loader energy
  configurable per tier.
