import { startModdexServer } from "./serve.mjs";
import { checkPassiveTreeData } from "./check-passive-tree.mjs";
import { checkAscendancySeals } from "./check-ascendancy-seals.mjs";
import { checkUniques } from "./check-uniques.mjs";
import { checkAscendancyDocs } from "./export-ascendancy-data.mjs";
import { checkRecipeLoopMutations, checkRecipeLoops } from "./check-recipe-loops.mjs";
import { checkRecyclingReturns } from "./check-recycling-returns.mjs";

const { server, url } = await startModdexServer({ port: 0, log: false });

try {
    const [indexResponse, passiveTreesRouteResponse, appResponse, guiEditorResponse, passiveTreeResponse, stylesResponse, dataResponse, guiDataResponse, passiveTreeDataResponse] = await Promise.all([
        fetch(url),
        fetch(new URL("./passive-trees", url)),
        fetch(new URL("./app.js", url)),
        fetch(new URL("./gui-editor.js", url)),
        fetch(new URL("./passive-tree-editor.js", url)),
        fetch(new URL("./styles.css", url)),
        fetch(new URL("./generated/modifier-data.json", url)),
        fetch(new URL("./generated/gui-layouts.json", url)),
        fetch(new URL("./generated/passive-trees.json", url))
    ]);

    assertOk(indexResponse, "index.html");
    assertOk(passiveTreesRouteResponse, "passive-trees route");
    assertOk(appResponse, "app.js");
    assertOk(guiEditorResponse, "gui-editor.js");
    assertOk(passiveTreeResponse, "passive-tree-editor.js");
    assertOk(stylesResponse, "styles.css");
    assertOk(dataResponse, "modifier-data.json");
    assertOk(guiDataResponse, "gui-layouts.json");
    assertOk(passiveTreeDataResponse, "passive-trees.json");

    const indexHtml = await indexResponse.text();
    const passiveTreesHtml = await passiveTreesRouteResponse.text();
    if (!indexHtml.includes("RNGTech Moddex")
            || !indexHtml.includes("RBOM")
            || !indexHtml.includes("Stages")
            || !indexHtml.includes("Passive Trees")
            || !indexHtml.includes("GUI")
            || !indexHtml.includes("Fluid Stack")
            || !indexHtml.includes("Player Inventory")
            || !indexHtml.includes("Affix gaps only")
            || !indexHtml.includes("Two Path Dead End")
            || !indexHtml.includes("Java Layout Class")
            || !indexHtml.includes("Load Source Tree")
            || !indexHtml.includes("Visual Audit")
            || !indexHtml.includes("Draft Name")
            || !indexHtml.includes("Duplicate")
            || !indexHtml.includes("Deshape Selected")
            || !indexHtml.includes("Selected Node")
            || !indexHtml.includes("passiveTreePreviewBody")) {
        throw new Error("index.html did not contain the expected title.");
    }
    if (!passiveTreesHtml.includes("RNGTech Moddex") || !passiveTreesHtml.includes("passiveTreeView")) {
        throw new Error("passive-trees route did not return the Moddex app.");
    }
    const appJs = await appResponse.text();
    if (!appJs.includes("passiveTreeView")
            || !appJs.includes("passiveTree")
            || !appJs.includes("/passive-trees")
            || !appJs.includes("activeViewFromPath")) {
        throw new Error("app.js did not wire the Passive Trees view.");
    }
    const guiEditorJs = await guiEditorResponse.text();
    if (!guiEditorJs.includes("undoGuiEdit")
            || !guiEditorJs.includes("keydown")
            || !guiEditorJs.includes("ctrlKey")
            || !guiEditorJs.includes("commitGuiPropertyOnEnter")
            || !guiEditorJs.includes("addFluidStack")
            || !guiEditorJs.includes("parentId")
            || !guiEditorJs.includes("visibleInJade")
            || !guiEditorJs.includes("inputGroup")
            || !guiEditorJs.includes("inputValue")
            || !guiEditorJs.includes("PLAYER_INVENTORY")
            || !guiEditorJs.includes("playerInventoryMarkup")) {
        throw new Error("gui-editor.js did not include expected GUI editor interaction support.");
    }
    const passiveTreeJs = await passiveTreeResponse.text();
    if (!passiveTreeJs.includes("rngtech.moddex.passive-tree.v1")
            || !passiveTreeJs.includes("generateJavaLayout")
            || !passiveTreeJs.includes("twoPathDeadEnd")
            || !passiveTreeJs.includes("branchedTravelPath")
            || !passiveTreeJs.includes("datapackCandidatePath")
            || !passiveTreeJs.includes("draftCandidatePath")
            || !passiveTreeJs.includes("createPassiveTreeDraft")
            || !passiveTreeJs.includes("switchPassiveTreeDraft")
            || !passiveTreeJs.includes("handlePassiveTreeCanvasPointerDown")
            || !passiveTreeJs.includes("undoPassiveTreeEdit")
            || !passiveTreeJs.includes("dragTargetsForSelection")
            || !passiveTreeJs.includes("handlePassiveTreeCanvasContextMenu")
            || !passiveTreeJs.includes("quickChangeGroupShape")
            || !passiveTreeJs.includes("transformPassiveTreeGroup")
            || !passiveTreeJs.includes("flipHorizontal")
            || !passiveTreeJs.includes("rotateClockwise")
            || !passiveTreeJs.includes("handlePassiveTreeCanvasDoubleClick")
            || !passiveTreeJs.includes("deshapeSelectedPassiveTreeGroup")
            || !passiveTreeJs.includes("connectBehindDeletedRefs")
            || !passiveTreeJs.includes("insertNodeOnConnection")
            || !passiveTreeJs.includes("removePassiveTreeLineFromEvent")
            || !passiveTreeJs.includes("removedInternalLinks")
            || !passiveTreeJs.includes("alwaysAllocated")
            || !passiveTreeJs.includes("saveCurrentShapeTemplate")
            || !passiveTreeJs.includes("loadPassiveTreeSourceCatalog")
            || !passiveTreeJs.includes("renderPassiveTreeVisualAudit")
            || !passiveTreeJs.includes("properGraphCrossings")
            || !passiveTreeJs.includes("renderPassiveTreePreview")
            || !passiveTreeJs.includes("javaShapePreview")) {
        throw new Error("passive-tree-editor.js did not include expected editor support.");
    }

    const passiveTreeData = await passiveTreeDataResponse.json();
    await checkPassiveTreeData(passiveTreeData);
    await checkRecipeLoops();
    await checkRecipeLoopMutations();
    await checkRecyclingReturns();
    await checkAscendancySeals();
    await checkUniques();
    await checkAscendancyView(url);
    await checkAscendancyDocs();

    const data = await dataResponse.json();
    const profiles = Object.keys(data.profiles ?? {});
    const crusher = data.profiles?.crusher;
    if (profiles.length === 0 || !crusher) {
        throw new Error("Modifier data did not include expected profiles.");
    }
    if (!crusher.definitions?.some((definition) => definition.stat === "PROCESSING_SPEED")) {
        throw new Error("Crusher profile did not include Processing Speed.");
    }
    const recipeCount = data.rbom?.recipes?.length ?? 0;
    const recipeConflicts = data.rbom?.recipeConflicts ?? [];
    const modularHammerRecipes = data.rbom?.recipesByOutput?.["rngtech:modular_hammer"] ?? [];
    const compostedBiomassRecipes = data.rbom?.recipesByOutput?.["rngtech:composted_biomass"] ?? [];
    if (recipeCount === 0 || modularHammerRecipes.length === 0 || compostedBiomassRecipes.length === 0) {
        throw new Error("RBOM data did not include expected code-backed pseudo recipes.");
    }
    if (recipeConflicts.length > 0) {
        const firstConflict = recipeConflicts[0]?.recipes?.map((recipe) => recipe.id).join(" vs ") ?? "unknown recipes";
        throw new Error(`RBOM data included ${recipeConflicts.length} overlapping crafting recipe conflict(s); first: ${firstConflict}.`);
    }
    const hammerRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:pseudo/tool_bench/modular_hammer");
    if (!hammerRecipe?.inputs?.some((input) => input.id === "rngtech:tiny_anvil" && input.reusable)) {
        throw new Error("Modular Hammer pseudo recipe did not keep the Tiny Anvil reusable.");
    }
    if (!hammerRecipe?.inputs?.some((input) => input.id === "rngtech:iron_hammer_head" && input.count === 1)) {
        throw new Error("Modular Hammer pseudo recipe did not include its hammer head.");
    }
    const primedCellCoreRecipes = data.rbom?.recipesByOutput?.["rngtech:battery_cell"] ?? [];
    const primedCellCoreRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:battery_assembly/primed_cell_core");
    if (primedCellCoreRecipes[0] !== "rngtech:battery_assembly/primed_cell_core"
            || primedCellCoreRecipe?.result?.count !== 1
            || !primedCellCoreRecipe.inputs?.some((input) => input.id === "rngtech:cell_shell" && input.count === 1)
            || !primedCellCoreRecipe.inputs?.some((input) =>
                    input.kind === "fluid"
                    && input.id === "rngtech:electrolyte_solution"
                    && input.count === 250)) {
        throw new Error("RBOM did not include the Battery Assembly primed cell core recipe with electrolyte input.");
    }
    const electrolyteSolutionRecipes = data.rbom?.recipesByOutput?.["rngtech:electrolyte_solution"] ?? [];
    const electrolyteMelterRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:melter/electrolyte_solution");
    const dryElectrolyteRecipe = data.rbom.recipes.find((recipe) =>
        recipe.id === "rngtech:pseudo/component_assembler/electrolyte_solution_from_dry_electrolyte");
    if (!electrolyteSolutionRecipes.includes("rngtech:melter/electrolyte_solution")
            || !electrolyteSolutionRecipes.includes("rngtech:pseudo/component_assembler/electrolyte_solution_from_dry_electrolyte")
            || electrolyteMelterRecipe?.result?.kind !== "fluid"
            || electrolyteMelterRecipe.result.count !== 1000
            || !electrolyteMelterRecipe.inputs?.some((input) => input.id === "rngtech:melter" && input.reusable)
            || dryElectrolyteRecipe?.result?.kind !== "fluid"
            || dryElectrolyteRecipe.result.count !== 125
            || !dryElectrolyteRecipe.inputs?.some((input) => input.id === "rngtech:battery_assembler" && input.reusable)
            || !dryElectrolyteRecipe.inputs?.some((input) => input.id === "rngtech:electrolyte" && input.count === 1)) {
        throw new Error("RBOM did not model Electrolyte Solution as a fluid output with both Melter and dry-electrolyte sources.");
    }
    const melterRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:melter");
    const sparksteelConductiveRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:calibration_conductive_sparksteel_coil");
    const copperConductiveRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:calibration_conductive_copper_coil");
    const melterConductiveGate = melterRecipe?.inputs?.find((input) => input.id === "rngtech:calibrated_conductive_component")?.calibration;
    if (!melterRecipe
            || melterRecipe.inputs?.some((input) => input.id === "rngtech:sparksteel_battery_cell")
            || melterConductiveGate?.family !== "conductive"
            || melterConductiveGate.minStage !== 5
            || melterConductiveGate.minStability !== 70
            || sparksteelConductiveRecipe?.result?.calibration?.stage !== 5
            || !calibrationRecipeSatisfies(sparksteelConductiveRecipe, melterConductiveGate)
            || !sparksteelConductiveRecipe.inputs?.some((input) => input.id === "rngtech:sparksteel_coil")
            || copperConductiveRecipe?.result?.calibration?.stage !== 2
            || calibrationRecipeSatisfies(copperConductiveRecipe, melterConductiveGate)) {
        throw new Error("RBOM did not preserve the Melter's Stage 5 conductive calibrated component gate.");
    }
    const ironBatteryCellRecipes = data.rbom?.recipesByOutput?.["rngtech:iron_battery_cell"] ?? [];
    const ironBatteryCellRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:battery_assembly/iron_battery_cell");
    if (ironBatteryCellRecipes[0] !== "rngtech:battery_assembly/iron_battery_cell"
            || ironBatteryCellRecipe?.metadata?.includes("Rolls result traits") !== true
            || !ironBatteryCellRecipe.inputs?.some((input) => input.id === "rngtech:battery_cell" && input.count === 1)
            || !ironBatteryCellRecipe.inputs?.some((input) => input.id === "c:ingots/iron" && input.count === 1)) {
        throw new Error("RBOM did not include the Iron Battery Cell assembly recipe.");
    }
    const coreResourceIds = new Set(data.rbom?.coreResourceIds ?? []);
    if (!coreResourceIds.has("minecraft:iron_ingot")
            || !coreResourceIds.has("minecraft:redstone")
            || coreResourceIds.has("rngtech:bronze_ingot")) {
        throw new Error("RBOM core resources did not stop at ingots/redstone while leaving alloy ingots expandable.");
    }
    const bronzeRecipes = data.rbom?.recipesByOutput?.["rngtech:bronze_ingot"] ?? [];
    if (!bronzeRecipes.length
            || !bronzeRecipes.some((recipeId) => data.rbom.recipes.some((recipe) =>
                    recipe.id === recipeId
                    && recipe.result?.id === "rngtech:bronze_ingot"
                    && recipe.progressionSource !== false))) {
        throw new Error("RBOM did not include an ordinary Bronze Ingot progression recipe.");
    }
    const bronzeCasingRecipes = data.rbom?.recipesByOutput?.["rngtech:bronze_casing"] ?? [];
    const bronzeCasingRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:materials/bronze_casing");
    const bronzeCasingPressRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:metal_press/bronze_casing");
    if (bronzeCasingRecipes[0] !== "rngtech:materials/bronze_casing"
            || !bronzeCasingRecipe?.inputs?.some((input) => input.id === "c:ingots/bronze" && input.count === 5)
            || !bronzeCasingRecipe.inputs?.some((input) => input.id === "c:nuggets/bronze" && input.count === 3)
            || !bronzeCasingPressRecipe?.inputs?.some((input) =>
                    input.id === "rngtech:casing_mold"
                    && input.reusable
                    && input.role === "tool")) {
        throw new Error("RBOM did not prefer the manual Bronze Casing recipe while preserving the press mold prerequisite.");
    }
    const steelIngotRecipe = (data.rbom?.recipesByOutput?.["rngtech:steel_ingot"] ?? [])
        .map((recipeId) => data.rbom.recipes.find((recipe) => recipe.id === recipeId))
        .filter(Boolean)
        .find((recipe) => recipe.inputs?.some((input) =>
            input.id === "rngtech:bronze_alloy_furnace_chassis"
            && input.reusable
            && input.role === "machine"));
    if (!steelIngotRecipe
            || !steelIngotRecipe.inputs?.some((input) =>
                    input.id === "rngtech:bronze_alloy_crucible"
                    && input.reusable
                    && input.role === "gear")) {
        throw new Error("RBOM did not model a Steel Ingot bootstrap route through Stage 3 Alloy Furnace setup.");
    }
    const ironNuggetRecipes = data.rbom?.recipesByOutput?.["minecraft:iron_nugget"] ?? [];
    const ironNuggetRecipe = data.rbom.recipes.find((recipe) => recipe.id === "minecraft:iron_nugget_from_iron_ingot");
    if (ironNuggetRecipes[0] !== "minecraft:iron_nugget_from_iron_ingot"
            || ironNuggetRecipe?.result?.count !== 9
            || !ironNuggetRecipe.inputs?.some((input) => input.id === "c:ingots/iron" && input.count === 1)) {
        throw new Error("RBOM did not prefer vanilla iron ingot-to-nugget conversion over tool-smelting salvage.");
    }
    const stagePreview = data.rbom?.stagePreview;
    const melter = stagePreview?.items?.find((item) => item.id === "rngtech:melter");
    if (!melter?.reachable
            || melter.issues.length
            || melter.reachability?.recipeId !== "rngtech:melter"
            || !melter.directStageInputs?.some((input) =>
                    input.id === "rngtech:calibrated_conductive_component"
                    && input.minStage === 5)) {
        throw new Error("Stage preview did not keep the Melter reachable through its Stage 5 conductive calibrated component gate.");
    }
    const modularHammer = stagePreview?.items?.find((item) => item.id === "rngtech:modular_hammer");
    if (!modularHammer?.craftable || modularHammer.firstRecipeId !== "rngtech:pseudo/tool_bench/modular_hammer") {
        throw new Error("Stage preview did not include the Modular Hammer pseudo recipe path.");
    }
    const compostedBiomass = stagePreview?.items?.find((item) => item.id === "rngtech:composted_biomass");
    if (!compostedBiomass?.reachable || compostedBiomass.firstRecipeId !== "rngtech:pseudo/wooden_composter/composted_biomass") {
        throw new Error("Stage preview did not include the Wooden Composter pseudo process path.");
    }
    if (!stagePreview?.categories?.some((category) => category.id === "component")) {
        throw new Error("Stage preview did not include category filters.");
    }
    const titaniumAlloyFurnace = stagePreview?.items?.find((item) => item.id === "rngtech:titanium_alloy_furnace_chassis");
    if (!titaniumAlloyFurnace?.reachable
            || titaniumAlloyFurnace.issues.length
            || titaniumAlloyFurnace.reachability?.recipeId !== "rngtech:titanium_alloy_furnace_chassis") {
        throw new Error("Stage preview did not mark the Titanium Alloy Furnace chain reachable after the Arclite bootstrap.");
    }
    const arcliteIngot = stagePreview?.items?.find((item) => item.id === "rngtech:arclite_ingot");
    if (!arcliteIngot?.reachable
            || arcliteIngot.issues.length
            || arcliteIngot.reachability?.recipeId !== "rngtech:alloy_furnace/arclite_from_ingots") {
        throw new Error("Stage preview did not use the Stage 4 direct-ingot Arclite bootstrap route.");
    }
    const arcliteCoil = stagePreview?.items?.find((item) => item.id === "rngtech:arclite_coil");
    const eliteCircuitBlank = stagePreview?.items?.find((item) => item.id === "rngtech:elite_circuit_blank");
    if (!arcliteCoil?.reachable || arcliteCoil.issues.length || !eliteCircuitBlank?.reachable || eliteCircuitBlank.issues.length) {
        throw new Error("Stage preview still blocks the Arclite Coil or Elite Circuit Blank chain.");
    }
    const bronzeIngot = stagePreview?.items?.find((item) => item.id === "rngtech:bronze_ingot");
    const steelIngot = stagePreview?.items?.find((item) => item.id === "rngtech:steel_ingot");
    const invarIngot = stagePreview?.items?.find((item) => item.id === "rngtech:invar_ingot");
    const invarBatteryCell = stagePreview?.items?.find((item) => item.id === "rngtech:invar_battery_cell");
    const steelCrusherChassis = stagePreview?.items?.find((item) => item.id === "rngtech:steel_crusher_chassis");
    if (!bronzeIngot?.reachable
            || bronzeIngot.issues.length
            || !steelIngot?.reachable
            || steelIngot.issues.length
            || !invarIngot?.reachable
            || invarIngot.issues.length
            || !invarBatteryCell?.reachable
            || invarBatteryCell.issues.length
            || !steelCrusherChassis?.reachable
            || steelCrusherChassis.issues.length) {
        throw new Error("Stage preview blocked Bronze, Steel, or Invar ingot progression paths.");
    }
    const basicEnergyConnector = stagePreview?.items?.find((item) => item.id === "rngtech:basic_energy_connector");
    const casingMold = stagePreview?.items?.find((item) => item.id === "rngtech:casing_mold");
    const copperSolarPanel = stagePreview?.items?.find((item) => item.id === "rngtech:copper_solar_panel");
    const ironResonanceCalibrator = stagePreview?.items?.find((item) =>
        item.id === "rngtech:iron_resonance_calibrator_chassis");
    const conductiveComponent = stagePreview?.items?.find((item) => item.id === "rngtech:calibrated_conductive_component");
    const advancedCircuit = stagePreview?.items?.find((item) => item.id === "rngtech:advanced_electric_circuit");
    if (!basicEnergyConnector?.reachable
            || basicEnergyConnector.stage !== 4
            || !basicEnergyConnector.stageFlags?.some((flag) => flag.id === "pressed_connector_steel_access")
            || !casingMold?.reachable
            || casingMold.stage !== 4
            || !casingMold.stageFlags?.some((flag) => flag.id === "casing_mold_steel_access")
            || !copperSolarPanel?.reachable
            || copperSolarPanel.stage !== 4
            || !copperSolarPanel.stageFlags?.some((flag) => flag.id === "copper_solar_panel_steel_access")
            || !ironResonanceCalibrator?.reachable
            || ironResonanceCalibrator.stage !== 4
            || !ironResonanceCalibrator.stageFlags?.some((flag) => flag.id === "resonance_calibrator_steel_access")
            || !conductiveComponent?.reachable
            || conductiveComponent.stage !== 4
            || !conductiveComponent.stageFlags?.some((flag) => flag.id === "conductive_calibration_steel_access")
            || !advancedCircuit?.reachable
            || advancedCircuit.stage !== 4
            || !advancedCircuit.stageFlags?.some((flag) => flag.id === "advanced_circuit_steel_access")) {
        throw new Error("Stage preview did not apply Steel-stage progression flags for pressed connectors, mold tooling, connector-dependent solar, resonance, conductive calibration, and advanced circuits.");
    }
    const collapseResidueRecipes = data.rbom?.recipesByOutput?.["rngtech:collapse_residue"] ?? [];
    const vacuumCollapseRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:vacuum_collapse/void_catalyst");
    const collapseResidue = stagePreview?.items?.find((item) => item.id === "rngtech:collapse_residue");
    const chaosCrystal = stagePreview?.items?.find((item) => item.id === "rngtech:chaos_crystal");
    const exoticAffixForge = stagePreview?.items?.find((item) => item.id === "rngtech:exotic_affix_forge");
    const exoticAffixCatalyst = stagePreview?.items?.find((item) => item.id === "rngtech:exotic_affix_catalyst");
    if (collapseResidueRecipes[0] !== "rngtech:vacuum_collapse/void_catalyst"
            || vacuumCollapseRecipe?.result?.id !== "rngtech:collapse_residue"
            || vacuumCollapseRecipe.requirements?.minimumChamberStage !== 7
            || !vacuumCollapseRecipe.metadata?.includes("Chamber Stage 7+")
            || !vacuumCollapseRecipe.metadata?.includes("Residue requires stabilizer")
            || !vacuumCollapseRecipe.inputs?.some((input) =>
                    input.id === "rngtech:vacuum_collapse_generator"
                    && input.reusable
                    && input.role === "machine")
            || !vacuumCollapseRecipe.inputs?.some((input) =>
                    input.id === "rngtech:tungstensteel_void_chamber"
                    && input.reusable
                    && input.role === "gear")
            || !vacuumCollapseRecipe.inputs?.some((input) =>
                    input.id === "rngtech:tungstensteel_collapse_nozzle"
                    && input.reusable
                    && input.role === "gear")
            || !vacuumCollapseRecipe.inputs?.some((input) =>
                    input.id === "rngtech:tungstensteel_dimensional_stabilizer"
                    && input.reusable
                    && input.role === "gear"
                    && input.note.includes("residue recovery"))
            || !collapseResidue?.craftable
            || !collapseResidue.reachable
            || collapseResidue.issues.length
            || collapseResidue.stage < 7
            || collapseResidue.reachability?.recipeId !== "rngtech:vacuum_collapse/void_catalyst"
            || !chaosCrystal?.craftable
            || !chaosCrystal.reachable
            || chaosCrystal.issues.length
            || !exoticAffixForge?.reachable
            || exoticAffixForge.issues.length
            || !exoticAffixCatalyst?.reachable
            || exoticAffixCatalyst.issues.length) {
        throw new Error("RBOM did not model Vacuum Collapse residue output and staged Gear requirements.");
    }
    const superchargedRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:alloy_furnace/supercharged_silica_gel_beads");
    const superchargedBeads = stagePreview?.items?.find((item) => item.id === "rngtech:supercharged_silica_gel_beads");
    if (superchargedRecipe?.progressionSource !== true
            || superchargedRecipe.requirements?.minimumComponentStage !== 6
            || !superchargedRecipe.inputs?.some((input) =>
                    input.id === "rngtech:titanium_alloy_furnace_chassis"
                    && input.reusable
                    && input.role === "machine")
            || !superchargedRecipe.inputs?.some((input) =>
                    input.id === "rngtech:titanium_alloy_crucible"
                    && input.reusable
                    && input.role === "gear")
            || !superchargedRecipe.inputs?.some((input) =>
                    input.id === "rngtech:titanium_heat_core"
                    && input.reusable
                    && input.role === "gear")
            || !superchargedBeads?.reachable
            || superchargedBeads.issues.length
            || superchargedBeads.reachability?.recipeId !== "rngtech:alloy_furnace/supercharged_silica_gel_beads") {
        throw new Error("Stage preview did not allow the intentional Stage 6 Alloy Furnace gate for Supercharged Silica Gel Beads.");
    }
    const malformedIngot = stagePreview?.items?.find((item) => item.id === "rngtech:malformed_ingot");
    const malformedRecoveryRecipe = data.rbom.recipes.find((recipe) => recipe.id === "rngtech:furnace/malformed/invar_nuggets");
    const malformedRecoveryItems = [
        ["rngtech:invar_nugget", "rngtech:furnace/malformed/invar_nuggets"],
        ["rngtech:arclite_nugget", "rngtech:furnace/malformed/arclite_nuggets"],
        ["rngtech:aethergold_nugget", "rngtech:furnace/malformed/aethergold_nuggets"],
        ["rngtech:tungstensteel_nugget", "rngtech:furnace/malformed/tungstensteel_nuggets"]
    ];
    if (malformedRecoveryRecipe?.progressionSource !== false
            || malformedRecoveryRecipe.sourceRole !== "failure_recovery"
            || !malformedRecoveryRecipe.metadata?.includes("Failure/recovery route")
            || malformedIngot?.issues?.length) {
        throw new Error("RBOM did not classify malformed-ingot recovery recipes as non-progression failure/recovery routes.");
    }
    for (const [itemId, recipeId] of malformedRecoveryItems) {
        const item = stagePreview?.items?.find((entry) => entry.id === itemId);
        if (!item?.reachable
                || item.issues.length
                || item.recipeIds.includes(recipeId)
                || !item.nonProgressionRecipeIds?.includes(recipeId)
                || item.reachability?.blockerId === "rngtech:malformed_ingot") {
            throw new Error(`${itemId} was still blocked by malformed-ingot recovery as an ordinary progression source.`);
        }
    }
    const guiData = await guiDataResponse.json();
    const melterLayout = guiData.layouts?.find((layout) => layout.screenClass === "MelterScreen");
    const cavitationLayout = guiData.layouts?.find((layout) => layout.screenClass === "CavitationGeneratorScreen");
    const bioGeneratorLayout = guiData.layouts?.find((layout) => layout.screenClass === "BioGeneratorScreen");
    const compressorTankLayout = guiData.layouts?.find((layout) => layout.screenClass === "CompressorTankScreen");
    const debugTankLayout = guiData.layouts?.find((layout) => layout.screenClass === "DebugTankScreen");
    const algaeLayout = guiData.layouts?.find((layout) => layout.screenClass === "AlgaePhotobioreactorScreen");
    if (!melterLayout?.elements?.some((element) => element.type === "FLUID_SLOT" && element.slotType === "BUCKET_INPUT")) {
        throw new Error("GUI layout data did not include the Melter bucket input slot.");
    }
    if (!melterLayout?.elements?.some((element) =>
            element.type === "PLAYER_INVENTORY"
            && element.tabId === "processing"
            && element.rows === 3
            && element.columns === 9
            && element.hotbar)
            || melterLayout.elements.some((element) => element.type === "PLAYER_INVENTORY" && element.tabId === "stats")) {
        throw new Error("GUI layout data did not include the Melter player inventory on the expected tabs.");
    }
    if (!melterLayout?.elements?.some((element) => element.type === "FE_METER")) {
        throw new Error("GUI layout data did not include the Melter FE meter.");
    }
    if (!melterLayout?.elements?.some((element) =>
            element.tabId === "processing"
            && element.type === "ITEM_SLOT"
            && element.slotType === "MACHINE_SELF"
            && element.slotRole === "MACHINE_SELF")) {
        throw new Error("GUI layout data did not classify the Melter processing self machine slot.");
    }
    if (!cavitationLayout?.elements?.some((element) => element.type === "FLUID_METER")
            || !cavitationLayout.elements.some((element) => element.type === "FE_METER")) {
        throw new Error("GUI layout data did not include expected Cavitation Generator meters.");
    }
    if (!compressorTankLayout?.elements?.some((element) =>
            element.type === "COMPRESSOR_TANK"
            && ["LOOSE_FLUID", "COMPRESSED_FLUID", "EQUIVALENT_FLUID"].includes(element.tankRole))) {
        throw new Error("GUI layout data did not classify Compressor Tank compression columns.");
    }
    if (!compressorTankLayout?.elements?.some((element) => element.type === "FLUID_TANK" && element.tankRole === "PLAIN")) {
        throw new Error("GUI layout data did not classify the Compressor Tank plain fluid tank.");
    }
    if (!debugTankLayout?.elements?.some((element) => element.type === "FLUID_TANK")) {
        throw new Error("GUI layout data did not include the Debug Tank fluid tank.");
    }
    if (!debugTankLayout?.elements?.some((element) => element.type === "FLUID_SLOT" && element.slotType === "BUCKET_INPUT")
            || !debugTankLayout.elements.some((element) => element.type === "FLUID_SLOT" && element.slotType === "BUCKET_OUTPUT")) {
        throw new Error("GUI layout data did not classify Debug Tank fluid container slots.");
    }
    if (!algaeLayout?.elements?.some((element) =>
            ["FLUID_METER", "FLUID_TANK"].includes(element.type)
            && element.tankRole === "WATER")
            || !algaeLayout.elements.some((element) =>
                    ["FLUID_METER", "FLUID_TANK"].includes(element.type)
                    && element.tankRole === "CARBON")) {
        throw new Error("GUI layout data did not include Algae Photobioreactor water/carbon gauges.");
    }
    if (!algaeLayout?.elements?.some((element) => element.slotRole === "WATER_CONTAINER_INPUT")
            || !algaeLayout.elements.some((element) => element.slotRole === "CARBON_CONTAINER_OUTPUT")
            || !algaeLayout.elements.some((element) => element.type === "ITEM_SLOT" && element.source?.expression?.includes("OUTPUT_X"))) {
        throw new Error("GUI layout data did not classify Algae Photobioreactor machine slots.");
    }
    if (!algaeLayout?.elements?.some((element) =>
            element.type === "PLAYER_INVENTORY"
            && element.tabId === "processing"
            && element.x === 7
            && element.y === 90)) {
        throw new Error("GUI layout data did not include the Algae Photobioreactor player inventory.");
    }
    if (!bioGeneratorLayout?.elements?.some((element) =>
            element.type === "STATUS_BAR"
            && element.statusSquares?.length === 4
            && element.statusSquares.some((square) => square.tooltip?.text === "rngtech.bio_generator.tooltip.fuel_value"))) {
        throw new Error("GUI layout data did not group Bio Generator status icon squares.");
    }
    if (!bioGeneratorLayout?.elements?.some((element) => element.type === "STATUS_BAR" && element.meterKind === "FUEL")) {
        throw new Error("GUI layout data did not classify the Bio Generator fuel bar.");
    }
    if (bioGeneratorLayout?.titleLabel?.x !== 8 || bioGeneratorLayout.titleLabel.y !== 6) {
        throw new Error("GUI layout data did not include the Bio Generator title label defaults.");
    }
    if (!bioGeneratorLayout?.elements?.some((element) =>
            element.tabId === "refinement"
            && element.type === "ITEM_SLOT"
            && element.slotType === "MACHINE_SELF"
            && element.slotRole === "MACHINE_SELF")) {
        throw new Error("GUI layout data did not classify the Bio Generator refinement self machine slot.");
    }
    const woodenCrusher = stagePreview?.items?.find((item) => item.id === "rngtech:wooden_crusher_chassis");
    if (woodenCrusher?.modifierCoverage?.profileId !== "crusher"
            || woodenCrusher.modifierCoverage.prefixGroups !== 10
            || woodenCrusher.modifierCoverage.suffixGroups !== 5) {
        throw new Error("Stage preview did not include expected modifier coverage for Wooden Crusher Chassis.");
    }
    const fuelBox = stagePreview?.items?.find((item) => item.id === "rngtech:copper_fuel_box");
    if (fuelBox?.modifierCoverage?.profileId !== "fuel_box"
            || fuelBox.modifierCoverage.prefixGroups !== 4
            || fuelBox.modifierCoverage.suffixGroups !== 5) {
        throw new Error("Stage preview did not include expected modifier coverage for Copper Fuel Box.");
    }
    const expectedQuestItems = new Map([
        ["rngtech:wooden_composter", 0],
        ["rngtech:composted_biomass", 0],
        ["rngtech:rich_biomass", 0],
        ["rngtech:plant_reagent", 0],
        ["rngtech:resin", 0],
        ["rngtech:flint_pick_head", 0],
        ["rngtech:wooden_tool_rod", 0],
        ["rngtech:wooden_crusher_chassis", 0],
        ["rngtech:potato_battery_cell", 0],
        ["rngtech:modular_pick", 0],
        ["rngtech:tiny_anvil", 1],
        ["rngtech:modular_hammer", 1],
        ["rngtech:modular_axe", 1],
        ["rngtech:modular_treefeller", 1],
        ["rngtech:cable", 1],
        ["rngtech:iron_compressor_tank", 1],
        ["rngtech:copper_compressor_tank", 2],
        ["rngtech:bronze_compressor_tank", 3],
        ["rngtech:steel_compressor_tank", 4],
        ["rngtech:lubricant_bucket", 6],
        ["rngtech:titanium_compressor_tank", 6],
        ["rngtech:tungstensteel_compressor_tank", 7]
    ]);
    for (const [itemId, stage] of expectedQuestItems) {
        const item = stagePreview?.items?.find((entry) => entry.id === itemId);
        if (!item || item.stage !== stage || !item.reachable || item.issues.length) {
            throw new Error(`${itemId} was not classified as a reachable stage ${stage} quest item.`);
        }
    }

    console.log(`Moddex smoke passed: ${profiles.length} profiles and ${recipeCount} recipes served from ${url}`);
} finally {
    await new Promise((resolve, reject) => {
        server.close((error) => error ? reject(error) : resolve());
    });
}

/** The Ascendancies tab serves its route, script, and generated catalog, and every family's ascendancies are in it. */
async function checkAscendancyView(url) {
    const [routeResponse, scriptResponse, dataResponse] = await Promise.all([
        fetch(new URL("./ascendancies", url)),
        fetch(new URL("./ascendancy-view.js", url)),
        fetch(new URL("./generated/ascendancies.json", url))
    ]);
    assertOk(routeResponse, "ascendancies route");
    assertOk(scriptResponse, "ascendancy-view.js");
    assertOk(dataResponse, "ascendancies.json");
    const html = await routeResponse.text();
    if (!html.includes("ascendancyView") || !html.includes("ascendancyCards") || !html.includes("/ascendancy-view.js")) {
        throw new Error("ascendancies route did not return the Ascendancies view.");
    }
    const script = await scriptResponse.text();
    if (!script.includes("treeSvg") || !script.includes("ascendancyDeclarationBody") || !script.includes("coveredBy")) {
        throw new Error("ascendancy-view.js did not include the tree, node, and declaration views.");
    }
    const data = await dataResponse.json();
    for (const family of data.families) {
        if (family.ascendancies.length < 2) {
            throw new Error(`ascendancies.json lists fewer than two ascendancies for ${family.id}.`);
        }
        for (const ascendancy of family.ascendancies) {
            if (ascendancy.nodes.filter((node) => node.kind === "ROOT").length !== 1 || !ascendancy.nodes.some((node) => node.kind === "DEEP")) {
                throw new Error(`ascendancies.json has a malformed tree for ${ascendancy.id}.`);
            }
        }
    }
    const uncovered = [...data.stats, ...data.behaviors].filter((entry) => entry.yield !== "none" && !entry.coveredBy.length);
    if (uncovered.length) {
        throw new Error(`ascendancies.json has yield entries without a loop bound: ${uncovered.map((entry) => entry.id).join(", ")}`);
    }
}

function assertOk(response, label) {
    if (!response.ok) {
        throw new Error(`${label} returned HTTP ${response.status}.`);
    }
}

function calibrationRecipeSatisfies(recipe, requirement) {
    const output = recipe?.result?.calibration;
    if (!output || !requirement) {
        return false;
    }
    return output.family === requirement.family
        && output.stage >= requirement.minStage
        && output.stabilityMax >= requirement.minStability
        && output.refinementPotentialMax >= requirement.minRefinementPotential;
}
