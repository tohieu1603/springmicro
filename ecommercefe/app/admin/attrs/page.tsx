"use client";

import { useEffect, useState } from "react";
import { Card, Table, Button, Modal, Form, Input, Select, Space, Tag, Popconfirm } from "antd";
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import type { Attr } from "@/lib/api/types";

export default function AttrsAdmin() {
  const [attrs, setAttrs] = useState<Attr[]>([]);
  const [editing, setEditing] = useState<Attr | "new" | null>(null);
  const [form] = Form.useForm();

  async function load() {
    try {
      const res = await api.get<Attr[] | { data: Attr[] }>("/api/attrs");
      const body = res.data as unknown as { data?: Attr[] };
      setAttrs(body?.data ?? (res.data as Attr[]));
    } catch {
      toast.error("Không tải được thuộc tính");
    }
  }
  useEffect(() => { load(); }, []);

  async function submit() {
    const v = await form.validateFields();
    try {
      if (editing === "new") {
        await api.post("/api/attrs", v);
        toast.success("Đã tạo thuộc tính");
      } else if (editing) {
        await api.patch(`/api/attrs/${editing.id}`, v);
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

  if (editing && editing !== "new") {
    setTimeout(() => {
      form.setFieldsValue({
        code: editing.code,
        name: editing.name,
        type: editing.type,
        values: editing.values.map((v) => ({ code: v.code, val: v.val })),
      });
    }, 0);
  }

  return (
    <Card
      title="Thuộc tính sản phẩm"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing("new")}>
          Thêm thuộc tính
        </Button>
      }
    >
      <Table
        rowKey="id"
        dataSource={attrs}
        columns={[
          { title: "Code", dataIndex: "code", render: (v) => <code className="font-mono text-xs">{v}</code> },
          { title: "Tên", dataIndex: "name" },
          { title: "Kiểu", dataIndex: "type", render: (t) => <Tag>{t}</Tag> },
          {
            title: "Giá trị",
            dataIndex: "values",
            render: (vs: Attr["values"]) =>
              vs.length > 0 ? (
                <Space size={[4, 4]} wrap>
                  {vs.slice(0, 6).map((v) => (
                    <Tag key={v.id}>{v.val}</Tag>
                  ))}
                  {vs.length > 6 && <Tag>+{vs.length - 6}</Tag>}
                </Space>
              ) : (
                "—"
              ),
          },
          {
            title: "",
            key: "act",
            render: (_, attr) => (
              <Space>
                <Button size="small" icon={<EditOutlined />} onClick={() => setEditing(attr)} />
                <Popconfirm
                  title={`Xóa "${attr.name}"?`}
                  okText="Xóa"
                  okButtonProps={{ danger: true }}
                  onConfirm={async () => {
                    try {
                      await api.delete(`/api/attrs/${attr.id}`);
                      toast.success("Đã xóa");
                      load();
                    } catch (e: unknown) {
                      const err = e as { response?: { data?: { message?: string } } };
                      toast.error(err.response?.data?.message || "Không xóa được");
                    }
                  }}
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
        title={editing === "new" ? "Thêm thuộc tính" : "Chỉnh thuộc tính"}
        onCancel={() => setEditing(null)}
        onOk={submit}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnClose
        width={640}
      >
        <Form form={form} layout="vertical" initialValues={{ type: "SELECT", values: [] }}>
          <div className="grid grid-cols-2 gap-3">
            <Form.Item name="code" label="Code" rules={[{ required: true }]} tooltip="Định danh, không dấu, viết thường">
              <Input placeholder="size" />
            </Form.Item>
            <Form.Item name="name" label="Tên hiển thị" rules={[{ required: true }]}>
              <Input placeholder="Kích cỡ" />
            </Form.Item>
          </div>
          <Form.Item name="type" label="Kiểu" rules={[{ required: true }]}>
            <Select
              options={[
                { value: "SELECT", label: "Chọn từ danh sách" },
                { value: "TEXT", label: "Văn bản tự do" },
                { value: "NUMBER", label: "Số" },
              ]}
            />
          </Form.Item>

          <Form.List name="values">
            {(fields, { add, remove }) => (
              <>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-label-bold">Giá trị (chỉ áp dụng cho SELECT)</span>
                  <Button size="small" icon={<PlusOutlined />} onClick={() => add({ code: "", val: "" })}>
                    Thêm
                  </Button>
                </div>
                {fields.map((f) => (
                  <Space key={f.key} className="flex" align="baseline" style={{ marginBottom: 8 }}>
                    <Form.Item name={[f.name, "code"]} rules={[{ required: true }]} style={{ marginBottom: 0 }}>
                      <Input placeholder="m" />
                    </Form.Item>
                    <Form.Item name={[f.name, "val"]} rules={[{ required: true }]} style={{ marginBottom: 0 }}>
                      <Input placeholder="M" />
                    </Form.Item>
                    <Button danger type="text" icon={<DeleteOutlined />} onClick={() => remove(f.name)} />
                  </Space>
                ))}
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </Card>
  );
}
