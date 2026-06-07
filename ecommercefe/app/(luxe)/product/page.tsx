import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { ProductServerApi } from "./services/server";
import { ProductDetailClient } from "./ProductDetailClient";
import { ProductJsonLd } from "./components/ProductJsonLd";

interface PageProps {
  searchParams: Promise<{ id?: string; slug?: string }>;
}

/**
 * Per-product OpenGraph + Twitter card. Reuses Next.js's request cache so the
 * fetch inside page() doesn't double-hit BE.
 */
export async function generateMetadata({ searchParams }: PageProps): Promise<Metadata> {
  const sp = await searchParams;
  const product = await ProductServerApi.getProduct({ id: sp.id, slug: sp.slug });
  if (!product) {
    return { title: "HIEU — Sản phẩm không tồn tại" };
  }

  const cover = product.thumbnail
    ?? product.images?.[0]
    ?? product.variants?.find((v) => v.image)?.image
    ?? undefined;

  const minPrice = product.variants?.[0]?.salePrice ?? product.variants?.[0]?.price;
  const desc = product.description
    ? product.description.slice(0, 160)
    : `${product.brand ?? "HIEU"} · ${product.name}`;

  return {
    title: `${product.name} — HIEU`,
    description: desc,
    openGraph: {
      title: product.name,
      description: desc,
      images: cover ? [{ url: cover, width: 1200, height: 1200, alt: product.name }] : undefined,
      type: "website",
      siteName: "HIEU",
    },
    twitter: {
      card: cover ? "summary_large_image" : "summary",
      title: product.name,
      description: desc,
      images: cover ? [cover] : undefined,
    },
    alternates: { canonical: `${env.SITE_URL}/product?id=${product.id}` },
    other: minPrice != null
      ? { "product:price:amount": String(minPrice), "product:price:currency": "VND" }
      : undefined,
  };
}

/**
 * Server-rendered container.
 *
 * Fetches product + related on the server so first paint already contains the
 * full hero, name, price, description and JSON-LD — Googlebot / social
 * crawlers see real content (not "Loading…"). Interactivity (variant picker,
 * qty, add-to-bag) is delegated to the "use client" shell below.
 *
 * Cache: BE responses are tagged `revalidate: 60` so updates from the admin
 * panel propagate within a minute without manual cache busting.
 */
export default async function ProductPage({ searchParams }: PageProps) {
  const sp = await searchParams;
  const product = await ProductServerApi.getProduct({ id: sp.id, slug: sp.slug });
  if (!product) notFound();

  const related = await ProductServerApi.getRelated(product.categoryId, product.id);

  // Self-URL for JSON-LD. env.SITE_URL is normalised in lib/env.ts.
  const productUrl = `${env.SITE_URL}/product?id=${product.id}`;

  return (
    <>
      <ProductJsonLd product={product} url={productUrl} />
      <ProductDetailClient initialProduct={product} initialRelated={related} />
    </>
  );
}
