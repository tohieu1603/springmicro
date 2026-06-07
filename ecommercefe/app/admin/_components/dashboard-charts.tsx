"use client";

import dynamic from "next/dynamic";
import { Card, Col, Row } from "antd";

/**
 * ApexCharts touches `window` at module load — must be dynamic + ssr:false.
 * Skipping SSR also means the chart only hydrates after first paint, which
 * is fine because the surrounding KPI cards already give the user something
 * to look at.
 */
const Chart = dynamic(() => import("react-apexcharts"), { ssr: false });

interface Props {
  revenueByDay: { date: string; revenue: number }[];
  ordersByStatus: { status: string; count: number }[];
  topProducts: { name: string; sales: number }[];
}

export function DashboardCharts({ revenueByDay, ordersByStatus, topProducts }: Props) {
  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={16}>
        <Card title="Doanh thu 30 ngày" extra={<span className="text-xs text-slate">Đơn vị: triệu ₫</span>}>
          <Chart
            type="area"
            height={320}
            series={[
              {
                name: "Doanh thu",
                data: revenueByDay.map((r) => ({ x: r.date, y: r.revenue / 1_000_000 })),
              },
            ]}
            options={{
              chart: {
                toolbar: { show: false },
                animations: { speed: 350 },
                fontFamily: "Inter, sans-serif",
              },
              colors: ["#FF6B35"],
              stroke: { curve: "smooth", width: 3 },
              fill: {
                type: "gradient",
                gradient: {
                  shadeIntensity: 1,
                  opacityFrom: 0.4,
                  opacityTo: 0.05,
                  stops: [0, 90, 100],
                },
              },
              dataLabels: { enabled: false },
              xaxis: { type: "datetime", labels: { format: "dd/MM" } },
              yaxis: { labels: { formatter: (v) => `${v.toFixed(1)}M` } },
              grid: { borderColor: "#E2E8F0", strokeDashArray: 4 },
              tooltip: {
                x: { format: "dd MMM yyyy" },
                y: { formatter: (v) => `${v.toFixed(2)} triệu` },
              },
            }}
          />
        </Card>
      </Col>

      <Col xs={24} lg={8}>
        <Card title="Trạng thái đơn">
          <Chart
            type="donut"
            height={320}
            series={ordersByStatus.map((o) => o.count)}
            options={{
              labels: ordersByStatus.map((o) => o.status),
              colors: ["#10B981", "#1A2B3C", "#F59E0B", "#DC2626"],
              legend: { position: "bottom" },
              dataLabels: {
                style: { fontSize: "11px", fontWeight: 600 },
                dropShadow: { enabled: false },
              },
              plotOptions: {
                pie: { donut: { size: "65%", labels: { show: true, total: { show: true, label: "Tổng" } } } },
              },
              chart: { fontFamily: "Inter, sans-serif" },
            }}
          />
        </Card>
      </Col>

      <Col xs={24}>
        <Card title="Top 5 sản phẩm bán chạy">
          <Chart
            type="bar"
            height={300}
            series={[{ name: "Đã bán", data: topProducts.map((t) => t.sales) }]}
            options={{
              chart: { toolbar: { show: false }, fontFamily: "Inter, sans-serif" },
              colors: ["#1A2B3C"],
              plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: "65%" } },
              dataLabels: { enabled: true, style: { colors: ["#fff"] } },
              xaxis: { categories: topProducts.map((t) => t.name) },
              grid: { borderColor: "#E2E8F0" },
            }}
          />
        </Card>
      </Col>
    </Row>
  );
}
