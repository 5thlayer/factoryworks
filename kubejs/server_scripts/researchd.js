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
  event.create('planetary_factory:automation_science_pack')
    .literalName('Automation Science Pack')
    .color(200, 60, 60)
    .sortingValue(100);
  event.create('planetary_factory:logistic_science_pack')
    .literalName('Logistic Science Pack')
    .color(60, 200, 60)
    .sortingValue(101);
  event.create('planetary_factory:chemical_science_pack')
    .literalName('Chemical Science Pack')
    .color(60, 60, 200)
    .sortingValue(102);

  // when using the packs in recipes
  // a data component is needed:
  // Item.of('researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]')
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
//     mechanism, belongs in `planetaryfactory_core` under ADR-0015, and would serve all seven of
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
  icon: 'planetaryfactory:engineers_steel_pick',
  has: ['ftbmaterials:steel_plate', 50],
  unlocks: ['planetaryfactory:assembling/pack/engineers_steel_pick']
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
  icon: 'planetaryfactory:offshore_pump',
  has: ['ftbmaterials:iron_plate', 50],
  unlocks: [
    'planetaryfactory:assembling/pipe',
    'planetaryfactory:assembling/offshore_pump',
    'planetaryfactory:assembling/boiler',
    'planetaryfactory:assembling/steam_engine'
  ]
});

// CHANGING THIS ID NEEDS A RESTART AND A DEQUEUE/REQUEUE, NOT `/reload`: the registry can re-read the
// previous script evaluation, and a queued task keeps the ingredient it was queued with. An id tested
// without both looks exactly like a wrong id.
fromFactorio('electronics', {
  icon: 'planetaryfactory:electronic_circuit',
  has: ['ftbmaterials:copper_plate', 10],
  unlocks: [
    'planetaryfactory:assembling/copper_cable',
    'planetaryfactory:assembling/electronic_circuit',
    'planetaryfactory:assembling/inserter',
    'planetaryfactory:assembling/lab',
    'planetaryfactory:assembling/small_electric_pole'
  ]
});

// --- Rung 1 -------------------------------------------------------------------------------------

fromFactorio('automation-science-pack', {
  iconPack: 'planetary_factory:automation_science_pack',
  has: ['researchd:research_lab', 1],
  unlocks: ['planetaryfactory:assembling/automation_science_pack']
});

fromFactorio('steel-processing', {
  icon: 'ftbmaterials:steel_plate',
  unlocks: [
    'planetaryfactory:steel_plate',
    'planetaryfactory:assembling/steel_chest'
  ]
});

// `long-handed-inserter` is excluded (ADR-0076).
fromFactorio('automation', {
  icon: 'planetaryfactory:assembling_machine',
  unlocks: ['planetaryfactory:assembling/assembling_machine_1']
});

fromFactorio('logistic-science-pack', {
  iconPack: 'planetary_factory:logistic_science_pack',
  unlocks: ['planetaryfactory:assembling/logistic_science_pack']
});

// --- Rung 2 -------------------------------------------------------------------------------------

fromFactorio('automation-2', {
  icon: 'planetaryfactory:assembling_machine_2',
  unlocks: ['planetaryfactory:assembling/assembling_machine_2']
});

fromFactorio('engine', {
  icon: 'planetaryfactory:engine_unit',
  unlocks: ['planetaryfactory:assembling/engine_unit']
});

// The barrel fill/empty rows are `native_mechanic`, never recipes.
fromFactorio('fluid-handling', {
  icon: 'oritech:portable_tank',
  unlocks: [
    'planetaryfactory:assembling/storage_tank',
    'planetaryfactory:assembling/pump',
    'planetaryfactory:assembling/barrel'
  ]
});

// Also grants `oil-processing`'s four unlocks. That node's trigger is mining crude oil, a fluid, and
// no Researchd method reads a fluid, so declared on its own it would be a dead gate on the Chemical
// Plant. It costs nothing in Factorio, so the pair's pack cost is unchanged (#206, #138).
fromFactorio('oil-gathering', {
  icon: 'planetaryfactory:pumpjack',
  unlocks: [
    'planetaryfactory:assembling/pumpjack',
    'planetaryfactory:assembling/oil_refinery',
    'planetaryfactory:assembling/chemical_plant',
    'planetaryfactory:oil_processing/basic_oil_processing',
    'planetaryfactory:chemistry/solid_fuel_from_petroleum_gas'
  ]
});

// The DSL resolves parents through a skipped node, so `plastics` parents onto `oil-gathering`.
fromFactorio('oil-processing', { skip: true });

fromFactorio('plastics', {
  icon: 'planetaryfactory:plastic_bar',
  unlocks: ['planetaryfactory:chemistry/plastic_bar']
});

// ---------------------------------------------------------------------------------------------
// BELT TIERS (#345, #349). Each node grants its tier's belt and splitter; the underground belt has
// no pack recipe.

fromFactorio('logistics', {
  icon: 'beltworks:splitter',
  unlocks: ['planetaryfactory:assembling/splitter']
});

fromFactorio('logistics-2', {
  icon: 'beltworks:improved_belt_tile',
  unlocks: [
    'planetaryfactory:assembling/fast_transport_belt',
    'planetaryfactory:assembling/fast_splitter'
  ]
});

fromFactorio('logistics-3', {
  icon: 'beltworks:express_belt_tile',
  unlocks: [
    'planetaryfactory:assembling/express_transport_belt',
    'planetaryfactory:assembling/express_splitter'
  ]
});

// ---------------------------------------------------------------------------------------------
// LOADER TIERS (#347, ADR-0076). The burner inserter's recipe is unlocked from the start, as in
// Factorio, and `electronics` grants the inserter's above.

fromFactorio('fast-inserter', {
  icon: 'beltworks:express_loader',
  unlocks: ['planetaryfactory:assembling/fast_inserter']
});

fromFactorio('bulk-inserter', {
  icon: 'beltworks:turbo_loader',
  unlocks: ['planetaryfactory:assembling/bulk_inserter']
});
