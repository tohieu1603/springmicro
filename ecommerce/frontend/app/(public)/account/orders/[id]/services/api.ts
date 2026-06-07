import "server-only";
import { fetchServerOrNull } from "@/lib/api/server";
import type { Order } from "@/lib/api/types";

export class OrderDetailApi {
  static async get(id: string | number): Promise<Order | null> {
    return fetchServerOrNull<Order>(`/api/orders/${id}`);
  }
}
