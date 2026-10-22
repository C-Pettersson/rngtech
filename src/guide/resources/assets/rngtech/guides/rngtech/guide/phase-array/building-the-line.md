---
navigation:
  title: "Building the line"
  parent: phase-array.md
  position: 0
  icon: rngtech:exotic_affix_forge
---

# Building the line

<GameScene zoom="2" interactive={true}>
  <Block id="rngtech:exotic_affix_forge" x="0" y="0" z="0" p:facing="east" />
  <Block id="rngtech:phase_coil" x="1" y="0" z="0" p:facing="east" p:formed="true" p:tier="2" />
  <Block id="rngtech:phase_coil" x="2" y="0" z="0" p:facing="east" p:formed="true" p:tier="2" />
  <Block id="rngtech:phase_coil" x="3" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_coil" x="4" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_coil" x="5" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_emitter" x="6" y="0" z="0" p:facing="west" p:formed="true" />
</GameScene>

*A five-Coil line: the Cradle at the left, the Emitter at the right, positions counted from the Emitter.*

Place the Cradle, then three to nine Coils in a straight horizontal row from any side of it, and the Emitter at the far end. The Cradle finds the line on its own and lights the parts while it is complete; break any part and the line goes dark, and the charge waits for it to re-form. Nothing is needed above, below, or beside the row, and no block may sit between the parts.

Each Coil has a **position**, counted from the Emitter: position 1 is the Coil next to the Emitter, position 9 the Coil next to the Cradle on the longest line. When a part is placed next to the line it turns to follow the line, whichever way you face.
