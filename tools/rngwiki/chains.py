"""Production chains: a machine graph whose every edge is checked against the recipe data.

The model is renderer-agnostic: the wiki draws it as HTML, and other exporters can draw the same nodes and edges.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable

from .resources import MOD_ID, Resources


@dataclass(frozen=True)
class Machine:
    """How a machine takes and makes resources: its recipe types, plus what its code handles without recipes."""

    recipe_types: tuple[str, ...] = ()
    fuel_tags: tuple[str, ...] = ()
    takes: tuple[str, ...] = ()
    makes: tuple[str, ...] = ()


# Keyed by one block id per machine; any other id listed on the same wiki page resolves to it.
# `fuel_tags`, `takes`, and `makes` cover inputs and outputs that live in Java rather than recipe JSON.
MACHINES: dict[str, Machine] = {
    "rngtech:algae_photobioreactor": Machine(("rngtech:algae_growth",), takes=("rngtech:urea",)),
    "rngtech:ammonia_fuel_cell": Machine(("rngtech:ammonia_power_cycle",)),
    "rngtech:ammonia_synthesizer": Machine(("rngtech:ammonia_synthesis", "rngtech:urea_synthesis")),
    "rngtech:battery_assembler": Machine(("rngtech:battery_assembly",)),
    "rngtech:bio_generator": Machine(fuel_tags=(
        "rngtech:bio_generator/potato_fuels",
        "rngtech:bio_generator/carrot_fuels",
        "rngtech:bio_generator/bread_fuels",
        "rngtech:bio_generator/sapling_biomass",
        "rngtech:bio_generator/seed_biomass",
        "rngtech:bio_generator/plant_biomass",
        "rngtech:bio_generator/organic_reagents",
        "rngtech:bio_generator/composted_biomass",
        "rngtech:bio_generator/rich_biomass",
        "rngtech:bio_generator/algae_biomass",
        "rngtech:bio_generator/dense_algae_biomass",
    )),
    "rngtech:bronze_alloy_furnace_chassis": Machine(("rngtech:alloy_furnace",)),
    "rngtech:cavitation_generator": Machine(("rngtech:cavitation",)),
    "rngtech:coal_gasifier": Machine(("rngtech:coal_gasification",)),
    "rngtech:corrosion_cell": Machine(("rngtech:corrosion_cell",), takes=("rngtech:electrolyte_solution",)),
    "rngtech:iron_component_recycler": Machine(("rngtech:component_recycling",)),
    "rngtech:iron_crusher_chassis": Machine(("rngtech:crusher",)),
    "rngtech:furnace": Machine(("rngtech:furnace",)),
    "rngtech:melter": Machine(("rngtech:melter",)),
    "rngtech:metal_press": Machine(("rngtech:metal_press",)),
    "rngtech:potential_reactor": Machine(("rngtech:potential_reactor",)),
    "rngtech:silica_gel_dehumidifier": Machine(("rngtech:desiccant_absorption",)),
    "rngtech:crude_solid_fuel_burner": Machine(fuel_tags=("rngtech:solid_fuel/item_fuels", "rngtech:solid_fuel/block_fuels")),
    "rngtech:steam_methane_reformer": Machine(("rngtech:gas_reforming",)),
    "rngtech:syngas_combustor": Machine(("rngtech:gas_combustion",)),
    "rngtech:vacuum_collapse_generator": Machine(("rngtech:vacuum_collapse",)),
    "rngtech:wooden_composter": Machine(
        fuel_tags=("rngtech:wooden_composter/inputs", "rngtech:wooden_composter/prepared_inputs"),
        makes=("rngtech:composted_biomass",),
    ),
    "rngtech:wooden_dehumidifier": Machine(("rngtech:wooden_dehumidifier_conversion",), makes=("minecraft:water",)),
}


@dataclass
class Flow:
    """One side of an edge: what a single craft of the chosen recipe makes or uses."""

    amount: int | None = None
    fluid: bool = False
    ticks: int | None = None
    recipe: str | None = None
    via: str = "recipe"

    def per_second(self) -> float | None:
        if self.amount is None or not self.ticks:
            return None
        return self.amount * 20 / self.ticks


@dataclass
class Node:
    key: str
    machine: str | None
    resource: str | None = None

    @property
    def external(self) -> bool:
        return self.machine is None


@dataclass
class Edge:
    source: Node
    resource: str
    target: Node
    made: Flow | None = None
    used: Flow | None = None
    loop: bool = False
    pins: dict = field(default_factory=dict)


@dataclass
class Chain:
    nodes: list[Node] = field(default_factory=list)
    edges: list[Edge] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


Split = Callable[[dict], tuple[list, list, dict]]


def _qualify(recipe_id: str) -> str:
    return recipe_id if ":" in recipe_id else f"{MOD_ID}:{recipe_id}"


def machine_id(key: str) -> str:
    return key.partition("#")[0]


class ChainBuilder:
    """Turns author edges into a checked Chain. `resolve` maps any wiki id to its MACHINES key, or None."""

    def __init__(self, res: Resources, split: Split, resolve: Callable[[str], str | None], is_fluid: Callable[[str], bool]):
        self.res = res
        self.split = split
        self.resolve = resolve
        self.is_fluid = is_fluid

    def build(self, raw_edges: list) -> Chain:
        chain = Chain()
        nodes: dict[str, Node] = {}

        def node(key: str | None, resource: str, side: str, index: int) -> Node:
            if key is None:
                pseudo = Node(f"{side}:{index}", None, resource)
                chain.nodes.append(pseudo)
                return pseudo
            if key not in nodes:
                machine = self.resolve(machine_id(key))
                if machine is None:
                    chain.errors.append(f"{machine_id(key)} is not a known chain machine")
                nodes[key] = Node(key, machine or machine_id(key))
                chain.nodes.append(nodes[key])
            return nodes[key]

        for index, raw in enumerate(raw_edges):
            if not isinstance(raw, (list, tuple)) or len(raw) not in (3, 4):
                chain.errors.append(f"edge {raw!r} must be [from, resource, to] or [from, resource, to, recipes]")
                continue
            source_key, resource, target_key = raw[:3]
            pins = raw[3] if len(raw) == 4 else {}
            if source_key is None and target_key is None:
                chain.errors.append(f"edge {raw!r} needs a machine on at least one end")
                continue
            source = node(source_key, resource, "in", index)
            target = node(target_key, resource, "out", index)
            chain.edges.append(Edge(source, resource, target, pins=pins))

        for edge in chain.edges:
            source = "(supplied)" if edge.source.external else edge.source.key
            target = "(leaves the chain)" if edge.target.external else edge.target.key
            label = f"{source} -[{edge.resource}]-> {target}"
            if not edge.source.external and edge.source.machine in MACHINES:
                edge.made = self._flow(chain, edge, edge.source, "out", edge.pins.get("from"))
                if edge.made is None:
                    chain.errors.append(f"{label}: {edge.source.machine} does not make {edge.resource}")
            if not edge.target.external and edge.target.machine in MACHINES:
                edge.used = self._flow(chain, edge, edge.target, "in", edge.pins.get("to"))
                if edge.used is None:
                    chain.errors.append(f"{label}: {edge.target.machine} does not take {edge.resource}")
        _mark_loops(chain)
        return chain

    def _flow(self, chain: Chain, edge: Edge, node: Node, direction: str, pin: str | None) -> Flow | None:
        machine = MACHINES[node.machine]
        resource = edge.resource
        candidates = []
        for recipe_id, recipe in self.res.recipes.items():
            if recipe.get("type") not in machine.recipe_types:
                continue
            inputs, outputs, scalars = self.split(recipe)
            stacks = outputs if direction == "out" else inputs
            matches = [s for s in stacks if resource in s.options]
            if matches:
                candidates.append((recipe_id, inputs, outputs, scalars, matches))
        if pin is not None:
            pinned = [c for c in candidates if c[0] == _qualify(pin)]
            if not pinned:
                chain.errors.append(f"{node.machine}: pinned recipe {_qualify(pin)} does not {'make' if direction == 'out' else 'take'} {resource}")
            candidates = pinned
        if candidates:
            recipe_id, inputs, outputs, scalars, matches = self._best(chain, node, candidates)
            ticks = scalars.get("processing_ticks")
            amount = sum(s.count for s in matches)
            return Flow(amount, matches[0].fluid, ticks if isinstance(ticks, int) else None, recipe_id)
        if direction == "in" and any(self.res.tag_contains(tag, resource) for tag in machine.fuel_tags):
            return Flow(fluid=self.is_fluid(resource), via="fuel")
        if resource in (machine.makes if direction == "out" else machine.takes):
            return Flow(fluid=self.is_fluid(resource), via="machine")
        return None

    def _best(self, chain: Chain, node: Node, candidates: list) -> tuple:
        """Prefer the recipe that matches the most of this machine's other edges in the chain, in edge order."""
        if len(candidates) == 1:
            return candidates[0]
        incoming = [e.resource for e in chain.edges if e.target is node]
        outgoing = [e.resource for e in chain.edges if e.source is node]

        def score(candidate) -> tuple:
            _, inputs, outputs, _, _ = candidate
            ins = [any(r in s.options for s in inputs) for r in incoming]
            outs = [any(r in s.options for s in outputs) for r in outgoing]
            return (sum(ins) + sum(outs), ins, outs)

        return max(candidates, key=score)


def _mark_loops(chain: Chain) -> None:
    """Mark edges that point back upstream, found by depth-first search in author order."""
    successors: dict[str, list[Edge]] = {n.key: [] for n in chain.nodes}
    for edge in chain.edges:
        successors[edge.source.key].append(edge)
    state: dict[str, int] = {}

    def visit(key: str) -> None:
        state[key] = 1
        for edge in successors[key]:
            if state.get(edge.target.key) == 1:
                edge.loop = True
            elif edge.target.key not in state:
                visit(edge.target.key)
        state[key] = 2

    for node in chain.nodes:
        if node.key not in state:
            visit(node.key)


@dataclass
class Layout:
    """Grid cells for a chain: node key -> (layer, slot), and the cells each long edge passes through.

    Layers follow the flow; slots order the nodes within a layer. A renderer picks the direction.
    """

    cells: dict[str, tuple[int, float]]
    routes: dict[int, list[tuple[int, float]]]
    layers: int
    slots: int


def layout(chain: Chain) -> Layout:
    """Layered layout: longest path over non-loop edges, sources pulled next to what they feed, and edges that
    skip layers routed through waypoint cells. Slots are ordered by a few barycenter sweeps and centred, so a
    short layer gets fractional slots."""
    forward = [(i, e) for i, e in enumerate(chain.edges) if not e.loop]
    preds: dict[str, list[str]] = {n.key: [] for n in chain.nodes}
    succs: dict[str, list[str]] = {n.key: [] for n in chain.nodes}
    for _, edge in forward:
        preds[edge.target.key].append(edge.source.key)
        succs[edge.source.key].append(edge.target.key)

    column: dict[str, int] = {}

    def depth(key: str) -> int:
        if key not in column:
            column[key] = 0
            column[key] = max((depth(p) + 1 for p in preds[key]), default=0)
        return column[key]

    for node in chain.nodes:
        depth(node.key)
    for node in sorted(chain.nodes, key=lambda n: -column[n.key]):
        if not preds[node.key] and succs[node.key]:
            column[node.key] = min(column[s] for s in succs[node.key]) - 1

    order = {n.key: float(i) for i, n in enumerate(chain.nodes)}
    up: dict[str, list[str]] = {key: [] for key in column}
    down: dict[str, list[str]] = {key: [] for key in column}
    waypoints: dict[int, list[str]] = {}
    for i, edge in forward:
        first, last = column[edge.source.key], column[edge.target.key]
        dummies = [f"~{i}:{c}" for c in range(first + 1, last)]
        for c, key in zip(range(first + 1, last), dummies):
            column[key] = c
            order[key] = order[edge.source.key] + 0.5
            up[key], down[key] = [], []
        waypoints[i] = dummies
        path = [edge.source.key, *dummies, edge.target.key]
        for a, b in zip(path, path[1:]):
            down[a].append(b)
            up[b].append(a)

    columns: dict[int, list[str]] = {}
    for key in sorted(column, key=lambda k: order[k]):
        columns.setdefault(column[key], []).append(key)
    tallest = max(len(keys) for keys in columns.values())
    row: dict[str, float] = {}

    def place(keys: list[str]) -> None:
        for index, key in enumerate(keys):
            row[key] = index + (tallest - len(keys)) / 2

    for keys in columns.values():
        place(keys)
    for sweep in range(4):
        neighbours = up if sweep % 2 == 0 else down
        for c in sorted(columns, reverse=sweep % 2 == 1):
            keys = columns[c]

            def barycenter(key: str) -> float:
                near = [row[n] for n in neighbours[key]]
                return sum(near) / len(near) if near else row[key]

            keys.sort(key=lambda k: (barycenter(k), row[k]))
            place(keys)

    left = min(column.values())
    cells = {n.key: (column[n.key] - left, row[n.key]) for n in chain.nodes}
    routes = {i: [(column[k] - left, row[k]) for k in keys] for i, keys in waypoints.items()}
    return Layout(cells, routes, max(column.values()) - left + 1, tallest)
