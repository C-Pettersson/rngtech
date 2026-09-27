import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import vm from "node:vm";
import { buildPassiveTreeData } from "./export-passive-tree-data.mjs";

const TOOL_ROOT = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(TOOL_ROOT, "../..");
const DEFAULT_SIZES = { STARTER: 10, TRAVEL: 8, NODE: 10, NOTABLE: 14, KEYSTONE: 18 };
const DEGREE_CAPS = { STARTER: 3, TRAVEL: 4, NODE: 3, NOTABLE: 2, KEYSTONE: 1 };
const FORESTRY_KIND_COUNTS = { STARTER: 1, TRAVEL: 48, NODE: 32, NOTABLE: 14, KEYSTONE: 5 };

/** Validate the exported Java graph, including the fitted gate used by the editor. */
export async function checkPassiveTreeData(catalog, { log = true } = {}) {
    const editorSource = await readFile(path.join(TOOL_ROOT, "web/passive-tree-editor.js"), "utf8");
    const results = [];
    const failures = [];
    if (!catalog.trees?.some((entry) => entry.id === "forestry_companion")) {
        failures.push("Missing Forestry Companion Java export");
    }
    for (const entry of catalog.trees ?? []) {
        const enumPath = path.resolve(PROJECT_ROOT, entry.sourcePath.replace("PassiveTreeLayout.java", "PassiveNode.java"));
        const enumSource = await readFile(enumPath, "utf8");
        const expectedNodes = new Map([...enumSource.matchAll(/\b([A-Z][A-Z0-9_]+)\s*\(\s*\d+\s*,\s*PassiveNodeKind\.([A-Z]+)/g)]
            .map((match) => [match[1], match[2]]));
        const result = auditPassiveTree(entry, editorSource, expectedNodes);
        results.push(result.metrics);
        failures.push(...result.errors.map((error) => `${entry.label ?? entry.id}: ${error}`));
        if (log) {
            console.log(`Passive-tree audit ${result.errors.length ? "FAIL" : "PASS"}: ${JSON.stringify(result.metrics)}`);
        }
    }
    if (failures.length) {
        throw new Error(`Passive-tree validation failed:\n${failures.map((error) => `- ${error}`).join("\n")}`);
    }
    return results;
}

export function auditPassiveTree(entry, editorSource, expectedNodes = new Map()) {
    const errors = [];
    const tree = entry.tree;
    const context = vm.createContext({
        document: { addEventListener() {} },
        crypto: globalThis.crypto,
        structuredClone,
        exportedTree: tree
    });
    vm.runInContext(editorSource, context, { filename: "passive-tree-editor.js", timeout: 5000 });
    const visual = vm.runInContext(`
        passiveTreeState.tree = exportedTree;
        const graph = passiveTreeVisualGraph();
        const bounds = pointBounds(graph.nodes.map((node) => node.point));
        const lengths = graph.edges.filter((edge) => !edge.endpointCommitment)
            .map((edge) => edge.length).sort((first, second) => first - second);
        const median = percentile(lengths, 0.5);
        const p90 = percentile(lengths, 0.9);
        const threshold = Math.max(240, median * 2.75);
        ({
            graph,
            crossings: properGraphCrossings(graph.edges).length,
            centrality: starterCentrality(graph.nodes, bounds),
            endpoints: passiveTreeEndpointSpread(graph.groups, graph.nodes),
            median,
            p90,
            threshold,
            longChords: lengths.filter((length) => length > threshold).length,
            internalLinks: graph.groups.flatMap(({group, points}) => groupLinks(group, points)
                .map((link) => [nodeKey(group.id, link.fromIndex), nodeKey(group.id, link.toIndex)]))
        });
    `, context, { timeout: 5000 });
    const { graph } = visual;
    const groupsById = new Map();
    const rawDetails = new Map();
    for (const group of tree.groups) {
        if (groupsById.has(group.id)) {
            errors.push(`Duplicate group id ${group.id}`);
        }
        groupsById.set(group.id, group);
        for (const detail of group.nodeDetails ?? []) {
            rawDetails.set(detail.name, detail);
        }
    }
    const nodes = graph.nodes.map((node) => ({
        ...node,
        size: rawDetails.get(node.detail.name)?.size ?? DEFAULT_SIZES[node.detail.kind]
    }));
    const nodesByKey = new Map(nodes.map((node) => [node.key, node]));
    const names = new Set();
    const counts = {};
    for (const node of nodes) {
        const { name, kind } = node.detail;
        counts[kind] = (counts[kind] ?? 0) + 1;
        if (names.has(name)) {
            errors.push(`Duplicate node id ${name}`);
        }
        names.add(name);
        if (!Object.hasOwn(DEGREE_CAPS, kind)) {
            errors.push(`Unknown node kind ${kind}: ${name}`);
        }
        if (!Number.isFinite(node.point.x) || !Number.isFinite(node.point.y)
                || !Number.isFinite(node.size) || node.size <= 0) {
            errors.push(`Invalid position or size: ${name}`);
        }
        if (expectedNodes.size && expectedNodes.get(name) !== kind) {
            errors.push(`Exported id/kind disagrees with Java enum: ${name}/${kind}`);
        }
    }
    for (const name of expectedNodes.keys()) {
        if (!names.has(name)) {
            errors.push(`Java enum node missing from export: ${name}`);
        }
    }
    if (nodes.length > 128) {
        errors.push(`Save cap exceeded: ${nodes.length} nodes`);
    }
    if (entry.id === "forestry_companion") {
        for (const [kind, expectedCount] of Object.entries(FORESTRY_KIND_COUNTS)) {
            if (counts[kind] !== expectedCount) {
                errors.push(`Expected ${expectedCount} ${kind} nodes; found ${counts[kind] ?? 0}`);
            }
        }
    }

    const seenEdges = new Set();
    let duplicates = 0;
    const authoredEdges = [
        ...visual.internalLinks,
        ...(tree.connections ?? []).map((edge) => [referenceKey(edge.from), referenceKey(edge.to)])
    ];
    for (const [first, second] of authoredEdges) {
        const key = edgeKey(first, second);
        if (!nodesByKey.has(first) || !nodesByKey.has(second) || first === second) {
            errors.push(`Invalid link ${first} to ${second}`);
        }
        if (seenEdges.has(key)) {
            duplicates++;
            errors.push(`Duplicate link ${first} to ${second}`);
        }
        seenEdges.add(key);
    }
    const edges = graph.edges.map((edge) => [edge.from.key, edge.to.key]);
    const adjacency = adjacencyFor(nodes.map((node) => node.key), edges);
    const starters = nodes.filter((node) => node.detail.kind === "STARTER");
    if (starters.length !== 1 || !starters[0]?.detail.alwaysAllocated) {
        errors.push("Tree must have exactly one always-allocated starter");
    }
    let degreeViolations = 0;
    for (const node of nodes) {
        const degree = adjacency.get(node.key).size;
        const cap = DEGREE_CAPS[node.detail.kind];
        if (degree > cap || ["STARTER", "KEYSTONE"].includes(node.detail.kind) && degree !== cap) {
            degreeViolations++;
            errors.push(`Invalid degree for ${node.detail.name}: ${degree} (${node.detail.kind} cap ${cap})`);
        }
        if (node.detail.kind !== "STARTER" && node.detail.alwaysAllocated) {
            errors.push(`Non-starter is always allocated: ${node.detail.name}`);
        }
    }
    const distances = shortestDistances(starters[0]?.key, adjacency);
    if (distances.size !== nodes.length) {
        errors.push(`${nodes.length - distances.size} nodes are unreachable from the starter`);
    }
    const keystoneDistances = {};
    for (const node of nodes.filter((candidate) => candidate.detail.kind === "KEYSTONE")) {
        const distance = distances.get(node.key);
        keystoneDistances[node.detail.name] = distance ?? null;
        if (distance < 7) {
            errors.push(`Keystone ${node.detail.name} is only ${distance} links from the starter`);
        }
    }

    let overlaps = 0;
    let linksThroughNodes = 0;
    let overlappingLinks = 0;
    for (let first = 0; first < nodes.length; first++) {
        for (let second = first + 1; second < nodes.length; second++) {
            const a = nodes[first];
            const b = nodes[second];
            const separation = (a.size + b.size) / 2;
            if (Math.abs(a.point.x - b.point.x) < separation && Math.abs(a.point.y - b.point.y) < separation) {
                overlaps++;
                errors.push(`Node bounds overlap: ${a.detail.name} and ${b.detail.name}`);
            }
        }
    }
    for (const edge of graph.edges) {
        const first = edge.from.key;
        const second = edge.to.key;
        const a = nodesByKey.get(first);
        const b = nodesByKey.get(second);
        const pathPoints = edge.path ?? [a.point, b.point];
        for (const node of nodes) {
            if (node.key !== first && node.key !== second
                    && pathPoints.slice(1).some((point, index) =>
                        segmentDistance(node.point, pathPoints[index], point) < node.size / 2 + 4)) {
                linksThroughNodes++;
                errors.push(`Link ${a.detail.name} to ${b.detail.name} crosses ${node.detail.name}`);
            }
        }
    }
    for (let first = 0; first < graph.edges.length; first++) {
        const a = graph.edges[first];
        const firstPath = a.path ?? [a.from.point, a.to.point];
        for (let second = first + 1; second < graph.edges.length; second++) {
            const b = graph.edges[second];
            if ([a.from.key, a.to.key].some((key) => key === b.from.key || key === b.to.key)) {
                continue;
            }
            const secondPath = b.path ?? [b.from.point, b.to.point];
            if (firstPath.slice(1).some((point, index) => secondPath.slice(1).some((otherPoint, otherIndex) =>
                segmentsOverlap(firstPath[index], point, secondPath[otherIndex], otherPoint)))) {
                overlappingLinks++;
                errors.push(`Overlapping links: ${a.from.detail.name} to ${a.to.detail.name} and ${b.from.detail.name} to ${b.to.detail.name}`);
            }
        }
    }

    const crossingLimit = Math.max(3, Math.round(edges.length * 0.02));
    const chordLimit = Math.max(3, Math.round(edges.length * 0.06));
    if (visual.crossings > crossingLimit) {
        errors.push(`Fitted crossing gate: ${visual.crossings} > ${crossingLimit}`);
    }
    if (visual.longChords > chordLimit || visual.p90 > visual.threshold) {
        errors.push(`Fitted edge-length gate: ${visual.longChords} long chords (limit ${chordLimit}), P90 ${round(visual.p90)} (limit ${round(visual.threshold)})`);
    }
    if (visual.centrality.x < 0.3 || visual.centrality.x > 0.7 || visual.centrality.y < 0.3 || visual.centrality.y > 0.7) {
        errors.push(`Fitted starter position gate: ${round(visual.centrality.x * 100)}% / ${round(visual.centrality.y * 100)}%`);
    }
    if (visual.endpoints.count >= 3 && (visual.endpoints.sectors < 5 || visual.endpoints.maximumGap > 120)) {
        errors.push(`Fitted endpoint gate: ${visual.endpoints.sectors}/8 sectors; ${round(visual.endpoints.maximumGap)} degree gap`);
    }

    const nodeHealth = graphHealth(adjacency);
    const groupEdges = new Map();
    let sameGroupLinks = 0;
    for (const edge of graph.edges) {
        const a = edge.from.groupId;
        const b = edge.to.groupId;
        if (a === b) {
            sameGroupLinks++;
        } else {
            groupEdges.set(edgeKey(a, b), [a, b]);
        }
    }
    const groupAdjacency = adjacencyFor([...groupsById.keys()], [...groupEdges.values()]);
    const groupHealth = graphHealth(groupAdjacency);
    const endpointGroups = new Set(graph.groups.filter(({ group, points }) => group.id.endsWith("_endpoint")
        || points.some((_, index) => group.nodeDetails?.[index]?.kind === "KEYSTONE")).map(({ group }) => group.id));
    const finalKeystoneLink = ([first, second]) => [first, second].some((key) =>
        nodesByKey.get(key)?.detail.kind === "KEYSTONE" && adjacency.get(key)?.size === 1);
    const metrics = {
        tree: entry.id,
        nodes: nodes.length,
        kinds: counts,
        groups: tree.groups.length,
        edges: edges.length,
        sameGroupLinks,
        crossGroupLinks: edges.length - sameGroupLinks,
        degreeViolations,
        keystoneDistances,
        nodeCycleRank: nodeHealth.cycleRank,
        nodeBridges: nodeHealth.bridges.length,
        nodeBridgesExcludingKeystones: nodeHealth.bridges.filter((edge) => !finalKeystoneLink(edge)).length,
        nodeArticulations: nodeHealth.articulations.size,
        longestDegreeTwoCorridor: longestCorridor(adjacency),
        groupEdges: groupEdges.size,
        groupCycleRank: groupHealth.cycleRank,
        groupBridges: groupHealth.bridges.length,
        groupBridgesExcludingEndpoints: groupHealth.bridges.filter((edge) => !edge.some((key) => endpointGroups.has(key))).length,
        groupArticulations: groupHealth.articulations.size,
        longestGroupCorridor: longestCorridor(groupAdjacency, endpointGroups),
        overlaps,
        linksThroughNodes,
        overlappingLinks,
        duplicateLinks: duplicates,
        crossings: visual.crossings,
        longChords: visual.longChords,
        edgeMedian: round(visual.median),
        edgeP90: round(visual.p90),
        starterPercent: { x: round(visual.centrality.x * 100), y: round(visual.centrality.y * 100) },
        endpointSectors: visual.endpoints.sectors,
        endpointMaximumGap: round(visual.endpoints.maximumGap)
    };
    return { metrics, errors };
}

function referenceKey(reference) {
    return reference ? `${reference.groupId}:${reference.nodeIndex}` : "missing";
}

function edgeKey(first, second) {
    return first < second ? `${first}|${second}` : `${second}|${first}`;
}

function adjacencyFor(keys, edges) {
    const result = new Map(keys.map((key) => [key, new Set()]));
    for (const [first, second] of edges) {
        result.get(first)?.add(second);
        result.get(second)?.add(first);
    }
    return result;
}

function shortestDistances(starter, adjacency) {
    if (!adjacency.has(starter)) {
        return new Map();
    }
    const distances = new Map([[starter, 0]]);
    const queue = [starter];
    for (let index = 0; index < queue.length; index++) {
        for (const neighbor of adjacency.get(queue[index])) {
            if (!distances.has(neighbor)) {
                distances.set(neighbor, distances.get(queue[index]) + 1);
                queue.push(neighbor);
            }
        }
    }
    return distances;
}

function graphHealth(adjacency) {
    const discovery = new Map();
    const low = new Map();
    const bridges = [];
    const articulations = new Set();
    let components = 0;
    let clock = 0;
    const visit = (node, parent) => {
        discovery.set(node, ++clock);
        low.set(node, clock);
        let children = 0;
        for (const neighbor of adjacency.get(node)) {
            if (!discovery.has(neighbor)) {
                children++;
                visit(neighbor, node);
                low.set(node, Math.min(low.get(node), low.get(neighbor)));
                if (low.get(neighbor) > discovery.get(node)) {
                    bridges.push([node, neighbor]);
                }
                if (parent !== undefined && low.get(neighbor) >= discovery.get(node)) {
                    articulations.add(node);
                }
            } else if (neighbor !== parent) {
                low.set(node, Math.min(low.get(node), discovery.get(neighbor)));
            }
        }
        if (parent === undefined && children > 1) {
            articulations.add(node);
        }
    };
    for (const node of adjacency.keys()) {
        if (!discovery.has(node)) {
            components++;
            visit(node);
        }
    }
    const edgeCount = [...adjacency.values()].reduce((sum, neighbors) => sum + neighbors.size, 0) / 2;
    return { bridges, articulations, cycleRank: edgeCount - adjacency.size + components };
}

function longestCorridor(adjacency, excluded = new Set()) {
    const candidates = new Set([...adjacency.keys()].filter((key) => adjacency.get(key).size === 2 && !excluded.has(key)));
    let longest = 0;
    while (candidates.size) {
        const queue = [candidates.values().next().value];
        candidates.delete(queue[0]);
        for (let index = 0; index < queue.length; index++) {
            for (const neighbor of adjacency.get(queue[index])) {
                if (candidates.delete(neighbor)) {
                    queue.push(neighbor);
                }
            }
        }
        longest = Math.max(longest, queue.length);
    }
    return longest;
}

function segmentDistance(point, first, second) {
    const dx = second.x - first.x;
    const dy = second.y - first.y;
    const lengthSquared = dx * dx + dy * dy;
    const progress = lengthSquared ? Math.max(0, Math.min(1, ((point.x - first.x) * dx + (point.y - first.y) * dy) / lengthSquared)) : 0;
    return Math.hypot(point.x - first.x - progress * dx, point.y - first.y - progress * dy);
}

function segmentsOverlap(firstStart, firstEnd, secondStart, secondEnd) {
    const onSegment = (start, end, point) => Math.min(start.x, end.x) <= point.x
        && point.x <= Math.max(start.x, end.x)
        && Math.min(start.y, end.y) <= point.y
        && point.y <= Math.max(start.y, end.y)
        && (end.x - start.x) * (point.y - start.y) - (end.y - start.y) * (point.x - start.x) === 0;
    return onSegment(firstStart, firstEnd, secondStart)
        || onSegment(firstStart, firstEnd, secondEnd)
        || onSegment(secondStart, secondEnd, firstStart)
        || onSegment(secondStart, secondEnd, firstEnd);
}

function round(value) {
    return Math.round(value * 100) / 100;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    await checkPassiveTreeData(await buildPassiveTreeData());
}
