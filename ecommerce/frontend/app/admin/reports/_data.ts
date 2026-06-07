import type { Dayjs } from "dayjs";
import { api } from "@/lib/api/client";

export interface ReportPayload {
  summary: { revenue: number; orders: number; aov: number };
  byDay: { date: string; revenue: number; orders: number; newCustomers: number }[];
  topProducts: { productId: string; name: string; qty: number; revenue: number }[];
  lowStock: { sku: string; quantity: number; minStockLevel: number; sold30d: number }[];
  segments: { name: string; count: number }[];
}

export async function fetchSummary(from: Dayjs, to: Dayjs): Promise<ReportPayload> {
  const res = await api.get<ReportPayload | { data: ReportPayload }>(
    `/api/analytics/reports?from=${from.toISOString()}&to=${to.toISOString()}`,
  );
  const body = res.data as unknown as { data?: ReportPayload };
  return body?.data ?? (res.data as ReportPayload);
}

export function mockReport(from: Dayjs, to: Dayjs): ReportPayload {
  const days: ReportPayload["byDay"] = [];
  let d = from;
  while (d.isBefore(to) || d.isSame(to, "day")) {
    days.push({
      date: d.format("YYYY-MM-DD"),
      revenue: Math.floor(2_000_000 + Math.random() * 8_000_000),
      orders: Math.floor(20 + Math.random() * 80),
      newCustomers: Math.floor(5 + Math.random() * 25),
    });
    d = d.add(1, "day");
  }
  const revenue = days.reduce((s, x) => s + x.revenue, 0);
  const orders = days.reduce((s, x) => s + x.orders, 0);
  return {
    summary: { revenue, orders, aov: Math.floor(revenue / Math.max(orders, 1)) },
    byDay: days,
    topProducts: [
      { productId: "1", name: "Áo thun cotton premium", qty: 412, revenue: 124_500_000 },
      { productId: "2", name: "Tai nghe wireless V2", qty: 281, revenue: 198_000_000 },
      { productId: "3", name: "Giày sneaker SK-01", qty: 244, revenue: 87_400_000 },
      { productId: "4", name: "Túi da Premium", qty: 198, revenue: 152_000_000 },
      { productId: "5", name: "Đồng hồ FitPro", qty: 156, revenue: 312_000_000 },
    ],
    lowStock: [
      { sku: "LMS-001-RED-M", quantity: 4, minStockLevel: 10, sold30d: 67 },
      { sku: "LMS-002-BLK-L", quantity: 7, minStockLevel: 15, sold30d: 49 },
      { sku: "LMS-008-WHT-S", quantity: 2, minStockLevel: 10, sold30d: 22 },
    ],
    segments: [
      { name: "VIP", count: 412 },
      { name: "Regular", count: 1240 },
      { name: "New", count: 870 },
      { name: "Dormant", count: 318 },
    ],
  };
}
