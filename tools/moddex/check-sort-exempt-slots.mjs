import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const SOURCE_ROOT = path.join(PROJECT_ROOT, "src/main/java");
const EXEMPT_LIST = path.join(SOURCE_ROOT, "com/rngtech/content/menu/SortExemptSlots.java");

/** Menus whose item-handler slots may stay sortable. The Debug Chest is a plain chest. */
const SORTABLE_MENUS = ["DebugChestMenu.java"];

const RAW_SLOT = /new SlotItemHandler\(/g;
const SLOT_CLASS = /class\s+(\w+)\s+extends\s+SlotItemHandler\b/g;

async function javaFiles(directory) {
    const files = [];
    for (const entry of await readdir(directory, { withFileTypes: true })) {
        const full = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            files.push(...(await javaFiles(full)));
        } else if (entry.name.endsWith(".java")) {
            files.push(full);
        }
    }
    return files.sort();
}

function lineOf(source, index) {
    return source.slice(0, index).split("\n").length;
}

/**
 * Inventory sort mods match exact slot class names, so a machine slot must be a MachineSlot or a class named in
 * SortExemptSlots. A bare or anonymous SlotItemHandler escapes the blacklist and lets a sort empty output slots.
 */
export async function findSortableMachineSlots() {
    const exemptSource = await readFile(EXEMPT_LIST, "utf8");
    const findings = [];
    for (const file of await javaFiles(SOURCE_ROOT)) {
        const source = await readFile(file, "utf8");
        const relative = path.relative(PROJECT_ROOT, file).replaceAll("\\", "/");
        if (!SORTABLE_MENUS.includes(path.basename(file))) {
            for (const match of source.matchAll(RAW_SLOT)) {
                findings.push(`${relative}:${lineOf(source, match.index)}: new SlotItemHandler(...); use MachineSlot`);
            }
        }
        for (const match of source.matchAll(SLOT_CLASS)) {
            if (!new RegExp(`\\b${match[1]}\\.class\\b`).test(exemptSource)) {
                findings.push(`${relative}:${lineOf(source, match.index)}: ${match[1]} is not listed in SortExemptSlots`);
            }
        }
    }
    return findings;
}

export async function checkSortExemptSlots() {
    const findings = await findSortableMachineSlots();
    if (findings.length > 0) {
        throw new Error(`Machine slots must stay out of inventory sorting:\n${findings.join("\n")}`);
    }
    console.log("Sort-exempt slot check passed.");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkSortExemptSlots();
}
