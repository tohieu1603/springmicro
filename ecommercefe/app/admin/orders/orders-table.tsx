"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Table, Tag, Space, Input, Select, Tooltip, Avatar, Progress,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Order, Page } from "@/lib/api/types";

const STATUS: Record<string, { color: string; label: string }> = {
  PENDING:             { color: "default", label: "Chờ xử lý" },
  INVENTORY_RESERVED:  { color: "blue",    label: "Đã giữ hàng" },
  PAYMENT_PENDING:     { color: "orange",  label: "Chờ thanh toán" },
  PAID:                { color: "green",   label: "Đã thanh toán" },
  CONFIRMED:           { color: "green",   label: "Đã xác nhận" },
  SHIPPED:             { color: "blue",    label: "Đang giao" },
  DELIVERED:           { color: "green",   label: "Đã giao" },
  CANCELLED:           { color: "red",     label: "Đã hủy" },
  FAILED:              { color: "red",     label: "Thất bại" },
};

const PAYMENT_LABEL: Record<string, string> = {
  SEPAY: "Sepay",
  MOMO: "MoMo",
  COD: "COD",
  VNPAY: "VNPay",
};

export function OrdersTable({ page }: { page: Page<Order> }) {
  const router = useRouter();
  const sp = useSearchParams();

  function update(key: string, value: string | undefined) {
    const next = new URLSearchParams(sp.toString());
    if (value) next.set(key, value);
    else next.delete(key);
    next.delete("page");
    router.push(`/admin/orders?${next.toString()}`);
  }

  const columns: ColumnsType<Order> = [
    {
      title: "Đơn hàng",
      key: "order",
      width: 220,
      fixed: "left",
      render: (_, o) => (
        <div className="min-w-0">
          <Link href={`/admin/orders/${o.id}`} className="font-medium hover:text-primary">
            {o.orderNumber}
          </Link>
          <div className="text-xs text-slate mt-0.5">
            {formatDate(o.createdAt, true)}
          </div>
        </div>
      ),
    },
    {
      title: "Khách",
      key: "customer",
      width: 220,
      render: (_, o) => (
        <div className="flex items-center gap-2 min-w-0">
          <Avatar style={{ background: "#1A2B3C", flexShrink: 0 }} size={32}>
            {o.recipientName?.charAt(0).toUpperCase()}
          </Avatar>
          <div className="min-w-0">
            <div className="text-sm font-medium truncate">{o.recipientName}</div>
            <div className="text-xs text-slate truncate">{o.recipientPhone}</div>
          </div>
        </div>
      ),
    },
    {
      title: "Mặt hàng",
      key: "items",
      width: 240,
      render: (_, o) => (
        <Tooltip
          title={
            <div className="space-y-1">
              {o.items.slice(0, 6).map((i, idx) => (
                <div key={idx} className="text-xs">
                  {i.productName} × {i.quantity}
                </div>
              ))}
              {o.items.length > 6 && <div className="text-xs">+ {o.items.length - 6} khác</div>}
            </div>
          }
        >
          <div>
            <div className="text-sm truncate max-w-[230px]">
              {o.items[0]?.productName ?? "—"}
            </div>
            <div className="text-xs text-slate mt-0.5">
              {o.items.length} sản phẩm •{" "}
              {o.items.reduce((s, i) => s + i.quantity, 0)} món
            </div>
          </div>
        </Tooltip>
      ),
    },
    {
      title: "Địa chỉ",
      key: "address",
      width: 180,
      render: (_, o) =>
        o.city ? (
          <div className="text-xs">
            <div className="font-medium">{o.city}</div>
            <div className="text-slate truncate max-w-[180px]">
              {o.district || ""}
              {o.ward ? `, ${o.ward}` : ""}
            </div>
          </div>
        ) : "—",
    },
    {
      title: "Tổng",
      key: "total",
      width: 150,
      align: "right",
      render: (_, o) => (
        <div className="text-right">
          <div className="font-bold text-accent">{formatVnd(o.totalAmount)}</div>
          {Number(o.discountAmount ?? 0) > 0 && (
            <div className="text-[11px] text-emerald-600">
              −{formatVnd(o.discountAmount!)}
            </div>
          )}
        </div>
      ),
    },
    {
      title: "Thanh toán",
      dataIndex: "paymentMethod",
      width: 110,
      render: (v: string) => (
        <Tag color={v === "COD" ? "orange" : "blue"}>{PAYMENT_LABEL[v] ?? v}</Tag>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 140,
      render: (s: string) => {
        const meta = STATUS[s] ?? { color: "default", label: s };
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
  ];

  return (
    <>
      <Space className="mb-4" wrap>
        <Input.Search
          placeholder="Số đơn / SĐT / email khách..."
          defaultValue={sp.get("q") || ""}
          onSearch={(v) => update("q", v)}
          allowClear
          style={{ width: 320 }}
        />
        <Select
          placeholder="Trạng thái"
          allowClear
          style={{ width: 200 }}
          defaultValue={sp.get("status") || undefined}
          onChange={(v) => update("status", v)}
          options={Object.entries(STATUS).map(([k, v]) => ({ value: k, label: v.label }))}
        />
        <Select
          placeholder="Thanh toán"
          allowClear
          style={{ width: 160 }}
          defaultValue={sp.get("paymentMethod") || undefined}
          onChange={(v) => update("paymentMethod", v)}
          options={Object.entries(PAYMENT_LABEL).map(([k, v]) => ({ value: k, label: v }))}
        />
      </Space>
      <Table
        rowKey="id"
        dataSource={page.content ?? []}
        columns={columns}
        scroll={{ x: 1100 }}
        size="middle"
        pagination={{
          // BE returns a cursor page ({content, nextCursor, hasMore, size}) —
          // not Spring's Page (number/totalElements). Coerce so antd doesn't
          // render NaN page numbers when those fields are missing.
          current: (page.number ?? 0) + 1,
          pageSize: page.size ?? (page.content?.length || 20),
          total: page.totalElements ?? page.content?.length ?? 0,
          showSizeChanger: false,
          showTotal: (t) => `${t} đơn`,
          onChange: (p) => {
            const next = new URLSearchParams(sp.toString());
            next.set("page", String(p - 1));
            router.push(`/admin/orders?${next.toString()}`);
          },
        }}
      />
    </>
  );
}
