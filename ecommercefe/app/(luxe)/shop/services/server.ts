import "server-only";
import { fetchServerSilent } from "@/lib/api/server";
import type { BeProduct } from "../types";

interface ListResp {
  items?: BeProduct[];
  content?: BeProduct[];
  totalElements?: number;
}

/**
 * Server-side product listing for the /shop RSC. Returns a typed object so
 * the SC can hand it straight into the client shell without a useEffect fetch.
 */
export class ShopServerApi {
  static async listProducts(params: {
    size?: number;
    categoryId?: string | null;
    q?: string;
    sort?: string;
  } = {}): Promise<{ items: BeProduct[]; total: number }> {
    // BE catalog uses `limit` (cursor pagination), not the JPA-style `size`.
    // Sending `size` silently falls back to the controller default of 20.
    const qs = new URLSearchParams();
    qs.set("limit", String(params.size ?? 24));
    if (params.categoryId) qs.set("categoryId", String(params.categoryId));
    if (params.q) qs.set("q", params.q);
    if (params.sort) qs.set("sort", params.sort);

    const res = await fetchServerSilent<ListResp>(
      `/api/products?${qs.toString()}`,
      { next: { revalidate: 60, tags: ["catalog:list"] } } as never,
    );
    if (!res) return { items: [], total: 0 };
    const items = res.items ?? res.content ?? [];
    return { items, total: res.totalElements ?? items.length };
  }
}
