import { useEffect, useMemo, useState } from "react";
import { addHolding, getPortfolio } from "../services/portfolioApi";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import Card, { CardHeader } from "../components/ui/Card";
import PageHeader from "../components/ui/PageHeader";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import StatusChip from "../components/ui/StatusChip";
import EmptyState from "../components/ui/EmptyState";
import { SkeletonCard, SkeletonTableRows } from "../components/ui/Skeleton";
import { useToast } from "../components/ui/toastContext";

const EMPTY_HOLDINGS = [];

function formatMoney(value, currency = "USD") {
  return Number(value ?? 0).toLocaleString(undefined, { style: "currency", currency, maximumFractionDigits: 2 });
}

function gainLoss(holding) {
  const cost = Number(holding.avgPrice) * Number(holding.quantity);
  const value = Number(holding.marketValue);
  const gain = value - cost;
  const pct = cost !== 0 ? (gain / cost) * 100 : 0;
  return { gain, pct };
}

export default function Portfolio() {
  const [portfolio, setPortfolio] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

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
    setSubmitting(true);
    try {
      await addHolding({
        symbol: symbol.trim().toUpperCase(),
        quantity: Number(quantity),
        price: Number(price),
      });
      toast.push(`Added ${quantity} shares of ${symbol.trim().toUpperCase()}`, { tone: "success" });
      setSymbol("");
      setQuantity("");
      setPrice("");
      await refreshPortfolio();
    } catch (e) {
      setError(e.message || "Failed to add holding");
      toast.push(e.message || "Failed to add holding", { tone: "danger" });
    } finally {
      setSubmitting(false);
    }
  }

  const holdings = useMemo(() => portfolio?.holdings ?? EMPTY_HOLDINGS, [portfolio]);

  const chartData = useMemo(
    () => holdings.map((h) => ({ symbol: h.symbol, value: Number(h.marketValue || 0) })),
    [holdings]
  );

  const topHolding = useMemo(() => {
    if (!holdings.length) return null;
    return [...holdings].sort((a, b) => Number(b.marketValue) - Number(a.marketValue))[0];
  }, [holdings]);

  return (
    <div className="vc-fade-in">
      <PageHeader
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Portfolio" }]}
        title="Portfolio"
        description="Your holdings and live market valuation."
        actions={
          <Button variant="outline" icon="bi-arrow-clockwise" onClick={refreshPortfolio} busy={loading} busyLabel="Refreshing…">
            Refresh
          </Button>
        }
      />

      {error && <Alert tone="danger" className="mb-3">{error}</Alert>}

      <div className="row g-3 mb-3">
        <div className="col-12 col-md-4">
          <Card className="h-100">
            <CardHeader title="Total Value" icon="bi-cash-stack" />
            {loading ? <SkeletonCard lines={1} /> : (
              <div className="fs-3 fw-bold" style={{ color: "var(--vc-text)" }}>{formatMoney(portfolio?.totalValue)}</div>
            )}
          </Card>
        </div>
        <div className="col-12 col-md-4">
          <Card className="h-100">
            <CardHeader title="Holdings" icon="bi-collection" />
            {loading ? <SkeletonCard lines={1} /> : (
              <div className="fs-3 fw-bold" style={{ color: "var(--vc-text)" }}>{holdings.length}</div>
            )}
          </Card>
        </div>
        <div className="col-12 col-md-4">
          <Card className="h-100">
            <CardHeader title="Largest Position" icon="bi-star-fill" />
            {loading ? <SkeletonCard lines={1} /> : topHolding ? (
              <>
                <div className="fs-4 fw-bold" style={{ color: "var(--vc-text)" }}>{topHolding.symbol}</div>
                <div className="small vc-text-muted">{formatMoney(topHolding.marketValue)}</div>
              </>
            ) : <div className="small vc-text-muted">No holdings yet</div>}
          </Card>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-lg-7">
          <Card>
            <CardHeader title="Holdings" icon="bi-table" />
            <div className="vc-scroll-x">
              <table className="table align-middle mb-0">
                <thead>
                  <tr className="small vc-text-muted">
                    <th scope="col">Symbol</th>
                    <th scope="col" className="text-end">Qty</th>
                    <th scope="col" className="text-end">Avg Price</th>
                    <th scope="col" className="text-end">Market Price</th>
                    <th scope="col" className="text-end">Market Value</th>
                    <th scope="col" className="text-end">P&amp;L</th>
                  </tr>
                </thead>
                <tbody>
                  {loading && <SkeletonTableRows rows={3} cols={6} />}
                  {!loading && holdings.map((h) => {
                    const { gain, pct } = gainLoss(h);
                    const positive = gain >= 0;
                    return (
                      <tr key={h.symbol}>
                        <td className="fw-semibold">{h.symbol}</td>
                        <td className="text-end">{h.quantity}</td>
                        <td className="text-end">{formatMoney(h.avgPrice)}</td>
                        <td className="text-end">{formatMoney(h.marketPrice)}</td>
                        <td className="text-end fw-medium">{formatMoney(h.marketValue)}</td>
                        <td className="text-end">
                          <StatusChip tone={positive ? "success" : "danger"} icon={positive ? "bi-arrow-up-short" : "bi-arrow-down-short"}>
                            {positive ? "+" : ""}{pct.toFixed(1)}%
                          </StatusChip>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {!loading && holdings.length === 0 && (
              <EmptyState
                icon="bi-pie-chart"
                title="No holdings yet"
                description="Add your first holding using the form to start tracking your portfolio."
              />
            )}
          </Card>

          {!loading && holdings.length > 0 && (
            <Card className="mt-3">
              <CardHeader title="Allocation by Market Value" icon="bi-bar-chart-fill" />
              <div style={{ height: 280 }}>
                <ResponsiveContainer>
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--vc-slate-200)" />
                    <XAxis dataKey="symbol" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => formatMoney(v)} />
                    <Bar dataKey="value" fill="var(--vc-blue-600)" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
          )}
        </div>

        <div className="col-12 col-lg-5">
          <Card>
            <CardHeader title="Add Holding" icon="bi-plus-circle-fill" />
            <form onSubmit={handleAddHolding} className="d-flex flex-column gap-3">
              <div>
                <label htmlFor="holding-symbol" className="form-label small vc-text-muted mb-1">Symbol</label>
                <input
                  id="holding-symbol"
                  className="form-control text-uppercase"
                  placeholder="AAPL"
                  value={symbol}
                  onChange={(e) => setSymbol(e.target.value)}
                  required
                />
              </div>
              <div>
                <label htmlFor="holding-qty" className="form-label small vc-text-muted mb-1">Quantity</label>
                <input
                  id="holding-qty"
                  className="form-control"
                  type="number"
                  step="0.0001"
                  min="0"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                />
              </div>
              <div>
                <label htmlFor="holding-price" className="form-label small vc-text-muted mb-1">Price</label>
                <input
                  id="holding-price"
                  className="form-control"
                  type="number"
                  step="0.0001"
                  min="0"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" busy={submitting} busyLabel="Adding…" icon="bi-plus-lg" className="justify-content-center">
                Add Holding
              </Button>
            </form>
          </Card>
        </div>
      </div>
    </div>
  );
}
