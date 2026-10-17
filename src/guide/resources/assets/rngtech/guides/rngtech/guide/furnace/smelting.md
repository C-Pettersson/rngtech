---
navigation:
  title: "Smelting"
  parent: furnace.md
  position: 0
  icon: rngtech:furnace
---

# Smelting

Put items in the top input slot. Each lane heats up and starts smelting once it reaches the recipe's temperature, and leaks heat while it has nothing to smelt. The Furnace pays FE, or fuel, for the heat it puts in, including what its walls lose, so a hotter lane costs more per smelt. See [Heat](rngtech:heat.md) for how lanes heat and the overheat slider on the Process tab.

Each metal smelts at its own pace: low-melting Lead and Tin in half the time of Iron, high-melting Osmium, Titanium, and Tungsten in two and a half to four times as long. The FE per smelt does not change.

Heat decides your progress. Unmodified electric chassis reach about:

| Heat Core | Max temperature | Unlocks |
|---|---:|---|
| Stage 0 Furnace (no core) | 800 | Iron and Copper |
| <ItemLink id="rngtech:iron_heat_core" /> | 800 | Iron and Copper |
| <ItemLink id="rngtech:copper_heat_core" /> | 1,000 | Bronze ingredients and other early metals |
| <ItemLink id="rngtech:bronze_heat_core" /> | 1,200 | Mid-tier metals |
| <ItemLink id="rngtech:steel_heat_core" /> | 1,400 | Stage 4 metals |
| <ItemLink id="rngtech:sparksteel_heat_core" /> | 1,600 | High-heat metals |
| <ItemLink id="rngtech:titanium_heat_core" /> | 1,800 | Late-game metals |
| <ItemLink id="rngtech:exotic_heat_core" /> | 2,200 | Every metal recipe |
| <ItemLink id="rngtech:aluminum_heat_core" /> | 850 | Low-heat Stage 5 sidegrade that heats up much faster |

Vanilla ores and raw metals go through the same heat gates as RNGTech metals, so you cannot skip heat progression by smelting them directly.

Crushed material smelts straight into ingots, which is handy for simple hopper lines, but it takes three times as long and costs 50% more FE than smelting dust. Crush it into dust first for faster, cheaper smelting.

There is no Steel Dust smelting recipe. Steel comes from Steel Blend, which you make in the [Alloy Furnace](rngtech:alloy-furnace.md) and then smelt here.

Stage 4 and higher metal recipes also need temperature stability. A Stage 0–3 Furnace will not start one without enough stability. A Stage 4 or higher Furnace starts it anyway, but poor stability or heat above the recipe's safe maximum builds up failure strain. A lane left above the recipe's overheat temperature by a hotter recipe cools down to it before smelting, so switching to a cooler metal costs time instead of ingots. Higher Overheat Tolerance shortens that wait. When strain fills, the input becomes a **Malformed Ingot** of that metal. Smelt it again to get two nuggets back. On any Furnace, a metal smelt pushed past its overheat temperature with the [overheat slider](rngtech:heat/the-overheat-slider.md) builds strain the same way. Ancient Debris never fails.
