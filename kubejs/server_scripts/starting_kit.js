// priority: 0
// The Showcase's starting kit: handed over once per player, on first join (ADR-0127).
// Item ids are the suite's own or vanilla's; tests/pack/test_starting_kit.py holds that they resolve.
const STARTING_KIT = [
  ['beltworks:belt_tile', 32],
  ['beltworks:loader', 2],
  ['wireworks:small_pole', 4],
  ['pipeworks:pipe', 16],
  ['craftworks:assembler_1', 1],
]

// A stage rather than persistent data: KubeJS keeps it on the player across death.
const STARTING_KIT_STAGE = 'factoryworks_starting_kit'

PlayerEvents.loggedIn(event => {
  const player = event.player
  if (player.stages.has(STARTING_KIT_STAGE)) {
    return
  }
  // Marked before the items are given, so a failure part-way costs items, not a second kit.
  player.stages.add(STARTING_KIT_STAGE)
  STARTING_KIT.forEach(entry => player.give(Item.of(entry[0], entry[1])))
})
