import StatusPage from "./StatusPage";
import Button from "../../components/ui/Button";

/** Rendered by ErrorBoundary when a render-time error is caught — offers a reload instead of a dead end. */
export default function ServerError({ onRetry }) {
  return (
    <StatusPage
      code={500}
      icon="bi-exclamation-octagon"
      title="Something went wrong"
      description="An unexpected error occurred. Your data is safe — try reloading the page, or contact support if the problem persists."
      primaryAction={
        <Button onClick={onRetry || (() => window.location.reload())} icon="bi-arrow-clockwise">
          Reload page
        </Button>
      }
    />
  );
}
