const state = {
    data: null,
    profileId: null,
    poolTag: "",
    stage: 8,
    rarity: "RARE",
    targets: new Set(),
    sandbox: null,
    selectedAffix: -1,
    activeView: "modifiers",
    rbomTargetId: "rngtech:modular_hammer",
    rbomQuantity: 1,
    rbomRecipeId: null,
    rbomResult: null,
    stagePreviewStage: 8,
    stageCategory: "core",
    stageIssuesOnly: false,
    stageAffixGapsOnly: false,
    stageMinPrefix: 4,
    stageMinSuffix: 4,
    stageSearch: ""
};

const VIEW_PATHS = {
    modifiers: "/",
    rbom: "/rbom",
    stages: "/stages",
    passiveTree: "/passive-trees",
    ascendancies: "/ascendancies",
    gui: "/gui"
};
const PATH_VIEWS = new Map(Object.entries(VIEW_PATHS).map(([view, path]) => [path, view]));
const els = {};
const kineticStats = new Set([
    "PROCESSING_SPEED",
    "INSTANT_PROCESS_CHANCE",
    "OUTPUT_AMOUNT",
    "SUPER_OUTPUT_CHANCE",
    "ENERGY_GENERATION",
    "ENERGY_CAPACITY",
    "PARALLEL_JOBS",
    "CRUSHER_SALVAGE_CHANCE",
    "MINING_SPEED",
    "FE_TRANSFER",
    "BATTERY_SUPPORT",
    "LUCK",
    "TREE_FELL_LIMIT",
    "ORE_BURST_SPEED"
]);
const efficiencyStats = new Set([
    "EFFICIENCY",
    "ENERGY_USAGE",
    "FUEL_EFFICIENCY",
    "IDLE_LOSS",
    "HIGH_HARDNESS_ENERGY_MITIGATION",
    "NO_BATTERY_OUTPUT_RETENTION",
    "DURABILITY",
    "SELF_REPAIR",
    "FE_USAGE",
    "CONTROL"
]);

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindElements();
    state.activeView = activeViewFromPath(window.location.pathname);
    await loadData();
    bindEvents();
    renderAll();
}

function bindElements() {
    for (const id of [
        "buildMeta",
        "refreshData",
        "profileSelect",
        "poolTagSelect",
        "stageInput",
        "raritySelect",
        "iterationInput",
        "poolSummary",
        "poolBody",
        "rollRules",
        "targetStats",
        "runOdds",
        "simulationOutput",
        "rollBase",
        "blankBase",
        "sandboxSummary",
        "sandboxStatus",
        "sandboxBody",
        "modifierView",
        "rbomView",
        "stageView",
        "passiveTreeView",
        "ascendancyView",
        "guiView",
        "rbomTargetSelect",
        "rbomQuantityInput",
        "rbomRecipeSelect",
        "buildRbom",
        "rbomRecipeMeta",
        "rbomSummary",
        "rbomMetrics",
        "rbomDirectBody",
        "rbomTotalsBody",
        "rbomPlanMeta",
        "rbomPlanBody",
        "stagePreviewSelect",
        "stageCategorySelect",
        "stageSearchInput",
        "stageMinPrefixInput",
        "stageMinSuffixInput",
        "stageAffixGapsOnly",
        "stageIssuesOnly",
        "stageMetrics",
        "stageCountMeta",
        "stageBody"
    ]) {
        els[id] = document.getElementById(id);
    }
}

async function loadData() {
    const response = await fetch(`/generated/modifier-data.json?ts=${Date.now()}`);
    state.data = await response.json();
    const firstProfile = sortedProfiles()[0]?.id ?? null;
    state.profileId = state.profileId && state.data.profiles[state.profileId] ? state.profileId : firstProfile;
    state.sandbox = blankTarget();
    state.rbomTargetId = targetExists(state.rbomTargetId) ? state.rbomTargetId : firstRbomTarget();
    state.rbomRecipeId = preferredRecipeId(state.rbomTargetId, state.rbomRecipeId);
    populateProfileSelect();
    populateRbomTargetSelect();
    populateRbomRecipeSelect();
    populateStageFilters();
    els.buildMeta.textContent = `Generated ${new Date(state.data.generatedAt).toLocaleString()} from Java source`;
}

function bindEvents() {
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.addEventListener("click", () => {
            navigateView(button.dataset.view);
            renderView();
        });
    });
    window.addEventListener("popstate", () => {
        state.activeView = activeViewFromPath(window.location.pathname);
        renderView();
    });
    els.refreshData.addEventListener("click", async () => {
        await loadData();
        renderAll();
    });
    els.profileSelect.addEventListener("change", () => {
        state.profileId = els.profileSelect.value;
        state.targets.clear();
        state.selectedAffix = -1;
        state.sandbox = blankTarget();
        renderAll();
    });
    els.poolTagSelect.addEventListener("change", () => {
        state.poolTag = els.poolTagSelect.value;
        renderPool();
    });
    els.stageInput.addEventListener("change", () => {
        state.stage = clampInt(Number(els.stageInput.value), 0, 8);
        els.stageInput.value = state.stage;
        renderAll();
    });
    els.raritySelect.addEventListener("change", () => {
        state.rarity = els.raritySelect.value;
        renderAll();
    });
    els.runOdds.addEventListener("click", runSimulation);
    els.rollBase.addEventListener("click", () => {
        state.sandbox = rollNaturalTarget();
        state.selectedAffix = -1;
        setStatus("Rolled a new target.");
        renderSandbox();
    });
    els.blankBase.addEventListener("click", () => {
        state.sandbox = blankTarget();
        state.selectedAffix = -1;
        setStatus("Created a blank target.");
        renderSandbox();
    });
    document.querySelector(".forge-panel").addEventListener("click", (event) => {
        const action = event.target?.dataset?.action;
        if (action) {
            applyForgeAction(action);
        }
    });
    els.rbomTargetSelect.addEventListener("change", () => {
        state.rbomTargetId = els.rbomTargetSelect.value;
        state.rbomRecipeId = preferredRecipeId(state.rbomTargetId);
        populateRbomRecipeSelect();
        renderRbom();
    });
    els.rbomQuantityInput.addEventListener("change", () => {
        state.rbomQuantity = clampInt(Number(els.rbomQuantityInput.value), 1, 999);
        els.rbomQuantityInput.value = state.rbomQuantity;
        renderRbom();
    });
    els.rbomRecipeSelect.addEventListener("change", () => {
        state.rbomRecipeId = els.rbomRecipeSelect.value;
        renderRbom();
    });
    els.buildRbom.addEventListener("click", renderRbom);
    els.stagePreviewSelect.addEventListener("change", () => {
        state.stagePreviewStage = Number(els.stagePreviewSelect.value);
        renderStages();
    });
    els.stageCategorySelect.addEventListener("change", () => {
        state.stageCategory = els.stageCategorySelect.value;
        renderStages();
    });
    els.stageMinPrefixInput.addEventListener("change", () => {
        state.stageMinPrefix = clampInt(Number(els.stageMinPrefixInput.value), 0, 32);
        els.stageMinPrefixInput.value = state.stageMinPrefix;
        renderStages();
    });
    els.stageMinSuffixInput.addEventListener("change", () => {
        state.stageMinSuffix = clampInt(Number(els.stageMinSuffixInput.value), 0, 32);
        els.stageMinSuffixInput.value = state.stageMinSuffix;
        renderStages();
    });
    els.stageAffixGapsOnly.addEventListener("change", () => {
        state.stageAffixGapsOnly = els.stageAffixGapsOnly.checked;
        renderStages();
    });
    els.stageIssuesOnly.addEventListener("change", () => {
        state.stageIssuesOnly = els.stageIssuesOnly.checked;
        renderStages();
    });
    els.stageSearchInput.addEventListener("input", () => {
        state.stageSearch = els.stageSearchInput.value.trim().toLowerCase();
        renderStages();
    });
}

function populateProfileSelect() {
    els.profileSelect.innerHTML = sortedProfiles()
        .map((profile) => `<option value="${profile.id}">${escapeHtml(profile.label)}</option>`)
        .join("");
    els.profileSelect.value = state.profileId;
}

function populateRbomTargetSelect() {
    const craftableIds = new Set(Object.keys(state.data.rbom?.recipesByOutput ?? {}));
    els.rbomTargetSelect.innerHTML = (state.data.rbom?.items ?? [])
        .filter((item) => craftableIds.has(item.id))
        .map((item) => `<option value="${item.id}">${escapeHtml(item.label)} (${escapeHtml(item.id)})</option>`)
        .join("");
    els.rbomTargetSelect.value = state.rbomTargetId;
    els.rbomQuantityInput.value = state.rbomQuantity;
}

function populateRbomRecipeSelect() {
    const recipes = recipesForOutput(state.rbomTargetId);
    els.rbomRecipeSelect.innerHTML = recipes.map((recipe) => {
        const label = `${recipe.typeLabel} - ${recipe.result.count}x`;
        return `<option value="${recipe.id}">${escapeHtml(label)}</option>`;
    }).join("");
    state.rbomRecipeId = preferredRecipeId(state.rbomTargetId, state.rbomRecipeId);
    els.rbomRecipeSelect.value = state.rbomRecipeId ?? "";
}

function populateStageFilters() {
    const preview = state.data.rbom?.stagePreview;
    els.stagePreviewSelect.innerHTML = (preview?.stages ?? [])
        .map((stage) => `<option value="${stage.stage}">${stage.stage} - ${escapeHtml(stage.name)}</option>`)
        .join("");
    if (!(preview?.stages ?? []).some((stage) => stage.stage === state.stagePreviewStage)) {
        state.stagePreviewStage = preview?.stages?.at(-1)?.stage ?? 0;
    }
    els.stagePreviewSelect.value = state.stagePreviewStage;
    els.stageCategorySelect.innerHTML = (preview?.categories ?? [])
        .map((category) => `<option value="${category.id}">${escapeHtml(category.label)}</option>`)
        .join("");
    if (!(preview?.categories ?? []).some((category) => category.id === state.stageCategory)) {
        state.stageCategory = "core";
    }
    els.stageCategorySelect.value = state.stageCategory;
    els.stageMinPrefixInput.value = state.stageMinPrefix;
    els.stageMinSuffixInput.value = state.stageMinSuffix;
    els.stageAffixGapsOnly.checked = state.stageAffixGapsOnly;
    els.stageIssuesOnly.checked = state.stageIssuesOnly;
    els.stageSearchInput.value = state.stageSearch;
}

function renderAll() {
    renderView();
    renderPool();
    renderOdds();
    renderTargetStats();
    renderSandbox();
    renderRbom();
    renderStages();
}

function renderView() {
    els.modifierView.classList.toggle("hidden", state.activeView !== "modifiers");
    els.rbomView.classList.toggle("hidden", state.activeView !== "rbom");
    els.stageView.classList.toggle("hidden", state.activeView !== "stages");
    els.passiveTreeView.classList.toggle("hidden", state.activeView !== "passiveTree");
    els.ascendancyView.classList.toggle("hidden", state.activeView !== "ascendancies");
    els.guiView.classList.toggle("hidden", state.activeView !== "gui");
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.classList.toggle("active", button.dataset.view === state.activeView);
    });
}

function navigateView(view) {
    state.activeView = view;
    const path = VIEW_PATHS[view] ?? "/";
    if (window.location.pathname !== path) {
        window.history.pushState({}, "", path);
    }
}

function activeViewFromPath(pathname) {
    const cleanPath = String(pathname || "/").replace(/\/+$/, "") || "/";
    return PATH_VIEWS.get(cleanPath) ?? "modifiers";
}

function renderPool() {
    const prof = profile();
    els.poolTagSelect.value = state.poolTag;
    const poolDefinitions = prof.definitions
        .filter((definition) => definition.canRoll)
        .filter((definition) => !state.poolTag || definition.targetable !== false && definition.lensTags?.includes(state.poolTag))
        .toSorted((left, right) =>
            left.slot.localeCompare(right.slot)
            || (left.modGroup ?? "").localeCompare(right.modGroup ?? "")
            || (left.affixLabel ?? left.statLabel).localeCompare(right.affixLabel ?? right.statLabel));
    const prefixCount = poolDefinitions.filter((definition) => definition.slot === "PREFIX").length;
    const suffixCount = poolDefinitions.filter((definition) => definition.slot === "SUFFIX").length;
    const prefixGroups = modGroupCount(poolDefinitions, "PREFIX");
    const suffixGroups = modGroupCount(poolDefinitions, "SUFFIX");
    els.poolSummary.textContent = `${prefixCount} prefix candidates / ${prefixGroups} groups, ${suffixCount} suffix candidates / ${suffixGroups} groups, ${prof.rollableBehaviors.length} behaviors`;
    els.poolBody.innerHTML = poolDefinitions
        .map((definition) => {
            const tiers = tierLabels(definition);
            while (tiers.length < 4) {
                tiers.push("");
            }
            return `
            <tr>
              <td class="${definition.slot === "PREFIX" ? "slot-prefix" : "slot-suffix"}">${titleCase(definition.slot)}</td>
              <td>
                ${escapeHtml(definition.affixLabel ?? definition.statLabel)}
                <span class="pool-group">${escapeHtml(definition.modGroup ?? "ungrouped")}</span>
              </td>
              <td>${escapeHtml(effectText(definition))}</td>
              <td>${escapeHtml(String(definition.rollWeight ?? 100))}</td>
              <td>${tagChips(definition)}</td>
              ${tiers.slice(0, 4).map((label) => `<td>${escapeHtml(label)}</td>`).join("")}
            </tr>
        `;
        })
        .join("");
}

function modGroupCount(definitions, slot) {
    return new Set(definitions
        .filter((definition) => definition.slot === slot)
        .map((definition) => definition.modGroup ?? definition.key)).size;
}

function renderOdds() {
    const rp = refinementPotentialRange(state.stage);
    const tierOdds = adjustedTierOdds(state.rarity, state.stage);
    const tierText = Object.entries(tierOdds)
        .map(([tier, chance]) => `T${tier} ${(chance * 100).toFixed(1)}%`)
        .join(", ");
    els.rollRules.innerHTML = [
        metric("RP", `${rp.min}-${rp.max}`, `Stage ${state.stage}`),
        metric("Natural Max", `T${naturalMaxTier(state.stage)}`, "Tier pressure"),
        metric("Selected Rarity", titleCase(state.rarity), slotText(state.rarity)),
        metric("Tier Odds", tierText || "None", "After overflow checks")
    ].join("");
}

function renderTargetStats() {
    const stats = uniqueStats(profile());
    els.targetStats.innerHTML = stats.map((stat) => {
        const label = statLabel(stat);
        const checked = state.targets.has(stat) ? "checked" : "";
        return `<label><input type="checkbox" value="${stat}" ${checked}>${escapeHtml(label)}</label>`;
    }).join("");
    els.targetStats.querySelectorAll("input").forEach((input) => {
        input.addEventListener("change", () => {
            if (input.checked) {
                state.targets.add(input.value);
            } else {
                state.targets.delete(input.value);
            }
        });
    });
}

function runSimulation() {
    const iterations = clampInt(Number(els.iterationInput.value), 100, 250000);
    els.iterationInput.value = iterations;
    const targets = [...state.targets];
    const counts = { any: 0, all: 0, rare: 0 };
    const tierCounts = { 1: 0, 2: 0, 3: 0, 4: 0 };
    let totalAffixes = 0;
    let totalRp = 0;
    for (let index = 0; index < iterations; index++) {
        const rolled = rollNaturalTarget();
        totalRp += rolled.refinementPotential;
        if (rolled.rarity === "RARE") {
            counts.rare++;
        }
        const rolledStats = new Set(rolled.modifiers.flatMap((modifier) =>
            (modifier.effects?.length ? modifier.effects : [modifier]).map((effect) => effect.stat)));
        if (targets.length > 0 && targets.some((target) => rolledStats.has(target))) {
            counts.any++;
        }
        if (targets.length > 0 && targets.every((target) => rolledStats.has(target))) {
            counts.all++;
        }
        for (const modifier of rolled.modifiers) {
            totalAffixes++;
            tierCounts[modifier.tier]++;
        }
    }
    els.simulationOutput.innerHTML = [
        metric("Any Target", targets.length ? percent(counts.any / iterations) : "Select stats", "At least one selected stat"),
        metric("All Targets", targets.length ? percent(counts.all / iterations) : "Select stats", "Every selected stat"),
        metric("Rare Roll", percent(counts.rare / iterations), "Natural rarity result"),
        metric("Average RP", (totalRp / iterations).toFixed(1), "Natural target"),
        metric("Affix Tier Mix", Object.entries(tierCounts).map(([tier, count]) => `T${tier} ${percent(count / Math.max(1, totalAffixes))}`).join(", "), "Rolled affixes")
    ].join("");
}

function renderSandbox() {
    const target = state.sandbox ?? blankTarget();
    state.sandbox = target;
    els.sandboxSummary.innerHTML = [
        row("Rarity", titleCase(target.rarity)),
        row("Refinement Potential", target.refinementPotential),
        row("Affixes", `${countSlot(target, "PREFIX")}P / ${countSlot(target, "SUFFIX")}S`),
        row("Behaviors", target.behaviors.map((behavior) => behaviorLabel(behavior)).join(", ") || "None")
    ].join("");
    els.sandboxBody.innerHTML = target.modifiers.length
        ? target.modifiers.map((modifier, index) => `
            <tr class="${index === state.selectedAffix ? "selected-affix" : ""}" data-affix-index="${index}">
              <td><input type="radio" name="selectedAffix" ${index === state.selectedAffix ? "checked" : ""}></td>
              <td class="${modifier.slot === "PREFIX" ? "slot-prefix" : "slot-suffix"}">${titleCase(modifier.slot)}</td>
              <td>${escapeHtml(modifier.affixLabel ?? statLabel(modifier.stat))}</td>
              <td>T${modifier.tier}</td>
              <td>${modifierValueText(modifier)}</td>
            </tr>
        `).join("")
        : `<tr><td colspan="5">No rolled affixes.</td></tr>`;
    els.sandboxBody.querySelectorAll("tr[data-affix-index]").forEach((rowElement) => {
        rowElement.addEventListener("click", () => {
            state.selectedAffix = Number(rowElement.dataset.affixIndex);
            renderSandbox();
        });
    });
}

function renderRbom() {
    if (!state.data?.rbom) {
        return;
    }
    const recipe = recipeById(state.rbomRecipeId);
    if (!recipe) {
        els.rbomRecipeMeta.textContent = "No recipe";
        els.rbomSummary.textContent = "";
        els.rbomMetrics.innerHTML = "";
        els.rbomDirectBody.innerHTML = emptyRow(4, "No item-producing recipe found.");
        els.rbomTotalsBody.innerHTML = emptyRow(3, "No totals.");
        els.rbomPlanBody.innerHTML = emptyRow(4, "No plan.");
        return;
    }

    const result = buildRbom(state.rbomTargetId, state.rbomQuantity, state.rbomRecipeId);
    state.rbomResult = result;
    const rootBatches = Math.ceil(state.rbomQuantity / recipe.result.count);
    els.rbomRecipeMeta.textContent = `${recipe.typeLabel}${recipe.metadata.length ? ` - ${recipe.metadata.join(", ")}` : ""}`;
    els.rbomSummary.textContent = `${state.rbomQuantity}x ${itemName(state.rbomTargetId)}`;
    els.rbomMetrics.innerHTML = [
        metric("Craft Steps", result.craftCount, "Whole batches"),
        metric("Raw Lines", result.totals.length, "Consumed"),
        metric("Reusable", result.tools.length, "Setup items"),
        metric("Unresolved Tags", result.totals.filter((entry) => entry.kind === "tag").length, "Choice inputs")
    ].join("");
    renderRbomDirect(recipe, rootBatches);
    renderRbomTotals(result);
    renderRbomPlan(result);
}

function renderRbomDirect(recipe, rootBatches) {
    els.rbomDirectBody.innerHTML = recipe.inputs.map((input) => {
        const quantity = input.reusable ? input.count : input.count * rootBatches;
        return `
            <tr>
              <td>${formatQuantity(quantity)}</td>
              <td>${escapeHtml(inputLabel(input))}<span class="tree-note mono">${escapeHtml(input.id)}</span></td>
              <td>${escapeHtml(inputKindText(input))}</td>
              <td>${escapeHtml(input.note || "")}</td>
            </tr>
        `;
    }).join("") || emptyRow(4, "No direct inputs.");
}

function renderRbomTotals(result) {
    const rows = [
        ...result.totals.map((entry) => ({ ...entry, source: entry.kind === "tag" ? "Choice" : "Consumed" })),
        ...result.tools.map((entry) => ({ ...entry, source: "Reusable setup" }))
    ];
    els.rbomTotalsBody.innerHTML = rows.map((entry) => `
        <tr>
          <td>${formatQuantity(entry.count)}</td>
          <td>${escapeHtml(entry.label)}<span class="tree-note mono">${escapeHtml(entry.id)}</span></td>
          <td>${escapeHtml(entry.source)}</td>
        </tr>
    `).join("") || emptyRow(3, "No raw totals.");
}

function renderRbomPlan(result) {
    const rows = flattenPlan(result.root);
    els.rbomPlanMeta.textContent = `${rows.length} lines`;
    els.rbomPlanBody.innerHTML = rows.map((node) => {
        const indent = `padding-left: ${10 + node.depth * 18}px`;
        if (node.type === "craft") {
            const spare = node.leftover ? `, ${formatQuantity(node.leftover)} spare` : "";
            return `
                <tr>
                  <td>${formatQuantity(node.quantity)}</td>
                  <td class="tree-target" style="${indent}">${escapeHtml(node.label)}<span class="tree-note mono">${escapeHtml(node.id)}</span></td>
                  <td>${escapeHtml(node.recipe.typeLabel)}<span class="tree-note mono">${escapeHtml(node.recipe.id)}</span></td>
                  <td>${node.batches} craft${node.batches === 1 ? "" : "s"} -> ${formatQuantity(node.produced)}${spare}</td>
                </tr>
            `;
        }
        return `
            <tr>
              <td>${formatQuantity(node.quantity)}</td>
              <td class="tree-target" style="${indent}">${escapeHtml(node.label)}<span class="tree-note mono">${escapeHtml(node.id)}</span></td>
              <td>${escapeHtml(node.typeLabel)}</td>
              <td>${escapeHtml(node.note || "")}</td>
            </tr>
        `;
    }).join("") || emptyRow(4, "No plan.");
}

function renderStages() {
    const preview = state.data?.rbom?.stagePreview;
    if (!preview) {
        els.stageMetrics.innerHTML = "";
        els.stageCountMeta.textContent = "No stage data";
        els.stageBody.innerHTML = emptyRow(7, "No stage preview data.");
        return;
    }

    const stage = preview.stages.find((entry) => entry.stage === state.stagePreviewStage);
    const summary = preview.summaryByStage?.[String(state.stagePreviewStage)] ?? {};
    const items = filteredStageItems(preview);
    const issueCount = items.filter((item) => item.issues.length).length;
    const affixGapCount = items.filter((item) => stageAffixGap(item)).length;
    els.stageCountMeta.textContent = `${items.length} visible, ${issueCount} recipe issue${issueCount === 1 ? "" : "s"}, ${affixGapCount} affix gap${affixGapCount === 1 ? "" : "s"}`;
    els.stageMetrics.innerHTML = [
        metric("Stage", stage ? `${stage.stage} - ${stage.name}` : `Stage ${state.stagePreviewStage}`, stage?.role ?? ""),
        metric("Core Items", summary.core ?? 0, "Machines, components, tool parts, targets"),
        metric("Impossible", summary.impossible ?? 0, "Recursive recipe blockers"),
        metric("No Recipe", summary.noRecipe ?? 0, "Registered but no craft"),
        metric("Affix Gaps", affixGapCount, `Needs ${state.stageMinPrefix}P / ${state.stageMinSuffix}S groups`)
    ].join("");
    els.stageBody.innerHTML = items.map((item) => {
        const rowClass = item.issues.length || stageAffixGap(item) ? "stage-row issue-row" : "stage-row";
        return `
            <tr class="${rowClass}">
              <td>${escapeHtml(item.label)}<span class="tree-note mono">${escapeHtml(item.id)}</span></td>
              <td>${stageCategoryPill(item.categoryLabel)}</td>
              <td>${stageModifierCell(item)}</td>
              <td>${stageCraftCell(item)}</td>
              <td>${stageRecipeCell(item)}</td>
              <td>${stageIssueCell(item)}</td>
              <td>${stageInputsCell(item)}</td>
            </tr>
        `;
    }).join("") || emptyRow(7, "No items match the current filters.");
}

function filteredStageItems(preview) {
    return (preview.items ?? [])
        .filter((item) => item.stage === state.stagePreviewStage)
        .filter((item) => stageCategoryMatches(item))
        .filter((item) => !state.stageIssuesOnly || item.issues.length)
        .filter((item) => !state.stageAffixGapsOnly || stageAffixGap(item))
        .filter((item) => {
            if (!state.stageSearch) {
                return true;
            }
            const coverage = item.modifierCoverage;
            const haystack = `${item.label} ${item.id} ${item.categoryLabel} ${coverage?.profileId ?? ""} ${coverage?.profileLabel ?? ""}`.toLowerCase();
            return haystack.includes(state.stageSearch);
        });
}

function stageAffixGap(item) {
    if (!stageAffixAuditable(item)) {
        return false;
    }
    const coverage = item.modifierCoverage;
    return !coverage
        || coverage.prefixGroups < state.stageMinPrefix
        || coverage.suffixGroups < state.stageMinSuffix;
}

function stageAffixAuditable(item) {
    return ["machine", "component", "tool"].includes(item.category);
}

function stageCategoryMatches(item) {
    switch (state.stageCategory) {
        case "all":
            return true;
        case "core":
            return ["machine", "component", "tool", "pseudo"].includes(item.category);
        case "issues":
            return item.issues.length > 0;
        default:
            return item.category === state.stageCategory;
    }
}

function stageModifierCell(item) {
    if (!stageAffixAuditable(item)) {
        return `<span class="muted">Not audited</span>`;
    }
    const coverage = item.modifierCoverage;
    if (!coverage) {
        return `<span class="issue-pill">No profile</span>`;
    }
    const groupText = `${coverage.prefixGroups}P / ${coverage.suffixGroups}S groups`;
    const candidateText = `${coverage.prefixCandidates}P / ${coverage.suffixCandidates}S candidates`;
    const behaviorText = coverage.behaviorCount ? `, ${coverage.behaviorCount} behavior${coverage.behaviorCount === 1 ? "" : "s"}` : "";
    const pillClass = stageAffixGap(item) ? "issue-pill" : "pill pill-ok";
    return [
        `<span class="${pillClass}">${escapeHtml(groupText)}</span>`,
        `<span class="tree-note">${escapeHtml(coverage.profileLabel)} (${escapeHtml(coverage.profileId)})</span>`,
        `<span class="tree-note">${escapeHtml(candidateText + behaviorText)}</span>`
    ].join("");
}

function stageCraftCell(item) {
    if (!item.reachable) {
        return `<span class="pill pill-danger">Impossible</span>`;
    }
    if (item.category === "material" && !item.craftable) {
        return `<span class="pill">Material</span>`;
    }
    if (!item.craftable) {
        return `<span class="pill pill-danger">No recipe</span>`;
    }
    return `<span class="pill pill-ok">Reachable</span><span class="tree-note">${item.recipeIds.length} recipe${item.recipeIds.length === 1 ? "" : "s"}</span>`;
}

function stageRecipeCell(item) {
    if (!item.firstRecipeId) {
        return `<span class="muted">None</span>`;
    }
    const note = item.firstRecipeNote ? `<span class="tree-note">${escapeHtml(item.firstRecipeNote)}</span>` : "";
    return `${escapeHtml(item.firstRecipeType ?? "Recipe")}<span class="tree-note mono">${escapeHtml(item.firstRecipeId)}</span>${note}`;
}

function stageIssueCell(item) {
    if (!item.issues.length) {
        return `<span class="pill pill-ok">Clear</span>`;
    }
    return item.issues.map((issue) => {
        const suggestion = issue.suggestion
            ? `<span class="issue-detail">Suggestion: ${escapeHtml(issue.suggestion)}</span>`
            : "";
        return `<span class="issue-pill" title="${escapeHtml(issue.detail)}">${escapeHtml(issue.label)}</span><span class="issue-detail">${escapeHtml(issue.detail)}</span>${suggestion}`;
    }).join("");
}

function stageInputsCell(item) {
    if (!item.directStageInputs.length) {
        return `<span class="muted">No staged direct inputs</span>`;
    }
    return item.directStageInputs.map((input) => {
        const stageText = input.minStage === input.maxStage ? `S${input.minStage}` : `S${input.minStage}-${input.maxStage}`;
        const material = input.category === "material" ? " material" : "";
        return `<span class="input-chip">${escapeHtml(stageText)} ${escapeHtml(input.label)}<span class="muted">${material}</span></span>`;
    }).join("");
}

function stageCategoryPill(label) {
    return `<span class="pill">${escapeHtml(label)}</span>`;
}

function buildRbom(targetId, quantity, recipeId) {
    const totals = new Map();
    const tools = new Map();
    const counters = { crafts: 0 };
    const root = expandItem(targetId, quantity, recipeId, [], 0, { totals, tools, counters });
    return {
        root,
        craftCount: counters.crafts,
        totals: sortedEntries(totals),
        tools: sortedEntries(tools)
    };
}

function expandItem(itemId, quantity, forcedRecipeId, path, depth, context) {
    if (path.includes(itemId)) {
        const leaf = leafNode("raw", "Cycle stop", itemId, quantity, depth, "Recipe cycle");
        addEntry(context.totals, "item", itemId, itemName(itemId), quantity, false);
        return leaf;
    }

    if (!forcedRecipeId && isCoreResource(itemId)) {
        addEntry(context.totals, "item", itemId, itemName(itemId), quantity, false);
        return leafNode("raw", "Core resource", itemId, quantity, depth, "Terminal RBOM resource");
    }

    const recipe = forcedRecipeId ? recipeById(forcedRecipeId) : preferredRecipeFor(itemId);
    if (!recipe) {
        addEntry(context.totals, "item", itemId, itemName(itemId), quantity, false);
        return leafNode("raw", "Raw item", itemId, quantity, depth, "No recipe");
    }

    const batches = Math.ceil(quantity / recipe.result.count);
    const produced = batches * recipe.result.count;
    const node = {
        type: "craft",
        id: itemId,
        label: itemName(itemId),
        quantity,
        recipe,
        batches,
        produced,
        leftover: produced - quantity,
        depth,
        children: []
    };
    context.counters.crafts++;

    for (const input of recipe.inputs) {
        const needed = input.reusable ? input.count : input.count * batches;
        if (input.reusable) {
            addEntry(context.tools, input.kind, input.id, inputLabel(input), needed, true);
            node.children.push(leafNode("tool", "Reusable", input.id, needed, depth + 1, input.note || inputKindText(input), inputLabel(input)));
            continue;
        }
        if (input.kind === "item") {
            const child = expandItem(input.id, needed, null, [...path, itemId], depth + 1, context);
            if (input.note) {
                child.note = child.note ? `${child.note}; ${input.note}` : input.note;
            }
            node.children.push(child);
            continue;
        }
        if (input.kind === "tag") {
            const resolved = resolveSingleTagItem(input.id);
            if (resolved) {
                const child = expandItem(resolved.id, needed, null, [...path, itemId], depth + 1, context);
                child.note = child.note ? `${child.note}; from #${input.id}` : `from #${input.id}`;
                node.children.push(child);
            } else {
                addEntry(context.totals, "tag", input.id, tagName(input.id), needed, false);
                node.children.push(leafNode("raw", "Tag choice", input.id, needed, depth + 1, tagChoicesText(input.id), tagName(input.id)));
            }
            continue;
        }
        if (input.kind === "fluid" && targetExists(input.id)) {
            const child = expandItem(input.id, needed, null, [...path, itemId], depth + 1, context);
            if (input.note) {
                child.note = child.note ? `${child.note}; ${input.note}` : input.note;
            }
            node.children.push(child);
            continue;
        }
        addEntry(context.totals, input.kind, input.id, inputLabel(input), needed, false);
        node.children.push(leafNode("raw", inputKindText(input), input.id, needed, depth + 1, input.note, inputLabel(input)));
    }
    return node;
}

function leafNode(type, typeLabel, id, quantity, depth, note = "", label = null) {
    return {
        type,
        typeLabel,
        id,
        label: label ?? itemName(id),
        quantity,
        depth,
        note,
        children: []
    };
}

function flattenPlan(node) {
    return [node, ...node.children.flatMap(flattenPlan)];
}

function addEntry(map, kind, id, label, count, maxOnly) {
    const key = `${kind}:${id}`;
    const existing = map.get(key);
    if (existing) {
        existing.count = maxOnly ? Math.max(existing.count, count) : existing.count + count;
    } else {
        map.set(key, { kind, id, label, count });
    }
}

function sortedEntries(map) {
    return [...map.values()].sort((left, right) => left.label.localeCompare(right.label));
}

function applyForgeAction(action) {
    switch (action) {
        case "add-prefix":
            addModifier("PREFIX");
            break;
        case "add-suffix":
            addModifier("SUFFIX");
            break;
        case "kinetic":
            applyLens(kineticStats);
            break;
        case "efficiency":
            applyLens(efficiencyStats);
            break;
        case "ascend":
            ascendRarity();
            break;
        case "chaos":
            chaosReroll();
            break;
        case "expansion":
            expansionFill();
            break;
        case "null":
            nullRemove();
            break;
    }
    renderSandbox();
}

function addModifier(slot, targetStats = []) {
    const target = state.sandbox;
    if (target.refinementPotential <= 0) {
        setStatus("No refinement potential.", true);
        return false;
    }
    const rarity = rarityAfterAdd(target.rarity);
    if (!availableSlots(target, rarity, targetStats).includes(slot)) {
        setStatus(`No open ${titleCase(slot)} slot for that pool.`, true);
        return false;
    }
    const modifier = rollModifier(slot, rarity, target.refinementPotential, target.modifiers, targetStats, target.behaviors);
    if (!modifier) {
        setStatus("No legal modifier available.", true);
        return false;
    }
    const cost = rollSpendableAddCost(target.refinementPotential, modifier.tier);
    target.rarity = rarity;
    target.refinementPotential -= cost;
    target.modifiers.push(modifier);
    setStatus(`Added ${affixName(modifier)} for ${cost} RP.`);
    return true;
}

function applyLens(targetStatsSet) {
    const target = state.sandbox;
    const selected = target.modifiers[state.selectedAffix];
    if (selected && modifierMatchesTargets(selected, targetStatsSet)) {
        upgradeModifier(state.selectedAffix);
        return;
    }
    const targetStats = [...targetStatsSet];
    const slots = availableSlots(target, rarityAfterAdd(target.rarity), targetStats);
    if (slots.length === 0) {
        setStatus("No matching open slot for that lens.", true);
        return;
    }
    addModifier(slots[0], targetStats);
}

function upgradeModifier(index) {
    const target = state.sandbox;
    const current = target.modifiers[index];
    if (!current) {
        setStatus("Select an affix first.", true);
        return false;
    }
    const definition = definitionFor(current);
    if (!definition) {
        setStatus("Selected affix is not in the current pool.", true);
        return false;
    }
    const targetTier = Math.min(4, Math.max(1, current.tier) + 1);
    const minCost = state.data.refinementPotential.addOrUpgrade.minByTier[targetTier];
    if (target.refinementPotential < minCost) {
        setStatus(`Needs at least ${minCost} RP.`, true);
        return false;
    }
    const cost = rollSpendableAddCost(target.refinementPotential, targetTier);
    target.modifiers[index] = rollFromDefinition(definition, targetTier);
    target.refinementPotential -= cost;
    setStatus(`${targetTier === current.tier ? "Tuned" : "Upgraded"} ${affixName(current)} for ${cost} RP.`);
    return true;
}

function ascendRarity() {
    const target = state.sandbox;
    if (target.rarity !== "MAGIC") {
        setStatus("Ascension requires Magic rarity.", true);
        return;
    }
    if (target.refinementPotential <= 0) {
        setStatus("No refinement potential.", true);
        return;
    }
    const attempts = rollSmallCost();
    let added = 0;
    for (let index = 0; index < attempts; index++) {
        const slots = availableSlots(target, "RARE");
        if (slots.length === 0) {
            break;
        }
        const modifier = rollModifier(randomChoice(slots), "RARE", target.refinementPotential, target.modifiers);
        if (!modifier) {
            break;
        }
        target.modifiers.push(modifier);
        added++;
    }
    target.rarity = "RARE";
    target.refinementPotential = 0;
    setStatus(added ? `Ascended and added ${added} affix${added === 1 ? "" : "es"}.` : "No open affix slots.", !added);
}

function chaosReroll() {
    const target = state.sandbox;
    if (target.refinementPotential <= 0) {
        setStatus("No refinement potential.", true);
        return;
    }
    const prefixCount = countSlot(target, "PREFIX");
    const suffixCount = countSlot(target, "SUFFIX");
    if (prefixCount + suffixCount === 0) {
        setStatus("No affixes to reroll.", true);
        return;
    }
    const rerolled = [];
    rollSlotModifiers("PREFIX", prefixCount, target.rarity, target.refinementPotential, rerolled);
    rollSlotModifiers("SUFFIX", suffixCount, target.rarity, target.refinementPotential, rerolled);
    target.modifiers = rerolled;
    const cost = Math.min(rollSmallCost(), target.refinementPotential);
    target.refinementPotential -= cost;
    setStatus(`Rerolled affixes for ${cost} RP.`);
}

function expansionFill() {
    const target = state.sandbox;
    if (target.refinementPotential <= 0) {
        setStatus("No refinement potential.", true);
        return;
    }
    let added = 0;
    while (target.refinementPotential > 0) {
        const rarity = rarityAfterAdd(target.rarity);
        const slots = availableSlots(target, rarity);
        if (slots.length === 0) {
            break;
        }
        const modifier = rollModifier(randomChoice(slots), rarity, target.refinementPotential, target.modifiers);
        if (!modifier) {
            break;
        }
        const cost = rollSpendableAddCost(target.refinementPotential, modifier.tier);
        target.rarity = rarity;
        target.refinementPotential -= cost;
        target.modifiers.push(modifier);
        added++;
    }
    setStatus(added ? `Filled ${added} slot${added === 1 ? "" : "s"}.` : "No open slots.", !added);
}

function nullRemove() {
    const target = state.sandbox;
    if (target.refinementPotential <= 0) {
        setStatus("No refinement potential.", true);
        return;
    }
    if (target.modifiers.length === 0) {
        setStatus("No removable affix.", true);
        return;
    }
    const index = Math.floor(Math.random() * target.modifiers.length);
    const [removed] = target.modifiers.splice(index, 1);
    const cost = Math.min(rollSmallCost(), target.refinementPotential);
    target.refinementPotential -= cost;
    state.selectedAffix = -1;
    setStatus(`Removed ${affixName(removed)} for ${cost} RP.`);
}

function rollNaturalTarget() {
    const rarity = rollNaturalRarity();
    const rpRange = refinementPotentialRange(state.stage);
    const target = {
        rarity,
        refinementPotential: randomInt(rpRange.min, rpRange.max),
        modifiers: [],
        behaviors: []
    };
    const slots = state.data.rarity.affixSlots[rarity];
    rollSlotModifiers("PREFIX", slots.prefix, rarity, target.refinementPotential, target.modifiers);
    let suffixDefinitions = availableDefinitions("SUFFIX", target.modifiers, [], target.behaviors);
    let behaviorPool = availableBehaviors(target.modifiers, target.behaviors);
    for (let index = 0; index < slots.suffix && (suffixDefinitions.length || behaviorPool.length); index++) {
        const selected = Math.floor(Math.random() * (suffixDefinitions.length + behaviorPool.length));
        if (selected < suffixDefinitions.length) {
            const definition = suffixDefinitions.splice(selected, 1)[0];
            target.modifiers.push(rollFromDefinition(definition, rollModifierTier(rarity, state.stage)));
        } else {
            const behavior = behaviorPool.splice(selected - suffixDefinitions.length, 1)[0];
            target.behaviors.push(behavior);
        }
        suffixDefinitions = availableDefinitions("SUFFIX", target.modifiers, [], target.behaviors);
        behaviorPool = availableBehaviors(target.modifiers, target.behaviors);
    }
    return target;
}

function blankTarget() {
    const range = refinementPotentialRange(state.stage);
    return {
        rarity: "NORMAL",
        refinementPotential: range.max,
        modifiers: [],
        behaviors: []
    };
}

function rollSlotModifiers(slot, count, rarity, availablePotential, output) {
    for (let index = 0; index < count; index++) {
        const modifier = rollModifier(slot, rarity, availablePotential, output);
        if (!modifier) {
            return;
        }
        output.push(modifier);
    }
}

function rollModifier(slot, rarity, availablePotential, existingModifiers, targetStats = [], existingBehaviors = []) {
    const definitions = availableDefinitions(slot, existingModifiers, targetStats, existingBehaviors);
    if (definitions.length === 0) {
        return null;
    }
    const tier = affordableTier(rollModifierTier(rarity, state.stage), availablePotential);
    return rollFromDefinition(randomChoice(definitions), tier);
}

function rollFromDefinition(definition, tier) {
    const effects = (definition.effects?.length ? definition.effects : [definition]).map((effect) => {
        const range = effect.tierRanges[Math.max(1, Math.min(4, tier)) - 1];
        return {
            stat: effect.stat,
            operation: effect.operation,
            tierRanges: effect.tierRanges,
            range,
            value: rollRange(range)
        };
    });
    const primary = effects[0];
    return {
        affixId: definition.affixId,
        affixLabel: definition.affixLabel,
        modGroup: definition.modGroup,
        slot: definition.slot,
        stat: primary.stat,
        operation: primary.operation,
        tier,
        range: primary.range,
        value: primary.value,
        effects
    };
}

function availableDefinitions(slot, existingModifiers, targetStats = [], existingBehaviors = state.sandbox?.behaviors ?? []) {
    const targetSet = new Set(targetStats);
    const occupied = occupiedGroups(existingModifiers, existingBehaviors);
    return profile().definitions
        .filter((definition) => definition.canRoll && definition.slot === slot)
        .filter((definition) => targetSet.size === 0 || definitionMatchesTargets(definition, targetSet))
        .filter((definition) => !occupied.has(definition.modGroup))
        .filter((definition) => !existingModifiers.some((modifier) => conflictsWith(definition, modifier)));
}

function availableBehaviors(existingModifiers, existingBehaviors = []) {
    const occupied = occupiedGroups(existingModifiers, existingBehaviors);
    return profile().rollableBehaviors
        .filter((entry) => !occupied.has(entry.modGroup))
        .map((entry) => entry.behavior);
}

function availableSlots(target, rarity, targetStats = []) {
    const limit = state.data.rarity.affixSlots[rarity];
    const slots = [];
    if (countSlot(target, "PREFIX") < limit.prefix && availableDefinitions("PREFIX", target.modifiers, targetStats, target.behaviors).length) {
        slots.push("PREFIX");
    }
    if (countSlot(target, "SUFFIX") < limit.suffix && availableDefinitions("SUFFIX", target.modifiers, targetStats, target.behaviors).length) {
        slots.push("SUFFIX");
    }
    return slots;
}

function definitionFor(modifier) {
    return profile().definitions.find((definition) => definition.canRoll && sameDefinition(definition, modifier));
}

function sameDefinition(definition, modifier) {
    if (definition.slot !== modifier.slot) {
        return false;
    }
    if (definition.affixId && modifier.affixId) {
        return definition.affixId === modifier.affixId;
    }
    return modifier.stat === definition.stat && modifier.operation === definition.operation;
}

function conflictsWith(definition, modifier) {
    return sameDefinition(definition, modifier)
        || (definition.modGroup && modifier.modGroup && definition.modGroup === modifier.modGroup)
        || (!modifier.affixId && modifier.stat === definition.stat && modifier.operation === definition.operation);
}

function occupiedGroups(modifiers, behaviors = []) {
    const groups = new Set();
    for (const modifier of modifiers) {
        if (modifier.modGroup) {
            groups.add(modifier.modGroup);
        }
    }
    for (const behavior of behaviors) {
        const group = behaviorModGroup(behavior);
        if (group) {
            groups.add(group);
        }
    }
    return groups;
}

function behaviorModGroup(behavior) {
    return profile().rollableBehaviors.find((entry) => entry.behavior === behavior)?.modGroup ?? behavior.toLowerCase();
}

function definitionMatchesTargets(definition, targetSet) {
    return (definition.effects?.length ? definition.effects : [definition]).some((effect) => targetSet.has(effect.stat));
}

function modifierMatchesTargets(modifier, targetSet) {
    return (modifier.effects?.length ? modifier.effects : [modifier]).some((effect) => targetSet.has(effect.stat));
}

function rollNaturalRarity() {
    const roll = Math.random();
    if (roll < 0.5) {
        return "NORMAL";
    }
    if (roll < 0.9) {
        return "MAGIC";
    }
    return "RARE";
}

function rollModifierTier(rarity, stage) {
    let tier = weighted(state.data.tiers.baseOdds[rarity]).tier;
    const natural = naturalMaxTier(stage);
    while (tier > natural && Math.random() >= overflowKeepChance(tier - natural)) {
        tier--;
    }
    return tier;
}

function adjustedTierOdds(rarity, stage) {
    const result = {};
    for (const entry of state.data.tiers.baseOdds[rarity]) {
        distributeTier(entry.tier, entry.chance, naturalMaxTier(stage), result);
    }
    return result;
}

function distributeTier(tier, chance, natural, result) {
    if (tier <= natural) {
        result[tier] = (result[tier] ?? 0) + chance;
        return;
    }
    const keep = overflowKeepChance(tier - natural);
    result[tier] = (result[tier] ?? 0) + chance * keep;
    distributeTier(tier - 1, chance * (1 - keep), natural, result);
}

function naturalMaxTier(stage) {
    if (stage <= 2) {
        return 1;
    }
    if (stage <= 4) {
        return 2;
    }
    if (stage <= 6) {
        return 3;
    }
    return 4;
}

function overflowKeepChance(overflow) {
    if (overflow <= 1) {
        return 0.35;
    }
    if (overflow === 2) {
        return 0.15;
    }
    return 0.05;
}

function refinementPotentialRange(stage) {
    if (stage <= 0) {
        return { min: 0, max: 2 };
    }
    if (stage <= 2) {
        return { min: 4, max: 8 };
    }
    if (stage <= 4) {
        return { min: 8, max: 14 };
    }
    if (stage <= 6) {
        return { min: 14, max: 22 };
    }
    return { min: 22, max: 32 };
}

function affordableTier(tier, potential) {
    let affordable = Math.max(1, Math.min(4, tier));
    while (affordable > 1 && state.data.refinementPotential.addOrUpgrade.minByTier[affordable] > potential) {
        affordable--;
    }
    return affordable;
}

function rollSpendableAddCost(potential, tier) {
    const min = state.data.refinementPotential.addOrUpgrade.minByTier[tier];
    return Math.min(randomInt(min, state.data.refinementPotential.addOrUpgrade.max), potential);
}

function rollSmallCost() {
    return weighted(state.data.refinementPotential.smallCostOdds).cost;
}

function rarityAfterAdd(rarity) {
    return rarity === "NORMAL" ? "MAGIC" : rarity;
}

function targetExists(itemId) {
    return Boolean(state.data?.rbom?.recipesByOutput?.[itemId]);
}

function firstRbomTarget() {
    return Object.keys(state.data?.rbom?.recipesByOutput ?? {})[0] ?? "";
}

function recipesForOutput(itemId) {
    return (state.data?.rbom?.recipesByOutput?.[itemId] ?? [])
        .map(recipeById)
        .filter(Boolean)
        .toSorted((left, right) => recipeChoiceRank(left) - recipeChoiceRank(right));
}

function recipeById(recipeId) {
    return state.data?.rbom?.recipes?.find((recipe) => recipe.id === recipeId) ?? null;
}

function preferredRecipeId(itemId, currentRecipeId = null) {
    const recipes = recipesForOutput(itemId);
    if (currentRecipeId && recipes.some((recipe) => recipe.id === currentRecipeId)) {
        return currentRecipeId;
    }
    return recipes[0]?.id ?? null;
}

function preferredRecipeFor(itemId) {
    return recipeById(preferredRecipeId(itemId));
}

function recipeChoiceRank(recipe) {
    if (recipe.type === "rngtech:alloy_furnace" && recipe.id.endsWith("_from_ingots")) {
        return 0;
    }
    if (recipe.type === "rngtech:alloy_furnace" && recipe.id.endsWith("_from_dusts")) {
        return 1;
    }
    if (!isCoreResource(recipe.result.id) && recipe.type === "rngtech:furnace" && recipe.id.includes("/alloy_blends/")) {
        return 2;
    }
    if (!isCoreResource(recipe.result.id)
            && recipe.type === "rngtech:furnace"
            && recipe.id.includes("/metals/")
            && recipe.id.endsWith("_from_dust")) {
        return 3;
    }
    return 10;
}

function isCoreResource(itemId) {
    return (state.data?.rbom?.coreResourceIds ?? []).includes(itemId);
}

function resolveSingleTagItem(tagId, seen = new Set()) {
    if (seen.has(tagId)) {
        return null;
    }
    seen.add(tagId);
    const values = state.data.rbom.tags[tagId]?.values ?? [];
    const required = values.filter((value) => value.required !== false);
    if (required.length !== 1) {
        return null;
    }
    const [value] = required;
    if (value.kind === "item") {
        return value;
    }
    if (value.kind === "tag") {
        return resolveSingleTagItem(value.id, seen);
    }
    return null;
}

function itemName(itemId) {
    return state.data.rbom.items.find((item) => item.id === itemId)?.label ?? titleCase(itemId.split(":").pop());
}

function tagName(tagId) {
    return state.data.rbom.tags[tagId]?.label ?? titleCase(tagId.split(":").pop());
}

function inputLabel(input) {
    if (input.kind === "tag") {
        return tagName(input.id);
    }
    if (input.kind === "item") {
        return itemName(input.id);
    }
    return input.label ?? titleCase(input.id.split(":").pop());
}

function inputKindText(input) {
    if (input.reusable) {
        return "Reusable";
    }
    if (input.kind === "tag") {
        return "Tag";
    }
    if (input.kind === "fluid") {
        return "Fluid";
    }
    return "Item";
}

function tagChoicesText(tagId) {
    const values = state.data.rbom.tags[tagId]?.values ?? [];
    const choices = values.slice(0, 4).map((value) => value.id);
    const suffix = values.length > choices.length ? ` +${values.length - choices.length}` : "";
    return choices.length ? `Choices: ${choices.join(", ")}${suffix}` : "Unresolved tag";
}

function profile() {
    return state.data.profiles[state.profileId];
}

function sortedProfiles() {
    return Object.values(state.data.profiles).sort((a, b) => {
        const byLabel = a.label.localeCompare(b.label, undefined, { sensitivity: "base" });
        return byLabel || a.id.localeCompare(b.id);
    });
}

function uniqueStats(prof) {
    return [...new Set(prof.definitions
        .filter((definition) => definition.canRoll)
        .flatMap((definition) => (definition.effects?.length ? definition.effects : [definition]).map((effect) => effect.stat)))]
        .sort((a, b) => statLabel(a).localeCompare(statLabel(b)));
}

function statLabel(stat) {
    const key = `rngtech.stat.${stat.toLowerCase()}`;
    return state.data.labels[key] ?? titleCase(stat);
}

function shortStatLabel(stat) {
    const key = `rngtech.stat.short.${stat.toLowerCase()}`;
    return state.data.labels[key] ?? statLabel(stat);
}

function behaviorLabel(behavior) {
    const key = `rngtech.behavior.${behavior.toLowerCase()}`;
    return state.data.labels[key] ?? titleCase(behavior);
}

function affixName(modifier) {
    return modifier.affixLabel ?? statLabel(modifier.stat);
}

function countSlot(target, slot) {
    return target.modifiers.filter((modifier) => modifier.slot === slot).length;
}

function rollRange(range) {
    if (range.wholeNumber) {
        return randomInt(range.min, range.max);
    }
    return range.min + Math.random() * (range.max - range.min);
}

function weighted(entries) {
    const roll = Math.random();
    let cumulative = 0;
    for (const entry of entries) {
        cumulative += entry.chance;
        if (roll <= cumulative) {
            return entry;
        }
    }
    return entries[entries.length - 1];
}

function randomChoice(values) {
    return values[Math.floor(Math.random() * values.length)];
}

function randomInt(min, max) {
    return min + Math.floor(Math.random() * (max - min + 1));
}

function clampInt(value, min, max) {
    return Math.max(min, Math.min(max, Number.isFinite(value) ? Math.trunc(value) : min));
}

function row(label, value) {
    return `<div class="sandbox-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(String(value))}</strong></div>`;
}

function metric(label, value, detail) {
    return `<div class="metric"><strong>${escapeHtml(String(value))}</strong><span>${escapeHtml(label)}${detail ? ` - ${escapeHtml(detail)}` : ""}</span></div>`;
}

function rangeLabel(range, operation) {
    if (operation === "ADD") {
        return `${range.min}-${range.max}`;
    }
    return `${range.min}-${range.max}%`;
}

function tierLabels(definition) {
    if (definition.behaviorOnly) {
        return ["Single", "", "", ""];
    }
    const effects = definition.effects?.length ? definition.effects : [definition];
    const tierCount = Math.max(...effects.map((effect) => effect.tierRanges?.length ?? 0), definition.tierRanges?.length ?? 0);
    return Array.from({ length: tierCount }, (_, index) => effects
        .map((effect) => effectTierLabel(effect, index, effects.length > 1))
        .filter(Boolean)
        .join(" + "));
}

function effectTierLabel(effect, index, includeStat) {
    const range = effect.tierRanges?.[index];
    if (!range) {
        return "";
    }
    const value = rangeLabel(range, effect.operation);
    return includeStat ? `${value} ${shortStatLabel(effect.stat)}` : value;
}

function effectText(definition) {
    if (definition.behaviorOnly) {
        return "Behavior";
    }
    const effects = definition.effects?.length ? definition.effects : [definition];
    return effects
        .map((effect) => `${operationText(effect.operation)} ${statLabel(effect.stat)}`)
        .join(" + ");
}

function tagChips(definition) {
    if (definition.behaviorOnly) {
        return `<span class="tag-muted">No (behavior)</span>`;
    }
    if (definition.targetable === false) {
        return `<span class="tag-muted">No</span>`;
    }
    if (definition.lensTags?.length) {
        return definition.lensTags
            .map((tag) => `<span class="tag-chip">${escapeHtml(titleCase(tag))}</span>`)
            .join("");
    }
    return `<span class="tag-muted">None</span>`;
}

function modifierValueText(modifier) {
    const effects = modifier.effects?.length ? modifier.effects : [modifier];
    return effects.map((effect) => singleModifierValueText(effect)).join(" + ");
}

function singleModifierValueText(modifier) {
    if (modifier.operation === "ADD") {
        return `+${formatNumber(modifier.value)} ${statLabel(modifier.stat)}`;
    }
    if (modifier.operation === "DECREASED_PERCENT") {
        return `${formatNumber(modifier.value)}% decreased ${statLabel(modifier.stat)}`;
    }
    return `+${formatNumber(modifier.value)}% ${statLabel(modifier.stat)}`;
}

function operationText(operation) {
    if (operation === "DECREASED_PERCENT") {
        return "Decreased";
    }
    if (operation === "INCREASED_PERCENT") {
        return "Increased";
    }
    return titleCase(operation);
}

function slotText(rarity) {
    const slots = state.data.rarity.affixSlots[rarity];
    return `${slots.prefix} prefix / ${slots.suffix} suffix`;
}

function formatNumber(value) {
    return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function formatQuantity(value) {
    return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function percent(value) {
    return `${(value * 100).toFixed(2)}%`;
}

function emptyRow(colspan, message) {
    return `<tr><td colspan="${colspan}">${escapeHtml(message)}</td></tr>`;
}

function setStatus(message, error = false) {
    els.sandboxStatus.textContent = message;
    els.sandboxStatus.classList.toggle("bad", error);
}

function titleCase(value) {
    return String(value).toLowerCase().split("_")
        .map((part) => part ? part[0].toUpperCase() + part.slice(1) : part)
        .join(" ");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
