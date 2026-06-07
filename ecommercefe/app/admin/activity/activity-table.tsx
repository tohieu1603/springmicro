"use client";

import { Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import { formatDate } from "@/lib/utils";

interface ActivityRow {
  id: string;
  userId: string;
  userName: string;
  action: string;
  entity: string;
  entityId: string;
  diff?: string;
  ip?: string;
  userAgent?: string;
  timestamp: string;
}

interface PageData {
  content: ActivityRow[];
  totalElements: number;
  number: number;
  size: number;
}

const ACTION_TONE: Record<string, string> = {
  CREATE: "green",
  UPDATE: "blue",
  DELETE: "red",
  LOGIN: "default",
  LOGOUT: "default",
};

export function ActivityTable({ page }: { page: PageData }) {
  const columns: ColumnsType<ActivityRow> = [
    { title: "Thời gian", dataIndex: "timestamp", width: 160, render: (v) => formatDate(v, true) },
    { title: "Người dùng", dataIndex: "userName" },
    {
      title: "Hành động",
      dataIndex: "action",
      width: 110,
      render: (a) => <Tag color={ACTION_TONE[a] || "default"}>{a}</Tag>,
    },
    { title: "Đối tượng", key: "e", render: (_, r) => `${r.entity} #${r.entityId}` },
    { title: "IP", dataIndex: "ip" },
  ];

  return (
    <Table
      rowKey="id"
      dataSource={page.content}
      columns={columns}
      expandable={{
        rowExpandable: (r) => Boolean(r.diff),
        expandedRowRender: (r) => (
          <pre className="text-xs bg-surface-soft p-3 rounded overflow-auto">{r.diff}</pre>
        ),
      }}
      pagination={{ current: page.number + 1, pageSize: page.size, total: page.totalElements }}
    />
  );
}
