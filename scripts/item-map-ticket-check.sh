#!/usr/bin/env bash
# Assert every item-map row that waits on a ticket names an open one (#278).
#
# An `undecided` row's `ticket` and any row's `blocked_by` both say "the converter skips this
# until that ticket lands". Once the ticket closes, the skip is permanent and the pointer lies.
#
# Network-touching: needs an authenticated `gh`. Run it after closing a ticket, or after editing
# `data/pack/item-map.json` -- it is not part of any offline check.
set -uo pipefail

cd "$(dirname "$0")/.."

issues=$(gh issue list --state all --limit 2000 --json number,state,title) || {
  echo "UNREADABLE: gh issue list failed"
  exit 1
}

ISSUES="$issues" python3 - <<'PY'
import json, os, re, sys

issues = {i["number"]: i for i in json.loads(os.environ["ISSUES"])}
rows = json.load(open("data/pack/item-map.json"))["items"]

fail = checked = 0
for name, row in sorted(rows.items()):
    refs = []
    if row.get("status") == "undecided":
        refs.append(("ticket", row.get("ticket")))
    if "blocked_by" in row:
        refs.append(("blocked_by", row["blocked_by"]))
    for field, ticket in refs:
        checked += 1
        issue = issues.get(ticket) if isinstance(ticket, int) else None
        if issue is None:
            print(f"MISSING {name}: {field} {ticket!r} names no issue")
            fail = 1
        elif issue["state"] != "OPEN":
            print(f"CLOSED  {name}: {field} #{ticket} -- {issue['title']}")
            fail = 1

print(f"{checked} ticket pointers checked", "" if fail else "-- all open")

sys.exit(fail)
PY
