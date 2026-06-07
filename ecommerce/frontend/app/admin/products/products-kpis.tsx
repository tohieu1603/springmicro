"use client";

import { Row, Col, Card, Statistic } from "antd";
import {
  AppstoreOutlined, FireOutlined, WarningOutlined, EyeInvisibleOutlined,
} from "@ant-design/icons";

export function ProductsKpis({
  total, active, lowStock, draft,
}: {
  total: number; active: number; lowStock: number; draft: number;
}) {
  return (
    <Row gutter={[12, 12]}>
      <Col xs={12} sm={6}>
        <Card><Statistic title="Tổng sản phẩm" value={total} prefix={<AppstoreOutlined />} /></Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Đang bán" value={active} prefix={<FireOutlined />}
            valueStyle={{ color: "#10B981" }} />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Sắp hết hàng (trang)" value={lowStock} prefix={<WarningOutlined />}
            valueStyle={{ color: "#F59E0B" }} />
        </Card>
      </Col>
      <Col xs={12} sm={6}>
        <Card>
          <Statistic title="Bản nháp" value={draft} prefix={<EyeInvisibleOutlined />}
            valueStyle={{ color: "#64748b" }} />
        </Card>
      </Col>
    </Row>
  );
}
