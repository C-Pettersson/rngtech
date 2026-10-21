---
navigation:
  title: "Outcomes"
  parent: phase-array.md
  position: 3
  icon: rngtech:exotic_affix_forge
---

# Outcomes

Each Coil holds the pulse with its own **Hold Chance**, 92% before affixes. With `R` the Coils the operation needs:

| Outcome | When | Effect |
|---|---|---|
| Success | At least `R` Coils hold | The operation does what it says. |
| Overhold | Every Coil holds on a line longer than `R` | Success with a bonus, named on the operation. Each surplus Coil has an 8% chance to trigger it. |
| Partial | The pulse slips one or two Coils short of `R` | A weaker version, named on the operation. |
| Miss | The pulse slips earlier | Nothing changes. Only the FE and the Glyph are spent. |
| Fracture | Rolled before the Coils engage, 1% per point of Strain above the line's Tolerance | One random affix drops a tier. A Tier 1 affix is removed. |

The pulse always runs the whole usable line, so a nine-Coil line fires a three-Coil operation past the three it needs. A slip past `R` does no harm: it still counts as Success and shows amber, not red, on the line. Holding every Coil is what buys the Overhold roll.

RP is spent on Success, Overhold, and Partial, never on a Miss or a Fracture. A Glyph is spent on every outcome unless the line's Glyph Salvage returns it on a Miss.
