import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { buildModifierData } from "./export-modifier-data.mjs";
import { buildGuiLayoutData } from "./export-gui-layout-data.mjs";
import { buildPassiveTreeData } from "./export-passive-tree-data.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const WEB_ROOT = path.join(__dirname, "web");
const GENERATED_ROOT = path.join(__dirname, "generated");
const START_PORT = Number(globalThis.process?.env?.PORT ?? 4177);
const APP_ROUTES = new Set(["/modifiers", "/rbom", "/stages", "/passive-trees", "/gui"]);

const mimeTypes = new Map([
    [".html", "text/html; charset=utf-8"],
    [".css", "text/css; charset=utf-8"],
    [".js", "text/javascript; charset=utf-8"],
    [".json", "application/json; charset=utf-8"],
    [".svg", "image/svg+xml; charset=utf-8"]
]);

export async function startModdexServer({ port = START_PORT, host = "127.0.0.1", log = true } = {}) {
    await buildModifierData();
    await buildGuiLayoutData();
    await buildPassiveTreeData();
    return listenWithFallback(port, host, log, port + 20);
}

export function createModdexServer() {
    return createServer(handleRequest);
}

async function listenWithFallback(port, host, log, maxPort) {
    return new Promise((resolve, reject) => {
        const server = createModdexServer();
        server.once("error", async (error) => {
            if (error.code === "EADDRINUSE" && port < maxPort) {
                try {
                    resolve(await listenWithFallback(port + 1, host, log, maxPort));
                } catch (fallbackError) {
                    reject(fallbackError);
                }
                return;
            }
            reject(error);
        });
        server.listen(port, host, () => {
            const address = server.address();
            const selectedPort = typeof address === "object" && address ? address.port : port;
            const url = `http://${host}:${selectedPort}/`;
            if (log) {
                console.log(`RNGTech Moddex running at ${url}`);
            }
            resolve({ server, port: selectedPort, url });
        });
    });
}

async function handleRequest(request, response) {
    try {
        const url = new URL(request.url, "http://127.0.0.1");
        const filePath = resolveFilePath(url.pathname);
        const fileStat = await stat(filePath);
        if (!fileStat.isFile()) {
            send(response, 404, "Not found");
            return;
        }
        const body = await readFile(filePath);
        response.writeHead(200, {
            "Content-Type": mimeTypes.get(path.extname(filePath)) ?? "application/octet-stream",
            "Cache-Control": "no-store"
        });
        response.end(body);
    } catch (error) {
        send(response, 404, "Not found");
    }
}

function resolveFilePath(pathname) {
    const cleanPath = decodeURIComponent(pathname).replaceAll("\\", "/");
    const appPath = cleanPath.replace(/\/+$/, "") || "/";
    if (appPath === "/" || appPath === "/index.html" || APP_ROUTES.has(appPath)) {
        return path.join(WEB_ROOT, "index.html");
    }
    if (cleanPath.startsWith("/generated/")) {
        return safeJoin(GENERATED_ROOT, cleanPath.slice("/generated/".length));
    }
    return safeJoin(WEB_ROOT, cleanPath.replace(/^\//, ""));
}

function safeJoin(root, relativePath) {
    const target = path.resolve(root, relativePath);
    if (!target.startsWith(path.resolve(root))) {
        throw new Error("Path escapes web root.");
    }
    return target;
}

function send(response, status, body) {
    response.writeHead(status, { "Content-Type": "text/plain; charset=utf-8" });
    response.end(body);
}

if (isDirectRun()) {
    await startModdexServer();
}

function isDirectRun() {
    return typeof process !== "undefined"
        && process.argv?.[1]
        && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
}
