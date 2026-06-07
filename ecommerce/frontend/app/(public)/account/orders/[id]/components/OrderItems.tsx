import Image from "next/image";
import { formatVnd } from "@/lib/utils";
import type { Order } from "@/lib/api/types";

export function OrderItems({ order }: { order: Order }) {
  return (
    <section className="rounded border border-border-base bg-white p-5">
      <h2 className="text-h3-d mb-4">Sản phẩm</h2>
      <div className="space-y-3">
        {order.items.map((i) => (
          <div key={i.variantId} className="flex gap-3">
            <div className="relative w-16 h-16 rounded bg-surface-soft overflow-hidden shrink-0">
              <Image
                src={i.variantImage || "/img/placeholder.svg"}
                alt={i.productName}
                fill
                sizes="64px"
                className="object-contain p-1"
              />
            </div>
            <div className="flex-1">
              <p className="font-medium text-sm">{i.productName}</p>
              <p className="text-xs text-slate mt-0.5">SKU: {i.variantSku} • × {i.quantity}</p>
            </div>
            <span className="font-semibold whitespace-nowrap">
              {formatVnd(Number(i.unitPrice) * i.quantity)}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}
