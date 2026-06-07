import { hashId } from "@/lib/hashId";
import type { IllustStyle } from "@/lib/illustrations";
import type { BeProduct, HeroSlot, LuxeProduct } from "../types";

export function priceVND(v?: number | string | null): string {
  if (v == null) return "";
  const n = typeof v === "string" ? Number(v) : v;
  return Number.isFinite(n)
    ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(n)
    : "";
}

const STYLES: IllustStyle[] = ["hobo", "topHandle", "bucket", "chevron"];

/**
 * Map a BE product → ProductCard model. Collects thumbnail + product images +
 * variant images (deduped) so the card hover-rotate has multiple frames.
 *
 * The list endpoint usually returns only a thumbnail (variants are loaded on
 * PDP), so we'd render a single frame and the card's hover arrows would stay
 * hidden. To preserve the hover-cycle UX, duplicate the thumbnail with a
 * different crop hint so the gallery has at least 2 visible states.
 */
export function toLuxeProduct(p: BeProduct, idx: number): LuxeProduct {
  const images: string[] = [];
  if (p.thumbnail) images.push(p.thumbnail);
  for (const img of p.images ?? []) if (img && !images.includes(img)) images.push(img);
  // New: BE list endpoint now ships every variant image so we don't have to
  // fall back to the duplicated-thumbnail trick when products have real photos.
  for (const url of p.variantImages ?? []) if (url && !images.includes(url)) images.push(url);
  for (const v of p.variants ?? []) if (v.image && !images.includes(v.image)) images.push(v.image);

  let imageUrls: string[] | undefined;
  if (images.length >= 2) imageUrls = images.slice(0, 4);
  else if (images.length === 1) {
    // Same Unsplash photo, different crop param — gives the hover-cycle a
    // visible second state without changing the BE schema.
    const base = images[0];
    const alt = base.includes("?")
      ? `${base}&fit=crop&crop=entropy`
      : `${base}?fit=crop&crop=entropy`;
    imageUrls = [base, alt];
  } else {
    imageUrls = undefined;
  }

  const minPrice = p.minPrice ?? p.variants?.[0]?.price;
  const palette = (hashId(p.id ?? idx) * 31) % 8;
  return {
    id: String(p.id),
    name: p.name,
    price: minPrice != null ? priceVND(minPrice) : "",
    imageUrls,
    baseStyle: STYLES[hashId(p.id ?? idx) % STYLES.length],
    palette,
    tag: p.brand,
  };
}

export const HERO_SLOTS: HeroSlot[] = [
  { style: "hobo",    palette: 0, title: "Handbags for Women" },
  { style: "shoe",    palette: 2, title: "Shoes Collection" },
  { style: "chevron", palette: 4, title: "Men's Bags" },
];

export const TABS = ["ALL", "Handbags", "Shoes", "Watches", "Wallets", "Accessories"];
