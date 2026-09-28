const PASSIVE_TREE_SCHEMA = "rngtech.moddex.passive-tree.v2";
const PASSIVE_TREE_LEGACY_SCHEMAS = new Set(["rngtech.moddex.passive-tree.v1", PASSIVE_TREE_SCHEMA]);
const PASSIVE_TREE_STORAGE_KEY = "rngtech.moddex.passive-tree.drafts";
const PASSIVE_TREE_LEGACY_STORAGE_KEY = "rngtech.moddex.passive-tree.tree";
const PASSIVE_TREE_SHAPES_KEY = "rngtech.moddex.passive-tree.saved-shapes";
const PASSIVE_TREE_HISTORY_LIMIT = 80;
const DEFAULT_PACKAGE = "com.rngtech.rpg.progression";
const DEFAULT_CLASS = "CrusherPassiveTreeLayout";
const DEFAULT_CANVAS = { width: 900, height: 650 };
const PASSIVE_TREE_SOURCE_URL = "/generated/passive-trees.json";
const NODE_KINDS = ["STARTER", "NODE", "TRAVEL", "NOTABLE", "KEYSTONE"];
const SECONDARY_IDENTITY_NONE = "";
const IDENTITY_PRESETS = [
    "Starter",
    "Kinetic Foundation",
    "Processing Speed",
    "Throughput",
    "Energy Capacity",
    "Energy Efficiency",
    "Energy Transfer",
    "Output Yield",
    "Precision",
    "Control",
    "Stability",
    "Heat",
    "Fuel",
    "Storage",
    "Automation",
    "Recovery",
    "Machine Sound",
    "Component Support",
    "Risk",
    "Custom"
];
const IDENTITY_COLORS = new Map([
    ["Starter", "#eef1f2"],
    ["Kinetic Foundation", "#e8e06a"],
    ["Processing Speed", "#4fb3ff"],
    ["Throughput", "#5dd6ff"],
    ["Energy Capacity", "#d7a642"],
    ["Energy Efficiency", "#36c2a0"],
    ["Energy Transfer", "#62d982"],
    ["Output Yield", "#bd8cff"],
    ["Precision", "#78a6ff"],
    ["Control", "#f08c5b"],
    ["Stability", "#91c56b"],
    ["Heat", "#ff6d4a"],
    ["Fuel", "#e5b14f"],
    ["Storage", "#9ac3d5"],
    ["Automation", "#b6a4ff"],
    ["Recovery", "#6ed6b3"],
    ["Machine Sound", "#b9c0c8"],
    ["Component Support", "#b7cf62"],
    ["Risk", "#d95f78"],
    ["Custom", "#aeb7bd"]
]);
const SHAPE_PARAMS = {
    point: [
        ["x", "X", 360],
        ["y", "Y", 300]
    ],
    circle: [
        ["centerX", "Center X", 360],
        ["centerY", "Center Y", 300],
        ["radius", "Radius", 52],
        ["startDegrees", "Start Degrees", 200]
    ],
    halfCircle: [
        ["centerX", "Center X", 360],
        ["centerY", "Center Y", 300],
        ["radius", "Radius", 82],
        ["startDegrees", "Start Degrees", 165]
    ],
    arc: [
        ["centerX", "Center X", 360],
        ["centerY", "Center Y", 300],
        ["radius", "Radius", 124],
        ["startDegrees", "Start Degrees", -70],
        ["sweepDegrees", "Sweep Degrees", 140]
    ],
    line: [
        ["startX", "Start X", 220],
        ["startY", "Start Y", 300],
        ["xStep", "X Step", 42],
        ["yStep", "Y Step", 0]
    ],
    travelPath: [
        ["fromX", "From X", 240],
        ["fromY", "From Y", 300],
        ["toX", "To X", 480],
        ["toY", "To Y", 300]
    ],
    twoPathDeadEnd: [
        ["notableX", "Notable X", 760],
        ["notableY", "Notable Y", 128],
        ["xStepFromNotable", "X Step From Notable", -62],
        ["yStepFromNotable", "Y Step From Notable", 0],
        ["xBranchOffset", "X Branch Offset", 0],
        ["yBranchOffset", "Y Branch Offset", 42]
    ],
    branchedTravelPath: [
        ["fromX", "From X", 240],
        ["fromY", "From Y", 300],
        ["toX", "To X", 520],
        ["toY", "To Y", 300],
        ["branchIndex", "Branch Index", 1],
        ["xBranchOffset", "X Branch Offset", 0],
        ["yBranchOffset", "Y Branch Offset", -58]
    ]
};
const SHAPE_LABELS = {
    free: "Free Nodes",
    point: "Point",
    circle: "Circle",
    halfCircle: "Half Circle",
    arc: "Arc",
    line: "Line",
    travelPath: "Travel Path",
    twoPathDeadEnd: "Two Path Dead End",
    branchedTravelPath: "Branched Travel Path"
};
const SHAPE_CONTEXT_ORDER = ["point", "circle", "halfCircle", "arc", "line", "travelPath", "twoPathDeadEnd", "branchedTravelPath", "free"];
const DEFAULT_GROUPS = [
    group("starter", "point", "Starter", "STARTER", "STARTER_POINT", 1, "STARTER", { x: 360, y: 300 }, ["STARTER"], { starter: true }),
    group("start_ring", "circle", "Kinetic Foundation", "START", "START_RING", 2, "NODE", { centerX: 360, centerY: 300, radius: 74, startDegrees: -120 }, ["EFFICIENT_DRIVE", "JAW_ALIGNMENT"]),
    group("efficiency_cluster", "circle", "Energy Efficiency", "EFFICIENCY", "EFFICIENCY_CLUSTER", 5, "NODE", { centerX: 285, centerY: 178, radius: 52, startDegrees: 200 }, ["INDUCTION_COILS", "SOFT_STARTER", "CURRENT_TRIM", "STABLE_INTAKE", "LOW_LOSS_BELT"]),
    group("capacity_cluster", "circle", "Energy Capacity", "CAPACITY", "CAPACITY_CLUSTER", 5, "NODE", { centerX: 507, centerY: 178, radius: 52, startDegrees: 200 }, ["COPPER_PLATES", "BUFFERED_DISCHARGE", "BUFFER_LAMINATE", "FRAME_RESERVOIR", "CAPACITOR_TRACE"]),
    group("output_cluster", "circle", "Output Yield", "OUTPUT", "OUTPUT_CLUSTER", 5, "NODE", { centerX: 507, centerY: 404, radius: 52, startDegrees: 160 }, ["ORE_SAMPLER", "SAMPLE_TRAY", "CLEAN_DUST_LEDGE", "ORE_SIEVE", "SIEVE_PLATES"]),
    group("speed_cluster", "circle", "Processing Speed", "SPEED", "SPEED_CLUSTER", 5, "NODE", { centerX: 285, centerY: 404, radius: 52, startDegrees: 200 }, ["KINETIC_LINKAGE", "HOPPER_RHYTHM", "MICRO_GEARING", "REINFORCED_FLYWHEEL", "KINETIC_OVERDRIVE"]),
    group("capacity_outer_path", "halfCircle", "Energy Capacity", "CAPACITY_OUTER", "CAPACITY_OUTER_PATH", 4, "NODE", { centerX: 610, centerY: 154, radius: 82, startDegrees: 165 }, ["RESERVE_CHANNELS", "CELL_CONTACTS", "MASSIVE_BUFFER", "RIGID_BUS_BAR"]),
    group("battery_cluster", "twoPathDeadEnd", "Energy Capacity", "BATTERY", "BATTERY_CLUSTER", 1, "KEYSTONE", { notableX: 760, notableY: 128, xStepFromNotable: -62, yStepFromNotable: 0, xBranchOffset: 0, yBranchOffset: 42 }, ["BATTERY_SHELL", "AUXILIARY_BANK", "CELL_BYPASS"]),
    group("efficiency_outer_path", "halfCircle", "Energy Efficiency", "EFFICIENCY_OUTER", "EFFICIENCY_OUTER_PATH", 4, "NODE", { centerX: 198, centerY: 154, radius: 82, startDegrees: -15 }, ["LOAD_SMOOTHING", "IDLE_DAMPERS", "RECOVERY_BUS", "INTERNAL_BUS"]),
    group("arc_pressure_cluster", "twoPathDeadEnd", "Energy Efficiency", "ARC_PRESSURE", "ARC_PRESSURE_CLUSTER", 1, "KEYSTONE", { notableX: 30, notableY: 145, xStepFromNotable: 62, yStepFromNotable: 0, xBranchOffset: 0, yBranchOffset: 42 }, ["ENERGY_GOVERNOR", "HARDENED_BEARINGS", "ARC_PRESSURE_DRIVE"]),
    group("output_outer_path", "halfCircle", "Output Yield", "OUTPUT_OUTER", "OUTPUT_OUTER_PATH", 4, "NODE", { centerX: 610, centerY: 430, radius: 82, startDegrees: 195 }, ["DUST_RECOVERY", "SORTING_RAKE", "MATERIAL_MEMORY", "FULL_SPECTRUM_SORTING"]),
    group("precision_cluster", "twoPathDeadEnd", "Output Yield", "PRECISION", "PRECISION_CLUSTER", 1, "KEYSTONE", { notableX: 770, notableY: 505, xStepFromNotable: -62, yStepFromNotable: 0, xBranchOffset: 0, yBranchOffset: 42 }, ["FALSE_TOOTH_SPACERS", "CALIBRATED_FEED", "PRECISION_JAW_MOUNT"]),
    group("center_bridges", "arc", "Control", "CENTER_BRIDGE", "CENTER_BRIDGES", 3, "NODE", { centerX: 360, centerY: 300, radius: 124, startDegrees: -70, sweepDegrees: 140 }, ["POWERED_SIEVE", "CHARGED_FLYWHEEL", "SCREENED_OVERDRIVE"]),
    group("speed_outer_path", "halfCircle", "Processing Speed", "SPEED_OUTER", "SPEED_OUTER_PATH", 4, "NODE", { centerX: 198, centerY: 430, radius: 82, startDegrees: -15 }, ["CRUSHING_MOMENTUM", "VENTED_OUTPUT_CHUTE", "HIGH_SPEED_BEARINGS", "COMPRESSION_RAMP"]),
    group("dense_cluster", "twoPathDeadEnd", "Processing Speed", "DENSE", "DENSE_CLUSTER", 1, "KEYSTONE", { notableX: 30, notableY: 505, xStepFromNotable: 62, yStepFromNotable: 0, xBranchOffset: 0, yBranchOffset: 42 }, ["BALANCED_THROW", "PARALLEL_FEED_RAIL", "DENSE_BATCHING"])
];

const passiveTreeState = {
    tree: null,
    drafts: [],
    activeDraftId: null,
    savedShapes: [],
    selectedGroupId: null,
    selectedNodeKey: null,
    selectedNodeKeys: new Set(),
    contextNodeKey: null,
    drag: null,
    selectionRect: null,
    connectionPreview: null,
    spaceHeld: false,
    undoStack: [],
    redoStack: [],
    restoring: false,
    sourceTrees: []
};
const ptEls = {};

document.addEventListener("DOMContentLoaded", initPassiveTreeEditor);

function initPassiveTreeEditor() {
    bindPassiveTreeElements();
    populateIdentitySelect();
    const draftCollection = loadStoredDraftCollection();
    passiveTreeState.drafts = draftCollection.drafts;
    passiveTreeState.activeDraftId = draftCollection.activeDraftId;
    passiveTreeState.tree = cloneTree(activeDraft().tree);
    passiveTreeState.savedShapes = loadStoredShapes();
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    bindPassiveTreeEvents();
    syncPassiveTreeDraftControls();
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
}

function bindPassiveTreeElements() {
    for (const id of [
        "passiveTreeIdInput",
        "passiveTreeDraftSelect",
        "passiveTreeDraftNameInput",
        "passiveTreePackageInput",
        "passiveTreeClassInput",
        "passiveTreeCanvasInput",
        "passiveTreeSourceSelect",
        "passiveTreeLoadSource",
        "passiveTreeSaveTree",
        "passiveTreeNewDraft",
        "passiveTreeDuplicateDraft",
        "passiveTreeDeleteDraft",
        "passiveTreeResetTree",
        "passiveTreeShapeMeta",
        "passiveTreeShapeKind",
        "passiveTreeNodeCount",
        "passiveTreeIdentitySelect",
        "passiveTreeIdentityNegative",
        "passiveTreeCustomIdentity",
        "passiveTreeSecondaryIdentitySelect",
        "passiveTreeSecondaryIdentityNegative",
        "passiveTreeSecondaryCustomIdentity",
        "passiveTreeNodePrefix",
        "passiveTreeShapeName",
        "passiveTreeNodeKind",
        "passiveTreeShapeParams",
        "passiveTreeNodeNames",
        "passiveTreeAddShape",
        "passiveTreeUpdateShape",
        "passiveTreeDeleteShape",
        "passiveTreeDeshapeSelected",
        "passiveTreeRegenerateNames",
        "passiveTreeSaveShape",
        "passiveTreeSavedShapes",
        "passiveTreeStatus",
        "passiveTreeCanvasWrap",
        "passiveTreeCanvas",
        "passiveTreeNodeContextMenu",
        "passiveTreeCanvasMeta",
        "passiveTreeGroupCount",
        "passiveTreeGroupList",
        "passiveTreeNodeProperties",
        "passiveTreePreviewMeta",
        "passiveTreeVisualAudit",
        "passiveTreeVisualAuditMetrics",
        "passiveTreePreviewMetrics",
        "passiveTreePreviewShapeSummary",
        "passiveTreePreviewBody",
        "passiveTreePreviewConnectionsBody",
        "passiveTreeCopyJava",
        "passiveTreeJavaOutput",
        "passiveTreeCopyJson",
        "passiveTreeJsonOutput"
    ]) {
        ptEls[id] = document.getElementById(id);
    }
}

function populateIdentitySelect() {
    ptEls.passiveTreeIdentitySelect.innerHTML = identitySelectOptions("Processing Speed", {
        includeStarter: true
    });
    if (ptEls.passiveTreeSecondaryIdentitySelect) {
        ptEls.passiveTreeSecondaryIdentitySelect.innerHTML = identitySelectOptions(SECONDARY_IDENTITY_NONE, {
            includeNone: true
        });
    }
}

function bindPassiveTreeEvents() {
    ptEls.passiveTreeDraftSelect.addEventListener("change", () => {
        switchPassiveTreeDraft(ptEls.passiveTreeDraftSelect.value);
    });
    ptEls.passiveTreeDraftNameInput.addEventListener("change", () => {
        renameActivePassiveTreeDraft();
    });
    for (const input of [
        ptEls.passiveTreeIdInput,
        ptEls.passiveTreePackageInput,
        ptEls.passiveTreeClassInput,
        ptEls.passiveTreeCanvasInput
    ]) {
        input.addEventListener("change", () => {
            const before = snapshotPassiveTreeState();
            updatePassiveTreeHeaderFromInputs();
            commitPassiveTreeHistory(before);
            renderPassiveTreeEditor();
        });
    }
    ptEls.passiveTreeShapeKind.addEventListener("change", () => {
        const shapeKind = ptEls.passiveTreeShapeKind.value;
        ptEls.passiveTreeNodeCount.value = clampedNodeCount(shapeKind, ptEls.passiveTreeNodeCount.value);
        renderShapeParams(shapeKind, defaultParams(shapeKind));
        regenerateFormNodeNames();
        updateShapeMeta();
    });
    ptEls.passiveTreeShapeParams.addEventListener("input", updateShapeMeta);
    ptEls.passiveTreeIdentitySelect.addEventListener("change", () => {
        syncFormIdentityCustomInputs();
        const identity = currentIdentity();
        const prefix = toConstantName(identity, "PASSIVE_NODE");
        ptEls.passiveTreeNodePrefix.value = prefix;
        ptEls.passiveTreeShapeName.value = uniqueShapeName(`${prefix}_CLUSTER`);
        regenerateFormNodeNames();
        updateShapeMeta();
    });
    ptEls.passiveTreeIdentityNegative?.addEventListener("change", updateShapeMeta);
    ptEls.passiveTreeCustomIdentity.addEventListener("change", () => {
        if (ptEls.passiveTreeIdentitySelect.value === "Custom") {
            const prefix = toConstantName(currentIdentity(), "PASSIVE_NODE");
            ptEls.passiveTreeNodePrefix.value = prefix;
            ptEls.passiveTreeShapeName.value = uniqueShapeName(`${prefix}_CLUSTER`);
            regenerateFormNodeNames();
        }
        updateShapeMeta();
    });
    ptEls.passiveTreeSecondaryIdentitySelect?.addEventListener("change", () => {
        syncFormIdentityCustomInputs();
        updateShapeMeta();
    });
    ptEls.passiveTreeSecondaryIdentityNegative?.addEventListener("change", updateShapeMeta);
    ptEls.passiveTreeSecondaryCustomIdentity?.addEventListener("change", updateShapeMeta);
    ptEls.passiveTreeNodeCount.addEventListener("change", () => {
        ptEls.passiveTreeNodeCount.value = clampedNodeCount(ptEls.passiveTreeShapeKind.value, ptEls.passiveTreeNodeCount.value);
        regenerateFormNodeNames();
        updateShapeMeta();
    });
    ptEls.passiveTreeNodePrefix.addEventListener("change", regenerateFormNodeNames);
    ptEls.passiveTreeShapeName.addEventListener("input", updateShapeMeta);
    ptEls.passiveTreeNodeKind.addEventListener("change", updateShapeMeta);
    ptEls.passiveTreeNodeNames.addEventListener("input", updateShapeMeta);
    ptEls.passiveTreeRegenerateNames.addEventListener("click", regenerateFormNodeNames);
    ptEls.passiveTreeAddShape.addEventListener("click", addPassiveTreeGroup);
    ptEls.passiveTreeUpdateShape.addEventListener("click", updateSelectedPassiveTreeGroup);
    ptEls.passiveTreeDeleteShape.addEventListener("click", deleteSelectedPassiveTreeGroup);
    ptEls.passiveTreeDeshapeSelected.addEventListener("click", deshapeSelectedPassiveTreeGroup);
    ptEls.passiveTreeSaveShape.addEventListener("click", saveCurrentShapeTemplate);
    ptEls.passiveTreeSaveTree.addEventListener("click", savePassiveTree);
    ptEls.passiveTreeNewDraft.addEventListener("click", () => createPassiveTreeDraft(false));
    ptEls.passiveTreeDuplicateDraft.addEventListener("click", () => createPassiveTreeDraft(true));
    ptEls.passiveTreeDeleteDraft.addEventListener("click", deleteActivePassiveTreeDraft);
    ptEls.passiveTreeResetTree.addEventListener("click", resetPassiveTree);
    ptEls.passiveTreeLoadSource.addEventListener("click", loadSelectedPassiveTreeSource);
    ptEls.passiveTreeCopyJava.addEventListener("click", () =>
        copyPassiveTreeText(ptEls.passiveTreeJavaOutput.value, "Copied Java layout."));
    ptEls.passiveTreeCopyJson.addEventListener("click", () =>
        copyPassiveTreeText(ptEls.passiveTreeJsonOutput.value, "Copied JSON brief."));
    ptEls.passiveTreeGroupList.addEventListener("click", handleGroupListClick);
    ptEls.passiveTreeNodeProperties.addEventListener("change", updateSelectedNodeFromProperties);
    ptEls.passiveTreeNodeProperties.addEventListener("click", handleSelectedNodeAction);
    ptEls.passiveTreeSavedShapes.addEventListener("click", handleSavedShapeClick);
    ptEls.passiveTreeCanvas.addEventListener("pointerdown", handlePassiveTreeCanvasPointerDown);
    ptEls.passiveTreeCanvas.addEventListener("pointermove", handlePassiveTreeCanvasPointerMove);
    ptEls.passiveTreeCanvas.addEventListener("pointerup", handlePassiveTreeCanvasPointerUp);
    ptEls.passiveTreeCanvas.addEventListener("pointerleave", handlePassiveTreeCanvasPointerUp);
    ptEls.passiveTreeCanvas.addEventListener("dblclick", handlePassiveTreeCanvasDoubleClick);
    ptEls.passiveTreeCanvas.addEventListener("contextmenu", handlePassiveTreeCanvasContextMenu);
    ptEls.passiveTreeCanvas.setAttribute("tabindex", "0");
    ptEls.passiveTreeNodeContextMenu.addEventListener("change", handlePassiveTreeContextMenuChange);
    ptEls.passiveTreeNodeContextMenu.addEventListener("click", handlePassiveTreeContextMenuClick);
    loadPassiveTreeSourceCatalog();
    document.addEventListener("click", handlePassiveTreeDocumentClick);
    document.addEventListener("keydown", handlePassiveTreeKeyDown);
    document.addEventListener("keyup", handlePassiveTreeKeyUp);
}

function loadStoredShapes() {
    const stored = readJsonStorage(PASSIVE_TREE_SHAPES_KEY);
    return Array.isArray(stored) ? stored.map(normalizeShapeTemplate).filter(Boolean) : [];
}

function loadStoredDraftCollection() {
    const stored = readJsonStorage(PASSIVE_TREE_STORAGE_KEY);
    if (PASSIVE_TREE_LEGACY_SCHEMAS.has(stored?.schema) && Array.isArray(stored.drafts)) {
        return normalizeDraftCollection(stored);
    }
    const legacy = PASSIVE_TREE_LEGACY_SCHEMAS.has(stored?.schema)
        ? stored
        : readJsonStorage(PASSIVE_TREE_LEGACY_STORAGE_KEY);
    if (PASSIVE_TREE_LEGACY_SCHEMAS.has(legacy?.schema) || legacy?.treeId) {
        return normalizeDraftCollection({
            schema: PASSIVE_TREE_SCHEMA,
            activeDraftId: "crusher_default",
            drafts: [
                {
                    id: "crusher_default",
                    name: "Crusher Default",
                    tree: legacy
                }
            ]
        });
    }
    return defaultDraftCollection();
}

function normalizeDraftCollection(collection) {
    const drafts = [];
    for (const entry of collection.drafts ?? []) {
        const draft = normalizeDraft(entry, drafts);
        if (draft) {
            drafts.push(draft);
        }
    }
    if (!drafts.length) {
        return defaultDraftCollection();
    }
    const activeDraftId = drafts.some((draft) => draft.id === collection.activeDraftId)
        ? collection.activeDraftId
        : drafts[0].id;
    return { activeDraftId, drafts };
}

function normalizeDraft(entry, existingDrafts = passiveTreeState.drafts) {
    if (!entry) {
        return null;
    }
    const tree = normalizeTree(entry.tree ?? entry);
    return {
        id: uniqueDraftId(entry.id || entry.name || tree.treeId, existingDrafts),
        name: normalizedDraftName(entry.name, tree.treeId),
        createdAt: entry.createdAt || entry.updatedAt || new Date().toISOString(),
        updatedAt: entry.updatedAt || new Date().toISOString(),
        createdFromDraftId: entry.createdFromDraftId ?? null,
        tree
    };
}

function syncPassiveTreeHeader() {
    ptEls.passiveTreeIdInput.value = passiveTreeState.tree.treeId;
    ptEls.passiveTreePackageInput.value = passiveTreeState.tree.packageName;
    ptEls.passiveTreeClassInput.value = passiveTreeState.tree.className;
    ptEls.passiveTreeCanvasInput.value = `${passiveTreeState.tree.canvas.width}x${passiveTreeState.tree.canvas.height}`;
}

function syncPassiveTreeDraftControls() {
    ptEls.passiveTreeDraftSelect.innerHTML = passiveTreeState.drafts
        .map((draft) => `<option value="${escapeHtml(draft.id)}">${escapeHtml(draft.name)}</option>`)
        .join("");
    ptEls.passiveTreeDraftSelect.value = passiveTreeState.activeDraftId;
    ptEls.passiveTreeDraftNameInput.value = activeDraft().name;
}

function updatePassiveTreeHeaderFromInputs() {
    passiveTreeState.tree.treeId = normalizedIdentifier(ptEls.passiveTreeIdInput.value, "crusher");
    passiveTreeState.tree.packageName = ptEls.passiveTreePackageInput.value.trim() || DEFAULT_PACKAGE;
    passiveTreeState.tree.className = normalizedClassName(ptEls.passiveTreeClassInput.value, DEFAULT_CLASS);
    passiveTreeState.tree.canvas = parseCanvasSize(ptEls.passiveTreeCanvasInput.value);
    syncPassiveTreeHeader();
}

function switchPassiveTreeDraft(draftId) {
    const target = passiveTreeState.drafts.find((draft) => draft.id === draftId);
    if (!target) {
        return;
    }
    persistActiveDraft(false);
    passiveTreeState.activeDraftId = target.id;
    passiveTreeState.tree = cloneTree(target.tree);
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    clearPassiveTreeHistory();
    writeDraftCollection();
    syncPassiveTreeDraftControls();
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Loaded draft ${target.name}.`);
}

function renameActivePassiveTreeDraft() {
    const draft = activeDraft();
    draft.name = normalizedDraftName(ptEls.passiveTreeDraftNameInput.value, passiveTreeState.tree.treeId);
    draft.updatedAt = new Date().toISOString();
    writeDraftCollection();
    syncPassiveTreeDraftControls();
    setPassiveTreeStatus(`Renamed draft to ${draft.name}.`);
}

function createPassiveTreeDraft(duplicateCurrent) {
    persistActiveDraft(false);
    const currentDraft = activeDraft();
    const sourceTree = duplicateCurrent
        ? cloneTree(passiveTreeState.tree)
        : emptyDraftTree(passiveTreeState.tree);
    const nameBase = duplicateCurrent ? `${currentDraft.name} Copy` : `${machineLabel(passiveTreeState.tree.treeId)} Draft`;
    const draft = {
        id: uniqueDraftId(nameBase),
        name: uniqueDraftName(nameBase),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        createdFromDraftId: duplicateCurrent ? currentDraft.id : null,
        tree: sourceTree
    };
    passiveTreeState.drafts.push(draft);
    passiveTreeState.activeDraftId = draft.id;
    passiveTreeState.tree = cloneTree(draft.tree);
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    clearPassiveTreeHistory();
    writeDraftCollection();
    syncPassiveTreeDraftControls();
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`${duplicateCurrent ? "Duplicated" : "Created"} draft ${draft.name}.`);
}

function deleteActivePassiveTreeDraft() {
    if (passiveTreeState.drafts.length <= 1) {
        setPassiveTreeStatus("Keep at least one draft for the machine.", true);
        return;
    }
    const deleted = activeDraft();
    passiveTreeState.drafts = passiveTreeState.drafts.filter((draft) => draft.id !== deleted.id);
    passiveTreeState.activeDraftId = passiveTreeState.drafts[0].id;
    passiveTreeState.tree = cloneTree(activeDraft().tree);
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    clearPassiveTreeHistory();
    writeDraftCollection();
    syncPassiveTreeDraftControls();
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Deleted draft ${deleted.name}.`);
}

function persistActiveDraft(write = true) {
    updatePassiveTreeHeaderFromInputs();
    const draft = activeDraft();
    draft.name = normalizedDraftName(ptEls.passiveTreeDraftNameInput.value, passiveTreeState.tree.treeId);
    draft.tree = cloneTree(passiveTreeState.tree);
    draft.createdAt = draft.createdAt || new Date().toISOString();
    draft.updatedAt = new Date().toISOString();
    if (write) {
        writeDraftCollection();
    }
}

function syncPassiveTreeFormFromSelected() {
    const selected = selectedGroup();
    const source = selected ?? group(
        nextGroupId(),
        "circle",
        "Processing Speed",
        "PROCESSING_SPEED",
        uniqueShapeName("PROCESSING_SPEED_CLUSTER"),
        5,
        "NODE",
        defaultParams("circle"),
        suggestedNodeNames("PROCESSING_SPEED", 5, "circle")
    );
    ptEls.passiveTreeShapeKind.value = source.shapeKind;
    ptEls.passiveTreeNodeCount.value = source.nodeCount;
    setIdentityControls(source);
    ptEls.passiveTreeNodePrefix.value = source.nodePrefix;
    ptEls.passiveTreeShapeName.value = source.shapeName;
    ptEls.passiveTreeNodeKind.value = NODE_KINDS.includes(source.nodeKind) ? source.nodeKind : "NODE";
    renderShapeParams(source.shapeKind, source.params);
    ptEls.passiveTreeNodeNames.value = normalizedNodeNames(source).join("\n");
    updateShapeMeta();
}

function setIdentityControls(source) {
    const state = normalizeIdentityState(
            source.identity,
            source.identityNegative,
            source.secondaryIdentity,
            source.secondaryIdentityNegative
    );
    setIdentitySelectControls(
            ptEls.passiveTreeIdentitySelect,
            ptEls.passiveTreeCustomIdentity,
            state.identity,
            { includeStarter: true }
    );
    if (ptEls.passiveTreeIdentityNegative) {
        ptEls.passiveTreeIdentityNegative.checked = state.identityNegative;
    }
    if (ptEls.passiveTreeSecondaryIdentitySelect && ptEls.passiveTreeSecondaryCustomIdentity) {
        setIdentitySelectControls(
                ptEls.passiveTreeSecondaryIdentitySelect,
                ptEls.passiveTreeSecondaryCustomIdentity,
                state.secondaryIdentity,
                { includeNone: true }
        );
    }
    if (ptEls.passiveTreeSecondaryIdentityNegative) {
        ptEls.passiveTreeSecondaryIdentityNegative.checked = Boolean(state.secondaryIdentity)
                && state.secondaryIdentityNegative;
    }
    syncFormIdentityCustomInputs();
}

function setIdentitySelectControls(select, customInput, identity, options = {}) {
    if (IDENTITY_PRESETS.includes(identity) && identity !== "Custom") {
        select.innerHTML = identitySelectOptions(identity, options);
        select.value = identity;
        customInput.value = "";
        customInput.disabled = true;
        return;
    }
    if (!identity && options.includeNone) {
        select.innerHTML = identitySelectOptions(SECONDARY_IDENTITY_NONE, options);
        select.value = SECONDARY_IDENTITY_NONE;
        customInput.value = "";
        customInput.disabled = true;
        return;
    }
    select.innerHTML = identitySelectOptions(identity, options);
    select.value = "Custom";
    customInput.value = identity === "Custom" ? "" : identity;
    customInput.disabled = false;
}

function syncFormIdentityCustomInputs() {
    ptEls.passiveTreeCustomIdentity.disabled = ptEls.passiveTreeIdentitySelect.value !== "Custom";
    if (!ptEls.passiveTreeSecondaryIdentitySelect || !ptEls.passiveTreeSecondaryCustomIdentity) {
        return;
    }
    ptEls.passiveTreeSecondaryCustomIdentity.disabled = ptEls.passiveTreeSecondaryIdentitySelect.value !== "Custom";
    if (!ptEls.passiveTreeSecondaryIdentitySelect.value && ptEls.passiveTreeSecondaryIdentityNegative) {
        ptEls.passiveTreeSecondaryIdentityNegative.checked = false;
    }
}

function identitySelectOptions(currentIdentity, options = {}) {
    const entries = [];
    if (options.includeNone) {
        entries.push([SECONDARY_IDENTITY_NONE, "None"]);
    }
    const presets = IDENTITY_PRESETS.filter((identity) =>
        options.includeStarter || identity !== "Starter");
    const hasCurrentPreset = presets.includes(currentIdentity);
    if (options.includeCurrent
            && currentIdentity
            && !hasCurrentPreset
            && currentIdentity !== "Custom") {
        entries.push([currentIdentity, currentIdentity]);
    }
    for (const identity of presets) {
        entries.push([identity, identity]);
    }
    return entries
        .map(([value, label]) => `<option value="${escapeHtml(value)}" ${value === currentIdentity ? "selected" : ""}>${escapeHtml(label)}</option>`)
        .join("");
}

function renderSignedIdentityControls(datasetName, state, disabled = false) {
    const disabledAttribute = disabled ? "disabled" : "";
    const primaryCustom = IDENTITY_PRESETS.includes(state.identity) && state.identity !== "Custom"
        ? ""
        : state.identity === "Custom" ? "" : state.identity;
    const secondaryCustom = !state.secondaryIdentity
            || IDENTITY_PRESETS.includes(state.secondaryIdentity) && state.secondaryIdentity !== "Custom"
        ? ""
        : state.secondaryIdentity === "Custom" ? "" : state.secondaryIdentity;
    return `
        <label>
          Primary
          <select data-${datasetName}="identity" ${disabledAttribute}>
            ${identitySelectOptions(state.identity, { includeStarter: true, includeCurrent: true })}
          </select>
        </label>
        <label class="toggle-label">
          <input data-${datasetName}="identityNegative" type="checkbox" ${state.identityNegative ? "checked" : ""} ${disabledAttribute}>
          Primary Negative
        </label>
        <label class="property-wide">
          Primary Custom
          <input data-${datasetName}="customIdentity" type="text" value="${escapeHtml(primaryCustom)}" ${state.identity === "Custom" || primaryCustom ? "" : "disabled"} ${disabledAttribute}>
        </label>
        <label>
          Secondary
          <select data-${datasetName}="secondaryIdentity" ${disabledAttribute}>
            ${identitySelectOptions(state.secondaryIdentity, { includeNone: true, includeCurrent: true })}
          </select>
        </label>
        <label class="toggle-label">
          <input data-${datasetName}="secondaryIdentityNegative" type="checkbox" ${state.secondaryIdentityNegative ? "checked" : ""} ${disabledAttribute}>
          Secondary Negative
        </label>
        <label class="property-wide">
          Secondary Custom
          <input data-${datasetName}="customSecondaryIdentity" type="text" value="${escapeHtml(secondaryCustom)}" ${state.secondaryIdentity === "Custom" || secondaryCustom ? "" : "disabled"} ${disabledAttribute}>
        </label>
    `;
}

function renderShapeParams(shapeKind, params) {
    if (shapeKind === "free") {
        ptEls.passiveTreeShapeParams.innerHTML = `<p class="muted property-wide">Deshaped nodes are positioned by dragging individual nodes on the canvas.</p>`;
        return;
    }
    const entries = SHAPE_PARAMS[shapeKind] ?? SHAPE_PARAMS.circle;
    ptEls.passiveTreeShapeParams.innerHTML = entries
        .map(([key, label, fallback]) => `
            <label>
              ${escapeHtml(label)}
              <input data-passive-tree-param="${escapeHtml(key)}" type="number" step="1" value="${escapeHtml(String(params[key] ?? fallback))}">
            </label>
        `)
        .join("");
}

function renderPassiveTreeEditor() {
    const exportJson = buildPassiveTreeExport();
    ptEls.passiveTreeJsonOutput.value = JSON.stringify(exportJson, null, 2);
    ptEls.passiveTreeJavaOutput.value = generateJavaLayout();
    renderPassiveTreeCanvas();
    renderPassiveTreeVisualAudit();
    renderPassiveTreeGroups();
    renderSelectedNodeProperties();
    renderSavedShapes();
    updateShapeMeta();
}

async function loadPassiveTreeSourceCatalog() {
    try {
        const response = await fetch(PASSIVE_TREE_SOURCE_URL, { cache: "no-store" });
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const payload = await response.json();
        passiveTreeState.sourceTrees = Array.isArray(payload.trees) ? payload.trees : [];
        ptEls.passiveTreeSourceSelect.innerHTML = passiveTreeState.sourceTrees.length
            ? passiveTreeState.sourceTrees.map((entry) => `<option value="${escapeHtml(entry.id)}">${escapeHtml(entry.label)} - ${entry.sourceFormat === "catalog" ? "JSON" : "Java"}</option>`).join("")
            : `<option value="">No typed Java trees found</option>`;
        ptEls.passiveTreeLoadSource.disabled = passiveTreeState.sourceTrees.length === 0;
    } catch (error) {
        passiveTreeState.sourceTrees = [];
        ptEls.passiveTreeSourceSelect.innerHTML = `<option value="">Java source load failed</option>`;
        ptEls.passiveTreeLoadSource.disabled = true;
        setPassiveTreeStatus(`Could not load Java passive trees: ${error.message}`, true);
    }
}

function loadSelectedPassiveTreeSource() {
    const source = passiveTreeState.sourceTrees.find((entry) => entry.id === ptEls.passiveTreeSourceSelect.value);
    if (!source?.tree) {
        setPassiveTreeStatus("Select an exported Java passive tree first.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    passiveTreeState.tree = normalizeTree(source.tree);
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    passiveTreeState.selectedNodeKey = null;
    passiveTreeState.selectedNodeKeys.clear();
    commitPassiveTreeHistory(before);
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Loaded ${source.label} from ${source.sourcePath}. Save Draft to keep an editable copy.`);
}

function renderPassiveTreeCanvas() {
    const canvas = passiveTreeState.tree.canvas;
    const groups = passiveTreeState.tree.groups.map((entry) => ({
        group: entry,
        points: computeGroupPoints(entry)
    }));
    const pointLookup = buildPointLookup(groups);
    ptEls.passiveTreeCanvas.setAttribute("viewBox", `0 0 ${canvas.width} ${canvas.height}`);
    ptEls.passiveTreeCanvas.setAttribute("width", String(canvas.width));
    ptEls.passiveTreeCanvas.setAttribute("height", String(canvas.height));
    ptEls.passiveTreeCanvasMeta.textContent = `${canvas.width}x${canvas.height} / ${totalNodeCount()} nodes`;
    ptEls.passiveTreeCanvas.innerHTML = [
        renderGrid(canvas.width, canvas.height),
        renderIdentityDefs(groups),
        renderExternalConnections(pointLookup),
        ...groups.flatMap(({ group: entry, points }) => [
            renderLinks(entry, points),
            renderNodes(entry, points)
        ]),
        renderConnectionPreview(),
        renderSelectionRect()
    ].join("");
}

function renderPassiveTreeVisualAudit() {
    if (!ptEls.passiveTreeVisualAudit) {
        return;
    }
    const graph = passiveTreeVisualGraph();
    if (!graph.nodes.length) {
        ptEls.passiveTreeVisualAudit.innerHTML = "";
        ptEls.passiveTreeVisualAuditMetrics.innerHTML = auditMetric("Visual Audit", "Empty", "Add nodes before auditing", false);
        return;
    }
    const bounds = pointBounds(graph.nodes.map((entry) => entry.point));
    const scale = Math.max(1, bounds.width / 1050, bounds.height / 620);
    const padding = Math.max(48 * scale, 60);
    const viewBox = {
        x: bounds.minX - padding,
        y: bounds.minY - padding,
        width: bounds.width + padding * 2,
        height: bounds.height + padding * 2
    };
    const routeEdges = graph.edges.filter((entry) => !entry.endpointCommitment);
    const edgeLengths = routeEdges.map((entry) => entry.length).sort((first, second) => first - second);
    const medianLength = percentile(edgeLengths, 0.5);
    const p90Length = percentile(edgeLengths, 0.9);
    const multipleStarts = graph.nodes.filter(entry => entry.detail.kind === "STARTER").length > 1;
    const neighborhoodLengths = routeEdges.filter(entry => entry.from.groupId !== entry.to.groupId)
        .map(entry => entry.length).sort((a, b) => a - b);
    const longThreshold = Math.max(240, (multipleStarts ? percentile(neighborhoodLengths, 0.5) : medianLength) * 2.75);
    const longEdges = routeEdges.filter((entry) => entry.length > longThreshold);
    const crossings = properGraphCrossings(graph.edges);
    const centrality = starterCentrality(graph.nodes, bounds);
    const endpointSpread = passiveTreeEndpointSpread(graph.groups, graph.nodes);
    const crossingLimit = multipleStarts ? 0 : Math.max(3, Math.round(graph.edges.length * 0.02));
    const longEdgeLimit = Math.max(3, Math.round(graph.edges.length * 0.06));
    const crossingsPass = crossings.length <= crossingLimit;
    const longEdgesPass = longEdges.length <= longEdgeLimit && p90Length <= longThreshold;
    const centralityPass = centrality.x >= 0.3 && centrality.x <= 0.7 && centrality.y >= 0.3 && centrality.y <= 0.7;
    const endpointPass = endpointSpread.count < 3
        || endpointSpread.sectors >= 5 && endpointSpread.maximumGap <= 120;
    ptEls.passiveTreeVisualAudit.setAttribute("viewBox", `${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`);
    ptEls.passiveTreeVisualAudit.innerHTML = [
        `<defs><radialGradient id="passiveTreeAuditBackdrop"><stop offset="0%" stop-color="#1b251d"></stop><stop offset="62%" stop-color="#0d1415"></stop><stop offset="100%" stop-color="#070a0d"></stop></radialGradient></defs>`,
        `<rect x="${viewBox.x}" y="${viewBox.y}" width="${viewBox.width}" height="${viewBox.height}" fill="url(#passiveTreeAuditBackdrop)"></rect>`,
        multipleStarts ? "" : renderPassiveTreeAuditRings(graph.nodes, bounds),
        graph.edges.map((entry) => renderPassiveTreeAuditLink(entry, longThreshold)).join(""),
        graph.nodes.map((entry) => renderPassiveTreeAuditNode(entry)).join(""),
        crossings.slice(0, 200).map((entry) => `<circle class="passive-tree-audit-crossing" cx="${entry.x}" cy="${entry.y}" r="${3.2 * scale}"></circle>`).join("")
    ].join("");
    const overallPass = crossingsPass && longEdgesPass && centralityPass && endpointPass;
    ptEls.passiveTreeVisualAuditMetrics.innerHTML = [
        auditMetric("Visual Gate", overallPass ? "PASS" : "FAIL", overallPass ? "Composition clears all fitted-view gates" : "Red markers require layout work", overallPass),
        auditMetric("Proper Crossings", crossings.length, `Limit ${crossingLimit}`, crossingsPass),
        auditMetric("Long Chords", longEdges.length, `>${Math.round(longThreshold)} px; limit ${longEdgeLimit}`, longEdgesPass),
        auditMetric("Edge P90 / Median", `${Math.round(p90Length)} / ${Math.round(medianLength)}`, `${(p90Length / Math.max(1, medianLength)).toFixed(2)}x`, longEdgesPass),
        auditMetric("Starter Position", `${Math.round(centrality.x * 100)}% / ${Math.round(centrality.y * 100)}%`, "Target 30-70% on both axes", centralityPass),
        auditMetric("Endpoint Spread", endpointSpread.count ? `${endpointSpread.sectors}/8 sectors` : "N/A", endpointSpread.count ? `${Math.round(endpointSpread.maximumGap)}° maximum gap` : "No endpoint groups", endpointPass)
    ].join("");
    ptEls.passiveTreePreviewMeta.textContent = `${graph.nodes.length} nodes / ${graph.edges.length} links / ${crossings.length} crossings`;
}

function passiveTreeVisualGraph() {
    const groups = passiveTreeState.tree.groups.map((entry) => ({
        group: entry,
        points: computeGroupPoints(entry)
    }));
    const nodes = groups.flatMap(({ group: entry, points }) => points.map((point, index) => ({
        key: nodeKey(entry.id, index),
        groupId: entry.id,
        index,
        point,
        detail: nodeDetail(entry, index)
    })));
    const lookup = new Map(nodes.map((entry) => [entry.key, entry]));
    const groupLookup = new Map(groups.map((entry) => [entry.group.id, entry]));
    const edges = new Map();
    const addEdge = (from, to, kind) => {
        if (!from || !to || from.key === to.key) {
            return;
        }
        const key = from.key < to.key ? `${from.key}|${to.key}` : `${to.key}|${from.key}`;
        const sourceGroup = groupLookup.get(from.groupId);
        const path = from.groupId === to.groupId
            ? passiveTreeLinkPath(sourceGroup.group, from.index, to.index, sourceGroup.points)
            : [from.point, to.point];
        edges.set(key, {
            key,
            from,
            to,
            kind,
            path,
            length: path.slice(1).reduce((length, point, index) => length + Math.hypot(point.x - path[index].x, point.y - path[index].y), 0)
        });
    };
    for (const { group: entry, points } of groups) {
        for (const link of groupLinks(entry, points)) {
            addEdge(lookup.get(nodeKey(entry.id, link.fromIndex)), lookup.get(nodeKey(entry.id, link.toIndex)), "internal");
        }
    }
    for (const connection of passiveTreeState.tree.connections) {
        addEdge(
            lookup.get(nodeKey(connection.from.groupId, connection.from.nodeIndex)),
            lookup.get(nodeKey(connection.to.groupId, connection.to.nodeIndex)),
            connection.kind
        );
    }
    const edgeList = [...edges.values()];
    const endpointGroups = new Set(groups.filter(({ group: entry, points }) => entry.id.endsWith("_endpoint")
        || points.some((_, index) => nodeDetail(entry, index).kind === "KEYSTONE")).map(({ group: entry }) => entry.id));
    const groupNeighbors = new Map(groups.map(({ group: entry }) => [entry.id, new Set()]));
    for (const edge of edgeList) {
        if (edge.from.groupId !== edge.to.groupId) {
            groupNeighbors.get(edge.from.groupId)?.add(edge.to.groupId);
            groupNeighbors.get(edge.to.groupId)?.add(edge.from.groupId);
        }
    }
    for (const edge of edgeList) {
        edge.endpointCommitment = edge.from.groupId !== edge.to.groupId && (
            endpointGroups.has(edge.from.groupId) && groupNeighbors.get(edge.from.groupId)?.size === 1
            || endpointGroups.has(edge.to.groupId) && groupNeighbors.get(edge.to.groupId)?.size === 1
        );
    }
    return { groups, nodes, edges: edgeList };
}

function renderPassiveTreeAuditRings(nodes, bounds) {
    const center = nodes.find((entry) => entry.detail.alwaysAllocated || entry.detail.kind === "STARTER")?.point
        ?? { x: (bounds.minX + bounds.maxX) / 2, y: (bounds.minY + bounds.maxY) / 2 };
    const outerRadius = Math.max(bounds.width, bounds.height) * 0.65;
    const rings = [];
    for (let radius = 120; radius <= outerRadius; radius += 120) {
        rings.push(`<circle class="passive-tree-audit-orbit" cx="${center.x}" cy="${center.y}" r="${radius}"></circle>`);
        rings.push(`<circle class="passive-tree-audit-orbit etched" cx="${center.x}" cy="${center.y}" r="${radius + 5}"></circle>`);
    }
    return `<g aria-hidden="true">${rings.join("")}</g>`;
}

function renderPassiveTreeAuditLink(entry, longThreshold) {
    const longChord = !entry.endpointCommitment && entry.length > longThreshold;
    const path = passiveTreePathData(entry.path);
    return `<g><title>${escapeHtml(entry.from.detail.name)} to ${escapeHtml(entry.to.detail.name)} - ${Math.round(entry.length)} px${entry.endpointCommitment ? " endpoint commitment" : ""}</title><path class="passive-tree-audit-link-shadow" d="${path}"></path><path class="passive-tree-audit-link${longChord ? " long-chord" : ""}" d="${path}"></path></g>`;
}

function passiveTreePathData(points) {
    return points.map((point, index) => `${index ? "L" : "M"} ${point.x} ${point.y}`).join(" ");
}

function passiveTreeLinkPath(group, fromIndex, toIndex, points = computeGroupPoints(group)) {
    const from = points[fromIndex];
    const to = points[toIndex];
    const straight = [from, to];
    const source = group.source;
    if (!source?.center || source.placements?.length !== points.length) {
        return straight;
    }
    const original = points.map((_, index) => source.placements.find((placement) => placement.nodeId === nodeDetail(group, index).name)
        ?? source.placements[index]);
    if (original.some((point) => !Number.isFinite(point?.x) || !Number.isFinite(point?.y))) {
        return straight;
    }
    const offsetX = points[0].x - original[0].x;
    const offsetY = points[0].y - original[0].y;
    if (points.some((point, index) => Math.abs(point.x - original[index].x - offsetX) > 0.001
            || Math.abs(point.y - original[index].y - offsetY) > 0.001)) {
        return straight;
    }
    const fromPlacement = original[fromIndex];
    const toPlacement = original[toIndex];
    if (!fromPlacement.orbit || fromPlacement.orbit !== toPlacement.orbit) {
        return straight;
    }
    const center = { x: source.center.x + offsetX, y: source.center.y + offsetY };
    const angleFrom = (point) => Math.atan2(point.y - center.y, point.x - center.x);
    const signedAngle = (angle) => Math.atan2(Math.sin(angle), Math.cos(angle));
    const fromAngle = angleFrom(from);
    const sweep = signedAngle(angleFrom(to) - fromAngle);
    if (Math.abs(sweep) > Math.PI * 2 / 3 + 1e-9) {
        return straight;
    }
    for (let index = 0; index < points.length; index++) {
        if (index === fromIndex || index === toIndex || original[index].orbit !== fromPlacement.orbit) {
            continue;
        }
        const offset = signedAngle(angleFrom(points[index]) - fromAngle);
        if (offset * sweep > 0 && Math.abs(offset) < Math.abs(sweep) - 1e-9) {
            return straight;
        }
    }
    const steps = Math.max(2, Math.ceil(Math.abs(sweep) / (Math.PI / 24)));
    const fromRadius = Math.hypot(from.x - center.x, from.y - center.y);
    const toRadius = Math.hypot(to.x - center.x, to.y - center.y);
    const path = [from];
    for (let index = 1; index < steps; index++) {
        const progress = index / steps;
        const angle = fromAngle + sweep * progress;
        const radius = fromRadius + (toRadius - fromRadius) * progress;
        path.push({ x: center.x + Math.round(Math.cos(angle) * radius), y: center.y + Math.round(Math.sin(angle) * radius) });
    }
    path.push(to);
    return path;
}

function renderPassiveTreeAuditNode(entry) {
    const kind = String(entry.detail.kind || "NODE").toLowerCase();
    const radius = nodeRadius(entry.detail.kind, entry.detail.size);
    const color = signedIdentityColor(entry.detail.identity, entry.detail.identityNegative);
    const major = ["starter", "notable", "keystone"].includes(kind);
    const glyphRadius = radius * (major ? 0.38 : 0.28);
    return `<g class="passive-tree-audit-node ${kind}${entry.detail.alwaysAllocated ? " starter" : ""}" transform="translate(${entry.point.x} ${entry.point.y})" style="--node-color: ${escapeHtml(color)}">
        <title>${escapeHtml(entry.detail.name)} - ${escapeHtml(titleCase(entry.detail.kind))} - ${escapeHtml(identitySummary(entry.detail))}</title>
        ${kind === "keystone" ? `<path class="passive-tree-audit-crown" d="M 0 ${-radius} L ${radius} 0 L 0 ${radius} L ${-radius} 0 Z"></path>` : ""}
        <circle class="passive-tree-audit-rim" r="${radius - (kind === "keystone" ? 2 : 0)}"></circle>
        <circle class="passive-tree-audit-core" r="${radius - (major ? 3 : 1.5)}"></circle>
        ${major ? `<circle class="passive-tree-audit-inner-ring" r="${Math.max(1, radius - 5)}"></circle>` : ""}
        <path class="passive-tree-audit-glyph" d="M 0 ${-glyphRadius} L ${glyphRadius} 0 L 0 ${glyphRadius} L ${-glyphRadius} 0 Z"></path>
    </g>`;
}

function properGraphCrossings(edges) {
    const crossings = [];
    for (let firstIndex = 0; firstIndex < edges.length; firstIndex++) {
        const first = edges[firstIndex];
        for (let secondIndex = firstIndex + 1; secondIndex < edges.length; secondIndex++) {
            const second = edges[secondIndex];
            if (first.from.key === second.from.key
                    || first.from.key === second.to.key
                    || first.to.key === second.from.key
                    || first.to.key === second.to.key) {
                continue;
            }
            const firstPath = first.path ?? [first.from.point, first.to.point];
            const secondPath = second.path ?? [second.from.point, second.to.point];
            let crossing;
            for (let firstSegment = 1; firstSegment < firstPath.length && !crossing; firstSegment++) {
                for (let secondSegment = 1; secondSegment < secondPath.length && !crossing; secondSegment++) {
                    crossing = properSegmentIntersection(firstPath[firstSegment - 1], firstPath[firstSegment], secondPath[secondSegment - 1], secondPath[secondSegment]);
                }
            }
            if (crossing) {
                crossings.push(crossing);
            }
        }
    }
    return crossings;
}

function properSegmentIntersection(firstStart, firstEnd, secondStart, secondEnd) {
    const firstDelta = { x: firstEnd.x - firstStart.x, y: firstEnd.y - firstStart.y };
    const secondDelta = { x: secondEnd.x - secondStart.x, y: secondEnd.y - secondStart.y };
    const denominator = firstDelta.x * secondDelta.y - firstDelta.y * secondDelta.x;
    if (Math.abs(denominator) < 0.00001) {
        return null;
    }
    const offset = { x: secondStart.x - firstStart.x, y: secondStart.y - firstStart.y };
    const firstProgress = (offset.x * secondDelta.y - offset.y * secondDelta.x) / denominator;
    const secondProgress = (offset.x * firstDelta.y - offset.y * firstDelta.x) / denominator;
    if (firstProgress <= 0 || firstProgress >= 1 || secondProgress <= 0 || secondProgress >= 1) {
        return null;
    }
    return {
        x: firstStart.x + firstDelta.x * firstProgress,
        y: firstStart.y + firstDelta.y * firstProgress
    };
}

function pointBounds(points) {
    const xs = points.map((entry) => entry.x);
    const ys = points.map((entry) => entry.y);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    return {
        minX,
        maxX,
        minY,
        maxY,
        width: Math.max(1, maxX - minX),
        height: Math.max(1, maxY - minY)
    };
}

function averagePoint(points) {
    return {
        x: points.reduce((sum, entry) => sum + entry.x, 0) / points.length,
        y: points.reduce((sum, entry) => sum + entry.y, 0) / points.length
    };
}

function percentile(sortedValues, progress) {
    if (!sortedValues.length) {
        return 0;
    }
    return sortedValues[Math.min(sortedValues.length - 1, Math.max(0, Math.ceil(sortedValues.length * progress) - 1))];
}

function starterCentrality(nodes, bounds) {
    const starter = nodes.find((entry) => entry.detail.alwaysAllocated) ?? nodes[0];
    return {
        x: (starter.point.x - bounds.minX) / bounds.width,
        y: (starter.point.y - bounds.minY) / bounds.height
    };
}

function passiveTreeEndpointSpread(groups, nodes) {
    const starter = nodes.find((entry) => entry.detail.alwaysAllocated) ?? nodes[0];
    const endpointGroups = groups.filter(({ group: entry, points }) => entry.id.endsWith("_endpoint")
        || points.some((_, index) => nodeDetail(entry, index).kind === "KEYSTONE"));
    const angles = endpointGroups.map(({ points }) => {
        const center = averagePoint(points);
        return (Math.atan2(center.y - starter.point.y, center.x - starter.point.x) * 180 / Math.PI + 360) % 360;
    }).sort((first, second) => first - second);
    const gaps = angles.map((angle, index) => (angles[(index + 1) % angles.length] - angle + 360) % 360);
    return {
        count: angles.length,
        sectors: new Set(angles.map((angle) => Math.floor(angle / 45) % 8)).size,
        maximumGap: gaps.length ? Math.max(...gaps) : 360
    };
}

function auditMetric(label, value, detail, pass) {
    return `<div class="metric ${pass ? "pass" : "fail"}"><strong>${escapeHtml(String(value))}</strong><span>${escapeHtml(label)} - ${escapeHtml(detail)}</span></div>`;
}

function renderGrid(width, height) {
    const lines = [];
    for (let x = 0; x <= width; x += 50) {
        lines.push(`<line class="passive-tree-grid-line" x1="${x}" y1="0" x2="${x}" y2="${height}"></line>`);
    }
    for (let y = 0; y <= height; y += 50) {
        lines.push(`<line class="passive-tree-grid-line" x1="0" y1="${y}" x2="${width}" y2="${y}"></line>`);
    }
    return `<g class="passive-tree-grid">${lines.join("")}</g>`;
}

function renderIdentityDefs(groups) {
    const gradients = [];
    for (const { group: entry, points } of groups) {
        points.forEach((_, index) => {
            const detail = nodeDetail(entry, index);
            if (!detail.secondaryIdentity) {
                return;
            }
            const primary = signedIdentityColor(detail.identity, detail.identityNegative);
            const secondary = signedIdentityColor(detail.secondaryIdentity, detail.secondaryIdentityNegative);
            gradients.push(`
                <linearGradient id="${escapeHtml(nodeGradientId(entry.id, index))}" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="${escapeHtml(primary)}"></stop>
                  <stop offset="49%" stop-color="${escapeHtml(primary)}"></stop>
                  <stop offset="51%" stop-color="${escapeHtml(secondary)}"></stop>
                  <stop offset="100%" stop-color="${escapeHtml(secondary)}"></stop>
                </linearGradient>
            `);
        });
    }
    return gradients.length ? `<defs>${gradients.join("")}</defs>` : "";
}

function renderLinks(entry, points) {
    const links = groupLinks(entry, points);
    if (!links.length) {
        return "";
    }
    const selected = entry.id === passiveTreeState.selectedGroupId ? " selected" : "";
    return `<g class="passive-tree-links${selected}" data-passive-tree-group-id="${escapeHtml(entry.id)}">${links
        .map((link) => `<path d="${passiveTreePathData(passiveTreeLinkPath(entry, link.fromIndex, link.toIndex, points))}" data-passive-tree-internal-link="true" data-passive-tree-group-id="${escapeHtml(entry.id)}" data-passive-tree-from-index="${link.fromIndex}" data-passive-tree-to-index="${link.toIndex}"></path>`)
        .join("")}</g>`;
}

function renderNodes(entry, points) {
    const selected = entry.id === passiveTreeState.selectedGroupId ? " selected" : "";
    return `<g class="passive-tree-node-group${selected}" data-passive-tree-group-id="${escapeHtml(entry.id)}">${points
        .map((point, index) => {
            const detail = nodeDetail(entry, index);
            const name = detail.name;
            const nodeClass = nodeKindClass(detail.kind);
            const key = nodeKey(entry.id, index);
            const nodeSelected = passiveTreeState.selectedNodeKeys.has(key) || passiveTreeState.selectedNodeKey === key ? " selected" : "";
            const starter = detail.alwaysAllocated ? " starter" : "";
            const negative = detail.identityNegative || detail.secondaryIdentityNegative;
            const hybrid = Boolean(detail.secondaryIdentity);
            const color = signedIdentityColor(detail.identity, detail.identityNegative);
            const fill = hybrid ? ` fill: url(#${nodeGradientId(entry.id, index)});` : "";
            return `
                <g class="passive-tree-node ${nodeClass}${nodeSelected}${starter}${negative ? " negative" : ""}${hybrid ? " hybrid" : ""}" data-passive-tree-group-id="${escapeHtml(entry.id)}" data-passive-tree-node-index="${index}" data-passive-tree-node-key="${escapeHtml(key)}">
                  <circle cx="${point.x}" cy="${point.y}" r="${nodeRadius(detail.kind, detail.size)}" style="--node-color: ${escapeHtml(color)};${fill}"></circle>
                  ${negative ? `<text class="passive-tree-node-sign" x="${point.x}" y="${point.y + 4}" text-anchor="middle">${escapeHtml(identityNegativeMarker(detail))}</text>` : ""}
                  <text class="passive-tree-node-label" x="${point.x + nodeRadius(detail.kind, detail.size) + 5}" y="${point.y - 9}">${escapeHtml(shortNodeLabel(name))}</text>
                </g>
            `;
        })
        .join("")}</g>`;
}

function buildPointLookup(groups) {
    return new Map(groups.flatMap(({ group: entry, points }) =>
        points.map((point, index) => [nodeKey(entry.id, index), { group: entry, point, detail: nodeDetail(entry, index), index }])));
}

function renderExternalConnections(pointLookup) {
    if (!passiveTreeState.tree.connections?.length) {
        return "";
    }
    return `<g class="passive-tree-external-connections">${passiveTreeState.tree.connections
        .map((connection) => {
            const fromEntry = pointLookup.get(nodeKey(connection.from.groupId, connection.from.nodeIndex));
            const toEntry = pointLookup.get(nodeKey(connection.to.groupId, connection.to.nodeIndex));
            const from = fromEntry?.point;
            const to = toEntry?.point;
            if (!from || !to) {
                return "";
            }
            const path = fromEntry.group.id === toEntry.group.id
                ? passiveTreeLinkPath(fromEntry.group, fromEntry.index, toEntry.index)
                : [from, to];
            return `<path d="${passiveTreePathData(path)}" data-passive-tree-connection-id="${escapeHtml(connection.id)}"></path>`;
        })
        .join("")}</g>`;
}

function renderConnectionPreview() {
    if (!passiveTreeState.connectionPreview) {
        return "";
    }
    const { from, to } = passiveTreeState.connectionPreview;
    return `<line class="passive-tree-connection-preview" x1="${from.x}" y1="${from.y}" x2="${to.x}" y2="${to.y}"></line>`;
}

function renderSelectionRect() {
    if (!passiveTreeState.selectionRect) {
        return "";
    }
    const rect = normalizedRect(passiveTreeState.selectionRect.start, passiveTreeState.selectionRect.end);
    return `<rect class="passive-tree-selection-rect" x="${rect.x}" y="${rect.y}" width="${rect.width}" height="${rect.height}"></rect>`;
}

function nodeIdentityState(entry, detail, starter) {
    if (starter) {
        return {
            identity: "Starter",
            identityNegative: false,
            secondaryIdentity: "",
            secondaryIdentityNegative: false
        };
    }
    const groupState = normalizeIdentityState(
            entry.identity,
            entry.identityNegative,
            entry.secondaryIdentity,
            entry.secondaryIdentityNegative
    );
    return normalizeIdentityState(
            detail.identity ?? groupState.identity,
            detail.identityNegative ?? groupState.identityNegative,
            Object.hasOwn(detail, "secondaryIdentity") ? detail.secondaryIdentity : groupState.secondaryIdentity,
            detail.secondaryIdentityNegative ?? groupState.secondaryIdentityNegative,
            groupState.identity
    );
}

function nodeDetail(entry, index) {
    const detail = entry.nodeDetails?.[index] ?? {};
    const name = detail.name || normalizedNodeNames(entry)[index] || `${entry.nodePrefix}_${index + 1}`;
    const starter = entry.starter && index === 0;
    return {
        name,
        kind: starter ? "STARTER" : detail.kind || entry.nodeKind,
        size: detail.size,
        ...nodeIdentityState(entry, detail, starter),
        alwaysAllocated: starter || detail.alwaysAllocated === true,
        grantsNothing: starter || detail.grantsNothing === true
    };
}

function ensureNodeDetails(entry) {
    const names = normalizedNodeNames(entry);
    const pointCount = computeGroupPoints(entry).length;
    const detailAt = (index) => entry.nodeDetails?.[index] ?? {};
    entry.nodeDetails = Array.from({ length: pointCount }, (_, index) => {
        const raw = detailAt(index);
        const detail = {
            name: names[index],
            kind: raw.kind,
            size: raw.size,
            alwaysAllocated: raw.alwaysAllocated === true,
            grantsNothing: raw.grantsNothing === true
        };
        if (raw.identity !== undefined) {
            detail.identity = raw.identity;
        }
        if (Object.hasOwn(raw, "identityNegative")) {
            detail.identityNegative = raw.identityNegative === true;
        }
        if (Object.hasOwn(raw, "secondaryIdentity")) {
            detail.secondaryIdentity = raw.secondaryIdentity;
        }
        if (Object.hasOwn(raw, "secondaryIdentityNegative")) {
            detail.secondaryIdentityNegative = raw.secondaryIdentityNegative === true;
        }
        return detail;
    });
}

function nodeKey(groupId, nodeIndex) {
    return `${groupId}:${nodeIndex}`;
}

function parseNodeKey(key) {
    const match = String(key || "").match(/^(.+):(\d+)$/);
    if (!match) {
        return null;
    }
    return {
        groupId: match[1],
        nodeIndex: Number(match[2])
    };
}

function renderPassiveTreeGroups() {
    ptEls.passiveTreeGroupCount.textContent = `${passiveTreeState.tree.groups.length} groups / ${totalNodeCount()} nodes`;
    if (!passiveTreeState.tree.groups.length) {
        ptEls.passiveTreeGroupList.innerHTML = `<p class="muted">No groups yet.</p>`;
        return;
    }
    ptEls.passiveTreeGroupList.innerHTML = passiveTreeState.tree.groups
        .map((entry) => {
            const selected = entry.id === passiveTreeState.selectedGroupId ? " selected" : "";
            const pointCount = computeGroupPoints(entry).length;
            const flags = [entry.starter ? "starter" : "", entry.shapeKind === "free" ? "deshaped" : ""]
                .filter(Boolean)
                .join(" / ");
            return `
                <button class="passive-tree-group-card${selected}" data-passive-tree-group-id="${escapeHtml(entry.id)}" type="button">
                  <strong>${escapeHtml(entry.shapeName)}</strong>
                  <span>${escapeHtml(identitySummary(entry))} / ${escapeHtml(SHAPE_LABELS[entry.shapeKind] ?? entry.shapeKind)} / ${pointCount} nodes${flags ? ` / ${escapeHtml(flags)}` : ""}</span>
                </button>
            `;
        })
        .join("");
}

function renderSelectedNodeProperties() {
    const selected = selectedNode();
    if (!selected) {
        ptEls.passiveTreeNodeProperties.innerHTML = `<p class="muted property-wide">Select a node on the canvas.</p>`;
        return;
    }
    const detail = nodeDetail(selected.group, selected.index);
    const identityControls = renderSignedIdentityControls("passive-tree-node-property", detail, detail.alwaysAllocated);
    ptEls.passiveTreeNodeProperties.innerHTML = `
        <label class="property-wide">
          Constant
          <input data-passive-tree-node-property="name" type="text" value="${escapeHtml(detail.name)}" ${detail.alwaysAllocated ? "disabled" : ""}>
        </label>
        <label>
          Kind
          <select data-passive-tree-node-property="kind" ${detail.alwaysAllocated ? "disabled" : ""}>
            ${NODE_KINDS.map((kind) => `<option value="${kind}" ${kind === detail.kind ? "selected" : ""}>${escapeHtml(titleCase(kind))}</option>`).join("")}
          </select>
        </label>
        ${identityControls}
        ${detail.alwaysAllocated ? "" : `<div class="property-wide button-row compact"><button data-passive-tree-node-action="delete" type="button">Delete Node</button></div>`}
        <p class="muted property-wide">${escapeHtml(selected.group.shapeKind === "free" ? "Position is edited by dragging this deshaped node on the canvas." : "Position is owned by the shape; deshape the group to move this node alone.")}</p>
        ${detail.alwaysAllocated ? `<p class="muted property-wide">Starter is always allocated and grants nothing.</p>` : ""}
    `;
}

function renderPassiveTreeNodeContextMenu(clientX, clientY) {
    const selected = contextNode();
    if (!selected) {
        closePassiveTreeContextMenu();
        return;
    }
    const targets = contextSelectedNodes();
    const bulk = targets.length > 1;
    const editableNodeCount = targets.filter((target) => !target.detail.alwaysAllocated).length;
    const editableGroupCount = contextSelectedGroupTargets(targets).filter((target) => !target.group.starter).length;
    const shapeOptions = SHAPE_CONTEXT_ORDER
        .map((shapeKind) => `<option value="${shapeKind}" ${shapeKind === selected.group.shapeKind ? "selected" : ""}>${escapeHtml(SHAPE_LABELS[shapeKind] ?? shapeKind)}</option>`)
        .join("");
    const identityOptions = identitySelectOptions(selected.detail.identity, {
        includeCurrent: true
    });
    const secondaryIdentityOptions = identitySelectOptions(selected.detail.secondaryIdentity, {
        includeNone: true,
        includeCurrent: true
    });
    const kindOptions = NODE_KINDS
        .filter((kind) => selected.detail.alwaysAllocated && editableNodeCount === 0 || kind !== "STARTER")
        .map((kind) => `<option value="${kind}" ${kind === selected.detail.kind ? "selected" : ""}>${escapeHtml(titleCase(kind))}</option>`)
        .join("");
    ptEls.passiveTreeNodeContextMenu.innerHTML = `
        <h3>${escapeHtml(bulk ? `${targets.length} Nodes` : shortNodeLabel(selected.detail.name))}</h3>
        <label>
          Shape
          <select data-passive-tree-context="shape" ${editableGroupCount === 0 ? "disabled" : ""}>
            ${shapeOptions}
          </select>
        </label>
        <label>
          Primary
          <select data-passive-tree-context="identity" ${editableNodeCount === 0 ? "disabled" : ""}>
            ${identityOptions}
          </select>
        </label>
        <label class="toggle-label">
          <input data-passive-tree-context="identityNegative" type="checkbox" ${selected.detail.identityNegative ? "checked" : ""} ${editableNodeCount === 0 ? "disabled" : ""}>
          Primary Negative
        </label>
        <label>
          Secondary
          <select data-passive-tree-context="secondaryIdentity" ${editableNodeCount === 0 ? "disabled" : ""}>
            ${secondaryIdentityOptions}
          </select>
        </label>
        <label class="toggle-label">
          <input data-passive-tree-context="secondaryIdentityNegative" type="checkbox" ${selected.detail.secondaryIdentityNegative ? "checked" : ""} ${editableNodeCount === 0 ? "disabled" : ""}>
          Secondary Negative
        </label>
        <label>
          Kind
          <select data-passive-tree-context="kind" ${editableNodeCount === 0 ? "disabled" : ""}>
            ${kindOptions}
          </select>
        </label>
        <div class="button-row compact passive-tree-context-actions">
          <button data-passive-tree-context-action="flipHorizontal" type="button" ${editableGroupCount === 0 ? "disabled" : ""}>Flip H</button>
          <button data-passive-tree-context-action="flipVertical" type="button" ${editableGroupCount === 0 ? "disabled" : ""}>Flip V</button>
        </div>
        <label>
          Rotate
          <input data-passive-tree-rotate-degrees type="number" step="15" value="15" ${editableGroupCount === 0 ? "disabled" : ""}>
        </label>
        <div class="button-row compact passive-tree-context-actions">
          <button data-passive-tree-context-action="rotateCounterClockwise" type="button" ${editableGroupCount === 0 ? "disabled" : ""}>CCW</button>
          <button data-passive-tree-context-action="rotateClockwise" type="button" ${editableGroupCount === 0 ? "disabled" : ""}>CW</button>
        </div>
        <p class="muted">${escapeHtml(contextMenuScopeLabel(selected, targets))}</p>
    `;
    ptEls.passiveTreeNodeContextMenu.style.left = `${clientX}px`;
    ptEls.passiveTreeNodeContextMenu.style.top = `${clientY}px`;
    ptEls.passiveTreeNodeContextMenu.classList.remove("hidden");
    const rect = ptEls.passiveTreeNodeContextMenu.getBoundingClientRect();
    const margin = 8;
    const left = Math.max(margin, Math.min(clientX, window.innerWidth - rect.width - margin));
    const top = Math.max(margin, Math.min(clientY, window.innerHeight - rect.height - margin));
    ptEls.passiveTreeNodeContextMenu.style.left = `${left}px`;
    ptEls.passiveTreeNodeContextMenu.style.top = `${top}px`;
}

function identityContextOptions(currentIdentityValue) {
    const options = IDENTITY_PRESETS.filter((identity) => identity !== "Starter");
    if (currentIdentityValue === "Starter") {
        return ["Starter", ...options];
    }
    if (currentIdentityValue && !options.includes(currentIdentityValue)) {
        return [currentIdentityValue, ...options];
    }
    return options;
}

function contextMenuScopeLabel(selected, targets) {
    if (targets.length > 1) {
        const editableNodes = targets.filter((target) => !target.detail.alwaysAllocated).length;
        const editableGroups = contextSelectedGroupTargets(targets).filter((target) => !target.group.starter).length;
        if (editableNodes === 0 && editableGroups === 0) {
            return "Selected nodes are locked.";
        }
        return `Changes apply to ${targets.length} selected nodes. Shape transforms affect ${editableGroups} owning group${editableGroups === 1 ? "" : "s"}.`;
    }
    return selected.detail.alwaysAllocated ? "Starter is locked." : "Shape changes apply to the clicked node's group.";
}

function renderSavedShapes() {
    if (!passiveTreeState.savedShapes.length) {
        ptEls.passiveTreeSavedShapes.innerHTML = `<p class="muted">No saved shapes.</p>`;
        return;
    }
    ptEls.passiveTreeSavedShapes.innerHTML = passiveTreeState.savedShapes
        .map((template) => `
            <div class="saved-shape-row">
              <button data-passive-tree-load-shape="${escapeHtml(template.id)}" type="button">
                ${escapeHtml(template.label)}
              </button>
              <button data-passive-tree-delete-shape="${escapeHtml(template.id)}" type="button">Delete</button>
            </div>
        `)
        .join("");
}

function updateShapeMeta() {
    const formGroup = readFormGroup(nextGroupId());
    const pointCount = computeGroupPoints(formGroup).length;
    ptEls.passiveTreeShapeMeta.textContent = `${pointCount} generated point${pointCount === 1 ? "" : "s"}`;
    renderPassiveTreePreview(formGroup);
}

function renderPassiveTreePreview(formGroup = readFormGroup(nextGroupId())) {
    if (!ptEls.passiveTreePreviewBody) {
        return;
    }
    const points = computeGroupPoints(formGroup);
    const internalLinks = groupLinks(formGroup, points);
    const draftInternalLinkCount = passiveTreeState.tree.groups.reduce((sum, entry) =>
        sum + groupLinks(entry, computeGroupPoints(entry)).length, 0);
    const selected = selectedGroup();
    ptEls.passiveTreePreviewMeta.textContent = `${totalNodeCount()} nodes / ${passiveTreeState.tree.connections.length + draftInternalLinkCount} links`;
    ptEls.passiveTreePreviewMetrics.innerHTML = [
        previewMetric("Input Nodes", points.length, SHAPE_LABELS[formGroup.shapeKind] ?? formGroup.shapeKind),
        previewMetric("Input Links", internalLinks.length, "Shape-owned"),
        previewMetric("Draft Nodes", totalNodeCount(), `${passiveTreeState.tree.groups.length} groups`),
        previewMetric("Draft Links", passiveTreeState.tree.connections.length + draftInternalLinkCount, `${passiveTreeState.tree.connections.length} authored`)
    ].join("");
    ptEls.passiveTreePreviewShapeSummary.innerHTML = [
        previewRow("Shape Input", `${formGroup.shapeName} / ${SHAPE_LABELS[formGroup.shapeKind] ?? formGroup.shapeKind}`),
        previewRow("Java Helper", javaShapePreview(formGroup)),
        previewRow("Params", previewParams(formGroup)),
        previewRow("Selected Group", selected ? `${selected.shapeName} / ${identitySummary(selected)}` : "None")
    ].join("");
    ptEls.passiveTreePreviewBody.innerHTML = points
        .map((point, index) => previewNodeRow("Input", formGroup, point, index))
        .join("") || emptyPassiveTreePreviewRow(8, "No generated nodes.");
    ptEls.passiveTreePreviewConnectionsBody.innerHTML = [
        ...internalLinks.map((link) => previewInternalLinkRow(formGroup, link)),
        ...passiveTreeState.tree.connections.map((connectionEntry) => previewAuthoredLinkRow(connectionEntry))
    ].join("") || emptyPassiveTreePreviewRow(4, "No generated or authored links.");
}

function previewMetric(label, value, detail) {
    return `<div class="metric"><strong>${escapeHtml(String(value))}</strong><span>${escapeHtml(label)}${detail ? ` - ${escapeHtml(detail)}` : ""}</span></div>`;
}

function previewRow(label, value) {
    return `<div class="preview-row"><span>${escapeHtml(label)}</span><strong title="${escapeHtml(value)}">${escapeHtml(value)}</strong></div>`;
}

function previewNodeRow(scope, groupEntry, point, index) {
    const detail = nodeDetail(groupEntry, index);
    return `
        <tr>
          <td>${escapeHtml(scope)}</td>
          <td class="mono">${escapeHtml(detail.name)}</td>
          <td>${escapeHtml(titleCase(detail.kind))}</td>
          <td>${escapeHtml(identitySummary(detail))}</td>
          <td>${escapeHtml(String(point.x))}</td>
          <td>${escapeHtml(String(point.y))}</td>
          <td class="mono">${escapeHtml(point.accessor || "literal")}</td>
          <td>${escapeHtml(nodeFlagText(detail))}</td>
        </tr>
    `;
}

function previewInternalLinkRow(groupEntry, link) {
    return `
        <tr>
          <td>Input shape</td>
          <td class="mono">${escapeHtml(previewNodeLabel(groupEntry, link.fromIndex))}</td>
          <td class="mono">${escapeHtml(previewNodeLabel(groupEntry, link.toIndex))}</td>
          <td>Internal</td>
        </tr>
    `;
}

function previewAuthoredLinkRow(connectionEntry) {
    const exported = connectionExport(connectionEntry);
    return `
        <tr>
          <td>Draft</td>
          <td class="mono">${escapeHtml(exported.from.name ?? exported.from.id)}</td>
          <td class="mono">${escapeHtml(exported.to.name ?? exported.to.id)}</td>
          <td>${escapeHtml(titleCase(exported.kind))}</td>
        </tr>
    `;
}

function previewNodeLabel(groupEntry, index) {
    return `${nodeDetail(groupEntry, index).name} [${index}]`;
}

function nodeFlagText(detail) {
    const flags = [
        detail.alwaysAllocated ? "Always allocated" : "",
        detail.grantsNothing ? "Grants nothing" : "",
        detail.secondaryIdentity ? "Hybrid" : "",
        detail.identityNegative ? "Primary negative" : "",
        detail.secondaryIdentityNegative ? "Secondary negative" : ""
    ].filter(Boolean);
    return flags.join(", ") || "-";
}

function previewParams(groupEntry) {
    if (groupEntry.shapeKind === "free") {
        return `${groupEntry.freePoints.length} free point${groupEntry.freePoints.length === 1 ? "" : "s"}`;
    }
    const entries = Object.entries(groupEntry.params ?? {});
    return entries.length
        ? entries.map(([key, value]) => `${key}=${previewNumber(value)}`).join(", ")
        : "No params";
}

function javaShapePreview(groupEntry) {
    const points = computeGroupPoints(groupEntry);
    if (groupEntry.shapeKind === "point") {
        const point = points[0] ?? { x: 0, y: 0 };
        return javaPoint(point.x, point.y);
    }
    if (groupEntry.shapeKind === "free") {
        return points.map((point) => javaPoint(point.x, point.y)).join(", ");
    }
    return `PassiveTreeLayouts.${javaMethod(groupEntry.shapeKind)}(${javaArgs(groupEntry).join(", ")})`;
}

function emptyPassiveTreePreviewRow(colspan, message) {
    return `<tr><td colspan="${colspan}">${escapeHtml(message)}</td></tr>`;
}

function previewNumber(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return String(value);
    }
    return Number.isInteger(number) ? String(number) : String(Math.round(number * 100) / 100);
}

function handleGroupListClick(event) {
    const button = event.target.closest("[data-passive-tree-group-id]");
    if (!button) {
        return;
    }
    selectPassiveTreeGroup(button.dataset.passiveTreeGroupId);
}

function selectPassiveTreeGroup(groupId) {
    passiveTreeState.selectedGroupId = groupId;
    passiveTreeState.selectedNodeKey = null;
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
}

function selectPassiveTreeNode(groupId, nodeIndex, additive = false) {
    const key = nodeKey(groupId, nodeIndex);
    passiveTreeState.selectedGroupId = groupId;
    passiveTreeState.selectedNodeKey = key;
    if (!additive) {
        passiveTreeState.selectedNodeKeys.clear();
    }
    passiveTreeState.selectedNodeKeys.add(key);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
}

function selectPassiveTreeNodeForPointerDown(groupId, nodeIndex, additive = false) {
    const key = nodeKey(groupId, nodeIndex);
    if (passiveTreeState.selectedNodeKeys.has(key)) {
        passiveTreeState.selectedGroupId = groupId;
        passiveTreeState.selectedNodeKey = key;
        syncPassiveTreeFormFromSelected();
        renderPassiveTreeEditor();
        return;
    }
    selectPassiveTreeNode(groupId, nodeIndex, additive);
}

function focusPassiveTreeContextNode(groupId, nodeIndex) {
    const key = nodeKey(groupId, nodeIndex);
    passiveTreeState.selectedGroupId = groupId;
    passiveTreeState.selectedNodeKey = key;
    if (!passiveTreeState.selectedNodeKeys.has(key)) {
        passiveTreeState.selectedNodeKeys.clear();
        passiveTreeState.selectedNodeKeys.add(key);
    }
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
}

function addPassiveTreeGroup() {
    const before = snapshotPassiveTreeState();
    const next = readFormGroup(nextGroupId());
    next.shapeName = uniqueShapeName(next.shapeName);
    passiveTreeState.tree.groups.push(next);
    passiveTreeState.selectedGroupId = next.id;
    commitPassiveTreeHistory(before);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Added ${next.shapeName}.`);
}

function updateSelectedPassiveTreeGroup() {
    const index = passiveTreeState.tree.groups.findIndex((entry) => entry.id === passiveTreeState.selectedGroupId);
    if (index < 0) {
        setPassiveTreeStatus("Select a group before updating.", true);
        return;
    }
    if (passiveTreeState.tree.groups[index].starter) {
        setPassiveTreeStatus("The starter node is managed automatically and always grants nothing.", true);
        syncPassiveTreeFormFromSelected();
        renderPassiveTreeEditor();
        return;
    }
    const before = snapshotPassiveTreeState();
    const updated = readFormGroup(passiveTreeState.selectedGroupId);
    updated.shapeName = uniqueShapeName(updated.shapeName, updated.id);
    passiveTreeState.tree.groups[index] = updated;
    commitPassiveTreeHistory(before);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Updated ${updated.shapeName}.`);
}

function deleteSelectedPassiveTreeGroup() {
    const selected = selectedGroup();
    if (!selected) {
        setPassiveTreeStatus("Select a group before deleting.", true);
        return;
    }
    if (selected.starter) {
        setPassiveTreeStatus("The starter node must stay in every passive tree.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    connectBehindDeletedRefs(groupNodeRefs(selected));
    passiveTreeState.tree.groups = passiveTreeState.tree.groups.filter((entry) => entry.id !== selected.id);
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    passiveTreeState.selectedNodeKey = null;
    passiveTreeState.selectedNodeKeys.clear();
    commitPassiveTreeHistory(before);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Deleted ${selected.shapeName}.`);
}

function handleSelectedNodeAction(event) {
    const action = event.target?.dataset?.passiveTreeNodeAction;
    if (action === "delete") {
        deleteSelectedPassiveTreeNode();
    }
}

function deleteSelectedPassiveTreeNode() {
    const selected = selectedNode();
    if (!selected) {
        setPassiveTreeStatus("Select a node before deleting.", true);
        return;
    }
    if (selected.detail.alwaysAllocated) {
        setPassiveTreeStatus("The starter node must stay in every passive tree.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    if (selected.group.shapeKind !== "free") {
        deshapeGroup(selected.group);
    }
    connectBehindDeletedRefs([{ groupId: selected.group.id, nodeIndex: selected.index }]);
    selected.group.freePoints.splice(selected.index, 1);
    selected.group.nodeNames.splice(selected.index, 1);
    selected.group.nodeDetails.splice(selected.index, 1);
    selected.group.nodeCount = selected.group.freePoints.length;
    shiftRemovedInternalLinksAfterNodeDelete(selected.group, selected.index);
    shiftConnectionsAfterNodeDelete(selected.group.id, selected.index);
    if (selected.group.freePoints.length === 0) {
        passiveTreeState.tree.groups = passiveTreeState.tree.groups.filter((entry) => entry.id !== selected.group.id);
    }
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    passiveTreeState.selectedNodeKey = null;
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedGroupId = selected.group.freePoints.length > 0 ? selected.group.id : passiveTreeState.tree.groups[0]?.id ?? null;
    commitPassiveTreeHistory(before);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Deleted ${selected.detail.name} and connected through it.`);
}

function deshapeSelectedPassiveTreeGroup() {
    const selected = selectedGroup();
    if (!selected) {
        setPassiveTreeStatus("Select a group before deshaping.", true);
        return;
    }
    if (selected.starter) {
        setPassiveTreeStatus("The starter node cannot be deshaped.", true);
        return;
    }
    if (selected.shapeKind === "free") {
        setPassiveTreeStatus(`${selected.shapeName} is already deshaped.`);
        return;
    }
    const before = snapshotPassiveTreeState();
    deshapeGroup(selected);
    commitPassiveTreeHistory(before);
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Deshaped ${selected.shapeName}; nodes can now move independently.`);
}

function deshapeGroup(entry) {
    if (entry.shapeKind === "free") {
        return;
    }
    const points = computeGroupPoints(entry);
    entry.freePoints = points.map((point) => ({ x: point.x, y: point.y }));
    entry.nodeDetails = points.map((_, index) => normalizeNodeDetail(nodeDetail(entry, index)));
    entry.nodeNames = normalizedNodeNames(entry);
    entry.nodeCount = points.length;
    entry.shapeKind = "free";
    entry.params = {};
}

function groupNodeRefs(entry) {
    return computeGroupPoints(entry).map((_, index) => ({ groupId: entry.id, nodeIndex: index }));
}

function connectBehindDeletedRefs(deletedRefs) {
    const deletedKeys = new Set(deletedRefs.map((ref) => nodeKey(ref.groupId, ref.nodeIndex)));
    const incoming = passiveTreeState.tree.connections.filter((entry) =>
        deletedKeys.has(nodeKey(entry.to.groupId, entry.to.nodeIndex))
        && !deletedKeys.has(nodeKey(entry.from.groupId, entry.from.nodeIndex)));
    const outgoing = passiveTreeState.tree.connections.filter((entry) =>
        deletedKeys.has(nodeKey(entry.from.groupId, entry.from.nodeIndex))
        && !deletedKeys.has(nodeKey(entry.to.groupId, entry.to.nodeIndex)));
    for (const fromEntry of incoming) {
        for (const toEntry of outgoing) {
            addConnectionIfMissing(fromEntry.from, toEntry.to);
        }
    }
}

function addConnectionIfMissing(from, to) {
    if (nodeKey(from.groupId, from.nodeIndex) === nodeKey(to.groupId, to.nodeIndex)) {
        return;
    }
    const exists = passiveTreeState.tree.connections.some((entry) =>
        nodeKey(entry.from.groupId, entry.from.nodeIndex) === nodeKey(from.groupId, from.nodeIndex)
        && nodeKey(entry.to.groupId, entry.to.nodeIndex) === nodeKey(to.groupId, to.nodeIndex));
    if (!exists) {
        passiveTreeState.tree.connections.push(connection(from, to));
    }
}

function shiftConnectionsAfterNodeDelete(groupId, deletedIndex) {
    passiveTreeState.tree.connections = passiveTreeState.tree.connections
        .filter((entry) =>
            nodeKey(entry.from.groupId, entry.from.nodeIndex) !== nodeKey(groupId, deletedIndex)
            && nodeKey(entry.to.groupId, entry.to.nodeIndex) !== nodeKey(groupId, deletedIndex))
        .map((entry) => ({
            ...entry,
            from: shiftNodeRef(entry.from, groupId, deletedIndex),
            to: shiftNodeRef(entry.to, groupId, deletedIndex)
        }));
}

function shiftConnectionsAfterNodeInsert(groupId, insertedIndex) {
    passiveTreeState.tree.connections = passiveTreeState.tree.connections
        .map((entry) => ({
            ...entry,
            from: shiftNodeRefAfterInsert(entry.from, groupId, insertedIndex),
            to: shiftNodeRefAfterInsert(entry.to, groupId, insertedIndex)
        }));
}

function shiftRemovedInternalLinksAfterNodeDelete(group, deletedIndex) {
    group.removedInternalLinks = normalizedInternalLinkKeys((group.removedInternalLinks ?? [])
        .map((key) => shiftInternalLinkKeyAfterNodeDelete(key, deletedIndex))
        .filter(Boolean));
}

function shiftInternalLinkKeyAfterNodeDelete(key, deletedIndex) {
    const [from, to] = normalizeInternalLinkKey(key).split(":").map(Number);
    if (!Number.isInteger(from) || !Number.isInteger(to) || from === deletedIndex || to === deletedIndex) {
        return "";
    }
    return internalLinkKey(
            from > deletedIndex ? from - 1 : from,
            to > deletedIndex ? to - 1 : to
    );
}

function shiftRemovedInternalLinksAfterNodeInsert(group, insertedIndex) {
    group.removedInternalLinks = normalizedInternalLinkKeys((group.removedInternalLinks ?? [])
        .map((key) => shiftInternalLinkKeyAfterNodeInsert(key, insertedIndex)));
}

function shiftInternalLinkKeyAfterNodeInsert(key, insertedIndex) {
    const [from, to] = normalizeInternalLinkKey(key).split(":").map(Number);
    if (!Number.isInteger(from) || !Number.isInteger(to)) {
        return "";
    }
    return internalLinkKey(
            from >= insertedIndex ? from + 1 : from,
            to >= insertedIndex ? to + 1 : to
    );
}

function shiftNodeRef(ref, groupId, deletedIndex) {
    if (ref.groupId !== groupId || ref.nodeIndex <= deletedIndex) {
        return ref;
    }
    return {
        ...ref,
        nodeIndex: ref.nodeIndex - 1
    };
}

function shiftNodeRefAfterInsert(ref, groupId, insertedIndex) {
    if (ref.groupId !== groupId || ref.nodeIndex < insertedIndex) {
        return ref;
    }
    return {
        ...ref,
        nodeIndex: ref.nodeIndex + 1
    };
}

function updateSelectedNodeFromProperties(event) {
    const selected = selectedNode();
    if (!selected || selected.detail.alwaysAllocated) {
        return;
    }
    const property = event.target?.dataset?.passiveTreeNodeProperty;
    if (!property) {
        return;
    }
    const before = snapshotPassiveTreeState();
    ensureNodeDetails(selected.group);
    const detail = selected.group.nodeDetails[selected.index] ?? {};
    if (property === "name") {
        detail.name = toConstantName(event.target.value, selected.detail.name);
        selected.group.nodeNames[selected.index] = detail.name;
    } else if (property === "kind") {
        const kind = NODE_KINDS.includes(event.target.value) && event.target.value !== "STARTER"
            ? event.target.value : selected.detail.kind;
        detail.size = passiveTreeSizeForKind(kind);
        detail.kind = kind;
    } else if (property === "identity") {
        detail.identity = identityValueFromNodeControls("identity", selected.detail.identity);
        detail.identityNegative = detail.identityNegative ?? selected.detail.identityNegative;
    } else if (property === "customIdentity") {
        detail.identity = identityValueFromNodeControls("identity", selected.detail.identity);
        detail.identityNegative = detail.identityNegative ?? selected.detail.identityNegative;
    } else if (property === "identityNegative") {
        detail.identity = detail.identity || selected.detail.identity;
        detail.identityNegative = event.target.checked;
    } else if (property === "secondaryIdentity") {
        detail.identity = detail.identity || selected.detail.identity;
        detail.identityNegative = detail.identityNegative ?? selected.detail.identityNegative;
        detail.secondaryIdentity = identityValueFromNodeControls("secondaryIdentity", "");
    } else if (property === "customSecondaryIdentity") {
        detail.identity = detail.identity || selected.detail.identity;
        detail.identityNegative = detail.identityNegative ?? selected.detail.identityNegative;
        detail.secondaryIdentity = identityValueFromNodeControls("secondaryIdentity", "");
    } else if (property === "secondaryIdentityNegative") {
        detail.identity = detail.identity || selected.detail.identity;
        detail.identityNegative = detail.identityNegative ?? selected.detail.identityNegative;
        detail.secondaryIdentity = Object.hasOwn(detail, "secondaryIdentity")
            ? detail.secondaryIdentity
            : selected.detail.secondaryIdentity;
        detail.secondaryIdentityNegative = event.target.checked && Boolean(detail.secondaryIdentity);
    }
    normalizeNodeIdentityDetail(detail, selected.group);
    selected.group.nodeDetails[selected.index] = detail;
    commitPassiveTreeHistory(before);
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Updated ${nodeDetail(selected.group, selected.index).name}.`);
}

function handlePassiveTreeCanvasContextMenu(event) {
    const nodeElement = event.target.closest("[data-passive-tree-node-key]");
    if (!nodeElement) {
        closePassiveTreeContextMenu();
        return;
    }
    event.preventDefault();
    const node = nodeFromElement(nodeElement);
    focusPassiveTreeContextNode(node.groupId, node.nodeIndex);
    passiveTreeState.contextNodeKey = nodeKey(node.groupId, node.nodeIndex);
    renderPassiveTreeNodeContextMenu(event.clientX, event.clientY);
}

function handlePassiveTreeContextMenuChange(event) {
    const action = event.target?.dataset?.passiveTreeContext;
    if (!action) {
        return;
    }
    const targets = contextSelectedNodes();
    if (!targets.length) {
        closePassiveTreeContextMenu();
        return;
    }
    if (action === "shape") {
        quickChangeSelectedGroupShapes(targets, event.target.value);
    } else if (action === "identity") {
        quickChangeSelectedNodeIdentity(targets, "primary", event.target.value);
    } else if (action === "identityNegative") {
        quickChangeSelectedNodeIdentityNegative(targets, "primary", event.target.checked);
    } else if (action === "secondaryIdentity") {
        quickChangeSelectedNodeIdentity(targets, "secondary", event.target.value);
    } else if (action === "secondaryIdentityNegative") {
        quickChangeSelectedNodeIdentityNegative(targets, "secondary", event.target.checked);
    } else if (action === "kind") {
        quickChangeSelectedNodeKind(targets, event.target.value);
    }
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    closePassiveTreeContextMenu();
}

function handlePassiveTreeContextMenuClick(event) {
    const action = event.target?.dataset?.passiveTreeContextAction;
    if (!action) {
        return;
    }
    event.preventDefault();
    const targets = contextSelectedNodes();
    if (!targets.length) {
        closePassiveTreeContextMenu();
        return;
    }
    if (action === "flipHorizontal") {
        transformSelectedPassiveTreeGroups(targets, "flipHorizontal");
    } else if (action === "flipVertical") {
        transformSelectedPassiveTreeGroups(targets, "flipVertical");
    } else if (action === "rotateClockwise") {
        transformSelectedPassiveTreeGroups(targets, "rotate", contextRotateDegrees());
    } else if (action === "rotateCounterClockwise") {
        transformSelectedPassiveTreeGroups(targets, "rotate", -contextRotateDegrees());
    }
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
}

function handlePassiveTreeDocumentClick(event) {
    if (ptEls.passiveTreeNodeContextMenu?.contains(event.target)) {
        return;
    }
    closePassiveTreeContextMenu();
}

function closePassiveTreeContextMenu() {
    passiveTreeState.contextNodeKey = null;
    ptEls.passiveTreeNodeContextMenu?.classList.add("hidden");
}

function quickChangeSelectedNodeIdentity(targets, role, identity) {
    const editable = targets.filter((target) => !target.detail.alwaysAllocated);
    if (!editable.length) {
        setPassiveTreeStatus("Selected node identity is locked.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    for (const target of editable) {
        ensureNodeDetails(target.group);
        const current = nodeDetail(target.group, target.index);
        const detail = target.group.nodeDetails[target.index] ?? {};
        detail.name = detail.name || current.name;
        if (role === "secondary") {
            detail.identity = detail.identity || current.identity;
            detail.identityNegative = detail.identityNegative ?? current.identityNegative;
            detail.secondaryIdentity = identity || "";
        } else {
            detail.identity = identity || target.group.identity;
            detail.identityNegative = detail.identityNegative ?? current.identityNegative;
        }
        normalizeNodeIdentityDetail(detail, target.group);
        target.group.nodeDetails[target.index] = detail;
    }
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`Changed ${editable.length} selected node${editable.length === 1 ? "" : "s"} ${role} identity to ${identity || "none"}.`);
}

function quickChangeSelectedNodeIdentityNegative(targets, role, negative) {
    const editable = targets.filter((target) => !target.detail.alwaysAllocated);
    if (!editable.length) {
        setPassiveTreeStatus("Selected node identity is locked.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    for (const target of editable) {
        ensureNodeDetails(target.group);
        const current = nodeDetail(target.group, target.index);
        const detail = target.group.nodeDetails[target.index] ?? {};
        detail.name = detail.name || current.name;
        detail.identity = detail.identity || current.identity;
        detail.identityNegative = detail.identityNegative ?? current.identityNegative;
        if (role === "secondary") {
            detail.secondaryIdentity = Object.hasOwn(detail, "secondaryIdentity")
                ? detail.secondaryIdentity
                : current.secondaryIdentity;
            detail.secondaryIdentityNegative = Boolean(negative) && Boolean(detail.secondaryIdentity);
        } else {
            detail.identityNegative = Boolean(negative);
        }
        normalizeNodeIdentityDetail(detail, target.group);
        target.group.nodeDetails[target.index] = detail;
    }
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`${negative ? "Marked" : "Cleared"} ${role} negative on ${editable.length} selected node${editable.length === 1 ? "" : "s"}.`);
}

function quickChangeSelectedNodeKind(targets, kind) {
    const editable = targets.filter((target) => !target.detail.alwaysAllocated);
    if (!editable.length) {
        setPassiveTreeStatus("Selected node kind is locked.", true);
        return;
    }
    if (!NODE_KINDS.includes(kind) || kind === "STARTER") {
        return;
    }
    const before = snapshotPassiveTreeState();
    const size = passiveTreeSizeForKind(kind);
    for (const target of editable) {
        ensureNodeDetails(target.group);
        const current = nodeDetail(target.group, target.index);
        const detail = target.group.nodeDetails[target.index] ?? {};
        detail.name = detail.name || current.name;
        detail.kind = kind;
        detail.size = size;
        target.group.nodeDetails[target.index] = detail;
    }
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`Changed ${editable.length} selected node${editable.length === 1 ? "" : "s"} to ${titleCase(kind)}.`);
}

function quickChangeSelectedGroupShapes(targets, shapeKind) {
    const editable = contextSelectedGroupTargets(targets).filter((target) => !target.group.starter);
    if (!editable.length) {
        setPassiveTreeStatus("Selected group shape is locked.", true);
        return;
    }
    if (shapeKind !== "free" && !SHAPE_PARAMS[shapeKind]) {
        return;
    }
    const before = snapshotPassiveTreeState();
    let changed = 0;
    let focusedTarget = null;
    for (const target of editable) {
        if (changePassiveTreeGroupShape(target.group, shapeKind, target.index)) {
            changed++;
            focusedTarget = focusedTarget ?? target;
        }
    }
    if (changed === 0) {
        setPassiveTreeStatus(`Selected group${editable.length === 1 ? " is" : "s are"} already ${SHAPE_LABELS[shapeKind] ?? shapeKind}.`);
        return;
    }
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    focusPassiveTreeGroupTarget(focusedTarget);
    prunePassiveTreeSelection();
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(shapeKind === "free"
        ? `Deshaped ${changed} selected group${changed === 1 ? "" : "s"}; nodes can now move independently.`
        : `Changed ${changed} selected group${changed === 1 ? "" : "s"} to ${SHAPE_LABELS[shapeKind] ?? shapeKind}.`);
}

function transformSelectedPassiveTreeGroups(targets, operation, degrees = 0) {
    const editable = contextSelectedGroupTargets(targets).filter((target) => !target.group.starter);
    if (!editable.length) {
        setPassiveTreeStatus("Selected group shape is locked.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    let changed = 0;
    for (const target of editable) {
        if (applyPassiveTreeGroupTransform(target.group, operation, degrees)) {
            changed++;
        }
    }
    if (changed === 0) {
        return;
    }
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    prunePassiveTreeSelection();
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`${passiveTreeTransformLabel(operation, degrees)} ${changed} selected group${changed === 1 ? "" : "s"}.`);
}

function quickChangeNodeIdentity(group, index, identity) {
    const current = nodeDetail(group, index);
    if (current.alwaysAllocated) {
        setPassiveTreeStatus("The starter node identity is locked.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    ensureNodeDetails(group);
    const detail = group.nodeDetails[index] ?? {};
    detail.name = detail.name || current.name;
    detail.identity = identity || group.identity;
    detail.identityNegative = detail.identityNegative ?? current.identityNegative;
    normalizeNodeIdentityDetail(detail, group);
    group.nodeDetails[index] = detail;
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`Changed ${current.name} identity to ${detail.identity}.`);
}

function quickChangeNodeKind(group, index, kind) {
    const current = nodeDetail(group, index);
    if (current.alwaysAllocated) {
        setPassiveTreeStatus("The starter node kind is locked.", true);
        return;
    }
    if (!NODE_KINDS.includes(kind) || kind === "STARTER") {
        return;
    }
    const before = snapshotPassiveTreeState();
    ensureNodeDetails(group);
    const detail = group.nodeDetails[index] ?? {};
    detail.name = detail.name || current.name;
    detail.size = passiveTreeSizeForKind(kind);
    detail.kind = kind;
    group.nodeDetails[index] = detail;
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`Changed ${current.name} to ${titleCase(kind)}.`);
}

function changePassiveTreeGroupShape(group, shapeKind, anchorIndex) {
    if (shapeKind === group.shapeKind) {
        return false;
    }
    if (shapeKind === "free") {
        deshapeGroup(group);
        return true;
    }
    const previousPoints = computeGroupPoints(group);
    const previousNames = normalizedNodeNames(group);
    const previousDetails = previousPoints.map((_, index) => normalizeNodeDetail(nodeDetail(group, index)));
    const nextNodeCount = quickShapeNodeCount(shapeKind, previousPoints.length);
    group.shapeKind = shapeKind;
    group.nodeCount = nextNodeCount;
    group.params = quickShapeParams(shapeKind, previousPoints, nextNodeCount, anchorIndex);
    group.freePoints = [];
    group.nodeNames = quickShapeNodeNames(group, previousNames, anchorIndex);
    group.nodeDetails = quickShapeNodeDetails(group, previousDetails, group.nodeNames, anchorIndex);
    group.removedInternalLinks = [];
    return true;
}

function quickChangeGroupShape(group, shapeKind, anchorIndex) {
    if (group.starter) {
        setPassiveTreeStatus("The starter shape is locked.", true);
        return;
    }
    if (shapeKind === group.shapeKind) {
        return;
    }
    if (shapeKind !== "free" && !SHAPE_PARAMS[shapeKind]) {
        return;
    }
    const before = snapshotPassiveTreeState();
    changePassiveTreeGroupShape(group, shapeKind, anchorIndex);
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    const nextIndex = Math.min(anchorIndex, effectiveNodeCount(group.shapeKind, group.nodeCount) - 1);
    passiveTreeState.selectedGroupId = group.id;
    passiveTreeState.selectedNodeKey = nodeKey(group.id, nextIndex);
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(shapeKind === "free"
        ? `Deshaped ${group.shapeName}; nodes can now move independently.`
        : `Changed ${group.shapeName} to ${SHAPE_LABELS[shapeKind] ?? shapeKind}.`);
}

function applyPassiveTreeGroupTransform(group, operation, degrees = 0) {
    if (group.starter) {
        return false;
    }
    const points = computeGroupPoints(group);
    if (!points.length) {
        return false;
    }
    const origin = centroid(points);
    const indexMap = transformPassiveTreeGroupParams(group, operation, degrees, origin);
    if (indexMap) {
        reorderPassiveTreeGroupNodes(group, indexMap);
        remapPassiveTreeGroupRefs(group.id, indexMap);
    }
    return true;
}

function transformPassiveTreeGroup(group, operation, degrees = 0) {
    if (group.starter) {
        setPassiveTreeStatus("The starter shape is locked.", true);
        return;
    }
    const before = snapshotPassiveTreeState();
    if (!applyPassiveTreeGroupTransform(group, operation, degrees)) {
        return;
    }
    passiveTreeState.tree.connections = validConnections(passiveTreeState.tree, passiveTreeState.tree.connections);
    commitPassiveTreeHistory(before);
    setPassiveTreeStatus(`${passiveTreeTransformLabel(operation, degrees)} ${group.shapeName}.`);
}

function transformPassiveTreeGroupParams(group, operation, degrees, origin) {
    const p = group.params;
    if (group.shapeKind === "free") {
        group.freePoints = group.freePoints.map((point) => transformPassiveTreePoint(point, operation, degrees, origin));
        return null;
    }
    if (group.shapeKind === "point") {
        const next = transformPassiveTreePoint({ x: p.x, y: p.y }, operation, degrees, origin);
        p.x = next.x;
        p.y = next.y;
        return null;
    }
    if (["circle", "halfCircle", "arc"].includes(group.shapeKind)) {
        const center = transformPassiveTreePoint({ x: p.centerX, y: p.centerY }, operation, degrees, origin);
        p.centerX = center.x;
        p.centerY = center.y;
        if (operation === "rotate") {
            p.startDegrees = normalizeDegrees(p.startDegrees + degrees);
            return null;
        }
        if (group.shapeKind === "circle") {
            p.startDegrees = transformPassiveTreeAngle(p.startDegrees, operation);
            return circleFlipIndexMap(effectiveNodeCount(group.shapeKind, group.nodeCount));
        }
        if (group.shapeKind === "halfCircle") {
            p.startDegrees = transformPassiveTreeAngle(p.startDegrees + 180, operation);
            return reverseIndexMap(effectiveNodeCount(group.shapeKind, group.nodeCount));
        }
        p.startDegrees = transformPassiveTreeAngle(p.startDegrees, operation);
        p.sweepDegrees = -p.sweepDegrees;
        return null;
    }
    if (group.shapeKind === "line") {
        const start = transformPassiveTreePoint({ x: p.startX, y: p.startY }, operation, degrees, origin);
        const step = transformPassiveTreeVector({ x: p.xStep, y: p.yStep }, operation, degrees);
        p.startX = start.x;
        p.startY = start.y;
        p.xStep = step.x;
        p.yStep = step.y;
        return null;
    }
    if (group.shapeKind === "travelPath" || group.shapeKind === "branchedTravelPath") {
        const from = transformPassiveTreePoint({ x: p.fromX, y: p.fromY }, operation, degrees, origin);
        const to = transformPassiveTreePoint({ x: p.toX, y: p.toY }, operation, degrees, origin);
        p.fromX = from.x;
        p.fromY = from.y;
        p.toX = to.x;
        p.toY = to.y;
        if (group.shapeKind === "branchedTravelPath") {
            const branch = transformPassiveTreeVector({ x: p.xBranchOffset, y: p.yBranchOffset }, operation, degrees);
            p.xBranchOffset = branch.x;
            p.yBranchOffset = branch.y;
        }
        return null;
    }
    if (group.shapeKind === "twoPathDeadEnd") {
        const notable = transformPassiveTreePoint({ x: p.notableX, y: p.notableY }, operation, degrees, origin);
        const step = transformPassiveTreeVector({ x: p.xStepFromNotable, y: p.yStepFromNotable }, operation, degrees);
        const branch = transformPassiveTreeVector({ x: p.xBranchOffset, y: p.yBranchOffset }, operation, degrees);
        p.notableX = notable.x;
        p.notableY = notable.y;
        p.xStepFromNotable = step.x;
        p.yStepFromNotable = step.y;
        p.xBranchOffset = branch.x;
        p.yBranchOffset = branch.y;
    }
    return null;
}

function handlePassiveTreeCanvasPointerDown(event) {
    if (event.button !== 0) {
        return;
    }
    closePassiveTreeContextMenu();
    ptEls.passiveTreeCanvas.focus({ preventScroll: true });
    const point = canvasPoint(event);
    const nodeElement = event.target.closest("[data-passive-tree-node-key]");
    if (passiveTreeState.spaceHeld) {
        startPassiveTreePan(event);
        return;
    }
    if (nodeElement && event.ctrlKey) {
        const from = nodeFromElement(nodeElement);
        passiveTreeState.drag = { kind: "connect", from, start: point, current: point, beforeSnapshot: snapshotPassiveTreeState() };
        passiveTreeState.connectionPreview = { from: pointForNode(from), to: point };
        ptEls.passiveTreeCanvas.setPointerCapture(event.pointerId);
        event.preventDefault();
        renderPassiveTreeCanvas();
        return;
    }
    if (event.shiftKey && !nodeElement) {
        if (removePassiveTreeLineFromEvent(event)) {
            event.preventDefault();
            return;
        }
        passiveTreeState.drag = { kind: "select", start: point, end: point };
        passiveTreeState.selectionRect = { start: point, end: point };
        ptEls.passiveTreeCanvas.setPointerCapture(event.pointerId);
        event.preventDefault();
        renderPassiveTreeCanvas();
        return;
    }
    if (nodeElement) {
        const node = nodeFromElement(nodeElement);
        selectPassiveTreeNodeForPointerDown(node.groupId, node.nodeIndex, event.shiftKey || event.metaKey);
        const targets = dragTargetsForSelection(node);
        passiveTreeState.drag = {
            kind: "moveSelection",
            start: point,
            last: point,
            node,
            groupIds: targets.groupIds,
            nodeKeys: targets.nodeKeys,
            beforeSnapshot: snapshotPassiveTreeState()
        };
        ptEls.passiveTreeCanvas.setPointerCapture(event.pointerId);
        event.preventDefault();
        return;
    }
    if (passiveTreeState.selectedNodeKeys.size === 0) {
        startPassiveTreePan(event);
    }
}

function removePassiveTreeLineFromEvent(event) {
    const connectionLine = event.target.closest("[data-passive-tree-connection-id]");
    if (connectionLine) {
        return deletePassiveTreeConnection(connectionLine.dataset.passiveTreeConnectionId);
    }
    const internalLine = event.target.closest("[data-passive-tree-internal-link]");
    if (internalLine) {
        return removePassiveTreeInternalLink(
                internalLine.dataset.passiveTreeGroupId,
                Number(internalLine.dataset.passiveTreeFromIndex),
                Number(internalLine.dataset.passiveTreeToIndex)
        );
    }
    return false;
}

function deletePassiveTreeConnection(connectionId) {
    const index = passiveTreeState.tree.connections.findIndex((entry) => entry.id === connectionId);
    if (index < 0) {
        return false;
    }
    const before = snapshotPassiveTreeState();
    const removed = passiveTreeState.tree.connections[index];
    passiveTreeState.tree.connections.splice(index, 1);
    commitPassiveTreeHistory(before);
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Removed connection ${connectionNodeLabel(removed.from)} -> ${connectionNodeLabel(removed.to)}.`);
    return true;
}

function removePassiveTreeInternalLink(groupId, fromIndex, toIndex) {
    const group = passiveTreeState.tree.groups.find((entry) => entry.id === groupId);
    const key = internalLinkKey(fromIndex, toIndex);
    if (!group || !key) {
        return false;
    }
    const removed = new Set(group.removedInternalLinks ?? []);
    if (removed.has(key)) {
        return false;
    }
    const before = snapshotPassiveTreeState();
    removed.add(key);
    group.removedInternalLinks = [...removed].sort(compareInternalLinkKeys);
    commitPassiveTreeHistory(before);
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Removed line ${connectionNodeLabel({ groupId, nodeIndex: fromIndex })} -> ${connectionNodeLabel({ groupId, nodeIndex: toIndex })}.`);
    return true;
}

function connectionNodeLabel(ref) {
    const group = passiveTreeState.tree.groups.find((entry) => entry.id === ref.groupId);
    return group ? nodeDetail(group, ref.nodeIndex).name : nodeKey(ref.groupId, ref.nodeIndex);
}

function handlePassiveTreeCanvasPointerMove(event) {
    const drag = passiveTreeState.drag;
    if (!drag) {
        return;
    }
    const point = canvasPoint(event);
    if (drag.kind === "select") {
        drag.end = point;
        passiveTreeState.selectionRect = { start: drag.start, end: point };
        renderPassiveTreeCanvas();
        return;
    }
    if (drag.kind === "connect") {
        drag.current = point;
        passiveTreeState.connectionPreview = { from: pointForNode(drag.from), to: point };
        renderPassiveTreeCanvas();
        return;
    }
    if (drag.kind === "pan") {
        ptEls.passiveTreeCanvasWrap.scrollLeft = drag.scrollLeft - (event.clientX - drag.clientX);
        ptEls.passiveTreeCanvasWrap.scrollTop = drag.scrollTop - (event.clientY - drag.clientY);
        return;
    }
    const dx = point.x - drag.last.x;
    const dy = point.y - drag.last.y;
    drag.last = point;
    if (drag.kind === "moveSelection") {
        for (const groupId of drag.groupIds) {
            const group = passiveTreeState.tree.groups.find((entry) => entry.id === groupId);
            if (group) {
                translateGroup(group, dx, dy);
            }
        }
        for (const key of drag.nodeKeys) {
            const parsed = parseNodeKey(key);
            const group = parsed ? passiveTreeState.tree.groups.find((entry) => entry.id === parsed.groupId) : null;
            if (group?.shapeKind === "free") {
                translateFreeNode(group, parsed.nodeIndex, dx, dy);
            }
        }
    }
    renderPassiveTreeEditor();
}

function startPassiveTreePan(event) {
    passiveTreeState.drag = {
        kind: "pan",
        clientX: event.clientX,
        clientY: event.clientY,
        scrollLeft: ptEls.passiveTreeCanvasWrap.scrollLeft,
        scrollTop: ptEls.passiveTreeCanvasWrap.scrollTop
    };
    ptEls.passiveTreeCanvas.setPointerCapture(event.pointerId);
    event.preventDefault();
}

function handlePassiveTreeCanvasPointerUp(event) {
    const drag = passiveTreeState.drag;
    if (!drag) {
        return;
    }
    if (drag.kind === "select") {
        selectNodesInRect(normalizedRect(drag.start, drag.end));
        passiveTreeState.selectionRect = null;
        passiveTreeState.drag = null;
        renderPassiveTreeEditor();
        setPassiveTreeStatus(`${passiveTreeState.selectedNodeKeys.size} node${passiveTreeState.selectedNodeKeys.size === 1 ? "" : "s"} selected.`);
        return;
    }
    if (drag.kind === "connect") {
        finishConnectionDrag(drag, event);
        passiveTreeState.connectionPreview = null;
        passiveTreeState.drag = null;
        commitPassiveTreeHistory(drag.beforeSnapshot);
        renderPassiveTreeEditor();
        return;
    }
    passiveTreeState.drag = null;
    if (drag.kind === "moveSelection") {
        commitPassiveTreeHistory(drag.beforeSnapshot);
        syncPassiveTreeFormFromSelected();
        renderPassiveTreeEditor();
    }
}

function handlePassiveTreeCanvasDoubleClick(event) {
    const line = event.target.closest("[data-passive-tree-connection-id], [data-passive-tree-internal-link]");
    if (!line) {
        return;
    }
    const point = canvasPoint(event);
    const connectionId = line.dataset.passiveTreeConnectionId;
    if (connectionId) {
        insertNodeOnConnection(connectionId, point);
        event.preventDefault();
        return;
    }
    if (line.dataset.passiveTreeInternalLink) {
        insertNodeOnInternalLink(
            line.dataset.passiveTreeGroupId,
            Number(line.dataset.passiveTreeFromIndex),
            Number(line.dataset.passiveTreeToIndex),
            point
        );
        event.preventDefault();
    }
}

function insertNodeOnConnection(connectionId, point) {
    const index = passiveTreeState.tree.connections.findIndex((entry) => entry.id === connectionId);
    if (index < 0) {
        return;
    }
    const before = snapshotPassiveTreeState();
    const original = passiveTreeState.tree.connections[index];
    passiveTreeState.tree.connections.splice(index, 1);
    const travel = createTravelNodeAt(point);
    addConnectionIfMissing(original.from, { groupId: travel.id, nodeIndex: 0 });
    addConnectionIfMissing({ groupId: travel.id, nodeIndex: 0 }, original.to);
    passiveTreeState.selectedGroupId = travel.id;
    passiveTreeState.selectedNodeKey = nodeKey(travel.id, 0);
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
    commitPassiveTreeHistory(before);
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Inserted ${travel.nodeNames[0]} on connection.`);
}

function insertNodeOnInternalLink(groupId, fromIndex, toIndex, point) {
    const group = passiveTreeState.tree.groups.find((entry) => entry.id === groupId);
    if (!group || group.starter) {
        return;
    }
    const before = snapshotPassiveTreeState();
    if (group.shapeKind !== "free") {
        deshapeGroup(group);
    }
    const insertIndex = toIndex > fromIndex ? toIndex : fromIndex + 1;
    const name = uniqueNodeName(group, `${group.nodePrefix}_TRAVEL`);
    shiftConnectionsAfterNodeInsert(group.id, insertIndex);
    shiftRemovedInternalLinksAfterNodeInsert(group, insertIndex);
    group.freePoints.splice(insertIndex, 0, { x: point.x, y: point.y });
    group.nodeNames.splice(insertIndex, 0, name);
    group.nodeDetails.splice(insertIndex, 0, { name, kind: "TRAVEL", identity: group.identity });
    group.nodeCount = group.freePoints.length;
    passiveTreeState.selectedGroupId = group.id;
    passiveTreeState.selectedNodeKey = nodeKey(group.id, insertIndex);
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
    commitPassiveTreeHistory(before);
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Inserted ${name} on ${group.shapeName}.`);
}

function handlePassiveTreeKeyDown(event) {
    if (handlePassiveTreeUndoRedoKeyDown(event)) {
        return;
    }
    if (event.key === "Escape") {
        closePassiveTreeContextMenu();
    }
    if (event.code === "Space") {
        if (isTextInputTarget(event.target)) {
            return;
        }
        passiveTreeState.spaceHeld = true;
        event.preventDefault();
    }
}

function handlePassiveTreeUndoRedoKeyDown(event) {
    if (!isPassiveTreeEditorActive() || !(event.ctrlKey || event.metaKey)) {
        return false;
    }
    const key = event.key.toLowerCase();
    if (key !== "z" && key !== "y") {
        return false;
    }
    if (isTextInputTarget(event.target)) {
        return false;
    }
    event.preventDefault();
    if (key === "z" && !event.shiftKey) {
        undoPassiveTreeEdit();
        return true;
    }
    redoPassiveTreeEdit();
    return true;
}

function handlePassiveTreeKeyUp(event) {
    if (event.code === "Space") {
        passiveTreeState.spaceHeld = false;
        if (!isTextInputTarget(event.target)) {
            event.preventDefault();
        }
    }
}

function isTextInputTarget(target) {
    return ["INPUT", "TEXTAREA", "SELECT"].includes(target?.tagName);
}

function isPassiveTreeEditorActive() {
    const passiveTreeView = document.getElementById("passiveTreeView");
    return Boolean(passiveTreeView && !passiveTreeView.classList.contains("hidden"));
}

function snapshotPassiveTreeState() {
    if (!passiveTreeState.tree) {
        return null;
    }
    return JSON.stringify({
        tree: passiveTreeState.tree,
        selectedGroupId: passiveTreeState.selectedGroupId,
        selectedNodeKey: passiveTreeState.selectedNodeKey,
        selectedNodeKeys: [...passiveTreeState.selectedNodeKeys]
    });
}

function commitPassiveTreeHistory(beforeSnapshot) {
    if (passiveTreeState.restoring || !beforeSnapshot || beforeSnapshot === snapshotPassiveTreeState()) {
        return;
    }
    if (passiveTreeState.undoStack.at(-1) !== beforeSnapshot) {
        passiveTreeState.undoStack.push(beforeSnapshot);
        if (passiveTreeState.undoStack.length > PASSIVE_TREE_HISTORY_LIMIT) {
            passiveTreeState.undoStack.shift();
        }
    }
    passiveTreeState.redoStack = [];
}

function clearPassiveTreeHistory() {
    passiveTreeState.undoStack = [];
    passiveTreeState.redoStack = [];
}

function undoPassiveTreeEdit() {
    const snapshot = passiveTreeState.undoStack.pop();
    if (!snapshot) {
        setPassiveTreeStatus("Nothing to undo.");
        return;
    }
    const current = snapshotPassiveTreeState();
    if (current) {
        passiveTreeState.redoStack.push(current);
    }
    restorePassiveTreeSnapshot(snapshot);
    setPassiveTreeStatus("Undid passive tree edit.");
}

function redoPassiveTreeEdit() {
    const snapshot = passiveTreeState.redoStack.pop();
    if (!snapshot) {
        setPassiveTreeStatus("Nothing to redo.");
        return;
    }
    const current = snapshotPassiveTreeState();
    if (current) {
        passiveTreeState.undoStack.push(current);
    }
    restorePassiveTreeSnapshot(snapshot);
    setPassiveTreeStatus("Redid passive tree edit.");
}

function restorePassiveTreeSnapshot(snapshot) {
    const restored = JSON.parse(snapshot);
    passiveTreeState.restoring = true;
    try {
        passiveTreeState.tree = cloneTree(restored.tree);
        passiveTreeState.selectedGroupId = restored.selectedGroupId ?? passiveTreeState.tree.groups[0]?.id ?? null;
        passiveTreeState.selectedNodeKey = restored.selectedNodeKey ?? null;
        passiveTreeState.selectedNodeKeys = new Set(restored.selectedNodeKeys ?? []);
        prunePassiveTreeSelection();
        syncPassiveTreeHeader();
        syncPassiveTreeDraftControls();
        syncPassiveTreeFormFromSelected();
        renderPassiveTreeEditor();
    } finally {
        passiveTreeState.restoring = false;
    }
}

function prunePassiveTreeSelection() {
    const validKeys = validPassiveTreeNodeKeys();
    passiveTreeState.selectedNodeKeys = new Set([...passiveTreeState.selectedNodeKeys]
        .filter((key) => validKeys.has(key)));
    if (!validKeys.has(passiveTreeState.selectedNodeKey)) {
        passiveTreeState.selectedNodeKey = passiveTreeState.selectedNodeKeys.values().next().value ?? null;
    }
    const selected = parseNodeKey(passiveTreeState.selectedNodeKey);
    if (selected) {
        passiveTreeState.selectedGroupId = selected.groupId;
        return;
    }
    if (!passiveTreeState.tree.groups.some((entry) => entry.id === passiveTreeState.selectedGroupId)) {
        passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    }
}

function validPassiveTreeNodeKeys() {
    return new Set(passiveTreeState.tree.groups.flatMap((entry) =>
        computeGroupPoints(entry).map((_, index) => nodeKey(entry.id, index))));
}

function canvasPoint(event) {
    const svgPoint = ptEls.passiveTreeCanvas.createSVGPoint();
    svgPoint.x = event.clientX;
    svgPoint.y = event.clientY;
    const matrix = ptEls.passiveTreeCanvas.getScreenCTM();
    if (!matrix) {
        return { x: 0, y: 0 };
    }
    const point = svgPoint.matrixTransform(matrix.inverse());
    return { x: round(point.x), y: round(point.y) };
}

function nodeFromElement(element) {
    return {
        groupId: element.dataset.passiveTreeGroupId,
        nodeIndex: Number(element.dataset.passiveTreeNodeIndex)
    };
}

function pointForNode(ref) {
    const group = passiveTreeState.tree.groups.find((entry) => entry.id === ref.groupId);
    return group ? computeGroupPoints(group)[ref.nodeIndex] ?? { x: 0, y: 0 } : { x: 0, y: 0 };
}

function dragTargetsForSelection(ref) {
    const selectedKey = nodeKey(ref.groupId, ref.nodeIndex);
    const keys = passiveTreeState.selectedNodeKeys.has(selectedKey)
        ? [...passiveTreeState.selectedNodeKeys]
        : [selectedKey];
    const groupIds = new Set();
    const nodeKeys = [];
    for (const key of keys) {
        const parsed = parseNodeKey(key);
        const group = parsed ? passiveTreeState.tree.groups.find((entry) => entry.id === parsed.groupId) : null;
        if (!group) {
            continue;
        }
        if (group.shapeKind === "free") {
            nodeKeys.push(key);
        } else {
            groupIds.add(group.id);
        }
    }
    return {
        groupIds: [...groupIds],
        nodeKeys
    };
}

function translateGroup(entry, dx, dy) {
    if (entry.shapeKind === "point") {
        entry.params.x = round(entry.params.x + dx);
        entry.params.y = round(entry.params.y + dy);
        return;
    }
    if (entry.shapeKind === "free") {
        entry.freePoints = entry.freePoints.map((point) => ({ x: round(point.x + dx), y: round(point.y + dy) }));
        return;
    }
    if (["circle", "halfCircle", "arc"].includes(entry.shapeKind)) {
        entry.params.centerX = round(entry.params.centerX + dx);
        entry.params.centerY = round(entry.params.centerY + dy);
        return;
    }
    if (entry.shapeKind === "line") {
        entry.params.startX = round(entry.params.startX + dx);
        entry.params.startY = round(entry.params.startY + dy);
        return;
    }
    if (entry.shapeKind === "travelPath" || entry.shapeKind === "branchedTravelPath") {
        entry.params.fromX = round(entry.params.fromX + dx);
        entry.params.fromY = round(entry.params.fromY + dy);
        entry.params.toX = round(entry.params.toX + dx);
        entry.params.toY = round(entry.params.toY + dy);
        return;
    }
    if (entry.shapeKind === "twoPathDeadEnd") {
        entry.params.notableX = round(entry.params.notableX + dx);
        entry.params.notableY = round(entry.params.notableY + dy);
    }
}

function translateFreeNode(entry, nodeIndex, dx, dy) {
    const point = entry.freePoints[nodeIndex];
    if (!point) {
        return;
    }
    point.x = round(point.x + dx);
    point.y = round(point.y + dy);
}

function selectNodesInRect(rect) {
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedNodeKey = null;
    for (const group of passiveTreeState.tree.groups) {
        computeGroupPoints(group).forEach((point, index) => {
            if (point.x >= rect.x
                    && point.x <= rect.x + rect.width
                    && point.y >= rect.y
                    && point.y <= rect.y + rect.height) {
                const key = nodeKey(group.id, index);
                passiveTreeState.selectedNodeKeys.add(key);
                passiveTreeState.selectedNodeKey = passiveTreeState.selectedNodeKey ?? key;
                passiveTreeState.selectedGroupId = group.id;
            }
        });
    }
    syncPassiveTreeFormFromSelected();
}

function finishConnectionDrag(drag, event) {
    const toElement = document.elementFromPoint(event.clientX, event.clientY)?.closest("[data-passive-tree-node-key]");
    const target = toElement ? nodeFromElement(toElement) : null;
    if (target && nodeKey(target.groupId, target.nodeIndex) !== nodeKey(drag.from.groupId, drag.from.nodeIndex)) {
        addConnectionIfMissing(drag.from, target);
        passiveTreeState.selectedGroupId = target.groupId;
        passiveTreeState.selectedNodeKey = nodeKey(target.groupId, target.nodeIndex);
        passiveTreeState.selectedNodeKeys.clear();
        passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
        setPassiveTreeStatus("Connected selected nodes.");
        return;
    }
    const travel = createTravelNodeAt(drag.current);
    addConnectionIfMissing(drag.from, { groupId: travel.id, nodeIndex: 0 });
    passiveTreeState.selectedGroupId = travel.id;
    passiveTreeState.selectedNodeKey = nodeKey(travel.id, 0);
    passiveTreeState.selectedNodeKeys.clear();
    passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
    setPassiveTreeStatus(`Added travel node ${travel.nodeNames[0]}.`);
}

function createTravelNodeAt(point) {
    const id = nextGroupId();
    const name = uniqueShapeName("TRAVEL_NODE");
    const travel = normalizeGroup(group(
        id,
        "free",
        "Control",
        "TRAVEL",
        name,
        1,
        "TRAVEL",
        {},
        [name],
        {
            freePoints: [{ x: point.x, y: point.y }],
            nodeDetails: [{ name, kind: "TRAVEL", identity: "Control" }]
        }
    ));
    passiveTreeState.tree.groups.push(travel);
    return travel;
}

function connection(from, to) {
    return {
        id: `connection_${Date.now()}_${passiveTreeState.tree.connections.length + 1}`,
        from: normalizeNodeRef(from),
        to: normalizeNodeRef(to),
        kind: "travel"
    };
}

function midpoint(from, to) {
    return {
        x: round((from.x + to.x) / 2),
        y: round((from.y + to.y) / 2)
    };
}

function quickShapeNodeCount(shapeKind, previousEffectiveCount) {
    const count = Math.max(1, previousEffectiveCount || 1);
    if (shapeKind === "point") {
        return 1;
    }
    if (shapeKind === "twoPathDeadEnd") {
        return clampedNodeCount(shapeKind, Math.max(1, Math.round((count - 1) / 2)));
    }
    if (shapeKind === "branchedTravelPath") {
        return clampedNodeCount(shapeKind, Math.max(1, count - 1));
    }
    return clampedNodeCount(shapeKind, count);
}

function quickShapeParams(shapeKind, points, nodeCount, anchorIndex) {
    const anchor = points[anchorIndex] ?? points[0] ?? { x: 360, y: 300 };
    const center = points.length > 1 ? centroid(points) : anchor;
    const radius = averageRadius(points, center, 74);
    if (shapeKind === "point") {
        return { x: anchor.x, y: anchor.y };
    }
    if (shapeKind === "circle") {
        return { centerX: center.x, centerY: center.y, radius, startDegrees: 200 };
    }
    if (shapeKind === "halfCircle") {
        return { centerX: center.x, centerY: center.y, radius, startDegrees: 180 };
    }
    if (shapeKind === "arc") {
        return { centerX: center.x, centerY: center.y, radius: Math.max(radius, 96), startDegrees: -70, sweepDegrees: 140 };
    }
    if (shapeKind === "line") {
        const first = points[0] ?? anchor;
        const last = points.length > 1 ? points[points.length - 1] : { x: anchor.x + 42, y: anchor.y };
        const divisor = Math.max(1, nodeCount - 1);
        let xStep = round((last.x - first.x) / divisor);
        let yStep = round((last.y - first.y) / divisor);
        if (xStep === 0 && yStep === 0) {
            xStep = 42;
        }
        return { startX: first.x, startY: first.y, xStep, yStep };
    }
    if (shapeKind === "travelPath") {
        const frame = endpointFrame(points, nodeCount, anchor);
        return { fromX: frame.from.x, fromY: frame.from.y, toX: frame.to.x, toY: frame.to.y };
    }
    if (shapeKind === "branchedTravelPath") {
        const frame = endpointFrame(points, nodeCount, anchor);
        return {
            fromX: frame.from.x,
            fromY: frame.from.y,
            toX: frame.to.x,
            toY: frame.to.y,
            branchIndex: Math.max(0, Math.min(nodeCount - 1, Math.floor((nodeCount - 1) / 2))),
            xBranchOffset: 0,
            yBranchOffset: -58
        };
    }
    if (shapeKind === "twoPathDeadEnd") {
        const notable = anchor;
        const first = points[0] ?? { x: notable.x - 62 * nodeCount, y: notable.y };
        let xStepFromNotable = round((first.x - notable.x) / nodeCount);
        let yStepFromNotable = round((first.y - notable.y) / nodeCount);
        if (xStepFromNotable === 0 && yStepFromNotable === 0) {
            xStepFromNotable = -62;
        }
        return {
            notableX: notable.x,
            notableY: notable.y,
            xStepFromNotable,
            yStepFromNotable,
            xBranchOffset: 0,
            yBranchOffset: 42
        };
    }
    return defaultParams(shapeKind);
}

function quickShapeNodeNames(group, previousNames, anchorIndex) {
    const count = effectiveNodeCount(group.shapeKind, group.nodeCount);
    const generated = suggestedNodeNames(group.nodePrefix, group.nodeCount, group.shapeKind);
    return Array.from({ length: count }, (_, index) => {
        const sourceIndex = group.shapeKind === "point" ? anchorIndex : index;
        return previousNames[sourceIndex] || generated[index] || `${group.nodePrefix}_${index + 1}`;
    });
}

function quickShapeNodeDetails(group, previousDetails, nodeNames, anchorIndex) {
    return nodeNames.map((name, index) => {
        const sourceIndex = group.shapeKind === "point" ? anchorIndex : index;
        const source = previousDetails[sourceIndex] ?? {};
        return normalizeNodeDetail({
            ...source,
            name,
            kind: source.kind && source.kind !== "STARTER" ? source.kind : group.nodeKind,
            identity: source.identity || group.identity,
            alwaysAllocated: false,
            grantsNothing: false
        });
    });
}

function centroid(points) {
    const sum = points.reduce((acc, point) => ({
        x: acc.x + point.x,
        y: acc.y + point.y
    }), { x: 0, y: 0 });
    return {
        x: round(sum.x / points.length),
        y: round(sum.y / points.length)
    };
}

function averageRadius(points, center, fallback) {
    const distances = points
        .map((point) => Math.hypot(point.x - center.x, point.y - center.y))
        .filter((distance) => distance > 0);
    if (!distances.length) {
        return fallback;
    }
    return Math.max(36, round(distances.reduce((sum, distance) => sum + distance, 0) / distances.length));
}

function endpointFrame(points, nodeCount, anchor) {
    if (points.length >= 2) {
        const first = points[0];
        const last = points[points.length - 1];
        const divisor = Math.max(1, points.length - 1);
        const xStep = round((last.x - first.x) / divisor);
        const yStep = round((last.y - first.y) / divisor);
        return {
            from: { x: round(first.x - xStep), y: round(first.y - yStep) },
            to: { x: round(last.x + xStep), y: round(last.y + yStep) }
        };
    }
    return {
        from: { x: round(anchor.x - 42), y: anchor.y },
        to: { x: round(anchor.x + 42), y: anchor.y }
    };
}

function contextRotateDegrees() {
    const input = ptEls.passiveTreeNodeContextMenu.querySelector("[data-passive-tree-rotate-degrees]");
    const degrees = Number(input?.value);
    if (!Number.isFinite(degrees) || degrees === 0) {
        return 15;
    }
    return Math.max(0.1, Math.min(360, Math.abs(degrees)));
}

function transformPassiveTreePoint(point, operation, degrees, origin) {
    if (operation === "flipHorizontal") {
        return {
            x: round(origin.x * 2 - point.x),
            y: round(point.y)
        };
    }
    if (operation === "flipVertical") {
        return {
            x: round(point.x),
            y: round(origin.y * 2 - point.y)
        };
    }
    const radians = degrees * Math.PI / 180;
    const dx = point.x - origin.x;
    const dy = point.y - origin.y;
    return {
        x: round(origin.x + dx * Math.cos(radians) - dy * Math.sin(radians)),
        y: round(origin.y + dx * Math.sin(radians) + dy * Math.cos(radians))
    };
}

function transformPassiveTreeVector(vector, operation, degrees) {
    if (operation === "flipHorizontal") {
        return { x: round(-vector.x), y: round(vector.y) };
    }
    if (operation === "flipVertical") {
        return { x: round(vector.x), y: round(-vector.y) };
    }
    const radians = degrees * Math.PI / 180;
    return {
        x: round(vector.x * Math.cos(radians) - vector.y * Math.sin(radians)),
        y: round(vector.x * Math.sin(radians) + vector.y * Math.cos(radians))
    };
}

function transformPassiveTreeAngle(degrees, operation) {
    if (operation === "flipHorizontal") {
        return normalizeDegrees(180 - degrees);
    }
    if (operation === "flipVertical") {
        return normalizeDegrees(-degrees);
    }
    return normalizeDegrees(degrees);
}

function circleFlipIndexMap(count) {
    return new Map(Array.from({ length: count }, (_, index) => [index, (count - index) % count]));
}

function reverseIndexMap(count) {
    return new Map(Array.from({ length: count }, (_, index) => [index, count - index - 1]));
}

function reorderPassiveTreeGroupNodes(group, indexMap) {
    const count = effectiveNodeCount(group.shapeKind, group.nodeCount);
    const names = normalizedNodeNames(group);
    const details = Array.from({ length: count }, (_, index) => normalizeNodeDetail(nodeDetail(group, index)));
    const nextNames = Array.from({ length: count }, (_, index) => names[index]);
    const nextDetails = Array.from({ length: count }, (_, index) => details[index]);
    for (const [from, to] of indexMap.entries()) {
        nextNames[to] = names[from];
        nextDetails[to] = details[from];
    }
    group.nodeNames = nextNames;
    group.nodeDetails = nextDetails;
    group.removedInternalLinks = remapInternalLinkKeys(group.removedInternalLinks, indexMap);
}

function remapInternalLinkKeys(keys, indexMap) {
    return normalizedInternalLinkKeys((keys ?? []).map((key) => {
        const [from, to] = normalizeInternalLinkKey(key).split(":").map(Number);
        if (!Number.isInteger(from) || !Number.isInteger(to)) {
            return "";
        }
        return internalLinkKey(indexMap.get(from) ?? from, indexMap.get(to) ?? to);
    }));
}

function remapPassiveTreeGroupRefs(groupId, indexMap) {
    passiveTreeState.tree.connections = passiveTreeState.tree.connections.map((entry) => ({
        ...entry,
        from: remapPassiveTreeNodeRef(entry.from, groupId, indexMap),
        to: remapPassiveTreeNodeRef(entry.to, groupId, indexMap)
    }));
    passiveTreeState.selectedNodeKey = remapPassiveTreeNodeKey(passiveTreeState.selectedNodeKey, groupId, indexMap);
    passiveTreeState.contextNodeKey = remapPassiveTreeNodeKey(passiveTreeState.contextNodeKey, groupId, indexMap);
    passiveTreeState.selectedNodeKeys = new Set([...passiveTreeState.selectedNodeKeys]
        .map((key) => remapPassiveTreeNodeKey(key, groupId, indexMap))
        .filter(Boolean));
}

function remapPassiveTreeNodeRef(ref, groupId, indexMap) {
    if (ref.groupId !== groupId || !indexMap.has(ref.nodeIndex)) {
        return ref;
    }
    return {
        ...ref,
        nodeIndex: indexMap.get(ref.nodeIndex)
    };
}

function remapPassiveTreeNodeKey(key, groupId, indexMap) {
    const parsed = parseNodeKey(key);
    if (!parsed || parsed.groupId !== groupId || !indexMap.has(parsed.nodeIndex)) {
        return key;
    }
    return nodeKey(groupId, indexMap.get(parsed.nodeIndex));
}

function normalizeDegrees(degrees) {
    const normalized = Number(degrees) % 360;
    return normalized < -180 ? normalized + 360 : normalized > 180 ? normalized - 360 : normalized;
}

function formatDegrees(degrees) {
    return `${Number.isInteger(degrees) ? degrees : Number(degrees).toFixed(1)} degrees`;
}

function passiveTreeTransformLabel(operation, degrees = 0) {
    if (operation === "rotate") {
        return `Rotated ${formatDegrees(degrees)}`;
    }
    return operation === "flipHorizontal" ? "Flipped horizontally" : "Flipped vertically";
}

function normalizedRect(start, end) {
    const x = Math.min(start.x, end.x);
    const y = Math.min(start.y, end.y);
    return {
        x,
        y,
        width: Math.abs(end.x - start.x),
        height: Math.abs(end.y - start.y)
    };
}

function saveCurrentShapeTemplate() {
    const templateGroup = readFormGroup(nextGroupId());
    const template = normalizeShapeTemplate({
        id: `${Date.now()}_${templateGroup.shapeName.toLowerCase()}`,
        label: `${templateGroup.shapeName} (${SHAPE_LABELS[templateGroup.shapeKind] ?? templateGroup.shapeKind})`,
        group: templateGroup
    });
    passiveTreeState.savedShapes = [
        template,
        ...passiveTreeState.savedShapes.filter((entry) => entry.label !== template.label)
    ].slice(0, 24);
    writeJsonStorage(PASSIVE_TREE_SHAPES_KEY, passiveTreeState.savedShapes);
    renderSavedShapes();
    setPassiveTreeStatus(`Saved shape ${template.label}.`);
}

function handleSavedShapeClick(event) {
    const loadButton = event.target.closest("[data-passive-tree-load-shape]");
    if (loadButton) {
        const template = passiveTreeState.savedShapes.find((entry) => entry.id === loadButton.dataset.passiveTreeLoadShape);
        if (template) {
            writeGroupToForm(template.group);
            setPassiveTreeStatus(`Loaded shape ${template.label}.`);
        }
        return;
    }
    const deleteButton = event.target.closest("[data-passive-tree-delete-shape]");
    if (deleteButton) {
        passiveTreeState.savedShapes = passiveTreeState.savedShapes
            .filter((entry) => entry.id !== deleteButton.dataset.passiveTreeDeleteShape);
        writeJsonStorage(PASSIVE_TREE_SHAPES_KEY, passiveTreeState.savedShapes);
        renderSavedShapes();
        setPassiveTreeStatus("Deleted saved shape.");
    }
}

function writeGroupToForm(entry) {
    ptEls.passiveTreeShapeKind.value = entry.shapeKind;
    ptEls.passiveTreeNodeCount.value = entry.nodeCount;
    setIdentityControls(entry);
    ptEls.passiveTreeNodePrefix.value = entry.nodePrefix;
    ptEls.passiveTreeShapeName.value = uniqueShapeName(entry.shapeName);
    ptEls.passiveTreeNodeKind.value = entry.nodeKind;
    renderShapeParams(entry.shapeKind, entry.params);
    ptEls.passiveTreeNodeNames.value = normalizedNodeNames(entry).join("\n");
    updateShapeMeta();
}

function savePassiveTree() {
    persistActiveDraft(true);
    syncPassiveTreeDraftControls();
    setPassiveTreeStatus(`Saved draft ${activeDraft().name}.`);
}

function resetPassiveTree() {
    const before = snapshotPassiveTreeState();
    const previous = passiveTreeState.tree;
    passiveTreeState.tree = defaultTree();
    passiveTreeState.tree.treeId = previous.treeId;
    passiveTreeState.tree.packageName = previous.packageName;
    passiveTreeState.tree.className = previous.className;
    passiveTreeState.tree.canvas = previous.canvas;
    passiveTreeState.selectedGroupId = passiveTreeState.tree.groups[0]?.id ?? null;
    passiveTreeState.selectedNodeKey = null;
    passiveTreeState.selectedNodeKeys.clear();
    commitPassiveTreeHistory(before);
    persistActiveDraft(true);
    syncPassiveTreeDraftControls();
    syncPassiveTreeHeader();
    syncPassiveTreeFormFromSelected();
    renderPassiveTreeEditor();
    setPassiveTreeStatus(`Reset draft ${activeDraft().name} to the crusher starter layout.`);
}

function regenerateFormNodeNames() {
    const shapeKind = ptEls.passiveTreeShapeKind.value;
    const count = clampedNodeCount(shapeKind, ptEls.passiveTreeNodeCount.value);
    const prefix = toConstantName(ptEls.passiveTreeNodePrefix.value || currentIdentity(), "PASSIVE_NODE");
    ptEls.passiveTreeNodePrefix.value = prefix;
    ptEls.passiveTreeNodeNames.value = suggestedNodeNames(prefix, count, shapeKind).join("\n");
    updateShapeMeta();
}

function readFormGroup(id) {
    const shapeKind = ptEls.passiveTreeShapeKind.value;
    const nodeCount = clampedNodeCount(shapeKind, ptEls.passiveTreeNodeCount.value);
    const identityState = currentIdentityState();
    const identity = identityState.identity;
    const nodePrefix = toConstantName(ptEls.passiveTreeNodePrefix.value || identity, "PASSIVE_NODE");
    const shapeName = toConstantName(ptEls.passiveTreeShapeName.value || `${nodePrefix}_CLUSTER`, `${nodePrefix}_CLUSTER`);
    const selected = selectedGroup();
    const draft = group(
        id,
        shapeKind,
        identity,
        nodePrefix,
        shapeName,
        shapeKind === "free" && selected?.shapeKind === "free" ? selected.nodeCount : nodeCount,
        ptEls.passiveTreeNodeKind.value,
        shapeKind === "free" ? {} : readShapeParams(shapeKind),
        nodeNamesFromText(),
        identityState
    );
    if (shapeKind === "free" && selected?.shapeKind === "free") {
        draft.freePoints = selected.freePoints.map((point) => ({ ...point }));
        draft.nodeDetails = selected.nodeDetails.map((detail) => ({ ...detail }));
        if (selected.source) {
            draft.source = JSON.parse(JSON.stringify(selected.source));
        }
    }
    draft.nodeNames = normalizedNodeNames(draft);
    return draft;
}

function readShapeParams(shapeKind) {
    const fallback = defaultParams(shapeKind);
    const params = { ...fallback };
    ptEls.passiveTreeShapeParams.querySelectorAll("[data-passive-tree-param]").forEach((input) => {
        params[input.dataset.passiveTreeParam] = Number.isFinite(Number(input.value))
            ? Number(input.value)
            : fallback[input.dataset.passiveTreeParam] ?? 0;
    });
    return params;
}

function currentIdentity() {
    const selected = ptEls.passiveTreeIdentitySelect.value;
    if (selected === "Custom") {
        return ptEls.passiveTreeCustomIdentity.value.trim() || "Custom";
    }
    return selected || "Custom";
}

function currentSecondaryIdentity() {
    if (!ptEls.passiveTreeSecondaryIdentitySelect) {
        return "";
    }
    const selected = ptEls.passiveTreeSecondaryIdentitySelect.value;
    if (!selected) {
        return "";
    }
    if (selected === "Custom") {
        return ptEls.passiveTreeSecondaryCustomIdentity?.value.trim() || "";
    }
    return selected;
}

function currentIdentityState() {
    return normalizeIdentityState(
            currentIdentity(),
            ptEls.passiveTreeIdentityNegative?.checked ?? false,
            currentSecondaryIdentity(),
            ptEls.passiveTreeSecondaryIdentityNegative?.checked ?? false
    );
}

function identityValueFromNodeControls(property, fallback) {
    const select = ptEls.passiveTreeNodeProperties.querySelector(`[data-passive-tree-node-property="${property}"]`);
    if (!select) {
        return fallback;
    }
    if (!select.value) {
        return "";
    }
    if (select.value === "Custom") {
        const customProperty = property === "identity" ? "customIdentity" : "customSecondaryIdentity";
        const custom = ptEls.passiveTreeNodeProperties
                .querySelector(`[data-passive-tree-node-property="${customProperty}"]`)
                ?.value
                ?.trim();
        return custom || "Custom";
    }
    return select.value;
}

function nodeNamesFromText() {
    return ptEls.passiveTreeNodeNames.value
        .split(/\r?\n/)
        .map((line) => toConstantName(line, ""))
        .filter(Boolean);
}

function selectedGroup() {
    return passiveTreeState.tree.groups.find((entry) => entry.id === passiveTreeState.selectedGroupId) ?? null;
}

function selectedNode() {
    return passiveTreeNodeForKey(passiveTreeState.selectedNodeKey);
}

function contextNode() {
    return passiveTreeNodeForKey(passiveTreeState.contextNodeKey);
}

function passiveTreeNodeForKey(key) {
    if (!key) {
        return null;
    }
    const parsed = parseNodeKey(key);
    if (!parsed) {
        return null;
    }
    const group = passiveTreeState.tree.groups.find((entry) => entry.id === parsed.groupId);
    if (!group || parsed.nodeIndex >= computeGroupPoints(group).length) {
        return null;
    }
    return {
        group,
        index: parsed.nodeIndex,
        key,
        detail: nodeDetail(group, parsed.nodeIndex)
    };
}

function contextSelectedNodes() {
    const selected = contextNode();
    if (!selected) {
        return [];
    }
    if (!passiveTreeState.selectedNodeKeys.has(selected.key)) {
        return [selected];
    }
    const keys = [
        selected.key,
        ...[...passiveTreeState.selectedNodeKeys].filter((key) => key !== selected.key)
    ];
    return keys
        .map((key) => passiveTreeNodeForKey(key))
        .filter(Boolean);
}

function contextSelectedGroupTargets(targets) {
    const groups = new Map();
    for (const target of targets) {
        if (!groups.has(target.group.id)) {
            groups.set(target.group.id, {
                group: target.group,
                index: target.index
            });
        }
    }
    return [...groups.values()];
}

function focusPassiveTreeGroupTarget(target) {
    if (!target) {
        return;
    }
    const index = Math.min(target.index, effectiveNodeCount(target.group.shapeKind, target.group.nodeCount) - 1);
    if (index < 0) {
        return;
    }
    passiveTreeState.selectedGroupId = target.group.id;
    passiveTreeState.selectedNodeKey = nodeKey(target.group.id, index);
    passiveTreeState.selectedNodeKeys.add(passiveTreeState.selectedNodeKey);
}

function buildPassiveTreeExport() {
    const draft = activeDraft();
    return {
        schema: PASSIVE_TREE_SCHEMA,
        draftId: draft.id,
        draftName: draft.name,
        draftCreatedAt: draft.createdAt,
        draftUpdatedAt: draft.updatedAt,
        createdFromDraftId: draft.createdFromDraftId,
        treeId: passiveTreeState.tree.treeId,
        packageName: passiveTreeState.tree.packageName,
        className: passiveTreeState.tree.className,
        canvas: passiveTreeState.tree.canvas,
        datapackCandidatePath: `data/rngtech/passive_trees/${passiveTreeState.tree.treeId}.json`,
        draftCandidatePath: `tools/moddex/passive-tree-drafts/${passiveTreeState.tree.treeId}/${draft.id}.json`,
        groups: passiveTreeState.tree.groups.map((entry) => {
            const nodeNames = normalizedNodeNames(entry);
            const groupIdentities = exportIdentities(entry);
            return {
                id: entry.id,
                starter: entry.starter,
                identity: entry.identity,
                identityNegative: entry.identityNegative,
                secondaryIdentity: entry.secondaryIdentity || undefined,
                secondaryIdentityNegative: entry.secondaryIdentityNegative,
                identities: groupIdentities,
                hybrid: groupIdentities.length > 1,
                nodeKind: entry.nodeKind,
                shape: {
                    kind: entry.shapeKind,
                    name: entry.shapeName,
                    nodeCount: entry.nodeCount,
                    params: entry.params,
                    freePoints: entry.shapeKind === "free" ? entry.freePoints : undefined,
                    removedInternalLinks: entry.removedInternalLinks?.length ? entry.removedInternalLinks : undefined
                },
                nodes: computeGroupPoints(entry).map((point, index) => {
                    const detail = nodeDetail(entry, index);
                    const identities = exportIdentities(detail);
                    return {
                        id: nodeKey(entry.id, index),
                        name: detail.name ?? nodeNames[index],
                        x: point.x,
                        y: point.y,
                        accessor: point.accessor,
                        identity: detail.identity,
                        identityNegative: detail.identityNegative,
                        secondaryIdentity: detail.secondaryIdentity || undefined,
                        secondaryIdentityNegative: detail.secondaryIdentityNegative,
                        identities,
                        hybrid: identities.length > 1,
                        nodeKind: detail.kind,
                        size: detail.size,
                        alwaysAllocated: detail.alwaysAllocated,
                        grantsNothing: detail.grantsNothing
                    };
                })
            };
        }),
        connections: passiveTreeState.tree.connections.map((entry) => connectionExport(entry))
    };
}

function exportIdentities(state) {
    const entries = [{
        role: "primary",
        label: state.identity,
        negative: Boolean(state.identityNegative)
    }];
    if (state.secondaryIdentity) {
        entries.push({
            role: "secondary",
            label: state.secondaryIdentity,
            negative: Boolean(state.secondaryIdentityNegative)
        });
    }
    return entries;
}

function connectionExport(entry) {
    const fromGroup = passiveTreeState.tree.groups.find((groupEntry) => groupEntry.id === entry.from.groupId);
    const toGroup = passiveTreeState.tree.groups.find((groupEntry) => groupEntry.id === entry.to.groupId);
    return {
        id: entry.id,
        kind: entry.kind,
        from: {
            id: nodeKey(entry.from.groupId, entry.from.nodeIndex),
            groupId: entry.from.groupId,
            nodeIndex: entry.from.nodeIndex,
            name: fromGroup ? nodeDetail(fromGroup, entry.from.nodeIndex).name : null
        },
        to: {
            id: nodeKey(entry.to.groupId, entry.to.nodeIndex),
            groupId: entry.to.groupId,
            nodeIndex: entry.to.nodeIndex,
            name: toGroup ? nodeDetail(toGroup, entry.to.nodeIndex).name : null
        }
    };
}

function generateJavaLayout() {
    const tree = passiveTreeState.tree;
    const lines = [
        `package ${tree.packageName};`,
        "",
        `final class ${tree.className} {`
    ];
    for (const entry of tree.groups) {
        appendJavaGroup(lines, entry);
    }
    lines.push(`    private ${tree.className}() {`);
    lines.push("    }");
    lines.push("}");
    return lines.join("\n");
}

function appendJavaGroup(lines, entry) {
    const nodeNames = normalizedNodeNames(entry);
    const points = computeGroupPoints(entry);
    if (entry.shapeKind === "point" || entry.shapeKind === "free") {
        points.forEach((point, index) => {
            lines.push(`    static final PassiveTreeLayouts.Point ${nodeNames[index]} = PassiveTreeLayouts.Point.of(${round(point.x)}, ${round(point.y)});`);
        });
        lines.push("");
        return;
    }
    const javaType = javaShapeType(entry.shapeKind);
    lines.push(`    private static final PassiveTreeLayouts.${javaType} ${entry.shapeName} = PassiveTreeLayouts.${javaMethod(entry.shapeKind)}(`);
    javaArgs(entry).forEach((arg, index, args) => {
        lines.push(`            ${arg}${index === args.length - 1 ? "" : ","}`);
    });
    lines.push("    );");
    points.forEach((point, index) => {
        lines.push(`    static final PassiveTreeLayouts.Point ${nodeNames[index]} = ${entry.shapeName}.${point.accessor};`);
    });
    lines.push("");
}

function javaArgs(entry) {
    const p = entry.params;
    if (entry.shapeKind === "circle") {
        return [javaPoint(p.centerX, p.centerY), intArg(p.radius), doubleArg(p.startDegrees), intArg(entry.nodeCount)];
    }
    if (entry.shapeKind === "halfCircle") {
        return [javaPoint(p.centerX, p.centerY), intArg(p.radius), doubleArg(p.startDegrees), intArg(entry.nodeCount)];
    }
    if (entry.shapeKind === "arc") {
        return [javaPoint(p.centerX, p.centerY), intArg(p.radius), doubleArg(p.startDegrees), doubleArg(p.sweepDegrees), intArg(entry.nodeCount)];
    }
    if (entry.shapeKind === "line") {
        return [javaPoint(p.startX, p.startY), intArg(p.xStep), intArg(p.yStep), intArg(entry.nodeCount)];
    }
    if (entry.shapeKind === "travelPath") {
        return [javaPoint(p.fromX, p.fromY), javaPoint(p.toX, p.toY), intArg(entry.nodeCount)];
    }
    if (entry.shapeKind === "twoPathDeadEnd") {
        return [
            javaPoint(p.notableX, p.notableY),
            intArg(entry.nodeCount),
            intArg(p.xStepFromNotable),
            intArg(p.yStepFromNotable),
            intArg(p.xBranchOffset),
            intArg(p.yBranchOffset)
        ];
    }
    if (entry.shapeKind === "branchedTravelPath") {
        return [
            javaPoint(p.fromX, p.fromY),
            javaPoint(p.toX, p.toY),
            intArg(entry.nodeCount),
            intArg(p.branchIndex),
            intArg(p.xBranchOffset),
            intArg(p.yBranchOffset)
        ];
    }
    return [];
}

function computeGroupPoints(entry) {
    if (entry.shapeKind === "free") {
        return entry.freePoints.map((point, index) => pointWithAccessor(point.x, point.y, `free(${index})`));
    }
    const p = entry.params;
    const count = clampedNodeCount(entry.shapeKind, entry.nodeCount);
    if (entry.shapeKind === "point") {
        return [pointWithAccessor(p.x, p.y, "")];
    }
    if (entry.shapeKind === "line") {
        return Array.from({ length: count }, (_, index) =>
            pointWithAccessor(p.startX + p.xStep * index, p.startY + p.yStep * index, `point(${index})`));
    }
    if (entry.shapeKind === "travelPath") {
        return Array.from({ length: count }, (_, index) => {
            const progress = (index + 1) / (count + 1);
            return pointWithAccessor(
                interpolate(p.fromX, p.toX, progress),
                interpolate(p.fromY, p.toY, progress),
                `point(${index})`
            );
        });
    }
    if (entry.shapeKind === "arc") {
        return arcPoints(p.centerX, p.centerY, p.radius, p.startDegrees, p.sweepDegrees, count);
    }
    if (entry.shapeKind === "halfCircle") {
        return arcPoints(p.centerX, p.centerY, p.radius, p.startDegrees, 180, count);
    }
    if (entry.shapeKind === "circle") {
        return Array.from({ length: count }, (_, index) =>
            pointOnCircle(p.centerX, p.centerY, p.radius, p.startDegrees + 360 * index / count, `point(${index})`));
    }
    if (entry.shapeKind === "twoPathDeadEnd") {
        return twoPathDeadEndPoints(entry);
    }
    if (entry.shapeKind === "branchedTravelPath") {
        return branchedTravelPathPoints(entry);
    }
    return [];
}

function arcPoints(centerX, centerY, radius, startDegrees, sweepDegrees, count) {
    const divisor = Math.max(1, count - 1);
    return Array.from({ length: count }, (_, index) =>
        pointOnCircle(centerX, centerY, radius, startDegrees + sweepDegrees * index / divisor, `point(${index})`));
}

function twoPathDeadEndPoints(entry) {
    const p = entry.params;
    const pathCount = clampedNodeCount(entry.shapeKind, entry.nodeCount);
    const first = [];
    const second = [];
    for (let index = 0; index < pathCount; index++) {
        const stepsFromNotable = pathCount - index;
        const baseX = p.notableX + p.xStepFromNotable * stepsFromNotable;
        const baseY = p.notableY + p.yStepFromNotable * stepsFromNotable;
        first.push(pointWithAccessor(baseX + p.xBranchOffset, baseY + p.yBranchOffset, `first(${index})`));
        second.push(pointWithAccessor(baseX - p.xBranchOffset, baseY - p.yBranchOffset, `second(${index})`));
    }
    return [
        ...first,
        ...second,
        pointWithAccessor(p.notableX, p.notableY, "notable()")
    ];
}

function branchedTravelPathPoints(entry) {
    const p = entry.params;
    const trunk = computeGroupPoints({
        ...entry,
        shapeKind: "travelPath",
        params: {
            fromX: p.fromX,
            fromY: p.fromY,
            toX: p.toX,
            toY: p.toY
        }
    }).map((point, index) => ({ ...point, accessor: `trunk(${index})` }));
    const branchIndex = Math.max(0, Math.min(trunk.length - 1, Math.trunc(p.branchIndex)));
    const branchBase = trunk[branchIndex] ?? { x: p.fromX, y: p.fromY };
    return [
        ...trunk,
        pointWithAccessor(branchBase.x + p.xBranchOffset, branchBase.y + p.yBranchOffset, "branch()")
    ];
}

function groupLinks(entry, points) {
    if (points.length < 2) {
        return [];
    }
    let links;
    if (entry.shapeKind === "twoPathDeadEnd") {
        const pathCount = clampedNodeCount(entry.shapeKind, entry.nodeCount);
        const first = indexedPoints(points, 0, pathCount);
        const second = indexedPoints(points, pathCount, pathCount);
        const notable = { point: points[points.length - 1], index: points.length - 1 };
        links = [
            ...sequentialLinks(first),
            ...sequentialLinks(second),
            linkBetween(first[first.length - 1], notable),
            linkBetween(second[second.length - 1], notable)
        ].filter((link) => link.from && link.to);
        return activeInternalLinks(entry, links);
    }
    if (entry.shapeKind === "branchedTravelPath") {
        const branch = { point: points[points.length - 1], index: points.length - 1 };
        const trunk = indexedPoints(points, 0, points.length - 1);
        const branchIndex = Math.max(0, Math.min(trunk.length - 1, Math.trunc(entry.params.branchIndex)));
        links = [
            ...sequentialLinks(trunk),
            linkBetween(trunk[branchIndex], branch)
        ].filter((link) => link.from && link.to);
        return activeInternalLinks(entry, links);
    }
    links = sequentialLinks(indexedPoints(points, 0, points.length));
    return activeInternalLinks(entry, links);
}

function activeInternalLinks(entry, links) {
    const removed = new Set(entry.removedInternalLinks ?? []);
    return links.filter((link) => !removed.has(internalLinkKey(link.fromIndex, link.toIndex)));
}

function indexedPoints(points, start, count) {
    return points.slice(start, start + count).map((point, offset) => ({ point, index: start + offset }));
}

function sequentialLinks(indexed) {
    const links = [];
    for (let index = 1; index < indexed.length; index++) {
        links.push(linkBetween(indexed[index - 1], indexed[index]));
    }
    return links;
}

function linkBetween(from, to) {
    return {
        from: from?.point,
        to: to?.point,
        fromIndex: from?.index ?? -1,
        toIndex: to?.index ?? -1
    };
}

function normalizedNodeNames(entry) {
    const expectedCount = effectiveNodeCount(entry.shapeKind, entry.nodeCount);
    const base = Array.isArray(entry.nodeNames)
        ? entry.nodeNames.map((name) => toConstantName(name, "")).filter(Boolean)
        : [];
    entry.nodeDetails?.forEach((detail, index) => {
        if (detail?.name) {
            base[index] = toConstantName(detail.name, "");
        }
    });
    const generated = suggestedNodeNames(entry.nodePrefix, entry.nodeCount, entry.shapeKind);
    return Array.from({ length: expectedCount }, (_, index) =>
        base[index] || generated[index] || `${entry.nodePrefix}_${index + 1}`);
}

function suggestedNodeNames(prefix, nodeCount, shapeKind) {
    const cleanPrefix = toConstantName(prefix, "PASSIVE_NODE");
    const count = effectiveNodeCount(shapeKind, nodeCount);
    if (shapeKind === "point") {
        return [cleanPrefix];
    }
    if (shapeKind === "free") {
        return Array.from({ length: nodeCount }, (_, index) => `${cleanPrefix}_${index + 1}`);
    }
    if (shapeKind === "twoPathDeadEnd") {
        const pathCount = clampedNodeCount(shapeKind, nodeCount);
        return [
            ...Array.from({ length: pathCount }, (_, index) => `${cleanPrefix}_FIRST_${index + 1}`),
            ...Array.from({ length: pathCount }, (_, index) => `${cleanPrefix}_SECOND_${index + 1}`),
            `${cleanPrefix}_NOTABLE`
        ];
    }
    if (shapeKind === "branchedTravelPath") {
        return [
            ...Array.from({ length: clampedNodeCount(shapeKind, nodeCount) }, (_, index) => `${cleanPrefix}_TRUNK_${index + 1}`),
            `${cleanPrefix}_BRANCH`
        ];
    }
    return Array.from({ length: count }, (_, index) => `${cleanPrefix}_${index + 1}`);
}

function effectiveNodeCount(shapeKind, nodeCount) {
    const count = clampedNodeCount(shapeKind, nodeCount);
    if (shapeKind === "point") {
        return 1;
    }
    if (shapeKind === "twoPathDeadEnd") {
        return count * 2 + 1;
    }
    if (shapeKind === "branchedTravelPath") {
        return count + 1;
    }
    return count;
}

function defaultTree() {
    return {
        treeId: "crusher",
        packageName: DEFAULT_PACKAGE,
        className: DEFAULT_CLASS,
        canvas: { ...DEFAULT_CANVAS },
        groups: DEFAULT_GROUPS.map((entry) => normalizeGroup(entry)),
        connections: []
    };
}

function defaultDraftCollection() {
    const tree = defaultTree();
    const draft = {
        id: "crusher_default",
        name: "Crusher Default",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        createdFromDraftId: null,
        tree
    };
    return {
        activeDraftId: draft.id,
        drafts: [draft]
    };
}

function emptyDraftTree(sourceTree) {
    return ensureStarterTree({
        treeId: sourceTree.treeId,
        packageName: sourceTree.packageName,
        className: sourceTree.className,
        canvas: { ...sourceTree.canvas },
        groups: [],
        connections: []
    });
}

function activeDraft() {
    return passiveTreeState.drafts.find((draft) => draft.id === passiveTreeState.activeDraftId)
        ?? passiveTreeState.drafts[0];
}

function cloneTree(tree) {
    return normalizeTree(JSON.parse(JSON.stringify(tree)));
}

function writeDraftCollection() {
    writeJsonStorage(PASSIVE_TREE_STORAGE_KEY, {
        schema: PASSIVE_TREE_SCHEMA,
        activeDraftId: passiveTreeState.activeDraftId,
        drafts: passiveTreeState.drafts
    });
}

function normalizeTree(stored) {
    const canvas = stored.canvas ?? DEFAULT_CANVAS;
    return ensureStarterTree({
        multipleStarts: stored.multipleStarts === true,
        treeId: normalizedIdentifier(stored.treeId, "crusher"),
        packageName: stored.packageName || DEFAULT_PACKAGE,
        className: normalizedClassName(stored.className, DEFAULT_CLASS),
        canvas: {
            width: clampedInt(canvas.width, 320, 10000, DEFAULT_CANVAS.width),
            height: clampedInt(canvas.height, 240, 10000, DEFAULT_CANVAS.height)
        },
        groups: Array.isArray(stored.groups) ? stored.groups.map(normalizeGroup).filter(Boolean) : [],
        connections: Array.isArray(stored.connections) ? stored.connections.map(normalizeConnection).filter(Boolean) : []
    });
}

function normalizeGroup(entry) {
    const shapeKind = entry.shapeKind === "free" ? "free" : SHAPE_PARAMS[entry.shapeKind] ? entry.shapeKind : "circle";
    const freePoints = Array.isArray(entry.freePoints) ? entry.freePoints.map(normalizeFreePoint).filter(Boolean) : [];
    const nodeCount = shapeKind === "free"
        ? clampedInt(freePoints.length || entry.nodeCount, 1, 24, 1)
        : clampedNodeCount(shapeKind, entry.nodeCount);
    const identityState = identityStateFrom(entry, "Custom");
    const identity = identityState.identity;
    const nodePrefix = toConstantName(entry.nodePrefix || identity, "PASSIVE_NODE");
    const shapeName = toConstantName(entry.shapeName || `${nodePrefix}_CLUSTER`, `${nodePrefix}_CLUSTER`);
    const normalized = group(
        entry.id || nextGroupId(),
        shapeKind,
        identity,
        nodePrefix,
        shapeName,
        nodeCount,
        NODE_KINDS.includes(entry.nodeKind) ? entry.nodeKind : "NODE",
        shapeKind === "free" ? {} : normalizeParams(shapeKind, entry.params ?? {}),
        entry.nodeNames ?? [],
        {
            starter: Boolean(entry.starter),
            freePoints,
            nodeDetails: Array.isArray(entry.nodeDetails) ? entry.nodeDetails.map(normalizeNodeDetail) : [],
            removedInternalLinks: entry.removedInternalLinks,
            ...identityState
        }
    );
    normalized.nodeNames = normalizedNodeNames(normalized);
    if (entry.source?.center && Array.isArray(entry.source.placements)) {
        normalized.source = JSON.parse(JSON.stringify(entry.source));
    }
    normalized.removedInternalLinks = normalizedInternalLinkKeys(
            normalized.removedInternalLinks,
            effectiveNodeCount(normalized.shapeKind, normalized.nodeCount)
    );
    return normalized;
}

function normalizeShapeTemplate(template) {
    if (!template?.group) {
        return null;
    }
    const normalized = normalizeGroup(template.group);
    return {
        id: String(template.id || `${Date.now()}_${normalized.shapeName.toLowerCase()}`),
        label: String(template.label || normalized.shapeName),
        group: normalized
    };
}

function group(id, shapeKind, identity, nodePrefix, shapeName, nodeCount, nodeKind, params, nodeNames, options = {}) {
    const identityState = normalizeIdentityState(
            identity,
            options.identityNegative,
            options.secondaryIdentity,
            options.secondaryIdentityNegative
    );
    return {
        id,
        shapeKind,
        identity: identityState.identity,
        identityNegative: identityState.identityNegative,
        secondaryIdentity: identityState.secondaryIdentity,
        secondaryIdentityNegative: identityState.secondaryIdentityNegative,
        nodePrefix,
        shapeName,
        nodeCount,
        nodeKind,
        params,
        nodeNames,
        starter: Boolean(options.starter),
        freePoints: options.freePoints ?? [],
        nodeDetails: options.nodeDetails ?? [],
        removedInternalLinks: normalizedInternalLinkKeys(options.removedInternalLinks)
    };
}

function normalizeParams(shapeKind, params) {
    return Object.fromEntries((SHAPE_PARAMS[shapeKind] ?? SHAPE_PARAMS.circle)
        .map(([key, , fallback]) => [key, Number.isFinite(Number(params[key])) ? Number(params[key]) : fallback]));
}

function defaultParams(shapeKind) {
    return Object.fromEntries((SHAPE_PARAMS[shapeKind] ?? SHAPE_PARAMS.circle)
        .map(([key, , fallback]) => [key, fallback]));
}

function clampedNodeCount(shapeKind, value) {
    if (shapeKind === "point") {
        return 1;
    }
    return clampedInt(value, 1, 24, 1);
}

function normalizeFreePoint(point) {
    if (!point) {
        return null;
    }
    return {
        x: round(point.x),
        y: round(point.y)
    };
}

function normalizeNodeDetail(detail) {
    if (!detail) {
        return {};
    }
    const identityState = identityStateFrom(detail, undefined);
    const normalized = {
        name: detail.name ? toConstantName(detail.name, "") : undefined,
        kind: NODE_KINDS.includes(detail.kind) ? detail.kind : undefined,
        size: Number.isFinite(detail.size) && detail.size > 0 ? detail.size : undefined,
        alwaysAllocated: detail.alwaysAllocated === true,
        grantsNothing: detail.grantsNothing === true
    };
    if (identityState.identity !== undefined) {
        normalized.identity = identityState.identity;
        normalized.identityNegative = identityState.identityNegative;
    }
    if (identityState.secondaryIdentity !== undefined) {
        normalized.secondaryIdentity = identityState.secondaryIdentity;
        normalized.secondaryIdentityNegative = identityState.secondaryIdentityNegative;
    }
    return normalized;
}

function normalizedInternalLinkKeys(links, nodeCount = null) {
    const normalized = new Set();
    if (!Array.isArray(links)) {
        return [];
    }
    for (const link of links) {
        const key = typeof link === "string"
            ? normalizeInternalLinkKey(link)
            : internalLinkKey(link?.fromIndex, link?.toIndex);
        if (!key) {
            continue;
        }
        const [first, second] = key.split(":").map(Number);
        if (nodeCount !== null && (first >= nodeCount || second >= nodeCount)) {
            continue;
        }
        normalized.add(key);
    }
    return [...normalized].sort(compareInternalLinkKeys);
}

function normalizeInternalLinkKey(key) {
    const match = String(key || "").match(/^(\d+):(\d+)$/);
    return match ? internalLinkKey(Number(match[1]), Number(match[2])) : "";
}

function internalLinkKey(fromIndex, toIndex) {
    const from = Number(fromIndex);
    const to = Number(toIndex);
    if (!Number.isInteger(from) || !Number.isInteger(to) || from < 0 || to < 0 || from === to) {
        return "";
    }
    return from < to ? `${from}:${to}` : `${to}:${from}`;
}

function compareInternalLinkKeys(first, second) {
    const [firstFrom, firstTo] = first.split(":").map(Number);
    const [secondFrom, secondTo] = second.split(":").map(Number);
    return firstFrom - secondFrom || firstTo - secondTo;
}

function normalizeNodeIdentityDetail(detail, groupEntry) {
    const state = normalizeIdentityState(
            detail.identity || groupEntry.identity,
            detail.identityNegative ?? groupEntry.identityNegative,
            Object.hasOwn(detail, "secondaryIdentity") ? detail.secondaryIdentity : groupEntry.secondaryIdentity,
            detail.secondaryIdentityNegative ?? groupEntry.secondaryIdentityNegative
    );
    detail.identity = state.identity;
    detail.identityNegative = state.identityNegative;
    detail.secondaryIdentity = state.secondaryIdentity;
    detail.secondaryIdentityNegative = state.secondaryIdentityNegative;
}

function identityStateFrom(entry, fallbackIdentity = "Custom") {
    const identities = Array.isArray(entry?.identities) ? entry.identities : [];
    const primary = identities.find((identity) => identity?.role === "primary");
    const secondary = identities.find((identity) => identity?.role === "secondary");
    const hasIdentity = Object.hasOwn(entry ?? {}, "identity") || primary;
    const hasSecondary = Object.hasOwn(entry ?? {}, "secondaryIdentity") || secondary;
    if (!hasIdentity && fallbackIdentity === undefined) {
        return {
            identity: undefined,
            identityNegative: undefined,
            secondaryIdentity: hasSecondary ? normalizeOptionalIdentityLabel(entry.secondaryIdentity ?? secondary?.label) : undefined,
            secondaryIdentityNegative: hasSecondary
                    ? Boolean(entry.secondaryIdentityNegative ?? secondary?.negative)
                    : undefined
        };
    }
    return normalizeIdentityState(
            entry?.identity ?? primary?.label ?? fallbackIdentity,
            entry?.identityNegative ?? primary?.negative,
            hasSecondary ? entry?.secondaryIdentity ?? secondary?.label : undefined,
            entry?.secondaryIdentityNegative ?? secondary?.negative,
            fallbackIdentity
    );
}

function normalizeIdentityState(identity, identityNegative, secondaryIdentity, secondaryIdentityNegative, fallbackIdentity = "Custom") {
    const primary = normalizeIdentityLabel(identity, fallbackIdentity);
    const primaryNegative = Boolean(identityNegative);
    let secondary = normalizeOptionalIdentityLabel(secondaryIdentity);
    let secondaryNegative = Boolean(secondaryIdentityNegative);
    if (!secondary || (secondary === primary && secondaryNegative === primaryNegative)) {
        secondary = "";
        secondaryNegative = false;
    }
    if (primary === "Starter") {
        return {
            identity: "Starter",
            identityNegative: false,
            secondaryIdentity: "",
            secondaryIdentityNegative: false
        };
    }
    return {
        identity: primary,
        identityNegative: primaryNegative,
        secondaryIdentity: secondary,
        secondaryIdentityNegative: secondaryNegative
    };
}

function normalizeIdentityLabel(identity, fallbackIdentity = "Custom") {
    const text = String(identity ?? "").trim();
    if (text) {
        return text;
    }
    return fallbackIdentity === undefined ? undefined : fallbackIdentity;
}

function normalizeOptionalIdentityLabel(identity) {
    return String(identity ?? "").trim();
}

function normalizeConnection(connection) {
    if (!connection?.from || !connection?.to) {
        return null;
    }
    return {
        id: String(connection.id || `connection_${Date.now()}`),
        from: normalizeNodeRef(connection.from),
        to: normalizeNodeRef(connection.to),
        kind: connection.kind || "travel"
    };
}

function normalizeNodeRef(ref) {
    return {
        groupId: String(ref.groupId || ""),
        nodeIndex: clampedInt(ref.nodeIndex, 0, 999, 0)
    };
}

function ensureStarterTree(tree) {
    if (tree.multipleStarts) {
        tree.connections = validConnections(tree, tree.connections ?? []);
        return tree;
    }
    if (!tree.groups.some((entry) => entry.starter)) {
        tree.groups.unshift(normalizeGroup(group(
            "starter",
            "point",
            "Starter",
            "STARTER",
            "STARTER_POINT",
            1,
            "STARTER",
            { x: Math.round(tree.canvas.width / 2), y: Math.round(tree.canvas.height / 2) },
            ["STARTER"],
            { starter: true }
        )));
    }
    const starter = tree.groups.find((entry) => entry.starter);
    starter.id = starter.id || "starter";
    starter.identity = "Starter";
    starter.identityNegative = false;
    starter.secondaryIdentity = "";
    starter.secondaryIdentityNegative = false;
    starter.nodeKind = "STARTER";
    starter.nodePrefix = "STARTER";
    starter.shapeName = "STARTER_POINT";
    starter.nodeNames = ["STARTER"];
    starter.nodeDetails = [{
        name: "STARTER",
        kind: "STARTER",
        size: starter.nodeDetails?.[0]?.size,
        identity: "Starter",
        identityNegative: false,
        secondaryIdentity: "",
        secondaryIdentityNegative: false,
        alwaysAllocated: true,
        grantsNothing: true
    }];
    tree.connections = validConnections(tree, tree.connections ?? []);
    return tree;
}

function validConnections(tree, connections) {
    const validKeys = new Set(tree.groups.flatMap((entry) =>
        computeGroupPoints(entry).map((_, index) => nodeKey(entry.id, index))));
    return connections.filter((connection) =>
        validKeys.has(nodeKey(connection.from.groupId, connection.from.nodeIndex))
        && validKeys.has(nodeKey(connection.to.groupId, connection.to.nodeIndex)));
}

function parseCanvasSize(value) {
    const match = String(value).toLowerCase().match(/(\d+)\s*x\s*(\d+)/);
    if (!match) {
        return { ...DEFAULT_CANVAS };
    }
    return {
        width: clampedInt(match[1], 320, 10000, DEFAULT_CANVAS.width),
        height: clampedInt(match[2], 240, 10000, DEFAULT_CANVAS.height)
    };
}

function totalNodeCount() {
    return passiveTreeState.tree.groups.reduce((sum, entry) => sum + effectiveNodeCount(entry.shapeKind, entry.nodeCount), 0);
}

function nextGroupId() {
    let index = passiveTreeState.tree?.groups?.length ?? 0;
    let id = `group_${index + 1}`;
    const existingIds = new Set(passiveTreeState.tree?.groups?.map((entry) => entry.id) ?? []);
    while (existingIds.has(id)) {
        index++;
        id = `group_${index + 1}`;
    }
    return id;
}

function uniqueShapeName(shapeName, currentGroupId = null) {
    const clean = toConstantName(shapeName, "PASSIVE_SHAPE");
    const existing = new Set(passiveTreeState.tree?.groups
        ?.filter((entry) => entry.id !== currentGroupId)
        .map((entry) => entry.shapeName) ?? []);
    if (!existing.has(clean)) {
        return clean;
    }
    let index = 2;
    while (existing.has(`${clean}_${index}`)) {
        index++;
    }
    return `${clean}_${index}`;
}

function uniqueNodeName(group, baseName) {
    const clean = toConstantName(baseName, "TRAVEL_NODE");
    const existing = new Set(passiveTreeState.tree.groups.flatMap((entry) => normalizedNodeNames(entry)));
    if (!existing.has(clean)) {
        return clean;
    }
    let index = 2;
    while (existing.has(`${clean}_${index}`)) {
        index++;
    }
    return `${clean}_${index}`;
}

function pointOnCircle(centerX, centerY, radius, degrees, accessor) {
    const radians = degrees * Math.PI / 180;
    return pointWithAccessor(
        centerX + Math.round(Math.cos(radians) * radius),
        centerY + Math.round(Math.sin(radians) * radius),
        accessor
    );
}

function pointWithAccessor(x, y, accessor) {
    return { x: round(x), y: round(y), accessor };
}

function interpolate(from, to, progress) {
    return Math.round(from + (to - from) * progress);
}

function javaShapeType(shapeKind) {
    if (shapeKind === "twoPathDeadEnd") {
        return "TwoPathCluster";
    }
    if (shapeKind === "branchedTravelPath") {
        return "BranchedPath";
    }
    return "Shape";
}

function javaMethod(shapeKind) {
    return shapeKind;
}

function javaPoint(x, y) {
    return `PassiveTreeLayouts.Point.of(${intArg(x)}, ${intArg(y)})`;
}

function intArg(value) {
    return String(round(value));
}

function doubleArg(value) {
    const number = Number(value);
    const text = Number.isInteger(number) ? `${number.toFixed(1)}` : String(number);
    return `${text}D`;
}

function identityColor(identity) {
    return IDENTITY_COLORS.get(identity) ?? IDENTITY_COLORS.get("Custom");
}

function signedIdentityColor(identity, negative = false) {
    const color = identityColor(identity);
    return negative ? mixHexColor(color, "#15181c", 0.45) : color;
}

function mixHexColor(hex, targetHex, targetWeight) {
    const source = parseHexColor(hex);
    const target = parseHexColor(targetHex);
    if (!source || !target) {
        return hex;
    }
    const sourceWeight = 1 - targetWeight;
    return `#${[0, 1, 2].map((index) => {
        const value = Math.round(source[index] * sourceWeight + target[index] * targetWeight);
        return value.toString(16).padStart(2, "0");
    }).join("")}`;
}

function parseHexColor(hex) {
    const match = String(hex || "").match(/^#([0-9a-f]{6})$/i);
    if (!match) {
        return null;
    }
    return [
        Number.parseInt(match[1].slice(0, 2), 16),
        Number.parseInt(match[1].slice(2, 4), 16),
        Number.parseInt(match[1].slice(4, 6), 16)
    ];
}

function identitySummary(state) {
    const primary = signedIdentityLabel(state.identity, state.identityNegative);
    if (!state.secondaryIdentity) {
        return primary;
    }
    return `${primary} / ${signedIdentityLabel(state.secondaryIdentity, state.secondaryIdentityNegative)}`;
}

function signedIdentityLabel(identity, negative = false) {
    return `${negative ? "-" : "+"}${identity || "Custom"}`;
}

function identityNegativeMarker(state) {
    if (state.identityNegative && state.secondaryIdentityNegative) {
        return "--";
    }
    return "-";
}

function nodeGradientId(groupId, index) {
    return `passive_tree_identity_${String(groupId).replace(/[^a-zA-Z0-9_-]/g, "_")}_${index}`;
}

function nodeKindClass(nodeKind) {
    return `passive-tree-${String(nodeKind || "NODE").toLowerCase()}`;
}

function passiveTreeSizeForKind(kind) {
    return passiveTreeState.tree.groups.flatMap((group) => group.nodeDetails ?? [])
        .find((detail) => detail.kind === kind && Number.isFinite(detail.size) && detail.size > 0)?.size;
}

function nodeRadius(nodeKind, size) {
    if (Number.isFinite(size) && size > 0) {
        return size / 2;
    }
    if (nodeKind === "KEYSTONE") {
        return 12;
    }
    if (nodeKind === "NOTABLE") {
        return 10;
    }
    if (nodeKind === "TRAVEL") {
        return 6;
    }
    return 8;
}

function shortNodeLabel(name) {
    const parts = String(name).split("_").filter(Boolean);
    if (parts.length <= 2) {
        return name;
    }
    return parts.slice(-2).join("_");
}

function uniqueDraftId(value, existingDrafts = passiveTreeState.drafts) {
    const base = normalizedIdentifier(value, "draft").replace(/[./-]+/g, "_");
    const existingIds = new Set(existingDrafts.map((draft) => draft.id));
    if (!existingIds.has(base)) {
        return base;
    }
    let index = 2;
    while (existingIds.has(`${base}_${index}`)) {
        index++;
    }
    return `${base}_${index}`;
}

function uniqueDraftName(value) {
    const base = normalizedDraftName(value, passiveTreeState.tree.treeId);
    const existingNames = new Set(passiveTreeState.drafts.map((draft) => draft.name));
    if (!existingNames.has(base)) {
        return base;
    }
    let index = 2;
    while (existingNames.has(`${base} ${index}`)) {
        index++;
    }
    return `${base} ${index}`;
}

function normalizedDraftName(value, treeId) {
    const clean = String(value || "").trim();
    return clean || `${machineLabel(treeId)} Draft`;
}

function machineLabel(treeId) {
    return titleCase(String(treeId || "machine").split(/[/:]/).pop());
}

function normalizedIdentifier(value, fallback) {
    const clean = String(value || "")
        .toLowerCase()
        .replace(/[^a-z0-9_./-]+/g, "_")
        .replace(/^_+|_+$/g, "");
    return clean || fallback;
}

function titleCase(value) {
    return String(value).toLowerCase().split(/[_\s-]+/)
        .map((part) => part ? part[0].toUpperCase() + part.slice(1) : part)
        .join(" ");
}

function normalizedClassName(value, fallback) {
    const clean = String(value || "").replace(/[^A-Za-z0-9_]/g, "");
    if (!clean || /^\d/.test(clean)) {
        return fallback;
    }
    return clean;
}

function toConstantName(value, fallback) {
    let clean = String(value || "")
        .trim()
        .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "");
    if (clean && /^\d/.test(clean)) {
        clean = `N_${clean}`;
    }
    return clean || fallback;
}

function clampedInt(value, min, max, fallback) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return fallback;
    }
    return Math.max(min, Math.min(max, Math.trunc(number)));
}

function round(value) {
    return Math.round(Number(value) || 0);
}

function readJsonStorage(key) {
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function writeJsonStorage(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch {
        setPassiveTreeStatus("Browser storage is unavailable.", true);
    }
}

async function copyPassiveTreeText(text, message) {
    try {
        await navigator.clipboard.writeText(text);
        setPassiveTreeStatus(message);
    } catch {
        setPassiveTreeStatus("Clipboard write failed.", true);
    }
}

function setPassiveTreeStatus(message, error = false) {
    ptEls.passiveTreeStatus.textContent = message;
    ptEls.passiveTreeStatus.classList.toggle("bad", error);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
