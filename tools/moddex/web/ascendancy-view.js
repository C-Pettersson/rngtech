const SVG_WIDTH = 460;
const SVG_HEIGHT = 316;
const COLUMN_GAP = 88;
const ROW_GAP = 58;
const KIND_LABELS = { ROOT: "Root", SMALL: "Small", NOTABLE: "Notable", DEEP: "Deep notable" };

const view = {
    data: null,
    familyId: "crusher",
    search: "",
    selected: null
};
const els = {};

document.addEventListener("DOMContentLoaded", initAscendancyView);

async function initAscendancyView() {
    for (const id of ["ascendancyFamilySelect", "ascendancySearchInput", "ascendancyMetrics", "ascendancyCards", "ascendancyNodeDetail",
        "ascendancyDeclarationBody", "ascendancyDeclarationMeta", "refreshData"]) {
        els[id] = document.getElementById(id);
    }
    if (!els.ascendancyCards) {
        return;
    }
    els.ascendancyFamilySelect.addEventListener("change", () => {
        view.familyId = els.ascendancyFamilySelect.value;
        view.selected = null;
        renderAscendancies();
    });
    els.ascendancySearchInput.addEventListener("input", () => {
        view.search = els.ascendancySearchInput.value.trim().toLowerCase();
        renderAscendancies();
    });
    els.ascendancyCards.addEventListener("click", (event) => {
        const target = event.target.closest("[data-node]");
        if (target) {
            view.selected = { ascendancy: target.dataset.ascendancy, node: target.dataset.node };
            renderAscendancies();
        }
    });
    els.refreshData?.addEventListener("click", loadAscendancyData);
    await loadAscendancyData();
}

async function loadAscendancyData() {
    const response = await fetch(`/generated/ascendancies.json?ts=${Date.now()}`);
    view.data = await response.json();
    els.ascendancyFamilySelect.innerHTML = view.data.families
        .map((family) => `<option value="${family.id}">${escapeHtml(family.label)} (${family.ascendancies.length})</option>`)
        .join("");
    if (!view.data.families.some((family) => family.id === view.familyId)) {
        view.familyId = view.data.families[0]?.id ?? "";
    }
    els.ascendancyFamilySelect.value = view.familyId;
    renderAscendancies();
}

function renderAscendancies() {
    const family = view.data?.families.find((entry) => entry.id === view.familyId);
    if (!family) {
        return;
    }
    const declared = [
        ...view.data.stats.map((entry) => ({ ...entry, kind: "Stat" })),
        ...view.data.behaviors.map((entry) => ({ ...entry, kind: "Behavior" }))
    ].filter((entry) => entry.families.includes(family.id));
    const nodes = family.ascendancies.flatMap((ascendancy) => ascendancy.nodes);
    const yields = declared.filter((entry) => entry.yield !== "none");
    els.ascendancyMetrics.innerHTML = [
        ["Ascendancies", family.ascendancies.length],
        ["Nodes", nodes.length],
        ["Deep notables", nodes.filter((node) => node.kind === "DEEP").length],
        ["Declared", declared.length],
        ["Yield entries", yields.length],
        ["Uncovered yields", yields.filter((entry) => !entry.coveredBy.length).length]
    ].map(([label, value]) => `<div class="metric"><strong>${value}</strong><span>${label}</span></div>`).join("");
    els.ascendancyCards.innerHTML = family.ascendancies.map(ascendancyCard).join("")
        || `<section class="panel"><p class="status-line">${escapeHtml(family.label)} has no ascendancies.</p></section>`;
    renderNodeDetail(family);
    els.ascendancyDeclarationMeta.textContent = `${declared.length} declared for ${family.label}`;
    els.ascendancyDeclarationBody.innerHTML = declared.map((entry) => `
        <tr>
          <td><code>${entry.id}</code></td>
          <td>${entry.kind}</td>
          <td class="wrap">${escapeHtml(entry.name ? `${entry.name}: ${entry.description}` : entry.description)}</td>
          <td class="${entry.yield === "none" ? "" : "slot-suffix"}">${entry.yield}</td>
          <td class="${entry.yield !== "none" && !entry.coveredBy.length ? "ascendancy-uncovered" : ""}">${entry.coveredBy.join(", ") || (entry.yield === "none" ? "" : "missing")}</td>
        </tr>`).join("");
}

function ascendancyCard(ascendancy) {
    const matches = new Set(ascendancy.nodes.filter(nodeMatches).map((node) => node.id));
    const rows = ascendancy.nodes
        .filter((node) => !view.search || matches.has(node.id))
        .map((node) => `
          <tr class="${isSelected(ascendancy, node) ? "selected" : ""}" data-ascendancy="${ascendancy.id}" data-node="${node.id}">
            <td>${node.kind === "SMALL" ? escapeHtml(node.name) : `<strong>${escapeHtml(node.name)}</strong>`}</td>
            <td>${KIND_LABELS[node.kind]}</td>
            <td class="wrap">${escapeHtml(summary(node))}</td>
          </tr>`)
        .join("");
    return `
      <section class="panel ascendancy-card">
        <div class="panel-heading">
          <h2>${escapeHtml(ascendancy.name)}</h2>
          <span>${ascendancy.nodes.length} nodes · <code>${ascendancy.id}</code></span>
        </div>
        ${treeSvg(ascendancy, matches)}
        <div class="table-wrap ascendancy-node-table">
          <table>
            <thead><tr><th>Node</th><th>Type</th><th>Effect</th></tr></thead>
            <tbody>${rows || `<tr><td colspan="3" class="status-line">No matching nodes.</td></tr>`}</tbody>
          </table>
        </div>
      </section>`;
}

function treeSvg(ascendancy, matches) {
    const position = new Map(ascendancy.nodes.map((node) => [node.id, {
        x: SVG_WIDTH / 2 + node.x * COLUMN_GAP,
        y: 44 + node.y * ROW_GAP
    }]));
    const links = ascendancy.nodes.filter((node) => node.parent).map((node) => {
        const from = position.get(node.parent);
        const to = position.get(node.id);
        return `<line x1="${from.x}" y1="${from.y}" x2="${to.x}" y2="${to.y}" class="ascendancy-link"></line>`;
    }).join("");
    const shapes = ascendancy.nodes.map((node) => {
        const { x, y } = position.get(node.id);
        const classes = ["ascendancy-node", `kind-${node.kind.toLowerCase()}`];
        if (view.search && matches.has(node.id)) {
            classes.push("match");
        }
        if (isSelected(ascendancy, node)) {
            classes.push("selected");
        }
        const labelY = node.kind === "ROOT" ? y - 22 : y + 26;
        const label = node.kind === "SMALL" ? "" : `<text x="${x}" y="${labelY}" class="ascendancy-label">${escapeHtml(shortName(node.name))}</text>`;
        return `<g class="${classes.join(" ")}" data-ascendancy="${ascendancy.id}" data-node="${node.id}">
          <title>${escapeHtml(`${node.name} (${KIND_LABELS[node.kind]})\n${summary(node)}`)}</title>
          ${nodeShape(node.kind, x, y)}${label}
        </g>`;
    }).join("");
    return `<svg class="ascendancy-tree" viewBox="0 0 ${SVG_WIDTH} ${SVG_HEIGHT}" role="img" aria-label="${escapeHtml(ascendancy.name)} tree">${links}${shapes}</svg>`;
}

function nodeShape(kind, x, y) {
    switch (kind) {
        case "ROOT":
            return `<circle cx="${x}" cy="${y}" r="15"></circle>`;
        case "SMALL":
            return `<circle cx="${x}" cy="${y}" r="7"></circle>`;
        case "DEEP":
            return `<rect x="${x - 11}" y="${y - 11}" width="22" height="22" transform="rotate(45 ${x} ${y})"></rect>`;
        default:
            return `<rect x="${x - 12}" y="${y - 12}" width="24" height="24" rx="5"></rect>`;
    }
}

function renderNodeDetail(family) {
    const ascendancy = family.ascendancies.find((entry) => entry.id === view.selected?.ascendancy);
    const node = ascendancy?.nodes.find((entry) => entry.id === view.selected?.node);
    if (!node) {
        els.ascendancyNodeDetail.innerHTML = `<p class="status-line">Select a node to see its effects, parent, and the stats and behaviors it uses.</p>`;
        return;
    }
    const parent = ascendancy.nodes.find((entry) => entry.id === node.parent);
    const lines = [...node.effects, ...node.behaviors.map((behavior) => `${behavior.text} (${behavior.id})`)];
    els.ascendancyNodeDetail.innerHTML = `
      <h2>${escapeHtml(node.name)}</h2>
      <p class="status-line">${KIND_LABELS[node.kind]} in ${escapeHtml(ascendancy.name)} · <code>${node.id}</code>${parent ? ` · after ${escapeHtml(parent.name)}` : ""}</p>
      <ul class="ascendancy-effects">${lines.map((line) => `<li>${escapeHtml(line)}</li>`).join("")}</ul>
      ${node.stats.length ? `<p class="status-line">Stats: ${node.stats.map((stat) => `<code>${stat}</code>`).join(" ")}</p>` : ""}`;
}

function summary(node) {
    return [...node.effects, ...node.behaviors.map((behavior) => behavior.text)].join(" · ");
}

function nodeMatches(node) {
    if (!view.search) {
        return false;
    }
    return [node.name, node.id, ...node.effects, ...node.behaviors.flatMap((behavior) => [behavior.id, behavior.text])]
        .some((text) => text.toLowerCase().includes(view.search));
}

function isSelected(ascendancy, node) {
    return view.selected?.ascendancy === ascendancy.id && view.selected?.node === node.id;
}

function shortName(name) {
    return name.length > 15 ? `${name.slice(0, 14)}…` : name;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}
