const VARIANTS = {
  primary: "vc-btn-brand btn",
  outline: "btn btn-outline-secondary",
  danger: "btn btn-danger",
  ghost: "btn btn-light",
};

/** Standard button with a built-in busy state (spinner + disabled) so every async action looks the same. */
export default function Button({
  variant = "primary",
  busy = false,
  busyLabel,
  icon,
  className = "",
  children,
  disabled,
  ...rest
}) {
  const cls = VARIANTS[variant] || VARIANTS.primary;
  return (
    <button className={`${cls} d-inline-flex align-items-center gap-2 ${className}`} disabled={disabled || busy} {...rest}>
      {busy && <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />}
      {!busy && icon && <i className={`bi ${icon}`} aria-hidden="true" />}
      <span>{busy && busyLabel ? busyLabel : children}</span>
    </button>
  );
}
