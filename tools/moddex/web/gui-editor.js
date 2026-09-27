const GUI_SCALE = 2;
const GUI_SCHEMA = "rngtech.moddex.gui.v1";
const GUI_HISTORY_LIMIT = 80;
const SLOT_TYPES = ["BUCKET_INPUT", "BUCKET_OUTPUT", "ITEM", "FILTERED_ITEM", "MACHINE_SELF", "COMPONENT"];
const ORIENTATIONS = ["HORIZONTAL", "VERTICAL"];
const TAB_PRESETS = ["PROCESSING", "GEAR", "STATS", "REFINEMENT"];
const DEFAULT_TAB_LABELS = {
    PROCESSING: "rngtech.tab.processing.short",
    GEAR: "rngtech.tab.gear",
    STATS: "rngtech.tab.stats",
    REFINEMENT: "rngtech.tab.refinement.short"
};
const ELEMENT_DEFAULTS = {
    ITEM_SLOT: { width: 18, height: 18, label: "Item", slotType: "ITEM", visibleInJade: false },
    FLUID_SLOT: { width: 18, height: 18, label: "Fluid", slotType: "BUCKET_INPUT", visibleInJade: false },
    FE_METER: { width: 10, height: 50, label: "FE", orientation: "VERTICAL", meterKind: "ENERGY", visibleInJade: true },
    FLUID_METER: { width: 10, height: 50, label: "Fluid", orientation: "VERTICAL", meterKind: "FLUID", visibleInJade: true },
    FLUID_TANK: { width: 28, height: 48, label: "Tank", orientation: "VERTICAL", meterKind: "FLUID", tankRole: "FLUID", visibleInJade: true },
    COMPRESSOR_TANK: { width: 10, height: 50, label: "Compressor", orientation: "VERTICAL", meterKind: "FLUID", tankRole: "COMPRESSED_FLUID", visibleInJade: true },
    STATUS_BAR: { width: 56, height: 8, label: "Status", orientation: "HORIZONTAL", meterKind: "STATUS", statusSquares: [], visibleInJade: false },
    LABEL: { width: 54, height: 10, label: "Label", align: "LEFT", visibleInJade: false },
    CUSTOM_METER: { width: 12, height: 48, label: "Meter", orientation: "VERTICAL", meterKind: "CUSTOM", visibleInJade: false },
    PLAYER_INVENTORY: {
        width: 162,
        height: 87,
        label: "Inventory",
        labelKey: "container.inventory",
        rows: 3,
        columns: 9,
        hotbar: true,
        slotSize: 18,
        slotOffsetX: 0,
        slotOffsetY: 11,
        visibleInJade: false
    }
};
const METER_ELEMENT_TYPES = ["FE_METER", "FLUID_METER", "FLUID_TANK", "COMPRESSOR_TANK", "STATUS_BAR", "CUSTOM_METER"];
const FLUID_PARENT_TYPES = ["FLUID_METER", "FLUID_TANK", "COMPRESSOR_TANK"];

const guiState = {
    data: null,
    layout: null,
    activeTabId: "processing",
    selectedId: null,
    drag: null,
    undoStack: [],
    redoStack: [],
    restoring: false
};

const guiEls = {};

document.addEventListener("DOMContentLoaded", initGuiEditor);

async function initGuiEditor() {
    bindGuiElements();
    bindGuiEvents();
    await loadGuiSources();
}

function bindGuiElements() {
    for (const id of [
        "guiSourceSelect",
        "guiTitleInput",
        "guiActiveTabSelect",
        "guiPresetTabSelect",
        "guiBlankLayout",
        "guiAddPresetTab",
        "guiAddCustomTab",
        "guiElementCount",
        "guiPaletteBody",
        "guiCanvasMeta",
        "guiCanvasWrap",
        "guiTabStrip",
        "guiCanvas",
        "guiPropertiesBody",
        "guiDeleteSelected",
        "guiJsonOutput",
        "guiCopyJson"
    ]) {
        guiEls[id] = document.getElementById(id);
    }
}

function bindGuiEvents() {
    guiEls.guiSourceSelect.addEventListener("change", () => {
        const before = snapshotGuiState();
        loadGuiLayout(guiEls.guiSourceSelect.value);
        commitGuiHistory(before);
    });
    guiEls.guiTitleInput.addEventListener("keydown", (event) => {
        if (event.key !== "Enter") {
            return;
        }
        event.preventDefault();
        commitGuiTitleInput();
    });
    guiEls.guiActiveTabSelect.addEventListener("change", () => {
        guiState.activeTabId = guiEls.guiActiveTabSelect.value;
        guiState.selectedId = null;
        renderGuiEditor();
    });
    guiEls.guiBlankLayout.addEventListener("click", () => {
        const before = snapshotGuiState();
        guiState.layout = blankGuiLayout();
        guiState.activeTabId = guiState.layout.tabs[0].id;
        guiState.selectedId = null;
        guiEls.guiSourceSelect.value = "__blank";
        commitGuiHistory(before);
        renderGuiEditor();
    });
    guiEls.guiAddPresetTab.addEventListener("click", () => {
        addGuiTab("PRESET", guiEls.guiPresetTabSelect.value);
    });
    guiEls.guiAddCustomTab.addEventListener("click", () => {
        addGuiTab("CUSTOM", "CUSTOM");
    });
    guiEls.guiDeleteSelected.addEventListener("click", deleteSelectedGuiEntry);
    guiEls.guiCopyJson.addEventListener("click", copyGuiJson);

    guiEls.guiPaletteBody.querySelectorAll("[data-gui-add]").forEach((button) => {
        button.addEventListener("click", () => addGuiEntry(button.dataset.guiAdd));
        button.addEventListener("dragstart", (event) => {
            guiState.drag = { kind: "palette", type: button.dataset.guiAdd };
            event.dataTransfer.setData("text/plain", button.dataset.guiAdd);
        });
    });

    guiEls.guiCanvas.addEventListener("dragover", allowGuiDrop);
    guiEls.guiCanvas.addEventListener("drop", dropOnGuiCanvas);
    guiEls.guiTabStrip.addEventListener("dragover", allowGuiDrop);
    guiEls.guiTabStrip.addEventListener("drop", dropOnGuiTabStrip);
    guiEls.guiCanvas.addEventListener("click", (event) => {
        const title = event.target.closest("[data-gui-title-label]");
        if (title) {
            guiState.selectedId = "__title";
            renderGuiEditor();
            return;
        }
        const element = event.target.closest("[data-gui-element-id]");
        if (!element) {
            guiState.selectedId = null;
        } else {
            guiState.selectedId = element.dataset.guiElementId;
        }
        renderGuiEditor();
    });
    guiEls.guiTabStrip.addEventListener("click", (event) => {
        const tab = event.target.closest("[data-gui-tab-id]");
        if (!tab) {
            return;
        }
        guiState.activeTabId = tab.dataset.guiTabId;
        guiState.selectedId = `tab:${tab.dataset.guiTabId}`;
        renderGuiEditor();
    });
    guiEls.guiTabStrip.addEventListener("dragstart", (event) => {
        const tab = event.target.closest("[data-gui-tab-id]");
        if (!tab) {
            return;
        }
        guiState.drag = { kind: "tab", id: tab.dataset.guiTabId };
        event.dataTransfer.setData("text/plain", `tab:${tab.dataset.guiTabId}`);
    });
    guiEls.guiCanvas.addEventListener("dragstart", (event) => {
        const title = event.target.closest("[data-gui-title-label]");
        if (title) {
            const rect = title.getBoundingClientRect();
            guiState.drag = {
                kind: "title",
                offsetX: event.clientX - rect.left,
                offsetY: event.clientY - rect.top
            };
            event.dataTransfer.setData("text/plain", "__title");
            return;
        }
        const element = event.target.closest("[data-gui-element-id]");
        if (!element) {
            return;
        }
        const rect = element.getBoundingClientRect();
        guiState.drag = {
            kind: "element",
            id: element.dataset.guiElementId,
            offsetX: event.clientX - rect.left,
            offsetY: event.clientY - rect.top
        };
        event.dataTransfer.setData("text/plain", element.dataset.guiElementId);
    });
    guiEls.guiPropertiesBody.addEventListener("keydown", commitGuiPropertyOnEnter);
    guiEls.guiPropertiesBody.addEventListener("change", commitGuiPropertyOnChange);
    guiEls.guiPropertiesBody.addEventListener("click", handleGuiPropertyAction);
    document.addEventListener("keydown", handleGuiKeydown);
}

async function loadGuiSources() {
    try {
        const response = await fetch(`/generated/gui-layouts.json?ts=${Date.now()}`);
        guiState.data = await response.json();
    } catch {
        guiState.data = { layouts: [] };
    }

    const preferred = guiState.data.layouts.find((layout) => layout.id === "melter") ?? guiState.data.layouts[0];
    guiEls.guiSourceSelect.innerHTML = [
        `<option value="__blank">Blank GUI</option>`,
        ...guiState.data.layouts.map((layout) =>
            `<option value="${escapeHtml(layout.id)}">${escapeHtml(layout.screenClass)} (${escapeHtml(layout.sourceFile)})</option>`)
    ].join("");
    if (preferred) {
        guiEls.guiSourceSelect.value = preferred.id;
        loadGuiLayout(preferred.id);
    } else {
        guiState.layout = blankGuiLayout();
        renderGuiEditor();
    }
    clearGuiHistory();
}

function loadGuiLayout(layoutId) {
    if (layoutId === "__blank") {
        guiState.layout = blankGuiLayout();
    } else {
        const source = guiState.data.layouts.find((layout) => layout.id === layoutId);
        guiState.layout = source ? layoutFromSource(source) : blankGuiLayout();
    }
    guiState.activeTabId = guiState.layout.tabs[0]?.id ?? "processing";
    guiState.selectedId = null;
    renderGuiEditor();
}

function blankGuiLayout() {
    return {
        schema: GUI_SCHEMA,
        source: {
            origin: "moddex-blank",
            doesNotModifyGameFiles: true
        },
        screen: {
            id: "new-machine-screen",
            title: "New Machine Screen",
            screenClass: "",
            menuClass: "",
            width: 240,
            height: 200,
            coordinateSpace: "gui-relative-pixels",
            titleLabel: titleLabel("New Machine Screen", 8, 6)
        },
        tabs: TAB_PRESETS.map((preset, index) => ({
            id: preset.toLowerCase(),
            type: "TAB",
            kind: "PRESET",
            preset,
            labelKey: DEFAULT_TAB_LABELS[preset],
            x: 8 + index * 56,
            y: -20,
            width: 54,
            height: 21,
            tooltip: { enabled: false, text: "" },
            description: `${preset} tab.`
        })),
        elements: []
    };
}

function layoutFromSource(source) {
    const elements = source.elements.map((element) => normalizeGuiElement(element));
    inferGuiRelationships(elements);
    return {
        schema: GUI_SCHEMA,
        source: {
            origin: "java-screen-parser",
            screenClass: source.screenClass,
            menuClass: source.menuClass,
            sourceFile: source.sourceFile,
            generatedAt: guiState.data.generatedAt,
            doesNotModifyGameFiles: true
        },
        screen: {
            id: source.id,
            title: titleCase(source.id),
            screenClass: source.screenClass,
            menuClass: source.menuClass,
            width: source.dimensions.width,
            height: source.dimensions.height,
            coordinateSpace: "gui-relative-pixels",
            titleLabel: titleLabel(titleCase(source.id), source.titleLabel?.x ?? 8, source.titleLabel?.y ?? 6)
        },
        tabs: source.tabs.map((tab) => normalizeGuiTab(tab)),
        elements
    };
}

function titleLabel(label, x, y) {
    return {
        type: "LABEL",
        label,
        labelKey: "screen.title",
        x,
        y,
        width: Math.max(54, label.length * 6),
        height: 10,
        align: "LEFT",
        tooltip: { enabled: false, text: "" },
        description: "Screen title label rendered by AbstractContainerScreen."
    };
}

function normalizeGuiTab(tab) {
    return {
        id: tab.id,
        type: "TAB",
        kind: tab.kind ?? "PRESET",
        preset: tab.preset ?? "CUSTOM",
        label: tab.label ?? labelFromKey(tab.labelKey ?? tab.id),
        labelKey: tab.labelKey ?? "",
        x: tab.x ?? 8,
        y: tab.y ?? -20,
        width: tab.width ?? 54,
        height: tab.height ?? 21,
        tooltip: tab.tooltip ?? { enabled: false, text: "" },
        description: tab.description ?? ""
    };
}

function normalizeGuiElement(element) {
    const defaults = ELEMENT_DEFAULTS[element.type] ?? {};
    return {
        id: element.id,
        type: element.type,
        tabId: element.tabId ?? "processing",
        x: element.x ?? 0,
        y: element.y ?? 0,
        width: element.width ?? defaults.width ?? 18,
        height: element.height ?? defaults.height ?? 18,
        label: element.label ?? defaults.label ?? titleCase(element.type),
        labelKey: element.labelKey ?? "",
        slotType: element.slotType ?? defaults.slotType,
        slotRole: element.slotRole ?? element.slotType ?? defaults.slotType,
        filter: element.filter ?? "",
        parentId: element.parentId ?? "",
        visibleInJade: Boolean(element.visibleInJade ?? defaults.visibleInJade ?? false),
        inputGroup: element.inputGroup ?? "",
        inputValue: element.inputValue ?? "",
        rows: element.rows ?? defaults.rows,
        columns: element.columns ?? defaults.columns,
        hotbar: Boolean(element.hotbar ?? defaults.hotbar ?? false),
        slotSize: element.slotSize ?? defaults.slotSize,
        slotOffsetX: element.slotOffsetX ?? defaults.slotOffsetX,
        slotOffsetY: element.slotOffsetY ?? defaults.slotOffsetY,
        orientation: element.orientation ?? defaults.orientation,
        meterKind: element.meterKind ?? defaults.meterKind,
        tankRole: element.tankRole ?? defaults.tankRole,
        align: element.align ?? defaults.align,
        tooltip: element.tooltip ?? { enabled: false, text: "" },
        description: element.description ?? "",
        statusSquares: (element.statusSquares ?? defaults.statusSquares ?? []).map((square) => ({ ...square })),
        source: element.source
    };
}

function inferGuiRelationships(elements) {
    for (const element of elements) {
        if (element.type !== "FLUID_SLOT" || element.parentId) {
            continue;
        }
        const parent = nearestFluidParent(elements, element);
        if (!parent) {
            continue;
        }
        parent.inputGroup ||= inputGroupFromId(parent.id);
        element.parentId = parent.id;
        element.inputGroup ||= parent.inputGroup;
    }
}

function nearestFluidParent(elements, child) {
    let best = null;
    let bestScore = Number.POSITIVE_INFINITY;
    const childCenterX = child.x + child.width / 2;
    for (const candidate of elements) {
        if (candidate.tabId !== child.tabId || !FLUID_PARENT_TYPES.includes(candidate.type)) {
            continue;
        }
        const candidateCenterX = candidate.x + candidate.width / 2;
        const verticalGap = child.y - (candidate.y + candidate.height);
        if (verticalGap < -4 || verticalGap > 28) {
            continue;
        }
        const score = Math.abs(childCenterX - candidateCenterX) + Math.abs(verticalGap);
        if (score < bestScore && score <= 32) {
            best = candidate;
            bestScore = score;
        }
    }
    return best;
}

function inputGroupFromId(id) {
    return String(id ?? "input").replace(/[^a-z0-9_]+/gi, "_").toLowerCase();
}

function renderGuiEditor() {
    if (!guiState.layout) {
        return;
    }
    if (!guiState.layout.tabs.some((tab) => tab.id === guiState.activeTabId)) {
        guiState.activeTabId = guiState.layout.tabs[0]?.id ?? "";
    }
    if (document.activeElement !== guiEls.guiTitleInput) {
        guiEls.guiTitleInput.value = guiState.layout.screen.title;
    }
    renderGuiTabSelect();
    renderGuiCanvas();
    renderGuiProperties();
    renderGuiJson();
}

function handleGuiKeydown(event) {
    if (!isGuiEditorActive() || !(event.ctrlKey || event.metaKey)) {
        return;
    }
    if (event.target === guiEls.guiJsonOutput) {
        return;
    }
    const key = event.key.toLowerCase();
    if (key === "z" && !event.shiftKey) {
        event.preventDefault();
        undoGuiEdit();
        return;
    }
    if (key === "y" || (key === "z" && event.shiftKey)) {
        event.preventDefault();
        redoGuiEdit();
    }
}

function commitGuiTitleInput() {
    if (!guiState.layout) {
        return;
    }
    const before = snapshotGuiState();
    guiState.layout.screen.title = guiEls.guiTitleInput.value;
    guiState.layout.screen.titleLabel.label = guiEls.guiTitleInput.value;
    guiState.layout.screen.titleLabel.width = Math.max(54, guiEls.guiTitleInput.value.length * 6);
    commitGuiHistory(before);
    renderGuiEditor();
}

function isGuiEditorActive() {
    const guiView = document.getElementById("guiView");
    return Boolean(guiView && !guiView.classList.contains("hidden"));
}

function snapshotGuiState() {
    if (!guiState.layout) {
        return null;
    }
    return JSON.stringify({
        layout: guiState.layout,
        activeTabId: guiState.activeTabId,
        selectedId: guiState.selectedId,
        sourceValue: guiEls.guiSourceSelect?.value ?? "__blank"
    });
}

function commitGuiHistory(beforeSnapshot) {
    if (guiState.restoring || !beforeSnapshot || beforeSnapshot === snapshotGuiState()) {
        return;
    }
    if (guiState.undoStack.at(-1) !== beforeSnapshot) {
        guiState.undoStack.push(beforeSnapshot);
        if (guiState.undoStack.length > GUI_HISTORY_LIMIT) {
            guiState.undoStack.shift();
        }
    }
    guiState.redoStack = [];
}

function clearGuiHistory() {
    guiState.undoStack = [];
    guiState.redoStack = [];
}

function undoGuiEdit() {
    const snapshot = guiState.undoStack.pop();
    if (!snapshot) {
        return;
    }
    const current = snapshotGuiState();
    if (current) {
        guiState.redoStack.push(current);
    }
    restoreGuiSnapshot(snapshot);
}

function redoGuiEdit() {
    const snapshot = guiState.redoStack.pop();
    if (!snapshot) {
        return;
    }
    const current = snapshotGuiState();
    if (current) {
        guiState.undoStack.push(current);
    }
    restoreGuiSnapshot(snapshot);
}

function restoreGuiSnapshot(snapshot) {
    const restored = JSON.parse(snapshot);
    guiState.restoring = true;
    try {
        guiState.layout = restored.layout;
        guiState.activeTabId = restored.activeTabId;
        guiState.selectedId = restored.selectedId;
        setGuiSourceValue(restored.sourceValue);
        renderGuiEditor();
    } finally {
        guiState.restoring = false;
    }
}

function setGuiSourceValue(value) {
    const options = [...guiEls.guiSourceSelect.options];
    guiEls.guiSourceSelect.value = options.some((option) => option.value === value) ? value : "__blank";
}

function renderGuiTabSelect() {
    guiEls.guiActiveTabSelect.innerHTML = guiState.layout.tabs.map((tab) =>
        `<option value="${escapeHtml(tab.id)}">${escapeHtml(tabDisplayLabel(tab))}</option>`).join("");
    guiEls.guiActiveTabSelect.value = guiState.activeTabId;
}

function renderGuiCanvas() {
    const layout = guiState.layout;
    const screenWidth = layout.screen.width * GUI_SCALE;
    const screenHeight = layout.screen.height * GUI_SCALE;
    guiEls.guiCanvasMeta.textContent = `${layout.screen.width}x${layout.screen.height}, ${layout.elements.length} element${layout.elements.length === 1 ? "" : "s"}`;
    guiEls.guiElementCount.textContent = `${layout.tabs.length} tab${layout.tabs.length === 1 ? "" : "s"}`;
    guiEls.guiTabStrip.style.width = `${screenWidth}px`;
    guiEls.guiCanvas.style.width = `${screenWidth}px`;
    guiEls.guiCanvas.style.height = `${screenHeight}px`;

    guiEls.guiTabStrip.innerHTML = layout.tabs.map((tab) => `
        <button
          class="gui-tab ${tab.id === guiState.activeTabId ? "active" : ""} ${guiState.selectedId === `tab:${tab.id}` ? "selected" : ""}"
          data-gui-tab-id="${escapeHtml(tab.id)}"
          draggable="true"
          type="button"
          style="left:${tab.x * GUI_SCALE}px;top:${(tab.y + 22) * GUI_SCALE}px;width:${tab.width * GUI_SCALE}px;height:${tab.height * GUI_SCALE}px"
          title="${escapeHtml(tab.description ?? "")}">
          ${escapeHtml(tabDisplayLabel(tab))}
        </button>
    `).join("");

    guiEls.guiCanvas.innerHTML = guiTitleMarkup(layout.screen.titleLabel)
        + layout.elements
        .filter((element) => element.tabId === guiState.activeTabId)
        .map(guiElementMarkup)
        .join("");
}

function guiTitleMarkup(title) {
    return `
        <div
          class="gui-element gui-label gui-title-label ${guiState.selectedId === "__title" ? "selected" : ""}"
          data-gui-title-label="true"
          draggable="true"
          style="left:${title.x * GUI_SCALE}px;top:${title.y * GUI_SCALE}px;width:${title.width * GUI_SCALE}px;height:${title.height * GUI_SCALE}px"
          title="${escapeHtml(title.description ?? "")}">
          <span>${escapeHtml(title.label ?? guiState.layout.screen.title)}</span>
        </div>
    `;
}

function guiElementMarkup(element) {
    const classes = [
        "gui-element",
        `gui-${element.type.toLowerCase()}`,
        element.type === "STATUS_BAR" && element.statusSquares?.length ? "gui-status-squares" : "",
        guiState.selectedId === element.id ? "selected" : ""
    ].filter(Boolean).join(" ");
    return `
        <div
          class="${classes}"
          data-gui-element-id="${escapeHtml(element.id)}"
          draggable="true"
          style="left:${element.x * GUI_SCALE}px;top:${element.y * GUI_SCALE}px;width:${element.width * GUI_SCALE}px;height:${element.height * GUI_SCALE}px"
          title="${escapeHtml(element.tooltip?.enabled ? element.tooltip.text : element.description ?? "")}">
          ${guiElementInnerMarkup(element)}
        </div>
    `;
}

function guiElementInnerMarkup(element) {
    if (element.type === "LABEL") {
        return `<span>${escapeHtml(element.label || labelFromKey(element.labelKey))}</span>`;
    }
    if (element.type === "PLAYER_INVENTORY") {
        return playerInventoryMarkup(element);
    }
    if (element.type === "ITEM_SLOT" || element.type === "FLUID_SLOT") {
        return `<span>${escapeHtml(slotShortLabel(element))}</span>`;
    }
    if (element.type === "STATUS_BAR" && element.statusSquares?.length) {
        return element.statusSquares.map((square) => `
            <span
              class="gui-status-square"
              style="left:${(square.x ?? 0) * GUI_SCALE}px;top:${(square.y ?? 0) * GUI_SCALE}px;width:${(square.size ?? 8) * GUI_SCALE}px;height:${(square.size ?? 8) * GUI_SCALE}px"
              title="${escapeHtml(square.tooltip?.enabled ? square.tooltip.text : square.description ?? "")}"></span>
        `).join("");
    }
    if (METER_ELEMENT_TYPES.includes(element.type)) {
        const fillStyle = element.orientation === "HORIZONTAL"
            ? "width:62%;height:100%;left:0;bottom:0"
            : "height:62%;width:100%;left:0;bottom:0";
        return `<span class="gui-meter-fill" style="${fillStyle}"></span><span>${escapeHtml(element.label ?? "")}</span>`;
    }
    return "";
}

function playerInventoryMarkup(element) {
    const columns = Number(element.columns ?? 9);
    const rows = Number(element.rows ?? 3);
    const slotSize = Number(element.slotSize ?? 18);
    const offsetX = Number(element.slotOffsetX ?? 0);
    const offsetY = Number(element.slotOffsetY ?? 11);
    const slots = [];
    slots.push(`<span class="gui-inventory-label">${escapeHtml(element.label ?? "Inventory")}</span>`);
    for (let row = 0; row < rows; row++) {
        for (let column = 0; column < columns; column++) {
            slots.push(inventorySlotMarkup(offsetX + column * slotSize, offsetY + row * slotSize, slotSize));
        }
    }
    if (element.hotbar) {
        const hotbarY = offsetY + rows * slotSize + 4;
        for (let column = 0; column < columns; column++) {
            slots.push(inventorySlotMarkup(offsetX + column * slotSize, hotbarY, slotSize));
        }
    }
    return slots.join("");
}

function inventorySlotMarkup(x, y, size) {
    return `<span class="gui-inventory-slot" style="left:${x * GUI_SCALE}px;top:${y * GUI_SCALE}px;width:${size * GUI_SCALE}px;height:${size * GUI_SCALE}px"></span>`;
}

function renderGuiProperties() {
    const selected = selectedGuiEntry();
    guiEls.guiDeleteSelected.disabled = !selected;
    if (!selected) {
        guiEls.guiPropertiesBody.innerHTML = `<div class="muted">No selection</div>`;
        return;
    }
    guiEls.guiPropertiesBody.innerHTML = selected.kind === "tab"
        ? tabPropertiesMarkup(selected.value)
        : selected.kind === "title"
        ? titlePropertiesMarkup(selected.value)
        : elementPropertiesMarkup(selected.value);
}

function titlePropertiesMarkup(title) {
    return [
        textField("label", "Title", title.label ?? ""),
        textField("labelKey", "Label Key", title.labelKey ?? "screen.title"),
        numberField("x", "X", title.x),
        numberField("y", "Y", title.y),
        numberField("width", "Width", title.width),
        numberField("height", "Height", title.height),
        tooltipFields(title),
        textareaField("description", "Description", title.description ?? "")
    ].join("");
}

function tabPropertiesMarkup(tab) {
    return [
        selectField("kind", "Kind", tab.kind, ["PRESET", "CUSTOM"]),
        selectField("preset", "Preset", tab.preset, ["CUSTOM", ...TAB_PRESETS]),
        textField("label", "Label", tab.label ?? ""),
        textField("labelKey", "Label Key", tab.labelKey ?? ""),
        numberField("x", "X", tab.x),
        numberField("y", "Y", tab.y),
        numberField("width", "Width", tab.width),
        numberField("height", "Height", tab.height),
        tooltipFields(tab),
        textareaField("description", "Description", tab.description ?? "")
    ].join("");
}

function elementPropertiesMarkup(element) {
    const fields = [
        selectField("type", "Type", element.type, Object.keys(ELEMENT_DEFAULTS)),
        selectField("tabId", "Tab", element.tabId, guiState.layout.tabs.map((tab) => tab.id)),
        textField("label", "Label", element.label ?? ""),
        textField("labelKey", "Label Key", element.labelKey ?? ""),
        numberField("x", "X", element.x),
        numberField("y", "Y", element.y),
        numberField("width", "Width", element.width),
        numberField("height", "Height", element.height),
        parentReferenceField(element),
        checkboxField("visibleInJade", "Visible in Jade", element.visibleInJade),
        textField("inputGroup", "Input Group", element.inputGroup ?? ""),
        textField("inputValue", "Input Value", element.inputValue ?? "")
    ];
    if (element.type === "ITEM_SLOT" || element.type === "FLUID_SLOT") {
        fields.push(selectField("slotType", "Slot Type", element.slotType ?? "ITEM", SLOT_TYPES));
        fields.push(textField("slotRole", "Slot Role", element.slotRole ?? ""));
        fields.push(textField("filter", "Filter", element.filter ?? ""));
    }
    if (METER_ELEMENT_TYPES.includes(element.type)) {
        fields.push(selectField("orientation", "Orientation", element.orientation ?? "VERTICAL", ORIENTATIONS));
        fields.push(textField("meterKind", "Meter Kind", element.meterKind ?? ""));
    }
    if (element.type === "FLUID_TANK" || element.type === "COMPRESSOR_TANK") {
        fields.push(textField("tankRole", "Tank Role", element.tankRole ?? ""));
    }
    if (element.type === "PLAYER_INVENTORY") {
        fields.push(numberField("columns", "Columns", element.columns ?? 9));
        fields.push(numberField("rows", "Rows", element.rows ?? 3));
        fields.push(checkboxField("hotbar", "Hotbar", element.hotbar));
        fields.push(numberField("slotSize", "Slot Size", element.slotSize ?? 18));
        fields.push(numberField("slotOffsetX", "Slot Offset X", element.slotOffsetX ?? 0));
        fields.push(numberField("slotOffsetY", "Slot Offset Y", element.slotOffsetY ?? 11));
    }
    fields.push(tooltipFields(element));
    fields.push(textareaField("description", "Description", element.description ?? ""));
    if (element.type === "STATUS_BAR") {
        fields.push(statusSquaresMarkup(element));
    }
    return fields.join("");
}

function tooltipFields(entry) {
    return `
        ${checkboxField("tooltip.enabled", "Tooltip", entry.tooltip?.enabled)}
        ${textField("tooltip.text", "Tooltip Text", entry.tooltip?.text ?? "")}
    `;
}

function parentReferenceField(element) {
    const options = [
        { id: "", label: "None" },
        ...guiState.layout.elements
            .filter((candidate) => candidate.id !== element.id)
            .map((candidate) => ({
                id: candidate.id,
                label: `${candidate.id} (${candidate.type})`
            }))
    ];
    return `
        <label>
          Parent Ref
          <select data-gui-field="parentId">
            ${options.map((option) =>
                `<option value="${escapeHtml(option.id)}" ${option.id === (element.parentId ?? "") ? "selected" : ""}>${escapeHtml(option.label)}</option>`)
                .join("")}
          </select>
        </label>
    `;
}

function checkboxField(field, label, checked) {
    return `
        <label class="toggle-label property-toggle">
          <input data-gui-field="${escapeHtml(field)}" type="checkbox" ${checked ? "checked" : ""}>
          ${escapeHtml(label)}
        </label>
    `;
}

function statusSquaresMarkup(element) {
    const squares = element.statusSquares ?? [];
    return `
        <div class="property-wide status-square-editor">
          <div class="panel-heading mini-heading">
            <h3>Status Squares</h3>
            <button data-gui-action="add-square" type="button">Add</button>
          </div>
          ${squares.map((square, index) => `
              <div class="status-square-row">
                ${numberField("square.x", "X", square.x ?? 0, index)}
                ${numberField("square.y", "Y", square.y ?? 0, index)}
                ${numberField("square.size", "Size", square.size ?? 8, index)}
                ${textField("square.description", "Description", square.description ?? "", index)}
                <button data-gui-action="remove-square" data-square-index="${index}" type="button">Remove</button>
              </div>
          `).join("") || `<div class="muted">No status squares</div>`}
        </div>
    `;
}

function textField(field, label, value, squareIndex = null) {
    return `
        <label>
          ${escapeHtml(label)}
          <input ${fieldAttrs(field, squareIndex)} type="text" value="${escapeHtml(value)}" title="Press Enter to apply">
        </label>
    `;
}

function textareaField(field, label, value) {
    return `
        <label class="property-wide">
          ${escapeHtml(label)}
          <textarea ${fieldAttrs(field)} rows="4" title="Press Enter to apply. Use Shift+Enter for a newline.">${escapeHtml(value)}</textarea>
        </label>
    `;
}

function numberField(field, label, value, squareIndex = null) {
    return `
        <label>
          ${escapeHtml(label)}
          <input ${fieldAttrs(field, squareIndex)} type="number" step="1" value="${Number(value ?? 0)}" title="Press Enter to apply">
        </label>
    `;
}

function selectField(field, label, value, options) {
    return `
        <label>
          ${escapeHtml(label)}
          <select ${fieldAttrs(field)}>
            ${options.map((option) => `<option value="${escapeHtml(option)}" ${option === value ? "selected" : ""}>${escapeHtml(titleCase(option))}</option>`).join("")}
          </select>
        </label>
    `;
}

function fieldAttrs(field, squareIndex = null) {
    if (squareIndex !== null) {
        return `data-gui-field="${escapeHtml(field)}" data-square-index="${squareIndex}"`;
    }
    return `data-gui-field="${escapeHtml(field)}"`;
}

function commitGuiPropertyOnEnter(event) {
    const field = event.target.dataset.guiField;
    if (!field || event.key !== "Enter") {
        return;
    }
    if (event.target.tagName === "TEXTAREA" && event.shiftKey) {
        return;
    }
    if (!isEnterAppliedField(event.target)) {
        return;
    }
    event.preventDefault();
    updateGuiProperty(event.target);
}

function commitGuiPropertyOnChange(event) {
    if (!event.target.dataset.guiField || isEnterAppliedField(event.target)) {
        return;
    }
    updateGuiProperty(event.target);
}

function isEnterAppliedField(target) {
    return target instanceof HTMLTextAreaElement
        || (target instanceof HTMLInputElement && ["text", "number"].includes(target.type));
}

function updateGuiProperty(target) {
    const field = target.dataset.guiField;
    if (!field) {
        return;
    }
    const selected = selectedGuiEntry();
    if (!selected) {
        return;
    }
    const before = snapshotGuiState();
    const rawValue = target.type === "checkbox" ? target.checked : target.value;
    const value = isNumericField(field) ? Number(rawValue) : rawValue;
    if (field.startsWith("square.")) {
        const square = selected.value.statusSquares[Number(target.dataset.squareIndex)];
        square[field.replace("square.", "")] = value;
    } else if (field.startsWith("tooltip.")) {
        selected.value.tooltip ??= { enabled: false, text: "" };
        selected.value.tooltip[field.replace("tooltip.", "")] = value;
    } else {
        selected.value[field] = value;
        if (selected.kind === "title" && field === "label") {
            guiState.layout.screen.title = value;
            selected.value.width = Math.max(54, String(value).length * 6);
        }
        if (field === "type") {
            applyElementDefaults(selected.value);
        }
        if (selected.kind === "tab" && field === "preset" && value !== "CUSTOM") {
            selected.value.kind = "PRESET";
            selected.value.labelKey ||= DEFAULT_TAB_LABELS[value] ?? "";
        }
        if (selected.kind === "element" && field === "tabId") {
            guiState.activeTabId = value;
        }
    }
    commitGuiHistory(before);
    renderGuiEditor();
}

function handleGuiPropertyAction(event) {
    const action = event.target.dataset.guiAction;
    if (!action) {
        return;
    }
    const selected = selectedGuiEntry();
    if (!selected || selected.kind !== "element" || selected.value.type !== "STATUS_BAR") {
        return;
    }
    const before = snapshotGuiState();
    selected.value.statusSquares ??= [];
    if (action === "add-square") {
        selected.value.statusSquares.push({
            id: uniqueId("status_square", selected.value.statusSquares),
            x: 0,
            y: 0,
            size: 8,
            description: "Dynamic status square."
        });
    }
    if (action === "remove-square") {
        selected.value.statusSquares.splice(Number(event.target.dataset.squareIndex), 1);
    }
    commitGuiHistory(before);
    renderGuiEditor();
}

function applyElementDefaults(element) {
    const defaults = ELEMENT_DEFAULTS[element.type] ?? {};
    element.width = defaults.width ?? element.width;
    element.height = defaults.height ?? element.height;
    element.label ||= defaults.label ?? titleCase(element.type);
    element.slotType = defaults.slotType ?? element.slotType;
    element.orientation = defaults.orientation ?? element.orientation;
    element.meterKind = defaults.meterKind ?? element.meterKind;
    element.tankRole = defaults.tankRole ?? element.tankRole;
    element.visibleInJade ??= defaults.visibleInJade ?? false;
    element.parentId ??= "";
    element.inputGroup ??= "";
    element.inputValue ??= "";
    element.rows = defaults.rows ?? element.rows;
    element.columns = defaults.columns ?? element.columns;
    element.hotbar = defaults.hotbar ?? element.hotbar;
    element.slotSize = defaults.slotSize ?? element.slotSize;
    element.slotOffsetX = defaults.slotOffsetX ?? element.slotOffsetX;
    element.slotOffsetY = defaults.slotOffsetY ?? element.slotOffsetY;
    element.statusSquares = defaults.statusSquares ? defaults.statusSquares.map((square) => ({ ...square })) : element.statusSquares;
}

function addGuiEntry(type, x = 12, y = 24) {
    if (type === "TAB") {
        addGuiTab("CUSTOM", "CUSTOM", x);
        return;
    }
    if (type === "FLUID_STACK") {
        addFluidStack(x, y);
        return;
    }
    const before = snapshotGuiState();
    const defaults = ELEMENT_DEFAULTS[type] ?? ELEMENT_DEFAULTS.LABEL;
    const element = normalizeGuiElement({
        id: uniqueId(type.toLowerCase(), guiState.layout.elements),
        type,
        tabId: guiState.activeTabId,
        x,
        y,
        ...defaults,
        tooltip: { enabled: false, text: "" },
        description: `${titleCase(type)} for an LLM implementation spec.`
    });
    guiState.layout.elements.push(element);
    guiState.selectedId = element.id;
    commitGuiHistory(before);
    renderGuiEditor();
}

function addFluidStack(x = 12, y = 24) {
    const before = snapshotGuiState();
    const baseX = clampInt(x, 0, Math.max(0, guiState.layout.screen.width - 40));
    const baseY = clampInt(y, 0, Math.max(0, guiState.layout.screen.height - 72));
    const inputGroup = uniqueInputGroup("fluid", guiState.layout.elements);
    const meterId = uniqueId("fluid_meter", guiState.layout.elements);
    const existingWithMeter = [...guiState.layout.elements, { id: meterId }];
    const inputId = uniqueId("fluid_bucket_input", existingWithMeter);
    const outputId = uniqueId("fluid_bucket_output", [...existingWithMeter, { id: inputId }]);
    const meter = normalizeGuiElement({
        id: meterId,
        type: "FLUID_METER",
        tabId: guiState.activeTabId,
        x: baseX + 15,
        y: baseY,
        width: 10,
        height: 50,
        label: "Fluid",
        orientation: "VERTICAL",
        meterKind: "FLUID",
        inputGroup,
        inputValue: "fluid",
        visibleInJade: true,
        tooltip: { enabled: true, text: "Fluid amount / capacity" },
        description: "Parent fluid meter for the linked bucket input and output slots."
    });
    const input = normalizeGuiElement({
        id: inputId,
        type: "FLUID_SLOT",
        tabId: guiState.activeTabId,
        x: baseX,
        y: baseY + 54,
        width: 18,
        height: 18,
        label: "Fluid Input",
        slotType: "BUCKET_INPUT",
        slotRole: "FLUID_CONTAINER_INPUT",
        parentId: meterId,
        inputGroup,
        inputValue: "bucket_input",
        tooltip: { enabled: false, text: "" },
        description: "Bucket or fluid-container input slot linked to the parent fluid meter."
    });
    const output = normalizeGuiElement({
        id: outputId,
        type: "FLUID_SLOT",
        tabId: guiState.activeTabId,
        x: baseX + 22,
        y: baseY + 54,
        width: 18,
        height: 18,
        label: "Fluid Output",
        slotType: "BUCKET_OUTPUT",
        slotRole: "FLUID_CONTAINER_OUTPUT",
        parentId: meterId,
        inputGroup,
        inputValue: "bucket_output",
        tooltip: { enabled: false, text: "" },
        description: "Bucket or fluid-container output slot linked to the parent fluid meter."
    });
    guiState.layout.elements.push(meter, input, output);
    guiState.selectedId = meter.id;
    commitGuiHistory(before);
    renderGuiEditor();
}

function addGuiTab(kind, preset, x = null) {
    const before = snapshotGuiState();
    const baseId = preset === "CUSTOM" ? "custom" : preset.toLowerCase();
    const tab = normalizeGuiTab({
        id: uniqueId(baseId, guiState.layout.tabs),
        kind,
        preset,
        label: preset === "CUSTOM" ? "Custom" : titleCase(preset),
        labelKey: preset === "CUSTOM" ? "" : DEFAULT_TAB_LABELS[preset],
        x: x ?? 8 + guiState.layout.tabs.length * 56,
        y: -20,
        width: 54,
        height: 21,
        tooltip: { enabled: false, text: "" },
        description: preset === "CUSTOM" ? "Custom tab for this GUI." : `${preset} preset tab.`
    });
    guiState.layout.tabs.push(tab);
    guiState.activeTabId = tab.id;
    guiState.selectedId = `tab:${tab.id}`;
    commitGuiHistory(before);
    renderGuiEditor();
}

function deleteSelectedGuiEntry() {
    const selected = selectedGuiEntry();
    if (!selected) {
        return;
    }
    const before = snapshotGuiState();
    if (selected.kind === "tab") {
        if (guiState.layout.tabs.length <= 1) {
            return;
        }
        guiState.layout.tabs = guiState.layout.tabs.filter((tab) => tab.id !== selected.value.id);
        guiState.layout.elements = guiState.layout.elements.filter((element) => element.tabId !== selected.value.id);
        guiState.activeTabId = guiState.layout.tabs[0].id;
    } else {
        guiState.layout.elements = guiState.layout.elements.filter((element) => element.id !== selected.value.id);
    }
    guiState.selectedId = null;
    commitGuiHistory(before);
    renderGuiEditor();
}

function allowGuiDrop(event) {
    event.preventDefault();
}

function dropOnGuiCanvas(event) {
    event.preventDefault();
    const point = guiPointFromEvent(event, guiEls.guiCanvas);
    if (guiState.drag?.kind === "palette") {
        addGuiEntry(guiState.drag.type, point.x, point.y);
    } else if (guiState.drag?.kind === "title") {
        const before = snapshotGuiState();
        const title = guiState.layout.screen.titleLabel;
        title.x = clampInt(Math.round(point.x - guiState.drag.offsetX / GUI_SCALE), 0, guiState.layout.screen.width - title.width);
        title.y = clampInt(Math.round(point.y - guiState.drag.offsetY / GUI_SCALE), 0, guiState.layout.screen.height - title.height);
        guiState.selectedId = "__title";
        commitGuiHistory(before);
        renderGuiEditor();
    } else if (guiState.drag?.kind === "element") {
        const element = guiState.layout.elements.find((entry) => entry.id === guiState.drag.id);
        if (element) {
            const before = snapshotGuiState();
            element.x = clampInt(Math.round(point.x - guiState.drag.offsetX / GUI_SCALE), 0, guiState.layout.screen.width - element.width);
            element.y = clampInt(Math.round(point.y - guiState.drag.offsetY / GUI_SCALE), 0, guiState.layout.screen.height - element.height);
            guiState.selectedId = element.id;
            commitGuiHistory(before);
            renderGuiEditor();
        }
    }
    guiState.drag = null;
}

function dropOnGuiTabStrip(event) {
    event.preventDefault();
    const point = guiPointFromEvent(event, guiEls.guiTabStrip);
    if (guiState.drag?.kind === "palette" && guiState.drag.type === "TAB") {
        addGuiTab("CUSTOM", "CUSTOM", point.x);
    } else if (guiState.drag?.kind === "tab") {
        const tab = guiState.layout.tabs.find((entry) => entry.id === guiState.drag.id);
        if (tab) {
            const before = snapshotGuiState();
            tab.x = point.x;
            guiState.selectedId = `tab:${tab.id}`;
            commitGuiHistory(before);
            renderGuiEditor();
        }
    }
    guiState.drag = null;
}

function guiPointFromEvent(event, target) {
    const rect = target.getBoundingClientRect();
    return {
        x: clampInt(Math.round((event.clientX - rect.left) / GUI_SCALE), 0, guiState.layout.screen.width),
        y: clampInt(Math.round((event.clientY - rect.top) / GUI_SCALE), 0, guiState.layout.screen.height)
    };
}

function selectedGuiEntry() {
    if (!guiState.selectedId) {
        return null;
    }
    if (guiState.selectedId.startsWith("tab:")) {
        const tabId = guiState.selectedId.slice("tab:".length);
        const tab = guiState.layout.tabs.find((entry) => entry.id === tabId);
        return tab ? { kind: "tab", value: tab } : null;
    }
    if (guiState.selectedId === "__title") {
        return { kind: "title", value: guiState.layout.screen.titleLabel };
    }
    const element = guiState.layout.elements.find((entry) => entry.id === guiState.selectedId);
    return element ? { kind: "element", value: element } : null;
}

function renderGuiJson() {
    guiEls.guiJsonOutput.value = JSON.stringify(buildGuiExport(), null, 2);
}

function buildGuiExport() {
    return {
        schema: GUI_SCHEMA,
        exportedAt: new Date().toISOString(),
        source: guiState.layout.source,
        screen: guiState.layout.screen,
        tabs: guiState.layout.tabs.map((tab) => ({
            id: tab.id,
            kind: tab.kind,
            preset: tab.preset,
            label: tab.label,
            labelKey: tab.labelKey,
            bounds: boundsFor(tab),
            tooltip: normalizedTooltip(tab),
            description: tab.description
        })),
        elements: guiState.layout.elements.map((element) => ({
            id: element.id,
            type: element.type,
            tabId: element.tabId,
            label: element.label,
            labelKey: element.labelKey,
            bounds: boundsFor(element),
            slotType: element.slotType,
            slotRole: element.slotRole,
            filter: element.filter,
            parentId: element.parentId || undefined,
            visibleInJade: Boolean(element.visibleInJade),
            inputGroup: element.inputGroup || undefined,
            inputValue: element.inputValue || undefined,
            rows: element.type === "PLAYER_INVENTORY" ? element.rows : undefined,
            columns: element.type === "PLAYER_INVENTORY" ? element.columns : undefined,
            hotbar: element.type === "PLAYER_INVENTORY" ? Boolean(element.hotbar) : undefined,
            slotSize: element.type === "PLAYER_INVENTORY" ? element.slotSize : undefined,
            slotOffsetX: element.type === "PLAYER_INVENTORY" ? element.slotOffsetX : undefined,
            slotOffsetY: element.type === "PLAYER_INVENTORY" ? element.slotOffsetY : undefined,
            orientation: element.orientation,
            meterKind: element.meterKind,
            tankRole: element.tankRole,
            statusSquares: element.type === "STATUS_BAR" ? element.statusSquares ?? [] : undefined,
            tooltip: normalizedTooltip(element),
            description: element.description,
            source: element.source
        })),
        implementationHints: [
            "Coordinates are GUI-relative pixels before leftPos/topPos is added.",
            "This Moddex export does not update game files; use it as an implementation brief for Java screen/menu work.",
            "parentId links child controls such as bucket slots to a visual parent such as a fluid meter.",
            "inputGroup/inputValue identify which recipe input group and value an element represents.",
            "PLAYER_INVENTORY describes the vanilla player inventory region when the GUI renders it for the selected tab/state.",
            "Descriptions are intentionally free-form so an LLM can infer behavior, accepted filters, data bindings, and rendering details."
        ]
    };
}

function boundsFor(entry) {
    return {
        x: Number(entry.x ?? 0),
        y: Number(entry.y ?? 0),
        width: Number(entry.width ?? 0),
        height: Number(entry.height ?? 0)
    };
}

function normalizedTooltip(entry) {
    return {
        enabled: Boolean(entry.tooltip?.enabled),
        text: entry.tooltip?.text ?? ""
    };
}

async function copyGuiJson() {
    const text = guiEls.guiJsonOutput.value;
    try {
        await navigator.clipboard.writeText(text);
    } catch {
        guiEls.guiJsonOutput.focus();
        guiEls.guiJsonOutput.select();
    }
}

function tabDisplayLabel(tab) {
    return tab.label || labelFromKey(tab.labelKey) || titleCase(tab.id);
}

function labelFromKey(key) {
    return key ? titleCase(key.split(".").at(-1)) : "";
}

function slotShortLabel(element) {
    if (element.slotType === "BUCKET_INPUT") {
        return "B+";
    }
    if (element.slotType === "BUCKET_OUTPUT") {
        return "B-";
    }
    if (element.slotType === "COMPONENT") {
        return "C";
    }
    if (element.slotType === "MACHINE_SELF") {
        return "M";
    }
    if (element.slotType === "FILTERED_ITEM") {
        return "F";
    }
    return "I";
}

function isNumericField(field) {
    return [
        "x",
        "y",
        "width",
        "height",
        "columns",
        "rows",
        "slotSize",
        "slotOffsetX",
        "slotOffsetY",
        "square.x",
        "square.y",
        "square.size"
    ].includes(field);
}

function uniqueId(base, entries) {
    const cleanBase = String(base).replace(/[^a-z0-9_-]+/gi, "_").toLowerCase();
    const ids = new Set(entries.map((entry) => entry.id));
    let candidate = cleanBase;
    let index = 2;
    while (ids.has(candidate)) {
        candidate = `${cleanBase}_${index++}`;
    }
    return candidate;
}

function uniqueInputGroup(base, entries) {
    const cleanBase = String(base).replace(/[^a-z0-9_]+/gi, "_").toLowerCase();
    const groups = new Set(entries.map((entry) => entry.inputGroup).filter(Boolean));
    let index = 1;
    let candidate = `${cleanBase}_${index}`;
    while (groups.has(candidate)) {
        candidate = `${cleanBase}_${++index}`;
    }
    return candidate;
}

function clampInt(value, min, max) {
    return Math.max(min, Math.min(max, Number.isFinite(value) ? Math.trunc(value) : min));
}

function titleCase(value) {
    return String(value).toLowerCase().split(/[_\-\s]+/)
        .map((part) => part ? part[0].toUpperCase() + part.slice(1) : part)
        .join(" ");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
