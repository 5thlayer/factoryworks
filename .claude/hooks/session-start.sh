#!/bin/bash
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# .claude/skills links into this submodule; without it no skill loads.
git -C "$CLAUDE_PROJECT_DIR" submodule update --init --recursive
