import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const OBJECTIVE_ROOT = path.join(DATA_ROOT, "rngtech/work_orders/objectives");
const REWARD_TABLE = path.join(DATA_ROOT, "rngtech/loot_table/challenges/work_order.json");
const UNIQUE_ROOT = path.join(DATA_ROOT, "rngtech/uniques");
const UNIQUE_REWARD = "rngtech:work_orders/unique_reward";
const UNIQUE_REWARD_TABLE = path.join(DATA_ROOT, "rngtech/loot_table/work_orders/unique_reward.json");
const ORDER = "rngtech:job_chip";
const FULFILLED = "rngtech:completed_job_chip";
const REQUISITION = "rngtech:requisition";
const TERMINAL = "rngtech:dispatch_terminal";
const CONDITION = "rngtech:work_order_recipes_enabled";
const RECLAIM_TYPES = new Set(["rngtech:component_recycling", "rngtech:potential_reactor"]);
const FAMILIES = ["crusher", "furnace"];
// Rewards are Requisitions, refinement currency (the Mirror Catalyst included), Glyphs, and Uniques: nothing with FE value or a
// place in the processing chain.
const REWARD_ITEMS = new Set([
    REQUISITION,
    "rngtech:glyph",
    "rngtech:chaos_crystal",
    "rngtech:expansion_crystal",
    "rngtech:null_crystal",
    "rngtech:affix_modifier",
    "rngtech:affix_upgrade",
    "rngtech:mastery_refund",
    "rngtech:volatile_catalyst",
    "rngtech:ascension_matrix",
    "rngtech:mirror_catalyst",
]);

// The Work Order families whose machines have a Gear slot for each Unique host. Battery Cells power every machine, so they
// roll for any job with no family entry.
const HOST_FAMILIES = {
    crush_head: ["crusher", "melter"],
    heat_core: ["alloy_furnace", "furnace", "melter", "metal_press"],
    alloy_crucible: ["alloy_furnace"],
    servo: ["alloy_furnace", "melter", "metal_press"],
    fluid_pump: ["forestry", "melter"],
    control_board: ["resonance_calibrator"],
    disassembly_head: ["component_recycler"],
    battery_cell: [],
};

/**
 * Work Orders stay a closed loop: orders come from the Dispatch Terminal and commands, so no recipe makes one or a Fulfilled
 * Work Order; the Requisition and Dispatch Terminal recipes carry the config condition; no recycling or reactor recipe takes
 * any of them back; the reward table is one pool that pays one kind: Requisitions, currency, Glyphs, or a Unique, whose
 * entries are Prestige bands that nest the Unique table; that table rolls each Unique once from the whole catalog, plus a
 * family entry for exactly the machines its part fits; and every family with progress hooks has an objective with one goal
 * per tier. masteryCheck holds the listed Unique rate between 1 in 200 and 1 in 100.
 */
export async function checkWorkOrders({ log = true } = {}) {
    const failures = [];
    let recipes = 0;
    const gated = new Set();
    for (const { file, json } of await jsonFiles(RECIPE_ROOT)) {
        recipes++;
        const relative = path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/");
        const output = json.result?.id ?? json.result?.stack?.id;
        if (output === ORDER || output === FULFILLED) {
            failures.push(`${relative} crafts ${output}; orders come from the Dispatch Terminal`);
        }
        if (output === REQUISITION || output === TERMINAL) {
            gated.add(output);
            if (!(json["neoforge:conditions"] ?? []).some((condition) => condition.type === CONDITION)) {
                failures.push(`${relative} makes ${output} without the ${CONDITION} condition`);
            }
        }
        if (RECLAIM_TYPES.has(json.type)) {
            const text = JSON.stringify(json.ingredient ?? json.ingredients ?? json.input ?? {});
            for (const item of [ORDER, FULFILLED, REQUISITION, TERMINAL]) {
                if (text.includes(`"${item}"`)) {
                    failures.push(`${relative} is a ${json.type} recipe for ${item}`);
                }
            }
        }
    }

    for (const item of [REQUISITION, TERMINAL]) {
        if (!gated.has(item)) {
            failures.push(`No ${item} recipe was found`);
        }
    }

    const table = JSON.parse(await readFile(REWARD_TABLE, "utf8"));
    if (table.type !== "rngtech:challenge") {
        failures.push("challenges/work_order must use the rngtech:challenge type");
    }
    let uniqueBands = 0;
    for (const pool of table.pools ?? []) {
        for (const entry of pool.entries ?? []) {
            if (entry.type === "minecraft:empty") {
                continue;
            }
            if (entry.type === "minecraft:loot_table") {
                uniqueBands++;
                const tests = (entry.conditions ?? []).map((condition) => condition.condition);
                if (entry.value !== UNIQUE_REWARD) {
                    failures.push(`challenges/work_order nests ${JSON.stringify(entry.value)}; only ${UNIQUE_REWARD} may be nested`);
                } else if (!tests.includes("rngtech:unique_loot_enabled") || !tests.includes("rngtech:order_prestige")) {
                    failures.push(`challenges/work_order's Unique entries need rngtech:unique_loot_enabled and an rngtech:order_prestige band`);
                }
            } else if (!REWARD_ITEMS.has(entry.name)) {
                failures.push(`challenges/work_order drops ${entry.name}; rewards are currency, Glyphs, and the Unique table`);
            }
        }
    }
    if (uniqueBands === 0) {
        failures.push(`challenges/work_order never rolls ${UNIQUE_REWARD}`);
    }

    const uniqueTable = JSON.parse(await readFile(UNIQUE_REWARD_TABLE, "utf8"));
    if (uniqueTable.type !== "rngtech:challenge" || (uniqueTable.pools ?? []).length !== 1 || uniqueTable.pools[0].rolls !== 1) {
        failures.push(`${UNIQUE_REWARD} must be one rngtech:challenge pool with one roll, so it pays one Unique`);
    }
    let uniqueEntries = 0;
    const uniqueRolls = new Map();
    for (const entry of uniqueTable.pools?.[0]?.entries ?? []) {
        uniqueEntries++;
        if (!(entry.conditions ?? []).some((condition) => condition.condition === "rngtech:unique_loot_enabled" && condition.unique === entry.name)) {
            failures.push(`${UNIQUE_REWARD} drops ${entry.name} without its own rngtech:unique_loot_enabled gate`);
        }
        if (!(entry.functions ?? []).some((fn) => fn.function === "rngtech:unidentified_unique")) {
            failures.push(`${UNIQUE_REWARD} drops ${entry.name} identified`);
        }
        const families = familyTerms(entry.conditions ?? []);
        const rolls = uniqueRolls.get(entry.name) ?? { catalog: 0, family: [] };
        if (families.length === 0) {
            rolls.catalog++;
        } else {
            rolls.family.push(families.sort());
        }
        uniqueRolls.set(entry.name, rolls);
    }

    for (const [unique, rolls] of uniqueRolls) {
        const host = JSON.parse(await readFile(path.join(UNIQUE_ROOT, `${unique.slice("rngtech:".length)}.json`), "utf8")).host;
        const fits = HOST_FAMILIES[host];
        if (fits === undefined) {
            failures.push(`${unique} is a ${host}; add its Work Order families to HOST_FAMILIES`);
            continue;
        }
        if (rolls.catalog !== 1) {
            failures.push(`${UNIQUE_REWARD} rolls ${unique} from the whole catalog ${rolls.catalog} times; expected once`);
        }
        const expected = fits.length === 0 ? [] : [JSON.stringify(fits)];
        const actual = rolls.family.map((families) => JSON.stringify(families));
        if (JSON.stringify(actual) !== JSON.stringify(expected)) {
            failures.push(`${UNIQUE_REWARD} favours ${unique} on ${actual.join(", ") || "no family"}; a ${host} fits ${fits.join(", ") || "every machine"}`);
        }
    }

    if ((table.pools ?? []).length !== 1 || table.pools[0].rolls !== 1) {
        failures.push("challenges/work_order must be one pool with one roll, so every job pays one reward kind");
    }

    const families = new Map();
    for (const { file, json } of await jsonFiles(OBJECTIVE_ROOT)) {
        const relative = path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/");
        if ((json.tiers ?? []).length !== 5) {
            failures.push(`${relative} needs one goal for each of the five tiers`);
        }
        families.set(json.family, (families.get(json.family) ?? 0) + 1);
    }
    for (const family of FAMILIES) {
        if (!families.has(family)) {
            failures.push(`no Work Order objective for the ${family}`);
        }
    }

    if (failures.length) {
        throw new Error(`Work Order check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Work Orders PASS: none of ${recipes} recipes crafts an order, Requisition and terminal recipes carry ${CONDITION}, `
            + `rewards are Requisitions, currency, Glyphs, and ${uniqueBands} Prestige bands of ${uniqueRolls.size} Uniques in ${uniqueEntries} entries, favoured on the machines they fit; `
            + `objectives for ${[...families.keys()].sort().join(", ")}`);
    }
    return recipes;
}

/** The families an entry's rngtech:machine_family conditions name, alone or inside minecraft:any_of. */
function familyTerms(conditions) {
    return conditions.flatMap((condition) => condition.condition === "rngtech:machine_family" ? [condition.family]
            : condition.condition === "minecraft:any_of" ? familyTerms(condition.terms ?? []) : []);
}

async function jsonFiles(root) {
    let entries;
    try {
        entries = await readdir(root, { recursive: true, withFileTypes: true });
    } catch (error) {
        if (error.code === "ENOENT") {
            return [];
        }
        throw error;
    }
    const files = [];
    for (const entry of entries) {
        if (entry.isFile() && entry.name.endsWith(".json")) {
            const file = path.join(entry.parentPath, entry.name);
            files.push({ file, json: JSON.parse(await readFile(file, "utf8")) });
        }
    }
    return files;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkWorkOrders();
}
