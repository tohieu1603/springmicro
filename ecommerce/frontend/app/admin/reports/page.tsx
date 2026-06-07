"use client";

import dynamic from "next/dynamic";
import { Card, Col, Row, Statistic, DatePicker, Space, Segmented, Table } from "antd";
import { useEffect, useState } from "react";
import dayjs, { type Dayjs } from "dayjs";
import { fetchSummary, mockReport } from "./_data";
import type { ReportPayload } from "./_data";
import { formatVnd } from "@/lib/utils";

const Chart = dynamic(() => import("react-apexcharts"), { ssr: false });

/**
 * Reports — pivots backend analytics by date range. If analytics-service
 * isn't ready, we fall back to a mock so admins can preview the layout.
 */
export default function ReportsPage() {
  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(29, "day"), dayjs()]);
  const [tab, setTab] = useState<string>("sales");
  const [data, setData] = useState<ReportPayload | null>(null);

  useEffect(() => {
    fetchSummary(range[0], range[1])
      .catch(() => mockReport(range[0], range[1]))
      .then(setData);
  }, [range]);

  if (!data) return <Card loading style={{ height: 400 }} />;

  return (
    <div className="space-y-4">
      <Card>
        <Space wrap className="flex justify-between w-full">
          <Segmented
            value={tab}
            onChange={(v) => setTab(String(v))}
            options={[
              { value: "sales", label: "Doanh thu" },
              { value: "inventory", label: "Tồn kho" },
              { value: "customers", label: "Khách hàng" },
            ]}
          />
          <DatePicker.RangePicker
            value={range}
            onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
            allowClear={false}
            format="DD/MM/YYYY"
          />
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card><Statistic title="Doanh thu" value={data.summary.revenue} formatter={(v) => formatVnd(Number(v))} valueStyle={{ color: "#FF6B35" }} /></Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card><Statistic title="Đơn hàng" value={data.summary.orders} /></Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card><Statistic title="Giá trị TB / đơn" value={data.summary.aov} formatter={(v) => formatVnd(Number(v))} /></Card>
        </Col>
      </Row>

      {tab === "sales" && (
        <>
          <Card title="Doanh thu theo ngày">
            <Chart
              type="line"
              height={320}
              series={[{ name: "Doanh thu", data: data.byDay.map((d) => ({ x: d.date, y: d.revenue })) }]}
              options={{
                chart: { toolbar: { show: false }, fontFamily: "Inter, sans-serif" },
                colors: ["#FF6B35"],
                stroke: { curve: "smooth", width: 3 },
                xaxis: { type: "datetime" },
                yaxis: { labels: { formatter: (v) => `${(v / 1_000_000).toFixed(1)}M` } },
                grid: { borderColor: "#E2E8F0", strokeDashArray: 4 },
              }}
            />
          </Card>

          <Card title="Top sản phẩm bán chạy">
            <Table
              rowKey="productId"
              dataSource={data.topProducts}
              pagination={false}
              columns={[
                { title: "Sản phẩm", dataIndex: "name" },
                { title: "Số lượng", dataIndex: "qty", width: 120 },
                { title: "Doanh thu", dataIndex: "revenue", render: (v) => formatVnd(v) },
              ]}
            />
          </Card>
        </>
      )}

      {tab === "inventory" && (
        <Card title="Sắp hết hàng">
          <Table
            rowKey="sku"
            dataSource={data.lowStock}
            pagination={false}
            columns={[
              { title: "SKU", dataIndex: "sku", render: (v) => <code className="font-mono text-xs">{v}</code> },
              { title: "Tồn kho", dataIndex: "quantity" },
              { title: "Ngưỡng", dataIndex: "minStockLevel" },
              { title: "Đã bán (30d)", dataIndex: "sold30d" },
            ]}
          />
        </Card>
      )}

      {tab === "customers" && (
        <Row gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Card title="Phân khúc khách hàng">
              <Chart
                type="donut"
                height={300}
                series={data.segments.map((s) => s.count)}
                options={{
                  labels: data.segments.map((s) => s.name),
                  colors: ["#1A2B3C", "#FF6B35", "#10B981", "#74777d"],
                  legend: { position: "bottom" },
                  chart: { fontFamily: "Inter, sans-serif" },
                }}
              />
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card title="Khách hàng mới theo ngày">
              <Chart
                type="bar"
                height={300}
                series={[{ name: "Khách mới", data: data.byDay.map((d) => d.newCustomers) }]}
                options={{
                  chart: { toolbar: { show: false }, fontFamily: "Inter, sans-serif" },
                  colors: ["#1A2B3C"],
                  xaxis: { categories: data.byDay.map((d) => d.date) },
                  grid: { borderColor: "#E2E8F0" },
                }}
              />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}
