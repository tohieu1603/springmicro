"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Table, Tag, Space, Input, Switch, Popconfirm, Button } from "antd";
import type { ColumnsType } from "antd/es/table";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { formatDate } from "@/lib/utils";

interface UserRow {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
  active: boolean;
  createdAt?: string;
}

interface PageData {
  content: UserRow[];
  number: number;
  size: number;
  totalElements: number;
}

export function UsersTable({ page }: { page: PageData }) {
  const router = useRouter();
  const sp = useSearchParams();

  async function toggleActive(u: UserRow, next: boolean) {
    try {
      await api.patch(`/api/users/${u.id}/active`, { active: next });
      toast.success(next ? "Đã kích hoạt" : "Đã khóa");
      router.refresh();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Thao tác thất bại");
    }
  }

  const columns: ColumnsType<UserRow> = [
    { title: "Username", dataIndex: "username" },
    { title: "Họ tên", dataIndex: "fullName" },
    { title: "Email", dataIndex: "email" },
    {
      title: "Vai trò",
      dataIndex: "roles",
      render: (roles: string[]) =>
        roles?.map((r) => (
          <Tag key={r} color={r.includes("ADMIN") ? "red" : "blue"}>
            {r}
          </Tag>
        )),
    },
    {
      title: "Trạng thái",
      dataIndex: "active",
      width: 120,
      render: (active: boolean, u) => (
        <Switch checked={active} onChange={(v) => toggleActive(u, v)} />
      ),
    },
    {
      title: "Tham gia",
      dataIndex: "createdAt",
      render: (v?: string) => (v ? formatDate(v) : "—"),
    },
  ];

  return (
    <>
      <Space className="mb-4">
        <Input.Search
          placeholder="Tìm theo username, email..."
          defaultValue={sp.get("q") || ""}
          onSearch={(v) => {
            const next = new URLSearchParams(sp.toString());
            if (v) next.set("q", v);
            else next.delete("q");
            next.delete("page");
            router.push(`/admin/users?${next.toString()}`);
          }}
          style={{ width: 280 }}
          allowClear
        />
      </Space>
      <Table
        rowKey="id"
        dataSource={page.content}
        columns={columns}
        pagination={{
          current: page.number + 1,
          pageSize: page.size,
          total: page.totalElements,
          onChange: (p) => {
            const next = new URLSearchParams(sp.toString());
            next.set("page", String(p - 1));
            router.push(`/admin/users?${next.toString()}`);
          },
        }}
      />
    </>
  );
}
