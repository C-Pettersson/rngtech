import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "../..");
const MASTERY_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data/rngtech/mastery");
const ASCENDANCY_ROOT = path.join(MASTERY_ROOT, "ascendancies");
const DECLARATIONS_PATH = path.join(MASTERY_ROOT, "declarations.json");
const LANG_PATH = path.join(PROJECT_ROOT, "src/main/resources/assets/rngtech/lang/en_us.json");
const BOUNDS_PATH = path.join(__dirname, "recipe-loop-bounds.json");
const MACHINE_STATS_PAGE = "docs/reference/machine-stats.md";
const OUTPUT_PATH = path.join(__dirname, "generated", "ascendancies.json");
const BLOCK_START = "<!-- ascendancy-trees:start -->";
const BLOCK_END = "<!-- ascendancy-trees:end -->";

/** Mastery families in display order, with the page that documents each family's ascendancies. */
export const FAMILIES = [
    { id: "crusher", label: "Crusher", page: "docs/content/crusher.md" },
    { id: "furnace", label: "Furnace", page: "docs/content/furnace.md" },
    { id: "alloy_furnace", label: "Alloy Furnace", page: "docs/content/alloy-furnace.md" },
    { id: "metal_press", label: "Metal Press", page: "docs/content/metal-press.md" },
    { id: "resonance_calibrator", label: "Resonance Calibrator", page: "docs/content/resonance-calibrator.md" },
    { id: "melter", label: "Melter", page: "docs/content/melter.md" },
    { id: "forestry", label: "Forestry Companion", page: "docs/content/tree-farm-automation.md" }
];

/** Stats whose added values are percentage points; others carry the unit shown here. */
const PERCENT_STATS = new Set([
    "LEDGER_RATE", "FLUX_RATE", "BLEND_SPEED", "SUPER_OUTPUT_CHANCE", "CRUSHER_SALVAGE_CHANCE", "INSTANT_PROCESS_CHANCE",
    "FLUID_YIELD", "OVERLEVEL_SPEED", "HEAT_WINDOW", "JAM_RECOVERY", "UNDER_LEVEL_EFFICIENCY", "AT_LEVEL_OUTPUT",
    "OVERDRIVE_CAP", "OVERDRIVE_SPEED", "HIGH_HARDNESS_ENERGY_MITIGATION"
]);
const UNITS = new Map([
    ["MOLD_SWAP_TIME", ["tick", "ticks"]],
    ["BLEND_HEAT_REDUCTION", ["°C", "°C"]],
    ["OVERDRIVE_MARGIN", ["°C", "°C"]],
    ["SUPER_OUTPUT_CADENCE", ["cycle", "cycles"]]
]);
const PASSIVE_LABELS = new Map([
    ["MANAGED_CELLS", ["managed cell", "managed cells"]],
    ["COMPONENT_STAGE_SUPPORT", ["Component Stage Support", "Component Stage Support"]]
]);
const KIND_LABELS = { ROOT: "Root", SMALL: "Small", NOTABLE: "Notable", DEEP: "Deep notable" };

/** Reads the shipped ascendancy catalog with display names, effect text, declarations, and loop-audit coverage. */
export async function loadAscendancyModel() {
    const index = JSON.parse(await readFile(path.join(ASCENDANCY_ROOT, "index.json"), "utf8"));
    const declarations = JSON.parse(await readFile(DECLARATIONS_PATH, "utf8"));
    const lang = JSON.parse(await readFile(LANG_PATH, "utf8"));
    const bounds = JSON.parse(await readFile(BOUNDS_PATH, "utf8"));
    const coverage = coverageById(bounds);
    const ascendancies = [];
    for (const file of index.ascendancies) {
        const raw = JSON.parse(await readFile(path.join(ASCENDANCY_ROOT, file), "utf8"));
        ascendancies.push(ascendancyModel(raw, lang));
    }
    const declared = (entries, key) => entries.map((entry) => ({
        id: entry[key],
        families: entry.families,
        yield: entry.yield ?? "none",
        name: key === "stat" ? statName(entry[key], lang) : null,
        description: key === "stat"
            ? (lang[`rngtech.stat.${entry[key].toLowerCase()}.description`] ?? "").replaceAll("%s", "N").replaceAll("%%", "%")
            : behaviorText(entry[key], lang),
        coveredBy: coverage.get(entry[key]) ?? []
    }));
    return {
        families: FAMILIES.map((family) => ({
            ...family,
            ascendancies: ascendancies.filter((ascendancy) => ascendancy.family === family.id)
        })),
        stats: declared(declarations.stats ?? [], "stat"),
        behaviors: declared(declarations.behaviors ?? [], "id")
    };
}

export async function buildAscendancyData() {
    const model = await loadAscendancyModel();
    await mkdir(path.dirname(OUTPUT_PATH), { recursive: true });
    await writeFile(OUTPUT_PATH, `${JSON.stringify({ generatedAt: new Date().toISOString(), ...model }, null, 2)}\n`);
    return model;
}

function ascendancyModel(raw, lang) {
    const byId = new Map(raw.nodes.map((node) => [node.id, node]));
    const deep = (node) => node.kind === "NOTABLE" && byId.get(byId.get(node.parent)?.parent)?.kind === "NOTABLE";
    const nodes = raw.nodes.map((node) => ({
        id: node.id,
        name: lang[`rngtech.mastery.ascendancy.${raw.id}.${node.id}`] ?? node.id,
        kind: deep(node) ? "DEEP" : node.kind,
        parent: node.parent ?? null,
        x: node.x,
        y: node.y,
        effects: effectLines(node, lang),
        behaviors: (node.behaviors ?? []).map((behavior) => ({ id: behavior, text: behaviorText(behavior, lang) })),
        stats: [...new Set([...(node.effects ?? []).map((effect) => effect.stat), ...Object.keys(node.fixed ?? {})])]
    }));
    return {
        id: raw.id,
        family: raw.family,
        name: lang[`rngtech.mastery.ascendancy.${raw.id}`] ?? raw.id,
        nodes: treeOrder(nodes)
    };
}

/** Root first, then each branch depth first, left to right. */
function treeOrder(nodes) {
    const children = (id) => nodes.filter((node) => node.parent === id).toSorted((left, right) => left.x - right.x || left.y - right.y);
    const ordered = [];
    const visit = (node) => {
        ordered.push(node);
        children(node.id).forEach(visit);
    };
    nodes.filter((node) => node.kind === "ROOT").forEach(visit);
    return ordered;
}

function effectLines(node, lang) {
    const lines = (node.effects ?? []).map((effect) => effectText(effect, lang));
    for (const [stat, value] of Object.entries(node.fixed ?? {})) {
        lines.push(`${statName(stat, lang)} fixed at ${withUnit(stat, value, false)}`);
    }
    for (const [stat, value] of Object.entries(node.passive ?? {})) {
        const [singular, plural] = PASSIVE_LABELS.get(stat) ?? [stat, stat];
        lines.push(`${signed(value)} ${Math.abs(value) === 1 ? singular : plural}`);
    }
    return lines;
}

export function effectText(effect, lang) {
    const name = statName(effect.stat, lang);
    const value = effect.value;
    switch (effect.operation) {
        case "ADD":
            return `${withUnit(effect.stat, value, true)} ${name}`;
        case "INCREASED_PERCENT":
            return value >= 0 ? `${number(value)}% increased ${name}` : `${number(-value)}% reduced ${name}`;
        case "DECREASED_PERCENT":
            return value >= 0 ? `${number(value)}% reduced ${name}` : `${number(-value)}% increased ${name}`;
        case "MORE":
            return value >= 1 ? `${number((value - 1) * 100)}% more ${name}` : `${number((1 - value) * 100)}% less ${name}`;
        case "LESS":
            return value <= 1 ? `${number((1 - value) * 100)}% less ${name}` : `${number((value - 1) * 100)}% more ${name}`;
        default:
            throw new Error(`Unknown modifier operation ${effect.operation} on ${effect.stat}`);
    }
}

function withUnit(stat, value, sign) {
    const shown = sign ? signed(value) : number(value);
    if (PERCENT_STATS.has(stat)) {
        return `${shown}%`;
    }
    const unit = UNITS.get(stat);
    return unit ? `${shown} ${Math.abs(value) === 1 ? unit[0] : unit[1]}` : shown;
}

function signed(value) {
    return value < 0 ? `−${number(-value)}` : `+${number(value)}`;
}

function number(value) {
    return String(Math.round(value * 100) / 100);
}

function statName(stat, lang) {
    return lang[`rngtech.stat.${stat.toLowerCase()}`] ?? stat;
}

/** A behavior's tooltip without its family prefix, which the family page already names. */
function behaviorText(behavior, lang) {
    const text = lang[`rngtech.mastery.behavior.${behavior.toLowerCase()}`] ?? behavior;
    const stripped = text.replace(/^[A-Z][A-Za-z ]+: /, "");
    return stripped.charAt(0).toUpperCase() + stripped.slice(1);
}

function coverageById(bounds) {
    const coverage = new Map();
    const add = (source, covers) => (covers ?? []).forEach((id) => coverage.set(id, [...(coverage.get(id) ?? []), source]));
    Object.entries(bounds.types ?? {}).forEach(([type, entry]) => add(type, entry.covers));
    Object.entries(bounds.worldSources ?? {}).forEach(([name, entry]) => add(`world:${name}`, entry.covers));
    return coverage;
}

/** The generated Markdown for one family's ascendancy trees. */
export function familyTreesMarkdown(family) {
    const sections = family.ascendancies.map((ascendancy) => {
        const names = new Map(ascendancy.nodes.map((node) => [node.id, node.name]));
        const rows = ascendancy.nodes.map((node) => {
            const name = node.kind === "SMALL" ? node.name : `**${node.name}**`;
            return `| ${name} | ${KIND_LABELS[node.kind]} | ${node.parent ? names.get(node.parent) : "—"} | ${nodeSummary(node)} |`;
        });
        return [`### ${ascendancy.name}`, "", "| Node | Type | After | Effect |", "|---|---|---|---|", ...rows].join("\n");
    });
    return [BLOCK_START, "", sections.join("\n\n"), "", BLOCK_END].join("\n");
}

/** Stat effects as one sentence, followed by each behavior's own sentence. */
export function nodeSummary(node) {
    const sentences = node.effects.length ? [`${node.effects.join("; ")}.`] : [];
    node.behaviors.forEach((behavior) => sentences.push(behavior.text.endsWith(".") ? behavior.text : `${behavior.text}.`));
    return sentences.join(" ");
}

/**
 * Family pages carry generated ascendancy tables between markers, and Machine Stats lists every declared stat. With
 * {@code write}, stale tables are rewritten; otherwise any drift fails.
 */
export async function checkAscendancyDocs({ write = false, log = true } = {}) {
    const model = await loadAscendancyModel();
    const failures = [];
    for (const family of model.families) {
        if (!family.ascendancies.length) {
            continue;
        }
        const pagePath = path.join(PROJECT_ROOT, family.page);
        const page = await readFile(pagePath, "utf8");
        const start = page.indexOf(BLOCK_START);
        const end = page.indexOf(BLOCK_END);
        if (start < 0 || end < start) {
            failures.push(`${family.page} has no ${BLOCK_START} ... ${BLOCK_END} block for its ascendancies`);
            continue;
        }
        const expected = familyTreesMarkdown(family);
        const current = page.slice(start, end + BLOCK_END.length);
        if (current !== expected) {
            if (write) {
                await writeFile(pagePath, page.slice(0, start) + expected + page.slice(end + BLOCK_END.length));
            } else {
                failures.push(`${family.page} ascendancy tables are stale; run node tools/moddex/export-ascendancy-data.mjs --write-docs`);
            }
        }
    }
    const statsPage = await readFile(path.join(PROJECT_ROOT, MACHINE_STATS_PAGE), "utf8");
    for (const stat of model.stats) {
        if (!statsPage.includes(`| \`${stat.id}\` |`)) {
            failures.push(`${MACHINE_STATS_PAGE} has no row for declared stat ${stat.id}`);
        }
    }
    if (failures.length) {
        throw new Error(`Ascendancy docs check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        const count = model.families.reduce((total, family) => total + family.ascendancies.length, 0);
        console.log(`Ascendancy docs PASS: ${count} ascendancies on ${model.families.length} family pages, ${model.stats.length} declared stats in Machine Stats`);
    }
    return model;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    if (process.argv.includes("--write-docs")) {
        await checkAscendancyDocs({ write: true });
    } else {
        await buildAscendancyData();
        console.log(`Wrote ${path.relative(PROJECT_ROOT, OUTPUT_PATH)}`);
    }
}
