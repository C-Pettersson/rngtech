// Layered geometry for the shared machine tree. Roads come first: a central travel hub and core ring, six starts on
// an outer start ring, home regions separated by boundary spokes, a middle ring, an outer field with star hubs, and a
// perimeter road. Reward pockets then branch from single road gates into the largest remaining open spaces.
export const CENTER = { x: 4000, y: 4000 };
export const START_ANGLES = [-150, -90, -30, 30, 90, 150];
export const RING_RADII = { hub: 210, core: 840, start: 1200, middle: 2250, outer: 3100 };
// Design radii are authored at a readable size, then scaled to the density of the reward pockets.
const SCALE = 0.58;
const RADIUS = { STARTER: 20, TRAVEL: 6, NODE: 9, NOTABLE: 17, KEYSTONE: 24 };
// Keystones may sit directly on a road, but never within this many allocations of any start.
export const KEYSTONE_START_DISTANCE = 10;
const KEYSTONE_SPACING = 280;
// Three core, nine home-region, seven outer-field, and five perimeter keystones; outer star hubs and the center add six more.
// Angles are staggered between layers so commitments do not line up along spokes.
const KEYSTONE_TARGETS = [
    ...[[60, 560], [180, 560], [-65, 700]].map(([angle, radius]) => ({ layer: "core", radius, angle, shapes: ["keyArc", "keyFork", "keyDirect"] })),
    ...[-168, -132, -72, -48, -12, 48, 72, 108, 168].map((angle, i) => ({ layer: "home", radius: 1850, angle,
        shapes: i % 2 ? ["keyFork", "keyArc", "keyDirect"] : ["keyDirect", "keyArc", "keyFork"] })),
    ...[-133, -65, 0, 25, 82, 148, 178].map((angle, i) => ({ layer: "outer", radius: 2600, angle,
        shapes: i % 2 ? ["keyDirect", "keyFork", "keyArc"] : ["keyArc", "keyDirect", "keyFork"] })),
    ...[-146, -80, -15, 64, 132].map((angle, i) => ({ layer: "perimeter", radius: RING_RADII.outer + 250, angle,
        shapes: [["keyDirect", "keyArc", "keyFork"], ["keyFork", "keyArc", "keyDirect"], ["keyArc", "keyFork", "keyDirect"]][i % 3] }))
];

const toRadians = degrees => degrees * Math.PI / 180;
const normalize = degrees => ((degrees % 360) + 540) % 360 - 180;
const at = (radius, degrees, origin = CENTER) => polar(radius * SCALE, degrees, origin);
const polar = (radius, degrees, origin = CENTER) => ({
    x: origin.x + radius * Math.cos(toRadians(degrees)),
    y: origin.y + radius * Math.sin(toRadians(degrees))
});
const angleOf = (point, origin = CENTER) => Math.atan2(point.y - origin.y, point.x - origin.x) * 180 / Math.PI;
const radiusOf = (point, origin = CENTER) => Math.hypot(point.x - origin.x, point.y - origin.y);
const distance = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
const cross = (a, b, c) => (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
const crossing = (a, b, c, d) => cross(a, b, c) * cross(a, b, d) < 0 && cross(c, d, a) * cross(c, d, b) < 0;
function segmentDistance(p, a, b) {
    const dx = b.x - a.x, dy = b.y - a.y;
    const t = Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)));
    return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}
export const middleRadius = degrees => SCALE * (RING_RADII.middle + 70 * Math.sin(toRadians(3 * degrees + 40)) + 45 * Math.cos(toRadians(5 * degrees + 10)));
export const outerRadius = degrees => SCALE * (RING_RADII.outer + 95 * Math.sin(toRadians(2 * degrees + 75)) + 60 * Math.cos(toRadians(5 * degrees + 25)));

// Pocket templates in gate-local coordinates: +x leaves the road, +y is the mirrored side.
const SHAPES = {
    arc: { gateLinks: [0], nodes: [["NODE", 62, 0], ["NODE", 108, 26], ["NOTABLE", 152, 80]], links: [[0, 1], [1, 2]] },
    diamond: { gateLinks: [0], nodes: [["NODE", 60, 0], ["NODE", 112, -36], ["NODE", 112, 36], ["NOTABLE", 166, 0]], links: [[0, 1], [0, 2], [1, 3], [2, 3]] },
    fork: { gateLinks: [0], nodes: [["NODE", 60, 0], ["NODE", 106, -42], ["NOTABLE", 158, -78], ["NODE", 106, 42], ["NOTABLE", 158, 78]], links: [[0, 1], [1, 2], [0, 3], [3, 4]] },
    chain: { gateLinks: [0], nodes: [["NODE", 56, 0], ["NODE", 108, 10], ["NOTABLE", 158, 30], ["NODE", 204, 58], ["NODE", 244, 94], ["NOTABLE", 276, 138]], links: [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5]] },
    horseshoe: { gateLinks: [0, 5], nodes: ringPoints(["NODE", "NODE", "NOTABLE", "NOTABLE", "NODE", "NODE"]), links: [[0, 1], [1, 2], [5, 4], [4, 3]] },
    ring: { gateLinks: [0, 5], nodes: ringPoints(["NODE", "NODE", "NOTABLE", "NOTABLE", "NODE", "NODE"]), links: [[0, 1], [1, 2], [2, 3], [5, 4], [4, 3]] },
    wheel: { gateLinks: [0], nodes: wheelPoints(), links: [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 0], [6, 2], [6, 4]] },
    // A three-by-three lattice entered from two corners, with notables across the far row.
    lattice: {
        gateLinks: [0, 2],
        nodes: [["NODE", 62, -56], ["NODE", 70, 0], ["NODE", 62, 56], ["NODE", 130, -58], ["NODE", 135, 0], ["NODE", 130, 58], ["NOTABLE", 200, -60], ["NOTABLE", 212, 0], ["NOTABLE", 200, 60]],
        links: [[0, 1], [1, 2], [0, 3], [1, 4], [2, 5], [3, 4], [4, 5], [3, 6], [5, 8], [6, 7], [7, 8]]
    },
    // Two arms climb from one entry to a scalloped crown of three notables.
    crown: {
        gateLinks: [0],
        nodes: [["NODE", 50, 0], ["NODE", 95, -50], ["NODE", 150, -100], ["NOTABLE", 220, -112], ["NODE", 240, -50], ["NOTABLE", 285, 0], ["NODE", 240, 50], ["NOTABLE", 220, 112], ["NODE", 150, 100], ["NODE", 95, 50]],
        links: [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 8], [8, 9], [9, 0]]
    },
    star: { gateLinks: [0], nodes: starPoints(), links: Array.from({ length: 12 }, (_, i) => [i, (i + 1) % 12]) },
    keyDirect: { gateLinks: [0], nodes: [["KEYSTONE", 96, 0]], links: [] },
    keyArc: { gateLinks: [0], nodes: [["NODE", 62, 0], ["NODE", 110, 24], ["NOTABLE", 162, 66], ["KEYSTONE", 238, 120]], links: [[0, 1], [1, 2], [2, 3]] },
    keyFork: { gateLinks: [0], nodes: [["NODE", 60, 0], ["NODE", 106, -44], ["NOTABLE", 158, -82], ["NODE", 110, 42], ["NOTABLE", 170, 64], ["KEYSTONE", 250, 88]], links: [[0, 1], [1, 2], [0, 3], [3, 4], [4, 5]] }
};

function ringPoints(kinds) {
    const radius = 88;
    return kinds.map((kind, index) => {
        const angle = Math.PI + (index + 1) * Math.PI * 2 / 7;
        return [kind, radius + radius * Math.cos(angle), radius * Math.sin(angle)];
    });
}

function wheelPoints() {
    const radius = 64, center = 58 + radius;
    const ring = [0, 1, 2, 3, 4, 5].map(index => {
        const angle = Math.PI + index * Math.PI / 3;
        return ["NODE", center + radius * Math.cos(angle), radius * Math.sin(angle)];
    });
    return [...ring, ["NOTABLE", center, 0]];
}

// Hexagram outline: tips alternate with inner vertices. The first tip is the entry; three tips are notables.
function starPoints() {
    const tip = 105, inner = tip / Math.sqrt(3), center = 58 + tip;
    return Array.from({ length: 12 }, (_, i) => {
        const angle = Math.PI + i * Math.PI / 6, radius = i % 2 ? inner : tip;
        return [[2, 6, 10].includes(i) ? "NOTABLE" : "NODE", center + radius * Math.cos(angle), radius * Math.sin(angle)];
    });
}

export function layoutMegaTree({ starts, totalNodes, keystones, centerFacing, extras = [] }) {
    let seed = 0x5eed17;
    const random = () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; };
    const nodes = [], byId = new Map(), adjacency = new Map(), links = [], groups = [];
    let travelCount = 0, groupCount = 0;
    const add = node => { nodes.push(node); byId.set(node.id, node); adjacency.set(node.id, new Set()); return node; };
    const link = (a, b) => { links.push([a.id, b.id]); adjacency.get(a.id).add(b.id); adjacency.get(b.id).add(a.id); };
    const degree = node => adjacency.get(node.id).size;
    const group = (prefix, point, role, extra = {}) => {
        const id = `${prefix}_${groupCount++}`;
        groups.push({ id, x: Math.round(point.x), y: Math.round(point.y), role, ...extra });
        return id;
    };
    const travel = (point, groupId) => add({ id: `path_${String(++travelCount).padStart(4, "0")}`, kind: "TRAVEL", x: point.x, y: point.y, group: groupId });
    const junction = point => travel(point, group("junction", point, "travel"));

    // Roads place paid travel steps at even arc-length intervals along straight, bowed, or curved paths.
    function road(from, to, { spacing = 76, curve = null, bow = 0, steps = null } = {}) {
        const samples = [];
        if (bow) {
            const dx = to.x - from.x, dy = to.y - from.y, length = Math.hypot(dx, dy);
            for (let i = 0; i <= 24; i++) {
                const t = i / 24, offset = bow * Math.sin(Math.PI * t);
                samples.push({ x: from.x + dx * t - dy / length * offset, y: from.y + dy * t + dx / length * offset });
            }
        } else if (curve) {
            const origin = curve.origin ?? CENTER;
            const a0 = angleOf(from, origin), sweep = normalize(angleOf(to, origin) - a0);
            const r0 = radiusOf(from, origin), r1 = radiusOf(to, origin);
            for (let i = 0; i <= 48; i++) {
                const t = i / 48;
                samples.push(polar(r0 + (r1 - r0) * t, a0 + sweep * t, origin));
            }
        } else {
            samples.push(from, to);
        }
        const lengths = [0];
        for (let i = 1; i < samples.length; i++) lengths.push(lengths[i - 1] + distance(samples[i - 1], samples[i]));
        const total = lengths.at(-1);
        const pointAt = target => {
            const i = Math.max(1, lengths.findIndex(length => length >= target));
            const t = (target - lengths[i - 1]) / Math.max(1e-9, lengths[i] - lengths[i - 1]);
            return { x: samples[i - 1].x + (samples[i].x - samples[i - 1].x) * t, y: samples[i - 1].y + (samples[i].y - samples[i - 1].y) * t };
        };
        const count = steps ?? Math.max(1, Math.round(total / spacing) - 1);
        const id = group("road", pointAt(total / 2), "travel");
        let previous = from;
        for (let step = 1; step <= count; step++) {
            const node = travel(pointAt(total * step / (count + 1)), id);
            link(previous, node);
            previous = node;
        }
        link(previous, to);
    }

    // Roundabouts turn a many-road crossing into a ring of degree-limited travel nodes.
    function roundabout(center, radius, count, rotation) {
        const id = group("hub", center, "travel");
        const ring = Array.from({ length: count }, (_, k) => travel(polar(radius, rotation + k * 360 / count, center), id));
        ring.forEach((node, k) => link(node, ring[(k + 1) % count]));
        ring.center = center;
        return ring;
    }
    const port = (ring, toward) => {
        const angle = angleOf(toward, ring.center);
        return ring.filter(node => degree(node) < 3)
            .sort((a, b) => Math.abs(normalize(angleOf(a, ring.center) - angle)) - Math.abs(normalize(angleOf(b, ring.center) - angle)))[0];
    };

    const middle = [], outer = [];
    const ringJunction = (list, radiusAt, degrees) => {
        const existing = list.find(node => Math.abs(normalize(node.angle - degrees)) < 3.5 && degree(node) < 4);
        if (existing) return existing;
        const node = junction(polar(radiusAt(degrees), degrees));
        node.angle = degrees;
        list.push(node);
        return node;
    };
    const mid = degrees => ringJunction(middle, middleRadius, degrees);
    const rim = degrees => ringJunction(outer, outerRadius, degrees);

    // Layer 0: one central travel node splits three ways. Each branch serves two neighboring starts through spokes of
    // equal step count, so every start pays the same number of points to reach the center.
    const boundary = START_ANGLES.map((angle, j) => angle + 30 + [0, 2, -1, 1, -2, 0][j]);
    const hubGroup = group("hub", CENTER, "travel");
    const centerNode = travel(CENTER, hubGroup);
    const branches = [0, 2, 4].map(j => {
        const branch = travel(at(RING_RADII.hub, boundary[j]), hubGroup);
        link(centerNode, branch);
        return branch;
    });

    // Layer 1: the core ring joins each start's inward road to the boundary spokes.
    const inner = START_ANGLES.map((angle, i) => junction(at(RING_RADII.core + [-10, 25, -30, 15, -5, 30][i], angle + [2, -1, 0, 1, -2, 1][i])));
    const spokeRoots = boundary.map((angle, j) => junction(at(RING_RADII.core + [40, 15, 55, 30, 60, 20][j], angle)));
    const coreRing = inner.flatMap((node, i) => [node, spokeRoots[i]]);
    coreRing.forEach((node, k) => road(node, coreRing[(k + 1) % coreRing.length], { spacing: 78, curve: {} }));
    inner.forEach((node, i) => road(branches[Math.floor(i / 2)], node, { steps: 4 }));

    // Boundary spokes carry travel from the core to the middle ring between home regions.
    const spokes = boundary.map((angle, j) => {
        const q1 = junction(at([1360, 1420, 1310, 1390, 1340, 1430][j], angle + [0, 2, -1, 1, -2, 0][j]));
        const q2 = junction(at([1830, 1770, 1880, 1800, 1860, 1790][j], angle + [1, -1, 2, 0, -1, 1][j]));
        road(spokeRoots[j], q1, { spacing: 80 });
        road(q1, q2, { spacing: 82 });
        road(q2, mid(angle), { spacing: 82 });
        return { q1, q2 };
    });

    // Layer 2: six starts move outward. Each home region has its own road silhouette.
    const starters = starts.map((start, i) => {
        const theta = START_ANGLES[i];
        const point = at(RING_RADII.start + [30, -10, 40, 0, 50, 10][i], theta + [1, 0, -1, 1, 0, -1][i]);
        const starter = add({ id: `start_${start}`, kind: "STARTER", x: point.x, y: point.y, group: group("start", point, "start"), start });
        road(starter, inner[i], { steps: 2 });
        return starter;
    });
    const templates = [lattice, trident, loop, bridge, fan, star];
    starters.forEach((starter, i) => {
        const theta = START_ANGLES[i];
        templates[i]({
            S: starter,
            L: (radius, offset) => at(radius, theta + offset),
            around: (radius, offset) => at(radius, theta + offset, starter),
            mid: offset => mid(theta + offset),
            left: spokes[(i + 5) % 6],
            right: spokes[i],
            theta
        });
    });

    function lattice({ S, L, mid, left, right }) {
        const A = junction(L(1530, -12)), B = junction(L(1520, 13));
        const A2 = junction(L(1920, -13)), B2 = junction(L(1890, 14));
        road(S, A); road(S, B); road(A, A2); road(B, B2);
        road(A2, B2, { curve: {} });
        road(A2, mid(-15)); road(B2, mid(16));
        road(A, left.q1); road(B, right.q1); road(A2, left.q2); road(B2, right.q2);
    }
    function trident({ S, L, mid, left, right, theta }) {
        const A = junction(L(1530, -17)), B = junction(L(1530, 17));
        const H = roundabout(L(1900, 0), 92, 5, theta);
        road(S, A); road(S, B);
        road(A, port(H, A)); road(B, port(H, B));
        const outward = mid(0);
        road(port(H, outward), outward);
        road(A, mid(-16)); road(B, mid(16));
        road(A, left.q1); road(B, right.q1);
    }
    function loop({ S, around, mid, left, right }) {
        const A = junction(around(340, -64)), B = junction(around(340, 64)), T = junction(around(360, 0));
        road(S, A); road(S, B);
        road(A, T, { curve: { origin: S } }); road(T, B, { curve: { origin: S } });
        road(T, mid(2));
        road(A, left.q1); road(B, right.q1);
        road(A, mid(-18)); road(B, right.q2);
    }
    function bridge({ S, L, mid, left, right, theta }) {
        const A = junction(L(1480, -15)), B = junction(L(1470, 15)), X = junction(L(1600, 0));
        const H = roundabout(L(1980, -2), 96, 6, theta + 30);
        road(S, A); road(S, B);
        road(A, X, { curve: {} }); road(X, B, { curve: {} });
        road(X, port(H, X));
        const west = mid(-17), east = mid(16);
        road(port(H, west), west); road(port(H, east), east);
        road(A, left.q1); road(B, right.q1); road(A, left.q2);
    }
    function fan({ S, L, mid, left, right }) {
        const A = junction(L(1480, -19)), B = junction(L(1440, 15)), F = junction(L(1790, 2)), A2 = junction(L(1910, -20));
        road(S, A); road(S, B);
        road(A, F); road(B, F);
        road(F, mid(-3)); road(F, mid(15));
        road(A, A2); road(A2, mid(-17)); road(A2, left.q2);
        road(A, left.q1); road(B, right.q1); road(B, right.q2);
    }
    function star({ S, L, mid, left, right, theta }) {
        const A = junction(L(1490, -14)), B = junction(L(1490, 14));
        const H = roundabout(L(1850, 1), 125, 7, theta);
        road(S, A); road(S, B);
        road(A, port(H, A)); road(B, port(H, B));
        const west = mid(-10), east = mid(12);
        road(port(H, west), west); road(port(H, east), east);
        road(port(H, left.q2), left.q2); road(port(H, right.q2), right.q2);
        road(A, left.q1); road(B, right.q1);
    }

    // Layer 3 and 4: outer star hubs and bowed climbs link the middle ring to the perimeter road.
    const outerHubs = [-161, -97, -29, 49, 117].map((angle, k) => {
        const H = roundabout(at(2700, angle), 86, 5, angle + 18);
        const from = mid(angle + 1);
        road(from, port(H, from));
        const west = rim(angle - 10), east = rim(angle + 10);
        road(port(H, west), west, { bow: k % 2 ? 36 : -36 }); road(port(H, east), east, { bow: k % 2 ? -36 : 36 });
        return H;
    });
    [-128, -64, 5, 84, 146, 171].forEach((angle, k) => road(mid(angle), rim(angle + 3), { bow: [55, -60, 50, -55, 60, -45][k] }));
    // The middle ring is an irregular polygon of straight roads; the perimeter road follows curved arcs.
    const ringRoad = (list, spacing, curve) => {
        list.sort((a, b) => a.angle - b.angle);
        list.forEach((node, k) => road(node, list[(k + 1) % list.length], { spacing, curve }));
    };
    ringRoad(middle, 88, null);
    ringRoad(outer, 98, {});

    // Reward pockets and keystones branch into space that roads leave open.
    const pockets = [], keystoneNodes = [];
    const itemsNear = (x0, y0, x1, y1) => ({
        nodes: nodes.filter(n => n.x >= x0 && n.x <= x1 && n.y >= y0 && n.y <= y1),
        links: links.filter(([a, b]) => {
            const p = byId.get(a), q = byId.get(b);
            return Math.max(p.x, q.x) >= x0 && Math.min(p.x, q.x) <= x1 && Math.max(p.y, q.y) >= y0 && Math.min(p.y, q.y) <= y1;
        }).map(([a, b]) => [byId.get(a), byId.get(b)])
    });
    const fits = (gate, proposed, segments) => {
        const xs = [gate, ...proposed].map(p => p.x), ys = [gate, ...proposed].map(p => p.y);
        const near = itemsNear(Math.min(...xs) - 80, Math.min(...ys) - 80, Math.max(...xs) + 80, Math.max(...ys) + 80);
        for (let i = 0; i < proposed.length; i++) {
            const p = proposed[i];
            for (const n of near.nodes) {
                const spacing = n.kind === "TRAVEL" || n.kind === "STARTER" ? 16 : 34;
                if (distance(p, n) < RADIUS[p.kind] + RADIUS[n.kind] + spacing) return false;
            }
            if (proposed.slice(0, i).some(n => distance(p, n) < RADIUS[p.kind] + RADIUS[n.kind] + 13)) return false;
            if (p.kind === "KEYSTONE" && keystoneNodes.some(k => distance(p, byId.get(k.id)) < KEYSTONE_SPACING)) return false;
            if (near.links.some(([a, b]) => segmentDistance(p, a, b) < RADIUS[p.kind] + 18)) return false;
        }
        for (const [a, b] of segments) {
            if ([...near.nodes, ...proposed].some(n => n !== a && n !== b && segmentDistance(n, a, b) < RADIUS[n.kind] + 9)) return false;
            if (near.links.some(([c, d]) => ![a, b].includes(c) && ![a, b].includes(d) && crossing(a, b, c, d))) return false;
        }
        for (let i = 0; i < segments.length; i++) for (let j = i + 1; j < segments.length; j++) {
            const [a, b] = segments[i], [c, d] = segments[j];
            if (![a, b].includes(c) && ![a, b].includes(d) && crossing(a, b, c, d)) return false;
        }
        return true;
    };
    const blocked = new Set();
    const propose = (gate, shapeName, angle, side, scale) => {
        const shape = SHAPES[shapeName], theta = toRadians(angle);
        const proposed = shape.nodes.map(([kind, x, y]) => {
            const lx = x * scale, ly = y * scale * side;
            return { kind, x: gate.x + Math.cos(theta) * lx - Math.sin(theta) * ly, y: gate.y + Math.sin(theta) * lx + Math.cos(theta) * ly };
        });
        const segments = [...shape.gateLinks.map(i => [gate, proposed[i]]), ...shape.links.map(([a, b]) => [proposed[a], proposed[b]])];
        return { proposed, segments };
    };
    const place = (gate, shapeName, attempt) => {
        const shape = SHAPES[shapeName], center = attempt.proposed.reduce((sum, p) => ({ x: sum.x + p.x / attempt.proposed.length, y: sum.y + p.y / attempt.proposed.length }), { x: 0, y: 0 });
        const pocketId = `pocket_${String(pockets.length + 1).padStart(3, "0")}`;
        const groupId = group("reward", center, "reward", { shape: shapeName, gate: gate.id });
        const created = attempt.proposed.map((p, slot) => add({ id: `${pocketId}_${slot}`, kind: p.kind, x: p.x, y: p.y, group: groupId, pocket: pocketId }));
        shape.gateLinks.forEach(i => link(gate, created[i]));
        shape.links.forEach(([a, b]) => link(created[a], created[b]));
        const angle = angleOf(center), radius = radiusOf(center);
        const layer = radius < (RING_RADII.core + 120) * SCALE ? "core" : radius < middleRadius(angle) ? "home" : radius < outerRadius(angle) ? "outer" : "perimeter";
        pockets.push({ id: pocketId, group: groupId, shape: shapeName, gate: gate.id, layer, angle, radius, nodes: created.map(n => n.id) });
        blocked.add(gate.id);
        for (const id of adjacency.get(gate.id)) blocked.add(id);
        return created;
    };
    // A used gate blocks its travel neighbors, so distinct gates keep at least two travel edges between them.
    const available = (gate, shapeName) => gate.kind === "TRAVEL" && !blocked.has(gate.id)
        && degree(gate) + SHAPES[shapeName].gateLinks.length <= 4;
    const tryPlace = (gate, direction, shapeNames, turns = [0, 12, -12, 24, -24, 36, -36]) => {
        for (const shapeName of shapeNames) {
            if (!available(gate, shapeName)) continue;
            for (const turn of turns) for (const side of [1, -1]) for (const scale of [1, 0.92, 1.1]) {
                const attempt = propose(gate, shapeName, direction + turn, side, scale);
                if (fits(gate, attempt.proposed, attempt.segments)) return place(gate, shapeName, attempt);
            }
        }
        return null;
    };

    // Keystones are spread through every layer, so builds can commit inward as well as outward.
    const startDistance = new Map();
    for (const starter of starters) {
        const depth = new Map([[starter.id, 0]]), queue = [starter.id];
        for (let i = 0; i < queue.length; i++) for (const next of adjacency.get(queue[i])) {
            if (depth.has(next) || byId.get(next).kind !== "TRAVEL") continue;
            depth.set(next, depth.get(queue[i]) + 1);
            queue.push(next);
        }
        for (const [id, value] of depth) startDistance.set(id, Math.min(startDistance.get(id) ?? Infinity, value));
    }
    if (KEYSTONE_TARGETS.length + outerHubs.length + 1 !== keystones) throw new Error("Keystone targets do not match the keystone count");
    const keystoneDepth = { keyDirect: 1, keyArc: 4, keyFork: 4 };
    const gapDirections = node => {
        const angles = [...adjacency.get(node.id)].map(id => angleOf(byId.get(id), node)).sort((a, b) => a - b);
        return angles.flatMap((angle, i) => {
            const next = i + 1 < angles.length ? angles[i + 1] : angles[0] + 360;
            return next - angle >= 100 ? [angle + (next - angle) / 2] : [];
        });
    };
    const placeKeystone = (gates, directionsOf, shapes, turns, layer, extra) => {
        for (const gate of gates) for (const direction of directionsOf(gate)) {
            const allowed = shapes.filter(name => startDistance.get(gate.id) + keystoneDepth[name] >= KEYSTONE_START_DISTANCE);
            const created = tryPlace(gate, direction, allowed, turns);
            if (created) return keystoneNodes.push({ id: created.find(n => n.kind === "KEYSTONE").id, layer, ...(extra ? { extra } : {}) });
        }
        return 0;
    };
    // The center keystone hangs directly from the center node and faces the chosen start when distances tie.
    const facing = starters.find(s => s.start === centerFacing);
    const towardFacing = gate => gapDirections(gate).sort((a, b) => Math.abs(normalize(a - angleOf(facing, gate))) - Math.abs(normalize(b - angleOf(facing, gate))));
    if (!placeKeystone([centerNode], towardFacing, ["keyDirect"], [0, 10, -10], "center")) throw new Error("Cannot place the center keystone");
    for (const H of outerHubs) {
        placeKeystone(H.filter(n => degree(n) === 2), n => [angleOf(n, H.center)], ["keyDirect"], [0, 20, -20, 40, -40], "outer");
    }
    KEYSTONE_TARGETS.forEach(({ layer, radius, angle, shapes }) => {
        const point = at(radius, angle);
        const toward = gate => gapDirections(gate).sort((a, b) => Math.abs(normalize(a - angleOf(point, gate))) - Math.abs(normalize(b - angleOf(point, gate))));
        for (const reach of [260, 420]) {
            const gates = nodes.filter(n => n.kind === "TRAVEL" && distance(n, point) < reach).sort((a, b) => distance(a, point) - distance(b, point));
            if (placeKeystone(gates, toward, shapes, [0, 15, -15, 30, -30], layer)) return;
        }
        throw new Error(`Cannot place ${layer} keystone near ${angle} degrees`);
    });

    // Remaining pockets fill the widest open spaces first, so reward density follows the road cells.
    const candidates = [];
    for (const node of nodes.filter(n => n.kind === "TRAVEL")) {
        const angles = [...adjacency.get(node.id)].map(id => angleOf(byId.get(id), node)).sort((a, b) => a - b);
        angles.forEach((angle, i) => {
            const next = i + 1 < angles.length ? angles[i + 1] : angles[0] + 360;
            if (next - angle >= 110) candidates.push({ gate: node, direction: angle + (next - angle) / 2, priority: random() });
        });
    }
    // Open space ahead of a gate, ignoring the road the gate itself sits on.
    const clearance = candidate => {
        const probe = polar(150, candidate.direction, candidate.gate);
        const own = new Set([candidate.gate.id]);
        for (const id of adjacency.get(candidate.gate.id)) {
            if (byId.get(id).kind !== "TRAVEL") continue;
            own.add(id);
            for (const next of adjacency.get(id)) if (byId.get(next).kind === "TRAVEL") own.add(next);
        }
        const reach = 320;
        const near = itemsNear(probe.x - reach, probe.y - reach, probe.x + reach, probe.y + reach);
        let best = reach;
        for (const n of near.nodes) if (!own.has(n.id)) best = Math.min(best, distance(probe, n) - RADIUS[n.kind]);
        for (const [a, b] of near.links) if (!own.has(a.id) || !own.has(b.id)) best = Math.min(best, segmentDistance(probe, a, b));
        const perimeter = radiusOf(probe) > outerRadius(angleOf(probe)) + 20;
        return perimeter ? Math.min(best, 150) : best;
    };
    candidates.forEach(candidate => { candidate.clearance = clearance(candidate); });
    // Large cells rotate through the showpiece shapes; tighter spaces fall back to compact pockets.
    const large = [["star", "crown", "wheel"], ["crown", "lattice", "ring"], ["lattice", "star", "wheel"], ["wheel", "crown", "lattice"]];
    const medium = [["wheel", "chain", "ring", "horseshoe"], ["ring", "lattice", "horseshoe", "fork"], ["horseshoe", "wheel", "chain", "fork"], ["chain", "fork", "ring", "wheel"]];
    const small = [["chain", "fork", "diamond", "horseshoe"], ["fork", "horseshoe", "arc", "diamond"], ["diamond", "chain", "horseshoe", "arc"], ["horseshoe", "arc", "fork", "diamond"]];
    const target = totalNodes - nodes.length;
    let placedRewards = 0, turn = 0;
    while (placedRewards < target) {
        const live = candidates.filter(c => !c.dead && !blocked.has(c.gate.id));
        if (!live.length) break;
        const candidate = live.reduce((best, c) => c.clearance + c.priority * 8 > best.clearance + best.priority * 8 ? c : best);
        if (candidate.clearance < 40) break;
        // Never leave a remainder smaller than the smallest pocket, so the catalog lands on its exact size.
        const remaining = target - placedRewards;
        let order = candidate.clearance >= 215 ? large[turn++ % large.length]
            : candidate.clearance >= 150 ? medium[turn++ % medium.length]
                : candidate.clearance >= 110 ? small[turn++ % small.length] : ["diamond", "arc"];
        order = [...order, "fork", "diamond", "arc"].filter((name, i, all) => all.indexOf(name) === i
            && SHAPES[name].nodes.length <= remaining && ![1, 2].includes(remaining - SHAPES[name].nodes.length));
        const created = tryPlace(candidate.gate, candidate.direction, order);
        if (!created) { candidate.dead = true; continue; }
        placedRewards += created.length;
        for (const other of candidates) if (!other.dead && distance(other.gate, candidate.gate) < 700) other.clearance = clearance(other);
    }

    // Later additions are placed after the original fill, so every earlier node keeps its ID and position.
    for (const extra of extras) {
        const point = at(extra.radius, extra.angle);
        const toward = gate => gapDirections(gate).sort((a, b) => Math.abs(normalize(a - angleOf(point, gate))) - Math.abs(normalize(b - angleOf(point, gate))));
        const turns = [0, 12, -12, 24, -24, 36, -36];
        let placed = false;
        for (const reach of [260, 420, 600]) {
            const gates = nodes.filter(n => n.kind === "TRAVEL" && distance(n, point) < reach).sort((a, b) => distance(a, point) - distance(b, point));
            if (extra.keystone) {
                placed = placeKeystone(gates, toward, extra.shapes, turns, extra.layer, extra.id) > 0;
            } else {
                for (const gate of gates) {
                    for (const direction of toward(gate)) if (!placed && tryPlace(gate, direction, extra.shapes, turns)) placed = true;
                    if (placed) break;
                }
            }
            if (placed) break;
        }
        if (!placed) throw new Error(`Cannot place ${extra.id}`);
        pockets.at(-1).extra = extra.id;
    }

    for (const node of nodes) { node.x = Math.round(node.x); node.y = Math.round(node.y); }
    return { nodes, links, groups, pockets, keystones: keystoneNodes, starters: starters.map(s => s.id) };
}
