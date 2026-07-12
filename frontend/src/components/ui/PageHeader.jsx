import Breadcrumbs from "./Breadcrumbs";

/** Consistent page title block: breadcrumb trail, title, optional description, and trailing actions. */
export default function PageHeader({ breadcrumbs, title, description, actions }) {
  return (
    <header className="mb-4">
      {breadcrumbs && <div className="mb-2"><Breadcrumbs items={breadcrumbs} /></div>}
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <h1 className="h4 fw-semibold mb-1" style={{ color: "var(--vc-text)" }}>{title}</h1>
          {description && <p className="mb-0 small vc-text-muted">{description}</p>}
        </div>
        {actions && <div className="d-flex gap-2 flex-wrap">{actions}</div>}
      </div>
    </header>
  );
}
