---
status: accepted
---

# The pack ships nothing Wube owns, earns nothing, and launches to Factorio players last

The pack recreates Factorio: Space Age in Minecraft, and its numbers, recipes and tech tree are
extracted from the game. When it is complete two communities will see it: Minecraft tech players,
whom it is built for, and Factorio players and Wube, who will judge it against the original. Nothing
about its public footprint had been decided on purpose (#301). ADR-0102 records the licences and
ADR-0101 the name; this ADR records the rest of the policy.

Wube's wiki states its terms at https://wiki.factorio.com/Factorio:Copyrights: its text is CC BY-NC-SA
3.0, and Wube-owned images, art and game assets are excluded even from that.

**Decision.**

- **The audience is Minecraft tech players first.** Factorio players see the pack once, when it is
  complete.
- **The fidelity promise is Factorio's numbers, structure and functional ids.** They are used. Its
  prose, art and sounds are not. The promise is checkable in public: the mechanic ledger is
  published (#307), and a science-per-minute benchmark states the scale (#305, deferred until the
  pack is complete).
- **The naming rule.** "Factorio" is the inspiration in a description, as "inspired by Factorio",
  and never in the pack's name, logo, art or branding. Coined proper nouns (the Space Age planet
  names, Nauvis, biter, spitter, Wube, Factorio) never appear in a string a player reads. Registry
  ids and lang keys are functional and exempt. Descriptive machine names that are plain English stay
  (Foundry, Recycler). One that is not, such as Biochamber, is renamed when its machine lands. #304
  holds the term table and the check over lang values.
- **The asset rule.** The pack ships no Wube art, sound or game text, and no wiki text or image,
  not even as a placeholder. Wiki descriptions are never copied, so CC BY-NC-SA's ShareAlike and
  NonCommercial terms never attach to the pack. The corpus keeps no localised text, and the
  extractor cannot bring it back (#303). Git history is not rewritten.
- **Monetisation.** CurseForge Rewards is off for the project, and the project page says the pack
  earns nothing. If Rewards cannot be turned off, the points are donated and the page says that
  instead. This holds from the first upload, so #70 carries it out.
- **The launch sequence.** Minecraft channels may see milestones early. At completion Wube gets a
  courtesy heads-up, once, from the author. About two weeks later the author posts once to
  r/factorio and to the official forums. The posts go out whether or not Wube replies. #308 carries
  it out.
- **Wube's requests.** Any specific request from Wube, whether a name, an asset or a data file, is
  honoured promptly and acknowledged publicly. The project continues.

**Consequences.**

- A name, an asset or a data file is checked against this policy, not argued again.
- Two of the rules are checked in the repository: no `localised_*` field in the corpus (#303), and
  no coined term in a lang value (#304). No check can see a Wube image, since a copy to compare
  against would itself redistribute it. That rule is held by this ADR and by review.
- The scale claim is a check that loads a world. It is built and run by hand before launch, and
  its absence today is a decision (#305).
- Rewards, the heads-up, the posts, the request commitment and the published ledger are steps on
  external services with nothing in the repository to assert. This ADR is their record.
- Any legal position beyond this is out of scope. It is a good-faith stance, not legal advice.
