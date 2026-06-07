"use client";

import { Card, Row, Col, Statistic } from "antd";
import {
  DollarOutlined,
  ShoppingCartOutlined,
  UserAddOutlined,
  RiseOutlined,
} from "@ant-design/icons";

interface KpiProps {
  totalRevenue: number;
  ordersToday: number;
  newCustomers: number;
  conversionRate: number;
}

/**
 * KPI cards — extracted to a client island because antd <Statistic> takes a
 * `formatter` function prop, and functions can't cross the RSC/CC boundary.
 */
export function KpiCards({ totalRevenue, ordersToday, newCustomers, conversionRate }: KpiProps) {
  const vnd = (v: string | number) => new Intl.NumberFormat("vi-VN").format(Number(v));
  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Doanh thu (30 ngày)"
            value={totalRevenue}
            precision={0}
            valueStyle={{ color: "#FF6B35" }}
            prefix={<DollarOutlined />}
            suffix="₫"
            formatter={vnd}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Đơn hôm nay"
            value={ordersToday}
            valueStyle={{ color: "#1A2B3C" }}
            prefix={<ShoppingCartOutlined />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Khách mới (7 ngày)"
            value={newCustomers}
            prefix={<UserAddOutlined />}
            valueStyle={{ color: "#10B981" }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Tỉ lệ chuyển đổi"
            value={conversionRate * 100}
            precision={2}
            suffix="%"
            prefix={<RiseOutlined />}
            valueStyle={{ color: "#10B981" }}
          />
        </Card>
      </Col>
    </Row>
  );
}
