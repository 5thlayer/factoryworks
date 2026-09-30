# Filming the showcase scenes

Three GameTests build a working factory where they are run, and leave it running after they pass (#538):

- `factoryworks_showcase:assembly_line`: copper plates to cable, cable and iron plates to circuits, on belts and feeders.
- `factoryworks_showcase:steam_power`: Offshore Pump, Boiler fed coal from a belt, two Steam Engines, and poles powering two Electric Furnaces smelting iron.
- `factoryworks_showcase:oil`: Pumpjack, Oil Refinery on basic oil processing, and a Chemical Plant making plastic.

The assembly line and the oil scene are powered by a creative pole; the steam scene powers itself.

## Running one

1. Install the core jar with `./gradlew :factoryworks_core:installToPack` while the game is closed.
2. Launch with `scripts/launch.py` (or `skillworks:quicklaunch`), which passes `-Dneoforge.enableGameTest=true`. From the CurseForge app, add that flag to the instance's JVM arguments instead; without it the scenes are not registered.
3. Create a creative world, fly to about y 200 so the sky hides the terrain, and run `/test run factoryworks_showcase:<scene>`.
4. `/test clearall` removes the scenes.

Each scene draws on chests stocked with 27 stacks, which last about half an hour of running. Press F1 to hide the HUD while recording.
