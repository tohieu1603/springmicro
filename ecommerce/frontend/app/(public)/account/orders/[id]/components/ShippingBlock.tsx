import type { Order } from "@/lib/api/types";

export function ShippingBlock({ order }: { order: Order }) {
  return (
    <div className="rounded border border-border-base bg-white p-5">
      <h2 className="text-h3-d mb-3">Giao đến</h2>
      <p className="text-sm font-medium">{order.recipientName}</p>
      <p className="text-sm text-slate mt-1">{order.recipientPhone}</p>
      <p className="text-sm text-slate mt-2 leading-relaxed">
        {[order.street, order.ward, order.district, order.city, order.country]
          .filter(Boolean).join(", ")}
      </p>
    </div>
  );
}
