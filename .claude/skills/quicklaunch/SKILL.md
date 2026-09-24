---
name: quicklaunch
description: Install the planetaryfactory_core jar and launch the pack straight into the most recent save, for the user to play or check a change in-world. Use only when the user asks for it -- "quicklaunch", "quick launch", "launch the game", "install the jar and launch" -- never on your own initiative to check a change, since it opens a window on the user's screen.
---

# Quick launch

From the repo root, run:

```bash
QUICKLAUNCH_LOG=<scratchpad>/launch.log scripts/quicklaunch.sh
```

Pass a save's name as the first argument to open that save instead of the most recent one. Do
**not** pass `--headless` or anything else: the user is at the display and wants the window.

The script refuses if a client is already running, installs the jar, launches the game detached as
kc00l, and waits for the log's `launching as` line. It exits non-zero on each of those failures.
Report its last line in one line: the jar was installed and which save was opened. If it exits
non-zero, report its error instead and do not retry: a running client is the user's to close.
