import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const BLOCK_LOOT_ROOT = path.join(DATA_ROOT, "rngtech/loot_table/blocks");
const CATALYST = "rngtech:mirror_catalyst";
const MIRRORED = "rngtech:mirrored";
const LOOT_DIRECTORIES = ["loot_table", "loot_tables", "loot_modifiers"];
const RECLAIM_TYPES = new Set(["rngtech:component_recycling", "rngtech:potential_reactor"]);
const WORK_ORDER_REWARDS = "rngtech/loot_table/challenges/work_order.json";
const MIN_TIER = 4;

/**
 * The Mirror Catalyst has one source: a Work Order of Tier IV or V may list it. No recipe makes it, no other loot table
 * or loot modifier hands it out, and no recycling or Potential Reactor recipe takes it. A Mirrored machine stays Mirrored
 * when broken, so every block loot table that copies a machine's traits copies the Mirrored component too.
 */
export async function checkMirrorCatalyst({ log = true } = {}) {
    const failures = [];
    for (const { file, json } of await jsonFiles(RECIPE_ROOT)) {
        const relative = path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/");
        const output = json.result?.id ?? json.result?.stack?.id;
        if (output === CATALYST) {
            failures.push(`${relative} makes ${CATALYST}; it comes only from Work Orders`);
        }
        if (RECLAIM_TYPES.has(json.type) && JSON.stringify(json).includes(`"${CATALYST}"`)) {
            failures.push(`${relative} is a ${json.type} recipe for ${CATALYST}`);
        }
    }

    for (const namespace of await readdir(DATA_ROOT)) {
        for (const directory of LOOT_DIRECTORIES) {
            for (const { file, text } of await jsonFiles(path.join(DATA_ROOT, namespace, directory), true)) {
                const relative = path.relative(DATA_ROOT, file).replaceAll(path.sep, "/");
                if (text.includes(`"${CATALYST}"`) && relative !== WORK_ORDER_REWARDS) {
                    failures.push(`${path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/")} hands out ${CATALYST}`);
                }
            }
        }
    }

    const rewards = JSON.parse(await readFile(path.join(DATA_ROOT, WORK_ORDER_REWARDS), "utf8"));
    const entries = (rewards.pools ?? []).flatMap((pool) => pool.entries ?? []).filter((entry) => entry.name === CATALYST);
    if (entries.length === 0) {
        failures.push(`challenges/work_order never lists ${CATALYST}`);
    }
    for (const entry of entries) {
        const tier = (entry.conditions ?? []).find((condition) => condition.condition === "rngtech:order_tier");
        if (!tier || (tier.min ?? 0) < MIN_TIER) {
            failures.push(`challenges/work_order lists ${CATALYST} below Tier ${MIN_TIER}`);
        }
        if ((entry.functions ?? []).length) {
            failures.push(`challenges/work_order changes the ${CATALYST} amount; it always pays one`);
        }
    }

    let machines = 0;
    for (const { file, json } of await jsonFiles(BLOCK_LOOT_ROOT)) {
        for (const pool of json.pools ?? []) {
            for (const entry of pool.entries ?? []) {
                for (const fn of entry.functions ?? []) {
                    if (fn.function !== "minecraft:copy_components" || !(fn.include ?? []).includes("rngtech:machine_traits")) {
                        continue;
                    }
                    machines++;
                    if (!fn.include.includes(MIRRORED)) {
                        failures.push(`${path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/")} copies machine traits without ${MIRRORED}`);
                    }
                }
            }
        }
    }

    if (failures.length) {
        throw new Error(`Mirror Catalyst check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Mirror Catalyst PASS: no recipe, no recycling source, ${entries.length} Work Order entries from Tier ${MIN_TIER}, `
            + `${machines} machine loot tables keep ${MIRRORED}`);
    }
    return entries.length;
}

async function jsonFiles(root, raw = false) {
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
        if (!entry.isFile() || !entry.name.endsWith(".json")) {
            continue;
        }
        const file = path.join(entry.parentPath, entry.name);
        const text = await readFile(file, "utf8");
        files.push(raw ? { file, text } : { file, json: JSON.parse(text) });
    }
    return files;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkMirrorCatalyst();
}
