import { redirect } from "next/navigation";
import { Container } from "@/components/ui/container";
import { getSession } from "@/lib/auth/session";

import { MyOrdersApi } from "./services/api";
import { OrderList } from "./components";

/**
 * Server container — auth + data fetch happen here, then prop-drill into a
 * dumb OrderList. The natyf "God Hook" rule applies to CSR pages; for SC
 * pages the SC itself is the orchestrator and acts as a data-fetch hook.
 */
export default async function MyOrders() {
  const user = await getSession();
  if (!user) redirect("/login?next=/account/orders");

  const orders = await MyOrdersApi.list();

  return (
    <Container className="py-8">
      <h1 className="text-h2-d mb-6">Đơn hàng của tôi</h1>
      <OrderList orders={orders} />
    </Container>
  );
}
