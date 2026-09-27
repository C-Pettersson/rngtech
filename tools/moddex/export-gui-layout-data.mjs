import { mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, "..", "..");
const SCREEN_ROOT = path.join(REPO_ROOT, "src", "main", "java", "com", "rngtech", "client", "screen");
const MENU_ROOT = path.join(REPO_ROOT, "src", "main", "java", "com", "rngtech", "content", "menu");
const GENERATED_ROOT = path.join(__dirname, "generated");
const OUTPUT_PATH = path.join(GENERATED_ROOT, "gui-layouts.json");
const SCHEMA = "rngtech.moddex.gui-source.v1";

const TAB_PRESETS = new Map([
    ["TAB_PROCESSING", "PROCESSING"],
    ["TAB_PROCESS", "PROCESSING"],
    ["TAB_GEAR", "GEAR"],
    ["TAB_CONFIGURATION", "STATS"],
    ["TAB_STATS", "STATS"],
    ["TAB_REFINEMENT", "REFINEMENT"]
]);

const DEFAULT_TAB_LABELS = new Map([
    ["PROCESSING", "rngtech.tab.processing.short"],
    ["GEAR", "rngtech.tab.gear"],
    ["STATS", "rngtech.tab.stats"],
    ["REFINEMENT", "rngtech.tab.refinement.short"]
]);

export async function buildGuiLayoutData() {
    const helperConstants = await loadQualifiedConstants();
    const files = (await readdir(SCREEN_ROOT))
        .filter((file) => file.endsWith("Screen.java") && !file.endsWith("Style.java"))
        .sort((left, right) => left.localeCompare(right));

    const layouts = [];
    for (const file of files) {
        const source = await readFile(path.join(SCREEN_ROOT, file), "utf8");
        const layout = extractScreenLayout(file, source, helperConstants);
        if (layout) {
            layouts.push(layout);
        }
    }

    const data = {
        schema: SCHEMA,
        generatedAt: new Date().toISOString(),
        sourceRoot: "src/main/java/com/rngtech/client/screen",
        coordinateSpace: "gui-relative-pixels",
        editorSchema: "rngtech.moddex.gui.v1",
        elementTypes: [
            "TAB",
            "ITEM_SLOT",
            "FLUID_SLOT",
            "FE_METER",
            "FLUID_METER",
            "FLUID_TANK",
            "COMPRESSOR_TANK",
            "STATUS_BAR",
            "STATUS_SQUARE",
            "LABEL",
            "CUSTOM_METER",
            "PLAYER_INVENTORY"
        ],
        slotTypes: [
            "BUCKET_INPUT",
            "BUCKET_OUTPUT",
            "ITEM",
            "FILTERED_ITEM",
            "MACHINE_SELF",
            "COMPONENT"
        ],
        layouts
    };

    await mkdir(GENERATED_ROOT, { recursive: true });
    await writeFile(OUTPUT_PATH, `${JSON.stringify(data, null, 2)}\n`, "utf8");
    return data;
}

function extractScreenLayout(file, source, helperConstants) {
    const screenClass = file.replace(/\.java$/, "");
    const menuClass = source.match(/extends\s+AbstractContainerScreen<([A-Za-z0-9_]+)>/)?.[1] ?? null;
    if (!menuClass) {
        return null;
    }

    const constants = collectIntConstants(source, helperConstants);
    const dimensions = {
        width: findAssignedInt(source, "imageWidth", constants, helperConstants) ?? 176,
        height: findAssignedInt(source, "imageHeight", constants, helperConstants) ?? 166
    };
    const inventoryLabel = {
        x: findAssignedInt(source, "inventoryLabelX", constants, helperConstants),
        y: findAssignedInt(source, "inventoryLabelY", constants, helperConstants)
    };
    const titleLabel = {
        x: findAssignedInt(source, "titleLabelX", constants, helperConstants) ?? 8,
        y: findAssignedInt(source, "titleLabelY", constants, helperConstants) ?? 6
    };

    const tabs = extractTabs(source, constants, helperConstants);
    const tabByConstant = new Map(tabs.map((tab) => [tab.sourceConstant, tab.id]));
    const elements = [
        ...extractSlotElements(source, screenClass, constants, helperConstants, tabByConstant),
        ...extractVisualElements(source, screenClass, constants, helperConstants, tabByConstant),
        ...extractLabelElements(source, screenClass, constants, helperConstants, tabByConstant),
        ...extractInventoryElements(source, screenClass, tabByConstant, inventoryLabel)
    ];
    applyTooltipHints(source, constants, helperConstants, elements, tabByConstant);

    return {
        id: kebabCase(screenClass.replace(/Screen$/, "")),
        screenClass,
        menuClass,
        sourceFile: `src/main/java/com/rngtech/client/screen/${file}`,
        dimensions,
        inventoryLabel,
        titleLabel,
        tabs,
        elements: elements
            .map((element, index) => ({ ...element, order: index }))
            .sort((left, right) => left.tabId.localeCompare(right.tabId) || left.y - right.y || left.x - right.x)
    };
}

async function loadQualifiedConstants() {
    const result = {};
    for (const file of ["RefinementMenuSupport.java"]) {
        const className = file.replace(/\.java$/, "");
        const source = await readFile(path.join(MENU_ROOT, file), "utf8");
        result[className] = collectIntConstants(source, result);
    }
    const files = (await readdir(SCREEN_ROOT)).filter((file) => file.endsWith(".java"));
    for (const file of files) {
        const className = file.replace(/\.java$/, "");
        const source = await readFile(path.join(SCREEN_ROOT, file), "utf8");
        result[className] = collectIntConstants(source, result);
    }
    return result;
}

function collectIntConstants(source, helperConstants) {
    const constants = {};
    const pending = [...source.matchAll(/\b(?:private\s+)?static\s+final\s+int\s+([A-Z0-9_]+)\s*=\s*([^;]+);/g)]
        .map((match) => [match[1], match[2].trim()]);

    let changed = true;
    while (changed && pending.length) {
        changed = false;
        for (let index = pending.length - 1; index >= 0; index--) {
            const [name, expression] = pending[index];
            const value = evalIntExpression(expression, constants, helperConstants);
            if (value !== null) {
                constants[name] = value;
                pending.splice(index, 1);
                changed = true;
            }
        }
    }
    return constants;
}

function findAssignedInt(source, name, constants, helperConstants) {
    const match = source.match(new RegExp(`\\b${name}\\s*=\\s*([^;]+);`));
    return match ? evalIntExpression(match[1], constants, helperConstants) : null;
}

function extractTabs(source, constants, helperConstants) {
    const tabCalls = [...source.matchAll(/renderTab\s*\(\s*guiGraphics\s*,\s*([^,]+),\s*Component\.translatable\("([^"]+)"\)\s*,\s*menu\.selectedTab\(\)\s*==\s*([A-Za-z0-9_]+)\.([A-Z0-9_]+)\s*\)/g)];
    const tabWidth = constants.TAB_WIDTH ?? 54;
    const tabSpacing = constants.TAB_SPACING ?? tabWidth + 2;
    const renderTabMethod = extractMethod(source, "renderTab") ?? "";
    const tabBaseX = evalIntExpression(renderTabMethod.match(/int\s+x\s*=\s*leftPos\s*\+\s*([^;]+?index\s*\*\s*[A-Z0-9_]+)\s*;/)?.[1]?.replace(/\+\s*index\s*\*\s*[A-Z0-9_]+/, "") ?? "8", constants, helperConstants) ?? 8;
    const tabY = evalIntExpression(renderTabMethod.match(/int\s+y\s*=\s*topPos\s*\+\s*([^;]+);/)?.[1] ?? "-20", constants, helperConstants) ?? -20;

    const tabs = tabCalls.map((match) => {
        const index = evalIntExpression(match[1], constants, helperConstants) ?? Number(match[1]) ?? 0;
        const sourceConstant = match[4];
        const preset = TAB_PRESETS.get(sourceConstant) ?? "CUSTOM";
        return {
            id: preset === "CUSTOM" ? kebabCase(sourceConstant.replace(/^TAB_/, "")) : preset.toLowerCase(),
            type: "TAB",
            kind: preset === "CUSTOM" ? "CUSTOM" : "PRESET",
            preset,
            sourceConstant,
            labelKey: match[2],
            x: tabBaseX + index * tabSpacing,
            y: tabY,
            width: tabWidth,
            height: 21,
            tooltip: { enabled: false, text: "" },
            description: preset === "CUSTOM"
                ? "Custom tab loaded from screen source."
                : `${preset} tab loaded from screen source.`
        };
    });

    if (tabs.length) {
        return uniqueTabs(tabs);
    }

    const simpleTabCalls = [...source.matchAll(/\btab\s*\(\s*guiGraphics\s*,\s*(\d+)\s*,\s*Component\.translatable\("([^"]+)"\)\s*\)/g)];
    if (simpleTabCalls.length) {
        return simpleTabCalls.map((match) => {
            const index = Number(match[1]);
            const preset = ["PROCESSING", "GEAR", "STATS", "REFINEMENT"][index] ?? "CUSTOM";
            return {
                id: preset === "CUSTOM" ? `custom-${index}` : preset.toLowerCase(),
                type: "TAB",
                kind: preset === "CUSTOM" ? "CUSTOM" : "PRESET",
                preset,
                sourceConstant: `TAB_${preset}`,
                labelKey: match[2],
                x: tabBaseX + index * tabSpacing,
                y: tabY,
                width: tabWidth,
                height: 21,
                tooltip: { enabled: false, text: "" },
                description: preset === "CUSTOM"
                    ? "Custom tab loaded from screen source."
                    : `${preset} tab loaded from screen source.`
            };
        });
    }

    if (!source.includes("menu.selectedTab()")) {
        return [{
            id: "main",
            type: "TAB",
            kind: "CUSTOM",
            preset: "CUSTOM",
            sourceConstant: "TAB_MAIN",
            labelKey: "rngtech.tab.main",
            x: 8,
            y: -20,
            width: 54,
            height: 21,
            tooltip: { enabled: false, text: "" },
            description: "Single-view screen loaded from source."
        }];
    }

    return ["PROCESSING", "GEAR", "STATS", "REFINEMENT"].map((preset, index) => ({
        id: preset.toLowerCase(),
        type: "TAB",
        kind: "PRESET",
        preset,
        sourceConstant: `TAB_${preset}`,
        labelKey: DEFAULT_TAB_LABELS.get(preset),
        x: 8 + index * 56,
        y: -20,
        width: 54,
        height: 21,
        tooltip: { enabled: false, text: "" },
        description: `${preset} tab placeholder.`
    }));
}

function uniqueTabs(tabs) {
    const seen = new Set();
    return tabs.filter((tab) => {
        const key = tab.id;
        if (seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
}

function extractSlotElements(source, screenClass, constants, helperConstants, tabByConstant) {
    const method = extractMethod(source, "renderSlotFrames");
    const elements = [];
    const selectedBlocks = method ? extractSelectedTabBlocks(method) : [];
    if (!selectedBlocks.length) {
        const renderBg = extractMethod(source, "renderBg") ?? "";
        return extractSlotElementsFromBody(
                source,
                renderBg,
                "renderBg",
                screenClass,
                fallbackTabId(tabByConstant),
                constants,
                helperConstants,
                elements,
                new Set()
        );
    }
    for (const block of selectedBlocks) {
        const tabId = tabByConstant.get(block.tabConstant) ?? tabIdForConstant(block.tabConstant);
        elements.push(...extractSlotElementsFromBody(
                source,
                block.body,
                "renderSlotFrames",
                screenClass,
                tabId,
                constants,
                helperConstants,
                elements,
                new Set()
        ));
    }
    return elements;
}

function extractSlotElementsFromBody(source, body, methodName, screenClass, tabId, constants, helperConstants, existingElements, seenMethods) {
    const seenKey = `${tabId}:${methodName}:${body.slice(0, 80)}`;
    if (seenMethods.has(seenKey)) {
        return [];
    }
    seenMethods.add(seenKey);

    const elements = [];
    for (const call of findCalls(body, "renderSlotFrame")) {
        const args = splitArgs(call);
        if (args[0]?.trim() !== "guiGraphics" || args.length < 3) {
            continue;
        }
        const x = evalIntExpression(args[1], constants, helperConstants);
        const y = evalIntExpression(args[2], constants, helperConstants);
        if (x === null || y === null || looksLikeLoopExpression(args[1]) || looksLikeLoopExpression(args[2])) {
            continue;
        }
        const role = inferSlotRole(args.join(" "), tabId, screenClass, x, y);
        elements.push({
            id: uniqueElementId([...existingElements, ...elements], `${kebabCase(screenClass)}-${tabId}-${role.kind.toLowerCase()}`),
            type: role.kind,
            tabId,
            x,
            y,
            width: 18,
            height: 18,
            slotType: role.slotType,
            slotRole: role.slotRole,
            filter: role.filter,
            label: role.label,
            tooltip: { enabled: false, text: "" },
            description: role.description,
            source: {
                method: methodName,
                expression: call.trim()
            }
        });
    }
    for (const invocation of calledRenderMethodInvocations(body)) {
        const method = extractMethodWithParams(source, invocation.name);
        if (!method) {
            continue;
        }
        elements.push(...extractSlotElementsFromBody(
                source,
                substituteMethodArguments(method.body, method.params, invocation.args),
                invocation.name,
                screenClass,
                tabId,
                constants,
                helperConstants,
                [...existingElements, ...elements],
                seenMethods
        ));
    }
    return elements;
}

function extractVisualElements(source, screenClass, constants, helperConstants, tabByConstant) {
    const elements = [];
    const renderBg = extractMethod(source, "renderBg") ?? "";
    const selectedBlocks = extractSelectedTabBlocks(renderBg);
    if (!selectedBlocks.length) {
        const tabId = fallbackTabId(tabByConstant);
        elements.push(...extractFillElements(
                renderBg,
                "renderBg",
                screenClass,
                tabId,
                constants,
                helperConstants,
                elements
        ));
        for (const invocation of calledRenderMethodInvocations(renderBg)) {
            elements.push(...extractVisualElementsFromInvocation(
                    source,
                    invocation,
                    screenClass,
                    tabId,
                    constants,
                    helperConstants,
                    elements,
                    new Set()
            ));
        }
        return elements;
    }
    for (const block of selectedBlocks) {
        const tabId = tabByConstant.get(block.tabConstant) ?? tabIdForConstant(block.tabConstant);
        for (const invocation of calledRenderMethodInvocations(block.body)) {
            elements.push(...extractVisualElementsFromInvocation(
                    source,
                    invocation,
                    screenClass,
                    tabId,
                    constants,
                    helperConstants,
                    elements,
                    new Set()
            ));
        }
    }
    return elements;
}

function extractVisualElementsFromMethod(source, methodName, screenClass, tabId, constants, helperConstants, existingElements, seenMethods) {
    const method = extractMethod(source, methodName);
    return extractVisualElementsFromBody(
            source,
            method,
            methodName,
            screenClass,
            tabId,
            constants,
            helperConstants,
            existingElements,
            seenMethods,
            `${tabId}:${methodName}`
    );
}

function extractVisualElementsFromInvocation(source, invocation, screenClass, tabId, constants, helperConstants, existingElements, seenMethods) {
    const method = extractMethodWithParams(source, invocation.name);
    if (!method) {
        return [];
    }
    return extractVisualElementsFromBody(
            source,
            substituteMethodArguments(method.body, method.params, invocation.args),
            invocation.name,
            screenClass,
            tabId,
            constants,
            helperConstants,
            existingElements,
            seenMethods,
            `${tabId}:${invocation.name}:${invocation.args.join("|")}`
    );
}

function extractVisualElementsFromBody(source, method, methodName, screenClass, tabId, constants, helperConstants, existingElements, seenMethods, seenKey) {
    if (seenMethods.has(seenKey)) {
        return [];
    }
    seenMethods.add(seenKey);

    if (!method) {
        return [];
    }

    const elements = [];
    const iconElements = extractIconBoxStatusBars(method, methodName, screenClass, tabId, constants, helperConstants, existingElements);
    elements.push(...iconElements);
    elements.push(...extractFillElements(method, methodName, screenClass, tabId, constants, helperConstants, [...existingElements, ...elements]));

    for (const nestedInvocation of calledRenderMethodInvocations(method)) {
        elements.push(...extractVisualElementsFromInvocation(
                source,
                nestedInvocation,
                screenClass,
                tabId,
                constants,
                helperConstants,
                [...existingElements, ...elements],
                seenMethods
        ));
    }
    return elements;
}

function extractIconBoxStatusBars(method, methodName, screenClass, tabId, constants, helperConstants, existingElements) {
    const iconCalls = findCalls(method, "renderIconBox")
        .map((call) => {
            const args = splitArgs(call);
            if (args[0]?.trim() !== "guiGraphics" || args.length < 3) {
                return null;
            }
            const x = evalIntExpression(args[1], constants, helperConstants);
            const y = evalIntExpression(args[2], constants, helperConstants);
            const size = constants.ICON_SIZE ?? 12;
            if (x === null || y === null) {
                return null;
            }
            return {
                x,
                y,
                size,
                expression: call.trim(),
                label: iconLabelFromExpression(args[1])
            };
        })
        .filter(Boolean);
    if (!iconCalls.length) {
        return [];
    }

    const groups = new Map();
    for (const call of iconCalls) {
        const group = groups.get(call.y) ?? [];
        group.push(call);
        groups.set(call.y, group);
    }
    const elements = [];
    for (const [y, calls] of groups) {
        const sorted = calls.toSorted((left, right) => left.x - right.x);
        const left = Math.min(...sorted.map((call) => call.x));
        const right = Math.max(...sorted.map((call) => call.x + call.size));
        const height = Math.max(...sorted.map((call) => call.size));
        const rect = { x: left, y, width: right - left, height };
        if (isDuplicateRect([...existingElements, ...elements], tabId, rect, "STATUS_BAR")) {
            continue;
        }
        elements.push({
            id: uniqueElementId([...existingElements, ...elements], `${kebabCase(screenClass)}-${tabId}-status_bar`),
            type: "STATUS_BAR",
            tabId,
            x: rect.x,
            y: rect.y,
            width: rect.width,
            height: rect.height,
            label: "Status",
            orientation: "HORIZONTAL",
            meterKind: "STATUS",
            tooltip: { enabled: false, text: "" },
            description: "Grouped status/icon squares loaded from renderIconBox calls.",
            statusSquares: sorted.map((call, index) => ({
                id: `${call.label.toLowerCase().replaceAll(" ", "_") || "status"}_${index + 1}`,
                x: call.x - rect.x,
                y: call.y - rect.y,
                size: call.size,
                label: call.label,
                tooltip: { enabled: false, text: "" },
                description: `${call.label} status square.`
            })),
            source: {
                method: methodName,
                expression: sorted.map((call) => call.expression).join("; ")
            }
        });
    }
    return elements;
}

function iconLabelFromExpression(expression) {
    const raw = expression.trim().replace(/_ICON_X$/, "").replace(/_X$/, "");
    return titleCase(raw || "status");
}

function extractFillElements(method, methodName, screenClass, tabId, constants, helperConstants, existingElements) {
    const elements = [];
    const coordinateOffsets = extractCoordinateOffsets(method, constants, helperConstants);
    const calls = findCalls(method, "guiGraphics.fill");
    for (const call of calls) {
        const args = splitArgs(call);
        if (args.length < 5) {
            continue;
        }
        const rect = rectFromFill(args, constants, helperConstants, coordinateOffsets);
        if (!rect || rect.width <= 0 || rect.height <= 0) {
            continue;
        }
        if (rect.width <= 3 || rect.height <= 3) {
            continue;
        }
        if (rect.width > 120 && rect.height > 80) {
            continue;
        }
        if (rect.width === 18 && rect.height === 18) {
            continue;
        }
        const sourceText = args.join(" ");
        const role = inferVisualRole(`${sourceText} ${coordinateContextFromMethod(method)}`, rect, methodName, screenClass);
        if (!role) {
            continue;
        }
        if (isDuplicateRect([...existingElements, ...elements], tabId, rect, role.type)) {
            continue;
        }
        const element = {
            id: uniqueElementId([...existingElements, ...elements], `${kebabCase(screenClass)}-${tabId}-${role.type.toLowerCase()}`),
            type: role.type,
            tabId,
            x: rect.x,
            y: rect.y,
            width: rect.width,
            height: rect.height,
            label: role.label,
            orientation: role.orientation,
            meterKind: role.meterKind,
            tankRole: role.tankRole,
            tooltip: { enabled: false, text: "" },
            description: role.description,
            source: {
                method: methodName,
                expression: call.trim()
            }
        };
        if (role.type === "STATUS_BAR") {
            element.statusSquares = role.statusSquares ?? [];
        }
        elements.push(element);
    }
    return elements;
}

function extractLabelElements(source, screenClass, constants, helperConstants, tabByConstant) {
    const elements = [];
    const renderLabels = extractMethod(source, "renderLabels") ?? "";
    const selectedBlocks = extractSelectedTabBlocks(renderLabels);
    if (!selectedBlocks.length) {
        const tabId = fallbackTabId(tabByConstant);
        const bodies = [renderLabels];
        for (const methodName of calledDrawMethods(renderLabels)) {
            const method = extractMethod(source, methodName);
            if (method) {
                bodies.push(method);
            }
        }
        for (const body of bodies) {
            for (const label of extractLabelsFromBody(body, screenClass, tabId, constants, helperConstants, elements)) {
                elements.push(label);
            }
        }
        return elements;
    }
    for (const block of selectedBlocks) {
        const tabId = tabByConstant.get(block.tabConstant) ?? tabIdForConstant(block.tabConstant);
        const bodies = [block.body];
        for (const methodName of calledDrawMethods(block.body)) {
            const method = extractMethod(source, methodName);
            if (method) {
                bodies.push(method);
            }
        }
        for (const body of bodies) {
            for (const label of extractLabelsFromBody(body, screenClass, tabId, constants, helperConstants, elements)) {
                elements.push(label);
            }
        }
    }
    return elements;
}

function extractInventoryElements(source, screenClass, tabByConstant, inventoryLabel) {
    const bodies = [
        { method: "renderBg", body: extractMethod(source, "renderBg") ?? "" },
        { method: "renderSlotFrames", body: extractMethod(source, "renderSlotFrames") ?? "" }
    ].filter((entry) => entry.body.includes("renderPlayerInventoryFrames"));
    if (!bodies.length) {
        return [];
    }

    const tabIds = new Set();
    for (const { body } of bodies) {
        for (const block of extractSelectedTabBlocks(body)) {
            if (block.body.includes("renderPlayerInventoryFrames")) {
                tabIds.add(tabByConstant.get(block.tabConstant) ?? tabIdForConstant(block.tabConstant));
            }
        }

        for (const excludedConstant of selectedTabExclusionsBeforeInventory(body)) {
            for (const [tabConstant, tabId] of tabByConstant.entries()) {
                if (tabConstant !== excludedConstant) {
                    tabIds.add(tabId);
                }
            }
        }

        if (!body.includes("menu.selectedTab()")) {
            tabIds.add(fallbackTabId(tabByConstant));
        }
    }

    if (!tabIds.size) {
        for (const tabId of tabByConstant.values()) {
            tabIds.add(tabId);
        }
    }

    const elements = [];
    for (const tabId of tabIds) {
        const labelX = inventoryLabel.x ?? 8;
        const labelY = inventoryLabel.y ?? 84;
        const slotX = Math.max(0, labelX - 1);
        const slotY = labelY + 11;
        const x = Math.min(labelX, slotX);
        elements.push({
            id: uniqueElementId(elements, `${kebabCase(screenClass)}-${tabId}-player_inventory`),
            type: "PLAYER_INVENTORY",
            tabId,
            x,
            y: labelY,
            width: 162,
            height: 87,
            label: "Inventory",
            labelKey: "container.inventory",
            columns: 9,
            rows: 3,
            hotbar: true,
            slotSize: 18,
            slotOffsetX: slotX - x,
            slotOffsetY: slotY - labelY,
            tooltip: { enabled: false, text: "" },
            description: "Player inventory and hotbar visible for this GUI state.",
            source: {
                method: "renderPlayerInventoryFrames",
                expression: "renderPlayerInventoryFrames(guiGraphics)"
            }
        });
    }
    return elements;
}

function selectedTabExclusionsBeforeInventory(body) {
    const excluded = new Set();
    const notEqualPattern = /menu\.selectedTab\(\)\s*!=\s*[A-Za-z0-9_]+\.([A-Z0-9_]+)[\s\S]{0,180}?renderPlayerInventoryFrames/g;
    for (const match of body.matchAll(notEqualPattern)) {
        excluded.add(match[1]);
    }

    const returnBeforeInventoryPattern = /menu\.selectedTab\(\)\s*==\s*[A-Za-z0-9_]+\.([A-Z0-9_]+)\s*\)\s*\{[\s\S]{0,80}\breturn\s*;[\s\S]{0,180}?renderPlayerInventoryFrames/g;
    for (const match of body.matchAll(returnBeforeInventoryPattern)) {
        excluded.add(match[1]);
    }
    return excluded;
}

function extractLabelsFromBody(body, screenClass, tabId, constants, helperConstants, existingElements) {
    const labels = [];
    const drawStringPattern = /guiGraphics\.drawString\s*\(\s*font\s*,\s*Component\.translatable\("([^"]+)"\)\s*,\s*([^,]+),\s*([^,]+),/g;
    for (const match of body.matchAll(drawStringPattern)) {
        const x = evalIntExpression(match[2], constants, helperConstants);
        const y = evalIntExpression(match[3], constants, helperConstants);
        if (x === null || y === null) {
            continue;
        }
        labels.push(labelElement(screenClass, tabId, match[1], x, y, "drawString", existingElements, labels));
    }

    const centeredPattern = /drawCentered\s*\(\s*guiGraphics\s*,\s*Component\.translatable\("([^"]+)"\)\s*,\s*([^,]+),\s*([^,]+)\s*\)/g;
    for (const match of body.matchAll(centeredPattern)) {
        const centerX = evalIntExpression(match[2], constants, helperConstants);
        const y = evalIntExpression(match[3], constants, helperConstants);
        if (centerX === null || y === null) {
            continue;
        }
        labels.push({
            ...labelElement(screenClass, tabId, match[1], centerX, y, "drawCentered", existingElements, labels),
            align: "CENTER"
        });
    }
    return labels;
}

function labelElement(screenClass, tabId, labelKey, x, y, method, existingElements, labels) {
    return {
        id: uniqueElementId([...existingElements, ...labels], `${kebabCase(screenClass)}-${tabId}-label`),
        type: "LABEL",
        tabId,
        x,
        y,
        width: Math.max(40, labelKey.split(".").at(-1).length * 6),
        height: 10,
        label: labelKey.split(".").at(-1).replaceAll("_", " "),
        labelKey,
        align: "LEFT",
        tooltip: { enabled: false, text: "" },
        description: "Text label loaded from screen source.",
        source: { method }
    };
}

function applyTooltipHints(source, constants, helperConstants, elements, tabByConstant) {
    const method = extractMethod(source, "renderValueTooltips") ?? "";
    if (!method) {
        return;
    }
    const tabConstant = method.match(/menu\.selectedTab\(\)\s*!=\s*[A-Za-z0-9_]+\.([A-Z0-9_]+)/)?.[1];
    const tabId = tabConstant ? tabByConstant.get(tabConstant) ?? tabIdForConstant(tabConstant) : fallbackTabId(tabByConstant);
    for (const call of findCalls(method, "CompactValueText.renderTooltipIfHovered")) {
        const args = splitArgs(call);
        if (args.length < 10) {
            continue;
        }
        const rect = {
            x: evalIntExpression(args[6], constants, helperConstants),
            y: evalIntExpression(args[7], constants, helperConstants),
            width: evalIntExpression(args[8], constants, helperConstants),
            height: evalIntExpression(args[9], constants, helperConstants)
        };
        if ([rect.x, rect.y, rect.width, rect.height].some((value) => value === null)) {
            continue;
        }
        const targetMatch = nearestElement(elements, tabId, rect);
        if (!targetMatch) {
            continue;
        }
        const translatable = call.match(/Component\.translatable\s*\(\s*"([^"]+)"/)?.[1];
        const tooltip = {
            enabled: true,
            text: translatable ?? "Dynamic tooltip from screen source."
        };
        if (targetMatch.square) {
            targetMatch.square.tooltip = tooltip;
            targetMatch.square.description = translatable ?? targetMatch.square.description;
            continue;
        }
        targetMatch.element.tooltip = tooltip;
        refineElementFromTooltip(targetMatch.element, call, translatable);
    }
}

function refineElementFromTooltip(element, call, translatable) {
    const hint = `${translatable ?? ""} ${call}`.toLowerCase();
    if (hint.includes("fuel") || hint.includes("burn")) {
        element.type = "STATUS_BAR";
        element.meterKind = "FUEL";
        element.label = element.label === "Meter" ? "Fuel" : element.label;
        element.statusSquares ??= [];
        return;
    }
    if (hint.includes("progress") || hint.includes("strain") || hint.includes("wear") || hint.includes("status")) {
        element.type = "STATUS_BAR";
        element.meterKind = hint.includes("strain") ? "STRAIN" : hint.includes("wear") ? "WEAR" : hint.includes("status") ? "STATUS" : "PROGRESS";
        element.label = element.label === "Meter" ? titleCase(element.meterKind) : element.label;
        element.statusSquares ??= [];
        return;
    }
    if (hint.includes("fluid")) {
        if (!["FLUID_TANK", "COMPRESSOR_TANK"].includes(element.type)) {
            element.type = "FLUID_METER";
        }
        element.meterKind = "FLUID";
        element.label = element.label === "Meter" ? "Fluid" : element.label;
        return;
    }
    if (hint.includes("energy")) {
        element.type = "FE_METER";
        element.meterKind = "ENERGY";
        element.label = element.label === "Meter" ? "FE" : element.label;
        return;
    }
}

function nearestElement(elements, tabId, rect) {
    let best = null;
    let bestScore = Number.POSITIVE_INFINITY;
    for (const element of elements) {
        if (element.tabId !== tabId || element.type === "LABEL") {
            continue;
        }
        for (const square of element.statusSquares ?? []) {
            const squareRect = {
                x: element.x + (square.x ?? 0),
                y: element.y + (square.y ?? 0),
                width: square.size ?? 0,
                height: square.size ?? 0
            };
            const score = rectScore(squareRect, rect);
            if (score < bestScore && score <= 8) {
                best = { element, square };
                bestScore = score;
            }
        }
        const dx = Math.abs(element.x - rect.x);
        const dy = Math.abs(element.y - rect.y);
        const dw = Math.abs(element.width - rect.width);
        const dh = Math.abs(element.height - rect.height);
        const score = dx + dy + dw + dh;
        if (score < bestScore && score <= 14) {
            best = { element, square: null };
            bestScore = score;
        }
    }
    return best;
}

function rectScore(left, right) {
    return Math.abs(left.x - right.x)
        + Math.abs(left.y - right.y)
        + Math.abs(left.width - right.width)
        + Math.abs(left.height - right.height);
}

function inferSlotRole(sourceText, tabId, screenClass, x, y) {
    const upper = sourceText.toUpperCase();
    if (upper.includes("PROCESSING_TARGET_SLOT") || (upper.includes("TARGET_SLOT_X") && !upper.includes("CONSUMABLE"))) {
        return {
            kind: "ITEM_SLOT",
            slotType: "MACHINE_SELF",
            slotRole: "MACHINE_SELF",
            filter: "SELF_MACHINE_DISPLAY",
            label: "Machine",
            description: tabId === "refinement"
                ? "Machine self display slot used as the refinement target."
                : "Machine self display slot used for processing-tab stat and trait context."
        };
    }
    if (upper.includes("CONSUMABLE_SLOT")) {
        return {
            kind: "ITEM_SLOT",
            slotType: "ITEM",
            slotRole: "REFINEMENT_CONSUMABLE",
            label: "Catalyst",
            description: "Refinement catalyst or consumable slot."
        };
    }
    if (upper.includes("FLUID") && upper.includes("OUTPUT")) {
        return {
            kind: "FLUID_SLOT",
            slotType: "BUCKET_OUTPUT",
            slotRole: "FLUID_CONTAINER_OUTPUT",
            label: "Fluid Output",
            description: "Bucket or fluid-container output slot."
        };
    }
    if ((upper.includes("WATER") || upper.includes("CARBON")) && upper.includes("OUTPUT")) {
        return {
            kind: "FLUID_SLOT",
            slotType: "BUCKET_OUTPUT",
            slotRole: upper.includes("WATER") ? "WATER_CONTAINER_OUTPUT" : "CARBON_CONTAINER_OUTPUT",
            label: upper.includes("WATER") ? "Water Output" : "Carbon Output",
            description: "Fluid-container output slot loaded from water/carbon screen coordinates."
        };
    }
    if (upper.includes("FLUID") || upper.includes("BUCKET")) {
        return {
            kind: "FLUID_SLOT",
            slotType: "BUCKET_INPUT",
            slotRole: "FLUID_CONTAINER_INPUT",
            label: "Fluid Input",
            description: "Bucket or fluid-container input slot."
        };
    }
    if (upper.includes("WATER") || upper.includes("CARBON")) {
        return {
            kind: "FLUID_SLOT",
            slotType: "BUCKET_INPUT",
            slotRole: upper.includes("WATER") ? "WATER_CONTAINER_INPUT" : "CARBON_CONTAINER_INPUT",
            label: upper.includes("WATER") ? "Water Input" : "Carbon Input",
            description: "Fluid-container input slot loaded from water/carbon screen coordinates."
        };
    }
    if (screenClass.toUpperCase().includes("TANK") && y >= 28 && y <= 56 && x >= 48 && x <= 112) {
        const isOutput = x >= 80;
        return {
            kind: "FLUID_SLOT",
            slotType: isOutput ? "BUCKET_OUTPUT" : "BUCKET_INPUT",
            slotRole: isOutput ? "FLUID_CONTAINER_OUTPUT" : "FLUID_CONTAINER_INPUT",
            label: isOutput ? "Fluid Output" : "Fluid Input",
            description: isOutput ? "Bucket or fluid-container output slot." : "Bucket or fluid-container input slot."
        };
    }
    if (tabId === "gear" || upper.includes("COMPONENT") || upper.includes("GEAR")) {
        return {
            kind: "ITEM_SLOT",
            slotType: "COMPONENT",
            slotRole: "COMPONENT",
            label: "Component",
            description: "Installed machine component slot."
        };
    }
    if (upper.includes("FILTER")) {
        return {
            kind: "ITEM_SLOT",
            slotType: "FILTERED_ITEM",
            slotRole: "FILTERED_ITEM",
            label: "Filtered Item",
            description: "Filtered item slot; describe the accepted item in the element description."
        };
    }
    return {
        kind: "ITEM_SLOT",
        slotType: "ITEM",
        slotRole: "ITEM",
        label: "Item",
        description: "General item slot."
    };
}

function inferVisualRole(sourceText, rect, methodName, screenClass) {
    const upper = sourceText.toUpperCase();
    const methodUpper = methodName.toUpperCase();
    const classUpper = screenClass.toUpperCase();
    const context = `${upper} ${methodUpper} ${classUpper}`;
    const tankSized = rect.width >= 10 && rect.height >= 16;
    if (upper.includes("ENERGY")) {
        return {
            type: "FE_METER",
            label: "FE",
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "ENERGY",
            description: "Forge Energy meter loaded from screen source."
        };
    }
    if (tankSized && context.includes("PLAIN_TANK")) {
        return {
            type: "FLUID_TANK",
            label: tankLabel(context, "Tank"),
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "FLUID",
            tankRole: tankRole(context),
            description: "Fluid tank loaded from screen source."
        };
    }
    if (tankSized && isCompressorTankContext(context)) {
        return {
            type: "COMPRESSOR_TANK",
            label: tankLabel(context, "Compressor"),
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "FLUID",
            tankRole: tankRole(context),
            description: "Compressor tank fluid column loaded from screen source."
        };
    }
    if (tankSized && isFluidTankContext(context)) {
        return {
            type: "FLUID_TANK",
            label: tankLabel(context, "Tank"),
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "FLUID",
            tankRole: tankRole(context),
            description: "Fluid tank loaded from screen source."
        };
    }
    if (rect.width <= 14 && rect.height <= 14 && (context.includes("STATUS") || context.includes("LIGHT") || context.includes("ICON"))) {
        return {
            type: "STATUS_BAR",
            label: "Status",
            orientation: "HORIZONTAL",
            meterKind: "STATUS",
            statusSquares: [{ id: "status_1", x: 0, y: 0, size: Math.max(rect.width, rect.height), description: "Dynamic status square." }],
            description: "Status square loaded from screen source."
        };
    }
    if (context.includes("STATUS") && Math.abs(rect.width - rect.height) <= 4) {
        return {
            type: "STATUS_BAR",
            label: "Status",
            orientation: "HORIZONTAL",
            meterKind: "STATUS",
            statusSquares: [{ id: "status_1", x: 0, y: 0, size: Math.max(rect.width, rect.height), description: "Dynamic status square." }],
            description: "Status square loaded from screen source."
        };
    }
    if (upper.includes("FLUID") || upper.includes("WATER") || upper.includes("CARBON")) {
        return {
            type: "FLUID_METER",
            label: "Fluid",
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "FLUID",
            description: "Fluid meter loaded from screen source."
        };
    }
    if (upper.includes("PROGRESS") || upper.includes("STRAIN") || upper.includes("WEAR")) {
        return {
            type: "STATUS_BAR",
            label: titleCase(upper.includes("STRAIN") ? "strain" : upper.includes("WEAR") ? "wear" : "progress"),
            orientation: rect.width >= rect.height ? "HORIZONTAL" : "VERTICAL",
            meterKind: upper.includes("STRAIN") ? "STRAIN" : upper.includes("WEAR") ? "WEAR" : "PROGRESS",
            statusSquares: [],
            description: "Status/progress bar loaded from screen source."
        };
    }
    if (upper.includes("FUEL") || upper.includes("BURN") || upper.includes("_BAR")) {
        const meterKind = upper.includes("FUEL") ? "FUEL" : "BAR";
        return {
            type: "STATUS_BAR",
            label: titleCase(meterKind),
            orientation: rect.width >= rect.height ? "HORIZONTAL" : "VERTICAL",
            meterKind,
            statusSquares: [],
            description: "Status bar loaded from screen source."
        };
    }
    if (upper.includes("HEAT") || upper.includes("PRESSURE")) {
        return {
            type: "CUSTOM_METER",
            label: titleCase(upper.includes("PRESSURE") ? "pressure" : "heat"),
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: upper.includes("PRESSURE") ? "PRESSURE" : "HEAT",
            description: "Custom meter loaded from screen source."
        };
    }
    if ((rect.width <= 20 && rect.height >= 30) || (rect.height <= 10 && rect.width >= 24)) {
        return {
            type: "CUSTOM_METER",
            label: "Meter",
            orientation: rect.height >= rect.width ? "VERTICAL" : "HORIZONTAL",
            meterKind: "CUSTOM",
            description: "Generic meter-like fill loaded from screen source."
        };
    }
    return null;
}

function isCompressorTankContext(context) {
    return context.includes("COMPRESSED_FLUID")
        || context.includes("LOOSE_FLUID")
        || context.includes("EQUIVALENT_FLUID")
        || (context.includes("COMPRESSOR_TANK") && context.includes("FLUID_BAR"));
}

function isFluidTankContext(context) {
    return context.includes("PLAIN_TANK")
        || context.includes("FLUID_TANK")
        || context.includes("FLUID_GAUGE")
        || context.includes("WATER_GAUGE")
        || context.includes("CARBON_GAUGE")
        || (context.includes("DEBUGTANK") && context.includes("RENDERSTATUS"));
}

function tankRole(context) {
    if (context.includes("LOOSE")) {
        return "LOOSE_FLUID";
    }
    if (context.includes("COMPRESSED")) {
        return "COMPRESSED_FLUID";
    }
    if (context.includes("EQUIVALENT")) {
        return "EQUIVALENT_FLUID";
    }
    if (context.includes("PLAIN")) {
        return "PLAIN";
    }
    if (context.includes("WATER")) {
        return "WATER";
    }
    if (context.includes("CARBON")) {
        return "CARBON";
    }
    return "FLUID";
}

function tankLabel(context, fallback) {
    return titleCase(tankRole(context).toLowerCase()) || fallback;
}

function rectFromFill(args, constants, helperConstants, coordinateOffsets) {
    const x1 = evalCoordinate(args[0], constants, helperConstants, coordinateOffsets);
    const y1 = evalCoordinate(args[1], constants, helperConstants, coordinateOffsets);
    const x2 = evalCoordinate(args[2], constants, helperConstants, coordinateOffsets);
    const y2 = evalCoordinate(args[3], constants, helperConstants, coordinateOffsets);
    if ([x1, y1, x2, y2].some((value) => value === null)) {
        return null;
    }
    return {
        x: Math.min(x1, x2),
        y: Math.min(y1, y2),
        width: Math.abs(x2 - x1),
        height: Math.abs(y2 - y1)
    };
}

function extractCoordinateOffsets(method, constants, helperConstants) {
    const offsets = {};
    const pattern = /\bint\s+([a-z][A-Za-z0-9_]*)\s*=\s*(leftPos|topPos)(?:\s*([+-])\s*([^;]+))?\s*;/g;
    for (const match of method.matchAll(pattern)) {
        const offset = match[4] ? evalIntExpression(match[4], constants, helperConstants) : 0;
        if (offset === null) {
            continue;
        }
        offsets[match[1]] = match[3] === "-" ? -offset : offset;
    }
    return offsets;
}

function coordinateContextFromMethod(method) {
    return [...method.matchAll(/\bint\s+[a-z][A-Za-z0-9_]*\s*=\s*(?:leftPos|topPos)(?:\s*[+-]\s*[^;]+)?\s*;/g)]
        .map((match) => match[0])
        .join(" ");
}

function evalCoordinate(expression, constants, helperConstants, coordinateOffsets = {}) {
    const normalized = expression
        .replace(/\bleftPos\b|\btopPos\b/g, "0")
        .replace(/\b([a-z][A-Za-z0-9_]*)\b/g, (match) => {
            const value = coordinateOffsets[match];
            return value === undefined ? match : String(value);
        })
        .replace(/\bfont\.width\([^)]*\)/g, "0");
    if (/\bmenu\b|\bMath\b|\bmouse\b|\brow\b|\bcolumn\b|\bpip\b|\bcolor\b|\bselected\b|\bstatusColor\b/.test(normalized)) {
        return null;
    }
    return evalIntExpression(normalized, constants, helperConstants);
}

function evalIntExpression(expression, constants, helperConstants) {
    if (!expression) {
        return null;
    }
    let normalized = expression
        .replace(/(?<=\d)_(?=\d)/g, "")
        .replace(/\(int\)/g, "")
        .replace(/\b([A-Za-z_][A-Za-z0-9_]*)\.([A-Z0-9_]+)\b/g, (match, className, constantName) => {
            const value = helperConstants?.[className]?.[constantName];
            return value === undefined ? match : String(value);
        })
        .replace(/\b([A-Z][A-Z0-9_]*)\b/g, (match) => {
            const value = constants[match];
            return value === undefined ? match : String(value);
        });
    if (/[A-Za-z]/.test(normalized) || !/^[0-9xXa-fA-F+\-*/%().\s]+$/.test(normalized)) {
        return null;
    }
    try {
        const value = Function(`"use strict"; return (${normalized});`)();
        return Number.isFinite(value) ? Math.trunc(value) : null;
    } catch {
        return null;
    }
}

function extractSelectedTabBlocks(methodBody) {
    const blocks = [];
    const pattern = /(?:if|else\s+if)\s*\(\s*menu\.selectedTab\(\)\s*==\s*[A-Za-z0-9_]+\.([A-Z0-9_]+)\s*\)\s*\{/g;
    let match;
    while ((match = pattern.exec(methodBody)) !== null) {
        const openBrace = pattern.lastIndex - 1;
        const closeBrace = findMatchingBrace(methodBody, openBrace);
        if (closeBrace === -1) {
            continue;
        }
        blocks.push({
            tabConstant: match[1],
            body: methodBody.slice(openBrace + 1, closeBrace)
        });
        pattern.lastIndex = closeBrace + 1;
    }
    return blocks;
}

function extractMethod(source, methodName) {
    return extractMethodWithParams(source, methodName)?.body ?? null;
}

function extractMethodWithParams(source, methodName) {
    const declarationPattern = new RegExp(`\\b(?:private|protected|public)\\s+(?:static\\s+)?[A-Za-z0-9_<>, ?]+\\s+${methodName}\\s*\\(`, "g");
    const match = declarationPattern.exec(source);
    if (!match) {
        return null;
    }
    const openParen = source.indexOf("(", match.index);
    if (openParen === -1) {
        return null;
    }
    const closeParen = findMatchingParen(source, openParen);
    if (closeParen === -1) {
        return null;
    }
    const params = splitArgs(source.slice(openParen + 1, closeParen))
        .map(parameterName)
        .filter(Boolean);
    const openBrace = source.indexOf("{", closeParen);
    if (openBrace === -1) {
        return null;
    }
    const closeBrace = findMatchingBrace(source, openBrace);
    return closeBrace === -1 ? null : {
        body: source.slice(openBrace + 1, closeBrace),
        params
    };
}

function parameterName(parameter) {
    const clean = parameter
        .replace(/@\w+(?:\([^)]*\))?/g, "")
        .replace(/\bfinal\b/g, "")
        .trim();
    if (!clean) {
        return null;
    }
    return clean.split(/\s+/).at(-1)?.replace(/\[\]$/, "") ?? null;
}

function substituteMethodArguments(body, params, args) {
    let substituted = body;
    for (let index = 0; index < params.length; index++) {
        const param = params[index];
        const arg = args[index]?.trim();
        if (!param || !arg || param === "guiGraphics") {
            continue;
        }
        substituted = substituted.replace(new RegExp(`(?<!\\.)\\b${escapeRegExp(param)}\\b`, "g"), `(${arg})`);
    }
    return substituted;
}

function findMatchingBrace(source, openBrace) {
    let depth = 0;
    for (let index = openBrace; index < source.length; index++) {
        const char = source[index];
        if (char === "{") {
            depth++;
        } else if (char === "}") {
            depth--;
            if (depth === 0) {
                return index;
            }
        }
    }
    return -1;
}

function findCalls(source, callName) {
    const calls = [];
    let index = 0;
    while (index < source.length) {
        const callIndex = source.indexOf(callName, index);
        if (callIndex === -1) {
            break;
        }
        const before = source[callIndex - 1] ?? "";
        const after = source[callIndex + callName.length] ?? "";
        if (/[A-Za-z0-9_$]/.test(before) || /[A-Za-z0-9_$]/.test(after)) {
            index = callIndex + callName.length;
            continue;
        }
        const openParen = source.indexOf("(", callIndex + callName.length);
        if (openParen === -1) {
            break;
        }
        const closeParen = findMatchingParen(source, openParen);
        if (closeParen === -1) {
            break;
        }
        calls.push(source.slice(openParen + 1, closeParen));
        index = closeParen + 1;
    }
    return calls;
}

function findMatchingParen(source, openParen) {
    let depth = 0;
    let inString = false;
    for (let index = openParen; index < source.length; index++) {
        const char = source[index];
        const previous = source[index - 1];
        if (char === "\"" && previous !== "\\") {
            inString = !inString;
        }
        if (inString) {
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
    return -1;
}

function splitArgs(source) {
    const args = [];
    let start = 0;
    let depth = 0;
    let inString = false;
    for (let index = 0; index < source.length; index++) {
        const char = source[index];
        const previous = source[index - 1];
        if (char === "\"" && previous !== "\\") {
            inString = !inString;
        }
        if (inString) {
            continue;
        }
        if (char === "(") {
            depth++;
        } else if (char === ")") {
            depth--;
        } else if (char === "," && depth === 0) {
            args.push(source.slice(start, index).trim());
            start = index + 1;
        }
    }
    args.push(source.slice(start).trim());
    return args;
}

function calledRenderMethodInvocations(source) {
    const invocations = [];
    const pattern = /\b(render[A-Z][A-Za-z0-9_]*)\s*\(/g;
    let match;
    while ((match = pattern.exec(source)) !== null) {
        const name = match[1];
        if (ignoredRenderMethodNames().has(name)) {
            continue;
        }
        const openParen = pattern.lastIndex - 1;
        const closeParen = findMatchingParen(source, openParen);
        if (closeParen === -1) {
            continue;
        }
        invocations.push({
            name,
            args: splitArgs(source.slice(openParen + 1, closeParen))
        });
        pattern.lastIndex = closeParen + 1;
    }
    return invocations;
}

function ignoredRenderMethodNames() {
    return new Set([
            "renderIconBox",
            "renderTab",
            "renderTabs",
            "renderButton",
            "renderButtons",
            "renderSlotFrame",
            "renderSlotFrames",
            "renderPlayerInventoryFrames"
    ]);
}

function calledDrawMethods(source) {
    return [...new Set([...source.matchAll(/\b(draw[A-Z][A-Za-z0-9_]*)\s*\(/g)]
        .map((match) => match[1])
        .filter((name) => !["drawString"].includes(name)))];
}

function tabIdForConstant(tabConstant) {
    return (TAB_PRESETS.get(tabConstant) ?? tabConstant.replace(/^TAB_/, "")).toLowerCase();
}

function fallbackTabId(tabByConstant) {
    return tabByConstant.get("TAB_PROCESSING") ?? tabByConstant.values().next().value ?? "main";
}

function uniqueElementId(elements, base) {
    let candidate = base;
    let index = 2;
    const ids = new Set(elements.map((element) => element.id));
    while (ids.has(candidate)) {
        candidate = `${base}-${index++}`;
    }
    return candidate;
}

function isDuplicateRect(elements, tabId, rect, type) {
    return elements.some((element) =>
        element.tabId === tabId
        && element.type === type
        && (sameRect(element, rect)
            || (["FLUID_TANK", "COMPRESSOR_TANK", "STATUS_BAR"].includes(type) && containsRect(element, rect))));
}

function sameRect(element, rect) {
    return Math.abs(element.x - rect.x) <= 1
        && Math.abs(element.y - rect.y) <= 1
        && Math.abs(element.width - rect.width) <= 2
        && Math.abs(element.height - rect.height) <= 2;
}

function containsRect(element, rect) {
    return rect.x >= element.x
        && rect.y >= element.y
        && rect.x + rect.width <= element.x + element.width
        && rect.y + rect.height <= element.y + element.height;
}

function looksLikeLoopExpression(expression) {
    return /\b(row|column|index|pip)\b/.test(expression);
}

function kebabCase(value) {
    return String(value)
        .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
        .replace(/[_\s]+/g, "-")
        .toLowerCase();
}

function titleCase(value) {
    return String(value).toLowerCase().split("_")
        .map((part) => part ? part[0].toUpperCase() + part.slice(1) : part)
        .join(" ");
}

function escapeRegExp(value) {
    return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

if (isDirectRun()) {
    const data = await buildGuiLayoutData();
    console.log(`Exported ${data.layouts.length} GUI layouts to ${path.relative(REPO_ROOT, OUTPUT_PATH)}`);
}

function isDirectRun() {
    return typeof process !== "undefined"
        && process.argv?.[1]
        && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
}
