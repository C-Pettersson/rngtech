---
wiki:
  category: Refinement
  icon: rngtech:exotic_affix_forge
  ids:
    - rngtech:exotic_affix_forge
    - rngtech:phase_emitter
    - rngtech:phase_coil
    - rngtech:glyph
---

# Phase Array

{{ infobox(
    variants=["rngtech:exotic_affix_forge", "rngtech:phase_emitter", "rngtech:phase_coil"],
    fields={
        "Type": "Refinement multiblock",
        "Stages": "8",
        "Power": "FE consumer",
        "Gear": "9 × {{ item('rngtech:exotic_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Phase Array** is the endgame refinement bench. Where the [Affix Forge](affix-forge.md) rolls dice for you, the Array lets you choose: raise this affix, remove that one, add the affix you want, swap one for another, or move an affix across from a donor item. It works on any refinable item of any stage, and it spends only a little [Refinement Potential](rarity-and-affixes.md#refinement-potential), so an item is done when you decide it is. It does not accept [Job Chips](work-orders.md).

Every operation is a **pulse**. The line charges FE, you fire it, and the pulse crosses the Coils one by one. Each Coil holds it or lets it slip. Reach far enough and the operation happens; slip early and nothing changes. Three things make you stop before your RP runs out:

- **FE** that grows with how good the item already is.
- **Strain**, one point per pulse, which raises the chance of a Fracture.
- **Glyphs**, which the deeper operations spend and which no recipe makes.

### In one minute

1. Place the **Cradle**, three to nine **Coils** in a straight row from one side of it, and the **Emitter** at the far end. The line lights up when it is complete.
2. Open the Cradle and put a **Glyph** in the seat of its tier. A Tier I Glyph opens the first three positions.
3. Put the item in the Cradle, pick an operation, and read its odds.
4. **Charge**, then **Fire**. Watch the Coils engage. Take the item out of the output.

## Obtaining

### Crafting

{{ crafting("rngtech:exotic_affix_forge", "rngtech:phase_emitter", "rngtech:phase_coil") }}

The Emitter and Coils roll rarity and affixes when crafted, like other Stage 8 parts, and keep them when mined. The Cradle does not roll. All three recipes need the Nullite material to be enabled.

### Glyphs

{{ item('rngtech:glyph', 'Glyphs') }} have no recipe. They come in tiers I to V.

Most Glyphs come from [Work Orders](work-orders.md#rewards). A job that pays a Glyph pays one of its own tier, so Tier V jobs are the only source of Tier V Glyphs.

| Source | Glyph |
|---|---|
| A [Work Order](work-orders.md#rewards) that pays a Glyph | The job's tier, I to V |
| Waking a [Dormant Machine](dormant-machines.md) | Tier I to IV by site |

Your modpack can also give Glyphs as quest rewards.

A Glyph with no tier counts as Tier I, so one from a quest or a command works.

A Glyph has two uses, and they compete: **seat** it in the Cradle to open the positions of its tier, or **spend** it on a deep operation. The tier is a floor for both: a Tier III Glyph opens any position up to 7 and pays for any operation that asks for Tier III or less.

### Breaking

Mine any part with a pickaxe. The Cradle drops itself and everything in its slots, including the Battery Cells. The seated Glyphs drop with the Cradle; a Coil carries nothing but its own affixes.

## Usage

### Building the line {#building}

Place the Cradle, then three to nine Coils in a straight horizontal row from any side of it, and the Emitter at the far end. The Cradle finds the line on its own and lights the parts while it is complete; break any part and the line goes dark, and the charge waits for it to re-form. Nothing is needed above, below, or beside the row, and no block may sit between the parts.

Each Coil has a **position**, counted from the Emitter: position 1 is the Coil next to the Emitter, position 9 the Coil next to the Cradle on the longest line. When a part is placed next to the line it turns to follow the line, whichever way you face.

### Opening positions {#positions}

The Cradle has five Glyph seats, one per tier, in a column under the output slot. A Glyph of that tier or higher in a seat opens every position of its tier, and positions open from the Emitter inward: the first closed one ends the usable line. Take a Glyph out and its positions close again. Each position asks for a seat:

| Position | Seat it needs | Operations it opens |
|---:|---:|---|
| 1 to 3 | I | Reforge |
| 4 | II | Attune |
| 5 | II | Resonate, Excise |
| 6 | III | Imprint |
| 7 | III | Transmute, Temper |
| 8 | IV | Exalt |
| 9 | V | Graft |

Shift-click a Glyph into the Cradle and it goes to its own seat if that seat is empty, else to the Glyph slot. While the line uses a position, its Coil shows the tier it runs at on its sides; a closed position shows nothing. Coils carry no tier of their own, so they are interchangeable, and a line grows by placing another Coil at the Emitter end.

Opening every position takes one Glyph of each tier. The Tier V is also what one Graft spends, so seating it ties up a Graft to make every later Graft possible.

### Running a pulse

1. Put the item in the Cradle's **target** slot. For a Graft, put the donor in the **donor** slot too. For an operation that spends a Glyph, put one in the **Glyph** slot.
2. Pick an operation from the row of marks. Operations that work on one affix or one empty slot ask you to click it in the list under the marks; Imprint then lists the affixes it can add, and Graft lists the donor's affixes.
3. Hover the mark for the FE, the Glyph, the RP, and the odds of every outcome for this item on this line. The chamber's hover shows the item's Strain and the Fracture chance it adds.
4. Click **Charge**. The line fills from FE on any side of the Cradle, the Emitter, or a Coil, and from the Battery Cells in the Cradle's sockets, up to the line's Charge Rate. A dip in supply pauses the charge; it never fails. The charge stays if you close the screen or switch operations, and a costlier operation asks for more.
5. Click **Fire**. The Coils engage from the Emitter inward, one every 0.4 seconds with a tone that climbs the line. The first Coil that slips stops the pulse. It flashes red or amber, the lit Coils go dark one by one, and the outcome applies. [Reading a slip](#reading-a-slip) tells you what the colour means.

The item hangs in the Cradle's bore while it sits in the chamber and is locked while the pulse runs. When the pulse ends it moves to the **output** slot; take it out before the next one. The readout under the line names the outcome and what it did until the next item goes in. The Cradle remembers the affix you selected. Put the item back in the chamber and the same affix is selected again, so you can Resonate one affix many times in a row.

### Outcomes

Each Coil holds the pulse with its own **Hold Chance**, 92% before affixes. With `R` the Coils the operation needs:

| Outcome | When | Effect |
|---|---|---|
| Success | At least `R` Coils hold | The operation does what it says. |
| Overhold | Every Coil holds on a line longer than `R` | Success with a bonus, named on the operation. Each surplus Coil has an 8% chance to trigger it. |
| Partial | The pulse slips one or two Coils short of `R` | A weaker version, named on the operation. |
| Miss | The pulse slips earlier | Nothing changes. Only the FE and the Glyph are spent. |
| Fracture | Rolled before the Coils engage, 1% per point of Strain above the line's Tolerance | One random affix drops a tier. A Tier 1 affix is removed. |

The pulse always runs the whole usable line, so a nine-Coil line fires a three-Coil operation past the three it needs. A slip past `R` does no harm: it still counts as Success. Only a line where every Coil holds rolls for Overhold.

#### Reading a slip

The colour of a slip tells you if it cost you the operation. The line on the screen keeps the colours until the next item goes in.

| You see | Where the slip is | Result |
|---|---|---|
| Every node green, no slip | No Coil slipped | Success, and an Overhold roll if the line is longer than `R` |
| Amber node, amber dust on the Coil, a soft chime | After the first `R` Coils | Success. The slip only cost the Overhold roll. |
| Red node, red dust and sparks on the Coil, a falling tone | Inside the first `R` Coils | Partial or Miss |

For example, Reforge needs 3 Coils. On a 7-Coil line, a slip at Coil 5 shows amber and the Reforge succeeds. A slip at Coil 3 shows red, and you get only the Partial result.

RP is spent on Success, Overhold, and Partial, never on a Miss or a Fracture. A Glyph is spent on every outcome unless the line's Glyph Salvage returns it on a Miss.

### Operations

{{ processing("phase_array", columns=["action", "coils", "glyph_tier", "base_fe", "refinement_potential_cost", "partial", "overhold"], always=True) }}

| Operation | On Success | You choose |
|---|---|---|
| **Reforge** | Rerolls rarity, affixes, and RP as if the item were newly crafted. Strain stays. Works at 0 RP. | Nothing |
| **Attune** | Rerolls every affix's value within its tier. | Nothing |
| **Resonate** | Raises the selected affix one tier, up to Tier 6. | One affix |
| **Excise** | Removes the selected affix. | One affix |
| **Imprint** | Adds the affix you choose to the empty slot you choose. The tier rolls with the item's normal weights. | An empty slot, then an affix |
| **Transmute** | Swaps the selected affix for another legal affix of the same slot, keeping its tier. | One affix |
| **Temper** | Sets Strain to 0. | Nothing |
| **Exalt** | Raises the selected Tier 6 affix to Tier 7. Stage 8 Exotic items only. | One Tier 6 affix |
| **Graft** | Moves the selected donor affix onto the empty slot you choose, keeping its tier and value. The donor is destroyed. | A donor affix and an empty slot |

Imprint and Graft need an empty slot, and rarity sets how many an item has: a Magic item holds one prefix and one suffix, a Rare item three of each, and nothing but Reforge changes rarity. Fixed identity affixes and implicits are never selectable. An affix the item could not roll on its own cannot be Imprinted or Grafted onto it, and neither can one from a modifier group the item already carries; the donor list dims such affixes and says why. A pack can retune, remove, or add operations: they are `rngtech:phase_array` recipes.

### Costs and Strain

A pulse's FE is the operation's base, times a factor for the item's stage, times `1.5` for every `3.5` points of the item's **Quality**, the sum of its affix tiers. The line's FE Thrift takes a percentage off.

| Item stage | Stage factor |
|---:|---:|
| 0 to 2 | 0.1 |
| 3 to 4 | 0.25 |
| 5 to 6 | 0.5 |
| 7 | 1 |
| 8 | 1.5 |

One Resonate on a Stage 8 item, from a 20 kFE/t supply:

| Item | Quality | FE | Charge time |
|---|---:|---:|---|
| Fresh Rare Exotic part | 12 | 12 MFE | 30 seconds |
| All affixes Tier 4 | 24 | 48 MFE | 2 minutes |
| Six Tier 6 affixes | 36 | 194 MFE | 8 minutes |
| Six Tier 7 affixes | 42 | 389 MFE | 16 minutes |

A Stage 5 item pays a third of that at the same Quality, and a Stage 2 item a fifteenth. The last pulses on an item cost thirty times the first.

Every pulse adds one **Strain** to the item, on every outcome, unless the line's Strain Mercy rolls. Strain shows on the item's tooltip with the Fracture chance it adds, and as marks around the chamber. Strain up to the line's Strain Tolerance adds no Fracture chance; every point past it adds 1%. Only Temper removes Strain, and Excise's Overhold takes one point off. A Reforge keeps it, so a reforged item is a fresh roll on a worn body.

### Gear

The Cradle has nine Battery Cell sockets. Any Battery Cell fits. While the line charges, each cell drains into the charge at its own output rate, so a full set carries a pulse to a line with no cable. Drained cells can be pulled out of the Cradle's bottom face.

### Line stats

The Emitter and Coils roll their own affixes, and the line reads them. Hover a Coil's node for its Hold Chance, Second Hold, Overhold Chance, and affixes, and the Emitter's node for the line's totals and its affixes.

| Stat | From | What it does |
|---|---|---|
| Charge Capacity, Charge Rate | Every part, summed | How much FE the line holds and how fast it takes it |
| Primed Charge Rate | Emitter | Extra Charge Rate, paid only while a Battery Cell sits in the Cradle's sockets |
| Hold Chance, Second Hold | Each Coil | Whether that Coil holds the pulse, and whether a slip there is rolled again |
| Overhold Chance | Each Coil, plus the Emitter | The chance a surplus hold triggers Overhold |
| Partial Window | The Emitter | How many Coils short of `R` still count as Partial |
| Strain Tolerance | Every part, summed | Strain that adds no Fracture chance |
| FE Thrift, FE Recovery | Every part, summed | A percentage off every pulse, and a percentage of a slipped pulse's FE returned to the charge |
| Glyph Salvage, Strain Mercy | Every part, summed | The chance a Glyph returns on a Miss, and the chance a pulse adds no Strain |

A Coil or the Emitter is itself a target: pull it out, refine it in the Cradle, and put it back. Pulling position 7 out drops the line to six Coils, so refine it in a second line or a friend's.

### Automation

| Side | Behavior |
|---|---|
| Any side of the Cradle, the Emitter, or a Coil | Accepts FE while the line charges, for example from a [Universal Cable](universal-cable.md). |
| Bottom of the Cradle | Hands out Battery Cells once they are drained, for example to a hopper. Nothing goes in this way. |

Nothing else moves by automation: the target, the Glyph, the donor, and the output are placed and taken by hand, and every pulse is a click.

## Screen

The Cradle has one screen. Its item area at the top right reads like a furnace: the item goes in, the Glyph is the fuel, and the result comes out at the end of the arrow.

- **The line** runs across the top from the Emitter at the left, one node per position, into the chamber. A lit node is a usable position. A dim node with an amber edge is closed, and its hover names the seat it waits on. After a pulse, the held nodes turn green and the slipped one turns red or amber. See [Reading a slip](#reading-a-slip).
- **The chamber** at the end of the line holds the target item. Marks around it show the item's Strain.
- **The Glyph slot** sits under the chamber, where a furnace keeps its fuel. **The donor slot** sits beside it. The donor slot is dim unless the operation is Graft.
- **The conduit** is the arrow from the chamber to the output. It fills with the charge, and its head lights when the charge is full. Hover it for the FE held and needed, the rate FE arrives, and the time to full.
- **The button** under the conduit reads Charge, Stop, Fire, or Pulse, whichever the line is ready for.
- **The output slot** is the large slot at the end of the arrow. The item goes there when the pulse ends.
- **The readout** under the line names the last outcome. With an item in the chamber, it also shows the item's Strain and RP.
- **The operation marks** run in a row above the list. A dim mark cannot run, and its hover tells you why: the missing position, Glyph, selection, or RP.
- **The list** shows what the chosen operation can act on: the item's affixes, its empty slots, the affixes it can take, or the donor's affixes.
- **The Glyph seats** run down the right side under the output slot, numbered I to V. A numeral lights while its seat holds a Glyph.
- **The cell sockets** sit beside your inventory, nine of them. Each ring dims as its cell empties.
- **Three lights** at the top right show blocked, waiting for input, and live.

Hover anything for its exact values, and an empty slot for what goes in it. In the world, Jade on the Cradle names the chosen operation and, with no line, the first block it lacks. With JEI installed, the operations are listed under the Phase Array category.

## Tips

- Read the odds before every pulse. When the Fracture chance passes what you will accept, Temper the item or call it done.
- Reforge is cheap on a bad item and nearly useless on a good one: a Rare with high Quality is a rare roll, and the item's Strain runs out before your luck does.
- Seat a Tier I Glyph first; it gives you Reforge on three Coils, and every seat after that opens a deeper operation.
- Nine Coils against a seven-Coil operation buy Overhold, not Success. Build long for the bonus, not for safety.
- A bank of charged Battery Cells in the sockets finishes a pulse that a cable alone would take minutes to fill.

## Data values

{{ data_values() }}

## See also

- [Affix Forge](affix-forge.md): the early catalyst-based station and how refinement works.
- [Rarity and Affixes](rarity-and-affixes.md)
- [Dormant Machines](dormant-machines.md)

{{ navbox() }}
