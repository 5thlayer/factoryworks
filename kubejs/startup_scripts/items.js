StartupEvents.registry('item', event => {
  // The circuit ladder, and plastic.
  //
  // First-party by ADR-0031's borrow/author rule: borrow an existing item unless the row sits on
  // a rung boundary or a mod's competing line would give a parallel escape. The circuits carry
  // progression and #62 removed GregTech's and Mekanism's competing lines; plastic gates rung 2
  // (ADR-0025). `copper-cable` is the counter-example and borrows
  // (`powergrid:wire`) -- see `data/pack/item-map.json`.
  //
  // THE SCIENCE PACKS ARE NOT HERE. They are Researchd research packs, not plain items:
  // `kubejs/server_scripts/researchd.js` declares them with `registerResearchPacks` under
  // `planetary_factory:` (an underscore, and not this pack's item namespace), and they are held
  // as `researchd:research_pack` carrying a `researchd:research_pack` data component. Registering
  // an item of the same name here would have shipped a second, inert pack the Lab cannot read.
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
  // `scripts/factorio-recipe-convert.py` into `kubejs/data/planetaryfactory/recipe/`.

  // Common
  //
  // The three tiers borrow Railcraft's circuit sprites, which are one PCB silhouette in four
  // board colours. That is what makes them right here and the GTCEu processors they replace
  // wrong: Factorio's green, red and blue circuits are the same object three times, and
  // `quantum_processor_assembly`, `wetware_processor_assembly` and `crystal_processor_assembly`
  // were three unrelated designs that never read as a ladder. Railcraft's fourth, yellow
  // `signal_circuit`, is deliberately unused -- Factorio has three tiers.
  //
  // A texture is referenced out of the installed jar, not copied into this repo. ADR-0026's
  // machines already borrow this way; #234 is writing down why that differs from committing a
  // derived sprite, and nothing here is redistributed.
  event.create('planetaryfactory:electronic_circuit')
    .displayName('Electronic Circuit')
    .texture('railcraft:item/receiver_circuit')

  event.create('planetaryfactory:advanced_circuit')
    .displayName('Advanced Circuit')
    .texture('railcraft:item/controller_circuit')

  event.create('planetaryfactory:processing_unit')
    .displayName('Processing Unit')
    .texture('railcraft:item/radio_circuit')

  // Plastic authors for the same reason: it gates rung 2 (ADR-0025), and a rung-boundary row
  // authors rather than borrows. Its recipe is the Chemical Plant's and arrives with #107.
  event.create('planetaryfactory:plastic_bar')
    .displayName('Plastic Bar')
    .texture('oritech:item/plastic_sheet')

  // The oil chapter's two solids and the rocket's two intermediates.
  //
  // `solid-fuel` authors because GregTech ships no solid fuel item and borrowing `minecraft:coal`
  // would have the oil chapter PRINT an ore the pack mines, which ADR-0032's 1:1 stance forbids.
  // The rocket pair authors on Factorio fidelity (#87): the Rocket Silo's cycle consumes
  // Factorio's own intermediates, which revises #41's HDPE-and-circuits triple.
  //
  // `planetaryfactory:rocket_fuel` is NOT `gtceu:rocket_fuel`. This is Factorio's solid item, made
  // from solid fuel and light oil; GregTech's is the FLUID the GCyR rocket entity burns (#41).
  event.create('planetaryfactory:solid_fuel')
    .displayName('Solid Fuel')
    .texture('minecraft:item/charcoal')

  event.create('planetaryfactory:rocket_fuel')
    .displayName('Rocket Fuel')
    .texture('minecraft:item/blaze_powder')

  // Paired with the Plastic Bar above on purpose: Oritech's `plastic_sheet` and
  // `reinforced_carbon_sheet` are one isometric silhouette in off-white and black, so the two
  // read as the same kind of thing. `carbon_fibre_strands` is the raw bundle and would not --
  // a Structure is a panel. Note Oritech's spelling is `fibre`; the `carbon_fiber_plate` this
  // replaces was a GTCEu name and never existed here under either spelling.
  event.create('planetaryfactory:low_density_structure')
    .displayName('Low Density Structure')
    .texture('oritech:item/reinforced_carbon_sheet')

  // Factorio's battery is a crafting INTERMEDIATE, not a placed power store: GregTech's batteries
  // are tiered chargeable hulls and Electro's capacitor is a different thing, so borrowing either
  // would put an EU container inside a recipe that wants lead and acid. No tier suffix -- there is
  // one battery, and a ladder that never arrives costs nothing to leave unnamed. The sprite is
  // still Oritech's, which is the distinction throughout this file: borrowing an item would put
  // that mod's behaviour in the recipe, borrowing its texture puts only the picture there.
  // `basic_battery` rather than `advanced_battery` because the two are a colour pair and this
  // item has no second tier to spend the purple one on.
  event.create('planetaryfactory:battery')
    .displayName('Battery')
    .texture('oritech:item/basic_battery')

  // The engine units author because no installed mod ships Factorio's engine as one item, and
  // they feed recipes the pack wants. ADR-0031's author case at its plainest: no borrow candidate
  // for the item. The two sprites are borrowed, and from different mods on purpose -- Railcraft's
  // `charge_motor` is plain grey steel and Oritech's `motor` is wound in copper, so the pair reads
  // mechanical-then-electric in the order Factorio's ladder does.
  event.create('planetaryfactory:engine_unit')
    .displayName('Engine Unit')
    .texture('railcraft:item/charge_motor')

  event.create('planetaryfactory:electric_engine_unit')
    .displayName('Electric Engine Unit')
    .texture('oritech:item/motor')

  // Sapros
  event.create('planetaryfactory:yumako_fresh')
    .displayName('Yumako')
    .texture('planetaryfactory:item/yumako');

  event.create('planetaryfactory:jellynut_fresh')
    .displayName('Jellynut')
    .texture('planetaryfactory:item/jellynut');

  event.create('planetaryfactory:iron_bacteria_fresh')
    .displayName('Iron Bacteria')
    .texture('planetaryfactory:item/iron_bacteria');

  event.create('planetaryfactory:copper_bacteria_fresh')
    .displayName('Copper Bacteria')
    .texture('planetaryfactory:item/copper_bacteria');
});
