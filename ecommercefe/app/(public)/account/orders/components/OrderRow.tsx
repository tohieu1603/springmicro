import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatDate, formatVnd } from "@/lib/utils";
import type { Order } from "@/lib/api/types";

import { ORDER_STATUS_LABEL, ORDER_STATUS_TONE } from "../types";

export function OrderRow({ order }: { order: Order }) {
  return (
    <Link
      href={`/account/orders/${order.id}`}
      className="block rounded border border-border-base bg-white p-4 hover:border-primary hover:shadow-soft transition-all"
    >
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <p className="font-semibold">{order.orderNumber}</p>
          <p className="text-xs text-slate mt-1">{formatDate(order.createdAt, true)}</p>
        </div>
        <Badge tone={ORDER_STATUS_TONE[order.status] ?? "neutral"}>
          {ORDER_STATUS_LABEL[order.status] ?? order.status}
        </Badge>
        <span className="font-bold text-accent">{formatVnd(order.totalAmount)}</span>
      </div>
      <p className="text-sm text-slate mt-2">
        {order.items.length} sản phẩm • {order.paymentMethod}
      </p>
    </Link>
  );
}
