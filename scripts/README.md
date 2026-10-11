# `scripts/`

| Script | Does |
|---|---|
| `adr-backlink-check.sh` | asserts every ticket an ADR declares superseded carries a comment naming that ADR. Needs an authenticated `gh`; run it after committing an ADR with a `supersedes:` key (`docs/agents/domain.md`). |
