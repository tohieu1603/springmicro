import { Card } from "antd";
import { fetchServer } from "@/lib/api/server";
import { OrdersTable } from "./orders-table";
import { OrdersKpis } from "./orders-kpis";
import type { Order, Page } from "@/lib/api/types";

interface PageProps {
  searchParams: Promise<{ q?: string; status?: string; page?: string; paymentMethod?: string }>;
}

/**
 * Order admin list — KPIs above, filterable table below. KPI strip is a
 * client island (`OrdersKpis`) because antd Statistic needs a function
 * `formatter` prop, which RSC can't serialise.
 */
export default async function AdminOrders({ searchParams }: PageProps) {
  const sp = await searchParams;
  const params = new URLSearchParams();
  params.set("page", String(Number(sp.page ?? 0) || 0));
  params.set("size", "20");
  if (sp.q) params.set("q", sp.q);
  if (sp.status) params.set("status", sp.status);
  if (sp.paymentMethod) params.set("paymentMethod", sp.paymentMethod);

  const [data, pending, paid] = await Promise.all([
    fetchServer<Page<Order>>(`/api/orders?${params.toString()}`).catch(
      () => ({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }),
    ),
    fetchServer<Page<Order>>(`/api/orders?status=PAYMENT_PENDING&size=1`).catch(
      () => ({ totalElements: 0 } as { totalElements: number }),
    ),
    fetchServer<Page<Order>>(`/api/orders?status=DELIVERED&size=50`).catch(
      () => ({ content: [] as Order[], totalElements: 0 }),
    ),
  ]);

  const revenue = (paid as { content?: Order[] }).content
    ?.reduce((s, o) => s + Number(o.totalAmount ?? 0), 0) ?? 0;

  return (
    <div className="space-y-4">
      <OrdersKpis
        totalFiltered={data.totalElements}
        pendingPayment={pending.totalElements}
        deliveredRecent={(paid as { totalElements: number }).totalElements ?? 0}
        revenueSample={revenue}
      />
      <Card title="Danh sách đơn hàng">
        <OrdersTable page={data} />
      </Card>
    </div>
  );
}
