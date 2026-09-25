---
status: accepted
---

# A placed block turns by its own contract, vanilla rotate by default, with a deny list

ADR-0083 left the placed half of Rotate to its follow-up: what turning means there is the block's
own, as in Factorio -- a belt tile turns a quarter, an underground belt would swap its ends, a machine
keeps its contents (#405).

**Decision (#405).** A press takes the held stack if it is rotatable and otherwise the block under
the crosshair, Factorio's target rule; the server decides, from one payload carrying the aimed
block. A stack is rotatable if the block it places has a facing, an axis or a sixteen-way rotation,
since vanilla cannot say whether a block's placement reads the look: stone or a pole is not, so
pressing with one held turns the aimed block. A placed block is turned by, in order:

1. The **deny list** in core: a block whose vanilla `rotate` is wrong is refused with its reason on
   the action bar, and nothing changes. The belts fork's splitter is on it until #407, since one
   half turned alone splits it, and so is a belt tile on a **slope** (a foot, middle or top), since
   turning one would break its climb (ADR-0085), and the wedge under one, which only its tile keeps.
2. The block's own contract, `TurnsInPlace`, if it implements it. The footprint machines and the
   mining rigs implement it by refusing until #406 turns them whole.
3. Otherwise vanilla's level-aware `rotate`, then the neighbour shape update vanilla would run
   later, applied only where the state changes. A block that would no longer stand is left as it
   was, and a door or bed half turned alone re-derives against its other half. A turned state
   that does not fit where it stands is refused: a level belt tile is held to the fork's own
   placement rule, so a turn that would slope a corner (#419) or needs a wedge with no room (#420)
   is refused with the placement's reason.

The press fires NeoForge's place event at the aimed block first and does nothing if it is
cancelled, since that event is how FTB Chunks guards a claim and a custom payload fires none.

The order is a Minecraft-free rule, `PlacedTurn`; the world half is `PlacedTurns`.

**Considered.** The fork implementing core's contract. The fork does not depend on core (ADR-0083),
so its blocks answer through vanilla `rotate` or the deny list. A preview of the placed turn: the
key acts at once and names a refusal, as the grilling decided.

**Consequences.** A block whose vanilla `rotate` is the identity ignores the key silently, as a
Factorio entity that cannot rotate does; the three furnaces are such blocks. A block entity is kept across the turn, since the block does not change. A new
foreign block whose vanilla turn is wrong turns wrongly until it is added to the deny list.
