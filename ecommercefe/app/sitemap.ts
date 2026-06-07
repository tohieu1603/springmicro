import type { MetadataRoute } from "next";

import { env } from "@/lib/env";
import { fetchServerSilent } from "@/lib/api/server";

/**
 * /sitemap.xml — generated at build time + revalidated hourly.
 *
 * Includes every public storefront URL: home, shop (all categories), every
 * ACTIVE product, static help/legal docs, vouchers, track, returns. Private
 * paths (admin/account/checkout/api) are omitted entirely — robots.txt also
 * disallows them.
 *
 * For very large catalogs (>50k items per Sitemaps protocol limit) split into
 * sitemap-products.xml / sitemap-categories.xml via Next's
 * `generateSitemaps()` route.
 */

interface BeProduct {
  id: string;
  slug?: string;
  updatedAt?: string;
}

interface ListResp {
  items?: BeProduct[];
  content?: BeProduct[];
}

const BASE = env.SITE_URL;

export const revalidate = 3600; // 1h

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const now = new Date();

  // ── 1. Static + low-frequency pages ─────────────────────────────
  const staticPages: MetadataRoute.Sitemap = [
    { path: "", priority: 1.0, freq: "daily" as const },
    { path: "/shop", priority: 0.9, freq: "daily" as const },
    { path: "/shop?cat=0", priority: 0.8, freq: "weekly" as const },
    { path: "/shop?cat=1", priority: 0.8, freq: "weekly" as const },
    { path: "/shop?cat=2", priority: 0.8, freq: "weekly" as const },
    { path: "/vouchers", priority: 0.7, freq: "daily" as const },
    { path: "/track", priority: 0.4, freq: "monthly" as const },
    { path: "/returns", priority: 0.5, freq: "monthly" as const },
    { path: "/about", priority: 0.4, freq: "monthly" as const },
    { path: "/contact", priority: 0.4, freq: "monthly" as const },
    { path: "/help/faq", priority: 0.5, freq: "monthly" as const },
    { path: "/help/shipping", priority: 0.5, freq: "monthly" as const },
    { path: "/help/returns", priority: 0.5, freq: "monthly" as const },
    { path: "/help/payment", priority: 0.5, freq: "monthly" as const },
    { path: "/legal/terms", priority: 0.3, freq: "yearly" as const },
    { path: "/legal/privacy", priority: 0.3, freq: "yearly" as const },
  ].map(({ path, priority, freq }) => ({
    url: `${BASE}${path}`,
    lastModified: now,
    changeFrequency: freq,
    priority,
  }));

  // ── 2. Product URLs from BE ─────────────────────────────────────
  // fetchServerSilent never throws — sitemap survives even if BE is down at build.
  const res = await fetchServerSilent<ListResp>(
    "/api/products?size=5000",
    { next: { revalidate: 3600, tags: ["catalog:list"] } } as never,
  );
  const items = res?.items ?? res?.content ?? [];

  const products: MetadataRoute.Sitemap = items.map((p) => ({
    url: `${BASE}/product?id=${p.id}`,
    lastModified: p.updatedAt ? new Date(p.updatedAt) : now,
    changeFrequency: "weekly",
    priority: 0.8,
  }));

  return [...staticPages, ...products];
}
