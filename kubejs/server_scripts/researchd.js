// priority: 0
// requires: researchd
// The pack's research tree. Topology, names and pack costs come from Factorio's extracted
// technology tree (data/factorio/technology.json); this file supplies only what Factorio
// cannot know -- which Minecraft item is the icon, which recipe a research unlocks, and
// which of our bodies gates it. See ADR-0022 and factorio_tech_dsl.js.
//
// A technology absent from this file is absent from the pack, and the DSL logs the ones
// still undeclared on every reload.
//
// The `priority: 0` header above is load order: this file must load AFTER
// factorio_tech_data.js (20) and factorio_tech_dsl.js (10), because fromFactorio() has to
// exist before these calls run. KubeJS does not load scripts alphabetically.

ResearchdEvents.registerResearchPacks(event => {
  event.create('factory_works:automation_science_pack')
    .literalName('Automation Science Pack')
    .color(200, 60, 60)
    .sortingValue(100);
  event.create('factory_works:logistic_science_pack')
    .literalName('Logistic Science Pack')
    .color(60, 200, 60)
    .sortingValue(101);
  event.create('factory_works:chemical_science_pack')
    .literalName('Chemical Science Pack')
    .color(60, 60, 200)
    .sortingValue(102);

  // when using the packs in recipes
  // a data component is needed:
  // Item.of('researchd:research_pack[researchd:research_pack="factory_works:automation_science_pack"]')
});

// Steel axe, the pack's first declared technology and ADR-0039's second tier.
//
// Factorio's row is carried exactly where it can be: it is one of the 33 trigger technologies, so
// it costs no science packs at all, and its prerequisite `steel-processing` is still undeclared --
// the DSL resolves through it rather than orphaning this one.
//
// TWO DELIBERATE DIVERGENCES, both ADR-0039's:
//
//   - The trigger. Factorio fires this on CRAFTING 50 steel plates, and Researchd has no
//     craft-triggered method -- its four are consumeItem, consumePack, checkItemPresence and the
//     combinators. `has` is checkItemPresence, which holds the plates rather than eating them, and
//     that is the closer of the two: Factorio's trigger charges nothing. A real craft trigger is
//     mechanism, belongs in `factoryworks_core` under ADR-0015, and would serve all seven of
//     #138's trigger technologies rather than this one alone.
//
//   - The effect. Factorio's is `character-mining-speed +1`; here it unlocks the Engineer's Steel
//     Pick recipe and the speed rides on the item. The outcome is the same -- mining doubles, 2.0s
//     to 1.0s -- and this is the first time the pack overrides an extracted effect rather than
//     supplying one, which is why it is written down: a reader diffing this file against
//     `data/factorio/technology.json` would otherwise read it as a bug.
//
// The unlock id is `assembling/pack/`: the recipe is hand-authored (ADR-0031's exception) and lives
// in the one subtree no converter owns -- nested INSIDE `assembling/` because GregTech re-registers
// every loaded GTRecipe under its own type path (#87), so a flat `pack/` would put a second id in
// the recipe manager and this gate would name the wrong one of the two.
// `tests/factorio/test_research_unlocks.py` asserts this id is a recipe the pack emits, which is
// the coupling that makes the divergence safe.
fromFactorio('steel-axe', {
  icon: 'factoryworks:engineers_steel_pick',
  has: ['factoryworks:steel_plate', 50],
  unlocks: ['factoryworks:assembling/pack/engineers_steel_pick']
});

// ---------------------------------------------------------------------------------------------
// THE CRITICAL PATH TO PLASTIC (#206): the ancestor closure of Factorio's `plastics`, every node
// `fromFactorio()`, since #170 times the pace and an invented cost would falsify it.
//
// Unlocks name only recipes the pack emits: a lock on an id nothing emits unlocks nothing, silently,
// so Factorio effects with no pack recipe are dropped (`test_research_unlocks.py`).
//
// Factorio's `craft-item` triggers are events and Researchd's `checkItemPresence` reads state, so the
// three craft triggers fire on holding the items. The check latches and charges nothing, as a
// Factorio trigger technology does, so spending the plates afterwards re-locks nothing. `consumeItem`
// is reserved for rung 0 (#42, ADR-0033). #138 generalises this.

// --- Rung 0 -------------------------------------------------------------------------------------

// `pipe-to-ground` has no pack recipe: underground pipes are excluded.
fromFactorio('steam-power', {
  icon: 'factoryworks:offshore_pump',
  has: ['factoryworks:iron_plate', 50],
  unlocks: [
    'factoryworks:assembling/pipe',
    'factoryworks:assembling/offshore_pump',
    'factoryworks:assembling/boiler',
    'factoryworks:assembling/steam_engine'
  ]
});

// CHANGING THIS ID NEEDS A RESTART AND A DEQUEUE/REQUEUE, NOT `/reload`: the registry can re-read the
// previous script evaluation, and a queued task keeps the ingredient it was queued with. An id tested
// without both looks exactly like a wrong id.
fromFactorio('electronics', {
  icon: 'factoryworks:electronic_circuit',
  has: ['factoryworks:copper_plate', 10],
  unlocks: [
    'factoryworks:assembling/copper_cable',
    'factoryworks:assembling/electronic_circuit',
    'factoryworks:assembling/inserter',
    'factoryworks:assembling/lab',
    'factoryworks:assembling/small_electric_pole'
  ]
});

// --- Rung 1 -------------------------------------------------------------------------------------

fromFactorio('automation-science-pack', {
  iconPack: 'factory_works:automation_science_pack',
  has: ['researchd:research_lab', 1],
  unlocks: ['factoryworks:assembling/automation_science_pack']
});

fromFactorio('steel-processing', {
  icon: 'factoryworks:steel_plate',
  unlocks: [
    'factoryworks:steel_plate',
    'factoryworks:assembling/steel_chest'
  ]
});

// `long-handed-inserter` has no recipe: the feeder's reach does its job (ADR-0100).
fromFactorio('automation', {
  icon: 'craftworks:assembler_1',
  unlocks: ['factoryworks:assembling/assembling_machine_1']
});

fromFactorio('logistic-science-pack', {
  iconPack: 'factory_works:logistic_science_pack',
  unlocks: ['factoryworks:assembling/logistic_science_pack']
});

// --- Rung 2 -------------------------------------------------------------------------------------

fromFactorio('automation-2', {
  icon: 'craftworks:assembler_2',
  unlocks: ['factoryworks:assembling/assembling_machine_2']
});

fromFactorio('engine', {
  icon: 'factoryworks:engine_unit',
  unlocks: ['factoryworks:assembling/engine_unit']
});

// The barrel fill/empty rows are `native_mechanic`, never recipes.
fromFactorio('fluid-handling', {
  icon: 'pipeworks:storage_tank',
  unlocks: [
    'factoryworks:assembling/storage_tank',
    'factoryworks:assembling/pump',
    'factoryworks:assembling/barrel'
  ]
});

// Also grants `oil-processing`'s unlock. That node's trigger is mining crude oil, a fluid, and no
// Researchd method reads a fluid, so declared on its own it would be a dead gate (#206, #138).
// Advanced oil processing is the only petroleum source, and chemical science needs petroleum, so
// it unlocks here rather than behind chemical science (#644).
fromFactorio('oil-gathering', {
  icon: 'factoryworks:pumpjack',
  unlocks: [
    'factoryworks:assembling/pumpjack',
    'factoryworks:oil_processing/advanced_oil_processing',
    'factoryworks:chemistry/solid_fuel_from_petroleum_gas'
  ]
});

// The DSL resolves parents through a skipped node, so `plastics` parents onto `oil-gathering`.
fromFactorio('oil-processing', { skip: true });

fromFactorio('plastics', {
  icon: 'factoryworks:plastic_bar',
  unlocks: ['factoryworks:chemistry/plastic_bar']
});

fromFactorio('advanced-oil-processing', {
  icon: 'factoryworks:solid_fuel',
  unlocks: [
    'factoryworks:chemistry/heavy_oil_cracking',
    'factoryworks:chemistry/light_oil_cracking',
    'factoryworks:chemistry/solid_fuel_from_heavy_oil',
    'factoryworks:chemistry/solid_fuel_from_light_oil'
  ]
});

// ---------------------------------------------------------------------------------------------
// BELT TIERS (#345, #349). Each node grants its tier's belt, splitter and loader (ADR-0100); the
// underground belt has no pack recipe.

fromFactorio('logistics', {
  icon: 'beltworks:splitter',
  unlocks: [
    'factoryworks:assembling/splitter',
    'factoryworks:assembling/loader'
  ]
});

fromFactorio('logistics-2', {
  icon: 'beltworks:improved_belt_tile',
  unlocks: [
    'factoryworks:assembling/fast_transport_belt',
    'factoryworks:assembling/fast_splitter',
    'factoryworks:assembling/fast_loader'
  ]
});

fromFactorio('logistics-3', {
  icon: 'beltworks:express_belt_tile',
  unlocks: [
    'factoryworks:assembling/express_transport_belt',
    'factoryworks:assembling/express_splitter',
    'factoryworks:assembling/express_loader'
  ]
});

// ---------------------------------------------------------------------------------------------
// FEEDER TIERS (#514, ADR-0100). The burner inserter's recipe is unlocked from the start, as in
// Factorio, and `electronics` grants the inserter's above.

fromFactorio('fast-inserter', {
  icon: 'beltworks:express_feeder',
  unlocks: ['factoryworks:assembling/fast_inserter']
});

fromFactorio('bulk-inserter', {
  icon: 'beltworks:turbo_feeder',
  unlocks: ['factoryworks:assembling/bulk_inserter']
});
