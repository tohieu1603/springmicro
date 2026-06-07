import "server-only";
import { fetchServer } from "@/lib/api/server";
import type { Page, Product } from "@/lib/api/types";

export class SearchApi {
  /**
   * Hit search-service for ES-backed results; fall back to catalog `q` filter
   * if search-service is unreachable. Always returns `{products, total}` so
   * the SC doesn't need to know which path served the query.
   */
  static async run(q: string, page: number): Promise<{ products: Product[]; total: number }> {
    if (!q) return { products: [], total: 0 };

    try {
      const res = await fetchServer<Page<Product> | { content: Product[]; totalElements: number }>(
        `/api/search?q=${encodeURIComponent(q)}&page=${page}&size=24`,
      );
      return {
        products: res.content,
        total: (res as Page<Product>).totalElements ?? res.content.length,
      };
    } catch {
      const fallback = await fetchServer<Page<Product>>(
        `/api/products?q=${encodeURIComponent(q)}&page=${page}&size=24&status=ACTIVE`,
      ).catch(() => null);
      return {
        products: fallback?.content ?? [],
        total: fallback?.totalElements ?? 0,
      };
    }
  }
}
