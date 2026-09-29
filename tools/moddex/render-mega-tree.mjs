import { readFile, writeFile } from "node:fs/promises";
import { megaCatalogUrl, diameters, LEVEL_POINTS } from "./check-mega-tree.mjs";

const tree = JSON.parse(await readFile(megaCatalogUrl, "utf8"));
const lookup = new Map(tree.nodes.map(node => [node.id, node]));
const escape = value => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll('"', "&quot;");
const colors = { CONTROL: "#72bda7", DRIVE: "#e6ab73", RESERVE: "#829bdb" };
// Frame the catalog bounds with room for the title above and the legend below.
const xs = tree.nodes.map(node => node.x), ys = tree.nodes.map(node => node.y);
const left = Math.min(...xs) - 160, right = Math.max(...xs) + 160;
const top = Math.min(...ys) - 420, bottom = Math.max(...ys) + 300;
const width = right - left, height = bottom - top;
const summary = `${tree.nodes.length} nodes · 6 starts · 3 attributes · ${LEVEL_POINTS} points at level 100`;
const svg = [
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${left} ${top} ${width} ${height}" width="1200" height="${Math.round(1200 * height / width)}" role="img" aria-labelledby="title description">`,
    '<title id="title">RNGTech shared machine passive tree</title>',
    `<desc id="description">${tree.nodes.length} nodes, six starts, ${tree.links.length} links. Control in green, Drive in copper, Reserve in blue. White notables and violet keystones. Each machine earns ${LEVEL_POINTS} points by level 100.</desc>`,
    `<rect x="${left}" y="${top}" width="${width}" height="${height}" fill="#111820"/>`,
    `<g font-family="sans-serif" fill="#e5ecf3"><text x="${left + 80}" y="${top + 150}" font-size="96">Machine Mastery</text><text x="${left + 85}" y="${top + 270}" font-size="58" fill="#a9b9c9">${summary}</text></g>`,
    '<g stroke="#66727d" stroke-width="5" opacity="0.65">',
    ...tree.links.map(([first, second]) => { const a = lookup.get(first), b = lookup.get(second); return `<path d="M${a.x},${a.y} L${b.x},${b.y}"/>`; }),
    '</g>',
    ...tree.nodes.map(node => {
        const color = node.kind === "KEYSTONE" ? "#c79dea" : node.kind === "NOTABLE" ? "#f1dcc1" : colors[node.effects[0]?.stat] ?? "#9fadb9";
        const radius = diameters[node.kind] / 2;
        return `<circle cx="${node.x}" cy="${node.y}" r="${radius}" fill="${color}" stroke="#111820" stroke-width="3"><title>${escape(node.name)} (${escape(node.id)})</title></circle>`;
    }),
    '<g font-family="sans-serif" fill="#e5ecf3" font-size="44" text-anchor="middle">',
    ...tree.nodes.filter(node => node.kind === "STARTER").map(node => `<text x="${node.x}" y="${node.y - 45}">${escape(node.name)}</text>`),
    '</g><g font-family="sans-serif" font-size="60">',
    ...Object.entries(colors).map(([name, color], i) => {
        const x = left + width * (0.15 + i * 0.3);
        return `<circle cx="${x}" cy="${bottom - 120}" r="28" fill="${color}"/><text x="${x + 65}" y="${bottom - 100}" fill="${color}">${name}</text>`;
    }),
    '</g></svg>'
];
await writeFile(new URL("../../docs/assets/machine-mega-passive-tree.svg", import.meta.url), svg.join("\n") + "\n");
console.log("Rendered shared mastery overview from runtime catalog");
