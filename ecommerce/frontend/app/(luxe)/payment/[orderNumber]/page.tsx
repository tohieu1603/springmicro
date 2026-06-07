import type { Metadata } from "next";
import { PaymentPendingClient } from "./PaymentPendingClient";

interface PageProps {
  params: Promise<{ orderNumber: string }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { orderNumber } = await params;
  return {
    title: `Thanh toán đơn ${orderNumber} — HIEU`,
    description: "Quét QR để hoàn tất thanh toán đơn hàng.",
    robots: { index: false, follow: false },
  };
}

/**
 * Pending-payment page for SePay (bank transfer / VietQR) orders.
 *
 * The order has been created in PAYMENT_PENDING state and we're waiting for
 * the bank to notify the SePay webhook → payment-service → Kafka → order-
 * service flips the state to CONFIRMED. The client polls the order endpoint
 * every 4s and auto-redirects to /track when status changes.
 */
export default async function PaymentPendingPage({ params }: PageProps) {
  const { orderNumber } = await params;
  return <PaymentPendingClient orderNumber={orderNumber} />;
}
