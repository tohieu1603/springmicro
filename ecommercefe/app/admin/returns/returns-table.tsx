"use client";

import { Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { formatDate, formatVnd } from "@/lib/utils";

interface ReturnRow {
  id: string;
  orderId: string;
  orderNumber: string;
  userId: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "REFUNDED";
  refundAmount?: string;
  createdAt: string;
}

interface PageData {
  content: ReturnRow[];
  number: number;
  size: number;
  totalElements: number;
}

const TONE: Record<string, string> = {
  PENDING: "orange",
  APPROVED: "blue",
  REJECTED: "red",
  REFUNDED: "green",
};

export function ReturnsTable({ page }: { page: PageData }) {
  const columns: ColumnsType<ReturnRow> = [
    { title: "Mã yêu cầu", dataIndex: "id" },
    { title: "Số đơn", dataIndex: "orderNumber" },
    { title: "Lý do", dataIndex: "reason", ellipsis: true },
    {
      title: "Hoàn tiền",
      dataIndex: "refundAmount",
      render: (v?: string) => (v ? formatVnd(v) : "—"),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      render: (s: string) => <Tag color={TONE[s] || "default"}>{s}</Tag>,
    },
    { title: "Ngày tạo", dataIndex: "createdAt", render: (v: string) => formatDate(v, true) },
  ];

  return (
    <Table
      rowKey="id"
      dataSource={page.content}
      columns={columns}
      pagination={{ current: page.number + 1, pageSize: page.size, total: page.totalElements }}
    />
  );
}
