import Link from "next/link";
import { notFound } from "next/navigation";
import { Card, Descriptions, Statistic, Row, Col, Table, Tag, Avatar } from "antd";
import { fetchServerOrNull, fetchServer } from "@/lib/api/server";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Order, Page } from "@/lib/api/types";

interface CustomerDetail {
  id: string;
  username: string;
  fullName?: string;
  email: string;
  phone?: string;
  ordersCount: number;
  lifetimeValue: number;
  avgOrderValue: number;
  lastOrderAt?: string;
  segment?: string;
  createdAt?: string;
}

export default async function CustomerDetail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [customer, orders] = await Promise.all([
    fetchServerOrNull<CustomerDetail>(`/api/users/${id}`),
    fetchServer<Page<Order>>(`/api/orders?userId=${id}&page=0&size=10`).catch(
      () => ({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 }),
    ),
  ]);
  if (!customer) notFound();

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex items-center gap-4">
          <Avatar size={64} style={{ background: "#1A2B3C", fontSize: 24 }}>
            {(customer.fullName || customer.username).charAt(0).toUpperCase()}
          </Avatar>
          <div>
            <h2 className="text-2xl font-bold m-0">{customer.fullName || customer.username}</h2>
            <p className="text-slate m-0 mt-1">{customer.email}{customer.phone ? ` • ${customer.phone}` : ""}</p>
            {customer.segment && <Tag color="gold" className="mt-2">{customer.segment}</Tag>}
          </div>
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}><Card><Statistic title="Tổng đơn" value={customer.ordersCount} /></Card></Col>
        <Col xs={24} sm={8}><Card><Statistic title="Lifetime value" value={customer.lifetimeValue} suffix="₫" formatter={(v) => new Intl.NumberFormat("vi-VN").format(Number(v))} valueStyle={{ color: "#FF6B35" }} /></Card></Col>
        <Col xs={24} sm={8}><Card><Statistic title="Giá trị đơn TB" value={customer.avgOrderValue} suffix="₫" formatter={(v) => new Intl.NumberFormat("vi-VN").format(Number(v))} /></Card></Col>
      </Row>

      <Card title="Đơn hàng gần đây">
        <Table
          rowKey="id"
          dataSource={orders.content}
          columns={[
            {
              title: "Số đơn",
              dataIndex: "orderNumber",
              render: (n, o) => <Link href={`/admin/orders/${o.id}`} className="hover:text-primary">{n}</Link>,
            },
            { title: "Trạng thái", dataIndex: "status", render: (s) => <Tag>{s}</Tag> },
            { title: "Tổng", dataIndex: "totalAmount", render: (v) => formatVnd(v) },
            { title: "Đặt lúc", dataIndex: "createdAt", render: (v) => formatDate(v, true) },
          ]}
          pagination={false}
        />
      </Card>
    </div>
  );
}
