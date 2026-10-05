# Authoring a pack model in Blockbench

A machine's model is a Blockbench `.bbmodel` with linked PNGs, under `data/art/models/<machine>/`.
`scripts/build-model-assets.py` writes the game's model JSON and copies the textures from it; nobody
exports from Blockbench (ADR-0112). The art rules are ADR-0111's: unanimated vanilla JSON, 16 px per
block, status shown by a light.

This page is the setup. How to author a machine comes with the Steam Engine (#574).

## Setup

Blockbench 5.2.1. No plugin is needed.

Settings (Blockbench → Settings…), set once per install:

| Setting | Value | Why |
|---|---|---|
| Export → Embed Textures | off | textures are linked PNGs; anything embedded ships under the Pack's licence |
| Export → Minified Project Files | off | the `.bbmodel` diffs line by line |
| Export → Export Asset Paths | Relative | an absolute path names one person's disk |
| Defaults → Default Java Block/Item Version | 1.21.11 - 26.2 | "Latest" previews 26.3 fields the Pack's 26.1.2 drops |

Importing a texture from `data/art/models/` warns that it is not in a resource pack. Tick **Don't Show
Again** and **Ignore**: the generator copies it into one. **Change Path** would link into
`kubejs/assets/`, which the check refuses.

Never load a Factorio sprite, a render or a vendored texture into a `.bbmodel`, even as a reference
image: whatever it saves ships (ADR-0103). Look at references in a separate viewer.

## Units

16 Blockbench units are one block, and one unit is one texture pixel. A face 4 units wide shows 4
pixels of its texture: a texture squeezed onto a smaller face breaks the density, which is checked
by eye, not by the check (ADR-0112). Cubes sit on whole units, and rotate only in 22.5° steps on one axis.

A model's front is its north face (−Z); the blockstate turns it to face the way it was placed.

## Starting a model

Copy `data/art/models/template/template.bbmodel` to `data/art/models/<machine>/<machine>.bbmodel` and
open the copy. Save before importing a texture, so its path is relative to where the model lives.

The template holds the **status light**: a 4×4×1 cube named `status_light`, on the north wall, its
north face showing the whole 4×4 lamp, light emission 15, shade off, no texture on the face against
the wall. Its texture's **Variable** is `status`. Move it to where the machine's panel is, standing 1
unit out of the body so the body's face does not cover it.

## The parts kit

`data/art/models/kit/` holds textures every machine shares, each drawn once:

| File | Shows |
|---|---|
| `status_light_working.png` | green: working |
| `status_light_blocked.png` | yellow: blocked on input or output |
| `status_light_no_power.png` | red: no power or low power |

A model links only `status_light_working`; the generator writes one child model per status that sets
`#status` to each lamp.
