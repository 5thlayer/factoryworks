local cases = {
  -- unpowered: inserter-fed input limit
  {kind="in", machine="assembling-machine-1", recipe="iron-gear-wheel", feed={["iron-plate"]=200}},
  {kind="in", machine="assembling-machine-2", recipe="engine-unit", feed={["steel-plate"]=50,["iron-gear-wheel"]=50,["pipe"]=100}},
  {kind="in", machine="assembling-machine-3", recipe="copper-cable", feed={["copper-plate"]=200}},
  {kind="in", machine="assembling-machine-2", recipe="electronic-circuit", feed={["iron-plate"]=100,["copper-cable"]=300}},
  {kind="in", machine="assembling-machine-3", recipe="iron-gear-wheel", feed={["iron-plate"]=200}},
  -- powered, script-filled: output threshold
  {kind="out", machine="assembling-machine-2", recipe="iron-gear-wheel", fill={["iron-plate"]=200}},
  {kind="out", machine="assembling-machine-2", recipe="copper-cable", fill={["copper-plate"]=200}},
  {kind="out", machine="assembling-machine-3", recipe="copper-cable", fill={["copper-plate"]=200}},
  {kind="out", machine="assembling-machine-1", recipe="pipe", fill={["iron-plate"]=200}},
  {kind="out", machine="assembling-machine-3", recipe="engine-unit", fill={["steel-plate"]=50,["iron-gear-wheel"]=50,["pipe"]=100}},
}
local function st(i) storage.c=storage.c or {}; storage.c[i]=storage.c[i] or {}; return storage.c[i] end
local function setup()
  local s = game.surfaces[1]
  game.forces.player.research_all_technologies(); game.forces.player.inserter_stack_size_bonus=0; game.forces.player.bulk_inserter_capacity_bonus=0
  local tiles = {}
  for x=-10,160 do for y=-20,20 do tiles[#tiles+1]={name="lab-dark-1",position={x,y}} end end
  s.request_to_generate_chunks({70,0},6); s.force_generate_chunk_requests()
  s.set_tiles(tiles)
  for _,e in pairs(s.find_entities_filtered{area={{-10,-20},{160,20}}}) do if e.valid and e.name~="character" then e.destroy() end end
  local eei = s.create_entity{name="electric-energy-interface",position={78,6},force="player"}
  eei.power_production = 1e9; eei.electric_buffer_size = 1e10
  for i,c in ipairs(cases) do
    local x = i*12
    if prototypes.recipe[c.recipe] then
    local a = s.create_entity{name=c.machine,position={x+0.5,0.5},force="player"}
    a.set_recipe(c.recipe)
    st(i).a = a
    if c.kind=="in" then
      local ch = s.create_entity{name="iron-chest",position={x+0.5,-2.5},force="player"}
      for n,k in pairs(c.feed) do ch.insert{name=n,count=k} end
      local ins = s.create_entity{name="burner-inserter",position={x+0.5,-1.5},force="player",direction=defines.direction.south}
      if not (ins.drop_target and ins.drop_target==a) then ins.direction=defines.direction.north end
      ins.insert{name="coal",count=50}
      st(i).ins = ins
    else
      st(i).ins_res = {}
      local inv = a.get_inventory(defines.inventory.crafter_input)
      for n,k in pairs(c.fill) do st(i).ins_res[n]=inv.insert{name=n,count=k} end
    end
    end
  end
  -- poles along y=4
  storage.poles=0 for x=78,160,6 do local p=s.create_entity{name="medium-electric-pole",position={x+0.5,3.5},force="player"} if p then storage.poles=storage.poles+1 end end
end
script.on_init(setup)
script.on_nth_tick(600, function(ev)
  for i,c0 in ipairs(cases) do local c=storage.c[i] if c0.kind=='out' and c and c.a then local inv=c.a.get_inventory(defines.inventory.crafter_input) for n,k in pairs(c0.fill) do inv.insert{name=n,count=k} end end end
  local out = {}
  for i,c0 in ipairs(cases) do
    local c = setmetatable(st(i),{__index=c0})
    local line = "tick="..ev.tick.." "..c.kind.." "..c.machine.." "..c.recipe
    if c.a and c.a.valid then
      local rp = prototypes.recipe[c.recipe]
      line = line.." energy="..rp.energy.." speed="..c.a.crafting_speed
      if c.ins then line = line.." ins_dir="..c.ins.direction.." drop="..tostring(c.ins.drop_target and c.ins.drop_target.name).." pick="..tostring(c.ins.pickup_target and c.ins.pickup_target.name).." ins_status="..(function(v) for k,n in pairs(defines.entity_status) do if n==v then return k end end end)(c.ins.status).." held="..tostring(c.ins.held_stack.valid_for_read and c.ins.held_stack.count) end
      local ii = c.a.get_inventory(defines.inventory.crafter_input)
      for n,k in pairs(ii.get_contents()) do line=line.." in:"..k.name.."="..k.count end
      local oi = c.a.get_inventory(defines.inventory.crafter_output)
      for n,k in pairs(oi.get_contents()) do line=line.." out:"..k.name.."="..k.count end
      line = line.." crafted="..c.a.products_finished.." progress="..c.a.crafting_progress.." status="..(function(v) for k,n in pairs(defines.entity_status) do if n==v then return k end end end)(c.a.status).." poles="..tostring(storage.poles).." net="..tostring(c.a.electric_network_id)
      if c.ins_res then for n,k in pairs(c.ins_res) do line=line.." scriptinserted:"..n.."="..k end end
      for _,ing in pairs(rp.ingredients) do line=line.." ing:"..ing.name.."x"..ing.amount end
      for _,pr in pairs(rp.products) do line=line.." prod:"..pr.name.."x"..(pr.amount or -1) end
    else line = line.." MISSING" end
    out[#out+1]=line
  end
  helpers.write_file("overload_probe.txt", table.concat(out,"\n").."\n", true)
end)
