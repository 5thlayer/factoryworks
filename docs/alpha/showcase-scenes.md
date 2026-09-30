# Filming the showcase scenes

Three GameTests build a working factory where they are run, and leave it running after they pass (#538):

- `factoryworks_showcase:assembly_line`: copper plates to cable, cable and iron plates to circuits, on belts and feeders.
- `factoryworks_showcase:steam_power`: Offshore Pump, Boiler fed coal from a belt, two Steam Engines, and poles powering two Electric Furnaces smelting iron.
- `factoryworks_showcase:oil`: Pumpjack, Oil Refinery on basic oil processing, and a Chemical Plant making plastic.

The assembly line and the oil scene are powered by a creative pole; the steam scene powers itself.

## Running one

The scenes are datapack test instances, so any launch of the pack has them; NeoForge's `-Dneoforge.enableGameTest` is ignored outside a dev run and is not needed.

1. Install the core jar with `./gradlew :factoryworks_core:installToPack` while the game is closed.
2. Create a creative world. `/test run` places a scene on the highest block in your column, three blocks south of you, not at your feet, so put one block at about y 200, stand on it, and run `/test run factoryworks_showcase:<scene>`. The sky then hides the terrain.
3. `/test clearall` removes the scenes.

Each scene draws on chests stocked with 27 stacks, which last about half an hour of running. Press F1 to hide the HUD while recording.
