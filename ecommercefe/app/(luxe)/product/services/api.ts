/**
 * Pure data-access for the product-detail page. All FE → BE calls live here
 * so the hook stays focused on state orchestration. Each method throws on
 * network errors and returns typed data on success; the hook translates
 * thrown errors into UI state.
 */

import { api } from "@/lib/api/client";
import type { BeProduct } from "../types";

export class ProductDetailApi {
  /** Fetch product by id (preferred) or slug. */
  static async getProduct(opts: { id?: string | null; slug?: string | null }): Promise<BeProduct> {
    const path = opts.id
      ? `/api/products/${encodeURIComponent(opts.id)}`
      : opts.slug
        ? `/api/products/by-slug/${encodeURIComponent(opts.slug)}`
        : null;
    if (!path) throw new Error("Missing product reference");
    const res = await api.get<BeProduct>(path);
    return res.data;
  }

  /** "You may also like" — same category, exclude the current product. */
  static async getRelated(categoryId: string | null | undefined, excludeId: string): Promise<BeProduct[]> {
    const path = categoryId
      ? `/api/products?categoryId=${categoryId}&size=10`
      : `/api/products?size=10`;
    const res = await api.get<{ items?: BeProduct[]; content?: BeProduct[] }>(path);
    const list = res.data.items ?? res.data.content ?? [];
    return list.filter((p) => p.id !== excludeId).slice(0, 7);
  }

  /**
   * Pre-flight check: re-fetch the product just before "add to bag" so a stale
   * tab can't push deleted / inactive / out-of-stock variants into the cart.
   * Returns the fresh server-side view so the caller can compare prices /
   * remaining stock against the local UI state.
   */
  static async preflight(productId: string): Promise<BeProduct> {
    return this.getProduct({ id: String(productId) });
  }

  /**
   * Add a single variant to the cart. Server validates everything; we just send.
   *
   * <p>Callers should invalidate {@code qk.cart()} after a successful add
   * (see {@code useInvalidate().cart()}). We no longer dispatch a custom
   * event here — every consumer (header badge, overlay, checkout) now
   * reads the cart through TanStack Query and reacts to invalidation.
   */
  static async addToCart(payload: { productId: string; variantId: string; quantity: number }) {
    await api.post("/api/cart/items", payload);
  }
}
