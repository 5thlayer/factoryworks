---
status: accepted
---

# The Alpha is the first public release, not a closed test before it

The Alpha was named as a closed test held before the first public upload (#534). It needs updates
delivered while its testers play from plastic to a rocket launch, and no closed channel does that:
CurseForge has no private or unlisted modpack status, a file downloads only once its project is
approved and public, and its idea for a private shareable pack is marked Not planned. So the Alpha
is the first public upload itself (#70), a CurseForge project labelled alpha, and what makes it a
test is a recruited group of **Alpha Testers**, most of them tech players who have never played
Factorio, with a tester-only `#stuck` channel on the pack's Discord. Anyone may still download the
pack and file a bug, and may apply for the role.

This keeps ADR-0103's sequence: the audience is Minecraft tech players, and Factorio players see the
pack at completion. The Alpha recruits on r/feedthebeast, the Discords of the mods the pack builds
on, and the players of the author's earlier pack, Mechanical Mastery+, who skip the application's
Factorio screen and take no more than a third of the places.

## Considered options

- **A CurseForge share code or exported zip.** It carries non-CurseForge jars, but a code expires
  in seven days and a recipient re-imports every update by hand.
- **A Modrinth shared instance.** Invite-only and pushes updates, but untested with a pack whose
  mods are almost all CurseForge's, and Modrinth waits on every jar having a Modrinth build (#70).
- **Prism with packwiz-installer.** Updates on each launch from the public manifest, but asks every
  tester to use one launcher and makes a closed test closed only by obscurity.

## Consequences

- The Alpha waits on everything #70 waits on, Researchd's 26.1.2 port being published upstream
  first (#524).
- Worlds carry over between Alpha updates by default; an update that breaks them is announced as a
  wipe.
