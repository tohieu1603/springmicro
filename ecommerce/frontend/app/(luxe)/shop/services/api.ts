import { api } from "@/lib/api/client";
import type { BeProduct } from "../types";

interface ListResp {
  items?: BeProduct[];
  content?: BeProduct[];
  totalElements?: number;
  nextCursor?: string | null;
  pageSize?: number;
}

export class ShopApi {
  /**
   * Cursor + filter list. Catalog returns `{items, nextCursor, totalElements}`;
   * pre-cursor services return `{content, totalElements}`. Both are normalized.
   */
  static async listProducts(params: {
    size?: number;
    cursor?: string | null;
    categoryId?: string | null;
    q?: string;
    sort?: string;
  } = {}): Promise<{ items: BeProduct[]; total: number; nextCursor: string | null }> {
    const qs = new URLSearchParams();
    qs.set("size", String(params.size ?? 24));
    if (params.cursor) qs.set("cursor", params.cursor);
    if (params.categoryId) qs.set("categoryId", String(params.categoryId));
    if (params.q) qs.set("q", params.q);
    if (params.sort) qs.set("sort", params.sort);

    const res = await api.get<ListResp>(`/api/products?${qs.toString()}`);
    const items = res.data.items ?? res.data.content ?? [];
    return {
      items,
      total: res.data.totalElements ?? items.length,
      nextCursor: res.data.nextCursor ?? null,
    };
  }
}
