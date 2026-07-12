import { Link } from "react-router-dom";
import AdminProvisioning from "./AdminProvisioning";

export default function AdminUsersPage() {
  return (
    <>
      <div style={{ maxWidth: 920, margin: "1rem auto", padding: "0 1rem", display: "flex", gap: 12 }}>
        <Link to="/admin/users">Users</Link>
        <Link to="/admin/accounts">Accounts</Link>
      </div>
      <AdminProvisioning />
    </>
  );
}