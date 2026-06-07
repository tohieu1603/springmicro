import { notFound, redirect } from "next/navigation";
import { Container } from "@/components/ui/container";
import { getSession } from "@/lib/auth/session";

import { OrderDetailApi } from "./services/api";
import {
  CancelOrderCard,
  OrderHeader,
  OrderItems,
  OrderSummary,
  PaymentPendingBanner,
  ReturnRequestCard,
  ShippingBlock,
} from "./components";

interface PageProps {
  params: Promise<{ id: string }>;
}

/**
 * Server container — auth + fetch + drill. Five dumb sub-components handle
 * presentation; swap any of them without touching data flow.
 */
export default async function OrderDetail({ params }: PageProps) {
  const user = await getSession();
  const { id } = await params;
  if (!user) redirect(`/login?next=/account/orders/${id}`);

  const order = await OrderDetailApi.get(id);
  if (!order) notFound();

  return (
    <Container className="py-8">
      <OrderHeader order={order} />
      <PaymentPendingBanner order={order} />

      <div className="grid lg:grid-cols-[1fr,360px] gap-6 mt-6">
        <OrderItems order={order} />
        <aside className="space-y-4">
          <OrderSummary order={order} />
          <ShippingBlock order={order} />
          <CancelOrderCard order={order} />
          <ReturnRequestCard order={order} />
        </aside>
      </div>
    </Container>
  );
}
