import StatusPage from "./StatusPage";

export default function Forbidden() {
  return (
    <StatusPage
      code={403}
      icon="bi-shield-lock"
      title="Access restricted"
      description="Your account doesn't have permission to view this page. If you believe this is a mistake, contact your administrator."
    />
  );
}
