---
status: accepted
---

# This repo is the suite's home, and the Showcase moves out

Decisions touching several Modules had nowhere to live: they landed in this repo, which is also the
Showcase pack, so suite and pack ADRs share one numbering and a cross-mod ticket has no parent. This
repo holds all the history, so it becomes the suite's home rather than a new repo being made.

**Decision.**

- **`5thlayer/factoryworks` is the suite's home**: the suite glossary, cross-Module ADRs, specs and
  research, and the parent issue of every cross-Module topic. Each affected Module gets a child issue
  in its own repo. A Module keeps its own glossary and ADRs for what only it touches. The repo is
  public.
- **The Showcase moves to `5thlayer/factoryworks-showcase`**: the packwiz manifest, `kubejs/`,
  configs, its publish files and the checks that guard pack content. The pack's ADRs stay here as
  history.
- **`mod/` is audited before it moves or goes**: what Fieldworks and the other Module repos already
  carry is deleted, and integration GameTests move with the Showcase.
- **The suite has no shared runtime library yet.** A survey of the Module repos found about 150 lines
  of runtime code copied between them, against about 1,100 lines of build and release tooling copied
  into every repo from libworks. **libworks becomes a dependency** (a Gradle plugin and scripts at a
  version) instead of a template copied once, so the tooling stops drifting. A runtime library is
  made when real shared runtime code appears.
- **Groundworks is a Module**, owning placement, and the one Module others may require. It is not a
  home for general code.
- **The Factorio corpus, its extractors and the mechanic ledger** still go to a private repo
  (ADR-0115), as a separate ticket.
