#!/usr/bin/env bash
# Install the planetaryfactory_core jar and launch the pack into the most recent save, as kc00l.
#
#   scripts/quicklaunch.sh [save name]
#
# The game is detached and its output goes to $QUICKLAUNCH_LOG (default: a temp file, printed).
set -euo pipefail
cd "$(dirname "$0")/.."

# Swapping the jar under a live client breaks class loading, and looks like a code bug. The game
# runs on CurseForge's bundled Java, not the JDK Gradle uses, so match that binary only.
if pgrep -fl "curseforge/Install/java/java-runtime-epsilon/Contents/Home/bin/java"; then
    echo "quicklaunch: a client is already running; close the game first." >&2
    exit 1
fi

./gradlew :planetaryfactory_core:installToPack -q

save="${1:-}"
if [[ -z "$save" ]]; then
    latest="$(ls -t saves/*/level.dat 2>/dev/null | head -1 || true)"
    [[ -n "$latest" ]] && save="$(basename "$(dirname "$latest")")"
fi

log="${QUICKLAUNCH_LOG:-$(mktemp -t quicklaunch).log}"
args=()
if [[ -n "$save" ]]; then
    args=(--quickPlaySingleplayer "$save")
else
    echo "quicklaunch: no save found; launching to the menu." >&2
fi

# Without both, launch.py guesses from usercache.json and may fall back to "Dev", a player the save
# has never seen, so the starting kit and opening quests fire again.
PF_PLAYER_NAME=kc00l PF_PLAYER_UUID=f033ed5f-f0aa-46b9-b818-c246e0b7aa0b \
    nohup python3 scripts/launch.py ${args[@]+"${args[@]}"} > "$log" 2>&1 &

for _ in $(seq 1 30); do
    player="$(grep -m1 -o 'launching as [^ ]*' "$log" 2>/dev/null || true)"
    [[ -n "$player" ]] && break
    sleep 1
done

echo "jar installed; ${save:+opening \"$save\" }${player:-player line not seen yet}; log: $log"
if [[ -n "$player" && "$player" != "launching as kc00l" ]]; then
    echo "quicklaunch: expected kc00l, got '${player#launching as }'" >&2
    exit 1
fi
