import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const UNIQUE_ROOT = path.join(DATA_ROOT, "rngtech/uniques");
const UNIQUE_LOOT_ROOT = path.join(DATA_ROOT, "rngtech/loot_table/uniques");
const CHALLENGE_ROOT = path.join(DATA_ROOT, "rngtech/loot_table/challenges");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const CONDITION = "rngtech:unique_loot_enabled";
const FUNCTION = "rngtech:unidentified_unique";
const CHALLENGE_TABLES = ["work_order"];
// The Work Order reward table pays a fulfilled order; check-work-orders.mjs checks its entries instead.
const REWARD_TABLES = new Set(["work_order"]);

/**
 * Unique sources stay pack-configurable and find-only: every Unique has a default loot table gated by its config key that
 * drops it unidentified and is added to vanilla loot by a loot modifier, the one shipped challenge table pays Work Orders, the
 * rngtech:uniques tag matches the catalog, and no recipe makes a Unique.
 */
export async function checkUniques({ log = true } = {}) {
    const failures = [];
    const index = await readJson(path.join(UNIQUE_ROOT, "index.json"));
    const uniques = index.uniques.map((file) => `rngtech:${file.replace(/\.json$/, "")}`);
    const modifierTables = await lootModifierTables();

    for (const unique of uniques) {
        const local = unique.slice("rngtech:".length);
        const table = await readJson(path.join(UNIQUE_LOOT_ROOT, `${local}.json`)).catch(() => null);
        if (!table) {
            failures.push(`${unique} has no default loot table loot_table/uniques/${local}.json`);
            continue;
        }
        failures.push(...defaultTableFailures(unique, table));
        if (!modifierTables.has(`rngtech:uniques/${local}`)) {
            failures.push(`no loot modifier adds rngtech:uniques/${local} to vanilla loot`);
        }
    }
    for (const file of await jsonFiles(UNIQUE_LOOT_ROOT)) {
        const unique = `rngtech:${path.basename(file, ".json")}`;
        if (!uniques.includes(unique)) {
            failures.push(`loot_table/uniques/${path.basename(file)} is not a catalog Unique`);
        }
    }

    const challengeFiles = (await jsonFiles(CHALLENGE_ROOT)).map((file) => path.basename(file, ".json")).sort();
    if (JSON.stringify(challengeFiles) !== JSON.stringify([...CHALLENGE_TABLES].sort())) {
        failures.push(`challenge tables are ${challengeFiles.join(", ")}; expected ${CHALLENGE_TABLES.join(", ")}, since machines roll no loot and only Work Orders pay`);
    }
    for (const name of challengeFiles) {
        const table = await readJson(path.join(CHALLENGE_ROOT, `${name}.json`));
        if (table.type !== "rngtech:challenge") {
            failures.push(`challenges/${name} must use the rngtech:challenge type`);
        }
        const pools = table.pools ?? [];
        if (REWARD_TABLES.has(name)) {
            continue;
        }
        for (const pool of pools) {
            if ((pool.conditions ?? []).length === 0) {
                failures.push(`challenges/${name} has a pool with no condition; every default reward is conditioned`);
            }
            for (const entry of pool.entries ?? []) {
                if (entry.name !== "rngtech:glyph") {
                    failures.push(`challenges/${name} drops ${entry.name}; only Glyphs ship by default, packs add the rest`);
                }
            }
        }
    }

    const tag = await readJson(path.join(DATA_ROOT, "rngtech/tags/item/uniques.json"));
    if (JSON.stringify([...tag.values].sort()) !== JSON.stringify([...uniques].sort())) {
        failures.push(`the rngtech:uniques tag lists ${tag.values.join(", ")}; expected the catalog ${uniques.join(", ")}`);
    }

    const nullitePartRecipes = ["phase_coil", "phase_emitter"];
    for (const file of await jsonFiles(RECIPE_ROOT)) {
        const text = await readFile(file, "utf8");
        const json = JSON.parse(text);
        const relative = path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/");
        const outputs = [json.result?.id, json.result?.stack?.id, json.result?.item, json.output?.id, json.output?.item]
                .filter(Boolean);
        for (const output of outputs) {
            if (uniques.includes(output)) {
                failures.push(`${relative} makes ${output}; Uniques are find-only`);
            }
        }
        if (text.includes('"rngtech:glyph"')) {
            failures.push(`${relative} names rngtech:glyph; no recipe makes, recycles, or burns a Glyph`);
        }
        const name = path.basename(file, ".json");
        if (nullitePartRecipes.includes(name) && path.dirname(file) === RECIPE_ROOT) {
            const conditions = json["neoforge:conditions"] ?? [];
            if (!conditions.some((condition) => condition.type === "rngtech:material_enabled" && condition.material === "nullite")) {
                failures.push(`${relative} lacks the rngtech:material_enabled nullite condition the Phase Array parts sit behind`);
            }
        }
    }

    if (failures.length) {
        throw new Error(`Unique source check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Unique sources PASS: ${uniques.length} Uniques with gated default loot, no machine challenge tables (only the Work Order reward table), no Unique or Glyph recipes, Phase Array parts behind Nullite`);
    }
    return uniques.length;
}

function defaultTableFailures(unique, table) {
    const failures = [];
    const pools = table.pools ?? [];
    const dropping = pools.filter((pool) => (pool.entries ?? []).some((entry) => entry.name === unique));
    if (dropping.length === 0) {
        failures.push(`the default table for ${unique} never drops it`);
    }
    for (const pool of pools) {
        const gated = (pool.conditions ?? []).some((condition) => condition.condition === CONDITION && condition.unique === unique);
        if (!gated) {
            failures.push(`a pool in the default table for ${unique} lacks ${CONDITION} for ${unique}`);
        }
        for (const entry of pool.entries ?? []) {
            if (!(entry.functions ?? []).some((fn) => fn.function === FUNCTION)) {
                failures.push(`an entry in the default table for ${unique} does not apply ${FUNCTION}`);
            }
        }
    }
    return failures;
}

async function lootModifierTables() {
    const global = await readJson(path.join(DATA_ROOT, "neoforge/loot_modifiers/global_loot_modifiers.json"));
    const tables = new Set();
    for (const id of global.entries) {
        const [namespace, local] = id.split(":");
        const modifier = await readJson(path.join(DATA_ROOT, namespace, "loot_modifiers", `${local}.json`)).catch(() => null);
        if (modifier?.table) {
            tables.add(modifier.table);
        }
    }
    return tables;
}

async function jsonFiles(root) {
    const files = [];
    for (const entry of await readdir(root, { recursive: true, withFileTypes: true }).catch(() => [])) {
        if (entry.isFile() && entry.name.endsWith(".json")) {
            files.push(path.join(entry.parentPath, entry.name));
        }
    }
    return files;
}

async function readJson(file) {
    return JSON.parse(await readFile(file, "utf8"));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkUniques();
}
