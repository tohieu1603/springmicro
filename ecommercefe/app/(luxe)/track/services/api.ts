import axios from "axios";
import type { Tracking } from "../types";

interface OrderLite {
  orderNumber: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Client-side lookup mirroring the server-side strategy (see services/server.ts):
 *
 *   1. Shipment timeline (post-ship) — phone-gated public endpoint.
 *   2. Public order-tracking endpoint — phone-gated, works pre-ship.
 *   3. Logged-in fallback via {@code /api/proxy/api/orders/by-number/...}
 *      which carries the session cookie through the Next proxy and trusts
 *      ownership inside the BE handler.
 *
 * Without all three, a logged-in user who forgets to include {@code ?phone}
 * in the URL would see "Không tìm thấy" even though the BE has the order.
 */
export class TrackApi {
  static async lookup(orderNumber: string, phone: string): Promise<Tracking> {
    const trimmedPhone = phone?.trim() ?? "";

    if (trimmedPhone) {
      const ship = await tryFetch<Tracking>(
        `/api/proxy/api/shipments/tracking/${encodeURIComponent(orderNumber)}?phone=${encodeURIComponent(trimmedPhone)}`,
      );
      if (ship) return ship;

      const pub = await tryFetch<OrderLite>(
        `/api/proxy/api/orders/track/${encodeURIComponent(orderNumber)}?phone=${encodeURIComponent(trimmedPhone)}`,
      );
      if (pub) return toTracking(pub);
    }

    // Logged-in fallback — proxy attaches the session cookie's JWT.
    const owned = await tryFetch<OrderLite>(
      `/api/proxy/api/orders/by-number/${encodeURIComponent(orderNumber)}`,
    );
    if (owned) return toTracking(owned);

    throw Object.assign(new Error("not found"), { response: { status: 404 } });
  }
}

async function tryFetch<T>(url: string): Promise<T | null> {
  try {
    const res = await axios.get<T | { data: T }>(url, {
      withCredentials: true,
      validateStatus: (s) => s < 500,
    });
    if (res.status >= 400) return null;
    const body = res.data as { data?: T };
    return body.data ?? (res.data as T);
  } catch {
    return null;
  }
}

function toTracking(o: OrderLite): Tracking {
  const events = [
    {
      timestamp: o.createdAt ?? new Date().toISOString(),
      status: "Đặt đơn thành công",
      description: "Đơn đã được ghi nhận, đang chờ cửa hàng xử lý.",
    },
    ...statusToEvents(o),
  ];
  return { orderNumber: o.orderNumber, status: o.status, events };
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
                description: "Cửa hàng đang đóng gói." }];
    case "SHIPPED":
      return [{ timestamp: now, status: "Đang giao",
                description: "Đơn đã rời kho, đang trên đường tới bạn." }];
    case "DELIVERED":
      return [{ timestamp: now, status: "Đã giao",
                description: "Đơn đã giao thành công." }];
    case "CANCELLED":
      return [{ timestamp: now, status: "Đã huỷ" }];
    case "FAILED":
      return [{ timestamp: now, status: "Đơn lỗi" }];
    default:
      return [];
  }
}
