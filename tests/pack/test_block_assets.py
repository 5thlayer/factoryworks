#!/usr/bin/env python3
"""Assert every block `factoryworks_core` registers has the pack-side files it needs (#254).

For each block the mod registers through a `DeferredRegister.createBlocks`: a blockstate whose
every variant names a model, every model's parent chain and textures resolving, a lang key, an item
model where the block has an item, and a loot table unless the block is registered with
`noLootTable`. Each missing hop fails quietly in a
running game -- a black-and-magenta cube, a raw key as the block's name, a block breaking into
nothing -- and `scripts/check-client-assets.py` cannot see a file that is simply absent.

The block list is read out of every register's source, and each tier ladder's names out of its
enum, so a new block is walked without anyone adding it here. A registration whose name this file cannot
work out fails rather than being skipped. A block with an item of its own name must drop it.

What a block's states are, and which judgement its art has to pass, stay in each machine's own
file: this walks whatever variants the blockstate declares.

Usage: tests/pack/test_block_assets.py
"""

import json
import pathlib
import re
import sys
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import test_item_map  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAVA = ROOT / "mod/src/main/java"
KUBEJS = ROOT / "kubejs"
ASSETS = KUBEJS / "assets/factoryworks"
LOOT = KUBEJS / "data/factoryworks/loot_table/blocks"
NAMESPACE = "factoryworks"

# Blocks with no item of their own, by the rule that places them. Each pattern must match a
# registered block and none it matches may have an item definition, so an entry cannot go stale.
NO_ITEM = {
    r".*_part": "a machine's part, placed and dropped by its anchor's item",
    r".*_ore": "placed by worldgen and paid out by OreMining, never picked up (ADR-0041)",
    r"oil_well": "placed by worldgen and never broken (ADR-0081)",
    r"wreck_hull(_stairs|_slab)?|wreck_window|cargo_hold": "the wreck's blocks, never held by a player (ADR-0107)",
    r"(superheated_)?steam": "a fluid's world block; no fluid here has a bucket (ADR-0037)",
}


def java_file(simple_name):
    found = [p for p in JAVA.rglob(f"{simple_name}.java")]
    if len(found) != 1:
        raise LookupError(f"{len(found)} files named {simple_name}.java")
    return found[0]


def split_args(text):
    args, depth, current = [], 0, []
    for char in text:
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        if char == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
        else:
            current.append(char)
    if "".join(current).strip():
        args.append("".join(current).strip())
    return args


def call_body(source, open_paren):
    depth = 0
    for i in range(open_paren, len(source)):
        if source[i] == "(":
            depth += 1
        elif source[i] == ")":
            depth -= 1
            if depth == 0:
                return source[open_paren + 1:i]
    raise ValueError("unbalanced parentheses")


class JavaEnum:
    """Evaluates an enum's name-building methods, e.g. `serializedName() + "_furnace"`."""

    def __init__(self, name):
        source = java_file(name).read_text(encoding="utf-8")
        self.name = name
        self.constants = {}
        for match in re.finditer(r"^\s{4}([A-Z][A-Z0-9_]*)\s*([(,;])", source, re.MULTILINE):
            args = call_body(source, match.end() - 1) if match.group(2) == "(" else ""
            self.constants[match.group(1)] = split_args(args)
        ctor = re.search(rf"\b{name}\(([^)]*)\)\s*\{{(.*?)\n    }}", source, re.DOTALL)
        params = [p.split()[-1].lstrip(".") for p in split_args(ctor.group(1))] if ctor else []
        self.fields = {}
        for field, param in re.findall(r"this\.(\w+)\s*=\s*(\w+);", ctor.group(2) if ctor else ""):
            if param in params:
                self.fields[field] = params.index(param)
        self.methods = dict(re.findall(r"\b(\w+)\(\)\s*\{\s*return ([^;]+);", source))

    def call(self, constant, method):
        if method not in self.methods:
            raise LookupError(f"{self.name} has no one-line {method}()")
        return "".join(self.term(constant, t.strip()) for t in self.methods[method].split("+"))

    def term(self, constant, term):
        if term.startswith('"') and term.endswith('"'):
            return term[1:-1]
        if term == "name()":
            return constant
        if term == "name().toLowerCase(Locale.ROOT)":
            return constant.lower()
        if term.endswith("()"):
            return self.call(constant, term[:-2])
        if term in self.fields:
            arg = self.constants[constant][self.fields[term]]
            if arg.startswith('"') and arg.endswith('"'):
                return arg[1:-1]
        raise LookupError(f"cannot evaluate {term!r} in {self.name}")


def registered_blocks():
    """Every block id path the mod registers, and the registrations it could not name."""
    blocks, unknown = {}, []
    for path in sorted(JAVA.rglob("*.java")):
        source = path.read_text(encoding="utf-8")
        for register in re.findall(r"(\w+)\s*=\s*DeferredRegister\.createBlocks\(", source):
            for match in re.finditer(rf"\b{register}\.registerBlock\(", source):
                body = call_body(source, match.end() - 1)
                arg = split_args(body)[0]
                try:
                    for name in resolve_name(arg, source[:match.start()], source):
                        blocks[name] = (body, source)
                except LookupError as error:
                    unknown.append(f"{path.name}: {register}.registerBlock({arg}, ...): {error}")
    return blocks, unknown


def resolve_name(arg, before, source):
    if arg.startswith('"') and arg.endswith('"'):
        return [arg[1:-1]]
    constant = re.fullmatch(r"(\w+)\.([A-Z_]+)", arg)
    if constant:
        found = re.search(rf'\b{constant.group(2)}\s*=\s*"([a-z0-9_]+)"',
                          java_file(constant.group(1)).read_text(encoding="utf-8"))
        if found:
            return [found.group(1)]
    ladder = re.fullmatch(r"(\w+)\.(\w+)\(\)", arg)
    if ladder:
        loops = re.findall(rf"for \((\w+) {ladder.group(1)} : \1\.values\(\)\)", before)
        if loops:
            enum = JavaEnum(loops[-1])
            return [enum.call(c, ladder.group(2)) for c in enum.constants]
    if re.fullmatch(r"[a-z]\w*", arg):
        helper = re.findall(rf"(\w+)\([^)]*\bString {arg}\b", before)
        if helper:
            calls = re.findall(rf"\b{helper[-1]}\(\s*([^,)]+)", source)
            names = [c[1:-1] for c in calls if c.startswith('"') and c.endswith('"')]
            if names and len(names) == len(calls) - 1:  # the one left over is its own signature
                return names
    raise LookupError("no rule names this registration")


class Assets:
    """Model and texture lookup across the pack, the installed jars and the client jar."""

    def __init__(self):
        self.jar_files = set()
        for jar in sorted((ROOT / "mods").glob("*.jar")):
            with zipfile.ZipFile(jar) as archive:
                self.jar_files.update(n for n in archive.namelist() if n.startswith("assets/"))
        self.vanilla = set()
        if test_item_map.VANILLA.exists():
            with zipfile.ZipFile(test_item_map.VANILLA) as archive:
                self.vanilla = {n for n in archive.namelist() if n.startswith("assets/minecraft/")}

    def exists(self, relative):
        """`relative` is `assets/<ns>/...`; vanilla is trusted when the client jar is absent."""
        namespace = relative.split("/")[1]
        if (KUBEJS / relative).is_file() or relative in self.jar_files:
            return True
        if namespace == "minecraft":
            return relative in self.vanilla if self.vanilla else True
        return False

    def read(self, relative):
        path = KUBEJS / relative
        if path.is_file():
            return json.loads(path.read_text(encoding="utf-8"))
        if relative.startswith("assets/minecraft/") and relative in self.vanilla:
            with zipfile.ZipFile(test_item_map.VANILLA) as archive:
                return json.loads(archive.read(relative))
        return None

    @staticmethod
    def split(reference):
        namespace, _, path = reference.partition(":")
        return (namespace, path) if path else ("minecraft", namespace)

    def walk_model(self, reference, failures, seen):
        if reference.startswith("builtin/") or reference in seen:
            return
        seen.add(reference)
        namespace, path = self.split(reference)
        relative = f"assets/{namespace}/models/{path}.json"
        if not self.exists(relative):
            failures.append(f"model {reference} is not on disk or in any jar")
            return
        model = self.read(relative)
        if model is None:
            return  # a foreign model inside a jar: present, not ours to walk
        if "parent" in model:
            self.walk_model(model["parent"], failures, seen)
        for slot, texture in (model.get("textures") or {}).items():
            if isinstance(texture, dict):
                texture = texture.get("sprite", "")
            if texture.startswith("#"):
                continue
            t_namespace, t_path = self.split(texture)
            if not self.exists(f"assets/{t_namespace}/textures/{t_path}.png"):
                failures.append(f"model {reference}'s {slot} texture {texture} is not on disk "
                                "or in any jar")


def blockstate_models(blockstate):
    entries = []
    for definition in (blockstate.get("variants") or {}).values():
        entries += definition if isinstance(definition, list) else [definition]
    for part in blockstate.get("multipart") or []:
        apply = part.get("apply", [])
        entries += apply if isinstance(apply, list) else [apply]
    return [entry["model"] for entry in entries]


def drops(table):
    names, stack = set(), list(table.get("pools", []))
    while stack:
        node = stack.pop()
        if isinstance(node, dict):
            if node.get("type") == "minecraft:item":
                names.add(node.get("name"))
            stack += [v for v in node.values() if isinstance(v, (dict, list))]
        elif isinstance(node, list):
            stack += node
    return names


def no_loot_table(body, source):
    if ".noLootTable()" in strip_comments(body):
        return True
    for helper in re.findall(r"\b(\w+)\(props\)", body):
        method = re.search(rf"\b{helper}\(BlockBehaviour\.Properties \w+\)\s*\{{(.*?)\n    }}",
                           source, re.DOTALL)
        if method and ".noLootTable()" in strip_comments(method.group(1)):
            return True
    constructed = re.search(r"\bnew (\w+)\(|(\w+)::new", body)
    if constructed:
        cls = constructed.group(1) or constructed.group(2)
        try:
            return ".noLootTable()" in strip_comments(java_file(cls).read_text(encoding="utf-8"))
        except LookupError:
            return False
    return False


def strip_comments(source):
    return re.sub(r"//[^\n]*|/\*.*?\*/", "", source, flags=re.DOTALL)


def check_block(name, body, source, assets, lang, has_item, failures):
    def fail(message):
        failures.append(f"{name}: {message}")

    blockstate = assets.read(f"assets/{NAMESPACE}/blockstates/{name}.json")
    if blockstate is None:
        fail("no blockstate, so it renders as the missing model")
    else:
        models = blockstate_models(blockstate)
        if not models:
            fail("its blockstate names no model")
        hops, seen = [], set()
        for model in models:
            assets.walk_model(model, hops, seen)
        failures.extend(f"{name}: {hop}" for hop in hops)

    if not lang.get(f"block.{NAMESPACE}.{name}", "").strip():
        fail(f"no lang entry block.{NAMESPACE}.{name}, so it shows its raw key")

    if has_item:
        hops = []
        assets.walk_model(f"{NAMESPACE}:item/{name}", hops, set())
        failures.extend(f"{name} (item): {hop}" for hop in hops)

    loot = LOOT / f"{name}.json"
    if no_loot_table(body, source):
        if loot.is_file() and drops(json.loads(loot.read_text())):
            fail("is registered with noLootTable, so what its loot table drops is never dropped")
    elif not loot.is_file():
        fail("no loot table, so it breaks into nothing")
    elif has_item and drops(json.loads(loot.read_text())) != {f"{NAMESPACE}:{name}"}:
        fail(f"its loot table does not drop exactly {NAMESPACE}:{name}")


def main():
    failures = []
    blocks, unknown = registered_blocks()
    failures += unknown
    if len(blocks) < 20:
        failures.append(f"only {len(blocks)} blocks parsed out of the mod -- has registration moved?")

    for pattern, reason in NO_ITEM.items():
        matched = [b for b in blocks if re.fullmatch(pattern, b)]
        if not matched:
            failures.append(f"NO_ITEM {pattern!r} ({reason}) matches no registered block")
        for block in matched:
            if (ASSETS / f"items/{block}.json").is_file():
                failures.append(f"NO_ITEM {pattern!r} covers {block}, which has an item")

    assets = Assets()
    lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for name, (body, source) in sorted(blocks.items()):
        has_item = not any(re.fullmatch(p, name) for p in NO_ITEM)
        check_block(name, body, source, assets, lang, has_item, failures)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(blocks)} registered blocks: every blockstate, model, texture, lang key, "
          "item model and loot table resolves")
    return 0


if __name__ == "__main__":
    sys.exit(main())
