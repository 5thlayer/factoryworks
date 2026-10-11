## Agent skills

The skills are mattpocock/skills, a git submodule at `.agents/mattpocock-skills` pinned to a release
tag. Upstream nests them by category, and Claude Code reads only `.claude/skills/<name>/SKILL.md`, so
`.agents/skills/<name>` is a symlink into the submodule for each skill the repo uses, and
`.claude/skills` is a symlink to `.agents/skills`. A fresh clone needs `git submodule update --init`
or the links dangle; every session runs it from `.claude/hooks/session-start.sh`, which also resets
the submodule to the commit the checkout records. Take a new release by checking out its tag in the
submodule and committing it before the session restarts, clears or compacts, or the hook undoes the
checkout; a skill moved upstream breaks its link, so check that `.claude/skills/*/SKILL.md` all
resolve.

### Code review

Review code only with the mattpocock `code-review` skill (`.claude/skills/code-review/`), which
checks a diff against this repo's standards and the originating ticket. Never use Claude Code's
built-in `/code-review`, `/review`, `/security-review` or `/simplify` in its place, even though the
built-in `/code-review` shares its name.

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Rejected scope

`.out-of-scope/` holds one file per rejected enhancement, so a `wontfix` keeps its reasoning and a
repeat request is recognised rather than re-argued. `/triage` reads it while gathering context. Only
rejected enhancements go there — never bugs, never something already built, never a deferral. A
Factorio mechanic the suite does not reproduce belongs in the mechanic ledger instead (private,
see "Factorio corpus" below), which distinguishes `excluded` from `blocked`; a decision with a considered alternative belongs in an ADR.
See `.out-of-scope/README.md`.

### Domain docs

Single-context — `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Testing policy

Which check a feature warrants — and whether it warrants one at all — is decided by the claim the
feature makes, not ad hoc per ticket. Six claims, six answers, and a content ticket names its check
kind explicitly so that "no check" is a recorded decision. See `docs/testing/what-to-check.md`.

### Code comments

This overrides "match the surrounding comment density". Much existing code is over-commented, so
do not copy it.

A comment explains why the code is not the obvious code: a trap, a constraint from outside the
code, or a choice a reader would otherwise undo. Write the minimum that explains it, then name the
ADR or ticket that holds the reasoning, e.g. `(ADR-0074)`. Code that is obvious gets no comment.

Keep these out of comments:

- History, such as which ticket built something, what it replaced, or what was tried before. It
  belongs in the commit and the ADR.
- Rejected alternatives. They belong in the ADR.
- How a test was verified, such as "dropping X turns this red". It belongs in the commit.
- A restatement of what the next lines do.

When you edit code, trim the comments you touch to this rule. Leave comments elsewhere alone.

### Factorio corpus

No public repository holds or reads Wube's data (ADR-0103, ADR-0126, #605). The corpus, its
extractors and converters, the mechanic ledger, research and specs are in the private repository
`adamico/factoryworks-corpus`. A ticket that needs them is worked from that repository.

`docs/port/blocked-removals-26.1.2.md` lists every class the 26.1.2 port deleted, with the ticket
that restores it. Read it before concluding a mechanic was dropped.
