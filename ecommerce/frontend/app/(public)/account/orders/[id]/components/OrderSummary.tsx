import { formatVnd } from "@/lib/utils";
import type { Order } from "@/lib/api/types";

function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-slate">{k}</span>
      <span>{v}</span>
    </div>
  );
}

export function OrderSummary({ order }: { order: Order }) {
  return (
    <div className="rounded border border-border-base bg-white p-5">
      <h2 className="text-h3-d mb-3">Tóm tắt</h2>
      <Row k="Tạm tính" v={formatVnd(order.subtotalAmount)} />
      {order.discountAmount && Number(order.discountAmount) > 0 && (
        <Row k="Giảm giá" v={`− ${formatVnd(order.discountAmount)}`} />
      )}
      {order.shippingFee && <Row k="Vận chuyển" v={formatVnd(order.shippingFee)} />}
      <div className="flex justify-between items-baseline mt-3 pt-3 border-t border-border-base">
        <span className="font-semibold">Tổng</span>
        <span className="text-xl font-bold text-accent">{formatVnd(order.totalAmount)}</span>
      </div>
    </div>
  );
}
