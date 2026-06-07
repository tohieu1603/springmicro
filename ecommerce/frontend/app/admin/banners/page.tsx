"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Form, Input, InputNumber, Modal, Switch, Table, Tag } from "antd";
import { useState } from "react";
import { toast } from "sonner";

import { api } from "@/lib/api/client";
import { qk } from "@/lib/query/keys";

interface Banner {
  id: string;
  title: string;
  subtitle?: string;
  imageUrl: string;
  targetUrl?: string;
  ctaLabel?: string;
  enabled: boolean;
  displayOrder: number;
  startsAt?: string;
  endsAt?: string;
}

/**
 * Admin hero banner manager — TanStack Query edition. Storefront's
 * {@code qk.banners.active()} query is invalidated on every mutation so the
 * homepage slideshow picks up changes without a hard reload.
 */
export default function BannersAdmin() {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<Banner | null>(null);
  const [form] = Form.useForm<Banner>();

  const { data: rows = [], isLoading } = useQuery({
    queryKey: qk.banners.admin(),
    queryFn: async () => {
      const res = await api.get<Banner[] | { data: Banner[] }>("/api/banners/admin");
      const body = res.data as { data?: Banner[] };
      return body.data ?? (res.data as Banner[]);
    },
    staleTime: 30_000,
  });

  // Optimistic inline patch (used by Switch + InputNumber).
  const patchMut = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<Banner> }) =>
      api.patch(`/api/banners/admin/${id}`, patch),
    onMutate: async ({ id, patch }) => {
      await qc.cancelQueries({ queryKey: qk.banners.admin() });
      const prev = qc.getQueryData<Banner[]>(qk.banners.admin()) ?? [];
      qc.setQueryData<Banner[]>(qk.banners.admin(),
        prev.map((b) => (b.id === id ? { ...b, ...patch } : b)));
      return { prev };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(qk.banners.admin(), ctx.prev);
      toast.error("Cập nhật thất bại");
    },
    onSettled: () => qc.invalidateQueries({ queryKey: qk.banners.active() }),
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => api.delete(`/api/banners/admin/${id}`),
    onSuccess: () => {
      toast.success("Đã xoá");
      qc.invalidateQueries({ queryKey: qk.banners.admin() });
      qc.invalidateQueries({ queryKey: qk.banners.active() });
    },
    onError: () => toast.error("Xoá thất bại"),
  });

  const saveMut = useMutation({
    mutationFn: async (values: Banner) => {
      if (editing && editing.id) {
        await api.patch(`/api/banners/admin/${editing.id}`, values);
      } else {
        await api.post("/api/banners/admin", values);
      }
    },
    onSuccess: () => {
      toast.success("Đã lưu");
      setEditing(null);
      form.resetFields();
      qc.invalidateQueries({ queryKey: qk.banners.admin() });
      qc.invalidateQueries({ queryKey: qk.banners.active() });
    },
    onError: () => toast.error("Lưu thất bại"),
  });

  const remove = (id: string) => {
    if (!window.confirm("Xoá banner này?")) return;
    deleteMut.mutate(id);
  };

  return (
    <Card
      title="Hero banner — Slideshow trang chủ"
      extra={
        <Button
          type="primary"
          onClick={() => { setEditing({ id: '' } as Banner); form.resetFields(); }}
        >
          + Thêm banner
        </Button>
      }
    >
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={rows}
        pagination={false}
        columns={[
          {
            title: "Ảnh", key: "img", width: 100,
            render: (_, b) => (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={b.imageUrl} alt={b.title}
                   style={{ width: 80, height: 50, objectFit: "cover", borderRadius: 4 }} />
            ),
          },
          {
            title: "Tiêu đề", key: "title",
            render: (_, b) => (
              <>
                <div className="font-medium">{b.title}</div>
                {b.subtitle && <div className="text-xs text-slate">{b.subtitle}</div>}
              </>
            ),
          },
          {
            title: "Link", key: "url",
            render: (_, b) => (
              b.targetUrl
                ? <a href={b.targetUrl} target="_blank" rel="noreferrer" className="text-xs">{b.targetUrl}</a>
                : <Tag>—</Tag>
            ),
          },
          {
            title: "Thứ tự", key: "order", width: 100,
            render: (_, b) => (
              <InputNumber size="small" min={0} max={9999} value={b.displayOrder}
                           onBlur={(e) => {
                             const n = Number((e.target as HTMLInputElement).value);
                             if (Number.isFinite(n) && n !== b.displayOrder) {
                               patchMut.mutate({ id: b.id, patch: { displayOrder: n } });
                             }
                           }} style={{ width: 70 }} />
            ),
          },
          {
            title: "Bật", key: "enabled", width: 70,
            render: (_, b) => (
              <Switch
                checked={b.enabled}
                onChange={(v) => patchMut.mutate({ id: b.id, patch: { enabled: v } })}
              />
            ),
          },
          {
            title: "", key: "actions", width: 150,
            render: (_, b) => (
              <>
                <Button size="small" onClick={() => { setEditing(b); form.setFieldsValue(b); }}>Sửa</Button>
                <Button size="small" danger className="ml-2" onClick={() => remove(b.id)}>Xoá</Button>
              </>
            ),
          },
        ]}
      />

      <Modal
        title={editing?.id ? `Sửa banner #${editing.id}` : "Thêm banner mới"}
        open={editing !== null}
        onCancel={() => { setEditing(null); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={saveMut.isPending}
        okText="Lưu"
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(v) => saveMut.mutate(v as Banner)}
          initialValues={{ enabled: true, displayOrder: 100 }}
        >
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true, max: 200 }]}>
            <Input />
          </Form.Item>
          <Form.Item name="subtitle" label="Phụ đề">
            <Input />
          </Form.Item>
          <Form.Item name="imageUrl" label="URL ảnh" rules={[{ required: true, type: "url" }]}>
            <Input placeholder="https://images.unsplash.com/..." />
          </Form.Item>
          <Form.Item name="targetUrl" label="URL đích (click banner đi đâu)">
            <Input placeholder="/shop?cat=1 hoặc https://..." />
          </Form.Item>
          <Form.Item name="ctaLabel" label="Nhãn nút CTA">
            <Input placeholder="VD: Khám phá ngay" />
          </Form.Item>
          <div className="grid grid-cols-2 gap-3">
            <Form.Item name="displayOrder" label="Thứ tự">
              <InputNumber min={0} max={9999} style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item name="enabled" label="Bật" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </Card>
  );
}
