import { Button } from "@/components/ui/button";
import type { Order } from "@/lib/api/types";

/**
 * Banner shown above the order detail when payment is still pending. Branches:
 *   • SePay / bank-transfer: links to the QR page where the user can scan and
 *     wait for the webhook (15-min TTL handled there).
 *   • MoMo / other off-site gateway: deep-link out to the wallet URL.
 *   • COD: nothing — no banner.
 */
export function PaymentPendingBanner({ order }: { order: Order }) {
  if (order.status !== "PAYMENT_PENDING") return null;
  const hasQr = !!order.qrCodeUrl;
  const hasPay = !!order.payUrl;
  if (!hasQr && !hasPay) return null;

  return (
    <div className="mt-4 rounded border border-warning bg-amber-50 p-4 flex flex-wrap gap-3 items-center">
      <span className="material-symbols-outlined text-warning">payments</span>
      <p className="flex-1 text-sm">
        Đơn của bạn đang chờ thanh toán. {hasQr ? "Quét QR để hoàn tất giao dịch." : ""}
      </p>
      <Button asChild variant="cta">
        {hasQr ? (
          <a href={`/payment/${order.orderNumber}`}>Quét QR thanh toán</a>
        ) : (
          <a href={order.payUrl!}>Thanh toán ngay</a>
        )}
      </Button>
    </div>
  );
}
