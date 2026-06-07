import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/utils";
import type { Order } from "@/lib/api/types";

export function OrderHeader({ order }: { order: Order }) {
  return (
    <>
      <Link href="/account/orders" className="text-sm text-primary hover:text-accent">
        ← Tất cả đơn hàng
      </Link>
      <div className="flex justify-between items-start mt-3 flex-wrap gap-3">
        <div>
          <h1 className="text-h2-d">Đơn {order.orderNumber}</h1>
          <p className="text-sm text-slate mt-1">Đặt lúc {formatDate(order.createdAt, true)}</p>
        </div>
        <Badge tone="primary">{order.status}</Badge>
      </div>
    </>
  );
}
