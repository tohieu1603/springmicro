"use client";

import { Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { formatDate, formatVnd } from "@/lib/utils";

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

const TONE: Record<string, string> = { SCHEDULED: "blue", ACTIVE: "green", ENDED: "default" };

export function FlashSalesTable({ items }: { items: FlashSale[] }) {
  const columns: ColumnsType<FlashSale> = [
    { title: "Sản phẩm", dataIndex: "productName" },
    { title: "Giá gốc", dataIndex: "originalPrice", render: (v) => formatVnd(v) },
    { title: "Giá sale", dataIndex: "salePrice", render: (v) => formatVnd(v) },
    { title: "Slot", key: "slot", render: (_, r) => `${r.reservedSlots} / ${r.totalSlots}` },
    { title: "Max/user", dataIndex: "maxPerUser" },
    { title: "Bắt đầu", dataIndex: "startTime", render: (v) => formatDate(v, true) },
    { title: "Kết thúc", dataIndex: "endTime", render: (v) => formatDate(v, true) },
    { title: "Trạng thái", dataIndex: "status", render: (s) => <Tag color={TONE[s] || "default"}>{s}</Tag> },
  ];

  return <Table rowKey="id" dataSource={items} columns={columns} />;
}
