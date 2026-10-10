import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const SCREEN_ROOT = path.join(PROJECT_ROOT, "src/main/java/com/rngtech/client/screen");

/**
 * Prose that is intentionally not translated, keyed by screen file name. Anything else that starts a
 * `Component.literal("...")` with two or more letters in a row belongs in `lang/en_us.json`.
 */
const ALLOWED_LITERALS = {
    // Brand name of an optional dependency, shown in a debug summary.
    "UniversalConnectorScreen.java": ["AE2 "]
};

const LITERAL = /Component\.literal\(\s*"((?:[^"\\]|\\.)*)"/g;
const PROSE = /[A-Za-z]{2}/;

/**
 * Finds `Component.literal` calls in machine screens that start with English prose. Symbols, digits, and
 * single-letter labels such as the "-" and "+" buttons or the "E"/"F"/"I" channel markers are not prose.
 */
export async function findScreenProse() {
    const findings = [];
    for (const file of (await readdir(SCREEN_ROOT)).filter((name) => name.endsWith(".java")).sort()) {
        const source = await readFile(path.join(SCREEN_ROOT, file), "utf8");
        const allowed = ALLOWED_LITERALS[file] ?? [];
        for (const match of source.matchAll(LITERAL)) {
            if (!PROSE.test(match[1]) || allowed.includes(match[1])) {
                continue;
            }
            const line = source.slice(0, match.index).split("\n").length;
            findings.push(`${file}:${line}: Component.literal("${match[1]}")`);
        }
    }
    return findings;
}

export async function checkScreenStrings() {
    const findings = await findScreenProse();
    if (findings.length > 0) {
        throw new Error(
            `Screens must use Component.translatable for prose; move these to lang/en_us.json or add an allowlist entry in check-screen-strings.mjs:\n${findings.join("\n")}`
        );
    }
    console.log("Screen string check passed.");
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkScreenStrings();
}
