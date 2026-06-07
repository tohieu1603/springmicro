"use client";

import { useEffect, useState } from "react";
import { Card, Table, Tag, Modal, Form, Input, Checkbox, Button, Space, Popconfirm } from "antd";
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import { toast } from "sonner";
import { api } from "@/lib/api/client";

interface Role {
  id: string;
  name: string;
  description?: string;
  permissions: string[];
}

const PERMISSIONS_GROUPS = [
  {
    title: "Sản phẩm",
    items: ["PRODUCT_READ", "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE"],
  },
  { title: "Đơn hàng", items: ["ORDER_READ", "ORDER_UPDATE", "ORDER_CANCEL", "ORDER_REFUND"] },
  { title: "Kho", items: ["INVENTORY_READ", "INVENTORY_ADJUST"] },
  { title: "Khách hàng", items: ["USER_READ", "USER_UPDATE", "USER_DEACTIVATE"] },
  { title: "Marketing", items: ["VOUCHER_MANAGE", "BANNER_MANAGE", "CAMPAIGN_MANAGE"] },
  { title: "Cài đặt", items: ["SETTINGS_READ", "SETTINGS_UPDATE", "ROLE_MANAGE"] },
];

export default function PermissionsAdmin() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [editing, setEditing] = useState<Role | "new" | null>(null);
  const [form] = Form.useForm();

  async function load() {
    try {
      const res = await api.get<Role[] | { data: Role[] }>("/api/roles");
      const body = res.data as unknown as { data?: Role[] };
      setRoles(body?.data ?? (res.data as Role[]));
    } catch {
      setRoles([
        { id: "1", name: "ADMIN", description: "Quyền cao nhất", permissions: PERMISSIONS_GROUPS.flatMap((g) => g.items) },
        { id: "2", name: "STAFF", description: "Quản trị viên cơ bản", permissions: ["PRODUCT_READ", "ORDER_READ", "ORDER_UPDATE"] },
        { id: "3", name: "USER", description: "Khách hàng", permissions: [] },
      ]);
    }
  }
  useEffect(() => { load(); }, []);

  if (editing && editing !== "new") {
    setTimeout(() => form.setFieldsValue(editing), 0);
  }

  async function save() {
    const v = await form.validateFields();
    try {
      if (editing === "new") {
        await api.post("/api/roles", v);
        toast.success("Đã tạo vai trò");
      } else if (editing) {
        await api.patch(`/api/roles/${editing.id}`, v);
        toast.success("Đã cập nhật");
      }
      setEditing(null);
      form.resetFields();
      load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Lưu thất bại");
    }
  }

  return (
    <Card
      title="Vai trò & quyền hạn"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing("new")}>
          Thêm vai trò
        </Button>
      }
    >
      <Table
        rowKey="id"
        dataSource={roles}
        columns={[
          { title: "Vai trò", dataIndex: "name", render: (v) => <Tag color={v === "ADMIN" ? "red" : "blue"}>{v}</Tag> },
          { title: "Mô tả", dataIndex: "description" },
          { title: "Số quyền", key: "p", render: (_, r) => r.permissions.length },
          {
            title: "",
            key: "act",
            render: (_, r) => (
              <Space>
                <Button size="small" icon={<EditOutlined />} onClick={() => setEditing(r)} />
                <Popconfirm
                  title={`Xóa vai trò "${r.name}"?`}
                  onConfirm={async () => {
                    try {
                      await api.delete(`/api/roles/${r.id}`);
                      toast.success("Đã xóa");
                      load();
                    } catch (e: unknown) {
                      const err = e as { response?: { data?: { message?: string } } };
                      toast.error(err.response?.data?.message || "Không xóa được");
                    }
                  }}
                  okText="Xóa"
                  okButtonProps={{ danger: true }}
                >
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        open={editing !== null}
        title={editing === "new" ? "Thêm vai trò" : "Sửa vai trò"}
        onCancel={() => setEditing(null)}
        onOk={save}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnClose
        width={760}
      >
        <Form form={form} layout="vertical">
          <div className="grid grid-cols-2 gap-3">
            <Form.Item name="name" label="Tên vai trò" rules={[{ required: true }]}>
              <Input placeholder="STAFF" />
            </Form.Item>
            <Form.Item name="description" label="Mô tả">
              <Input />
            </Form.Item>
          </div>
          <Form.Item name="permissions" label="Quyền hạn">
            <Checkbox.Group style={{ width: "100%" }}>
              <div className="space-y-4">
                {PERMISSIONS_GROUPS.map((g) => (
                  <div key={g.title}>
                    <p className="font-semibold text-sm mb-2">{g.title}</p>
                    <div className="grid grid-cols-2 gap-1">
                      {g.items.map((p) => (
                        <Checkbox key={p} value={p}>{p}</Checkbox>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
