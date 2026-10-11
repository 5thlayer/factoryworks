# A custom modpack

FactoryWorks does not make or maintain a modpack of its own. The suite is a set of standalone mods
(ADR-0115), and a player or pack author puts them in whatever pack they like.

## Why this is out of scope

The Showcase was the last trace of the old overhaul modpack. Once Factorio fidelity went, it had no
content of its own: no blocks, items, textures or recipes. What remained was a packwiz manifest,
configs and a starting kit, and every one of those duplicates a decision each mod already makes for
itself. Keeping a pack means curating third-party jars, configs and a release on CurseForge, all of
which is work that ships nothing the mods do not.

A mod that needs another mod to be useful says so in its own dependencies. A combination worth
showing goes in that mod's README or its gallery, not in a pack.

## Prior requests

- #663: "Move the Showcase to 5thlayer/factoryworks-showcase"
