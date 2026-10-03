import assert from "node:assert/strict";
import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { KEYSTONE_START_DISTANCE } from "./layout-mega-tree.mjs";

export const megaCatalogUrl = new URL("../../src/main/resources/data/rngtech/mastery/machine_tree.json", import.meta.url);
export const diameters = { STARTER: 40, TRAVEL: 12, NODE: 18, NOTABLE: 34, KEYSTONE: 48 };
export const CATALOG_NODES = 1346;
// Points earned from levels 2-100. Allocation storage allows 120 for future non-level sources.
export const LEVEL_POINTS = 99;

export function auditMegaTree(tree) {
    const errors = [], byId = new Map(tree.nodes.map(node => [node.id, node]));
    const adjacency = new Map(tree.nodes.map(node => [node.id, []]));
    const edgeIds = new Set();
    for (const [a, b] of tree.links) {
        const id = [a, b].sort().join(":");
        if (a === b || !byId.has(a) || !byId.has(b) || edgeIds.has(id)) { errors.push(`Invalid or duplicate link ${id}`); continue; }
        edgeIds.add(id); adjacency.get(a).push(b); adjacency.get(b).push(a);
    }
    const caps = { STARTER: 3, TRAVEL: 4, NODE: 3, NOTABLE: 2, KEYSTONE: 1 };
    for (const node of tree.nodes) {
        const degree = adjacency.get(node.id).length;
        if (degree > caps[node.kind] || ["STARTER", "KEYSTONE"].includes(node.kind) && degree !== caps[node.kind]) errors.push(`Degree ${degree}: ${node.id}`);
        if (node.kind === "TRAVEL" && degree < 2) errors.push(`Travel dead end without a reward: ${node.id}`);
        if (node.kind === "TRAVEL" && node.effects.some(e => !tree.attributes.includes(e.stat))) errors.push(`Non-attribute travel ${node.id}`);
        for (const effect of node.effects) {
            if (!Number.isFinite(effect.value)) errors.push(`Invalid effect ${node.id}`);
            if (effect.operation === "MORE" && effect.value < 1 || effect.operation === "LESS" && (effect.value < 0 || effect.value > 1)) errors.push(`Incorrect multiplier keyword ${node.id}`);
        }
    }
    let overlaps = 0, throughNodes = 0, crossings = 0;
    for (let i = 0; i < tree.nodes.length; i++) for (let j = i + 1; j < tree.nodes.length; j++) {
        const a = tree.nodes[i], b = tree.nodes[j];
        if (Math.hypot(a.x - b.x, a.y - b.y) < (diameters[a.kind] + diameters[b.kind]) / 2 + 2) {
            overlaps++; errors.push(`Overlap ${a.id} / ${b.id}`);
        }
    }
    const edges = tree.links.map(([a,b]) => [byId.get(a), byId.get(b)]);
    for (const [a,b] of edges) for (const n of tree.nodes) {
        if (n === a || n === b) continue;
        if (segmentDistance(n,a,b) < diameters[n.kind] / 2 + 1) { throughNodes++; errors.push(`Link through ${n.id}: ${a.id} / ${b.id}`); }
    }
    const cross = (a,b,c) => (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);
    for (let i = 0; i < edges.length; i++) for (let j = i + 1; j < edges.length; j++) {
        const [a,b] = edges[i], [c,d] = edges[j];
        if (a===c || a===d || b===c || b===d) continue;
        if (cross(a,b,c)*cross(a,b,d) < 0 && cross(c,d,a)*cross(c,d,b) < 0) { crossings++; errors.push(`Crossing ${a.id}/${b.id} and ${c.id}/${d.id}`); }
    }
    const travelNodes=tree.nodes.filter(n=>n.kind==="TRAVEL");
    const isTravel=id=>byId.get(id)?.kind==="TRAVEL";
    const walkTravel=start=>{
        const depth=new Map([[start,0]]),queue=[start];
        for(let i=0;i<queue.length;i++)for(const next of adjacency.get(queue[i])) {
            if(!isTravel(next)||depth.has(next))continue;
            depth.set(next,depth.get(queue[i])+1);queue.push(next);
        }
        return depth;
    };
    if(!travelNodes.length||walkTravel(travelNodes[0].id).size!==travelNodes.length)errors.push("Travel network must connect without reward nodes or foreign starts");
    const seenRewards=new Set(), gates=[], notableInvestment=[], keystoneInvestment=[];
    let rewardPockets=0;
    for(const node of tree.nodes.filter(n=>["NODE","NOTABLE","KEYSTONE"].includes(n.kind))) {
        if(seenRewards.has(node.id))continue;
        const component=[node.id],entries=new Set();
        seenRewards.add(node.id);rewardPockets++;
        for(let i=0;i<component.length;i++)for(const next of adjacency.get(component[i])) {
            if(isTravel(next)){entries.add(next);continue;}
            if(byId.get(next).kind==="STARTER"){errors.push("Reward bypasses travel: "+next);continue;}
            if(!seenRewards.has(next)){seenRewards.add(next);component.push(next);}
        }
        if(entries.size!==1){errors.push("Reward pocket must have one road gate: "+node.id);continue;}
        const gate=[...entries][0];if(!gates.includes(gate))gates.push(gate);
        const depth=new Map([[gate,0]]),queue=[gate],members=new Set(component);
        for(let i=0;i<queue.length;i++)for(const next of adjacency.get(queue[i])) {
            if(!members.has(next)||depth.has(next))continue;
            depth.set(next,depth.get(queue[i])+1);queue.push(next);
        }
        for(const id of component) {
            if(byId.get(id).kind==="NOTABLE") {
                notableInvestment.push(depth.get(id));
                if(depth.get(id)<3)errors.push("Notable needs at least three reward allocations: "+id);
            }
            // Keystones may attach directly to a road; their distance from every start is checked below.
            if(byId.get(id).kind==="KEYSTONE") keystoneInvestment.push(depth.get(id));
        }
    }
    let minimumGateTravel=Infinity;
    for(let i=0;i<gates.length;i++) {
        const depths=walkTravel(gates[i]);
        for(const next of gates.slice(i+1))minimumGateTravel=Math.min(minimumGateTravel,depths.get(next)??Infinity);
    }
    if(minimumGateTravel<2)errors.push("Reward pockets need intervening travel allocations");
    const investment={
        travelNodes:travelNodes.length,
        rewardPockets:gates.length, rewardComponents:rewardPockets,
        minimumGateTravel,
        notablePointsFromRoad:{min:Math.min(...notableInvestment),max:Math.max(...notableInvestment)},
        keystonePointsFromRoad:{min:Math.min(...keystoneInvestment),max:Math.max(...keystoneInvestment)}
    };
    const roots = tree.nodes.filter(node => node.kind === "STARTER");
    const distances = {}, builds = {};
    for (const root of roots) {
        const depth = new Map([[root.id, 0]]), parent = new Map(), queue = [root.id];
        for (let i = 0; i < queue.length; i++) for (const next of adjacency.get(queue[i])) {
            if (depth.has(next) || byId.get(next).kind === "STARTER") continue;
            depth.set(next, depth.get(queue[i])+1); parent.set(next, queue[i]); queue.push(next);
        }
        if (depth.size !== tree.nodes.length - roots.length + 1) errors.push(`Unreachable nodes from ${root.id}`);
        distances[root.id] = Object.fromEntries(tree.nodes.filter(n=>n.kind === "KEYSTONE").map(n=>[n.id,depth.get(n.id)]));
        for (const [id, distance] of Object.entries(distances[root.id])) {
            if (distance < KEYSTONE_START_DISTANCE || distance > LEVEL_POINTS) errors.push(`Keystone commitment ${root.id} / ${id}: ${distance}`);
        }
        const ordered = [];
        const candidates = tree.nodes.filter(n=>n.kind === "KEYSTONE").sort((a,b)=>depth.get(a.id)-depth.get(b.id));
        // Representative route combines the nearest endpoint with an opposite-sector specialty.
        for (const destination of [candidates[0], candidates[Math.floor(candidates.length*0.7)]]) {
            const path = [];
            for (let id=destination.id; id!==root.id; id=parent.get(id)) path.unshift(id);
            for (const id of path) if (!ordered.includes(id)) ordered.push(id);
        }
        for (let i=0; ordered.length < LEVEL_POINTS && i < queue.length; i++) {
            const id=queue[i];
            if (id!==root.id && !ordered.includes(id) && adjacency.get(id).some(n=>n===root.id || ordered.includes(n))) ordered.push(id);
        }
        builds[root.id] = ordered;
        if (ordered.length !== LEVEL_POINTS) errors.push(`Representative build budget from ${root.id}: ${ordered.length}`);
    }
    // Every start sits in the occupied center region, with room for core pathing inside the start ring.
    const xs = tree.nodes.map(n => n.x), ys = tree.nodes.map(n => n.y);
    const bounds = { minX: Math.min(...xs), minY: Math.min(...ys), width: Math.max(...xs) - Math.min(...xs), height: Math.max(...ys) - Math.min(...ys) };
    const startPositions = Object.fromEntries(roots.map(root => [root.id, {
        x: Math.round((root.x - bounds.minX) / bounds.width * 100) / 100,
        y: Math.round((root.y - bounds.minY) / bounds.height * 100) / 100
    }]));
    for (const [id, position] of Object.entries(startPositions)) {
        if (position.x < 0.3 || position.x > 0.7 || position.y < 0.3 || position.y > 0.7) errors.push(`Start outside center region: ${id}`);
    }
    const lengths = edges.map(([a,b])=>Math.hypot(a.x-b.x,a.y-b.y)).sort((a,b)=>a-b);
    return { errors, builds, metrics: { nodes:tree.nodes.length, starts:roots.length, startPositions, kinds:Object.fromEntries(Object.keys(caps).map(k=>[k,tree.nodes.filter(n=>n.kind===k).length])), groups: new Set(tree.nodes.map(n=>n.group)).size, edges:tree.links.length, cycleRank:tree.links.length-tree.nodes.length+1, investment, overlaps, throughNodes, crossings, medianLength:lengths[Math.floor(lengths.length/2)], p90Length:lengths[Math.floor(lengths.length*.9)], distances } };
}

function segmentDistance(p,a,b) {
    const dx=b.x-a.x,dy=b.y-a.y;
    const t=Math.max(0,Math.min(1,((p.x-a.x)*dx+(p.y-a.y)*dy)/(dx*dx+dy*dy)));
    return Math.hypot(p.x-a.x-t*dx,p.y-a.y-t*dy);
}

export async function checkMegaTree({log=true}={}) {
    const tree=JSON.parse(await readFile(megaCatalogUrl,"utf8"));
    const audit=auditMegaTree(tree);
    assert.equal(tree.nodes.length,CATALOG_NODES);
    assert.equal(new Set(tree.nodes.map(n=>n.id)).size,tree.nodes.length);
    assert.equal(audit.metrics.starts,6);
    if(log) console.log(`Mega tree ${audit.errors.length ? "FAIL" : "PASS"}: ${JSON.stringify({...audit.metrics,distances:undefined})}`);
    assert.equal(audit.errors.length,0,audit.errors.slice(0,30).join("\n"));
    // These mutations would preserve reward values while breaking the investment rules.
    const reward=tree.groups.find(g=>g.role==="reward"&&g.shape==="ring");
    const notable=tree.nodes.find(n=>n.group===reward.id&&n.kind==="NOTABLE");
    const shortcut=structuredClone(tree);
    shortcut.links.push([reward.gate,notable.id]);
    assert.ok(auditMegaTree(shortcut).errors.some(e=>e.startsWith("Notable needs at least three")));
    const second=tree.groups.find(g=>g.role==="reward"&&g.gate!==reward.gate);
    const bypass=structuredClone(tree);
    bypass.links.push([notable.id,tree.nodes.find(n=>n.group===second.id).id]);
    assert.ok(auditMegaTree(bypass).errors.some(e=>e.startsWith("Reward pocket must have one road gate")));
    const lookup=new Map(tree.nodes.map(n=>[n.id,n]));
    const disconnected=structuredClone(tree);
    // Cutting a start's first road step from the rest of its road leaves that step reachable only through the start.
    const starter=tree.nodes.find(n=>n.kind==="STARTER");
    const exit=tree.links.flatMap(([a,b])=>a===starter.id?[b]:b===starter.id?[a]:[])[0];
    const approach=disconnected.links.findIndex(([a,b])=>(a===exit||b===exit)&&lookup.get(a===exit?b:a).kind==="TRAVEL");
    assert.ok(approach>=0);
    disconnected.links.splice(approach,1);
    assert.ok(auditMegaTree(disconnected).errors.some(e=>e.startsWith("Travel network must connect")));
    return {tree,...audit};
}

if(process.argv[1]===fileURLToPath(import.meta.url)) {
    const {tree,...audit}=await checkMegaTree();
    if(process.argv.includes("--report")) await writeFile(new URL("../../docs/reference/machine-mega-tree-audit.json",import.meta.url),JSON.stringify(audit,null,4)+"\n");
}
