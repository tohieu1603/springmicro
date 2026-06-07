import type { Metadata } from "next";

import { ShopServerApi } from "./services/server";
import { HERO_SLOTS } from "./utils/mapper";
import { ShopClient } from "./ShopClient";

interface PageProps {
  searchParams: Promise<{ cat?: string; q?: string; sort?: string }>;
}

export async function generateMetadata({ searchParams }: PageProps): Promise<Metadata> {
  const sp = await searchParams;
  const catIdx = parseInt(sp.cat ?? "0", 10);
  const hero = HERO_SLOTS[catIdx] ?? HERO_SLOTS[0];
  const title = `${hero.title} — HIEU`;
  const desc = "Khám phá bộ sưu tập túi xách, giày, đồng hồ và phụ kiện luxe của HIEU.";
  return {
    title,
    description: desc,
    openGraph: { title, description: desc, type: "website", siteName: "HIEU" },
    twitter: { card: "summary", title, description: desc },
  };
}

/**
 * Server-rendered category listing. Fetches a 24-product page at the edge so
 * the grid renders in the initial HTML — Googlebot indexes product cards as
 * crawlable internal links, and LCP for the hero image is sub-second.
 *
 * Filter/sort are URL-driven so future filter changes trigger a fresh RSC
 * fetch on navigation rather than a client-side spinner.
 */
export default async function ShopPage({ searchParams }: PageProps) {
  const sp = await searchParams;
  const catIdx = parseInt(sp.cat ?? "0", 10);
  const { items, total } = await ShopServerApi.listProducts({
    size: 24,
    q: sp.q,
    sort: sp.sort,
  });

  return <ShopClient initialItems={items} initialTotal={total} catIdx={catIdx} />;
}
