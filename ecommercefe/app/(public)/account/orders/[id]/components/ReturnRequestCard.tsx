"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import type { Order } from "@/lib/api/types";

/**
 * Customer-side return CTA. Mirrors the BE policy in RequestReturnHandler:
 *   - Only after the order is DELIVERED (BE state machine enforces the rest).
 *   - Within 7 days from {@code deliveredAt} — BE re-validates, this gate
 *     is just so the button disappears when it's obvious.
 *   - Surfaces InvalidOrderState (400) and the rate limit (429) as toasts.
 */
const RETURN_WINDOW_DAYS = 7;

const REASONS = [
  "Sản phẩm bị lỗi / hư hỏng",
  "Không đúng mô tả / hình ảnh",
  "Giao sai sản phẩm",
  "Không vừa size / màu sắc",
  "Đổi ý không muốn dùng nữa",
  "Lý do khác",
];

const RETURN_TYPES = [
  { value: "REFUND",   label: "Hoàn tiền" },
  { value: "EXCHANGE", label: "Đổi sản phẩm khác" },
];

export function ReturnRequestCard({ order }: { order: Order }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [reasonKey, setReasonKey] = useState(REASONS[0]);
  const [extra, setExtra] = useState("");
  const [returnType, setReturnType] = useState<string>("REFUND");
  const [busy, setBusy] = useState(false);

  if (order.status !== "DELIVERED" || !withinWindow(order)) return null;

  const submit = async () => {
    const reason = reasonKey === "Lý do khác"
      ? extra.trim()
      : (extra.trim() ? `${reasonKey} — ${extra.trim()}` : reasonKey);
    if (reason.length < 5) {
      toast.error("Vui lòng nhập lý do trả hàng (≥ 5 ký tự)");
      return;
    }
    setBusy(true);
    try {
      await api.post(`/api/orders/return-requests/${order.id}`, {
        reason, returnType, images: null,
      });
      toast.success("Đã gửi yêu cầu trả hàng — đội hỗ trợ sẽ liên hệ trong 24 giờ");
      router.refresh();
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Gửi yêu cầu thất bại");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rounded border border-border-base bg-white p-5">
      <h2 className="text-h3-d mb-2">Yêu cầu trả hàng</h2>
      <p className="text-xs text-slate mb-3">
        Đơn đã giao trong vòng {RETURN_WINDOW_DAYS} ngày — bạn có thể yêu cầu hoàn tiền
        hoặc đổi sản phẩm khác.
      </p>

      {!open ? (
        <Button variant="secondary" size="sm" onClick={() => setOpen(true)}>
          Tạo yêu cầu trả hàng
        </Button>
      ) : (
        <div className="space-y-3">
          <div>
            <label className="text-xs text-slate block mb-1">Hình thức</label>
            <div className="flex gap-3">
              {RETURN_TYPES.map((t) => (
                <label key={t.value} className="flex items-center gap-1.5 text-sm cursor-pointer">
                  <input
                    type="radio"
                    name="return-type"
                    value={t.value}
                    checked={returnType === t.value}
                    onChange={(e) => setReturnType(e.target.value)}
                  />
                  <span>{t.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs text-slate block">Lý do</label>
            {REASONS.map((r) => (
              <label key={r} className="flex items-start gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="return-reason"
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
                ? "Mô tả vấn đề chi tiết (≥ 5 ký tự)"
                : "Ghi chú thêm (tuỳ chọn)"
            }
            value={extra}
            onChange={(e) => setExtra(e.target.value)}
            className="w-full border border-border-base rounded p-2 text-sm"
            rows={3}
          />

          <div className="flex gap-2">
            <Button variant="secondary" size="sm" onClick={() => setOpen(false)} disabled={busy}>
              Quay lại
            </Button>
            <Button variant="cta" size="sm" onClick={submit} disabled={busy}>
              {busy ? "Đang gửi…" : "Gửi yêu cầu"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function withinWindow(order: Order): boolean {
  const delivered = (order as Order & { deliveredAt?: string }).deliveredAt;
  if (!delivered) return false;
  const elapsedMs = Date.now() - new Date(delivered).getTime();
  return elapsedMs < RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
}
