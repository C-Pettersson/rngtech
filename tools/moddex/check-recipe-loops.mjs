import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");

/**
 * A calibrated component that recycles back into its calibration input forms a cycle,
 * so both recipes must set `bonus_output: false` or Super Output grows the input.
 */
export async function checkCalibrationRecyclingLoops({ log = true } = {}) {
    const recipes = await readRecipes(RECIPE_ROOT);
    const tags = new Map();
    const recyclers = [];
    for (const recipe of recipes.filter((entry) => entry.json.type === "rngtech:component_recycling")) {
        recyclers.push({ ...recipe, inputs: await ingredientItems(recipe.json.ingredient, tags) });
    }
    const failures = new Set();
    let loops = 0;
    for (const calibration of recipes.filter((entry) => entry.json.type === "rngtech:calibration")) {
        const inputs = await ingredientItems(calibration.json.ingredient, tags);
        const output = calibration.json.result?.stack?.id;
        for (const recycler of recyclers) {
            if (!recycler.inputs.has(output) || !recycler.json.outputs?.some((entry) => inputs.has(entry.stack?.id))) {
                continue;
            }
            loops++;
            for (const recipe of [calibration, recycler]) {
                if (recipe.json.bonus_output !== false) {
                    failures.add(`${recipe.id} must set "bonus_output": false (cycle with ${recipe === calibration ? recycler.id : calibration.id})`);
                }
            }
        }
    }
    if (failures.size) {
        throw new Error(`Recipe loop validation failed:\n${[...failures].map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Recipe loop audit PASS: ${loops} calibration/recycling cycles opt out of bonus output`);
    }
    return loops;
}

async function readRecipes(root) {
    const recipes = [];
    for (const entry of await readdir(root, { recursive: true, withFileTypes: true })) {
        if (!entry.isFile() || !entry.name.endsWith(".json")) {
            continue;
        }
        const file = path.join(entry.parentPath, entry.name);
        const id = `rngtech:${path.relative(root, file).replaceAll(path.sep, "/").replace(/\.json$/, "")}`;
        recipes.push({ id, json: JSON.parse(await readFile(file, "utf8")) });
    }
    return recipes;
}

async function ingredientItems(ingredient, tags) {
    const items = new Set();
    for (const entry of [ingredient ?? []].flat()) {
        if (entry.item) {
            items.add(entry.item);
        } else if (entry.tag) {
            for (const item of await tagItems(entry.tag, tags)) {
                items.add(item);
            }
        }
    }
    return items;
}

async function tagItems(tagId, tags) {
    if (!tags.has(tagId)) {
        tags.set(tagId, new Set());
        const [namespace, tagPath] = tagId.split(":");
        let values = [];
        try {
            values = JSON.parse(await readFile(path.join(DATA_ROOT, namespace, "tags/item", `${tagPath}.json`), "utf8")).values ?? [];
        } catch (error) {
            if (error.code !== "ENOENT") {
                throw error;
            }
        }
        const items = new Set();
        for (const value of values) {
            const id = typeof value === "string" ? value : value.id;
            for (const item of id.startsWith("#") ? await tagItems(id.slice(1), tags) : [id]) {
                items.add(item);
            }
        }
        tags.set(tagId, items);
    }
    return tags.get(tagId);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkCalibrationRecyclingLoops();
}
