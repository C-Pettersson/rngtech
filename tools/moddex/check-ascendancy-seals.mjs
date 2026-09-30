import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PROJECT_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const RECIPE_ROOT = path.join(PROJECT_ROOT, "src/main/resources/data/rngtech/recipe");
const SEAL_OUTPUT = /^rngtech:(ascendancy_seal_\d+|[a-z_]*seal_core)$/;
const CONDITION = "rngtech:ascendancy_seal_recipes_enabled";

/**
 * Every recipe that makes an Ascendancy Seal or Seal Core must carry the config condition, so pack makers can turn all
 * default Seal recipes off and award Seals another way.
 */
export async function checkAscendancySeals({ log = true } = {}) {
    const failures = [];
    let seals = 0;
    for (const entry of await readdir(RECIPE_ROOT, { recursive: true, withFileTypes: true })) {
        if (!entry.isFile() || !entry.name.endsWith(".json")) {
            continue;
        }
        const file = path.join(entry.parentPath, entry.name);
        const json = JSON.parse(await readFile(file, "utf8"));
        const output = json.result?.id ?? json.result?.stack?.id;
        if (!output || !SEAL_OUTPUT.test(output)) {
            continue;
        }
        seals++;
        if (!(json["neoforge:conditions"] ?? []).some((condition) => condition.type === CONDITION)) {
            failures.push(`${path.relative(PROJECT_ROOT, file).replaceAll(path.sep, "/")} makes ${output} without the ${CONDITION} condition`);
        }
    }
    if (seals === 0) {
        failures.push("No Ascendancy Seal recipes were found");
    }
    if (failures.length) {
        throw new Error(`Ascendancy Seal recipe check failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    }
    if (log) {
        console.log(`Ascendancy Seal recipes PASS: ${seals} recipes carry ${CONDITION}`);
    }
    return seals;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkAscendancySeals();
}
