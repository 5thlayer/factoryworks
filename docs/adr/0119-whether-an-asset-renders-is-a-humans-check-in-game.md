---
status: accepted
---

# Whether an asset renders is a human's check in game

`scripts/check-client-assets.py` (#276) booted the Pack's real client with no display and read the
asset manager's complaints from its log. The repo root is the game instance, so that client is the
player's own: it loads the player's options and mods, and on 2026-10-05 an agent's run of it opened
the player's latest save and saved it with jars from an unpushed change.

**Decision.** The check is removed. Whether a model, blockstate, texture or fluid renders is checked
by a human in game when the change is delivered, alongside whether it looks right. Agents run only
checks that start no client: the static asset walkers (`test_block_assets.py`,
`test_data_formats.py`), `check-datapack-load.py` and the GameTest harness, all of them server or
file checks. An agent hands the in-game checks to the human in its report.

**Considered: the check stops at the title screen**, so it never joins a world. Rejected: it would
still boot the player's instance, with the player's options and window, and an agent would still
run it unasked, since a check in the table is one an agent runs.

**Consequences.** A present asset the game rejects is not caught until a human looks. A missing item
model definition was never logged anyway and stays with `test_data_formats.py`.
