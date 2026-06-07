import "server-only";
import { cookies } from "next/headers";
import { fetchServerSilent } from "@/lib/api/server";
import { env } from "@/lib/env";
import type { Tracking } from "../types";

interface OrderLite {
  orderNumber: string;
  status: string;
  recipientPhone?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Server-side tracking lookup. Strategy:
 *
 *   1. If a {@code phone} is provided → guest-friendly path. Try the
 *      shipment timeline first (post-ship), then fall back to the public
 *      {@code /api/orders/track/...?phone=...} endpoint which gates by
 *      recipientPhone.
 *   2. If no phone but the user has an access cookie → logged-in path. Use
 *      the regular JWT-protected {@code /api/orders/by-number/...} endpoint
 *      which checks ownership inside the handler.
 *   3. Otherwise → return null so the page renders the form prompting for
 *      phone instead of a "not found" screen.
 */
export class TrackServerApi {
  static async lookup(orderNumber: string, phone: string): Promise<Tracking | null> {
    if (!orderNumber) return null;

    const trimmedPhone = phone?.trim() ?? "";
    if (trimmedPhone) {
      return lookupWithPhone(orderNumber, trimmedPhone);
    }

    // No phone — only viable if the caller has a session cookie. Server-side
    // serverApi auto-attaches the cookie's bearer; without it the call 401s.
    const ck = await cookies();
    if (!ck.get(env.AUTH_COOKIE_ACCESS)) return null;

    const order = await fetchServerSilent<OrderLite>(
      `/api/orders/by-number/${encodeURIComponent(orderNumber)}`,
      { next: { revalidate: 30 } } as never,
    );
    if (!order) return null;
    return toTracking(order);
  }
}

async function lookupWithPhone(orderNumber: string, phone: string): Promise<Tracking | null> {
  // Step 1 — real shipment timeline (only present after admin marks SHIPPED).
  const shipment = await fetchServerSilent<Tracking>(
    `/api/shipments/tracking/${encodeURIComponent(orderNumber)}?phone=${encodeURIComponent(phone)}`,
    { next: { revalidate: 30 } } as never,
  );
  if (shipment) return shipment;

  // Step 2 — public order endpoint gated by recipientPhone.
  const order = await fetchServerSilent<OrderLite>(
    `/api/orders/track/${encodeURIComponent(orderNumber)}?phone=${encodeURIComponent(phone)}`,
    { next: { revalidate: 30 } } as never,
  );
  if (!order) return null;
  return toTracking(order);
}

function toTracking(order: OrderLite): Tracking {
  return {
    orderNumber: order.orderNumber,
    status: order.status,
    events: [
      {
        timestamp: order.createdAt ?? new Date().toISOString(),
        status: "Đặt đơn thành công",
        description: "Đơn của bạn đã được ghi nhận, đang chờ cửa hàng xử lý.",
      },
      ...statusToEvents(order),
    ],
  };
}

function statusToEvents(o: OrderLite): { timestamp: string; status: string; description?: string }[] {
  const now = o.updatedAt ?? new Date().toISOString();
  switch (o.status) {
    case "PAYMENT_PENDING":
      return [{ timestamp: now, status: "Chờ thanh toán",
                description: "Vui lòng hoàn tất thanh toán để đơn được xử lý." }];
    case "PAYMENT_COMPLETED":
      return [{ timestamp: now, status: "Đã thanh toán",
                description: "Cửa hàng sẽ xác nhận đơn trong thời gian sớm nhất." }];
    case "CONFIRMED":
      return [{ timestamp: now, status: "Đã xác nhận",
                description: "Cửa hàng đang đóng gói. Bạn sẽ nhận được mã vận đơn khi giao." }];
    case "SHIPPED":
      return [{ timestamp: now, status: "Đang giao",
                description: "Đơn đã rời kho, đang trên đường tới bạn." }];
    case "DELIVERED":
      return [{ timestamp: now, status: "Đã giao",
                description: "Đơn đã giao thành công. Bạn có 7 ngày để yêu cầu trả hàng nếu cần." }];
    case "CANCELLED":
      return [{ timestamp: now, status: "Đã huỷ",
                description: "Đơn đã được huỷ. Liên hệ hỗ trợ nếu bạn cần thêm thông tin." }];
    case "FAILED":
      return [{ timestamp: now, status: "Đơn lỗi",
                description: "Có vấn đề khi xử lý đơn. Vui lòng liên hệ hỗ trợ." }];
    default:
      return [];
  }
}
