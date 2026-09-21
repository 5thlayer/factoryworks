---
name: quicklaunch
description: Install the planetaryfactory_core jar and launch the pack straight into the most recent save, for the user to play or check a change in-world. Use only when the user asks for it -- "quicklaunch", "quick launch", "launch the game", "install the jar and launch" -- never on your own initiative to check a change, since it opens a window on the user's screen.
---

# Quick launch

Run these steps in order from the repo root.

1. **Refuse if a client is already running.** Replacing the jar under a live game breaks class
   loading, and the failure looks like a code bug.

   The game runs on CurseForge's bundled Java (`JAVA` in `scripts/launch.py`), not on the
   Homebrew JDK that Gradle uses, so match that binary and nothing else:

   ```bash
   pgrep -fl "curseforge/Install/java/java-runtime-epsilon/Contents/Home/bin/java"
   ```

   If this prints anything, stop and ask the user to close the game.

2. **Install the jar.**

   ```bash
   ./gradlew :planetaryfactory_core:installToPack -q
   ```

   If the build fails, report the error and do not launch.

3. **Pick the most recent save**, by when its `level.dat` last changed:

   ```bash
   basename "$(dirname "$(ls -t saves/*/level.dat | head -1)")"
   ```

   If there is no save, launch without `--quickPlaySingleplayer` and say so.

4. **Launch in the background**, with the log going to the scratchpad:

   ```bash
   python3 scripts/launch.py --quickPlaySingleplayer "<save>" > <scratchpad>/launch.log 2>&1
   ```

   Run it with `run_in_background`, because the game blocks until it is closed. Do **not** pass
   `--headless`: the user is at the display and wants the window. `launch.py` has no `--help`, and
   any argument it does not recognise is passed on to the game.

5. **Report in one line**: the jar was installed, and which save was opened.
