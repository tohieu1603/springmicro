"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import {
  Table, Tag, Avatar, Space, Input, Button, Dropdown, Popconfirm, Tooltip, Modal, Select,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  MoreOutlined, EyeOutlined, LockOutlined, UnlockOutlined,
  CheckCircleOutlined, StopOutlined, MailOutlined, SafetyOutlined,
} from "@ant-design/icons";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { formatDate, formatVnd } from "@/lib/utils";

export interface CustomerRow {
  id: string;
  username: string;
  fullName?: string;
  firstName?: string;
  lastName?: string;
  email: string;
  active?: boolean;
  ordersCount: number;
  lifetimeValue: number;
  lastOrderAt?: string;
  segment?: "NEW" | "REGULAR" | "VIP" | "DORMANT";
  createdAt?: string;
  lastLogin?: string;
  roles?: string[];
}

interface PageData {
  content: CustomerRow[];
  totalElements: number;
  size: number;
  number: number;
}

const SEGMENT_TONE: Record<string, string> = {
  NEW: "blue", REGULAR: "default", VIP: "gold", DORMANT: "orange",
};

const ROLE_TONE: Record<string, string> = {
  ROLE_ADMIN: "red", ROLE_MANAGER: "purple", ROLE_USER: "blue",
};

/**
 * Customer admin list. Inline actions:
 *   • Lock / Unlock — BE: POST /api/users/{id}/status/LOCK|UNLOCK
 *   • Disable / Enable — BE: POST /api/users/{id}/status/DISABLE|ENABLE
 *   • Assign role — BE: POST /api/users/{id}/roles + body {roleName}
 *
 * Optimistic UI: state updates immediately; revert on error.
 */
export function CustomersTable({ page }: { page: PageData }) {
  const router = useRouter();
  const sp = useSearchParams();
  const [rows, setRows] = useState<CustomerRow[]>(page.content);
  const [busy, setBusy] = useState<Set<string>>(new Set());
  const [roleOf, setRoleOf] = useState<CustomerRow | null>(null);
  const [roleName, setRoleName] = useState("ROLE_USER");

  function patch(key: string, value: string | undefined) {
    const next = new URLSearchParams(sp.toString());
    if (value) next.set(key, value);
    else next.delete(key);
    next.delete("page");
    router.push(`/admin/customers?${next.toString()}`);
  }

  async function changeStatus(u: CustomerRow, t: "LOCK" | "UNLOCK" | "ENABLE" | "DISABLE") {
    setBusy((b) => new Set(b).add(u.id));
    const prev = u.active;
    setRows((rs) =>
      rs.map((r) =>
        r.id === u.id
          ? { ...r, active: t === "ENABLE" || t === "UNLOCK" ? true : false }
          : r,
      ),
    );
    try {
      await api.post(`/api/users/${u.id}/status/${t}`);
      toast.success(`Đã ${t === "LOCK" ? "khóa" : t === "UNLOCK" ? "mở khóa" : t === "ENABLE" ? "kích hoạt" : "vô hiệu"} ${u.username}`);
    } catch (e: unknown) {
      setRows((rs) => rs.map((r) => (r.id === u.id ? { ...r, active: prev } : r)));
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Thao tác thất bại");
    } finally {
      setBusy((b) => { const n = new Set(b); n.delete(u.id); return n; });
    }
  }

  async function assignRole() {
    if (!roleOf) return;
    setBusy((b) => new Set(b).add(roleOf.id));
    try {
      await api.post(`/api/users/${roleOf.id}/roles`, { roleName });
      setRows((rs) =>
        rs.map((r) =>
          r.id === roleOf.id
            ? { ...r, roles: [...(r.roles ?? []), roleName] }
            : r,
        ),
      );
      toast.success(`Đã gán ${roleName} cho ${roleOf.username}`);
      setRoleOf(null);
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Gán vai trò thất bại");
    } finally {
      setBusy((b) => { const n = new Set(b); n.delete(roleOf.id); return n; });
    }
  }

  function copyEmail(email: string) {
    navigator.clipboard.writeText(email).then(
      () => toast.success("Đã sao chép email"),
      () => toast.error("Không sao chép được"),
    );
  }

  const columns: ColumnsType<CustomerRow> = [
    {
      title: "Khách hàng",
      key: "name",
      width: 280,
      fixed: "left",
      render: (_, c) => {
        const displayName =
          c.fullName?.trim() ||
          [c.lastName, c.firstName].filter(Boolean).join(" ").trim() ||
          c.username;
        return (
          <Link href={`/admin/customers/${c.id}`} className="flex items-center gap-3 group min-w-0">
            <Avatar style={{ background: "#1A2B3C", flexShrink: 0 }} size={40}>
              {displayName.charAt(0).toUpperCase()}
            </Avatar>
            <div className="min-w-0">
              <div className="font-medium text-on-surface group-hover:text-primary line-clamp-1">
                {displayName}
              </div>
              <div className="text-xs text-slate flex items-center gap-1 truncate">
                @{c.username}
              </div>
            </div>
          </Link>
        );
      },
    },
    {
      title: "Email",
      key: "email",
      width: 240,
      render: (_, c) => (
        <Tooltip title="Click để copy">
          <button
            onClick={() => copyEmail(c.email)}
            className="text-sm text-blue-600 hover:underline text-left max-w-full truncate"
          >
            {c.email}
          </button>
        </Tooltip>
      ),
    },
    {
      title: "Vai trò",
      dataIndex: "roles",
      width: 160,
      render: (roles?: string[]) =>
        roles && roles.length > 0
          ? roles.map((r) => (
              <Tag key={r} color={ROLE_TONE[r] || "default"}>{r.replace("ROLE_", "")}</Tag>
            ))
          : <Tag>USER</Tag>,
    },
    {
      title: "Đơn",
      dataIndex: "ordersCount",
      width: 80,
      align: "right",
      render: (n: number) => <span className="font-semibold">{n}</span>,
    },
    {
      title: "LTV",
      dataIndex: "lifetimeValue",
      width: 130,
      align: "right",
      render: (v: number) => (
        v > 0 ? <span className="font-semibold text-accent">{formatVnd(v)}</span> : <span className="text-slate">—</span>
      ),
    },
    {
      title: "Phân khúc",
      dataIndex: "segment",
      width: 110,
      render: (s?: string) => s ? <Tag color={SEGMENT_TONE[s]}>{s}</Tag> : <span className="text-slate text-xs">—</span>,
    },
    {
      title: "Trạng thái",
      key: "status",
      width: 130,
      render: (_, c) =>
        c.active ? (
          <Tag color="green" icon={<CheckCircleOutlined />}>Hoạt động</Tag>
        ) : (
          <Tag color="red" icon={<StopOutlined />}>Đã khóa</Tag>
        ),
    },
    {
      title: "Tham gia",
      dataIndex: "createdAt",
      width: 110,
      render: (v?: string) => (
        <Tooltip title={v ? formatDate(v, true) : "—"}>
          <span className="text-xs text-slate">{v ? formatDate(v) : "—"}</span>
        </Tooltip>
      ),
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 250,
      fixed: "right",
      render: (_, c) => (
        <Space size={4}>
          <Tooltip title="Xem chi tiết">
            <Link href={`/admin/customers/${c.id}`}>
              <Button size="small" icon={<EyeOutlined />} />
            </Link>
          </Tooltip>
          <Tooltip title="Gán vai trò">
            <Button
              size="small"
              icon={<SafetyOutlined />}
              onClick={() => setRoleOf(c)}
              loading={busy.has(c.id)}
            />
          </Tooltip>
          {c.active ? (
            <Popconfirm
              title={`Khóa ${c.username}?`}
              description="User sẽ không đăng nhập được"
              okText="Khóa"
              okButtonProps={{ danger: true }}
              onConfirm={() => changeStatus(c, "LOCK")}
            >
              <Tooltip title="Khóa tài khoản">
                <Button size="small" danger icon={<LockOutlined />} loading={busy.has(c.id)} />
              </Tooltip>
            </Popconfirm>
          ) : (
            <Tooltip title="Mở khóa">
              <Button
                size="small"
                icon={<UnlockOutlined />}
                onClick={() => changeStatus(c, "UNLOCK")}
                loading={busy.has(c.id)}
              />
            </Tooltip>
          )}
          <Dropdown
            menu={{
              items: [
                {
                  key: "email",
                  icon: <MailOutlined />,
                  label: <a href={`mailto:${c.email}`}>Gửi email</a>,
                },
                {
                  key: "enable",
                  icon: <CheckCircleOutlined />,
                  label: "Kích hoạt",
                  onClick: () => changeStatus(c, "ENABLE"),
                },
                {
                  key: "disable",
                  icon: <StopOutlined />,
                  danger: true,
                  label: "Vô hiệu hóa",
                  onClick: () => changeStatus(c, "DISABLE"),
                },
              ],
            }}
            trigger={["click"]}
          >
            <Button size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Space className="mb-4" wrap>
        <Input.Search
          placeholder="Tìm theo tên, email, username..."
          defaultValue={sp.get("q") || ""}
          allowClear
          onSearch={(v) => patch("q", v)}
          style={{ width: 280 }}
        />
        <Select
          placeholder="Trạng thái"
          allowClear
          style={{ width: 140 }}
          options={[
            { value: "active", label: "Hoạt động" },
            { value: "locked", label: "Đã khóa" },
          ]}
          onChange={(v) => patch("status", v)}
        />
      </Space>

      <Table
        rowKey="id"
        dataSource={rows}
        columns={columns}
        scroll={{ x: 1200 }}
        size="middle"
        pagination={{
          current: page.number + 1,
          pageSize: page.size,
          total: page.totalElements,
          showSizeChanger: false,
          showTotal: (t) => `${t} khách hàng`,
          onChange: (p) => {
            const next = new URLSearchParams(sp.toString());
            next.set("page", String(p - 1));
            router.push(`/admin/customers?${next.toString()}`);
          },
        }}
      />

      <Modal
        open={!!roleOf}
        title={`Gán vai trò cho ${roleOf?.username ?? ""}`}
        onCancel={() => setRoleOf(null)}
        onOk={assignRole}
        okText="Gán"
        cancelText="Hủy"
        destroyOnHidden
      >
        <p className="text-sm text-slate mb-3">
          Vai trò hiện tại: <b>{(roleOf?.roles ?? ["ROLE_USER"]).join(", ")}</b>
        </p>
        <Select
          value={roleName}
          onChange={setRoleName}
          style={{ width: "100%" }}
          options={[
            { value: "ROLE_USER", label: "Khách hàng (USER)" },
            { value: "ROLE_MANAGER", label: "Quản lý (MANAGER)" },
            { value: "ROLE_ADMIN", label: "Quản trị (ADMIN)" },
          ]}
        />
      </Modal>
    </>
  );
}
