import Link from "next/link";
import { Empty } from "@/components/ui/empty";
import type { Order } from "@/lib/api/types";

import { OrderRow } from "./OrderRow";

export function OrderList({ orders }: { orders: Order[] }) {
  if (orders.length === 0) {
    return (
      <Empty
        title="Chưa có đơn hàng"
        description="Khám phá sản phẩm và đặt đơn đầu tiên nhé."
        cta={<Link href="/shop" className="text-accent font-semibold">Mua sắm ngay</Link>}
      />
    );
  }
  return (
    <div className="space-y-3">
      {orders.map((o) => (
        <OrderRow key={o.id} order={o} />
      ))}
    </div>
  );
}
