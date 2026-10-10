import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { checkRecipeLoops } from "./check-recipe-loops.mjs";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const CATALYST = "rngtech:volatile_catalyst";
const JAM_DEBRIS = "rngtech:jam_debris";
const CONDITION = "rngtech:volatile_catalyst_recipes_enabled";
const LOOT_DIRECTORIES = ["loot_table", "loot_tables", "loot_modifiers"];
const RECLAIM_TYPES = new Set(["rngtech:component_recycling", "rngtech:potential_reactor"]);

/**
 * The Volatile Catalyst is gated by RNGTech challenges only: every recipe that makes it carries the config condition so
 * pack makers can turn it off, no loot table or loot modifier hands it out, and neither the catalyst nor Jam Debris can
 * be recycled or fed to the Potential Reactor. The loop audit must see Jam Debris as a Crusher jam output.
 */
export async function checkVolatileCatalyst({ log = true } = {}) {
    const failures = [];
    let recipes = 0;
    for (const { file, json } of await jsonFiles(RECIPE_ROOT)) {
        const relative = path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/");
        const output = json.result?.id ?? json.result?.stack?.id;
        if (output === CATALYST) {
            recipes++;
            if (!(json["neoforge:conditions"] ?? []).some((condition) => condition.type === CONDITION)) {
                failures.push(`${relative} makes ${CATALYST} without the ${CONDITION} condition`);
            }
        }
        if (RECLAIM_TYPES.has(json.type)) {
            const text = JSON.stringify(json.ingredient ?? json.ingredients ?? json.input ?? {});
            for (const item of [CATALYST, JAM_DEBRIS]) {
                if (text.includes(`"${item}"`)) {
                    failures.push(`${relative} is a ${json.type} recipe for ${item}`);
                }
            }
        }
    }
    if (recipes === 0) {
        failures.push(`No ${CATALYST} recipe was found`);
    }

    for (const namespace of await readdir(DATA_ROOT)) {
        for (const directory of LOOT_DIRECTORIES) {
            for (const { file, text } of await jsonFiles(path.join(DATA_ROOT, namespace, directory), true)) {
                if (text.includes(`"${CATALYST}"`)) {
                    failures.push(`${path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/")} hands out ${CATALYST}`);
                }
            }
        }
    }

    const audit = await checkRecipeLoops({ log: false });
    if (!audit.jamDebrisSources) {
        failures.push("The recipe loop audit counts no Crusher jam as a Jam Debris source");
    }

    if (failures.length) {
        throw new Error(`Volatile Catalyst check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Volatile Catalyst PASS: ${recipes} recipe(s) carry ${CONDITION}, no loot or recycling source, `
            + `${audit.jamDebrisSources} Crusher recipes jam into Jam Debris`);
    }
    return recipes;
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
    await checkVolatileCatalyst();
}
