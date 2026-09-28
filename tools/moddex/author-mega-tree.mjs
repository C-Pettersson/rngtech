import { mkdir, readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { layoutMegaTree } from "./layout-mega-tree.mjs";

// Deterministic authoring source. Runtime and ModDex read the same checked-in catalog.
export function createMegaTree() {
    const attributes = ["CONTROL", "DRIVE", "RESERVE"];
    const starts = ["control", "control_drive", "drive", "drive_reserve", "reserve", "reserve_control"];
    const pairs = [[0], [0, 1], [1], [1, 2], [2], [2, 0]];
    const nodes = [], groups = [];
    const effect = (stat, value, operation = "INCREASED_PERCENT") => ({ stat, operation, value });
    const theme = (name, stat, small, notable, operation = "INCREASED_PERCENT") => ({ name, stat, small, notable, operation });
    const themes = [
        theme("Precision", "STABILITY", 2, 6), theme("Economy", "ENERGY_USAGE", 1, 3, "DECREASED_PERCENT"),
        theme("Actuation", "PROCESSING_SPEED", 2, 6), theme("Recovery", "OUTPUT_AMOUNT", 0.5, 1.5),
        theme("Momentum", "PROCESSING_SPEED", 2, 6), theme("Hardness", "HIGH_HARDNESS_ENERGY_MITIGATION", 1, 3, "ADD"),
        theme("Thermal Reach", "MAX_TEMPERATURE", 1, 3), theme("Heat Flow", "HEAT_TRANSFER", 2, 6),
        theme("Reserves", "ENERGY_CAPACITY", 3, 9), theme("Insulation", "HEAT_ISOLATION", 3, 9),
        theme("Regulation", "TEMPERATURE_STABILITY", 2, 6), theme("Conservation", "ENERGY_USAGE", 1, 3, "DECREASED_PERCENT"),
        theme("Control", "CONTROL", 5, 15), theme("Drive", "DRIVE", 5, 15),
        theme("Reserve", "RESERVE", 5, 15), theme("Fluid Handling", "FLUID_TRANSFER", 3, 9),
        theme("Power Supply", "ENERGY_GENERATION", 2, 6), theme("Fuel Economy", "FUEL_DURATION", 2, 6),
        theme("Thermal Inertia", "COOLING_RATE", 2, 6, "DECREASED_PERCENT"), theme("Startup", "WARMUP_TIME", 2, 6, "DECREASED_PERCENT"),
        theme("Field Work", "TREE_FELL_LIMIT", 1, 2, "ADD"), theme("Charge Routing", "ENERGY_TRANSFER", 3, 9),
        theme("Catalysis", "CATALYST_EFFICIENCY", 2, 6), theme("Calibration", "CALIBRATION_PRECISION", 2, 6)
    ];
    const add = (node) => {
        nodes.push({ effects: [], behaviors: [], fixed: {}, ceilings: {}, scaling: [], passive: {}, ...node });
        return node.id;
    };
    for (let s = 0; s < 6; s++) {
        add({ id: `start_${starts[s]}`, name: starts[s].replaceAll("_", " / "), kind: "STARTER", x: 0, y: 0, group: `start_${s}`, start: starts[s] });
    }
    for (let ring = 0; ring < 5; ring++) {
        for (let g = 0; g < 12; g++) {
            const sector = Math.floor(g / 2);
            const id = `r${ring}_${starts[sector]}_${g % 2}`;
            const t = themes[(g + ring * 5) % themes.length];
            groups.push({ id, x: 0, y: 0, region: starts[sector], rewardBand: ring, theme: t.name });
            for (let n = 0; n < 12; n++) {
                const travel = [0, 2, 3, 6, 8, 9].includes(n);
                const notable = n === 4 || n === 10;
                const node = { id: `${id}_${n}`, name: travel ? "Attribute" : `${t.name}${notable ? " Mastery" : ""}`, kind: travel ? "TRAVEL" : notable ? "NOTABLE" : "NODE", x: 0, y: 0, group: id };
                if (travel) {
                    const pair = pairs[sector];
                    node.effects = [effect(attributes[pair[n % pair.length]], 2, "ADD")];
                } else {
                    node.effects = [effect(t.stat, notable ? t.notable : t.small, t.operation)];
                    if (notable && ring === 2) {
                        node.name = `${attributes[sector % 3][0]}${attributes[sector % 3].slice(1).toLowerCase()} Application`;
                        node.scaling = [{ attribute: attributes[sector % 3], stat: ["ENERGY_USAGE", "PROCESSING_SPEED", "ENERGY_CAPACITY"][sector % 3], operation: sector % 3 === 0 ? "DECREASED_PERCENT" : "INCREASED_PERCENT", perPoint: sector % 3 === 0 ? 0.01 : 0.08 }];
                    }
                    if (notable && ring === 3 && g % 3 === 0) {
                        node.name = "Integrated Attributes";
                        node.effects = attributes.map(a => effect(a, 8));
                    }
                    if (ring === 1 && n === 10 && g % 3 === 1) {
                        node.name = "Component Mount";
                        node.passive = { COMPONENT_STAGE_SUPPORT: 1 };
                    }
                    if (ring === 1 && n === 10 && g % 3 === 2) {
                        node.name = "Managed Territory";
                        node.passive = { MANAGED_CELLS: 8 };
                    }
                }
                add(node);
            }
        }
    }
    const keys = [
        { id: "attribute_transfiguration", name: "Attribute Transfiguration", effects: attributes.map(a => effect(a, 2, "MORE")), behaviors: ["NO_INHERENT_ATTRIBUTES"] },
        { id: "silent_operation", name: "Silent Operation", behaviors: ["MUTE_MACHINE_SOUND"] },
        { id: "magnet_mode", name: "Magnet Mode", behaviors: ["MAGNET_MODE"] },
        { id: "serrated_leaf_protocol", name: "Serrated Leaf Protocol", behaviors: ["SERRATED_LEAF_PROTOCOL"] },
        { id: "manual_throttle", name: "Manual Throttle", behaviors: ["MANUAL_THROTTLE"] },
        { id: "coasting_clutch", name: "Coasting Clutch", behaviors: ["COASTING_CLUTCH"] },
        { id: "seedling_magnet", name: "Seedling Magnet", behaviors: ["MAGNET_MODE", "SEEDLING_MAGNET"] },
        { id: "precision_jaw_mount", name: "Precision Jaw Mount", behaviors: ["MATCHING_HEAD"], effects: [effect("OUTPUT_AMOUNT", 10), effect("CRUSHER_SALVAGE_CHANCE", 2, "ADD")] },
        { id: "dense_batching", name: "Dense Batching", behaviors: ["DENSE_PARALLEL"], effects: [effect("PARALLEL_JOBS", 2, "ADD"), effect("PROCESSING_SPEED", 0.75, "LESS"), effect("ENERGY_USAGE", 1.25, "MORE")] },
        { id: "soft_material_specialist", name: "Soft Material Specialist", recipeHardnessCeiling: 2, effects: [effect("PROCESSING_SPEED", 3, "MORE"), effect("ENERGY_USAGE", 1.5, "MORE")] },
        { id: "cell_bypass", name: "Cell Bypass", behaviors: ["BLOCK_BATTERY"], effects: [effect("ENERGY_CAPACITY", 100), effect("NO_BATTERY_OUTPUT_RETENTION", 100, "ADD"), effect("ENERGY_USAGE", 1.1, "MORE")] },
        { id: "redline_drive", name: "Redline Drive", effects: [effect("PROCESSING_SPEED", 1.5, "MORE"), effect("ENERGY_USAGE", 1.8, "MORE")] },
        { id: "low_heat_specialist", name: "Low Heat Specialist", fixed: { MAX_TEMPERATURE: 800 }, effects: [effect("PROCESSING_SPEED", 2, "MORE")] },
        { id: "flash_annealing", name: "Flash Annealing", fixed: { MAX_TEMPERATURE: 600 }, effects: [effect("PROCESSING_SPEED", 2, "MORE"), effect("ENERGY_USAGE", 1.5, "MORE")] },
        { id: "quench_protocol", name: "Quench Protocol", behaviors: ["QUENCH_PROTOCOL"], effects: [effect("HEAT_TRANSFER", 0.75, "LESS")] },
        { id: "closed_loop_recuperator", name: "Closed Loop Recuperator", behaviors: ["CLOSED_LOOP_RECUPERATOR"], effects: [effect("PROCESSING_SPEED", 0.85, "LESS")] },
        { id: "thermal_reservoir", name: "Thermal Reservoir", effects: [effect("COOLING_RATE", 0.4, "LESS"), effect("HEAT_TRANSFER", 0.5, "LESS")] },
        { id: "cold_standby", name: "Cold Standby", effects: [effect("IDLE_LOSS", 0.4, "LESS"), effect("ENERGY_TRANSFER", 0.5, "LESS")] },
        { id: "high_flux", name: "High Flux", effects: [effect("ENERGY_GENERATION", 1.5, "MORE"), effect("FUEL_DURATION", 0.6, "LESS")] },
        { id: "deep_reserve", name: "Deep Reserve", effects: [effect("ENERGY_CAPACITY", 2, "MORE"), effect("PROCESSING_SPEED", 0.7, "LESS")] },
        { id: "regulated_heat", name: "Regulated Heat", ceilings: { MAX_TEMPERATURE: 1000 }, effects: [effect("TEMPERATURE_STABILITY", 1.8, "MORE")] },
        { id: "deliberate_work", name: "Deliberate Work", effects: [effect("ENERGY_USAGE", 0.75, "LESS"), effect("PROCESSING_SPEED", 0.7, "LESS")] },
        { id: "measured_recovery", name: "Measured Recovery", effects: [effect("OUTPUT_AMOUNT", 1.1, "MORE"), effect("PROCESSING_SPEED", 0.65, "LESS")] },
        { id: "reserve_actuation", name: "Reserve Actuation", behaviors: ["NO_INHERENT_ATTRIBUTES"], scaling: [{ attribute: "RESERVE", stat: "PROCESSING_SPEED", operation: "INCREASED_PERCENT", perPoint: 0.5 }] }
    ];
    keys.forEach(key => add({ ...key, kind: "KEYSTONE", group: `keystone_${key.id}`, x: 0, y: 0 }));
    return layoutMegaTree({ version: 1, id: "rngtech:machine_mastery", attributes, starts, groups, nodes, links: [] });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    const directory = new URL("../../src/main/resources/data/rngtech/mastery/", import.meta.url);
    await mkdir(directory, { recursive: true });
    const tree = createMegaTree();
    await writeFile(new URL("machine_tree.json", directory), JSON.stringify(tree, null, 4) + "\n");
    const languagePath = new URL("../../src/main/resources/assets/rngtech/lang/en_us.json", import.meta.url);
    const language = JSON.parse(await readFile(languagePath, "utf8"));
    for (const node of tree.nodes) language[`rngtech.mastery.shared.${node.id}`] = node.name.replace(/\b\w/g, c => c.toUpperCase());
    await writeFile(languagePath, JSON.stringify(language, null, 2) + "\n");
    console.log(`Authored ${tree.nodes.length} nodes and ${tree.links.length} links`);
}
