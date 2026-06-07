import { redirect } from "next/navigation";
import { Container } from "@/components/ui/container";
import { getSession } from "@/lib/auth/session";

import { AccountTiles } from "./components";
import { LogoutButton } from "./logout-button";

export default async function AccountHome() {
  const user = await getSession();
  if (!user) redirect("/login?next=/account");

  return (
    <Container className="py-8">
      <h1 className="text-h2-d">Xin chào, {user.fullName || user.username}</h1>
      <p className="text-sm text-slate mt-1">{user.email}</p>

      <AccountTiles />

      <div className="mt-6">
        <LogoutButton />
      </div>
    </Container>
  );
}
