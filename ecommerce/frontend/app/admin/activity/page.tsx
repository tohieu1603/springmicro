import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { ActivityTable } from "./activity-table";

interface ActivityRow {
  id: string;
  userId: string;
  userName: string;
  action: string;
  entity: string;
  entityId: string;
  diff?: string;
  ip?: string;
  userAgent?: string;
  timestamp: string;
}

export default async function ActivityLog({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const sp = await searchParams;
  const page = Math.max(0, Number(sp.page ?? 0) || 0);

  const data = await fetchServer<{
    content: ActivityRow[]; totalElements: number; number: number; size: number;
  }>(`/api/audit/activity?page=${page}&size=30`).catch(() => ({
    content: [], totalElements: 0, number: 0, size: 30,
  }));

  return (
    <Card title="Lịch sử hoạt động (Audit)">
      <ActivityTable page={data} />
    </Card>
  );
}
