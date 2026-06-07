import "server-only";
import { fetchServer } from "@/lib/api/server";
import type { Order, Page } from "@/lib/api/types";

export class MyOrdersApi {
  /** Server-side fetch of the current user's orders. Returns [] on failure. */
  static async list(page = 0, size = 20): Promise<Order[]> {
    const res = await fetchServer<Page<Order>>(`/api/orders/my?page=${page}&size=${size}`).catch(
      () => null,
    );
    return res?.content ?? [];
  }
}
