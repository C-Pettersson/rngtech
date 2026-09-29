import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { CENTER, START_ANGLES, layoutMegaTree } from "./layout-mega-tree.mjs";

// Deterministic authoring source. Runtime and ModDex read the same checked-in catalog.
export const TREE_VERSION = 2;
// Nodes placed by the original fill; EXTRAS append after it, so the catalog holds more.
export const TOTAL_NODES = 1300;
const ATTRIBUTES = ["CONTROL", "DRIVE", "RESERVE"];
const STARTS = ["control", "control_drive", "drive", "drive_reserve", "reserve", "reserve_control"];
const START_ATTRIBUTES = [[0], [0, 1], [1], [1, 2], [2], [2, 0]];

const effect = (stat, value, operation = "INCREASED_PERCENT") => ({ stat, operation, value });
// A tagged effect only reaches machines in that group; keystones tag their payoff with the group that pays their cost.
const tagged = (tag, taggedEffect) => ({ ...taggedEffect, tag });
const title = value => value.toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const slug = value => value.toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");
const theme = (name, primary, small, notable, secondary, notables) => ({ name, primary, small, notable, secondary, notables });

// Small nodes grant the primary stat; notables grant a larger primary value and a supporting secondary effect.
const THEMES = {
    precision: theme("Precision", ["STABILITY", "INCREASED_PERCENT"], 2, 6, effect("ENERGY_USAGE", 2, "DECREASED_PERCENT"),
        ["Steady Bearings", "True Alignment", "Balanced Rotors", "Fine Tolerances", "Dampened Frame", "Level Mounting", "Vibration Sinks", "Trued Shafts", "Gyroscopic Brace", "Squared Chassis", "Rigid Couplings", "Stable Footing"]),
    economy: theme("Economy", ["ENERGY_USAGE", "DECREASED_PERCENT"], 1, 3, effect("STABILITY", 3),
        ["Lean Draw", "Efficient Windings", "Low Loss Bus", "Current Trim", "Soft Starter", "Load Smoothing", "Frugal Circuits", "Idle Dampers", "Measured Current", "Thrifty Relays", "Power Factor", "Cool Contacts"]),
    calibration: theme("Calibration", ["CALIBRATION_PRECISION", "INCREASED_PERCENT"], 2, 6, effect("STABILITY", 3),
        ["Reference Gauges", "Zeroed Dials", "Tuned Resonators", "Vernier Scales", "Trim Pots", "Calibrated Feed", "Test Bench", "Standard Weights", "Laser Guides", "Micro Adjusters"]),
    fieldWork: theme("Field Work", ["TREE_FELL_LIMIT", "ADD"], 1, 2, effect("PROCESSING_SPEED", 3),
        ["Timber Rigging", "Long Reach Saws", "Grove Sweeps", "Canopy Hooks", "Feller Heads", "Root Pullers", "Stump Grinders", "Lumber Chains", "Forest Survey", "Broad Swath", "Log Skidders", "Bark Strippers"]),
    actuation: theme("Actuation", ["PROCESSING_SPEED", "INCREASED_PERCENT"], 2, 6, effect("CONTROL", 4, "ADD"),
        ["Snap Actuators", "Servo Linkage", "Quick Release", "Pneumatic Rams", "Rapid Cycling", "Hair Triggers", "Short Stroke", "Spring Returns", "Cam Followers", "Fast Clutches", "Linear Motors", "Toggle Presses"]),
    momentum: theme("Momentum", ["PROCESSING_SPEED", "INCREASED_PERCENT"], 2, 6, effect("DRIVE", 4, "ADD"),
        ["Heavy Flywheel", "Kinetic Linkage", "Rolling Start", "Kinetic Overdrive", "Hopper Rhythm", "Charged Flywheel", "Inertial Drive", "Unbroken Cadence", "Spin Up", "Runaway Gears", "Freewheel Hub", "Driven Mass"]),
    hardness: theme("Hardness", ["HIGH_HARDNESS_ENERGY_MITIGATION", "ADD"], 1, 3, effect("STABILITY", 3),
        ["Hardened Bearings", "Arc Pressure Drive", "Carbide Teeth", "Tempered Jaws", "Impact Plates", "Diamond Edges", "Bedrock Breakers", "Stress Relief", "Work Hardening", "Shock Mounts", "Manganese Liners", "Anvil Beds"]),
    recovery: theme("Recovery", ["OUTPUT_AMOUNT", "INCREASED_PERCENT"], 0.5, 1.5, effect("PROCESSING_SPEED", 2),
        ["Ore Sieve", "Dust Recovery", "Sorting Rake", "Sample Tray", "Clean Dust Ledge", "Material Memory", "Fine Screens", "Tailings Pass", "Double Sort", "Scrap Catcher", "Riffle Tables", "Grit Traps"]),
    thermalReach: theme("Thermal Reach", ["MAX_TEMPERATURE", "INCREASED_PERCENT"], 1, 3, effect("HEAT_TRANSFER", 2),
        ["White Heat", "Refractory Lining", "Blast Nozzles", "Firebrick Walls", "Oxygen Feed", "Crucible Glaze", "Arc Chamber", "Stoked Core", "Molten Pour", "Superheat", "Bellows Draft", "Kiln Crown"]),
    heatFlow: theme("Heat Flow", ["HEAT_TRANSFER", "INCREASED_PERCENT"], 2, 6, effect("WARMUP_TIME", 2, "DECREASED_PERCENT"),
        ["Conduction Fins", "Radiant Plates", "Copper Cladding", "Forced Draft", "Heat Pipes", "Flame Spreaders", "Convection Loops", "Hot Air Jackets", "Thermal Bridge", "Flux Channels", "Baffle Plates", "Hearth Fans"]),
    startup: theme("Startup", ["WARMUP_TIME", "DECREASED_PERCENT"], 2, 6, effect("HEAT_TRANSFER", 2),
        ["Preheater Coils", "Pilot Flame", "Quick Ignition", "Glow Plugs", "Warm Standby", "Kindling Tray", "Spark Gap", "Flash Start", "Primed Burners", "Heat Soak", "Tinder Box", "Fire Starter"]),
    fuelEconomy: theme("Fuel Economy", ["FUEL_DURATION", "INCREASED_PERCENT"], 2, 6, effect("HEAT_ISOLATION", 3),
        ["Slow Burn", "Banked Coals", "Clean Combustion", "Air Dampers", "Ember Keeper", "Fuel Metering", "Long Grate", "Charcoal Bed", "Lean Mixture", "Afterburner Loop", "Stoking Rhythm", "Coke Oven"]),
    reserves: theme("Reserves", ["ENERGY_CAPACITY", "INCREASED_PERCENT"], 3, 9, effect("RESERVE", 4, "ADD"),
        ["Frame Reservoir", "Buffer Laminate", "Massive Buffer", "Battery Shell", "Auxiliary Bank", "Deep Cells", "Capacitor Trace", "Copper Plates", "Stacked Cells", "Charge Vault", "Spare Capacity", "Reservoir Tanks"]),
    chargeRouting: theme("Charge Routing", ["ENERGY_TRANSFER", "INCREASED_PERCENT"], 3, 9, effect("ENERGY_CAPACITY", 3),
        ["Reserve Channels", "Internal Bus", "Recovery Bus", "Wide Busbars", "Cell Contacts", "Buffered Discharge", "Low Ohm Paths", "Split Feeds", "Trunk Lines", "Relay Mesh", "Surge Gates", "Charge Ladders"]),
    powerSupply: theme("Power Supply", ["ENERGY_GENERATION", "INCREASED_PERCENT"], 2, 6, effect("FUEL_DURATION", 2),
        ["Induction Coils", "Dynamo Windings", "Field Magnets", "Turbine Blades", "Stator Rings", "Exciter Loop", "Generator Tuning", "Rotor Balance", "Phase Matching", "Peak Output", "Brush Contacts", "Armature Wrap"]),
    conservation: theme("Conservation", ["ENERGY_USAGE", "DECREASED_PERCENT"], 1, 3, effect("ENERGY_CAPACITY", 3),
        ["Energy Governor", "Standby Mode", "Sleep Timers", "Regenerative Brakes", "Heat Recapture", "Waste Not", "Trickle Charge", "Night Shift", "Load Shedding", "Power Budget", "Idle Cutoff", "Quiet Hours"]),
    insulation: theme("Insulation", ["HEAT_ISOLATION", "INCREASED_PERCENT"], 3, 9, effect("TEMPERATURE_STABILITY", 2),
        ["Mineral Wool", "Double Walls", "Ceramic Jackets", "Vacuum Gaps", "Reflective Foil", "Clay Packing", "Sealed Doors", "Thermal Blankets", "Heat Shields", "Cork Lagging", "Brick Lining", "Air Pockets"]),
    regulation: theme("Regulation", ["TEMPERATURE_STABILITY", "INCREASED_PERCENT"], 2, 6, effect("STABILITY", 2),
        ["Thermostat Loop", "Bimetal Switch", "Feedback Controller", "Setpoint Hold", "Damped Swing", "Feedback Valve", "Steady Flame", "Temperature Log", "Governor Valve", "Balanced Draft", "Trim Burner", "Pressure Relief"]),
    thermalInertia: theme("Thermal Inertia", ["COOLING_RATE", "DECREASED_PERCENT"], 2, 6, effect("HEAT_ISOLATION", 3),
        ["Heat Battery", "Thick Hearth", "Stone Mass", "Slow Cooling", "Retained Warmth", "Cast Iron Body", "Ember Bed", "Thermal Mass", "Warm Core", "Kiln Memory", "Soapstone Walls", "Sand Bath"]),
    fluidHandling: theme("Fluid Handling", ["FLUID_TRANSFER", "INCREASED_PERCENT"], 3, 9, effect("ENERGY_USAGE", 1, "DECREASED_PERCENT"),
        ["Wide Pipes", "Pump Impellers", "Smooth Bore", "Pressure Seals", "Flow Valves", "Siphon Loops", "Check Valves", "Manifold Taps", "Quick Couplers", "Bilge Pumps", "Header Tanks", "Gravity Feed"]),
    catalysis: theme("Catalysis", ["CATALYST_EFFICIENCY", "INCREASED_PERCENT"], 2, 6, effect("STABILITY", 2),
        ["Active Sites", "Porous Beds", "Reagent Dosing", "Clean Catalyst", "Surface Area", "Promoter Salts", "Reaction Bed", "Spent Recovery", "Selective Media", "Contact Time", "Mixing Vanes", "Dwell Chamber"]),
    control: theme("Control", ["CONTROL", "INCREASED_PERCENT"], 5, 15, effect("CONTROL", 5, "ADD"),
        ["Guiding Hand", "Mindful Operation", "Composure", "Fine Control", "Command Logic", "Oversight", "Discipline", "Steady Nerves", "Watchful Eye", "Clear Signals"]),
    drive: theme("Drive", ["DRIVE", "INCREASED_PERCENT"], 5, 15, effect("DRIVE", 5, "ADD"),
        ["Raw Power", "Torque Surge", "Driving Force", "Horsepower", "Brute Gearing", "Engine Heart", "Full Throttle", "Pulling Power", "Iron Will", "Hard Push"]),
    reserve: theme("Reserve", ["RESERVE", "INCREASED_PERCENT"], 5, 15, effect("RESERVE", 5, "ADD"),
        ["Deep Reserves", "Stockpile", "Endurance", "Stamina Cells", "Hoarded Charge", "Backup Supply", "Resilience", "Staying Power", "Held Breath", "Second Wind"])
};

// Home regions carry their start's identity; outer pockets blend a region with its nearest neighbor.
const REGION_THEMES = {
    control: ["precision", "economy", "calibration", "control"],
    control_drive: ["fieldWork", "actuation", "precision"],
    drive: ["momentum", "hardness", "recovery", "drive"],
    drive_reserve: ["thermalReach", "heatFlow", "startup", "fuelEconomy"],
    reserve: ["reserves", "chargeRouting", "powerSupply", "conservation", "reserve"],
    reserve_control: ["insulation", "regulation", "thermalInertia", "fluidHandling", "catalysis"]
};
const CORE_THEMES = ["control", "drive", "reserve"];

const SPECIAL_NOTABLES = {
    application: [
        { name: "Governed Draw", attribute: 0, region: "control" }, { name: "Measured Hand", attribute: 0, region: "reserve_control" },
        { name: "Torque Conversion", attribute: 1, region: "drive" }, { name: "Driven Cadence", attribute: 1, region: "control_drive" },
        { name: "Stored Momentum", attribute: 2, region: "reserve" }, { name: "Deep Cell Tap", attribute: 2, region: "drive_reserve" }
    ],
    integrated: [["Balanced Chassis", "core", null], ["Unified Frame", "outer", "control_drive"], ["Harmonic Core", "outer", "drive_reserve"],
        ["Tri Phase Design", "outer", "reserve"], ["Integrated Attributes", "outer", "reserve_control"]],
    componentMount: [["Component Mount", "drive"], ["Reinforced Mount", "drive"], ["Stage Adapter", "drive_reserve"], ["Universal Bracket", "control_drive"]],
    managedTerritory: [["Managed Territory", "control_drive"], ["Wide Canopy", "control_drive"], ["Grove Survey", "control"], ["Orchard Plan", "drive"]]
};
const SCALING = [
    { attribute: "CONTROL", stat: "ENERGY_USAGE", operation: "DECREASED_PERCENT", perPoint: 0.01 },
    { attribute: "DRIVE", stat: "PROCESSING_SPEED", operation: "INCREASED_PERCENT", perPoint: 0.08 },
    { attribute: "RESERVE", stat: "ENERGY_CAPACITY", operation: "INCREASED_PERCENT", perPoint: 0.08 }
];

// The center keystone is equally far from every start; core keystones reward any start that paths inward. The rest
// are listed clockwise from the Control start and fill the remaining slots in angle order, so family keystones stay
// near their starts.
const KEYSTONES = [
    { id: "attribute_transfiguration", layer: "core", name: "Attribute Transfiguration", effects: ATTRIBUTES.map(a => effect(a, 2, "MORE")), behaviors: ["NO_INHERENT_ATTRIBUTES"] },
    { id: "silent_operation", layer: "center", name: "Silent Operation", behaviors: ["MUTE_MACHINE_SOUND"] },
    { id: "steady_state", name: "Steady State", effects: [effect("STABILITY", 1.5, "MORE"), effect("PROCESSING_SPEED", 0.85, "LESS")] },
    { id: "deliberate_work", name: "Deliberate Work", effects: [effect("ENERGY_USAGE", 0.75, "LESS"), effect("PROCESSING_SPEED", 0.7, "LESS")] },
    { id: "manual_throttle", name: "Manual Throttle", behaviors: ["MANUAL_THROTTLE"] },
    { id: "coasting_clutch", name: "Coasting Clutch", behaviors: ["COASTING_CLUTCH"] },
    { id: "serrated_leaf_protocol", name: "Serrated Leaf Protocol", behaviors: ["SERRATED_LEAF_PROTOCOL"] },
    { id: "magnet_mode", name: "Magnet Mode", behaviors: ["MAGNET_MODE"] },
    { id: "seedling_magnet", name: "Seedling Magnet", behaviors: ["MAGNET_MODE", "SEEDLING_MAGNET"] },
    { id: "clear_cutting", name: "Clear Cutting", effects: [effect("TREE_FELL_LIMIT", 10, "ADD"), effect("PROCESSING_SPEED", 0.8, "LESS")] },
    { id: "singular_drive", layer: "core", name: "Singular Drive", effects: [effect("DRIVE", 1.5, "MORE"), effect("CONTROL", 0.5, "LESS"), effect("RESERVE", 0.5, "LESS")] },
    { id: "redline_drive", name: "Redline Drive", effects: [effect("PROCESSING_SPEED", 1.5, "MORE"), effect("ENERGY_USAGE", 1.8, "MORE")] },
    { id: "dense_batching", name: "Dense Batching", behaviors: ["DENSE_PARALLEL"], effects: [effect("PARALLEL_JOBS", 2, "ADD"), effect("PROCESSING_SPEED", 0.75, "LESS"), effect("ENERGY_USAGE", 1.25, "MORE")] },
    { id: "precision_jaw_mount", name: "Precision Jaw Mount", behaviors: ["MATCHING_HEAD"], effects: [effect("OUTPUT_AMOUNT", 10), effect("CRUSHER_SALVAGE_CHANCE", 2, "ADD")] },
    { id: "soft_material_specialist", name: "Soft Material Specialist", recipeHardnessCeiling: 2, effects: [tagged("CRUSHING", effect("PROCESSING_SPEED", 3, "MORE")), effect("ENERGY_USAGE", 1.5, "MORE")] },
    { id: "heavy_yield", name: "Heavy Yield", effects: [effect("OUTPUT_AMOUNT", 1.15, "MORE"), effect("ENERGY_USAGE", 1.5, "MORE")] },
    // Heat limits are ceilings, so a weak heat source is never raised to the limit.
    { id: "flash_annealing", name: "Flash Annealing", ceilings: { MAX_TEMPERATURE: 600 }, effects: [tagged("HEATED", effect("PROCESSING_SPEED", 2, "MORE")), effect("ENERGY_USAGE", 1.5, "MORE")] },
    { id: "low_heat_specialist", name: "Low Heat Specialist", ceilings: { MAX_TEMPERATURE: 800 }, effects: [tagged("HEATED", effect("PROCESSING_SPEED", 2, "MORE"))] },
    { id: "quench_protocol", name: "Quench Protocol", behaviors: ["QUENCH_PROTOCOL"], effects: [effect("HEAT_TRANSFER", 0.75, "LESS")] },
    { id: "closed_loop_recuperator", name: "Closed Loop Recuperator", behaviors: ["CLOSED_LOOP_RECUPERATOR"], effects: [effect("PROCESSING_SPEED", 0.85, "LESS")] },
    { id: "kiln_discipline", name: "Kiln Discipline", effects: [effect("TEMPERATURE_STABILITY", 1.6, "MORE"), effect("WARMUP_TIME", 1.4, "MORE")] },
    { id: "thermal_reservoir", name: "Thermal Reservoir", effects: [effect("COOLING_RATE", 0.4, "LESS"), effect("HEAT_TRANSFER", 0.5, "LESS")] },
    { id: "high_flux", name: "High Flux", effects: [effect("ENERGY_GENERATION", 1.5, "MORE"), effect("FUEL_DURATION", 0.6, "LESS")] },
    { id: "cell_bypass", name: "Cell Bypass", behaviors: ["BLOCK_BATTERY"], effects: [tagged("CRUSHING", effect("ENERGY_CAPACITY", 100)), effect("NO_BATTERY_OUTPUT_RETENTION", 100, "ADD"), effect("ENERGY_USAGE", 1.1, "MORE")] },
    { id: "deep_reserve", name: "Deep Reserve", effects: [effect("ENERGY_CAPACITY", 2, "MORE"), effect("PROCESSING_SPEED", 0.7, "LESS")] },
    { id: "lean_grid", name: "Lean Grid", effects: [tagged("ENERGY_BUFFER", effect("ENERGY_USAGE", 0.7, "LESS")), effect("ENERGY_CAPACITY", 0.5, "LESS")] },
    { id: "reserve_actuation", layer: "core", name: "Reserve Actuation", behaviors: ["NO_INHERENT_ATTRIBUTES"], scaling: [{ attribute: "RESERVE", stat: "PROCESSING_SPEED", operation: "INCREASED_PERCENT", perPoint: 0.5 }] },
    { id: "cold_standby", name: "Cold Standby", effects: [effect("IDLE_LOSS", 0.4, "LESS"), effect("ENERGY_TRANSFER", 0.5, "LESS")] },
    { id: "regulated_heat", name: "Regulated Heat", ceilings: { MAX_TEMPERATURE: 1000 }, effects: [effect("TEMPERATURE_STABILITY", 1.8, "MORE")] },
    { id: "measured_recovery", name: "Measured Recovery", effects: [effect("OUTPUT_AMOUNT", 1.1, "MORE"), effect("PROCESSING_SPEED", 0.65, "LESS")] },
    { id: "single_pass", extra: "single_pass", name: "Single Pass", behaviors: ["NO_BONUS_OUTPUT"], effects: [tagged("BONUS_OUTPUT", effect("PROCESSING_SPEED", 1.3, "MORE"))] }
];

// Additions placed after the original fill, in design coordinates. Output constellations mirror Clean Dust Ledge on the
// left, and Single Pass sits between them so trading bonus output for speed is a local choice.
const EXTRAS = [
    { id: "left_recovery_upper", angle: -122, radius: 3250, shapes: ["wheel"], theme: "recovery" },
    { id: "left_recovery_lower", angle: 158, radius: 3050, shapes: ["wheel"], theme: "recovery" },
    { id: "single_pass", angle: -150, radius: 2900, shapes: ["keyDirect"], keystone: true, layer: "outer" }
];

const regionAt = angle => {
    let best = 0;
    START_ANGLES.forEach((start, i) => {
        if (Math.abs(((angle - start) % 360 + 540) % 360 - 180) < Math.abs(((angle - START_ANGLES[best]) % 360 + 540) % 360 - 180)) best = i;
    });
    return best;
};
const offsetFrom = (angle, region) => ((angle - START_ANGLES[region]) % 360 + 540) % 360 - 180;

export function createMegaTree() {
    // Distances to the center keystone tie; it faces the Crusher's Drive start.
    const layout = layoutMegaTree({ starts: STARTS, totalNodes: TOTAL_NODES, keystones: KEYSTONES.filter(k => !k.extra).length, centerFacing: "drive", extras: EXTRAS });
    const byId = new Map(layout.nodes.map(node => [node.id, node]));
    const angleOf = node => Math.atan2(node.y - CENTER.y, node.x - CENTER.x) * 180 / Math.PI;
    const content = new Map();

    // Travel attributes follow the start sectors; boundary spokes carry the attribute both neighbors share.
    // Hybrid regions alternate by road, so each road reads as one attribute.
    let hubTurn = 0, roadTurn = 0;
    const roadAttribute = new Map();
    for (const node of layout.nodes.filter(n => n.kind === "TRAVEL")) {
        const angle = angleOf(node), region = regionAt(angle), radius = Math.hypot(node.x - CENTER.x, node.y - CENTER.y);
        let attribute;
        if (radius < 200) {
            attribute = hubTurn++ % 3;
        } else {
            const offset = offsetFrom(angle, region);
            const neighbor = (region + (offset < 0 ? 5 : 1)) % 6;
            const shared = START_ATTRIBUTES[region].filter(a => START_ATTRIBUTES[neighbor].includes(a));
            const options = Math.abs(offset) > 24 && shared.length ? shared : START_ATTRIBUTES[region];
            const key = `${node.group}:${options.join()}`;
            if (!roadAttribute.has(key)) roadAttribute.set(key, options[roadTurn++ % options.length]);
            attribute = roadAttribute.get(key);
        }
        const name = title(ATTRIBUTES[attribute]);
        content.set(node.id, { name, effects: [effect(ATTRIBUTES[attribute], 2, "ADD")] });
    }

    // Pockets draw the regional theme with the fewest notables so far, balancing themes across the whole tree.
    const usage = new Map(), nameUse = new Map(), usedNames = new Set(KEYSTONES.map(k => k.name));
    const pockets = layout.pockets.map(pocket => {
        const region = regionAt(pocket.angle), offset = offsetFrom(pocket.angle, region);
        const neighbor = STARTS[(region + (offset < 0 ? 5 : 1)) % 6];
        const themes = pocket.layer === "core" ? CORE_THEMES
            : pocket.layer === "home" || Math.abs(offset) < 12 ? REGION_THEMES[STARTS[region]]
                : [...REGION_THEMES[STARTS[region]], ...REGION_THEMES[neighbor]];
        const chosen = EXTRAS.find(e => e.id === pocket.extra)?.theme ?? themes.reduce((best, key) => (usage.get(key) ?? 0) < (usage.get(best) ?? 0) ? key : best);
        const notables = pocket.nodes.filter(id => byId.get(id).kind === "NOTABLE").length;
        usage.set(chosen, (usage.get(chosen) ?? 0) + notables);
        return { ...pocket, region: STARTS[region], theme: chosen };
    });
    const nextName = key => {
        const names = THEMES[key].notables;
        for (let i = nameUse.get(key) ?? 0; i < names.length; i++) {
            if (!usedNames.has(names[i])) { nameUse.set(key, i + 1); usedNames.add(names[i]); return names[i]; }
        }
        throw new Error(`Theme ${key} needs more notable names`);
    };
    const notableSlots = pockets.filter(pocket => !pocket.extra)
        .flatMap(pocket => pocket.nodes.filter(id => byId.get(id).kind === "NOTABLE").map(id => ({ id, pocket })));

    // Special notables replace ordinary notables in chosen regions and layers, one per pocket and spread apart.
    const special = new Map(), claimed = [];
    const claim = (predicate, entry) => {
        const spread = slot => Math.min(Infinity, ...claimed.map(p => Math.hypot(p.x - byId.get(slot.id).x, p.y - byId.get(slot.id).y)));
        const slot = notableSlots.filter(s => !claimed.some(p => p.id === s.pocket.id) && predicate(s.pocket))
            .reduce((best, s) => !best || spread(s) > spread(best) ? s : best, null);
        if (!slot) throw new Error(`No notable slot for ${entry.name}`);
        special.set(slot.id, entry);
        claimed.push({ id: slot.pocket.id, x: byId.get(slot.id).x, y: byId.get(slot.id).y });
        usedNames.add(entry.name);
    };
    const center = pockets.filter(p => p.layer === "core" && p.nodes.some(id => byId.get(id).kind === "NOTABLE")).sort((a, b) => a.radius - b.radius)[0];
    claim(p => p === center, { name: "Convergence", effects: ATTRIBUTES.map(a => effect(a, 10)) });
    SPECIAL_NOTABLES.application.forEach(({ name, attribute, region }) => claim(p => p.layer === "home" && p.region === region,
        { name, effects: [effect(ATTRIBUTES[attribute], 10)], scaling: [SCALING[attribute]] }));
    SPECIAL_NOTABLES.integrated.forEach(([name, layer, region]) => claim(p => p !== center && p.layer === layer && (!region || p.region === region),
        { name, effects: ATTRIBUTES.map(a => effect(a, 8)) }));
    SPECIAL_NOTABLES.componentMount.forEach(([name, region]) => claim(p => p.layer === "outer" && p.region === region,
        { name, effects: [effect("HIGH_HARDNESS_ENERGY_MITIGATION", 2, "ADD")], passive: { COMPONENT_STAGE_SUPPORT: 1 } }));
    SPECIAL_NOTABLES.managedTerritory.forEach(([name, region]) => claim(p => (p.layer === "home" || p.layer === "outer") && p.region === region,
        { name, effects: [effect("TREE_FELL_LIMIT", 1, "ADD")], passive: { MANAGED_CELLS: 8 } }));

    // Center and core keystones take their own slots; the rest follow the start order around the tree.
    const slotLayer = layer => layer === "center" || layer === "core" ? layer : "ring";
    const keystoneFor = new Map();
    for (const layer of ["center", "core", "ring"]) {
        const wanted = KEYSTONES.filter(k => !k.extra && (k.layer ?? "ring") === layer);
        const slots = layout.keystones.filter(k => !k.extra && slotLayer(k.layer) === layer).map(k => byId.get(k.id)).sort((a, b) => angleOf(a) - angleOf(b));
        if (slots.length !== wanted.length) throw new Error(`Expected ${wanted.length} ${layer} keystone slots, found ${slots.length}`);
        slots.forEach((node, i) => keystoneFor.set(node.id, wanted[i]));
    }
    for (const slot of layout.keystones.filter(k => k.extra)) keystoneFor.set(slot.id, KEYSTONES.find(k => k.extra === slot.extra));

    // Notables and keystones take IDs from their names; ordinary pocket nodes follow their pocket's first notable.
    const rename = new Map(), nodes = [], keystoneNodes = [];
    const build = (layoutId, id, fields, target = nodes) => {
        const { x, y, group } = byId.get(layoutId);
        rename.set(layoutId, id);
        target.push({ effects: [], behaviors: [], fixed: {}, ceilings: {}, scaling: [], passive: {}, id, ...fields, x, y, group });
    };
    for (const starter of layout.nodes.filter(n => n.kind === "STARTER")) {
        build(starter.id, starter.id, { name: starter.start.replaceAll("_", " / "), kind: "STARTER", start: starter.start });
    }
    for (const node of layout.nodes.filter(n => n.kind === "TRAVEL")) build(node.id, node.id, { kind: "TRAVEL", ...content.get(node.id) });
    for (const pocket of pockets) {
        const t = THEMES[pocket.theme];
        const entries = new Map(pocket.nodes.filter(id => byId.get(id).kind === "NOTABLE").map(id => [id,
            special.get(id) ?? { name: nextName(pocket.theme), effects: [effect(t.primary[0], t.notable, t.primary[1]), t.secondary] }]));
        const base = entries.size ? slug([...entries.values()][0].name) : null;
        let small = 0;
        for (const id of pocket.nodes) {
            const kind = byId.get(id).kind;
            if (kind === "NODE") build(id, `${base}_${++small}`, { name: t.name, kind, effects: [effect(t.primary[0], t.small, t.primary[1])] });
            if (kind === "NOTABLE") build(id, slug(entries.get(id).name), { kind, ...entries.get(id) });
            if (kind === "KEYSTONE") {
                const { id: keystoneId, layer, extra, ...keystone } = keystoneFor.get(id);
                build(id, keystoneId, { kind, ...keystone }, keystoneNodes);
            }
        }
        const keystoneOnly = pocket.nodes.every(id => byId.get(id).kind === "KEYSTONE");
        Object.assign(layout.groups.find(g => g.id === pocket.group), { ...(keystoneOnly ? {} : { theme: t.name }), region: pocket.region, layer: pocket.layer, gate: pocket.gate });
    }
    nodes.push(...keystoneNodes);
    const links = layout.links.map(([a, b]) => [rename.get(a), rename.get(b)]);
    if (new Set(nodes.map(n => n.id)).size !== nodes.length) throw new Error("Duplicate node ids");
    return { version: TREE_VERSION, id: "rngtech:machine_mastery", attributes: ATTRIBUTES, starts: STARTS, groups: layout.groups, nodes, links };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    const tree = createMegaTree();
    const catalog = new URL("../../src/main/resources/data/rngtech/mastery/machine_tree.json", import.meta.url);
    await writeFile(catalog, JSON.stringify(tree, null, 4) + "\n");
    const languagePath = new URL("../../src/main/resources/assets/rngtech/lang/en_us.json", import.meta.url);
    const language = JSON.parse(await readFile(languagePath, "utf8"));
    const prefix = "rngtech.mastery.shared.";
    const shared = Object.fromEntries(tree.nodes.map(node => [prefix + node.id, node.name.replace(/\b\w/g, c => c.toUpperCase())]));
    const updated = {};
    let inserted = false;
    for (const [key, value] of Object.entries(language)) {
        if (!key.startsWith(prefix)) { updated[key] = value; continue; }
        if (!inserted) { Object.assign(updated, shared); inserted = true; }
    }
    if (!inserted) Object.assign(updated, shared);
    await writeFile(languagePath, JSON.stringify(updated, null, 2) + "\n");
    console.log(`Authored ${tree.nodes.length} nodes and ${tree.links.length} links`);
}
