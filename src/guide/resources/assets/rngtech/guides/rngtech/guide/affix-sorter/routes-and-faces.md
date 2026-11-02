---
navigation:
  title: "Routes and faces"
  parent: affix-sorter.md
  position: 1
  icon: rngtech:affix_sorter
---

# Routes and faces

Each face of the sorter is an **input**, a **Pass** face, or a **Reject** face. Click a face on the Process tab to change it. By default, the left face is Pass and the right face is Reject, as seen from the front.

The collar around the port on each face shows its setting. A green collar is a Pass face. A red collar is a Reject face. A bare collar is an input face. The collars of a route flash when an item goes into its buffer.

- Cables and hoppers insert items through input faces.
- Cables and hoppers extract items through Pass and Reject faces. The sorter never pushes items out.
- Each route has three buffer slots. When the buffer for an item is full, the sorter stops. It never reroutes or voids an item.
- The sorter accepts only identified items.
