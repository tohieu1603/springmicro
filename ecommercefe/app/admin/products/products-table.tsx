"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Table, Tag, Space, Input, Select, Image, Button, Tooltip, Dropdown, Switch, Popconfirm,
} from "antd";
import {
  EditOutlined, MoreOutlined, EyeOutlined, CopyOutlined, DeleteOutlined, FileTextOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Category, Page, Product } from "@/lib/api/types";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Đang bán", DRAFT: "Bản nháp", ARCHIVED: "Lưu trữ", DELETED: "Đã xóa",
};

/**
 * Admin product list with inline status toggle.
 *
 * BE endpoint: `POST /api/products/{id}/status/{ACTIVATE|DEACTIVATE|DRAFT}`.
 * Returns 204; we optimistically update the row + revert on failure.
 */
export function ProductsTable({
  page,
  categories,
}: {
  page: Page<Product>;
  categories: Category[];
}) {
  const router = useRouter();
  const sp = useSearchParams();
  const [rows, setRows] = useState<Product[]>(page.content);
  const [busy, setBusy] = useState<Set<string>>(new Set());

  const catNameById = useMemo(() => {
    const m = new Map<string, string>();
    for (const c of categories) m.set(c.id, c.name);
    return m;
  }, [categories]);

  function update(key: string, value: string | undefined) {
    const next = new URLSearchParams(sp.toString());
    if (value) next.set(key, value);
    else next.delete(key);
    next.delete("page");
    router.push(`/admin/products?${next.toString()}`);
  }

  async function changeStatus(p: Product, next: "ACTIVATE" | "DEACTIVATE" | "DRAFT") {
    setBusy((b) => new Set(b).add(p.id));
    const prevStatus = p.status;
    const newStatus =
      next === "ACTIVATE" ? "ACTIVE" : next === "DEACTIVATE" ? "ARCHIVED" : "DRAFT";
    setRows((rs) => rs.map((r) => (r.id === p.id ? { ...r, status: newStatus } : r)));
    try {
      await api.post(`/api/products/${p.id}/status/${next}`);
      toast.success(`Đã chuyển trạng thái ${p.name} → ${STATUS_LABEL[newStatus]}`);
    } catch (e: unknown) {
      setRows((rs) => rs.map((r) => (r.id === p.id ? { ...r, status: prevStatus } : r)));
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Đổi trạng thái thất bại");
    } finally {
      setBusy((b) => {
        const n = new Set(b);
        n.delete(p.id);
        return n;
      });
    }
  }

  async function softDelete(p: Product) {
    setBusy((b) => new Set(b).add(p.id));
    try {
      await api.delete(`/api/products/${p.id}`);
      setRows((rs) => rs.filter((r) => r.id !== p.id));
      toast.success(`Đã xóa "${p.name}"`);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Xóa thất bại");
    } finally {
      setBusy((b) => {
        const n = new Set(b);
        n.delete(p.id);
        return n;
      });
    }
  }

  const columns: ColumnsType<Product> = [
    {
      title: "Sản phẩm",
      key: "name",
      width: 360,
      fixed: "left",
      render: (_, p) => (
        <div className="flex items-center gap-3">
          {p.thumbnail ? (
            <Image
              src={p.thumbnail}
              alt={p.name}
              width={56}
              height={56}
              preview={false}
              fallback="/img/placeholder.svg"
              style={{ objectFit: "cover", background: "#f7f9fb", borderRadius: 6 }}
            />
          ) : (
            <div className="w-14 h-14 rounded bg-surface-soft" />
          )}
          <div className="min-w-0">
            <Link href={`/admin/products/${p.id}`} className="font-medium hover:text-primary line-clamp-2">
              {p.name}
            </Link>
            <div className="text-xs text-slate mt-0.5 flex items-center gap-2 flex-wrap">
              <span className="font-mono">#{p.id}</span>
              {p.brand && <span>• {p.brand}</span>}
              {p.slug && (
                <Link href={`/p/${p.slug}`} className="text-blue-600 hover:underline truncate max-w-[160px]" target="_blank">
                  /p/{p.slug}
                </Link>
              )}
            </div>
          </div>
        </div>
      ),
    },
    {
      title: "Danh mục",
      key: "category",
      width: 140,
      render: (_, p) =>
        p.categoryId && catNameById.has(p.categoryId)
          ? <Tag color="blue">{catNameById.get(p.categoryId)}</Tag>
          : <span className="text-slate text-xs">—</span>,
    },
    {
      title: "Biến thể",
      key: "variants",
      width: 100,
      align: "center",
      render: (_, p) => {
        const sale = p.variants.filter((v) => v.salePrice && Number(v.salePrice) < Number(v.price)).length;
        return (
          <Tooltip title={sale ? `${sale} đang sale` : "Không có sale"}>
            <Tag>{p.variants.length}</Tag>
          </Tooltip>
        );
      },
    },
    {
      title: "Tồn kho",
      key: "stock",
      width: 110,
      align: "right",
      render: (_, p) => {
        const total = p.variants.reduce((s, v) => s + (v.quantity ?? 0), 0);
        const low = p.variants.some((v) => (v.quantity ?? 0) <= 5);
        return (
          <span className={low ? "text-amber-600 font-semibold" : ""}>
            {total.toLocaleString("vi-VN")}
          </span>
        );
      },
    },
    {
      title: "Giá (từ)",
      key: "price",
      width: 150,
      align: "right",
      render: (_, p) => {
        const min = p.variants.reduce<number | null>((m, v) => {
          const n = Number(v.salePrice ?? v.price);
          return m === null || n < m ? n : m;
        }, null);
        const max = p.variants.reduce<number | null>((m, v) => {
          const n = Number(v.salePrice ?? v.price);
          return m === null || n > m ? n : m;
        }, null);
        if (min === null) return "—";
        return (
          <div className="text-right">
            <div className="font-semibold">{formatVnd(min)}</div>
            {max !== null && max !== min && (
              <div className="text-xs text-slate">đến {formatVnd(max)}</div>
            )}
          </div>
        );
      },
    },
    {
      title: "Trạng thái",
      key: "status",
      width: 160,
      render: (_, p) => (
        <Space size={6}>
          <Switch
            size="small"
            checked={p.status === "ACTIVE"}
            loading={busy.has(p.id)}
            onChange={(on) => changeStatus(p, on ? "ACTIVATE" : "DEACTIVATE")}
          />
          <span className="text-xs text-on-surface-variant">
            {STATUS_LABEL[p.status] ?? p.status}
          </span>
        </Space>
      ),
    },
    {
      title: "Cập nhật",
      dataIndex: "updatedAt",
      width: 130,
      render: (v: string) => (
        <Tooltip title={v ? formatDate(v, true) : "—"}>
          <span className="text-xs text-on-surface-variant">{v ? formatDate(v) : "—"}</span>
        </Tooltip>
      ),
    },
    {
      title: "",
      key: "actions",
      width: 70,
      fixed: "right",
      render: (_, p) => (
        <Dropdown
          menu={{
            items: [
              { key: "edit", icon: <EditOutlined />, label: <Link href={`/admin/products/${p.id}`}>Chỉnh sửa</Link> },
              { key: "view", icon: <EyeOutlined />, label: <Link href={`/p/${p.slug}`} target="_blank">Xem storefront</Link> },
              { key: "dup", icon: <CopyOutlined />, label: <Link href={`/admin/products/new?clone=${p.id}`}>Nhân bản</Link> },
              { key: "draft", icon: <FileTextOutlined />, label: "Chuyển bản nháp", onClick: () => changeStatus(p, "DRAFT") },
              { type: "divider" },
              {
                key: "del",
                icon: <DeleteOutlined />,
                danger: true,
                label: (
                  <Popconfirm
                    title={`Xóa "${p.name}"?`}
                    description="Có thể khôi phục từ Lưu trữ"
                    okText="Xóa"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => softDelete(p)}
                  >
                    <span>Xóa</span>
                  </Popconfirm>
                ),
              },
            ],
          }}
          trigger={["click"]}
        >
          <Button type="text" icon={<MoreOutlined />} />
        </Dropdown>
      ),
    },
  ];

  return (
    <>
      <Space className="mb-4" wrap>
        <Input.Search
          placeholder="Tìm tên, SKU, mã sản phẩm..."
          defaultValue={sp.get("q") || ""}
          onSearch={(v) => update("q", v)}
          allowClear
          style={{ width: 300 }}
        />
        <Select
          placeholder="Trạng thái"
          allowClear
          style={{ width: 160 }}
          defaultValue={sp.get("status") || undefined}
          onChange={(v) => update("status", v)}
          options={[
            { value: "ACTIVE", label: "Đang bán" },
            { value: "DRAFT", label: "Bản nháp" },
            { value: "ARCHIVED", label: "Lưu trữ" },
          ]}
        />
        <Select
          placeholder="Danh mục"
          allowClear
          showSearch
          optionFilterProp="label"
          style={{ width: 200 }}
          defaultValue={sp.get("categoryId") ? Number(sp.get("categoryId")) : undefined}
          onChange={(v) => update("categoryId", v != null ? String(v) : undefined)}
          options={categories.map((c) => ({ value: c.id, label: c.name }))}
        />
      </Space>
      <Table
        rowKey="id"
        dataSource={rows}
        columns={columns}
        scroll={{ x: 1100 }}
        size="middle"
        pagination={{
          current: page.number + 1,
          pageSize: page.size,
          total: page.totalElements,
          showSizeChanger: false,
          showTotal: (total) => `${total} sản phẩm`,
          onChange: (p) => {
            const next = new URLSearchParams(sp.toString());
            next.set("page", String(p - 1));
            router.push(`/admin/products?${next.toString()}`);
          },
        }}
      />
    </>
  );
}
