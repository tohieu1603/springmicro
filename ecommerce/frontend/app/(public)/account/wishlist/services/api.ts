import { api } from "@/lib/api/client";
import type { Product } from "@/lib/api/types";

export class WishlistApi {
  static async list(): Promise<Product[]> {
    const res = await api.get<Product[] | { data: Product[] }>("/api/users/me/wishlist");
    const body = res.data as { data?: Product[] };
    return body.data ?? (res.data as Product[]);
  }
}
