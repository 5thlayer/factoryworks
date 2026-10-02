# How modern tech mods size and draw their machine models

**Answer in one line: all three mods use vanilla texel density, 16 texels per block edge, and
none of them uses GeckoLib. Static geometry is vanilla block-model JSON, much of it authored in
Blockbench. A model with more surface gets a larger sheet (32x32, 64x64, 128x128), never a
higher density. Motion comes from a block-entity renderer that moves baked parts, or from
`.mcmeta` animated textures. Their art reads as polished for four reasons: a dark, desaturated
body; a few small, saturated accents drawn fullbright on a separate overlay layer; a shared kit
of ports and LEDs; and cube-grid detail that stays on the 1/16 grid.**

Issue: #568, part of map #567 (Pack 3D models authored in Blockbench).

## Sources

Each repository was read through the GitHub API at the default branch's HEAD on 2026-10-02.
Paths below are relative to each repository root.

| Mod | Branch, HEAD | Target |
|---|---|---|
| Mekanism (`mekanism/Mekanism`) | `1.21.x`, `bcd7a8bf59` | MC 1.21.1, mod 10.7.19 (`gradle.properties`) |
| Applied Energistics 2 (`AppliedEnergistics/Applied-Energistics-2`) | `main`, `ab5c88d175` | MC 26.2, NeoForge 26.2.0.88 (`gradle.properties`); latest release v26.1.13-beta |
| Refined Storage 2 (`refinedmods/refinedstorage2`) | `develop`, `8464d151bb` | multiloader NeoForge and Fabric, post-1.21.9 render API (`SubmitNodeCollector`); latest release v3.2.1 |

Texture sizes were read from the PNG headers. **Texel density** is texels per 16 model units,
which is texels per block edge. It was computed face by face for each model listed below: the
UV width, scaled to the sheet's size, divided by the geometric width of the face. The script is
throwaway and is not in the repo.

## Summary table

| | Mekanism | AE2 | RS2 |
|---|---|---|---|
| Texel density | 16 px per block | 16 px per block | 16 px per block |
| 1-block cube face | 16x16 | 16x16 | 16x16 |
| Sheet for a detailed single-block model | 32x32 | 32x32 or 64x64 | 16x16 per part |
| Sheet for a multi-block model | 32x32 for the 1x2x1 centrifuge; 64x64 plus two 32x32 sheets for the 3x2x3 Digital Miner; 128x128 for the 5-tall wind generator | none (no multi-block models) | none |
| Model format | vanilla JSON (Blockbench, `neoforge:composite`); OBJ only for MekaSuit and transmitters; Java `ModelPart` for the wind generator and turbine | vanilla JSON (Blockbench); custom baked models for cables, quartz glass and the crafting CPU | vanilla JSON, datagen-expanded per dye colour; custom geometry for the disk LEDs |
| Animation | BER rotates or translates baked JSON parts; `.mcmeta` frames on active faces | BER (inscriber presses, crank, held items); `.mcmeta` (interpolated light pulses) | `.mcmeta` on emissive cutouts; BER draws LED cubes |
| Emissive | `neoforge_data: {block_light: 15, sky_light: 15}` on overlay elements | the same `neoforge_data`, per face, plus `shade: false` | vanilla `light_emission: 15` on an overlay element |
| Connected textures | optional CTM-mod `.mcmeta` on multiblock casings | custom baked model (quartz glass, crafting CPU) | none |
| GeckoLib | no | no | no |

## Mekanism

### Simple machines: a cube with 16x16 faces and an animated front

Machines like the Enrichment Chamber use `models/block/machine.json`. It is a single
`[0,0,0]→[16,16,16]` cube, with one texture per face under `textures/block/enrichment_chamber/`,
each **16x16**. The blockstate swaps to an `_active` model whose only change is the front
texture: `front_active.png` is **32x224**, seven 32x32 frames at `frametime: 3`. So the
animated screen alone is drawn at 32 px per block, and the rest of the block stays at 16.

`machine_front_led.json` and `machine_up_led.json` add the glow. Each places a zero-thickness
plane 0.02 units in front of the face, textured with an LED overlay and marked
`"neoforge_data": {"block_light": 15, "sky_light": 15}`. The LED plane renders fullbright and
the body under it renders normally lit.

### Large and detailed models: Blockbench JSON, bigger sheets, same 16 px per block

The detailed models carry `"credit": "Made with love by CyanideX using Blockbench"`. Their UVs
are in Blockbench's 0-16 space, so on a 32x32 sheet one UV unit is 2 texels and on a 64x64
sheet it is 4.

- **Chemical Infuser** (1x1x1, 32 elements, `models/block/chemical_infuser.json`). The body
  sheet `textures/block/models/chemical_infuser.png` is **32x32**. Of 109 faces, 73 are at
  16 px per block and 27 are at 8, which are stretched trim. The ports use the shared 16x16
  `ports.png` and `ports_led.png`, all at 16.
- **Isotopic Centrifuge** (1x2x1, `models/block/isotopic_centrifuge.json`). It is a
  `neoforge:composite` model with a `cutout` child of 33 elements (3 of them fullbright) and a
  `translucent` child for the glass. Bounds are `[0..16, 0..32, 0..16]`. Its one body sheet is
  **32x32**, at 16 px per block.
- **Digital Miner** (3x2x3, 63 elements, bounds `x -16..32, y 0..32, z -16..32`). It uses a
  **64x64** body sheet, two **32x32** element sheets and the shared 16x16 `ports`, `ports_led`
  and `leds`. Of its 288 UV'd faces, 244 are at exactly 16 px per block. The rest are thin
  trim stretched to 2-12 px per block, four faces at 32, and two outliers. The extra
  blocks of the footprint are invisible **bounding blocks** (`BlockBounding`,
  `TileEntityBoundingBlock`, `AttributeHasBounding`). They forward interaction to the main
  block, which renders the whole model from its own position.
- **Wind Generator** (`src/generators/.../ModelWindGenerator.java`). It is a Java `ModelPart`
  model, `createLayerDefinition(128, 128, …)`, with `render/wind_generator.png` at **128x128**.
  The `addBox` units are texels, so it too is 16 px per block, on a mast 65 units tall. The
  blades are animated by rotating the part in a BER.

So Mekanism keeps the density fixed and grows the sheet. 32x32 is enough for a dense 1-block
model, and 64x64 plus helper sheets covers a 3x2x3 machine. The sheet is far smaller than the
footprint's surface because faces reuse and mirror UV regions.

### Animation

There are no animation files. `MekanismModelCache` registers small baked JSON parts
(`block/pigment_mixer_shaft`, `block/vibrator_shaft`, `block/liquifier_blade`). A BER renders
them each tick with a `PoseStack` transform. `RenderPigmentMixer` rotates the shaft
`gameTime * 5°` about the Y axis, and draws only while the machine is active. Its static model
leaves the shaft out in the active state, so the BER owns the part only while it moves.
Multiblock fluid contents (the boiler, the evaporation plant, the dynamic tank) are drawn by
BERs. OBJ, through NeoForge's loader, is used only for the MekaSuit, the Meka-Tool and the
transmitters (`models/entity/*.obj`, `models/transmitter_*.obj`).

### Multiblock casings and connected textures

Each casing (boiler, induction, SPS, dynamic tank, thermal evaporation, structural glass) is a
**16x16** block texture. Its `.mcmeta` carries a `"ctm"` section of type `CTM` that connects
to its own casing, valve and controller blocks. This only takes effect when the
ConnectedTexturesMod is installed, and otherwise the casings tile as plain blocks.

### Palette

The bodies are neutral, dark grey steel. `chemical_infuser.png` has a mean saturation of 0.00,
a mean value of 0.27 and 107 grey shades. `digital_miner.png` has saturation 0.01 and value
0.24, and the cube-machine face textures are stored as 8-bit grayscale PNGs. Colour lives only
in the small shared overlays. `ports_led.png` has saturation 0.53, and its colours (gold,
copper, teal) code what each port carries. The look comes from a dark body with many fine value
steps and a little bright, colour-coded, fullbright trim.

## Applied Energistics 2

- **The cubes** (controller, drive, interfaces) have **16x16** faces. The lights are separate
  16xN strips: `controller_lights.png` and `molecular_assembler_lights.png` are each **16x192**,
  twelve 16x16 frames. Their `.mcmeta` uses `interpolate: true` with a long `frametime` (25 for
  the controller), which makes a smooth pulse rather than a flicker.
- **Modelled blocks** were authored in Blockbench (credits such as
  `"Made with Blockbench by Sea_Kerman"`) and declare `texture_size`.
  - The Inscriber and the Charger each use one **64x64** sheet at 16 px per block, every face
    at 4 UV units, which is 16 texels for 16 units. The sheets are sparse: `inscriber.png` has
    only 1,440 of 4,096 texels opaque.
  - The Crystal Resonance Generator uses **32x32** frames (`crg.png` is 32x64, two
    interpolated frames), also at 16 px per block.
- **Emissive**: an overlay cube with `"neoforge_data": {"block_light": 15, "sky_light": 15}`
  on each face and `"shade": false` (`controller_block_lights.json`). The molecular assembler's
  lights use `render_type: tripwire` to sort correctly against its translucent glass.
- **Animation**: there are no animated models. `InscriberRenderer` builds the press plates from
  raw vertices on an `inscriber_inside` sprite. It moves them along an eased compress and
  decompress curve, and renders the items being processed. `CrankRenderer` spins the crank, and
  the molecular assembler and charger BERs draw the held item.
- **Multi-block structures** are assembled from 1x1 blocks whose appearance follows their
  state. The controller has column, inside-a and inside-b variants, each with a powered
  version. The crafting CPU uses custom baked models (`UnitBakedModel`, `MonitorBakedModel`,
  `LightBakedModel`) that draw a ring texture on the formed edges, plus a fullbright light layer
  "only drawn fullbright if the multiblock is currently powered" (the `LightBakedModel` doc
  comment). Quartz glass connects through its own `QuartzGlassModel`, so AE2 has no CTM
  dependency.
- **Palette**: `inscriber.png` has 27 colours, a mean saturation of 0.15 and a mean value of
  0.65. It pairs cool slate (`#413F54`, `#4D4D67`) with near-white panels (`#DEDFE3`,
  `#F2F2F2`). Saturated colour (fluix purple and cyan) is kept for the animated light layers.

## Refined Storage 2

- **All block textures are 16x16** at 16 px per block. Even the modelled `portable_grid`
  (6 elements, 8 textures) is at 16 throughout. RS2 has no large models.
- **The emissive layer is the design system.** Each device is an opaque body plus a "cutout"
  overlay cube carrying vanilla's element-level `"light_emission": 15`
  (`models/block/emissive_cutout.json`, `emissive_all_cutout.json`,
  `emissive_sides_cutout.json`). It uses no NeoForge extension, so the same model works on
  Fabric. Each device ships **16 dye-coloured cutouts** (`textures/block/<device>/cutouts/
  <colour>.png`) plus an `inactive` one. Datagen writes a model per colour into
  `src/generated/`. The controller's cutouts are **16x192**, twelve frames at `frametime: 2`.
  A cutout such as `grid/cutouts/blue.png` is small but vivid, with 90 opaque texels, 50
  colours, a saturation of 0.71 and a value of 0.89. The body under it is a muted grey.
- **Dynamic bits**: `DiskLedsCustomGeometryRenderer` draws 1x1x1-unit LED cubes, coloured by
  each disk's state, in the drive and the disk interface. `StorageMonitorBlockEntityRenderer`
  draws the displayed item. Nothing else moves.

## What makes the art read as modern

1. **The density stays vanilla.** Every face sits at 16 texels per block, so modded machines
   share a pixel grid with vanilla blocks and with each other. Where a single surface is drawn
   above 16, it is a 32x32 animated screen (Mekanism's active fronts), not the body.
2. **Detail comes from geometry on the 1/16 grid.** The models are many small boxes (32 to 63
   elements on Mekanism's large models) with insets, ports, vents and trim.
3. **The body is dark and desaturated, with small saturated accents.** Mekanism's body is
   saturation 0.00-0.01 at value 0.24-0.27. AE2 uses slate and white. RS2 uses grey.
   Colour carries meaning: what a port carries, which network, whether the machine is on.
4. **Light sits on its own fullbright overlay** (a plane or cube 0.02 units proud of the face,
   or a second element), with an interpolated or frame-animated pulse. This one technique is
   most of the "high-tech" read, and all three mods use it.
5. **A shared kit.** Mekanism's `ports.png`, `ports_led.png` and `leds.png` (all 16x16) appear
   on every detailed model, and RS2's cutout template is reused across every device. The
   repetition makes a set of machines look like one product line.
6. **Motion is restrained and cheap.** A rotating shaft, a press stroke, LED colour, an
   animated screen. Animation runs through BER transforms of baked parts and `.mcmeta`. None of
   the three ships skeletal or keyframe animation.

## Implications for FactoryWorks

These are facts to decide against; the decision itself belongs to map #567.

- At the shared 16 px per block, a model's bounding box in Blockbench pixels equals its
  footprint times 16. A 2x1x2 machine is 32x16x32 and a 3x3 machine 48 units on each
  horizontal side. Mekanism fits a 3x2x3 machine into a 64x64 sheet plus two 32x32 helpers by
  reusing UVs. A 3x3 GeckoLib model with little reuse would plausibly need **128x128**, the
  size Mekanism's wind generator uses. That figure is an extrapolation from the numbers above,
  not something any of the three mods ships.
- GeckoLib is not what makes these mods look polished. None of them uses it. Factorio's
  machines are mostly animated sprites (pistons, fans, smoke), and the same motion can come
  from a BER moving baked parts plus animated textures. A GeckoLib bone animation would be a
  choice made for authoring convenience, not a precedent these mods set.
- Factorio's art is a dark, desaturated body with coloured trim and working lights, which is
  already the palette these mods use. Their fullbright overlay technique transfers directly
  (`neoforge_data` light, or vanilla `light_emission`). GeckoLib's equivalent is its emissive
  ("glowmask") texture layer, which this research did not examine.
