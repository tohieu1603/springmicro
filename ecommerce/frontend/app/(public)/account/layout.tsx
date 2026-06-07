import { redirect } from "next/navigation";
import { Container } from "@/components/ui/container";
import { getSession } from "@/lib/auth/session";
import { AccountNav } from "./_layout-nav";

export default async function AccountLayout({ children }: { children: React.ReactNode }) {
  const user = await getSession();
  if (!user) redirect("/login?next=/account");

  return (
    <Container className="py-8 grid grid-cols-1 lg:grid-cols-[260px,1fr] gap-8">
      <AccountNav user={user} />
      <div>{children}</div>
    </Container>
  );
}
