/** Loading placeholder — a shimmer block sized by the caller via width/height. */
export function Skeleton({ width = "100%", height = 16, radius = 6, className = "" }) {
  return (
    <span
      className={`d-inline-block vc-skeleton ${className}`}
      style={{ width, height, borderRadius: radius }}
      aria-hidden="true"
    />
  );
}

/** A ready-made skeleton for card-shaped content: title + a few lines. */
export function SkeletonCard({ lines = 3 }) {
  return (
    <div aria-busy="true" aria-live="polite">
      <span className="visually-hidden">Loading…</span>
      <Skeleton width="40%" height={14} className="mb-3" />
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} height={12} className="mb-2" width={i === lines - 1 ? "60%" : "100%"} />
      ))}
    </div>
  );
}

/** Skeleton rows for a table body while data loads. */
export function SkeletonTableRows({ rows = 3, cols = 5 }) {
  return (
    <>
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r} aria-hidden="true">
          {Array.from({ length: cols }).map((_, c) => (
            <td key={c}>
              <Skeleton height={12} />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}
