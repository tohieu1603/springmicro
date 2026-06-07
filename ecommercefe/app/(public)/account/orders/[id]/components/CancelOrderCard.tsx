"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import type { Order } from "@/lib/api/types";

/**
 * Customer-side cancel CTA. Mirrors the BE policy in CancelOrderHandler:
 *   - Only shown when status is in the early-flow set (pre-CONFIRMED).
 *   - Requires a reason ≥ 5 chars before the request is even sent.
 *   - Surfaces the rate-limit (HTTP 429 → ORDER-CANCEL-RATE-LIMIT) as a
 *     dedicated toast so the user understands they need to wait.
 */
const CANCELLABLE = new Set([
  "PENDING",
  "INVENTORY_RESERVED",
  "PAYMENT_PENDING",
  "PAYMENT_FAILED",
]);

const REASONS = [
  "Đổi ý không muốn mua nữa",
  "Tìm được giá tốt hơn ở nơi khác",
  "Thông tin sản phẩm không như mong đợi",
  "Chậm xác nhận đơn hàng",
  "Muốn đổi địa chỉ / phương thức thanh toán",
  "Lý do khác",
];

export function CancelOrderCard({ order }: { order: Order }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reasonKey, setReasonKey] = useState<string>(REASONS[0]);
  const [extra, setExtra] = useState("");
  const [busy, setBusy] = useState(false);

  if (!CANCELLABLE.has(order.status)) return null;

  const submit = async () => {
    const reason = reasonKey === "Lý do khác"
      ? extra.trim()
      : (extra.trim() ? `${reasonKey} — ${extra.trim()}` : reasonKey);
    if (reason.length < 5) {
      toast.error("Vui lòng nhập lý do hủy đơn (ít nhất 5 ký tự)");
      return;
    }
    setBusy(true);
    try {
      await api.delete(`/api/orders/${order.id}`, { data: { reason } });
      toast.success("Đã hủy đơn hàng");
      router.refresh();
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { code?: string; message?: string } } };
      const code = err.response?.data?.code;
      if (err.response?.status === 429 || code === "ORDER-CANCEL-RATE-LIMIT") {
        toast.error("Bạn đã hủy quá nhiều đơn gần đây. Vui lòng thử lại sau.");
      } else if (code === "ORDER-CANCEL-NOT-ALLOWED") {
        toast.error("Đơn không thể hủy ở trạng thái hiện tại.");
        router.refresh();
      } else {
        toast.error(err.response?.data?.message || "Hủy đơn thất bại");
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded border border-border-base bg-white p-5">
      <h2 className="text-h3-d mb-2">Hủy đơn hàng</h2>
      <p className="text-xs text-slate mb-3">
        Bạn có thể hủy khi đơn còn ở trạng thái sớm. Sau khi shop xác nhận hoặc giao hàng,
        vui lòng dùng chức năng "Trả hàng" thay thế.
      </p>

      {!open ? (
        <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
          Hủy đơn này
        </Button>
      ) : (
        <div className="space-y-3">
          <div className="space-y-1">
            {REASONS.map((r) => (
              <label key={r} className="flex items-start gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="cancel-reason"
                  value={r}
                  checked={reasonKey === r}
                  onChange={(e) => setReasonKey(e.target.value)}
                  className="mt-1"
                />
                <span>{r}</span>
              </label>
            ))}
          </div>
          <textarea
            placeholder={
              reasonKey === "Lý do khác"
                ? "Vui lòng mô tả lý do (≥ 5 ký tự)"
                : "Ghi chú thêm (tùy chọn)"
            }
            value={extra}
            onChange={(e) => setExtra(e.target.value)}
            className="w-full border border-border-base rounded p-2 text-sm"
            rows={2}
          />
          <div className="flex gap-2">
            <Button variant="secondary" size="sm" onClick={() => setOpen(false)} disabled={busy}>
              Quay lại
            </Button>
            <Button variant="cta" size="sm" onClick={submit} disabled={busy}>
              {busy ? "Đang hủy…" : "Xác nhận hủy"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
