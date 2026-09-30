import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");

const RECYCLING = "rngtech:component_recycling";
const CALIBRATION = "rngtech:calibration";
const SHAPED = new Set(["minecraft:crafting_shaped", "rngtech:trait_shaped", "rngtech:calibrated_shaped"]);
const SHAPELESS = new Set(["minecraft:crafting_shapeless", "rngtech:battery_assembly"]);

const WOOD_TYPES = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"];

/** Vanilla item tags that recipes making recyclable items reference but this repository does not define. */
const VANILLA_TAGS = {
    "minecraft:planks": WOOD_TYPES.map((wood) => `minecraft:${wood}_planks`),
    "minecraft:wooden_slabs": WOOD_TYPES.map((wood) => `minecraft:${wood}_slab`),
    "minecraft:wooden_buttons": WOOD_TYPES.map((wood) => `minecraft:${wood}_button`)
};

/**
 * Finds craft/recycle cycles that return more of an item than the craft consumed. This is a per-recipe rule that the
 * general loop audit in `check-recipe-loops.mjs` does not cover: a recycling return may not exceed what any recipe making
 * its input consumed of the same item, even when the rest of the craft is lost.
 *
 * Each Component Recycler recipe is compared with every recipe that makes its input, and outputs
 * behind a Recovery Filter count. An output is a finding when recycling returns more than the craft
 * consumed, or when Super Output makes the cycle meet or exceed that amount. The Resonance
 * Calibrator's Super Output adds one result unless the calibration sets `bonus_output: false`.
 * The Component Recycler has no Super Output; `recyclerSuperOutput` models one copy of every
 * output, treating all outputs as stackable, for the report and the self-check.
 */
export async function findRecyclingLoops({ recyclerSuperOutput = false } = {}) {
    const recipes = await readRecipes(RECIPE_ROOT);
    const tags = new Map();
    const producers = new Map();
    for (const recipe of recipes) {
        if (recipe.json.type === RECYCLING) {
            continue;
        }
        for (const result of recipeResults(recipe.json)) {
            if (!producers.has(result.id)) {
                producers.set(result.id, []);
            }
            producers.get(result.id).push({ recipe, count: result.count });
        }
    }

    const findings = [];
    let cycles = 0;
    for (const recycler of recipes.filter((entry) => entry.json.type === RECYCLING)) {
        for (const input of await ingredientItems(recycler.json.ingredient, tags, recycler.id)) {
            for (const producer of producers.get(input) ?? []) {
                const consumed = await consumption(producer.recipe, tags);
                const producerYield = producer.recipe.json.type === CALIBRATION && producer.recipe.json.bonus_output !== false;
                const made = producer.count + (producerYield ? 1 : 0);
                for (const output of recycler.json.outputs ?? []) {
                    const item = output.stack?.id;
                    if (!consumed.has(item)) {
                        continue;
                    }
                    cycles++;
                    const each = output.stack.count ?? 1;
                    const base = each * producer.count;
                    const returned = each * (recyclerSuperOutput ? 2 : 1) * made;
                    const spent = consumed.get(item);
                    if (base > spent || (returned > base && returned >= spent)) {
                        findings.push({
                            recycler: recycler.id,
                            producer: producer.recipe.id,
                            input,
                            item,
                            consumed: spent,
                            made: producer.count,
                            base,
                            returned,
                            filter: output.requires_filter === true,
                            recyclerYield: recyclerSuperOutput,
                            producerYield
                        });
                    }
                }
            }
        }
    }
    return { findings, cycles };
}

/** Fails on any craft/recycle loop, and when modelling Recycler Super Output no longer finds one. */
export async function checkRecyclingReturns({ log = true } = {}) {
    const { findings, cycles } = await findRecyclingLoops();
    if (findings.length) {
        throw new Error(`Recycling return check failed: ${summary(findings)}\n${findings.map((finding) => `- ${describe(finding)}`).join("\n")}`);
    }
    if (!(await findRecyclingLoops({ recyclerSuperOutput: true })).findings.length) {
        throw new Error("Recycling return check self-test failed: modelling Recycler Super Output no longer finds a loop");
    }
    if (log) {
        console.log(`Recycling returns PASS: ${cycles} craft/recycle cycles return no more than the craft consumed`);
    }
    return cycles;
}

function summary(findings) {
    const recyclers = new Set(findings.map((finding) => finding.recycler)).size;
    return `${findings.length} output${findings.length === 1 ? "" : "s"} in ${recyclers} recycling recipe${recyclers === 1 ? "" : "s"}`;
}

function describe(finding) {
    const sources = [finding.producerYield && `${finding.producer} Super Output`, finding.recyclerYield && "Recycler Super Output"]
        .filter(Boolean);
    const made = finding.made === 1 ? "" : ` per ${finding.made} crafted`;
    return `${finding.recycler}: ${finding.producer} consumes ${finding.consumed} ${finding.item}${made}; recycling returns `
        + `${finding.base}${sources.length ? `, ${finding.returned} with ${sources.join(" and ")}` : ""}`
        + `${finding.filter ? " (Recovery Filter)" : ""}`;
}

function recipeResults(json) {
    const results = [];
    const add = (stack) => {
        const id = stack?.id ?? stack?.item;
        if (typeof id === "string") {
            results.push({ id, count: stack.count ?? 1 });
        }
    };
    if (typeof json.result === "string") {
        results.push({ id: json.result, count: 1 });
    } else if (json.result?.stack) {
        add(json.result.stack);
    } else {
        add(json.result);
    }
    for (const stack of [json.results ?? [], json.outputs ?? []].flat()) {
        add(stack.stack ?? stack);
    }
    return results;
}

/** Item counts a recipe consumes; a slot that accepts several items counts toward each of them. */
async function consumption(recipe, tags) {
    const { json } = recipe;
    const slots = [];
    if (SHAPED.has(json.type)) {
        const symbols = new Map();
        for (const symbol of (json.pattern ?? []).join("")) {
            if (symbol !== " ") {
                symbols.set(symbol, (symbols.get(symbol) ?? 0) + 1);
            }
        }
        for (const [symbol, count] of symbols) {
            const key = json.key?.[symbol];
            slots.push({ ingredient: json.type === "rngtech:calibrated_shaped" ? key?.ingredient : key, count });
        }
    } else if (SHAPELESS.has(json.type)) {
        for (const ingredient of json.ingredients ?? []) {
            slots.push({ ingredient, count: 1 });
        }
    } else if (json.type === CALIBRATION) {
        slots.push({ ingredient: json.ingredient, count: 1 }, { ingredient: json.catalyst, count: 1 });
    } else {
        throw new Error(`Recycling return check cannot read inputs of ${recipe.id} (${json.type}), which makes a recyclable item`);
    }
    const consumed = new Map();
    for (const slot of slots) {
        for (const item of await ingredientItems(slot.ingredient, tags, recipe.id)) {
            consumed.set(item, (consumed.get(item) ?? 0) + slot.count);
        }
    }
    return consumed;
}

async function ingredientItems(ingredient, tags, recipeId) {
    const items = new Set();
    for (const entry of [ingredient ?? []].flat()) {
        if (entry.item) {
            items.add(entry.item);
        } else if (entry.tag) {
            const members = await tagItems(entry.tag, tags);
            if (!members.size) {
                throw new Error(`Recycling return check cannot resolve #${entry.tag} in ${recipeId}`);
            }
            for (const item of members) {
                items.add(item);
            }
        } else {
            throw new Error(`Recycling return check cannot read an ingredient in ${recipeId}: ${JSON.stringify(entry)}`);
        }
    }
    return items;
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

async function tagItems(tagId, tags) {
    if (!tags.has(tagId)) {
        tags.set(tagId, new Set());
        const [namespace, tagPath] = tagId.split(":");
        let values = VANILLA_TAGS[tagId] ?? [];
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
    if (process.argv.includes("--report")) {
        const { findings, cycles } = await findRecyclingLoops({ recyclerSuperOutput: process.argv.includes("--recycler-super-output") });
        for (const finding of findings) {
            console.log(describe(finding));
        }
        console.log(`${summary(findings)}; ${cycles} craft/recycle cycles checked`);
    } else {
        await checkRecyclingReturns();
    }
}
