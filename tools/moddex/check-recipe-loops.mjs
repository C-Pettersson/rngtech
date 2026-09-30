import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const DATA_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const BOUNDS_PATH = path.join(PROJECT_ROOT, "tools/moddex/recipe-loop-bounds.json");
const DECLARATIONS_PATH = path.join(DATA_ROOT, "rngtech/mastery/declarations.json");
const EPSILON = 1e-9;
const MAX_LOOP_ROUNDS = 200;
const MAX_SIMPLEX_PIVOTS = 100000;
const PERTURBATION = 1e-10;
/** Net items per unit of total recipe runs; real loops gain far more, and the perturbation far less. */
const ITEM_THRESHOLD = 1e-4;
const FE_THRESHOLD = 1;
/** Fluids are measured in buckets so their coefficients stay comparable with item counts. */
const MB_PER_BUCKET = 1000;

const SHAPED = new Set(["minecraft:crafting_shaped", "rngtech:trait_shaped", "rngtech:calibrated_shaped", "rngtech:tool_damage_shaped"]);
const SHAPELESS = new Set(["minecraft:crafting_shapeless", "rngtech:tool_damage_shapeless"]);
/** Recipe types whose `energy` is generated rather than consumed. */
const FE_PRODUCERS = new Set([
    "rngtech:potential_reactor", "rngtech:corrosion_cell", "rngtech:cavitation",
    "rngtech:gas_combustion", "rngtech:ammonia_power_cycle", "rngtech:vacuum_collapse"
]);
const FE_CONSUMERS = new Set([
    "rngtech:crusher", "rngtech:furnace", "rngtech:alloy_furnace", "rngtech:metal_press", "rngtech:melter",
    "rngtech:calibration", "rngtech:component_recycling", "rngtech:battery_assembly",
    "rngtech:coal_gasification", "rngtech:gas_reforming", "rngtech:ammonia_synthesis"
]);
const PASSIVE = new Set(["rngtech:algae_growth", "rngtech:desiccant_absorption", "rngtech:wooden_dehumidifier_conversion"]);
/** Code-defined recipes: their item conversions are listed in specialReactions(), or they transform no items. */
const SPECIAL = new Set(["rngtech:carbon_exhaust_bucket", "rngtech:identify_traits", "rngtech:exotic_affix_forge"]);
const INPUT_KEYS = [
    "ingredient", "ingredients", "primary_ingredient", "secondary_ingredient", "catalyst", "stabilizer", "plate", "electrolyte",
    "carbon_input", "dry_input", "fluid_input", "water_input", "gas_input", "nitrogen_input", "hydrogen_input", "ammonia_input"
];
/** Catalyst beds are installed Gear for these types, not consumed inputs. */
const REUSABLE_KEYS = new Map([["rngtech:ammonia_synthesis", new Set(["catalyst"])], ["rngtech:gas_reforming", new Set(["catalyst"])]]);
const OUTPUT_KEYS = [
    "result", "outputs", "output", "fluid_output", "residue", "saturated_output",
    "water_output", "exhaust_output", "hydrogen_output", "carbon_monoxide_output"
];
const FLUID_BUCKETS = ["ammonia", "carbon_exhaust", "carbon_monoxide", "electrolyte_solution", "hydrogen", "lubricant", "methane", "nitrogen", "syngas"];
const WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"];
/** Vanilla item tags that RNGTech recipes use but that are defined by Minecraft rather than in this repository. */
const VANILLA_TAGS = new Map([
    ["minecraft:planks", WOODS.map((wood) => `minecraft:${wood}_planks`)],
    ["minecraft:wooden_buttons", WOODS.map((wood) => `minecraft:${wood}_button`)],
    ["minecraft:wooden_slabs", WOODS.map((wood) => `minecraft:${wood}_slab`)],
    ["minecraft:logs", ["minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:crimson_stem", "minecraft:warped_stem"]],
    ["minecraft:logs_that_burn", ["minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:jungle_log"]],
    ["minecraft:leaves", ["minecraft:oak_leaves", "minecraft:spruce_leaves", "minecraft:birch_leaves"]],
    ["minecraft:saplings", ["minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling"]],
    ["minecraft:flowers", ["minecraft:dandelion", "minecraft:poppy"]],
    ["minecraft:smelts_to_glass", ["minecraft:sand", "minecraft:red_sand"]]
]);
/** Vanilla crafting that turns items RNGTech recipes return into items they consume. */
const VANILLA_RECIPES = [
    ["minecraft:stick", { type: "minecraft:crafting_shapeless", ingredients: [{ tag: "minecraft:planks" }, { tag: "minecraft:planks" }], result: { id: "minecraft:stick", count: 4 } }],
    ["minecraft:chest", { type: "minecraft:crafting_shaped", pattern: ["PPP", "P P", "PPP"], key: { P: { tag: "minecraft:planks" } }, result: { id: "minecraft:chest" } }],
    ["minecraft:furnace", { type: "minecraft:crafting_shaped", pattern: ["CCC", "C C", "CCC"], key: { C: { item: "minecraft:cobblestone" } }, result: { id: "minecraft:furnace" } }],
    ...WOODS.flatMap((wood) => [
        [`minecraft:${wood}_button`, { type: "minecraft:crafting_shapeless", ingredients: [{ item: `minecraft:${wood}_planks` }], result: { id: `minecraft:${wood}_button` } }],
        [`minecraft:${wood}_slab`, { type: "minecraft:crafting_shaped", pattern: ["PPP"], key: { P: { item: `minecraft:${wood}_planks` } }, result: { id: `minecraft:${wood}_slab`, count: 6 } }]
    ])
];
const VANILLA_COMPACTION = [
    ["minecraft:iron_nugget", "minecraft:iron_ingot"], ["minecraft:iron_ingot", "minecraft:iron_block"],
    ["minecraft:gold_nugget", "minecraft:gold_ingot"], ["minecraft:gold_ingot", "minecraft:gold_block"],
    ["minecraft:copper_ingot", "minecraft:copper_block"], ["minecraft:netherite_ingot", "minecraft:netherite_block"],
    ["minecraft:raw_iron", "minecraft:raw_iron_block"], ["minecraft:raw_gold", "minecraft:raw_gold_block"],
    ["minecraft:raw_copper", "minecraft:raw_copper_block"], ["minecraft:coal", "minecraft:coal_block"],
    ["minecraft:redstone", "minecraft:redstone_block"], ["minecraft:lapis_lazuli", "minecraft:lapis_block"],
    ["minecraft:diamond", "minecraft:diamond_block"], ["minecraft:emerald", "minecraft:emerald_block"]
];

/**
 * Recipe loop audit. A loop is a mix of recipe runs that returns every consumed input that is not free (water, and the
 * outputs of recipes that only use free inputs) while producing either more of some item or net FE. FE is free when
 * looking for item loops. Bonus-eligible outputs take their recipe type's worst-case bound, and the Potential Reactor's
 * stripping of recyclable machines and parts pays the conservative `strippingFe` bound.
 *
 * A linear program finds the loop with the largest gain. Each loop found is shrunk to a minimal recipe set for the
 * report, its bonus or stripping recipe is set aside, and the search repeats until no loop remains.
 */
export async function checkRecipeLoops({ log = true, report = false, mutate = null } = {}) {
    const bounds = JSON.parse(await readFile(BOUNDS_PATH, "utf8"));
    const declarations = JSON.parse(await readFile(DECLARATIONS_PATH, "utf8"));
    const tags = new Map();
    const recipes = await readRecipes(RECIPE_ROOT);
    mutate?.(recipes);
    const reactions = specialReactions(await tagItems("c:dusts/coal", tags));
    for (const recipe of [...recipes, ...VANILLA_RECIPES.map(([id, json]) => ({ id, json }))]) {
        reactions.push(...await recipeReactions(recipe, tags, bounds));
    }
    reactions.push(...strippingReactions(reactions, bounds));
    const network = loopNetwork(reactions, new Set(bounds.free ?? []));
    const allowed = new Map((bounds.allow ?? []).map((entry) => [signatureOf(entry.recipes), entry.reason]));
    const usedAllowances = new Set();
    const failures = [...coverageFailures(bounds, declarations)];
    const findings = [];

    // Item loops are set aside before the FE search, so each FE loop reported is one that FE alone makes possible.
    let remaining = network.candidates;
    for (const kind of ["items", "energy"]) {
        let round = 0;
        for (let loop = solveLoop(remaining, network.free, kind); loop; loop = solveLoop(remaining, network.free, kind)) {
            if (++round > MAX_LOOP_ROUNDS) {
                throw new Error(`Recipe loop audit: more than ${MAX_LOOP_ROUNDS} ${kind} loops; fix the reported ones first:\n${findings.join("\n")}`);
            }
            const minimal = minimize(loop, network.free, kind);
            const signature = signatureOf(minimal.recipes.map((entry) => entry.id));
            if (allowed.has(signature)) {
                usedAllowances.add(signature);
            } else {
                findings.push(describeLoop(kind, minimal));
            }
            remaining = setAside(remaining, minimal.blamed);
        }
    }

    for (const signature of allowed.keys()) {
        if (!usedAllowances.has(signature)) {
            failures.push(`Stale allowlist entry no longer matches a failing loop: ${signature}`);
        }
    }
    failures.push(...findings);
    if (report) {
        console.log(`Recipe loop report: ${recipes.length} recipes, ${reactions.length} reactions, ${network.candidates.length} can run in a loop`);
        findings.forEach((finding) => console.log(`- ${finding}`));
    }
    if (failures.length) {
        throw new Error(`Recipe loop audit failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Recipe loop audit PASS: ${recipes.length} recipes, ${reactions.length} reactions, `
            + `${network.candidates.length} can run in a loop, no item or FE loop, ${allowed.size} allowlisted`);
    }
    return { recipes: recipes.length, reactions: reactions.length };
}

/** Every declared yield stat or behavior must be covered by a bound, so a new yield source cannot skip the audit. */
function coverageFailures(bounds, declarations) {
    const covered = new Set(Object.values(bounds.types ?? {}).flatMap((type) => type.covers ?? []));
    const declared = [
        ...(declarations.stats ?? []).map((entry) => [entry.stat, entry.yield]),
        ...(declarations.behaviors ?? []).map((entry) => [entry.id, entry.yield])
    ];
    return declared
        .filter(([id, yieldKind]) => (yieldKind ?? "none") !== "none" && !covered.has(id))
        .map(([id]) => `Declared yield ${id} has no loop-audit bound in tools/moddex/recipe-loop-bounds.json`);
}

/**
 * A reaction consumes and produces nodes: items, `fluid:` ids, and `any:` nodes that stand for one of several
 * accepted inputs. Outputs are worst-case counts: bonus-eligible outputs are multiplied by the type's bound.
 */
async function recipeReactions(recipe, tags, bounds) {
    const { id, json } = recipe;
    const type = json.type;
    if (SPECIAL.has(type)) {
        return [];
    }
    if (!SHAPED.has(type) && !SHAPELESS.has(type) && !FE_PRODUCERS.has(type) && !FE_CONSUMERS.has(type) && !PASSIVE.has(type)) {
        throw new Error(`${id}: unknown recipe type ${type}; teach tools/moddex/check-recipe-loops.mjs how it converts items`);
    }
    const choices = [];
    const inputs = await consumedInputs(json, tags, id, choices, new Set(bounds.freeInputKeys ?? []));
    const bound = json.bonus_output === false ? 1 : bounds.types?.[type]?.maxOutputMultiplier ?? 1;
    const energy = Number(json.energy ?? 0) * (FE_PRODUCERS.has(type) ? 1 : FE_CONSUMERS.has(type) ? -1 : 0);
    const outputs = new Map();
    for (const output of recipeOutputs(json)) {
        outputs.set(output.node, (outputs.get(output.node) ?? 0) + output.count * bound);
    }
    const reactions = [...choices, { id, type, inputs, outputs, fe: energy, bound }];
    const failure = json.failure_output && recipeOutputs({ result: json.failure_output })[0];
    if (failure) {
        reactions.push({ id: `${id}#failure`, type, inputs, outputs: new Map([[failure.node, failure.count]]), fe: energy, bound: 1 });
    }
    return reactions;
}

/** Consumed inputs, leaving out reusable parts and inputs under `freeKeys`, such as cheap catalysts that do not stop a loop. */
async function consumedInputs(json, tags, id, choices, freeKeys) {
    const entries = [];
    if (SHAPED.has(json.type)) {
        const counts = new Map();
        for (const row of json.pattern ?? []) {
            for (const symbol of row) {
                if (symbol !== " ") {
                    counts.set(symbol, (counts.get(symbol) ?? 0) + 1);
                }
            }
        }
        for (const [symbol, count] of counts) {
            const entry = json.key?.[symbol];
            if (entry && !sameIngredient(entry, json.tool)) {
                entries.push([entry, count]);
            }
        }
    } else if (SHAPELESS.has(json.type)) {
        for (const entry of json.ingredients ?? []) {
            if (!sameIngredient(entry, json.tool)) {
                entries.push([entry, 1]);
            }
        }
    } else {
        const reusable = REUSABLE_KEYS.get(json.type) ?? new Set();
        for (const key of INPUT_KEYS) {
            if (json[key] !== undefined && !reusable.has(key) && !freeKeys.has(key)) {
                const count = key === "ingredient" ? json.input_count ?? 1 : 1;
                for (const entry of key === "ingredients" ? json[key] : [json[key]]) {
                    entries.push([entry, count]);
                }
            }
        }
    }
    const inputs = new Map();
    for (const [entry, count] of entries) {
        const input = await ingredientNode(entry, count, tags, `${id}/${inputs.size}`, choices);
        if (input) {
            inputs.set(input.node, (inputs.get(input.node) ?? 0) + input.count);
        }
    }
    return inputs;
}

/** One consumed node. An ingredient that accepts several items becomes an `any:` node that each accepted item converts into. */
async function ingredientNode(entry, count, tags, key, choices) {
    if (!entry) {
        return null;
    }
    if (entry.ingredient?.fluid) {
        return { node: `fluid:${entry.ingredient.fluid}`, count: (entry.amount ?? MB_PER_BUCKET) / MB_PER_BUCKET };
    }
    if (entry.ingredient) {
        return ingredientNode(entry.ingredient, count * (entry.count ?? 1), tags, key, choices);
    }
    if (entry.fluid) {
        return { node: `fluid:${entry.fluid}`, count: (entry.amount ?? MB_PER_BUCKET) / MB_PER_BUCKET };
    }
    const effective = count * (entry.count ?? 1);
    const items = [];
    for (const alternative of [entry].flat()) {
        if (alternative.item) {
            items.push(alternative.item);
        } else if (alternative.tag) {
            items.push(...await tagItems(alternative.tag, tags));
        }
    }
    const unique = [...new Set(items)];
    if (unique.length === 0) {
        return null;
    }
    if (unique.length === 1) {
        return { node: unique[0], count: effective };
    }
    const node = `any:${key}`;
    for (const item of unique) {
        choices.push({ id: `${node}<-${item}`, type: "choice", inputs: new Map([[item, 1]]), outputs: new Map([[node, 1]]), fe: 0, bound: 1 });
    }
    return { node, count: effective };
}

function recipeOutputs(json) {
    const outputs = [];
    for (const key of OUTPUT_KEYS) {
        const value = json[key];
        if (value === undefined) {
            continue;
        }
        const entries = key === "outputs" ? value.map((entry) => entry.stack ?? entry) : [value.stack ?? value];
        for (const entry of entries) {
            const id = entry.id ?? entry.item ?? entry.fluid;
            if (!id) {
                continue;
            }
            const fluid = entry.amount !== undefined || entry.fluid !== undefined;
            outputs.push({ node: fluid ? `fluid:${id}` : id, count: fluid ? (entry.amount ?? MB_PER_BUCKET) / MB_PER_BUCKET : entry.count ?? 1 });
        }
    }
    return outputs;
}

/** Conversions defined in code or by vanilla data rather than by RNGTech recipe files. None pay bonus output. */
function specialReactions(coalDusts) {
    const reactions = [];
    const add = (id, input, inputCount, output, outputCount) => reactions.push({
        id, type: "special", inputs: new Map([[input, inputCount]]), outputs: new Map([[output, outputCount]]), fe: 0, bound: 1
    });
    for (const [small, large] of VANILLA_COMPACTION) {
        add(`minecraft:compaction/${large}`, small, 9, large, 1);
        add(`minecraft:decompaction/${large}`, large, 1, small, 9);
    }
    for (const fluid of FLUID_BUCKETS) {
        add(`rngtech:drain/${fluid}_bucket`, `rngtech:${fluid}_bucket`, 1, `fluid:rngtech:${fluid}`, 1);
        add(`rngtech:fill/${fluid}_bucket`, `fluid:rngtech:${fluid}`, 1, `rngtech:${fluid}_bucket`, 1);
    }
    add("minecraft:drain/lava_bucket", "minecraft:lava_bucket", 1, "fluid:minecraft:lava", 1);
    add("minecraft:fill/lava_bucket", "fluid:minecraft:lava", 1, "minecraft:lava_bucket", 1);
    for (const dust of coalDusts) {
        add(`rngtech:carbon_exhaust_bucket<-${dust}`, dust, 1, "rngtech:carbon_exhaust_bucket", 1);
    }
    // Dry electrolyte dissolves into 125 mB of Electrolyte Solution in the Component Assembler and Corrosion Cell.
    add("rngtech:dissolve/electrolyte", "rngtech:electrolyte", 1, "fluid:rngtech:electrolyte_solution", 125 / MB_PER_BUCKET);
    return reactions;
}

/**
 * The Potential Reactor pays FE for the RPG traits of machines and parts and returns a stripped copy that the Component
 * Recycler still accepts. Each recyclable input that can carry traits gets a stripping reaction worth the conservative
 * `strippingFe` bound, and its recycling reactions also accept the stripped copy.
 */
function strippingReactions(reactions, bounds) {
    const payout = bounds.strippingFe ?? 0;
    const notStrippable = new Set(bounds.notStrippable ?? []);
    const added = [];
    const stripped = new Set();
    for (const reaction of reactions.filter((entry) => entry.type === "rngtech:component_recycling")) {
        for (const [input, count] of [...reaction.inputs]) {
            if (notStrippable.has(input) || input.startsWith("any:")) {
                continue;
            }
            if (!stripped.has(input)) {
                stripped.add(input);
                added.push({ id: `rngtech:strip/${input}`, type: "stripping", inputs: new Map([[input, 1]]), outputs: new Map([[`stripped:${input}`, 1]]), fe: payout, bound: 1 });
            }
            const inputs = new Map(reaction.inputs);
            inputs.delete(input);
            inputs.set(`stripped:${input}`, count);
            added.push({ ...reaction, id: `${reaction.id}#stripped`, inputs });
        }
    }
    return added;
}

/**
 * Reactions whose inputs are all free are sources, not loops, and their outputs count as free. A reaction can only take
 * part in a loop when every non-free input is produced by some other remaining reaction.
 */
function loopNetwork(reactions, freeNodes) {
    const free = new Set(freeNodes);
    const isSource = (reaction) => [...reaction.inputs.keys()].every((node) => free.has(node));
    for (let grew = true; grew;) {
        grew = false;
        for (const reaction of reactions.filter(isSource)) {
            for (const output of reaction.outputs.keys()) {
                grew = !free.has(output) || grew;
                free.add(output);
            }
        }
    }
    return { free, candidates: prune(reactions.filter((reaction) => !isSource(reaction)), free) };
}

function prune(reactions, free) {
    let candidates = reactions;
    for (let before = -1; before !== candidates.length;) {
        before = candidates.length;
        const produced = new Set(candidates.flatMap((reaction) => [...reaction.outputs.keys()]));
        candidates = candidates.filter((reaction) => [...reaction.inputs.keys()].every((node) => free.has(node) || produced.has(node)));
    }
    return candidates;
}

/** Mirrors the likely fix: a bonus recipe keeps running without its bonus, and any other blamed recipe is removed. */
function setAside(reactions, blamed) {
    if (blamed.bound <= 1) {
        return reactions.filter((reaction) => reaction !== blamed);
    }
    const outputs = new Map([...blamed.outputs].map(([node, count]) => [node, count / blamed.bound]));
    return reactions.map((reaction) => (reaction === blamed ? { ...blamed, outputs, bound: 1 } : reaction));
}

/** Drops support recipes, least-run first, while the rest still loops, so the report names a minimal loop. */
function minimize(loop, free, kind) {
    let current = loop;
    for (const reaction of [...loop.support].sort((a, b) => loop.runs.get(a) - loop.runs.get(b))) {
        if (current.support.length > 1 && current.support.includes(reaction)) {
            current = solveLoop(current.support.filter((entry) => entry !== reaction), free, kind) ?? current;
        }
    }
    return current;
}

/**
 * Maximizes the net item gain, or net FE, over recipe run counts x >= 0 with sum(x) <= 1, subject to every non-free node
 * being produced at least as much as it is consumed. Returns the loop's recipes, or null when no loop is found.
 */
function solveLoop(reactions, free, kind) {
    const candidates = prune(reactions, free);
    const found = optimize(candidates, free, kind, true);
    if (!found) {
        return null;
    }
    // Re-solve on the support alone without the slack, so a result that only exists thanks to the perturbation is dropped.
    const support = candidates.filter((_, position) => found.solution[position] > EPSILON);
    const exact = optimize(support, free, kind, false);
    if (!exact) {
        return null;
    }
    const net = (reaction, node) => (reaction.outputs.get(node) ?? 0) - (reaction.inputs.get(node) ?? 0);
    const running = support.map((reaction, position) => ({ reaction, runs: exact.solution[position] })).filter((entry) => entry.runs > EPSILON);
    const gains = exact.nodes.map((node) => [node, running.reduce((sum, entry) => sum + entry.runs * net(entry.reaction, node), 0)])
        .filter(([, amount]) => amount > ITEM_THRESHOLD);
    const value = exact.value;
    // Set aside a bonus recipe first, then a stripping recipe, so the next search can surface a different loop.
    const priority = (reaction) => (reaction.bound > 1 ? 2 : reaction.type === "stripping" ? 1 : 0);
    const blamed = [...running].sort((a, b) => priority(b.reaction) - priority(a.reaction) || b.runs - a.runs)[0].reaction;
    return {
        value,
        gains,
        blamed,
        support: running.map((entry) => entry.reaction),
        runs: new Map(running.map((entry) => [entry.reaction, entry.runs])),
        recipes: running.map((entry) => ({ id: entry.reaction.id, runs: entry.runs, bound: entry.reaction.bound }))
    };
}

/**
 * Builds and solves the loop program. With `perturb`, distinct tiny slacks remove the degeneracy of the all-zero
 * right-hand side so largest-coefficient pivoting is fast; without it, Bland's rule guarantees termination.
 */
function optimize(candidates, free, kind, perturb) {
    const nodes = [...new Set(candidates.flatMap((reaction) => [...reaction.inputs.keys(), ...reaction.outputs.keys()]))]
        .filter((node) => !free.has(node));
    const net = (reaction, node) => (reaction.outputs.get(node) ?? 0) - (reaction.inputs.get(node) ?? 0);
    const objective = candidates.map((reaction) => (kind === "items" ? nodes.reduce((sum, node) => sum + net(reaction, node), 0) : reaction.fe));
    const scale = Math.max(0, ...objective.map(Math.abs));
    if (scale === 0) {
        return null;
    }
    const rows = nodes.map((node) => candidates.map((reaction) => -net(reaction, node)));
    rows.push(candidates.map(() => 1));
    const limits = [...nodes.map((_, i) => (perturb ? PERTURBATION * (1 + ((i * 7919) % 997) / 997) : 0)), 1];
    rows.forEach((row, i) => {
        const rowScale = Math.max(...row.map(Math.abs));
        if (rowScale > 0) {
            rows[i] = row.map((value) => value / rowScale);
            limits[i] /= rowScale;
        }
    });
    const { value, solution } = maximize(objective.map((entry) => entry / scale), rows, limits, !perturb);
    return value * scale > (kind === "items" ? ITEM_THRESHOLD : FE_THRESHOLD) ? { value: value * scale, solution, nodes } : null;
}

function describeLoop(kind, loop) {
    const recipes = loop.recipes.map((entry) => `${entry.id}${entry.bound > 1 ? ` (x${entry.bound} bonus)` : ""} x${entry.runs.toPrecision(3)}`).join(", ");
    const blamed = loop.blamed.id.replace(/#(failure|stripped)$/, "");
    const hint = loop.blamed.bound > 1 ? `set "bonus_output": false on ${blamed}` : `rebalance ${blamed}`;
    if (kind === "energy") {
        return `returns its inputs with ${loop.value.toFixed(1)} net FE per unit of runs (likely fix: ${hint}): ${recipes}`;
    }
    const gains = loop.gains.map(([node, amount]) => `${node} +${amount.toPrecision(3)}`).join(", ");
    return `returns its inputs and gains ${gains} (likely fix: ${hint}): ${recipes}`;
}

/**
 * Dense tableau simplex for maximize c.x subject to A.x <= b, x >= 0, and b >= 0. Pivots on the most negative reduced
 * cost, or on the first negative one with `bland`, which rules out cycling on a degenerate program.
 */
function maximize(objective, rows, limits, bland = false) {
    const m = rows.length;
    const n = objective.length;
    const width = n + m + 1;
    const tableau = rows.map((row, i) => {
        const line = new Float64Array(width);
        row.forEach((value, j) => { line[j] = value; });
        line[n + i] = 1;
        line[width - 1] = limits[i];
        return line;
    });
    const goal = new Float64Array(width);
    objective.forEach((value, j) => { goal[j] = -value; });
    const basis = Array.from({ length: m }, (_, i) => n + i);
    for (let pivots = 0; pivots < MAX_SIMPLEX_PIVOTS; pivots++) {
        let entering = -1;
        for (let j = 0; j < width - 1 && !(bland && entering >= 0); j++) {
            if (goal[j] < -EPSILON && (entering < 0 || goal[j] < goal[entering])) {
                entering = j;
            }
        }
        if (entering < 0) {
            const solution = new Float64Array(n);
            basis.forEach((variable, i) => { if (variable < n) { solution[variable] = tableau[i][width - 1]; } });
            return { value: goal[width - 1], solution };
        }
        let leaving = -1;
        let best = Infinity;
        for (let i = 0; i < m; i++) {
            const coefficient = tableau[i][entering];
            if (coefficient > EPSILON) {
                const ratio = tableau[i][width - 1] / coefficient;
                if (ratio < best - EPSILON || (Math.abs(ratio - best) <= EPSILON && basis[i] < basis[leaving])) {
                    best = ratio;
                    leaving = i;
                }
            }
        }
        if (leaving < 0) {
            // The feasible region is bounded by sum(x) <= 1, so a column without a pivot only carries rounding noise.
            goal[entering] = 0;
            continue;
        }
        const pivotRow = tableau[leaving];
        const pivot = pivotRow[entering];
        for (let j = 0; j < width; j++) {
            pivotRow[j] /= pivot;
        }
        for (const line of [...tableau, goal]) {
            if (line !== pivotRow && line[entering] !== 0) {
                const factor = line[entering];
                for (let j = 0; j < width; j++) {
                    line[j] -= factor * pivotRow[j];
                }
            }
        }
        basis[leaving] = entering;
    }
    throw new Error("Recipe loop audit: the loop program did not converge");
}

function signatureOf(recipeIds) {
    return [...new Set(recipeIds.map((id) => id.replace(/#(failure|stripped)$/, "")))].sort().join(",");
}

function sameIngredient(entry, tool) {
    return Boolean(tool) && JSON.stringify(entry) === JSON.stringify(tool);
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

/** Resolves an item tag from the repository or VANILLA_TAGS. An unknown tag fails, so no ingredient is silently free. */
async function tagItems(tagId, tags) {
    if (!tags.has(tagId)) {
        tags.set(tagId, new Set());
        const [namespace, tagPath] = tagId.split(":");
        let values = VANILLA_TAGS.get(tagId);
        if (!values) {
            try {
                values = JSON.parse(await readFile(path.join(DATA_ROOT, namespace, "tags/item", `${tagPath}.json`), "utf8")).values ?? [];
            } catch (error) {
                if (error.code === "ENOENT") {
                    throw new Error(`Recipe loop audit: unknown item tag ${tagId}; add its members to VANILLA_TAGS in tools/moddex/check-recipe-loops.mjs`);
                }
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

/**
 * Proves the audit still catches known loops: re-enabling bonus output on a reversible conversion, or on the
 * calibrate-then-recycle pair, must fail it.
 */
export async function checkRecipeLoopMutations() {
    const mutations = [
        ["rngtech:crusher/copper_dust_from_ingot", "rngtech:furnace/metals/copper_from_dust"],
        ["rngtech:calibration_structural_iron_plate", "rngtech:component_recycling/calibrated_structural_component"]
    ];
    for (const ids of mutations) {
        const mutate = (recipes) => {
            for (const id of ids) {
                const recipe = recipes.find((entry) => entry.id === id);
                if (!recipe) {
                    throw new Error(`Recipe loop mutation: missing ${id}`);
                }
                delete recipe.json.bonus_output;
            }
        };
        const caught = await checkRecipeLoops({ log: false, mutate }).then(() => false, () => true);
        if (!caught) {
            throw new Error(`Recipe loop audit missed a loop when bonus output returned to ${ids.join(" and ")}`);
        }
    }
    console.log(`Recipe loop mutations PASS: ${mutations.length} reintroduced loops were caught`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkRecipeLoops({ report: process.argv.includes("--report") });
    await checkRecipeLoopMutations();
}
