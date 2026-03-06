import test from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";

const PORT = 5055;
const BASE_URL = `http://localhost:${PORT}`;

function startServer() {
  return spawn("node", ["index.js"], {
    cwd: new URL("..", import.meta.url).pathname,
    env: { ...process.env, PORT: String(PORT) },
    stdio: "inherit"
  });
}

async function waitForServer() {
  for (let i = 0; i < 20; i++) {
    try {
      const res = await fetch(`${BASE_URL}/api/price?symbol=AAPL`);
      if (res.ok) return;
    } catch (e) {
      // ignore
    }
    await new Promise(r => setTimeout(r, 100));
  }
  throw new Error("Server did not start in time");
}

let serverProcess;

test.before(async () => {
  serverProcess = startServer();
  await waitForServer();
});

test.after(() => {
  if (serverProcess) serverProcess.kill("SIGTERM");
});

test("Endpoint test: GET /api/price?symbol=AAPL returns symbol and price", async () => {
  const res = await fetch(`${BASE_URL}/api/price?symbol=AAPL`);
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.symbol, "AAPL");
  assert.equal(typeof body.price, "number");
});

test("Unknown symbol returns fallback price", async () => {
  const res = await fetch(`${BASE_URL}/api/price?symbol=UNKNOWN`);
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.symbol, "UNKNOWN");
  assert.equal(typeof body.price, "number");
  assert.ok(body.price > 0);
});

test("Latency check below 300ms", async () => {
  const start = Date.now();
  const res = await fetch(`${BASE_URL}/api/price?symbol=AAPL`);
  const end = Date.now();
  assert.equal(res.status, 200);
  const duration = end - start;
  assert.ok(duration < 300, `Latency ${duration}ms exceeded 300ms`);
});

test("Deterministic price for same symbol within same run", async () => {
  const res1 = await fetch(`${BASE_URL}/api/price?symbol=MSFT`);
  const res2 = await fetch(`${BASE_URL}/api/price?symbol=MSFT`);
  const body1 = await res1.json();
  const body2 = await res2.json();

  assert.equal(body1.symbol, "MSFT");
  assert.equal(body2.symbol, "MSFT");
  assert.equal(body1.price, body2.price);
});