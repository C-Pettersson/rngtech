---
navigation:
  title: "Solar Array Controller"
  parent: solar-panel.md
  position: 2
  icon: rngtech:crude_solar_panel
---

# Solar Array Controller

Place the controller on the same level as your panels. Every 20 ticks it scans a square around itself and takes over every panel that connects back to it through adjacent panels. Panels inside the square that are not connected to the rest are ignored.

| Range | Area | Max panels |
|---:|---|---:|
| 1 | 3×3 around the controller | 8 |
| 2 | 5×5 around the controller | 24 |

The controller starts at range 1. Range rolls on the controller and an installed Solar Array Extender raise it, up to a maximum of 2.

While a controller owns a panel, that panel stops exporting on its own. The controller adds up its panels' output, applies its own stats once to the total, stores the FE, and exports it to adjacent blocks. Its cable port is on the underside of its base. A few more rules:

- **Set bonus.** If every panel spot in range holds a panel of the same stage, the array gets +20% output.
- **Overlap.** A panel inside two controllers' ranges splits its output between them.
- **No output cap.** Like a standalone panel, the controller does not limit its own output, so the receiver or connector tier decides how much FE moves each tick.

The Process tab has a button that toggles an in-world outline of the controller's range. The outline stays visible after you close the screen.

The controller shows the state of its array without opening the screen:

- **Array map.** The display on top has one light for each panel spot in range, laid out like the field with north at the top. The controller's own spot is mint. A panel it owns is amber while it generates in sunlight, pale blue while it generates in moonlight, and dim when it isn't generating. An empty spot is dark.
- **Battery bay.** An installed Battery Cell shows behind the glass on the front.
- **Extender.** An installed Solar Array Extender sits on the bracket at the back.
- **Status lamp.** The lamp on the front lights while the array generates.

A controller that converts moonlight (Moonlit Conversion, Lunar Inverter, or the Aethergold extender) keeps its panels tracking at night, following the moon.

- [Controller affixes](rngtech:solar-panel/solar-array-controller/controller-affixes.md)
