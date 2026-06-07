import "server-only";
import { fetchServerSilent, fetchServerOrNull } from "@/lib/api/server";
import type { BeProduct } from "../types";

/**
 * Server-side data layer for /product. Uses fetchServer (host axios with
 * cookie forwarding) instead of the browser proxy so RSC can call BE directly.
 */
export class ProductServerApi {
  static async getProduct(opts: { id?: string | null; slug?: string | null }): Promise<BeProduct | null> {
    const path = opts.id
      ? `/api/products/${encodeURIComponent(opts.id)}`
      : opts.slug
        ? `/api/products/by-slug/${encodeURIComponent(opts.slug)}`
        : null;
    if (!path) return null;
    // Cache tag = catalog:product:<id|slug>. /api/internal/revalidate can bust
    // just this product when the admin updates it (Kafka event → webhook).
    const tag = opts.id
      ? `catalog:product:${opts.id}`
      : `catalog:product-slug:${opts.slug}`;
    return fetchServerOrNull<BeProduct>(
      path,
      { next: { revalidate: 60, tags: [tag, "catalog:list"] } } as never,
    );
  }

  static async getRelated(categoryId: string | null | undefined, excludeId: string): Promise<BeProduct[]> {
    const path = categoryId
      ? `/api/products?categoryId=${categoryId}&size=10`
      : `/api/products?size=10`;
    const res = await fetchServerSilent<{ items?: BeProduct[]; content?: BeProduct[] }>(
      path,
      { next: { revalidate: 60, tags: ["catalog:list"] } } as never,
    );
    if (!res) return [];
    const list = res.items ?? res.content ?? [];
    return list.filter((p) => p.id !== excludeId).slice(0, 7);
  }
}
