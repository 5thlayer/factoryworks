---
status: accepted
---

# Researchd leaves the Showcase

ADR-0115 kept research in the Showcase, Researchd, Porting Dead Libs and the tech tree, until it is
extracted to a private repository. Researchd has no 26.1.2 release upstream: the Showcase runs a fork pinned
by commit, and the port and a sync fix offered upstream
([Porting-Dead-Mods/Researchd#23](https://github.com/Porting-Dead-Mods/Researchd/pull/23),
[#24](https://github.com/Porting-Dead-Mods/Researchd/pull/24)) have had no answer.

**Decision.** Researchd and Porting Dead Libs leave the Showcase, and the tech tree with them: the Researchd
scripts, the extracted tree they read, the `researchd` lock source and the research checks. Every recipe is
unlocked from the start. The tree is not frozen or extracted first, since nothing would read it.

**Considered: keep the fork until upstream answers.** The Showcase would carry a dependency built from a
commit, which no pack author can install from CurseForge, to demo a tree that is not the suite's.

**Consequences.**

- ADR-0115's research clause is superseded; the rest of it stands.
- The tech-tree half of the freeze and the corpus move's exemption for the frozen tree have nothing to hold.
- A world loses its research progress, and a recipe locked by research is open.
