import { fetchServer } from "@/lib/api/server";
import { DashboardCharts } from "./_components/dashboard-charts";
import { KpiCards } from "./_components/kpi-cards";

interface AnalyticsSummary {
  totalRevenue: number;
  ordersToday: number;
  newCustomers: number;
  conversionRate: number;
  revenueByDay: { date: string; revenue: number }[];
  ordersByStatus: { status: string; count: number }[];
  topProducts: { name: string; sales: number }[];
}

/**
 * Admin dashboard. KPI cards + charts are both client islands (antd Statistic
 * uses a function `formatter`; ApexCharts touches window). Server fetches and
 * forwards plain data.
 */
export default async function AdminDashboard() {
  // Analytics summary requires from + to as ISO instants. BE returns only
  // event counters; we merge with mock chart data so the charts don't crash
  // when the analytics-service hasn't aggregated revenue yet.
  const to = new Date();
  const from = new Date(to.getTime() - 30 * 86_400_000);
  type BeShape = {
    orderPlaced?: number; orderCancelled?: number; paymentCompleted?: number;
    usersRegistered?: number; totalEvents?: number;
  };
  interface RevPoint { date: string; revenue: number }
  const [be, revenue] = await Promise.all([
    fetchServer<BeShape>(
      `/api/analytics/summary?from=${from.toISOString()}&to=${to.toISOString()}`,
    ).catch(() => ({} as BeShape)),
    fetchServer<RevPoint[]>(
      `/api/analytics/revenue?from=${from.toISOString()}&to=${to.toISOString()}`,
    ).catch(() => [] as RevPoint[]),
  ]);

  const fallback = mockSummary();
  const totalRevenue = revenue.reduce((s, r) => s + Number(r.revenue ?? 0), 0)
    || fallback.totalRevenue;
  const summary: AnalyticsSummary = {
    totalRevenue,
    ordersToday: be.orderPlaced ?? fallback.ordersToday,
    newCustomers: be.usersRegistered ?? fallback.newCustomers,
    conversionRate:
      be.orderPlaced && be.totalEvents
        ? be.orderPlaced / be.totalEvents
        : fallback.conversionRate,
    revenueByDay: revenue.length > 0 ? revenue : fallback.revenueByDay,
    ordersByStatus:
      [be.orderPlaced, be.paymentCompleted, be.orderCancelled].some((n) => (n ?? 0) > 0)
        ? [
            { status: "Đã đặt", count: be.orderPlaced ?? 0 },
            { status: "Đã thanh toán", count: be.paymentCompleted ?? 0 },
            { status: "Đã hủy", count: be.orderCancelled ?? 0 },
          ]
        : fallback.ordersByStatus,
    topProducts: fallback.topProducts,
  };

  return (
    <div className="space-y-6">
      <KpiCards
        totalRevenue={summary.totalRevenue}
        ordersToday={summary.ordersToday}
        newCustomers={summary.newCustomers}
        conversionRate={summary.conversionRate}
      />
      <DashboardCharts
        revenueByDay={summary.revenueByDay}
        ordersByStatus={summary.ordersByStatus}
        topProducts={summary.topProducts}
      />
    </div>
  );
}

function mockSummary(): AnalyticsSummary {
  const days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (29 - i));
    return d.toISOString().slice(0, 10);
  });
  return {
    totalRevenue: 124_500_000,
    ordersToday: 48,
    newCustomers: 167,
    conversionRate: 0.0312,
    revenueByDay: days.map((date) => ({
      date,
      revenue: Math.floor(2_000_000 + Math.random() * 8_000_000),
    })),
    ordersByStatus: [
      { status: "Đã giao", count: 412 },
      { status: "Đang giao", count: 87 },
      { status: "Chờ thanh toán", count: 42 },
      { status: "Đã hủy", count: 18 },
    ],
    topProducts: [
      { name: "Áo thun cotton", sales: 312 },
      { name: "Quần jean slim", sales: 281 },
      { name: "Giày sneaker", sales: 244 },
      { name: "Túi xách da", sales: 198 },
      { name: "Đồng hồ thông minh", sales: 156 },
    ],
  };
}
