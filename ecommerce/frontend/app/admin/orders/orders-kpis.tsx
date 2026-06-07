"use client";

import { Row, Col, Card, Statistic } from "antd";
import {
  ShoppingOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  DollarOutlined,
} from "@ant-design/icons";

interface Props {
  totalFiltered: number;
  pendingPayment: number;
  deliveredRecent: number;
  revenueSample: number;
}

/**
 * Order KPI strip. Client island because antd <Statistic> takes a function
 * `formatter` — can't cross RSC→CC boundary.
 */
export function OrdersKpis({
  totalFiltered, pendingPayment, deliveredRecent, revenueSample,
}: Props) {
  const vnd = (v: string | number) => new Intl.NumberFormat("vi-VN").format(Number(v));
  return (
    <Row gutter={[12, 12]}>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Tổng đơn (lọc hiện tại)" value={totalFiltered} prefix={<ShoppingOutlined />} />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Chờ thanh toán" value={pendingPayment} prefix={<ClockCircleOutlined />}
            valueStyle={{ color: "#F59E0B" }} />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Đã giao (gần đây)" value={deliveredRecent} prefix={<CheckCircleOutlined />}
            valueStyle={{ color: "#10B981" }} />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Doanh thu mẫu" value={revenueSample} suffix="₫"
            prefix={<DollarOutlined />}
            valueStyle={{ color: "#FF6B35" }}
            formatter={vnd}
          />
        </Card>
      </Col>
    </Row>
  );
}
