import { Link } from "react-router-dom";

/** items: [{ label, to? }] — the last item is rendered as the current page (no link). */
export default function Breadcrumbs({ items = [] }) {
  return (
    <nav aria-label="Breadcrumb">
      <ol className="breadcrumb mb-0 small">
        {items.map((item, i) => {
          const isLast = i === items.length - 1;
          return (
            <li
              key={item.label}
              className={`breadcrumb-item${isLast ? " active" : ""}`}
              aria-current={isLast ? "page" : undefined}
            >
              {isLast || !item.to ? item.label : <Link to={item.to}>{item.label}</Link>}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
