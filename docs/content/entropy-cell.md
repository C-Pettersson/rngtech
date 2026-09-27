# Entropy Cell

Status: Planned


Planned resource id: `rngtech:entropy_cell`

## Summary

The Entropy Cell is a risky generator that converts order into FE.

Order can mean stored charge, remaining Refinement Potential, stable rarity structure, or future catalyst quality. The machine pays well when the player feeds it valuable structured items, so the UI must make the cost explicit before anything is consumed.

The Entropy Cell is not a [Battery Cell](battery-cells.md). It is a planned generator block.

## Design Role

The Entropy Cell should be a pressure valve for progression systems.

It lets players turn unwanted structured value into energy. A bad Rare machine part, a charged cell with poor traits, or a refinement target with no future use can become power instead of dead inventory.

Primary design goals:

- Convert meaningful progression value into FE.
- Create a high-risk energy path for players who understand rarity and refinement.
- Give poor rolls and spent targets a second use.
- Prevent accidental loss of valuable items.

## Input Contract

The Entropy Cell should only consume explicit inputs placed in its input slot or selected through its own UI.

Eligible input categories can include:

| Input Category | Planned Cost | Notes |
|---|---|---|
| Charged energy items | Stored FE and optional efficiency loss | Safest input. Mostly works as conversion or discharge. |
| Refinable machine stacks | Refinement Potential, rolled affixes, or rarity value | Requires strong preview text before consumption. |
| Machine parts | Part rarity, modifiers, or future durability | Good sink for bad rolls. |
| Battery cells | Stored FE plus cell rarity value | Must not confuse this with normal battery use. |
| Catalysts | Future catalyst quality or charge | Useful if refinement creates spent catalyst chains. |

The machine should not pull items from adjacent inventories unless the player enables automation for a specific input class.

## Power Model

The Entropy Cell should price each consumed property separately.

Suggested calculation shape:

```text
stored charge value = extracted FE * discharge efficiency
rarity value = rarity tier value * item category multiplier
affix value = sum(eligible rolled affix values)
refinement value = remaining Refinement Potential * target multiplier
effective FE = stored charge value + rarity value + affix value + refinement value
```

`ENERGY_GENERATION` should affect FE/t. `EFFICIENCY` should affect total FE retained from consumed order. `STABILITY` should reduce backlash, waste, or forced cooldowns.

## Consumption Modes

The Entropy Cell should support explicit modes so players can decide what gets destroyed.

Planned modes:

| Mode | Behavior |
|---|---|
| Discharge | Drains stored FE from compatible items without destroying the item. |
| Strip | Removes eligible rolled affixes or Refinement Potential and outputs FE. |
| Collapse | Consumes the item stack and converts its full eligible value into FE. |

Collapse should require a confirmation or a clear one-click preview because it can destroy valuable items.

## Risk and Backlash

Entropy generation should feel unstable.

Possible risk hooks:

- Low `STABILITY` wastes part of the calculated FE.
- Collapsing Rare targets can create backlash residue.
- Removing multiple affixes in one operation can force a cooldown.
- Low-efficiency cells can leak FE instead of storing it in the internal buffer.

Backlash should be readable and recoverable. The machine should not silently delete high-value targets.

## Gear

The Entropy Cell should use Gear slots that make destructive conversion safer and more deliberate. The first implementation should expose the Entropy Core, Stabilizer Ring, and Discharge Matrix slots. Containment Lens should wait until Strip mode can remove affixes or Refinement Potential.

| Gear Slot | Required? | Role | Stat and Behavior Hooks |
|---|---|---|---|
| Entropy Core | Yes | Defines the base conversion rate and which consumption modes the machine can use. | `ENERGY_GENERATION`, `PROCESSING_LEVEL`, mode access |
| Stabilizer Ring | No | Reduces backlash, waste, and cooldowns from high-value inputs. | `STABILITY`, cooldown reduction, backlash resistance |
| Discharge Matrix | No | Improves safe FE extraction from charged items and battery cells. | `EFFICIENCY`, `ENERGY_TRANSFER`, Discharge mode output |
| Containment Lens | No | Focuses destructive conversion on affixes, rarity value, or Refinement Potential. | Strip value, affix preservation, preview accuracy |

The Entropy Core should gate destructive modes. Early cores can support Discharge only. Midgame cores can unlock Strip. Collapse should require a high-stage core plus enough stabilization that the UI can give a reliable output preview.

## Automation

The Entropy Cell should use standard capabilities:

- Item capability for the selected input slot and residue output.
- Energy capability for FE output.

Automation should default to conservative behavior. External item insertion should only feed the safest configured mode, such as Discharge, unless the player enables destructive modes in the UI.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine implicit | Identity can carry entropy, containment, or backlash behavior. |
| Machine prefix | Can affect buffer size, stability, processing level, or safe-mode input handling. |
| Machine suffix | Can affect `ENERGY_GENERATION`, `EFFICIENCY`, conversion rate, and cooldown behavior. |
| Machine enchant | Reserved for behavior such as preserving the base item after stripping affixes. |
| Machine part modifiers | Containment cores, stabilizers, and lenses can affect conversion value and risk. |

## Implementation Notes

No Entropy Cell block, block entity, recipe type, menu, screen, loot table, or destructive conversion rule is currently registered.

A future implementation should build a preview-first UI before adding Collapse behavior. The preview should show the exact item cost, FE output, residue, and whether the input item survives.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Affix Forge](affix-forge.md)
- [Battery Cells](battery-cells.md)
- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Progression](../systems/progression.md)
- [Rarity](../reference/rarity.md)
