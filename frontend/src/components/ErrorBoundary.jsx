import { Component } from "react";
import ServerError from "../pages/errors/ServerError";

/**
 * Catches render-time errors anywhere in the tree so a single failing view degrades gracefully
 * instead of blanking the whole app.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    // In production this would report to an error-tracking service.
    console.error("UI error boundary caught an error:", error, info);
  }

  render() {
    if (this.state.hasError) {
      return <ServerError onRetry={() => window.location.reload()} />;
    }
    return this.props.children;
  }
}
