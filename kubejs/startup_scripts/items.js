StartupEvents.registry('item', event => {
  // The circuit ladder, and plastic.
  //
  // First-party by ADR-0031's borrow/author rule: borrow an existing item unless the row sits on
  // a rung boundary or a mod's competing line would give a parallel escape. The circuits carry
  // progression and #62 removed GregTech's and Mekanism's competing lines; plastic gates rung 2
  // (ADR-0025). `copper-cable` is the counter-example and borrows
  // (`powergrid:wire`) -- see `data/pack/item-map.json`.
  //
  // EVERY TEXTURE MUST BE A FILE THAT EXISTS. GregTech generates its MATERIAL items (plates,
  // gears, dusts, most batteries) at runtime from a material set, so `gtceu:item/<material>_plate`
  // and `gtceu:item/max_battery` name no PNG in the jar and render as the missing-texture checker
  // with no error anywhere. Every path below was checked against
  // `assets/gtceu/textures/item/` in `gtceu-1.21.1-7.0.2.jar`, or is vanilla's.
  //
  // EVERY TEXTURE HERE IS A PLACEHOLDER -- one borrowed GregTech icon on all four items. Art is a
  // "looks or feels right" check (docs/testing/what-to-check.md) and lands with the quest book,
  // not with the converter.
  //
  // Their recipes are not written here: they are the corpus's, emitted by
  // `scripts/factorio-recipe-convert.py` into `kubejs/data/factoryworks/recipe/`.

  // Common
  //
  // The circuit sprites and the engine unit's are Stand-in art written by
  // `scripts/gen-standin-textures.py` (ADR-0109).
  event.create('factoryworks:electronic_circuit')
    .displayName('Electronic Circuit')
    .texture('factoryworks:item/electronic_circuit')

  event.create('factoryworks:advanced_circuit')
    .displayName('Advanced Circuit')
    .texture('factoryworks:item/advanced_circuit')

  event.create('factoryworks:processing_unit')
    .displayName('Processing Unit')
    .texture('factoryworks:item/processing_unit')

  // The eight material forms are the Pack's own items with Stand-in art from
  // `scripts/gen-standin-textures.py` (ADR-0109). `raw_uranium` is not `uranium_ore`, which is the
  // ore block.
  event.create('factoryworks:iron_plate')
    .displayName('Iron Plate')
    .texture('factoryworks:item/iron_plate')

  event.create('factoryworks:copper_plate')
    .displayName('Copper Plate')
    .texture('factoryworks:item/copper_plate')

  event.create('factoryworks:steel_plate')
    .displayName('Steel Plate')
    .texture('factoryworks:item/steel_plate')

  event.create('factoryworks:iron_gear_wheel')
    .displayName('Iron Gear Wheel')
    .texture('factoryworks:item/iron_gear_wheel')

  event.create('factoryworks:iron_stick')
    .displayName('Iron Stick')
    .texture('factoryworks:item/iron_stick')

  event.create('factoryworks:copper_cable')
    .displayName('Copper Cable')
    .texture('factoryworks:item/copper_cable')

  event.create('factoryworks:sulfur')
    .displayName('Sulfur')
    .texture('factoryworks:item/sulfur')

  event.create('factoryworks:raw_uranium')
    .displayName('Uranium Ore')
    .texture('factoryworks:item/raw_uranium')

  // Plastic authors for the same reason: it gates rung 2 (ADR-0025), and a rung-boundary row
  // authors rather than borrows.
  event.create('factoryworks:plastic_bar')
    .displayName('Plastic Bar')
    .texture('factoryworks:item/plastic_bar')

  // The oil chapter's two solids and the rocket's two intermediates.
  //
  // `solid-fuel` authors because GregTech ships no solid fuel item and borrowing `minecraft:coal`
  // would have the oil chapter PRINT an ore the pack mines, which ADR-0032's 1:1 stance forbids.
  // The rocket pair authors on Factorio fidelity (#87): the Rocket Silo's cycle consumes
  // Factorio's own intermediates, which revises #41's HDPE-and-circuits triple.
  //
  // `factoryworks:rocket_fuel` is NOT `gtceu:rocket_fuel`. This is Factorio's solid item, made
  // from solid fuel and light oil; GregTech's is the FLUID the GCyR rocket entity burns (#41).
  event.create('factoryworks:solid_fuel')
    .displayName('Solid Fuel')
    .texture('minecraft:item/charcoal')

  event.create('factoryworks:rocket_fuel')
    .displayName('Rocket Fuel')
    .texture('minecraft:item/blaze_powder')

  event.create('factoryworks:low_density_structure')
    .displayName('Low Density Structure')
    .texture('factoryworks:item/low_density_structure')

  // A crafting intermediate, not a placed power store: borrowing a mod's battery would put an
  // energy container inside a recipe that wants lead and acid.
  event.create('factoryworks:battery')
    .displayName('Battery')
    .texture('factoryworks:item/battery')

  // The engine units author because no installed mod ships Factorio's engine as one item
  // (ADR-0031).
  event.create('factoryworks:engine_unit')
    .displayName('Engine Unit')
    .texture('factoryworks:item/engine_unit')

  event.create('factoryworks:electric_engine_unit')
    .displayName('Electric Engine Unit')
    .texture('factoryworks:item/electric_engine_unit')

  // Sapros
  event.create('factoryworks:yumako_fresh')
    .displayName('Yumako')
    .texture('factoryworks:item/yumako');

  event.create('factoryworks:jellynut_fresh')
    .displayName('Jellynut')
    .texture('factoryworks:item/jellynut');

  event.create('factoryworks:iron_bacteria_fresh')
    .displayName('Iron Bacteria')
    .texture('factoryworks:item/iron_bacteria');

  event.create('factoryworks:copper_bacteria_fresh')
    .displayName('Copper Bacteria')
    .texture('factoryworks:item/copper_bacteria');
});
