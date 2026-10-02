---
status: accepted
---

# A pack model's source is its .bbmodel, and a Python port of Blockbench's exporter writes it

Each machine's model is a Blockbench `.bbmodel` under `data/art/models/<machine>/`, with its textures
beside it as PNGs the `.bbmodel` links by relative path. Textures every machine shares, such as the
status lights, live once under `data/art/models/kit/`. `scripts/build-model-assets.py` reads every
`.bbmodel`, writes the vanilla model JSON to `kubejs/assets/factoryworks/models/block/` and copies the
textures to `textures/block/<machine>/`. Each model gets a base model whose status light names a
`#status` texture, and three child models that set it green, yellow or red (ADR-0111).

The exported files are generated output, so `--check` regenerates them and compares byte for byte. It
also refuses a `.bbmodel` whose format is not "5.0", that embeds a texture or a reference image, that
names a path outside `data/art/models/`, or whose texture breaks 16 px per block.

`.bbmodel` files are kept out of the packwiz upload, and `data/art/models/` is the Pack's own CC BY
art in `REUSE.toml`.

**Considered.** Exporting in Blockbench and checking structure only (#570): rejected, since Blockbench
has no headless export, so the committed JSON would be hand-exported output, and its export settings
(the Java block version, absolute paths) would be traps every author has to set. Textures embedded in
the `.bbmodel`, Blockbench's default: rejected, since a shared kit texture would be copied into every
model, texture edits would need Blockbench, and anything embedded ships, a reference image included.

**Consequences.** A Blockbench upgrade that changes the format or the exporter needs the port updated;
the format pin makes that fail loudly rather than drift.
