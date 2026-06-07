"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Card, Input, Modal, Space, Tag } from "antd";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import type { Order } from "@/lib/api/types";

/**
 * Admin actions for an order. Each button calls the matching POST endpoint
 * on order-service ({@code /api/orders/:id/confirm|ship|deliver}) — those
 * route through the aggregate's state machine, so illegal moves (e.g. ship
 * before payment) return 400 with a domain message that we surface as a
 * toast.
 *
 * Cancel uses the existing DELETE endpoint with a reason payload; admin can
 * always cancel regardless of state (the BE saga handles inventory release).
 */
// PAYMENT_PENDING is included on Confirm because COD orders sit there until
// admin manually moves them — the BE handler folds the PAYMENT_COMPLETED
// transition into the confirm() call so the button does the right thing in
// one click.
const STEP = {
  confirm: { label: "Xác nhận đơn",  from: new Set(["PAYMENT_PENDING", "PAYMENT_COMPLETED"]) },
  ship:    { label: "Đánh dấu giao", from: new Set(["CONFIRMED"]) },
  deliver: { label: "Đã nhận hàng",  from: new Set(["SHIPPED"]) },
};

export function AdminOrderActions({ order }: { order: Order }) {
  const router = useRouter();
  const [busy, setBusy] = useState<string | null>(null);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");

  const call = async (action: "confirm" | "ship" | "deliver") => {
    setBusy(action);
    try {
      await api.post(`/api/orders/${order.id}/${action}`, action === "ship" ? {} : undefined);
      toast.success(`Đã ${STEP[action].label.toLowerCase()}`);
      router.refresh();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message ?? `Không thể ${STEP[action].label.toLowerCase()}`);
    } finally {
      setBusy(null);
    }
  };

  const cancel = async () => {
    if (cancelReason.trim().length < 5) {
      toast.error("Lý do tối thiểu 5 ký tự");
      return;
    }
    setBusy("cancel");
    try {
      await api.delete(`/api/orders/${order.id}`, { data: { reason: cancelReason } });
      toast.success("Đã huỷ đơn");
      setCancelOpen(false);
      router.refresh();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message ?? "Huỷ đơn thất bại");
    } finally {
      setBusy(null);
    }
  };

  // OrderStatus union evolves over time — compare via string set to avoid TS
  // narrowing breakage when new statuses are added (e.g. REFUNDED below).
  const TERMINAL: ReadonlySet<string> = new Set([
    "DELIVERED", "CANCELLED", "FAILED", "REFUNDED",
  ]);
  const noActions = TERMINAL.has(order.status);

  return (
    <Card title="Thao tác">
      {noActions ? (
        <Tag>Đơn đã kết thúc — không còn thao tác</Tag>
      ) : (
        <Space wrap>
          {(Object.keys(STEP) as Array<keyof typeof STEP>).map((k) => (
            <Button
              key={k}
              type="primary"
              loading={busy === k}
              disabled={!STEP[k].from.has(order.status)}
              onClick={() => call(k)}
            >
              {STEP[k].label}
            </Button>
          ))}
          <Button danger loading={busy === "cancel"} onClick={() => setCancelOpen(true)}>
            Huỷ đơn
          </Button>
        </Space>
      )}

      <Modal
        title="Huỷ đơn hàng"
        open={cancelOpen}
        confirmLoading={busy === "cancel"}
        onCancel={() => setCancelOpen(false)}
        onOk={cancel}
        okText="Xác nhận huỷ"
        okButtonProps={{ danger: true }}
      >
        <p className="text-sm text-slate mb-2">Lý do huỷ (≥ 5 ký tự):</p>
        <Input.TextArea
          rows={3}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          placeholder="VD: Khách yêu cầu huỷ qua hotline, kho hết stock, …"
        />
      </Modal>
    </Card>
  );
}
