import { mkdir, readFile, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "../..");
const PROGRESSION_ROOT = path.join(PROJECT_ROOT, "src/main/java/com/rngtech/rpg/progression");
const GENERATED_ROOT = path.join(__dirname, "generated");
const OUTPUT_PATH = path.join(GENERATED_ROOT, "passive-trees.json");
const ORBIT_RADII = [0, 48, 86, 126, 168];
const ORBIT_ANGLES = [
    [0],
    [0, 60, 120, 180, 240, 300],
    [0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330],
    [0, 30, 45, 60, 90, 120, 135, 150, 180, 210, 225, 240, 270, 300, 315, 330],
    [0, 10, 20, 30, 40, 45, 50, 60, 70, 80, 90, 100, 110, 120, 130, 135, 140, 150, 160, 170,
        180, 190, 200, 210, 220, 225, 230, 240, 250, 260, 270, 280, 290, 300, 310, 315, 320, 330, 340, 350]
];

export async function buildPassiveTreeData() {
    const trees = [];
    const files = await readdir(PROGRESSION_ROOT);
    for (const fileName of files.filter((entry) => entry.endsWith("PassiveTreeLayout.java")).sort()) {
        const tree = await parseTypedTree(fileName);
        if (tree) {
            trees.push(tree);
        }
    }
    const result = {
        generatedAt: new Date().toISOString(),
        trees
    };
    await mkdir(GENERATED_ROOT, { recursive: true });
    await writeFile(OUTPUT_PATH, `${JSON.stringify(result, null, 2)}\n`, "utf8");
    return result;
}

async function parseTypedTree(layoutFileName) {
    const layoutSource = await readFile(path.join(PROGRESSION_ROOT, layoutFileName), "utf8");
    if (!layoutSource.includes("List<GroupSeed>") || !layoutSource.includes("Map<String, Placement>")) {
        return null;
    }
    const baseName = layoutFileName.slice(0, -"PassiveTreeLayout.java".length);
    const definitionPath = path.join(PROGRESSION_ROOT, `${baseName}PassiveTreeDefinition.java`);
    const nodePath = path.join(PROGRESSION_ROOT, `${baseName}PassiveNode.java`);
    const [definitionSource, nodeSource] = await Promise.all([
        readFile(definitionPath, "utf8"),
        readFile(nodePath, "utf8")
    ]);
    const centers = parseGroupCenters(layoutSource);
    const placements = parsePlacements(layoutSource, centers);
    if (!centers.size || !placements.size) {
        return null;
    }
    const nodeMetadata = parseNodeMetadata(nodeSource, placements.keys(), baseName);
    const edges = parseEdges(definitionSource);
    return createTree(baseName, centers, placements, nodeMetadata, edges);
}

function parseGroupCenters(source) {
    const centers = new Map();
    for (const match of source.matchAll(/group\("([a-z0-9_]+)",\s*(-?\d+),\s*(-?\d+)\)/g)) {
        centers.set(match[1], { x: Number(match[2]), y: Number(match[3]) });
    }
    return centers;
}

function parsePlacements(source, centers) {
    const placements = new Map();
    const pattern = /put\(placements, "([A-Z0-9_]+)", "([a-z0-9_]+)",\s*(\d+),\s*(\d+)\)/g;
    for (const match of source.matchAll(pattern)) {
        const [, nodeId, groupId, orbitText, orbitIndexText] = match;
        const orbit = Number(orbitText);
        const orbitIndex = Number(orbitIndexText);
        const center = centers.get(groupId);
        if (!center || !ORBIT_ANGLES[orbit]?.[orbitIndex] && ORBIT_ANGLES[orbit]?.[orbitIndex] !== 0) {
            throw new Error(`Invalid passive-tree placement ${nodeId}: ${groupId}/${orbit}/${orbitIndex}`);
        }
        placements.set(nodeId, {
            groupId,
            orbit,
            orbitIndex,
            ...orbitPoint(center, orbit, orbitIndex)
        });
    }
    return placements;
}

function parseNodeMetadata(source, nodeIds, baseName) {
    const result = new Map();
    const sizes = { STARTER: 10, TRAVEL: 8, NODE: 10, NOTABLE: 14, KEYSTONE: 18 };
    const displaySize = source.match(/private static int displaySize\(PassiveNodeKind kind\)\s*\{(.*?)\n    \}/s)?.[1];
    for (const match of (displaySize ?? "").matchAll(/case (STARTER|TRAVEL|NODE|NOTABLE|KEYSTONE) -> (\d+);/g)) {
        sizes[match[1]] = Number(match[2]);
    }
    for (const nodeId of nodeIds) {
        const start = source.search(new RegExp(`\\b${nodeId}\\s*\\(`));
        const tail = start >= 0 ? source.slice(start) : "";
        const kind = tail.match(/PassiveNodeKind\.([A-Z]+)/)?.[1] ?? "NODE";
        const layoutAnchor = `${baseName}PassiveTreeLayout.${nodeId}`;
        const afterLayout = tail.slice(Math.max(0, tail.indexOf(layoutAnchor) + layoutAnchor.length));
        const iconKey = afterLayout.match(/"([a-z0-9_]+)"/)?.[1] ?? "custom";
        result.set(nodeId, {
            kind,
            size: sizes[kind],
            identity: identityLabel(iconKey),
            alwaysAllocated: nodeId === "STARTER",
            grantsNothing: nodeId === "STARTER"
        });
    }
    return result;
}

function parseEdges(source) {
    const edges = new Map();
    for (const match of source.matchAll(/\bspec\((.*?)\)/gs)) {
        const nodeIds = [...match[1].matchAll(/PassiveNode\.([A-Z0-9_]+)/g)].map((entry) => entry[1]);
        if (!nodeIds.length) {
            continue;
        }
        for (const target of nodeIds.slice(1)) {
            const first = nodeIds[0] < target ? nodeIds[0] : target;
            const second = nodeIds[0] < target ? target : nodeIds[0];
            edges.set(`${first}|${second}`, { first, second });
        }
    }
    return [...edges.values()];
}

function createTree(baseName, centers, placements, nodeMetadata, edges) {
    const minX = Math.min(...[...placements.values()].map((entry) => entry.x));
    const minY = Math.min(...[...placements.values()].map((entry) => entry.y));
    const offsetX = minX < 80 ? 80 - minX : 0;
    const offsetY = minY < 80 ? 80 - minY : 0;
    const groups = [];
    const nodeRefs = new Map();
    for (const [groupId] of centers) {
        const groupPlacements = [...placements.entries()].filter(([, placement]) => placement.groupId === groupId);
        if (!groupPlacements.length) {
            continue;
        }
        const groupIdentity = groupPlacements
            .map(([nodeId]) => nodeMetadata.get(nodeId)?.identity)
            .find((identity) => identity && identity !== "Starter") ?? identityLabel(groupId);
        const group = {
            id: groupId,
            shapeKind: "free",
            identity: groupIdentity,
            nodePrefix: constantName(groupId),
            shapeName: constantName(groupId),
            nodeCount: groupPlacements.length,
            nodeKind: "NODE",
            params: {},
            nodeNames: groupPlacements.map(([nodeId]) => nodeId),
            starter: groupPlacements.some(([nodeId]) => nodeId === "STARTER"),
            freePoints: groupPlacements.map(([, placement]) => ({
                x: placement.x + offsetX,
                y: placement.y + offsetY
            })),
            nodeDetails: groupPlacements.map(([nodeId]) => ({
                name: nodeId,
                ...nodeMetadata.get(nodeId)
            })),
            removedInternalLinks: Array.from(
                { length: Math.max(0, groupPlacements.length - 1) },
                (_, index) => `${index}:${index + 1}`
            ),
            source: {
                center: {
                    x: centers.get(groupId).x + offsetX,
                    y: centers.get(groupId).y + offsetY
                },
                placements: groupPlacements.map(([nodeId, placement]) => ({
                    nodeId,
                    orbit: placement.orbit,
                    orbitIndex: placement.orbitIndex,
                    x: placement.x + offsetX,
                    y: placement.y + offsetY
                }))
            }
        };
        groupPlacements.forEach(([nodeId], nodeIndex) => nodeRefs.set(nodeId, { groupId, nodeIndex }));
        groups.push(group);
    }
    const points = groups.flatMap((group) => group.freePoints);
    const width = Math.max(900, Math.ceil(Math.max(...points.map((point) => point.x)) + 80));
    const height = Math.max(650, Math.ceil(Math.max(...points.map((point) => point.y)) + 80));
    return {
        id: snakeCase(baseName),
        label: spacedName(baseName),
        sourcePath: `src/main/java/com/rngtech/rpg/progression/${baseName}PassiveTreeLayout.java`,
        tree: {
            treeId: snakeCase(baseName),
            packageName: "com.rngtech.rpg.progression",
            className: `${baseName}PassiveTreeLayout`,
            canvas: { width, height },
            groups,
            connections: edges.map(({ first, second }, index) => ({
                id: `java_${index + 1}_${first.toLowerCase()}_${second.toLowerCase()}`,
                kind: "authored",
                from: nodeRefs.get(first),
                to: nodeRefs.get(second)
            })).filter((entry) => entry.from && entry.to)
        }
    };
}

function orbitPoint(center, orbit, orbitIndex) {
    const radians = ORBIT_ANGLES[orbit][orbitIndex] * Math.PI / 180;
    const radius = ORBIT_RADII[orbit];
    return {
        x: center.x + Math.round(Math.sin(radians) * radius),
        y: center.y - Math.round(Math.cos(radians) * radius)
    };
}

function identityLabel(value) {
    const labels = {
        control: "Control",
        energy_efficiency: "Energy Efficiency",
        full_spectrum_sorting: "Output Yield",
        kinetic_overdrive: "Processing Speed",
        output_yield: "Output Yield",
        processing_speed: "Processing Speed",
        stability: "Stability",
        starter: "Starter",
        torque_channel: "Throughput"
    };
    return labels[value] ?? spacedName(value);
}

function constantName(value) {
    return String(value).replace(/([a-z0-9])([A-Z])/g, "$1_$2").replace(/[^a-zA-Z0-9]+/g, "_").toUpperCase();
}

function snakeCase(value) {
    return String(value).replace(/([a-z0-9])([A-Z])/g, "$1_$2").replace(/[^a-zA-Z0-9]+/g, "_").toLowerCase();
}

function spacedName(value) {
    return String(value).replace(/([a-z0-9])([A-Z])/g, "$1 $2").replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

if (isDirectRun()) {
    await buildPassiveTreeData();
}

function isDirectRun() {
    return typeof process !== "undefined"
        && process.argv?.[1]
        && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
}
