-- Probe-only recipes: product amounts base Factorio lacks, to tell the output box rule apart (#520).
local function r(name, category, ingredients, results)
  data:extend{{type="recipe", name=name, categories={category}, energy_required=1, enabled=true, icon="__base__/graphics/icons/fluid/steam.png",
    ingredients=ingredients, results=results}}
end
local water = {{type="fluid", name="water", amount=10}}
r("probe-chem-100", "chemistry", water, {{type="fluid", name="steam", amount=100}})
r("probe-chem-60", "chemistry", water, {{type="fluid", name="steam", amount=60}})
r("probe-chem-two", "chemistry", water, {{type="fluid", name="steam", amount=10}, {type="fluid", name="light-oil", amount=10}})
r("probe-refinery-20", "oil-processing", water, {{type="fluid", name="steam", amount=20}})
r("probe-refinery-two", "oil-processing", water, {{type="fluid", name="steam", amount=20}, {type="fluid", name="light-oil", amount=20}})
