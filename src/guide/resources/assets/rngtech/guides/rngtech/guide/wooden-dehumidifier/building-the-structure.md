---
navigation:
  title: "Building the structure"
  parent: wooden-dehumidifier.md
  position: 0
  icon: rngtech:wooden_dehumidifier
---

# Building the structure

The structure is a controller plus at least four frames:

1. Place the <ItemLink id="rngtech:wooden_dehumidifier" /> controller.
2. Place <ItemLink id="rngtech:wooden_dehumidifier_frame" /> blocks so that at least one touches a face of the controller, and the rest touch each other face to face. Diagonal contact does not count.
3. Keep every frame within 4 blocks of the controller on each axis. Frames further out are ignored.

Any shape works, such as a ring or barrel around the controller. Once the controller finds four or more connected frames, they all switch to their formed look and the machine starts working. With fewer than four, it stops and shows an invalid-structure status.

More frames make a bigger and faster machine, up to 25 frames:

| Frames | Water tank | Converted-output tank | Base water rate |
|---:|---:|---:|---:|
| 4 | 4,000 mB | 4,000 mB | 240 mB/min |
| 25 | 25,000 mB | 25,000 mB | 1,500 mB/min |

Each frame adds 1,000 mB to each tank and 60 mB/min to the base rate. Frames beyond 25 still count as part of the structure but add nothing.
