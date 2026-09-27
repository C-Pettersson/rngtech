# Material Usage Audit

Status: Implemented

This reference classifies registered material families by their consumers in recipes and machine components.

Method: scan `src/main/resources/data/rngtech/recipe/**/*.json` for ingredient-side material references after ignoring recipe outputs and enablement conditions. Lifecycle-only paths are ore/crusher/furnace material conversion recipes, default direct-alloy production recipes, material-form recipes, component recycling recipes, and simple Metal Press casing recipes where present. Hard-coded material systems in Java were checked separately with `rg` for renamed material ids.

## No Actual Use

These families are registered and have lifecycle support, but no current machine, component, frame, part, alloy, or other non-lifecycle recipe consumes them.

| Material | Current lifecycle support | Missing real consumer |
|---|---|---|
| `zinc` | Ore/worldgen, raw/crushed/dust/ingot forms, tags, and non-casing material-form recipes | The removed stage-3 alloy path leaves Zinc without an alloy or component consumer. |

## Used In Progression

These families have at least one non-lifecycle recipe consumer. Alloy ingredient use counts here because it gates downstream progression materials.

| Material | Primary non-lifecycle use |
|---|---|
| `iron` | Early frames, casings, circuits, tools, calibration patterns, Steel and Invar alloy inputs |
| `copper` | Early conductors, Bus Bars, circuits, connectors, Bronze and Arclite alloy inputs |
| `bronze` | Stage 3 chassis, machine parts, Heat Cores, Fuel Boxes, reactor parts, tools |
| `tin` | Bronze and Aethergold alloy inputs |
| `gold` | Battery chassis, solar parts, refinement materials, Sparksteel alloy input |
| `steel` | Stage 4 machines and parts, molds, Reinforced Machine Frame, Tungstensteel alloy input |
| `invar` | Invar Battery Cell and Steel Crusher Chassis inputs |
| `nickel` | Invar alloy input |
| `lead` | Lead Battery Cell, Lead Furnace Chassis, Resonance Calibrator Chassis, Nullite alloy input |
| `silver` | Sparksteel, Arclite, and Aethergold alloy inputs |
| `aluminum` | Advanced Machine Frame, Stage 5 machines and parts, modular tool parts |
| `sparksteel` | Battery Cell, Battery Chassis, Heat Core, connectors, solar parts, routed tool rod |
| `osmium` | Required Melter Fluid Pump, using Osmium Gears and Plates |
| `titanium` | Stage 6 machines and parts, Elite Circuit Blank, late-game machine inputs |
| `arclite` | Battery Cell, Battery Chassis, connectors, Elite Circuit Blank, Titanium chassis/head inputs |
| `tungsten` | Tungstensteel alloy input |
| `tungstensteel` | Exotic Machine Frame, exotic machines and parts, collapse and high-end tool parts |
| `nullite` | Battery Cell, Battery Chassis, Resonance Calibrator, high-end tool parts |
| `aethergold` | Solar Array Extender |
| `platinum` | Nullite alloy input |
| `netherite` | Netherite material forms and Ultimate Circuit inputs |
| `naquadah` | Default `rngtech:materials/exotic/*` tag target for Exotic batteries, chassis, machines, tools, and high-end forms, plus Ultimate Circuit inputs |

## Coverage limits

Zinc has catalog and conversion support but no progression consumer identified by this audit. Catalog support alone does not establish a progression use. Recheck ingredient references when recipes change.
