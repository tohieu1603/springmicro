import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { UsersTable } from "./users-table";
import type { Page } from "@/lib/api/types";

interface UserRow {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
  active: boolean;
  createdAt?: string;
}

export default async function AdminUsers({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; page?: string }>;
}) {
  const sp = await searchParams;
  const qs = new URLSearchParams();
  qs.set("page", String(Number(sp.page ?? 0) || 0));
  qs.set("size", "20");
  if (sp.q) qs.set("q", sp.q);

  const data = await fetchServer<Page<UserRow>>(`/api/users?${qs.toString()}`).catch(
    () => ({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }),
  );

  return (
    <Card title="Người dùng">
      <UsersTable page={data} />
    </Card>
  );
}
