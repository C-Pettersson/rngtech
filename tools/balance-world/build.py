"""Generates the `rngbalance` datapack: one function per balance-test setup.

Each setup builds where the player stands, facing east (+X): a Universal Cable trunk along Z = 0 with the machines
on its north side, wired by pre-configured Universal Connectors. Infinite Debug Chests and Debug Tanks stand in for
the inputs you would supply yourself, and Debug Trashcans take overflow, so every chain runs on its own. Debug blocks
only work in dev runs (runClient, runServer).

    python tools/balance-world/build.py --install "<path to world folder>"

Without --install the pack is written to build/balance-world/rngbalance.
"""

import argparse
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NAMESPACE = "rngbalance"
PACK_FORMAT = 48  # Minecraft 1.21.1

ENERGY = "rngtech:exotic_energy_connector"
FLUID = "rngtech:arclite_fluid_connector"
ITEM = "rngtech:arclite_item_connector"

ORIGIN_X = 2
SIGN_X = -1
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}


def snbt(value):
    """Minimal SNBT writer: dicts, lists, strings, ints, floats, and raw `Tag` values."""
    if isinstance(value, Tag):
        return value.text
    if isinstance(value, bool):
        return "1b" if value else "0b"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):
        return f"{value}d"
    if isinstance(value, str):
        return json.dumps(value)
    if isinstance(value, list):
        return "[" + ",".join(snbt(v) for v in value) + "]"
    if isinstance(value, dict):
        return "{" + ",".join(f"{key(k)}:{snbt(v)}" for k, v in value.items()) + "}"
    raise TypeError(value)


def key(name):
    return name if name.replace("_", "").isalnum() and not name[0].isdigit() else json.dumps(name)


class Tag:
    def __init__(self, text):
        self.text = text


def stack(item, count=1, components=None, slot=None):
    entry = {} if slot is None else {"Slot": slot}
    entry.update({"id": item, "count": count})
    if components:
        entry["components"] = components
    return entry


def inventory(size, slots):
    """ItemStackHandler NBT from {slot: item id or stack dict}."""
    items = []
    for slot, item in sorted(slots.items()):
        entry = stack(item) if isinstance(item, str) else dict(item)
        items.append({"Slot": slot, **{k: v for k, v in entry.items() if k != "Slot"}})
    return {"Size": size, "Items": items}


def tank(fluid, amount):
    return {"Fluid": {"id": fluid, "amount": amount}}


def charged_cell(item, energy):
    return stack(item, components={"rngtech:battery_cell_energy": energy})


def white_hot(value):
    """A Magic part with one flat Max Temperature prefix ("White-Hot")."""
    return {
        "rngtech:machine_traits": {
            "rarity": "magic",
            "refinement_potential": 0,
            "modifiers": [
                {
                    "affix_id": "max_temperature_flat",
                    "mod_group": "max_temperature_flat",
                    "slot": "prefix",
                    "stat": "max_temperature",
                    "operation": "add",
                    "tier": 3,
                    "value": float(value),
                }
            ],
        }
    }


def bucket(fluid):
    namespace, path = fluid.split(":")
    return f"{namespace}:{path}_bucket"


class Fluid:
    """One fluid row on a connector. `out` pushes into the machine, otherwise it pulls from it."""

    def __init__(self, channel, fluid, out):
        self.channel, self.fluid, self.out = channel, fluid, out


class Item:
    """One item row. Machines take input on their top side and give output on their bottom side."""

    def __init__(self, channel, out, item=None, attach=None):
        self.channel, self.out, self.item = channel, out, item
        self.attach = attach if attach is not None else ("up" if out else "down")


def fin(channel, fluid):
    return Fluid(channel, fluid, out=True)


def fout(channel, fluid):
    return Fluid(channel, fluid, out=False)


def iin(channel, item=None, attach=None):
    return Item(channel, out=True, item=item, attach=attach)


def iout(channel, item=None, attach=None):
    return Item(channel, out=False, item=item, attach=attach)


class Setup:
    def __init__(self, name, title, summary):
        self.name, self.title, self.summary = name, title, summary
        self.blocks = []  # (x, y, z, block, nbt)
        self.cables = {}  # (x, y, z) -> {face: connector nbt}
        self.trunk_end = 0

    def block(self, x, y, z, block, nbt=None):
        self.blocks.append((x, y, z, block, nbt))

    def machine(self, x, block, nbt=None, energy=None, fluids=(), items=(), side="north"):
        """A machine beside the trunk at column x. `energy` is the connector's FE mode: input, output, or both."""
        z = -1 if side == "north" else 1
        self.block(x, 0, z, block, nbt)
        if energy or fluids or items:
            self.cables.setdefault((x, 0, 0), {})[side] = connector(side, energy, fluids, items)
        self.trunk_end = max(self.trunk_end, x)

    def extra_cable(self, x, z, face, energy=None, fluids=(), items=(), y=0):
        """A cable off the trunk (it must touch the trunk or another cable) with a connector on `face`."""
        self.cables.setdefault((x, y, z), {})[face] = connector(face, energy, fluids, items)

    def bounds(self):
        xs = [b[0] for b in self.blocks] + [c[0] for c in self.cables] + [SIGN_X, self.trunk_end]
        ys = [b[1] for b in self.blocks] + [c[1] for c in self.cables] + [0]
        zs = [b[2] for b in self.blocks] + [c[2] for c in self.cables] + [0]
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    def commands(self):
        out = []
        for x, y, z, block, nbt in self.blocks:
            out.append(f"setblock {rel(x, y, z)} {block}{snbt(nbt) if nbt else ''} replace")
        positions = [(x, 0, 0) for x in range(0, self.trunk_end + 1)]
        positions += sorted((p for p in self.cables if p[1:] != (0, 0)), key=lambda p: (p[1], p[2], p[0]))
        for x, y, z in positions:
            faces = self.cables.get((x, y, z), {})
            state = ",".join(f"connector_{face}=true" for face in faces)
            nbt = {"UniversalConnectors": faces} if faces else None
            out.append(f"setblock {rel(x, y, z)} rngtech:cable{'[' + state + ']' if state else ''}"
                       f"{snbt(nbt) if nbt else ''} replace")
        out.append(sign(SIGN_X, 0, 0, self.title))
        return out


def connector(face, energy, fluids, items):
    attach = OPPOSITE[face]
    slots = {}
    data = {"Channel": 0, "BridgeChannel": 0, "AttachAs": attach, "Mode": energy or "both",
            "Distribution": "even"}
    filters = {}
    if energy:
        slots[0] = ENERGY
    fluid_modules = {}
    for index, row in enumerate(fluids):
        slots[1 + index] = FLUID
        fluid_modules[str(index)] = {"Channel": row.channel, "AttachAs": attach,
                                     "Mode": "output" if row.out else "input"}
        filters[index * 2] = bucket(row.fluid)
    for index in range(len(fluids), 3):
        fluid_modules[str(index)] = {"Channel": 0, "AttachAs": "none", "Mode": "input"}
    item_modules = {}
    for index, row in enumerate(items):
        slots[4 + index] = ITEM
        item_modules[str(index)] = {"Channel": row.channel, "AttachAs": row.attach,
                                    "Mode": "output" if row.out else "input"}
        if row.item:
            filters[6 + index * 2] = row.item
    for index in range(len(items), 3):
        item_modules[str(index)] = {"Channel": 0, "AttachAs": "none", "Mode": "input"}
    data["Inventory"] = inventory(8, slots)
    data["FilterInventory"] = inventory(12, filters)
    data["FluidModules"] = fluid_modules
    data["ItemModules"] = item_modules
    return data


def rel(x, y, z, absolute_y=None):
    """Position relative to the player; layouts start ORIGIN_X blocks east so nothing lands on the player."""
    x += ORIGIN_X

    def axis(v):
        return "~" if v == 0 else f"~{v}"
    return f"{axis(x)} {axis(y) if absolute_y is None else absolute_y} {axis(z)}"


def sign(x, y, z, text):
    lines = [json.dumps(json.dumps({"text": text, "bold": True}))] + [json.dumps('""')] * 3
    return (f"setblock {rel(x, y, z)} minecraft:oak_sign[rotation=4]"
            f"{{front_text:{{messages:[{','.join(lines)}]}}}} replace")


# ---- shared pieces -----------------------------------------------------------------------------------------------


def debug_chest(*items):
    return {"Templates": inventory(27, dict(enumerate(items)))}


def debug_tank(fluid):
    return {"TemplateFluid": {"id": fluid, "amount": 1000}}


def battery_bank(charge_cells=2, charge=2_000_000):
    """Exotic Battery Chassis: the net-surplus meter. A few charged cells start chains that need FE before they run."""
    cells = {slot: charged_cell("rngtech:exotic_battery_cell", charge if slot < charge_cells else 0)
             for slot in range(10)}
    return {"Inventory": inventory(14, cells)}


def gas_gear(heat_core=None, cell=None, servo=None, catalyst=None):
    return {"GearInventory": inventory(4, {k: v for k, v in
                                           {0: heat_core, 1: cell, 2: servo, 3: catalyst}.items() if v})}


WATER = "minecraft:water"


def wooden_dehumidifier(s, x, nbt=None, fluids=(), items=()):
    """Full-size Wooden Dehumidifier: a hollow 3x3x3 frame cube (25 frames) north of the trunk at column x.

    The controller replaces the middle block of the cube's south face, one block up and facing the trunk, so a cable
    riser above the trunk carries its connector.
    """
    for y in range(3):
        for dx in (-1, 0, 1):
            for z in (-3, -2, -1):
                if (dx, y, z) == (0, 1, -1):
                    s.block(x, y, z, "rngtech:wooden_dehumidifier[facing=south]", nbt)
                elif (dx, y, z) != (0, 1, -2):
                    s.block(x + dx, y, z, "rngtech:wooden_dehumidifier_frame")
    s.extra_cable(x, 0, "north", fluids=fluids, items=items, y=1)
    s.trunk_end = max(s.trunk_end, x)


# ---- setups --------------------------------------------------------------------------------------------------------


def coal():
    s = Setup("coal", "Coal S1-4", "Solid Fuel Burner per stage (crude, copper, alloy, steel) on coal; each burns "
                                   "into a Debug Trashcan behind it, so it runs flat out.")
    s.machine(0, "rngtech:debug_chest", debug_chest("minecraft:coal"), items=[iout(0)])
    burners = [
        ("crude", "iron", "iron", "iron"),
        ("copper", "copper", "copper", "copper"),
        ("alloy", "bronze", "bronze", "lead"),
        ("steel", "steel", "steel", "invar"),
    ]
    for i, (chassis, core, box, cell) in enumerate(burners):
        x = 2 + i * 2
        nbt = {"GearInventory": inventory(3, {0: f"rngtech:{core}_heat_core", 1: f"rngtech:{cell}_battery_cell",
                                              2: f"rngtech:{box}_fuel_box"}),
               "FuelInventory": inventory(2, {})}
        s.machine(x, f"rngtech:{chassis}_solid_fuel_burner", nbt, items=[iin(0, attach="south")])
        s.block(x, 0, -2, "rngtech:debug_trashcan")
    return s


def syngas():
    s = Setup("syngas", "Syngas S5", "Coal > Crusher > Coal Gasifier > Syngas Combustor; exhaust and gasification "
                                     "residue feed two Algae Photobioreactors, algae feeds a Bio Generator.")
    # Channels: fluid 0 water, 1 syngas, 2 carbon exhaust; item 0 coal, 1 coal dust, 2 residue, 3 algae.
    s.machine(0, "rngtech:debug_chest", debug_chest("minecraft:coal"), items=[iout(0)])
    s.machine(2, "rngtech:aluminum_crusher_chassis",
              {"Inventory": inventory(4, {1: "rngtech:sparksteel_battery_cell", 2: "rngtech:aluminum_crush_head"})},
              energy="output", items=[iin(0), iout(1)])
    s.machine(4, "rngtech:debug_tank", debug_tank(WATER), fluids=[fout(0, WATER)])
    s.machine(6, "rngtech:coal_gasifier", gas_gear("rngtech:aluminum_heat_core", "rngtech:sparksteel_battery_cell"),
              energy="output", fluids=[fin(0, WATER), fout(1, "rngtech:syngas")], items=[iin(1), iout(2)])
    s.machine(8, "rngtech:syngas_combustor", {**gas_gear(), "Mixture": 100}, energy="input",
              fluids=[fin(1, "rngtech:syngas"), fout(2, "rngtech:carbon_exhaust")])
    wooden_dehumidifier(s, 10, {"SelectedOutput": "rngtech:carbon_exhaust"},
                        fluids=[fout(2, "rngtech:carbon_exhaust")], items=[iin(2)])
    for x in (14, 16):
        s.machine(x, "rngtech:algae_photobioreactor", {"Density": 1.0},
                  fluids=[fin(0, WATER), fin(2, "rngtech:carbon_exhaust")], items=[iout(3)])
        s.block(x, 2, -1, "minecraft:glowstone")
    s.machine(18, "rngtech:bio_generator",
              {"GearInventory": inventory(2, {0: "rngtech:lead_battery_cell", 1: "rngtech:bio_chamber"})},
              energy="input", items=[iin(3, attach="south")])
    s.machine(20, "rngtech:debug_trashcan", None, fluids=[fin(2, "rngtech:carbon_exhaust")])
    s.machine(22, "rngtech:exotic_battery_chassis", battery_bank(), energy="both")
    return s


def melter_gear(heat_core):
    return {"GearInventory": inventory(5, {0: heat_core, 1: "rngtech:titanium_crush_head",
                                           2: "rngtech:arclite_battery_cell", 4: "rngtech:osmium_fluid_pump"})}


def reformer_gear():
    return gas_gear("rngtech:titanium_heat_core", "rngtech:arclite_battery_cell",
                    catalyst="rngtech:reforming_catalyst_bed")


def reformer(s, x):
    """Steam Methane Reformer: four fluid rows, so CO leaves through a second cable on its west side."""
    s.machine(x, "rngtech:steam_methane_reformer", reformer_gear(), energy="output",
              fluids=[fin(0, WATER), fin(1, "rngtech:methane"), fout(2, "rngtech:hydrogen")])
    s.extra_cable(x - 1, -1, "east", fluids=[fout(3, "rngtech:carbon_monoxide")])


def carbon_loop():
    s = Setup("carbon_loop", "Carbon loop S6", "Potatoes > Crusher > Organic Reagent; three Photobioreactors grow "
                                               "algae on Reformer CO; Melter makes methane; Reformer makes H2 (to "
                                               "trashcan) and CO (back to the algae). Powered by a Debug Battery.")
    # Channels: fluid 0 water, 1 methane, 2 hydrogen, 3 carbon monoxide; item 0 potato, 1 reagent, 2 algae.
    s.machine(0, "rngtech:debug_battery", {"OutputRate": 100_000}, energy="input")
    s.machine(2, "rngtech:debug_chest", debug_chest("minecraft:potato"), items=[iout(0)])
    s.machine(4, "rngtech:titanium_crusher_chassis",
              {"Inventory": inventory(4, {1: "rngtech:arclite_battery_cell", 2: "rngtech:titanium_crush_head"})},
              energy="output", items=[iin(0), iout(1)])
    s.machine(6, "rngtech:debug_tank", debug_tank(WATER), fluids=[fout(0, WATER)])
    # The loop cannot start itself: seed the cultures with CO so the first algae reaches the Melter.
    for x in (8, 10, 12):
        s.machine(x, "rngtech:algae_photobioreactor",
                  {"Density": 1.0, "CarbonTank": tank("rngtech:carbon_monoxide", 4000)},
                  fluids=[fin(0, WATER), fin(3, "rngtech:carbon_monoxide")], items=[iout(2)])
        s.block(x, 2, -1, "minecraft:glowstone")
    # Methane needs 1,400 heat; a plain Titanium Heat Core reaches 1,200, so this one rolled White-Hot +300.
    s.machine(14, "rngtech:melter",
              melter_gear(stack("rngtech:titanium_heat_core", components=white_hot(300))),
              energy="output", fluids=[fin(0, WATER), fout(1, "rngtech:methane")],
              items=[iin(1, "rngtech:organic_reagent"), iin(2, "rngtech:algae_biomass")])
    reformer(s, 16)
    s.machine(18, "rngtech:debug_trashcan", None, fluids=[fin(2, "rngtech:hydrogen")])
    return s


def ammonia():
    s = Setup("ammonia", "Ammonia S6", "Methane (debug) > Reformer > H2; Cavitation Generator splits water into N2; "
                                       "Ammonia Synthesizer > Ammonia Fuel Cell. CO and residue go to trashcans.")
    # Channels: fluid 0 water, 1 methane, 2 hydrogen, 3 carbon monoxide, 4 nitrogen, 5 ammonia; item 0 residue.
    s.machine(0, "rngtech:debug_tank", debug_tank("rngtech:methane"), fluids=[fout(1, "rngtech:methane")])
    s.machine(2, "rngtech:debug_tank", debug_tank(WATER), fluids=[fout(0, WATER)])
    reformer(s, 4)
    s.machine(6, "rngtech:debug_trashcan", None, fluids=[fin(3, "rngtech:carbon_monoxide")])
    s.machine(8, "rngtech:cavitation_generator",
              {"GearInventory": inventory(5, {0: "rngtech:nitrogen_extraction_rotor",
                                              1: "rngtech:nitrogen_separation_nozzle",
                                              3: "rngtech:arclite_battery_cell"})},
              energy="both", fluids=[fin(0, WATER), fout(4, "rngtech:nitrogen")])
    s.machine(10, "rngtech:ammonia_synthesizer", {"GearInventory": inventory(1, {0: "rngtech:ammonia_catalyst_bed"})},
              energy="output",
              fluids=[fin(2, "rngtech:hydrogen"), fin(4, "rngtech:nitrogen"), fout(5, "rngtech:ammonia")])
    s.machine(12, "rngtech:ammonia_fuel_cell",
              {"GearInventory": inventory(2, {0: "rngtech:fuel_cell_membrane", 1: "rngtech:arclite_battery_cell"})},
              energy="input", fluids=[fin(5, "rngtech:ammonia")], items=[iout(0)])
    s.machine(14, "rngtech:debug_trashcan", None, items=[iin(0, attach="south")])
    s.machine(16, "rngtech:exotic_battery_chassis", battery_bank(), energy="both")
    return s


def void_catalyst():
    s = Setup("void_catalyst", "Void Catalyst S7", "Ammonia + Ender Pearls > Melter > Ender Slurry; Component "
                                                   "Assembler seals it with Nullite Ingot and Tungstensteel Plate; "
                                                   "Vacuum Collapse Generator burns the catalyst.")
    # Channels: fluid 5 ammonia, 6 ender slurry; item 0 pearls, 1 nullite, 2 plates, 3 catalyst, 4 residue.
    s.machine(0, "rngtech:debug_tank", debug_tank("rngtech:ammonia"), fluids=[fout(5, "rngtech:ammonia")])
    s.machine(2, "rngtech:debug_chest",
              debug_chest("minecraft:ender_pearl", "rngtech:nullite_ingot", "rngtech:tungstensteel_plate"),
              items=[iout(0, "minecraft:ender_pearl", attach="south"),
                     iout(1, "rngtech:nullite_ingot", attach="south"),
                     iout(2, "rngtech:tungstensteel_plate", attach="south")])
    s.machine(4, "rngtech:melter", melter_gear("rngtech:titanium_heat_core"), energy="output",
              fluids=[fin(5, "rngtech:ammonia"), fout(6, "rngtech:ender_slurry")], items=[iin(0)])
    s.machine(6, "rngtech:battery_assembler", {"GearInventory": inventory(1, {})}, energy="output",
              fluids=[fin(6, "rngtech:ender_slurry")], items=[iin(1), iin(2), iout(3)])
    s.machine(8, "rngtech:vacuum_collapse_generator",
              {"GearInventory": inventory(3, {0: "rngtech:tungstensteel_void_chamber",
                                              1: "rngtech:tungstensteel_collapse_nozzle",
                                              2: "rngtech:tungstensteel_dimensional_stabilizer"})},
              energy="input", items=[iin(3), iout(4)])
    s.machine(10, "rngtech:debug_trashcan", None, items=[iin(4, attach="south")])
    s.machine(12, "rngtech:exotic_battery_chassis", battery_bank(), energy="both")
    return s


def water():
    s = Setup("water", "Water supply", "Full-size Wooden Dehumidifier (25 frames, needs sky), and a Silica Gel "
                                       "Dehumidifier whose beads loop through an Aluminum Furnace. Water drains "
                                       "into a trashcan.")
    # Channels: fluid 0 water; item 0 saturated beads, 1 dry beads.
    wooden_dehumidifier(s, 0, fluids=[fout(0, WATER)])
    s.machine(4, "rngtech:silica_gel_dehumidifier",
              {"Inventory": inventory(6, {i: stack("rngtech:silica_gel_beads", 16) for i in range(3)})},
              fluids=[fout(0, WATER)], items=[iout(0), iin(1)])
    s.block(4, 1, -1, "rngtech:silica_gel_column_casing")
    s.block(4, 2, -1, "rngtech:silica_gel_column_casing")
    s.machine(6, "rngtech:aluminum_furnace_chassis",
              {"Inventory": inventory(9, {4: "rngtech:sparksteel_battery_cell"}),
               "GearInventory": inventory(1, {0: "rngtech:aluminum_heat_core"})},
              items=[iin(0), iout(1)])
    s.block(6, 0, -2, "rngtech:debug_battery", {"OutputRate": 10_000})
    s.machine(8, "rngtech:debug_trashcan", None, fluids=[fin(0, WATER)])
    return s


def side_power():
    s = Setup("side_power", "Side power", "Solar array (controller + 8 Sparksteel panels), three Bio Generators "
                                          "(potato, composted biomass, algae), Corrosion Cell on iron plates, "
                                          "Potential Reactor on byproduct. Each burns into a trashcan behind it.")
    # Channels: item 0 potato, 1 compost, 2 algae, 3 iron plate, 4 electrolyte, 5 byproduct, 6 residue.
    s.machine(0, "rngtech:debug_chest",
              debug_chest("minecraft:potato", "rngtech:composted_biomass", "rngtech:algae_biomass"),
              items=[iout(0, "minecraft:potato", "south"), iout(1, "rngtech:composted_biomass", "south"),
                     iout(2, "rngtech:algae_biomass", "south")])
    for i, x in enumerate((2, 4, 6)):
        s.machine(x, "rngtech:bio_generator",
                  {"GearInventory": inventory(2, {0: "rngtech:lead_battery_cell", 1: "rngtech:bio_chamber"})},
                  items=[iin(i, attach="south")])
        s.block(x, 0, -2, "rngtech:debug_trashcan")
    s.machine(8, "rngtech:debug_chest",
              debug_chest("rngtech:iron_plate", "rngtech:electrolyte", "rngtech:recycling_byproduct"),
              items=[iout(3, "rngtech:iron_plate", "south"), iout(4, "rngtech:electrolyte", "south"),
                     iout(5, "rngtech:recycling_byproduct", "south")])
    s.machine(10, "rngtech:corrosion_cell", {"GearInventory": inventory(3, {})},
              items=[iin(3), iin(4, attach="south"), iout(6)])
    s.block(10, 0, -2, "rngtech:debug_trashcan")
    s.machine(12, "rngtech:potential_reactor",
              {"GearInventory": inventory(3, {0: "rngtech:steel_reactor_chamber"})},
              items=[iin(5), iout(6)])
    s.block(12, 0, -2, "rngtech:debug_trashcan")
    s.machine(14, "rngtech:debug_trashcan", None, items=[iin(6, attach="south")])
    # Solar: controller ringed by 8 panels at z -2..-4, trashcan under the controller.
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            block = "rngtech:solar_array_controller" if dx == dz == 0 else "rngtech:sparksteel_solar_panel"
            nbt = {"GearInventory": inventory(2, {})} if dx == dz == 0 else None
            s.block(18 + dx, 1, -3 + dz, block, nbt)
    s.block(18, 0, -3, "rngtech:debug_trashcan")
    return s


def mastery(start, target, level=60):
    """A machine_progression component at `level` with the shortest path from `start` to `target` allocated."""
    tree = json.loads((ROOT / "src/main/resources/data/rngtech/mastery/machine_tree.json").read_text(encoding="utf-8"))
    kinds = {node["id"]: node["kind"] for node in tree["nodes"]}
    links = {}
    for a, b in tree["links"]:
        links.setdefault(a, []).append(b)
        links.setdefault(b, []).append(a)
    start_id = f"start_{start}"
    previous, queue = {start_id: None}, [start_id]
    while queue and target not in previous:
        current = queue.pop(0)
        for nxt in links.get(current, []):
            if nxt not in previous and kinds[nxt] != "STARTER":
                previous[nxt] = current
                queue.append(nxt)
    path, node = [], target
    while previous[node] is not None:
        path.append(node)
        node = previous[node]
    return {"rngtech:machine_progression": {"tree_version": tree["version"], "xp": Tag("93085595L"), "level": level,
                                            "start": start_id, "allocated_nodes": path[::-1]}}


def nitrogen_atmosphere():
    s = Setup("nitrogen_atmosphere", "Nitrogen atmosphere",
              "Two Blast Draft Tungstensteel Furnaces smelt tin dust at 40% slider, only the first on Nitrogen; a "
              "Titanium Alloy Furnace and a Metal Press with Blast Draft and Nitrogen wait for hand-fed input.")
    # Channels: fluid 0 nitrogen; item 0 tin dust, 1 output.
    nitrogen = "rngtech:nitrogen"
    blast = mastery("drive_reserve", "blast_draft")
    s.machine(0, "rngtech:debug_battery", {"OutputRate": 100_000}, energy="input")
    s.machine(2, "rngtech:debug_tank", debug_tank(nitrogen), fluids=[fout(0, nitrogen)])
    s.machine(4, "rngtech:debug_chest", debug_chest(stack("rngtech:tin_dust", 64)), items=[iout(0)])
    for x, fluids in ((6, [fin(0, nitrogen)]), (8, [])):
        s.machine(x, "rngtech:tungstensteel_furnace_chassis",
                  {"Inventory": inventory(9, {4: "rngtech:nullite_battery_cell"}),
                   "GearInventory": inventory(1, {0: "rngtech:titanium_heat_core"}),
                   "HeatTrack": 40, "components": blast},
                  energy="output", fluids=fluids, items=[iin(0), iout(1)])
    s.machine(10, "rngtech:titanium_alloy_furnace_chassis",
              {"GearInventory": inventory(4, {0: "rngtech:titanium_heat_core", 1: "rngtech:titanium_alloy_crucible",
                                              2: "rngtech:arclite_battery_cell"}),
               "HeatTrack": 40, "components": blast},
              energy="output", fluids=[fin(0, nitrogen)])
    s.machine(12, "rngtech:metal_press",
              {"GearInventory": inventory(8, {0: "rngtech:steel_heat_core", 2: "rngtech:plate_mold",
                                              7: "rngtech:invar_battery_cell"}),
               "HeatTrack": 40, "components": mastery("control", "blast_draft")},
              energy="output", fluids=[fin(0, nitrogen)])
    s.machine(14, "rngtech:debug_trashcan", None, items=[iin(1, attach="south")])
    return s


def ore_lines():
    """Crusher > Furnace per stage, from tools/balance/processing-setups.json, on the stage's raw ore."""
    data = json.loads((ROOT / "tools/balance/processing-setups.json").read_text(encoding="utf-8"))
    setups = []
    for row in data["stages"]:
        stage, metal = row["stage"], row["metal"]
        ore = f"minecraft:raw_{metal}" if metal in ("iron", "copper") else f"rngtech:raw_{metal}"
        s = Setup(f"ore_s{stage}", f"Ore line S{stage}",
                  f"Stage {stage} {metal}: Debug Chest raw ore > {row['crusherChassis']} Crusher > "
                  f"{row['furnace']['chassis']} Furnace > trashcan, on a Debug Battery.")
        cell = f"rngtech:{row['batteryCell']}_battery_cell"
        s.machine(0, "rngtech:debug_battery", {"OutputRate": 100_000}, energy="input")
        s.machine(2, "rngtech:debug_chest", debug_chest(ore), items=[iout(0)])
        s.machine(4, f"rngtech:{row['crusherChassis']}_crusher_chassis",
                  {"Inventory": inventory(4, {1: cell, 2: f"rngtech:{row['crushHead']}_crush_head"})},
                  energy="output", items=[iin(0), iout(1)])
        s.machine(6, f"rngtech:{row['furnace']['chassis']}_furnace_chassis",
                  {"Inventory": inventory(9, {4: cell}),
                   "GearInventory": inventory(1, {0: f"rngtech:{row['furnace']['heatCore']}_heat_core"})},
                  energy="output", items=[iin(1), iout(2)])
        s.machine(8, "rngtech:debug_trashcan", None, items=[iin(2, attach="south")])
        setups.append(s)
    return setups


def all_setups():
    return [coal(), syngas(), carbon_loop(), ammonia(), void_catalyst(), water(), side_power(), nitrogen_atmosphere(),
            *ore_lines()]


# ---- datapack ------------------------------------------------------------------------------------------------------


def write_pack(target):
    if target.exists():
        shutil.rmtree(target)
    functions = target / "data" / NAMESPACE / "function"
    functions.mkdir(parents=True)
    (target / "pack.mcmeta").write_text(json.dumps(
        {"pack": {"pack_format": PACK_FORMAT, "description": "RNGTech balance-test setups"}}, indent=4) + "\n")
    setups = all_setups()
    help_lines = ['tellraw @s {"text":"RNGTech balance setups (build east from your feet):","color":"gold"}']
    for s in setups:
        (lo, hi) = s.bounds()
        size = f"{hi[0] - lo[0] + 1}x{hi[1] - lo[1] + 1}x{hi[2] - lo[2] + 1}"
        span = f"x +{lo[0] + ORIGIN_X}..+{hi[0] + ORIGIN_X}, z {lo[2]}..{hi[2]}"
        build = s.commands()
        tag = f"{NAMESPACE}.{s.name}"
        region = f"{rel(*lo)} {rel(*hi)}"
        marker = [f"kill @e[type=minecraft:marker,tag={tag}]",
                  f'summon minecraft:marker ~ ~ ~ {{Tags:["{NAMESPACE}","{tag}"]}}',
                  f'tellraw @s {{"text":"Built {s.title}. Rebuild in place: /function {NAMESPACE}:rebuild/{s.name}",'
                  f'"color":"green"}}']
        write(functions / "internal" / "place" / f"{s.name}.mcfunction",
              [f"# {s.title}: {s.summary}", *build, *marker])
        write(functions / f"{s.name}.mcfunction", [
            f"# {s.title} ({size} blocks at {span}, y {lo[1]}..{hi[1]} from your feet).",
            f"# {s.summary}",
            "# Refuses to build unless the footprint is empty air; use force/<name> to clear it first.",
            f"execute unless blocks {region} {rel(lo[0], 0, lo[2], absolute_y=300)} all run return run "
            f'tellraw @s {{"text":"{s.title}: the {size} footprint ({span} from your feet) is not '
            f'empty. Move, or run /function {NAMESPACE}:force/{s.name} to clear it.","color":"red"}}',
            f"function {NAMESPACE}:internal/place/{s.name}",
        ])
        write(functions / "force" / f"{s.name}.mcfunction", [
            f"# Clears the {s.title} footprint, then builds.",
            f"fill {rel(*lo)} {rel(*hi)} minecraft:air",
            f"function {NAMESPACE}:internal/place/{s.name}",
        ])
        write(functions / "rebuild" / f"{s.name}.mcfunction", [
            f"# Clears and rebuilds {s.title} where it was last built (fresh machines after a balance change).",
            f"execute unless entity @e[type=minecraft:marker,tag={tag}] run return run "
            f'tellraw @s {{"text":"{s.title} has not been built yet.","color":"red"}}',
            f"execute as @e[type=minecraft:marker,tag={tag},limit=1] at @s run function {NAMESPACE}:force/{s.name}",
        ])
        help_lines.append(
            f'tellraw @s [{{"text":" {s.name}","color":"aqua","clickEvent":{{"action":"suggest_command",'
            f'"value":"/function {NAMESPACE}:{s.name}"}}}},{{"text":" {size} - {s.summary}","color":"gray"}}]')
    help_lines.append('tellraw @s {"text":"Also: rebuild/<name>, rebuild_all, force/<name>, world_rules.",'
                      '"color":"gold"}')
    write(functions / "help.mcfunction", help_lines)
    write(functions / "rebuild_all.mcfunction",
          [f"execute if entity @e[type=minecraft:marker,tag={NAMESPACE}.{s.name}] run function "
           f"{NAMESPACE}:rebuild/{s.name}" for s in setups])
    write(functions / "world_rules.mcfunction", [
        "# Fixed noon, clear weather, no mob spawns: steady solar, algae, and dehumidifier rates.",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule doMobSpawning false",
        "time set noon",
        "weather clear",
    ])
    return setups


def write(path, lines):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--install", type=Path, help="world folder; writes <world>/datapacks/rngbalance")
    args = parser.parse_args()
    target = (args.install / "datapacks" / NAMESPACE) if args.install else ROOT / "build/balance-world" / NAMESPACE
    setups = write_pack(target)
    print(f"Wrote {len(setups)} setups to {target}")


if __name__ == "__main__":
    main()
