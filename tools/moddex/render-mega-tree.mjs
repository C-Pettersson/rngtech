import { readFile, writeFile } from "node:fs/promises";
import { megaCatalogUrl, diameters } from "./check-mega-tree.mjs";

const tree = JSON.parse(await readFile(megaCatalogUrl, "utf8"));
const lookup = new Map(tree.nodes.map(node => [node.id, node]));
const escape = value => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll('"', "&quot;");
const colors = { CONTROL: "#72bda7", DRIVE: "#e6ab73", RESERVE: "#829bdb" };
const svg = [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6000 6250" width="1200" height="1250" role="img" aria-labelledby="title description">',
    '<title id="title">RNGTech shared machine passive tree</title>',
    `<desc id="description">750 nodes, six starts, ${tree.links.length} links. Control in green, Drive in copper, Reserve in blue. White notables and violet keystones. Each machine has 79 points at level 80.</desc>`,
    '<rect width="6000" height="6250" fill="#111820"/>',
    '<g font-family="sans-serif" fill="#e5ecf3"><text x="240" y="230" font-size="96">Machine Mastery</text><text x="245" y="365" font-size="58" fill="#a9b9c9">750 nodes · 6 starts · 3 attributes · 79 points</text></g>',
    '<g stroke="#66727d" stroke-width="5" opacity="0.65">',
    ...tree.links.map(([first, second]) => { const a=lookup.get(first), b=lookup.get(second); return `<path d="M${a.x},${a.y} L${b.x},${b.y}"/>`; }),
    '</g>',
    ...tree.nodes.map(node => {
        const color = node.kind === "KEYSTONE" ? "#c79dea" : node.kind === "NOTABLE" ? "#f1dcc1" : colors[node.effects[0]?.stat] ?? "#9fadb9";
        const radius = diameters[node.kind] / 2;
        return `<circle cx="${node.x}" cy="${node.y}" r="${radius}" fill="${color}" stroke="#111820" stroke-width="3"><title>${escape(node.name)} (${escape(node.id)})</title></circle>`;
    }),
    '<g font-family="sans-serif" fill="#e5ecf3" font-size="44" text-anchor="middle">',
    ...tree.nodes.filter(node=>node.kind === "STARTER").map(node => `<text x="${node.x}" y="${node.y-45}">${escape(node.name)}</text>`),
    '</g><g font-family="sans-serif" font-size="60">',
    ...Object.entries(colors).map(([name,color],i)=>`<circle cx="${900+i*1700}" cy="5980" r="28" fill="${color}"/><text x="${965+i*1700}" y="6000" fill="${color}">${name}</text>`),
    '</g></svg>'
];
await writeFile(new URL("../../docs/assets/machine-mega-passive-tree.svg", import.meta.url), svg.join("\n") + "\n");
console.log("Rendered shared mastery overview from runtime catalog");
