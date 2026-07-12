import StatusPage from "./StatusPage";

export default function NotFound() {
  return (
    <StatusPage
      code={404}
      icon="bi-signpost-split"
      title="Page not found"
      description="The page you're looking for doesn't exist or may have been moved."
    />
  );
}
