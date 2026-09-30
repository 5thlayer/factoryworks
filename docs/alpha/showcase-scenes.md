# Filming the showcase scenes

`/factoryworks showcase <scene>` builds a working factory where you stand, and it keeps running (#538):

- `assembly_line`: copper plates to cable, cable and iron plates to circuits, on belts and feeders.
- `steam_power`: Offshore Pump, Boiler fed coal from a belt, two Steam Engines, and poles powering two Electric Furnaces smelting iron.
- `oil`: Pumpjack, Oil Refinery on basic oil processing, and a Chemical Plant making plastic.

The assembly line and the oil scene are powered by a creative pole; the steam scene powers itself.

## Running one

1. Install the core jar with `./gradlew :factoryworks_core:installToPack` while the game is closed.
2. In a creative world, fly to about y 200 so the sky hides the terrain, and run `/factoryworks showcase <scene>`. It needs cheats (permission level 2).
3. The scene's 24x16 floor goes one block under your feet, with its north-west corner where you stand; everything in its 24x8x16 box is replaced. Fly off it and film.

The same scenes are GameTests under `factoryworks_showcase:*`, which the check run never selects; swap the run's `--tests` selector for that one to check they still make their product.

Each scene draws on chests stocked with 27 stacks, which last about half an hour of running. Press F1 to hide the HUD while recording.
