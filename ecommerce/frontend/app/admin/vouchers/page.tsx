"use client";

import { useEffect, useState } from "react";
import { Card, Table, Tag, Button, Modal, Form, Input, InputNumber, DatePicker, Select, Space, Switch } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import dayjs, { Dayjs } from "dayjs";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { formatVnd, formatDate } from "@/lib/utils";
import type { Voucher } from "@/lib/api/types";

interface PageDto<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
}

export default function VouchersAdmin() {
  const [data, setData] = useState<PageDto<Voucher> | null>(null);
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  async function load() {
    try {
      const res = await api.get<PageDto<Voucher> | { data: PageDto<Voucher> }>(
        `/api/vouchers?page=${page}&size=20`,
      );
      const body = res.data as unknown as { data?: PageDto<Voucher> };
      setData(body?.data ?? (res.data as PageDto<Voucher>));
    } catch {
      toast.error("Không tải được mã giảm giá");
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function submit() {
    const v = await form.validateFields();
    const payload = {
      ...v,
      code: String(v.code).toUpperCase(),
      startDate: (v.dateRange?.[0] as Dayjs)?.toISOString(),
      endDate: (v.dateRange?.[1] as Dayjs)?.toISOString(),
    };
    delete (payload as any).dateRange;
    try {
      await api.post("/api/vouchers", payload);
      toast.success("Đã tạo voucher");
      setOpen(false);
      form.resetFields();
      load();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Tạo thất bại");
    }
  }

  return (
    <Card
      title="Mã giảm giá"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
          Tạo mã
        </Button>
      }
    >
      <Table
        rowKey="id"
        dataSource={data?.content ?? []}
        columns={[
          { title: "Mã", dataIndex: "code", render: (v: string) => <code className="font-mono">{v}</code> },
          { title: "Loại", dataIndex: "type" },
          {
            title: "Giá trị",
            key: "v",
            render: (_, v) =>
              v.type === "PERCENTAGE" ? `${v.discountValue}%` : formatVnd(v.discountValue),
          },
          {
            title: "Đơn tối thiểu",
            dataIndex: "minOrderAmount",
            render: (v?: string) => (v ? formatVnd(v) : "—"),
          },
          {
            title: "Sử dụng",
            key: "u",
            render: (_, v) => `${v.usedCount}${v.usageLimit ? ` / ${v.usageLimit}` : ""}`,
          },
          {
            title: "Hiệu lực",
            key: "exp",
            render: (_, v) =>
              v.endDate ? formatDate(v.endDate) : "Không giới hạn",
          },
          {
            title: "Active",
            dataIndex: "active",
            render: (a: boolean) => (a ? <Tag color="green">Hoạt động</Tag> : <Tag>Tạm dừng</Tag>),
          },
        ]}
        pagination={{
          current: page + 1,
          pageSize: data?.size ?? 20,
          total: data?.totalElements ?? 0,
          onChange: (p) => setPage(p - 1),
        }}
      />

      <Modal open={open} title="Tạo mã giảm giá" onCancel={() => setOpen(false)} onOk={submit} okText="Tạo" cancelText="Hủy" destroyOnClose>
        <Form form={form} layout="vertical" initialValues={{ type: "PERCENTAGE", active: true }}>
          <div className="grid grid-cols-2 gap-3">
            <Form.Item name="code" label="Mã" rules={[{ required: true }]}>
              <Input placeholder="WELCOME10" />
            </Form.Item>
            <Form.Item name="type" label="Loại">
              <Select
                options={[
                  { value: "PERCENTAGE", label: "Phần trăm" },
                  { value: "FIXED_AMOUNT", label: "Số tiền cố định" },
                ]}
              />
            </Form.Item>
            <Form.Item name="discountValue" label="Giá trị" rules={[{ required: true }]}>
              <InputNumber style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item name="minOrderAmount" label="Đơn tối thiểu">
              <InputNumber style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item name="maxDiscountAmount" label="Giảm tối đa">
              <InputNumber style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item name="usageLimit" label="Số lượt tối đa">
              <InputNumber style={{ width: "100%" }} />
            </Form.Item>
          </div>
          <Form.Item name="dateRange" label="Hiệu lực">
            <DatePicker.RangePicker style={{ width: "100%" }} showTime />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
