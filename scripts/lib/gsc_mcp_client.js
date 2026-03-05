const { spawn } = require("child_process");
const fs = require("fs");
const path = require("path");

function resolveDefaultMcpEntry() {
    const appData = process.env.APPDATA;
    if (!appData) {
        return null;
    }
    const defaultPath = path.join(
        appData,
        "npm",
        "node_modules",
        "search-console-mcp",
        "dist",
        "index.js"
    );
    return fs.existsSync(defaultPath) ? defaultPath : null;
}

function parseToolText(result) {
    const text = (result && result.content || []).find((item) => item.type === "text")?.text || "";
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

function createGscMcpClient() {
    const explicitEntry = process.env.SEARCH_CONSOLE_MCP_ENTRY;
    const mcpEntry = explicitEntry || resolveDefaultMcpEntry();
    if (!mcpEntry) {
        throw new Error("Unable to find search-console-mcp entry. Set SEARCH_CONSOLE_MCP_ENTRY.");
    }

    const child = spawn(process.execPath, [mcpEntry], {
        stdio: ["pipe", "pipe", "pipe"],
        env: { ...process.env },
        windowsHide: true
    });

    let buffer = "";
    let nextId = 1;
    const pending = new Map();

    child.on("exit", (code, signal) => {
        if (pending.size > 0) {
            for (const [, handler] of pending) {
                handler.reject(new Error(`MCP process exited (code=${code}, signal=${signal})`));
            }
            pending.clear();
        }
    });

    child.stderr.on("data", () => {
        // Ignore stderr chatter from MCP transport.
    });

    child.stdout.on("data", (chunk) => {
        buffer += chunk.toString("utf8");
        while (true) {
            const newlineIndex = buffer.indexOf("\n");
            if (newlineIndex === -1) {
                break;
            }
            const line = buffer.slice(0, newlineIndex).replace(/\r$/, "");
            buffer = buffer.slice(newlineIndex + 1);
            if (!line.trim()) {
                continue;
            }
            const message = JSON.parse(line);
            if (Object.prototype.hasOwnProperty.call(message, "id")) {
                const handler = pending.get(message.id);
                if (!handler) {
                    continue;
                }
                pending.delete(message.id);
                if (message.error) {
                    handler.reject(new Error(JSON.stringify(message.error)));
                } else {
                    handler.resolve(message.result);
                }
            }
        }
    });

    function send(payload) {
        child.stdin.write(JSON.stringify(payload) + "\n");
    }

    function request(method, params) {
        const id = nextId++;
        return new Promise((resolve, reject) => {
            pending.set(id, { resolve, reject });
            send({ jsonrpc: "2.0", id, method, params });
        });
    }

    async function initialize() {
        await request("initialize", {
            protocolVersion: "2024-11-05",
            capabilities: {},
            clientInfo: { name: "offerverdict-gsc", version: "1.0.0" }
        });
        send({ jsonrpc: "2.0", method: "notifications/initialized", params: {} });
    }

    async function callTool(name, args) {
        const result = await request("tools/call", { name, arguments: args });
        return parseToolText(result);
    }

    function close() {
        child.kill();
    }

    return { initialize, callTool, close };
}

module.exports = {
    createGscMcpClient
};
