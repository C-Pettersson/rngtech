// Stable node rewards are authored separately from roads and optional reward pockets.
export function layoutMegaTree(tree) {
    let seed = 0x71ac93;
    const random = () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; };
    const center = { x: 3000, y: 3000 };
    const distance = (a,b) => Math.hypot(a.x-b.x,a.y-b.y);
    const cross = (a,b,c) => (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);
    const crossing = (a,b,c,d) => cross(a,b,c)*cross(a,b,d)<0 && cross(c,d,a)*cross(c,d,b)<0;
    const segmentDistance = (p,a,b) => {
        const dx=b.x-a.x,dy=b.y-a.y,t=Math.max(0,Math.min(1,((p.x-a.x)*dx+(p.y-a.y)*dy)/(dx*dx+dy*dy)));
        return Math.hypot(p.x-a.x-t*dx,p.y-a.y-t*dy);
    };
    const points=[];
    for(let g=0;g<12;g++) {
        const angle=(g-.5)*Math.PI/6 + .025*Math.sin(g*1.7), radius=690+55*Math.sin(g*2.2);
        points.push({x:center.x+radius*Math.cos(angle),y:center.y+radius*Math.sin(angle)});
    }
    for(let attempt=0;points.length<60 && attempt<100000;attempt++) {
        const angle=random()*Math.PI*2, radius=950+random()*1580;
        const boundary=2350*(.91+.07*Math.sin(angle*3+.7)+.08*Math.cos(angle*5+.2));
        const point={x:center.x+radius*Math.cos(angle)*1.08,y:center.y+radius*Math.sin(angle)*.96};
        if(radius>boundary || points.some(p=>distance(p,point)<410))continue;
        points.push(point);
    }
    if(points.length!==60)throw new Error("Cannot place travel junctions");
    const candidates=[];
    for(let a=0;a<points.length;a++)for(let b=a+1;b<points.length;b++) {
        if(segmentDistance(center,points[a],points[b])<530)continue;
        if(points.some((p,i)=>i!==a&&i!==b&&segmentDistance(p,points[a],points[b])<160))continue;
        candidates.push({a,b,length:distance(points[a],points[b])});
    }
    candidates.sort((a,b)=>a.length-b.length);
    const roads=[], degree=points.map(()=>0), parent=points.map((_,i)=>i);
    const root=i=>parent[i]===i?i:(parent[i]=root(parent[i]));
    const canAdd=e=>degree[e.a]<(e.a<12?3:4)&&degree[e.b]<(e.b<12?3:4)&&!roads.some(other=>
        ![e.a,e.b].includes(other.a)&&![e.a,e.b].includes(other.b)&&crossing(points[e.a],points[e.b],points[other.a],points[other.b]));
    const addRoad=e=>{roads.push(e);degree[e.a]++;degree[e.b]++;parent[root(e.a)]=root(e.b);};
    for(const e of candidates)if(root(e.a)!==root(e.b)&&canAdd(e))addRoad(e);
    if(new Set(points.map((_,i)=>root(i))).size!==1)throw new Error("Disconnected travel roads");
    for(const e of candidates)if(roads.length<92&&!roads.includes(e)&&canAdd(e))addRoad(e);

    const rewards=tree.groups.map(group=>({...group,nodes:tree.nodes.filter(n=>n.group===group.id&&n.kind!=="TRAVEL")}));
    const pool=tree.nodes.filter(n=>n.kind==="TRAVEL"), nodes=new Map(tree.nodes.map(n=>[n.id,n]));
    const placed=[], links=[], adjacency=new Map(tree.nodes.map(n=>[n.id,[]]));
    tree.groups=[];
    const position=(node,point,group)=>{node.x=Math.round(point.x);node.y=Math.round(point.y);node.group=group;placed.push(node);return node;};
    const link=(a,b)=>{links.push([a.id,b.id]);adjacency.get(a.id).push(b.id);adjacency.get(b.id).push(a.id);};
    const group=(id,point,role,extra={})=>tree.groups.push({id,x:Math.round(point.x),y:Math.round(point.y),role,...extra});
    const travel=(point,id)=>{
        // Keep the attribute regions recognizable while retaining every stable ID and effect.
        const angle=(Math.atan2(point.y-center.y,point.x-center.x)+Math.PI*2)%(Math.PI*2);
        const sector=Math.round(angle/(Math.PI/3))%6;
        const region=tree.starts[sector];
        let index=pool.findIndex(n=>n.id.match(/^r\d+_(.*)_[01]_\d+$/)?.[1]===region);
        if(index<0)index=0;
        return position(pool.splice(index,1)[0],point,id);
    };
    const junctions=points.map((p,i)=>{const id="junction_"+i;group(id,p,"travel");return travel(p,id);});
    // Allocate intermediate attribute steps in proportion to road length.
    roads.forEach(e=>e.steps=2);
    let remaining=360-junctions.length-18-roads.length*2;
    while(remaining-->0)roads.reduce((best,e)=>e.length/(e.steps+1)>best.length/(best.steps+1)?e:best).steps++;
    const anchors=[];
    roads.forEach((e,index)=>{
        const a=junctions[e.a],b=junctions[e.b],id="road_"+index;
        group(id,{x:(a.x+b.x)/2,y:(a.y+b.y)/2},"travel");
        let previous=a;
        for(let step=1;step<=e.steps;step++) {
            const t=step/(e.steps+1),node=travel({x:a.x+(b.x-a.x)*t,y:a.y+(b.y-a.y)*t},id);
            anchors.push({node,angle:Math.atan2(b.y-a.y,b.x-a.x),road:index});
            link(previous,node);previous=node;
        }
        link(previous,b);
    });
    tree.nodes.filter(n=>n.kind==="STARTER").forEach((starter,s)=>{
        const angle=s*Math.PI/3,radius=255+[5,35,-15,15,0,-25][s],id=starter.group;
        position(starter,{x:center.x+radius*Math.cos(angle),y:center.y+radius*Math.sin(angle)},id);
        group(id,starter,"start");
        const exits=[];
        for(const hub of [junctions[s*2],junctions[s*2+1]]) {
            const node=travel({x:(starter.x+hub.x)/2,y:(starter.y+hub.y)/2},id);
            link(starter,node);link(node,hub);exits.push(node);
        }
        const a=exits[0],dx=a.x-starter.x,dy=a.y-starter.y,length=Math.hypot(dx,dy);
        const third=travel({x:(starter.x+a.x)/2+dy/length*95,y:(starter.y+a.y)/2-dx/length*95},id);
        link(starter,third);link(third,a);
    });
    if(pool.length)throw new Error("Unused travel nodes: "+pool.length);
    const radius=n=>({STARTER:20,TRAVEL:6,NODE:9,NOTABLE:17,KEYSTONE:24})[n.kind];
    for(const node of junctions.filter(n=>adjacency.get(n.id).length===1)) {
        const neighbor=nodes.get(adjacency.get(node.id)[0]);
        anchors.push({node,angle:Math.atan2(node.y-neighbor.y,node.x-neighbor.x),required:true});
    }
    const gates=new Set();
    const valid=(proposed,segments)=>{
        for(let i=0;i<proposed.length;i++) {
            const p=proposed[i];
            if(placed.some(n=>distance(p,n)<radius(p)+radius(n)+13))return false;
            if(proposed.slice(0,i).some(n=>distance(p,n)<radius(p)+radius(n)+13))return false;
            if(links.some(([a,b])=>segmentDistance(p,nodes.get(a),nodes.get(b))<radius(p)+8))return false;
        }
        for(const [a,b] of segments) {
            if([...placed,...proposed].some(n=>n.id!==a.id&&n.id!==b.id&&segmentDistance(n,a,b)<radius(n)+8))return false;
            if(links.some(([c,d])=>![a.id,b.id].includes(c)&&![a.id,b.id].includes(d)&&crossing(a,b,nodes.get(c),nodes.get(d))))return false;
        }
        for(let i=0;i<segments.length;i++)for(let j=i+1;j<segments.length;j++) {
            const [a,b]=segments[i],[c,d]=segments[j];
            if(![a.id,b.id].includes(c.id)&&![a.id,b.id].includes(d.id)&&crossing(a,b,c,d))return false;
        }
        return true;
    };
    const pockets=[];
    rewards.forEach((reward,i)=>{
        const ordinary=reward.nodes.filter(n=>n.kind==="NODE"),notables=reward.nodes.filter(n=>n.kind==="NOTABLE");
        if(i%2===0)pockets.push({id:reward.id,theme:reward.theme,region:reward.region,shape:i%4===0?"ring":"horseshoe",nodes:[ordinary[0],ordinary[1],notables[0],notables[1],ordinary[3],ordinary[2]]});
        else for(let j=0;j<2;j++)pockets.push({id:reward.id+"_"+j,theme:reward.theme,region:reward.region,shape:"arc",nodes:[ordinary[j*2],ordinary[j*2+1],notables[j]]});
    });
    // Larger pockets claim space first; each is an optional investment from one road gate.
    pockets.sort((a,b)=>b.nodes.length-a.nodes.length);
    const tips=[];
    for(const [index,pocket] of pockets.entries()) {
        let choice;
        const available=anchors.filter(a=>!gates.has(a.node.id)&&!adjacency.get(a.node.id).some(id=>gates.has(id)));
        // Seeded ordering avoids a repeated ring of identical reward groups.
        for(const a of available)a.priority=a.required?-1:random();
        available.sort((a,b)=>a.priority-b.priority);
        for(const anchor of available) {
            for(const side of [1,-1])for(const turn of [0,.3,-.3,.6,-.6])for(const scale of [1,.85,1.15]) {
                const theta=anchor.angle+side*Math.PI/2+turn,r=88*scale;
                const transform=(x,y)=>({x:Math.round(anchor.node.x+Math.cos(theta)*x-Math.sin(theta)*y),y:Math.round(anchor.node.y+Math.sin(theta)*x+Math.cos(theta)*y)});
                let proposed;
                if(pocket.shape==="arc") {
                    proposed=pocket.nodes.map((n,i)=>({...n,...transform((65+i*45)*scale,side*[0,28,82][i]*scale)}));
                } else {
                    proposed=pocket.nodes.map((n,i)=>{
                        const angle=Math.PI+(i+1)*Math.PI*2/7;
                        return {...n,...transform(r+r*Math.cos(angle),r*Math.sin(angle))};
                    });
                }
                const segments=pocket.shape==="arc"?[[anchor.node,proposed[0]],[proposed[0],proposed[1]],[proposed[1],proposed[2]]]:
                    [[anchor.node,proposed[0]],[proposed[0],proposed[1]],[proposed[1],proposed[2]],[anchor.node,proposed[5]],[proposed[5],proposed[4]],[proposed[4],proposed[3]]];
                if(pocket.shape==="ring")segments.push([proposed[2],proposed[3]]);
                if(!valid(proposed,segments))continue;
                choice={anchor,proposed,segments};break;
            }
            if(choice)break;
        }
        if(!choice)throw new Error("Cannot place reward pocket "+pocket.id+" ("+index+")");
        gates.add(choice.anchor.node.id);
        group(pocket.id,choice.proposed[Math.floor(choice.proposed.length/2)],"reward",{theme:pocket.theme,region:pocket.region,shape:pocket.shape,gate:choice.anchor.node.id});
        for(const proposed of choice.proposed)position(nodes.get(proposed.id),proposed,pocket.id);
        for(const [a,b] of choice.segments)link(nodes.get(a.id),nodes.get(b.id));
        for(const n of pocket.nodes.filter(n=>n.kind==="NOTABLE"&&adjacency.get(n.id).length===1))tips.push(n);
    }
    const keys=tree.nodes.filter(n=>n.kind==="KEYSTONE");
    // Spread commitments around the perimeter, continuing an invested reward arm.
    const usedTips=new Set();
    keys.forEach((key,k)=>{
        const targetAngle=k*Math.PI*2/keys.length;
        const candidates=tips.filter(n=>!usedTips.has(n.id)&&distance(n,center)>1500).sort((a,b)=>{
            const score=n=>distance(n,center)*.2+1500*Math.cos(Math.atan2(n.y-center.y,n.x-center.x)-targetAngle);
            return score(b)-score(a);
        });
        let choice;
        for(const tip of candidates) {
            const parent=nodes.get(adjacency.get(tip.id)[0]),outward=Math.atan2(tip.y-parent.y,tip.x-parent.x);
            for(const turn of [0,.4,-.4,.8,-.8,1.2,-1.2])for(const length of [90,120,160]) {
                const point={...key,x:Math.round(tip.x+length*Math.cos(outward+turn)),y:Math.round(tip.y+length*Math.sin(outward+turn))};
                if(valid([point],[[tip,point]])){choice={tip,point};break;}
            }
            if(choice)break;
        }
        if(!choice)throw new Error("Cannot place keystone "+key.id);
        position(key,choice.point,key.group);group(key.group,key,"keystone");
        link(choice.tip,key);usedTips.add(choice.tip.id);
    });
    tree.links=links;
    return tree;
}
