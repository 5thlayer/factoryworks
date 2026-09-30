// The two authored sizes are the rows x 9 of `startup_scripts/blocks.js`; client and startup
// scripts do not share a scope, so they are restated here.
const CHEST_STACKS = {
  'minecraft:chest': 27,
  'factoryworks:iron_chest': 36,
  'factoryworks:steel_chest': 54,
};

ItemEvents.modifyTooltips((event) => {
  Object.keys(CHEST_STACKS).forEach((id) => {
    event.add(id, Text.translatable('tooltip.factoryworks.chest.stacks', `${CHEST_STACKS[id]}`).gray());
  });
});
