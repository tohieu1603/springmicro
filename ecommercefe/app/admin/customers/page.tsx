import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { CustomersTable } from "./customers-table";

interface CustomerRow {
  id: string;
  username: string;
  fullName?: string;
  email: string;
  ordersCount: number;
  lifetimeValue: number;
  lastOrderAt?: string;
  segment?: "NEW" | "REGULAR" | "VIP" | "DORMANT";
  active: boolean;
}

export default async function AdminCustomers({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; segment?: string; page?: string }>;
}) {
  const sp = await searchParams;
  const qs = new URLSearchParams({ page: String(Number(sp.page ?? 0) || 0), size: "20" });
  if (sp.q) qs.set("q", sp.q);
  if (sp.segment) qs.set("segment", sp.segment);

  // BE /api/users returns ApiResponse-shape OR plain page — be defensive about both.
  type UserListRow = {
    id: string; username: string; fullName?: string; firstName?: string; lastName?: string;
    email: string; enabled?: boolean; active?: boolean; createdAt?: string;
  };
  type AnyShape = {
    content?: UserListRow[];
    items?: UserListRow[];
    totalElements?: number;
    size?: number;
    pageSize?: number;
    number?: number;
  };
  const raw: AnyShape = await fetchServer<AnyShape>(`/api/users?${qs.toString()}`).catch(() => ({}));
  const rows: UserListRow[] = Array.isArray(raw.content)
    ? raw.content
    : Array.isArray(raw.items)
    ? raw.items
    : [];
  // Enrich with order aggregates per user. The new BE endpoint
  // /api/orders/customers/stats?userIds=... returns orderCount, LTV, lastOrderAt.
  type Stat = { userId: string; orderCount: number; lifetimeValue: number; lastOrderAt?: string };
  const ids = rows.map((u) => u.id).filter(Boolean);
  const statsParam = ids.map((id) => `userIds=${encodeURIComponent(id)}`).join("&");
  const statsResp = ids.length > 0
    ? await fetchServer<Stat[] | { data: Stat[] }>(`/api/orders/customers/stats?${statsParam}`)
        .catch(() => [] as Stat[])
    : [];
  const statsList: Stat[] = Array.isArray(statsResp)
    ? statsResp
    : ((statsResp as { data?: Stat[] }).data ?? []);
  const statsByUser = new Map<string, Stat>();
  statsList.forEach((s) => statsByUser.set(s.userId, s));

  const segmentOf = (s?: Stat): CustomerRow["segment"] => {
    if (!s || s.orderCount === 0) return "NEW";
    if (s.lifetimeValue > 5_000_000) return "VIP";
    if (s.lastOrderAt) {
      const days = (Date.now() - new Date(s.lastOrderAt).getTime()) / 86_400_000;
      if (days > 90) return "DORMANT";
    }
    return "REGULAR";
  };

  const data = {
    content: rows.map((u) => {
      const s = statsByUser.get(u.id);
      return {
        ...u,
        fullName: u.fullName ?? [u.lastName, u.firstName].filter(Boolean).join(" ").trim(),
        ordersCount: Number(s?.orderCount ?? 0),
        lifetimeValue: Number(s?.lifetimeValue ?? 0),
        lastOrderAt: s?.lastOrderAt,
        segment: segmentOf(s),
        active: u.active ?? u.enabled ?? true,
      };
    }),
    totalElements: raw.totalElements ?? rows.length,
    size: raw.size ?? raw.pageSize ?? 20,
    number: raw.number ?? (Number(sp.page ?? 0) || 0),
  };

  return (
    <Card title="Khách hàng">
      <CustomersTable page={data} />
    </Card>
  );
}
