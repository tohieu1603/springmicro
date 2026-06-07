import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { ReturnsTable } from "./returns-table";
import type { Page } from "@/lib/api/types";

interface ReturnRow {
  id: string;
  orderId: string;
  orderNumber: string;
  userId: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "REFUNDED";
  refundAmount?: string;
  createdAt: string;
}

export default async function ReturnsAdmin() {
  const data = await fetchServer<Page<ReturnRow>>(`/api/orders/return-requests?page=0&size=20`).catch(
    () => ({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }),
  );
  return (
    <Card title="Yêu cầu trả hàng">
      <ReturnsTable page={data} />
    </Card>
  );
}
