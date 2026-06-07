import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { FlashSalesTable } from "./flash-sales-table";
import type { Page } from "@/lib/api/types";

interface FlashSale {
  id: string;
  productId: string;
  productName: string;
  originalPrice: string;
  salePrice: string;
  totalSlots: number;
  reservedSlots: number;
  maxPerUser: number;
  startTime: string;
  endTime: string;
  status: "SCHEDULED" | "ACTIVE" | "ENDED";
}

export default async function FlashSalesAdmin() {
  const data = await fetchServer<Page<FlashSale>>(`/api/flash-sales?page=0&size=20`).catch(
    () => ({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }),
  );
  return (
    <Card title="Flash sale">
      <FlashSalesTable items={data.content} />
    </Card>
  );
}
