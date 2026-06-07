"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { api } from "@/lib/api/client";

interface OrderShape {
  id: string;
  orderNumber: string;
  status: string;
  paymentMethod: string;
  totalAmount: number | string;
  qrCodeUrl?: string | null;
  payUrl?: string | null;
  bankCode?: string | null;
  bankAccount?: string | null;
  accountName?: string | null;
  transferContent?: string | null;
  createdAt?: string;
}

const QR_TTL_MS = 15 * 60 * 1000;
const POLL_MS = 4000;

function priceVND(v: number | string) {
  const n = typeof v === "string" ? Number(v) : v;
  if (!Number.isFinite(n)) return "";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n);
}

/**
 * SePay pending-payment screen.
 *
 * Two background loops:
 *   1. Status poll — every {@link POLL_MS}, refetches the order. When status
 *      flips out of PAYMENT_PENDING we toast + redirect to /track.
 *   2. TTL countdown — derived from createdAt + 15min. When expired we stop
 *      polling and show a "QR expired" panel with a retry CTA.
 *
 * The page handles both the happy path (user pays while watching) and the
 * "I'll come back later" path — landing here via /account/orders/pending
 * shows the same QR with the remaining time recomputed from the order's
 * createdAt timestamp.
 */
export function PaymentPendingClient({ orderNumber }: { orderNumber: string }) {
  const router = useRouter();
  const [order, setOrder] = useState<OrderShape | null>(null);
  const [loading, setLoading] = useState(true);
  const [now, setNow] = useState(Date.now());

  const load = useCallback(async () => {
    try {
      const res = await api.get<OrderShape | { data: OrderShape }>(
        `/api/orders/by-number/${encodeURIComponent(orderNumber)}`,
      );
      const body = res.data as { data?: OrderShape };
      const fresh = body.data ?? (res.data as OrderShape);
      setOrder(fresh);
      return fresh;
    } catch (e: unknown) {
      const err = e as { response?: { status?: number } };
      if (err.response?.status === 404) toast.error("Không tìm thấy đơn hàng");
      return null;
    } finally {
      setLoading(false);
    }
  }, [orderNumber]);

  useEffect(() => { load(); }, [load]);

  // Status poll: stops when order leaves PAYMENT_PENDING or page unmounts.
  useEffect(() => {
    if (!order || order.status !== "PAYMENT_PENDING") return;
    const id = window.setInterval(async () => {
      const fresh = await load();
      if (fresh && fresh.status !== "PAYMENT_PENDING") {
        window.clearInterval(id);
        if (fresh.status === "CONFIRMED" || fresh.status === "PAYMENT_COMPLETED" || fresh.status === "PAID") {
          toast.success("Thanh toán thành công!");
          router.push(`/track?o=${fresh.orderNumber}`);
        } else if (fresh.status === "CANCELLED" || fresh.status === "PAYMENT_FAILED") {
          toast.error("Đơn hàng đã bị huỷ");
        }
      }
    }, POLL_MS);
    return () => window.clearInterval(id);
  }, [order, load, router]);

  // Tick the countdown display once per second.
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);

  const expiresAt = useMemo(() => {
    if (!order?.createdAt) return null;
    return new Date(order.createdAt).getTime() + QR_TTL_MS;
  }, [order?.createdAt]);

  const remainingMs = expiresAt ? Math.max(0, expiresAt - now) : 0;
  const expired = expiresAt != null && remainingMs <= 0;
  const mm = Math.floor(remainingMs / 60000);
  const ss = Math.floor((remainingMs % 60000) / 1000);

  if (loading) {
    return <div style={{ padding: 80, textAlign: "center", color: "#888" }}>Đang tải đơn hàng…</div>;
  }
  if (!order) {
    return (
      <div style={{ padding: 80, textAlign: "center" }}>
        <h2>Không tìm thấy đơn hàng</h2>
        <Link href="/account/orders" style={{ textDecoration: "underline" }}>Xem đơn hàng của tôi</Link>
      </div>
    );
  }

  const isPending = order.status === "PAYMENT_PENDING";

  return (
    <section style={{ maxWidth: 920, margin: "60px auto", padding: "0 24px" }}>
      <h1 style={{ fontSize: 22, letterSpacing: 3, textAlign: "center", margin: "0 0 8px" }}>
        THANH TOÁN ĐƠN HÀNG
      </h1>
      <p style={{ textAlign: "center", color: "#666", marginBottom: 40 }}>
        Mã đơn: <strong>{order.orderNumber}</strong> · Tổng:&nbsp;
        <strong>{priceVND(order.totalAmount)}</strong>
      </p>

      {!isPending ? (
        <div style={{ background: "#e8f5e9", border: "1px solid #b8d8b9", padding: 24, textAlign: "center", borderRadius: 4 }}>
          <h3 style={{ margin: "0 0 8px" }}>Đơn đã được xử lý</h3>
          <p>Trạng thái hiện tại: <strong>{order.status}</strong></p>
          <Link href={`/track?o=${order.orderNumber}`} style={{ textDecoration: "underline" }}>
            Theo dõi đơn hàng →
          </Link>
        </div>
      ) : expired ? (
        <div style={{ background: "#fff3cd", border: "1px solid #ffe69c", padding: 24, textAlign: "center", borderRadius: 4 }}>
          <h3 style={{ margin: "0 0 8px" }}>Mã QR đã hết hạn</h3>
          <p>Mã QR có hiệu lực trong 15 phút. Vui lòng tạo đơn mới hoặc liên hệ hỗ trợ.</p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center", marginTop: 16 }}>
            <Link href="/checkout" className="co-btn-primary" style={{ padding: "10px 18px" }}>
              Tạo đơn mới
            </Link>
            <Link href="/account/orders" className="co-btn-secondary" style={{ padding: "10px 18px" }}>
              Đơn hàng của tôi
            </Link>
          </div>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 40, alignItems: "start" }}>
          <div style={{ textAlign: "center" }}>
            {order.qrCodeUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={order.qrCodeUrl}
                alt="QR thanh toán"
                width={320}
                height={320}
                style={{ width: 320, height: 320, border: "1px solid #eee", borderRadius: 6 }}
              />
            ) : (
              <div style={{ width: 320, height: 320, border: "1px dashed #ccc", display: "inline-flex", alignItems: "center", justifyContent: "center", color: "#888" }}>
                Đang tạo mã QR…
              </div>
            )}
            <div style={{ marginTop: 12, fontSize: 14, color: "#555" }}>
              Còn hiệu lực: <strong style={{ color: remainingMs < 60000 ? "#c00" : "#000" }}>
                {String(mm).padStart(2, "0")}:{String(ss).padStart(2, "0")}
              </strong>
            </div>
          </div>

          <div style={{ fontSize: 14, lineHeight: 1.8 }}>
            <h3 style={{ marginTop: 0, fontSize: 14, letterSpacing: 2 }}>HƯỚNG DẪN</h3>
            <ol style={{ paddingLeft: 18 }}>
              <li>Mở app ngân hàng → chọn <strong>Quét QR</strong></li>
              <li>Kiểm tra số tiền + nội dung chuyển đúng <strong>{order.orderNumber}</strong></li>
              <li>Xác nhận giao dịch — hệ thống sẽ tự cập nhật đơn hàng trong vài giây</li>
            </ol>

            {(order.bankCode || order.bankAccount) && (
              <div style={{ background: "#fafafa", border: "1px solid #eee", padding: 14, marginTop: 16, borderRadius: 4 }}>
                <div><strong>Ngân hàng:</strong> {order.bankCode ?? "—"}</div>
                <div><strong>Số tài khoản:</strong> {order.bankAccount ?? "—"}</div>
                <div><strong>Chủ tài khoản:</strong> {order.accountName ?? "—"}</div>
                <div><strong>Nội dung:</strong> {order.transferContent ?? order.orderNumber}</div>
                <div><strong>Số tiền:</strong> {priceVND(order.totalAmount)}</div>
              </div>
            )}

            <p style={{ marginTop: 18, fontSize: 12, color: "#888" }}>
              Trang sẽ tự cập nhật khi giao dịch hoàn tất. Bạn có thể đóng tab và quay lại
              qua <Link href="/account/orders" style={{ textDecoration: "underline" }}>Đơn hàng của tôi</Link>.
            </p>
          </div>
        </div>
      )}
    </section>
  );
}
