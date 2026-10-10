import { readFile, mkdir, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../..");
const RESOURCES_ROOT = path.join(ROOT, "src/main/resources");
const DATA_ROOT = path.join(RESOURCES_ROOT, "data");
const RECIPE_ROOT = path.join(DATA_ROOT, "rngtech/recipe");
const MATERIAL_CATALOG_PATH = path.join(DATA_ROOT, "rngtech/materials/catalog.json");
const RPG_SRC = path.join(ROOT, "src/main/java/com/rngtech/rpg");
const MACHINE_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/machine");
const ENERGY_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/energy");
const TOOL_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/tool");
const CALIBRATION_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/calibration");
const RECYCLING_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/recycling");
const DIVINITY_SRC = path.join(ROOT, "src/main/java/com/rngtech/content/divinity");
const REFINEMENT_SRC = path.join(RPG_SRC, "refinement");
const LANG_PATH = path.join(RESOURCES_ROOT, "assets/rngtech/lang/en_us.json");
const OUTPUT_PATH = path.join(__dirname, "generated/modifier-data.json");

const STAGE_DEFINITIONS = [
    { stage: 0, name: "Primitive Stage", role: "Manual or tutorial crafting" },
    { stage: 1, name: "Iron Stage", role: "First mechanical components" },
    { stage: 2, name: "Copper Stage", role: "First energy and heat transfer" },
    { stage: 3, name: "Bronze Stage", role: "First alloy pressure" },
    { stage: 4, name: "Steel Stage", role: "Stable midgame machines" },
    { stage: 5, name: "Aluminum Stage", role: "Better control and mitigation" },
    { stage: 6, name: "Titanium Stage", role: "High heat and throughput" },
    { stage: 7, name: "Tungstensteel Stage", role: "Late-game material pressure" },
    { stage: 8, name: "Exotic Stage", role: "Optional endgame extension" }
];

const CORE_STAGE_CATEGORIES = new Set(["machine", "component", "tool", "pseudo"]);

const DECREASED_PERCENT_STATS = new Set([
    "ENERGY_USAGE",
    "FE_USAGE",
    "ORE_BURST_FE_USAGE",
    "IDLE_LOSS",
    "WARMUP_TIME",
    "COOLING_RATE"
]);

const LEGACY_AFFIX_VARIANT_SUFFIXES = ["tuned", "reinforced", "harmonic", "focused", "amplified"];
const SUPPRESSED_MATERIAL_FORMS = new Map([
    ["bronze", new Set(["ore", "crushed"])]
]);
const MATERIAL_FORMS_REQUIRING_RECIPE = new Set(["plate", "gear", "rod", "coil", "casing"]);
const NON_PROGRESSION_RECIPE_ROLES = new Set(["failure_recovery"]);
const PROGRESSION_STAGE_FLAGS = [
    {
        id: "pressed_connector_steel_access",
        label: "Pressed connector Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: pressed connectors require Steel-stage Connector Mold access",
        detail: "Basic, Copper, and Gold pressed connector modules expose lower transfer tiers, but standalone survival access starts with Steel-stage Connector Mold tooling.",
        pattern: /^rngtech:(?:basic|copper|gold)_(?:energy|fluid|item)_connector$/
    },
    {
        id: "casing_mold_steel_access",
        label: "Casing Mold Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: Casing Mold requires Steel ingot tooling",
        detail: "Casing Mold is an early press utility concept, but standalone survival crafting requires Steel Ingots.",
        ids: new Set(["rngtech:casing_mold"])
    },
    {
        id: "copper_solar_panel_steel_access",
        label: "Copper Solar Panel Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: Copper Solar Panel depends on Steel-stage pressed connectors",
        detail: "Copper Solar Panel remains copper-tier hardware, but standalone survival crafting consumes a Copper Energy Connector from the Steel-stage Connector Mold path.",
        ids: new Set(["rngtech:copper_solar_panel"])
    },
    {
        id: "resonance_calibrator_steel_access",
        label: "Resonance Calibrator Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: early Resonance Calibrator parts require Steel-stage access",
        detail: "Iron and Copper Resonance Calibrator hardware are cheaper low-tier alternatives, not pre-Steel progression access.",
        pattern: /^rngtech:(?:iron|copper)_(?:resonance_calibrator_chassis|resonance_coil|control_board|stabilizer_matrix)$/
    },
    {
        id: "conductive_calibration_steel_access",
        label: "Conductive calibration Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: Conductive calibration depends on Steel-stage connector tooling",
        detail: "Calibrated Conductive Component can carry low component-stage output data, but its default survival pattern chain depends on Energy Coil and pressed connectors.",
        ids: new Set(["rngtech:calibrated_conductive_component"])
    },
    {
        id: "advanced_circuit_steel_access",
        label: "Advanced circuit Steel access floor",
        stageFloor: 4,
        source: "progression stage flag: Advanced Electric Circuit requires Steel-stage plate forming",
        detail: "Advanced Electric Circuit uses Steel Plate through the Advanced Circuit Blank path, so ModDex treats it as Steel-stage access.",
        ids: new Set(["rngtech:advanced_electric_circuit"])
    }
];
const CRAFTING_GRID_WIDTH = 3;
const CRAFTING_GRID_HEIGHT = 3;
const SHAPED_CRAFTING_TYPES = new Set([
    "minecraft:crafting_shaped",
    "rngtech:calibrated_shaped",
    "rngtech:trait_shaped",
    "rngtech:tool_damage_shaped"
]);
const TRIMMED_SHAPED_CRAFTING_TYPES = new Set([
    "minecraft:crafting_shaped",
    "rngtech:trait_shaped",
    "rngtech:tool_damage_shaped"
]);

export async function buildModifierData(outputPath = OUTPUT_PATH) {
    const [profilesSource, rollerSource, operationSource, langSource] = await Promise.all([
        readJava("ModifierEligibilityProfiles.java"),
        readJava("MachineTraitRoller.java"),
        readFile(path.join(REFINEMENT_SRC, "RefinementOperation.java"), "utf8"),
        readFile(LANG_PATH, "utf8")
    ]);
    const lang = JSON.parse(langSource);
    const ranges = parseRangeLists(profilesSource);
    const constants = parseStringConstants(profilesSource);
    const behaviorLists = parseBehaviorLists(profilesSource);
    const definitions = parseDefinitions(profilesSource, ranges, constants);
    const profiles = parseProfiles(profilesSource, definitions, behaviorLists, ranges, constants, lang);
    const operations = parseRefinementOperations(operationSource, lang);
    const rbom = await buildRecipeBom(lang, profiles);
    const data = {
        schemaVersion: 1,
        generatedAt: new Date().toISOString(),
        source: {
            profiles: "src/main/java/com/rngtech/rpg/ModifierEligibilityProfiles.java",
            roller: "src/main/java/com/rngtech/rpg/MachineTraitRoller.java",
            refinement: "src/main/java/com/rngtech/rpg/refinement/RefinementEngine.java",
            recipes: "src/main/resources/data/rngtech/recipe",
            tags: "src/main/resources/data/*/tags/item"
        },
        labels: buildLabels(lang),
        rarity: {
            naturalOdds: [
                { rarity: "NORMAL", chance: 0.5 },
                { rarity: "MAGIC", chance: 0.4 },
                { rarity: "RARE", chance: 0.1 }
            ],
            affixSlots: {
                NORMAL: { prefix: 0, suffix: 0 },
                MAGIC: { prefix: 1, suffix: 1 },
                RARE: { prefix: 3, suffix: 3 },
                UNIQUE: { prefix: 0, suffix: 0 }
            }
        },
        tiers: {
            baseOdds: {
                NORMAL: [{ tier: 1, chance: 1 }],
                MAGIC: [{ tier: 1, chance: 0.7 }, { tier: 2, chance: 0.3 }],
                RARE: [{ tier: 2, chance: 0.5 }, { tier: 3, chance: 0.4 }, { tier: 4, chance: 0.1 }],
                UNIQUE: [{ tier: 4, chance: 1 }]
            },
            naturalMaxByStage: [
                { maxStage: 0, tier: 1 },
                { maxStage: 2, tier: 1 },
                { maxStage: 4, tier: 2 },
                { maxStage: 6, tier: 3 },
                { maxStage: null, tier: 4 }
            ],
            overflowKeepChance: [
                { overflow: 1, chance: 0.35 },
                { overflow: 2, chance: 0.15 },
                { overflow: 3, chance: 0.05 }
            ]
        },
        refinementPotential: {
            byStage: [
                { maxStage: 0, min: 0, max: 2 },
                { maxStage: 2, min: 4, max: 8 },
                { maxStage: 4, min: 8, max: 14 },
                { maxStage: 6, min: 14, max: 22 },
                { maxStage: null, min: 22, max: 32 }
            ],
            smallCostOdds: [
                { cost: 1, chance: 0.5 },
                { cost: 2, chance: 0.3 },
                { cost: 3, chance: 0.15 },
                { cost: 4, chance: 0.05 }
            ],
            addOrUpgrade: {
                min: 1,
                max: 18,
                minByTier: { 1: 1, 2: 4, 3: 7, 4: 10 }
            }
        },
        refinementOperations: operations,
        profiles,
        rbom
    };
    await mkdir(path.dirname(outputPath), { recursive: true });
    await writeFile(outputPath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
    return data;
}

async function readJava(fileName) {
    return readFile(path.join(RPG_SRC, fileName), "utf8");
}

async function buildRecipeBom(lang, profiles) {
    const [recipePaths, tagPaths, materialCatalog] = await Promise.all([
        listJsonFiles(RECIPE_ROOT),
        listJsonFiles(DATA_ROOT).then((files) => files.filter(isItemTagPath)),
        readMaterialCatalog()
    ]);
    const tags = {};
    for (const tagPath of tagPaths) {
        const tag = JSON.parse(await readFile(tagPath, "utf8"));
        const id = itemTagId(tagPath);
        tags[id] = {
            id,
            label: tagLabel(id, lang),
            path: relativeToRoot(tagPath),
            values: tagValues(tag.values ?? [])
        };
    }

    const recipes = [];
    const craftingRecipes = [];
    const itemIds = new Set();
    const itemLabels = new Map();
    for (const recipePath of recipePaths) {
        const recipeJson = JSON.parse(await readFile(recipePath, "utf8"));
        const recipe = normalizeRecipe(recipeJson, recipeId(recipePath), recipePath, lang);
        if (!recipe) {
            continue;
        }
        recipes.push(recipe);
        const craftingRecipe = normalizeCraftingConflictRecipe(recipeJson, recipe);
        if (craftingRecipe) {
            craftingRecipes.push(craftingRecipe);
        }
        rememberItem(itemIds, itemLabels, recipe.result.id, recipe.result.label);
        for (const input of recipe.inputs) {
            if (input.kind === "item" || input.kind === "fluid") {
                rememberItem(itemIds, itemLabels, input.id, input.label);
            }
        }
    }
    rememberLangItems(itemIds, itemLabels, lang);
    for (const pseudoRecipe of await buildPseudoRecipes(lang, materialCatalog, tags)) {
        recipes.push(pseudoRecipe);
        rememberItem(itemIds, itemLabels, pseudoRecipe.result.id, pseudoRecipe.result.label);
        for (const input of pseudoRecipe.inputs) {
            if (input.kind === "item" || input.kind === "fluid") {
                rememberItem(itemIds, itemLabels, input.id, input.label);
            }
        }
    }
    craftingRecipes.push(...vanillaCraftingConflictRecipes(lang));
    recipes.sort((left, right) => recipeSortKey(left).localeCompare(recipeSortKey(right)));
    const recipeConflicts = findRecipeConflicts(craftingRecipes, tags);
    const recipesByOutput = recipesByOutputMap(recipes);

    const items = [...itemIds]
        .sort((left, right) => (itemLabels.get(left) ?? itemLabel(left, lang)).localeCompare(itemLabels.get(right) ?? itemLabel(right, lang)))
        .map((id) => ({ id, label: itemLabels.get(id) ?? itemLabel(id, lang) }));
    const stagePreview = await buildStagePreview({
        recipes,
        recipesByOutput,
        tags,
        items,
        lang,
        materialCatalog,
        profiles
    });

    return {
        recipes,
        recipesByOutput,
        coreResourceIds: buildCoreResourceIds(materialCatalog, tags),
        recipeConflicts,
        tags,
        items,
        stagePreview
    };
}

function recipesByOutputMap(recipes) {
    const recipesByOutput = {};
    for (const recipe of recipes) {
        recipesByOutput[recipe.result.id] ??= [];
        recipesByOutput[recipe.result.id].push(recipe.id);
    }
    return recipesByOutput;
}

function recipeProgressionSource(recipe) {
    return !NON_PROGRESSION_RECIPE_ROLES.has(recipe.sourceRole);
}

function recipeSourceRole(json, id) {
    if (isMalformedRecoveryRecipe(json, id)) {
        return "failure_recovery";
    }
    return "ordinary";
}

function isMalformedRecoveryRecipe(json, id = "") {
    return json.group === "malformed_ingot_recovery"
        || json.malformed_material !== undefined
        || id.startsWith("rngtech:furnace/malformed/");
}

function buildCoreResourceIds(materialCatalog, tags) {
    const ids = new Set();
    const alloyMaterials = materialGroupFamilies(materialCatalog, "alloys");
    for (const material of materialCatalog.materials ?? []) {
        if (!alloyMaterials.has(material.id)) {
            for (const id of materialFormIds(materialCatalog, tags, material.id, "ingot")) {
                ids.add(id);
            }
        }
    }
    for (const material of materialCatalog.non_stage_materials ?? []) {
        if (material.maps_to) {
            ids.add(material.maps_to);
        }
    }
    return [...ids].sort();
}

function materialGroupFamilies(materialCatalog, groupId) {
    const group = (materialCatalog.material_groups ?? []).find((entry) => entry.id === groupId);
    return new Set(group?.families ?? []);
}

function materialFormIds(materialCatalog, tags, materialId, formId) {
    const ids = new Set();
    const tagId = materialFormTagId(formId, materialId);
    for (const id of tagItems(tagId, tags)) {
        ids.add(id);
    }
    for (const id of materialCatalog.vanilla_form_mappings?.[materialId]?.[formId] ?? []) {
        ids.add(id);
    }
    if (ids.size === 0) {
        const form = (materialCatalog.metal_forms ?? []).find((entry) => entry.id === formId);
        if (form?.item_pattern && !isSuppressedMaterialForm(materialId, formId)) {
            ids.add(`rngtech:${form.item_pattern.replace("{material}", materialId)}`);
        }
    }
    return ids;
}

function materialFormTagId(formId, materialId) {
    const tagForm = {
        ingot: "ingots",
        dust: "dusts",
        nugget: "nuggets",
        storage_block: "storage_blocks",
        plate: "plates",
        gear: "gears",
        rod: "rods",
        coil: "coils",
        casing: "casings"
    }[formId] ?? `${formId}s`;
    return `c:${tagForm}/${materialId}`;
}

async function buildPseudoRecipes(lang, materialCatalog, tags) {
    const stages = await divinitySiphonStages();
    return [
        ...questSurfacePseudoRecipes(lang),
        ...vanillaMaterialConversionPseudoRecipes(materialCatalog, tags, lang),
        ...stages.map((stage) => divinitySiphonPseudoRecipe(stage, lang))
    ];
}

function vanillaMaterialConversionPseudoRecipes(materialCatalog, tags, lang) {
    const recipes = [];
    for (const material of materialCatalog.materials ?? []) {
        const ingotIds = [...materialFormIds(materialCatalog, tags, material.id, "ingot")];
        const nuggetIds = [...materialFormIds(materialCatalog, tags, material.id, "nugget")];
        if (ingotIds.length !== 1 || nuggetIds.length !== 1) {
            continue;
        }
        const nuggetId = nuggetIds[0];
        if (!nuggetId.startsWith("minecraft:")) {
            continue;
        }
        recipes.push({
            id: `minecraft:${itemPath(nuggetId)}_from_${material.id}_ingot`,
            type: "minecraft:crafting_shapeless",
            typeLabel: recipeTypeLabel("minecraft:crafting_shapeless"),
            path: `builtin/minecraft/${itemPath(nuggetId)}_from_ingot.json`,
            group: "vanilla_material_conversions",
            result: {
                kind: "item",
                id: nuggetId,
                count: 9,
                label: itemLabel(nuggetId, lang)
            },
            inputs: [labelInput({
                kind: "tag",
                id: materialFormTagId("ingot", material.id),
                count: 1,
                reusable: false,
                role: "",
                note: "Vanilla ingot-to-nugget conversion"
            }, lang)],
            requirements: {
                minimumComponentStage: null,
                requiredComponentStage: null,
                minimumMaterialStage: null,
                requiredProcessingLevel: null,
                minimumTemperature: null
            },
            outputStage: null,
            metadata: ["Vanilla conversion"]
        });
    }
    return recipes;
}

async function divinitySiphonStages() {
    const stageSource = await readOptionalUtf8(path.join(DIVINITY_SRC, "DivinitySiphonStage.java"));
    if (!stageSource) {
        return [];
    }
    return [...stageSource.matchAll(/([A-Z]+)\((\d+),\s*[\d_L]+,\s*[\d_]+,\s*(\d+),\s*[\d_]+\)/g)]
        .map((match) => ({
            name: match[1],
            serializedName: match[1].toLowerCase(),
            componentStage: Number(match[2]),
            requiredLinks: Number(match[3])
        }));
}

function questSurfacePseudoRecipes(lang) {
    return [
        pseudoProcessRecipe({
            id: "tool_bench/modular_pick",
            resultId: "rngtech:modular_pick",
            stage: 0,
            path: "src/main/java/com/rngtech/content/menu/ToolBenchMenu.java",
            group: "tool_bench_assembly",
            inputs: [
                pseudoItemInput("rngtech:flint_pick_head", 1, lang),
                pseudoItemInput("rngtech:wooden_tool_rod", 1, lang)
            ],
            metadata: ["Tool Bench assembly", "Primitive pick setup"]
        }, lang),
        pseudoProcessRecipe({
            id: "tool_bench/modular_hammer",
            resultId: "rngtech:modular_hammer",
            stage: 1,
            path: "src/main/java/com/rngtech/content/menu/ToolBenchMenu.java",
            group: "tool_bench_assembly",
            inputs: [
                pseudoItemInput("rngtech:tiny_anvil", 1, lang, { reusable: true, role: "tool", note: "Tool Bench Gear unlock" }),
                pseudoItemInput("rngtech:iron_hammer_head", 1, lang),
                pseudoItemInput("rngtech:iron_tool_rod", 1, lang)
            ],
            metadata: ["Tool Bench assembly", "First powered hammer setup"]
        }, lang),
        pseudoProcessRecipe({
            id: "tool_bench/modular_axe",
            resultId: "rngtech:modular_axe",
            stage: 1,
            path: "src/main/java/com/rngtech/content/menu/ToolBenchMenu.java",
            group: "tool_bench_assembly",
            inputs: [
                pseudoItemInput("rngtech:iron_axe_head", 1, lang),
                pseudoItemInput("rngtech:iron_tool_rod", 1, lang)
            ],
            metadata: ["Tool Bench assembly", "First axe setup"]
        }, lang),
        pseudoProcessRecipe({
            id: "tool_bench/modular_treefeller",
            resultId: "rngtech:modular_treefeller",
            stage: 1,
            path: "src/main/java/com/rngtech/content/menu/ToolBenchMenu.java",
            group: "tool_bench_assembly",
            inputs: [
                pseudoItemInput("rngtech:tiny_anvil", 1, lang, { reusable: true, role: "tool", note: "Tool Bench Gear unlock" }),
                pseudoItemInput("rngtech:iron_treefeller_head", 1, lang),
                pseudoItemInput("rngtech:iron_tool_rod", 1, lang)
            ],
            metadata: ["Tool Bench assembly", "First treefeller setup"]
        }, lang),
        pseudoProcessRecipe({
            id: "wooden_composter/composted_biomass",
            resultId: "rngtech:composted_biomass",
            stage: 0,
            path: "src/main/java/com/rngtech/content/blockentity/WoodenComposterBlockEntity.java",
            group: "wooden_composter",
            inputs: [
                pseudoItemInput("rngtech:wooden_composter", 1, lang, { reusable: true, role: "machine", note: "Composting machine" }),
                pseudoItemInput("minecraft:wheat_seeds", 8, lang, { role: "input", note: "Representative compostable item" })
            ],
            metadata: ["Wooden Composter process", "Representative compostable input"]
        }, lang),
        pseudoProcessRecipe({
            id: "melter/lubricant_bucket",
            resultId: "rngtech:lubricant_bucket",
            stage: 6,
            path: "src/main/resources/data/rngtech/recipe/melter/lubricant.json",
            group: "melter_lubricants",
            inputs: [
                pseudoItemInput("rngtech:melter", 1, lang, { reusable: true, role: "machine", note: "Melter" }),
                pseudoItemInput("minecraft:bucket", 1, lang, { role: "container" }),
                pseudoItemInput("minecraft:water_bucket", 1, lang, { role: "fluid", note: "Representative water input" }),
                pseudoItemInput("rngtech:plant_reagent", 1, lang, { role: "input", note: "Representative lubricant additive" }),
                pseudoItemInput("rngtech:resin", 1, lang, { role: "input", note: "Representative lubricant base" })
            ],
            metadata: ["Melter fluid process", "Bucketed lubricant output"]
        }, lang),
        pseudoProcessRecipe({
            id: "component_assembler/electrolyte_solution_from_dry_electrolyte",
            resultId: "rngtech:electrolyte_solution",
            resultKind: "fluid",
            resultCount: 125,
            path: "src/main/java/com/rngtech/content/blockentity/BatteryAssemblerBlockEntity.java",
            group: "component_assembly_fluids",
            inputs: [
                pseudoItemInput("rngtech:battery_assembler", 1, lang, { reusable: true, role: "machine", note: "Component Assembler" }),
                pseudoItemInput("rngtech:electrolyte", 1, lang, { role: "input", note: "Dry electrolyte reagent" })
            ],
            metadata: ["Component Assembler dry electrolyte conversion", "125 mB per item"]
        }, lang)
    ];
}

function pseudoProcessRecipe({
    id,
    resultId,
    resultKind = "item",
    resultCount = 1,
    stage = null,
    path: recipePath,
    group,
    inputs,
    metadata
}, lang) {
    return {
        id: `rngtech:pseudo/${id}`,
        type: "rngtech:pseudo_process",
        typeLabel: "Pseudo Process",
        path: recipePath,
        group,
        result: {
            kind: resultKind,
            id: resultId,
            count: resultCount,
            label: itemLabel(resultId, lang)
        },
        inputs,
        requirements: {
            minimumComponentStage: null,
            requiredComponentStage: null,
            minimumMaterialStage: null,
            requiredProcessingLevel: null,
            minimumTemperature: null
        },
        outputStage: stage,
        metadata
    };
}

function pseudoItemInput(id, count, lang, options = {}) {
    return labelInput({
        kind: "item",
        id,
        count,
        reusable: options.reusable ?? false,
        role: options.role ?? "",
        note: options.note ?? ""
    }, lang);
}

function divinitySiphonPseudoRecipe(stage, lang) {
    const prefix = stage.serializedName;
    const casingCount = 25 - 1 - stage.requiredLinks;
    const matrixLink = stage.name === "BREATH" ? "entropy_matrix_link" : "ascendant_matrix_link";
    const resultId = `rngtech:pseudo/complete_${prefix}_divinity_siphon`;
    const label = `Complete ${titleCase(prefix)} Divinity Siphon`;
    const inputs = [
        pseudoInput(`${prefix}_divinity_siphon_controller`, 1, lang),
        pseudoInput(`${prefix}_siphon_casing`, casingCount, lang),
        pseudoInput(`${prefix}_divine_conduit_pylon`, 4, lang),
        pseudoInput(`${prefix}_reliquary_lens_housing`, 4, lang),
        pseudoInput(`${prefix}_containment_seal`, 4, lang),
        pseudoInput(`${prefix}_ritual_heart`, 1, lang),
        pseudoInput(matrixLink, stage.requiredLinks, lang)
    ];
    return {
        id: resultId,
        type: "rngtech:pseudo_target",
        typeLabel: "Pseudo Target",
        path: "src/main/java/com/rngtech/content/divinity/DivinitySiphonStructure.java",
        group: "complete_multiblocks",
        result: {
            kind: "item",
            id: resultId,
            count: 1,
            label
        },
        inputs,
        requirements: {
            minimumComponentStage: null,
            requiredComponentStage: stage.componentStage,
            minimumMaterialStage: null,
            requiredProcessingLevel: null,
            minimumTemperature: null
        },
        outputStage: stage.componentStage,
        metadata: [
            "Complete multiblock",
            `Stage ${stage.componentStage}`,
            `${inputs.reduce((sum, input) => sum + input.count, 0)} blocks`
        ]
    };
}

function pseudoInput(pathId, count, lang) {
    const id = `rngtech:${pathId}`;
    return {
        kind: "item",
        id,
        count,
        reusable: false,
        role: "structure",
        note: "Placed block",
        label: itemLabel(id, lang)
    };
}

function rememberItem(itemIds, itemLabels, id, label) {
    itemIds.add(id);
    if (label && !itemLabels.has(id)) {
        itemLabels.set(id, label);
    }
}

function rememberLangItems(itemIds, itemLabels, lang) {
    for (const [key, label] of Object.entries(lang)) {
        const match = key.match(/^(?:item|block)\.rngtech\.([a-z0-9_/.]+)$/);
        if (match && !isSuppressedMaterialFormItem(`rngtech:${match[1]}`)) {
            rememberItem(itemIds, itemLabels, `rngtech:${match[1]}`, label);
        }
    }
}

async function readMaterialCatalog() {
    return JSON.parse(await readFile(MATERIAL_CATALOG_PATH, "utf8"));
}

async function buildStagePreview({ recipes, recipesByOutput, tags, items, lang, materialCatalog, profiles }) {
    const progressionRecipes = recipes.filter((recipe) => recipeProgressionSource(recipe));
    const progressionRecipesByOutput = recipesByOutputMap(progressionRecipes);
    const materialStages = new Map((materialCatalog.materials ?? []).map((material) => [material.id, material.stage]));
    const materialNames = [...materialStages.keys()].sort((left, right) => right.length - left.length);
    const alloyMaterials = materialGroupFamilies(materialCatalog, "alloys");
    const materialItemIds = materialFormItemIds(materialCatalog, tags);
    const materialFormByItem = materialFormInfoByItem(materialCatalog);
    const directMaterialInputRecipeIds = directItemInputRecipeIdsByItem(progressionRecipes, materialItemIds);
    const stageByItem = new Map();
    const stageSourceByItem = new Map();
    const stageFlagsByItem = new Map();
    const recipesById = new Map(recipes.map((recipe) => [recipe.id, recipe]));

    const addStage = (id, stage, source) => {
        if (stage === null || stage === undefined || Number.isNaN(stage)) {
            return;
        }
        const normalizedStage = Number(stage);
        if (!stageByItem.has(id)) {
            stageByItem.set(id, normalizedStage);
            stageSourceByItem.set(id, source);
        }
    };

    for (const id of materialItemIds) {
        const stage = inferMaterialItemStage(id, materialCatalog, materialStages);
        addStage(id, stage, "material catalog");
    }
    for (const [tagId, tag] of Object.entries(tags)) {
        const match = tagId.match(/^rngtech:materials\/stage_(\d+)$/);
        if (!match) {
            continue;
        }
        for (const value of tag.values) {
            if (value.kind === "item") {
                materialItemIds.add(value.id);
                addStage(value.id, Number(match[1]), `#${tagId}`);
            }
        }
    }
    for (const entry of await knownComponentStageEntries()) {
        addStage(entry.id, entry.stage, entry.source);
    }
    for (const material of materialCatalog.non_stage_materials ?? []) {
        if (material.handling === "register") {
            addStage(`rngtech:${material.id}`, 0, "non-stage material catalog");
        }
    }
    for (const recipe of progressionRecipes) {
        addStage(recipe.result.id, recipe.outputStage, `${recipe.id} result data`);
    }

    for (const item of items) {
        if (!stageByItem.has(item.id)) {
            const inferred = inferStageFromItemId(item.id, materialStages, materialNames);
            addStage(item.id, inferred, "item id");
        }
    }

    applyProgressionStageFlags(stageByItem, stageSourceByItem, stageFlagsByItem);
    inferRecipeOutputStages(progressionRecipes, stageByItem, stageSourceByItem, materialItemIds, materialStages, materialNames, tags);
    applyProgressionStageFlags(stageByItem, stageSourceByItem, stageFlagsByItem);
    inferRecipeOutputStages(progressionRecipes, stageByItem, stageSourceByItem, materialItemIds, materialStages, materialNames, tags);
    applyProgressionStageFlags(stageByItem, stageSourceByItem, stageFlagsByItem);

    const context = {
        recipesByOutput: progressionRecipesByOutput,
        recipesById,
        tags,
        materialStages,
        materialNames,
        alloyMaterials,
        materialItemIds,
        stageByItem,
        materialFormByItem,
        directMaterialInputRecipeIds,
        itemById: new Map(items.map((item) => [item.id, item])),
        reachabilityMemo: new Map()
    };
    const stageItems = items
        .filter((item) => item.id.startsWith("rngtech:") && stageByItem.has(item.id))
        .map((item) => {
            const stage = stageByItem.get(item.id);
            const recipeIds = progressionRecipesByOutput[item.id] ?? [];
            const nonProgressionRecipeIds = (recipesByOutput[item.id] ?? [])
                .filter((recipeId) => !recipeIds.includes(recipeId));
            const firstRecipe = recipeIds.map((id) => recipesById.get(id)).filter(Boolean)[0] ?? null;
            const category = stageItemCategory(item.id, materialItemIds);
            const modifierCoverage = modifierCoverageForItem(item.id, category, profiles);
            const directStageInputs = firstRecipe
                ? firstRecipe.inputs
                    .filter((input) => !input.reusable)
                    .map((input) => inputStageInfo(input, context))
                    .filter(Boolean)
                : [];
            const reachability = describeReachability(analyzeReachability(item.id, stage, context), context);
            const materialInputRecipeIds = directMaterialInputRecipeIds.get(item.id) ?? [];
            const stageItem = { ...item, stage };
            const issues = stageItemIssues(stageItem, category, recipeIds, reachability, materialInputRecipeIds, context);
            return {
                id: item.id,
                label: item.label,
                stage,
                stageName: STAGE_DEFINITIONS.find((entry) => entry.stage === stage)?.name ?? `Stage ${stage}`,
                category,
                categoryLabel: stageCategoryLabel(category),
                stageFlags: stageFlagsByItem.get(item.id) ?? [],
                modifierCoverage,
                stageSource: stageSourceByItem.get(item.id) ?? "inferred",
                craftable: recipeIds.length > 0,
                reachable: reachability.reachable,
                reachability,
                recipeIds,
                nonProgressionRecipeIds,
                materialInputRecipeIds,
                firstRecipeId: firstRecipe?.id ?? null,
                firstRecipeType: firstRecipe?.typeLabel ?? null,
                firstRecipeNote: firstRecipe?.metadata?.join(", ") ?? "",
                directStageInputs,
                issues
            };
        })
        .sort((left, right) => stageItemSortKey(left).localeCompare(stageItemSortKey(right)));

    return {
        stages: STAGE_DEFINITIONS,
        categories: stageFilterCategories(),
        items: stageItems,
        summaryByStage: stageSummary(stageItems)
    };
}

function materialFormItemIds(materialCatalog, tags) {
    const ids = new Set();
    for (const material of materialCatalog.materials ?? []) {
        for (const form of materialCatalog.metal_forms ?? []) {
            if (form.item_pattern) {
                const id = `rngtech:${form.item_pattern.replace("{material}", material.id)}`;
                if (!isSuppressedMaterialForm(material.id, form.id)) {
                    ids.add(id);
                }
            }
        }
    }
    for (const [tagId, tag] of Object.entries(tags)) {
        if (!tagId.startsWith("rngtech:materials/stage_")) {
            continue;
        }
        for (const value of tag.values) {
            if (value.kind === "item") {
                ids.add(value.id);
            }
        }
    }
    return ids;
}

function materialFormInfoByItem(materialCatalog) {
    const entries = new Map();
    for (const material of materialCatalog.materials ?? []) {
        for (const form of materialCatalog.metal_forms ?? []) {
            if (isSuppressedMaterialForm(material.id, form.id)) {
                continue;
            }
            if (form.item_pattern) {
                entries.set(`rngtech:${form.item_pattern.replace("{material}", material.id)}`, {
                    material: material.id,
                    form: form.id,
                    stage: material.stage
                });
            }
        }
    }
    return entries;
}

function directItemInputRecipeIdsByItem(recipes, materialItemIds) {
    const recipeIdsByItem = new Map();
    for (const recipe of recipes) {
        for (const input of recipe.inputs ?? []) {
            if (input.reusable || input.kind !== "item" || !materialItemIds.has(input.id)) {
                continue;
            }
            const recipeIds = recipeIdsByItem.get(input.id) ?? [];
            recipeIds.push(recipe.id);
            recipeIdsByItem.set(input.id, recipeIds);
        }
    }
    return recipeIdsByItem;
}

function inferMaterialItemStage(id, materialCatalog, materialStages) {
    const local = itemPath(id);
    for (const material of materialCatalog.materials ?? []) {
        for (const form of materialCatalog.metal_forms ?? []) {
            if (isSuppressedMaterialForm(material.id, form.id)) {
                continue;
            }
            if (form.item_pattern?.replace("{material}", material.id) === local) {
                return materialStages.get(material.id) ?? null;
            }
        }
    }
    return null;
}

function isSuppressedMaterialForm(materialId, formId) {
    return SUPPRESSED_MATERIAL_FORMS.get(materialId)?.has(formId) ?? false;
}

function isSuppressedMaterialFormItem(id) {
    const local = itemPath(id);
    for (const [materialId, forms] of SUPPRESSED_MATERIAL_FORMS) {
        for (const formId of forms) {
            if (formId === "ore" && local === `${materialId}_ore`) {
                return true;
            }
            if (formId === "crushed" && local === `crushed_${materialId}`) {
                return true;
            }
        }
    }
    return false;
}

async function knownComponentStageEntries() {
    const entries = [
        ...staticStageEntries()
    ];
    await appendEnumStages(entries, path.join(MACHINE_SRC, "CrusherChassisMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_crusher_chassis`, javaNumber(entry.args[0]), "CrusherChassisMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "FurnaceChassisMaterial.java"), (entry) => [
        stageEntry(javaString(entry.args[0]), javaNumber(entry.args[1]), "FurnaceChassisMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "AlloyFurnaceChassisMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_alloy_furnace_chassis`, javaNumber(entry.args[0]), "AlloyFurnaceChassisMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "CrushHeadMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_crush_head`, javaNumber(entry.args[0]), "CrushHeadMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "ServoMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_servo`, javaNumber(entry.args[0]), "ServoMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "FluidPumpMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_fluid_pump`, javaNumber(entry.args[0]), "FluidPumpMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "CathodeMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_cathode`, javaNumber(entry.args[0]), "CathodeMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "AlloyCrucibleMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_alloy_crucible`, javaNumber(entry.args[0]), "AlloyCrucibleMaterial")
    ]);
    await appendEnumStages(entries, path.join(MACHINE_SRC, "CompressorTankMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_compressor_tank`, javaNumber(entry.args[0]), "CompressorTankMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "BatteryCellMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_battery_cell`, javaNumber(entry.args[0]), "BatteryCellMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "BatteryChassisMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_battery_chassis`, javaNumber(entry.args[0]), "BatteryChassisMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "SolidFuelBurnerChassis.java"), (entry) => [
        stageEntry(javaString(entry.args[0]), javaNumber(entry.args[1]), "SolidFuelBurnerChassis")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "SolarPanelMaterial.java"), (entry) => [
        stageEntry(`${javaString(entry.args[0])}_solar_panel`, javaNumber(entry.args[1]), "SolarPanelMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "HeatCoreMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_heat_core`, javaNumber(entry.args[0]), "HeatCoreMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "FuelBoxMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_fuel_box`, javaNumber(entry.args[0]), "FuelBoxMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "ReactorChamberMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_reactor_chamber`, javaNumber(entry.args[0]), "ReactorChamberMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "RecoveryFilterMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_recovery_filter`, javaNumber(entry.args[0]), "RecoveryFilterMaterial")
    ]);
    await appendEnumStages(entries, path.join(ENERGY_SRC, "ContainmentLiningMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_containment_lining`, javaNumber(entry.args[0]), "ContainmentLiningMaterial")
    ]);
    await appendEnumStages(entries, path.join(CALIBRATION_SRC, "ResonanceCalibratorChassis.java"), (entry) => [
        stageEntry(`${entry.name}_resonance_calibrator_chassis`, javaNumber(entry.args[0]), "ResonanceCalibratorChassis")
    ]);
    await appendEnumStages(entries, path.join(CALIBRATION_SRC, "CalibrationGearMaterial.java"), (entry) =>
        ["resonance_coil", "control_board", "stabilizer_matrix"]
            .map((suffix) => stageEntry(`${entry.name}_${suffix}`, javaNumber(entry.args[0]), "CalibrationGearMaterial"))
    );
    await appendEnumStages(entries, path.join(RECYCLING_SRC, "ComponentRecyclerChassis.java"), (entry) => [
        stageEntry(`${entry.name}_component_recycler`, javaNumber(entry.args[0]), "ComponentRecyclerChassis")
    ]);
    await appendEnumStages(entries, path.join(RECYCLING_SRC, "DisassemblyHeadMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_disassembly_head`, javaNumber(entry.args[0]), "DisassemblyHeadMaterial")
    ]);
    await appendEnumStages(entries, path.join(TOOL_SRC, "ToolRodMaterial.java"), (entry) => [
        stageEntry(`${entry.name}_tool_rod`, javaNumber(entry.args[0]), "ToolRodMaterial")
    ]);
    await appendEnumStages(entries, path.join(TOOL_SRC, "ToolHeadMaterial.java"), (entry) =>
        ["pick", "hammer", "axe", "treefeller"]
            .map((family) => stageEntry(`${entry.name}_${family}_head`, javaNumber(entry.args[0]), "ToolHeadMaterial"))
    );
    await appendOptionalEnumStages(entries, path.join(DIVINITY_SRC, "DivinitySiphonStage.java"), (entry) => {
        const stage = javaNumber(entry.args[0]);
        const prefix = entry.name;
        const matrixLink = prefix === "breath" ? "entropy_matrix_link" : "ascendant_matrix_link";
        return [
            `${prefix}_divinity_siphon_controller`,
            `${prefix}_siphon_casing`,
            `${prefix}_divine_conduit_pylon`,
            `${prefix}_reliquary_lens_housing`,
            `${prefix}_containment_seal`,
            `${prefix}_ritual_heart`,
            `${prefix}_votive`,
            matrixLink,
            `pseudo/complete_${prefix}_divinity_siphon`
        ].map((id) => stageEntry(id, stage, "DivinitySiphonStage"));
    });
    return entries;
}

async function appendEnumStages(entries, filePath, mapper) {
    const source = await readFile(filePath, "utf8");
    appendEnumStageEntries(entries, source, mapper);
}

async function appendOptionalEnumStages(entries, filePath, mapper) {
    const source = await readOptionalUtf8(filePath);
    if (!source) {
        return;
    }
    appendEnumStageEntries(entries, source, mapper);
}

function appendEnumStageEntries(entries, source, mapper) {
    for (const entry of parseEnumEntries(source)) {
        entries.push(...mapper(entry));
    }
}

async function readOptionalUtf8(filePath) {
    try {
        return await readFile(filePath, "utf8");
    } catch (error) {
        if (error?.code === "ENOENT") {
            return null;
        }
        throw error;
    }
}

function parseEnumEntries(source) {
    const body = enumConstantBody(source);
    if (!body) {
        return [];
    }
    return splitTopLevel(body)
        .map((entry) => entry.trim())
        .filter(Boolean)
        .map((entry) => {
            const open = entry.indexOf("(");
            if (open < 0) {
                return null;
            }
            const close = matchingParen(entry, open);
            return {
                name: entry.slice(0, open).trim().toLowerCase(),
                args: splitTopLevel(entry.slice(open + 1, close))
            };
        })
        .filter(Boolean);
}

function enumConstantBody(source) {
    const match = source.match(/enum\s+\w+[^{]*\{/);
    if (!match) {
        return "";
    }
    const bodyStart = match.index + match[0].length;
    let depth = 0;
    let quote = null;
    for (let index = bodyStart; index < source.length; index++) {
        const char = source[index];
        const previous = source[index - 1];
        if (quote) {
            if (char === quote && previous !== "\\") {
                quote = null;
            }
            continue;
        }
        if (char === "\"" || char === "'") {
            quote = char;
            continue;
        }
        if (char === "(" || char === "[" || char === "{") {
            depth++;
            continue;
        }
        if (char === ")" || char === "]" || char === "}") {
            depth--;
            continue;
        }
        if (char === ";" && depth === 0) {
            return source.slice(bodyStart, index);
        }
    }
    return "";
}

function stageEntry(id, stage, source) {
    return {
        id: id.startsWith("rngtech:") ? id : `rngtech:${id}`,
        stage,
        source
    };
}

function staticStageEntries() {
    return [
        stageEntry("furnace", 0, "static stage catalog"),
        stageEntry("furnace_machine_chassis", 0, "static stage catalog"),
        stageEntry("washer_machine_chassis", 0, "static stage catalog"),
        stageEntry("wooden_composter", 0, "static stage catalog"),
        stageEntry("solid_fuel_burner_machine_chassis", 1, "static stage catalog"),
        stageEntry("forming_hammer", 1, "static stage catalog"),
        stageEntry("tiny_anvil", 1, "static stage catalog"),
        stageEntry("plate_mold", 1, "static stage catalog"),
        stageEntry("casing_mold", 1, "static stage catalog"),
        stageEntry("cable", 1, "static stage catalog"),
        stageEntry("basic_wire", 2, "static stage catalog"),
        stageEntry("basic_electric_circuit", 2, "static stage catalog"),
        stageEntry("advanced_electric_circuit", 3, "static stage catalog"),
        stageEntry("elite_electric_circuit", 6, "static stage catalog"),
        stageEntry("ultimate_electric_circuit", 7, "static stage catalog"),
        stageEntry("potential_reactor", 4, "static stage catalog"),
        stageEntry("metal_press", 4, "static stage catalog"),
        stageEntry("melter", 6, "static stage catalog"),
        stageEntry("tool_bench", 0, "static stage catalog"),
        stageEntry("affix_forge", 4, "static stage catalog"),
        stageEntry("affix_injector", 4, "static stage catalog"),
        stageEntry("ascension_matrix", 5, "static stage catalog"),
        stageEntry("nullifier_coil", 4, "static stage catalog"),
        stageEntry("potato_battery_cell", 0, "static stage catalog"),
        stageEntry("modular_pick", 0, "static stage catalog"),
        stageEntry("modular_hammer", 1, "static stage catalog"),
        stageEntry("modular_axe", 1, "static stage catalog"),
        stageEntry("modular_treefeller", 1, "static stage catalog"),
        stageEntry("composted_biomass", 0, "static stage catalog"),
        stageEntry("rich_biomass", 0, "static stage catalog"),
        stageEntry("lubricant_bucket", 6, "static stage catalog"),
        stageEntry("kinetic_modifier_lens", 4, "static stage catalog"),
        stageEntry("efficiency_modifier_lens", 4, "static stage catalog"),
        stageEntry("power_modifier_lens", 4, "static stage catalog"),
        stageEntry("speed_modifier_lens", 4, "static stage catalog"),
        stageEntry("yield_modifier_lens", 4, "static stage catalog"),
        stageEntry("stability_modifier_lens", 4, "static stage catalog"),
        stageEntry("control_modifier_lens", 4, "static stage catalog"),
        stageEntry("chaos_crystal", 5, "static stage catalog"),
        stageEntry("expansion_crystal", 5, "static stage catalog"),
        stageEntry("null_crystal", 5, "static stage catalog"),
        stageEntry("conservation_crystal", 5, "static stage catalog"),
        stageEntry("transmutation_crystal", 5, "static stage catalog"),
        stageEntry("resonance_crystal", 5, "static stage catalog"),
        stageEntry("destabilization_crystal", 5, "static stage catalog"),
        stageEntry("stabilization_crystal", 5, "static stage catalog"),
        stageEntry("volatile_catalyst", 5, "static stage catalog"),
        stageEntry("jam_debris", 5, "static stage catalog")
    ];
}

function inferRecipeOutputStages(recipes, stageByItem, stageSourceByItem, materialItemIds, materialStages, materialNames, tags) {
    let changed = true;
    let passes = 0;
    while (changed && passes < 5) {
        changed = false;
        passes++;
        for (const recipe of recipes) {
            if (!recipe.result.id.startsWith("rngtech:")) {
                continue;
            }
            const existingStage = stageByItem.get(recipe.result.id);
            const existingSource = stageSourceByItem.get(recipe.result.id) ?? "";
            if (existingStage !== undefined && !existingSource.endsWith(" inputs")) {
                continue;
            }
            const inferred = inferRecipeStage(recipe, { stageByItem, materialItemIds, materialStages, materialNames, tags });
            if (inferred !== null && (existingStage === undefined || inferred > existingStage)) {
                stageByItem.set(recipe.result.id, inferred);
                stageSourceByItem.set(recipe.result.id, `${recipe.id} inputs`);
                changed = true;
            }
        }
    }
}

function inferRecipeStage(recipe, context) {
    const requirement = componentRequirementStage(recipe.requirements);
    const inputStages = recipe.inputs
        .filter((input) => !input.reusable)
        .map((input) => inputStageInfo(input, context)?.maxStage)
        .filter((stage) => stage !== null && stage !== undefined);
    const maxInputStage = inputStages.length ? Math.max(...inputStages) : null;
    const candidates = [requirement, maxInputStage].filter((stage) => stage !== null && stage !== undefined);
    return candidates.length ? Math.max(...candidates) : null;
}

function applyProgressionStageFlags(stageByItem, stageSourceByItem, stageFlagsByItem) {
    for (const id of [...stageByItem.keys()]) {
        for (const flag of PROGRESSION_STAGE_FLAGS) {
            if (!progressionStageFlagMatches(flag, id)) {
                continue;
            }
            const currentStage = stageByItem.get(id);
            if (currentStage === null || currentStage === undefined) {
                continue;
            }
            const normalizedFloor = Number(flag.stageFloor);
            const applied = currentStage < normalizedFloor;
            if (applied) {
                stageByItem.set(id, normalizedFloor);
                stageSourceByItem.set(id, flag.source);
            }
            const flags = stageFlagsByItem.get(id) ?? [];
            if (!flags.some((entry) => entry.id === flag.id)) {
                flags.push({
                    id: flag.id,
                    label: flag.label,
                    stageFloor: normalizedFloor,
                    originalStage: currentStage,
                    applied,
                    detail: flag.detail
                });
                stageFlagsByItem.set(id, flags);
            }
        }
    }
}

function progressionStageFlagMatches(flag, itemId) {
    return Boolean(flag.ids?.has(itemId) || flag.pattern?.test(itemId));
}

function stageItemIssues(item, category, recipeIds, reachability, materialInputRecipeIds, context) {
    const issues = [];
    if (recipeIds.length === 0 && category !== "material") {
        issues.push({
            code: "NO_RECIPE",
            label: "No way to craft",
            detail: "No item-producing recipe outputs this id."
        });
        return issues;
    }
    const recipeGatedMaterial = category === "material" && alloyMaterialFormHasRecipe(item.id, recipeIds, context);
    if (!reachability.reachable && (category !== "material" || materialFormNeedsRecipe(item.id, context) || recipeGatedMaterial)) {
        const issueCode = recipeIds.length === 0 ? "NO_RECIPE" : "BLOCKED_CHAIN";
        issues.push({
            code: issueCode,
            label: reachabilityIssueLabel(issueCode, reachability),
            detail: reachability.detail,
            suggestion: reachabilityIssueSuggestion(item, reachability, context)
        });
    }
    if (category === "material"
            && recipeIds.length === 0
            && materialInputRecipeIds.length > 0
            && materialFormNeedsRecipe(item.id, context)
            && !issues.some((issue) => issue.code === "NO_RECIPE")) {
        const sample = materialInputRecipeIds.slice(0, 3).join(", ");
        const suffix = materialInputRecipeIds.length > 3 ? `, and ${materialInputRecipeIds.length - 3} more` : "";
        issues.push({
            code: "NO_RECIPE",
            label: "No way to craft",
            detail: `${item.label} is a direct input to ${sample}${suffix}, but no item-producing recipe outputs this material form.`,
            suggestion: materialNoRecipeSuggestion(item, context)
        });
    }

    return issues;
}

function alloyMaterialFormHasRecipe(itemId, recipeIds, context) {
    const formInfo = context.materialFormByItem.get(itemId);
    return Boolean(formInfo && context.alloyMaterials.has(formInfo.material) && recipeIds.length > 0);
}

function reachabilityIssueLabel(issueCode, reachability) {
    switch (issueCode) {
        case "NO_RECIPE":
            return "No way to craft";
        case "BLOCKED_CHAIN":
            if (reachability.code === "SAME_STAGE_GATE") {
                return "Requires same-stage gate";
            }
            if (reachability.code === "HIGH_STAGE_INPUT") {
                return "Requires higher-stage input";
            }
            return "Impossible to craft";
        case "SAME_STAGE_GATE":
            return "Requires same-stage gate";
        case "HIGH_STAGE_INPUT":
            return "Requires higher-stage input";
        case "CALIBRATION_GATE":
            return "Requires calibrated source";
        default:
            return "Impossible to craft";
    }
}

function reachabilityIssueSuggestion(item, reachability, context) {
    if (reachability.code === "SAME_STAGE_GATE") {
        const blocker = reachability.blockerId ? itemDisplayName(reachability.blockerId, context) : "the gated dependency";
        return `Break the stage ${item.stage} bootstrap loop: make ${blocker} craftable before stage ${item.stage}, lower one bootstrap recipe gate below stage ${item.stage}, or replace the same-stage input in ${item.label}'s recipe chain with a lower-stage substitute.`;
    }
    if (reachability.code === "NO_RECIPE") {
        const blocker = reachability.blockerId ? { id: reachability.blockerId, label: itemDisplayName(reachability.blockerId, context) } : item;
        return materialNoRecipeSuggestion(blocker, context);
    }
    if (reachability.code === "HIGH_STAGE_INPUT") {
        return `Replace the higher-stage input or move ${item.label} to the stage where that input becomes available.`;
    }
    if (reachability.code === "CALIBRATION_GATE") {
        return `Add or adjust a calibration recipe that satisfies ${item.label}'s calibrated ingredient requirement.`;
    }
    return `Add a lower-stage recipe path for ${item.label} or adjust the recipe chain so every required input is reachable before stage ${item.stage}.`;
}

function materialNoRecipeSuggestion(item, context) {
    const formInfo = context.materialFormByItem.get(item.id);
    if (formInfo && context.alloyMaterials.has(formInfo.material)) {
        return `Add an explicit recipe path for ${item.label}; alloy material forms are not treated as free base resources by the stage analyzer.`;
    }
    return `Add an item-producing recipe for ${item.label}, or remove it as a direct recipe input.`;
}

function analyzeReachability(itemId, targetStage, context, pathIds = [], calibrationRequirement = null) {
    const calibration = normalizeCalibrationRequirement(calibrationRequirement);
    const memoKey = `${targetStage}|${itemId}|${calibrationRequirementKey(calibration)}`;
    const canUseMemo = !pathIds.includes(itemId);
    if (canUseMemo && context.reachabilityMemo.has(memoKey)) {
        return context.reachabilityMemo.get(memoKey);
    }

    const itemLabelText = itemDisplayName(itemId, context);
    if (!itemId.startsWith("rngtech:")) {
        return reachableResult("external", `${itemLabelText} is outside RNGTech.`);
    }

    const stage = itemStage(itemId, context);
    const materialItem = context.materialItemIds.has(itemId);
    const materialItemRecipeIds = context.recipesByOutput[itemId] ?? [];
    if (materialItem && !materialItemRecipeIds.length && !materialFormNeedsRecipe(itemId, context)) {
        const result = reachableResult("material", `${itemLabelText} is a stage ${stage ?? "unknown"} material form.`);
        if (canUseMemo) {
            context.reachabilityMemo.set(memoKey, result);
        }
        return result;
    }
    if (materialItem && !materialItemRecipeIds.length) {
        const formInfo = context.materialFormByItem.get(itemId);
        const detail = formInfo
            ? `${itemLabelText} is a ${formInfo.form} material form that is directly required by recipes, but no item-producing recipe outputs it.`
            : `${itemLabelText} is a material form that is directly required by recipes, but no item-producing recipe outputs it.`;
        const result = blockedResult("NO_RECIPE", detail, [itemId], itemId);
        if (canUseMemo) {
            context.reachabilityMemo.set(memoKey, result);
        }
        return result;
    }

    if (pathIds.includes(itemId)) {
        return blockedResult("RECIPE_CYCLE", `Recipe chain cycles back to ${itemLabelText}.`, [...pathIds, itemId], itemId);
    }

    const recipes = (context.recipesByOutput[itemId] ?? [])
        .map((recipeId) => context.recipesById.get(recipeId))
        .filter(Boolean)
        .filter((recipe) => recipeSatisfiesCalibrationRequirement(recipe, calibration));
    if (!recipes.length) {
        const code = calibration ? "CALIBRATION_GATE" : "NO_RECIPE";
        const detail = calibration
            ? `${itemLabelText} has no recipe output satisfying ${calibrationRequirementText(calibration)}.`
            : `${itemLabelText} has no item-producing recipe.`;
        const result = blockedResult(code, detail, [itemId], itemId);
        if (canUseMemo) {
            context.reachabilityMemo.set(memoKey, result);
        }
        return result;
    }

    const failures = [];
    for (const recipe of recipes) {
        const recipeResult = analyzeRecipeReachability(recipe, targetStage, context, [...pathIds, itemId]);
        if (recipeResult.reachable) {
            const result = {
                ...reachableResult("recipe", `${itemLabelText} can be crafted through ${recipe.id}.`),
                recipeId: recipe.id
            };
            if (canUseMemo) {
                context.reachabilityMemo.set(memoKey, result);
            }
            return result;
        }
        failures.push({ ...recipeResult, recipeId: recipe.id });
    }

    const failure = chooseReachabilityFailure(failures);
    const result = blockedResult(
        failure.code ?? "BLOCKED_CHAIN",
        `${itemLabelText} is blocked: ${failure.detail}`,
        prependPath(itemId, failure.pathIds),
        failure.blockerId
    );
    result.recipeId = failure.recipeId ?? null;
    if (pathIds.length === 0) {
        context.reachabilityMemo.set(memoKey, result);
    }
    return result;
}

function materialFormNeedsRecipe(itemId, context) {
    const formInfo = context.materialFormByItem.get(itemId);
    if (!formInfo) {
        return false;
    }
    const usedAsDirectInput = (context.directMaterialInputRecipeIds.get(itemId)?.length ?? 0) > 0;
    if (context.alloyMaterials.has(formInfo.material) && usedAsDirectInput) {
        return true;
    }
    if (!MATERIAL_FORMS_REQUIRING_RECIPE.has(formInfo.form)) {
        return false;
    }
    return usedAsDirectInput;
}

function analyzeRecipeReachability(recipe, targetStage, context, pathIds) {
    const gate = componentRequirementStage(recipe.requirements);
    if (!recipe.type.startsWith("rngtech:pseudo_") && gate !== null && gate >= targetStage) {
        if (intentionalMachineProcessGateCandidate(recipe)) {
            const gateResult = analyzeRecipeInputReachability(recipe, gate, context, pathIds);
            if (gateResult.reachable) {
                return reachableResult(
                    "machine_process_gate",
                    `${recipe.id} uses an intentional stage ${gate} machine-process gate; its machine setup and process inputs are reachable by stage ${gate}.`
                );
            }
            return gateResult;
        }
        return blockedResult(
            "SAME_STAGE_GATE",
            `${recipe.id} requires component stage ${gate} before the stage ${targetStage} item is craftable.`,
            pathIds,
            pathIds.at(-1)
        );
    }

    return analyzeRecipeInputReachability(recipe, targetStage, context, pathIds);
}

function analyzeRecipeInputReachability(recipe, targetStage, context, pathIds) {
    const failures = [];
    for (const input of recipe.inputs) {
        const inputResult = analyzeInputReachability(input, targetStage, context, pathIds);
        if (!inputResult.reachable) {
            failures.push(inputResult);
        }
    }
    if (!failures.length) {
        return reachableResult("recipe", `${recipe.id} inputs are reachable.`);
    }
    return chooseReachabilityFailure(failures);
}

function intentionalMachineProcessGateCandidate(recipe) {
    return recipe.result?.kind === "item"
        && recipe.inputs.some((input) => input.reusable && input.role === "machine")
        && recipe.inputs.some((input) => input.reusable && input.role === "gear");
}

function analyzeInputReachability(input, targetStage, context, pathIds) {
    if (input.kind === "item") {
        const result = analyzeReachability(input.id, targetStage, context, pathIds, input.calibration);
        return result.reachable ? result : {
            ...result,
            pathIds: prependPath(input.id, result.pathIds)
        };
    }
    if (input.kind === "fluid" && input.id.startsWith("rngtech:")) {
        const result = analyzeReachability(input.id, targetStage, context, pathIds);
        return result.reachable ? result : {
            ...result,
            pathIds: prependPath(input.id, result.pathIds)
        };
    }
    if (input.kind === "tag") {
        return analyzeTagReachability(input.id, targetStage, context, pathIds);
    }
    return reachableResult(input.kind, `${input.id} is treated as an external ${input.kind} input.`);
}

function analyzeTagReachability(tagId, targetStage, context, pathIds, seen = new Set()) {
    const materialTag = materialTagInfo(tagId);
    if (materialTag) {
        const stage = context.materialStages.get(materialTag.material);
        if (context.alloyMaterials.has(materialTag.material)) {
            const alloyTagResult = analyzeKnownTagReachability(tagId, targetStage, context, pathIds, seen);
            if (!alloyTagResult.reachable || context.tags[tagId]) {
                return alloyTagResult;
            }
        }
        return reachableResult("material_tag", `#${tagId} is a stage ${stage ?? "external"} material tag.`);
    }
    if (seen.has(tagId)) {
        return blockedResult("RECIPE_CYCLE", `Tag #${tagId} references itself.`, pathIds, tagId);
    }

    return analyzeKnownTagReachability(tagId, targetStage, context, pathIds, seen);
}

function analyzeKnownTagReachability(tagId, targetStage, context, pathIds, seen = new Set()) {
    if (seen.has(tagId)) {
        return blockedResult("RECIPE_CYCLE", `Tag #${tagId} references itself.`, pathIds, tagId);
    }

    const tag = context.tags[tagId];
    if (!tag) {
        return reachableResult("external_tag", `#${tagId} has no RNGTech tag file and is treated as external.`);
    }

    const failures = [];
    for (const value of tag.values) {
        const result = value.kind === "item"
            ? analyzeReachability(value.id, targetStage, context, pathIds)
            : analyzeTagReachability(value.id, targetStage, context, pathIds, new Set([...seen, tagId]));
        if (result.reachable) {
            return reachableResult("tag_choice", `#${tagId} has reachable choice ${value.id}.`);
        }
        failures.push(result);
    }

    const failure = chooseReachabilityFailure(failures);
    return blockedResult(
        failure.code ?? "BLOCKED_TAG",
        `#${tagId} has no reachable choice: ${failure.detail}`,
        failure.pathIds ?? pathIds,
        failure.blockerId ?? tagId
    );
}

function chooseReachabilityFailure(failures) {
    if (!failures.length) {
        return blockedResult("BLOCKED_CHAIN", "No reachable recipe path.", [], null);
    }
    const priority = {
        SAME_STAGE_GATE: 1,
        HIGH_STAGE_INPUT: 2,
        CALIBRATION_GATE: 3,
        NO_RECIPE: 4,
        BLOCKED_TAG: 5,
        RECIPE_CYCLE: 6,
        BLOCKED_CHAIN: 7
    };
    return [...failures].sort((left, right) => {
        const priorityDiff = (priority[left.code] ?? 10) - (priority[right.code] ?? 10);
        if (priorityDiff !== 0) {
            return priorityDiff;
        }
        return (left.pathIds?.length ?? 999) - (right.pathIds?.length ?? 999);
    })[0];
}

function reachableResult(source, detail) {
    return {
        reachable: true,
        source,
        detail,
        pathIds: [],
        path: []
    };
}

function blockedResult(code, detail, pathIds, blockerId) {
    return {
        reachable: false,
        code,
        detail,
        pathIds: [...new Set(pathIds.filter(Boolean))],
        path: [],
        blockerId
    };
}

function prependPath(itemId, pathIds = []) {
    return [itemId, ...pathIds.filter((pathId) => pathId !== itemId)];
}

function describeReachability(result, context) {
    if (result.reachable) {
        return result;
    }
    const path = (result.pathIds ?? []).map((id) => ({
        id,
        label: id.includes(":") ? itemDisplayName(id, context) : `#${id}`
    }));
    const chain = path.length > 1 ? ` Chain: ${path.map((entry) => entry.label).join(" -> ")}.` : "";
    return {
        ...result,
        detail: `${result.detail}${chain}`,
        path
    };
}

function inputStageInfo(input, context) {
    if (input.kind === "item") {
        const stage = itemStage(input.id, context);
        const calibration = normalizeCalibrationRequirement(input.calibration);
        const requiredStage = calibration?.minStage ?? null;
        if (stage === null && requiredStage === null) {
            return null;
        }
        const minStage = Math.max(stage ?? 0, requiredStage ?? 0);
        return {
            kind: "item",
            id: input.id,
            label: input.label ?? itemLabel(input.id, {}),
            count: input.count,
            minStage,
            maxStage: minStage,
            category: context.materialItemIds.has(input.id) ? "material" : stageItemCategory(input.id, context.materialItemIds)
        };
    }
    if (input.kind === "tag") {
        const tagStage = tagStageInfo(input.id, context);
        if (!tagStage) {
            return null;
        }
        return {
            kind: "tag",
            id: input.id,
            label: input.label ?? tagLabel(input.id),
            count: input.count,
            minStage: tagStage.minStage,
            maxStage: tagStage.maxStage,
            category: tagStage.material ? "material" : "tag"
        };
    }
    return null;
}

function itemStage(id, context) {
    return context.stageByItem.get(id)
        ?? inferStageFromItemId(id, context.materialStages, context.materialNames)
        ?? null;
}

function itemDisplayName(id, context) {
    if (!id.includes(":")) {
        return `#${id}`;
    }
    return context.itemById?.get(id)?.label ?? itemLabel(id, {});
}

function materialTagInfo(tagId) {
    const match = tagId.match(/^c:(ingots|ores|dusts|nuggets|storage_blocks|plates|gears|rods|coils|casings)\/(.+)$/);
    if (!match) {
        return null;
    }
    return {
        form: match[1],
        material: match[2]
    };
}

function tagStageInfo(tagId, context, seen = new Set()) {
    const materialTag = materialTagInfo(tagId);
    if (materialTag) {
        const stage = context.materialStages.get(materialTag.material);
        return stage === undefined ? null : { minStage: stage, maxStage: stage, material: true };
    }
    const stageTag = tagId.match(/^rngtech:materials\/stage_(\d+)$/);
    if (stageTag) {
        const stage = Number(stageTag[1]);
        return { minStage: stage, maxStage: stage, material: true };
    }
    if (seen.has(tagId)) {
        return null;
    }
    seen.add(tagId);
    const tag = context.tags[tagId];
    if (!tag) {
        return null;
    }
    const childStages = tag.values
        .map((value) => {
            if (value.kind === "item") {
                const stage = itemStage(value.id, context);
                return stage === null ? null : { minStage: stage, maxStage: stage, material: context.materialItemIds.has(value.id) };
            }
            return tagStageInfo(value.id, context, seen);
        })
        .filter(Boolean);
    if (!childStages.length) {
        return null;
    }
    return {
        minStage: Math.min(...childStages.map((entry) => entry.minStage)),
        maxStage: Math.max(...childStages.map((entry) => entry.maxStage)),
        material: childStages.every((entry) => entry.material)
    };
}

function componentRequirementStage(requirements = {}) {
    const stages = [
        requirements.minimumComponentStage,
        requirements.requiredComponentStage,
        requirements.minimumStage,
        requirements.minimumChamberStage
    ].filter((stage) => stage !== null && stage !== undefined);
    return stages.length ? Math.max(...stages) : null;
}

function inferStageFromItemId(id, materialStages, materialNames) {
    const local = itemPath(id);
    if (!local) {
        return null;
    }
    if (local.startsWith("breath_") || local === "entropy_matrix_link" || local === "pseudo/complete_breath_divinity_siphon") {
        return 9;
    }
    if (local.startsWith("heart_") || local === "ascendant_matrix_link" || local === "pseudo/complete_heart_divinity_siphon") {
        return 10;
    }
    if (local.startsWith("exotic_")) {
        return 8;
    }
    for (const material of materialNames) {
        if (local === material
                || local.startsWith(`${material}_`)
                || local.startsWith(`crushed_${material}`)
                || local.endsWith(`_${material}`)) {
            return materialStages.get(material) ?? null;
        }
    }
    return null;
}

function stageItemCategory(id, materialItemIds) {
    const local = itemPath(id);
    if (id.startsWith("rngtech:pseudo/")) {
        return "pseudo";
    }
    if (materialItemIds.has(id)) {
        return "material";
    }
    if (/(?:^|_)(crusher|furnace|melter|press|reactor|generator|calibrator|recycler|solar_panel|solar_array_controller|battery_chassis|solid_fuel_burner|tool_bench|affix_forge|wooden_composter|compressor_tank)$/.test(local)
            || local.endsWith("_chassis")
            || local.endsWith("_compressor_tank")
            || local.endsWith("_component_recycler")
            || local.endsWith("_resonance_calibrator_chassis")
            || local.endsWith("_divinity_siphon_controller")) {
        return "machine";
    }
    if (local.endsWith("_pick_head")
            || local.endsWith("_hammer_head")
            || local.endsWith("_axe_head")
            || local.endsWith("_treefeller_head")
            || local.endsWith("_tool_rod")
            || local.startsWith("modular_")) {
        return "tool";
    }
    if (local.endsWith("_crush_head")
            || local.endsWith("_disassembly_head")
            || local.endsWith("_battery_cell")
            || local.endsWith("_heat_core")
            || local.endsWith("_fuel_box")
            || local.endsWith("_reactor_chamber")
            || local.endsWith("_recovery_filter")
            || local.endsWith("_containment_lining")
            || local.endsWith("_servo")
            || local.endsWith("_fluid_pump")
            || local.endsWith("_cathode")
            || local.endsWith("_alloy_crucible")
            || local.endsWith("_resonance_coil")
            || local.endsWith("_control_board")
            || local.endsWith("_stabilizer_matrix")
            || local.endsWith("_siphon_casing")
            || local.endsWith("_divine_conduit_pylon")
            || local.endsWith("_reliquary_lens_housing")
            || local.endsWith("_containment_seal")
            || local.endsWith("_ritual_heart")
            || local.endsWith("_matrix_link")
            || local.endsWith("_votive")
            || local.endsWith("_component")) {
        return "component";
    }
    return "utility";
}

function modifierCoverageForItem(id, category, profiles) {
    if (!["machine", "component", "tool"].includes(category)) {
        return null;
    }
    const profileId = modifierProfileIdForItem(id);
    if (!profileId) {
        return null;
    }
    const profile = profiles[profileId];
    if (!profile) {
        return null;
    }
    const definitions = profile.definitions.filter((definition) => definition.canRoll);
    return {
        profileId: profile.id,
        profileLabel: profile.label,
        prefixCandidates: definitions.filter((definition) => definition.slot === "PREFIX").length,
        suffixCandidates: definitions.filter((definition) => definition.slot === "SUFFIX").length,
        prefixGroups: modifierGroupCount(definitions, "PREFIX"),
        suffixGroups: modifierGroupCount(definitions, "SUFFIX"),
        behaviorCount: profile.rollableBehaviors.length
    };
}

function modifierGroupCount(definitions, slot) {
    return new Set(definitions
        .filter((definition) => definition.slot === slot)
        .map((definition) => definition.modGroup ?? definition.key)).size;
}

function modifierProfileIdForItem(id) {
    const local = itemPath(id);
    if (id.startsWith("rngtech:pseudo/")) {
        return null;
    }
    if (local === "unique_potato_battery_cell") {
        return "unique_battery_cell";
    }
    if (local.endsWith("_battery_cell")) {
        return "battery_cell";
    }
    if (local.endsWith("_battery_chassis")) {
        return "battery_chassis";
    }
    if (local.endsWith("_crusher_chassis")) {
        return "crusher";
    }
    if (local === "furnace") {
        return "furnace";
    }
    if (local.endsWith("_furnace_chassis")) {
        return "electric_furnace";
    }
    if (local.endsWith("_solid_fuel_burner")) {
        return "solid_fuel_burner";
    }
    if (local === "bio_generator") {
        return "bio_generator";
    }
    if (local === "solar_array_controller") {
        return "solar_array_controller";
    }
    if (local.endsWith("_solar_panel")) {
        return "solar_panel";
    }
    if (local === "potential_reactor") {
        return "potential_reactor";
    }
    if (local === "corrosion_cell") {
        return "corrosion_cell";
    }
    if (local === "vacuum_collapse_generator") {
        return "vacuum_collapse_generator";
    }
    if (local.endsWith("_divinity_siphon_controller")) {
        return "divinity_siphon";
    }
    if (local.endsWith("_component_recycler")) {
        return "component_recycler";
    }
    if (local === "crude_metal_press" || local === "metal_press") {
        return "metal_press";
    }
    if (local === "melter") {
        return "melter";
    }
    if (local === "cavitation_generator") {
        return "cavitation_generator";
    }
    if (local === "ammonia_synthesizer") {
        return "ammonia_synthesizer";
    }
    if (local === "ammonia_fuel_cell") {
        return "ammonia_fuel_cell";
    }
    if (local.endsWith("_compressor_tank")) {
        return "compressor_tank";
    }
    if (local.endsWith("_alloy_furnace_chassis")) {
        return "alloy_furnace";
    }
    if (local.endsWith("_resonance_calibrator_chassis")) {
        return "resonance_calibrator";
    }
    if (local.endsWith("_pick_head")
            || local.endsWith("_hammer_head")
            || local.endsWith("_axe_head")
            || local.endsWith("_treefeller_head")) {
        return "tool_head";
    }
    if (local.endsWith("_tool_rod")) {
        return "tool_rod";
    }
    if (local.startsWith("modular_")) {
        return "modular_tool";
    }
    if (local.endsWith("_crush_head")) {
        return "crush_head";
    }
    if (local.endsWith("_alloy_crucible")) {
        return "alloy_crucible";
    }
    if (local.endsWith("_heat_core")) {
        return "heat_core";
    }
    if (local.endsWith("_servo")) {
        return "servo";
    }
    if (local.endsWith("_fuel_box")) {
        return "fuel_box";
    }
    if (local === "bio_chamber") {
        return "bio_chamber";
    }
    if (local.endsWith("_reactor_chamber")) {
        return "reactor_chamber";
    }
    if (local.endsWith("_recovery_filter")) {
        return "recovery_filter";
    }
    if (local.endsWith("_containment_lining")) {
        return "containment_lining";
    }
    if (local.endsWith("_ammonia_catalyst_bed")) {
        return "ammonia_catalyst_bed";
    }
    if (local.endsWith("_fuel_cell_membrane")) {
        return "fuel_cell_membrane";
    }
    if (local.endsWith("_cavitation_rotor")) {
        return "cavitation_rotor";
    }
    if (local.endsWith("_void_chamber")) {
        return "void_chamber";
    }
    if (local.endsWith("_collapse_nozzle")) {
        return "collapse_nozzle";
    }
    if (local.endsWith("_dimensional_stabilizer")) {
        return "dimensional_stabilizer";
    }
    if (local.endsWith("_disassembly_head")) {
        return "disassembly_head";
    }
    if (local.endsWith("_fluid_pump")) {
        return "fluid_pump";
    }
    if (local.endsWith("_cathode")) {
        return "cathode";
    }
    if (local.endsWith("_solar_array_extender")) {
        return "solar_array_extender";
    }
    if (local.endsWith("_resonance_coil")) {
        return "resonance_coil";
    }
    if (local.endsWith("_control_board")) {
        return "control_board";
    }
    if (local.endsWith("_stabilizer_matrix")) {
        return "stabilizer_matrix";
    }
    return null;
}

function stageFilterCategories() {
    return [
        { id: "core", label: "Machines + Components" },
        { id: "issues", label: "Issues" },
        { id: "machine", label: "Machines" },
        { id: "component", label: "Components" },
        { id: "tool", label: "Tool Parts" },
        { id: "pseudo", label: "Pseudo Targets" },
        { id: "utility", label: "Utilities" },
        { id: "material", label: "Materials" },
        { id: "all", label: "All" }
    ];
}

function stageCategoryLabel(category) {
    return stageFilterCategories().find((entry) => entry.id === category)?.label
        ?? titleCase(category);
}

function stageItemSortKey(item) {
    const issuePrefix = item.issues.length ? "0" : "1";
    const categoryOrder = ["machine", "component", "tool", "pseudo", "utility", "material"].indexOf(item.category);
    return [
        String(item.stage).padStart(2, "0"),
        issuePrefix,
        String(categoryOrder < 0 ? 9 : categoryOrder),
        item.label
    ].join("|");
}

function stageSummary(items) {
    const summary = {};
    for (const stage of STAGE_DEFINITIONS) {
        const stageItems = items.filter((item) => item.stage === stage.stage);
        summary[stage.stage] = {
            total: stageItems.length,
            core: stageItems.filter((item) => CORE_STAGE_CATEGORIES.has(item.category)).length,
            craftable: stageItems.filter((item) => item.craftable).length,
            reachable: stageItems.filter((item) => item.reachable).length,
            impossible: stageItems.filter((item) => item.issues.length > 0).length,
            noRecipe: stageItems.filter((item) => item.issues.some((issue) => issue.code === "NO_RECIPE")).length,
            blockedChains: stageItems.filter((item) => item.issues.some((issue) => issue.code !== "NO_RECIPE")).length
        };
    }
    return summary;
}

function itemPath(id) {
    return id.split(":")[1] ?? id;
}

function javaString(value) {
    return value.trim().replace(/^"|"$/g, "");
}

function javaNumber(value) {
    return Number(value.trim().replaceAll("_", "").replace(/[FLD]$/i, ""));
}

function numericOrNull(value) {
    return value === null || value === undefined ? null : Number(value);
}

async function listJsonFiles(root) {
    const entries = await readdir(root, { withFileTypes: true });
    const files = [];
    for (const entry of entries) {
        const entryPath = path.join(root, entry.name);
        if (entry.isDirectory()) {
            files.push(...await listJsonFiles(entryPath));
        } else if (entry.isFile() && entry.name.endsWith(".json")) {
            files.push(entryPath);
        }
    }
    return files;
}

function normalizeRecipe(json, id, recipePath, lang) {
    const shapeError = recipeShapeError(json);
    if (shapeError) {
        throw new Error(`${relativeToRoot(recipePath)}: ${shapeError}`);
    }
    const result = normalizeResult(json);
    if (!result) {
        return null;
    }
    const inputs = normalizeRecipeInputs(json);
    if (inputs.length === 0) {
        return null;
    }
    const sourceRole = recipeSourceRole(json, id);
    return {
        id,
        type: json.type,
        typeLabel: recipeTypeLabel(json.type),
        path: relativeToRoot(recipePath),
        group: json.group ?? "",
        sourceRole,
        progressionSource: !NON_PROGRESSION_RECIPE_ROLES.has(sourceRole),
        result: {
            ...result,
            label: itemLabel(result.id, lang)
        },
        inputs: aggregateInputs(inputs).map((input) => labelInput(input, lang)),
        requirements: recipeRequirements(json),
        outputStage: result.stage ?? result.calibration?.stage ?? null,
        metadata: recipeMetadata(json)
    };
}

function recipeShapeError(json) {
    if (json.type !== "rngtech:calibrated_shaped") {
        return "";
    }
    for (const [symbol, entry] of Object.entries(json.key ?? {})) {
        if (!entry?.ingredient) {
            return `calibrated_shaped key "${symbol}" must wrap its input in an "ingredient" object.`;
        }
    }
    return "";
}

function normalizeResult(json) {
    if (json.fluid_output) {
        return fluidResult(json.fluid_output);
    }
    if (json.type === "rngtech:vacuum_collapse" && json.residue) {
        return stackResult(json.residue);
    }
    if (!json.result) {
        return null;
    }
    if (json.result.stack) {
        const result = stackResult(json.result.stack);
        const calibration = normalizeCalibrationOutput(json);
        return result
            ? {
                ...result,
                stage: json.result.stage ?? result.stage ?? null,
                ...(calibration ? { calibration } : {})
            }
            : null;
    }
    return stackResult(json.result);
}

function stackResult(stack) {
    const id = stack.id ?? stack.item;
    if (!id) {
        return null;
    }
    return {
        kind: "item",
        id,
        count: stack.count ?? 1,
        stage: stack.stage ?? null
    };
}

function fluidResult(stack) {
    const id = stack.id ?? stack.fluid;
    if (!id) {
        return null;
    }
    return {
        kind: "fluid",
        id,
        count: stack.amount ?? stack.count ?? 1,
        stage: stack.stage ?? null
    };
}

function normalizeRecipeInputs(json) {
    switch (json.type) {
        case "minecraft:crafting_shaped":
        case "rngtech:calibrated_shaped":
        case "rngtech:trait_shaped":
            return shapedInputs(json);
        case "rngtech:tool_damage_shaped":
            return shapedInputs(json, json.tool);
        case "minecraft:crafting_shapeless":
            return (json.ingredients ?? []).flatMap((ingredient) => normalizeIngredient(ingredient, 1));
        case "rngtech:tool_damage_shapeless":
            return shapelessInputs(json, json.tool);
        case "rngtech:metal_press":
            return [
                ...normalizeIngredient(json.ingredient, json.input_count ?? 1),
                ...normalizeIngredient(json.mold, 1, { reusable: true, role: "tool", note: "Mold" })
            ];
        case "rngtech:alloy_furnace":
            return [
                ...alloyFurnaceProcessInputs(json),
                ...(json.ingredients ?? []).flatMap((entry) => normalizeIngredient(entry, 1))
            ];
        case "rngtech:battery_assembly":
            return [
                ...(json.ingredients ?? []).flatMap((entry) => normalizeIngredient(entry, 1)),
                ...normalizeSizedFluidIngredient(json.fluid_input, { role: "fluid", note: "Battery assembly fluid input" })
            ];
        case "rngtech:melter":
            return [
                ...melterProcessInputs(json),
                ...normalizeIngredient(json.primary_ingredient, 1),
                ...normalizeIngredient(json.secondary_ingredient, 1),
                ...normalizeSizedFluidIngredient(json.fluid_input, { role: "fluid", note: "Melter fluid input" })
            ];
        case "rngtech:furnace":
        case "rngtech:crusher":
            return normalizeIngredient(json.ingredient, 1);
        case "rngtech:vacuum_collapse":
            return [
                ...vacuumCollapseProcessInputs(json),
                ...normalizeIngredient(json.ingredient, 1)
            ];
        case "rngtech:calibration":
            return [
                ...normalizeIngredient(json.ingredient, 1),
                ...normalizeIngredient(json.pattern, 1, { reusable: true, role: "tool", note: "Reusable pattern" }),
                ...normalizeIngredient(json.catalyst, 1, { role: "catalyst" }),
                ...normalizeIngredient(json.stabilizer, 1, { role: "stabilizer" })
            ];
        default:
            return [];
    }
}

function shapedInputs(json, reusableTool) {
    const counts = new Map();
    for (const row of json.pattern ?? []) {
        for (const symbol of row) {
            if (symbol !== " ") {
                counts.set(symbol, (counts.get(symbol) ?? 0) + 1);
            }
        }
    }
    const inputs = [];
    for (const [symbol, count] of counts) {
        const entry = json.key?.[symbol];
        if (!entry) {
            continue;
        }
        const reusable = reusableTool && sameIngredient(entry, reusableTool);
        inputs.push(...normalizeIngredient(entry, reusable ? 1 : count, {
            reusable,
            role: reusable ? "tool" : undefined,
            note: reusable ? "Durability tool" : undefined
        }));
    }
    return inputs;
}

function shapelessInputs(json, reusableTool) {
    return (json.ingredients ?? []).flatMap((ingredient) => {
        const reusable = reusableTool && sameIngredient(ingredient, reusableTool);
        return normalizeIngredient(ingredient, 1, {
            reusable,
            role: reusable ? "tool" : undefined,
            note: reusable ? "Durability tool" : undefined
        });
    });
}

function alloyFurnaceProcessInputs(json) {
    const requiredStage = componentRequirementStage(recipeRequirements(json));
    const setup = alloyFurnaceSetupForStage(requiredStage);
    if (!setup) {
        return [];
    }
    return [
        ...normalizeIngredient({ item: setup.chassis }, 1, {
            reusable: true,
            role: "machine",
            note: `Alloy Furnace chassis for stage ${setup.stage}+ process`
        }),
        ...normalizeIngredient({ item: setup.crucible }, 1, {
            reusable: true,
            role: "gear",
            note: `Alloy Crucible for stage ${setup.stage}+ process`
        }),
        ...normalizeIngredient({ item: setup.heatCore }, 1, {
            reusable: true,
            role: "gear",
            note: `Heat Core for stage ${setup.stage}+ process`
        })
    ];
}

function vacuumCollapseProcessInputs(json) {
    const requiredStage = numericOrNull(json.minimum_chamber_stage) ?? 7;
    const setup = vacuumCollapseSetupForStage(requiredStage);
    return [
        ...normalizeIngredient({ item: "rngtech:vacuum_collapse_generator" }, 1, {
            reusable: true,
            role: "machine",
            note: "Vacuum Collapse Generator"
        }),
        ...normalizeIngredient({ item: setup.chamber }, 1, {
            reusable: true,
            role: "gear",
            note: `Void Chamber for stage ${setup.stage}+ process`
        }),
        ...normalizeIngredient({ item: setup.nozzle }, 1, {
            reusable: true,
            role: "gear",
            note: `Collapse Nozzle for stage ${setup.stage}+ process`
        }),
        ...normalizeIngredient({ item: setup.stabilizer }, 1, {
            reusable: true,
            role: "gear",
            note: json.residue_requires_stabilizer
                ? "Dimensional Stabilizer required for residue recovery"
                : `Dimensional Stabilizer for stage ${setup.stage}+ process`
        })
    ];
}

function vacuumCollapseSetupForStage(requiredStage) {
    const setups = [
        {
            stage: 7,
            chamber: "rngtech:tungstensteel_void_chamber",
            nozzle: "rngtech:tungstensteel_collapse_nozzle",
            stabilizer: "rngtech:tungstensteel_dimensional_stabilizer"
        },
        {
            stage: 8,
            chamber: "rngtech:nullite_void_chamber",
            nozzle: "rngtech:nullite_collapse_nozzle",
            stabilizer: "rngtech:nullite_dimensional_stabilizer"
        }
    ];
    return setups.find((setup) => setup.stage >= requiredStage) ?? setups.at(-1);
}

function melterProcessInputs(json) {
    const requiredProcessingLevel = numericOrNull(json.required_processing_level);
    const processNote = requiredProcessingLevel === null
        ? "Melter process"
        : `Processing ${requiredProcessingLevel}+ Melter process`;
    return [
        ...normalizeIngredient({ item: "rngtech:melter" }, 1, {
            reusable: true,
            role: "machine",
            note: "Melter"
        }),
        ...normalizeIngredient({ item: "rngtech:titanium_heat_core" }, 1, {
            reusable: true,
            role: "gear",
            note: `Heat Core for ${processNote}`
        }),
        ...normalizeIngredient({ item: "rngtech:titanium_crush_head" }, 1, {
            reusable: true,
            role: "gear",
            note: `Crush Head for ${processNote}`
        })
    ];
}

function alloyFurnaceSetupForStage(requiredStage) {
    if (requiredStage === null || requiredStage === undefined) {
        return null;
    }
    const setups = [
        {
            stage: 3,
            chassis: "rngtech:bronze_alloy_furnace_chassis",
            crucible: "rngtech:bronze_alloy_crucible",
            heatCore: "rngtech:bronze_heat_core"
        },
        {
            stage: 4,
            chassis: "rngtech:steel_alloy_furnace_chassis",
            crucible: "rngtech:steel_alloy_crucible",
            heatCore: "rngtech:steel_heat_core"
        },
        {
            stage: 6,
            chassis: "rngtech:titanium_alloy_furnace_chassis",
            crucible: "rngtech:titanium_alloy_crucible",
            heatCore: "rngtech:titanium_heat_core"
        }
    ];
    return setups.find((setup) => setup.stage >= requiredStage) ?? setups.at(-1);
}

function normalizeIngredient(entry, count = 1, options = {}) {
    if (!entry) {
        return [];
    }
    if (Array.isArray(entry)) {
        const first = entry[0];
        if (!first) {
            return [];
        }
        return normalizeIngredient(first, count, { ...options, note: appendNote(options.note, "First listed alternative") });
    }
    if (entry.ingredient) {
        const calibration = normalizeCalibrationRequirement(entry.calibration) ?? normalizeCalibrationRequirement(options.calibration);
        return normalizeIngredient(entry.ingredient, count * (entry.count ?? 1), {
            ...options,
            calibration,
            note: appendNote(options.note, calibrationNote(entry.calibration))
        });
    }
    const effectiveCount = count * (entry.count ?? 1);
    const calibration = normalizeCalibrationRequirement(options.calibration);
    if (entry.item) {
        return [{
            kind: "item",
            id: entry.item,
            count: effectiveCount,
            reusable: options.reusable ?? false,
            role: options.role ?? "",
            note: options.note ?? "",
            ...(calibration ? { calibration } : {})
        }];
    }
    if (entry.tag) {
        return [{
            kind: "tag",
            id: entry.tag,
            count: effectiveCount,
            reusable: options.reusable ?? false,
            role: options.role ?? "",
            note: options.note ?? "",
            ...(calibration ? { calibration } : {})
        }];
    }
    if (entry.fluid) {
        return [{
            kind: "fluid",
            id: entry.fluid,
            count: entry.amount ?? effectiveCount,
            reusable: options.reusable ?? false,
            role: options.role ?? "",
            note: options.note ?? "",
            ...(calibration ? { calibration } : {})
        }];
    }
    return [];
}

function normalizeSizedFluidIngredient(entry, options = {}) {
    const ingredient = entry?.ingredient;
    if (!ingredient) {
        return [];
    }
    const amount = entry.amount ?? 1;
    if (ingredient.fluid) {
        return [{
            kind: "fluid",
            id: ingredient.fluid,
            count: amount,
            reusable: options.reusable ?? false,
            role: options.role ?? "",
            note: options.note ?? ""
        }];
    }
    if (ingredient.tag) {
        return [{
            kind: "fluid",
            id: ingredient.tag,
            count: amount,
            reusable: options.reusable ?? false,
            role: options.role ?? "",
            note: options.note ?? ""
        }];
    }
    return normalizeIngredient(ingredient, amount, options);
}

function normalizeCalibrationRequirement(calibration) {
    if (!calibration) {
        return null;
    }
    const minRefinementPotential = Math.max(
        numericOrNull(calibration.min_refinement_potential ?? calibration.minRefinementPotential) ?? 0,
        numericOrNull(calibration.consume_refinement_potential ?? calibration.consumeRefinementPotential) ?? 0
    );
    return {
        family: calibration.family ?? "",
        minStage: numericOrNull(calibration.min_stage ?? calibration.minStage) ?? 0,
        minStability: numericOrNull(calibration.min_stability ?? calibration.minStability) ?? 0,
        maxStability: numericOrNull(calibration.max_stability ?? calibration.maxStability) ?? 100,
        minRefinementPotential,
        consumeRefinementPotential: numericOrNull(calibration.consume_refinement_potential ?? calibration.consumeRefinementPotential) ?? 0
    };
}

function normalizeCalibrationOutput(json) {
    if (json.type !== "rngtech:calibration") {
        return null;
    }
    const result = json.result ?? {};
    return {
        family: json.family ?? "",
        stage: numericOrNull(result.stage) ?? 0,
        stabilityMin: numericOrNull(result.stability?.min) ?? 0,
        stabilityMax: numericOrNull(result.stability?.max) ?? 0,
        refinementPotentialMin: numericOrNull(result.refinement_potential?.min) ?? 0,
        refinementPotentialMax: numericOrNull(result.refinement_potential?.max) ?? 0
    };
}

function calibrationRequirementKey(calibration) {
    if (!calibration) {
        return "";
    }
    return [
        calibration.family,
        calibration.minStage,
        calibration.minStability,
        calibration.maxStability,
        calibration.minRefinementPotential,
        calibration.consumeRefinementPotential
    ].join(":");
}

function calibrationRequirementText(calibration) {
    if (!calibration) {
        return "";
    }
    const parts = [];
    if (calibration.family) {
        parts.push(titleCase(calibration.family));
    }
    if (calibration.minStage) {
        parts.push(`stage ${calibration.minStage}+`);
    }
    if (calibration.maxStability < 100) {
        parts.push(`stability ${calibration.minStability}-${calibration.maxStability}`);
    } else if (calibration.minStability) {
        parts.push(`stability ${calibration.minStability}+`);
    }
    if (calibration.minRefinementPotential) {
        parts.push(`RP ${calibration.minRefinementPotential}+`);
    }
    return parts.length ? parts.join(", ") : "calibrated state";
}

function recipeSatisfiesCalibrationRequirement(recipe, calibration) {
    if (!calibration) {
        return true;
    }
    const output = recipe.result?.calibration;
    if (!output) {
        return false;
    }
    if (calibration.family && output.family !== calibration.family) {
        return false;
    }
    return output.stage >= calibration.minStage
        && output.stabilityMax >= calibration.minStability
        && output.stabilityMin <= calibration.maxStability
        && output.refinementPotentialMax >= calibration.minRefinementPotential;
}

function aggregateInputs(inputs) {
    const result = new Map();
    for (const input of inputs) {
        const key = [input.kind, input.id, input.reusable, input.role, input.note, calibrationRequirementKey(input.calibration)].join("|");
        const existing = result.get(key);
        if (existing) {
            existing.count += input.count;
        } else {
            result.set(key, { ...input });
        }
    }
    return [...result.values()];
}

function labelInput(input, lang) {
    if (input.kind === "tag") {
        return { ...input, label: tagLabel(input.id, lang) };
    }
    if (input.kind === "fluid") {
        return { ...input, label: itemLabel(input.id, lang) };
    }
    return { ...input, label: itemLabel(input.id, lang) };
}

function recipeRequirements(json) {
    return {
        minimumComponentStage: numericOrNull(json.minimum_component_stage),
        requiredComponentStage: numericOrNull(json.required_component_stage),
        minimumStage: numericOrNull(json.minimum_stage),
        minimumChamberStage: numericOrNull(json.minimum_chamber_stage),
        minimumMaterialStage: numericOrNull(json.minimum_material_stage),
        requiredProcessingLevel: numericOrNull(json.required_processing_level),
        minimumTemperature: numericOrNull(json.minimum_temperature)
    };
}

function recipeMetadata(json) {
    const metadata = [];
    if (isMalformedRecoveryRecipe(json)) {
        metadata.push("Failure/recovery route");
    }
    if (json.energy) {
        metadata.push(`Energy ${json.energy}`);
    }
    if (json.processing_ticks) {
        metadata.push(`Ticks ${json.processing_ticks}`);
    }
    if (json.minimum_component_stage) {
        metadata.push(`Stage ${json.minimum_component_stage}+`);
    }
    if (json.required_component_stage) {
        metadata.push(`Stage ${json.required_component_stage}`);
    }
    if (json.minimum_stage) {
        metadata.push(`Calibrator Stage ${json.minimum_stage}+`);
    }
    if (json.minimum_chamber_stage) {
        metadata.push(`Chamber Stage ${json.minimum_chamber_stage}+`);
    }
    if (json.residue_requires_stabilizer) {
        metadata.push("Residue requires stabilizer");
    }
    if (json.minimum_material_stage) {
        metadata.push(`Material Stage ${json.minimum_material_stage}+`);
    }
    if (json.required_processing_level) {
        metadata.push(`Processing ${json.required_processing_level}+`);
    }
    if (json.minimum_temperature) {
        metadata.push(`Temp ${json.minimum_temperature}+`);
    }
    if (json.family) {
        metadata.push(`Family ${titleCase(json.family)}`);
    }
    if (json.type === "rngtech:calibration" && json.result?.stage !== undefined) {
        metadata.push(`Output Stage ${json.result.stage}`);
    }
    if (json.roll_result_traits) {
        metadata.push("Rolls result traits");
    }
    return metadata;
}

function recipeTypeLabel(type) {
    const id = type.includes(":") ? type.split(":")[1] : type;
    return titleCase(id);
}

function sameIngredient(left, right) {
    const leftId = left.ingredient?.item ?? left.item ?? left.ingredient?.tag ?? left.tag;
    const rightId = right.ingredient?.item ?? right.item ?? right.ingredient?.tag ?? right.tag;
    return Boolean(leftId && rightId && leftId === rightId);
}

function appendNote(left, right) {
    return [left, right].filter(Boolean).join("; ");
}

function calibrationNote(calibration) {
    const requirement = normalizeCalibrationRequirement(calibration);
    if (!requirement) {
        return "";
    }
    const text = calibrationRequirementText(requirement);
    return text ? `Calibration ${text}` : "";
}

function recipeSortKey(recipe) {
    return [
        recipe.result.label,
        String(recipePriority(recipe)).padStart(2, "0"),
        String(recipeRequirementPriority(recipe)).padStart(2, "0"),
        String(recipeVariantPriority(recipe)).padStart(2, "0"),
        recipe.id
    ].join("|");
}

function recipeRequirementPriority(recipe) {
    return componentRequirementStage(recipe.requirements) ?? 0;
}

function recipeVariantPriority(recipe) {
    if (recipe.type === "rngtech:alloy_furnace") {
        if (recipe.id.endsWith("_from_dusts")) {
            return 0;
        }
        if (recipe.id.endsWith("_from_ingots")) {
            return 1;
        }
    }
    return 0;
}

function recipePriority(recipe) {
    if (/^rngtech:materials\/[^/]+_(?:casing|plate|gear|rod|coil)$/.test(recipe.id)) {
        return 1;
    }
    switch (recipe.type) {
        case "minecraft:crafting_shaped":
        case "minecraft:crafting_shapeless":
        case "rngtech:calibrated_shaped":
        case "rngtech:tool_damage_shaped":
        case "rngtech:tool_damage_shapeless":
            return 1;
        case "rngtech:metal_press":
            return 2;
        case "rngtech:battery_assembly":
            return 3;
        case "rngtech:alloy_furnace":
            return 4;
        case "rngtech:calibration":
            return 5;
        case "rngtech:furnace":
        case "rngtech:crusher":
        case "rngtech:melter":
        case "rngtech:vacuum_collapse":
            return 6;
        default:
            return 9;
    }
}

function vanillaCraftingConflictRecipes(lang) {
    const iron = { item: "minecraft:iron_ingot" };
    const stick = { item: "minecraft:stick" };
    return [
        vanillaShapedConflictRecipe("minecraft:iron_axe", "minecraft:iron_axe", ["II", "IS", " S"], { I: iron, S: stick }, lang),
        vanillaShapedConflictRecipe("minecraft:iron_hoe", "minecraft:iron_hoe", ["II", " S", " S"], { I: iron, S: stick }, lang),
        vanillaShapedConflictRecipe("minecraft:iron_pickaxe", "minecraft:iron_pickaxe", ["III", " S ", " S "], { I: iron, S: stick }, lang),
        vanillaShapedConflictRecipe("minecraft:iron_shovel", "minecraft:iron_shovel", ["I", "S", "S"], { I: iron, S: stick }, lang),
        vanillaShapedConflictRecipe("minecraft:iron_sword", "minecraft:iron_sword", ["I", "I", "S"], { I: iron, S: stick }, lang)
    ].filter(Boolean);
}

function vanillaShapedConflictRecipe(id, resultId, pattern, key, lang) {
    const json = {
        type: "minecraft:crafting_shaped",
        category: "equipment",
        pattern,
        key,
        result: { id: resultId }
    };
    return normalizeCraftingConflictRecipe(json, {
        id,
        type: json.type,
        typeLabel: recipeTypeLabel(json.type),
        path: `builtin/${id.replace(":", "/")}.json`,
        group: "",
        result: {
            kind: "item",
            id: resultId,
            count: 1,
            label: itemLabel(resultId, lang)
        }
    });
}

function normalizeCraftingConflictRecipe(json, recipe) {
    if (SHAPED_CRAFTING_TYPES.has(json.type)) {
        return normalizeShapedConflictRecipe(json, recipe);
    }
    if (json.type === "minecraft:crafting_shapeless") {
        return normalizeShapelessConflictRecipe(json, recipe);
    }
    return null;
}

function normalizeShapedConflictRecipe(json, recipe) {
    const pattern = TRIMMED_SHAPED_CRAFTING_TYPES.has(json.type)
        ? trimCraftingPattern(json.pattern ?? [])
        : rectangularPattern(json.pattern ?? []);
    if (pattern.length === 0) {
        return null;
    }
    const width = pattern[0].length;
    const height = pattern.length;
    if (width > CRAFTING_GRID_WIDTH || height > CRAFTING_GRID_HEIGHT) {
        return null;
    }
    const grids = shapedConflictGrids(pattern, json.key ?? {});
    if (grids.length === 0) {
        return null;
    }
    return {
        ...craftingConflictRecipeBase(recipe),
        kind: "shaped",
        grids
    };
}

function normalizeShapelessConflictRecipe(json, recipe) {
    const ingredients = [];
    for (const entry of json.ingredients ?? []) {
        const ingredient = craftingConflictIngredient(entry);
        if (!ingredient) {
            return null;
        }
        ingredients.push(ingredient);
    }
    if (ingredients.length === 0 || ingredients.length > CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT) {
        return null;
    }
    return {
        ...craftingConflictRecipeBase(recipe),
        kind: "shapeless",
        ingredients
    };
}

function craftingConflictRecipeBase(recipe) {
    return {
        id: recipe.id,
        type: recipe.type,
        typeLabel: recipe.typeLabel,
        path: recipe.path,
        result: recipe.result
    };
}

function rectangularPattern(pattern) {
    const rows = pattern.map((row) => String(row ?? ""));
    const width = rows.reduce((max, row) => Math.max(max, row.length), 0);
    return width === 0 ? [] : rows.map((row) => row.padEnd(width, " "));
}

function trimCraftingPattern(pattern) {
    const rows = rectangularPattern(pattern);
    let top = -1;
    let bottom = -1;
    let left = Number.POSITIVE_INFINITY;
    let right = -1;
    for (let y = 0; y < rows.length; y++) {
        for (let x = 0; x < rows[y].length; x++) {
            if (rows[y][x] === " ") {
                continue;
            }
            if (top < 0) {
                top = y;
            }
            bottom = y;
            left = Math.min(left, x);
            right = Math.max(right, x);
        }
    }
    if (top < 0) {
        return [];
    }
    return rows.slice(top, bottom + 1).map((row) => row.slice(left, right + 1));
}

function shapedConflictGrids(pattern, key) {
    const width = pattern[0].length;
    const height = pattern.length;
    const symbolIngredients = new Map();
    for (const row of pattern) {
        for (const symbol of row) {
            if (symbol === " " || symbolIngredients.has(symbol)) {
                continue;
            }
            const ingredient = craftingConflictIngredient(key[symbol]);
            if (!ingredient) {
                return [];
            }
            symbolIngredients.set(symbol, ingredient);
        }
    }

    const grids = [];
    const seen = new Set();
    for (let yOffset = 0; yOffset <= CRAFTING_GRID_HEIGHT - height; yOffset++) {
        for (let xOffset = 0; xOffset <= CRAFTING_GRID_WIDTH - width; xOffset++) {
            for (const mirrored of [false, true]) {
                const grid = Array(CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT).fill(null);
                for (let y = 0; y < height; y++) {
                    for (let x = 0; x < width; x++) {
                        const symbolX = mirrored ? width - x - 1 : x;
                        const symbol = pattern[y][symbolX];
                        if (symbol === " ") {
                            continue;
                        }
                        grid[x + xOffset + (y + yOffset) * CRAFTING_GRID_WIDTH] = symbolIngredients.get(symbol);
                    }
                }
                const key = conflictGridKey(grid);
                if (!seen.has(key)) {
                    seen.add(key);
                    grids.push(grid);
                }
            }
        }
    }
    return grids;
}

function conflictGridKey(grid) {
    return grid.map((ingredient) => ingredient ? ingredientKey(ingredient) : ".").join("/");
}

function craftingConflictIngredient(entry, inheritedCalibration = null) {
    const alternatives = craftingIngredientAlternatives(entry, inheritedCalibration);
    return alternatives.length ? { alternatives } : null;
}

function craftingIngredientAlternatives(entry, inheritedCalibration = null) {
    if (!entry) {
        return [];
    }
    if (Array.isArray(entry)) {
        return entry.flatMap((alternative) => craftingIngredientAlternatives(alternative, inheritedCalibration));
    }
    const calibration = normalizeCalibration(entry.calibration ?? inheritedCalibration);
    if (entry.ingredient) {
        return craftingIngredientAlternatives(entry.ingredient, calibration);
    }
    if (entry.item) {
        return [{ kind: "item", id: entry.item, calibration }];
    }
    if (entry.tag) {
        return [{ kind: "tag", id: entry.tag, calibration }];
    }
    return [];
}

function normalizeCalibration(calibration) {
    if (!calibration) {
        return null;
    }
    const consumeRefinementPotential = Number(calibration.consume_refinement_potential ?? 0);
    return {
        family: calibration.family ?? "",
        minStage: Number(calibration.min_stage ?? 0),
        minStability: Number(calibration.min_stability ?? 0),
        minRefinementPotential: Math.max(Number(calibration.min_refinement_potential ?? 0), consumeRefinementPotential)
    };
}

function findRecipeConflicts(craftingRecipes, tags) {
    const conflicts = [];
    for (let leftIndex = 0; leftIndex < craftingRecipes.length; leftIndex++) {
        for (let rightIndex = leftIndex + 1; rightIndex < craftingRecipes.length; rightIndex++) {
            const left = craftingRecipes[leftIndex];
            const right = craftingRecipes[rightIndex];
            if (sameCraftingResult(left.result, right.result) || !craftingRecipesOverlap(left, right, tags)) {
                continue;
            }
            conflicts.push({
                code: "OVERLAPPING_RECIPE",
                label: "Overlapping recipe",
                detail: `${left.id} and ${right.id} can match the same crafting input.`,
                recipes: [recipeConflictSummary(left), recipeConflictSummary(right)]
            });
        }
    }
    return conflicts.sort((left, right) => left.detail.localeCompare(right.detail));
}

function sameCraftingResult(left, right) {
    return left.id === right.id && left.count === right.count;
}

function recipeConflictSummary(recipe) {
    return {
        id: recipe.id,
        type: recipe.type,
        result: recipe.result,
        path: recipe.path
    };
}

function craftingRecipesOverlap(left, right, tags) {
    if (left.kind === "shaped" && right.kind === "shaped") {
        return left.grids.some((leftGrid) => right.grids.some((rightGrid) => craftingGridsOverlap(leftGrid, rightGrid, tags)));
    }
    if (left.kind === "shaped" && right.kind === "shapeless") {
        return shapedAndShapelessOverlap(left, right, tags);
    }
    if (left.kind === "shapeless" && right.kind === "shaped") {
        return shapedAndShapelessOverlap(right, left, tags);
    }
    return craftingIngredientMultisetsOverlap(left.ingredients, right.ingredients, tags);
}

function craftingGridsOverlap(leftGrid, rightGrid, tags) {
    for (let index = 0; index < leftGrid.length; index++) {
        if (Boolean(leftGrid[index]) !== Boolean(rightGrid[index])) {
            return false;
        }
        if (leftGrid[index] && !craftingIngredientsOverlap(leftGrid[index], rightGrid[index], tags)) {
            return false;
        }
    }
    return true;
}

function shapedAndShapelessOverlap(shaped, shapeless, tags) {
    return shaped.grids.some((grid) => {
        const ingredients = grid.filter(Boolean);
        return craftingIngredientMultisetsOverlap(ingredients, shapeless.ingredients, tags);
    });
}

function craftingIngredientMultisetsOverlap(left, right, tags) {
    if (left.length !== right.length) {
        return false;
    }
    const candidates = left
        .map((ingredient, index) => ({
            index,
            matches: right
                .map((other, otherIndex) => craftingIngredientsOverlap(ingredient, other, tags) ? otherIndex : -1)
                .filter((otherIndex) => otherIndex >= 0)
        }))
        .sort((a, b) => a.matches.length - b.matches.length);
    if (candidates.some((candidate) => candidate.matches.length === 0)) {
        return false;
    }
    const used = new Set();
    const search = (index) => {
        if (index >= candidates.length) {
            return true;
        }
        for (const candidate of candidates[index].matches) {
            if (used.has(candidate)) {
                continue;
            }
            used.add(candidate);
            if (search(index + 1)) {
                return true;
            }
            used.delete(candidate);
        }
        return false;
    };
    return search(0);
}

function craftingIngredientsOverlap(left, right, tags) {
    return left.alternatives.some((leftAlternative) =>
        right.alternatives.some((rightAlternative) => ingredientAlternativesOverlap(leftAlternative, rightAlternative, tags)));
}

function ingredientAlternativesOverlap(left, right, tags) {
    return calibrationsOverlap(left.calibration, right.calibration) && ingredientTermsOverlap(left, right, tags);
}

function calibrationsOverlap(left, right) {
    if (!left || !right) {
        return true;
    }
    return left.family === right.family;
}

function ingredientTermsOverlap(left, right, tags) {
    if (left.kind === "item" && right.kind === "item") {
        return left.id === right.id;
    }
    if (left.kind === "item" && right.kind === "tag") {
        return tagContainsItem(right.id, left.id, tags);
    }
    if (left.kind === "tag" && right.kind === "item") {
        return tagContainsItem(left.id, right.id, tags);
    }
    return tagsOverlap(left.id, right.id, tags);
}

function tagContainsItem(tagId, itemId, tags, seen = new Set()) {
    if (seen.has(tagId)) {
        return false;
    }
    seen.add(tagId);
    for (const value of tags[tagId]?.values ?? []) {
        if (value.kind === "item" && value.id === itemId) {
            return true;
        }
        if (value.kind === "tag" && tagContainsItem(value.id, itemId, tags, seen)) {
            return true;
        }
    }
    return false;
}

function tagsOverlap(leftTagId, rightTagId, tags) {
    if (leftTagId === rightTagId) {
        return true;
    }
    const rightItems = tagItems(rightTagId, tags);
    for (const item of tagItems(leftTagId, tags)) {
        if (rightItems.has(item)) {
            return true;
        }
    }
    return false;
}

function tagItems(tagId, tags, seen = new Set()) {
    if (seen.has(tagId)) {
        return new Set();
    }
    seen.add(tagId);
    const items = new Set();
    for (const value of tags[tagId]?.values ?? []) {
        if (value.kind === "item") {
            items.add(value.id);
        } else if (value.kind === "tag") {
            for (const item of tagItems(value.id, tags, seen)) {
                items.add(item);
            }
        }
    }
    return items;
}

function ingredientKey(ingredient) {
    return ingredient.alternatives.map((alternative) => {
        const calibration = alternative.calibration
            ? `@${alternative.calibration.family}:${alternative.calibration.minStage}:${alternative.calibration.minStability}:${alternative.calibration.minRefinementPotential}`
            : "";
        return `${alternative.kind}:${alternative.id}${calibration}`;
    }).sort().join("|");
}

function isItemTagPath(filePath) {
    const normalized = relativeToRoot(filePath);
    return /src\/main\/resources\/data\/[^/]+\/tags\/item\/.+\.json$/.test(normalized);
}

function itemTagId(filePath) {
    const relative = path.relative(DATA_ROOT, filePath).replaceAll("\\", "/");
    const [namespace, , , ...segments] = relative.split("/");
    return `${namespace}:${segments.join("/").replace(/\.json$/, "")}`;
}

function recipeId(filePath) {
    return `rngtech:${path.relative(RECIPE_ROOT, filePath).replaceAll("\\", "/").replace(/\.json$/, "")}`;
}

function tagValues(values) {
    return values.map((value) => {
        if (typeof value === "string") {
            return value.startsWith("#")
                ? { kind: "tag", id: value.slice(1), required: true }
                : { kind: "item", id: value, required: true };
        }
        const id = value.id ?? "";
        return {
            kind: id.startsWith("#") ? "tag" : "item",
            id: id.startsWith("#") ? id.slice(1) : id,
            required: value.required ?? true
        };
    }).filter((value) => value.id);
}

function itemLabel(id, lang) {
    const [namespace, itemPath] = id.split(":");
    return lang[`item.${namespace}.${itemPath}`]
        ?? lang[`block.${namespace}.${itemPath}`]
        ?? lang[`fluid.${namespace}.${itemPath}`]
        ?? titleCase(itemPath ?? id);
}

function tagLabel(id) {
    const [, tagPath = id] = id.split(":");
    const segments = tagPath.split("/");
    if (segments.length >= 2) {
        return `${titleCase(segments.at(-1))} ${titleCase(segments.slice(0, -1).join(" "))}`;
    }
    return titleCase(tagPath);
}

function relativeToRoot(filePath) {
    return path.relative(ROOT, filePath).replaceAll("\\", "/");
}

function parseRangeLists(source) {
    const ranges = new Map();
    const regex = /private static final List<ModifierValueRange>\s+(\w+)\s*=\s*List\.of\(([\s\S]*?)\);/g;
    for (const match of source.matchAll(regex)) {
        ranges.set(match[1], parseModifierValueRanges(match[2]));
    }
    return ranges;
}

function parseModifierValueRanges(expression) {
    const entries = splitTopLevel(expression)
        .map((entry) => entry.trim())
        .filter(Boolean);
    return entries.map((entry) => {
        let match = entry.match(/^new ModifierValueRange\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)(?:,\s*(true|false))?\)$/);
        if (match) {
            return {
                min: Number(match[1]),
                max: Number(match[2]),
                wholeNumber: match[3] === "true"
            };
        }
        match = entry.match(/^ModifierValueRange\.fixed\((-?\d+(?:\.\d+)?)\)$/);
        if (match) {
            const value = Number(match[1]);
            return {
                min: value,
                max: value,
                wholeNumber: false
            };
        }
        throw new Error(`Unsupported ModifierValueRange expression: ${entry}`);
    });
}

function parseStringConstants(source) {
    const constants = new Map();
    const stringRegex = /(?:public|private) static final String\s+(\w+)\s*=\s*"([^"]*)";/g;
    for (const match of source.matchAll(stringRegex)) {
        constants.set(match[1], match[2]);
    }
    const intRegex = /(?:public|private) static final int\s+(\w+)\s*=\s*([0-9_]+);/g;
    for (const match of source.matchAll(intRegex)) {
        constants.set(match[1], javaNumber(match[2]));
    }
    return constants;
}

function parseBehaviorLists(source) {
    const lists = new Map();
    const regex = /private static final List<MachineBehavior>\s+(\w+)\s*=\s*List\.of\(([\s\S]*?)\);/g;
    for (const match of source.matchAll(regex)) {
        lists.set(match[1], enumRefs(match[2], "MachineBehavior"));
    }
    return lists;
}

function parseDefinitions(source, ranges, constants) {
    const definitions = new Map();
    const regex = /private static final ModifierDefinition\s+(\w+)\s*=/g;
    let match;
    while ((match = regex.exec(source)) !== null) {
        const name = match[1];
        const expression = readAssignmentExpression(source, regex.lastIndex);
        const definition = parseDefinitionExpression(expression, ranges, constants);
        if (definition) {
            definitions.set(name, { id: name, ...definition });
        }
    }
    return definitions;
}

function parseDefinitionExpression(expression, ranges, constants) {
    const withChaining = (definition) => applyDefinitionChaining(definition, expression, constants);
    if (expression.startsWith("runtimeUntiered")) {
        const args = splitTopLevel(callArgs(expression, "runtimeUntiered"));
        const affixId = stringValue(args[0], constants);
        const modGroup = stringValue(args[1], constants);
        const slot = enumRef(args[2], "ModifierSlot");
        const stat = enumRef(args[3], "MachineStat");
        const operation = enumRef(args[4], "ModifierOperation");
        const value = numericValue(args[5], constants);
        const rollWeight = numericValue(args[6], constants);
        return withChaining(withDefaultEffects({
            affixId,
            modGroup,
            slot,
            stat,
            operation,
            tierRanges: [{ min: value, max: value, wholeNumber: true }],
            canRoll: true,
            untiered: true,
            targetable: false,
            rollWeight
        }));
    }
    if (expression.startsWith("runtime")) {
        const args = splitTopLevel(callArgs(expression, "runtime"));
        const affixId = stringValue(args[0], constants);
        const modGroup = stringValue(args[1], constants);
        const slot = enumRef(args[2], "ModifierSlot");
        const stat = enumRef(args[3], "MachineStat");
        const operation = enumRef(args[4], "ModifierOperation");
        const rangeName = args[5].trim();
        const rollWeight = numericValue(args[6], constants);
        return withChaining(withDefaultEffects({
            affixId,
            modGroup,
            slot,
            stat,
            operation,
            tierRanges: resolveRangeList(rangeName, ranges),
            canRoll: true,
            targetable: false,
            rollWeight
        }));
    }
    if (expression.startsWith("behaviorOnly")) {
        const args = splitTopLevel(callArgs(expression, "behaviorOnly"));
        const affixId = stringValue(args[0], constants);
        const slot = enumRef(args[1], "ModifierSlot");
        const stat = enumRef(args[2], "MachineStat");
        const rollWeight = numericValue(args[3], constants);
        return withChaining(withDefaultEffects({
            affixId,
            modGroup: affixId,
            slot,
            stat,
            operation: "ADD",
            tierRanges: [{ min: 0, max: 0, wholeNumber: true }],
            canRoll: true,
            untiered: true,
            behaviorOnly: true,
            targetable: false,
            rollWeight
        }));
    }
    if (expression.startsWith("ModifierDefinition.rollableUntiered")) {
        const args = splitTopLevel(callArgs(expression, "ModifierDefinition.rollableUntiered"));
        const affixId = stringValue(args[0], constants);
        const modGroup = stringValue(args[1], constants);
        const slot = enumRef(args[2], "ModifierSlot");
        const stat = enumRef(args[3], "MachineStat");
        const operation = enumRef(args[4], "ModifierOperation");
        const tierRanges = parseModifierValueRanges(args[5].trim());
        return withChaining(withDefaultEffects({
            affixId,
            modGroup,
            slot,
            stat,
            operation,
            tierRanges,
            canRoll: true,
            untiered: true
        }));
    }
    if (expression.startsWith("ModifierDefinition.rollable")) {
        const args = splitTopLevel(callArgs(expression, "ModifierDefinition.rollable"));
        if (!isEnumRef(args[0], "ModifierSlot")) {
            const affixId = stringValue(args[0], constants);
            const modGroup = stringValue(args[1], constants);
            const slot = enumRef(args[2], "ModifierSlot");
            if (args[3].trim().startsWith("List.of")) {
                const effects = parseEffectDefinitions(args[3], ranges);
                return withChaining(withDefaultEffects({
                    affixId,
                    modGroup,
                    slot,
                    stat: effects[0].stat,
                    operation: effects[0].operation,
                    tierRanges: effects[0].tierRanges,
                    effects,
                    canRoll: true
                }));
            }
            const stat = enumRef(args[3], "MachineStat");
            const operation = enumRef(args[4], "ModifierOperation");
            const rangeName = args[5].trim();
            return withChaining(withDefaultEffects({
                affixId,
                modGroup,
                slot,
                stat,
                operation,
                tierRanges: resolveRangeList(rangeName, ranges),
                canRoll: true
            }));
        }
        const slot = enumRef(args[0], "ModifierSlot");
        const stat = enumRef(args[1], "MachineStat");
        const operation = enumRef(args[2], "ModifierOperation");
        const rangeName = args[3].trim();
        return withChaining(withDefaultEffects({
            affixId: stat.toLowerCase(),
            modGroup: defaultModGroup(stat, operation),
            slot,
            stat,
            operation,
            tierRanges: resolveRangeList(rangeName, ranges),
            canRoll: true
        }));
    }
    if (expression.startsWith("ModifierDefinition.fixed")) {
        const args = splitTopLevel(callArgs(expression, "ModifierDefinition.fixed"));
        const slot = enumRef(args[0], "ModifierSlot");
        const stat = enumRef(args[1], "MachineStat");
        const operation = enumRef(args[2], "ModifierOperation");
        return withChaining({
            affixId: stat.toLowerCase(),
            modGroup: defaultModGroup(stat, operation),
            slot,
            stat,
            operation,
            tierRanges: [],
            canRoll: false
        });
    }
    if (expression.startsWith("percent")) {
        const args = splitTopLevel(callArgs(expression, "percent"));
        const stat = enumRef(args[1], "MachineStat");
        const operation = DECREASED_PERCENT_STATS.has(stat) ? "DECREASED_PERCENT" : "INCREASED_PERCENT";
        return withChaining(withDefaultEffects({
            affixId: stat.toLowerCase(),
            modGroup: defaultModGroup(stat, operation),
            slot: enumRef(args[0], "ModifierSlot"),
            stat,
            operation,
            tierRanges: mustGet(ranges, "PERCENT_TIER_RANGES", "percent tier ranges"),
            canRoll: true
        }));
    }
    if (expression.startsWith("reducedPercent")) {
        const args = splitTopLevel(callArgs(expression, "reducedPercent"));
        const stat = enumRef(args[1], "MachineStat");
        return withChaining(withDefaultEffects({
            affixId: stat.toLowerCase(),
            modGroup: defaultModGroup(stat, "DECREASED_PERCENT"),
            slot: enumRef(args[0], "ModifierSlot"),
            stat,
            operation: "DECREASED_PERCENT",
            tierRanges: mustGet(ranges, "PERCENT_TIER_RANGES", "percent tier ranges"),
            canRoll: true
        }));
    }
    return null;
}

function applyDefinitionChaining(definition, expression, constants) {
    const rollWeightMatch = expression.match(/\.withRollWeight\(([^)]+)\)/);
    const explicitLensTags = parseLensTags(expression);
    const behaviorOnly = definition.behaviorOnly || expression.includes(".asBehaviorOnly()");
    const explicitlyNonTargetable =
        behaviorOnly || expression.includes(".withoutTargetedRefinement()") || definition.targetable === false;
    const targetable = definition.canRoll === true && !explicitlyNonTargetable;
    const flags = [];
    if (definition.canRoll) {
        flags.push("CAN_ROLL");
    }
    if (definition.untiered) {
        flags.push("UNTIERED");
    }
    if (explicitlyNonTargetable) {
        flags.push("NON_TARGETABLE");
    }
    if (behaviorOnly) {
        flags.push("BEHAVIOR_ONLY");
    }
    return {
        ...definition,
        behaviorOnly,
        targetable,
        rollWeight: rollWeightMatch ? numericValue(rollWeightMatch[1], constants) : (definition.rollWeight ?? 100),
        lensTags: explicitLensTags ?? inferLensTags(definition),
        flags
    };
}

function parseLensTags(expression) {
    const match = expression.match(/\.withLensTags\(Set\.of\(([^)]*)\)\)/);
    if (!match) {
        return null;
    }
    const args = splitTopLevel(match[1]).map((arg) => arg.trim()).filter(Boolean);
    return args.map((arg) => enumRef(arg, "ModifierLensTag"));
}

function inferLensTags(definition) {
    const stats = definition.effects?.length
        ? definition.effects.map((effect) => effect.stat)
        : [definition.stat];
    const tags = new Set();
    for (const stat of stats) {
        addLensTags(tags, stat);
    }
    return [...tags];
}

function addLensTags(tags, stat) {
    if ([
        "ENERGY_CAPACITY", "ENERGY_GENERATION", "ENERGY_TRANSFER", "FE_TRANSFER", "BATTERY_SUPPORT",
        "BURST_TRANSFER", "BURST_DURATION", "POTATO_POWER", "CARROT_POWER", "BREAD_POWER", "SAPLING_POWER",
        "SEED_POWER", "PLANT_POWER", "ORGANIC_REAGENT_POWER", "COMPOSTED_BIOMASS_POWER", "ALGAE_POWER",
        "RICH_BIOMASS_POWER", "PEAK_SOLAR_GENERATION", "MAX_TEMPERATURE", "HEAT_TRANSFER", "FUEL_DURATION"
    ].includes(stat)) {
        tags.add("POWER");
    }
    if ([
        "PROCESSING_SPEED", "INSTANT_PROCESS_CHANCE", "MINING_SPEED", "ATTACK_SPEED", "ORE_BURST_SPEED",
        "WARMUP_TIME", "COOLING_RATE", "PROCESSING_LEVEL", "MINING_LEVEL", "BATCH_SIZE"
    ].includes(stat)) {
        tags.add("SPEED");
    }
    if ([
        "OUTPUT_AMOUNT", "SUPER_OUTPUT_CHANCE", "MAGIC_FIND", "LUCK", "REFINEMENT_POTENTIAL",
        "REFINEMENT_POTENTIAL_BONUS", "CRUSHER_SALVAGE_CHANCE", "GLOBAL_MODIFIER_STRENGTH"
    ].includes(stat)) {
        tags.add("YIELD");
    }
    if ([
        "STABILITY", "TEMPERATURE_STABILITY", "OVERHEAT_TOLERANCE", "DURABILITY", "SELF_REPAIR",
        "HEAT_ISOLATION", "IDLE_LOSS", "HIGH_HARDNESS_ENERGY_MITIGATION"
    ].includes(stat)) {
        tags.add("STABILITY");
    }
    if ([
        "INPUT_SLOTS", "OUTPUT_SLOTS", "ADDON_SLOTS", "UPGRADE_LIMIT", "BUFFER_SIZE", "FLUID_CAPACITY",
        "COMPRESSION_RATIO", "FLUID_TRANSFER", "CALIBRATION_QUALITY", "CALIBRATION_PRECISION",
        "CATALYST_EFFICIENCY", "AREA_WIDTH", "AREA_HEIGHT", "TREE_FELL_LIMIT", "VEIN_MINE_LIMIT",
        "BLOCK_FILTER_SLOTS", "CONTROL", "BATTERY_SLOTS", "SOLAR_PANEL_LIMIT", "MOONLIGHT_CONVERSION",
        "WEATHER_RECOVERY", "SOLAR_PANEL_SYNCHRONIZATION", "SOLAR_PANEL_ARBITRATION", "OVERFLOW_SHUNTING",
        "CLEAR_SKY_AMPLIFICATION", "LUNAR_INVERSION", "OUTPUT_GUARD_GRACE", "NO_BATTERY_OUTPUT_RETENTION",
        "CRUSHER_INPUT_FILTER", "ENERGY_USAGE", "FE_USAGE", "VEIN_MINE_FE_USAGE", "ORE_BURST_FE_USAGE",
        "FUEL_EFFICIENCY", "EFFICIENCY"
    ].includes(stat)) {
        tags.add("CONTROL");
    }
    if ([
        "PROCESSING_SPEED", "INSTANT_PROCESS_CHANCE", "BATCH_SIZE",
        "MINING_SPEED", "ATTACK_SPEED", "ORE_BURST_SPEED"
    ].includes(stat)) {
        tags.add("KINETIC");
    }
    if ([
        "EFFICIENCY", "ENERGY_USAGE", "FUEL_EFFICIENCY", "IDLE_LOSS", "HIGH_HARDNESS_ENERGY_MITIGATION",
        "NO_BATTERY_OUTPUT_RETENTION", "DURABILITY", "SELF_REPAIR", "FE_USAGE", "VEIN_MINE_FE_USAGE",
        "ORE_BURST_FE_USAGE", "CONTROL"
    ].includes(stat)) {
        tags.add("EFFICIENCY");
    }
}

function parseEffectDefinitions(expression, ranges) {
    return splitTopLevel(callArgs(expression.trim(), "List.of")).map((effectExpression) => {
        const args = splitTopLevel(callArgs(effectExpression.trim(), "ModifierEffectDefinition.of"));
        const stat = enumRef(args[0], "MachineStat");
        const operation = enumRef(args[1], "ModifierOperation");
        const rangeName = args[2].trim();
        return {
            stat,
            operation,
            tierRanges: resolveRangeList(rangeName, ranges)
        };
    });
}

function resolveRangeList(expression, ranges) {
    const trimmed = expression.trim();
    if (trimmed.startsWith("List.of")) {
        return parseModifierValueRanges(callArgs(trimmed, "List.of"));
    }
    return mustGet(ranges, trimmed, `range list ${trimmed}`);
}

function withDefaultEffects(definition) {
    if (definition.effects?.length > 1) {
        return definition;
    }
    const { effects, ...singleEffectDefinition } = definition;
    return singleEffectDefinition;
}

function defaultModGroup(stat, operation) {
    return `${operation.toLowerCase()}:${stat.toLowerCase()}`;
}

function stringValue(text, constants) {
    const trimmed = text.trim();
    if (isQuoted(trimmed)) {
        return unquote(trimmed);
    }
    if (constants.has(trimmed)) {
        const constant = constants.get(trimmed);
        if (typeof constant === "string") {
            return constant;
        }
    }
    const serializedName = trimmed.match(/^\w+\.([A-Z_]+)\.getSerializedName\(\)$/);
    if (serializedName) {
        return serializedName[1].toLowerCase();
    }
    throw new Error(`Could not parse string value from: ${text}`);
}

function numericValue(text, constants) {
    const trimmed = text.trim();
    if (constants.has(trimmed)) {
        const constant = constants.get(trimmed);
        if (typeof constant === "number") {
            return constant;
        }
    }
    const parsed = javaNumber(trimmed);
    if (!Number.isNaN(parsed)) {
        return parsed;
    }
    throw new Error(`Could not parse numeric value from: ${text}`);
}

function parseProfiles(source, definitions, behaviorLists, ranges, constants, lang) {
    const profiles = {};
    const regex = /public static final ModifierEligibilityProfile\s+(\w+)\s*=/g;
    let match;
    while ((match = regex.exec(source)) !== null) {
        const constantName = match[1];
        const expression = readAssignmentExpression(source, regex.lastIndex);
        const args = splitTopLevel(callArgs(expression, "profile"));
        const id = unquote(args[0]);
        const capabilities = enumRefs(args[1], "ModifierCapability");
        const resolvedDefinitions = resolveProfileDefinitions(args[2], definitions, ranges, constants);
        const rollableBehaviors = args[3] ? resolveBehaviorList(args[3], behaviorLists) : [];
        profiles[id] = {
            id,
            constantName,
            label: titleCase(id),
            capabilities,
            definitions: resolvedDefinitions.map((definition) => labelDefinition(definition, lang)),
            rollableBehaviors: rollableBehaviors.map((behavior) => ({
                behavior,
                id: behavior.toLowerCase(),
                modGroup: behavior === "BULK_SPEED" ? (constants.get("PROCESSING_SPEED_GROUP") ?? "processing_speed") : behavior.toLowerCase(),
                label: lang[`rngtech.behavior.${behavior.toLowerCase()}`] ?? titleCase(behavior)
            }))
        };
    }
    return profiles;
}

function resolveProfileDefinitions(expression, definitions, ranges, constants) {
    const trimmed = expression.trim();
    if (trimmed.startsWith("List.of")) {
        return listSymbols(trimmed).map((definitionId) => mustGet(definitions, definitionId, `definition ${definitionId}`));
    }
    if (trimmed.startsWith("affixes")) {
        const args = splitTopLevel(callArgs(trimmed, "affixes"));
        const { extras, stats } = profileDefinitionArgs(args, definitions);
        return uniqueDefinitions([...extras, ...stats.flatMap((stat) => definitionsForStatArgument(stat, definitions))]);
    }
    if (trimmed.startsWith("processingAffixes")) {
        const args = splitTopLevel(callArgs(trimmed, "processingAffixes"));
        const actionId = unquote(args[0]);
        const itemOutput = args[1].trim() === "true";
        const { extras, stats } = profileDefinitionArgs(args.slice(2), definitions);
        const result = [
            ...extras,
            ...stats.flatMap((stat) => definitionsForStatArgument(stat, definitions)),
            processingSpecific(actionId, ranges, constants),
            mustGet(definitions, "INSTANT_PROCESS", "definition INSTANT_PROCESS")
        ];
        if (itemOutput) {
            result.push(mustGet(definitions, "SUPER_OUTPUT", "definition SUPER_OUTPUT"));
        }
        return uniqueDefinitions(result);
    }
    if (trimmed.startsWith("processingStatAffixes")) {
        const args = splitTopLevel(callArgs(trimmed, "processingStatAffixes"));
        const actionId = unquote(args[0]);
        const { extras, stats } = profileDefinitionArgs(args.slice(1), definitions);
        return uniqueDefinitions([
            ...extras,
            ...stats.flatMap((stat) => definitionsForStatArgument(stat, definitions)),
            processingSpecific(actionId, ranges, constants)
        ]);
    }
    if (trimmed.startsWith("poweredProcessingAffixes")) {
        const args = splitTopLevel(callArgs(trimmed, "poweredProcessingAffixes"));
        const actionId = unquote(args[0]);
        const itemOutput = args[1].trim() === "true";
        return uniqueDefinitions([
            ...resolveProfileDefinitions(
                `processingAffixes(${JSON.stringify(actionId)}, ${itemOutput}, ${args.slice(2).join(", ")})`,
                definitions,
                ranges,
                constants
            ),
            mustGet(definitions, "OVERCLOCKED", "definition OVERCLOCKED")
        ]);
    }
    if (trimmed === "crusherAffixes()") {
        return uniqueDefinitions([
            ...resolveProfileDefinitions(
                "poweredProcessingAffixes(\"crushing\", true, MachineStat.ENERGY_USAGE, MachineStat.OUTPUT_AMOUNT, MachineStat.PROCESSING_SPEED)",
                definitions,
                ranges,
                constants
            ),
            ...[
                "CRUSHER_FRAME",
                "CRUSHER_KINETICS",
                "CRUSHER_JAWS",
                "CRUSHER_ORE_HANDLING",
                "CRUSHER_BATTERY_LINK",
                "CRUSHER_FEED_CONTROL",
                "CRUSHER_COMPRESSION",
                "CRUSHER_VIBRATION",
                "CRUSHER_THROUGHPUT",
                "CRUSHER_SALVAGE"
            ].map((definitionId) => mustGet(definitions, definitionId, `definition ${definitionId}`))
        ]);
    }
    if (trimmed === "crushHeadAffixes()") {
        return uniqueDefinitions([
            ...resolveProfileDefinitions(
                "processingAffixes(\"crushing\", true, MachineStat.PROCESSING_SPEED, MachineStat.OUTPUT_AMOUNT)",
                definitions,
                ranges,
                constants
            ),
            ...[
                "CRUSH_HEAD_PULVERIZING",
                "CRUSH_HEAD_JAGGED",
                "CRUSH_HEAD_KINETIC"
            ].map((definitionId) => mustGet(definitions, definitionId, `definition ${definitionId}`))
        ]);
    }
    if (trimmed === "solarArrayControllerAffixes()") {
        return uniqueDefinitions([
            ...resolveProfileDefinitions(
                "affixes(MACHINE_ENERGY_CAPACITY_ADD, MachineStat.ENERGY_CAPACITY, MachineStat.EFFICIENCY, MachineStat.ENERGY_GENERATION, MachineStat.STABILITY)",
                definitions,
                ranges,
                constants
            ),
            ...[
                "SOLAR_PANEL_LIMIT",
                "MOONLIGHT_CONVERSION",
                "CLOUD_PIERCER",
                "PANEL_SYNCHRONIZER",
                "PANEL_ARBITRATION",
                "CLEAR_SKY_AMPLIFIER",
                "LUNAR_INVERTER"
            ].map((definitionId) => mustGet(definitions, definitionId, `definition ${definitionId}`))
        ]);
    }
    if (trimmed === "batteryChassisAffixes()") {
        return uniqueDefinitions(resolveProfileDefinitions(
                "affixes(List.of(BATTERY_CHASSIS_CHARGED_STORAGE, BATTERY_CHASSIS_BALANCE_MODE, BATTERY_CHASSIS_BATTERY_SLOTS), MachineStat.ENERGY_CAPACITY, MachineStat.EFFICIENCY, MachineStat.ENERGY_TRANSFER, MachineStat.BURST_TRANSFER, MachineStat.BURST_DURATION, MachineStat.STABILITY, MachineStat.IDLE_LOSS, MachineStat.GLOBAL_MODIFIER_STRENGTH)",
                definitions,
                ranges,
                constants
        ));
    }
    if (trimmed === "solarPanelAffixes()") {
        return uniqueDefinitions([
            ...resolveProfileDefinitions(
                "affixes(MACHINE_ENERGY_CAPACITY_ADD, MachineStat.ENERGY_CAPACITY, MachineStat.EFFICIENCY, MachineStat.ENERGY_GENERATION, MachineStat.ENERGY_TRANSFER)",
                definitions,
                ranges,
                constants
            ),
            mustGet(definitions, "PEAK_SOLAR", "definition PEAK_SOLAR"),
            mustGet(definitions, "SOLAR_HORIZON_CATCHER", "definition SOLAR_HORIZON_CATCHER")
        ]);
    }
    throw new Error(`Unsupported profile definition expression: ${trimmed}`);
}

function profileDefinitionArgs(args, definitions) {
    const extras = [];
    const stats = [];
    for (const arg of args) {
        const trimmed = arg.trim();
        if (!trimmed) {
            continue;
        }
        if (trimmed.startsWith("MachineStat.")) {
            stats.push(enumRef(trimmed, "MachineStat"));
            continue;
        }
        if (trimmed.startsWith("List.of")) {
            extras.push(...listSymbols(trimmed).map((definitionId) =>
                mustGet(definitions, definitionId, `definition ${definitionId}`)));
            continue;
        }
        if (definitions.has(trimmed)) {
            extras.push(definitions.get(trimmed));
            continue;
        }
        throw new Error(`Unsupported profile definition argument: ${trimmed}`);
    }
    return { extras, stats };
}

function processingSpecific(actionId, ranges, constants) {
    const definition = withDefaultEffects({
        id: actionId,
        affixId: actionId,
        modGroup: constants.get("PROCESSING_SPEED_GROUP") ?? "processing_speed",
        slot: "SUFFIX",
        stat: "PROCESSING_SPEED",
        operation: "INCREASED_PERCENT",
        tierRanges: mustGet(ranges, "PROCESSING_SPECIFIC_SPEED_RANGES", "processing-specific speed ranges"),
        canRoll: true
    });
    return {
        ...definition,
        targetable: true,
        rollWeight: constants.get("PROCESSING_SPECIFIC_SPEED_WEIGHT") ?? 100,
        lensTags: inferLensTags(definition),
        flags: ["CAN_ROLL"]
    };
}

function definitionForStat(stat, definitions) {
    const definitionIds = {
        ENERGY_CAPACITY: "ENERGY_CAPACITY",
        PROCESSING_SPEED: "PROCESSING_SPEED",
        EFFICIENCY: "EFFICIENCY",
        ENERGY_USAGE: "ENERGY_USAGE",
        OUTPUT_AMOUNT: "OUTPUT_AMOUNT",
        HEAT_TRANSFER: "HEAT_TRANSFER",
        MAX_TEMPERATURE: "MAX_TEMPERATURE",
        HEAT_ISOLATION: "HEAT_ISOLATION",
        WARMUP_TIME: "WARMUP_TIME",
        COOLING_RATE: "COOLING_RATE",
        INPUT_SLOTS: "FUEL_SLOTS",
        ENERGY_GENERATION: "ENERGY_GENERATION",
        ENERGY_TRANSFER: "ENERGY_TRANSFER",
        FLUID_TRANSFER: "FLUID_TRANSFER",
        FLUID_CAPACITY: "FLUID_CAPACITY",
        FUEL_EFFICIENCY: "FUEL_EFFICIENCY",
        STABILITY: "STABILITY",
        TEMPERATURE_STABILITY: "TEMPERATURE_STABILITY",
        OVERHEAT_TOLERANCE: "OVERHEAT_TOLERANCE",
        BURST_TRANSFER: "BURST_TRANSFER",
        BURST_DURATION: "BURST_DURATION",
        IDLE_LOSS: "IDLE_LOSS",
        GLOBAL_MODIFIER_STRENGTH: "GLOBAL_MODIFIER_STRENGTH",
        CALIBRATION_QUALITY: "CALIBRATION_QUALITY",
        CALIBRATION_PRECISION: "CALIBRATION_PRECISION",
        CATALYST_EFFICIENCY: "CATALYST_EFFICIENCY",
        REFINEMENT_POTENTIAL_BONUS: "REFINEMENT_POTENTIAL_BONUS",
        DURABILITY: "DURABILITY_PERCENT",
        SELF_REPAIR: "TOOL_SELF_REPAIR",
        MINING_SPEED: "TOOL_MINING_SPEED",
        ATTACK_SPEED: "TOOL_ATTACK_SPEED",
        FE_USAGE: "TOOL_FE_USAGE",
        FE_TRANSFER: "TOOL_FE_TRANSFER",
        CONTROL: "TOOL_CONTROL",
        ORE_BURST_SPEED: "TOOL_ORE_BURST_SPEED",
        BATTERY_SUPPORT: "TOOL_BATTERY_SUPPORT",
        LUCK: "TOOL_LUCK",
        TREE_FELL_LIMIT: "TOOL_TREE_FELL_LIMIT",
        POTATO_POWER: "POTATO_POWER",
        CARROT_POWER: "CARROT_POWER",
        BREAD_POWER: "BREAD_POWER",
        SAPLING_POWER: "SAPLING_POWER",
        SEED_POWER: "SEED_POWER",
        PLANT_POWER: "PLANT_POWER",
        ORGANIC_REAGENT_POWER: "ORGANIC_REAGENT_POWER",
        COMPOSTED_BIOMASS_POWER: "COMPOSTED_BIOMASS_POWER",
        ALGAE_POWER: "ALGAE_POWER",
        RICH_BIOMASS_POWER: "RICH_BIOMASS_POWER",
        FUEL_DURATION: "FUEL_DURATION",
        SOLAR_PANEL_LIMIT: "SOLAR_PANEL_LIMIT",
        MOONLIGHT_CONVERSION: "MOONLIGHT_CONVERSION",
        WEATHER_RECOVERY: "CLOUD_PIERCER",
        SOLAR_PANEL_SYNCHRONIZATION: "PANEL_SYNCHRONIZER",
        SOLAR_PANEL_ARBITRATION: "PANEL_ARBITRATION",
        OVERFLOW_SHUNTING: "OVERFLOW_SHUNTING",
        CLEAR_SKY_AMPLIFICATION: "CLEAR_SKY_AMPLIFIER",
        LUNAR_INVERSION: "LUNAR_INVERTER",
        PEAK_SOLAR_GENERATION: "PEAK_SOLAR",
        INSTANT_PROCESS_CHANCE: "INSTANT_PROCESS",
        SUPER_OUTPUT_CHANCE: "SUPER_OUTPUT",
        BATTERY_SLOTS: "BATTERY_CHASSIS_BATTERY_SLOTS"
    };
    const definitionId = definitionIds[stat];
    if (!definitionId) {
        throw new Error(`No rollable modifier definition for stat ${stat}`);
    }
    return mustGet(definitions, definitionId, `definition ${definitionId}`);
}

function uniqueDefinitions(definitions) {
    return [...new Set(definitions)];
}

function definitionsForStatArgument(stat, definitions) {
    const result = [];
    if (stat === "ENERGY_GENERATION") {
        result.push(mustGet(definitions, "ENERGY_GENERATION_ADD", "definition ENERGY_GENERATION_ADD"));
    }
    if (stat === "MAX_TEMPERATURE") {
        result.push(mustGet(definitions, "MAX_TEMPERATURE_ADD", "definition MAX_TEMPERATURE_ADD"));
    }
    if (stat === "DURABILITY") {
        result.push(mustGet(definitions, "DURABILITY_ADD", "definition DURABILITY_ADD"));
    }
    result.push(definitionForStat(stat, definitions));
    return result;
}

function parseRefinementOperations(source, lang) {
    const body = source.slice(source.indexOf("{") + 1, source.indexOf(";\n\n    private final"));
    const operations = {};
    for (const entry of splitTopLevel(body)) {
        const trimmed = entry.trim();
        if (!trimmed) {
            continue;
        }
        const name = trimmed.slice(0, trimmed.indexOf("(")).trim();
        const args = splitTopLevel(trimmed.slice(trimmed.indexOf("(") + 1, trimmed.lastIndexOf(")")));
        const serialized = name.toLowerCase();
        operations[name] = {
            operation: name,
            id: serialized,
            label: lang[`rngtech.tooltip.refinement_operation.${serialized}`] ?? titleCase(name),
            action: enumRef(args[0], "RefinementAction"),
            targetStats: args[1] ? enumRefs(args[1], "MachineStat") : []
        };
    }
    return operations;
}

function labelDefinition(definition, lang) {
    const statId = definition.stat.toLowerCase();
    const operationId = definition.operation.toLowerCase();
    const slotId = definition.slot.toLowerCase();
    const affixId = definition.affixId ?? definition.id?.toLowerCase();
    const labeled = {
        ...definition,
        key: `${definition.slot}:${affixId ?? `${definition.stat}:${definition.operation}`}`,
        statId,
        operationId,
        affixId,
        affixLabel: affixLabel(slotId, affixId, definition.stat, lang),
        statLabel: lang[`rngtech.stat.${statId}`] ?? titleCase(definition.stat),
        operationLabel: titleCase(definition.operation)
    };
    if (definition.effects?.length) {
        labeled.effects = definition.effects.map((effect) => ({
            ...effect,
            statId: effect.stat.toLowerCase(),
            operationId: effect.operation.toLowerCase(),
            statLabel: lang[`rngtech.stat.${effect.stat.toLowerCase()}`] ?? titleCase(effect.stat),
            operationLabel: titleCase(effect.operation)
        }));
    }
    return labeled;
}

function affixLabel(slotId, affixId, stat, lang) {
    const exact = affixId ? lang[`rngtech.affix.${slotId}.${affixId}`] : null;
    if (exact) {
        return exact;
    }
    const variant = LEGACY_AFFIX_VARIANT_SUFFIXES.find((suffix) => affixId?.endsWith(`_${suffix}`));
    if (variant) {
        return lang[`rngtech.affix.${slotId}.${variant}`] ?? titleCase(variant);
    }
    const statId = stat.toLowerCase();
    return lang[`rngtech.affix.${slotId}.${statId}`] ?? titleCase(affixId ?? statId);
}

function buildLabels(lang) {
    const labels = {};
    for (const [key, value] of Object.entries(lang)) {
        if (key.startsWith("rngtech.stat.")
                || key.startsWith("rngtech.affix.")
                || key.startsWith("rngtech.behavior.")
                || key.startsWith("rngtech.tooltip.refinement_operation.")
                || key.startsWith("rngtech.tooltip.refinement_modifier.")) {
            labels[key] = value;
        }
    }
    return labels;
}

function resolveBehaviorList(expression, behaviorLists) {
    const trimmed = expression.trim();
    if (trimmed.startsWith("List.of")) {
        return enumRefs(trimmed, "MachineBehavior");
    }
    return mustGet(behaviorLists, trimmed, `behavior list ${trimmed}`);
}

function readAssignmentExpression(source, startIndex) {
    let depth = 0;
    let quote = null;
    for (let index = startIndex; index < source.length; index++) {
        const char = source[index];
        const previous = source[index - 1];
        if (quote) {
            if (char === quote && previous !== "\\") {
                quote = null;
            }
            continue;
        }
        if (char === "\"" || char === "'") {
            quote = char;
            continue;
        }
        if (char === "(" || char === "[" || char === "{") {
            depth++;
            continue;
        }
        if (char === ")" || char === "]" || char === "}") {
            depth--;
            continue;
        }
        if (char === ";" && depth === 0) {
            return source.slice(startIndex, index).trim();
        }
    }
    throw new Error("Could not read Java assignment expression.");
}

function callArgs(expression, callName) {
    const start = expression.indexOf(`${callName}(`);
    if (start < 0) {
        throw new Error(`Could not find call ${callName} in ${expression}`);
    }
    const open = start + callName.length;
    const close = matchingParen(expression, open);
    return expression.slice(open + 1, close);
}

function matchingParen(text, openIndex) {
    let depth = 0;
    let quote = null;
    for (let index = openIndex; index < text.length; index++) {
        const char = text[index];
        const previous = text[index - 1];
        if (quote) {
            if (char === quote && previous !== "\\") {
                quote = null;
            }
            continue;
        }
        if (char === "\"" || char === "'") {
            quote = char;
            continue;
        }
        if (char === "(") {
            depth++;
        } else if (char === ")") {
            depth--;
            if (depth === 0) {
                return index;
            }
        }
    }
    throw new Error("Unbalanced parentheses.");
}

function splitTopLevel(text) {
    const parts = [];
    let depth = 0;
    let quote = null;
    let start = 0;
    for (let index = 0; index < text.length; index++) {
        const char = text[index];
        const previous = text[index - 1];
        if (quote) {
            if (char === quote && previous !== "\\") {
                quote = null;
            }
            continue;
        }
        if (char === "\"" || char === "'") {
            quote = char;
            continue;
        }
        if (char === "(" || char === "[" || char === "{") {
            depth++;
            continue;
        }
        if (char === ")" || char === "]" || char === "}") {
            depth--;
            continue;
        }
        if (char === "," && depth === 0) {
            parts.push(text.slice(start, index).trim());
            start = index + 1;
        }
    }
    const last = text.slice(start).trim();
    if (last) {
        parts.push(last);
    }
    return parts;
}

function enumRef(text, enumName) {
    const match = text.match(new RegExp(`${enumName}\\.([A-Z_]+)`));
    if (!match) {
        throw new Error(`Could not parse ${enumName} reference from: ${text}`);
    }
    return match[1];
}

function isEnumRef(text, enumName) {
    return new RegExp(`^\\s*${enumName}\\.[A-Z_]+\\s*$`).test(text);
}

function enumRefs(text, enumName) {
    return [...text.matchAll(new RegExp(`${enumName}\\.([A-Z_]+)`, "g"))].map((match) => match[1]);
}

function listSymbols(text) {
    const trimmed = text.trim();
    if (!trimmed.startsWith("List.of")) {
        return [];
    }
    return splitTopLevel(callArgs(trimmed, "List.of")).map((symbol) => symbol.trim()).filter(Boolean);
}

function isQuoted(text) {
    return /^"[^"]*"$/.test(text.trim());
}

function unquote(text) {
    return text.trim().replace(/^"|"$/g, "");
}

function mustGet(map, key, label) {
    if (!map.has(key)) {
        throw new Error(`Missing ${label}.`);
    }
    return map.get(key);
}

function titleCase(value) {
    return value.toLowerCase()
        .split("_")
        .map((part) => part ? part[0].toUpperCase() + part.slice(1) : part)
        .join(" ");
}

if (isDirectRun()) {
    const data = await buildModifierData();
    console.log(`Exported ${Object.keys(data.profiles).length} modifier profiles to ${path.relative(ROOT, OUTPUT_PATH)}`);
}

function isDirectRun() {
    return typeof process !== "undefined"
        && process.argv?.[1]
        && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
}
