# A modpack with content of its own

FactoryWorks makes no modpack that authors content: no custom recipes, datapacks, scripts, quests
or art. The Showcase is a modpack, but it only curates the suite's Modules, the third-party mods
beside them and their configs (ADR-0131).

## Why this is out of scope

The suite is a set of standalone mods (ADR-0115) that plays in an ordinary world and mixes with
other tech mods. A pack that rewrites recipes or adds content shows a game no other pack gets, so
it demonstrates nothing about the mods a pack author would install. It is also the overhaul modpack
the suite was split out of, and a pack's tweak that a Module needs belongs in that Module.

A mod that needs another mod to be useful says so in its own dependencies.

## Prior requests

- #663: "Move the Showcase to 5thlayer/factoryworks-showcase", which retired the Showcase until
  ADR-0131 brought it back without content
