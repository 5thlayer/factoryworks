#!/bin/bash
set -euo pipefail

# .claude/skills links into this submodule; without it no skill loads.
git -C "$CLAUDE_PROJECT_DIR" submodule update --init --recursive
