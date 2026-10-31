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

The pulse always runs the whole usable line, so a nine-Coil line fires a three-Coil operation past the three it needs. A slip past `R` does no harm: it still counts as Success. Only a line where every Coil holds rolls for Overhold.

## Reading a slip

The colour of a slip tells you if it cost you the operation. The line on the screen keeps the colours until the next item goes in.

| You see | Where the slip is | Result |
|---|---|---|
| Every node green, no slip | No Coil slipped | Success, and an Overhold roll if the line is longer than `R` |
| Amber node, amber dust on the Coil, a soft chime | After the first `R` Coils | Success. The slip only cost the Overhold roll. |
| Red node, red dust and sparks on the Coil, a falling tone | Inside the first `R` Coils | Partial or Miss |

For example, Reforge needs 3 Coils. On a 7-Coil line, a slip at Coil 5 shows amber and the Reforge succeeds. A slip at Coil 3 shows red, and you get only the Partial result.

RP is spent on Success, Overhold, and Partial, never on a Miss or a Fracture. A Glyph is spent on every outcome unless the line's Glyph Salvage returns it on a Miss.
