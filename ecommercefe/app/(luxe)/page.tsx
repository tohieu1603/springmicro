import type { Metadata } from "next";

import HeroSlideshow from "@/components/luxe/HeroSlideshow";
import FlashSaleStrip from "@/components/storefront/FlashSaleStrip";
import {
  CategoryGrid,
  ServicesSection,
  SubscribeSection,
} from "@/components/luxe/HomeSections";

/**
 * Home OG card + Twitter. Hero is a CSR island that auto-rotates, but Next's
 * App Router pre-renders its initial slide server-side so first paint already
 * shows real content (no skeleton).
 */
export const metadata: Metadata = {
  title: "HIEU — Maison de Couture",
  description:
    "Bộ sưu tập túi xách, giày, đồng hồ và phụ kiện luxe của HIEU. Thủ công tại Ý, giao hàng toàn quốc.",
  openGraph: {
    title: "HIEU — Maison de Couture",
    description:
      "Bộ sưu tập túi xách, giày, đồng hồ và phụ kiện luxe của HIEU. Thủ công tại Ý, giao hàng toàn quốc.",
    type: "website",
    siteName: "HIEU",
  },
  twitter: {
    card: "summary_large_image",
    title: "HIEU — Maison de Couture",
    description: "Bộ sưu tập túi xách, giày, đồng hồ và phụ kiện luxe của HIEU.",
  },
};

/**
 * HIEU home — Server Component. Emits HTML for the hero, category grid,
 * services band and newsletter on the first request. Sub-components that
 * need interactivity (HeroSlideshow auto-rotate, Subscribe form) are
 * "use client" islands that hydrate without blocking first paint.
 */
export default function HomePage() {
  return (
    <>
      <HeroSlideshow />
      <FlashSaleStrip />
      <CategoryGrid />
      <ServicesSection />
      <SubscribeSection />
    </>
  );
}
