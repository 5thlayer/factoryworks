#!/usr/bin/env bash
# Assert every item-map row that waits on a ticket names an open one (#278), every `planned` or
# `blocked` mechanic-ledger section names at least one open issue in its `ticket` (#379), and every
# row of `data/pack/mechanic-obtainable.json` names an open one (#453).
#
# An `undecided` row's `ticket` and any row's `blocked_by` both say "the converter skips this
# until that ticket lands". Once the ticket closes, the skip is permanent and the pointer lies.
# A ledger `ticket` is prose that keeps closed refs as history, so one open ref is enough.
#
# Network-touching: needs an authenticated `gh`. Run it after closing a ticket, or after editing
# `data/pack/item-map.json`, `data/pack/mechanic-obtainable.json` or `docs/factorio-mechanics.md`
# -- it is not part of any offline check.
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

for row in json.load(open("data/pack/mechanic-obtainable.json"))["rows"]:
    checked += 1
    ticket = row.get("ticket")
    issue = issues.get(ticket) if isinstance(ticket, int) else None
    if issue is None:
        print(f"MISSING mechanic {row.get('id')}: ticket {ticket!r} names no issue")
        fail = 1
    elif issue["state"] != "OPEN":
        print(f"CLOSED  mechanic {row.get('id')}: ticket #{ticket} -- {issue['title']}")
        fail = 1

print(f"{checked} ticket pointers checked", "" if fail else "-- all open")

ledger_fail = sections = 0
text = open("docs/factorio-mechanics.md").read()
for body in re.split(r"^##+ ", text, flags=re.M)[1:]:
    name = body.split("\n", 1)[0].strip()
    verdict = re.search(r"^- \*\*verdict\*\*: `([^`]+)`", body, re.M)
    ticket = re.search(r"^- \*\*ticket\*\*:(.*(?:\n  .*)*)", body, re.M)
    if not verdict or verdict[1] not in ("planned", "blocked"):
        continue
    sections += 1
    refs = [int(n) for n in re.findall(r"#(\d+)", ticket[1])] if ticket else []
    if any(issues.get(n, {}).get("state") == "OPEN" for n in refs):
        continue
    ledger_fail = 1
    if not refs:
        print(f"MISSING {name} ({verdict[1]}): ticket names no issue")
    for n in refs:
        issue = issues.get(n)
        if issue is None:
            print(f"MISSING {name} ({verdict[1]}): ticket #{n} names no issue")
        else:
            print(f"CLOSED  {name} ({verdict[1]}): ticket #{n} -- {issue['title']}")

if ledger_fail:
    print("fix: change the section's verdict, or point it at a new open ticket -- never the reopened old one")
print(f"{sections} ledger ticket pointers checked", "" if ledger_fail else "-- all open")
sys.exit(fail or ledger_fail)
PY
