import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Card, Descriptions, Tag, Steps } from "antd";
import { fetchServerOrNull } from "@/lib/api/server";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Order } from "@/lib/api/types";
import { AdminOrderActions } from "./AdminOrderActions";

const FLOW = ["PENDING", "INVENTORY_RESERVED", "PAYMENT_PENDING", "PAID", "SHIPPED", "DELIVERED"];

export default async function AdminOrderDetail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const order = await fetchServerOrNull<Order>(`/api/orders/${id}`);
  if (!order) notFound();

  const idx = FLOW.indexOf(order.status);

  return (
    <div className="space-y-4">
      <Card
        title={`Đơn ${order.orderNumber}`}
        extra={<Tag color="blue">{order.status}</Tag>}
      >
        <Steps
          current={idx >= 0 ? idx : 0}
          status={order.status === "CANCELLED" || order.status === "FAILED" ? "error" : "process"}
          items={FLOW.map((s) => ({ title: s }))}
        />
      </Card>

      <div className="grid lg:grid-cols-[1fr,360px] gap-4">
        <Card title="Sản phẩm">
          <div className="space-y-3">
            {order.items.map((i) => (
              <div key={i.variantId} className="flex gap-3">
                <div className="relative w-16 h-16 rounded bg-surface-soft overflow-hidden shrink-0">
                  <Image src={i.variantImage || "/img/placeholder.svg"} alt={i.productName} fill sizes="64px" className="object-contain p-1" />
                </div>
                <div className="flex-1">
                  <p className="font-medium">{i.productName}</p>
                  <p className="text-xs text-slate">SKU: {i.variantSku} • × {i.quantity}</p>
                </div>
                <span className="font-semibold">{formatVnd(Number(i.unitPrice) * i.quantity)}</span>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Chi tiết">
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Khách">{order.recipientName}</Descriptions.Item>
            <Descriptions.Item label="SĐT">{order.recipientPhone}</Descriptions.Item>
            <Descriptions.Item label="Tạm tính">{formatVnd(order.subtotalAmount)}</Descriptions.Item>
            {order.discountAmount && Number(order.discountAmount) > 0 && (
              <Descriptions.Item label="Giảm">−{formatVnd(order.discountAmount)}</Descriptions.Item>
            )}
            <Descriptions.Item label="Tổng">
              <span className="font-bold text-accent">{formatVnd(order.totalAmount)}</span>
            </Descriptions.Item>
            <Descriptions.Item label="Thanh toán">{order.paymentMethod}</Descriptions.Item>
            <Descriptions.Item label="Tạo lúc">{formatDate(order.createdAt, true)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>

      <Card title="Địa chỉ giao hàng">
        <p className="text-sm">
          {[order.street, order.ward, order.district, order.city, order.country]
            .filter(Boolean)
            .join(", ")}
        </p>
      </Card>

      <AdminOrderActions order={order} />
    </div>
  );
}
