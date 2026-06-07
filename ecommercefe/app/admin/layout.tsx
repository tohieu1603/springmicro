import { redirect } from "next/navigation";
import { getSession, isAdmin } from "@/lib/auth/session";
import { AdminShell } from "./_components/shell";
import type { AuthUser } from "@/lib/api/types";

/**
 * Admin gate. SSR-checks the auth cookie + role. Non-admins get a hard
 * redirect (preserves SEO indexer behaviour) rather than a flash of admin UI.
 *
 * TEMP: ADMIN_AUTH_BYPASS=true trong .env.local cho phép truy cập admin không
 * cần BE/đăng nhập (dev only). Khi BE sẵn sàng, gỡ flag hoặc set =false để
 * khôi phục bảo vệ.
 */
const BYPASS = process.env.ADMIN_AUTH_BYPASS === "true";

const MOCK_USER: AuthUser = {
  id: "dev-admin",
  username: "dev-admin",
  email: "dev@local",
  fullName: "Dev Admin",
  roles: ["ADMIN"],
};

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  if (BYPASS) {
    return <AdminShell user={MOCK_USER}>{children}</AdminShell>;
  }
  const user = await getSession();
  if (!user) redirect("/login?next=/admin");
  if (!isAdmin(user)) redirect("/");
  return <AdminShell user={user}>{children}</AdminShell>;
}
