import express from "express";

const app = express();
const port = process.env.PORT || 8080;

const basePrices = {
  AAPL: 185.32,
  MSFT: 402.18,
  AMZN: 173.44,
  GOOGL: 142.75,
  TSLA: 193.21
};

function resolvePrice(symbol) {
  if (basePrices[symbol]) return basePrices[symbol];
  // deterministic fallback: 100 + (char sum % 100)
  const sum = symbol.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  return Number((100 + (sum % 100) + 0.32).toFixed(2));
}

app.get("/api/price", (req, res) => {
  const symbol = (req.query.symbol || "").toString().trim().toUpperCase();
  if (!symbol) {
    return res.status(400).json({ error: "symbol is required" });
  }
  const price = resolvePrice(symbol);
  return res.json({ symbol, price });
});

app.listen(port, () => {
  console.log(`Mock stock API running on port ${port}`);
});