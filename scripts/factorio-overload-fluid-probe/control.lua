-- in: unpowered, fed by infinity pipes; the input fluidbox stops at the fluid's limit.
-- out: powered, fed by infinity pipes and script-filled items; the output fluidbox stops at its threshold.
local cases = {
  {kind="in", machine="assembling-machine-2", recipe="concrete"},
  {kind="in", machine="assembling-machine-3", recipe="concrete"},
  {kind="in", machine="chemical-plant", recipe="plastic-bar"},
  {kind="in", machine="chemical-plant", recipe="heavy-oil-cracking"},
  {kind="in", machine="chemical-plant", recipe="lubricant"},
  {kind="in", machine="chemical-plant", recipe="sulfuric-acid"},
  {kind="in", machine="oil-refinery", recipe="basic-oil-processing"},
  {kind="in", machine="oil-refinery", recipe="advanced-oil-processing"},
  {kind="in", machine="chemical-plant", recipe="lubricant", beacons=true},
  {kind="in", machine="chemical-plant", recipe="heavy-oil-cracking", beacons=true},
  {kind="in", machine="assembling-machine-3", recipe="concrete", beacons=true},
  {kind="in", machine="oil-refinery", recipe="coal-liquefaction"},
  {kind="in", machine="oil-refinery", recipe="basic-oil-processing", beacons=true},
  {kind="in", machine="chemical-plant", recipe="light-oil-cracking"},
  {kind="in", machine="chemical-plant", recipe="solid-fuel-from-light-oil"},
  {kind="in", machine="assembling-machine-2", recipe="water-barrel"},
  {kind="in", machine="assembling-machine-2", recipe="empty-water-barrel"},
  {kind="in", machine="assembling-machine-3", recipe="empty-crude-oil-barrel"},
  {kind="in", machine="chemical-plant", recipe="probe-chem-100"},
  {kind="in", machine="chemical-plant", recipe="probe-chem-60"},
  {kind="in", machine="chemical-plant", recipe="probe-chem-two"},
  {kind="in", machine="oil-refinery", recipe="probe-refinery-20"},
  {kind="in", machine="oil-refinery", recipe="probe-refinery-two"},
  {kind="out", machine="chemical-plant", recipe="lubricant"},
  {kind="out", machine="chemical-plant", recipe="heavy-oil-cracking"},
  {kind="out", machine="chemical-plant", recipe="sulfuric-acid", fill={["sulfur"]=200,["iron-plate"]=200}},
  {kind="out", machine="oil-refinery", recipe="basic-oil-processing"},
  {kind="out", machine="oil-refinery", recipe="advanced-oil-processing"},
}
local function fname(f) if not f or not f.fluid then return nil end return type(f.fluid)=="string" and f.fluid or f.fluid.name end
local function proto(a,j) local p=a.get_fluid_box_prototype(j) if p and p.object_name==nil then p=p[1] end return p end
local function status(v) for k,n in pairs(defines.entity_status) do if n==v then return k end end end
local function setup()
  local s = game.surfaces[1]
  game.forces.player.research_all_technologies()
  s.request_to_generate_chunks({100,0},8); s.force_generate_chunk_requests()
  local tiles = {}
  for x=-10,200 do for y=-20,20 do tiles[#tiles+1]={name="lab-dark-1",position={x,y}} end end
  s.set_tiles(tiles)
  for _,e in pairs(s.find_entities_filtered{area={{-10,-20},{200,20}}}) do if e.valid and e.name~="character" then e.destroy() end end
  storage.c = {}
  for i,c in ipairs(cases) do
    local a = s.create_entity{name=c.machine,position={i*14+0.5,0.5},force="player"}
    a.set_recipe(c.recipe)
    for j=1,a.fluids_count do
      local pr = proto(a,j)
      if pr and pr.production_type == "input" then
        local want = a.get_fluid_filter(j)
        for _,pc in pairs(a.get_fluid_box_pipe_connections(j) or {}) do
          local p = s.create_entity{name="infinity-pipe",position=pc.target_position,force="player"}
          if p and fname(want) then p.set_infinity_pipe_filter{name=fname(want),percentage=1,mode="at-least"} end
        end
      end
    end
    if c.fill then local inv=a.get_inventory(defines.inventory.crafter_input) for n,k in pairs(c.fill) do inv.insert{name=n,count=k} end end
    if c.beacons then
      for _,d in pairs({{-3,-3},{-3,0},{-3,3},{3,-3},{3,0},{3,3}}) do
        local b = s.create_entity{name="beacon",position={i*14+0.5+d[1],0.5+d[2]},force="player"}
        if b then b.get_module_inventory().insert{name="speed-module-3",count=2} end
      end
    end
    if c.kind=="out" or c.beacons then
      local e = s.create_entity{name="electric-energy-interface",position={i*14+0.5,8.5},force="player"}
      e.power_production=1e9; e.electric_buffer_size=1e10
      s.create_entity{name="substation",position={i*14+4,4},force="player"}
    end
    storage.c[i] = a
  end
end
script.on_init(setup)
script.on_nth_tick(600, function(ev)
  local out = {}
  for i,c in ipairs(cases) do
    local a = storage.c[i]
    if c.fill then local inv=a.get_inventory(defines.inventory.crafter_input) for n,k in pairs(c.fill) do inv.insert{name=n,count=k} end end
    local rp = prototypes.recipe[c.recipe]
    local line = "tick="..ev.tick.." "..c.kind.." "..c.machine.." "..c.recipe.." energy="..rp.energy.." speed="..a.crafting_speed
    for j=1,a.fluids_count do
      local f, pr, fl = a.get_fluid(j), proto(a,j), a.get_fluid_filter(j)
      line = line.." box"..j..":"..tostring(pr and pr.production_type)..":"..tostring(fname(fl)).."@"..tostring(pr and pr.get_volume()).."="..(f and f.amount or 0).."/"..a.get_fluid_capacity(j)
    end
    line = line.." crafted="..a.products_finished.." status="..status(a.status)
    for _,ing in pairs(rp.ingredients) do line=line.." ing:"..ing.name.."x"..ing.amount end
    for _,pr in pairs(rp.products) do line=line.." prod:"..pr.name.."x"..(pr.amount or -1) end
    out[#out+1]=line
  end
  helpers.write_file("overload_fluid_probe.txt", table.concat(out,"\n").."\n", true)
end)
