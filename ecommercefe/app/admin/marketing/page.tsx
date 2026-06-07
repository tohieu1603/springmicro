"use client";

import { Card, Tabs, Table, Tag, Button, Statistic, Row, Col, Empty } from "antd";
import { PlusOutlined, MailOutlined, NotificationOutlined } from "@ant-design/icons";
import { useState } from "react";
import { toast } from "sonner";

/**
 * Marketing hub — campaigns + email templates + push notifications.
 * Heavy work happens on the BE; the UI is the conductor.
 */
export default function MarketingPage() {
  const [tab, setTab] = useState("campaigns");

  return (
    <Card title="Marketing">
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          {
            key: "campaigns",
            label: "Chiến dịch",
            children: <Campaigns />,
          },
          {
            key: "emails",
            label: "Email template",
            children: <EmailTemplates />,
          },
          {
            key: "push",
            label: "Push notifications",
            children: <PushPanel />,
          },
        ]}
      />
    </Card>
  );
}

function Campaigns() {
  const data = [
    { id: 1, name: "Black Friday 2026", channel: "Email + Push", status: "DRAFT", reach: 0, conversion: 0 },
    { id: 2, name: "Welcome series", channel: "Email", status: "ACTIVE", reach: 4_280, conversion: 318 },
    { id: 3, name: "Abandoned cart", channel: "Email", status: "ACTIVE", reach: 1_240, conversion: 89 },
  ];
  return (
    <>
      <div className="flex justify-between items-center mb-3">
        <Row gutter={16}>
          <Col><Statistic title="Đang chạy" value={data.filter((d) => d.status === "ACTIVE").length} /></Col>
          <Col><Statistic title="Tổng tiếp cận" value={data.reduce((s, d) => s + d.reach, 0)} /></Col>
          <Col><Statistic title="Tổng chuyển đổi" value={data.reduce((s, d) => s + d.conversion, 0)} /></Col>
        </Row>
        <Button type="primary" icon={<PlusOutlined />}>Tạo chiến dịch</Button>
      </div>
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "Tên", dataIndex: "name" },
          { title: "Kênh", dataIndex: "channel" },
          {
            title: "Trạng thái",
            dataIndex: "status",
            render: (s) => <Tag color={s === "ACTIVE" ? "green" : "default"}>{s}</Tag>,
          },
          { title: "Tiếp cận", dataIndex: "reach" },
          { title: "Chuyển đổi", dataIndex: "conversion" },
        ]}
      />
    </>
  );
}

function EmailTemplates() {
  const templates = [
    { code: "WELCOME", name: "Chào mừng khách hàng mới", subject: "Chào mừng đến với {{brand}}" },
    { code: "ORDER_CONFIRM", name: "Xác nhận đơn hàng", subject: "Đơn {{orderNumber}} đã được xác nhận" },
    { code: "ORDER_SHIPPED", name: "Đơn đã giao vận chuyển", subject: "Đơn {{orderNumber}} đang được giao" },
    { code: "PASSWORD_RESET", name: "Đặt lại mật khẩu", subject: "Đặt lại mật khẩu {{brand}}" },
  ];
  return (
    <Table
      rowKey="code"
      dataSource={templates}
      columns={[
        { title: "Mã", dataIndex: "code", render: (c) => <code className="font-mono">{c}</code> },
        { title: "Tên", dataIndex: "name" },
        { title: "Tiêu đề", dataIndex: "subject" },
        {
          title: "",
          key: "act",
          render: () => <Button size="small" icon={<MailOutlined />}>Sửa</Button>,
        },
      ]}
    />
  );
}

function PushPanel() {
  return (
    <Empty
      image={<NotificationOutlined style={{ fontSize: 48, color: "#94a3b8" }} />}
      description={
        <div className="space-y-1 text-center">
          <p className="font-medium">Chưa có chiến dịch push nào</p>
          <p className="text-xs text-slate">Khi BE expose endpoint /api/push/campaigns FE sẽ tự render.</p>
        </div>
      }
    >
      <Button type="primary" onClick={() => toast.info("Mock — chưa wire BE")}>Gửi push</Button>
    </Empty>
  );
}
