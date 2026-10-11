StartupEvents.registry('item', event => {
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
