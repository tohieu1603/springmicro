import "server-only";
import { fetchServer, fetchServerOrNull } from "@/lib/api/server";
import type { Category, Page, Product, VariantAttr } from "@/lib/api/types";

/**
 * Catalog-service ships variant attrs with `valId` + `valText`. Older FE code
 * reads `attrValId` + `val`. Normalize on read so call sites keep working
 * regardless of which side gets updated first.
 */
/**
 * Normalises:
 *   1. variant.attr fields (`valId`/`valText` → also expose `attrValId`/`val`).
 *   2. List-endpoint summaries (no `variants` array, only minPrice/totalStock)
 *      → synthesise a single placeholder variant so ProductCard renders price.
 *      Detail endpoint (by-id / by-slug) ships full variants — left alone.
 */
function normalizeAttrs(p: Product): Product {
  const summary = p as Product & {
    minPrice?: number | string;
    maxPrice?: number | string;
    totalStock?: number;
  };
  if ((!p.variants || p.variants.length === 0) && summary.minPrice != null) {
    p.variants = [
      {
        id: '',
        sku: p.slug || "summary",
        price: String(summary.minPrice),
        salePrice:
          summary.maxPrice != null &&
          Number(summary.minPrice) < Number(summary.maxPrice)
            ? null
            : null,
        quantity: summary.totalStock ?? 1,
        attrs: [],
      },
    ];
  }
  for (const v of p.variants ?? []) {
    for (const a of v.attrs ?? []) {
      const x = a as VariantAttr & { valId?: string | null; valText?: string };
      if (x.valId != null && x.attrValId == null) x.attrValId = x.valId;
      if (x.valText != null && x.val == null) x.val = x.valText;
    }
  }
  return p;
}

/**
 * Catalog-service queries used by RSC pages. Returning typed shapes keeps
 * the call sites free of `any`. All requests pass the user's cookie via the
 * shared serverApi instance so admin pages can see non-ACTIVE products.
 */

export interface ListProductsParams {
  page?: number;
  size?: number;
  categoryId?: string;
  q?: string;
  status?: string;
  sort?: string;
}

/**
 * BE returns CURSOR pagination: `{items, nextCursor, pageSize, totalElements}`.
 * We adapt to the Page<T> shape FE pages have been using
 * (`{content, number, size, totalElements, totalPages}`) so call sites stay
 * unchanged. `number` is approximated from `params.page` since the cursor API
 * doesn't echo it back.
 */
interface CursorPage<T> {
  items: T[];
  nextCursor?: string | null;
  pageSize: number;
  totalElements: number;
}

export async function listProducts(params: ListProductsParams = {}): Promise<Page<Product>> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") search.set(k, String(v));
  });
  const qs = search.toString();
  const raw = await fetchServer<CursorPage<Product> | Page<Product>>(
    `/api/products${qs ? `?${qs}` : ""}`,
  );
  // Accept both shapes — cursor (from catalog-service) or page (other services).
  if ("items" in raw) {
    const size = raw.pageSize || params.size || 20;
    return {
      content: raw.items.map(normalizeAttrs),
      number: params.page ?? 0,
      size,
      totalElements: raw.totalElements,
      totalPages: Math.max(1, Math.ceil(raw.totalElements / Math.max(1, size))),
    };
  }
  return { ...raw, content: raw.content.map(normalizeAttrs) };
}

export async function getProductBySlug(slug: string): Promise<Product | null> {
  const p = await fetchServerOrNull<Product>(`/api/products/by-slug/${encodeURIComponent(slug)}`);
  return p ? normalizeAttrs(p) : null;
}

export async function getProductById(id: string | string): Promise<Product | null> {
  const p = await fetchServerOrNull<Product>(`/api/products/${id}`);
  return p ? normalizeAttrs(p) : null;
}

export async function listCategories(): Promise<Category[]> {
  // Backend returns a flat list; the tree builder lives in lib/categories.ts.
  return fetchServer<Category[]>(`/api/categories`);
}
