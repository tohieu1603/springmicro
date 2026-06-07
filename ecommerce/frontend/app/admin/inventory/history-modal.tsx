"use client";

import { useEffect, useState } from "react";
import { Modal, Table, Tag, Empty, Alert, Button, Pagination } from "antd";
import type { ColumnsType } from "antd/es/table";
import { formatDate } from "@/lib/utils";
import { api } from "@/lib/api/client";

export interface StockEvent {
  ts: string;
  sku: string;
  productId: string;
  delta: number;
  before: number;
  after: number;
  by: string;
  note?: string;
}

/**
 * Local-storage-backed audit trail for stock adjustments. BE doesn't yet expose
 * a per-SKU history endpoint, so we capture every adjust on the admin side and
 * persist locally. When the BE adds `/api/inventory/{sku}/history` we'll prefer
 * that source and demote this to a fallback.
 */
const KEY = "lm_stock_history";
const MAX = 500;

export function loadHistory(): StockEvent[] {
  if (typeof window === "undefined") return [];
  try {
    return JSON.parse(localStorage.getItem(KEY) || "[]") as StockEvent[];
  } catch {
    return [];
  }
}

export function appendHistory(evt: StockEvent) {
  if (typeof window === "undefined") return;
  const list = loadHistory();
  list.unshift(evt);
  if (list.length > MAX) list.length = MAX;
  localStorage.setItem(KEY, JSON.stringify(list));
}

interface BeMovement {
  id: string;
  productId: string;
  sku: string;
  delta: number;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  actor?: string;
  note?: string;
  createdAt: string;
}

export function StockHistoryModal({
  open,
  onClose,
  filterSku,
}: {
  open: boolean;
  onClose: () => void;
  filterSku?: string;
}) {
  const [rows, setRows] = useState<StockEvent[]>([]);
  const [source, setSource] = useState<"server" | "local">("server");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoading(true);
    (async () => {
      try {
        // Prefer the BE audit trail when available; fall back to localStorage so
        // historic adjustments captured before the BE endpoint shipped are not lost.
        const params = new URLSearchParams({ size: "200" });
        if (filterSku) params.set("sku", filterSku);
        const res = await api.get<{ content: BeMovement[] } | { data: { content: BeMovement[] } }>(
          `/api/inventory/movements?${params.toString()}`,
          { validateStatus: (s) => s < 500 },
        );
        const body = res.data as unknown as { data?: { content: BeMovement[] } };
        const content = (body?.data ?? (res.data as { content: BeMovement[] }))?.content ?? [];
        if (cancelled) return;
        if (res.status === 200) {
          setSource("server");
          setRows(content.map((m) => ({
            ts: m.createdAt,
            sku: m.sku,
            productId: m.productId,
            delta: m.delta,
            before: m.quantityBefore,
            after: m.quantityAfter,
            by: m.actor ?? "—",
            note: m.note,
          })));
          return;
        }
      } catch {
        // fall through to local
      }
      if (!cancelled) {
        setSource("local");
        const local = loadHistory();
        setRows(filterSku ? local.filter((r) => r.sku === filterSku) : local);
      }
      setLoading(false);
    })();
    return () => { cancelled = true; };
  }, [open, filterSku]);

  const filtered = rows;

  const columns: ColumnsType<StockEvent> = [
    {
      title: "Thời gian",
      dataIndex: "ts",
      width: 140,
      render: (v) => <span className="text-xs">{formatDate(v, true)}</span>,
    },
    {
      title: "SKU",
      dataIndex: "sku",
      width: 160,
      render: (v) => <span className="font-mono text-xs">{v}</span>,
    },
    {
      title: "Thay đổi",
      key: "delta",
      width: 110,
      align: "right",
      render: (_, r) =>
        r.delta > 0 ? (
          <Tag color="green">+{r.delta}</Tag>
        ) : (
          <Tag color="red">{r.delta}</Tag>
        ),
    },
    {
      title: "Trước / Sau",
      key: "ba",
      width: 130,
      align: "right",
      render: (_, r) => (
        <span className="text-xs">
          <b>{r.before}</b> → <b>{r.after}</b>
        </span>
      ),
    },
    { title: "Người thực hiện", dataIndex: "by", width: 140 },
    { title: "Ghi chú", dataIndex: "note", ellipsis: true },
  ];

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={
        <Button
          danger
          onClick={() => {
            localStorage.removeItem(KEY);
            setRows([]);
          }}
        >
          Xóa lịch sử
        </Button>
      }
      width={860}
      title={filterSku ? `Lịch sử kho: ${filterSku}` : "Lịch sử điều chỉnh kho"}
      destroyOnHidden
    >
      <Alert
        type={source === "server" ? "success" : "info"}
        message={source === "server" ? "Nguồn: BE /api/inventory/movements" : "Nguồn: lịch sử local"}
        description={
          source === "server"
            ? "Đã đồng bộ với audit log trên server (table stock_movements)."
            : "BE không sẵn sàng — đang dùng localStorage tạm thời."
        }
        showIcon
        className="mb-3"
      />
      {filtered.length === 0 ? (
        <Empty description="Chưa có thao tác nào" />
      ) : (
        <Table
          rowKey={(r) => `${r.ts}-${r.sku}`}
          dataSource={filtered}
          columns={columns}
          size="small"
          pagination={{ pageSize: 10 }}
        />
      )}
    </Modal>
  );
}
