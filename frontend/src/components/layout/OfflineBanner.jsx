import { useEffect, useState } from "react";

/** Global banner shown whenever the browser reports it has lost network connectivity. */
export default function OfflineBanner() {
  const [online, setOnline] = useState(navigator.onLine);

  useEffect(() => {
    const goOnline = () => setOnline(true);
    const goOffline = () => setOnline(false);
    window.addEventListener("online", goOnline);
    window.addEventListener("offline", goOffline);
    return () => {
      window.removeEventListener("online", goOnline);
      window.removeEventListener("offline", goOffline);
    };
  }, []);

  if (online) return null;

  return (
    <div
      className="text-center small fw-medium py-2 px-3"
      style={{ background: "var(--vc-amber-500)", color: "#111" }}
      role="alert"
    >
      <i className="bi bi-wifi-off me-2" aria-hidden="true" />
      You're offline. Some actions won't work until your connection is restored.
    </div>
  );
}
