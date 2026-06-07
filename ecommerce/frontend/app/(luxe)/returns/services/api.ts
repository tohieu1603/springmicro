import axios from "axios";
import { api } from "@/lib/api/client";
import type { Reason } from "../types";

export class ReturnsApi {
  /** Resolve order id from the user-facing order number. Guest-safe (raw axios). */
  static async resolveOrderId(orderNumber: string): Promise<string | null> {
    try {
      const res = await axios.get<{ id?: string } | { data?: { id: string } }>(
        `/api/proxy/api/orders/by-number/${encodeURIComponent(orderNumber)}`,
      );
      const body = res.data as { id?: string; data?: { id: string } };
      return body.id ?? body.data?.id ?? null;
    } catch {
      return null;
    }
  }

  static async submit(orderId: string, payload: {
    reason: Reason;
    description: string;
    contactEmail: string;
  }) {
    await api.post(`/api/orders/return-requests/${orderId}`, payload);
  }
}
