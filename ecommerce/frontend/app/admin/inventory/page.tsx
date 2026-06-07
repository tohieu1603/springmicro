"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Card, Table, Tag, Input, Space, Button, InputNumber, Modal, Form,
  Row, Col, Statistic, Progress, Image, Tooltip,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  InboxOutlined, WarningOutlined, FallOutlined, ShoppingOutlined,
} from "@ant-design/icons";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Product } from "@/lib/api/types";
import { StockHistoryModal, appendHistory } from "./history-modal";
import { HistoryOutlined } from "@ant-design/icons";

interface InventoryRow {
  id: string;
  productId: string;
  sku: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  minStockLevel: number;
  lowStock: boolean;
  lastUpdated?: string;
}

interface PageDto<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
}

/**
 * Admin inventory dashboard.
 *  - KPI strip: total SKUs, low-stock count, out-of-stock count, total stock value
 *  - Table augmented with product name + thumbnail (resolved via /api/products
 *    side fetch keyed by productId).
 *  - Adjust dialog uses PATCH /api/inventory/{id}/stock {delta}.
 */
export default function InventoryAdmin() {
  const [data, setData] = useState<PageDto<InventoryRow> | null>(null);
  const [products, setProducts] = useState<Record<string, Product>>({});
  const [loading, setLoading] = useState(true);
  const [adjustOf, setAdjustOf] = useState<InventoryRow | null>(null);
  const [form] = Form.useForm();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const [historySku, setHistorySku] = useState<string | undefined>();

  async function load() {
    setLoading(true);
    try {
      const qs = new URLSearchParams({ page: String(page), size: "50" });
      const res = await api.get<{
        items?: InventoryRow[]; content?: InventoryRow[];
        pageSize?: number; totalElements?: number;
      }>(`/api/inventory/?${qs.toString()}`);
      const raw = res.data as Record<string, unknown>;
      const rows: InventoryRow[] = (raw.items as InventoryRow[]) ?? (raw.content as InventoryRow[]) ?? [];
      setData({
        content: rows,
        number: page,
        size: (raw.pageSize as number) ?? 50,
        totalElements: (raw.totalElements as number) ?? rows.length,
      });

      // Resolve product names in a SINGLE catalog call instead of N round-trips:
      // grab a wide product page once and index by id. Inventory rows whose
      // productId isn't in that page just render the SKU + id (no extra fetch).
      // This trades a tiny memory cost for ~30× fewer HTTP calls and silences
      // the 404 spam from products that no longer exist.
      const ids = Array.from(new Set(rows.map((r) => r.productId)));
      const missing = ids.filter((id) => !products[id]);
      if (missing.length > 0) {
        try {
          const pr = await api.get<{ items?: Product[]; content?: Product[] }>(
            `/api/products?size=200`,
          );
          const all = (pr.data.items ?? pr.data.content ?? []) as Product[];
          const next: Record<string, Product> = { ...products };
          for (const p of all) next[p.id] = p;
          setProducts(next);
        } catch {
          // No catalog → table still works using SKU only.
        }
      }
    } catch {
      toast.error("Không tải được tồn kho");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function submitAdjust() {
    if (!adjustOf) return;
    const v = await form.validateFields();
    const delta = Number(v.delta);
    const note = String(v.note ?? "");
    const before = adjustOf.quantity;
    try {
      await api.patch(`/api/inventory/${adjustOf.productId}/stock`, { delta });
      // Capture history client-side. When BE exposes a per-SKU history endpoint
      // we'll prefer that source; the local store keeps a session-wide trail in
      // the meantime so admins can see what they changed.
      appendHistory({
        ts: new Date().toISOString(),
        sku: adjustOf.sku,
        productId: adjustOf.productId,
        delta,
        before,
        after: before + delta,
        by: "admin",
        note,
      });
      toast.success(`Đã ${delta >= 0 ? "nhập" : "xuất"} ${Math.abs(delta)} • ${adjustOf.sku}`);
      setAdjustOf(null);
      form.resetFields();
      load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Cập nhật thất bại");
    }
  }

  // ── KPIs ────────────────────────────────────────────────────────────
  const rows = data?.content ?? [];
  const totalSkus = rows.length;
  const lowStockCount = rows.filter((r) => r.lowStock && r.availableQuantity > 0).length;
  const outOfStock = rows.filter((r) => r.availableQuantity <= 0).length;
  const stockValue = rows.reduce((s, r) => {
    const p = products[r.productId];
    const variant = p?.variants?.find((v) => v.sku === r.sku);
    const price = Number(variant?.price ?? 0);
    return s + r.quantity * price;
  }, 0);

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      if (q) {
        const needle = q.toLowerCase();
        const p = products[r.productId];
        const matchProduct = p?.name?.toLowerCase().includes(needle);
        if (!r.sku?.toLowerCase().includes(needle) && !matchProduct) return false;
      }
      if (statusFilter === "low" && !r.lowStock) return false;
      if (statusFilter === "out" && r.availableQuantity > 0) return false;
      if (statusFilter === "ok" && (r.lowStock || r.availableQuantity <= 0)) return false;
      return true;
    });
  }, [rows, q, statusFilter, products]);

  const columns: ColumnsType<InventoryRow> = [
    {
      title: "Sản phẩm / SKU",
      key: "product",
      width: 320,
      fixed: "left",
      render: (_, r) => {
        const p = products[r.productId];
        const variant = p?.variants?.find((v) => v.sku === r.sku);
        const img = variant?.image || p?.thumbnail;
        return (
          <div className="flex items-center gap-2 min-w-0">
            {img ? (
              <Image
                src={img}
                width={42}
                height={42}
                preview={false}
                fallback="/img/placeholder.svg"
                style={{ objectFit: "cover", borderRadius: 4, background: "#f7f9fb" }}
                alt={p?.name ?? r.sku}
              />
            ) : (
              <div className="w-[42px] h-[42px] rounded bg-surface-soft shrink-0" />
            )}
            <div className="min-w-0">
              <div className="text-sm font-medium truncate">
                {p?.name ?? <span className="text-slate">SP #{r.productId}</span>}
              </div>
              <div className="text-xs text-slate flex flex-wrap gap-x-2">
                <span className="font-mono">{r.sku}</span>
                {variant?.attrs?.slice(0, 2).map((a, i) => (
                  <span key={i}>{a.attrName}: {a.valText ?? a.val}</span>
                ))}
              </div>
            </div>
          </div>
        );
      },
    },
    {
      title: "Tổng kho",
      dataIndex: "quantity",
      width: 110,
      align: "right",
      render: (v: number) => v.toLocaleString("vi-VN"),
    },
    {
      title: "Đang giữ",
      dataIndex: "reservedQuantity",
      width: 100,
      align: "right",
      render: (v: number) => (v > 0 ? <span className="text-amber-600">{v}</span> : v),
    },
    {
      title: "Khả dụng",
      dataIndex: "availableQuantity",
      width: 110,
      align: "right",
      render: (v: number) => (
        <span className={v <= 0 ? "text-red-600 font-semibold" : "font-semibold"}>
          {v.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      title: "Mức cảnh báo",
      key: "threshold",
      width: 200,
      render: (_, r) => {
        const safe = r.minStockLevel || 10;
        const pct = Math.min(100, Math.round((r.availableQuantity / Math.max(1, safe * 2)) * 100));
        const status = r.availableQuantity <= 0 ? "exception"
                       : r.lowStock ? "active" : "success";
        return (
          <Tooltip title={`Tối thiểu: ${safe}`}>
            <Progress percent={pct} size="small" status={status} showInfo={false} />
            <div className="text-[10px] text-slate mt-0.5">
              {r.availableQuantity} / mục tiêu {safe * 2}
            </div>
          </Tooltip>
        );
      },
    },
    {
      title: "Trạng thái",
      key: "status",
      width: 120,
      render: (_, r) =>
        r.availableQuantity <= 0
          ? <Tag color="red">Hết hàng</Tag>
          : r.lowStock
          ? <Tag color="orange">Sắp hết</Tag>
          : <Tag color="green">Đủ</Tag>,
    },
    {
      title: "Cập nhật",
      dataIndex: "lastUpdated",
      width: 130,
      render: (v?: string) => (
        <span className="text-xs text-slate">{v ? formatDate(v, true) : "—"}</span>
      ),
    },
    {
      title: "",
      key: "act",
      width: 170,
      fixed: "right",
      render: (_, r) => (
        <Space size={4}>
          <Button size="small" onClick={() => setAdjustOf(r)}>Nhập/xuất</Button>
          <Button
            size="small"
            icon={<HistoryOutlined />}
            onClick={() => {
              setHistorySku(r.sku);
              setHistoryOpen(true);
            }}
          />
        </Space>
      ),
    },
  ];

  return (
    <div className="space-y-4">
      <Row gutter={[12, 12]}>
        <Col xs={12} sm={6}>
          <Card><Statistic title="Tổng SKU" value={totalSkus} prefix={<InboxOutlined />} /></Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic title="Sắp hết" value={lowStockCount} prefix={<WarningOutlined />}
              valueStyle={{ color: "#F59E0B" }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic title="Hết hàng" value={outOfStock} prefix={<FallOutlined />}
              valueStyle={{ color: "#DC2626" }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic title="Giá trị tồn kho" value={stockValue}
              formatter={(v) => formatVnd(Number(v))}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: "#10B981" }} />
          </Card>
        </Col>
      </Row>

      <Card
        title="Tồn kho theo SKU"
        extra={
          <Button
            icon={<HistoryOutlined />}
            onClick={() => {
              setHistorySku(undefined);
              setHistoryOpen(true);
            }}
          >
            Lịch sử điều chỉnh
          </Button>
        }
      >
        <Space className="mb-4" wrap>
          <Input.Search
            placeholder="Tìm SKU hoặc tên sản phẩm..."
            allowClear
            onSearch={(v) => setQ(v)}
            style={{ width: 280 }}
          />
          <Space>
            {(["", "low", "out", "ok"] as const).map((s) => (
              <Button
                key={s}
                size="small"
                type={statusFilter === s ? "primary" : "default"}
                onClick={() => setStatusFilter(s)}
              >
                {s === "" ? "Tất cả" : s === "low" ? "Sắp hết" : s === "out" ? "Hết hàng" : "Đủ"}
              </Button>
            ))}
          </Space>
        </Space>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={filtered}
          columns={columns}
          scroll={{ x: 1100 }}
          size="middle"
          pagination={{
            current: page + 1,
            pageSize: data?.size ?? 50,
            total: data?.totalElements ?? 0,
            onChange: (p) => setPage(p - 1),
            showTotal: (t) => `${t} SKU`,
          }}
        />
      </Card>

      <Modal
        open={!!adjustOf}
        title={`Điều chỉnh tồn — ${adjustOf?.sku ?? ""}`}
        onCancel={() => setAdjustOf(null)}
        onOk={submitAdjust}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <p className="text-sm text-slate mb-2">
            Hiện có: <b>{adjustOf?.quantity}</b> • Khả dụng: <b>{adjustOf?.availableQuantity}</b>
          </p>
          <Form.Item
            name="delta"
            label="Điều chỉnh (+ nhập kho, − xuất kho)"
            rules={[{ required: true, message: "Bắt buộc" }]}
          >
            <InputNumber style={{ width: "100%" }} placeholder="VD: 50 (nhập) hoặc -10 (xuất)" />
          </Form.Item>
          <Form.Item name="note" label="Ghi chú (tùy chọn)">
            <Input placeholder="Lý do điều chỉnh: nhập lô mới / hàng lỗi / kiểm kê …" />
          </Form.Item>
        </Form>
      </Modal>

      <StockHistoryModal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        filterSku={historySku}
      />
    </div>
  );
}
