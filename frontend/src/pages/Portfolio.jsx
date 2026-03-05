import { useEffect, useMemo, useState } from "react";
import { addHolding, getPortfolio } from "../services/portfolioApi";

export default function Portfolio() {
  const [portfolio, setPortfolio] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const [symbol, setSymbol] = useState("");
  const [quantity, setQuantity] = useState("");
  const [price, setPrice] = useState("");

  useEffect(() => {
    refreshPortfolio();
  }, []);

  async function refreshPortfolio() {
    try {
      setLoading(true);
      const data = await getPortfolio();
      setPortfolio(data);
      setError("");
    } catch (e) {
      setError(e.message || "Failed to load portfolio");
    } finally {
      setLoading(false);
    }
  }

  async function handleAddHolding(e) {
    e.preventDefault();
    try {
      await addHolding({
        symbol,
        quantity: Number(quantity),
        price: Number(price)
      });
      setSymbol("");
      setQuantity("");
      setPrice("");
      refreshPortfolio();
    } catch (e) {
      setError(e.message || "Failed to add holding");
    }
  }

  const chartData = useMemo(() => {
    if (!portfolio?.holdings) return [];
    return portfolio.holdings.map(h => ({
      symbol: h.symbol,
      value: Number(h.marketValue || 0)
    }));
  }, [portfolio]);

  if (loading) {
    return <div style={{ padding: "2rem" }}>Loading portfolio...</div>;
  }

  return (
    <div style={{ maxWidth: 960, margin: "2rem auto", padding: "0 1rem" }}>
      <h2>Portfolio</h2>
      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <div style={{ marginBottom: "2rem" }}>
        <h3>Total Value</h3>
        <p>${portfolio?.totalValue ?? 0}</p>
      </div>

      <div style={{ marginBottom: "2rem" }}>
        <h3>Add Holding</h3>
        <form onSubmit={handleAddHolding} style={{ display: "grid", gap: "0.5rem", maxWidth: 360 }}>
          <input
            placeholder="Symbol (e.g., AAPL)"
            value={symbol}
            onChange={e => setSymbol(e.target.value)}
            required
          />
          <input
            placeholder="Quantity"
            type="number"
            step="0.0001"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
            required
          />
          <input
            placeholder="Price"
            type="number"
            step="0.0001"
            value={price}
            onChange={e => setPrice(e.target.value)}
            required
          />
          <button type="submit">Add Holding</button>
        </form>
      </div>

      <div style={{ marginBottom: "2rem" }}>
        <h3>Holdings</h3>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th align="left">Symbol</th>
              <th align="right">Qty</th>
              <th align="right">Avg Price</th>
              <th align="right">Market Price</th>
              <th align="right">Market Value</th>
            </tr>
          </thead>
          <tbody>
            {portfolio?.holdings?.map(h => (
              <tr key={h.symbol}>
                <td>{h.symbol}</td>
                <td align="right">{h.quantity}</td>
                <td align="right">{h.avgPrice}</td>
                <td align="right">{h.marketPrice}</td>
                <td align="right">{h.marketValue}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div>
        <h3>Portfolio Chart</h3>
        <SimpleBarChart data={chartData} />
      </div>
    </div>
  );
}

function SimpleBarChart({ data }) {
  const width = 720;
  const height = 240;
  const max = Math.max(...data.map(d => d.value), 1);
  const barWidth = data.length ? width / data.length : width;

  return (
    <svg width={width} height={height} style={{ border: "1px solid #ccc" }}>
      {data.map((d, i) => {
        const barHeight = (d.value / max) * (height - 40);
        const x = i * barWidth + 10;
        const y = height - barHeight - 20;

        return (
          <g key={d.symbol}>
            <rect
              x={x}
              y={y}
              width={barWidth - 20}
              height={barHeight}
              fill="#3b82f6"
            />
            <text x={x} y={height - 5} fontSize="12">{d.symbol}</text>
          </g>
        );
      })}
    </svg>
  );
}